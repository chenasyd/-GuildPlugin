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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plugin update manager with dual-source version checking and automatic download.
 * <p>
 * Queries BOTH GitHub and Modrinth simultaneously and returns the highest version found.
 * Supports non-standard version formats including pre-release suffixes ({@code x.x.x-snapshot.N})
 * and third-party fork markers ({@code x.x.x-forkname.N}).
 * <p>
 * Version naming convention:
 * <ul>
 *   <li>Official release: {@code 1.6.5} or {@code v1.6.5}</li>
 *   <li>Official pre-release: {@code 1.6.6-snapshot.2} or {@code v1.6.6-snapshot.2}</li>
 *   <li>Third-party fork: {@code 1.6.4-elaria.1} (not officially maintained)</li>
 * </ul>
 */
public class UpdateManager {

    private static final String GITHUB_API_URL = "https://api.github.com/repos/chenasyd/-GuildPlugin/releases";
    private static final String MODRINTH_API_URL = "https://api.modrinth.com/v2/project/8mvSrFJf/version";
    private static final String USER_AGENT = "GuildPlugin/UpdateManager (chenasyd)";
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 15000;
    private static final int DOWNLOAD_TIMEOUT = 60000;

    /**
     * Official version pattern (after stripping optional v prefix):
     * MAJOR.MINOR.PATCH or MAJOR.MINOR.PATCH-snapshot.N
     */
    private static final Pattern OFFICIAL_VERSION_PATTERN =
            Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(?:-(snapshot)\\.(\\d+))?$");

    /**
     * Any recognizable version pattern (including third-party forks):
     * MAJOR.MINOR.PATCH[-suffix.N]
     */
    private static final Pattern ANY_VERSION_PATTERN =
            Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(?:-([a-zA-Z][a-zA-Z0-9]*)\\.(\\d+))?$");

    private final JavaPlugin plugin;
    private final Logger logger;
    private final Gson gson;

