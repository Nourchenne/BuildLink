# BuildLink - Script de demarrage PowerShell
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  BuildLink - Demarrage de l'application" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verifier Java
Write-Host "[INFO] Verification de Java..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "[OK] $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "[ERREUR] Java n'est pas installe ou pas dans le PATH" -ForegroundColor Red
    Write-Host "Installez Java 17 ou superieur : https://adoptium.net/" -ForegroundColor Yellow
    Read-Host "Appuyez sur Entree pour quitter"
    exit 1
}
Write-Host ""

# Verifier Maven
Write-Host "[INFO] Verification de Maven..." -ForegroundColor Yellow
$mavenCmd = "mvn"
try {
    Get-Command mvn -ErrorAction Stop | Out-Null
    Write-Host "[OK] Maven detecte" -ForegroundColor Green
} catch {
    Write-Host "[INFO] Maven non trouve, utilisation du wrapper (mvnw)" -ForegroundColor Yellow
    $mavenCmd = ".\mvnw.cmd"
}
Write-Host ""

# Information MySQL
Write-Host "[INFO] Verification de MySQL..." -ForegroundColor Yellow
Write-Host "Assurez-vous que :" -ForegroundColor Cyan
Write-Host "  - MySQL est installe et demarre" -ForegroundColor Cyan
Write-Host "  - Le port 3306 est accessible" -ForegroundColor Cyan
Write-Host "  - L'utilisateur 'root' avec le mot de passe 'root' existe" -ForegroundColor Cyan
Write-Host ""

# Compilation
Write-Host "[INFO] Compilation du projet..." -ForegroundColor Yellow
& $mavenCmd clean compile
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERREUR] La compilation a echoue" -ForegroundColor Red
    Read-Host "Appuyez sur Entree pour quitter"
    exit 1
}
Write-Host ""

# Demarrage
Write-Host "[INFO] Demarrage de l'application BuildLink..." -ForegroundColor Yellow
Write-Host ""
Write-Host "L'application sera accessible a :" -ForegroundColor Green
Write-Host "  http://localhost:8080" -ForegroundColor Cyan -NoNewline
Write-Host " (Page d'accueil)" -ForegroundColor Gray
Write-Host "  http://localhost:8080/auth/login" -ForegroundColor Cyan -NoNewline
Write-Host " (Connexion)" -ForegroundColor Gray
Write-Host "  http://localhost:8080/auth/register" -ForegroundColor Cyan -NoNewline
Write-Host " (Inscription)" -ForegroundColor Gray
Write-Host ""
Write-Host "Appuyez sur Ctrl+C pour arreter l'application" -ForegroundColor Yellow
Write-Host ""

& $mavenCmd spring-boot:run

