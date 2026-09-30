param([string]$BuildTools,[string]$AndroidJar,[string]$Jdk,[string]$SigningDirectory=(Join-Path $env:LOCALAPPDATA 'SaleStartAssistantSigning'))
$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$taskDir=Join-Path $taskRoot ('build/device-tests-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskDir,($taskDir+'/classes'),($taskDir+'/dex') -Force | Out-Null
function Invoke-TestTool([string]$exe,[string[]]$arguments){& $exe @arguments;if($LASTEXITCODE -ne 0){throw 'Device test build failed'}}
Invoke-TestTool ($Jdk+'/bin/javac.exe') @('-encoding','UTF-8','--release','8','-cp',($AndroidJar+';'+$taskRoot+'/build/classes.jar'),'-d',($taskDir+'/classes'),($taskRoot+'/tests/PurchaseTest.java'),($taskRoot+'/tests/DeviceSelfTest.java'))
Add-Type -AssemblyName System.IO.Compression.FileSystem
[IO.Compression.ZipFile]::CreateFromDirectory(($taskDir+'/classes'),($taskDir+'/tests.jar'))
Invoke-TestTool ($Jdk+'/bin/java.exe') @('-cp',($BuildTools+'/lib/d8.jar'),'com.android.tools.r8.D8','--release','--min-api','26','--lib',$AndroidJar,'--classpath',($taskRoot+'/build/classes.jar'),'--output',($taskDir+'/dex'),($taskDir+'/tests.jar'))
Invoke-TestTool ($BuildTools+'/aapt2.exe') @('link','-o',($taskDir+'/unsigned.apk'),'-I',$AndroidJar,'--manifest',($taskRoot+'/tests/AndroidManifest.xml'))
$taskZip=[IO.Compression.ZipFile]::Open(($taskDir+'/unsigned.apk'),[IO.Compression.ZipArchiveMode]::Update)
try{[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($taskZip,($taskDir+'/dex/classes.dex'),'classes.dex')|Out-Null}finally{$taskZip.Dispose()}
Invoke-TestTool ($BuildTools+'/zipalign.exe') @('-f','4',($taskDir+'/unsigned.apk'),($taskDir+'/aligned.apk'))
$taskSecure=ConvertTo-SecureString ([IO.File]::ReadAllText(($SigningDirectory+'/password.dpapi')))
$taskPointer=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($taskSecure)
try{
 $env:SALE_TEST_KEY_PASSWORD=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($taskPointer)
 Invoke-TestTool ($Jdk+'/bin/java.exe') @('-jar',($BuildTools+'/lib/apksigner.jar'),'sign','--ks',($SigningDirectory+'/release.jks'),'--ks-key-alias','sale-start','--ks-pass','env:SALE_TEST_KEY_PASSWORD','--out',($taskRoot+'/output/device-selftest.apk'),($taskDir+'/aligned.apk'))
}finally{[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($taskPointer);Remove-Item Env:SALE_TEST_KEY_PASSWORD -ErrorAction SilentlyContinue}
Write-Output 'Built output/device-selftest.apk (not a release asset).'
