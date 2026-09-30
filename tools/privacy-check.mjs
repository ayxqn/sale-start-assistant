import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const files=[];
function visit(dir){for(const e of fs.readdirSync(dir,{withFileTypes:true})){if(['.git','build','dist','output','.private','node_modules'].includes(e.name))continue;const f=path.join(dir,e.name);if(e.isDirectory())visit(f);else files.push(f);}}
visit(root);
const problems=[];
for(const f of files){const rel=path.relative(root,f);if(/\.(?:dpapi|jks|keystore|p12|rsa\.json|apk|zip)$/i.test(rel))problems.push(rel+': private or generated file');
const s=fs.readFileSync(f,'utf8');if(/(?:gh[pousr]_[A-Za-z0-9]{25,}|github_pat_[A-Za-z0-9_]{30,}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----)/.test(s))problems.push(rel+': possible credential');
if(/(?:SESSDATA|bili_jct|access_token|refresh_token)\s*[:=]\s*["'][^"']{10,}/.test(s)&&!rel.includes('privacy-check'))problems.push(rel+': possible session');
if(/C:[\\/]Users[\\/]|(?<![:/\w])[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/.test(s)&&!rel.includes('privacy-check'))problems.push(rel+': personal path or email');}
const manifest=fs.readFileSync(path.join(root,'app/src/main/AndroidManifest.xml'),'utf8');
if(/uses-permission|service|receiver|provider/.test(manifest))problems.push('manifest: unexpected permission or background component');
if(!manifest.includes('android:allowBackup="false"'))problems.push('manifest: backups must be disabled');
if(problems.length){console.error(problems.join('\n'));process.exit(1);}
console.log(`Privacy check passed on ${files.length} source/document files. Manual review still required.`);
