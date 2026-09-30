import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const output=path.join(root,'dependencies');fs.mkdirSync(output,{recursive:true});
const artifacts=[
 ['com/squareup/okhttp3/okhttp','4.12.0'],['com/squareup/okio/okio-jvm','3.6.0'],
 ['org/jetbrains/kotlin/kotlin-stdlib','1.8.21'],['org/jetbrains/kotlin/kotlin-stdlib-common','1.8.21'],
 ['org/jetbrains/kotlin/kotlin-stdlib-jdk7','1.8.21'],['org/jetbrains/kotlin/kotlin-stdlib-jdk8','1.8.21'],['org/jetbrains/annotations','13.0'],['org/json/json','20240303']
];
const inventory=[];
const lock=JSON.parse(fs.readFileSync(path.join(root,'dependencies.lock.json'),'utf8'));
for(const [group,version] of artifacts){
 const target=group==='org/json/json'?path.join(root,'test-dependencies'):output;fs.mkdirSync(target,{recursive:true});
 const artifact=group.split('/').at(-1),filename=artifact+'-'+version+'.jar';
 const url='https://repo.maven.apache.org/maven2/'+group+'/'+version+'/'+filename;
 const sumResponse=await fetch(url+'.sha256',{redirect:'error',signal:AbortSignal.timeout(30000)});
 const fallback=!sumResponse.ok;const sr=fallback?await fetch(url+'.sha1',{redirect:'error',signal:AbortSignal.timeout(30000)}):sumResponse;
 if(!sr.ok)throw Error('Missing published checksum: '+filename);
 const checksum=(await sr.text()).trim().split(/\s+/)[0].toLowerCase(),algo=fallback?'sha1':'sha256';
 if(!new RegExp('^[a-f0-9]{'+(fallback?40:64)+'}$').test(checksum))throw Error('Bad checksum');
 let bytes=fs.existsSync(path.join(target,filename))?fs.readFileSync(path.join(target,filename)):null;
 if(!bytes||crypto.createHash(algo).update(bytes).digest('hex')!==checksum){
   const response=await fetch(url,{redirect:'error',signal:AbortSignal.timeout(30000)});if(!response.ok)throw Error('Dependency download failed: '+filename);
   bytes=Buffer.from(await response.arrayBuffer());if(crypto.createHash(algo).update(bytes).digest('hex')!==checksum)throw Error('Checksum failed: '+filename);
   fs.writeFileSync(path.join(target,filename),bytes);
 }
 const sha256=crypto.createHash('sha256').update(bytes).digest('hex');if(!lock.some(x=>x.filename===filename&&x.sha256===sha256&&x.url===url))throw Error('Pinned dependency hash differs: '+filename);inventory.push({filename,version,url,sha256,published_checksum_algorithm:algo});
 console.log('Verified '+filename);
}
fs.writeFileSync(path.join(output,'inventory.json'),JSON.stringify(inventory,null,2));
