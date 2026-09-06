package com.guild.module.example.territory.gui;

import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.module.example.territory.TerritoryCommandHandler;
import com.guild.module.example.territory.TerritoryModule;
import com.guild.module.example.territory.TerritoryRecord;
import com.guild.module.example.territory.TerritorySelectionManager;
import com.guild.module.example.territory.TerritorySettings;
import com.guild.module.example.territory.TerritoryTexts;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 领地模块 Bedrock {@link SimpleForm} 构建与发送。
 */
final class TerritoryBedrockForms {

    private static final int BTN_WAND = 0;
    private static final int BTN_POS1 = 1;
    private static final int BTN_POS2 = 2;
    private static final int BTN_CLAIM = 3;
    private static final int BTN_UNCLAIM = 4;
    private static final int BTN_BACK_MANAGE = 5;

    private TerritoryBedrockForms() {
    }

    static boolean openManagement(TerritoryModule module, Guild guild, Player player, boolean manageMode) {
        if (!BedrockFormSender.isAvailable()) {
            return false;
        }

        TerritoryTexts texts = module.getTexts();
        SimpleForm.Builder builder = SimpleForm.builder()
                .title(texts.format(player, "module.territory.gui.bedrock-manage-title",
                        "&6公会领地 - {0}", guild.getName()))
                .content(buildManagementContent(module, guild, player, texts));

        if (manageMode) {
            builder.button(texts.format(player, "module.territory.gui.wand", "&a选区斧"))
                    .button(texts.format(player, "module.territory.gui.pos1", "&aPos1"))
                    .button(texts.format(player, "module.territory.gui.pos2", "&ePos2"))
                    .button(texts.format(player, "module.territory.gui.claim", "&a声明领地"))
                    .button(texts.format(player, "module.territory.gui.unclaim", "&c放弃领地"));
        }
        builder.button(texts.format(player, "module.territory.gui.back", "&c返回"));

        builder.validResultHandler(response -> CompatibleScheduler.runTask(
                module.getContext().getPlugin(), player, () -> {
                    int id = response.clickedButtonId();
                    if (manageMode) {
                        if (id == BTN_BACK_MANAGE) {
                            module.getContext().navigateBack(player);
                            return;
                        }
                        TerritoryCommandHandler handler = module.getCommandHandler();
                        if (handler == null) {
                            return;
                        }
                        switch (id) {
                            case BTN_WAND -> {
                                handler.giveWand(player);
                                openManagement(module, guild, player, true);
                            }
                            case BTN_POS1 -> {
                                handler.setCorner(player, true);
                                openManagement(module, guild, player, true);
                            }
                            case BTN_POS2 -> {
                                handler.setCorner(player, false);
                                openManagement(module, guild, player, true);
                            }
                            case BTN_CLAIM -> module.getContext().openGUI(player,
                                    new ConfirmTerritoryClaimGUI(module, guild, player));
                            case BTN_UNCLAIM -> module.getContext().openGUI(player,
                                    new ConfirmTerritoryUnclaimGUI(module, guild, player));
                            default -> {
                            }
                        }
                    } else if (id == 0) {
                        module.getContext().navigateBack(player);
                    }
                }));

        return BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
    }

    static boolean openClaimConfirm(TerritoryModule module, Guild guild, Player player) {
        if (!BedrockFormSender.isAvailable()) {
            return false;
        }

        TerritoryTexts texts = module.getTexts();
        SimpleForm form = SimpleForm.builder()
                .title(texts.format(player, "module.territory.gui.confirm-claim-title", "&a确认声明领地"))
                .content(buildClaimConfirmContent(module, player, texts))
                .button(texts.format(player, "module.territory.gui.confirm", "&a确认"))
                .button(texts.format(player, "module.territory.gui.cancel", "&c取消"))
                .validResultHandler(response -> CompatibleScheduler.runTask(
                        module.getContext().getPlugin(), player, () -> {
                            if (response.clickedButtonId() == 0) {
                                TerritoryCommandHandler handler = module.getCommandHandler();
                                if (handler != null) {
                                    handler.claim(player);
                                }
                            }
                            module.getContext().openGUI(player,
                                    new TerritoryManagementGUI(module, guild, player, true));
                        }))
                .closedResultHandler(() -> CompatibleScheduler.runTask(
                        module.getContext().getPlugin(), player, () ->
                                module.getContext().openGUI(player,
                                        new TerritoryManagementGUI(module, guild, player, true))))
                .build();

        return BedrockFormSender.sendForm(player.getUniqueId(), form);
    }

