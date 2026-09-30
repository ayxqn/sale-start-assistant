param(
    [Parameter(Mandatory=$true)][string]$BuildTools,
    [Parameter(Mandatory=$true)][string]$AndroidJar,
    [Parameter(Mandatory=$true)][string]$Jdk,
    [string]$SigningDirectory = (Join-Path $env:LOCALAPPDATA 'SaleStartAssistantSigning')
)
$ErrorActionPreference='Stop'
Set-StrictMode -Version Latest
$taskRoot=Split-Path $PSScriptRoot -Parent
$taskBuild=Join-Path $taskRoot 'build'
$taskDist=Join-Path $taskRoot 'dist'
New-Item -ItemType Directory -Path $taskBuild,$taskDist -Force | Out-Null
function Run-Tool([string]$tool,[string[]]$arguments) {
    & $tool @arguments
    if ($LASTEXITCODE -ne 0) { throw "Build tool failed: $([IO.Path]::GetFileName($tool))" }
}
$taskJava=Join-Path $Jdk 'bin/java.exe'
$taskJavac=Join-Path $Jdk 'bin/javac.exe'
$taskAapt=Join-Path $BuildTools 'aapt2.exe'
if($BuildTools -match '[^\x00-\x7F]' -or $AndroidJar -match '[^\x00-\x7F]') {throw 'Put the Android SDK in an ASCII-only path; some Windows SDK tools cannot read Unicode paths.'}
foreach($taskFile in @($taskJava,$taskJavac,$taskAapt,$AndroidJar)) {
    if(-not(Test-Path -LiteralPath $taskFile -PathType Leaf)){throw 'A required Android SDK or JDK file is missing.'}
}
if(-not(Test-Path -LiteralPath (Join-Path $taskRoot 'app/src/main/assets/fonts/ChillRoundF.ttf'))){throw 'Required bundled rounded font is missing.'}
$taskTests=Join-Path $taskBuild 'tests'
New-Item -ItemType Directory -Path $taskTests -Force | Out-Null
$taskDeps=@(Get-ChildItem -LiteralPath (Join-Path $taskRoot 'dependencies') -Filter '*.jar' | Select-Object -ExpandProperty FullName)
if($taskDeps.Count -ne 7){throw 'Run node tools/fetch-deps.mjs before building.'}
$taskJsonTest=Join-Path $taskRoot 'test-dependencies/json-20240303.jar'
$taskRules=Join-Path $taskRoot 'app/src/main/java/io/github/ayxqn/salestart/PurchaseRules.java'
$taskClient=Join-Path $taskRoot 'app/src/main/java/io/github/ayxqn/salestart/BiliClient.java'
Run-Tool $taskJavac @('-encoding','UTF-8','--release','8','-cp',$taskJsonTest,'-d',$taskTests,$taskRules,$taskClient,(Join-Path $taskRoot 'tests/PurchaseTest.java'))
Run-Tool $taskJava @('-cp',($taskTests+';'+$taskJsonTest),'io.github.ayxqn.salestart.PurchaseTest')

