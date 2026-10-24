import test from 'node:test';
import assert from 'node:assert/strict';
import {createHmac,createCipheriv} from 'node:crypto';
import {newRoom,join,applyAction,publicState} from '../logic.mjs';
let counter=0;
const act=(r,p,type,fields={})=>applyAction(r,p,{type,eventId:'private_event_'+(++counter)+'_new',...fields},1200);
const key=Buffer.from([...Array(32).keys()]);
function lobby(){const r=newRoom('couple','NORMAL','host',1000),h=r.participants[0],g=join(r,r.invite,'guest',1001);h.connected=true;g.connected=true;return {r,h,g};}
function seal(r,domain,text){const iv=Buffer.from([...Array(12).keys()]),c=createCipheriv('aes-256-gcm',key,iv);
 c.setAAD(Buffer.from(r.id+':'+domain));return {iv:iv.toString('base64url'),cipher:Buffer.concat([c.update(text),c.final(),c.getAuthTag()]).toString('base64url')};}
test('sealed lobby prompts and host-only configuration',()=>{
 const {r,h,g}=lobby(),secret=seal(r,'challenge','اختر أغنية مناسبة للحظة');
 act(r,h,'configure',{rounds:6,variety:'ONE_LEVEL',level:'NORMAL',custom:[secret]});
 assert.equal(publicState(r).customCount,1);
 assert.ok(!JSON.stringify(publicState(r)).includes('اختر أغنية'));
 assert.throws(()=>act(r,g,'configure',{rounds:6,variety:'ONE_LEVEL',level:'NORMAL',custom:[]}));
 act(r,g,'consent',{value:true});
 assert.throws(()=>act(r,h,'configure',{rounds:6,variety:'ONE_LEVEL',level:'NORMAL',custom:[]}));
});
test('plaintext messages rejected and ciphertext stored without body',()=>{
 const {r,h,g}=lobby();act(r,g,'consent',{value:true});act(r,h,'start');
 const actor=r.participants.find(p=>p.id===r.actorId),encrypted=seal(r,'message','نص خاص');
 assert.throws(()=>act(r,actor,'message',{body:'نص مكشوف'}));
 act(r,actor,'message',encrypted);
 const state=JSON.stringify(publicState(r));assert.ok(!state.includes('نص خاص'));assert.ok(!state.includes('"body"'));
});
test('quiz receives HMAC tags only; result exposes agreement without choice data',()=>{
 const {r,h,g}=lobby();act(r,g,'consent',{value:true});act(r,h,'quiz_open',{pack:2});
 const tag=(v,i)=>createHmac('sha256',key).update(['sirr','v4','quiz',r.id,r.quiz.id,r.quiz.indices[i],v].join(':')).digest('base64url');
 assert.throws(()=>act(r,h,'quiz_submit',{quizId:r.quiz.id,answers:Array(8).fill(0)}));
 act(r,h,'quiz_submit',{quizId:r.quiz.id,tags:Array.from({length:8},(_,i)=>tag(i%3,i))});
 act(r,g,'quiz_submit',{quizId:r.quiz.id,tags:Array.from({length:8},(_,i)=>tag(i%3,i))});
 assert.equal(r.quiz.result.agreement,8);
 assert.ok(!JSON.stringify(publicState(r)).includes('tags'));
});
test('JVM/WebCrypto AES-GCM and HMAC known-answer vectors',()=>{
 const r='11111111-1111-1111-1111-111111111111',iv=Buffer.from([...Array(12).keys()]);
 const c=createCipheriv('aes-256-gcm',key,iv);c.setAAD(Buffer.from(r+':message'));
 assert.equal(Buffer.concat([c.update('رسالة عربية آمنة'),c.final(),c.getAuthTag()]).toString('base64url'),
 'n7MOqB1CG59V6LdTCDHJtSsPDexZW4fe4eI8A8XAUlI48QoQ7mYkCzYV2_FCjg');
 assert.equal(createHmac('sha256',key).update(['sirr','v4','quiz',r,'quiz42',3,2].join(':')).digest('base64url'),
 'T9nEV9JXn__FxR5E9lr_e1aNomJIIRL7dGEEx-GxJso');
});
