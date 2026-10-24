import test from 'node:test';
import assert from 'node:assert/strict';
import {spawn} from 'node:child_process';
import {createServer as createTcpServer} from 'node:net';
import {setTimeout as sleep} from 'node:timers/promises';
import {fileURLToPath} from 'node:url';
import {dirname,resolve} from 'node:path';
import WebSocket from 'ws';
import {newRoom,applyAction} from '../logic.mjs';

async function unusedLocalPort(){
  const listener=createTcpServer();
  await new Promise((resolve,reject)=>listener.once('error',reject).listen(0,'127.0.0.1',resolve));
  const port=listener.address().port;
  await new Promise((resolve,reject)=>listener.close(err=>err?reject(err):resolve()));
  return port;
}
async function until(check,description){
  for(let n=0;n<120;n++){
    const value=check();
    if(value)return value;
    await sleep(50);
  }
  throw Error('Timeout: '+description);
}

test('recent idempotency survives bounded 2048-event history',()=>{
  const room=newRoom('couple','SOFT','host');
  const host=room.participants[0];
  for(let i=0;i<2050;i++) {
    assert.equal(applyAction(room,host,{type:'consent',eventId:'m9_history_'+i,value:true}),true);
  }
  assert.equal(room.events.size,2048);
  assert.equal(applyAction(room,host,{type:'consent',eventId:'m9_history_2049',value:true}),false);
});

test('large encrypted configuration receives success and duplicate ACKs over WebSocket', {timeout:20000}, async t=>{
  const port=await unusedLocalPort();
  const base='http://127.0.0.1:'+port;
  const dir=resolve(dirname(fileURLToPath(import.meta.url)),'..');
  const child=spawn(process.execPath,['server.mjs'],{
    cwd:dir,
    env:{...process.env,PORT:String(port),PUBLIC_ORIGIN:base,NODE_ENV:'test'},
    stdio:['ignore','pipe','pipe']
  });
  let exited=false,stderr='';
  child.on('exit',()=>{exited=true});
  child.stderr.on('data',b=>{stderr+=b.toString()});
  let ws;
  t.after(()=>{ws?.terminate();if(!exited)child.kill('SIGTERM')});

  let ready=false;
  for(let i=0;i<100;i++){
    if(exited)throw Error('Relay exited: '+stderr);
    try {
      const response=await fetch(base+'/health');
      if(response.ok){ready=true;break}
    } catch {}
    await sleep(50);
  }
  assert.equal(ready,true,'Relay did not start: '+stderr);
  const response=await fetch(base+'/api/rooms',{
    method:'POST',headers:{'Content-Type':'application/json'},
    body:JSON.stringify({nickname:'host',mode:'couple',level:'SOFT',rounds:10,variety:'ONE_LEVEL'})
  });
  assert.equal(response.status,201);
  const room=await response.json();
  ws=new WebSocket('ws://127.0.0.1:'+port+'/ws');
  const messages=[];
  ws.on('message',raw=>{messages.push(JSON.parse(String(raw)))});
  await new Promise((resolve,reject)=>{ws.once('open',resolve);ws.once('error',reject)});
  ws.send(JSON.stringify({type:'authenticate',roomId:room.roomId,auth:room.auth,
    contentVersion:4,protocolVersion:4}));
  await until(()=>messages.find(m=>m.type==='authenticated'),'WebSocket authentication');
  const custom=Array.from({length:20},(_,i)=>({
    iv:'iv_'+String(i).padStart(13,'0'),
    cipher:'A'.repeat(700)
  }));
  const action={type:'configure',eventId:'m9_long_config_001',rounds:10,
    variety:'ONE_LEVEL',level:'SOFT',custom};
  const frame=JSON.stringify(action);
  assert.ok(Buffer.byteLength(frame,'utf8')>8192);
  assert.ok(Buffer.byteLength(frame,'utf8')<64*1024);
  ws.send(frame);
  const first=await until(()=>messages.find(m=>m.type==='ack'&&m.eventId===action.eventId),
    'first successful configuration ACK');
  assert.equal(first.duplicate,false);
  await until(()=>messages.find(m=>m.type==='state'&&m.data?.customCount===20),
    'configuration applied');
  ws.send(frame);
  const duplicate=await until(()=>messages.find(m=>m.type==='ack'&&m.eventId===action.eventId&&m.duplicate===true),
    'duplicate ACK');
  assert.equal(duplicate.duplicate,true);
});
