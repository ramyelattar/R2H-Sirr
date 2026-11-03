/* Encrypted chunk transfer protocol M6. Retains selected File only while page is open. */
(() => {
 'use strict';
 const CHUNK=512*1024,MAX=48*1024*1024,ENC=new TextEncoder();
 let pending=null,controller=null;
 function b64(bytes){return btoa(String.fromCharCode(...bytes)).replace(/\+/g,'-').replace(/\//g,'_').replace(/=/g,'');}
 function un64(s){let x=s.replace(/-/g,'+').replace(/_/g,'/');return Uint8Array.from(atob(x+'='.repeat((4-x.length%4)%4)),c=>c.charCodeAt(0));}
 function headers(auth){return {Authorization:'Bearer '+auth};}
 async function json(url,method,auth,body,signal){
  const result=await fetch(url,{method,signal,headers:{...headers(auth),...(body?{'Content-Type':'application/json'}:{})},
   ...(body?{body:JSON.stringify(body)}:{})});
  if(!result.ok)throw Error('تعذر نقل الملف ('+result.status+')');return result.json();
 }
 function settings(args){const base='/api/rooms/'+args.roomId;return {base,auth:args.auth,kind:args.kind,key:args.key,roomId:args.roomId,progress:args.progress||(()=>{})};}
 async function upload(file,args){
  const a=settings(args),name=ENC.encode(file.type),prefix=new Uint8Array(1+name.length);
  prefix[0]=name.length;prefix.set(name,1);const total=file.size+prefix.length;
  if(file.size<1||total>MAX||name.length>64)throw Error('ملف غير صالح أو يتجاوز ٤٨ ميجابايت');
  if(!pending||pending.file!==file||pending.roomId!==a.roomId){
   const created=await json(a.base+'/uploads','POST',a.auth,{kind:a.kind,totalBytes:total});
   pending={file,id:created.id,roomId:a.roomId,kind:a.kind};
  }
  const up=pending;controller=new AbortController();const signal=controller.signal;
  try{
   const status=await json(a.base+'/uploads/'+up.id,'GET',a.auth,null,signal);
   const received=new Set(status.received);
   for(let i=0;i<status.count;i++){
    if(signal.aborted)throw Error('تم إيقاف النقل');
    if(received.has(i)){a.progress('استعادة الأجزاء '+(i+1)+'/'+status.count);continue;}
    const start=i*CHUNK,end=Math.min(start+CHUNK,total),block=new Uint8Array(end-start);
    if(start<prefix.length)block.set(prefix.slice(start,Math.min(end,prefix.length)));
    const from=Math.max(0,start-prefix.length),to=Math.max(0,end-prefix.length);
    if(to>from)block.set(new Uint8Array(await file.slice(from,to).arrayBuffer()),Math.max(0,prefix.length-start));
    const iv=crypto.getRandomValues(new Uint8Array(12));
    const aad=ENC.encode(a.roomId+':chunk:'+up.id+':'+i+':'+a.kind);
    const cipher=await crypto.subtle.encrypt({name:'AES-GCM',iv,additionalData:aad},a.key,block);
    let ok=false;
    for(let retry=0;retry<3&&!ok;retry++){
     try{
      const reply=await fetch(a.base+'/uploads/'+up.id+'/chunks/'+i,{method:'PUT',signal,
       headers:{...headers(a.auth),'Content-Type':'application/octet-stream','X-Encrypted-Iv':b64(iv)},body:cipher});
      if(!reply.ok)throw Error('جزء مرفوض: '+(i+1));ok=true;
     }catch(err){
      if(signal.aborted)throw err;
      const check=await json(a.base+'/uploads/'+up.id,'GET',a.auth,null,signal).catch(()=>null);
      if(check?.received?.includes(i))ok=true;else if(retry===2)throw err;
     }
    }
    a.progress('تم إرسال '+(i+1)+'/'+status.count+' أجزاء مشفرة');
   }
   const finished=await json(a.base+'/uploads/'+up.id+'/complete','POST',a.auth,null,signal);
   pending=null;return finished.id;
  }finally{controller=null;}
 }
 async function download(item,args){
  const a=settings({...args,kind:item.kind});
  const path=a.base+'/media/'+item.id;
  const manifest=await json(path+'/manifest','GET',a.auth);
  if(manifest.kind!==item.kind||manifest.totalBytes<1||manifest.totalBytes>MAX||
   manifest.count!==Math.ceil(manifest.totalBytes/CHUNK))throw Error('بيانات ملف غير صالحة');
  const data=new Uint8Array(manifest.totalBytes);let offset=0;
  for(let i=0;i<manifest.count;i++){
   const response=await fetch(path+'/chunks/'+i,{headers:headers(a.auth)});
   if(!response.ok)throw Error('جزء ناقص: '+(i+1));
   const iv=un64(response.headers.get('X-Encrypted-Iv')||'');if(iv.length!==12)throw Error('IV غير صالح');
   const aad=ENC.encode(a.roomId+':chunk:'+item.id+':'+i+':'+item.kind);
   const plain=new Uint8Array(await crypto.subtle.decrypt({name:'AES-GCM',iv,additionalData:aad},a.key,await response.arrayBuffer()));
   if(plain.length!==Math.min(CHUNK,manifest.totalBytes-i*CHUNK))throw Error('الجزء غير مطابق');
   data.set(plain,offset);offset+=plain.length;
   a.progress('تم تنزيل '+(i+1)+'/'+manifest.count+' أجزاء مشفرة');
  }
  return data;
 }
 function hasPending(){return !!pending;}
 async function resume(args){if(!pending)throw Error('لا يوجد ملف للاستئناف');return upload(pending.file,{...args,kind:pending.kind});}
 async function cancel(args){controller?.abort();if(!pending)return;
  const prior=pending;pending=null;
  await json('/api/rooms/'+args.roomId+'/uploads/'+prior.id,'DELETE',args.auth).catch(()=>{});
 }
 globalThis.SirrM6={upload,download,resume,cancel,hasPending,MAX};
})();
