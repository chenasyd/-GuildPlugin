package com.guild.module.cloud;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.guild.GuildPlugin;
import com.guild.core.module.ModuleManager;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.ColorUtils;
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
import java.util.List;

/**
 * GitHub 云端模块仓库
 * 从 GitHub Releases 获取模块列表，支持下载模块 jar 到本地 modules 目录
 */
public class CloudModuleRepository {

    private static final String RELEASES_API =
            "https://api.github.com/repos/chenasyd/-GuildPlugin/releases/latest";
    private static final String USER_AGENT = "GuildPlugin/CloudModuleRepository (chenasyd)";
    private static final String MODULE_PREFIX = "modules-";
    private static final int TIMEOUT = 10000;

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

    public record ModuleInfo(String name, String fileName, String downloadUrl, long size) {}

    public CloudModuleRepository(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 异步列出云端模块
     */
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
                    sender.sendMessage(ColorUtils.colorize(
                            "&e" + m.name() + " &7- " + formatSize(m.size())));
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
                        .filter(m -> m.name().equalsIgnoreCase(moduleName))
                        .findFirst().orElse(null);

                if (target == null) {
                    reply(sender, "&cModule '" + moduleName + "' not found in cloud repository.");
                    return;
                }

                File modulesDir = plugin.getServiceContainer().get(ModuleManager.class).getModulesDirectory();
                File outFile = new File(modulesDir, target.fileName());

                reply(sender, "&7Downloading " + target.fileName() + " (" + formatSize(target.size()) + ")...");

                // 下载
                HttpURLConnection conn = (HttpURLConnection) new URL(target.downloadUrl()).openConnection();
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setConnectTimeout(TIMEOUT);
                conn.setReadTimeout(60000); // 下载超时 60s

                try (InputStream in = conn.getInputStream()) {
                    Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    conn.disconnect();
                }

                reply(sender, "&aDownloaded " + target.fileName()
                        + ". Use &e/guildmodule load " + target.fileName() + " &ato load it.");

            } catch (Exception e) {
                reply(sender, "&cDownload failed: " + e.getMessage());
            }
        });
    }

    // ==================== 内部方法 ====================

    private List<ModuleInfo> fetchModules() {
        List<ModuleInfo> list = new ArrayList<>();
        try {
            JsonObject release = fetchRelease();
            if (release == null) return list;

            JsonArray assets = release.getAsJsonArray("assets");
            for (JsonElement e : assets) {
                JsonObject a = e.getAsJsonObject();
                String name = a.get("name").getAsString();
                if (name.startsWith(MODULE_PREFIX) && name.endsWith(".jar")) {
                    String moduleName = name.substring(MODULE_PREFIX.length(), name.length() - 4);
                    list.add(new ModuleInfo(moduleName, name,
                            a.get("browser_download_url").getAsString(),
                            a.get("size").getAsLong()));
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("[CloudModuleRepo] Fetch failed: " + ex.getMessage());
        }
        return list;
    }

    private JsonObject fetchRelease() throws IOException {
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
            return gson.fromJson(sb.toString(), JsonObject.class);
        } finally {
            conn.disconnect();
        }
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
}
