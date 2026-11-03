import test from 'node:test';
import assert from 'node:assert/strict';
import {randomBytes,createCipheriv,createDecipheriv,createHmac} from 'node:crypto';
import {newRoom,join,applyAction,publicState} from '../logic.mjs';
import {CipherMediaStore} from '../media.mjs';
let eventCounter=0;
const act=(r,p,type,extra={})=>applyAction(r,p,{type,eventId:`m3_test_event_${++eventCounter}_unique`,...extra},1200);
function room(){
 const r=newRoom('couple','NORMAL','المضيف',1000),host=r.participants[0];host.connected=true;
 const guest=join(r,r.invite,'الضيف',1100);guest.connected=true;
 act(r,guest,'consent',{value:true});return {r,host,guest};
}
test('quiz: private HMAC answers, consent and aggregate only',()=>{
 const key=Buffer.alloc(32,7);
 const tags=(r,values)=>values.map((v,i)=>createHmac('sha256',key)
    .update(['sirr','v4','quiz',r.id,r.quiz.id,r.quiz.indices[i],v].join(':')).digest('base64url'));
 const {r,host,guest}=room();
 assert.throws(()=>act(r,guest,'quiz_open',{pack:0}));
 act(r,host,'quiz_open',{pack:3});
 assert.equal(publicState(r,1300).quiz.pack,3);
 assert.throws(()=>act(r,guest,'quiz_submit',{quizId:r.quiz.id,answers:[1,1]}));
 assert.throws(()=>act(r,guest,'quiz_submit',{quizId:r.quiz.id,answers:[-1,0,0,0,0,0,0,0]}));
 act(r,host,'quiz_submit',{quizId:r.quiz.id,tags:tags(r,[0,1,2,1,1,0,2,2])});
 assert.equal(publicState(r,1300).quiz.submitted.length,1);
 assert.ok(!JSON.stringify(publicState(r,1300)).includes('answers'));
 assert.throws(()=>act(r,host,'quiz_submit',{quizId:r.quiz.id,answers:Array(8).fill(1)}));
 act(r,guest,'quiz_submit',{quizId:r.quiz.id,tags:tags(r,[0,1,1,0,1,0,2,2])});
 assert.equal(r.quiz.result.agreement,6);
 assert.equal(r.quiz.result.scores,undefined);
 assert.deepEqual(r.quiz.submissions.map(s=>Object.keys(s)),[['id'],['id']]);
 act(r,host,'quiz_close');assert.equal(publicState(r,1300).quiz,null);
});
test('quiz host can replace pack and cannot use invalid pack or submit after close',()=>{
 const {r,host,guest}=room();
 assert.throws(()=>act(r,host,'quiz_open',{pack:7}));
 act(r,host,'quiz_open',{pack:0});const previous=r.quiz.id;
 act(r,host,'quiz_open',{pack:1});assert.notEqual(r.quiz.id,previous);
 assert.throws(()=>act(r,guest,'quiz_submit',{quizId:previous,answers:Array(8).fill(1)}));
});
test('cipher media: actor only, encrypted-only, quota, expiry, room isolation and revoke',()=>{
 const {r,host,guest}=room();
 const store=new CipherMediaStore({fileLimit:1024,totalLimit:2048,perRoomLimit:1500});
 const iv=randomBytes(12).toString('base64url');const cipher=Buffer.alloc(900,1);
 assert.throws(()=>store.add(r,host,'image',iv,cipher,1300));
 act(r,host,'start');
 assert.throws(()=>store.add(r,guest,'image',iv,cipher,1300));
 const actor=r.participants.find(p=>p.id===r.actorId);
 const media=store.add(r,actor,'image',iv,cipher,1300);
 assert.ok(store.get(r,guest,media.id,1350));
 assert.equal(publicState(r,1400).media[0].size,900);
 assert.throws(()=>store.add(r,actor,'image',iv,cipher,1350));
 assert.equal(store.get(r,host,media.id,1300+15*60_000),null);
 store.prune(1300+15*60_000);assert.equal(store.used,0);
 store.add(r,actor,'audio',iv,cipher,1300);
 store.removeRoom(r.id);assert.equal(store.used,0);
});
test('cipher media: WebCrypto-compatible AES-GCM shape with AAD and integrity failure',()=>{
 const key=randomBytes(32),iv=randomBytes(12),roomId='abc',kind='audio';
 const mime=Buffer.from('audio/ogg');const original=Buffer.concat([Buffer.from([mime.length]),mime,Buffer.from('a short private clip')]);
 const enc=createCipheriv('aes-256-gcm',key,iv);enc.setAAD(Buffer.from(`${roomId}:${kind}`));
 const cipher=Buffer.concat([enc.update(original),enc.final(),enc.getAuthTag()]);
 const dec=createDecipheriv('aes-256-gcm',key,iv);dec.setAAD(Buffer.from(`${roomId}:${kind}`));
 dec.setAuthTag(cipher.subarray(-16));
 assert.deepEqual(Buffer.concat([dec.update(cipher.subarray(0,-16)),dec.final()]),original);
 const bad=createDecipheriv('aes-256-gcm',key,iv);bad.setAAD(Buffer.from(`${roomId}:image`));bad.setAuthTag(cipher.subarray(-16));
 assert.throws(()=>Buffer.concat([bad.update(cipher.subarray(0,-16)),bad.final()]));
});
