import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..'),files=[];
function visit(dir){for(const e of fs.readdirSync(dir,{withFileTypes:true})){if(['.git','build','dist','output','.private','node_modules','dependencies','test-dependencies','__pycache__'].includes(e.name))continue;const f=path.join(dir,e.name);if(e.isDirectory())visit(f);else files.push(f);}}
visit(root);
const problems=[];
for(const f of files){
 const rel=path.relative(root,f).replaceAll('\\','/');
 if(/\.(?:dpapi|jks|keystore|p12|rsa\.json|apk|zip|log|jsonl)$/i.test(rel))problems.push(rel+': private or generated file');
 if(/\.(png|ttf)$/i.test(rel))continue;
 const s=fs.readFileSync(f,'utf8');
 if(/(?:gh[pousr]_[A-Za-z0-9]{25,}|github_pat_[A-Za-z0-9_]{30,}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----)/.test(s))problems.push(rel+': possible credential');
 if(/(?:SESSDATA|bili_jct|access_token|refresh_token)\s*[:=]\s*["'][^"']{10,}/.test(s)&&!rel.includes('privacy-check'))problems.push(rel+': possible session');
 if(!rel.startsWith('app/src/main/assets/licenses/')&&!rel.includes('privacy-check')&&/C:[\\/]Users[\\/]|(?<![:/\w])[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/.test(s))problems.push(rel+': personal path or email');
}
const manifest=fs.readFileSync(path.join(root,'app/src/main/AndroidManifest.xml'),'utf8');
const expected=['INTERNET','WAKE_LOCK','FOREGROUND_SERVICE','FOREGROUND_SERVICE_SPECIAL_USE','POST_NOTIFICATIONS','REQUEST_IGNORE_BATTERY_OPTIMIZATIONS'].map(x=>'android.permission.'+x);
const actual=[...manifest.matchAll(/<uses-permission android:name="([^"]+)"/g)].map(m=>m[1]);
if(actual.length!==expected.length||actual.some(x=>!expected.includes(x)))problems.push('manifest: unexpected permission set');
if(!manifest.includes('android:allowBackup="false"')||!manifest.includes('android:usesCleartextTraffic="false"')||manifest.includes('android:debuggable="true"')||manifest.includes('<receiver'))problems.push('manifest: unsafe backup/network/debug/receiver configuration');
for(const kind of ['service','provider'])if([...manifest.matchAll(new RegExp('<'+kind+'\\s[^>]*>','g'))].some(m=>!m[0].includes('android:exported="false"')))problems.push('manifest: private component is exported');
if(problems.length){console.error(problems.join('\n'));process.exit(1);}
console.log('Privacy boundary check passed on '+files.length+' source/resource files; exactly six declared permissions. This is not a full security audit.');
