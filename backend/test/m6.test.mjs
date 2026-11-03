import test from 'node:test';
import assert from 'node:assert/strict';
import {randomBytes,createCipheriv,createDecipheriv} from 'node:crypto';
import {newRoom,join,applyAction,publicState} from '../logic.mjs';
import {CipherChunkStore,CHUNK_SIZE,MAX_CHUNK_FILE} from '../chunks.mjs';
let seq=0;
function roomReady(){const room=newRoom('couple','SOFT','مضيف',1000),host=room.participants[0];host.connected=true;
 const guest=join(room,room.invite,'ضيف',1001);guest.connected=true;
 applyAction(room,guest,{type:'consent',value:true,eventId:'m6_'+(++seq)+'_consent'},1200);
 applyAction(room,host,{type:'start',eventId:'m6_'+(++seq)+'_start'},1201);
 return {room,host,guest};}
function crypt(room,id,index,kind,clear,key=randomBytes(32)){
 const iv=randomBytes(12),cipher=createCipheriv('aes-256-gcm',key,iv);
 cipher.setAAD(Buffer.from(room.id+':chunk:'+id+':'+index+':'+kind));
 return {iv:iv.toString('base64url'),encrypted:Buffer.concat([cipher.update(clear),cipher.final(),cipher.getAuthTag()]),key};
}
test('M6 chunk creation, idempotent retry, verified decryption and release',()=>{
 const {room,host,guest}=roomReady(),store=new CipherChunkStore(),plain=randomBytes(CHUNK_SIZE+44);
 const item=store.create(room,host,{kind:'video',totalBytes:plain.length},1400);
 assert.equal(item.count,2);assert.deepEqual(item.received,[]);
 const first=crypt(room,item.id,0,'video',plain.subarray(0,CHUNK_SIZE));
 store.put(room,host,item.id,0,first.iv,first.encrypted,1401);
 assert.deepEqual(store.put(room,host,item.id,0,first.iv,first.encrypted,1402).received,[0]);
 assert.throws(()=>store.put(room,host,item.id,0,crypt(room,item.id,0,'video',plain.subarray(0,CHUNK_SIZE)).iv,first.encrypted,1404),/conflict/);
 assert.throws(()=>store.complete(room,host,item.id,1404),/missing chunks/);
 const second=crypt(room,item.id,1,'video',plain.subarray(CHUNK_SIZE),first.key);
 store.put(room,host,item.id,1,second.iv,second.encrypted,1404);store.complete(room,host,item.id,1405);
 assert.equal(publicState(room,1406).media[0].transfer,'chunked');
 assert.equal(store.manifest(room,guest,item.id,1406).count,2);
 const decrypted=[first,second].map((p,i)=>{
   const part=store.chunk(room,guest,item.id,i,1406),iv=Buffer.from(part.iv,'base64url');
   const d=createDecipheriv('aes-256-gcm',first.key,iv);
   d.setAAD(Buffer.from(room.id+':chunk:'+item.id+':'+i+':video'));
   d.setAuthTag(part.data.subarray(-16));return Buffer.concat([d.update(part.data.subarray(0,-16)),d.final()]);
 });
 assert.deepEqual(Buffer.concat(decrypted),plain);
 store.removeRoom(room.id);assert.equal(store.used,0);
});
test('M6 permissions, sizes and cancellation',()=>{
 const {room,host,guest}=roomReady(),store=new CipherChunkStore();
 const item=store.create(room,host,{kind:'audio',totalBytes:100},1400);
 const p=crypt(room,item.id,0,'audio',randomBytes(100));
 assert.throws(()=>store.put(room,guest,item.id,0,p.iv,p.encrypted,1401));
 assert.throws(()=>store.put(room,host,item.id,2,p.iv,p.encrypted,1401));
 assert.throws(()=>store.put(room,host,item.id,0,p.iv,p.encrypted.subarray(0,99),1401));
 assert.throws(()=>store.create(room,host,{kind:'video',totalBytes:MAX_CHUNK_FILE+1},1400));
 store.put(room,host,item.id,0,p.iv,p.encrypted,1401);
 assert.throws(()=>store.cancel(room,guest,item.id,1402));
 store.cancel(room,host,item.id,1402);assert.equal(store.used,0);
});
test('M6 expiry frees staged and completed ciphertext',()=>{
 const {room,host}=roomReady(),store=new CipherChunkStore();
 const a=store.create(room,host,{kind:'video',totalBytes:32},1400);
 const piece=crypt(room,a.id,0,'video',randomBytes(32));
 store.put(room,host,a.id,0,piece.iv,piece.encrypted,1401);
 const expiry=store.owned(room,host,a.id,1402).expiresAt;
 store.prune(expiry);assert.equal(store.used,0);
 const b=store.create(room,host,{kind:'audio',totalBytes:32},1401);
 const part=crypt(room,b.id,0,'audio',randomBytes(32));
 store.put(room,host,b.id,0,part.iv,part.encrypted,1402);
 store.complete(room,host,b.id,1403);assert.ok(store.used>0);
 store.prune(expiry+1);assert.equal(store.completed.size,0);assert.equal(store.used,0);
});
test('M6 concurrent uploads limited to four per room',()=>{
 const {room,host}=roomReady(),store=new CipherChunkStore();
 const ids=[0,1,2,3].map(()=>store.create(room,host,{kind:'video',totalBytes:CHUNK_SIZE},1400));
 assert.throws(()=>store.create(room,host,{kind:'video',totalBytes:100},1401));
 store.cancel(room,host,ids[0].id,1402);
 assert.ok(store.create(room,host,{kind:'video',totalBytes:100},1403).id);
});
