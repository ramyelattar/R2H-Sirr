import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {newRoom,join,applyAction,publicState,selectQuizIndices,deck} from '../logic.mjs';
const quiz=JSON.parse(readFileSync(new URL('../public/quizzes.json',import.meta.url),'utf8'));
const catalog=JSON.parse(readFileSync(new URL('../public/challenges.json',import.meta.url),'utf8'));
let i=0;const command=(type,extra={})=>({type,eventId:`m7_event_${++i}_unique`,...extra});

test('M7 canonical Arabic content expanded in all packages and 8-of-25 indices',()=>{
 assert.equal(quiz.version,3);assert.equal(catalog.version,3);
 assert.equal(quiz.packs.length,7);assert.ok(quiz.packs.every(p=>p.questions.length===25));
 assert.equal(new Set(quiz.packs.flatMap(p=>p.questions.map(q=>q.text))).size,175);
 for(const level of ['SOFT','NORMAL','SEXY','HOT','HARD']) {
  assert.equal(deck(level,'couple').length,26);
  assert.equal(deck(level,'party').length,20);
 }
 const indices=selectQuizIndices(1,(n)=>n-1);
 assert.equal(indices.length,8);assert.ok(indices.every(n=>n>=0&&n<25));
});

test('M7 remote quiz result exposes only aggregated option distributions and agreement',()=>{
 const room=newRoom('couple','NORMAL','أ',1000);const host=room.participants[0];host.connected=true;
 const other=join(room,room.invite,'ب',1010);other.connected=true;
 applyAction(room,other,command('consent',{value:true}),1020);
 applyAction(room,host,command('start'),1021);
 applyAction(room,host,command('quiz_open',{pack:2}),1022);
 const id=room.quiz.id;
 applyAction(room,host,command('quiz_submit',{quizId:id,answers:[0,1,2,0,1,2,0,1]}),1023);
 applyAction(room,other,command('quiz_submit',{quizId:id,answers:[0,2,2,0,0,2,1,1]}),1024);
 const result=publicState(room,1025).quiz.result;
 assert.deepEqual(result.counts.map(x=>x.values),[[3,3,2],[3,2,3]]);
 assert.equal(result.agreement,5);
 assert.deepEqual(room.quiz.submissions.map(x=>Object.keys(x)),[['id'],['id']]);
 assert.equal(publicState(room,1025).contentVersion,3);
});
