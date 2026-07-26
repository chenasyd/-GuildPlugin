package com.guild.core.hook;

import org.a.imagoCore.image.display.gui.GuiTitleRenderer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the ImagoCore GUI integration configuration for Guild Plugin.
 *
 * <p>Configuration file: {@code plugins/GuildPlugin/imago-gui.yml}
 *
 * <h3>Example configuration:</h3>
 * <pre>{@code
 * # ImagoCore GUI Integration
 * # Maps Guild Plugin GUI types to ImagoCore background images
 * # and decoration overlays.
 *
 * enabled: true
 *
 * # GUI type → ImagoCore entry ID mapping
 * # The entry ID format is "<slots>-<name>" matching ImagoCore's gui/ folder
 * bindings:
 *   MainGuildGUI: "54-default"
 *   GuildInfoGUI: "54-default"
 *
 * # Decoration overlays (optional)
 * # char:  ImagoCore char/ 下的图片名（不含 .png）
 * # x:     距背景左边缘的水平像素偏移
 * # ascent: 垂直位置覆盖（可选，负值下移；省略则用 char.yml 默认值）
 * overlays:
 *   MainGuildGUI:
 *     - char: "guild_banner"
 *       x: 30
 *       ascent: -40
 *     - char: "corner_ornament"
 *       x: 5
 * }</pre>
 */
public class ImagoGuiConfig {

    private final File configFile;
    private final Logger logger;

    private boolean enabled;
    private final Map<String, String> bindings = new LinkedHashMap<>();
    private final Map<String, List<OverlayConfig>> overlays = new LinkedHashMap<>();

    public ImagoGuiConfig(File dataFolder, Logger logger) {
        this.configFile = new File(dataFolder, "imago-gui.yml");
        this.logger = logger;
    }

