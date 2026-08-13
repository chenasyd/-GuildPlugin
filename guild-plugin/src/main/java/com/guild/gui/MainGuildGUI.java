package com.guild.gui;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.gui.layout.GuiImageLayoutConfig;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.utils.CompatibleScheduler;

import org.geysermc.cumulus.form.SimpleForm;

/**
 * 主工会GUI - 七个主要入口
 *
 * <p>支持两种渲染模式：
 * <ul>
 *   <li><b>标准模式</b>：使用物品图标 + 玻璃板边框</li>
 *   <li><b>图像模式</b>：ImagoCore 背景图 + 透明载体物品（多槽位按钮）</li>
 * </ul>
 */
public class MainGuildGUI implements GUI {

    // ── 功能常量（对应 gui-image-layout.yml 中的键名）──────────
    public static final String FUNC_CREATE_GUILD = "CREATE_GUILD";
    public static final String FUNC_GUILD_INFO = "GUILD_INFO";
    public static final String FUNC_MEMBER_MANAGE = "MEMBER_MANAGE";
    public static final String FUNC_APPLICATION_MANAGE = "APPLICATION_MANAGE";
    public static final String FUNC_GUILD_SETTINGS = "GUILD_SETTINGS";
    public static final String FUNC_GUILD_LIST = "GUILD_LIST";
    public static final String FUNC_GUILD_RELATIONS = "GUILD_RELATIONS";

    private final GuildPlugin plugin;
    private final Player player;
    private final LanguageManager languageManager;

