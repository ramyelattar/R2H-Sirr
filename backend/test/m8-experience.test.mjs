import test from 'node:test';
import assert from 'node:assert/strict';
import {deck,newRoom,join,applyAction} from '../logic.mjs';

let sequence=0;
const action=(type,details={})=>({type,eventId:`m8_event_${++sequence}_2026`,...details});
function activeRoom(level,rounds,variety='ONE_LEVEL'){
  const room=newRoom('couple',level,'الأول',Date.now(),{rounds,variety});
  const host=room.participants[0],guest=join(room,room.invite,'التاني');
  host.connected=true;guest.connected=true;
  applyAction(room,guest,action('consent',{value:true}));
  applyAction(room,host,action('start'));
  return room;
}
test('curated Egyptian scenes cover every group and every mood',()=>{
  for(const scope of ['couple','party'])for(const level of ['SOFT','NORMAL','SEXY','HOT','HARD']){
    const list=deck(level,scope);
    assert.ok(list.length>=10);
    assert.equal(new Set(list.slice(0,10)).size,10);
    assert.ok(list.slice(0,10).every(text=>typeof text==='string'&&[...text].length>=20));
  }
});
test('short single-level sessions use the new ten curated mini-games first',()=>{
  const room=activeRoom('SOFT',10);
  const featured=new Set(deck('SOFT','couple').slice(0,10));
  const played=[];
  for(let i=0;i<10;i++){
    assert.ok(featured.has(room.prompt),`unexpected legacy prompt in round ${i+1}`);
    played.push(room.prompt);
    const actor=room.participants.find(p=>p.id===room.actorId);
    applyAction(room,actor,action('complete'));
  }
  assert.equal(new Set(played).size,10);
  assert.equal(room.status,'ENDED');
});
test('18-round mixed sessions do not repeat prompts',()=>{
  const room=activeRoom('HARD',18,'MIX_LOWER');
  const played=[];
  for(let i=0;i<18;i++){
    played.push(room.prompt);
    const actor=room.participants.find(p=>p.id===room.actorId);
    applyAction(room,actor,action('complete'));
  }
  assert.equal(room.status,'ENDED');
  assert.equal(new Set(played).size,18);
});