$taskResources=Join-Path $taskBuild 'resources.zip'
Run-Tool $taskAapt @('compile','--dir',(Join-Path $taskRoot 'app/src/main/res'),'-o',$taskResources)
$taskUnsigned=Join-Path $taskBuild 'unsigned.apk'
Run-Tool $taskAapt @('link','-o',$taskUnsigned,'-I',$AndroidJar,'--manifest',(Join-Path $taskRoot 'app/src/main/AndroidManifest.xml'),'-A',(Join-Path $taskRoot 'app/src/main/assets'),'--min-sdk-version','26','--target-sdk-version','35',$taskResources)
$taskBuildId=[Guid]::NewGuid().ToString('N')
$taskClasses=Join-Path $taskBuild ('classes-'+$taskBuildId)
$taskDex=Join-Path $taskBuild ('dex-'+$taskBuildId)
New-Item -ItemType Directory -Path $taskClasses,$taskDex -Force | Out-Null
$taskSources=@(Get-ChildItem -LiteralPath (Join-Path $taskRoot 'app/src/main/java') -Filter '*.java' -Recurse | Select-Object -ExpandProperty FullName)
$taskCompileClasspath=(@($AndroidJar)+$taskDeps) -join ';'
Run-Tool $taskJavac (@('-encoding','UTF-8','--release','8','-classpath',$taskCompileClasspath,'-d',$taskClasses)+$taskSources)
$taskClassesJar=Join-Path $taskBuild 'classes.jar'
Add-Type -AssemblyName System.IO.Compression.FileSystem
if(Test-Path -LiteralPath $taskClassesJar){[IO.File]::Delete($taskClassesJar)}
[IO.Compression.ZipFile]::CreateFromDirectory($taskClasses,$taskClassesJar)
Run-Tool $taskJava (@('-cp',(Join-Path $BuildTools 'lib/d8.jar'),'com.android.tools.r8.D8','--release','--min-api','26','--lib',$AndroidJar,'--output',$taskDex,$taskClassesJar)+$taskDeps)
# Generated APK modification is a build output, not source editing.
Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskZip=[IO.Compression.ZipFile]::Open($taskUnsigned,[IO.Compression.ZipArchiveMode]::Update)
try {
    # Windows aapt2 may keep backslashes in nested asset names; Android uses '/'.
    $taskAssetEntries=@($taskZip.Entries | Where-Object { $_.FullName.Contains('\') })
    foreach($taskEntry in $taskAssetEntries){
        $taskCanonical=$taskEntry.FullName.Replace('\','/')
        if($taskZip.GetEntry($taskCanonical)){throw 'Duplicate canonical APK asset path'}
        $taskNewEntry=$taskZip.CreateEntry($taskCanonical,[IO.Compression.CompressionLevel]::Optimal)
        $taskRead=$taskEntry.Open();$taskWrite=$taskNewEntry.Open()
        try{$taskRead.CopyTo($taskWrite)}finally{$taskRead.Dispose();$taskWrite.Dispose()}
        $taskEntry.Delete()
    }
    foreach($taskDexFile in Get-ChildItem -LiteralPath $taskDex -Filter '*.dex') {
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($taskZip,$taskDexFile.FullName,$taskDexFile.Name,[IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
} finally {$taskZip.Dispose()}
$taskAligned=Join-Path $taskBuild 'aligned.apk'
Run-Tool (Join-Path $BuildTools 'zipalign.exe') @('-f','-p','4',$taskUnsigned,$taskAligned)

# The signing key and its DPAPI-protected password stay outside the repository.
# Windows-only: do not copy the protected password to another Windows account.
New-Item -ItemType Directory -Path $SigningDirectory -Force | Out-Null
$taskCurrentSid=[Security.Principal.WindowsIdentity]::GetCurrent().User.Value
& icacls $SigningDirectory /inheritance:r /grant:r "*${taskCurrentSid}:(OI)(CI)F" 'SYSTEM:(OI)(CI)F' | Out-Null
if($LASTEXITCODE -ne 0){throw 'Could not restrict signing directory permissions.'}
$taskKey=Join-Path $SigningDirectory 'release.jks'
$taskPasswordFile=Join-Path $SigningDirectory 'password.dpapi'
if((Test-Path -LiteralPath $taskKey) -xor (Test-Path -LiteralPath $taskPasswordFile)){throw 'Signing key/password pair is incomplete; do not overwrite it.'}
if(-not(Test-Path -LiteralPath $taskPasswordFile)) {
    $taskRandom=[byte[]]::new(32)
    [Security.Cryptography.RandomNumberGenerator]::Fill($taskRandom)
    $taskNewPassword=[Convert]::ToBase64String($taskRandom)
    $taskSecure=ConvertTo-SecureString $taskNewPassword -AsPlainText -Force
    [IO.File]::WriteAllText($taskPasswordFile,(ConvertFrom-SecureString $taskSecure))
}else{$taskSecure=ConvertTo-SecureString ([IO.File]::ReadAllText($taskPasswordFile))}
$taskPointer=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($taskSecure)
try {
    $env:SALE_START_KEY_PASSWORD=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($taskPointer)
    if(-not(Test-Path -LiteralPath $taskKey)) {
        Run-Tool $taskJava @('sun.security.tools.keytool.Main','-genkeypair','-keystore',$taskKey,'-storetype','JKS','-alias','sale-start','-storepass:env','SALE_START_KEY_PASSWORD','-keypass:env','SALE_START_KEY_PASSWORD','-keyalg','RSA','-keysize','3072','-validity','3650','-dname','CN=Sale Start Assistant,OU=Personal Project,O=ayxqn')
    }
    $taskApk=Join-Path $taskDist 'sale-start-assistant-2.0.0.apk'
    Run-Tool $taskJava @('-jar',(Join-Path $BuildTools 'lib/apksigner.jar'),'sign','--ks',$taskKey,'--ks-key-alias','sale-start','--ks-pass','env:SALE_START_KEY_PASSWORD','--key-pass','env:SALE_START_KEY_PASSWORD','--out',$taskApk,$taskAligned)
    Run-Tool $taskJava @('-jar',(Join-Path $BuildTools 'lib/apksigner.jar'),'verify','--verbose','--print-certs',$taskApk)
    (Get-FileHash -LiteralPath $taskApk -Algorithm SHA256).Hash.ToLowerInvariant()+'  '+[IO.Path]::GetFileName($taskApk) | Set-Content -LiteralPath (Join-Path $taskDist 'SHA256SUMS.txt') -Encoding ascii
    Write-Output ('Built and signature-verified: '+$taskApk)
}finally{[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($taskPointer);Remove-Item Env:SALE_START_KEY_PASSWORD -ErrorAction SilentlyContinue}
