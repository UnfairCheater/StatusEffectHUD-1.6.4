<#
    fetch-deps.ps1 - downloads the build and verification dependencies.

    Everything lands in lib\ (compile classpath) and tools\ (verification tooling).
    Run this once after cloning; build.ps1 and verify.ps1 then work offline.
#>

$ErrorActionPreference = 'Stop'

$root   = Split-Path -Parent $MyInvocation.MyCommand.Path
$libDir = Join-Path $root 'lib'
$toolDir = Join-Path $root 'tools'
New-Item -ItemType Directory -Force -Path $libDir, $toolDir | Out-Null

function Get-File($url, $target) {
    if (Test-Path $target) { Write-Host "have  $(Split-Path -Leaf $target)"; return }
    Write-Host "fetch $url"
    Invoke-WebRequest $url -OutFile $target -UseBasicParsing -TimeoutSec 600
    Write-Host "  -> $((Get-Item $target).Length) bytes"
}

# --- compile classpath ------------------------------------------------------
# Minecraft 1.6.4 client (vanilla) - provides the obfuscated game classes the mod compiles against
Get-File 'https://launcher.mojang.com/v1/objects/1703704407101cf72bd88e68579e3696ce733ecd/client.jar' `
         (Join-Path $libDir 'minecraft-1.6.4.jar')

# Forge 1.6.4 universal jar (the version declared in the original mcmod.info is 9.11.1.932;
# 9.11.1.1345 is the last 1.6.4 build and is API compatible)
Get-File 'https://maven.minecraftforge.net/net/minecraftforge/forge/1.6.4-9.11.1.1345/forge-1.6.4-9.11.1.1345-universal.jar' `
         (Join-Path $libDir 'forge-1.6.4-universal.jar')

# Minecraft 1.6.4 libraries
$deps = @{
    'lwjgl.jar'         = 'https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl/2.9.0/lwjgl-2.9.0.jar'
    'lwjgl_util.jar'    = 'https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl_util/2.9.0/lwjgl_util-2.9.0.jar'
    'guava.jar'         = 'https://libraries.minecraft.net/com/google/guava/guava/14.0/guava-14.0.jar'
    'commons-lang3.jar' = 'https://libraries.minecraft.net/org/apache/commons/commons-lang3/3.1/commons-lang3-3.1.jar'
    'commons-io.jar'    = 'https://libraries.minecraft.net/commons-io/commons-io/2.4/commons-io-2.4.jar'
    'gson.jar'          = 'https://libraries.minecraft.net/com/google/code/gson/gson/2.2.2/gson-2.2.2.jar'
    'argo.jar'          = 'https://libraries.minecraft.net/argo/argo/2.25_fixed/argo-2.25_fixed.jar'
    'bcprov.jar'        = 'https://libraries.minecraft.net/org/bouncycastle/bcprov-jdk15on/1.47/bcprov-jdk15on-1.47.jar'
}
foreach ($name in $deps.Keys) {
    Get-File $deps[$name] (Join-Path $libDir $name)
}

# --- verification tooling ---------------------------------------------------
# ASM 9.7, used by tools/VerifyJar.java (bytecode walk) and tools/HideGuiCheck.java
$asm = @{
    'asm.jar'         = 'https://repo1.maven.org/maven2/org/ow2/asm/asm/9.7/asm-9.7.jar'
    'asm-tree.jar'    = 'https://repo1.maven.org/maven2/org/ow2/asm/asm-tree/9.7/asm-tree-9.7.jar'
    'asm-commons.jar' = 'https://repo1.maven.org/maven2/org/ow2/asm/asm-commons/9.7/asm-commons-9.7.jar'
}
foreach ($name in $asm.Keys) {
    Get-File $asm[$name] (Join-Path $toolDir $name)
}

Write-Host ''
Write-Host 'dependencies ready - now run build.ps1 (and verify.ps1)'
