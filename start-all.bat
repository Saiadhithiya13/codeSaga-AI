@echo off
echo Starting CodeSage AI Servers...

:: Start the Frontend
echo Starting Frontend (React + Vite)...
start "CodeSage Frontend" cmd /k "cd frontend && echo Installing Frontend Dependencies... && npm install && echo Starting Vite Dev Server... && npm run dev"

:: Start the Backend
echo Starting Backend (Spring Boot)...
start "CodeSage Backend" powershell -NoExit -ExecutionPolicy Bypass -Command "& { Set-Location '%~dp0'; .\start-backend.ps1 }"

echo ---------------------------------------------------
echo Both servers are launching in separate windows!
echo - Frontend window: npm run dev
echo - Backend window: Spring Boot / mvnw
echo.
echo You can safely close this window.
echo ---------------------------------------------------
pause
