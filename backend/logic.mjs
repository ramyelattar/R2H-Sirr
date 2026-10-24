import {randomBytes, randomUUID, randomInt} from 'node:crypto';
import {readFileSync} from 'node:fs';
const challengeCatalog=JSON.parse(readFileSync(new URL('./public/challenges.json',import.meta.url),'utf8'));
const quizCatalog=JSON.parse(readFileSync(new URL('./public/quizzes.json',import.meta.url),'utf8'));
if(challengeCatalog.version!==3 || quizCatalog.version!==3) throw Error('Content version mismatch');

export const now = () => Date.now();
export const token = () => randomBytes(32).toString('base64url');
export const LEVELS = new Set(['SOFT','NORMAL','SEXY','HOT','HARD']);
export const MODE = new Set(['couple','party']);
export const ROUND_OPTIONS = Object.freeze([6,10,12,18]);
const ORDER=['SOFT','NORMAL','SEXY','HOT','HARD'];
/** Strictly validate remote settings and never accept arbitrary session keys. */
export function settingsFor(mode, input={}) {
  if(!input || typeof input!=='object' || Array.isArray(input)) throw Error('invalid settings');
  const rounds=input.rounds===undefined?(mode==='couple'?10:12):input.rounds;
  if(!ROUND_OPTIONS.includes(rounds)) throw Error('invalid round count');
  const variety=input.variety===undefined?false:input.variety;
  const avoidRepeat=input.avoidRepeat===undefined?true:input.avoidRepeat;
  if(typeof variety!=='boolean' || typeof avoidRepeat!=='boolean')throw Error('invalid toggles');
  const prompts=input.customPrompts===undefined?[]:input.customPrompts;
  if(!Array.isArray(prompts)||prompts.length>30)throw Error('invalid custom prompts');
  const cleaned=prompts.map(x=>{
    if(typeof x!=='string')throw Error('invalid prompt');
    const v=x.trim().replace(/\s+/gu,' ');
    if([...v].length<6||[...v].length>200||/[\u0000-\u001f\u007f\u202a-\u202e\u2066-\u2069]/u.test(v))throw Error('invalid prompt');
    return v;
  });
  if(new Set(cleaned.map(x=>x.normalize('NFC'))).size!==cleaned.length)throw Error('duplicate prompts');
  return {rounds,variety,avoidRepeat,customPrompts:cleaned};
}

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
export function newRoom(mode, level, nickname, clock=now(), options={}) {
  if (!MODE.has(mode) || !LEVELS.has(level) || !validName(nickname)) throw Error('invalid room');
  const settings=settingsFor(mode,options);
  const id = randomUUID(); const invite = token(); const hostAuth = token();
  return {
    id, mode, level, invite, status:'WAITING', createdAt:clock, expiresAt:clock + 60*60*1000,
    seq:0, round:0, turnCount:settings.rounds, settings, actorId:null,
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
    settings:{rounds:room.settings.rounds,variety:room.settings.variety,avoidRepeat:room.settings.avoidRepeat,customPromptCount:room.settings.customPrompts.length},
    prompt:room.prompt, seq:room.seq, expiresAt:room.expiresAt, serverNow:clock,
    participants:room.participants.map(p=>({id:p.id, name:p.name, consent:p.consent, score:p.score, skips:p.skips, connected:p.connected,host:p.host})),
    messages:room.messages.map(m=>({author:m.author, body:m.body, round:m.round})),
    contentVersion:3,
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
  const index=ORDER.indexOf(room.level);
  const primary=deck(room.level,room.mode);
  const mixed=room.settings.variety && index>0 ? deck(ORDER[index-1],room.mode) : [];
  const catalog=[...new Set([...primary,...mixed])];
  const custom=room.settings.customPrompts;
  const allow=p=>!room.settings.avoidRepeat || !room.seenChallenges.includes(p);
  const own=custom.filter(allow), generic=catalog.filter(allow);
  // Balance user-authored and library prompts without making the deck deterministic.
  const useCustom=own.length>0 && (room.round%2===0 || generic.length===0);
  const preferred=useCustom?own:generic;
  const fallback=preferred.length?preferred:(own.length?own:(generic.length?generic:[...custom,...catalog]));
  room.prompt=fallback[randomInt(fallback.length)];
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
    case 'configure': {
      if (!member.host || room.status!=='WAITING') throw Error('only host can configure waiting room');
      const settings=settingsFor(room.mode,command.settings);
      room.settings=settings;room.turnCount=settings.rounds;room.seenChallenges=[];
      break;
    }
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
                  counts:[{id:a.id,values:[0,1,2].map(option=>a.answers.filter(x=>x===option).length)},
                          {id:b.id,values:[0,1,2].map(option=>b.answers.filter(x=>x===option).length)}],
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
