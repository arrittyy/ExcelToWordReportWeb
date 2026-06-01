@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0.."

if not exist "frontend\dist\index.html" (
    echo 未找到 frontend\dist\，请先运行 deploy\仅打包不上传.bat 构建前端。
    pause
    exit /b 1
)

echo 正在将 frontend\dist 打包为 deploy\dist.zip ...
powershell -NoProfile -Command "Compress-Archive -Path '.\frontend\dist\*' -DestinationPath '.\deploy\dist.zip' -Force"
if %errorlevel% neq 0 (
    echo 打包失败！
    pause
    exit /b 1
)

echo 完成：deploy\dist.zip
echo.
echo 用 Workbench 上传 dist.zip 到 ECS 的 /opt/reportweb/ 后，
echo 在 ECS 终端执行：
echo   cd /opt/reportweb ^&^& rm -rf www/* ^&^& unzip -o dist.zip -d www ^&^& rm dist.zip
echo.
pause
