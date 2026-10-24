import test from 'node:test';
import assert from 'node:assert/strict';
import {newRoom,join,applyAction,publicState,gameOptions} from '../logic.mjs';
let id=0;const cmd=(type,fields={})=>({type,eventId:`m71_${++id}_event`,...fields});
test('18 rounds with mixed levels never repeat a prompt',()=>{
 const r=newRoom('couple','HARD','host',100,{rounds:18,variety:'MIX_LOWER'});
 const host=r.participants[0],guest=join(r,r.invite,'guest',101);
 host.connected=true;guest.connected=true;
 applyAction(r,guest,cmd('consent',{value:true}),102);
 applyAction(r,host,cmd('start'),103);
 const seen=[];
 for(let n=0;n<18;n++){const actor=r.participants.find(x=>x.id===r.actorId);seen.push(r.prompt);applyAction(r,actor,cmd('complete'),104+n);}
 assert.equal(r.status,'ENDED');assert.equal(new Set(seen).size,18);
});
test('only host configures before guests accept',()=>{
 const r=newRoom('party','NORMAL','host',100),host=r.participants[0],guest=join(r,r.invite,'guest',101);
 assert.throws(()=>applyAction(r,guest,cmd('configure',{rounds:6,variety:'ONE_LEVEL',level:'SOFT'}),103));
 applyAction(r,host,cmd('configure',{rounds:6,variety:'MIX_LOWER',level:'HOT'}),104);
 assert.equal(publicState(r,105).maxRounds,6);
 applyAction(r,guest,cmd('consent',{value:true}),106);
 assert.throws(()=>applyAction(r,host,cmd('configure',{rounds:12,variety:'ONE_LEVEL',level:'SOFT'}),107));
});
test('invalid session configurations are rejected',()=>{
 for(const rounds of [0,5,7,19,100,'10',null])assert.throws(()=>newRoom('couple','SOFT','host',100,{rounds,variety:'ONE_LEVEL'}));
 assert.throws(()=>gameOptions(10,'UNKNOWN'));
});
