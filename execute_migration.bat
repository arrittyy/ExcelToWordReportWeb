@echo off
echo ============================================
echo 执行数据库迁移：添加客户方和客户方人员字段
echo ============================================
echo.
echo 请确保：
echo 1. PostgreSQL 服务正在运行
echo 2. 数据库 reportweb 存在
echo 3. 用户名: postgres, 密码: 121212
echo.
echo 如果 psql 不在 PATH 中，请手动在 pgAdmin 中执行 add_customer_fields_migration.sql
echo.
pause

REM 尝试使用 psql 执行迁移（如果可用）
if exist "C:\Program Files\PostgreSQL\*\bin\psql.exe" (
    for /f "delims=" %%i in ('dir /b /s "C:\Program Files\PostgreSQL\*\bin\psql.exe" 2^>nul') do (
        "%%i" -h localhost -U postgres -d reportweb -f add_customer_fields_migration.sql
        goto :done
    )
)

echo.
echo 未找到 psql 命令，请使用以下方法之一：
echo.
echo 方法1：使用 pgAdmin
echo   1. 打开 pgAdmin
echo   2. 连接到数据库 reportweb
echo   3. 右键数据库 -^> Query Tool
echo   4. 复制 add_customer_fields_migration.sql 的内容并执行
echo.
echo 方法2：找到 psql.exe 的完整路径后手动执行：
echo   "C:\Program Files\PostgreSQL\版本号\bin\psql.exe" -h localhost -U postgres -d reportweb -f add_customer_fields_migration.sql
echo.
pause

:done
echo.
echo 迁移完成！
pause


