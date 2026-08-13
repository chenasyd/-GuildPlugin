# Database Backup & Maintenance

Backup directory: `plugins/GuildPlugin/backup/`

## Automatic backups

After the database is ready on startup (async):

1. **Daily**: no successful backup yet today (server local date) → backup once  
2. **Version change**: plugin version differs from `.last-backup-meta.yml` → backup once  

If both match, only one archive is created. Toggle in `config.yml`:

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

When count exceeds `max-backups`, oldest files are deleted first.

## Manual backup / maintenance

System Settings GUI:

- **Backup Data**: run an async backup now  
- **Database Maintenance**: print status and run SQLite `PRAGMA optimize` + `VACUUM`, or MySQL `OPTIMIZE TABLE`

## File formats

| Type | Example name | Contents |
|------|--------------|----------|
| SQLite | `guild-backup_{ver}_{yyyyMMdd-HHmmss}_sqlite.zip` | `guild.db` + `-wal`/`-shm` if present |
| MySQL | `..._mysql.sql.gz` | JDBC logical dump (default) or `mysqldump` |

## Restore

**SQLite**: stop the server → unzip → overwrite the plugin data-folder DB files → start.

**MySQL** (example):

```bash
gunzip -c guild-backup_..._mysql.sql.gz | mysql -uUSER -p DATABASE
```

中文: [Database-Backup.md](./Database-Backup.md)
