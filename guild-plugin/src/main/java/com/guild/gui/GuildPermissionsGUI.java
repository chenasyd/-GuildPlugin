package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.models.Guild;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.geysermc.cumulus.form.SimpleForm;

import java.util.Arrays;
import java.util.List;

/**
 * 公会权限设置GUI
 */
public class GuildPermissionsGUI implements GUI {

    public static final String FUNC_LEADER_PERMS = "LEADER_PERMS";
    public static final String FUNC_OFFICER_PERMS = "OFFICER_PERMS";
    public static final String FUNC_MEMBER_PERMS = "MEMBER_PERMS";
    public static final String FUNC_INFO = "INFO";
    public static final String FUNC_STATUS = "STATUS";
    public static final String FUNC_BACK = "BACK";

    private final GuildPlugin plugin;
    private final Guild guild;
    private final Player player;
    private final LanguageManager languageManager;

    public GuildPermissionsGUI(GuildPlugin plugin, Guild guild, Player player) {
        this.plugin = plugin;
        this.guild = guild;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-permissions.title",
                "&6Guild Permissions"));
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void setupInventory(Inventory inventory) {
        fillBorder(inventory);
        displayPermissions(inventory);
        setupButtons(inventory);
        plugin.getGuiManager().applyImageModeIfNeeded(player, inventory, getGuiType());
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 49) {
            plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, player));
        }
    }

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

    private void displayPermissions(Inventory inventory) {
        inventory.setItem(10, createItem(
                Material.GOLDEN_HELMET,
                t("gui.guild-permissions.leader-title", "&6Leader permissions"),
                t("gui.guild-permissions.leader-lore-1", "&7• All permissions"),
                t("gui.guild-permissions.leader-lore-2", "&7• Manage members"),
                t("gui.guild-permissions.leader-lore-3", "&7• Modify settings"),
                t("gui.guild-permissions.leader-lore-4", "&7• Delete guild")));

        inventory.setItem(12, createItem(
                Material.IRON_HELMET,
                t("gui.guild-permissions.officer-title", "&eOfficer permissions"),
                t("gui.guild-permissions.officer-lore-1", "&7• Invite members"),
                t("gui.guild-permissions.officer-lore-2", "&7• Kick members"),
                t("gui.guild-permissions.officer-lore-3", "&7• Handle applications"),
                t("gui.guild-permissions.officer-lore-4", "&7• Set guild home")));

        inventory.setItem(14, createItem(
                Material.LEATHER_HELMET,
                t("gui.guild-permissions.member-title", "&7Member permissions"),
                t("gui.guild-permissions.member-lore-1", "&7• View guild info"),
                t("gui.guild-permissions.member-lore-2", "&7• Teleport to guild home"),
                t("gui.guild-permissions.member-lore-3", "&7• Apply to other guilds")));

        inventory.setItem(16, createItem(
                Material.BOOK,
                t("gui.guild-permissions.info-title", "&ePermission notes"),
                t("gui.guild-permissions.info-lore-1", "&7Permissions are role-based"),
                t("gui.guild-permissions.info-lore-2", "&7Leaders can promote/demote"),
                t("gui.guild-permissions.info-lore-3", "&7Officers manage members"),
                t("gui.guild-permissions.info-lore-4", "&7Members have basic access")));

        inventory.setItem(22, createItem(
                Material.SHIELD,
                t("gui.guild-permissions.status-title", "&aCurrent status"),
                t("gui.guild-permissions.status-guild", "&7Guild: &e{0}")
                        .replace("{0}", guild.getName()),
                t("gui.guild-permissions.status-system", "&7Permission system: &aOK"),
                t("gui.guild-permissions.status-check", "&7Permission checks: &aEnabled")));
    }

    private void setupButtons(Inventory inventory) {
        inventory.setItem(49, createItem(
                Material.ARROW,
                t("gui.guild-permissions.back", "&7Back"),
                t("gui.guild-permissions.back-hint", "&7Return to guild settings")));
    }

    private String t(String key, String fallback) {
        return ColorUtils.colorize(languageManager.getGuiMessage(player, key, fallback));
    }

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) return false;

        String guildName = ColorUtils.stripColor(guild.getName());
        String content = languageManager.getGuiColoredMessage(player, "gui.guild-permissions.bedrock-content",
                "&6=== Leader Permissions ===\n&f• All permissions\n&f• Manage members\n&f• Modify settings\n&f• Delete guild\n\n&e=== Officer Permissions ===\n&f• Invite members\n&f• Kick members\n&f• Handle applications\n&f• Set guild home\n\n&7=== Member Permissions ===\n&f• View guild info\n&f• Teleport to guild home\n&f• Apply to join other guilds\n\n&aCurrent status: &fGuild {guild} | Permission system running normally",
                "{guild}", guildName);

        SimpleForm form = SimpleForm.builder()
                .title(languageManager.getGuiColoredMessage(player, "gui.guild-permissions.bedrock-title", "&6Guild Permissions"))
                .content(content)
                .button(languageManager.getGuiColoredMessage(player, "gui.guild-permissions.bedrock-back", "&cBack"))
                .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () ->
                        plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, player))))
                .closedResultHandler(response -> {})
                .build();

        BedrockFormSender.sendForm(player.getUniqueId(), form);
        return true;
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(List.of(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
