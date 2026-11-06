'use strict';
const assert = require('node:assert/strict');
const express = require('./backend-require')('express');
const cookieParser = require('./backend-require')('cookie-parser');
const { resolveReadSeason, visibleSeasons, installPublicData } = require('../../../backend_server/website_backend/public-data');
(async () => {
 const calls=[]; let rankMode = 'id';
 const seasons=[{id:9,name:'Preseason 8',is_preseason:1,is_current:1},{id:8,name:'Season 7',is_preseason:0,is_current:0},{id:1,name:'Preseason',is_preseason:1,is_current:0}];
 seasons.unshift({id:11,name:'Season 10',is_preseason:0,is_current:0});
 seasons.push({id:7,name:'Season 6',is_preseason:0,is_current:0},{id:6,name:'Season 5',is_preseason:0,is_current:0});
 const pool={query:async(sql,args=[])=>{
   calls.push({sql,args});
   if(sql.includes('FROM website_sessions'))return [[{player_id:42,display_name:'firestarter03'}]];
   if(sql.includes('SELECT id FROM seasons WHERE'))return [seasons.filter(s=>s.id===args[0])];
   if(sql.includes('FROM seasons ORDER'))return [seasons];
   if(sql.includes('AS player_rank')) {
     if(sql.includes('l.player_id IS NULL')) return [rankMode==='legacy' ? [{score:'123456',player_rank:12}] : []];
     return [rankMode==='id' ? [{score:'9007199254740993',player_rank:7}] : []];
   }
   if(sql.includes('CAST(l.score AS CHAR)'))return [[{player:'Example',score:'9007199254740993'}]];
   throw new Error('Unexpected SQL');
 }};
 assert.equal(await resolveReadSeason(pool,{query:{}},async()=>9),9);
 assert.equal(await resolveReadSeason(pool,{query:{season_id:'1'}},async()=>9),1);
 for(const value of ['0','-1','1 OR 1=1',['1','9'],'1.5'])await assert.rejects(resolveReadSeason(pool,{query:{season_id:value}},async()=>9),e=>e.status===400);
 await assert.rejects(resolveReadSeason(pool,{query:{season_id:'99'}},async()=>9),e=>e.status===404);
 const app=express();app.use(cookieParser());installPublicData(app,pool,async()=>9);
 const server=await new Promise(r=>{const s=app.listen(0,'127.0.0.1',()=>r(s))});
 try{
  const base='http://127.0.0.1:'+server.address().port;
  const data=await(await fetch(base+'/website/data/seasons')).json();
  assert.equal(data[0].name,'Preseason 8');assert.deepEqual(data.map(s=>s.id),[9,8,7]);assert.equal(data[0].is_current,true);
  const rows=await(await fetch(base+'/website/data/leaderboard?board=alltime_kills&season_id=8')).json();
  assert.equal(rows[0].score,'9007199254740993');assert.deepEqual(calls.find(c=>c.sql.includes('CAST(l.score AS CHAR)')).args,[8,'alltime_kills']);
  assert.equal((await fetch(base+'/website/data/leaderboard?board=bad!')).status,400);
  assert.equal((await fetch(base+'/website/data/my-rank?board=alltime_kills&season_id=8')).status,401);
  const res=await fetch(base+'/website/data/my-rank?board=alltime_kills&season_id=8',{headers:{Cookie:'cclive_web='+'a'.repeat(64)}});
  const rank=await res.json();assert.equal(rank.self.rank,7);assert.equal(rank.self.score,'9007199254740993');assert.equal(res.headers.get('cache-control'),'private, no-store');
  assert.deepEqual(calls.find(c=>c.sql.includes('AS player_rank')).args,[8,'alltime_kills',42]);
  rankMode='legacy';
  const legacy=await(await fetch(base+'/website/data/my-rank?board=alltime_kills&season_id=8',{headers:{Cookie:'cclive_web='+'a'.repeat(64)}})).json();
  assert.equal(legacy.self.rank,12);assert.equal(legacy.self.score,'123456');
  assert.deepEqual(calls.find(c=>c.sql.includes('l.player_id IS NULL')).args,[8,'alltime_kills','firestarter03']);
  rankMode='none';
  const absent=await(await fetch(base+'/website/data/my-rank?board=alltime_kills&season_id=8',{headers:{Cookie:'cclive_web='+'a'.repeat(64)}})).json();assert.equal(absent.self,null);
  assert.deepEqual(visibleSeasons([{id:81,name:'Season 5',is_current:0},{id:100,name:'Season 6',is_current:1}]).map(s=>s.id),[100]);
  assert.deepEqual(visibleSeasons([{id:81,name:'Season 8',is_current:0}]),[]);
  console.log('public-data tests ok: season names/IDs, validation, exact scores and identity-bound rank');
 }finally{await new Promise(r=>server.close(r));}
})().catch(err=>{console.error(err);process.exitCode=1;});