    static boolean openUnclaimConfirm(TerritoryModule module, Guild guild, Player player) {
        if (!BedrockFormSender.isAvailable()) {
            return false;
        }

        TerritoryTexts texts = module.getTexts();
        SimpleForm form = SimpleForm.builder()
                .title(texts.format(player, "module.territory.gui.confirm-unclaim-title", "&c确认放弃领地"))
                .content(buildUnclaimConfirmContent(module, guild, player, texts))
                .button(texts.format(player, "module.territory.gui.confirm", "&c确认放弃"))
                .button(texts.format(player, "module.territory.gui.cancel", "&a取消"))
                .validResultHandler(response -> CompatibleScheduler.runTask(
                        module.getContext().getPlugin(), player, () -> {
                            if (response.clickedButtonId() == 0) {
                                TerritoryCommandHandler handler = module.getCommandHandler();
                                if (handler != null) {
                                    handler.unclaim(player);
                                }
                            }
                            module.getContext().openGUI(player,
                                    new TerritoryManagementGUI(module, guild, player, true));
                        }))
                .closedResultHandler(() -> CompatibleScheduler.runTask(
                        module.getContext().getPlugin(), player, () ->
                                module.getContext().openGUI(player,
                                        new TerritoryManagementGUI(module, guild, player, true))))
                .build();

        return BedrockFormSender.sendForm(player.getUniqueId(), form);
    }

    private static String buildManagementContent(TerritoryModule module, Guild guild,
                                                 Player player, TerritoryTexts texts) {
        String worldName = player.getWorld().getName();
        StringBuilder sb = new StringBuilder();

        sb.append(texts.format(player, "module.territory.gui.header-guild", "&7公会: &f{0}", guild.getName()))
                .append('\n');
        sb.append(texts.format(player, "module.territory.gui.header-world", "&7当前世界: &f{0}", worldName))
                .append('\n');
        sb.append(texts.format(player, "module.territory.gui.header-server", "&7本机 ID: &f{0}",
                module.getRepository().getLocalServerId())).append('\n');

        if (module.isWorldGuardReady()) {
            sb.append(texts.format(player, "module.territory.gui.wg-ready", "&aWorldGuard 就绪"));
        } else {
            String missing = module.getAvailability() != null
                    ? module.getAvailability().describeMissing()
                    : "WorldGuard";
            sb.append(texts.format(player, "module.territory.gui.wg-degraded-lore", "&7缺少: &f{0}", missing));
        }
        sb.append("\n\n");

        Optional<TerritoryRecord> current = findTerritory(module, guild.getId(), worldName);
        if (current.isEmpty()) {
            sb.append(texts.format(player, "module.territory.gui.current-none", "&7当前世界无领地"));
        } else {
            TerritoryRecord territory = current.get();
            sb.append(texts.format(player, "module.territory.gui.current-claimed", "&a已声明领地")).append('\n');
            sb.append(texts.format(player, "module.territory.info-line-region", "&7区域: &f{0}",
                    territory.getRegionId())).append('\n');
            sb.append(texts.format(player, "module.territory.gui.volume-line", "&7体积: &f{0} 方块",
                    formatVolume(territory)));
        }
        sb.append("\n\n");

        TerritorySelectionManager selections = module.getSelectionManager();
        if (selections != null && selections.hasCompleteSelection(player)) {
            TerritorySelectionManager.Session session = selections.of(player);
            sb.append(texts.format(player, "module.territory.gui.selection-ready", "&a选区就绪")).append('\n');
            sb.append(texts.format(player, "module.territory.gui.selection-pos1", "&7Pos1: &f{0}",
                    formatLocation(session.pos1))).append('\n');
            sb.append(texts.format(player, "module.territory.gui.selection-pos2", "&7Pos2: &f{0}",
                    formatLocation(session.pos2))).append('\n');
            sb.append(texts.format(player, "module.territory.selection-volume", "&7选区体积: &f{0} 方块",
                    selections.selectionVolume(player)));
        } else {
            sb.append(texts.format(player, "module.territory.gui.selection-incomplete", "&7选区未完成")).append('\n');
            sb.append(texts.format(player, "module.territory.gui.selection-pos1", "&7Pos1: &f{0}",
                    formatPos(selections, player, true))).append('\n');
            sb.append(texts.format(player, "module.territory.gui.selection-pos2", "&7Pos2: &f{0}",
                    formatPos(selections, player, false)));
        }
        sb.append("\n\n");

        List<TerritoryRecord> territories = module.getRepository().findByGuildId(guild.getId());
        sb.append(texts.format(player, "module.territory.gui.bedrock-list-header",
                "&7全部领地 (&f{0}&7):", territories.size()));
        if (territories.isEmpty()) {
            sb.append('\n').append(texts.format(player, "module.territory.gui.bedrock-list-empty",
                    "&8暂无记录"));
        } else {
            int limit = Math.min(territories.size(), 8);
            for (int i = 0; i < limit; i++) {
                TerritoryRecord record = territories.get(i);
                sb.append('\n').append(texts.format(player, "module.territory.gui.list-item",
                        "&f{0} @ {1}", record.getServerId(), record.getWorldName()));
            }
            if (territories.size() > limit) {
                sb.append('\n').append(texts.format(player, "module.territory.gui.bedrock-list-more",
                        "&8… 另有 {0} 处", territories.size() - limit));
            }
        }

        return sb.toString();
    }

