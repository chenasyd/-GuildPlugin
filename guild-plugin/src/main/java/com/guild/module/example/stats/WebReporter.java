package com.guild.module.example.stats;

import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.config.ModuleConfigSection;
import com.guild.module.example.stats.model.GuildStatistics;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class WebReporter {
    private final GuildPluginAPI api;
    private final Logger logger;
    private final ModuleConfigSection config;

    public WebReporter(GuildPluginAPI api, Logger logger, ModuleConfigSection config) {
        this.api = api;
        this.logger = logger;
        this.config = config;
    }

    public void reportAllGuilds(List<GuildStatistics> allStats) {
        String endpoint = getStringConfig("web-report.endpoint", "");
        String token = getStringConfig("web-report.token", "");

        if (endpoint == null || endpoint.isEmpty()) {
            return;
        }

        String jsonPayload = buildJsonPayload(allStats);

        Map<String, String> headers = Map.of(
            "Content-Type", "application/json",
            "Authorization", "Bearer " + (token != null ? token : "")
        );

        api.httpPost(endpoint, jsonPayload, headers)
            .thenAccept(response -> {
                logger.info("[Stats-Web] 上报成功: " + response.length() + " bytes");
            })
            .exceptionally(ex -> {
                logger.warning("[Stats-Web] 上报失败: " + ex.getMessage());
                return null;
            });
    }

    public void healthCheck() {
        String healthEndpoint = getStringConfig("web-report.health-endpoint", "");
        if (healthEndpoint == null || healthEndpoint.isEmpty()) {
            return;
        }
        String token = getStringConfig("web-report.token", "");
        Map<String, String> headers = Map.of(
            "Accept", "application/json",
            "Authorization", "Bearer " + (token != null ? token : "")
        );
        api.httpGet(healthEndpoint, headers)
            .thenAccept(response -> logger.info("[Stats-Web] 健康检查通过"))
            .exceptionally(ex -> {
                logger.warning("[Stats-Web] 健康检查失败: " + ex.getMessage());
                return null;
            });
    }

    public CompletableFuture<String> fetchRemoteConfig(String url) {
        if (url == null || url.isEmpty()) {
            return CompletableFuture.completedFuture("");
        }
        return api.httpGet(url)
            .exceptionally(ex -> {
                logger.warning("[Stats-Web] 远程配置拉取失败: " + ex.getMessage());
                return "";
            });
    }

    private String getStringConfig(String path, String defaultValue) {
        if (config == null) return defaultValue;
        return config.getString(path, defaultValue);
    }

    private String buildJsonPayload(List<GuildStatistics> statsList) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"timestamp\":").append(System.currentTimeMillis())
          .append(",\"guilds\":[");

        for (int i = 0; i < statsList.size(); i++) {
            if (i > 0) sb.append(",");
            GuildStatistics s = statsList.get(i);
            sb.append("{")
              .append("\"id\":").append(s.getGuildId()).append(",")
              .append("\"name\":\"").append(escapeJson(s.getGuildName())).append("\",")
              .append("\"score\":").append(String.format("%.1f", s.getOverallScore())).append(",")
              .append("\"members\":").append(s.getMemberCount()).append(",")
              .append("\"balance\":").append(String.format("%.0f", s.getBalance())).append(",")
              .append("\"activity\":").append(String.format("%.1f", s.getActivityScore()))
              .append("}");
        }

        sb.append("]}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
