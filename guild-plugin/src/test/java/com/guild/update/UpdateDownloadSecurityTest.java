package com.guild.update;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateDownloadSecurityTest {

    @TempDir
    Path tempDir;

    @Test
    void isAllowedDownloadUrl_acceptsGitHubAndModrinthHttps() {
        assertTrue(UpdateDownloadSecurity.isAllowedDownloadUrl(
                "https://github.com/chenasyd/-GuildPlugin/releases/download/v1.0/guild-plugin-1.0.jar"));
        assertTrue(UpdateDownloadSecurity.isAllowedDownloadUrl(
                "https://objects.githubusercontent.com/github-production-release-asset-2e65be/123/guild.jar"));
        assertTrue(UpdateDownloadSecurity.isAllowedDownloadUrl(
                "https://cdn.modrinth.com/data/abc/versions/1.0/guild-plugin-1.0.jar"));
    }

    @Test
    void isAllowedDownloadUrl_rejectsHttpAndUnknownHosts() {
        assertFalse(UpdateDownloadSecurity.isAllowedDownloadUrl(
                "http://github.com/chenasyd/-GuildPlugin/releases/download/v1.0/guild.jar"));
        assertFalse(UpdateDownloadSecurity.isAllowedDownloadUrl(
                "https://evil.example.com/guild.jar"));
        assertFalse(UpdateDownloadSecurity.isAllowedDownloadUrl(null));
        assertFalse(UpdateDownloadSecurity.isAllowedDownloadUrl("   "));
        assertFalse(UpdateDownloadSecurity.isAllowedDownloadUrl("not-a-url"));
    }

    @Test
    void looksLikeJar_detectsZipMagic() throws IOException {
        Path jarLike = tempDir.resolve("valid.jar");
        Files.write(jarLike, UpdateDownloadSecurity.JAR_MAGIC);

        Path notJar = tempDir.resolve("invalid.jar");
        Files.write(notJar, new byte[]{0x00, 0x01, 0x02, 0x03});

        assertTrue(UpdateDownloadSecurity.looksLikeJar(jarLike));
        assertFalse(UpdateDownloadSecurity.looksLikeJar(notJar));
        assertFalse(UpdateDownloadSecurity.looksLikeJar(tempDir.resolve("missing.jar")));
    }

    @Test
    void verifySha512_matchesExpectedHash() throws Exception {
        Path file = tempDir.resolve("artifact.jar");
        byte[] content = "guild-plugin-update-payload".getBytes();
        Files.write(file, content);

        String expected = sha512Hex(content);
        assertTrue(UpdateDownloadSecurity.verifySha512(file, expected));
        assertFalse(UpdateDownloadSecurity.verifySha512(file, expected + "0"));
    }

    @Test
    void verifySha512_skipsWhenExpectedMissing() throws IOException {
        Path file = tempDir.resolve("artifact.jar");
        Files.write(file, UpdateDownloadSecurity.JAR_MAGIC);
        assertTrue(UpdateDownloadSecurity.verifySha512(file, null));
        assertTrue(UpdateDownloadSecurity.verifySha512(file, "  "));
    }

    private static String sha512Hex(byte[] content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        return HexFormat.of().formatHex(digest.digest(content));
    }
}
