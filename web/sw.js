const CACHE_NAME='prompt-notes-shell-v1.1.0';
const APP_ROOT=self.registration.scope;
const INDEX_URL=new URL('index.html',APP_ROOT).href;
const SHELL=[APP_ROOT,INDEX_URL,new URL('manifest.webmanifest',APP_ROOT).href,new URL('icon.svg',APP_ROOT).href];
self.addEventListener('install',event=>{
  event.waitUntil(caches.open(CACHE_NAME).then(cache=>cache.addAll(SHELL)).then(()=>self.skipWaiting()));
});
self.addEventListener('activate',event=>{
  event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(key=>key!==CACHE_NAME).map(key=>caches.delete(key)))).then(()=>self.clients.claim()));
});
self.addEventListener('fetch',event=>{
  const request=event.request;
  if(request.method!=='GET')return;
  const url=new URL(request.url);
  if(url.origin!==self.location.origin)return;
  if(request.mode==='navigate'||url.href===INDEX_URL||url.href===APP_ROOT){
    event.respondWith(fetch(request,{cache:'no-store'}).then(response=>{
      if(response&&response.ok){
        const copy=response.clone();
        caches.open(CACHE_NAME).then(cache=>cache.put(INDEX_URL,copy));
      }
      return response;
    }).catch(()=>caches.match(INDEX_URL).then(cached=>cached||caches.match(APP_ROOT))));
  }
});
