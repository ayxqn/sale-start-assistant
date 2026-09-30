// Explicit maintainer publication tool. Credentials stay in process memory.
import fs from 'node:fs';
import path from 'node:path';
import cp from 'node:child_process';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
if(!process.argv.includes('--publish'))throw Error('Use --publish only when a maintainer intends to publish the reviewed artifacts.');
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..'),repo='ayxqn/sale-start-assistant',tag='v2.0.0';
const git=process.env.SALE_GIT_EXE||'git';
const run=(args,extra={})=>cp.execFileSync(git,args,{cwd:root,encoding:'utf8',stdio:['pipe','pipe','pipe'],env:{...process.env,GIT_TERMINAL_PROMPT:'0',GCM_INTERACTIVE:'Never'},...extra}).trim();
if(run(['status','--porcelain']))throw Error('Commit reviewed source before publishing.');
const sha=run(['rev-parse','HEAD']);
if(!run(['ls-remote','origin','refs/heads/main']).startsWith(sha))throw Error('Push the reviewed commit before publishing.');
const raw=run(['-c','credential.interactive=never','credential','fill'],{input:'protocol=https\nhost=github.com\n\n'});
const credentials=Object.fromEntries(raw.split(/\r?\n/).map(l=>{const i=l.indexOf('=');return[l.slice(0,i),l.slice(i+1)];}));
if(!credentials.password)throw Error('Existing GitHub credential unavailable.');
async function request(url,method='GET',body,type='application/json'){
 const u=new URL(url);if(u.protocol!=='https:'||!['api.github.com','uploads.github.com'].includes(u.hostname)||!u.pathname.startsWith('/repos/'+repo+'/'))throw Error('Unexpected publication destination');
 const r=await fetch(url,{method,redirect:'error',signal:AbortSignal.timeout(type==='application/json'?60000:600000),headers:{Authorization:'Bearer '+credentials.password,Accept:'application/vnd.github+json','X-GitHub-Api-Version':'2022-11-28','User-Agent':'SaleStartAssistantRelease',...(body?{'Content-Type':type}:{})},body:body?(type==='application/json'?JSON.stringify(body):body):undefined});
 if(!r.ok)throw Error('GitHub request failed, HTTP '+r.status);return r.json();
}
const base='https://api.github.com/repos/'+repo;
const notes=fs.readFileSync(path.join(root,'docs/RELEASE_2.md'),'utf8');
let release=(await request(base+'/releases')).find(x=>x.tag_name===tag);
if(!release)release=await request(base+'/releases','POST',{tag_name:tag,target_commitish:sha,name:'开售小助手 2.0.0 · 独立登录与后台订单',body:notes,draft:true,prerelease:true,make_latest:'false'});
if(!release.prerelease)throw Error('Unexpected non-prerelease state.');
const files=['sale-start-assistant-2.0.0.apk','sale-start-assistant-manual-2.0-zh-CN.pdf','sale-start-assistant-source-2.0.0.zip','SHA256SUMS.txt'];
for(const name of files){
 const bytes=fs.readFileSync(path.join(root,'dist',name)),hash=crypto.createHash('sha256').update(bytes).digest('hex');
 const existing=release.assets.find(a=>a.name===name);
 if(existing){if(existing.size!==bytes.length||existing.digest!=='sha256:'+hash)throw Error('Existing release asset differs; never overwrite silently.');continue;}
 const type=name.endsWith('.apk')?'application/vnd.android.package-archive':name.endsWith('.pdf')?'application/pdf':name.endsWith('.zip')?'application/zip':'text/plain';
 const a=await request(release.upload_url.replace(/\{.*$/,'')+'?name='+encodeURIComponent(name),'POST',bytes,type);
 if(a.state!=='uploaded'||a.size!==bytes.length||a.digest!=='sha256:'+hash)throw Error('Uploaded asset verification failed.');console.log('Verified upload: '+name);
}
release=await request(base+'/releases/'+release.id);
if(!files.every(n=>release.assets.some(a=>a.name===n&&a.state==='uploaded')))throw Error('Missing assets.');
if(release.draft)release=await request(base+'/releases/'+release.id,'PATCH',{draft:false,prerelease:true,body:notes,make_latest:'false'});
const receipt={url:release.html_url,tag,commit:sha,prerelease:release.prerelease,assets:release.assets.map(a=>({name:a.name,size:a.size,digest:a.digest,url:a.browser_download_url}))};
fs.mkdirSync(path.join(root,'output'),{recursive:true});fs.writeFileSync(path.join(root,'output','release-v2-receipt.json'),JSON.stringify(receipt,null,2));console.log(JSON.stringify(receipt));
