package com.guild.sdk.api;

import com.guild.sdk.http.HttpClientProvider;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP、服务器时间、控制台与模块语言资源域 API。
 *
 * @since 1.6.7
 */
public interface ModuleRuntimeAPI {

    CompletableFuture<String> httpGet(String url, Map<String, String> headers);

    CompletableFuture<String> httpGet(String url);

    CompletableFuture<String> httpPost(String url, String body, Map<String, String> headers);

    HttpClientProvider getHttpClient();

    LocalDateTime getServerTime();

    String getServerTimeString();

    String getServerDateString();

    String getServerTimePlusMinutes(int minutes);

    String getServerTimePlusDays(int days);

    String formatServerTime(LocalDateTime dateTime);

    String formatServerDate(LocalDateTime dateTime);

    void consoleInfo(String message);

    void consoleWarn(String message);

    void consoleSevere(String message);

    void consoleInfo(String message, String... args);

    void consoleWarn(String message, String... args);

    void consoleSevere(String message, String... args);

    boolean loadModuleLanguageResource(String moduleId, String lang);

    boolean releaseModuleLanguageResource(String moduleId, String lang);

    File getModuleLanguageFile(String moduleId, String lang);
}
