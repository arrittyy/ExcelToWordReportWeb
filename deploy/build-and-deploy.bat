@echo off
chcp 65001 >nul
setlocal

REM ========== 配置：改成你的 ECS 公网 IP ==========
set ECS_IP=101.200.184.32
set ECS_USER=root
REM ==============================================

echo ========================================
echo   检测报告 Web - 打包并上传到阿里云 ECS
echo ========================================
echo.

cd /d "%~dp0.."

echo [1/3] 打包后端...
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo 后端打包失败！
    pause
    exit /b 1
)
echo 后端打包完成：target\report-web-0.0.1-SNAPSHOT.jar
echo.

echo [2/3] 构建前端...
cd frontend
call npm run build 2>nul
if %errorlevel% neq 0 (
    echo 前端构建失败，尝试先 npm install...
    call npm install
    call npm run build
)
if %errorlevel% neq 0 (
    echo 前端构建失败！
    cd ..
    pause
    exit /b 1
)
cd ..
echo 前端构建完成：frontend\dist\
echo.

echo [3/3] 上传到 ECS %ECS_USER%@%ECS_IP% ...
scp target/report-web-0.0.1-SNAPSHOT.jar %ECS_USER%@%ECS_IP%:/opt/reportweb/
scp -r frontend/dist/* %ECS_USER%@%ECS_IP%:/opt/reportweb/www/
if %errorlevel% neq 0 (
    echo 上传失败，请检查网络和 ECS_IP 是否正确。
    pause
    exit /b 1
)
echo.
echo ========================================
echo   完成！请在 ECS 上重启后端（见下方说明）
echo ========================================
echo.
echo 在 ECS 上执行以下命令之一重启应用：
echo   systemctl restart reportweb
echo 或（若未配置 systemd）：
echo   pkill -f report-web; cd /opt/reportweb ^&^& source env.sh ^&^& nohup java -jar report-web-0.0.1-SNAPSHOT.jar ^> logs/app.log 2^>^&1 ^&
echo.
pause
