package com.guild.update;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;

/**
 * 更新包下载安全校验：HTTPS 白名单、JAR 魔数、Modrinth SHA-512。
 */
public final class UpdateDownloadSecurity {

    static final byte[] JAR_MAGIC = {0x50, 0x4B, 0x03, 0x04};

    private static final Set<String> ALLOWED_DOWNLOAD_HOSTS = Set.of(
            "github.com",
            "objects.githubusercontent.com",
            "cdn.modrinth.com"
    );

    private UpdateDownloadSecurity() {
    }

    /**
     * 仅允许 HTTPS，且 host 在白名单内。
     */
    public static boolean isAllowedDownloadUrl(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            return false;
        }
        try {
            URL url = new URL(urlString.trim());
            if (!"https".equalsIgnoreCase(url.getProtocol())) {
                return false;
            }
            String host = url.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }
            return ALLOWED_DOWNLOAD_HOSTS.contains(host.toLowerCase(Locale.ROOT));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 校验文件头是否为 ZIP/JAR（PK\x03\x04）。
     */
    public static boolean looksLikeJar(Path file) throws IOException {
        if (file == null || !Files.isRegularFile(file) || Files.size(file) < JAR_MAGIC.length) {
            return false;
        }
        byte[] magic = new byte[JAR_MAGIC.length];
        try (InputStream in = Files.newInputStream(file)) {
            if (in.read(magic) != JAR_MAGIC.length) {
                return false;
            }
        }
        for (int i = 0; i < JAR_MAGIC.length; i++) {
            if (magic[i] != JAR_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean verifySha512(Path file, String expectedHex) throws IOException {
        if (expectedHex == null || expectedHex.isBlank()) {
            return true;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash;
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            hash = digest.digest();
            String actualHex = HexFormat.of().formatHex(hash);
            return actualHex.equalsIgnoreCase(expectedHex.trim());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 not available", e);
        }
    }
}
