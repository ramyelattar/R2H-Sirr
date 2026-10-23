import test from 'node:test';
import assert from 'node:assert/strict';
import {newRoom,join,applyAction,publicState,deck} from '../logic.mjs';
const cmd=(type,extras={},n=cryptoRandom())=>({type,eventId:n,...extras});
let serial=0;function cryptoRandom(){return `test_event_${++serial}_xyz`;}
function ready(mode='couple',size=2){
  const r=newRoom(mode,'SEXY','المضيف',1000);
  const host=r.participants[0];host.connected=true;
  for(let i=1;i<size;i++){
    const guest=join(r,r.invite,`ضيف ${i}`,1001);guest.connected=true;
    applyAction(r,guest,cmd('consent',{value:true}),1002);
  }
  return {r,host};
}
test('creates invite tokens and holds until all players consent',()=>{
  const r=newRoom('couple','SOFT','المضيف',1000);
  assert.equal(r.status,'WAITING');
  assert.throws(()=>applyAction(r,r.participants[0],cmd('start'),1001));
  const p=join(r,r.invite,'الضيف',1002);
  assert.notEqual(p.auth,r.participants[0].auth);
  assert.throws(()=>join(r,'wrong','شخص',1002));
});
test('pair is server authoritative and idempotent',()=>{
  const {r,host}=ready();
  applyAction(r,host,cmd('start'),1010);
  assert.equal(r.status,'ACTIVE');
  for(let i=0;i<10;i++){
    const actor=r.participants.find(p=>p.id===r.actorId);
    assert.ok(actor);
    const e=cmd(i===3?'skip':'complete');
    assert.equal(applyAction(r,actor,e,1010),true);
    assert.equal(applyAction(r,actor,e,1010),false);
  }
  assert.equal(r.status,'ENDED');
  assert.equal(r.participants.reduce((n,p)=>n+p.score+p.skips,0),10);
  assert.equal(publicState(r,1100).participants.length,2);
});
test('party supports four and twelve deterministic scoreboard increments',()=>{
  const {r,host}=ready('party',4);
  applyAction(r,host,cmd('start'),1010);
  for(let i=0;i<12;i++){
    const actor=r.participants.find(p=>p.id===r.actorId);
    applyAction(r,actor,cmd('complete'),1012);
  }
  assert.equal(r.status,'ENDED');
  assert.deepEqual(r.participants.map(p=>p.score),[3,3,3,3]);
  assert.throws(()=>join(r,r.invite,'زائر آخر',1015));
});
test('revocation and expiry prevent actions',()=>{
  const {r,host}=ready();
  applyAction(r,host,cmd('revoke'),2000);
  assert.throws(()=>applyAction(r,host,cmd('start'),2001));
  const x=newRoom('couple','NORMAL','أ',1000);
  assert.throws(()=>join(x,x.invite,'ب',1000+60*60*1000));
});
test('prompts remain stable and nonempty in all five levels',()=>{
  for(const level of ['SOFT','NORMAL','SEXY','HOT','HARD']) assert.equal(deck(level).length,26);
});