    public UpdateManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.gson = new Gson();
    }

    // ==================== Version Parsing ====================

    /**
     * Parsed representation of a plugin version string.
     * Supports: x.x.x, x.x.x-snapshot.N, x.x.x-forkname.N
     */
    public static class PluginVersion implements Comparable<PluginVersion> {
        public final int major;
        public final int minor;
        public final int patch;
        /** Pre-release suffix name (e.g. "snapshot", "elaria"), or null for release */
        public final String suffix;
        /** Pre-release suffix number, or 0 if no suffix */
        public final int suffixNum;
        /** Whether this matches the official naming convention */
        public final boolean official;
        /** The original normalized string (without v prefix) */
        public final String raw;

        public PluginVersion(int major, int minor, int patch, String suffix, int suffixNum, String raw) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.suffix = suffix;
            this.suffixNum = suffixNum;
            this.official = (suffix == null || "snapshot".equalsIgnoreCase(suffix));
            this.raw = raw;
        }

        /**
         * Whether this is a release version (no pre-release suffix).
         */
        public boolean isRelease() {
            return suffix == null;
        }

        /**
         * Whether this is an official pre-release (snapshot).
         */
        public boolean isSnapshot() {
            return "snapshot".equalsIgnoreCase(suffix);
        }

        /**
         * Whether this is a third-party fork version.
         */
        public boolean isFork() {
            return suffix != null && !"snapshot".equalsIgnoreCase(suffix);
        }

        @Override
        public int compareTo(PluginVersion other) {
            // Compare base version: MAJOR.MINOR.PATCH
            int cmp = Integer.compare(this.major, other.major);
            if (cmp != 0) return cmp;
            cmp = Integer.compare(this.minor, other.minor);
            if (cmp != 0) return cmp;
            cmp = Integer.compare(this.patch, other.patch);
            if (cmp != 0) return cmp;

            // Same base version — compare suffixes:
            // release (no suffix) > snapshot > fork
            if (this.suffix == null && other.suffix == null) return 0;
            if (this.suffix == null) return 1;   // release > any suffix
            if (other.suffix == null) return -1;  // any suffix < release

            // Both have suffixes
            boolean thisSnapshot = "snapshot".equalsIgnoreCase(this.suffix);
            boolean otherSnapshot = "snapshot".equalsIgnoreCase(other.suffix);
            if (thisSnapshot && !otherSnapshot) return 1;   // snapshot > fork
            if (!thisSnapshot && otherSnapshot) return -1;  // fork < snapshot

            // Same suffix type — compare suffix number
            cmp = Integer.compare(this.suffixNum, other.suffixNum);
            if (cmp != 0) return cmp;

            // Both are forks with same number — compare suffix name alphabetically
            return this.suffix.compareToIgnoreCase(other.suffix);
        }

        @Override
        public String toString() {
            return raw;
        }
    }

    /**
     * Parse a version string into a PluginVersion object.
     * Handles optional v/V prefix and various suffix formats.
     *
     * @param versionStr the version string (e.g. "v1.6.5", "1.6.6-snapshot.2", "1.6.4-elaria.1")
     * @return parsed PluginVersion, or null if completely unparseable
     */
    public static PluginVersion parseVersion(String versionStr) {
        if (versionStr == null || versionStr.isEmpty()) return null;

        String normalized = stripVPrefix(versionStr).trim();
        Matcher m = ANY_VERSION_PATTERN.matcher(normalized);
        if (!m.matches()) return null;

        int major = Integer.parseInt(m.group(1));
        int minor = Integer.parseInt(m.group(2));
        int patch = Integer.parseInt(m.group(3));
        String suffix = m.group(4); // may be null
        int suffixNum = m.group(5) != null ? Integer.parseInt(m.group(5)) : 0;

        return new PluginVersion(major, minor, patch, suffix, suffixNum, normalized);
    }

    /**
     * Validate whether a local version string follows the official naming convention.
     * Logs a warning if the version appears to be a third-party fork or is unrecognizable.
     *
     * @param localVersion the version from plugin.yml
     * @return true if official format, false otherwise
     */
    public boolean validateLocalVersion(String localVersion) {
        String normalized = stripVPrefix(localVersion).trim();

        Matcher officialMatcher = OFFICIAL_VERSION_PATTERN.matcher(normalized);
        if (officialMatcher.matches()) {
            return true;
        }

        // Check if it's a recognizable fork version
        Matcher anyMatcher = ANY_VERSION_PATTERN.matcher(normalized);
        if (anyMatcher.matches()) {
            String suffix = anyMatcher.group(4);
            logger.warning("[UpdateManager] Current plugin version \"" + localVersion + "\" uses suffix \"-"
                    + suffix + "." + anyMatcher.group(5) + "\" which differs from the official naming convention.");
            logger.warning("[UpdateManager] This appears to be a third-party fork version. "
                    + "It is NOT officially maintained and may not receive updates.");
            logger.warning("[UpdateManager] Official version format: x.x.x (release) or x.x.x-snapshot.N (pre-release).");
        } else {
            logger.warning("[UpdateManager] Current plugin version \"" + localVersion
                    + "\" does not match any recognized version format.");
            logger.warning("[UpdateManager] Expected format: [v]x.x.x or [v]x.x.x-snapshot.N "
                    + "(e.g. 1.6.5, v1.6.5, 1.6.6-snapshot.2)");
            logger.warning("[UpdateManager] This may be a modified third-party version not officially maintained.");
        }
        return false;
    }

    // ==================== Version Info ====================

    /**
     * Version information returned by update checks.
     */
    public static class VersionInfo {
        public final String version;
        public final String changelog;
        public final String downloadUrl;
        public final String fileName;
        public final String source;
        public final PluginVersion parsedVersion;

        public VersionInfo(String version, String changelog, String downloadUrl, String fileName, String source) {
            this.version = version;
            this.changelog = changelog;
            this.downloadUrl = downloadUrl;
            this.fileName = fileName;
            this.source = source;
            this.parsedVersion = parseVersion(version);
        }

        @Override
        public String toString() {
            return source + " v" + version;
        }
    }

    // ==================== Dual-Source Check ====================

    /**
     * Check latest version from BOTH GitHub and Modrinth simultaneously.
     * Returns the highest version found across both sources.
     *
     * @return latest version info, or null if both sources fail
     */
    public VersionInfo checkLatestVersion() {
        VersionInfo ghInfo = checkGitHub();
        VersionInfo mrInfo = checkModrinth();

        if (ghInfo == null && mrInfo == null) {
            return null;
        }
        if (ghInfo == null) {
            logger.info("[UpdateManager] GitHub unavailable, using Modrinth result.");
            return mrInfo;
        }
        if (mrInfo == null) {
            logger.info("[UpdateManager] Modrinth unavailable, using GitHub result.");
            return ghInfo;
        }

        // Both sources returned results — pick the higher version
        PluginVersion ghVer = ghInfo.parsedVersion;
        PluginVersion mrVer = mrInfo.parsedVersion;

        if (ghVer == null && mrVer == null) return ghInfo;
        if (ghVer == null) return mrInfo;
        if (mrVer == null) return ghInfo;

        if (ghVer.compareTo(mrVer) >= 0) {
            logger.info("[UpdateManager] Dual-source check: GitHub=" + ghInfo.version
                    + ", Modrinth=" + mrInfo.version + " -> using GitHub");
            return ghInfo;
        } else {
            logger.info("[UpdateManager] Dual-source check: GitHub=" + ghInfo.version
                    + ", Modrinth=" + mrInfo.version + " -> using Modrinth");
            return mrInfo;
        }
    }

    // ==================== GitHub API ====================

    /**
     * Fetch all releases from GitHub and find the highest version with a plugin JAR.
     * Handles tag_name with or without "v" prefix.
     */
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
            JsonArray releases = gson.fromJson(json, JsonArray.class);

            VersionInfo best = null;
            PluginVersion bestVersion = null;

            for (int i = 0; i < releases.size(); i++) {
                JsonObject release = releases.get(i).getAsJsonObject();

                // Skip drafts
                if (release.has("draft") && release.get("draft").getAsBoolean()) continue;

                String tagName = release.get("tag_name").getAsString();
                String version = stripVPrefix(tagName);

                // Must be a parseable version
                PluginVersion parsed = parseVersion(version);
                if (parsed == null) continue;

                // Find plugin JAR in assets
                JsonArray assets = release.getAsJsonArray("assets");
                if (assets == null) continue;

                String downloadUrl = null;
                String fileName = null;
                for (int j = 0; j < assets.size(); j++) {
                    JsonObject asset = assets.get(j).getAsJsonObject();
                    String name = asset.get("name").getAsString();
                    if (name.startsWith("guild-plugin-") && name.endsWith(".jar")
                            && !name.contains("original")) {
                        downloadUrl = asset.get("browser_download_url").getAsString();
                        fileName = name;
                        break;
                    }
                }

                if (downloadUrl == null) continue;

                // Track the highest version
                if (bestVersion == null || parsed.compareTo(bestVersion) > 0) {
                    String changelog = release.has("body") && !release.get("body").isJsonNull()
                            ? release.get("body").getAsString() : "";
                    best = new VersionInfo(version, changelog, downloadUrl, fileName, "GitHub");
                    bestVersion = parsed;
                }
            }

            if (best == null) {
                logger.warning("[UpdateManager] No plugin JAR found in any GitHub release");
            }
            return best;

        } catch (SocketTimeoutException e) {
            logger.warning("[UpdateManager] GitHub API timed out");
        } catch (IOException e) {
            logger.warning("[UpdateManager] GitHub check failed: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    // ==================== Modrinth API ====================

    /**
     * Fetch all versions from Modrinth and find the highest version.
     * Considers ALL version types (release, alpha, beta) since snapshots are published as alpha.
     */
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

            VersionInfo best = null;
            PluginVersion bestVersion = null;

            for (int i = 0; i < versions.size(); i++) {
                JsonObject v = versions.get(i).getAsJsonObject();

                // version_number has no "v" prefix, e.g. "1.6.5" or "1.6.6-snapshot.2"
                String version = v.get("version_number").getAsString();
                PluginVersion parsed = parseVersion(version);
                if (parsed == null) continue;

                // Must have files
                JsonArray files = v.getAsJsonArray("files");
                if (files == null || files.size() == 0) continue;

                // Find primary file (or first file)
                JsonObject primaryFile = null;
                for (int j = 0; j < files.size(); j++) {
                    JsonObject f = files.get(j).getAsJsonObject();
                    if (f.has("primary") && f.get("primary").getAsBoolean()) {
                        primaryFile = f;
                        break;
                    }
                }
                if (primaryFile == null) {
                    primaryFile = files.get(0).getAsJsonObject();
                }

                String fileName = primaryFile.get("filename").getAsString();
                // Only consider plugin JARs (skip bungee JARs)
                if (!fileName.startsWith("guild-plugin-")) continue;

                // Track the highest version
                if (bestVersion == null || parsed.compareTo(bestVersion) > 0) {
                    String changelog = v.has("changelog") && !v.get("changelog").isJsonNull()
                            ? v.get("changelog").getAsString() : "";
                    String downloadUrl = primaryFile.get("url").getAsString();
                    best = new VersionInfo(version, changelog, downloadUrl, fileName, "Modrinth");
                    bestVersion = parsed;
                }
            }

            if (best == null) {
                logger.warning("[UpdateManager] No plugin version found on Modrinth");
            }
            return best;

        } catch (SocketTimeoutException e) {
            logger.warning("[UpdateManager] Modrinth API timed out");
        } catch (IOException e) {
            logger.warning("[UpdateManager] Modrinth check failed: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    // ==================== Download ====================

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

    // ==================== Utility ====================

    /**
     * Remove the "v" or "V" prefix from a version string.
     * Handles both "v1.6.5" and "1.6.5" formats transparently.
     */
    public static String stripVPrefix(String version) {
        if (version == null) return "";
        return version.startsWith("v") || version.startsWith("V")
                ? version.substring(1) : version;
    }

    /**
     * Compare two version strings with full support for pre-release suffixes.
     * <p>
     * Comparison rules:
     * <ol>
     *   <li>Compare MAJOR.MINOR.PATCH numerically</li>
     *   <li>If base equal: release (no suffix) &gt; snapshot.N &gt; fork.N</li>
     *   <li>If same suffix type: compare suffix number</li>
     * </ol>
     * <p>
     * Both inputs may have an optional "v" prefix which is stripped automatically.
     *
     * @return negative if v1 &lt; v2, 0 if equal, positive if v1 &gt; v2
     */
    public static int compareVersions(String v1, String v2) {
        PluginVersion pv1 = parseVersion(v1);
        PluginVersion pv2 = parseVersion(v2);

        // If either is unparseable, fall back to legacy numeric comparison
        if (pv1 == null || pv2 == null) {
            return compareVersionsLegacy(v1, v2);
        }
        return pv1.compareTo(pv2);
    }

    /**
     * Legacy version comparison — splits on "." and compares numeric parts.
     * Used as fallback when version strings don't match the expected pattern.
     */
    private static int compareVersionsLegacy(String v1, String v2) {
        String s1 = stripVPrefix(v1);
        String s2 = stripVPrefix(v2);
        String[] parts1 = s1.split("\\.");
        String[] parts2 = s2.split("\\.");
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
