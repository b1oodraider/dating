@echo off
rem =========================================================================
rem  e2e-test.bat - run the day-10 end-to-end test by double-click.
rem  Thin wrapper over e2e-test.ps1 (cmd has no JSON/JWT/HTTP - logic is in PS).
rem  Prereqs: stack is up (docker compose up), core healthy, notification up.
rem  Args are forwarded to the .ps1, e.g.:  e2e-test.bat -Gateway http://localhost:8088
rem =========================================================================
setlocal
rem go to the script folder (where docker-compose.yaml is) so `docker compose logs` finds the service
cd /d "%~dp0"

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0e2e-test.ps1" %*
set "EXITCODE=%ERRORLEVEL%"

echo.
if "%EXITCODE%"=="0" (
    echo [OK] E2E PASS
) else (
    echo [X] E2E FAIL ^(exit %EXITCODE%^)
)
echo.
pause
exit /b %EXITCODE%
