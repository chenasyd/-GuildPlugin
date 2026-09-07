package com.guild.core.platform;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 运行时仅 Spigot API；Paper 依赖若存在，必须限定为 test scope（与 Enforcer 策略一致）。
 */
class PaperTestScopeComplianceTest {

    private static final Pattern PAPER_DEPENDENCY = Pattern.compile(
            "<dependency>[\\s\\S]*?<groupId>io\\.papermc\\.paper</groupId>[\\s\\S]*?</dependency>",
            Pattern.MULTILINE);

    @Test
    void pomPaperDependenciesAreTestScopedOnly() throws IOException {
        Path pom = resolvePluginPom();
        String content = Files.readString(pom);

        Matcher matcher = PAPER_DEPENDENCY.matcher(content);
        if (!matcher.find()) {
            // 未声明 paper-api 亦符合策略（MockBukkit 冒烟 + Mockito 为主）
            return;
        }
        do {
            String block = matcher.group();
            assertTrue(block.contains("<scope>test</scope>"),
                    () -> "io.papermc.paper 依赖必须声明 <scope>test</scope>，不得进入 compile/runtime/provided：\n"
                            + block);
            assertFalse(block.contains("<scope>compile</scope>"));
            assertFalse(block.contains("<scope>runtime</scope>"));
            assertFalse(block.contains("<scope>provided</scope>"));
        } while (matcher.find());
    }

    private static Path resolvePluginPom() throws IOException {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        Path direct = cwd.resolve("pom.xml");
        if (Files.isRegularFile(direct) && Files.readString(direct).contains("<artifactId>guild-plugin</artifactId>")) {
            return direct;
        }
        Path nested = cwd.resolve("guild-plugin/pom.xml");
        if (Files.isRegularFile(nested)) {
            return nested;
        }
        throw new IllegalStateException("Cannot locate guild-plugin/pom.xml from " + cwd);
    }
}
