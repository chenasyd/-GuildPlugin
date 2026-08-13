package com.guild.activity;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.gui.GuildInfoGUI;
import com.guild.gui.MemberDetailsGUI;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Builtin guild activity / contribution ranking GUI (hybrid total score).
 */
public final class GuildActivityGUI implements GUI {

    public enum ReturnTo {
        GUILD_INFO,
        MEMBER_DETAILS
    }

    private static final int CONTENT_START = 9;
    private static final int CONTENT_END = 44;
    private static final int COLUMNS = 7;
    private static final int PER_PAGE = 28;

    private final GuildPlugin plugin;
    private final LanguageManager languageManager;
    private final Guild guild;
    private final Player viewer;
    private final ReturnTo returnTo;
    private final GuildMember detailsMember;

    private int currentPage = 1;
    private List<MemberActivityScore> scores = List.of();
    private boolean loaded;

    public GuildActivityGUI(GuildPlugin plugin, Guild guild, Player viewer, ReturnTo returnTo) {
        this(plugin, guild, viewer, returnTo, null);
    }

    public GuildActivityGUI(GuildPlugin plugin, Guild guild, Player viewer,
                            ReturnTo returnTo, GuildMember detailsMember) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.guild = guild;
        this.viewer = viewer;
        this.returnTo = returnTo != null ? returnTo : ReturnTo.GUILD_INFO;
        this.detailsMember = detailsMember;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(modMsg("module.activity.title", "&6成员贡献 / 活跃度"));
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void setupInventory(Inventory inventory) {
        inventory.clear();
        fillBorder(inventory);
        inventory.setItem(4, createItem(Material.BOOK,
                ColorUtils.colorize(modMsg("module.activity.header", "&6&l成员贡献排行")),
                ColorUtils.colorize(modMsg("module.activity.header-lore1", "&7总分 = 经济贡献 + 活跃度 × 权重")),
                ColorUtils.colorize(modMsg("module.activity.header-lore2", "&7经济来自 guild_contributions；活跃来自在线统计"))));

        if (!loaded) {
            inventory.setItem(22, createItem(Material.CLOCK,
                    ColorUtils.colorize(modMsg("module.activity.loading", "&e加载中..."))));
            setupNav(inventory, 1, 1);
            fillInterior(inventory);
            loadAsync(inventory);
            return;
        }

        renderPage(inventory);
    }

    /** Module lang: {@code lang/modules/builtin-activity/}. */
    private String modMsg(String key, String fallback) {
        return languageManager.getModuleMessage(viewer, key, fallback);
    }

    private void loadAsync(Inventory inventory) {
        var service = plugin.getActivityScoreService();
        if (service == null || !service.getSettings().isEnabled()) {
            CompatibleScheduler.runTask(plugin, viewer, () -> {
                loaded = true;
                scores = List.of();
                if (viewer.getOpenInventory().getTopInventory().equals(inventory)) {
                    setupInventory(inventory);
                }
            });
            return;
        }
        service.getGuildScoresAsync(guild.getId()).thenAccept(list ->
                CompatibleScheduler.runTask(plugin, viewer, () -> {
                    loaded = true;
                    scores = list != null ? list : List.of();
                    if (viewer.isOnline() && viewer.getOpenInventory().getTopInventory().equals(inventory)) {
                        setupInventory(inventory);
                    }
                }));
    }

    private void renderPage(Inventory inventory) {
        int totalPages = Math.max(1, (int) Math.ceil(scores.size() / (double) PER_PAGE));
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        if (scores.isEmpty()) {
            inventory.setItem(22, createItem(Material.BARRIER,
                    ColorUtils.colorize(modMsg("module.activity.empty", "&7暂无成员")),
                    ColorUtils.colorize(modMsg("module.activity.empty-hint", "&7工会尚无成员可统计"))));
        } else {
            int from = (currentPage - 1) * PER_PAGE;
            int to = Math.min(from + PER_PAGE, scores.size());
            for (int i = from; i < to; i++) {
                int slot = mapToSlot(i - from);
                if (slot < 0) continue;
                inventory.setItem(slot, buildScoreItem(scores.get(i)));
            }
        }

        setupNav(inventory, currentPage, totalPages);
        fillInterior(inventory);
    }

