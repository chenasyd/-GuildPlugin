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
# 仅在 imago-gui.yml 中 enabled: true 且对应 GUI 有绑定时生效
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
# 功能常量说明（MainGuildGUI）:
#   CREATE_GUILD      - 创建工会   (原始槽位: 4)
#   GUILD_INFO        - 工会信息   (原始槽位: 20)
#   MEMBER_MANAGE     - 成员管理   (原始槽位: 22)
#   APPLICATION_MANAGE - 申请管理  (原始槽位: 24)
#   GUILD_SETTINGS    - 工会设置   (原始槽位: 29)
#   GUILD_LIST        - 工会列表   (原始槽位: 31)
#   GUILD_RELATIONS   - 工会关系   (原始槽位: 33)
#
# 槽位编号参考 (54槽 / 6行9列):
#   行0:  0  1  2  3  4  5  6  7  8
#   行1:  9 10 11 12 13 14 15 16 17
#   行2: 18 19 20 21 22 23 24 25 26
#   行3: 27 28 29 30 31 32 33 34 35
#   行4: 36 37 38 39 40 41 42 43 44
#   行5: 45 46 47 48 49 50 51 52 53
#
# 示例: 将"创建工会"设为 3x2 大按钮区域 (行0列0-2 + 行1列0-2)
#   CREATE_GUILD: [0, 1, 2, 9, 10, 11]
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
""";

            java.nio.file.Files.writeString(configFile.toPath(), content,
                    java.nio.charset.StandardCharsets.UTF_8);
            logger.info("[GuiImageLayout] Created default gui-image-layout.yml");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create gui-image-layout.yml", e);
        }
    }
}
