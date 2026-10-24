import test from 'node:test';
import assert from 'node:assert/strict';
import {newRoom,join,applyAction,publicState,settingsFor,deck,ROUND_OPTIONS} from '../logic.mjs';
let n=0;
const cmd=(type,extra={})=>({type,eventId:`m7_custom_${++n}_event`,...extra});
function ready(mode,config){
 const room=newRoom(mode,'HARD','مضيف',1000,config);
 const host=room.participants[0];host.connected=true;
 const guest=join(room,room.invite,'ضيف',1001);guest.connected=true;
 applyAction(room,guest,cmd('consent',{value:true}),1002);
 return {room,host,guest};
}
test('remote round options and defaults remain backward compatible',()=>{
 assert.deepEqual(ROUND_OPTIONS,[6,10,12,18]);
 assert.equal(newRoom('couple','SOFT','أ',100).turnCount,10);
 assert.equal(newRoom('party','SOFT','أ',100).turnCount,12);
 for(const rounds of ROUND_OPTIONS){
  const room=newRoom('couple','HOT','أ',100,{rounds,variety:true,avoidRepeat:true});
  assert.equal(room.turnCount,rounds);
  assert.deepEqual(publicState(room,101).settings,{rounds,variety:true,avoidRepeat:true,customPromptCount:0});
 }
});
test('host can configure before starting and clients receive only a count, not entire custom deck',()=>{
 const {room,host,guest}=ready('couple',{rounds:6});
 const custom=['احك حكاية جميلة عن يوم جمعكما','اختارا أغنية تعبر عن مزاج الجلسة'];
 assert.throws(()=>applyAction(room,guest,cmd('configure',{settings:{rounds:18}}),1003),/host/);
 assert.equal(room.turnCount,6);
 assert.equal(applyAction(room,host,cmd('configure',{settings:{rounds:18,variety:true,customPrompts:custom}}),1003),true);
 assert.equal(room.turnCount,18);
 const info=publicState(room,1004);
 assert.equal(info.settings.customPromptCount,2);
 assert.equal(JSON.stringify(info).includes(custom[0]),false);
 applyAction(room,host,cmd('start'),1005);
 assert.equal(room.prompt,custom[0]===room.prompt?custom[0]:custom[1]);
 assert.throws(()=>applyAction(room,host,cmd('configure',{settings:{rounds:6}}),1006),/waiting/);
 for(let i=0;i<18;i++){
  const member=room.participants.find(p=>p.id===room.actorId);
  applyAction(room,member,cmd('complete'),1100+i);
 }
 assert.equal(room.status,'ENDED');
 assert.equal(new Set(room.seenChallenges).size,18);
});
test('invalid settings are rejected without updating room state',()=>{
 const {room,host}=ready('party',{rounds:12});
 for(const data of [
  {rounds:100},{rounds:'12'},{variety:'yes'},{avoidRepeat:null},
  {customPrompts:['abc']},{customPrompts:['مرحبا بالعالم','مرحبا بالعالم']},
  {customPrompts:['مقبول هذا التحدي\u202e']},{customPrompts:Array(31).fill('رسالة طويلة مختلفة')},
  {customPrompts:'a single string'},null
 ]) {
  assert.throws(()=>applyAction(room,host,cmd('configure',{settings:data}),1050));
  assert.equal(room.turnCount,12);
 }
});
test('level variety always uses requested level and immediate softer level, no cross-party leakage',()=>{
 const {room,host}=ready('party',{rounds:18,variety:true,avoidRepeat:true});
 applyAction(room,host,cmd('start'),1100);
 const allowed=new Set([...deck('HARD','party'),...deck('HOT','party')]);
 for(let i=0;i<18;i++){
  assert.ok(allowed.has(room.prompt));
  const member=room.participants.find(p=>p.id===room.actorId);
  applyAction(room,member,cmd('skip'),1150+i);
 }
 assert.equal(new Set(room.seenChallenges).size,18);
});
test('duplicate configure event is idempotent and guest cannot start session',()=>{
 const {room,host,guest}=ready('couple',{rounds:6});
 const event=cmd('configure',{settings:{rounds:12}});
 assert.equal(applyAction(room,host,event,1004),true);
 assert.equal(applyAction(room,host,event,1005),false);
 assert.throws(()=>applyAction(room,guest,cmd('start'),1006));
 assert.equal(room.turnCount,12);
});
