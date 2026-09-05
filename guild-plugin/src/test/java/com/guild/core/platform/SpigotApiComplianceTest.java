package com.guild.core.platform;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 编译期约束：主源码不得直接 import Paper API（Folia/Paper 特性须反射探测）。
 */
class SpigotApiComplianceTest {

    private static final List<String> FORBIDDEN_IMPORT_PREFIXES = List.of(
            "import io.papermc.",
            "import com.destroystokyo.paper."
    );

    @Test
    void mainSourcesMustNotImportPaperApi() throws IOException {
        Path mainJavaRoot = resolveMainJavaRoot();
        List<String> violations = new ArrayList<>();

        Files.walkFileTree(mainJavaRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!file.toString().endsWith(".java")) {
                    return FileVisitResult.CONTINUE;
                }
                for (String line : Files.readAllLines(file)) {
                    String trimmed = line.trim();
                    for (String prefix : FORBIDDEN_IMPORT_PREFIXES) {
                        if (trimmed.startsWith(prefix)) {
                            violations.add(mainJavaRoot.relativize(file) + ": " + trimmed);
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        assertTrue(violations.isEmpty(),
                () -> "主源码检测到 Paper API import，请改用 spigot-api 或 Class.forName 反射：\n"
                        + String.join("\n", violations));
    }

    private static Path resolveMainJavaRoot() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        Path direct = cwd.resolve("src/main/java");
        if (Files.isDirectory(direct)) {
            return direct;
        }
        Path nested = cwd.resolve("guild-plugin/src/main/java");
        if (Files.isDirectory(nested)) {
            return nested;
        }
        throw new IllegalStateException("Cannot locate src/main/java from " + cwd);
    }
}
