package com.guild.sdk;

import com.guild.GuildPlugin;
import com.guild.core.time.TimeProvider;
import com.guild.core.utils.ConsoleLogger;
import com.guild.sdk.http.HttpClientProvider;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP、服务器时间、控制台输出与模块语言资源工具。
 */
public final class ModuleRuntimeTools {

    private final GuildPlugin plugin;
    private final HttpClientProvider httpClient;

    public ModuleRuntimeTools(GuildPlugin plugin, HttpClientProvider httpClient) {
        this.plugin = plugin;
        this.httpClient = httpClient;
    }

    public CompletableFuture<String> httpGet(String url, Map<String, String> headers) {
        return httpClient.httpGet(url, headers);
    }

    public CompletableFuture<String> httpPost(String url, String body, Map<String, String> headers) {
        return httpClient.httpPost(url, body, headers);
    }

    public HttpClientProvider getHttpClient() {
        return httpClient;
    }

    public LocalDateTime getServerTime() {
        return TimeProvider.nowLocalDateTime();
    }

    public String getServerTimeString() {
        return TimeProvider.nowString();
    }

    public String getServerDateString() {
        return TimeProvider.formatDate(TimeProvider.nowLocalDateTime());
    }

    public String getServerTimePlusMinutes(int minutes) {
        return TimeProvider.plusMinutesString(minutes);
    }

    public String getServerTimePlusDays(int days) {
        return TimeProvider.plusDaysString(days);
    }

    public String formatServerTime(LocalDateTime dateTime) {
        return TimeProvider.format(dateTime);
    }

    public String formatServerDate(LocalDateTime dateTime) {
        return TimeProvider.formatDate(dateTime);
    }

    public void consoleInfo(String message) {
        ConsoleLogger.info(message);
    }

    public void consoleWarn(String message) {
        ConsoleLogger.warn(message);
    }

    public void consoleSevere(String message) {
        ConsoleLogger.severe(message);
    }

    public void consoleInfo(String message, String... args) {
        ConsoleLogger.info(message, args);
    }

    public void consoleWarn(String message, String... args) {
        ConsoleLogger.warn(message, args);
    }

    public void consoleSevere(String message, String... args) {
        ConsoleLogger.severe(message, args);
    }

    public boolean loadModuleLanguageResource(String moduleId, String lang) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            return false;
        }
        if (lang == null || lang.trim().isEmpty()) {
            return plugin.getLanguageManager().loadModuleLanguageResourcesForModule(moduleId);
        }
        return plugin.getLanguageManager().loadModuleLanguageResourcesForModule(moduleId, lang.toLowerCase());
    }

    public boolean releaseModuleLanguageResource(String moduleId, String lang) {
        if (moduleId == null || moduleId.trim().isEmpty() || lang == null || lang.trim().isEmpty()) {
            return false;
        }
        String moduleDirName = moduleId.toLowerCase();
        String language = lang.toLowerCase();
        String resourcePath = "lang/modules/" + moduleDirName + "/" + language + ".yml";
        if (plugin.getResource(resourcePath) == null) {
            return false;
        }
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (file.exists()) {
            return false;
        }
        try {
            plugin.saveResource(resourcePath, false);
            plugin.getLogger().info("Extracted bundled module language file: " + resourcePath);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to extract bundled module language file "
                    + resourcePath + ": " + e.getMessage());
            return false;
        }
    }

    public File getModuleLanguageFile(String moduleId, String lang) {
        if (moduleId == null || moduleId.trim().isEmpty() || lang == null || lang.trim().isEmpty()) {
            return null;
        }
        String moduleDirName = moduleId.toLowerCase();
        String language = lang.toLowerCase();
        return new File(plugin.getDataFolder(), "lang/modules/" + moduleDirName + "/" + language + ".yml");
    }
}
