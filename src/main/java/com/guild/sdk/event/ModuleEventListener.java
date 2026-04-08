package com.guild.sdk.event;

/**
 * 模块事件监听器基础包
 * <p>
 * 提供工会核心事件的回调钩子，模块可以实现这些方法来响应事件。
 * 使用方式：
 * <pre>{@code
 * ctx.getApi().onGuildCreate(event -> {
 *     logger.info("新工会创建: " + event.getGuildName());
 * });
 * }</pre>
 */