    public MainGuildGUI(GuildPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.main-menu.title", "&6Guild System"));
    }
    
    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) return false;

        SimpleForm form = SimpleForm.builder()
                .title(languageManager.getGuiColoredMessage(player, "gui.main-menu.title", "&6Guild System"))
                .content(languageManager.getGuiColoredMessage(player, "gui.main-menu.bedrock-content", "&fChoose a function:"))
                .button(languageManager.getGuiColoredMessage(player, "gui.main-menu.create-guild.name", "&aCreate Guild"))
                .button(languageManager.getGuiColoredMessage(player, "gui.main-menu.guild-info.name", "&eGuild Info"))
                .button(languageManager.getGuiColoredMessage(player, "gui.main-menu.member-management.name", "&eMember Management"))
                .button(languageManager.getGuiColoredMessage(player, "gui.main-menu.application-management.name", "&eApplication Management"))
                .button(languageManager.getGuiColoredMessage(player, "gui.main-menu.guild-settings.name", "&eGuild Settings"))
                .button(languageManager.getGuiColoredMessage(player, "gui.main-menu.guild-list.name", "&eGuild List"))
                .button(languageManager.getGuiColoredMessage(player, "gui.main-menu.guild-relations.name", "&eGuild Relations"))
                .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    switch (response.clickedButtonId()) {
                        case 0 -> openCreateGuildGUI(player);
                        case 1 -> openGuildInfoGUI(player);
                        case 2 -> openMemberManagementGUI(player);
                        case 3 -> openApplicationManagementGUI(player);
                        case 4 -> openGuildSettingsGUI(player);
                        case 5 -> openGuildListGUI(player);
                        case 6 -> openGuildRelationsGUI(player);
                    }
                }))
                .build();

        return BedrockFormSender.sendForm(player.getUniqueId(), form);
    }

    @Override
    public void setupInventory(Inventory inventory) {
        // 图像布局模式：透明载体 + 多槽位（基岩版玩家跳过）
        if (plugin.getGuiManager().isImageLayoutActive(player, getGuiType())) {
            setupImageLayout(inventory);
            return;
        }

        // ── 标准模式（原始行为）──────────────────────────────────
        // 填充边框
        fillBorder(inventory);

        // 工会信息按钮
        ItemStack guildInfo = createItem(
            Material.BOOK,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.main-menu.guild-info.name", "&eGuild Info")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.main-menu.guild-info.lore.1", "&7View detailed guild information")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.main-menu.guild-info.lore.2", "&7Including basic info, statistics, etc."))
        );
        inventory.setItem(20, guildInfo);

        // 成员管理按钮
        ItemStack memberManagement = createItem(
            Material.PLAYER_HEAD,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.main-menu.member-management.name", "&eMember Management")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.main-menu.member-management.lore.1", "&7Manage guild members")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.main-menu.member-management.lore.2", "&7Invite, kick, permission management"))
        );
        inventory.setItem(22, memberManagement);

        // 申请管理按钮
        ItemStack applicationManagement = createItem(
            Material.PAPER,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.main-menu.application-management.name", "&eApplication Management")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.main-menu.application-management.lore.1", "&7Handle join applications")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.main-menu.application-management.lore.2", "&7View application history"))
        );
        inventory.setItem(24, applicationManagement);

        // 工会设置按钮
        ItemStack guildSettings = createItem(
            Material.COMPASS,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.main-menu.guild-settings.name", "&eGuild Settings")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.main-menu.guild-settings.lore.1", "&7Modify guild settings")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.main-menu.guild-settings.lore.2", "&7Description, tag, permissions, etc."))
        );
        inventory.setItem(29, guildSettings);

        // 工会列表按钮
        ItemStack guildList = createItem(
            Material.BOOKSHELF,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.main-menu.guild-list.name", "&eGuild List")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.main-menu.guild-list.lore.1", "&7View all guilds")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.main-menu.guild-list.lore.2", "&7Search, filter functions"))
        );
        inventory.setItem(31, guildList);

        // 工会关系按钮
        ItemStack guildRelations = createItem(
            Material.RED_WOOL,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.main-menu.guild-relations.name", "&eGuild Relations")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.main-menu.guild-relations.lore.1", "&7Manage guild relations")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.main-menu.guild-relations.lore.2", "&7Allies, enemies, etc."))
        );
        inventory.setItem(33, guildRelations);

        // 创建工会按钮
        ItemStack createGuild = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.main-menu.create-guild.name", "&aCreate Guild")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.main-menu.create-guild.lore.1", "&7Create a new guild")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.main-menu.create-guild.lore.2", "&7Requires coins"))
        );
        inventory.setItem(4, createGuild);
    }

    /**
     * 图像布局模式：使用透明载体物品填充配置的功能槽位。
     * 不填充边框和背景（由 ImagoCore 图片标题渲染）。
     */
    private void setupImageLayout(Inventory inventory) {
        GuiImageLayoutConfig layoutConfig = plugin.getGuiManager().getImageLayoutConfig();
        Material mat = layoutConfig.getTransparentMaterial();
        int modelData = layoutConfig.getTransparentModelData();

        // 为每个功能创建透明载体物品并填充到配置的槽位
        placeTransparentFunction(inventory, mat, modelData, FUNC_CREATE_GUILD,
                "gui.main-menu.create-guild.name", "&aCreate Guild",
                new String[]{"gui.main-menu.create-guild.lore.1", "gui.main-menu.create-guild.lore.2"},
                new String[]{"&7Create a new guild", "&7Requires coins"});

        placeTransparentFunction(inventory, mat, modelData, FUNC_GUILD_INFO,
                "gui.main-menu.guild-info.name", "&eGuild Info",
                new String[]{"gui.main-menu.guild-info.lore.1", "gui.main-menu.guild-info.lore.2"},
                new String[]{"&7View detailed guild information", "&7Including basic info, statistics, etc."});

        placeTransparentFunction(inventory, mat, modelData, FUNC_MEMBER_MANAGE,
                "gui.main-menu.member-management.name", "&eMember Management",
                new String[]{"gui.main-menu.member-management.lore.1", "gui.main-menu.member-management.lore.2"},
                new String[]{"&7Manage guild members", "&7Invite, kick, permission management"});

        placeTransparentFunction(inventory, mat, modelData, FUNC_APPLICATION_MANAGE,
                "gui.main-menu.application-management.name", "&eApplication Management",
                new String[]{"gui.main-menu.application-management.lore.1", "gui.main-menu.application-management.lore.2"},
                new String[]{"&7Handle join applications", "&7View application history"});

        placeTransparentFunction(inventory, mat, modelData, FUNC_GUILD_SETTINGS,
                "gui.main-menu.guild-settings.name", "&eGuild Settings",
                new String[]{"gui.main-menu.guild-settings.lore.1", "gui.main-menu.guild-settings.lore.2"},
                new String[]{"&7Modify guild settings", "&7Description, tag, permissions, etc."});

        placeTransparentFunction(inventory, mat, modelData, FUNC_GUILD_LIST,
                "gui.main-menu.guild-list.name", "&eGuild List",
                new String[]{"gui.main-menu.guild-list.lore.1", "gui.main-menu.guild-list.lore.2"},
                new String[]{"&7View all guilds", "&7Search, filter functions"});

        placeTransparentFunction(inventory, mat, modelData, FUNC_GUILD_RELATIONS,
                "gui.main-menu.guild-relations.name", "&eGuild Relations",
                new String[]{"gui.main-menu.guild-relations.lore.1", "gui.main-menu.guild-relations.lore.2"},
                new String[]{"&7Manage guild relations", "&7Allies, enemies, etc."});
    }

    /**
     * 在配置的所有槽位放置透明载体物品（携带功能名称和描述）。
     */
    private void placeTransparentFunction(Inventory inventory, Material mat, int modelData,
                                           String funcName, String nameKey, String defaultName,
                                           String[] loreKeys, String[] defaultLores) {
        GuiImageLayoutConfig layoutConfig = plugin.getGuiManager().getImageLayoutConfig();
        List<Integer> slots = layoutConfig.getSlots(getGuiType(), funcName);
        if (slots.isEmpty()) return;

        // 构建物品名称和描述
        String displayName = ColorUtils.colorize(
                languageManager.getGuiMessage(player, nameKey, defaultName));
        String[] loreLines = new String[loreKeys.length];
        for (int i = 0; i < loreKeys.length; i++) {
            loreLines[i] = ColorUtils.colorize(
                    languageManager.getGuiMessage(player, loreKeys[i], defaultLores[i]));
        }

        // 创建透明载体物品
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(Arrays.asList(loreLines));
            meta.setCustomModelData(modelData);
            item.setItemMeta(meta);
        }

        // 填充到所有配置的槽位
        for (int slot : slots) {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, item.clone());
            }
        }
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // 图像布局模式：通过配置反查功能（基岩版玩家跳过）
        if (plugin.getGuiManager().isImageLayoutActive(player, getGuiType())) {
            GuiImageLayoutConfig layoutConfig = plugin.getGuiManager().getImageLayoutConfig();
            String func = layoutConfig.getFunctionAtSlot(getGuiType(), slot);
            if (func == null) return;
            dispatchFunction(player, func);
            return;
        }

        // ── 标准模式（原始行为）──────────────────────────────────
        switch (slot) {
            case 20: // 工会信息
                openGuildInfoGUI(player);
                break;
            case 22: // 成员管理
                openMemberManagementGUI(player);
                break;
            case 24: // 申请管理
                openApplicationManagementGUI(player);
                break;
            case 29: // 工会设置
                openGuildSettingsGUI(player);
                break;
            case 31: // 工会列表
                openGuildListGUI(player);
                break;
            case 33: // 工会关系
                openGuildRelationsGUI(player);
                break;
            case 4: // 创建工会
                openCreateGuildGUI(player);
                break;
        }
    }

    /**
     * 根据功能常量分发点击事件（图像布局模式使用）。
     */
    private void dispatchFunction(Player player, String func) {
        switch (func) {
            case FUNC_CREATE_GUILD -> openCreateGuildGUI(player);
            case FUNC_GUILD_INFO -> openGuildInfoGUI(player);
            case FUNC_MEMBER_MANAGE -> openMemberManagementGUI(player);
            case FUNC_APPLICATION_MANAGE -> openApplicationManagementGUI(player);
            case FUNC_GUILD_SETTINGS -> openGuildSettingsGUI(player);
            case FUNC_GUILD_LIST -> openGuildListGUI(player);
            case FUNC_GUILD_RELATIONS -> openGuildRelationsGUI(player);
        }
    }
    
    /**
     * 打开工会信息GUI
     */
    private void openGuildInfoGUI(Player player) {
        // 检查玩家是否有工会
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // 确保在玩家实体线程中执行GUI操作
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (guild == null) {
                    String message = languageManager.getGuiMessage(player, "gui.common.no-guild", "&cYou do not have a guild yet");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // 打开工会信息GUI
                GuildInfoGUI guildInfoGUI = new GuildInfoGUI(plugin, player, guild);
                plugin.getGuiManager().openGUI(player, guildInfoGUI);
            });
        });
    }
    
    /**
     * 打开成员管理GUI
     */
    private void openMemberManagementGUI(Player player) {
        // 检查玩家是否有工会
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // 确保在玩家实体线程中执行GUI操作
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (guild == null) {
                    String message = languageManager.getGuiMessage(player, "gui.common.no-guild", "&cYou do not have a guild yet");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // 打开成员管理GUI
                MemberManagementGUI memberManagementGUI = new MemberManagementGUI(plugin, guild, player);
                plugin.getGuiManager().openGUI(player, memberManagementGUI);
            });
        });
    }
    
    /**
     * 打开申请管理GUI
     */
    private void openApplicationManagementGUI(Player player) {
        // 检查玩家是否有工会
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // 确保在玩家实体线程中执行GUI操作
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (guild == null) {
                    String message = languageManager.getGuiMessage(player, "gui.common.no-guild", "&cYou do not have a guild yet");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // 检查权限
                plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
                    // 确保在玩家实体线程中执行GUI操作
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        if (member == null || !member.getRole().canInvite()) {
                            String message = languageManager.getGuiMessage(player, "gui.common.no-permission", "&cInsufficient permission");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }

                        // 打开申请管理GUI
                        ApplicationManagementGUI applicationManagementGUI = new ApplicationManagementGUI(plugin, guild, player);
                        plugin.getGuiManager().openGUI(player, applicationManagementGUI);
                    });
                });
            });
        });
    }
    
    /**
     * 打开工会设置GUI
     */
    private void openGuildSettingsGUI(Player player) {
        // 检查玩家是否有工会
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // 确保在玩家实体线程中执行GUI操作
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (guild == null) {
                    String message = languageManager.getGuiMessage(player, "gui.common.no-guild", "&cYou do not have a guild yet");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // 检查角色：会长 → 完整设置GUI，普通成员 → 成员专用GUI
                plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        if (member == null) {
                            String message = languageManager.getGuiMessage(player, "gui.common.leader-only", "&cOnly the guild leader can perform this operation");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }

                        if (member.getRole() == com.guild.models.GuildMember.Role.LEADER) {
                            // 打开工会设置GUI（完整版）
                            GuildSettingsGUI guildSettingsGUI = new GuildSettingsGUI(plugin, guild, player);
                            plugin.getGuiManager().openGUI(player, guildSettingsGUI);
                        } else {
                            // 打开成员工会GUI（简化版：仅传送家 + 离开工会）
                            MemberGuildGUI memberGUI = new MemberGuildGUI(plugin, guild, player);
                            plugin.getGuiManager().openGUI(player, memberGUI);
                        }
                    });
                });
            });
        });
    }
    
    /**
     * 打开工会列表GUI
     */
    private void openGuildListGUI(Player player) {
        // 打开工会列表GUI
        GuildListGUI guildListGUI = new GuildListGUI(plugin, player);
        plugin.getGuiManager().openGUI(player, guildListGUI);
    }
    
    /**
     * 打开工会关系GUI
     */
    private void openGuildRelationsGUI(Player player) {
        // 检查玩家是否有工会
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // 确保在玩家实体线程中执行GUI操作
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (guild == null) {
                    String message = languageManager.getGuiMessage(player, "gui.common.no-guild", "&cYou do not have a guild yet");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // 检查权限
                plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
                    // 确保在玩家实体线程中执行GUI操作
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        if (member == null || member.getRole() != com.guild.models.GuildMember.Role.LEADER) {
                            String message = languageManager.getGuiMessage(player, "gui.common.manage-relations-leader-only", "&cOnly the guild leader can manage relations");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }

                        // 打开工会关系GUI
                        GuildRelationsGUI guildRelationsGUI = new GuildRelationsGUI(plugin, guild, player);
                        plugin.getGuiManager().openGUI(player, guildRelationsGUI);
                    });
                });
            });
        });
    }
    
    /**
     * 打开创建工会GUI
     */
    private void openCreateGuildGUI(Player player) {
        // 检查玩家是否已有工会
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // 确保在玩家实体线程中执行GUI操作
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (guild != null) {
                    String message = languageManager.getGuiMessage(player, "gui.create-guild.create.already-in-guild", "&cYou are already in a guild!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // 打开创建工会GUI
                CreateGuildGUI createGuildGUI = new CreateGuildGUI(plugin, player);
                plugin.getGuiManager().openGUI(player, createGuildGUI);
            });
        });
    }
    
    /**
     * 填充边框
     */
    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 45, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }
    
    /**
     * 创建物品
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
