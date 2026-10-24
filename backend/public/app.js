(() => {
 'use strict';
 const $=id=>document.getElementById(id);
 const roomId=location.pathname.split('/')[2];
 const fragment=new URLSearchParams(location.hash.slice(1));
 const invite=fragment.get('invite'),inviteKey=fragment.get('key');
 history.replaceState({},'',location.pathname); // Neither join secret nor E2EE key in browser history
 const storeKey=`sirr:${roomId}`;
 let identity=null,socket=null,authenticated=false,lastSeq=-1,deliberatelyLeft=false,retries=0,retryTimer=null;
 let packs=[],currentQuizId=null,answers=[],lastState=null,lastMediaIds='',activeMediaUrl=null,lastEffectSeq=-1;
 let currentRoomRound=-1;
 try {const x=JSON.parse(sessionStorage.getItem(storeKey)||'null');if(x&&x.expiresAt>Date.now())identity=x;}catch{}
 function notice(message){$('notice').textContent=message;}
 function id(){const b=new Uint8Array(16);crypto.getRandomValues(b);return [...b].map(x=>x.toString(16).padStart(2,'0')).join('');}
 function b64(bytes){return btoa(String.fromCharCode(...bytes)).replace(/\+/g,'-').replace(/\//g,'_').replace(/=/g,'');}
 function un64(value){const x=value.replace(/-/g,'+').replace(/_/g,'/');return Uint8Array.from(atob(x+'='.repeat((4-x.length%4)%4)),c=>c.charCodeAt(0));}
 async function key(){if(!identity?.key)throw Error('لا يوجد مفتاح تشفير من رابط الدعوة');return crypto.subtle.importKey('raw',un64(identity.key),{name:'AES-GCM'},false,['encrypt','decrypt']);}


 const fxDefaults={sound:false,vibrate:false,cinematic:true,volume:30};
 let fx=(()=>{try{return {...fxDefaults,...JSON.parse(localStorage.getItem('sirr-effect-options')||'{}')};}catch{return {...fxDefaults};}})();
 let audioCtx=null,roundForCue=-1;
 function updateFx(){
  fx.sound=$('fxSound').checked;fx.vibrate=$('fxVibrate').checked;
  fx.cinematic=$('fxMotion').checked;fx.volume=Number($('fxVolume').value);
  localStorage.setItem('sirr-effect-options',JSON.stringify(fx));
  document.body.classList.toggle('fx-motion-off',!fx.cinematic||matchMedia('(prefers-reduced-motion:reduce)').matches);
 }
 function fxSpark(){
  if(!fx.cinematic||matchMedia('(prefers-reduced-motion:reduce)').matches)return;
  const host=$('fxParticles');host.replaceChildren();
  for(let n=0;n<16;n++){
   const spark=document.createElement('i');spark.className='fx-spark';
   const theta=Math.PI*2*n/16;
   spark.style.setProperty('--fx-x',Math.round(Math.cos(theta)*(35+n%4*19))+'px');
   spark.style.setProperty('--fx-y',Math.round(Math.sin(theta)*(35+n%4*19))+'px');
   host.append(spark);
  }
  setTimeout(()=>host.replaceChildren(),700);
 }
 function fxCue(kind){
  if(fx.sound&&fx.volume>0)try{
   audioCtx ||= new (window.AudioContext||window.webkitAudioContext)();
   if(audioCtx.state==='running'){
    const osc=audioCtx.createOscillator(),gain=audioCtx.createGain(),t=audioCtx.currentTime;
    osc.type='sine';osc.frequency.value={round:450,complete:720,skip:260,reveal:620}[kind]||520;
    gain.gain.setValueAtTime(Math.min(.12,fx.volume/800),t);
    gain.gain.exponentialRampToValueAtTime(.001,t+.13);
    osc.connect(gain).connect(audioCtx.destination);osc.start(t);osc.stop(t+.14);
   }
  }catch{}
  if(fx.vibrate && navigator.vibrate)try{navigator.vibrate(kind==='complete'?75:30)}catch{}
  if(kind==='complete'||kind==='reveal')fxSpark();
 }
 $('fxSound').checked=fx.sound;$('fxVibrate').checked=fx.vibrate;$('fxMotion').checked=fx.cinematic;
 $('fxVolume').value=fx.volume;
 for(const id of ['fxSound','fxVibrate','fxMotion','fxVolume'])$(id).addEventListener('change',updateFx);
 $('fxPreview').addEventListener('click',()=>{
  if(fx.sound){audioCtx ||= new (window.AudioContext||window.webkitAudioContext)();void audioCtx.resume().then(()=>fxCue('reveal'));}
  else fxCue('reveal');
 });
 updateFx();
 async function sealText(domain,plain){
  const iv=crypto.getRandomValues(new Uint8Array(12));
  const cipher=await crypto.subtle.encrypt({name:'AES-GCM',iv,additionalData:new TextEncoder().encode(roomId+':'+domain)},await key(),new TextEncoder().encode(plain));
  return {iv:b64(iv),cipher:b64(new Uint8Array(cipher))};
 }
 async function openText(domain,item){
  const iv=un64(item.iv),data=un64(item.cipher);
  if(iv.length!==12||data.length<17||data.length>3072)throw Error('encrypted data invalid');
  return new TextDecoder('utf-8',{fatal:true}).decode(await crypto.subtle.decrypt({name:'AES-GCM',iv,additionalData:new TextEncoder().encode(roomId+':'+domain)},await key(),data));
 }
 async function quizTags(q,answers){
  const macKey=await crypto.subtle.importKey('raw',un64(identity.key),{name:'HMAC',hash:'SHA-256'},false,['sign']);
  return Promise.all(answers.map(async(answer,i)=>{
   const input=['sirr','v4','quiz',roomId,q.id,q.indices[i],answer].join(':');
   return b64(new Uint8Array(await crypto.subtle.sign('HMAC',macKey,new TextEncoder().encode(input))));
  }));
 }
 async function sendPrivateMessage(plain){
  if(!plain.trim()||[...plain].length>500)throw Error('invalid private message');
  action('message',await sealText('message',plain.trim()));
 }
 async function showSealedMessages(state){
  const pane=$('messages');pane.replaceChildren();const seq=state.seq;
  for(const item of state.messages){
   try{
    const plain=await openText('message',item);
    if(lastState?.seq!==seq)return;
    const line=document.createElement('p');line.textContent=item.author+': '+plain;pane.append(line);
   }catch{if(lastState?.seq===seq)notice('تعذر فتح رسالة مشفرة');}
  }
 }
 async function showSealedChallenge(state){
  try{
   const plain=await openText('challenge',state.promptSecret);
   if(lastState?.seq===state.seq)$('challenge').textContent=plain;
  }catch{if(lastState?.seq===state.seq)$('challenge').textContent='تعذر فك التحدي الخاص';}
 }
 async function configureLobby(){
  const lines=$('waitingCustom').value.split(/\r?\n/).map(x=>x.trim()).filter(Boolean);
  if(lines.length>20||new Set(lines).size!==lines.length||lines.some(x=>[...x].length<6||[...x].length>200))
   throw Error('حتى ٢٠ تحديًا من ٦ إلى ٢٠٠ حرف دون تكرار');
  const custom=await Promise.all(lines.map(line=>sealText('challenge',line)));
  action('configure',{level:$('waitingLevel').value,variety:$('waitingVariety').value,rounds:Number($('waitingRounds').value),custom});
 }
 function renderLobby(state){
  const me=state.participants.find(p=>p.id===identity.participantId);
  const waiting=state.status==='WAITING';
  $('agree').hidden=!waiting||!!me?.consent;
  const edit=waiting&&!!me?.host&&!state.participants.some(p=>!p.host&&p.consent);
  $('waitingConfig').hidden=!edit;
  $('waitingSummary').textContent=waiting?state.maxRounds+' جولة، '+state.customCount+' تحديات خاصة. وافق بعد مراجعة الإعدادات.':'';
  if(edit&&currentRoomRound!==state.maxRounds){
   $('waitingRounds').value=String(state.maxRounds);
   $('waitingVariety').value=state.variety;
   $('waitingLevel').value=state.level;
   currentRoomRound=state.maxRounds;
  }
 }
 async function loadQuizzes(){try{const r=await fetch('/quizzes.json',{cache:'no-store'});const x=await r.json();if(x.version!==4||x.packs.length!==7)throw Error('incompatible quizzes');packs=x.packs;if(lastState)renderQuiz(lastState);}catch{notice('تعذر تحميل بنك الأسئلة العربي');}}
 function action(type,extra={}){
  if(!authenticated||socket?.readyState!==WebSocket.OPEN)return notice('لا يوجد اتصال آمن حالي');
  socket.send(JSON.stringify({type,eventId:id(),...extra}));
 }
 function connect(){
  if(!identity||deliberatelyLeft)return;
  $('gate').hidden=true;$('game').hidden=false;
  const u=new URL('/ws',location.origin);u.protocol=u.protocol==='https:'?'wss:':'ws:';
  socket=new WebSocket(u);
  socket.addEventListener('open',()=>socket.send(JSON.stringify({type:'authenticate',roomId,auth:identity.auth,contentVersion:4,protocolVersion:4})));
  socket.addEventListener('message',evt=>{
   let x;try{x=JSON.parse(evt.data);}catch{return;}
   if(x.type==='authenticated'){
    authenticated=true;retries=0;$('connect').textContent='متصل';notice('');
    // Explicit consent only after the player reviews lobby settings.
   }else if(x.type==='state')render(x.data);
   else if(x.type==='error')notice(x.message);
  });
  socket.addEventListener('close',event=>{
   authenticated=false;$('connect').textContent='غير متصل';
   if([4000,4003,4009].includes(event.code)){deliberatelyLeft=true;sessionStorage.removeItem(storeKey);notice(event.code===4009?'نسخة المحتوى قديمة؛ افتح الرابط من جديد بعد تحديث التطبيق':'الجلسة انتهت أو لم يعد الدخول مسموحًا');return;}
   if(!deliberatelyLeft){const wait=Math.min(30000,1000*2**Math.min(retries++,5));notice('انقطع الاتصال؛ ستتم إعادة المحاولة.');clearTimeout(retryTimer);retryTimer=setTimeout(connect,wait);}
  });
 }
 function render(s){
  if(s.seq<lastSeq)return;lastSeq=s.seq;lastState=s;
  $('title').textContent=s.mode==='party'?'تحديات المجموعة':'تحديات الشريكين';
  const summary={WAITING:'في انتظار المشاركين',ACTIVE:'الجلسة جارية',ENDED:'انتهت الجلسة',EXPIRED:'انتهت الصلاحية',REVOKED:'أُلغيت الدعوة'};
  $('round').textContent=`${summary[s.status]||s.status} — الجولة ${Math.min(s.round+1,s.maxRounds)} من ${s.maxRounds} — ${s.variety==='MIX_LOWER'?'مستويان':'مستوى واحد'}`;
  document.documentElement.style.setProperty('--game-progress', `${Math.min(100,Math.round(s.round/Math.max(1,s.maxRounds)*100))}%`);
  if(s.status==='ACTIVE' && s.round!==roundForCue && roundForCue!==-1){
    const card=$('challenge');card?.classList.remove('reveal-pop');void card?.offsetWidth;card?.classList.add('reveal-pop');
  }
  if(s.status==='ACTIVE'&&roundForCue!==s.round){roundForCue=s.round;fxCue('round');}
  lastEffectSeq=s.seq;
  $('challenge').textContent=s.promptSecret?'جارِ فك التحدي الخاص…':(s.prompt||'تبدأ الجولة بعد موافقة الجميع وبدء المضيف.');
  if(s.promptSecret)void showSealedChallenge(s);
  const list=$('participants');list.replaceChildren();
  for(const p of s.participants){const li=document.createElement('li');li.textContent=`${p.name} — ${p.score} نقطة — ${p.connected?'متصل':'غير متصل'} — ${p.consent?'موافق':'بانتظار الموافقة'}`;list.append(li);}
  const myTurn=s.status==='ACTIVE'&&s.actorId===identity.participantId;
  $('actor').hidden=!myTurn;$('mediaUpload').hidden=!myTurn;
  $('cards').hidden=!myTurn||s.mode!=='couple';
  void showSealedMessages(s);
  renderLobby(s);
  renderQuiz(s);
  const ids=s.media.map(m=>m.id).join(',');if(ids!==lastMediaIds){lastMediaIds=ids;renderMedia(s.media);}
  if(['REVOKED','EXPIRED','ENDED'].includes(s.status)){
   $('actor').hidden=true;$('leave').disabled=true;deliberatelyLeft=true;
   sessionStorage.removeItem(storeKey);if(activeMediaUrl){URL.revokeObjectURL(activeMediaUrl);activeMediaUrl=null;}
  }
 }
 function renderQuiz(s){
  const panel=$('quiz');panel.replaceChildren();
  if(s.participants.length!==2||!s.participants.every(p=>p.consent)||!packs.length)return;
  const mine=s.participants.find(p=>p.id===identity.participantId),q=s.quiz;
  if(!q){if(mine?.host){
   const select=document.createElement('select');packs.forEach((p,i)=>{const o=document.createElement('option');o.value=i;o.textContent=p.name;select.append(o);});
   const button=document.createElement('button');button.textContent='فتح اختبار مشترك';button.onclick=()=>action('quiz_open',{pack:Number(select.value)});
   panel.append(select,button);
  }return;}
  if(q.id!==currentQuizId){currentQuizId=q.id;answers=[];}
  const heading=document.createElement('h2');heading.textContent=`اختبار مشترك: ${packs[q.pack].name}`;panel.append(heading);
  if(mine?.host){const close=document.createElement('button');close.className='secondary';close.textContent='إغلاق الاختبار';close.onclick=()=>action('quiz_close');panel.append(close);}
  if(q.result){
   for(const v of q.result.counts||[]){
    const p=document.createElement('p');const name=s.participants.find(m=>m.id===v.id)?.name||'مشارك';
    p.textContent=`${name}: `+v.values.map((n,i)=>`الخيار ${i+1}: ${n}`).join('، ');panel.append(p);
   }
   const p=document.createElement('p');
   p.textContent=`تطابقت ${q.result.agreement} من ${q.result.questions} إجابات. هذه مقارنة وصفية لا تقيس التوافق النفسي.`;
   panel.append(p);return;
  }
  if(q.submitted.includes(identity.participantId)){const p=document.createElement('p');p.textContent='وصلت إجاباتك، ننتظر المشارك الثاني. لا تُعرض إجابات منفردة.';panel.append(p);return;}
  const step=answers.length;
  if(step>=8){const done=document.createElement('button');done.textContent='إرسال مقارنة مشفرة';done.onclick=async()=>{try{action('quiz_submit',{quizId:q.id,tags:await quizTags(q,answers)});}catch{notice('تعذر حساب رموز الاختبار');}};panel.append(done);return;}
  const question=packs[q.pack].questions[q.indices[step]];
  const p=document.createElement('p');p.textContent=`السؤال ${step+1}/8: ${question.text}`;panel.append(p);
  for(const [i,label] of question.choices.entries()){
   const button=document.createElement('button');button.className='secondary';button.textContent=label;
   button.onclick=()=>{answers.push(i);renderQuiz(s);};panel.append(button);
  }
 }
 function renderMedia(media){
  const panel=$('mediaList');panel.replaceChildren();
  for(const item of media){const b=document.createElement('button');b.className='secondary';
   b.textContent=`عرض ${ {image:'صورة',audio:'صوت',video:'فيديو',card:'بطاقة خاصة'}[item.kind] } مشفر — ${(item.size/1024).toFixed(0)} كيلوبايت`;
   b.disabled=item.kind==='card'&&item.authorId===identity.participantId;
   b.onclick=()=>download(item);panel.append(b);
  }
 }
 async function upload(file){
  if(!file)return;
  const kind=file.type.startsWith('image/')?'image':file.type.startsWith('audio/')?'audio':file.type.startsWith('video/')?'video':null;
  if(!kind)throw Error('الملف يجب أن يكون صورة أو صوتًا أو فيديو');
  const iv=crypto.getRandomValues(new Uint8Array(12));
  const types=new Set(['image/jpeg','image/png','image/webp','audio/mpeg','audio/ogg','audio/mp4','video/mp4','video/webm']);
  if(!types.has(file.type))throw Error('هذه الصيغة غير مدعومة');
  if(file.size>8*1024*1024-512)return SirrM6.upload(file,{roomId,auth:identity.auth,kind,key:await key(),progress:notice});
  const bytes=new Uint8Array(await file.arrayBuffer()),name=new TextEncoder().encode(file.type);
  const plaintext=new Uint8Array(1+name.length+bytes.length);plaintext[0]=name.length;plaintext.set(name,1);plaintext.set(bytes,1+name.length);
  const encrypted=await crypto.subtle.encrypt({name:'AES-GCM',iv,additionalData:new TextEncoder().encode(`${roomId}:${kind}`)},await key(),plaintext);
  const res=await fetch(`/api/rooms/${roomId}/media`,{method:'POST',headers:{Authorization:`Bearer ${identity.auth}`,'Content-Type':'application/octet-stream','X-Encrypted-Iv':b64(iv),'X-Media-Kind':kind},body:encrypted});
  if(!res.ok)throw Error('فشل رفع الملف المشفر؛ تحقق من حجم الملف ودورك في الجولة');
  const saved=await res.json();notice('تم إرسال الملف مشفرًا. ينتهي توفره خلال ١٥ دقيقة أو عند إغلاق الجلسة.');return saved.id;
 }
 async function download(item){
  try{
   let kind=item.kind,data;
   if(item.transfer==='chunked'){
    data=await SirrM6.download(item,{roomId,auth:identity.auth,key:await key(),progress:notice});
   }else{
    const res=await fetch(`/api/rooms/${roomId}/media/${item.id}`,{headers:{Authorization:`Bearer ${identity.auth}`}});
    if(!res.ok)throw Error('الملف منتهي الصلاحية أو غير متاح');
    const iv=un64(res.headers.get('X-Encrypted-Iv')||'');if(iv.length!==12)throw Error('مفتاح تشفير غير صالح');
    kind=res.headers.get('X-Media-Kind');if(kind!==item.kind)throw Error('نوع الملف غير مطابق');
    data=new Uint8Array(await crypto.subtle.decrypt({name:'AES-GCM',iv,additionalData:new TextEncoder().encode(`${roomId}:${kind}`)},await key(),await res.arrayBuffer()));
   }
   const len=data[0];if(len<1||len>64)throw Error('تنسيق ملف غير صالح');
   const mime=new TextDecoder().decode(data.slice(1,len+1));
   const supported=new Set(['image/jpeg','image/png','image/webp','audio/mpeg','audio/ogg','audio/mp4','video/mp4','video/webm']);
   if(kind==='card'){
    if(mime!=='application/vnd.sirr.card+json')throw Error('بطاقة غير مدعومة');
    const dataText=new TextDecoder('utf-8',{fatal:true}).decode(data.slice(len+1));
    const card=JSON.parse(dataText);
    if(![1,2].includes(card.version)||!['whispers','scratch'].includes(card.type)||!Array.isArray(card.lines)||
       card.lines.length<1||card.lines.length>3||(card.type==='whispers'&&card.lines.length!==3)||
       card.lines.some(s=>typeof s!=='string'||!s.trim()||[...s].length>350)) throw Error('محتوى البطاقة غير صالح');
    if(card.version===2){const a=card.attachments||{};if(Object.keys(a).some(k=>!['imageId','audioId'].includes(k))||Object.values(a).some(v=>typeof v!=='string'||!/^[0-9a-f-]{36}$/.test(v)))throw Error('مرفقات غير صالحة');}
    showCard(card);return;
   }
   if(!supported.has(mime)||!mime.startsWith(`${kind}/`))throw Error('ملف بامتداد غير مدعوم');
   if(activeMediaUrl)URL.revokeObjectURL(activeMediaUrl);
   activeMediaUrl=URL.createObjectURL(new Blob([data.slice(len+1)],{type:mime}));
   const viewer=$('viewer');viewer.replaceChildren();
   const element=document.createElement(kind==='image'?'img':kind==='audio'?'audio':'video');
   element.src=activeMediaUrl;element.style.maxWidth='100%';
   if(kind!=='image'){element.controls=true;element.preload='metadata';}
   viewer.append(element);
  }catch(e){notice(`تعذر فتح الوسيط: ${e.message}`);}
 }
 function showCard(card){
  const panel=$('viewer');panel.replaceChildren();let index=0;
  function show(){
   panel.replaceChildren();
   if(index>=card.lines.length){const end=document.createElement('p');end.textContent='انتهى عرض البطاقة.';panel.append(end);
    for(const [id,kind,label] of [[card.attachments?.imageId,'image','عرض الصورة الختامية'],[card.attachments?.audioId,'audio','تشغيل الصوت المرفق']])if(id){
     const b=document.createElement('button');b.textContent=label;b.onclick=()=>download({id,kind});panel.append(b);}
    return;}
   const title=document.createElement('h3');title.textContent=`${card.type==='whispers'?'همسة':'طبقة كشف'} ${index+1}/${card.lines.length}`;panel.append(title);
   const content=document.createElement('article');content.className='challenge';content.textContent=card.lines[index];
   panel.append(content);
   const advance=document.createElement('button');advance.textContent=index+1===card.lines.length?'إنهاء':'الرسالة التالية';
   advance.className='secondary';advance.onclick=()=>{index++;show();};
   if(card.type==='scratch'){
    advance.disabled=true;
    const canvas=document.createElement('canvas');canvas.width=480;canvas.height=220;
    canvas.style.cssText='display:block;max-width:100%;width:100%;height:220px;touch-action:none;border-radius:14px;margin-top:-220px;position:relative;';
    content.style.minHeight='220px';content.style.display='flex';content.style.alignItems='center';
    content.style.justifyContent='center';
    const ctx=canvas.getContext('2d');ctx.fillStyle='#bda581';ctx.fillRect(0,0,480,220);
    ctx.globalCompositeOperation='destination-out';
    let down=false;const cells=new Set();
    function mark(evt){
      const box=canvas.getBoundingClientRect();
      const x=(evt.clientX-box.left)/box.width*480,y=(evt.clientY-box.top)/box.height*220;
      if(x<0||x>480||y<0||y>220)return;
      ctx.beginPath();ctx.arc(x,y,24,0,Math.PI*2);ctx.fill();
      const col=Math.min(11,Math.max(0,Math.floor(x/40))),row=Math.min(9,Math.max(0,Math.floor(y/22)));
      for(let dx=-1;dx<=1;dx++)for(let dy=-1;dy<=1;dy++){
       const cx=col+dx,cy=row+dy;if(cx>=0&&cx<12&&cy>=0&&cy<10)cells.add(cy*12+cx);
      }
      if(cells.size>=66){advance.disabled=false;canvas.remove();fxCue('reveal');}
    }
    canvas.addEventListener('pointerdown',e=>{down=true;canvas.setPointerCapture(e.pointerId);mark(e);});
    canvas.addEventListener('pointermove',e=>{if(down)mark(e);});
    canvas.addEventListener('pointerup',()=>{down=false;});
    canvas.addEventListener('pointercancel',()=>{down=false;});
    panel.append(canvas);
   }
   panel.append(advance);
  }show();
 }
 async function sendCard(){
  const type=$('cardType').value;
  const controls=[$('cardOne'),$('cardTwo'),$('cardThree')].slice(0,type==='whispers'?3:2);
  const lines=controls.map(c=>c.value.trim());
  if(!lines.every(s=>s.length>0&&[...s].length<=350))throw Error('أكمل الرسائل، بحد ٣٥٠ حرفًا لكل رسالة');
  const attachments={};
  if($('cardImage').files?.[0]){if($('cardImage').files[0].size>8*1024*1024-512)throw Error('الصورة الختامية لا تتجاوز ٨ ميجابايت');
   attachments.imageId=await upload($('cardImage').files[0]);}
  if($('cardAudio').files?.[0]){if($('cardAudio').files[0].size>8*1024*1024-512)throw Error('الصوت المرفق لا يتجاوز ٨ ميجابايت');
   attachments.audioId=await upload($('cardAudio').files[0]);}
  const bytes=new TextEncoder().encode(JSON.stringify({version:2,type,lines,attachments}));
  if(bytes.length>12*1024)throw Error('البطاقة طويلة جدًا');
  const mime='application/vnd.sirr.card+json',name=new TextEncoder().encode(mime);
  const plaintext=new Uint8Array(1+name.length+bytes.length);plaintext[0]=name.length;
  plaintext.set(name,1);plaintext.set(bytes,1+name.length);
  const iv=crypto.getRandomValues(new Uint8Array(12));
  const ciphertext=await crypto.subtle.encrypt({name:'AES-GCM',iv,additionalData:new TextEncoder().encode(`${roomId}:card`)},await key(),plaintext);
  const result=await fetch(`/api/rooms/${roomId}/media`,{method:'POST',headers:{Authorization:`Bearer ${identity.auth}`,
   'Content-Type':'application/octet-stream','X-Encrypted-Iv':b64(iv),'X-Media-Kind':'card'},body:ciphertext});
  if(!result.ok)throw Error('تعذر إرسال البطاقة المشفرة');
  controls.forEach(c=>c.value='');$('cardImage').value='';$('cardAudio').value='';notice('تم إرسال البطاقة المشفرة للمستلم. متاحة لأول فتح فقط.');
 }
 $('cardType').onchange=()=>$('cardThirdLabel').hidden=$('cardType').value==='scratch';
 $('sendCard').onclick=async()=>{try{await sendCard();}catch(error){notice(error.message);}};
 $('join').addEventListener('click',async()=>{
  const nickname=$('nickname').value.trim();
  if(!invite||!inviteKey||un64(inviteKey).length!==32)return notice('رابط الدعوة ناقص أو لا يحتوي مفتاح التشفير');
  if(!nickname||[...nickname].length>32||!$('adult').checked||!$('consent').checked)return notice('اكتب الاسم وأكّد العمر والموافقة');
  $('join').disabled=true;
  try{const res=await fetch(`/api/rooms/${roomId}/join`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({invite,nickname})});
   if(!res.ok)throw Error('رابط منتهي أو غرفة ممتلئة');
   identity={...await res.json(),key:inviteKey,ready:false};sessionStorage.setItem(storeKey,JSON.stringify(identity));connect();
  }catch(e){notice(e.message);$('join').disabled=false;}
 });
 $('complete').onclick=()=>{fxCue('complete');action('complete');};$('skip').onclick=()=>{fxCue('skip');action('skip');};
 $('send').onclick=async()=>{const body=$('reply').value.trim();if(body){try{await sendPrivateMessage(body);$('reply').value='';}catch{notice('تعذر تشفير الرسالة');}}};
 $('file').onchange=async e=>{try{await upload(e.target.files?.[0]);}catch(err){notice(err.message);}finally{e.target.value='';}};
 $('resumeUpload').onclick=async()=>{try{await SirrM6.resume({roomId,auth:identity.auth,key:await key(),progress:notice});}catch(e){notice(e.message);}};
 $('cancelUpload').onclick=async()=>{await SirrM6.cancel({roomId,auth:identity.auth});notice('تم طلب إلغاء النقل');};
 $('agree').onclick=()=>action('consent',{value:true});
 $('applyWaiting').onclick=async()=>{try{await configureLobby();notice('تم إرسال الإعدادات المشفرة');}catch(e){notice(e.message);}};
 $('leave').onclick=()=>{if(confirm('إنهاء الجلسة للجميع؟')){action('leave');deliberatelyLeft=true;sessionStorage.removeItem(storeKey);}};
 window.addEventListener('pagehide',()=>{if(audioCtx)void audioCtx.close();if(activeMediaUrl)URL.revokeObjectURL(activeMediaUrl);clearTimeout(retryTimer);});
 loadQuizzes();if(identity)connect();
})();
