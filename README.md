# GuildPlugin - Feature-Complete Minecraft Guild System

**GuildPlugin** 是一个功能完整的 Minecraft 公会/工会系统插件，支持多语言、经济系统、关系管理、等级系统、完整 GUI 界面等丰富功能。

## 📚 文档语言 / Documentation Language

- **🇨🇳 中文文档**: [README_CN.md](./README_CN.md)
- **🇬🇧 English Documentation**: [README_EN.md](./README_EN.md)

## ✨ 核心特性

- **公会管理**: 创建、解散、成员管理、权限系统
- **经济系统**: 资金管理、存款取款、Vault 集成
- **关系系统**: 联盟、敌对、战争、停战
- **等级系统**: 公会成长与成员上限提升
- **完整 GUI**: 图形化界面，操作便捷
- **多语言**: 支持中文、英文、波兰语
- **异步处理**: 高性能数据库操作
- **模块化 SDK**: 支持外部模块扩展开发（4个示例模块，SDK覆盖率100%）
- **CustomGUI 系统**: 模块可独立注册/打开/注销自定义GUI
- **EventBus 事件总线**: 模块间松耦合通信
- **ServiceContainer 服务容器**: 模块可访问核心系统服务

## 🔨 快速构建 (Maven Multi-Module)

```bash
# 构建所有模块
mvn clean install

# 仅构建 SDK（供外部模块开发者使用）
mvn clean install -pl guild-sdk

# 构建 Plugin 并打包示例模块
mvn clean package -pl guild-plugin -Pbuild-member-rank-module
```

## 📋 系统要求

- **Minecraft**: 1.21+
- **Java**: JDK 17+
- **可选依赖**: Vault（经济）、PlaceholderAPI（变量）

## 🔗 项目链接

- **GitHub**: [chenasyd/-GuildPlugin](https://github.com/chenasyd/-GuildPlugin)
- **Issues**: [报告问题](https://github.com/chenasyd/-GuildPlugin/issues)
- **License**: [GNU GPL v3.0](./LICENSE)

---

**选择上方对应语言链接查看完整文档 / Choose your language above for full documentation**

## 配置新增说明 — 等级系统 / New config: Level requirements

已在 `config.yml` 中新增等级相关配置，用于替代代码中硬编码的升级费用：

- `guild.max-level` (int): 最大等级，达到该等级后无下一级，默认 `10`。
- `guild.levels.<n>` (double): 键为当前等级 `n`，值为升级到下一级所需的资金。例如 `guild.levels.1: 5000.0` 表示从等级1升到等级2需要5000。

示例配置片段：

```yaml
guild:
	max-level: 10
	levels:
		1: 5000.0
		2: 10000.0
		3: 20000.0
		# ...
```

插件在启动时会加载这些配置并在 GUI（例如工会信息面板）中使用它们来显示“下级所需”与进度条。修改后无需重启服务器，但建议使用 `/guildadmin reload` 进行热加载并确认 UI 显示正确。