    /**
     * Loads (or reloads) the configuration.
     * Creates a default config file if none exists.
     */
    public void load() {
        if (!configFile.exists()) {
            createDefault();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        enabled = config.getBoolean("enabled", true);

        bindings.clear();
        ConfigurationSection bindSec = config.getConfigurationSection("bindings");
        if (bindSec != null) {
            for (String guiType : bindSec.getKeys(false)) {
                String entryId = bindSec.getString(guiType);
                if (entryId != null && !entryId.isEmpty()) {
                    bindings.put(guiType, entryId);
                }
            }
        }

        overlays.clear();
        ConfigurationSection overlaySec = config.getConfigurationSection("overlays");
        if (overlaySec != null) {
            for (String guiType : overlaySec.getKeys(false)) {
                List<Map<?, ?>> list = overlaySec.getMapList(guiType);
                List<OverlayConfig> overlayList = new ArrayList<>();
                for (Map<?, ?> map : list) {
                    String charName = (String) map.get("char");
                    int x = map.containsKey("x") ? ((Number) map.get("x")).intValue() : 0;
                    Integer ascent = map.containsKey("ascent")
                            ? ((Number) map.get("ascent")).intValue() : null;
                    if (charName != null && !charName.isEmpty()) {
                        overlayList.add(new OverlayConfig(charName, x, ascent));
                    }
                }
                if (!overlayList.isEmpty()) {
                    overlays.put(guiType, overlayList);
                }
            }
        }

        logger.info("[ImagoGuiConfig] Loaded " + bindings.size() + " bindings, "
                + overlays.size() + " overlay groups. Enabled: " + enabled);
    }

    private void createDefault() {
        try {
            configFile.getParentFile().mkdirs();

            String content = """
# ============================================================
# ImagoCore GUI 图像集成配置 (imago-gui.yml)
# ============================================================
#
# enabled: 全局开关，设为 false 时所有 GUI 均不使用图像模式
#
# bindings: 每个 GUI 的图像模式开关
#   值 = ImagoCore entry ID (如 "54-mainguildgui") → 启用图像标题+透明化
#   值 = "false" → 禁用该 GUI 的图像模式，保持纯净样式（原始标题+原始物品）
#
# 优先级规则:
#   本配置 > gui-image-layout.yml
#   若某 GUI 在此处设为 false，则 gui-image-layout.yml 中该 GUI 的布局不生效
#
# overlays: 可选的叠加装饰层（char/ 或 gui/ 中的图片）
#   char: 图片名（不含 .png）
#   x:    距背景左边缘的水平像素偏移
#   ascent: 垂直位置覆盖（可选，负值下移；省略则用默认值）
# ============================================================

enabled: true

bindings:
  # ── 主要功能 GUI ──
  MainGuildGUI: "54-default"          # 工会主界面（入口）
  GuildInfoGUI: false                  # 工会信息
  GuildSettingsGUI: false              # 工会设置
  MemberGuildGUI: false                # 成员列表（工会内）
  GuildFundsGUI: false                 # 工会资金
  GuildLogsGUI: false                  # 工会日志
  GuildListGUI: false                  # 工会列表（浏览所有工会）
  GuildRelationsGUI: false             # 工会关系
  CreateGuildGUI: false                # 创建工会
  EconomyManagementGUI: false          # 经济管理

  # ── 管理/操作 GUI ──
  ApplicationManagementGUI: false      # 申请管理
  MemberManagementGUI: false           # 成员管理
  MemberDetailsGUI: false              # 成员详情
  GuildDetailGUI: false                # 工会详情（查看其他工会）
  GuildFilterGUI: false                # 工会筛选
  GuildPermissionsGUI: false           # 权限设置
  GuildListManagementGUI: false        # 工会列表管理

  # ── 确认对话框 ──
  ConfirmDeleteGuildGUI: false         # 确认解散工会
  ConfirmLeaveGuildGUI: false          # 确认退出工会
  ConfirmChangeFundsGUI: false         # 确认资金操作

  # ── 成员操作 GUI ──
  DemoteMemberGUI: false               # 降级成员
  PromoteMemberGUI: false              # 升级成员
  KickMemberGUI: false                 # 踢出成员
  InviteMemberGUI: false               # 邀请成员

  # ── 管理员/系统 GUI ──
  AdminGuildGUI: false                 # 管理员工会管理
  SystemSettingsGUI: false             # 系统设置

  # ── 关系 GUI ──
  CreateRelationGUI: false             # 创建关系
  RelationManagementGUI: false         # 关系管理

  # ── 输入 GUI ──
  GuildTagInputGUI: false              # 工会标签输入
  GuildDescriptionInputGUI: false      # 工会描述输入
  GuildNameInputGUI: false             # 工会名称输入

# ============================================================
# 叠加装饰层配置（可选）
# 示例:
# overlays:
#   MainGuildGUI:
#     - char: "guild_banner"
#       x: 30
#       ascent: -40
#     - char: "corner_ornament"
#       x: 5
# ============================================================
""";

            java.nio.file.Files.writeString(configFile.toPath(), content,
                    java.nio.charset.StandardCharsets.UTF_8);
            logger.info("[ImagoGuiConfig] Created default imago-gui.yml");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create imago-gui.yml", e);
        }
    }

    // ── Accessors ───────────────────────────────────────────────

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the ImagoCore entry ID bound to a GUI type.
     *
     * @param guiType the GUI class simple name (e.g. "MainGuildGUI")
     * @return the entry ID, or null if not bound
     */
    public String getBinding(String guiType) {
        return bindings.get(guiType);
    }

    /**
     * Gets the overlay configurations for a GUI type.
     *
     * @param guiType the GUI class simple name
     * @return list of overlay configs, or empty list
     */
    public List<OverlayConfig> getOverlays(String guiType) {
        return overlays.getOrDefault(guiType, Collections.emptyList());
    }

    /**
     * Checks if a GUI type has an active ImagoCore configuration.
     * Returns false if the GUI is not bound OR if its binding value is "false"
     * (which explicitly disables image mode for that GUI).
     */
    public boolean hasConfig(String guiType) {
        String value = bindings.get(guiType);
        return value != null && !"false".equalsIgnoreCase(value);
    }

    /**
     * Checks if a GUI type is explicitly disabled in the config (value = "false").
     */
    public boolean isGuiDisabled(String guiType) {
        String value = bindings.get(guiType);
        return "false".equalsIgnoreCase(value);
    }

    /**
     * Returns all configured GUI type → entry ID bindings.
     */
    public Map<String, String> getAllBindings() {
        return Collections.unmodifiableMap(bindings);
    }

    // ── Overlay config record ───────────────────────────────────

    /**
     * A single overlay decoration configuration.
     */
    public static class OverlayConfig {
        private final String charName;
        private final int x;
        private final Integer ascent; // null = use char.yml default

        public OverlayConfig(String charName, int x, Integer ascent) {
            this.charName = charName;
            this.x = x;
            this.ascent = ascent;
        }

        /** The char image name (filename without .png in ImagoCore/char/). */
        public String getCharName() {
            return charName;
        }

        /** Horizontal offset from background left edge in pixels. */
        public int getX() {
            return x;
        }

        /**
         * Vertical position override (ascent). Negative values move the
         * image downward into the item area. Null means use the char's
         * default ascent from char.yml.
         */
        public Integer getAscent() {
            return ascent;
        }
    }
}
