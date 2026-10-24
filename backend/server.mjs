import {createServer} from 'node:http';
import {readFile} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import {dirname,join} from 'node:path';
import {WebSocket,WebSocketServer} from 'ws';
import {newRoom,join as joinRoom,participantByToken,publicState,applyAction,now} from './logic.mjs';
import {CipherMediaStore} from './media.mjs';
import {CipherChunkStore,CHUNK_SIZE} from './chunks.mjs';

const port=Number(process.env.PORT||8787);
const origin=(process.env.PUBLIC_ORIGIN||'').replace(/\/$/,'');
if(process.env.NODE_ENV==='production' && !origin.startsWith('https://')) throw Error('PUBLIC_ORIGIN must use HTTPS');
const base=origin || `http://localhost:${port}`;
const rooms=new Map(),sockets=new Map(),rate=new Map();
const cipherStore=new CipherMediaStore();
const chunkStore=new CipherChunkStore();
const assetsDir=join(dirname(fileURLToPath(import.meta.url)),'public');
const wss=new WebSocketServer({noServer:true,maxPayload:8192,perMessageDeflate:false});
const headers={
 'Cache-Control':'no-store','Referrer-Policy':'no-referrer','X-Content-Type-Options':'nosniff',
 'X-Frame-Options':'DENY','Cross-Origin-Resource-Policy':'same-origin',
 'Content-Security-Policy':"default-src 'self'; connect-src 'self' ws: wss:; style-src 'self'; script-src 'self'; img-src 'self' blob:; media-src 'self' blob:; base-uri 'none'; form-action 'self'",
};
function respond(res,status,data) {res.writeHead(status,{...headers,'Content-Type':'application/json; charset=utf-8'});res.end(JSON.stringify(data));}
function bad(res,msg,status=400) {respond(res,status,{error:msg});}
function limit(ip,kind='normal') {const key=kind+':'+ip,t=now(),x=rate.get(key)||{start:t,count:0};if(t-x.start>60_000){x.start=t;x.count=0;}x.count++;rate.set(key,x);return x.count<=(kind==='chunk'?500:100);}
async function body(req,max=16384) {
 let size=0;const parts=[];for await(const chunk of req){size+=chunk.length;if(size>max) throw Error('too large');parts.push(chunk);}
 return Buffer.concat(parts);
}
function broadcast(room) {
 const data=JSON.stringify({type:'state',data:publicState(room)});
 for(const socket of sockets.get(room.id)||[]) if(socket.readyState===WebSocket.OPEN)socket.send(data);
}
function getMember(req,room) {
 const match=/^Bearer ([A-Za-z0-9_-]{32,})$/.exec(req.headers.authorization||'');
 return match && participantByToken(room,match[1]);
}
const http=createServer(async (req,res)=>{
 const ip=req.socket.remoteAddress||'unknown';
 const chunkRequest=/^\/api\/rooms\/[0-9a-f-]{36}\/(?:uploads|media)\/[0-9a-f-]{36}\/chunks\/[0-9]+$/.test(req.url.split('?')[0]);
 if(!limit(ip,chunkRequest?'chunk':'normal'))return bad(res,'too many requests',429);
 const url=new URL(req.url,base);
 if(req.method==='POST'&&url.pathname==='/api/rooms') {
  try {const json=JSON.parse((await body(req)).toString('utf8'));
   if(rooms.size>=500)return bad(res,'server busy',503);
   const room=newRoom(json.mode,json.level,json.nickname,now(),{rounds:json.rounds,variety:json.variety});rooms.set(room.id,room);
   const host=room.participants[0];
   return respond(res,201,{roomId:room.id,participantId:host.id,auth:host.auth,
    inviteUrl:`${base}/join/${room.id}#invite=${encodeURIComponent(room.invite)}`,expiresAt:room.expiresAt});
  }catch{return bad(res,'room creation failed');}
 }
 const joinMatch=/^\/api\/rooms\/([0-9a-f-]{36})\/join$/.exec(url.pathname);
 if(req.method==='POST'&&joinMatch){
  const room=rooms.get(joinMatch[1]);if(!room)return bad(res,'room not found',404);
  try{const json=JSON.parse((await body(req)).toString('utf8'));const p=joinRoom(room,json.invite,json.nickname);broadcast(room);
   return respond(res,200,{roomId:room.id,participantId:p.id,auth:p.auth,expiresAt:room.expiresAt});
  }catch{return bad(res,'invalid invitation',403);}
 }
 // M6: resumable opaque chunk uploads; every encrypted chunk is authenticated by the clients.
 const uploadMatch=/^\/api\/rooms\/([0-9a-f-]{36})\/uploads(?:\/([0-9a-f-]{36})(?:\/(chunks\/([0-9]+)|complete))?)?$/.exec(url.pathname);
 if(uploadMatch){
  const room=rooms.get(uploadMatch[1]),member=room&&getMember(req,room);
  if(!room||!member||room.expiresAt<=now())return bad(res,'not authorized',403);
  const id=uploadMatch[2],action=uploadMatch[3],index=uploadMatch[4];
  try{
   if(req.method==='POST'&&!id){const data=JSON.parse((await body(req,2048)).toString('utf8'));
    return respond(res,201,chunkStore.create(room,member,data));}
   if(!id)return bad(res,'method not allowed',405);
   if(req.method==='GET'&&!action)return respond(res,200,chunkStore.snapshot(chunkStore.owned(room,member,id)));
   if(req.method==='DELETE'&&!action){chunkStore.cancel(room,member,id);return respond(res,200,{cancelled:true});}
   if(req.method==='PUT'&&index!==undefined){
    if(Number(req.headers['content-length'])>CHUNK_SIZE+16)return bad(res,'chunk too large',413);
    const encrypted=await body(req,CHUNK_SIZE+16);
    return respond(res,200,chunkStore.put(room,member,id,Number(index),req.headers['x-encrypted-iv'],encrypted));
   }
   if(req.method==='POST'&&action==='complete'){
    const result=chunkStore.complete(room,member,id);room.seq++;broadcast(room);return respond(res,201,result);}
  }catch{return bad(res,'chunk operation rejected',400);}
  return bad(res,'method not allowed',405);
 }
 const chunkDownload=/^\/api\/rooms\/([0-9a-f-]{36})\/media\/([0-9a-f-]{36})\/(manifest|chunks\/([0-9]+))$/.exec(url.pathname);
 if(req.method==='GET'&&chunkDownload){
  const room=rooms.get(chunkDownload[1]),member=room&&getMember(req,room);
  if(!room||!member||room.expiresAt<=now())return bad(res,'not authorized',403);
  if(chunkDownload[3]==='manifest'){
   const manifest=chunkStore.manifest(room,member,chunkDownload[2]);
   return manifest?respond(res,200,manifest):bad(res,'media not available',404);
  }
  const part=chunkStore.chunk(room,member,chunkDownload[2],Number(chunkDownload[4]));
  if(!part)return bad(res,'media not available',404);
  res.writeHead(200,{...headers,'Content-Type':'application/octet-stream',
   'Content-Length':part.data.length,'X-Encrypted-Iv':part.iv,'X-Media-Kind':part.kind});
  return res.end(part.data);
 }
 const mediaMatch=/^\/api\/rooms\/([0-9a-f-]{36})\/media(?:\/([0-9a-f-]{36}))?$/.exec(url.pathname);
 if(mediaMatch && ['POST','GET'].includes(req.method)) {
  const room=rooms.get(mediaMatch[1]);const member=room&&getMember(req,room);
  if(!room||!member||room.expiresAt<=now())return bad(res,'not authorized',403);
  if(req.method==='POST'&&!mediaMatch[2]){
   try {
    const kind=req.headers['x-media-kind'];const iv=req.headers['x-encrypted-iv'];
    if(typeof kind!=='string'||typeof iv!=='string')return bad(res,'missing metadata');
    if(Number(req.headers['content-length'])>cipherStore.fileLimit)return bad(res,'file too large',413);
    const encrypted=await body(req,cipherStore.fileLimit);
    const media=cipherStore.add(room,member,kind,iv,encrypted);
    room.seq++;broadcast(room);return respond(res,201,{id:media.id,expiresAt:media.expiresAt});
   }catch{return bad(res,'upload rejected',413);}
  }
  if(req.method==='GET'&&mediaMatch[2]){
   const m=cipherStore.get(room,member,mediaMatch[2]);if(!m)return bad(res,'media not available',404);
   if(m.kind==='card'){cipherStore.consume(room,member,m.id);room.seq++;broadcast(room);}
   res.writeHead(200,{...headers,'Content-Type':'application/octet-stream',
    'Content-Length':m.ciphertext.length,'X-Encrypted-Iv':m.iv,'X-Media-Kind':m.kind});return res.end(m.ciphertext);
  }
  return bad(res,'method not supported',405);
 }
 if(req.method==='GET'&&/^\/join\/[0-9a-f-]{36}$/.test(url.pathname)) {
  const data=await readFile(join(assetsDir,'index.html'));
  res.writeHead(200,{...headers,'Content-Type':'text/html; charset=utf-8'});return res.end(data);
 }
 const staticFiles={'/m6-transfer.js':['m6-transfer.js','text/javascript; charset=utf-8'],
  '/app.js':['app.js','text/javascript; charset=utf-8'],
  '/style.css':['style.css','text/css; charset=utf-8'],
  '/quizzes.json':['quizzes.json','application/json; charset=utf-8']};
 if(req.method==='GET'&&staticFiles[url.pathname]) {
  const [file,mime]=staticFiles[url.pathname];const data=await readFile(join(assetsDir,file));
  res.writeHead(200,{...headers,'Content-Type':mime});return res.end(data);
 }
 if(req.method==='GET'&&url.pathname==='/health')return respond(res,200,{ok:true});
 return bad(res,'not found',404);
});
http.on('upgrade',(req,socket,head)=>{
 if(!limit(req.socket.remoteAddress||'unknown'))return socket.destroy();
 const url=new URL(req.url,base);
 if(url.pathname!=='/ws'||url.search)return socket.destroy();
 if(origin&&req.headers.origin&&req.headers.origin!==origin)return socket.destroy();
 wss.handleUpgrade(req,socket,head,ws=>{
  let room=null,member=null,authenticated=false;
  const authTimeout=setTimeout(()=>ws.close(4003,'authentication required'),5000);
  ws.on('message',raw=>{
   let data;try{data=JSON.parse(String(raw));}catch{return ws.close(4002,'invalid data');}
   if(!authenticated){
    if(data.type!=='authenticate')return ws.close(4003,'authenticate first');
    if(data.contentVersion!==3 || data.protocolVersion!==4)return ws.close(4009,'protocol version mismatch');
    room=rooms.get(data.roomId);
    member=room&&participantByToken(room,data.auth);
    if(!room||!member||room.expiresAt<=now()||['REVOKED','EXPIRED','ENDED'].includes(room.status))return ws.close(4003,'not authorized');
    authenticated=true;clearTimeout(authTimeout);
    // Only terminate earlier connections after successful authentication.
    for(const prior of sockets.get(room.id)||[])if(prior.memberId===member.id)prior.close(4001,'session moved');
    ws.memberId=member.id;ws.roomId=room.id;ws.actionsAt=now();ws.actions=0;
    if(!sockets.has(room.id))sockets.set(room.id,new Set());
    sockets.get(room.id).add(ws);member.connected=true;room.seq++;
    ws.send(JSON.stringify({type:'authenticated'}));broadcast(room);return;
   }
   const t=now();if(t-ws.actionsAt>60_000){ws.actionsAt=t;ws.actions=0;}
   if(++ws.actions>90)return ws.close(4008,'rate limited');
   try {const updated=applyAction(room,member,data);
    if(updated){if(['ENDED','REVOKED'].includes(room.status)){cipherStore.removeRoom(room.id);chunkStore.removeRoom(room.id);}broadcast(room);}
    else ws.send(JSON.stringify({type:'ack',eventId:data.eventId,duplicate:true}));
   }catch{if(ws.readyState===WebSocket.OPEN)ws.send(JSON.stringify({type:'error',message:'تعذر تنفيذ الأمر أو انتهت الجلسة'}));}
  });
  ws.on('close',()=>{
   clearTimeout(authTimeout);
   if(!authenticated)return;
   sockets.get(room.id)?.delete(ws);
   if(![...(sockets.get(room.id)||[])].some(x=>x.memberId===member.id)){
    member.connected=false;room.seq++;broadcast(room);
   }
  });
 });
});
const cleanup=setInterval(()=>{
 const t=now();cipherStore.prune(t);chunkStore.prune(t);
 for(const [id,room] of rooms){
  if(room.expiresAt<=t){room.status='EXPIRED';room.media=[];room.quiz=null;
   cipherStore.removeRoom(id);chunkStore.removeRoom(id);broadcast(room);
   for(const s of sockets.get(id)||[])s.close(4000,'expired');sockets.delete(id);rooms.delete(id);
  }else if(['ENDED','REVOKED'].includes(room.status)&&(room.closedAt||room.createdAt)+5*60_000<t){
   cipherStore.removeRoom(id);chunkStore.removeRoom(id);sockets.delete(id);rooms.delete(id);
  }
 }
 for(const [ip,x] of rate)if(t-x.start>120_000)rate.delete(ip);
},30_000);cleanup.unref();
http.listen(port,()=>process.stdout.write(`Sirr relay listening on port ${port}\n`));
