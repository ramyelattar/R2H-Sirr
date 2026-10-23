import {randomBytes, randomUUID, randomInt} from 'node:crypto';
import {readFileSync} from 'node:fs';
const challengeCatalog=JSON.parse(readFileSync(new URL('./public/challenges.json',import.meta.url),'utf8'));
const quizCatalog=JSON.parse(readFileSync(new URL('./public/quizzes.json',import.meta.url),'utf8'));
if(challengeCatalog.version!==2 || quizCatalog.version!==2) throw Error('Content version mismatch');

export const now = () => Date.now();
export const token = () => randomBytes(32).toString('base64url');
export const LEVELS = new Set(['SOFT','NORMAL','SEXY','HOT','HARD']);
export const MODE = new Set(['couple','party']);
/** Use the same canonical v2 decks on Android and the remote server. */
export function deck(level,mode='couple') {
  if (!LEVELS.has(level) || !MODE.has(mode)) throw Error('unknown catalog');
  return challengeCatalog[mode][level];
}
/** Unbiased eight-of-twenty question sampler; indices are shared across clients. */
export function selectQuizIndices(pack, pick=(limit)=>randomInt(limit)) {
  if (!Number.isInteger(pack) || pack<0 || pack>=quizCatalog.packs.length) throw Error('unknown quiz');
  const length=quizCatalog.packs[pack].questions.length;
  const indices=Array.from({length},(_,i)=>i);
  for(let i=length-1;i>0;i--){const j=pick(i+1);if(!Number.isInteger(j)||j<0||j>i)throw Error('invalid random index');
    [indices[i],indices[j]]=[indices[j],indices[i]];}
  return indices.slice(0,8);
}
export function newRoom(mode, level, nickname, clock=now()) {
  if (!MODE.has(mode) || !LEVELS.has(level) || !validName(nickname)) throw Error('invalid room');
  const id = randomUUID(); const invite = token(); const hostAuth = token();
  return {
    id, mode, level, invite, status:'WAITING', createdAt:clock, expiresAt:clock + 60*60*1000,
    seq:0, round:0, turnCount:mode==='couple'?10:12, actorId:null,
    prompt:null, events:new Set(), messages:[],
    participants:[{id:randomUUID(),auth:hostAuth,name:nickname,consent:true,score:0,skips:0,connected:false,host:true}],
    seenChallenges:[], quiz:null, media:[],
  };
}
export function validName(name) { return typeof name==='string' && name.trim().length>=1 && [...name].length<=32; }
export function publicState(room, clock=now()) {
  return {
    id:room.id, mode:room.mode, level:room.level, status:room.status,
    round:room.round, maxRounds:room.turnCount, actorId:room.actorId,
    prompt:room.prompt, seq:room.seq, expiresAt:room.expiresAt, serverNow:clock,
    participants:room.participants.map(p=>({id:p.id, name:p.name, consent:p.consent, score:p.score, skips:p.skips, connected:p.connected,host:p.host})),
    messages:room.messages.map(m=>({author:m.author, body:m.body, round:m.round})),
    contentVersion:2,
    quiz: room.quiz ? {id:room.quiz.id,pack:room.quiz.pack,indices:room.quiz.indices,
      submitted:room.quiz.submissions.map(x=>x.id),result:room.quiz.result} : null,
    media:room.media.filter(item=>item.expiresAt>clock).map(({id,kind,authorId,round,size,expiresAt})=>({id,kind,authorId,round,size,expiresAt}))
  };
}
export function participantByToken(room, auth) { return room.participants.find(p=>p.auth===auth); }
export function join(room, invite, name, clock=now()) {
  if (room.invite!==invite || room.status!=='WAITING' || room.expiresAt <= clock || !validName(name)) throw Error('not joinable');
  if (room.participants.length >= (room.mode==='couple'?2:4)) throw Error('room full');
  const member = {id:randomUUID(), auth:token(), name:name.trim(), consent:false, score:0,skips:0,connected:false,host:false};
  room.participants.push(member); room.seq++; return member;
}
function rotate(room,clock) {
  if (room.round>=room.turnCount) {room.status='ENDED'; room.closedAt=clock; room.actorId=null; room.prompt=null; room.media=[]; room.quiz=null; room.messages=[];return;}
  const i=room.round % room.participants.length;
  room.actorId=room.participants[i].id;
  const d=deck(room.level,room.mode); const available=d.filter(x=>!room.seenChallenges.includes(x));
  const source=available.length?available:d;
  room.prompt=source[Math.floor(Math.random()*source.length)];
  room.seenChallenges.push(room.prompt);
}
/** Server-authoritative transitions; eventId deduplicates actor network retries. */
export function applyAction(room, member, command, clock=now()) {
  const {type,eventId}=command;
  if (typeof eventId!=='string' || !/^[A-Za-z0-9_-]{8,80}$/.test(eventId)) throw Error('event id missing');
  if (room.events.has(eventId)) return false;
  if (room.expiresAt<=clock || ['EXPIRED','REVOKED','ENDED'].includes(room.status)) throw Error('room unavailable');
  if (room.events.size>1000) room.events.clear();
  switch(type) {
    case 'consent':
      if (room.status!=='WAITING' || command.value!==true) throw Error('invalid consent');
      member.consent=true; break;
    case 'start':
      if (!member.host || room.status!=='WAITING') throw Error('only host may start');
      if (room.participants.length < 2 || room.participants.some(p=>!p.consent || !p.connected)) throw Error('players missing or not ready');
      room.status='ACTIVE'; room.round=0; rotate(room,clock); break;
    case 'complete':
    case 'skip':
      if (room.status!=='ACTIVE' || room.actorId!==member.id) throw Error('not your turn');
      if (type==='complete') member.score++; else member.skips++;
      room.round++; rotate(room,clock); break;
    case 'message':
      if (room.status!=='ACTIVE' || room.actorId!==member.id) throw Error('not your turn');
      if (typeof command.body!=='string' || [...command.body].length>500) throw Error('message too long');
      room.messages.push({author:member.name,body:command.body,round:room.round});
      if (room.messages.length>10) room.messages.shift();
      break;
    case 'quiz_open': {
      if (!member.host || room.participants.length!==2 || !room.participants.every(p=>p.consent)) throw Error('two consenting players required');
      if (!Number.isInteger(command.pack) || command.pack<0 || command.pack>=7) throw Error('unknown quiz');
      room.quiz={id:randomUUID(),pack:command.pack,indices:selectQuizIndices(command.pack),submissions:[],result:null};
      break;
    }
    case 'quiz_submit': {
      const q=room.quiz;
      if (!q || q.result || command.quizId!==q.id || q.submissions.some(s=>s.id===member.id)) throw Error('quiz not accepting submission');
      if (!Array.isArray(command.answers) || command.answers.length!==8 ||
        !command.answers.every(a=>Number.isInteger(a)&&a>=0&&a<=2)) throw Error('invalid answers');
      q.submissions.push({id:member.id,answers:command.answers.slice()});
      if (q.submissions.length===2) {
        const [a,b]=q.submissions;
        q.result={scores:[{id:a.id,score:a.answers.reduce((s,x)=>s+x,0)},
                          {id:b.id,score:b.answers.reduce((s,x)=>s+x,0)}],
                  agreement:a.answers.filter((x,i)=>x===b.answers[i]).length,questions:8};
        // Never store individual answers after computing agreed aggregate.
        q.submissions=q.submissions.map(s=>({id:s.id}));
      }
      break;
    }
    case 'quiz_close':
      if (!member.host) throw Error('host only');
      room.quiz=null;break;
    case 'leave':
      room.status='ENDED'; room.closedAt=clock; room.actorId=null; room.prompt=null; room.messages=[]; room.quiz=null; room.media=[]; break;
    case 'revoke':
      if (!member.host) throw Error('host only');
      room.status='REVOKED'; room.closedAt=clock; room.actorId=null; room.prompt=null; room.messages=[]; room.quiz=null; room.media=[]; break;
    default: throw Error('unknown action');
  }
  room.events.add(eventId); room.seq++; return true;
}
