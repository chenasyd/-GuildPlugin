package com.guild.core.module;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleDescriptor {
    private String id;
    private String name;
    private String version;
    private String author;
    private String description;
    private String main;
    private String apiVersion;
    private List<String> depends = new ArrayList<>();
    private List<String> softDepends = new ArrayList<>();
    private String type = "mixed";
    private String configPrefix;
    private File sourceFile;

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
}
