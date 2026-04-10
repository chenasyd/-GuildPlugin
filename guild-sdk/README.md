# `guild-sdk`

用于给外部模块工程提供编译期依赖的轻量 SDK。

## 用途

- 为独立模块工程提供 `com.guild.sdk.*`、`GuildModule`、`ModuleContext`、`AbstractModuleGUI` 等类型。
- 该模块是**编译期桩（stub）**，运行时应由主插件提供真实实现。
- 外部模块工程应将 `guild-sdk` 以 `provided` 方式引入，**不要打进最终模块 JAR**。

## 构建

```bash
mvn -f guild-sdk/pom.xml clean install
```

构建完成后，可在独立模块工程中引用：

```xml
<dependency>
    <groupId>com.guild</groupId>
    <artifactId>guild-sdk</artifactId>
    <version>1.3.6</version>
    <scope>provided</scope>
</dependency>
```

示例可参考仓库根目录的 `module-example-pom.xml`。
