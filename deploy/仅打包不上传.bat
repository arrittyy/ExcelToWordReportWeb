@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0.."

echo ========================================
echo   检测报告 Web - 仅打包（不上传）
echo ========================================
echo.

echo [1/2] 打包后端...
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo 后端打包失败！
    pause
    exit /b 1
)
echo 完成：target\report-web-0.0.1-SNAPSHOT.jar
echo.

echo [2/2] 构建前端...
cd frontend
call npm run build 2>nul
if %errorlevel% neq 0 (
    call npm install
    call npm run build
)
cd ..
if %errorlevel% neq 0 (
    echo 前端构建失败！
    pause
    exit /b 1
)
echo 完成：frontend\dist\
echo.
echo 打包完成。手动上传命令（把 IP 换成你的 ECS）：
echo   scp target/report-web-0.0.1-SNAPSHOT.jar root@你的IP:/opt/reportweb/
echo   scp -r frontend/dist/* root@你的IP:/opt/reportweb/www/
echo.
pause
