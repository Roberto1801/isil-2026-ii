#requires -Version 5.1
<#
.SYNOPSIS
Inicia PA4 en procesos independientes. No inicia bases ni ejecuta migraciones.
.DESCRIPTION
Requiere Java 21, Maven, MySQL/SQL Server activos y WildFly ya provisionado
con inventario-service. Conserva logs y PID (sin secretos) en .pa4-runtime.
Las claves se solicitan con SecureString y solo se pasan en el entorno del
proceso hijo. Como cualquier variable de entorno, son accesibles al propio
proceso y a administradores locales. No se escriben en argumentos ni archivos.
No use Start-Transcript ni Maven -X para registrar una sesion con secretos.
.EXAMPLE
.\iniciar-pa4.ps1
.EXAMPLE
.\iniciar-pa4.ps1 -MavenPath 'C:\Herramientas\apache-maven-3.10.0\bin\mvn.cmd' -TimeoutSeconds 240
#>
[CmdletBinding()]
param(
    [ValidateSet('WildFly','Principal','Reportes')][string[]]$Servicios = @('WildFly','Reportes','Principal'),
    [string]$MavenPath,
    [string]$WildFlyHome,
    [ValidateRange(1,65535)][int]$MySqlPort = 3307,
    [ValidatePattern('^[a-zA-Z0-9_]+$')][string]$MySqlDatabase = 'pedidosdb',
    [string]$AppDbUser = 'spedidos_app',
    [string]$ReportDbUser = 'spedidos_reportes',
    [ValidateRange(30,900)][int]$TimeoutSeconds = 180
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'estado-pa4.ps1') -MySqlPort $MySqlPort
$root = $PSScriptRoot
$course = Join-Path $root 'ADVANCED-BUSINESS-APPLICATION-DEVELOPMENT'
$session = 'Sesi' + [char]0xF3 + 'n '
$appPath = Join-Path $course ($session + '5/sistema-pedidos-springboot')
$reportPath = Join-Path $course ($session + '12/pedidos-reportes-service')
$inventoryPath = Join-Path $course ($session + '4/inventario-service')
$runtime = Join-Path $root '.pa4-runtime'
if (-not $WildFlyHome) { $WildFlyHome = Join-Path $inventoryPath 'target/server' }
$appPath = [IO.Path]::GetFullPath($appPath)
$reportPath = [IO.Path]::GetFullPath($reportPath)
$WildFlyHome = [IO.Path]::GetFullPath($WildFlyHome)

function ConvertTo-Pa4Literal([string]$Value) { return "'" + $Value.Replace("'", "''") + "'" }

function Read-Pa4Secret([string]$Label) {
    while ($true) {
        $secret = Read-Host $Label -AsSecureString
        if ($secret.Length -gt 0) { return $secret }
        $secret.Dispose()
        Write-Warning 'La clave no puede estar vacia.'
    }
}

function Get-Pa4Property([string]$Path, [string]$Key) {
    # Solo se extrae la clave solicitada; nunca se imprime el archivo.
    $line = Get-Content -LiteralPath $Path -Encoding UTF8 |
        Where-Object { $_ -match ('^\s*' + [regex]::Escape($Key) + '\s*=') } |
        Select-Object -Last 1
    if (-not $line) { throw "Falta la propiedad requerida: $Key" }
    return ($line -split '=', 2)[1].Trim()
}

function Test-Pa4TrackedProcess([string]$Name) {
    $path = Join-Path $runtime ($Name + '.json')
    if (-not (Test-Path -LiteralPath $path)) { return $false }
    try {
        $record = Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
        $process = Get-Process -Id $record.ProcessId -ErrorAction Stop
        return $process.StartTime.ToUniversalTime().ToString('o') -eq $record.StartUtc
    } catch { return $false }
}

