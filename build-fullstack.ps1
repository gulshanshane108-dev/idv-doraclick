# Merged full-stack build: React UI -> Spring static -> single JAR (port 8080).
$ErrorActionPreference = "Stop"

$root = $PSScriptRoot

Write-Host "== 1/2 Building frontend =="
Set-Location (Join-Path $root "frontend")
npm install
npm run build

Write-Host "== 2/2 Building backend JAR =="
Set-Location $root
mvn package -DskipTests

Write-Host ""
Write-Host "Done. Run with:"
Write-Host "  java -jar target\backend-0.0.1-SNAPSHOT.jar"
Write-Host "Then open http://localhost:8080"
