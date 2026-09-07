# JaCoCo 覆盖率门禁

## 当前阶段（2）

- **门禁**：`com.guild.gui.base` 行覆盖率阈值 **25%**（自 ~19% 提升）
- **测试**：`GuiLayoutUtilsTest`、`AbstractPagedListGUITest`、`AbstractPagedMemberGUITest`、`AbstractPagedPlayerGUITest`

## 阶段 1（已完成）

- CI 使用 `mvn verify -Pcoverage-gate`，7 包行覆盖率门禁

## 阶段 0（已完成）

- 预置 `coverage-gate` profile、上传 JaCoCo 报告 artifact

## 构建与报告

- **CI / 本地**：`mvn verify -Pcoverage-gate -pl guild-plugin -am`
- **报告**：`guild-plugin/target/site/jacoco/index.html`；CI artifact 保留 14 天

## 门禁包与阈值

| 包 | 属性 | 阈值 |
|----|------|------|
| `com.guild.core.database.schema` | `jacoco.schema.line.minimum` | 95% |
| `com.guild.war.model` | `jacoco.war.model.line.minimum` | 85% |
| `com.guild.world.registry` | `jacoco.world.registry.line.minimum` | 84% |
| `com.guild.core.permissions` | `jacoco.permissions.line.minimum` | 70% |
| `com.guild.services.repository` | `jacoco.repository.line.minimum` | 40% |
| `com.guild.gui.base` | `jacoco.gui.base.line.minimum` | 25% |
| `com.guild.core.gui.session` | `jacoco.gui.session.line.minimum` | 50% |

## 故意不门禁的范围

- `com.guild.gui`（具体 GUI，~6800 行，当前 ~0%）
- `com.guild.core.gui`（`GUIManager` 等，需集成测）
- 全模块 bundle 总覆盖率（当前 ~12%，不适合一刀切）

JaCoCo 采集已排除：`com/guild/module/example/**`（示例模块）

## 调阈值（ratchet）

补测试后，在 `guild-plugin/pom.xml` 的 `coverage-gate` profile 中**只提高**对应 `jacoco.*.line.minimum` 属性，不要降低。

## 后续阶段

1. **阶段 3**：按需增加 CLASS 级规则（如 `UpdateDownloadSecurity`）；继续提高 `gui.base` 阈值