'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const express = require('./backend-require')('express');
const cookieParser = require('./backend-require')('cookie-parser');
const { createAvatarCache, installAvatarCache, MAX_BYTES } = require('../../../backend_server/website_backend/avatar-cache');
const UUID = '47a5c2fcb3174180b96fca977c51eb62';
const DAY = 86400000;
// A PNG-shaped fixture tests signature, dimensions and bounded transfer, not image rendering.
function png() {
  const b = Buffer.alloc(45); Buffer.from('89504e470d0a1a0a','hex').copy(b);
  b.writeUInt32BE(13,8); b.write('IHDR',12); b.writeUInt32BE(64,16); b.writeUInt32BE(64,20);
  b.write('IEND',37); return b;
}
(async () => {
 const directory = await fs.mkdtemp(path.join(os.tmpdir(),'cclive-avatar-test-'));
 let server;
 try {
  let time = Date.now(), calls = 0, fail = false, oversized = false;
  const cache = createAvatarCache({ directory, now: () => time, fetchImpl: async (url, opts) => {
   calls++; assert.equal(url,'https://mc-heads.net/avatar/'+UUID+'/64');
   assert.equal(opts.redirect,'error'); assert.equal(opts.headers.Cookie,undefined);
   await new Promise(r => setTimeout(r,15));
   if (fail) throw new Error('offline');
   return new Response(oversized ? Buffer.alloc(MAX_BYTES+1) : png(),{headers:{'content-type':'image/png'}});
  }});
  await assert.rejects(cache.get('../escape'));
  const results=await Promise.all([cache.get(UUID),cache.get(UUID)]);
  assert.equal(calls,1); assert.deepEqual(results[0],png());
  await cache.get(UUID); assert.equal(calls,1);
  time+=DAY+1; await cache.get(UUID); assert.equal(calls,2);
  time+=DAY+1; fail=true;
  assert.deepEqual(await cache.get(UUID),png()); assert.equal(calls,3);
  await cache.get(UUID); assert.equal(calls,3);
  time+=6*60000; fail=false; oversized=true;
  assert.deepEqual(await cache.get(UUID),png()); assert.equal(calls,4);
  await assert.rejects(cache.get('a'.repeat(32))); // no cached image, failed upstream never saved
  time+=31*DAY; await cache.cleanup();
  assert.equal((await fs.readdir(directory)).filter(n=>n.endsWith('.png')).length,0);
  const app=express(); app.use(cookieParser());
  const installation=installAvatarCache(app,{query:async()=>[[{mc_uuid:UUID}]]},{directory,fetchImpl:async()=>new Response(png(),{headers:{'content-type':'image/png'}})});
  server=await new Promise(resolve=>{const s=app.listen(0,'127.0.0.1',()=>resolve(s));});
  const base='http://127.0.0.1:'+server.address().port+'/website/avatar/';
  assert.equal((await fetch(base+UUID)).status,401);
  const headers={Cookie:'cclive_web='+'a'.repeat(64)};
  assert.equal((await fetch(base+'a'.repeat(32),{headers})).status,403);
  assert.equal((await fetch(base+'invalid',{headers})).status,400);
  const response=await fetch(base+UUID,{headers});
  assert.equal(response.status,200); assert.match(response.headers.get('content-type'),/image\/png/);
  assert.equal(response.headers.get('cache-control'),'private, no-store');
  assert.deepEqual(Buffer.from(await response.arrayBuffer()),png());
  installation.close();
  console.log('avatar-cache tests ok');
 } finally {
  if(server) await new Promise(resolve=>server.close(resolve));
  await fs.rm(directory,{recursive:true,force:true});
 }
})().catch(err=>{console.error(err);process.exitCode=1;});
