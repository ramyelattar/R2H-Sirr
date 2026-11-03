import test from 'node:test';
import assert from 'node:assert/strict';
import {deck,newRoom,join,applyAction,publicState} from '../logic.mjs';
import {readFileSync} from 'node:fs';

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

test('M8 content v4 stays consistent in the lobby and quiz deck',()=>{
  const c=JSON.parse(readFileSync(new URL('../public/challenges.json',import.meta.url),'utf8'));
  const q=JSON.parse(readFileSync(new URL('../public/quizzes.json',import.meta.url),'utf8'));
  assert.equal(c.version,4);
  assert.equal(q.version,4);
  assert.equal(q.packs.length,7);
  assert.ok(q.packs.every(pack=>pack.questions.length===25));
  const room=newRoom('couple','SOFT','المضيف',Date.now());
  assert.equal(publicState(room).contentVersion,4);
});
