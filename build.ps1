<#
    build.ps1 - builds StatusEffectHUD for Minecraft 1.6.4 from source.

    Requirements (all editable below):
      * a JDK 7 or 8        - the mod must be compiled for Java 6 bytecode (class file 50)
      * the Minecraft 1.6.4 client jar
      * the Forge 1.6.4 universal jar (9.11.1.932 or newer, e.g. 9.11.1.1345)
      * LWJGL 2.9.x, Guava 14, commons-lang3, commons-io, gson, argo, bcprov

    Everything is expected in the lib\ folder next to this script. Download the two game
    jars with fetch-deps.ps1 (run it once), the rest come from libraries.minecraft.net.

    Usage:  powershell -ExecutionPolicy Bypass -File build.ps1
#>

$ErrorActionPreference = 'Stop'

$root    = Split-Path -Parent $MyInvocation.MyCommand.Path
$libDir  = Join-Path $root 'lib'
$srcDir  = Join-Path $root 'src'
$buildDir = Join-Path $root 'build'
$classDir = Join-Path $buildDir 'classes'
$stageDir = Join-Path $buildDir 'jar'

$javac = Join-Path $env:JAVA_HOME 'bin\javac.exe'
if (-not (Test-Path $javac)) { $javac = 'javac' }
$jar = Join-Path $env:JAVA_HOME 'bin\jar.exe'
if (-not (Test-Path $jar)) { $jar = 'jar' }

Write-Host "javac : $javac"
Write-Host "jar   : $jar"

# --- clean -------------------------------------------------------------------
foreach ($d in @($classDir, $stageDir)) {
    if (Test-Path $d) { Remove-Item $d -Recurse -Force }
}
New-Item -ItemType Directory -Force -Path $classDir, $stageDir | Out-Null

# --- classpath ---------------------------------------------------------------
$libs = Get-ChildItem $libDir -Filter *.jar -ErrorAction SilentlyContinue
if (-not $libs) { throw "no jars found in $libDir - run fetch-deps.ps1 first" }
$cp = ($libs | ForEach-Object { $_.FullName }) -join ';'
Write-Host "classpath jars: $($libs.Count)"

# --- compile -----------------------------------------------------------------
# The source list goes through an @argfile so paths containing spaces stay safe, and stderr is
# redirected to Out-Null because javac's "unchecked operations" notes are not errors. Failure is
# detected through $LASTEXITCODE / the produced classes instead.
$sourceList = Get-ChildItem $srcDir -Filter *.java -Recurse | ForEach-Object { $_.FullName }
Write-Host "compiling $($sourceList.Count) source files ..."

$argFile = Join-Path $buildDir 'sources.txt'
$sourceList | Set-Content $argFile -Encoding ASCII

$prev = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
& $javac -source 1.6 -target 1.6 -nowarn -encoding UTF-8 -cp $cp -d $classDir ('@' + $argFile) 2>&1 | Out-Null
$javacExit = $LASTEXITCODE
$ErrorActionPreference = $prev

if ($javacExit -ne 0 -or -not (Test-Path (Join-Path $classDir 'StatusEffectHUDMod.class')))
{
    throw "javac failed (exit $javacExit) - re-run javac by hand to see the errors"
}
Write-Host "compiled $((Get-ChildItem $classDir -Filter *.class).Count) class files"

# --- stage -------------------------------------------------------------------
# -Recurse is required: the Log helper lives in a package directory
Copy-Item (Join-Path $classDir '*') $stageDir -Recurse -Force
Copy-Item (Join-Path $root 'mcmod.info') $stageDir -Force
Copy-Item (Join-Path $root 'assets') $stageDir -Recurse -Force
New-Item -ItemType Directory -Force -Path (Join-Path $stageDir 'META-INF') | Out-Null
Set-Content (Join-Path $stageDir 'META-INF\MANIFEST.MF') `
    "Manifest-Version: 1.0`r`nCreated-By: StatusEffectHUD 1.6.4`r`n" -Encoding ASCII

# --- package -----------------------------------------------------------------
# note: short-form jar flags, the JDK 7/8 jar tool has no long options
$outJar = Join-Path $buildDir 'StatusEffectHUD-1.6.4-v1.27.1.jar'
if (Test-Path $outJar) { Remove-Item $outJar -Force }

$ErrorActionPreference = 'Continue'
& $jar cf $outJar -C $stageDir . 2>&1 | Out-Null
$jarExit = $LASTEXITCODE
$ErrorActionPreference = $prev

if ($jarExit -ne 0 -or -not (Test-Path $outJar))
{
    throw "jar failed (exit $jarExit)"
}

Write-Host ""
Write-Host "built: $outJar"
Write-Host "size : $((Get-Item $outJar).Length) bytes"
Write-Host ""
Write-Host "Drop the jar into .minecraft\mods together with Forge 1.6.4."
