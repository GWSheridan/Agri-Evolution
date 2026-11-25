param(
    [Parameter(Mandatory)][ValidateSet('forge', 'fabric')][string]$Loader,
    [Parameter(Mandatory)][string]$Java
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$versions = ConvertFrom-StringData (Get-Content (Join-Path $root 'gradle.properties') -Raw)
$minecraft = $versions.minecraft_version
$distribution = Join-Path $root "$Loader/build/libs/$($versions.mod_id)-$minecraft-$Loader-$($versions.mod_version).jar"
if (!(Test-Path -LiteralPath $distribution -PathType Leaf)) { throw "Build the $Loader distribution first" }

$javaPath = (Resolve-Path -LiteralPath $Java).Path
$run = Join-Path $root "build/server-checks/$Loader-$([Guid]::NewGuid().ToString('N'))"
$mods = New-Item -ItemType Directory -Path (Join-Path $run 'mods') -Force
Copy-Item -LiteralPath $distribution -Destination $mods.FullName

function New-LoopbackPort {
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
    $listener.Start()
    try { return $listener.LocalEndpoint.Port } finally { $listener.Stop() }
}

$serverPort = New-LoopbackPort
do { $consolePort = New-LoopbackPort } while ($consolePort -eq $serverPort)
$env:AGRI_TEST_RCON = [Guid]::NewGuid().ToString('N')
Set-Content -LiteralPath (Join-Path $run 'eula.txt') -Value 'eula=true'
@(
    'server-ip=127.0.0.1', "server-port=$serverPort", 'online-mode=false',
    'enable-rcon=true', "rcon.port=$consolePort", "rcon.password=$env:AGRI_TEST_RCON",
    'view-distance=3', 'simulation-distance=3', 'spawn-protection=0',
    'level-type=minecraft:flat',
    'generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}'
) | Set-Content -LiteralPath (Join-Path $run 'server.properties')

Push-Location -LiteralPath $run
try {
    if ($Loader -eq 'forge') {
        $forge = "$minecraft-$($versions.forge_version)"
        Invoke-WebRequest "https://maven.minecraftforge.net/net/minecraftforge/forge/$forge/forge-$forge-installer.jar" -OutFile installer.jar
        & $javaPath -jar installer.jar --installServer *> install.log
        if ($LASTEXITCODE -ne 0) {
            Get-Content install.log -Tail 40
            throw 'Forge server installation failed'
        }
        $argumentFile = if ($IsWindows) { 'win_args.txt' } else { 'unix_args.txt' }
        $serverArguments = @("@libraries/net/minecraftforge/forge/$forge/$argumentFile", 'nogui')
    } else {
        $api = $versions.fabric_api_version
        $installer = $versions.fabric_installer_version
        $fabric = $versions.fabric_loader_version
        Invoke-WebRequest "https://meta.fabricmc.net/v2/versions/loader/$minecraft/$fabric/$installer/server/jar" -OutFile server.jar
        Invoke-WebRequest "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$api/fabric-api-$api.jar" -OutFile mods/fabric-api.jar
        $serverArguments = @('-jar', 'server.jar', 'nogui')
    }

    foreach ($phase in @('seed', 'reload')) {
        $log = Join-Path $run "$phase.log"
        $launch = @{
            FilePath = $javaPath
            ArgumentList = @('-Xmx2G') + $serverArguments
            WorkingDirectory = $run
            RedirectStandardOutput = $log
            RedirectStandardError = (Join-Path $run "$phase-error.log")
            PassThru = $true
        }
        if ($IsWindows) { $launch.WindowStyle = 'Hidden' }
        $server = Start-Process @launch
        try {
            $ready = $false
            $timeout = [DateTime]::UtcNow.AddMinutes(4)
            while (!$server.HasExited -and [DateTime]::UtcNow -lt $timeout) {
                if (Select-String -LiteralPath $log -SimpleMatch "RCON running on 127.0.0.1:${consolePort}" -Quiet) {
                    $ready = $true
                    break
                }
                Start-Sleep -Seconds 1
            }
            if (!$ready) { throw "$Loader did not start" }
            & node (Join-Path $PSScriptRoot 'verify_loader_server.cjs') $consolePort $phase
            if ($LASTEXITCODE -ne 0) { throw "$Loader $phase check failed" }
            if (!$server.WaitForExit(45000) -or $server.ExitCode -ne 0) { throw "$Loader did not shut down cleanly" }
        } catch {
            Get-Content -LiteralPath $log -Tail 40
            Get-Content -LiteralPath $launch.RedirectStandardError -Tail 20
            throw
        } finally {
            if (!$server.HasExited) { Stop-Process -InputObject $server }
        }
    }
} finally {
    Pop-Location
    Remove-Item Env:AGRI_TEST_RCON -ErrorAction SilentlyContinue
}
