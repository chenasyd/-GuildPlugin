package com.guild.update;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.guild.core.utils.CompatibleScheduler;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Modrinth 版本检测器
 * <p>
 * 每天首次启动时通过 Modrinth API 检查插件是否有新版本发布。
 * 检查逻辑：将本地版本号与 API 返回的版本列表中最新的 release 版本号进行语义化比较。
 * <p>
 * 检查时机：插件 onEnable 后异步执行一次，之后每 24 小时检查一次。
 * 仅比较 release 类型版本，忽略 alpha/beta。
 */
public class UpdateChecker {

    private static final String MODRINTH_PROJECT_ID = "8mvSrFJf";
    private static final String VERSIONS_API_URL =
            "https://api.modrinth.com/v2/project/" + MODRINTH_PROJECT_ID + "/version";
    private static final String USER_AGENT = "GuildPlugin/UpdateChecker (chenasyd)";
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 15000;
    private static final long CHECK_INTERVAL_TICKS = 20L * 60 * 60 * 24; // 24 hours

    private final JavaPlugin plugin;
    private final Logger logger;
    private final Gson gson;

    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.gson = new Gson();
    }

    /**
     * 启动版本检测。立即异步执行一次，之后每 24 小时循环。
     * <p>
     * 采用自调度模式：首次异步执行 → 延迟后调度下一次异步执行，
     * 以兼容 CompatibleScheduler 不支持 runTaskTimerAsync 的限制。
     */
    public void start() {
        // 立即异步执行首次检查
        CompatibleScheduler.runTaskAsync(plugin, this::checkForUpdates);

        // 24 小时后触发循环调度
        CompatibleScheduler.runTaskLater(plugin, this::scheduleNextCheck, CHECK_INTERVAL_TICKS);
    }

    /**
     * 调度下一次异步版本检查。
     * 每次执行完后自动排定下一轮，形成持久循环。
     */
    private void scheduleNextCheck() {
        CompatibleScheduler.runTaskAsync(plugin, () -> {
            checkForUpdates();
            // 递归调度：延迟后再次触发本方法，维持循环
            CompatibleScheduler.runTaskLater(plugin, this::scheduleNextCheck, CHECK_INTERVAL_TICKS);
        });
    }

    /**
     * 执行版本检测的核心逻辑
     */
    private void checkForUpdates() {
        try {
            String localVersion = plugin.getDescription().getVersion();
            JsonArray versions = fetchVersions();

            if (versions == null || versions.isEmpty()) {
                logger.warning("[UpdateChecker] Failed to fetch version list, skipping check.");
                return;
            }

            String latestRelease = findLatestReleaseVersion(versions);
            if (latestRelease == null) {
                logger.warning("[UpdateChecker] No release version found on Modrinth.");
                return;
            }

            int comparison = compareVersions(localVersion, latestRelease);
            if (comparison < 0) {
                logger.info("[UpdateChecker] New version available: v" + latestRelease + " (current: v" + localVersion + ")");
            } else {
                logger.info("[UpdateChecker] No new version available");
            }

        } catch (Exception e) {
            logger.warning("[UpdateChecker] Update check failed: " + e.getMessage());
        }
    }

    /**
     * 从 Modrinth API 获取版本列表
     */
    private JsonArray fetchVersions() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(VERSIONS_API_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", USER_AGENT);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                logger.warning("[UpdateChecker] Modrinth API returned HTTP " + responseCode);
                return null;
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            return gson.fromJson(response.toString(), JsonArray.class);

        } catch (Exception e) {
            logger.warning("[UpdateChecker] HTTP request failed: " + e.getMessage());
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 从版本列表中找出最新的 release 版本号
     * API 返回的列表已按 date_published 降序排列，第一个 release 即为最新
     */
    private String findLatestReleaseVersion(JsonArray versions) {
        for (int i = 0; i < versions.size(); i++) {
            JsonObject v = versions.get(i).getAsJsonObject();
            String type = v.has("version_type") ? v.get("version_type").getAsString() : "";
            if ("release".equals(type)) {
                return v.get("version_number").getAsString();
            }
        }
        return null;
    }

    /**
     * 语义化版本号比较
     * <p>
     * 支持格式：x.y.z（如 1.5.4）或 x.y（如 1.0）
     *
     * @return -1: v1 < v2; 0: v1 == v2; 1: v1 > v2
     */
    static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int maxLen = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLen; i++) {
            int num1 = i < parts1.length ? parsePart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parsePart(parts2[i]) : 0;
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

    private static int parsePart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
