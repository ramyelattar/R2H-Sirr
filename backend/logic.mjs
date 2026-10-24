import {randomBytes, randomUUID, randomInt} from 'node:crypto';
import {readFileSync} from 'node:fs';
const challengeCatalog=JSON.parse(readFileSync(new URL('./public/challenges.json',import.meta.url),'utf8'));
const quizCatalog=JSON.parse(readFileSync(new URL('./public/quizzes.json',import.meta.url),'utf8'));
if(challengeCatalog.version!==3 || quizCatalog.version!==3) throw Error('Content version mismatch');

export const now = () => Date.now();
export const token = () => randomBytes(32).toString('base64url');
export const LEVELS = new Set(['SOFT','NORMAL','SEXY','HOT','HARD']);
export const MODE = new Set(['couple','party']);
/** Canonical Arabic challenge decks shared across Android and browser. */
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
export const ROUND_CHOICES = new Set([6,10,12,18]);
export const VARIETIES = new Set(['ONE_LEVEL','MIX_LOWER']);
export function gameOptions(rounds, variety) {
  if(!ROUND_CHOICES.has(rounds) || !VARIETIES.has(variety)) throw Error('invalid game options');
  return {rounds,variety};
}
export function gameDeck(room) {
  const primary=deck(room.level,room.mode);
  if(room.variety==='ONE_LEVEL' || room.level==='SOFT')return primary;
  const lower=([...LEVELS].indexOf(room.level)-1);
  return primary.concat(deck([...LEVELS][lower],room.mode));
}
export function newRoom(mode, level, nickname, clock=now(), options={}) {
  if (!MODE.has(mode) || !LEVELS.has(level) || !validName(nickname)) throw Error('invalid room');
  const settings=gameOptions(options.rounds === undefined ? (mode==='couple'?10:12) : options.rounds,options.variety === undefined ? 'ONE_LEVEL' : options.variety);
  const id = randomUUID(); const invite = token(); const hostAuth = token();
  return {
    id, mode, level, invite, status:'WAITING', createdAt:clock, expiresAt:clock + 60*60*1000,
    seq:0, round:0, turnCount:settings.rounds, variety:settings.variety, actorId:null,
    prompt:null, promptSecret:null, events:new Set(), messages:[],
    participants:[{id:randomUUID(),auth:hostAuth,name:nickname,consent:true,score:0,skips:0,connected:false,host:true}],
    seenChallenges:[], sealedCustom:[], customSeen:[], quiz:null, media:[],
  };
}
export function validName(name) { return typeof name==='string' && name.trim().length>=1 && [...name].length<=32; }
export function publicState(room, clock=now()) {
  return {
    id:room.id, mode:room.mode, level:room.level, variety:room.variety, status:room.status,
    round:room.round, maxRounds:room.turnCount, actorId:room.actorId,
    prompt:room.prompt, promptSecret:room.promptSecret||null, customCount:room.sealedCustom.length, seq:room.seq, expiresAt:room.expiresAt, serverNow:clock,
    participants:room.participants.map(p=>({id:p.id, name:p.name, consent:p.consent, score:p.score, skips:p.skips, connected:p.connected,host:p.host})),
    messages:room.messages.map(({author,authorId,iv,cipher,round})=>({author,authorId,iv,cipher,round})),
    contentVersion:3, protocolVersion:4,
    quiz: room.quiz ? {id:room.quiz.id,pack:room.quiz.pack,indices:room.quiz.indices,
      submitted:room.quiz.submissions.map(x=>x.id),result:room.quiz.result} : null,
    media:room.media.filter(item=>item.expiresAt>clock).map(({id,kind,authorId,round,size,expiresAt,transfer})=>({id,kind,authorId,round,size,expiresAt,transfer:transfer||"single"}))
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
  const custom=room.sealedCustom.filter(x=>!room.customSeen.includes(x.iv));
  if(custom.length && room.round%3===0){
    room.prompt=null;room.promptSecret=custom[randomInt(custom.length)];room.customSeen.push(room.promptSecret.iv);
  }else{
    const d=gameDeck(room); const available=d.filter(x=>!room.seenChallenges.includes(x));
    if(!available.length)throw Error('not enough unique challenges');
    // The first ten prompts of each level are curated Egyptian mini-games.
    const baseSize=deck(room.level,room.mode).length;
    const featured=available.filter(x=>{
      const index=d.indexOf(x);
      return index>=0&&(index<10||(index>=baseSize&&index<baseSize+10));
    });
    const source=featured.length?featured:available;
    room.prompt=source[randomInt(source.length)];room.promptSecret=null;room.seenChallenges.push(room.prompt);
  }
}
/** Server-authoritative transitions; eventId deduplicates actor network retries. */
const IV=/^[A-Za-z0-9_-]{16}$/;
const CIPHER=/^[A-Za-z0-9_-]{24,4096}$/;
const TAG=/^[A-Za-z0-9_-]{43}$/;
export function sealedPromptList(value){
  if(!Array.isArray(value)||value.length>20)throw Error('invalid encrypted prompt list');
  if(value.some(x=>!x||typeof x!=='object'||!IV.test(x.iv)||!CIPHER.test(x.cipher)))throw Error('invalid encrypted prompt');
  if(new Set(value.map(x=>x.iv)).size!==value.length)throw Error('repeated iv');
  return value.map(({iv,cipher})=>({iv,cipher}));
}
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
    case 'configure': {
      if (!member.host || room.status!=='WAITING' || room.participants.some(p=>p.id!==member.id && p.consent)) throw Error('configuration locked');
      const config=gameOptions(command.rounds,command.variety);
      if(!LEVELS.has(command.level)) throw Error('invalid level');
      const pool=config.variety==='ONE_LEVEL'||command.level==='SOFT' ? deck(command.level,room.mode) :
        deck(command.level,room.mode).concat(deck([...LEVELS][[...LEVELS].indexOf(command.level)-1],room.mode));
      if(new Set(pool).size<config.rounds)throw Error('insufficient prompts');
      room.turnCount=config.rounds;room.variety=config.variety;room.level=command.level;
      room.sealedCustom=sealedPromptList(command.custom||[]);room.customSeen=[];
      break;
    }
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
      if(Object.hasOwn(command,'body')||!IV.test(command.iv)||!CIPHER.test(command.cipher))throw Error('encrypted messages required');
      room.messages.push({author:member.name,authorId:member.id,iv:command.iv,cipher:command.cipher,round:room.round});
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
      if(Object.hasOwn(command,'answers')||!Array.isArray(command.tags)||command.tags.length!==8 ||
         !command.tags.every(x=>typeof x==='string'&&TAG.test(x))) throw Error('private quiz commitments required');
      q.submissions.push({id:member.id,tags:command.tags.slice()});
      if(q.submissions.length===2){
        const [a,b]=q.submissions;
        q.result={agreement:a.tags.filter((x,i)=>x===b.tags[i]).length,questions:8};
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
