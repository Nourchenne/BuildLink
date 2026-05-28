@echo off
echo ========================================
echo    BuildLink - Demarrage de l'application
echo ========================================
echo.

REM Verifier que Java est installe
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERREUR] Java n'est pas installe ou n'est pas dans le PATH
    echo Installez Java 17 ou superieur : https://adoptium.net/
    pause
    exit /b 1
)

echo [INFO] Java detecte
java -version
echo.

REM Verifier que Maven est disponible
where mvn >nul 2>&1
if errorlevel 1 (
    echo [INFO] Maven non trouve, utilisation du wrapper Maven (mvnw)
    set MAVEN_CMD=mvnw.cmd
) else (
    echo [INFO] Maven detecte
    set MAVEN_CMD=mvn
)
echo.

REM Verifier que MySQL est accessible (tentative de connexion)
echo [INFO] Verification de MySQL...
echo Si vous voyez une erreur de connexion MySQL, assurez-vous que :
echo   - MySQL est installe et demarre
echo   - Le port 3306 est accessible
echo   - L'utilisateur 'root' avec le mot de passe 'root' existe
echo.

echo [INFO] Compilation du projet...
call %MAVEN_CMD% clean compile
if errorlevel 1 (
    echo [ERREUR] La compilation a echoue
    pause
    exit /b 1
)
echo.

echo [INFO] Demarrage de l'application BuildLink...
echo L'application sera accessible a : http://localhost:8080
echo.
echo Appuyez sur Ctrl+C pour arreter l'application
echo.

call %MAVEN_CMD% spring-boot:run

