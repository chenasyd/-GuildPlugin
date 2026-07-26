package com.guild.core.geyser;

import com.guild.comm.api.BungeeClientAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * 统一的玩家连接类型服务 —— 整合 GeyserAPI（直接反射）与 BungeeClientAPI（代理推送）两套检测源。
 * <p>
 * 查询优先级：
 * <ol>
 *   <li>BungeeClientAPI 缓存（信息最丰富，含 platform / inputMode）</li>
 *   <li>GeyserAPI 反射检测（仅 boolean，无需代理即可工作）</li>
 *   <li>均不可用 → 视为 Java 版玩家</li>
 * </ol>
 * <p>
 * 使用方式：
 * <pre>{@code
 *   PlayerConnectionService.initialize(logger);
 *   ...
 *   if (PlayerConnectionService.isBedrockPlayer(player)) { ... }
 *   PlayerConnectionInfo info = PlayerConnectionService.getConnectionInfo(player);
 *   ClickType adapted = PlayerConnectionService.adaptClick(player, clickType);
 * }</pre>
 */
public final class PlayerConnectionService {

    private static Logger logger;

    private PlayerConnectionService() {}

    // ── Lifecycle ────────────────────────────────────────────────

    /**
     * 初始化连接服务。应在 GuildPlugin#onEnable 中调用，
     * 位于 GeyserAPI.initialize 和 BungeeClientAPI.initialize 之后。
     */
    public static void initialize(Logger pluginLogger) {
        logger = pluginLogger;
        logger.info("[PlayerConnection] Service initialized."
                + " Geyser=" + GeyserAPI.isAvailable()
                + ", BungeeClient=" + BungeeClientAPI.isInitialized());
    }

    /**
     * 关闭连接服务，清理所有缓存。
     */
    public static void shutdown() {
        // GeyserAPI 和 BungeeClientAPI 各自管理自己的 shutdown
    }

    // ── Detection ────────────────────────────────────────────────

    /**
     * 判断玩家是否为基岩版玩家。
     *
     * @param player 目标玩家
     * @return true 如果玩家通过 Geyser 连接
     */
    public static boolean isBedrockPlayer(Player player) {
        return isBedrockPlayer(player.getUniqueId());
    }

    /**
     * 判断 UUID 对应玩家是否为基岩版玩家。
     */
    public static boolean isBedrockPlayer(UUID uuid) {
        // 优先查 Bungee 推送的缓存（信息更权威，由代理端直接检测）
        if (BungeeClientAPI.isInitialized()) {
            BungeeClientAPI.PlayerConnectionInfo info = BungeeClientAPI.getConnectionInfo(uuid);
            if (info != null) {
                return info.isBedrock();
            }
        }
        // 回退到 GeyserAPI 直接反射检测
        return GeyserAPI.isBedrockPlayer(uuid);
    }

    /**
     * 获取玩家的连接信息（合并两个来源）。
     *
     * @param player 目标玩家
     * @return 连接信息，如果无法获取则返回一个默认的 JAVA 类型信息
     */
    public static ConnectionInfo getConnectionInfo(Player player) {
        UUID uuid = player.getUniqueId();

        // 优先使用 Bungee 推送的丰富信息
        if (BungeeClientAPI.isInitialized()) {
            BungeeClientAPI.PlayerConnectionInfo bungeeInfo = BungeeClientAPI.getConnectionInfo(uuid);
            if (bungeeInfo != null) {
                return new ConnectionInfo(
                        uuid, player.getName(),
                        bungeeInfo.isBedrock(),
                        bungeeInfo.getPlatform(),
                        bungeeInfo.getInputMode(),
                        Source.BUNGEE
                );
            }
        }

        // 回退到 GeyserAPI（仅能判断 boolean）
        boolean bedrock = GeyserAPI.isBedrockPlayer(uuid);
        return new ConnectionInfo(uuid, player.getName(), bedrock, null, null,
                bedrock ? Source.GEYSER_LOCAL : Source.NONE);
    }

    /**
     * 对基岩版玩家的点击类型进行 GUI 适配。
     * <p>
     * 基岩版客户端无法可靠触发右键点击，此方法将右键映射为左键。
     * Java 版玩家原样返回。
     *
     * @param player        目标玩家
     * @param originalClick 原始点击类型
     * @return 适配后的点击类型
     */
    public static ClickType adaptClick(Player player, ClickType originalClick) {
        return BedrockGUIAdapter.adapt(player, originalClick);
    }

    // ── Detection with Logging ────────────────────────────────────

