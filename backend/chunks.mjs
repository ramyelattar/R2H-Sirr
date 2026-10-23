import {randomUUID, timingSafeEqual} from 'node:crypto';

export const CHUNK_SIZE=512*1024;
export const MAX_CHUNK_FILE=48*1024*1024;
const MAX_GLOBAL_BYTES=128*1024*1024;
const MAX_ROOM_BYTES=64*1024*1024;
const MAX_UPLOADS=40;
const TTL_MS=15*60_000;
const IV_RE=/^[A-Za-z0-9_-]{16}$/;

/** Opaque AES-GCM ciphertext chunks only. RAM-backed: no server-restart persistence. */
export class CipherChunkStore {
  constructor(){this.uploads=new Map();this.completed=new Map();this.used=0;}
  prune(t=Date.now()){
    for(const [id,item] of this.uploads) if(item.expiresAt<=t)this.remove(id,this.uploads);
    for(const [id,item] of this.completed) if(item.expiresAt<=t)this.remove(id,this.completed);
  }
  remove(id,source){const item=source.get(id);if(!item)return;source.delete(id);
    for(const chunk of item.chunks.values())this.used-=chunk.data.length;
  }
  roomBytes(roomId){let amount=0;for(const list of [this.uploads,this.completed])
    for(const item of list.values())if(item.roomId===roomId)
      for(const chunk of item.chunks.values())amount+=chunk.data.length;
    return amount;
  }
  create(room,actor,{kind,totalBytes},clock=Date.now()){
    this.prune(clock);
    if(room.status!=='ACTIVE'||room.actorId!==actor.id||!room.participants.every(p=>p.consent))throw Error('not your turn');
    if(!['image','audio','video'].includes(kind)||!Number.isSafeInteger(totalBytes)||totalBytes<1||totalBytes>MAX_CHUNK_FILE)throw Error('invalid metadata');
    if(this.uploads.size>=MAX_UPLOADS||[...this.uploads.values()].filter(x=>x.roomId===room.id).length>=4)throw Error('upload quota');
    const count=Math.ceil(totalBytes/CHUNK_SIZE);
    const item={id:randomUUID(),roomId:room.id,kind,totalBytes,count,authorId:actor.id,
      round:room.round,chunks:new Map(),expiresAt:Math.min(room.expiresAt,clock+TTL_MS)};
    this.uploads.set(item.id,item);return this.snapshot(item);
  }
  owned(room,actor,id,clock=Date.now()){
    const item=this.uploads.get(id);
    if(!item||item.roomId!==room.id||item.authorId!==actor.id||room.status!=='ACTIVE'||
       item.expiresAt<=clock||!room.participants.some(p=>p.id===actor.id&&p.consent))throw Error('upload not available');
    return item;
  }
  snapshot(item){return {id:item.id,kind:item.kind,totalBytes:item.totalBytes,chunkSize:CHUNK_SIZE,
    count:item.count,received:[...item.chunks.keys()].sort((a,b)=>a-b),expiresAt:item.expiresAt};}
  put(room,actor,id,index,iv,cipher,clock=Date.now()){
    const item=this.owned(room,actor,id,clock);
    if(!Number.isSafeInteger(index)||index<0||index>=item.count||!IV_RE.test(iv)||!Buffer.isBuffer(cipher))throw Error('invalid chunk');
    const expectPlain=index===item.count-1?item.totalBytes-index*CHUNK_SIZE:CHUNK_SIZE;
    if(cipher.length!==expectPlain+16)throw Error('bad chunk length');
    const previous=item.chunks.get(index);
    if(previous){const same=previous.iv===iv && previous.data.length===cipher.length&&timingSafeEqual(previous.data,cipher);
      if(!same)throw Error('chunk conflict');return this.snapshot(item);}
    if(this.used+cipher.length>MAX_GLOBAL_BYTES||this.roomBytes(room.id)+cipher.length>MAX_ROOM_BYTES)throw Error('storage quota');
    // Re-check live permissions and TTL after HTTP body reception, before commit.
    item.chunks.set(index,{iv,data:Buffer.from(cipher)});this.used+=cipher.length;
    return this.snapshot(item);
  }
  complete(room,actor,id,clock=Date.now()){
    const item=this.owned(room,actor,id,clock);
    if(item.chunks.size!==item.count)throw Error('missing chunks');
    this.uploads.delete(id);this.completed.set(id,item);
    room.media=room.media.filter(x=>x.expiresAt>clock).slice(-29);
    room.media.push({id:item.id,kind:item.kind,authorId:item.authorId,round:item.round,
      size:item.totalBytes,expiresAt:item.expiresAt,transfer:'chunked'});
    return {id:item.id,expiresAt:item.expiresAt};
  }
  get(room,member,id,clock=Date.now()){
    const item=this.completed.get(id);
    if(!item||item.roomId!==room.id||room.status!=='ACTIVE'||item.expiresAt<=clock||
      !room.participants.some(p=>p.id===member.id&&p.consent))return null;
    return item;
  }
  manifest(room,member,id,clock=Date.now()){
    const item=this.get(room,member,id,clock);if(!item)return null;
    return {id:item.id,kind:item.kind,totalBytes:item.totalBytes,chunkSize:CHUNK_SIZE,
      count:item.count,expiresAt:item.expiresAt};
  }
  chunk(room,member,id,index,clock=Date.now()){
    const item=this.get(room,member,id,clock);
    if(!item||!Number.isSafeInteger(index)||index<0||index>=item.count)return null;
    return {kind:item.kind,index, ...item.chunks.get(index)};
  }
  cancel(room,actor,id,clock=Date.now()){
    this.owned(room,actor,id,clock);this.remove(id,this.uploads);
  }
  removeRoom(roomId){for(const list of [this.uploads,this.completed])
    for(const [id,item] of list)if(item.roomId===roomId)this.remove(id,list);}
}
