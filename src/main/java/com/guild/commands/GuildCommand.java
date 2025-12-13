package com.guild.commands;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import com.guild.gui.MainGuildGUI;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.models.GuildRelation;
import com.guild.services.GuildService;
import com.guild.core.utils.CompatibleScheduler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Główne polecenie gildii
 */
public class GuildCommand implements CommandExecutor, TabCompleter {

    private final GuildPlugin plugin;

    public GuildCommand(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("general.player-only", "&cTo polecenie może być wykonane tylko przez gracza!")));
            return true;
        }

        if (args.length == 0) {
            // Otwórz główne GUI
            MainGuildGUI mainGuildGUI = new MainGuildGUI(plugin);
            plugin.getGuiManager().openGUI(player, mainGuildGUI);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                handleCreate(player, args);
                break;
            case "info":
                handleInfo(player);
                break;
            case "members":
                handleMembers(player);
                break;
            case "invite":
                handleInvite(player, args);
                break;
            case "kick":
                handleKick(player, args);
                break;
            case "promote":
                handlePromote(player, args);
                break;
            case "demote":
                handleDemote(player, args);
                break;
            case "accept":
                handleAccept(player, args);
                break;
            case "decline":
                handleDecline(player, args);
                break;
            case "leave":
                handleLeave(player);
                break;
            case "delete":
                handleDelete(player);
                break;
            case "sethome":
                handleSetHome(player);
                break;
            case "home":
                handleHome(player);
                break;
            case "relation":
                handleRelation(player, args);
                break;
            case "economy":
                handleEconomy(player, args);
                break;
            case "deposit":
                handleDeposit(player, args);
                break;
            case "withdraw":
                handleWithdraw(player, args);
                break;
            case "transfer":
                handleTransfer(player, args);
                break;
            case "logs":
                handleLogs(player, args);
                break;
            case "placeholder":
                handlePlaceholder(player, args);
                break;
            case "time":
                handleTime(player);
                break;
            case "help":
                handleHelp(player);
                break;
            default:
                player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("general.unknown-command", "&cNieznane polecenie! Użyj /guild help, aby zobaczyć pomoc.")));
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList(
                "create", "info", "members", "invite", "kick", "promote", "demote", "accept", "decline", "leave", "delete", "sethome", "home", "relation", "economy", "deposit", "withdraw", "transfer", "logs", "placeholder", "time", "help"
            );

            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "relation":
                    List<String> relationSubCommands = Arrays.asList("list", "create", "delete", "accept", "reject");
                    for (String cmd : relationSubCommands) {
                        if (cmd.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(cmd);
                        }
                    }
                    break;
                case "economy":
                    List<String> economySubCommands = Arrays.asList("info", "deposit", "withdraw", "transfer");
                    for (String cmd : economySubCommands) {
                        if (cmd.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(cmd);
                        }
                    }
                    break;
                case "invite":
                case "kick":
                case "promote":
                case "demote":
                    // Pobierz listę graczy online
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(player.getName());
                        }
                    }
                    break;
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String subSubCommand = args[1].toLowerCase();

            if (subCommand.equals("relation") && subSubCommand.equals("create")) {
                // Prosta podpowiedź dla tworzenia relacji
                List<String> suggestions = Arrays.asList("nazwa_gildii_docelowej");
                for (String suggestion : suggestions) {
                    if (suggestion.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(suggestion);
                    }
                }
            } else if (subCommand.equals("relation") && (subSubCommand.equals("delete") || subSubCommand.equals("accept") || subSubCommand.equals("reject"))) {
                // Prosta podpowiedź dla operacji relacji
                List<String> suggestions = Arrays.asList("nazwa_gildii");
                for (String suggestion : suggestions) {
                    if (suggestion.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(suggestion);
                    }
                }
            } else if (subCommand.equals("transfer")) {
                // Prosta podpowiedź dla przelewu
                List<String> suggestions = Arrays.asList("nazwa_gildii_docelowej");
                for (String suggestion : suggestions) {
                    if (suggestion.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(suggestion);
                    }
                }
            }
        } else if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            String subSubCommand = args[1].toLowerCase();

            if (subCommand.equals("relation") && subSubCommand.equals("create")) {
                // Podpowiedź typu relacji
                List<String> relationTypes = Arrays.asList("ally", "enemy", "war", "truce", "neutral");
                for (String type : relationTypes) {
                    if (type.toLowerCase().startsWith(args[3].toLowerCase())) {
                        completions.add(type);
                    }
                }
            } else if (subCommand.equals("deposit") || subCommand.equals("withdraw") ||
                      (subCommand.equals("transfer") && args.length == 4)) {
                // Sugestie kwot (tylko kilka popularnych)
                List<String> amounts = Arrays.asList("100", "500", "1000", "5000", "10000");
                for (String amount : amounts) {
                    if (amount.startsWith(args[3])) {
                        completions.add(amount);
                    }
                }
            }
        }

        return completions;
    }

    /**
     * Obsługa polecenia tworzenia gildii
     */
    private void handleCreate(Player player, String[] args) {
        // Sprawdź uprawnienia
        if (!plugin.getPermissionManager().hasPermission(player, "guild.create")) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.no-permission", "&cNie masz uprawnień do wykonania tej operacji!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (args.length < 2) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.usage", "&eUżycie: /guild create <nazwa_gildii> [tag] [opis]");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        String name = args[1];
        String tag = args.length > 2 ? args[2] : null;
        String description = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : null;

        // Walidacja danych wejściowych
        if (name.length() < 3 || name.length() > 20) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.name-too-short", "&cNazwa gildii jest za krótka! Wymagane minimum 3 znaki.");
            player.sendMessage(ColorUtils.colorize(message.replace("{min}", "3")));
            return;
        }

        if (tag != null && (tag.length() < 2 || tag.length() > 6)) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.tag-too-long", "&cTag gildii jest za długi! Maksymalnie 6 znaków.");
            player.sendMessage(ColorUtils.colorize(message.replace("{max}", "6")));
            return;
        }

        if (description != null && description.length() > 100) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.description-too-long", "&cOpis gildii nie może przekraczać 100 znaków!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Sprawdź system ekonomii
        double creationCost = plugin.getConfigManager().getConfig("config.yml").getDouble("guild.creation-cost", 5000.0);
        if (!plugin.getEconomyManager().isVaultAvailable()) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.economy-not-available", "&cSystem ekonomii jest niedostępny, nie można utworzyć gildii!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (!plugin.getEconomyManager().hasBalance(player, creationCost)) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.insufficient-funds", "&cNiewystarczające środki! Utworzenie gildii kosztuje &e{amount}!")
                .replace("{amount}", plugin.getEconomyManager().format(creationCost));
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cUsługa gildii nie została zainicjalizowana!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Pobierz opłatę za utworzenie
        if (!plugin.getEconomyManager().withdraw(player, creationCost)) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.payment-failed", "&cNie udało się pobrać opłaty za utworzenie!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Utwórz gildię (asynchronicznie)
        guildService.createGuildAsync(name, tag, description, player.getUniqueId(), player.getName())
            .thenAcceptAsync(success -> {
                if (success) {
                    String template = plugin.getConfigManager().getMessagesConfig().getString("create.success", "&aGildia {name} została utworzona pomyślnie!");
                    player.sendMessage(ColorUtils.replaceWithColorIsolation(template, "{name}", name));

                    String costMessage = plugin.getConfigManager().getMessagesConfig().getString("create.cost-info", "&eKoszt utworzenia: {amount}")
                        .replace("{amount}", plugin.getEconomyManager().format(creationCost));
                    player.sendMessage(ColorUtils.colorize(costMessage));

                    String nameMessage = plugin.getConfigManager().getMessagesConfig().getString("create.name-info", "&eNazwa gildii: {name}");
                    player.sendMessage(ColorUtils.colorize(nameMessage.replace("{name}", name)));

                    if (tag != null) {
                        String tagMessage = plugin.getConfigManager().getMessagesConfig().getString("create.tag-info", "&eTag gildii: [{tag}]");
                        player.sendMessage(ColorUtils.colorize(tagMessage.replace("{tag}", tag)));
                    }

                    if (description != null) {
                        String descMessage = plugin.getConfigManager().getMessagesConfig().getString("create.description-info", "&eOpis gildii: {description}");
                        player.sendMessage(ColorUtils.colorize(descMessage.replace("{description}", description)));
                    }
                } else {
                    // Zwrot pieniędzy
                    plugin.getEconomyManager().deposit(player, creationCost);
                    String failMessage = plugin.getConfigManager().getMessagesConfig().getString("create.failed", "&cTworzenie gildii nie powiodło się! Możliwe przyczyny:");
                    player.sendMessage(ColorUtils.colorize(failMessage));

                    String reason1 = plugin.getConfigManager().getMessagesConfig().getString("create.failed-reason-1", "&c- Nazwa gildii lub tag już istnieje");
                    String reason2 = plugin.getConfigManager().getMessagesConfig().getString("create.failed-reason-2", "&c- Jesteś już członkiem innej gildii");
                    player.sendMessage(ColorUtils.colorize(reason1));
                    player.sendMessage(ColorUtils.colorize(reason2));
                }
            }, runnable -> CompatibleScheduler.runTask(plugin, runnable));
    }

    /**
     * Obsługa polecenia informacji o gildii
     */
    private void handleInfo(Player player) {
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cUsługa gildii nie została zainicjalizowana!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("info.no-guild", "&cNie należysz jeszcze do żadnej gildii!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        GuildMember member = guildService.getGuildMember(player.getUniqueId());
        int memberCount = guildService.getGuildMemberCount(guild.getId());

        String header = plugin.getConfigManager().getMessagesConfig().getString("info.title", "&6=== Informacje o Gildii ===");
        player.sendMessage(ColorUtils.colorize(header));

        String nameMessage = plugin.getConfigManager().getMessagesConfig().getString("info.name", "&eNazwa: &f{name}");
        player.sendMessage(ColorUtils.colorize(nameMessage.replace("{name}", guild.getName())));

        if (guild.getTag() != null && !guild.getTag().isEmpty()) {
            String tagMessage = plugin.getConfigManager().getMessagesConfig().getString("info.tag", "&eTag: &f{tag}");
            player.sendMessage(ColorUtils.colorize(tagMessage.replace("{tag}", guild.getTag())));
        }
        if (guild.getDescription() != null && !guild.getDescription().isEmpty()) {
            String descMessage = plugin.getConfigManager().getMessagesConfig().getString("info.description", "&eOpis: &f{description}");
            player.sendMessage(ColorUtils.colorize(descMessage.replace("{description}", guild.getDescription())));
        }

        String leaderMessage = plugin.getConfigManager().getMessagesConfig().getString("info.leader", "&eLider: &f{leader}");
        player.sendMessage(ColorUtils.colorize(leaderMessage.replace("{leader}", guild.getLeaderName())));

        String membersMessage = plugin.getConfigManager().getMessagesConfig().getString("info.members", "&eLiczba członków: &f{count}/{max}");
        player.sendMessage(ColorUtils.colorize(membersMessage
            .replace("{count}", String.valueOf(memberCount))
            .replace("{max}", String.valueOf(guild.getMaxMembers()))));

        String roleMessage = plugin.getConfigManager().getMessagesConfig().getString("info.role", "&eTwoja rola: &f{role}");
        player.sendMessage(ColorUtils.colorize(roleMessage.replace("{role}", member.getRole().getDisplayName())));

        // Ujednolicenie formatu czasu TimeProvider
        java.time.format.DateTimeFormatter TF = com.guild.core.time.TimeProvider.FULL_FORMATTER;
        String createdMessage = plugin.getConfigManager().getMessagesConfig().getString("info.created", "&eData utworzenia: &f{date}");
        String createdFormatted = guild.getCreatedAt() != null ? guild.getCreatedAt().format(TF) : "Nieznana";
        player.sendMessage(ColorUtils.colorize(createdMessage.replace("{date}", createdFormatted)));
    }

    /**
     * Obsługa polecenia członków gildii
     */
    private void handleMembers(Player player) {
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cUsługa gildii nie została zainicjalizowana!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("info.no-guild", "&cNie należysz jeszcze do żadnej gildii!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        List<GuildMember> members = guildService.getGuildMembers(guild.getId());
        if (members.isEmpty()) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("members.no-members", "&cBrak członków w gildii!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        String title = plugin.getConfigManager().getMessagesConfig().getString("members.title", "&6=== Członkowie Gildii ===");
        player.sendMessage(ColorUtils.colorize(title));

        for (GuildMember member : members) {
            String status = "";
            Player onlinePlayer = Bukkit.getPlayer(member.getPlayerUuid());
            if (onlinePlayer != null) {
                status = "&a[Online]";
            } else {
                status = "&7[Offline]";
            }

            String memberFormat = plugin.getConfigManager().getMessagesConfig().getString("members.member-format", "&e{role} {name} &7- {status}");
            String memberMessage = memberFormat
                .replace("{role}", member.getRole().getDisplayName())
                .replace("{name}", member.getPlayerName())
                .replace("{status}", status);
            player.sendMessage(ColorUtils.colorize(memberMessage));
        }

        String totalMessage = plugin.getConfigManager().getMessagesConfig().getString("members.total", "&eŁącznie: {count} osób");
        player.sendMessage(ColorUtils.colorize(totalMessage.replace("{count}", String.valueOf(members.size()))));
    }

    /**
     * Obsługa polecenia zaproszenia
     */
    private void handleInvite(Player player, String[] args) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.invite")) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("invite.no-permission", "&cNie masz uprawnień do zapraszania graczy!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (args.length < 2) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("invite.usage", "&eUżycie: /guild invite <nazwa_gracza>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        String targetPlayerName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.player-not-found", "&cGracz {player} nie jest online!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }

        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cUsługa gildii nie została zainicjalizowana!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Sprawdź, czy zapraszający ma gildię
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("info.no-guild", "&cNie należysz jeszcze do żadnej gildii!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Sprawdź uprawnienia zapraszania (sterowane konfiguracją)
        if (!plugin.getPermissionManager().canInviteMembers(player)) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("invite.no-permission", "&cNie masz uprawnień do zapraszania graczy!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Sprawdź, czy cel ma gildię
        if (guildService.getPlayerGuild(targetPlayer.getUniqueId()) != null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("invite.already-in-guild", "&cGracz {player} jest już w innej gildii!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }

        // Sprawdź, czy zaprasza samego siebie
        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("invite.cannot-invite-self", "&cNie możesz zaprosić samego siebie!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Wyślij zaproszenie (asynchronicznie)
        guildService.sendInvitationAsync(guild.getId(), player.getUniqueId(), player.getName(), targetPlayer.getUniqueId(), targetPlayerName)
            .thenAcceptAsync(success -> {
                if (success) {
                    String sentMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.sent", "&aWysłano zaproszenie do gildii dla {player}!");
                    player.sendMessage(ColorUtils.colorize(sentMessage.replace("{player}", targetPlayerName)));

                    String inviteTitle = plugin.getConfigManager().getMessagesConfig().getString("invite.title", "&6=== Zaproszenie do Gildii ===");
                    targetPlayer.sendMessage(ColorUtils.colorize(inviteTitle));

                    String inviteMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.received", "&e{inviter} zaprasza cię do gildii: {guild}");
                    targetPlayer.sendMessage(ColorUtils.colorize(inviteMessage
                        .replace("{inviter}", player.getName())
                        .replace("{guild}", guild.getName())));

                    if (guild.getTag() != null && !guild.getTag().isEmpty()) {
                        String tagMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.guild-tag", "&eTag gildii: [{tag}]");
                        targetPlayer.sendMessage(ColorUtils.colorize(tagMessage.replace("{tag}", guild.getTag())));
                    }

                    String acceptMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.accept-command", "&eWpisz &a/guild accept {inviter} &eaby zaakceptować");
                    targetPlayer.sendMessage(ColorUtils.colorize(acceptMessage.replace("{inviter}", player.getName())));

                    String declineMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.decline-command", "&eWpisz &c/guild decline {inviter} &eaby odrzucić");
                    targetPlayer.sendMessage(ColorUtils.colorize(declineMessage.replace("{inviter}", player.getName())));
                } else {
                    String failMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.already-invited", "&c{player} już otrzymał zaproszenie!");
                    player.sendMessage(ColorUtils.colorize(failMessage.replace("{player}", targetPlayerName)));
                }
            }, runnable -> CompatibleScheduler.runTask(plugin, runnable));
    }

    /**
     * Obsługa polecenia wyrzucania
     */
    private void handleKick(Player player, String[] args) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.kick")) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("kick.no-permission", "&cNie masz uprawnień do wyrzucania graczy!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (args.length < 2) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("kick.usage", "&eUżycie: /guild kick <nazwa_gracza>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        String targetPlayerName = args[1];

        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cUsługa gildii nie została zainicjalizowana!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Sprawdź, czy wyrzucający ma gildię
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("info.no-guild", "&cNie należysz jeszcze do żadnej gildii!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Sprawdź uprawnienia wyrzucania (sterowane konfiguracją)
        if (!plugin.getPermissionManager().canKickMembers(player)) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("kick.no-permission", "&cNie masz uprawnień do wyrzucania graczy!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Znajdź gracza docelowego
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("kick.player-not-found", "&cGracz {player} nie jest online!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }

        // Sprawdź, czy cel jest w tej samej gildii
        GuildMember targetMember = guildService.getGuildMember(targetPlayer.getUniqueId());
        if (targetMember == null || targetMember.getGuildId() != guild.getId()) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("kick.not-in-guild", "&cGracz {player} nie jest w twojej gildii!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }

        // Sprawdź, czy wyrzuca samego siebie
        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("kick.cannot-kick-self", "&cNie możesz wyrzucić samego siebie! Użyj /guild leave aby opuścić gildię.");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Sprawdź, czy wyrzuca lidera
        if (targetMember.getRole() == GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("kick.cannot-kick-leader", "&cNie można wyrzucić lidera gildii!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Wykonaj wyrzucenie
        boolean success = guildService.removeGuildMember(targetPlayer.getUniqueId(), player.getUniqueId());
        if (success) {
            String successMessage = plugin.getConfigManager().getMessagesConfig().getString("kick.success", "&aWyrzucono {player} z gildii!");
            player.sendMessage(ColorUtils.colorize(successMessage.replace("{player}", targetPlayerName)));

            String kickedMessage = plugin.getConfigManager().getMessagesConfig().getString("kick.kicked", "&cZostałeś wyrzucony z gildii {guild}!");
            targetPlayer.sendMessage(ColorUtils.colorize(kickedMessage.replace("{guild}", guild.getName())));
        } else {
            String failMessage = plugin.getConfigManager().getMessagesConfig().getString("kick.failed", "&cWyrzucenie gracza nie powiodło się!");
            player.sendMessage(ColorUtils.colorize(failMessage));
        }
    }

    /**
     * Obsługa polecenia opuszczenia gildii
     */
    private void handleLeave(Player player) {
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cUsługa gildii nie została zainicjalizowana!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Sprawdź, czy gracz ma gildię
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("info.no-guild", "&cNie należysz jeszcze do żadnej gildii!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        GuildMember member = guildService.getGuildMember(player.getUniqueId());
        if (member == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("leave.member-error", "&cBłąd informacji o członku gildii!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Sprawdź, czy jest liderem
        if (member.getRole() == GuildMember.Role.LEADER) {
            String message1 = plugin.getConfigManager().getMessagesConfig().getString("leave.leader-cannot-leave", "&cLider gildii nie może opuścić gildii!");
            String message2 = plugin.getConfigManager().getMessagesConfig().getString("leave.use-delete", "&cJeśli chcesz rozwiązać gildię, użyj polecenia /guild delete.");
            player.sendMessage(ColorUtils.colorize(message1));
            player.sendMessage(ColorUtils.colorize(message2));
            return;
        }

        // Wykonaj opuszczenie
        boolean success = guildService.removeGuildMember(player.getUniqueId(), player.getUniqueId());
        if (success) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("leave.success-with-guild", "&aPomyślnie opuściłeś gildię: {guild}");
            player.sendMessage(ColorUtils.colorize(message.replace("{guild}", guild.getName())));
        } else {
            String message = plugin.getConfigManager().getMessagesConfig().getString("leave.failed", "&cOpuszczenie gildii nie powiodło się!");
            player.sendMessage(ColorUtils.colorize(message));
        }
    }

    /**
     * Obsługa polecenia usunięcia gildii
     */
    private void handleDelete(Player player) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.delete")) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("delete.no-permission", "&cNie masz uprawnień do usuwania gildii!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cUsługa gildii nie została zainicjalizowana!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Sprawdź, czy gracz ma gildię
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("info.no-guild", "&cNie należysz jeszcze do żadnej gildii!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Uprawnienia usunięcia zostały sprawdzone wcześniej i są kontrolowane przez system uprawnień

        // Potwierdź usunięcie
        String warningMessage = plugin.getConfigManager().getMessagesConfig().getString("delete.warning", "&cOstrzeżenie: Usunięcie gildii trwale ją rozwiąże, a wszyscy członkowie zostaną usunięci!");
        String confirmMessage = plugin.getConfigManager().getMessagesConfig().getString("delete.confirm-command", "&cJeśli jesteś pewien, że chcesz usunąć gildię, wpisz ponownie: /guild delete confirm");
        String cancelMessage = plugin.getConfigManager().getMessagesConfig().getString("delete.cancel-command", "&cLub wpisz: /guild delete cancel aby anulować operację");

        player.sendMessage(ColorUtils.colorize(warningMessage));
        player.sendMessage(ColorUtils.colorize(confirmMessage));
        player.sendMessage(ColorUtils.colorize(cancelMessage));

        // TODO: Zaimplementuj mechanizm potwierdzenia, aby uniknąć przypadkowego usunięcia
    }

    /**
     * Obsługa polecenia ustawienia domu gildii
     */
    private void handleSetHome(Player player) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.sethome")) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.no-permission", "&cNie masz uprawnień do wykonania tej operacji!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cUsługa gildii nie została zainicjalizowana!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Sprawdź, czy gracz ma gildię
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("info.no-guild", "&cNie należysz jeszcze do żadnej gildii!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Sprawdź, czy jest liderem
        if (!guildService.isGuildLeader(player.getUniqueId())) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("sethome.only-leader", "&cTylko lider gildii może ustawić dom gildii!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Ustaw dom gildii (asynchronicznie)
        guildService.setGuildHomeAsync(guild.getId(), player.getLocation(), player.getUniqueId())
            .thenAcceptAsync(success -> {
                if (success) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("sethome.success", "&aDom gildii został ustawiony pomyślnie!");
                    player.sendMessage(ColorUtils.colorize(message));
                } else {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("sethome.failed", "&cUstawianie domu gildii nie powiodło się!");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            }, runnable -> CompatibleScheduler.runTask(plugin, runnable));
    }

    /**
     * Obsługa polecenia teleportacji do domu gildii
     */
    private void handleHome(Player player) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.home")) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.no-permission", "&cNie masz uprawnień do wykonania tej operacji!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cUsługa gildii nie została zainicjalizowana!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Sprawdź, czy gracz ma gildię
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("info.no-guild", "&cNie należysz jeszcze do żadnej gildii!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Pobierz lokalizację domu gildii (asynchronicznie)
        guildService.getGuildHomeAsync(guild.getId())
            .thenAcceptAsync(homeLocation -> {
                if (homeLocation == null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("home.not-set", "&cDom gildii nie został jeszcze ustawiony!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // Teleportuj do domu gildii
                player.teleport(homeLocation);
                String message = plugin.getConfigManager().getMessagesConfig().getString("home.success", "&aTeleportowano do domu gildii!");
                player.sendMessage(ColorUtils.colorize(message));
            }, runnable -> CompatibleScheduler.runTask(plugin, runnable));
    }

    /**
     * Obsługa polecenia awansu
     */
    private void handlePromote(Player player, String[] args) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.promote")) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.promote.no-permission", "&cNie masz uprawnień do awansowania graczy!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (args.length < 2) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.promote.usage", "&eUżycie: /guild promote <gracz>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        String targetPlayerName = args[1];

        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cUsługa gildii nie została zainicjalizowana!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Sprawdź, czy awansujący ma gildię
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("info.no-guild", "&cNie należysz jeszcze do żadnej gildii!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Przeszło walidację węzła, konfiguracja sterowana, nie wymusza już "tylko lidera"

        // Znajdź gracza docelowego
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.promote.player-not-found", "&cGracz {player} nie jest online!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }

        // Sprawdź, czy cel jest w tej samej gildii
        GuildMember targetMember = guildService.getGuildMember(targetPlayer.getUniqueId());
        if (targetMember == null || targetMember.getGuildId() != guild.getId()) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.promote.not-in-guild", "&cGracz {player} nie jest w twojej gildii!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }

        // Sprawdź, czy awansuje samego siebie
        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.promote.cannot-promote-self", "&cNie możesz awansować samego siebie!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Sprawdź obecną rolę
        GuildMember.Role currentRole = targetMember.getRole();
        GuildMember.Role newRole = null;

        if (currentRole == GuildMember.Role.MEMBER) {
            newRole = GuildMember.Role.OFFICER;
        } else if (currentRole == GuildMember.Role.OFFICER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.promote.already-highest", "&cGracz {player} ma już najwyższą rangę!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }

        if (newRole != null) {
            // Wykonaj awans
            boolean success = guildService.updateMemberRole(targetPlayer.getUniqueId(), newRole, player.getUniqueId());
            if (success) {
                String successMessage = plugin.getConfigManager().getMessagesConfig().getString("permissions.promote.success", "&aAwansowano {player} na {role}!");
                player.sendMessage(ColorUtils.colorize(successMessage
                    .replace("{player}", targetPlayerName)
                    .replace("{role}", newRole.getDisplayName())));

                String promotedMessage = plugin.getConfigManager().getMessagesConfig().getString("permissions.promote.success", "&aZostałeś awansowany na {role}!");
                targetPlayer.sendMessage(ColorUtils.colorize(promotedMessage.replace("{role}", newRole.getDisplayName())));
            } else {
                String failMessage = plugin.getConfigManager().getMessagesConfig().getString("permissions.promote.cannot-promote", "&cNie można awansować tego gracza!");
                player.sendMessage(ColorUtils.colorize(failMessage));
            }
        }
    }

    /**
     * Obsługa polecenia degradacji
     */
    private void handleDemote(Player player, String[] args) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.demote")) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.demote.no-permission", "&cNie masz uprawnień do degradowania graczy!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (args.length < 2) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.demote.usage", "&eUżycie: /guild demote <gracz>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        String targetPlayerName = args[1];

        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cUsługa gildii nie została zainicjalizowana!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Sprawdź, czy degradujący ma gildię
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("info.no-guild", "&cNie należysz jeszcze do żadnej gildii!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Przeszło walidację węzła, konfiguracja sterowana, nie wymusza już "tylko lidera"

        // Znajdź gracza docelowego
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.demote.player-not-found", "&cGracz {player} nie jest online!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }

        // Sprawdź, czy cel jest w tej samej gildii
        GuildMember targetMember = guildService.getGuildMember(targetPlayer.getUniqueId());
        if (targetMember == null || targetMember.getGuildId() != guild.getId()) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.demote.not-in-guild", "&cGracz {player} nie jest w twojej gildii!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }

        // Sprawdź, czy degraduje samego siebie
        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.demote.cannot-demote-self", "&cNie możesz zdegradować samego siebie!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Sprawdź, czy degraduje lidera
        if (targetMember.getRole() == GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.demote.cannot-demote-leader", "&cNie można zdegradować lidera gildii!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Sprawdź obecną rolę
        GuildMember.Role currentRole = targetMember.getRole();
        GuildMember.Role newRole = null;

        if (currentRole == GuildMember.Role.OFFICER) {
            newRole = GuildMember.Role.MEMBER;
        } else if (currentRole == GuildMember.Role.MEMBER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.demote.already-lowest", "&cGracz {player} ma już najniższą rangę!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }

        if (newRole != null) {
            // Wykonaj degradację
            boolean success = guildService.updateMemberRole(targetPlayer.getUniqueId(), newRole, player.getUniqueId());
            if (success) {
                String successMessage = plugin.getConfigManager().getMessagesConfig().getString("permissions.demote.success", "&aZdegradowano {player} na {role}!");
                player.sendMessage(ColorUtils.colorize(successMessage
                    .replace("{player}", targetPlayerName)
                    .replace("{role}", newRole.getDisplayName())));

                String demotedMessage = plugin.getConfigManager().getMessagesConfig().getString("permissions.demote.success", "&aZostałeś zdegradowany na {role}!");
                targetPlayer.sendMessage(ColorUtils.colorize(demotedMessage.replace("{role}", newRole.getDisplayName())));
            } else {
                String failMessage = plugin.getConfigManager().getMessagesConfig().getString("permissions.demote.cannot-demote", "&cNie można zdegradować tego gracza!");
                player.sendMessage(ColorUtils.colorize(failMessage));
            }
        }
    }

    /**
     * Obsługa polecenia akceptacji zaproszenia
     */
    private void handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("invite.accept-command", "&eWpisz &a/guild accept {inviter} &eaby zaakceptować");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        String inviterName = args[1];
        Player inviter = Bukkit.getPlayer(inviterName);
        if (inviter == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.player-not-found", "&cGracz {player} nie jest online!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", inviterName)));
            return;
        }

        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cUsługa gildii nie została zainicjalizowana!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Przetwórz zaproszenie
        boolean success = guildService.processInvitation(player.getUniqueId(), inviter.getUniqueId(), true);
        if (success) {
            String successMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.accepted", "&aZaakceptowałeś zaproszenie do gildii {guild}!");
            player.sendMessage(ColorUtils.colorize(successMessage));

            String inviterMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.accepted", "&a{player} zaakceptował twoje zaproszenie!");
            inviter.sendMessage(ColorUtils.colorize(inviterMessage.replace("{player}", player.getName())));
        } else {
            String failMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.expired", "&cZaproszenie do gildii wygasło!");
            player.sendMessage(ColorUtils.colorize(failMessage));
        }
    }

    /**
     * Obsługa polecenia odrzucenia zaproszenia
     */
    private void handleDecline(Player player, String[] args) {
        if (args.length < 2) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("invite.decline-command", "&eWpisz &c/guild decline {inviter} &eaby odrzucić");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        String inviterName = args[1];
        Player inviter = Bukkit.getPlayer(inviterName);
        if (inviter == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.player-not-found", "&cGracz {player} nie jest online!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", inviterName)));
            return;
        }

        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cUsługa gildii nie została zainicjalizowana!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Przetwórz zaproszenie
        boolean success = guildService.processInvitation(player.getUniqueId(), inviter.getUniqueId(), false);
        if (success) {
            String successMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.declined", "&cOdrzuciłeś zaproszenie do gildii {guild}!");
            player.sendMessage(ColorUtils.colorize(successMessage));

            String inviterMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.declined", "&c{player} odrzucił twoje zaproszenie!");
            inviter.sendMessage(ColorUtils.colorize(inviterMessage.replace("{player}", player.getName())));
        } else {
            String failMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.expired", "&cZaproszenie do gildii wygasło!");
            player.sendMessage(ColorUtils.colorize(failMessage));
        }
    }

    /**
     * Obsługa polecenia pomocy
     */
    private void handleHelp(Player player) {
        String title = plugin.getConfigManager().getMessagesConfig().getString("help.title", "&6=== Pomoc Systemu Gildii ===");
        player.sendMessage(ColorUtils.colorize(title));

        String mainMenu = plugin.getConfigManager().getMessagesConfig().getString("help.main-menu", "&e/guild &7- Otwórz główne menu gildii");
        player.sendMessage(ColorUtils.colorize(mainMenu));

        String create = plugin.getConfigManager().getMessagesConfig().getString("help.create", "&e/guild create <nazwa> [tag] [opis] &7- Utwórz gildię");
        player.sendMessage(ColorUtils.colorize(create));

        String info = plugin.getConfigManager().getMessagesConfig().getString("help.info", "&e/guild info &7- Zobacz informacje o gildii");
        player.sendMessage(ColorUtils.colorize(info));

        String members = plugin.getConfigManager().getMessagesConfig().getString("help.members", "&e/guild members &7- Zobacz członków gildii");
        player.sendMessage(ColorUtils.colorize(members));

        String invite = plugin.getConfigManager().getMessagesConfig().getString("help.invite", "&e/guild invite <gracz> &7- Zaproś gracza do gildii");
        player.sendMessage(ColorUtils.colorize(invite));

        String kick = plugin.getConfigManager().getMessagesConfig().getString("help.kick", "&e/guild kick <gracz> &7- Wyrzuć członka z gildii");
        player.sendMessage(ColorUtils.colorize(kick));

        String promote = plugin.getConfigManager().getMessagesConfig().getString("help.promote", "&e/guild promote <gracz> &7- Awansuj członka gildii");
        player.sendMessage(ColorUtils.colorize(promote));

        String demote = plugin.getConfigManager().getMessagesConfig().getString("help.demote", "&e/guild demote <gracz> &7- Zdegraduj członka gildii");
        player.sendMessage(ColorUtils.colorize(demote));

        String accept = plugin.getConfigManager().getMessagesConfig().getString("help.accept", "&e/guild accept <zapraszający> &7- Zaakceptuj zaproszenie do gildii");
        player.sendMessage(ColorUtils.colorize(accept));

        String decline = plugin.getConfigManager().getMessagesConfig().getString("help.decline", "&e/guild decline <zapraszający> &7- Odrzuć zaproszenie do gildii");
        player.sendMessage(ColorUtils.colorize(decline));

        String leave = plugin.getConfigManager().getMessagesConfig().getString("help.leave", "&e/guild leave &7- Opuść gildię");
        player.sendMessage(ColorUtils.colorize(leave));

        String delete = plugin.getConfigManager().getMessagesConfig().getString("help.delete", "&e/guild delete &7- Usuń gildię");
        player.sendMessage(ColorUtils.colorize(delete));

        String sethome = plugin.getConfigManager().getMessagesConfig().getString("help.sethome", "&e/guild sethome &7- Ustaw dom gildii");
        player.sendMessage(ColorUtils.colorize(sethome));

        String home = plugin.getConfigManager().getMessagesConfig().getString("help.home", "&e/guild home &7- Teleportuj do domu gildii");
        player.sendMessage(ColorUtils.colorize(home));

        String help = plugin.getConfigManager().getMessagesConfig().getString("help.help", "&e/guild help &7- Pokaż tę pomoc");
        player.sendMessage(ColorUtils.colorize(help));

        String relation = "&e/guild relation &7- Zarządzaj relacjami gildii";
        player.sendMessage(ColorUtils.colorize(relation));

        String economy = "&e/guild economy &7- Zarządzaj ekonomią gildii";
        player.sendMessage(ColorUtils.colorize(economy));

        String deposit = "&e/guild deposit <kwota> &7- Wpłać środki do gildii";
        player.sendMessage(ColorUtils.colorize(deposit));

        String withdraw = "&e/guild withdraw <kwota> &7- Wypłać środki z gildii";
        player.sendMessage(ColorUtils.colorize(withdraw));

        String transfer = "&e/guild transfer <gildia> <kwota> &7- Przelej środki do innej gildii";
        player.sendMessage(ColorUtils.colorize(transfer));

        String logs = "&e/guild logs &7- Zobacz logi gildii";
        player.sendMessage(ColorUtils.colorize(logs));
    }

    /**
     * Obsługa polecenia relacji
     */
    private void handleRelation(Player player, String[] args) {
        // Pobierz gildię gracza
        Guild guild = plugin.getGuildService().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("relation.no-guild", "&cNie należysz jeszcze do żadnej gildii!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Sprawdź uprawnienia (tylko lider może zarządzać relacjami)
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("relation.only-leader", "&cTylko lider gildii może zarządzać relacjami!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (args.length == 1) {
            // Pokaż pomoc zarządzania relacjami
            showRelationHelp(player);
            return;
        }

        switch (args[1].toLowerCase()) {
            case "list":
                handleRelationList(player, guild);
                break;
            case "create":
                if (args.length < 4) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("relation.create-usage", "&eUżycie: /guild relation create <gildia_docelowa> <typ_relacji>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                handleRelationCreate(player, guild, args[2], args[3]);
                break;
            case "delete":
                if (args.length < 3) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("relation.delete-usage", "&eUżycie: /guild relation delete <gildia_docelowa>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                handleRelationDelete(player, guild, args[2]);
                break;
            case "accept":
                if (args.length < 3) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("relation.accept-usage", "&eUżycie: /guild relation accept <gildia_docelowa>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                handleRelationAccept(player, guild, args[2]);
                break;
            case "reject":
                if (args.length < 3) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("relation.reject-usage", "&eUżycie: /guild relation reject <gildia_docelowa>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                handleRelationReject(player, guild, args[2]);
                break;
            default:
                String message = plugin.getConfigManager().getMessagesConfig().getString("relation.unknown-subcommand", "&cNieznane podpolecenie! Użyj /guild relation aby zobaczyć pomoc.");
                player.sendMessage(ColorUtils.colorize(message));
                break;
        }
    }

    /**
     * Pokaż pomoc zarządzania relacjami
     */
    private void showRelationHelp(Player player) {
        String title = plugin.getConfigManager().getMessagesConfig().getString("relation.help-title", "&6=== Zarządzanie Relacjami Gildii ===");
        player.sendMessage(ColorUtils.colorize(title));

        String list = plugin.getConfigManager().getMessagesConfig().getString("relation.help-list", "&e/guild relation list &7- Zobacz wszystkie relacje");
        player.sendMessage(ColorUtils.colorize(list));

        String create = plugin.getConfigManager().getMessagesConfig().getString("relation.help-create", "&e/guild relation create <gildia> <typ> &7- Utwórz relację");
        player.sendMessage(ColorUtils.colorize(create));

        String delete = plugin.getConfigManager().getMessagesConfig().getString("relation.help-delete", "&e/guild relation delete <gildia> &7- Usuń relację");
        player.sendMessage(ColorUtils.colorize(delete));

        String accept = plugin.getConfigManager().getMessagesConfig().getString("relation.help-accept", "&e/guild relation accept <gildia> &7- Zaakceptuj prośbę o relację");
        player.sendMessage(ColorUtils.colorize(accept));

        String reject = plugin.getConfigManager().getMessagesConfig().getString("relation.help-reject", "&e/guild relation reject <gildia> &7- Odrzuć prośbę o relację");
        player.sendMessage(ColorUtils.colorize(reject));

        String types = plugin.getConfigManager().getMessagesConfig().getString("relation.help-types", "&7Typy relacji: &eally(sojusznik), enemy(wróg), war(wojna), truce(rozejm), neutral(neutralny)");
        player.sendMessage(ColorUtils.colorize(types));
    }

    /**
     * Obsługa listy relacji
     */
    private void handleRelationList(Player player, Guild guild) {
        plugin.getGuildService().getGuildRelationsAsync(guild.getId()).thenAccept(relations -> {
            if (relations == null || relations.isEmpty()) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("relation.no-relations", "&7Twoja gildia nie ma jeszcze żadnych relacji.");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            String title = plugin.getConfigManager().getMessagesConfig().getString("relation.list-title", "&6=== Lista Relacji Gildii ===");
            player.sendMessage(ColorUtils.colorize(title));

            for (GuildRelation relation : relations) {
                String otherGuildName = relation.getOtherGuildName(guild.getId());
                String status = relation.getStatus().name();
                String type = relation.getType().name();

                String relationInfo = plugin.getConfigManager().getMessagesConfig().getString("relation.list-format", "&e{other_guild} &7- {type} ({status})")
                    .replace("{other_guild}", otherGuildName)
                    .replace("{type}", type)
                    .replace("{status}", status);
                player.sendMessage(ColorUtils.colorize(relationInfo));
            }
        });
    }

    /**
     * Obsługa tworzenia relacji
     */
    private void handleRelationCreate(Player player, Guild guild, String targetGuildName, String relationTypeStr) {
        // Walidacja typu relacji
        GuildRelation.RelationType relationType;
        try {
            relationType = GuildRelation.RelationType.valueOf(relationTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("relation.invalid-type", "&cNieprawidłowy typ relacji! Prawidłowe typy: ally, enemy, war, truce, neutral");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Pobierz gildię docelową
        plugin.getGuildService().getGuildByNameAsync(targetGuildName).thenAccept(targetGuild -> {
            if (targetGuild == null) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("relation.target-not-found", "&cGildia docelowa {guild} nie istnieje!")
                    .replace("{guild}", targetGuildName);
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            if (targetGuild.getId() == guild.getId()) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("relation.cannot-relation-self", "&cNie można nawiązać relacji z samym sobą!");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            // Utwórz relację
            plugin.getGuildService().createGuildRelationAsync(guild.getId(), targetGuild.getId(), guild.getName(), targetGuild.getName(), relationType, player.getUniqueId(), player.getName())
                .thenAccept(success -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relation.create-success", "&aWysłano prośbę o relację {type} do {guild}!")
                            .replace("{guild}", targetGuildName)
                            .replace("{type}", relationType.name());
                        player.sendMessage(ColorUtils.colorize(message));
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relation.create-failed", "&cTworzenie relacji nie powiodło się! Relacja może już istnieć.");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
        });
    }

    /**
     * Obsługa usuwania relacji
     */
    private void handleRelationDelete(Player player, Guild guild, String targetGuildName) {
        // Pobierz gildię docelową
        plugin.getGuildService().getGuildByNameAsync(targetGuildName).thenAccept(targetGuild -> {
            if (targetGuild == null) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("relation.target-not-found", "&cGildia docelowa {guild} nie istnieje!")
                    .replace("{guild}", targetGuildName);
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            // Pobierz relację, a następnie usuń
            plugin.getGuildService().getGuildRelationAsync(guild.getId(), targetGuild.getId())
                .thenCompose(relation -> {
                    if (relation == null) {
                        return CompletableFuture.completedFuture(false);
                    }
                    return plugin.getGuildService().deleteGuildRelationAsync(relation.getId());
                })
                .thenAccept(success -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relation.delete-success", "&aUsunięto relację z {guild}!")
                            .replace("{guild}", targetGuildName);
                        player.sendMessage(ColorUtils.colorize(message));
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relation.delete-failed", "&cUsunięcie relacji nie powiodło się! Relacja może nie istnieć.");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
        });
    }

    /**
     * Obsługa akceptacji relacji
     */
    private void handleRelationAccept(Player player, Guild guild, String targetGuildName) {
        // Pobierz gildię docelową
        plugin.getGuildService().getGuildByNameAsync(targetGuildName).thenAccept(targetGuild -> {
            if (targetGuild == null) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("relation.target-not-found", "&cGildia docelowa {guild} nie istnieje!")
                    .replace("{guild}", targetGuildName);
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            // Pobierz relację, a następnie zaakceptuj
            plugin.getGuildService().getGuildRelationAsync(guild.getId(), targetGuild.getId())
                .thenCompose(relation -> {
                    if (relation == null) {
                        return CompletableFuture.completedFuture(false);
                    }
                    return plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), GuildRelation.RelationStatus.ACTIVE);
                })
                .thenAccept(success -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relation.accept-success", "&aZaakceptowano prośbę o relację od {guild}!")
                            .replace("{guild}", targetGuildName);
                        player.sendMessage(ColorUtils.colorize(message));
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relation.accept-failed", "&cAkceptacja relacji nie powiodła się! Może nie być oczekującej prośby.");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
        });
    }

    /**
     * Obsługa odrzucenia relacji
     */
    private void handleRelationReject(Player player, Guild guild, String targetGuildName) {
        // Pobierz gildię docelową
        plugin.getGuildService().getGuildByNameAsync(targetGuildName).thenAccept(targetGuild -> {
            if (targetGuild == null) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("relation.target-not-found", "&cGildia docelowa {guild} nie istnieje!")
                    .replace("{guild}", targetGuildName);
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            // Pobierz relację, a następnie odrzuć
            plugin.getGuildService().getGuildRelationAsync(guild.getId(), targetGuild.getId())
                .thenCompose(relation -> {
                    if (relation == null) {
                        return CompletableFuture.completedFuture(false);
                    }
                    return plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), GuildRelation.RelationStatus.CANCELLED);
                })
                .thenAccept(success -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relation.reject-success", "&cOdrzucono prośbę o relację od {guild}!")
                            .replace("{guild}", targetGuildName);
                        player.sendMessage(ColorUtils.colorize(message));
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relation.reject-failed", "&cOdrzucenie relacji nie powiodło się! Może nie być oczekującej prośby.");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
        });
    }

    /**
     * Obsługa polecenia ekonomii gildii
     */
    private void handleEconomy(Player player, String[] args) {
        // Pobierz gildię gracza
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            if (guild == null) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("economy.no-guild", "&cNie należysz jeszcze do żadnej gildii!");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            // Wyświetl informacje ekonomiczne gildii
            String message = plugin.getConfigManager().getMessagesConfig().getString("economy.info", "&6Informacje Ekonomiczne Gildii");
            player.sendMessage(ColorUtils.colorize(message));

            String balanceMessage = plugin.getConfigManager().getMessagesConfig().getString("economy.balance", "&7Aktualne saldo: &e{balance}")
                .replace("{balance}", plugin.getEconomyManager().format(guild.getBalance()));
            player.sendMessage(ColorUtils.colorize(balanceMessage));

            String levelMessage = plugin.getConfigManager().getMessagesConfig().getString("economy.level", "&7Aktualny poziom: &e{level}")
                .replace("{level}", String.valueOf(guild.getLevel()));
            player.sendMessage(ColorUtils.colorize(levelMessage));

            String maxMembersMessage = plugin.getConfigManager().getMessagesConfig().getString("economy.max-members", "&7Maks. członków: &e{max_members}")
                .replace("{max_members}", String.valueOf(guild.getMaxMembers()));
            player.sendMessage(ColorUtils.colorize(maxMembersMessage));
        });
    }

    /**
     * Obsługa polecenia wpłaty
     */
    private void handleDeposit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtils.colorize("&cUżycie: /guild deposit <kwota>"));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtils.colorize("&cFormat kwoty jest nieprawidłowy!"));
            return;
        }

        if (amount <= 0) {
            player.sendMessage(ColorUtils.colorize("&cKwota musi być większa od 0!"));
            return;
        }

        // Pobierz gildię gracza
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            if (guild == null) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("economy.no-guild", "&cNie należysz jeszcze do żadnej gildii!");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            // Sprawdź saldo gracza
            if (!plugin.getEconomyManager().hasBalance(player, amount)) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("economy.insufficient-balance", "&cNiewystarczające saldo!");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            // Wykonaj wpłatę
            plugin.getEconomyManager().withdraw(player, amount);
            plugin.getGuildService().updateGuildBalanceAsync(guild.getId(), guild.getBalance() + amount).thenAccept(success -> {
                if (success) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("economy.deposit-success", "&aPomyślnie wpłacono &e{amount} &ado gildii!")
                        .replace("{amount}", plugin.getEconomyManager().format(amount));
                    player.sendMessage(ColorUtils.colorize(message));
                } else {
                    // Zwrot pieniędzy
                    plugin.getEconomyManager().deposit(player, amount);
                    String message = plugin.getConfigManager().getMessagesConfig().getString("economy.deposit-failed", "&cWpłata nie powiodła się!");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }

    /**
     * Obsługa polecenia wypłaty
     */
    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtils.colorize("&cUżycie: /guild withdraw <kwota>"));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtils.colorize("&cFormat kwoty jest nieprawidłowy!"));
            return;
        }

        if (amount <= 0) {
            player.sendMessage(ColorUtils.colorize("&cKwota musi być większa od 0!"));
            return;
        }

        // Pobierz gildię gracza
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            if (guild == null) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("economy.no-guild", "&cNie należysz jeszcze do żadnej gildii!");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            // Sprawdź saldo gildii
            if (guild.getBalance() < amount) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("economy.guild-insufficient-balance", "&cNiewystarczające saldo gildii!");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            // Sprawdź uprawnienia (tylko lider może wypłacać)
            plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
                if (member == null || member.getRole() != GuildMember.Role.LEADER) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("economy.leader-only", "&cTylko lider gildii może wypłacać środki!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // Wykonaj wypłatę
                plugin.getGuildService().updateGuildBalanceAsync(guild.getId(), guild.getBalance() - amount).thenAccept(success -> {
                    if (success) {
                        plugin.getEconomyManager().deposit(player, amount);
                        String message = plugin.getConfigManager().getMessagesConfig().getString("economy.withdraw-success", "&aPomyślnie wypłacono &e{amount} &az gildii!")
                            .replace("{amount}", plugin.getEconomyManager().format(amount));
                        player.sendMessage(ColorUtils.colorize(message));
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("economy.withdraw-failed", "&cWypłata nie powiodła się!");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
            });
        });
    }

    /**
     * Obsługa polecenia przelewu
     */
    private void handleTransfer(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ColorUtils.colorize("&cUżycie: /guild transfer <gildia> <kwota>"));
            return;
        }

        String targetGuildName = args[1];
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtils.colorize("&cFormat kwoty jest nieprawidłowy!"));
            return;
        }

        if (amount <= 0) {
            player.sendMessage(ColorUtils.colorize("&cKwota musi być większa od 0!"));
            return;
        }

        // Pobierz gildię gracza
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(sourceGuild -> {
            if (sourceGuild == null) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("economy.no-guild", "&cNie należysz jeszcze do żadnej gildii!");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            // Sprawdź uprawnienia (tylko lider może robić przelewy)
            plugin.getGuildService().getGuildMemberAsync(sourceGuild.getId(), player.getUniqueId()).thenAccept(member -> {
                if (member == null || member.getRole() != GuildMember.Role.LEADER) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("economy.leader-only", "&cTylko lider gildii może wykonywać przelewy!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // Sprawdź saldo gildii
                if (sourceGuild.getBalance() < amount) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("economy.guild-insufficient-balance", "&cNiewystarczające saldo gildii!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // Znajdź gildię docelową
                plugin.getGuildService().getGuildByNameAsync(targetGuildName).thenAccept(targetGuild -> {
                    if (targetGuild == null) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("economy.target-guild-not-found", "&cGildia docelowa nie istnieje!");
                        player.sendMessage(ColorUtils.colorize(message));
                        return;
                    }

                    // Nie można przelać do siebie
                    if (sourceGuild.getId() == targetGuild.getId()) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("economy.cannot-transfer-to-self", "&cNie można przelać środków do własnej gildii!");
                        player.sendMessage(ColorUtils.colorize(message));
                        return;
                    }

                    // Wykonaj przelew
                    plugin.getGuildService().updateGuildBalanceAsync(sourceGuild.getId(), sourceGuild.getBalance() - amount).thenAccept(success1 -> {
                        if (success1) {
                            plugin.getGuildService().updateGuildBalanceAsync(targetGuild.getId(), targetGuild.getBalance() + amount).thenAccept(success2 -> {
                                if (success2) {
                                    String message = plugin.getConfigManager().getMessagesConfig().getString("economy.transfer-success", "&aPomyślnie przelano &e{amount} &ado gildii &e{target}!")
                                        .replace("{target}", targetGuildName)
                                        .replace("{amount}", plugin.getEconomyManager().format(amount));
                                    player.sendMessage(ColorUtils.colorize(message));
                                } else {
                                    // Wycofaj zmiany
                                    plugin.getGuildService().updateGuildBalanceAsync(sourceGuild.getId(), sourceGuild.getBalance() + amount);
                                    String message = plugin.getConfigManager().getMessagesConfig().getString("economy.transfer-failed", "&cPrzelew nie powiódł się!");
                                    player.sendMessage(ColorUtils.colorize(message));
                                }
                            });
                        } else {
                            String message = plugin.getConfigManager().getMessagesConfig().getString("economy.transfer-failed", "&cPrzelew nie powiódł się!");
                            player.sendMessage(ColorUtils.colorize(message));
                        }
                    });
                });
            });
        });
    }

    /**
     * Obsługa polecenia logów
     */
    private void handleLogs(Player player, String[] args) {
        // Pobierz gildię gracza
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            if (guild == null) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("general.no-guild", "&cNie należysz jeszcze do żadnej gildii!");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            // Sprawdź uprawnienia
            plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
                if (member == null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("general.no-permission", "&cNiewystarczające uprawnienia!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // Otwórz GUI logów gildii
                plugin.getGuiManager().openGUI(player, new com.guild.gui.GuildLogsGUI(plugin, guild, player));
            });
        });
    }

    /**
     * Obsługa polecenia testu placeholderów
     */
    private void handlePlaceholder(Player player, String[] args) {
        if (!player.hasPermission("guild.admin")) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("general.no-permission", "&cNie masz uprawnień do wykonania tej operacji!")));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ColorUtils.colorize("&cUżycie: /guild placeholder <nazwa_zmiennej>"));
            player.sendMessage(ColorUtils.colorize("&ePrzykład: /guild placeholder name"));
            player.sendMessage(ColorUtils.colorize("&eDostępne zmienne: name, tag, description, leader, membercount, role, hasguild, isleader, isofficer"));
            return;
        }

        String placeholder = "%guild_" + args[1] + "%";
        String result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, placeholder);

        player.sendMessage(ColorUtils.colorize("&6=== Test PlaceholderAPI ==="));
        player.sendMessage(ColorUtils.colorize("&eZmienna: &f" + placeholder));
        player.sendMessage(ColorUtils.colorize("&eWynik: &f" + result));
        player.sendMessage(ColorUtils.colorize("&6========================"));
    }

    /**
     * /guild time: Wyświetla rzeczywisty czas systemowy i aktualny czas w świecie gry
     */
    private void handleTime(Player player) {
        String title = plugin.getConfigManager().getMessagesConfig().getString("time.title", "&6=== Test Czasu ===");
        String realNow = com.guild.core.time.TimeProvider.nowString();
        // Czas świata Minecraft (cykl dzienny 0-23999 ticks)
        long ticks = player.getWorld().getTime() % 24000L;
        int hours = (int)((ticks / 1000L + 6) % 24); // 0 tick odpowiada 06:00
        int minutes = (int)((ticks % 1000L) * 60L / 1000L);
        String gameTime = String.format("%02d:%02d", hours, minutes);
        String ticksStr = String.valueOf(ticks);
        player.sendMessage(ColorUtils.colorize(title));
        player.sendMessage(ColorUtils.colorize("&eCzas rzeczywisty: &f" + realNow));
        player.sendMessage(ColorUtils.colorize("&eCzas gry: &f" + gameTime + " &7(" + ticksStr + " ticks)"));
    }
}
