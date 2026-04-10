package com.guild.core.module.exception;

/**
 * 模块加载异常 - 模块加载过程中发生错误时抛出
 */
public class ModuleLoadException extends Exception {

    public ModuleLoadException(String message) {
        super(message);
    }

    public ModuleLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
