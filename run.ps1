# Carga las variables de .env y arranca la app contra Supabase.
# Uso:  .\run.ps1
$envFile = Join-Path $PSScriptRoot ".env"

if (-not (Test-Path $envFile)) {
    Write-Host "No se encontro el archivo .env" -ForegroundColor Red
    Write-Host "Copia .env.example a .env y completa la contraseña." -ForegroundColor Yellow
    exit 1
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#")) {
        $idx = $line.IndexOf("=")
        if ($idx -gt 0) {
            $name  = $line.Substring(0, $idx).Trim()
            $value = $line.Substring($idx + 1).Trim()
            Set-Item -Path "Env:$name" -Value $value
        }
    }
}

Write-Host "Conectando a Supabase y arrancando la app..." -ForegroundColor Green
& "$PSScriptRoot\mvnw.cmd" spring-boot:run
