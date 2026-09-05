package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.gui.base.AbstractConfirmGUI;
import com.guild.models.Guild;
import com.guild.models.GuildContribution;
import com.guild.models.GuildLog;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 确认变更公会资金 GUI */
public class ConfirmChangeFundsGUI extends AbstractConfirmGUI {

    private final String operationType;
    private final double amount;

    public ConfirmChangeFundsGUI(GuildPlugin plugin, Guild guild, Player player,
                                 String operationType, double amount) {
        super(plugin, guild, player, "EconomyManagementGUI");
        this.operationType = operationType;
        this.amount = amount;
    }

    @Override
    protected String infoFunctionName() {
        return FUNC_DETAILS;
    }

    @Override
    protected String titleKey() {
        return "gui.confirm-funds.title";
    }

    @Override
    protected String titleDefault() {
        return "&6Confirm Fund Change";
    }

    @Override
    protected String bedrockTitleKey() {
        return "gui.confirm-funds.bedrock-title";
    }

    @Override
    protected String bedrockTitleDefault() {
        return "&6Confirm Fund Change";
    }

    @Override
    protected String bedrockContentKey() {
        return "gui.confirm-funds.bedrock-content";
    }

    @Override
    protected String bedrockContentDefault() {
        return "";
    }

    @Override
    protected String bedrockConfirmKey() {
        return "gui.confirm-funds.bedrock-confirm";
    }

    @Override
    protected String bedrockConfirmDefault() {
        return "&aConfirm Change";
    }

    @Override
    protected String bedrockCancelKey() {
        return "gui.confirm-funds.bedrock-cancel";
    }

    @Override
    protected String bedrockCancelDefault() {
        return "&cCancel";
    }

    @Override
    protected Material confirmMaterial() {
        return Material.EMERALD_BLOCK;
    }

    @Override
    protected Material cancelMaterial() {
        return Material.REDSTONE_BLOCK;
    }

    @Override
    protected String confirmButtonKey() {
        return "gui.confirm-funds.confirm";
    }

    @Override
    protected String confirmButtonDefault() {
        return "&aConfirm";
    }

    @Override
    protected String confirmLoreKey() {
        return "gui.confirm-funds.confirm-desc";
    }

    @Override
    protected String confirmLoreDefault() {
        return "Execute balance change";
    }

    @Override
    protected String cancelButtonKey() {
        return "gui.confirm-funds.cancel";
    }

    @Override
    protected String cancelButtonDefault() {
        return "&cCancel";
    }

    @Override
    protected String cancelLoreKey() {
        return "gui.confirm-funds.cancel-desc";
    }

    @Override
    protected String cancelLoreDefault() {
        return "Cancel change";
    }

