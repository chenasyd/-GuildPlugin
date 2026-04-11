package com.guild.module.example.announcement;

import com.guild.GuildPlugin;
import com.guild.core.module.GuildModule;
import com.guild.core.module.ModuleContext;
import com.guild.core.module.ModuleDescriptor;
import com.guild.core.module.ModuleState;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.event.MemberEventData;
import com.guild.sdk.event.MemberEventHandler;
import com.guild.module.example.announcement.gui.AnnouncementEditGUI;
import com.guild.module.example.announcement.gui.AnnouncementListGUI;
import com.guild.module.example.announcement.gui.AnnouncementViewGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 工会公告模块 - 示例模块
 * <p>
 * 功能：
 * <ul>
 *   <li>在 GuildSettingsGUI 的模块页面注入"插件公告"按钮（birch_sign 物品）</li>
 *   <li>提供公告的创建、编辑、删除功能</li>
 *   <li>通过 GUI 界面管理所有操作</li>
 * </ul>
 */
public class AnnouncementModule implements GuildModule {

    private ModuleContext context;
    private ModuleDescriptor descriptor;
    private ModuleState state = ModuleState.UNLOADED;
    private AnnouncementManager announcementManager;

    @Override
    public void onEnable(ModuleContext context) throws Exception {
        this.context = context;
        this.state = ModuleState.ACTIVE;

        // 数据目录: plugins/GuildPlugin/data/announcements/
        File dataDir = new File(context.getPlugin().getDataFolder(), "data" + File.separator + "announcements");
        this.announcementManager = new AnnouncementManager(dataDir, context.getLogger());

        // 从磁盘加载已保存的公告数据
        announcementManager.loadAll();

        GuildPluginAPI api = context.getApi();

        // 在 GuildSettingsGUI 中注册"插件公告"按钮（自动分配槽位，管理入口）
        ItemStack settingsButton = createSettingsButton();
        api.registerGUIButton(
                "GuildSettingsGUI",
                GUIExtensionHook.AUTO_SLOT,
                settingsButton,
                "announcement",
                (player, ctx) -> handleOpenAnnouncementList(player, ctx)
        );

        // 在 GuildInfoGUI 中注册"告示牌"按钮（固定槽位12，查看入口）
        ItemStack infoButton = createInfoButton();
        api.registerGUIButton(
                "GuildInfoGUI",
                12,
                infoButton,
                "announcement",
                (player, ctx) -> handleOpenAnnouncementView(player, ctx)
        );

        api.onMemberJoin(new MemberEventHandler() {
            @Override
            public void onEvent(MemberEventData data) {
                if (context.getConfig().getBoolean("welcome-announcement.enabled", false)) {
                    context.getLogger().info("[Announcement] 新成员 " + data.getPlayerName()
                        + " 加入公会 " + data.getGuildName());
                }
            }
            @Override
            public Object getModuleInstance() { return AnnouncementModule.this; }
        });

        api.onMemberLeave(new MemberEventHandler() {
            @Override
            public void onEvent(MemberEventData data) {
                context.getLogger().info("[Announcement] 成员 " + data.getPlayerName()
                    + " 离开公会 " + data.getGuildName());
            }
            @Override
            public Object getModuleInstance() { return AnnouncementModule.this; }
        });

        context.getLogger().info(ColorUtils.colorize(context.getMessage("module.announcement.loaded",
                "&a[公告模块] 公告系统已启用")));

        var langManager = context.getLanguageManager();
        String welcomeKey = "module.announcement.welcome-hint";
        context.getLogger().info("[Announcement-Lang] 动态获取消息: "
            + ColorUtils.colorize(langManager.getMessage(welcomeKey,
                "&7提示: 使用 /guild announcement 管理公告")));
    }

    @Override
    public void onDisable() {
        this.state = ModuleState.UNLOADED;
        if (announcementManager != null) {
            announcementManager.saveAll();
            announcementManager.clearAll();
        }
        context.getLogger().info(ColorUtils.colorize(context.getMessage("module.announcement.unloaded",
                "&e[公告模块] 公告系统已关闭")));
    }

    @Override
    public ModuleDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public void setDescriptor(ModuleDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public ModuleState getState() {
        return state;
    }

    /** 处理打开公告列表按钮点击（管理入口） */
    private void handleOpenAnnouncementList(Player player, Object... ctx) {
        Guild guild = extractGuild(ctx);
        if (guild == null) {
            player.sendMessage(ColorUtils.colorize(
                    context.getMessage("module.announcement.error.no-guild",
                            "&c无法获取工会信息")));
            return;
        }

        if (!hasManagePermission(player)) {
            player.sendMessage(ColorUtils.colorize(
                    context.getMessage("module.announcement.error.no-permission",
                            "&c你没有管理公告的权限")));
            return;
        }

        context.openGUI(player,
                new AnnouncementListGUI(this, guild, player));
    }

    /** 处理打开公告浏览按钮点击（所有成员可见） */
    private void handleOpenAnnouncementView(Player player, Object... ctx) {
        Guild guild = extractGuild(ctx);
        if (guild == null) {
            player.sendMessage(ColorUtils.colorize(
                    context.getMessage("module.announcement.error.no-guild",
                            "&c无法获取工会信息")));
            return;
        }

        context.openGUI(player,
                new AnnouncementViewGUI(this, guild));
    }

    /** 打开编辑公告界面 */
    public void openEditGUI(Player player, Guild guild, String announcementId) {
        context.openGUI(player,
                new AnnouncementEditGUI(this, guild, announcementId, player));
    }

    /** 打开创建新公告界面 */
    public void openCreateGUI(Player player, Guild guild) {
        context.openGUI(player,
                new AnnouncementEditGUI(this, guild, null, player));
    }

    // ==================== 权限检查 ====================

    public boolean hasManagePermission(Player player) {
        UUID uuid = player.getUniqueId();
        GuildPlugin plugin = context.getPlugin();
        GuildMember member = plugin.getGuildService().getGuildMember(uuid);
        if (member == null) return false;
        return member.getRole() == GuildMember.Role.LEADER ||
               member.getRole() == GuildMember.Role.OFFICER;
    }

    // ==================== 工具方法 ====================

    private Guild extractGuild(Object... ctx) {
        if (ctx != null && ctx.length > 0 && ctx[0] instanceof Guild) {
            return (Guild) ctx[0];
        }
        return null;
    }

    private ItemStack createSettingsButton() {
        Material material;
        try {
            material = Material.valueOf("BIRCH_SIGN");
        } catch (IllegalArgumentException e) {
            material = Material.OAK_SIGN;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&e&l" +
                    context.getMessage("module.announcement.button-name", "插件公告")));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7" +
                    context.getMessage("module.announcement.button-desc",
                            "管理和发布工会公告")));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createInfoButton() {
        Material material;
        try {
            material = Material.valueOf("OAK_SIGN");
        } catch (IllegalArgumentException e) {
            material = Material.OAK_SIGN;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&6&l" +
                    context.getMessage("module.announcement.info-button-name", "告示牌")));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7" +
                    context.getMessage("module.announcement.info-button-desc",
                            "查看工会公告")));
            lore.add(ColorUtils.colorize("&7" +
                    context.getMessage("module.announcement.info-button-hint",
                            "&7点击查看全部公告")));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ==================== Getter ====================

    public ModuleContext getContext() { return context; }
    public AnnouncementManager getAnnouncementManager() { return announcementManager; }
}
