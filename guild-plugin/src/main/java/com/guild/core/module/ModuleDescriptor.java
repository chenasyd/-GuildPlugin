package com.guild.core.module;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 模块描述符 - 从 module.yml 解析得到，描述模块的基本信息
 */
public class ModuleDescriptor {

    private String id;              // 唯一标识符（如 guild-query-enhancer）
    private String name;            // 显示名称
    private String version;         // 版本号
    private String author;          // 作者
    private String description;     // 描述
    private String main;            // 模块主类全限定名
    private String apiVersion;      // 要求的核心API版本
    private List<String> depends;   // 硬依赖列表
    private List<String> softDepends; // 软依赖列表
    private String type;            // 模块类型：gui | command | event | api | mixed
    private String configPrefix;    // 配置段前缀
    private File sourceFile;        // 来源 jar 文件

    public ModuleDescriptor() {
        this.depends = new ArrayList<>();
        this.softDepends = new ArrayList<>();
        this.type = "mixed";
    }

    /**
     * 从 YamlConfiguration 解析模块描述符
     *
     * @param config 已解析的Yaml配置
     * @return 模块描述符实例
     * @throws IllegalArgumentException 缺少必填字段时抛出
     */
    public static ModuleDescriptor fromConfig(YamlConfiguration config) {
        ModuleDescriptor desc = new ModuleDescriptor();

        // 必填字段校验
        if (!config.contains("id")) {
            throw new IllegalArgumentException("module.yml 缺少必填字段: id");
        }
        if (!config.contains("main")) {
            throw new IllegalArgumentException("module.yml 缺少必填字段: main");
        }

        desc.id = config.getString("id");
        desc.name = config.getString("name", desc.id);
        desc.version = config.getString("version", "1.0.0");
        desc.author = config.getString("author", "Unknown");
        desc.description = config.getString("description", "");
        desc.main = config.getString("main");
        desc.apiVersion = config.getString("api-version", "1.0.0");
        desc.type = config.getString("type", "mixed");
        desc.configPrefix = config.getString("config-prefix", desc.id);

        // 依赖列表
        if (config.contains("depends")) {
            desc.depends = config.getStringList("depends");
        }
        if (config.contains("soft-depends")) {
            desc.softDepends = config.getStringList("soft-depends");
        }

        return desc;
    }

    /**
     * 从 YAML 字符串内容解析模块描述符
     */
    public static ModuleDescriptor fromYaml(String yamlContent) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yamlContent);
        } catch (Exception e) {
            throw new IllegalArgumentException("无法解析 module.yml: " + e.getMessage(), e);
        }
        return fromConfig(config);
    }

    /**
     * 从 jar 文件中的 module.yml 解析
     */
    public static ModuleDescriptor fromJar(File jarFile) throws Exception {
        java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile);
        java.util.jar.JarEntry entry = jar.getJarEntry("module.yml");
        if (entry == null) {
            jar.close();
            throw new IllegalArgumentException("jar 文件中缺少 module.yml: " + jarFile.getName());
        }

        StringBuilder sb = new StringBuilder();
        try (java.io.InputStream is = jar.getInputStream(entry);
             java.io.BufferedReader reader = new java.io.BufferedReader(
                     new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        jar.close();

        ModuleDescriptor desc = fromYaml(sb.toString());
        desc.sourceFile = jarFile;
        return desc;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMain() { return main; }
    public void setMain(String main) { this.main = main; }

    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }

    public List<String> getDepends() { return Collections.unmodifiableList(depends); }
    public void setDepends(List<String> depends) { this.depends = new ArrayList<>(depends); }

    public List<String> getSoftDepends() { return Collections.unmodifiableList(softDepends); }
    public void setSoftDepends(List<String> softDepends) { this.softDepends = new ArrayList<>(softDepends); }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getConfigPrefix() { return configPrefix; }
    public void setConfigPrefix(String configPrefix) { this.configPrefix = configPrefix; }

    public File getSourceFile() { return sourceFile; }
    public void setSourceFile(File sourceFile) { this.sourceFile = sourceFile; }

    @Override
    public String toString() {
        return "ModuleDescriptor{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", author='" + author + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
