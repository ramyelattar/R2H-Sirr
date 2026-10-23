import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {newRoom,join,applyAction,publicState,deck,selectQuizIndices} from '../logic.mjs';
import {CipherMediaStore} from '../media.mjs';
import {randomBytes,createCipheriv,createDecipheriv} from 'node:crypto';
const quizzes=JSON.parse(readFileSync(new URL('../public/quizzes.json',import.meta.url),'utf8'));
const catalog=JSON.parse(readFileSync(new URL('../public/challenges.json',import.meta.url),'utf8'));
let tick=0;function act(room,member,type,extra={}) {return applyAction(room,member,{type,eventId:`m4_unit_event_${++tick}_xx`,...extra},1200);}
function prepared(mode='couple') {
 const room=newRoom(mode,'SEXY','المضيف',1000);const host=room.participants[0];host.connected=true;
 const guest=join(room,room.invite,'الشريك',1010);guest.connected=true;act(room,guest,'consent',{value:true});act(room,host,'start');
 return {room,host,guest};
}
test('content v3: 230 original Arabic prompts, unique per scope and level, 175 varied questions',()=>{
 assert.equal(quizzes.version,3);assert.equal(catalog.version,3);
 assert.equal(quizzes.packs.length,7);assert.ok(quizzes.packs.every(p=>p.questions.length===25));
 const qtexts=quizzes.packs.flatMap(p=>p.questions.map(q=>q.text));
 assert.equal(qtexts.length,175);assert.equal(new Set(qtexts).size,175);
 for(const q of quizzes.packs.flatMap(p=>p.questions)){
   assert.equal(q.choices.length,3);
   assert.ok(q.text.length>=15 && q.choices.every(c=>c.length>=2));
 }
 const prompts=[];
 for(const mode of ['couple','party'])for(const level of ['SOFT','NORMAL','SEXY','HOT','HARD']){
   const list=deck(level,mode);assert.ok(list.length>=12);assert.equal(new Set(list).size,list.length);
   prompts.push(...list);
 }
 assert.equal(prompts.length,230);assert.equal(new Set(prompts).size,230);
});
test('remote eight-question choices are unique, valid and included in public room state',()=>{
 const values=selectQuizIndices(3,()=>0);
 assert.equal(values.length,8);assert.equal(new Set(values).size,8);
 assert.ok(values.every(i=>i>=0&&i<25));
 assert.throws(()=>selectQuizIndices(7));assert.throws(()=>selectQuizIndices(1,()=>200));
 const {room,host}=prepared();act(room,host,'quiz_open',{pack:5});
 assert.deepEqual(publicState(room).quiz.indices,room.quiz.indices);
 assert.equal(room.quiz.indices.length,8);assert.ok(room.quiz.indices.every(i=>i>=0&&i<25));
});
test('room never repeats a prompt within 10 rounds for couple or 12 for party',()=>{
 for(const mode of ['couple','party']){
  const {room,host}=prepared(mode);const seen=new Set();
  for(let i=0;i<room.turnCount;i++){
   assert.ok(!seen.has(room.prompt));seen.add(room.prompt);
   const actor=room.participants.find(p=>p.id===room.actorId);
   act(room,actor,'complete');
  }
  assert.equal(seen.size,room.turnCount);
 }
});
test('one-use secret card encrypted ciphertext: receiver only and consumed once',()=>{
 const {room}=prepared();const store=new CipherMediaStore({fileLimit:1024,perRoomLimit:2048,totalLimit:2048});
 const author=room.participants.find(p=>p.id===room.actorId),receiver=room.participants.find(p=>p.id!==author.id);
 const key=randomBytes(32),iv=randomBytes(12),mime=Buffer.from('application/vnd.sirr.card+json');
 const payload=Buffer.from(JSON.stringify({version:1,type:'whispers',lines:['رسالة أ','رسالة ب','رسالة ج']}));
 const clear=Buffer.concat([Buffer.from([mime.length]),mime,payload]);
 const e=createCipheriv('aes-256-gcm',key,iv);e.setAAD(Buffer.from(`${room.id}:card`));
 const encrypted=Buffer.concat([e.update(clear),e.final(),e.getAuthTag()]);
 const card=store.add(room,author,'card',iv.toString('base64url'),encrypted,1300);
 assert.equal(store.get(room,author,card.id,1400),null);
 assert.ok(store.get(room,receiver,card.id,1400));
 const retrieved=store.consume(room,receiver,card.id,1401);
 assert.deepEqual(retrieved.ciphertext,encrypted);
 assert.equal(store.consume(room,receiver,card.id,1402),null);
 assert.equal(store.used,0);assert.equal(room.media.length,0);
 const d=createDecipheriv('aes-256-gcm',key,iv);d.setAAD(Buffer.from(`${room.id}:card`));d.setAuthTag(encrypted.subarray(-16));
 assert.deepEqual(Buffer.concat([d.update(encrypted.subarray(0,-16)),d.final()]),clear);
});
test('card cannot be uploaded in group rooms, or by non-actor',()=>{
 const {room}=prepared('party');const store=new CipherMediaStore();
 const actor=room.participants.find(p=>p.id===room.actorId),other=room.participants.find(p=>p.id!==room.actorId);
 // Group creation is prohibited by client UI; server must enforce too.
 assert.throws(()=>store.add(room,other,'card',randomBytes(12).toString('base64url'),Buffer.alloc(42),1300));
 assert.throws(()=>store.add(room,actor,'card',randomBytes(12).toString('base64url'),Buffer.alloc(42),1300));
});
