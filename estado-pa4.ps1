#requires -Version 5.1
<#
.SYNOPSIS
Comprueba los cinco puertos de PA4 y las respuestas HTTP de las aplicaciones.
No inicia ni detiene procesos. Un puerto de BD abierto no verifica credenciales.
#>
[CmdletBinding()]
param(
    [ValidateRange(1,65535)][int]$MySqlPort = 3307,
    [ValidateRange(1,65535)][int]$SqlServerPort = 1433
)

function Test-Pa4Port {
    param([int]$Port, [string]$Server = 'localhost')
    $client = New-Object Net.Sockets.TcpClient
    try {
        $pending = $client.ConnectAsync($Server, $Port)
        if (-not $pending.Wait(1500)) { return $false }
        return $client.Connected
    } catch { return $false } finally { $client.Dispose() }
}

function Get-Pa4Http {
    param([ValidateSet('Principal','WildFly','Reportes')][string]$Service)
    $urls = @{
        Principal = 'http://localhost:8081/sistema-pedidos/login'
        WildFly = 'http://localhost:8082/inventario-service/api/inventario/productos'
        Reportes = 'http://localhost:8083/pedidos-reportes-service/api/v1/pedidos/estadisticas/resumen'
    }
    try {
        # Sin redirecciones ni credenciales; las consultas de API son GET de lectura.
        $response = Invoke-WebRequest -Uri $urls[$Service] -UseBasicParsing -TimeoutSec 4 -MaximumRedirection 0 -ErrorAction Stop
        $valid = $false
        if ($response.StatusCode -eq 200) {
            $body = [string]$response.Content
            if ($Service -eq 'Principal') {
                $valid = $body.Contains('name="username"') -and
                         $body.Contains('name="password"') -and
                         $body.Contains('/sistema-pedidos/login')
            } elseif ([string]$response.Headers['Content-Type'] -match 'json') {
                $data = ConvertFrom-Json -InputObject $body -ErrorAction Stop
                if ($Service -eq 'WildFly') {
                    $valid = $body.TrimStart().StartsWith('[')
                } else {
                    $valid = $null -ne $data -and
                        $null -ne $data.PSObject.Properties['totalPedidos'] -and
                        $null -ne $data.PSObject.Properties['totalUnidades']
                }
            }
        }
        return [pscustomobject]@{ Ready = $valid; Detail = $(if ($valid) {'HTTP 200, respuesta esperada'} else {'HTTP recibido, contenido inesperado'}) }
    } catch {
        # No imprimir cuerpos ni excepciones que pudieran contener datos.
        return [pscustomobject]@{ Ready = $false; Detail = 'Sin respuesta HTTP valida (error, espera agotada o redireccion)' }
    }
}

if ($MyInvocation.InvocationName -ne '.') {
    $rows = @(
        [pscustomobject]@{ Servicio='MySQL'; Puerto=$MySqlPort; Estado=$(if (Test-Pa4Port $MySqlPort) {'TCP ACTIVO; autenticacion no comprobada'} else {'INACTIVO'}) }
        [pscustomobject]@{ Servicio='SQL Server'; Puerto=$SqlServerPort; Estado=$(if (Test-Pa4Port $SqlServerPort) {'TCP ACTIVO; autenticacion no comprobada'} else {'INACTIVO'}) }
    )
    foreach ($item in @(@('Principal',8081), @('WildFly',8082), @('Reportes',8083))) {
        $open = Test-Pa4Port $item[1]
        $detail = 'INACTIVO'
        if ($open) {
            $http = Get-Pa4Http $item[0]
            $detail = if ($http.Ready) {'LISTO: ' + $http.Detail} else {'PUERTO OCUPADO: ' + $http.Detail}
        }
        $rows += [pscustomobject]@{ Servicio=$item[0]; Puerto=$item[1]; Estado=$detail }
    }
    $rows | Format-Table -AutoSize -Wrap
}
