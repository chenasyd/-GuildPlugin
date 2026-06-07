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
 * GitHub 云端模块仓库
 * 从 chenasyd/GuildPlugin Releases 获取所有模块 jar，支持下载到本地 modules 目录。
 * <p>
 * tag_name 格式为 {@code sdk-1.5.5}，其中 SDK 版本号仅作为信息展示，默认向后兼容，
 * 实际 SDK 版本不影响模块兼容性。
 */
public class CloudModuleRepository {

    /** 模块仓库 — 获取全部 Release（每个 release 的 tag 代表 SDK 版本） */
    private static final String RELEASES_API =
            "https://api.github.com/repos/chenasyd/GuildPlugin/releases";
    private static final String USER_AGENT = "GuildPlugin/CloudModuleRepository (chenasyd)";
    private static final int TIMEOUT = 10000;
    /** 当目标文件已存在时，用于生成随机后缀的字符池 */
    private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random RANDOM = new Random();

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

    private final GuildPlugin plugin;
    private final Gson gson = new Gson();

    /**
     * @param name         模块显示名（去掉 .jar 后缀的文件名）
     * @param fileName     原始文件名（如 modules-announcement.jar）
     * @param downloadUrl  浏览器下载链接
     * @param size         文件大小（字节）
     * @param sdkVersion   对应的 SDK 版本号（从 tag_name 剥离 sdk- 前缀），仅作信息展示
     */
    public record ModuleInfo(String name, String fileName, String downloadUrl,
                             long size, String sdkVersion) {}

    public CloudModuleRepository(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 异步列出云端模块 — 遍历所有 Release，按文件名去重取最新 SDK 版本
     */
    /** 最低兼容 API 版本 — 低于此版本的模块不建议下载使用 */
    private static final String MINIMUM_COMPATIBLE_API = "1.6.3";

    public void listModules(CommandSender sender) {
        CompatibleScheduler.runTaskAsync(plugin, () -> {
            List<ModuleInfo> modules = fetchModules();
            CompatibleScheduler.runTask(plugin, () -> {
                if (modules.isEmpty()) {
                    sender.sendMessage(ColorUtils.colorize("&cNo cloud modules found."));
                    return;
                }
                sender.sendMessage(ColorUtils.colorize("&b&l=== Cloud Modules ==="));
                for (ModuleInfo m : modules) {
                    boolean outdated = m.sdkVersion() == null || m.sdkVersion().isEmpty()
                            || UpdateManager.compareVersions(m.sdkVersion(), MINIMUM_COMPATIBLE_API) < 0;
                    if (outdated) {
                        sender.sendMessage(ColorUtils.colorize(
                                "&c" + m.fileName() + " &7- " + formatSize(m.size())
                                + " &8(sdk: " + m.sdkVersion() + ")"
                                + " &c&l[不推荐] &7需要 SDK " + MINIMUM_COMPATIBLE_API + "+"));
                    } else {
                        sender.sendMessage(ColorUtils.colorize(
                                "&a" + m.fileName() + " &7- " + formatSize(m.size())
                                + " &8(sdk: " + m.sdkVersion() + ")"));
                    }
                }
            });
        });
    }

    /**
     * 异步下载指定云端模块
     */
    public void downloadModule(CommandSender sender, String moduleName) {
        CompatibleScheduler.runTaskAsync(plugin, () -> {
            try {
                List<ModuleInfo> modules = fetchModules();
                ModuleInfo target = modules.stream()
                        .filter(m -> m.name().equalsIgnoreCase(moduleName)
                                || m.fileName().equalsIgnoreCase(moduleName)
                                || (m.fileName().equalsIgnoreCase(moduleName + ".jar")))
                        .findFirst().orElse(null);

                if (target == null) {
                    reply(sender, "&cModule '" + moduleName + "' not found in cloud repository.");
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

                reply(sender, "&7Downloading " + outFile.getName() + " ("
                        + formatSize(target.size()) + ", sdk: " + target.sdkVersion() + ")...");

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
                    reply(sender, "&aDownloaded as &e" + outFile.getName()
                            + " &a(saved with new name because &e" + target.fileName()
                            + " &ais in use).");
                    reply(sender, "&6Tip: &7Unload the old module first, then replace the jar file: "
                            + "&e/guildmodule unload <id> &7-> replace jar -> "
                            + "&e/guildmodule load " + outFile.getName());
                } else {
                    reply(sender, "&aDownloaded " + outFile.getName()
                            + ". Use &e/guildmodule load " + outFile.getName()
                            + " &ato load it.");
                }

            } catch (Exception e) {
                reply(sender, "&cDownload failed: " + e.getMessage());
            }
        });
    }

    // ==================== 内部方法 ====================

    /**
     * 拉取所有 Release，按文件名去重（最新 SDK 版本优先）。
     * GitHub releases API 默认按创建时间倒序，因此第一个出现的 release 即是最新。
     */
    private List<ModuleInfo> fetchModules() {
        Map<String, ModuleInfo> dedup = new LinkedHashMap<>();
        try {
            JsonArray releases = fetchReleases();
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
                            sdkVersion));
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("[CloudModuleRepo] Fetch failed: " + ex.getMessage());
        }
        return new ArrayList<>(dedup.values());
    }

    /**
     * 请求全部 Release（返回 JSON 数组）
     */
    private JsonArray fetchReleases() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(RELEASES_API).openConnection();
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

    private void reply(CommandSender sender, String msg) {
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
