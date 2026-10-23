import {randomUUID} from 'node:crypto';

/** Keeps ciphertext only, in RAM. AAD is `roomId:kind`, verified by clients. */
export class CipherMediaStore {
  constructor({fileLimit=8*1024*1024, totalLimit=32*1024*1024, perRoomLimit=12*1024*1024}={}) {
    this.entries=new Map();this.fileLimit=fileLimit;this.totalLimit=totalLimit;this.perRoomLimit=perRoomLimit;this.used=0;
  }
  add(room, member, kind, iv, encrypted, clock=Date.now()) {
    if(!['image','audio','video','card'].includes(kind) || !/^[A-Za-z0-9_-]{16}$/.test(iv)) throw Error('invalid media metadata');
    if(room.status!=='ACTIVE' || room.actorId!==member.id || !room.participants.every(p=>p.consent) ||
       (kind==='card' && room.mode!=='couple')) throw Error('not authorized for upload');
    if(!Buffer.isBuffer(encrypted) || encrypted.length<17 || encrypted.length>(kind==='card'?16*1024:this.fileLimit)) throw Error('invalid file size');
    this.prune(clock);
    const roomUsage=[...this.entries.values()].filter(x=>x.roomId===room.id).reduce((n,x)=>n+x.ciphertext.length,0);
    if(this.used+encrypted.length>this.totalLimit || roomUsage+encrypted.length>this.perRoomLimit) throw Error('media quota');
    const item={id:randomUUID(),roomId:room.id,kind,iv,ciphertext:encrypted,
      authorId:member.id,round:room.round,size:encrypted.length,expiresAt:Math.min(room.expiresAt,clock+15*60_000)};
    this.entries.set(item.id,item);this.used+=encrypted.length;
    room.media=room.media.filter(x=>x.expiresAt>clock).slice(-29);
    room.media.push({id:item.id,kind,authorId:member.id,round:room.round,size:item.size,expiresAt:item.expiresAt});
    return item;
  }
  get(room, member, id, clock=Date.now()) {
    const item=this.entries.get(id);
    if(!item || item.roomId!==room.id || item.expiresAt<=clock || room.status!=='ACTIVE' ||
      (item.kind==='card' && (room.mode!=='couple' || member.id===item.authorId)) ||
      !room.participants.some(p=>p.id===member.id && p.consent)) return null;
    return item;
  }
  /** One-use encrypted card. Delivery consumes the server copy, not any saved recipient copy. */
  consume(room,member,id,clock=Date.now()) {
    const item=this.get(room,member,id,clock);
    if (!item || item.kind!=='card') return null;
    this.entries.delete(id);this.used-=item.ciphertext.length;
    room.media=room.media.filter(x=>x.id!==id);
    return item;
  }
  removeRoom(roomId) {
    for(const [id,item] of this.entries) if(item.roomId===roomId){this.used-=item.ciphertext.length;this.entries.delete(id);}
  }
  prune(clock=Date.now()) {
    for(const [id,item] of this.entries) if(item.expiresAt<=clock) {this.used-=item.ciphertext.length;this.entries.delete(id);}
  }
}