    private ItemStack buildScoreItem(MemberActivityScore score) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) {
            return head;
        }

        meta.setOwningPlayer(Bukkit.getOfflinePlayer(score.getPlayerUuid()));

        String rankColor = switch (score.getRank()) {
            case 1 -> "&6";
            case 2 -> "&7";
            case 3 -> "&c";
            default -> "&e";
        };
        String onlineTag = score.isOnline()
                ? modMsg("module.activity.online", "&a在线")
                : modMsg("module.activity.offline", "&7离线");
        meta.setDisplayName(ColorUtils.colorize(rankColor + "#" + score.getRank() + " &f" + score.getPlayerName()
                + " &8(" + onlineTag + "&8)"));

        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize(modMsg("module.activity.lore-total", "&7总分: &a{0}")
                .replace("{0}", format(score.getTotalScore()))));
        lore.add(ColorUtils.colorize(modMsg("module.activity.lore-economy", "&7经济贡献: &f{0}")
                .replace("{0}", format(score.getEconomyPts()))));
        lore.add(ColorUtils.colorize(modMsg("module.activity.lore-activity", "&7活跃度: &f{0}")
                .replace("{0}", format(score.getActivityPts()))));
        meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private void setupNav(Inventory inventory, int page, int totalPages) {
        if (page > 1) {
            inventory.setItem(45, createItem(Material.ARROW,
                    ColorUtils.colorize(languageManager.getGuiMessage(viewer, "gui.common.previous-page", "&e上一页"))));
        }
        inventory.setItem(49, createItem(Material.BARRIER,
                ColorUtils.colorize(modMsg("module.activity.back", "&c返回")),
                ColorUtils.colorize(modMsg("module.activity.back-hint", "&7返回上一界面"))));
        if (page < totalPages) {
            inventory.setItem(53, createItem(Material.ARROW,
                    ColorUtils.colorize(languageManager.getGuiMessage(viewer, "gui.common.next-page", "&e下一页"))));
        }
        if (loaded && !scores.isEmpty()) {
            inventory.setItem(48, createItem(Material.PAPER,
                    ColorUtils.colorize(languageManager.getGuiMessage(viewer, "gui.common.page-info", "&7第{0}页/共{1}页")
                            .replace("{0}", String.valueOf(page))
                            .replace("{1}", String.valueOf(totalPages)))));
        }
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        if (slot == 49) {
            navigateBack(player);
            return;
        }
        int totalPages = Math.max(1, (int) Math.ceil(scores.size() / (double) PER_PAGE));
        if (slot == 45 && currentPage > 1) {
            currentPage--;
            plugin.getGuiManager().openGUI(player, this);
            return;
        }
        if (slot == 53 && currentPage < totalPages) {
            currentPage++;
            plugin.getGuiManager().openGUI(player, this);
        }
    }

    private void navigateBack(Player player) {
        if (returnTo == ReturnTo.MEMBER_DETAILS && detailsMember != null) {
            plugin.getGuiManager().openGUI(player, new MemberDetailsGUI(plugin, guild, detailsMember, player));
        } else {
            plugin.getGuiManager().openGUI(player, new GuildInfoGUI(plugin, player, guild));
        }
    }

    private static int mapToSlot(int linearIndex) {
        if (linearIndex < 0 || linearIndex >= PER_PAGE) return -1;
        int row = linearIndex / COLUMNS;
        int col = linearIndex % COLUMNS;
        return CONTENT_START + row * 9 + col + 1;
    }

    private void fillBorder(Inventory inv) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(i + 45, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, border);
            inv.setItem(i + 8, border);
        }
    }

    private void fillInterior(Inventory inv) {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = CONTENT_START; slot <= CONTENT_END; slot++) {
            int col = slot % 9;
            if (col == 0 || col == 8) continue;
            if (inv.getItem(slot) == null) {
                inv.setItem(slot, filler);
            }
        }
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && lore.length > 0) {
                List<String> list = new ArrayList<>();
                for (String line : lore) {
                    list.add(line);
                }
                meta.setLore(list);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String format(double v) {
        if (Math.abs(v - Math.rint(v)) < 0.05) {
            return String.valueOf((long) Math.rint(v));
        }
        return String.format("%.1f", v);
    }
}
