package com.guild.module.cloud;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.guild.GuildPlugin;
import com.guild.core.module.ModuleManager;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.ColorUtils;
import com.guild.update.UpdateManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 云端模块仓库管理器
 * <p>
 * 内置官方仓库（不可修改），开发者模式下可添加多个外置仓库。
 * 每个仓库拥有独立别名，方便在 {@code /guildmodule cloud} 列表中区分来源。
 * <p>
 * tag_name 格式为 {@code sdk-1.5.5}，其中 SDK 版本号仅作为信息展示，
 * 实际 SDK 版本不影响模块兼容性。
 */
public class CloudModuleRepository {

    /** 官方仓库 API URL */
    private static final String OFFICIAL_API_URL =
            "https://api.github.com/repos/chenasyd/GuildPlugin/releases";
    private static final String USER_AGENT = "GuildPlugin/CloudModuleRepository (chenasyd)";
    private static final int TIMEOUT = 10000;
    /** 当目标文件已存在时，用于生成随机后缀的字符池 */
    private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random RANDOM = new Random();
    /** 最低兼容 API 版本 — 低于此版本的模块不建议下载使用 */
    private static final String MINIMUM_COMPATIBLE_API = "1.6.3";

    static {
        // 绕过 SSL 证书验证 — 兼容旧 Java 版本 / 受限环境
        try {
            TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] c, String a) {}
                        public void checkServerTrusted(X509Certificate[] c, String a) {}
                    }
            };
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustAll, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((h, s) -> true);
        } catch (Exception ignored) {
        }
    }

    /**
     * 仓库来源
     *
     * @param name   仓库别名（显示用，支持 & 颜色代码）
     * @param apiUrl 仓库 API 地址（返回 GitHub Releases JSON 格式）
     */
    public record RepositorySource(String name, String apiUrl) {}

    /**
     * 云端模块信息
     *
     * @param name        模块显示名（去掉 .jar 后缀的文件名）
     * @param fileName    原始文件名（如 modules-announcement.jar）
     * @param downloadUrl 浏览器下载链接
     * @param size        文件大小（字节）
     * @param sdkVersion  对应的 SDK 版本号
     * @param sourceName  来源仓库别名
     */
    public record ModuleInfo(String name, String fileName, String downloadUrl,
                             long size, String sdkVersion, String sourceName) {}

    private final GuildPlugin plugin;
    private final Gson gson = new Gson();
    private final List<RepositorySource> sources = new ArrayList<>();
    private final boolean devMode;
    private final String officialSourceName;

    public CloudModuleRepository(GuildPlugin plugin) {
        this.plugin = plugin;

        // 官方仓库始终存在且不可修改，名称从语言键读取
        officialSourceName = plugin.getLanguageManager().getCoreMessage(
                "cloud.official-repo", "&b&lOfficial Repository");
        sources.add(new RepositorySource(officialSourceName, OFFICIAL_API_URL));

        // 读取开发者模式与外置仓库配置
        FileConfiguration config = plugin.getConfigManager().getMainConfig();
        devMode = config.getBoolean("developer-mode.enabled", false);

        if (devMode) {
            List<Map<?, ?>> customRepos = config.getMapList("developer-mode.repositories");
            if (customRepos != null && !customRepos.isEmpty()) {
                for (Map<?, ?> repo : customRepos) {
                    String name = repo.containsKey("name") ? repo.get("name").toString().trim() : null;
                    String api = repo.containsKey("api") ? repo.get("api").toString().trim() : null;
                    if (name != null && !name.isEmpty() && api != null && !api.isEmpty()) {
                        sources.add(new RepositorySource("&e" + name, api));
                        plugin.getLogger().info("[CloudModuleRepo] Registered external repository: "
                                + name + " -> " + api);
                    }
                }
            }
            plugin.getLogger().info("[CloudModuleRepo] Developer mode enabled, "
                    + (sources.size() - 1) + " external repository(s) registered.");
        }
    }

    // ==================== 公开方法 ====================

    /**
     * 异步列出所有仓库的云端模块，按仓库分组显示
     */
    public void listModules(CommandSender sender) {
        CompatibleScheduler.runTaskAsync(plugin, () -> {
            // 按仓库分组抓取
            Map<String, List<ModuleInfo>> grouped = new LinkedHashMap<>();
            for (RepositorySource source : sources) {
                List<ModuleInfo> modules = fetchModules(source);
                if (!modules.isEmpty()) {
                    grouped.put(source.name(), modules);
                }
            }

            CompatibleScheduler.runTask(plugin, () -> {
                if (grouped.isEmpty()) {
                    replyLang(sender, "cloud.empty", "&cNo cloud modules found.");
                    return;
                }

                replyLang(sender, "cloud.title", "&b&l=== Cloud Modules ===");
                boolean first = true;
                for (Map.Entry<String, List<ModuleInfo>> entry : grouped.entrySet()) {
                    if (!first) {
                        sender.sendMessage(ColorUtils.colorize(""));
                    }
                    first = false;

                    // 仓库头
                    String sourceLabel = entry.getKey();
                    String officialTag = getLang(sender, "cloud.official-tag", "&8[Official]");
                    if (officialSourceName.equals(sourceLabel)) {
                        sourceLabel += " " + officialTag;
                    }
                    sender.sendMessage(ColorUtils.colorize("&7--- " + sourceLabel + " &7---"));

                    // 模块列表
                    for (ModuleInfo m : entry.getValue()) {
                        boolean outdated = m.sdkVersion() == null || m.sdkVersion().isEmpty()
                                || UpdateManager.compareVersions(m.sdkVersion(), MINIMUM_COMPATIBLE_API) < 0;
                        if (outdated) {
                            String outdatedMsg = getLang(sender, "cloud.outdated",
                                    "&c&l[Not Recom.] &7Requires SDK {0}+",
                                    MINIMUM_COMPATIBLE_API);
                            sender.sendMessage(ColorUtils.colorize(
                                    "&c" + m.fileName() + " &7- " + formatSize(m.size())
                                    + " &8(sdk: " + m.sdkVersion() + ")"
                                    + " " + outdatedMsg));
                        } else {
                            sender.sendMessage(ColorUtils.colorize(
                                    "&a" + m.fileName() + " &7- " + formatSize(m.size())
                                    + " &8(sdk: " + m.sdkVersion() + ")"));
                        }
                    }
                }
            });
        });
    }

    /**
     * 异步下载指定云端模块（在所有仓库中搜索）
     */
    public void downloadModule(CommandSender sender, String moduleName) {
        CompatibleScheduler.runTaskAsync(plugin, () -> {
            try {
                // 遍历所有仓库查找模块
                ModuleInfo target = null;
                for (RepositorySource source : sources) {
                    List<ModuleInfo> modules = fetchModules(source);
                    target = modules.stream()
                            .filter(m -> m.name().equalsIgnoreCase(moduleName)
                                    || m.fileName().equalsIgnoreCase(moduleName)
                                    || (m.fileName().equalsIgnoreCase(moduleName + ".jar")))
                            .findFirst().orElse(null);
                    if (target != null) break;
                }

                if (target == null) {
                    replyLang(sender, "cloud.not-found",
                            "&cModule '{0}' not found in any repository.", moduleName);
                    return;
                }

                File modulesDir = plugin.getServiceContainer().get(ModuleManager.class)
                        .getModulesDirectory();
                File outFile = new File(modulesDir, target.fileName());

                // If target file already exists (possibly loaded and locked by JVM),
                // rename the download to avoid "file in use" conflict
                boolean wasRenamed = false;
                if (outFile.exists()) {
                    String originalName = target.fileName();
                    int dotIndex = originalName.lastIndexOf('.');
                    String base = dotIndex > 0 ? originalName.substring(0, dotIndex) : originalName;
                    String ext = dotIndex > 0 ? originalName.substring(dotIndex) : ".jar";
                    String newName = base + "-" + generateRandomSuffix(5) + ext;
                    outFile = new File(modulesDir, newName);
                    wasRenamed = true;
                }

                String downloadingMsg = getLang(sender, "cloud.downloading",
                        "&7[{0}] Downloading {1} ({2}, sdk: {3})...",
                        target.sourceName(), outFile.getName(),
                        formatSize(target.size()), target.sdkVersion());
                replyRaw(sender, downloadingMsg);

                // 下载
                HttpURLConnection conn = (HttpURLConnection)
                        new URL(target.downloadUrl()).openConnection();
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setConnectTimeout(TIMEOUT);
                conn.setReadTimeout(60000); // 下载超时 60s

                try (InputStream in = conn.getInputStream()) {
                    Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    conn.disconnect();
                }

                if (wasRenamed) {
                    replyLang(sender, "cloud.download-renamed",
                            "&aDownloaded as &e{0} &a(saved with new name because &e{1} &ais in use).",
                            outFile.getName(), target.fileName());
                    replyLang(sender, "cloud.download-tip",
                            "&6Tip: &7Unload old module first, replace jar: &e/guildmodule unload <id> &7-> replace jar -> &e/guildmodule load {0}",
                            outFile.getName());
                } else {
                    replyLang(sender, "cloud.download-complete",
                            "&aDownloaded {0}. Use &e/guildmodule load {0} &ato load it.",
                            outFile.getName());
                }

            } catch (Exception e) {
                replyLang(sender, "cloud.download-failed",
                        "&cDownload failed: {0}", e.getMessage());
            }
        });
    }

    // ==================== 内部方法 ====================

    /**
     * 从指定仓库抓取模块列表，按文件名去重（最新 SDK 版本优先）。
     */
    private List<ModuleInfo> fetchModules(RepositorySource source) {
        Map<String, ModuleInfo> dedup = new LinkedHashMap<>();
        try {
            JsonArray releases = fetchReleases(source.apiUrl());
            if (releases == null) return new ArrayList<>();

            for (JsonElement relElem : releases) {
                JsonObject release = relElem.getAsJsonObject();
                String tagName = release.get("tag_name").getAsString();
                String sdkVersion = extractSdkVersion(tagName);

                JsonArray assets = release.getAsJsonArray("assets");
                if (assets == null) continue;

                for (JsonElement a : assets) {
                    JsonObject asset = a.getAsJsonObject();
                    String fileName = asset.get("name").getAsString();

                    if (!fileName.endsWith(".jar")) continue;

                    String displayName = fileName.substring(0, fileName.length() - 4);
                    // 按文件名去重 — 保留最先遇到的（最新 SDK）
                    dedup.putIfAbsent(fileName, new ModuleInfo(
                            displayName,
                            fileName,
                            asset.get("browser_download_url").getAsString(),
                            asset.get("size").getAsLong(),
                            sdkVersion,
                            source.name()));
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("[CloudModuleRepo] Fetch failed for "
                    + source.name() + ": " + ex.getMessage());
        }
        return new ArrayList<>(dedup.values());
    }

    /**
     * 请求指定 URL 的 Release 列表（返回 JSON 数组）
     */
    private JsonArray fetchReleases(String apiUrl) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);

        try {
            if (conn.getResponseCode() != 200) return null;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            return gson.fromJson(sb.toString(), JsonArray.class);
        } finally {
            conn.disconnect();
        }
    }

    /**
     * 从 tag_name 提取 SDK 版本号。
     * 例: "sdk-1.5.5" → "1.5.5"
     */
    private static String extractSdkVersion(String tagName) {
        if (tagName.startsWith("sdk-")) {
            return tagName.substring(4);
        }
        return tagName;
    }

    // ==================== 多语言消息工具方法 ====================

    /**
     * 获取指定玩家的语言文本（{0} {1} 索引占位符）
     */
    private String getLang(CommandSender sender, String key, String defaultMsg, String... args) {
        String lang = (sender instanceof Player)
                ? plugin.getLanguageManager().getPlayerLanguage((Player) sender)
                : plugin.getLanguageManager().getDefaultLanguage();
        return plugin.getLanguageManager().getCoreIndexedMessage(lang, key, defaultMsg, args);
    }

    /**
     * 发送多语言消息到玩家/控制台（异步安全）
     */
    private void replyLang(CommandSender sender, String key, String defaultMsg, String... args) {
        String msg = getLang(sender, key, defaultMsg, args);
        CompatibleScheduler.runTask(plugin, () ->
                sender.sendMessage(ColorUtils.colorize(msg)));
    }

    /**
     * 直接发送已格式化的文本（异步安全）
     */
    private void replyRaw(CommandSender sender, String msg) {
        CompatibleScheduler.runTask(plugin, () ->
                sender.sendMessage(ColorUtils.colorize(msg)));
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    /**
     * 生成指定长度的随机小写字母+数字字符串，用于文件重命名后缀。
     */
    private static String generateRandomSuffix(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM_CHARS.charAt(RANDOM.nextInt(RANDOM_CHARS.length())));
        }
        return sb.toString();
    }
}