    /**
     * 执行完整的连接类型检测链并逐步输出日志。
     * <p>
     * 依次检查 BungeeClientAPI 缓存 → GeyserAPI 反射检测 → 默认 Java，
     * 每一步均输出到控制台（INFO）以便观察检测过程。
     *
     * @param player 加入服务器的玩家
     * @return 检测结果 ConnectionInfo
     */
    public static ConnectionInfo detectAndLog(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        logger.info("[PlayerConnection] === 检测开始: " + name + " (" + uuid + ") ===");

        // ── Step 1: BungeeClientAPI 缓存 ──
        if (BungeeClientAPI.isInitialized()) {
            BungeeClientAPI.PlayerConnectionInfo bungeeInfo = BungeeClientAPI.getConnectionInfo(uuid);
            if (bungeeInfo != null) {
                logger.info("[PlayerConnection] [1/2] BungeeClientAPI: 命中缓存"
                        + " → type=" + bungeeInfo.getConnectionType()
                        + ", platform=" + bungeeInfo.getPlatform()
                        + ", inputMode=" + bungeeInfo.getInputMode());
                ConnectionInfo result = new ConnectionInfo(uuid, name,
                        bungeeInfo.isBedrock(), bungeeInfo.getPlatform(),
                        bungeeInfo.getInputMode(), Source.BUNGEE);
                logger.info("[PlayerConnection] [结果] " + name + " → "
                        + (result.isBedrock() ? "BEDROCK" : "JAVA")
                        + " (来源: BungeeCord 代理推送)");
                return result;
            } else {
                logger.info("[PlayerConnection] [1/2] BungeeClientAPI: 已初始化但无该玩家缓存"
                        + "（代理尚未推送连接信息）");
            }
        } else {
            logger.info("[PlayerConnection] [1/2] BungeeClientAPI: 未初始化（跳过代理检测）");
        }

        // ── Step 2: GeyserAPI 反射检测 ──
        if (GeyserAPI.isAvailable()) {
            boolean bedrock = GeyserAPI.isBedrockPlayer(uuid);
            logger.info("[PlayerConnection] [2/2] GeyserAPI: "
                    + (bedrock ? "检测到基岩版连接 ✓" : "未检测到基岩版连接"));
            if (bedrock) {
                ConnectionInfo result = new ConnectionInfo(uuid, name,
                        true, null, null, Source.GEYSER_LOCAL);
                logger.info("[PlayerConnection] [结果] " + name + " → BEDROCK"
                        + " (来源: 本服 Geyser 反射检测)");
                return result;
            }
        } else {
            logger.info("[PlayerConnection] [2/2] GeyserAPI: 不可用（跳过本地检测）");
        }

        // ── Step 3: 默认 Java ──
        ConnectionInfo result = new ConnectionInfo(uuid, name,
                false, null, null, Source.NONE);
        logger.info("[PlayerConnection] [结果] " + name + " → JAVA (无检测源命中，默认 Java 版)");
        return result;
    }

    // ── Cache Management ─────────────────────────────────────────

    /**
     * 玩家加入时预热缓存。
     * 主动调用 GeyserAPI 检测，确保首次查询不会因懒加载而延迟。
     */
    public static void onPlayerJoin(UUID uuid) {
        // 预热 GeyserAPI 缓存（computeIfAbsent 会在首次调用时执行反射检测）
        GeyserAPI.isBedrockPlayer(uuid);
    }

    /**
     * 玩家退出时清理所有缓存。
     */
    public static void onPlayerQuit(UUID uuid) {
        GeyserAPI.onPlayerQuit(uuid);
        if (BungeeClientAPI.isInitialized()) {
            BungeeClientAPI.removeConnectionInfo(uuid);
        }
    }

    // ── Inner Classes ────────────────────────────────────────────

    /** 检测来源。 */
    public enum Source {
        /** BungeeCord 代理端推送（信息最丰富）。 */
        BUNGEE,
        /** 本服 Geyser-Spigot 直接检测（仅 boolean）。 */
        GEYSER_LOCAL,
        /** 无法检测，默认 Java。 */
        NONE
    }

    /**
     * 不可变的玩家连接信息。
     */
    public static class ConnectionInfo {
        private final UUID uuid;
        private final String name;
        private final boolean bedrock;
        private final String platform;   // "Windows", "iOS", "Android" 等（可能为 null）
        private final String inputMode;  // "TOUCH", "CONTROLLER", "KEYBOARD_MOUSE" 等（可能为 null）
        private final Source source;

        public ConnectionInfo(UUID uuid, String name, boolean bedrock,
                              String platform, String inputMode, Source source) {
            this.uuid = uuid;
            this.name = name;
            this.bedrock = bedrock;
            this.platform = platform;
            this.inputMode = inputMode;
            this.source = source;
        }

        public UUID getUuid() { return uuid; }
        public String getName() { return name; }
        public boolean isBedrock() { return bedrock; }
        public String getPlatform() { return platform; }
        public String getInputMode() { return inputMode; }
        public Source getSource() { return source; }

        @Override
        public String toString() {
            return String.format("ConnectionInfo[%s/%s bedrock=%s platform=%s input=%s source=%s]",
                    name, uuid, bedrock, platform, inputMode, source);
        }
    }
}
