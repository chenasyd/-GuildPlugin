@echo off
echo 正在构建工会插件...

REM 检查Maven是否安装
mvn -version >nul 2>&1
if errorlevel 1 (
    echo 错误: 未找到Maven，请先安装Maven
    pause
    exit /b 1
)

REM 清理并编译项目
echo 清理项目...
mvn clean

echo 编译项目...
mvn compile

echo 打包项目...
mvn package

if errorlevel 1 (
    echo 构建失败！
    pause
    exit /b 1
)

echo 构建成功！
echo 插件文件位置: target/guild-plugin-1.0.0.jar
pause
