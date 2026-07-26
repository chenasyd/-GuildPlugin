package com.guild.core.gui.layout;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 图像GUI布局配置管理器。
 *
 * <p>配置文件：{@code plugins/GuildPlugin/gui-image-layout.yml}
 *
 * <p>当 imago-gui.yml 中 enabled: true 且对应 GUI 有绑定时，
 * 本配置决定每个功能按钮占据哪些槽位，以及使用什么透明载体物品。
 *
 * <h3>配置结构：</h3>
 * <pre>{@code
 * # 透明载体物品（视觉不可见，仅用于悬浮提示）
 * transparent_item:
 *   material: BARRIER
 *   custom_model_data: 10001
 *
 * # 各GUI的布局配置
 * layouts:
 *   MainGuildGUI:
 *     CREATE_GUILD: [0, 1, 2, 9, 10, 11]
 *     GUILD_INFO: [3, 4, 5, 12, 13, 14]
 * }</pre>
 */
public class GuiImageLayoutConfig {

    private final File configFile;
    private final Logger logger;

    // 透明载体物品设置
    private Material transparentMaterial = Material.BARRIER;
    private int transparentModelData = 10001;

    // guiType → (functionName → slotList)
    private final Map<String, Map<String, List<Integer>>> layouts = new LinkedHashMap<>();

    // 缓存：guiType → (slot → functionName) 反向索引
    private final Map<String, Map<Integer, String>> slotIndex = new LinkedHashMap<>();

    public GuiImageLayoutConfig(File dataFolder, Logger logger) {
        this.configFile = new File(dataFolder, "gui-image-layout.yml");
        this.logger = logger;
    }

