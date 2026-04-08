package com.guild.core.module.exception;

/**
 * 模块冲突异常 - 模块ID或功能冲突时抛出
 */
public class ModuleConflictException extends ModuleLoadException {

    public ModuleConflictException(String message) {
        super(message);
    }

    public ModuleConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
