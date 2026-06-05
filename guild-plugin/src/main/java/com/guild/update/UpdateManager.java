package com.guild.update;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/**
 * Plugin update manager with dual-source version checking and automatic download.
 * <p>
 * Prioritizes GitHub API with automatic fallback to Modrinth on timeout or failure.
 * Supports both version checking and JAR download to the plugins folder.
 */
public class UpdateManager {

    private static final String GITHUB_API_URL = "https://api.github.com/repos/chenasyd/-GuildPlugin/releases/latest";
    private static final String MODRINTH_API_URL = "https://api.modrinth.com/v2/project/8mvSrFJf/version";
    private static final String USER_AGENT = "GuildPlugin/UpdateManager (chenasyd)";
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 15000;
    private static final int DOWNLOAD_TIMEOUT = 60000;

    private final JavaPlugin plugin;
    private final Logger logger;
    private final Gson gson;

    public UpdateManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.gson = new Gson();
    }

    /**
     * Version information returned by update checks.
     */
    public static class VersionInfo {
        public final String version;
        public final String changelog;
        public final String downloadUrl;
        public final String fileName;
        public final String source;

        public VersionInfo(String version, String changelog, String downloadUrl, String fileName, String source) {
            this.version = version;
            this.changelog = changelog;
            this.downloadUrl = downloadUrl;
            this.fileName = fileName;
            this.source = source;
        }

        @Override
        public String toString() {
            return source + " v" + version;
        }
    }

    /**
     * Check latest version. Tries GitHub first, falls back to Modrinth.
     *
     * @return latest version info, or null if both sources fail
     */
    public VersionInfo checkLatestVersion() {
        VersionInfo ghInfo = checkGitHub();
        if (ghInfo != null) {
            return ghInfo;
        }

        logger.info("[UpdateManager] GitHub unavailable, falling back to Modrinth...");
        return checkModrinth();
    }

    // ---- GitHub API ----

    private VersionInfo checkGitHub() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(GITHUB_API_URL);
            conn = openConnection(url, CONNECT_TIMEOUT, READ_TIMEOUT);

            int code = conn.getResponseCode();
            if (code != 200) {
                logger.warning("[UpdateManager] GitHub returned HTTP " + code);
                return null;
            }

            String json = readResponseBody(conn);
            JsonObject release = gson.fromJson(json, JsonObject.class);

            // tag_name always has "v" prefix, e.g. "v1.5.5"
            String tagName = release.get("tag_name").getAsString();
            String version = stripVPrefix(tagName);
            String changelog = release.has("body") && !release.get("body").isJsonNull()
                    ? release.get("body").getAsString() : "";

            // Find plugin JAR in assets (filter out SDK, modules, original)
            JsonArray assets = release.getAsJsonArray("assets");
            for (int i = 0; i < assets.size(); i++) {
                JsonObject asset = assets.get(i).getAsJsonObject();
                String name = asset.get("name").getAsString();
                if (name.startsWith("guild-plugin-") && name.endsWith(".jar")
                        && !name.contains("original")) {
                    String downloadUrl = asset.get("browser_download_url").getAsString();
                    return new VersionInfo(version, changelog, downloadUrl, name, "GitHub");
                }
            }

            logger.warning("[UpdateManager] No plugin JAR found in GitHub assets");

        } catch (SocketTimeoutException e) {
            logger.warning("[UpdateManager] GitHub API timed out");
        } catch (IOException e) {
            logger.warning("[UpdateManager] GitHub check failed: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    // ---- Modrinth API ----

    private VersionInfo checkModrinth() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(MODRINTH_API_URL);
            conn = openConnection(url, CONNECT_TIMEOUT, READ_TIMEOUT);

            int code = conn.getResponseCode();
            if (code != 200) {
                logger.warning("[UpdateManager] Modrinth returned HTTP " + code);
                return null;
            }

            String json = readResponseBody(conn);
            JsonArray versions = gson.fromJson(json, JsonArray.class);

            // List is sorted by date_published descending; pick first release
            for (int i = 0; i < versions.size(); i++) {
                JsonObject v = versions.get(i).getAsJsonObject();
                String type = v.has("version_type") ? v.get("version_type").getAsString() : "";
                if (!"release".equals(type)) continue;

                // version_number has no "v" prefix, e.g. "1.5.5"
                String version = v.get("version_number").getAsString();
                String changelog = v.has("changelog") && !v.get("changelog").isJsonNull()
                        ? v.get("changelog").getAsString() : "";

                JsonArray files = v.getAsJsonArray("files");
                if (files.isEmpty()) break;

                JsonObject file = files.get(0).getAsJsonObject();
                String downloadUrl = file.get("url").getAsString();
                String fileName = file.get("filename").getAsString();

                return new VersionInfo(version, changelog, downloadUrl, fileName, "Modrinth");
            }

            logger.warning("[UpdateManager] No release version found on Modrinth");

        } catch (SocketTimeoutException e) {
            logger.warning("[UpdateManager] Modrinth API timed out");
        } catch (IOException e) {
            logger.warning("[UpdateManager] Modrinth check failed: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    // ---- Download ----

    /**
     * Download the update JAR to the plugins folder.
     *
     * @param info   version info from checkLatestVersion()
     * @param sender command sender to receive progress messages, may be null
     * @return the downloaded file, or null on failure
     */
    public File downloadUpdate(VersionInfo info, CommandSender sender) {
        File pluginsFolder = plugin.getDataFolder().getParentFile();
        String targetName = "GuildPlugin-" + info.version + ".jar";
        File targetFile = new File(pluginsFolder, targetName);

        if (sender != null) {
            sender.sendMessage("[GuildPlugin] Downloading from " + info.source + "...");
            sender.sendMessage("[GuildPlugin] File: " + info.fileName);
        }

        HttpURLConnection conn = null;
        try {
            URL url = new URL(info.downloadUrl);
            conn = openConnection(url, CONNECT_TIMEOUT, DOWNLOAD_TIMEOUT);
            conn.setInstanceFollowRedirects(true);

            int code = conn.getResponseCode();
            if (code != 200) {
                if (sender != null) sender.sendMessage("[GuildPlugin] Download failed: HTTP " + code);
                logger.warning("[UpdateManager] Download returned HTTP " + code);
                return null;
            }

            long contentLength = conn.getContentLengthLong();
            if (sender != null && contentLength > 0) {
                sender.sendMessage("[GuildPlugin] Size: " + formatSize(contentLength));
            }

            // Download to temp file, then move to destination
            Path tempFile = Files.createTempFile("guild-update-", ".jar");
            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            long actualSize = Files.size(tempFile);
            if (contentLength > 0 && actualSize != contentLength) {
                Files.delete(tempFile);
                if (sender != null) sender.sendMessage("[GuildPlugin] Download incomplete, aborting.");
                return null;
            }

            // Remove old JARs — both pattern-matched and renamed
            List<String> manualCleanup = scanAndRemoveAllOldJars(pluginsFolder, info.version, sender);

            // Move to plugins folder
            Files.move(tempFile, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            logger.info("[UpdateManager] Downloaded " + targetName + " (" + formatSize(actualSize) + ")");
            if (sender != null) {
                sender.sendMessage("[GuildPlugin] Saved: " + targetName + " (" + formatSize(actualSize) + ")");
                if (!manualCleanup.isEmpty()) {
                    sender.sendMessage("[GuildPlugin] §c§lWARNING: §cThe following old JARs could not be deleted (file may be locked):");
                    for (String name : manualCleanup) {
                        sender.sendMessage("[GuildPlugin] §c  - " + name);
                    }
                    sender.sendMessage("[GuildPlugin] §cPlease manually delete them before restarting the server!");
                }
                sender.sendMessage("[GuildPlugin] §aRestart the server to apply the update.");
            }

            return targetFile;

        } catch (Exception e) {
            if (sender != null) sender.sendMessage("[GuildPlugin] Download failed: " + e.getMessage());
            logger.severe("[UpdateManager] Download failed: " + e.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // ---- Utility ----

    /**
     * Remove the "v" prefix from a GitHub tag name.
     * Modrinth version numbers already lack the prefix.
     */
    static String stripVPrefix(String version) {
        if (version == null) return "";
        return version.startsWith("v") || version.startsWith("V")
                ? version.substring(1) : version;
    }

    /**
     * Compare two semantic version strings.
     *
     * @return -1 if v1 &lt; v2, 0 if equal, 1 if v1 &gt; v2
     */
    public static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int maxLen = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLen; i++) {
            int num1 = i < parts1.length ? parseNumericPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseNumericPart(parts2[i]) : 0;
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

    private static int parseNumericPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private HttpURLConnection openConnection(URL url, int connectTimeout, int readTimeout) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        return conn;
    }

    private String readResponseBody(HttpURLConnection conn) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /**
     * Scan and remove all old GuildPlugin JARs, including renamed ones.
     * Standard pattern-matched JARs are deleted first, then a deeper scan
     * of all JARs checks plugin.yml to detect renamed copies.
     *
     * @param pluginsFolder the plugins directory
     * @param newVersion    the newly downloaded version (to keep)
     * @param sender        command sender for progress messages
     * @return list of JAR filenames that could not be deleted (need manual cleanup)
     */
    private List<String> scanAndRemoveAllOldJars(File pluginsFolder, String newVersion, CommandSender sender) {
        List<String> needsManualCleanup = new ArrayList<>();
        if (pluginsFolder == null || !pluginsFolder.isDirectory()) return needsManualCleanup;

        // Step 1: Standard pattern-based deletion (GuildPlugin-x.y.z.jar)
        File[] stdJars = pluginsFolder.listFiles((dir, name) ->
                name.matches("[Gg]uild[Pp]lugin-\\d+\\.\\d+.*\\.jar"));
        if (stdJars != null) {
            for (File old : stdJars) {
                if (old.getName().contains(newVersion)) continue;
                tryDeleteOrWarn(old, "pattern-matched", needsManualCleanup, sender);
            }
        }

        // Step 2: Deep scan — check plugin.yml in all remaining JARs
        File[] allJars = pluginsFolder.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".jar"));
        if (allJars != null) {
            for (File jar : allJars) {
                // Skip the newly downloaded file
                if (jar.getName().contains(newVersion)) continue;
                // Skip files already handled in step 1 (standard naming)
                if (jar.getName().matches("[Gg]uild[Pp]lugin-\\d+\\.\\d+.*\\.jar")) continue;

                if (isGuildPluginJar(jar)) {
                    tryDeleteOrWarn(jar, "renamed", needsManualCleanup, sender);
                }
            }
        }

        return needsManualCleanup;
    }

    /**
     * Try to delete a file; if it fails, add to manual cleanup list and warn.
     */
    private void tryDeleteOrWarn(File file, String reason, List<String> needsCleanup, CommandSender sender) {
        try {
            if (file.delete()) {
                logger.info("[UpdateManager] Removed " + reason + " old JAR: " + file.getName());
                if (sender != null) {
                    sender.sendMessage("[GuildPlugin] §aRemoved old JAR: §f" + file.getName());
                }
            } else {
                needsCleanup.add(file.getName());
                logger.warning("[UpdateManager] Could not delete " + file.getName() + " (file may be locked)");
            }
        } catch (Exception e) {
            needsCleanup.add(file.getName());
            logger.warning("[UpdateManager] Could not remove " + file.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Check whether a JAR file is a GuildPlugin by reading its plugin.yml.
     */
    private boolean isGuildPluginJar(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry("plugin.yml");
            if (entry == null) return false;
            try (InputStream is = jar.getInputStream(entry);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    // Match "main: com.guild.GuildPlugin" (with or without quotes, any spacing)
                    if (trimmed.startsWith("main:") && trimmed.contains("com.guild.GuildPlugin")) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
            // Can't read the JAR — not a concern, just skip
        }
        return false;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
