$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot
Push-Location $raiz

try {
    # Permite definir credenciales propias sin exigir un archivo .env.
    if (Test-Path ".env") {
        Get-Content ".env" | ForEach-Object {
            if ($_ -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
                [Environment]::SetEnvironmentVariable($matches[1], $matches[2], "Process")
            }
        }
    }

    if (-not $env:DB_USERNAME) {
        $env:DB_USERNAME = "gestor_franquicias"
    }

    if (-not $env:DB_PASSWORD) {
        $env:DB_PASSWORD = "franquicias_demo_local_2026"
    }

    $env:DB_URL = "r2dbc:postgresql://localhost:5434/gestor_franquicias"
    $env:DB_MIGRATION_URL = "jdbc:postgresql://localhost:5434/gestor_franquicias"

    docker compose stop api
    if ($LASTEXITCODE -ne 0) { throw "No se pudo detener la API de Docker Compose." }

    docker compose up -d base-datos
    if ($LASTEXITCODE -ne 0) { throw "No se pudo iniciar PostgreSQL." }

    mvn spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw "Falló la ejecución de Spring Boot con Maven." }
}
finally {
    Pop-Location
}
