package com.guild.core.module.exception;

import java.util.List;

/**
 * 模块依赖异常 - 依赖检查失败时抛出
 */
public class ModuleDependencyException extends ModuleLoadException {

    private final String moduleId;
    private final List<String> missingDependencies;

    public ModuleDependencyException(String moduleId, List<String> missingDependencies) {
        super(buildMessage(moduleId, missingDependencies));
        this.moduleId = moduleId;
        this.missingDependencies = missingDependencies;
    }

    public String getModuleId() {
        return moduleId;
    }

    public List<String> getMissingDependencies() {
        return missingDependencies;
    }

    private static String buildMessage(String moduleId, List<String> missing) {
        return "模块依赖缺失 [" + moduleId + "]: " + missing;
    }
}