function Test-Pa4JavaProcess([string]$Directory) {
    $processes = @(Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" -ErrorAction Stop)
    return @($processes | Where-Object {
        $_.CommandLine -and $_.CommandLine.Replace('/', '\').IndexOf(
            $Directory.Replace('/', '\'), [StringComparison]::OrdinalIgnoreCase) -ge 0
    }).Count -gt 0
}

function Start-Pa4Process {
    param([string]$Name, [string]$Directory, [string]$Executable,
          [string[]]$Arguments, [hashtable]$Environment)
    $logBase = Join-Path $runtime ($Name + '-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
    # La orden codificada solo contiene rutas y opciones publicas, nunca claves.
    $command = "& " + (ConvertTo-Pa4Literal $Executable) + " " +
        (($Arguments | ForEach-Object { ConvertTo-Pa4Literal $_ }) -join ' ') +
        '; exit $LASTEXITCODE'
    $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($command))
    $saved = @{}
    try {
        foreach ($key in $Environment.Keys) {
            $saved[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
            $value = $Environment[$key]
            if ($value -is [Security.SecureString]) {
                $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($value)
                try { $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
                finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
                [Environment]::SetEnvironmentVariable($key, $plain, 'Process')
                $plain = $null
            } else {
                [Environment]::SetEnvironmentVariable($key, $value, 'Process')
            }
        }
        $process = Start-Process -FilePath "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe" -ArgumentList @(
            '-NoProfile','-NonInteractive','-EncodedCommand',$encoded
        ) -WorkingDirectory $Directory -WindowStyle Hidden -PassThru -RedirectStandardOutput ($logBase + '.out.log') -RedirectStandardError ($logBase + '.err.log')
        [pscustomobject]@{ ProcessId=$process.Id; StartUtc=$process.StartTime.ToUniversalTime().ToString('o'); Log=$logBase } |
            ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runtime ($Name + '.json')) -Encoding UTF8
        Write-Host "$Name iniciado (PID $($process.Id)). Logs: $logBase.*.log"
        return $process
    } finally {
        foreach ($key in $saved.Keys) {
            [Environment]::SetEnvironmentVariable($key, $saved[$key], 'Process')
        }
    }
}

function Wait-Pa4Ready([string]$Name, [Diagnostics.Process]$Process) {
    $watch = [Diagnostics.Stopwatch]::StartNew()
    $nextNotice = 15
    while ($watch.Elapsed.TotalSeconds -lt $TimeoutSeconds) {
        if ($Process.HasExited) { return 'FALLO: proceso finalizado; revisar logs' }
        $http = Get-Pa4Http $Name
        if ($http.Ready) { return 'LISTO: respuesta HTTP comprobada' }
        if ($watch.Elapsed.TotalSeconds -ge $nextNotice) {
            Write-Host "${Name}: esperando respuesta HTTP ($([int]$watch.Elapsed.TotalSeconds) s)..."
            $nextNotice += 15
        }
        Start-Sleep -Seconds 2
    }
    return 'TIEMPO AGOTADO: proceso conservado; revisar logs antes de reintentar'
}

function Assert-Pa4WildFly {
    $xmlPath = Join-Path $WildFlyHome 'standalone/configuration/standalone.xml'
    if (-not (Test-Path (Join-Path $WildFlyHome 'bin/standalone.bat'))) { throw 'Falta WildFly provisionado: indique -WildFlyHome o prepare el servidor previamente.' }
    [xml]$config = Get-Content -LiteralPath $xmlPath -Raw
    $ds = $config.SelectSingleNode('//*[local-name()="datasource" and @pool-name="MSSQLServerDS"]')
    if (-not $ds -or -not $ds.OuterXml.Contains('env.MSSQLSERVER_PASSWORD') -or
        -not $ds.OuterXml.Contains('env.MSSQLSERVER_URL') -or
        -not $ds.OuterXml.Contains('env.MSSQLSERVER_JNDI') -or
        -not $ds.OuterXml.Contains('env.MSSQLSERVER_USER')) {
        throw 'El datasource WildFly no admite las variables esperadas. Revisar su configuracion antes de iniciar.'
    }
    foreach ($property in $config.SelectNodes('//*[local-name()="system-properties"]/*')) {
        if ($property.name -like 'org.wildfly.datasources.mssqlserver.*') {
            throw 'Hay propiedades de datasource que pueden prevalecer sobre el entorno. Revisar WildFly antes de iniciar.'
        }
    }
    $deployment = $config.SelectSingleNode('//*[local-name()="deployment" and @name="inventario-service.war"]')
    if (-not $deployment -or $deployment.GetAttribute('enabled') -eq 'false') { throw 'Inventario no figura habilitado como despliegue administrado. No se redesplegara automaticamente.' }
    $contentNode = $deployment.SelectSingleNode('*[local-name()="content"]')
    $hash = $contentNode.GetAttribute('sha1')
    if ($hash -notmatch '^[a-fA-F0-9]{40}$') { throw 'Contenido del despliegue WildFly no reconocido.' }
    $content = Join-Path $WildFlyHome ('standalone/data/content/' + $hash.Substring(0,2) + '/' + $hash.Substring(2) + '/content')
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::OpenRead($content)
    try {
        $entry = $zip.GetEntry('WEB-INF/classes/META-INF/persistence.xml')
        if (-not $entry) { throw 'No se puede verificar persistence.xml del WAR desplegado.' }
        $reader = New-Object IO.StreamReader($entry.Open())
        try { [xml]$persistence = $reader.ReadToEnd() } finally { $reader.Dispose() }
        $action = $persistence.SelectSingleNode('//*[local-name()="property" and @name="jakarta.persistence.schema-generation.database.action"]')
        if (-not $action -or $action.GetAttribute('value') -ne 'none') { throw 'El WAR desplegado no desactiva la generacion de esquema. No se iniciara.' }
    } finally { $zip.Dispose() }
}

$mutex = $null
$locked = $false
$secrets = @()
$results = @()
$services = @(
    [pscustomobject]@{ Name='WildFly'; Port=8082; Directory=$WildFlyHome },
    [pscustomobject]@{ Name='Reportes'; Port=8083; Directory=$reportPath },
    [pscustomobject]@{ Name='Principal'; Port=8081; Directory=$appPath }
)
$services = @($services | Where-Object { $_.Name -in $Servicios })
try {
    # Evita dos iniciadores simultaneos en esta sesion de Windows.
    $mutex = New-Object Threading.Mutex($false, 'Local\ISIL_PA4_8081_8082_8083')
    try { $locked = $mutex.WaitOne(0) } catch [Threading.AbandonedMutexException] { $locked = $true }
    if (-not $locked) { throw 'Otro iniciador PA4 esta trabajando. No se iniciaran duplicados.' }

    foreach ($db in @(@('MySQL',$MySqlPort), @('SQL Server',1433))) {
        if (-not (Test-Pa4Port $db[1])) { throw "$($db[0]) no responde en el puerto $($db[1]). Inicielo manualmente." }
        Write-Host "$($db[0]): TCP activo en $($db[1]); autenticacion pendiente del servicio."
    }

    $pending = @()
    foreach ($service in $services) {
        if (Test-Pa4Port $service.Port) {
            $http = Get-Pa4Http $service.Name
            $state = if ($http.Ready) {'YA ACTIVO: respuesta HTTP comprobada'} else {'CONFLICTO: puerto ocupado sin respuesta esperada; no se inicia'}
            $results += [pscustomobject]@{ Servicio=$service.Name; Estado=$state }
        } elseif (Test-Pa4TrackedProcess $service.Name) {
            $results += [pscustomobject]@{ Servicio=$service.Name; Estado='PROCESO PREVIO ACTIVO: revisar logs; no se duplica' }
        } else {
            # Detectar tambien arranques manuales aun sin puerto abierto.
            if (Test-Pa4JavaProcess $service.Directory) {
                $results += [pscustomobject]@{ Servicio=$service.Name; Estado='PROCESO JAVA PREVIO: no se inicia otra instancia' }
            } else { $pending += $service }
        }
    }
    if ($pending.Count -eq 0) { return }

    $java = Get-Command java.exe -ErrorAction Stop
    $javaHome = Split-Path (Split-Path $java.Source -Parent) -Parent
    $release = Join-Path $javaHome 'release'
    if (-not (Test-Path $release) -or (Get-Content $release -Raw) -notmatch 'JAVA_VERSION="21[."]') { throw 'Se requiere Java 21 en PATH.' }
    if (@($pending | Where-Object Name -ne 'WildFly').Count -gt 0) {
        if (-not $MavenPath) {
            $maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
            if ($maven) { $MavenPath = $maven.Source }
            elseif (Test-Path 'C:\Herramientas\apache-maven-3.10.0\bin\mvn.cmd') { $MavenPath = 'C:\Herramientas\apache-maven-3.10.0\bin\mvn.cmd' }
            else {
                $cached = Get-ChildItem (Join-Path $env:USERPROFILE '.m2/wrapper/dists/apache-maven-3.9.16') -Filter mvn.cmd -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
                if ($cached) { $MavenPath = $cached.FullName }
            }
        }
        if (-not $MavenPath -or -not (Test-Path -LiteralPath $MavenPath)) { throw 'No se encontro Maven. Indique -MavenPath con la ruta a mvn.cmd.' }
        $MavenPath = (Resolve-Path -LiteralPath $MavenPath).Path
    }
    foreach ($service in $pending) {
        if (-not (Test-Path -LiteralPath $service.Directory)) { throw "Falta el directorio de $($service.Name)." }
        if ($service.Name -eq 'WildFly') { Assert-Pa4WildFly }
        elseif (-not (Test-Path (Join-Path $service.Directory 'pom.xml'))) { throw "Falta pom.xml de $($service.Name)." }
    }
    # No pasar el archivo local completo a WildFly.
    $dbConfig = Join-Path $inventoryPath 'config/database.properties'
    $sqlUrl = Get-Pa4Property $dbConfig 'org.wildfly.datasources.mssqlserver.connection-url'
    $sqlUser = Get-Pa4Property $dbConfig 'org.wildfly.datasources.mssqlserver.user-name'
    $sqlJndi = Get-Pa4Property $dbConfig 'org.wildfly.datasources.mssqlserver.jndi-name'
    if ($sqlUrl -notmatch '^jdbc:sqlserver://localhost:1433;' -or $sqlUrl -match '(?i)password\s*=') { throw 'La URL SQL Server no coincide con localhost:1433 o incluye una clave. Revisar antes de iniciar.' }
    if ($sqlJndi -ne 'java:jboss/datasources/InventarioDS') { throw 'El JNDI no coincide con InventarioPU.' }

    New-Item -ItemType Directory -Path $runtime -Force | Out-Null
    # Mantener los logs locales fuera de futuros commits.
    Set-Content -LiteralPath (Join-Path $runtime '.gitignore') -Value '*' -Encoding ASCII
    foreach ($service in $pending) {
        # Segunda comprobacion inmediatamente antes de arrancar.
        if (Test-Pa4Port $service.Port) {
            $results += [pscustomobject]@{ Servicio=$service.Name; Estado='PUERTO OCUPADO durante preparacion: no se inicia' }
            continue
        }
        if ($service.Name -eq 'WildFly' -and ((Test-Pa4Port 9992) -or (Test-Pa4Port 8445))) {
            $results += [pscustomobject]@{ Servicio='WildFly'; Estado='CONFLICTO en puerto auxiliar 9992/8445: no se inicia' }
            continue
        }
        $environment = @{
            JAVA_HOME=$javaHome; JAVA_OPTS=$null; JAVA_TOOL_OPTIONS=$null; JDK_JAVA_OPTIONS=$null
            MAVEN_OPTS=$null; MAVEN_ARGS=$null; SPRING_APPLICATION_JSON=$null
            SPRING_PROFILES_ACTIVE=$null; SPRING_CONFIG_LOCATION=$null; SPRING_CONFIG_ADDITIONAL_LOCATION=$null
            DB_URL=$null; DB_USERNAME=$null; DB_PASSWORD=$null; SUPERVISOR_PASSWORD=$null
            SPRING_DATASOURCE_URL=$null; SPRING_DATASOURCE_USERNAME=$null; SPRING_DATASOURCE_PASSWORD=$null
            MSSQLSERVER_PASSWORD=$null; MSSQLSERVER_URL=$null; MSSQLSERVER_USER=$null; MSSQLSERVER_JNDI=$null
        }
        if ($service.Name -eq 'WildFly') {
            $secret = Read-Pa4Secret 'Clave SQL Server para inventario'
            $secrets += $secret
            $environment.MSSQLSERVER_PASSWORD=$secret
            $environment.MSSQLSERVER_URL=$sqlUrl
            $environment.MSSQLSERVER_USER=$sqlUser
            $environment.MSSQLSERVER_JNDI=$sqlJndi
            $environment.JBOSS_HOME=$WildFlyHome
            $environment.NOPAUSE='true'
            $environment.JAVA=$java.Source
            $environment.STANDALONE_CONF=$null
            $environment.SERVER_OPTS=$null
            $environment.JBOSS_BASE_DIR=Join-Path $WildFlyHome 'standalone'
            $environment.JBOSS_CONFIG_DIR=Join-Path $WildFlyHome 'standalone/configuration'
            $executable = Join-Path $WildFlyHome 'bin/standalone.bat'
            $arguments = @('-c','standalone.xml','-Djboss.socket.binding.port-offset=2')
        } else {
            $secret = Read-Pa4Secret ("Clave MySQL de " + $service.Name)
            $secrets += $secret
            $environment.DB_URL = "jdbc:mysql://localhost:${MySqlPort}/${MySqlDatabase}?serverTimezone=America/Lima"
            $environment.DB_USERNAME = if ($service.Name -eq 'Principal') {$AppDbUser} else {$ReportDbUser}
            $environment.DB_PASSWORD = $secret
            $environment.SPRING_DATASOURCE_URL = $environment.DB_URL
            $environment.SPRING_DATASOURCE_USERNAME = $environment.DB_USERNAME
            $environment.SPRING_DATASOURCE_PASSWORD = $secret
            if ($service.Name -eq 'Principal') {
                $supervisor = Read-Pa4Secret 'Clave de acceso del usuario supervisor'
                $secrets += $supervisor
                $environment.SUPERVISOR_PASSWORD = $supervisor
                $operador = Read-Pa4Secret 'Clave de acceso del usuario operador'
                $secrets += $operador
                $environment.PEDIDOS_OPERADOR_PASSWORD = $operador
                $consulta = Read-Pa4Secret 'Clave de acceso del usuario consulta'
                $secrets += $consulta
                $environment.PEDIDOS_CONSULTA_PASSWORD = $consulta
            }
            $context = if ($service.Name -eq 'Principal') {'/sistema-pedidos'} else {'/pedidos-reportes-service'}
            $options = @(
                "--server.port=$($service.Port)", "--server.servlet.context-path=$context",
                '--spring.jpa.hibernate.ddl-auto=validate', '--spring.jpa.generate-ddl=false',
                '--spring.jpa.properties.hibernate.hbm2ddl.auto=validate',
                '--spring.jpa.properties.jakarta.persistence.schema-generation.database.action=none',
                '--spring.sql.init.mode=never', '--spring.flyway.enabled=false', '--spring.liquibase.enabled=false',
                '--debug=false', '--trace=false', '--spring.jpa.show-sql=false',
                '--logging.level.root=INFO', '--logging.level.org.springframework=INFO',
                '--logging.level.org.hibernate=INFO', '--logging.level.org.springframework.jdbc.core=INFO'
            )
            $executable = $MavenPath
            $arguments = @('-B', '-Dmaven.test.skip=true', 'spring-boot:run',
                ('-Dspring-boot.run.arguments=' + ($options -join ' ')))
        }
        try {
            # Las preguntas de claves pueden demorar: repetir controles al terminar.
            if ((Test-Pa4Port $service.Port) -or (Test-Pa4TrackedProcess $service.Name) -or
                (Test-Pa4JavaProcess $service.Directory)) {
                $results += [pscustomobject]@{ Servicio=$service.Name; Estado='ARRANQUE PREVIO DETECTADO: no se duplica' }
                continue
            }
            if ($service.Name -eq 'WildFly' -and ((Test-Pa4Port 9992) -or (Test-Pa4Port 8445))) {
                $results += [pscustomobject]@{ Servicio=$service.Name; Estado='CONFLICTO en puerto auxiliar: no se inicia' }
                continue
            }
            $process = Start-Pa4Process $service.Name $service.Directory $executable $arguments $environment
            $state = Wait-Pa4Ready $service.Name $process
            $results += [pscustomobject]@{ Servicio=$service.Name; Estado=$state }
        } catch {
            $results += [pscustomobject]@{ Servicio=$service.Name; Estado='FALLO al iniciar; comprobar rutas, permisos y logs' }
        }
    }
} catch {
    # Los errores de prevalidacion contienen solo diagnosticos publicos.
    Write-Warning ('Preparacion interrumpida: ' + $_.Exception.Message)
} finally {
    foreach ($secret in $secrets) { $secret.Dispose() }
    if ($locked) { $mutex.ReleaseMutex() }
    if ($mutex) { $mutex.Dispose() }
    foreach ($service in $services) {
        if (@($results | Where-Object Servicio -eq $service.Name).Count -eq 0) {
            $results += [pscustomobject]@{ Servicio=$service.Name; Estado='NO INICIADO: preparacion interrumpida' }
        }
    }
    if ($results.Count -gt 0) { $results | Format-Table -AutoSize -Wrap }
    Write-Host 'No se han terminado procesos. Estado actual: .\estado-pa4.ps1'
}
