# 数据库备份与维护

备份目录：`plugins/GuildPlugin/backup/`

## 自动备份

启动且数据库就绪后异步检查：

1. **按日**：当天（服务器本地日期）尚未成功备份 → 备份一次  
2. **按版本**：当前插件版本与 `.last-backup-meta.yml` 中记录不同 → 备份一次  

两者同时命中只打一份。可在 `config.yml` 关闭：

```yaml
backup:
  enabled: true
  directory: backup
  max-backups: 50
  on-startup-daily: true
  on-version-change: true
  mysql:
    method: jdbc          # jdbc | mysqldump
    mysqldump-path: mysqldump
```

超过 `max-backups` 时从最旧文件开始删除。

## 手动备份 / 维护

系统设置 GUI：

- **备份数据**：立即异步备份  
- **数据库维护**：输出类型/路径/最近备份，并对 SQLite 执行 `PRAGMA optimize` + `VACUUM`，对 MySQL 尝试 `OPTIMIZE TABLE`

## 文件格式

| 类型 | 文件名示例 | 内容 |
|------|------------|------|
| SQLite | `guild-backup_{ver}_{yyyyMMdd-HHmmss}_sqlite.zip` | `guild.db` + 存在的 `-wal`/`-shm` |
| MySQL | `..._mysql.sql.gz` | JDBC 逻辑导出（默认）或 `mysqldump` |

## 恢复

**SQLite**：停服 → 解压 zip → 用其中的 `guild.db`（及 wal/shm）覆盖插件数据目录对应文件 → 启动。

**MySQL**：维护窗内导入，例如：

```bash
gunzip -c guild-backup_..._mysql.sql.gz | mysql -uUSER -p DATABASE
```

English: [Database-Backup_EN.md](./Database-Backup_EN.md)
