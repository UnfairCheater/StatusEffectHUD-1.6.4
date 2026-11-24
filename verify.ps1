<#
    verify.ps1 - static verification of the built StatusEffectHUD jar.

    Three checks are run:

      1. VerifyJar  - ASM walk of every class in the jar: no dangling type references, and no
                      SRG/MCP name leaking into bytecode the retail client would load.
      2. Probe164   - resolves every Minecraft/Forge class, field, method and constructor the port
                      uses, through reflection against the real 1.6.4 + Forge jars.
      3. api916     - repeats the same probe against Forge 9.11.1.916, the build the target modpack
                      actually ships (download it first, see below).

    The Mojang-signed client and the Forge jar share the default package, so the JVM refuses to
    load them together: signature-stripped copies are built in build\lib-unsigned first, which is
    what Forge's own runtime class loader does.
#>

$ErrorActionPreference = 'Stop'

$root   = Split-Path -Parent $MyInvocation.MyCommand.Path
$libDir = Join-Path $root 'lib'
$unsig  = Join-Path $root 'build\lib-unsigned'
$tools  = Join-Path $root 'tools'
$outDir = Join-Path $root 'build\verify'
$jar    = Join-Path $root 'build\StatusEffectHUD-1.6.4-v1.27.1.jar'

$javac = Join-Path $env:JAVA_HOME 'bin\javac.exe'
if (-not (Test-Path $javac)) { $javac = 'javac' }
$java = Join-Path $env:JAVA_HOME 'bin\java.exe'
if (-not (Test-Path $java)) { $java = 'java' }

if (-not (Test-Path $jar)) { throw "run build.ps1 first - $jar not found" }

New-Item -ItemType Directory -Force -Path $unsig, $outDir | Out-Null

# --- 1. signature-stripped copies of the game jars ---------------------------
$strip = @'
import zipfile, os, re, sys
src_dir, dst_dir = sys.argv[1], sys.argv[2]
sig = re.compile(r'^META-INF/([^/]+\.(SF|RSA|DSA|EC))$', re.I)
for f in os.listdir(src_dir):
    if not f.endswith('.jar'):
        continue
    zin = zipfile.ZipFile(os.path.join(src_dir, f))
    zout = zipfile.ZipFile(os.path.join(dst_dir, f), 'w', zipfile.ZIP_DEFLATED)
    for item in zin.infolist():
        if sig.match(item.filename) or item.filename.upper() == 'META-INF/MANIFEST.MF':
            continue
        zout.writestr(item, zin.read(item.filename))
    zout.writestr('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\r\n\r\n')
    zin.close(); zout.close()
print('unsigned jars ready in ' + dst_dir)
'@
$stripFile = Join-Path $outDir 'strip_signatures.py'
$strip | Set-Content $stripFile -Encoding ASCII
& py $stripFile $libDir $unsig

# --- 2. compile the verifiers ------------------------------------------------
$asmCp = "$tools\asm.jar;$tools\asm-tree.jar;$tools\asm-commons.jar"

$prev = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
& $javac -nowarn -cp $asmCp -d $outDir "$tools\VerifyJar.java" 2>&1 | Out-Null
$c1 = $LASTEXITCODE
& $javac -nowarn -d $outDir "$tools\Probe164.java" 2>&1 | Out-Null
$c2 = $LASTEXITCODE
$ErrorActionPreference = $prev
if ($c1 -ne 0 -or $c2 -ne 0) { throw "failed to compile the verification tools ($c1 / $c2)" }

$unsigCp = (Get-ChildItem $unsig -Filter *.jar | ForEach-Object { $_.FullName }) -join ';'

Write-Host ''
Write-Host '=== check 1: bytecode references (no dangling types, no SRG leaks) ==='
& $java -cp "$outDir;$asmCp" VerifyJar $jar $libDir "$tools\deobf-1.6.4.txt"
$r1 = $LASTEXITCODE

Write-Host ''
Write-Host '=== check 2: API members resolve against the 1.6.4 + Forge jars ==='
& $java -cp "$outDir;$unsigCp" Probe164
$r2 = $LASTEXITCODE

# --- 2b. the container centring behaviour ------------------------------------
# Extracts the mod classes so the fake-hierarchy test can call the real methods.
Write-Host ''
Write-Host '=== check 2b: container centring (normal + creative widths, button follow) ==='
$modClasses = Join-Path $root 'build\classes'
if (-not (Test-Path $modClasses)) { New-Item -ItemType Directory -Force -Path $modClasses | Out-Null }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead($jar)
foreach ($entry in $zip.Entries)
{
    if ($entry.Name -eq '' -or -not $entry.Name.EndsWith('.class')) { continue }
    $target = Join-Path $modClasses $entry.FullName
    New-Item -ItemType Directory -Force -Path (Split-Path $target) | Out-Null
    [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $target, $true)
}
$zip.Dispose()

$prevEap = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
& $javac -nowarn -cp "$modClasses;$unsigCp" -d $outDir "$tools\InvShiftFake.java" 2>&1 | Out-Null
& $java -cp "$outDir;$modClasses;$unsigCp" InvShiftFake
$r2b = $LASTEXITCODE
$ErrorActionPreference = $prevEap

# --- 2c. F1 (hide GUI) must gate the HUD -------------------------------------
# Both HUD mods draw from the render tick, so vanilla's own hideGUI guard does not cover them;
# without an explicit check they flicker while F1 is held (no 2D projection is set up).
Write-Host ''
Write-Host '=== check 2c: F1 hide-GUI gates the HUD ==='
$prevEap2 = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
& $javac -nowarn -cp $asmCp -d $outDir "$tools\HideGuiCheck.java" 2>&1 | Out-Null
& $java -cp "$outDir;$asmCp" HideGuiCheck $jar StatusEffectHUD onRenderTick
$r2c = $LASTEXITCODE
$ErrorActionPreference = $prevEap2

# --- 3. optional: repeat against Forge 9.11.1.916 ---------------------------
$forge916 = Join-Path $root 'build\forge916'
if (Test-Path $forge916)
{
    $cp916 = (Get-ChildItem $forge916 -Filter *.jar | ForEach-Object { $_.FullName }) -join ';'
    Write-Host ''
    Write-Host '=== check 3: same probe against Forge 9.11.1.916 ==='
    & $java -cp "$outDir;$cp916" Probe164
    $r3 = $LASTEXITCODE
}
else
{
    Write-Host ''
    Write-Host '=== check 3 skipped: build\forge916 not prepared ==='
    $r3 = 0
}

Write-Host ''
if ($r1 -eq 0 -and $r2 -eq 0 -and $r2b -eq 0 -and $r2c -eq 0 -and $r3 -eq 0) {
    Write-Host 'ALL CHECKS PASSED'
    exit 0
}
Write-Host "FAILED (VerifyJar=$r1 Probe164=$r2 Centre=$r2b F1=$r2c Probe916=$r3)"
exit 1