    /**
     * 加载（或重新加载）配置。
     * 若文件不存在则生成带注释的默认配置。
     */
    public void load() {
        if (!configFile.exists()) {
            createDefault();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // 透明物品设置
        transparentMaterial = Material.matchMaterial(
                config.getString("transparent_item.material", "BARRIER"));
        if (transparentMaterial == null) {
            transparentMaterial = Material.BARRIER;
        }
        transparentModelData = config.getInt("transparent_item.custom_model_data", 10001);

        // 布局
        layouts.clear();
        slotIndex.clear();

        ConfigurationSection layoutsSec = config.getConfigurationSection("layouts");
        if (layoutsSec != null) {
            for (String guiType : layoutsSec.getKeys(false)) {
                ConfigurationSection guiSec = layoutsSec.getConfigurationSection(guiType);
                if (guiSec == null) continue;

                Map<String, List<Integer>> funcMap = new LinkedHashMap<>();
                Map<Integer, String> reverseMap = new HashMap<>();

                for (String funcName : guiSec.getKeys(false)) {
                    List<Integer> slots = guiSec.getIntegerList(funcName);
                    if (slots.isEmpty()) continue;

                    funcMap.put(funcName, Collections.unmodifiableList(slots));
                    for (int slot : slots) {
                        reverseMap.put(slot, funcName);
                    }
                }

                if (!funcMap.isEmpty()) {
                    layouts.put(guiType, Collections.unmodifiableMap(funcMap));
                    slotIndex.put(guiType, Collections.unmodifiableMap(reverseMap));
                }
            }
        }

        logger.info("[GuiImageLayout] Loaded " + layouts.size() + " GUI layouts. "
                + "Transparent item: " + transparentMaterial.name()
                + " (model_data=" + transparentModelData + ")");
    }

    // ── 查询接口 ────────────────────────────────────────────────

    /**
     * 检查指定 GUI 是否有图像布局配置。
     */
    public boolean hasLayout(String guiType) {
        return layouts.containsKey(guiType);
    }

    /**
     * 获取指定 GUI 中某个功能对应的槽位列表。
     *
     * @param guiType  GUI 类型名（如 "MainGuildGUI"）
     * @param function 功能常量名（如 "CREATE_GUILD"）
     * @return 槽位列表，未配置时返回空列表
     */
    public List<Integer> getSlots(String guiType, String function) {
        Map<String, List<Integer>> funcMap = layouts.get(guiType);
        if (funcMap == null) return Collections.emptyList();
        return funcMap.getOrDefault(function, Collections.emptyList());
    }

    /**
     * 根据槽位反查功能名。
     *
     * @param guiType GUI 类型名
     * @param slot    槽位编号
     * @return 功能常量名，未匹配时返回 null
     */
    public String getFunctionAtSlot(String guiType, int slot) {
        Map<Integer, String> reverseMap = slotIndex.get(guiType);
        if (reverseMap == null) return null;
        return reverseMap.get(slot);
    }

    /**
     * 获取指定 GUI 的全部功能 → 槽位映射。
     */
    public Map<String, List<Integer>> getLayout(String guiType) {
        return layouts.getOrDefault(guiType, Collections.emptyMap());
    }

    // ── 透明物品 ────────────────────────────────────────────────

    public Material getTransparentMaterial() {
        return transparentMaterial;
    }

    public int getTransparentModelData() {
        return transparentModelData;
    }

    // ── 默认配置生成 ────────────────────────────────────────────

    private void createDefault() {
        try {
            configFile.getParentFile().mkdirs();

            // 使用手动写入以保留注释（YamlConfiguration 不支持注释）
            String content = """
# ============================================================
# 图像GUI布局配置 (gui-image-layout.yml)
# 仅在 imago-gui.yml 中 enabled: true 且对应 GUI 绑定值非 false 时生效
# 优先级: imago-gui.yml > 本文件
# ============================================================

# 透明载体物品设置
# 该物品在资源包中通过 CustomModelData 映射为透明材质，
# 视觉上不可见（不遮挡背景图），但鼠标悬浮时仍显示名称和描述。
# material: 生存模式无法获取的物品（BARRIER / STRUCTURE_VOID / LIGHT）
# custom_model_data: 对应资源包中透明模型覆盖的 CustomModelData 值
transparent_item:
  material: BARRIER
  custom_model_data: 10001

# ============================================================
# GUI 布局配置
# 格式: layouts.<GUI类型>.<功能常量>: [槽位列表]
#
# 槽位编号参考 (54槽 / 6行9列):
#   行0:  0  1  2  3  4  5  6  7  8
#   行1:  9 10 11 12 13 14 15 16 17
#   行2: 18 19 20 21 22 23 24 25 26
#   行3: 27 28 29 30 31 32 33 34 35
#   行4: 36 37 38 39 40 41 42 43 44
#   行5: 45 46 47 48 49 50 51 52 53
#
# 注意: 只有 MainGuildGUI 使用了多槽位布局系统。
# 其他 GUI 启用图像模式后自动应用透明化（无需布局配置），
# 若需为其他 GUI 添加多槽位布局，需同时在对应 GUI 类中实现布局逻辑。
# ============================================================

layouts:
  MainGuildGUI:
    # 创建工会 - 3x2 区域 (行0-1, 列0-2)
    CREATE_GUILD: [0, 1, 2, 9, 10, 11]
    # 工会信息 - 3x2 区域 (行0-1, 列3-5)
    GUILD_INFO: [3, 4, 5, 12, 13, 14]
    # 成员管理 - 3x2 区域 (行0-1, 列6-8)
    MEMBER_MANAGE: [6, 7, 8, 15, 16, 17]
    # 申请管理 - 3x2 区域 (行2-3, 列0-2)
    APPLICATION_MANAGE: [18, 19, 20, 27, 28, 29]
    # 工会设置 - 3x2 区域 (行2-3, 列3-5)
    GUILD_SETTINGS: [21, 22, 23, 30, 31, 32]
    # 工会列表 - 3x2 区域 (行2-3, 列6-8)
    GUILD_LIST: [24, 25, 26, 33, 34, 35]
    # 工会关系 - 3x2 区域 (行4-5, 列0-2)
    GUILD_RELATIONS: [36, 37, 38, 45, 46, 47]

# ============================================================
# 以下为其他 GUI 的功能槽位参考（注释状态，启用需配合代码修改）
# ============================================================
#
# GuildInfoGUI (54槽):
#   SUMMARY: 10, STATS: 19, ECONOMY: 28, STATUS: 36, BACK: 49
#
# GuildSettingsGUI (54槽):
#   OVERVIEW: 10, TEXT_EDIT: 11, SET_HOME: 13, MEMBER_MGMT: 15,
#   GUILD_FUNDS: 28, LOGS: 31, HOME_TELEPORT: 33, LEAVE: 34,
#   DELETE: 36, BACK: 49
#
# MemberGuildGUI (27槽):
#   HOME_TELEPORT: 11, GUILD_INFO: 13, LEAVE_GUILD: 15
#
# GuildFundsGUI (54槽):
#   CONTENT: [10-43], PAGE_INFO: 46, PREV_PAGE: 48, BACK: 49,
#   NEXT_PAGE: 50, REFRESH: 51
#
# GuildLogsGUI (54槽):
#   CONTENT: [10-43], PAGE_INFO: 46, BACK: 49, NEXT_PAGE: 50, REFRESH: 51
#
# GuildListGUI (54槽):
#   CONTENT: [10-44], PREV_PAGE: 18, NEXT_PAGE: 26, SEARCH: 45,
#   FILTER: 47, BACK: 49
#
# GuildRelationsGUI (54槽):
#   CONTENT: [10-43], CREATE_RELATION: 45, PAGE_INFO: 46,
#   PREV_PAGE: 48, BACK: 49, NEXT_PAGE: 50
#
# CreateGuildGUI (54槽):
#   CURRENT_NAME: 11, CURRENT_TAG: 13, CURRENT_DESC: 15,
#   NAME_INPUT: 20, TAG_INPUT: 22, DESC_INPUT: 24,
#   CONFIRM: 39, CANCEL: 41
#
# EconomyManagementGUI (54槽):
#   CONTENT: [10-43], BACK: 46, PREV_PAGE: 48, PAGE_INFO: 49,
#   NEXT_PAGE: 50, REFRESH: 52
#
# ApplicationManagementGUI (54槽):
#   CONTENT: [10-44], PREV_PAGE: 18, NEXT_PAGE: 26,
#   PENDING: 47, BACK: 49, HISTORY: 51
#
# MemberManagementGUI (54槽):
#   CONTENT: [10-43], PREV_PAGE: 18, NEXT_PAGE: 26, INVITE: 45,
#   KICK: 47, PROMOTE: 49, DEMOTE: 51, BACK: 53
#
# MemberDetailsGUI (54槽):
#   MEMBER_HEAD: 13, BASIC_INFO: 20, ROLE_INFO: 21, TIME_INFO: 22,
#   CONTRIBUTION: 23, KICK: 37, PROMOTE_DEMOTE: 39, MESSAGE: 41, BACK: 49
#
# GuildDetailGUI (54槽):
#   GUILD_NAME: 4, LEADER_HEAD: 12, DESCRIPTION: 14, ECONOMY: 16,
#   MEMBERS: [19-25,28-34,37-43], BACK: 45, FREEZE: 47, DELETE: 49,
#   TRANSFER: 51, REFRESH: 53
#
# GuildPermissionsGUI (54槽):
#   LEADER_PERMS: 10, OFFICER_PERMS: 12, MEMBER_PERMS: 14,
#   INFO: 16, STATUS: 22, BACK: 49
#
# AdminGuildGUI (54槽):
#   GUILD_LIST_MGMT: 20, ECONOMY_MGMT: 22, RELATION_MGMT: 24,
#   STATISTICS: 29, SYSTEM_SETTINGS: 31, BACK: 49
#
# SystemSettingsGUI (54槽):
#   DEBUG: 10, AUTO_SAVE: 12, ECONOMY: 14, RELATION: 16,
#   LEVEL_SYSTEM: 19, APPLICATION: 21, INVITE: 23, GUILD_HOME: 25,
#   RELOAD: 28, DB_MAINT: 30, BACKUP: 32, BACK: 49, SAVE: 51
#
# ConfirmDeleteGuildGUI (27槽): CONFIRM: 11, INFO: 13, CANCEL: 15
# ConfirmLeaveGuildGUI (27槽): CONFIRM: 11, INFO: 13, CANCEL: 15
# ConfirmChangeFundsGUI (27槽): CONFIRM: 11, DETAILS: 13, CANCEL: 15
#
# DemoteMemberGUI (54槽):  CONTENT: [10-43], PREV: 45, BACK: 49, NEXT: 53
# PromoteMemberGUI (54槽): CONTENT: [10-43], PREV: 45, BACK: 49, NEXT: 53
# KickMemberGUI (54槽):    CONTENT: [10-43], PREV: 45, BACK: 49, NEXT: 53
# InviteMemberGUI (54槽):  CONTENT: [10-43], PREV: 45, BACK: 49, NEXT: 53
#
# CreateRelationGUI (54槽):
#   TYPE_SELECTOR: [0-8], TARGETS: [10-43], CONFIRM: 45,
#   SELECTION: 47, BACK: 49, PAGE_INFO: 51, PREV: 52, NEXT: 53
#
# RelationManagementGUI (54槽):
#   CONTENT: [10-43], BACK: 46, PREV: 48, PAGE_INFO: 49,
#   NEXT: 50, REFRESH: 52
#
# GuildFilterGUI (54槽):
#   CONTENT: [10-44], PREV: 18, NEXT: 26, MIN_LEVEL: 46,
#   MAX_LEVEL: 47, SORT: 48, BACK: 52
#
# GuildListManagementGUI (54槽):
#   CONTENT: [10-13], BACK: 46, PREV: 48, PAGE_INFO: 49,
#   NEXT: 50, REFRESH: 52
""";

            java.nio.file.Files.writeString(configFile.toPath(), content,
                    java.nio.charset.StandardCharsets.UTF_8);
            logger.info("[GuiImageLayout] Created default gui-image-layout.yml");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create gui-image-layout.yml", e);
        }
    }
}
