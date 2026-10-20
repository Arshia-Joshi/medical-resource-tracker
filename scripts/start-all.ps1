# Compiles the Java code and starts 3 nodes, the gateway and the React UI,
# each in its own window. Run from the project root: scripts\start-all.ps1
$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "backend"

javac -d "$backend\out" "$backend\src\mrt\*.java"
if ($LASTEXITCODE -ne 0) { Write-Host "Compile failed"; exit 1 }

foreach ($id in 1, 2, 3) {
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$host.UI.RawUI.WindowTitle='Node $id'; java -cp '$backend\out' mrt.ServerMain $id"
    Start-Sleep -Seconds 2
}

Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$host.UI.RawUI.WindowTitle='Gateway'; java -cp '$backend\out' mrt.Gateway"

$frontend = Join-Path $root "frontend"
if (-not (Test-Path "$frontend\node_modules")) { npm install --prefix $frontend }
Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$host.UI.RawUI.WindowTitle='React UI'; cd '$frontend'; npm run dev"

Write-Host "Open http://localhost:5173"
