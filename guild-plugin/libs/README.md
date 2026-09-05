# Local Maven repository (`guild-plugin/libs`)

This directory is a **file-based Maven repository** used at compile time for third-party JARs that are not published to Maven Central.

## ImagoCore (optional, `provided`)

GUI image mode integrates with [ImagoCore](https://github.com/) when the plugin is installed on the server. The dependency is **`provided`** — the Guild plugin JAR does **not** bundle ImagoCore.

### Layout (already in repo)

```
libs/
└── org/a/imagocore/1.0-SNAPSHOT/
    ├── imagocore-1.0-SNAPSHOT.jar
    └── imagocore-1.0-SNAPSHOT.pom
```

`guild-plugin/pom.xml` declares:

```xml
<repository>
    <id>project-local-repo</id>
    <url>file://${project.basedir}/libs</url>
</repository>
```

### Refreshing ImagoCore

If you need a newer ImagoCore build:

1. Build or obtain `imagocore-1.0-SNAPSHOT.jar` (or update the version in `pom.xml`).
2. Install into this tree:

```bash
# From repo root (Unix)
mkdir -p guild-plugin/libs/org/a/imagocore/1.0-SNAPSHOT
cp /path/to/imagocore.jar guild-plugin/libs/org/a/imagocore/1.0-SNAPSHOT/imagocore-1.0-SNAPSHOT.jar
```

Or install with Maven into the local tree:

```bash
mvn install:install-file \
  -Dfile=/path/to/imagocore.jar \
  -DgroupId=org.a \
  -DartifactId=imagocore \
  -Dversion=1.0-SNAPSHOT \
  -Dpackaging=jar \
  -DlocalRepositoryPath=guild-plugin/libs
```

3. Rebuild: `mvn clean package -pl guild-plugin -am`

### Building without ImagoCore

If `libs/org/a/imagocore/...` is missing, **`mvn compile` will fail** because the compile-time API is required. For CI or minimal builds, keep the committed JAR in `libs/` or temporarily comment out the ImagoCore dependency (not recommended for release builds).

Runtime: servers without ImagoCore still load GuildPlugin; image GUI mode is disabled when the plugin is absent (`softdepend` in `plugin.yml`).
