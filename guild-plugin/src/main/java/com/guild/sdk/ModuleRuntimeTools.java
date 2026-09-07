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
 * HTTP、服务器时间与控制台输出工具。
 */
public final class ModuleRuntimeTools {

    private final HttpClientProvider httpClient;
    private final ModuleLanguageSupport language;

    public ModuleRuntimeTools(GuildPlugin plugin, HttpClientProvider httpClient) {
        this.httpClient = httpClient;
        this.language = new ModuleLanguageSupport(plugin);
    }

    ModuleRuntimeTools(HttpClientProvider httpClient, ModuleLanguageSupport language) {
        this.httpClient = httpClient;
        this.language = language;
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
        return language.loadModuleLanguageResource(moduleId, lang);
    }

    public boolean releaseModuleLanguageResource(String moduleId, String lang) {
        return language.releaseModuleLanguageResource(moduleId, lang);
    }

    public File getModuleLanguageFile(String moduleId, String lang) {
        return language.getModuleLanguageFile(moduleId, lang);
    }
}
