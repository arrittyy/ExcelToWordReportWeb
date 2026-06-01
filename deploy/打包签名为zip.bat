@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0.."

if not exist "signatures" (
    echo 未找到 signatures 目录。
    pause
    exit /b 1
)

echo 正在将 signatures 打包为 deploy\signatures.zip ...
powershell -NoProfile -Command "Compress-Archive -Path '.\signatures\*' -DestinationPath '.\deploy\signatures.zip' -Force"
if %errorlevel% neq 0 (
    echo 打包失败！
    pause
    exit /b 1
)

echo 完成：deploy\signatures.zip
echo.
echo 用 Workbench 上传 signatures.zip 到 ECS 的 /opt/reportweb/ 后，
echo 在 ECS 终端执行：
echo   cd /opt/reportweb ^&^& unzip -o signatures.zip -d signatures ^&^& rm -f signatures.zip
echo.
pause