    @Override
    protected ItemStack createInfoItem() {
        double currentBalance = guild.getBalance();
        double newBalance = calculateNewBalance(currentBalance);
        String operationName = getOperationName();

        List<String> infoLore = new ArrayList<>();
        infoLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.confirm-funds.guild", "Guild") + ": &e" + guild.getName()));
        infoLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.confirm-funds.operation", "Operation") + ": " + operationName));
        infoLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.confirm-funds.current-balance", "Current Balance") + ": &6"
                + plugin.getEconomyManager().format(currentBalance)));
        infoLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.confirm-funds.amount", "Amount") + ": &f"
                + plugin.getEconomyManager().format(amount)));
        infoLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.confirm-funds.new-balance", "New Balance") + ": &a"
                + plugin.getEconomyManager().format(newBalance)));

        return createItem(getOperationMaterial(),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.confirm-funds.info-title", "&6Change Details")),
                infoLore.toArray(new String[0]));
    }

    @Override
    protected String[] bedrockContentPlaceholders() {
        return new String[0];
    }

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) {
            return false;
        }

        double currentBalance = guild.getBalance();
        double newBalance = calculateNewBalance(currentBalance);
        String operationName = ColorUtils.colorize(getOperationName());

        String content = languageManager.getGuiColoredMessage(player, "gui.confirm-funds.bedrock-guild",
                "&fGuild: &e{guild_name}", "{guild_name}", guild.getName()) + "\n"
                + languageManager.getGuiColoredMessage(player, "gui.confirm-funds.bedrock-operation",
                "&fOperation: {operation}", "{operation}", operationName) + "\n"
                + languageManager.getGuiColoredMessage(player, "gui.confirm-funds.bedrock-current-balance",
                "&fCurrent Balance: &6{balance}", "{balance}",
                plugin.getEconomyManager().format(currentBalance)) + "\n"
                + languageManager.getGuiColoredMessage(player, "gui.confirm-funds.bedrock-amount",
                "&fAmount: &f{amount}", "{amount}", plugin.getEconomyManager().format(amount)) + "\n"
                + languageManager.getGuiColoredMessage(player, "gui.confirm-funds.bedrock-new-balance",
                "&fNew Balance: &a{balance}", "{balance}",
                plugin.getEconomyManager().format(newBalance));

        SimpleForm form = SimpleForm.builder()
                .title(languageManager.getGuiColoredMessage(player, bedrockTitleKey(), bedrockTitleDefault()))
                .content(content)
                .button(languageManager.getGuiColoredMessage(player, bedrockConfirmKey(), bedrockConfirmDefault()))
                .button(languageManager.getGuiColoredMessage(player, bedrockCancelKey(), bedrockCancelDefault()))
                .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    if (response.clickedButtonId() == 0) {
                        onConfirm(player);
                    } else {
                        onCancel(player);
                    }
                }))
                .closedResultHandler(() -> CompatibleScheduler.runTask(plugin, player, () -> onCancel(player)))
                .build();

        return BedrockFormSender.sendForm(player.getUniqueId(), form);
    }

    @Override
    protected void onConfirm(Player player) {
        executeChange(player);
    }

    @Override
    protected void onCancel(Player player) {
        returnToEconomyManagement(player);
    }

    private void executeChange(Player player) {
        double currentBalance = guild.getBalance();
        double newBalance = calculateNewBalance(currentBalance);

        plugin.getGuildService().updateGuildBalanceAsync(guild.getId(), newBalance,
                        player.getUniqueId().toString(), player.getName())
                .thenAccept(success -> CompatibleScheduler.runTask(plugin, player, () -> {
                    if (success) {
                        String formattedAmount = String.format("%.2f", amount);
                        String playerName = player.getName();
                        String guildName = guild.getName();

                        GuildContribution.ContributionType contribType;
                        String contribDesc;
                        GuildLog.LogType logType;
                        String logDesc;

                        switch (operationType) {
                            case "add":
                                contribType = GuildContribution.ContributionType.DEPOSIT;
                                contribDesc = languageManager.getGuiMessage(player,
                                                "gui.confirm-funds.funds.change.add-contrib",
                                                "{player} added {amount}")
                                        .replace("{player}", playerName)
                                        .replace("{amount}", formattedAmount);
                                logType = GuildLog.LogType.FUND_DEPOSITED;
                                logDesc = languageManager.getGuiMessage(player,
                                                "gui.confirm-funds.funds.change.add-log",
                                                "{player} added {amount} to {guild}")
                                        .replace("{player}", playerName)
                                        .replace("{guild}", guildName)
                                        .replace("{amount}", formattedAmount);
                                break;
                            case "remove":
                                contribType = GuildContribution.ContributionType.WITHDRAW;
                                contribDesc = languageManager.getGuiMessage(player,
                                                "gui.confirm-funds.funds.change.remove-contrib",
                                                "{player} removed {amount}")
                                        .replace("{player}", playerName)
                                        .replace("{amount}", formattedAmount);
                                logType = GuildLog.LogType.FUND_WITHDRAWN;
                                logDesc = languageManager.getGuiMessage(player,
                                                "gui.confirm-funds.funds.change.remove-log",
                                                "{player} removed {amount} from {guild}")
                                        .replace("{player}", playerName)
                                        .replace("{guild}", guildName)
                                        .replace("{amount}", formattedAmount);
                                break;
                            default:
                                double diff = newBalance - currentBalance;
                                String diffStr = String.format("%.2f", Math.abs(diff));
                                contribType = GuildContribution.ContributionType.ADMIN;
                                logType = diff >= 0 ? GuildLog.LogType.FUND_DEPOSITED : GuildLog.LogType.FUND_WITHDRAWN;
                                String newBalanceStr = String.format("%.2f", newBalance);

                                if (diff >= 0) {
                                    contribDesc = languageManager.getGuiMessage(player,
                                                    "gui.confirm-funds.funds.change.set-contrib-increase",
                                                    "{player} set funds to {new}(+{diff})")
                                            .replace("{player}", playerName)
                                            .replace("{new}", newBalanceStr)
                                            .replace("{diff}", diffStr);
                                    logDesc = languageManager.getGuiMessage(player,
                                                    "gui.confirm-funds.funds.change.set-log-increase",
                                                    "{player} set {guild} funds to {new}")
                                            .replace("{player}", playerName)
                                            .replace("{guild}", guildName)
                                            .replace("{new}", newBalanceStr);
                                } else {
                                    contribDesc = languageManager.getGuiMessage(player,
                                                    "gui.confirm-funds.funds.change.set-contrib-decrease",
                                                    "{player} set funds to {new}(-{diff})")
                                            .replace("{player}", playerName)
                                            .replace("{new}", newBalanceStr)
                                            .replace("{diff}", diffStr);
                                    logDesc = languageManager.getGuiMessage(player,
                                                    "gui.confirm-funds.funds.change.set-log-decrease",
                                                    "{player} set {guild} funds to {new}")
                                            .replace("{player}", playerName)
                                            .replace("{guild}", guildName)
                                            .replace("{new}", newBalanceStr);
                                }
                                break;
                        }

                        plugin.getGuildService().addGuildContributionAsync(
                                guild.getId(), player.getUniqueId(), player.getName(),
                                amount, contribType, contribDesc);

                        plugin.getGuildService().logGuildActionAsync(
                                guild.getId(), guild.getName(),
                                player.getUniqueId().toString(), player.getName(),
                                logType, logDesc,
                                languageManager.getGuiMessage(player,
                                                "gui.confirm-funds.funds.change.log-details",
                                                "Amount:{amount}")
                                        .replace("{amount}", formattedAmount));

                        player.sendMessage(ColorUtils.colorize("&a" + languageManager.getGuiMessage(player,
                                "gui.confirm-funds.success", "&aFund change successful!")));
                    } else {
                        player.sendMessage(ColorUtils.colorize("&c" + languageManager.getGuiMessage(player,
                                "gui.confirm-funds.failed", "&cFund change failed!")));
                    }
                    returnToEconomyManagement(player);
                }));
    }

    private void returnToEconomyManagement(Player player) {
        plugin.getGuiManager().openGUI(player, new EconomyManagementGUI(plugin, player));
    }

    private double calculateNewBalance(double currentBalance) {
        return switch (operationType) {
            case "set" -> amount;
            case "add" -> currentBalance + amount;
            case "remove" -> Math.max(0, currentBalance - amount);
            default -> currentBalance;
        };
    }

    private String getOperationName() {
        String key = "gui.confirm-funds.operation-" + operationType;
        return switch (operationType) {
            case "set" -> languageManager.getGuiMessage(viewer, key, "&eSet funds");
            case "add" -> languageManager.getGuiMessage(viewer, key, "&aAdd funds");
            case "remove" -> languageManager.getGuiMessage(viewer, key, "&cDeduct funds");
            default -> operationType;
        };
    }

    private Material getOperationMaterial() {
        return switch (operationType) {
            case "set" -> Material.GOLD_BLOCK;
            case "add" -> Material.EMERALD_BLOCK;
            case "remove" -> Material.LAVA_BUCKET;
            default -> Material.PAPER;
        };
    }
}
