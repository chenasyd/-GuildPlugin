#!/bin/bash

echo "正在构建工会插件..."

# 检查Maven是否安装
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到Maven，请先安装Maven"
    exit 1
fi

# 清理并编译项目
echo "清理项目..."
mvn clean

echo "编译项目..."
mvn compile

echo "打包项目..."
mvn package

if [ $? -ne 0 ]; then
    echo "构建失败！"
    exit 1
fi

echo "构建成功！"
echo "插件文件位置: target/guild-plugin-1.0.0.jar"
