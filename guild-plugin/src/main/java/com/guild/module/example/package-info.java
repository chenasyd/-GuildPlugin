/**
 * Built-in example modules demonstrating the Guild SDK (API 1.6.6+).
 *
 * <p><b>Expected patterns (aligned with SDK Developer Guide):</b>
 * <ul>
 *   <li>{@code implements GuildModule} + root {@code module.yml}
 *       ({@code api-version}, {@code folia-compatible: true})</li>
 *   <li>Register with {@code moduleId}: GUI buttons, sub-commands, placeholders</li>
 *   <li>Custom GUIs via {@link com.guild.sdk.gui.ModuleGUIRegistration}
 *       (layout / imageBinding / moduleId) or {@code AbstractModuleGUI}</li>
 *   <li>Guild events with {@code getModuleInstance()} for auto-cleanup</li>
 *   <li>Bukkit listeners via {@code ModuleContext#registerEvents}</li>
 *   <li>Player messages via {@code context.getMessage(player, ...)} / module lang files</li>
 *   <li>Data under {@code ModuleDataDirectory} ({@code modules/{id}/data/})</li>
 * </ul>
 *
 * <p><b>Modules:</b> announcement, guild-stats, guild-quest, member-rank, api-test, testlang.
 */
package com.guild.module.example;