    private static String buildClaimConfirmContent(TerritoryModule module, Player player, TerritoryTexts texts) {
        TerritorySelectionManager selections = module.getSelectionManager();
        TerritorySettings settings = module.getSettings();
        long volume = selections != null ? selections.selectionVolume(player) : 0L;
        double cost = settings != null ? settings.getClaimCost() : 0;

        String worldName = player.getWorld().getName();
        if (selections != null && selections.hasCompleteSelection(player)) {
            worldName = selections.of(player).pos1.getWorld().getName();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(texts.format(player, "module.territory.info-line-world", "&7世界: &f{0}", worldName))
                .append('\n');
        sb.append(texts.format(player, "module.territory.selection-volume", "&7选区体积: &f{0} 方块", volume));
        if (cost > 0) {
            sb.append('\n').append(texts.format(player, "module.territory.gui.claim-cost", "&7费用: &f{0}",
                    formatMoney(module, cost)));
        }
        sb.append("\n\n").append(texts.format(player, "module.territory.gui.confirm-claim-warning",
                "&c此操作不可撤销，请确认选区正确"));
        return sb.toString();
    }

    private static String buildUnclaimConfirmContent(TerritoryModule module, Guild guild,
                                                     Player player, TerritoryTexts texts) {
        String worldName = player.getWorld().getName();
        Optional<TerritoryRecord> record = findTerritory(module, guild.getId(), worldName);
        if (record.isEmpty()) {
            return texts.format(player, "module.territory.gui.confirm-unclaim-none", "&7当前世界无领地")
                    + "\n\n"
                    + texts.format(player, "module.territory.not-found", "&7当前世界（{0}）暂无公会领地。", worldName);
        }

        TerritoryRecord territory = record.get();
        return texts.format(player, "module.territory.info-line-world", "&7世界: &f{0}", territory.getWorldName())
                + '\n'
                + texts.format(player, "module.territory.info-line-region", "&7区域: &f{0}", territory.getRegionId())
                + "\n\n"
                + texts.format(player, "module.territory.gui.confirm-unclaim-warning",
                "&c放弃后该区域将不再受 WorldGuard 保护");
    }

    private static Optional<TerritoryRecord> findTerritory(TerritoryModule module, int guildId, String worldName) {
        Optional<TerritoryRecord> record = module.getRepository().get(guildId, worldName);
        if (record.isEmpty() && module.getBridge().isOperational()) {
            record = module.getBridge().findTerritory(guildId, worldName);
        }
        return record;
    }

    private static long formatVolume(TerritoryRecord record) {
        long dx = record.getMaxX() - record.getMinX() + 1L;
        long dy = record.getMaxY() - record.getMinY() + 1L;
        long dz = record.getMaxZ() - record.getMinZ() + 1L;
        return dx * dy * dz;
    }

    private static String formatPos(TerritorySelectionManager selections, Player player, boolean pos1) {
        if (selections == null) {
            return "-";
        }
        TerritorySelectionManager.Session session = selections.of(player);
        return formatLocation(pos1 ? session.pos1 : session.pos2);
    }

    private static String formatLocation(Location loc) {
        if (loc == null) {
            return "-";
        }
        return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private static String formatMoney(TerritoryModule module, double amount) {
        if (module.getContext().getPlugin().getEconomyManager().isVaultAvailable()) {
            return module.getContext().getPlugin().getEconomyManager().format(amount);
        }
        return String.format(Locale.ROOT, "%.2f", amount);
    }
}
