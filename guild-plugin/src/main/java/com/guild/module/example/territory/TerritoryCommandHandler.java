package com.guild.module.example.territory;

import com.guild.core.module.ModuleContext;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.sdk.data.MemberData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** {@code /guild territory} 子命令处理器。 */
public final class TerritoryCommandHandler {

    private final TerritoryModule module;
    private final ModuleContext context;
    private final TerritorySelectionManager selections;
    private final TerritoryTexts texts;

    public TerritoryCommandHandler(TerritoryModule module, ModuleContext context,
                                   TerritorySelectionManager selections, TerritoryTexts texts) {
        this.module = module;
        this.context = context;
        this.selections = selections;
        this.texts = texts;
    }

    public void handle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            texts.send(sender, "module.territory.player-only", "&c仅玩家可执行此命令。");
            return;
        }

        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "claim" -> handleClaim(player);
            case "unclaim" -> handleUnclaim(player);
            case "info" -> handleInfo(player);
            case "wand" -> handleWand(player);
            case "pos1" -> handlePos(player, true);
            case "pos2" -> handlePos(player, false);
            case "help" -> sendHelp(player);
            default -> sendHelp(player);
        }
    }

    private void handleClaim(Player player) {
        if (!checkPermission(player, "guild.territory.claim")) {
            return;
        }
        if (!module.getBridge().isOperational()) {
            texts.send(player, "module.territory.wg-missing",
                    "&c未检测到 WorldGuard/WorldEdit，无法声明领地。");
            return;
        }
        if (!context.getPlugin().getMembershipRules().canManageGuild(player)) {
            texts.send(player, "module.territory.not-manager",
                    "&c仅公会管理可声明领地。");
            return;
        }
        if (!selections.hasCompleteSelection(player)) {
            texts.send(player, "module.territory.selection-incomplete",
                    "&c请先设置 Pos1/Pos2（&e/guild territory wand&c 或 pos1/pos2）。");
            return;
        }

        TerritorySelectionManager.Session session = selections.of(player);
        Location pos1 = session.pos1;
        Location pos2 = session.pos2;
        String worldName = pos1.getWorld().getName();

        if (!isWorldAllowed(worldName)) {
            texts.send(player, "module.territory.world-blocked",
                    "&c此世界不允许声明公会领地。");
            return;
        }

        long volume = selections.selectionVolume(player);
        long maxVolume = context.getConfig().getInt("claim.max-volume", 50_000);
        if (volume > maxVolume) {
            texts.send(player, "module.territory.volume-too-large",
                    "&c选区过大（{0} > {1}）。", volume, maxVolume);
            return;
        }

        Guild guild = context.getPlugin().getGuildService().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            texts.send(player, "module.territory.not-in-guild",
                    "&c你不在任何公会中。");
            return;
        }

        if (module.getRepository().get(guild.getId(), worldName).isPresent()) {
            texts.send(player, "module.territory.already-claimed",
                    "&c本世界已有公会领地，请先 &e/guild territory unclaim&c。");
            return;
        }

        texts.send(player, "module.territory.claiming",
                "&e正在声明领地…");

        context.getApi().getGuildMembers(guild.getId()).thenAccept(members -> {
            if (members == null || members.isEmpty()) {
                CompatibleScheduler.runTask(context.getPlugin(), player, () ->
                        texts.send(player, "module.territory.claim-failed",
                                "&c无法加载公会成员列表。"));
                return;
            }

            UUID leaderUuid = TerritoryMemberSync.findLeaderUuid(members);
            List<UUID> memberUuids = new ArrayList<>(members.size());
            for (MemberData member : members) {
                if (member.getPlayerUuid() != null) {
                    memberUuids.add(member.getPlayerUuid());
                }
            }

            TerritoryClaimRequest request = new TerritoryClaimRequest(
                    guild.getId(),
                    guild.getName(),
                    worldName,
                    pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ(),
                    pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ(),
                    leaderUuid,
                    memberUuids
            );

            CompatibleScheduler.runTask(context.getPlugin(), player, () -> {
                Optional<TerritoryRecord> created = module.getBridge().claimTerritory(request);
                if (created.isPresent()) {
                    TerritoryRecord record = created.get();
                    texts.send(player, "module.territory.claim-success",
                            "&a已声明领地 &f{0}&a（{1}），区域 ID: &f{2}",
                            guild.getName(), worldName, record.getRegionId());
                    session.wandMode = false;
                } else {
                    texts.send(player, "module.territory.claim-failed",
                            "&c声明失败：区域重叠或 WorldGuard 保存失败。");
                }
            });
        }).exceptionally(error -> {
            CompatibleScheduler.runTask(context.getPlugin(), player, () ->
                    texts.send(player, "module.territory.claim-failed",
                            "&c声明失败：无法加载成员数据。"));
            return null;
        });
    }

    private void handleUnclaim(Player player) {
        if (!checkPermission(player, "guild.territory.unclaim")) {
            return;
        }
        if (!module.getBridge().isOperational()) {
            texts.send(player, "module.territory.wg-missing",
                    "&c未检测到 WorldGuard/WorldEdit。");
            return;
        }
        if (!context.getPlugin().getMembershipRules().canManageGuild(player)) {
            texts.send(player, "module.territory.not-manager",
                    "&c仅公会管理可放弃领地。");
            return;
        }

        Guild guild = context.getPlugin().getGuildService().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            texts.send(player, "module.territory.not-in-guild",
                    "&c你不在任何公会中。");
            return;
        }

        String worldName = player.getWorld().getName();
        if (module.getRepository().get(guild.getId(), worldName).isEmpty()
                && module.getBridge().findTerritory(guild.getId(), worldName).isEmpty()) {
            texts.send(player, "module.territory.not-found",
                    "&c当前世界没有公会领地。");
            return;
        }

        CompatibleScheduler.runTask(context.getPlugin(), player, () -> {
            boolean removed = module.getBridge().unclaimTerritory(guild.getId(), worldName);
            if (removed) {
                texts.send(player, "module.territory.unclaim-success",
                        "&a已放弃领地（{0}）。", worldName);
            } else {
                texts.send(player, "module.territory.unclaim-failed",
                        "&c放弃领地失败。");
            }
        });
    }

    private void handleInfo(Player player) {
        if (!checkPermission(player, "guild.territory.info")) {
            return;
        }

        GuildMember member = context.getPlugin().getGuildService().getGuildMember(player.getUniqueId());
        if (member == null) {
            texts.send(player, "module.territory.not-in-guild",
                    "&c你不在任何公会中。");
            return;
        }

        String worldName = player.getWorld().getName();
        int guildId = member.getGuildId();

        Optional<TerritoryRecord> record = module.getRepository().get(guildId, worldName);
        if (record.isEmpty() && module.getBridge().isOperational()) {
            record = module.getBridge().findTerritory(guildId, worldName);
        }

        if (record.isEmpty()) {
            texts.send(player, "module.territory.not-found",
                    "&7当前世界（{0}）暂无公会领地。", worldName);
            return;
        }

        TerritoryRecord territory = record.get();
        texts.send(player, "module.territory.info-header",
                "&6—— 公会领地 ——");
        texts.send(player, "module.territory.info-line-region",
                "&7区域: &f{0}", territory.getRegionId());
        texts.send(player, "module.territory.info-line-world",
                "&7世界: &f{0}", territory.getWorldName());
        texts.send(player, "module.territory.info-line-bounds",
                "&7范围: &f({0},{1},{2}) &7→ &f({3},{4},{5})",
                territory.getMinX(), territory.getMinY(), territory.getMinZ(),
                territory.getMaxX(), territory.getMaxY(), territory.getMaxZ());
    }

    private void handleWand(Player player) {
        if (!checkPermission(player, "guild.territory.claim")) {
            return;
        }
        if (!context.getPlugin().getMembershipRules().canManageGuild(player)) {
            texts.send(player, "module.territory.not-manager",
                    "&c仅公会管理可使用选区工具。");
            return;
        }

        Material wand = parseMaterial(context.getConfig().getString("claim.wand-material", "WOODEN_AXE"));
        TerritorySelectionManager.Session session = selections.of(player);
        session.wandMode = true;
        player.getInventory().addItem(new ItemStack(wand, 1));
        texts.send(player, "module.territory.wand-given",
                "&a已给予选区斧（&f{0}&a）。左键 Pos1，右键 Pos2。", wand.name());
    }

    private void handlePos(Player player, boolean pos1) {
        if (!checkPermission(player, "guild.territory.claim")) {
            return;
        }
        if (!context.getPlugin().getMembershipRules().canManageGuild(player)) {
            texts.send(player, "module.territory.not-manager",
                    "&c仅公会管理可设置选区。");
            return;
        }

        TerritorySelectionManager.Session session = selections.of(player);
        Location loc = player.getLocation();
        if (pos1) {
            session.pos1 = loc;
            texts.send(player, "module.territory.pos1",
                    "&a[领地] Pos1: &f{0},{1},{2}",
                    loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        } else {
            session.pos2 = loc;
            texts.send(player, "module.territory.pos2",
                    "&a[领地] Pos2: &f{0},{1},{2}",
                    loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }
        if (selections.hasCompleteSelection(player)) {
            texts.send(player, "module.territory.selection-volume",
                    "&7选区体积: &f{0} &7方块", selections.selectionVolume(player));
        }
    }

    private void sendHelp(Player player) {
        texts.send(player, "module.territory.help-header", "&6/guild territory &7- 公会领地");
        texts.send(player, "module.territory.help-claim", "&eclaim &7- 声明选区为领地");
        texts.send(player, "module.territory.help-unclaim", "&eunclaim &7- 放弃当前世界领地");
        texts.send(player, "module.territory.help-info", "&einfo &7- 查看当前世界领地");
        texts.send(player, "module.territory.help-wand", "&ewand &7- 获取选区斧");
        texts.send(player, "module.territory.help-pos", "&epos1|pos2 &7- 以当前位置设角点");
    }

    private boolean isWorldAllowed(String worldName) {
        List<String> allowList = context.getConfig().getStringList("claim.allowed-worlds");
        if (allowList != null && !allowList.isEmpty()) {
            return allowList.stream().anyMatch(w -> w.equalsIgnoreCase(worldName));
        }
        List<String> denyList = context.getConfig().getStringList("claim.denied-worlds");
        if (denyList == null || denyList.isEmpty()) {
            return true;
        }
        return denyList.stream().noneMatch(w -> w.equalsIgnoreCase(worldName));
    }

    private boolean checkPermission(Player player, String permission) {
        if (context.getPlugin().getPermissionManager().hasPermission(player, permission)) {
            return true;
        }
        texts.send(player, "module.territory.no-permission",
                "&c你没有权限执行此操作。");
        return false;
    }

    private static Material parseMaterial(String name) {
        if (name == null || name.isBlank()) {
            return Material.WOODEN_AXE;
        }
        Material material = Material.matchMaterial(name.trim().toUpperCase());
        return material != null ? material : Material.WOODEN_AXE;
    }

    /** {@link TerritoryModule} 读取配置时复用。 */
    public static Material parseMaterialPublic(String name) {
        return parseMaterial(name);
    }
}
