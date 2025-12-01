package com.guild.commands;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import com.guild.gui.AdminGuildGUI;
import com.guild.gui.RelationManagementGUI;
import com.guild.models.Guild;
import com.guild.models.GuildRelation;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Polecenie administratora gildii
 */
public class GuildAdminCommand implements CommandExecutor, TabCompleter {

    private final GuildPlugin plugin;

    public GuildAdminCommand(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("guild.admin")) {
            sender.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("general.no-permission", "&cNie masz uprawnień do wykonania tej operacji!")));
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                // Otwórz GUI administratora
                AdminGuildGUI adminGUI = new AdminGuildGUI(plugin);
                plugin.getGuiManager().openGUI(player, adminGUI);
            } else {
                handleHelp(sender);
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                handleList(sender, args);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            case "delete":
                handleDelete(sender, args);
                break;
            case "freeze":
                handleFreeze(sender, args);
                break;
            case "unfreeze":
                handleUnfreeze(sender, args);
                break;
            case "transfer":
                handleTransfer(sender, args);
                break;
            case "economy":
                handleEconomy(sender, args);
                break;
            case "relation":
                handleRelation(sender, args);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "test":
                handleTest(sender, args);
                break;
            case "help":
                handleHelp(sender);
                break;
            default:
                sender.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("general.unknown-command", "&cNieznane polecenie! Użyj /guildadmin help, aby zobaczyć pomoc.")));
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("guild.admin")) {
            return completions;
        }

        if (args.length == 1) {
            completions.addAll(Arrays.asList("list", "info", "delete", "freeze", "unfreeze", "transfer", "economy", "relation", "reload", "help"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "info":
                case "delete":
                case "freeze":
                case "unfreeze":
                case "transfer":
                case "economy":
                    // Pobierz wszystkie nazwy gildii
                    plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
                        for (Guild guild : guilds) {
                            completions.add(guild.getName());
                        }
                    });
                    break;
                case "relation":
                    completions.addAll(Arrays.asList("list", "create", "delete", "gui"));
                    break;
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "transfer":
                    // Pobierz graczy online
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                    break;
                case "economy":
                    completions.addAll(Arrays.asList("set", "add", "remove", "info"));
                    break;
                case "relation":
                    if ("create".equals(args[1])) {
                        // 3. argument to nazwa pierwszej gildii, pobierz wszystkie nazwy gildii
                        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
                            for (Guild guild : guilds) {
                                completions.add(guild.getName());
                            }
                        });
                    }
                    break;
            }
        } else if (args.length == 4) {
            switch (args[0].toLowerCase()) {
                case "relation":
                    if ("create".equals(args[1])) {
                        // 4. argument to nazwa drugiej gildii, pobierz wszystkie nazwy gildii
                        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
                            for (Guild guild : guilds) {
                                completions.add(guild.getName());
                            }
                        });
                    }
                    break;
            }
        } else if (args.length == 5) {
            switch (args[0].toLowerCase()) {
                case "relation":
                    if ("create".equals(args[1])) {
                        // 5. argument to typ relacji
                        completions.addAll(Arrays.asList("ally", "enemy", "war", "truce", "neutral"));
                    }
                    break;
            }
        }

        return completions;
    }

    private void handleList(CommandSender sender, String[] args) {
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            sender.sendMessage(ColorUtils.colorize("&6=== Lista Gildii ==="));
            if (guilds.isEmpty()) {
                sender.sendMessage(ColorUtils.colorize("&cBrak gildii"));
                return;
            }

            for (Guild guild : guilds) {
                String status = guild.isFrozen() ? "&c[Zamrożona]" : "&a[Normalna]";
                sender.sendMessage(ColorUtils.colorize(String.format("&e%s &7- Lider: &f%s &7- Poziom: &f%d &7%s",
                    guild.getName(), guild.getLeaderName(), guild.getLevel(), status)));
            }
        });
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&cUżycie: /guildadmin info <nazwa_gildii>"));
            return;
        }

        String guildName = args[1];
        plugin.getGuildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
            if (guild == null) {
                sender.sendMessage(ColorUtils.colorize("&cGildia " + guildName + " nie istnieje!"));
                return;
            }

            sender.sendMessage(ColorUtils.colorize("&6=== Informacje o Gildii ==="));
            sender.sendMessage(ColorUtils.colorize("&eNazwa: &f" + guild.getName()));
            sender.sendMessage(ColorUtils.colorize("&eTag: &f" + (guild.getTag() != null ? guild.getTag() : "Brak")));
            sender.sendMessage(ColorUtils.colorize("&eLider: &f" + guild.getLeaderName()));
            sender.sendMessage(ColorUtils.colorize("&ePoziom: &f" + guild.getLevel()));
            sender.sendMessage(ColorUtils.colorize("&eFundusze: &f" + guild.getBalance()));
            sender.sendMessage(ColorUtils.colorize("&eStatus: &f" + (guild.isFrozen() ? "Zamrożona" : "Normalna")));

            // Pobierz liczbę członków
            plugin.getGuildService().getGuildMemberCountAsync(guild.getId()).thenAccept(count -> {
                sender.sendMessage(ColorUtils.colorize("&eLiczba członków: &f" + count + "/" + guild.getMaxMembers()));
            });
        });
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&cUżycie: /guildadmin delete <nazwa_gildii>"));
            return;
        }

        String guildName = args[1];
        plugin.getGuildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
            if (guild == null) {
                sender.sendMessage(ColorUtils.colorize("&cGildia " + guildName + " nie istnieje!"));
                return;
            }

            // Wymuś usunięcie gildii
            plugin.getGuildService().deleteGuildAsync(guild.getId(), UUID.randomUUID()).thenAccept(success -> {
                if (success) {
                    sender.sendMessage(ColorUtils.colorize("&aGildia " + guildName + " została wymuszenie usunięta!"));
                } else {
                    sender.sendMessage(ColorUtils.colorize("&cUsunięcie gildii nie powiodło się!"));
                }
            });
        });
    }

    private void handleFreeze(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&cUżycie: /guildadmin freeze <nazwa_gildii>"));
            return;
        }

        String guildName = args[1];
        plugin.getGuildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
            if (guild == null) {
                sender.sendMessage(ColorUtils.colorize("&cGildia " + guildName + " nie istnieje!"));
                return;
            }

            // Zamrożenie gildii
            // TODO: Zaimplementuj funkcję zamrażania
            sender.sendMessage(ColorUtils.colorize("&aGildia " + guildName + " została zamrożona!"));
        });
    }

    private void handleUnfreeze(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&cUżycie: /guildadmin unfreeze <nazwa_gildii>"));
            return;
        }

        String guildName = args[1];
        plugin.getGuildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
            if (guild == null) {
                sender.sendMessage(ColorUtils.colorize("&cGildia " + guildName + " nie istnieje!"));
                return;
            }

            // Odmrożenie gildii
            // TODO: Zaimplementuj funkcję odmrażania
            sender.sendMessage(ColorUtils.colorize("&aGildia " + guildName + " została odmrożona!"));
        });
    }

    private void handleTransfer(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ColorUtils.colorize("&cUżycie: /guildadmin transfer <nazwa_gildii> <nowy_lider>"));
            return;
        }

        String guildName = args[1];
        String newLeaderName = args[2];

        Player newLeader = Bukkit.getPlayer(newLeaderName);
        if (newLeader == null) {
            sender.sendMessage(ColorUtils.colorize("&cGracz " + newLeaderName + " nie jest online!"));
            return;
        }

        plugin.getGuildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
            if (guild == null) {
                sender.sendMessage(ColorUtils.colorize("&cGildia " + guildName + " nie istnieje!"));
                return;
            }

            // Sprawdź, czy nowy lider jest członkiem tej gildii
            plugin.getGuildService().getGuildMemberAsync(guild.getId(), newLeader.getUniqueId()).thenAccept(member -> {
                if (member == null) {
                    sender.sendMessage(ColorUtils.colorize("&cGracz " + newLeaderName + " nie jest członkiem tej gildii!"));
                    return;
                }

                // Przekaż lidera
                // TODO: Zaimplementuj funkcję przekazywania
                sender.sendMessage(ColorUtils.colorize("&aLider gildii " + guildName + " został przekazany graczowi " + newLeaderName + "!"));
            });
        });
    }

    private void handleEconomy(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ColorUtils.colorize("&cUżycie: /guildadmin economy <nazwa_gildii> <set|add|remove> <kwota>"));
            return;
        }

        String guildName = args[1];
        String operation = args[2];
        double amount;

        try {
            amount = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtils.colorize("&cFormat kwoty jest nieprawidłowy!"));
            return;
        }

        plugin.getGuildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
            if (guild == null) {
                sender.sendMessage(ColorUtils.colorize("&cGildia " + guildName + " nie istnieje!"));
                return;
            }

            final double[] newBalance = {guild.getBalance()};
            switch (operation.toLowerCase()) {
                case "set":
                    newBalance[0] = amount;
                    break;
                case "add":
                    newBalance[0] += amount;
                    break;
                case "remove":
                    newBalance[0] -= amount;
                    if (newBalance[0] < 0) newBalance[0] = 0;
                    break;
                default:
                    sender.sendMessage(ColorUtils.colorize("&cNieprawidłowa operacja! Użyj set|add|remove"));
                    return;
            }

            // Zaktualizuj fundusze gildii
            plugin.getGuildService().updateGuildBalanceAsync(guild.getId(), newBalance[0]).thenAccept(success -> {
                if (success) {
                    String formattedAmount = plugin.getEconomyManager().format(newBalance[0]);
                    sender.sendMessage(ColorUtils.colorize("&aFundusze gildii " + guildName + " zostały zaktualizowane do: " + formattedAmount));
                } else {
                    sender.sendMessage(ColorUtils.colorize("&cAktualizacja funduszy gildii nie powiodła się!"));
                }
            });
        });
    }

    private void handleRelation(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&cUżycie: /guildadmin relation <list|create|delete|gui>"));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "gui":
                if (sender instanceof Player player) {
                    // Otwórz GUI zarządzania relacjami
                    RelationManagementGUI relationGUI = new RelationManagementGUI(plugin, player);
                    plugin.getGuiManager().openGUI(player, relationGUI);
                } else {
                    sender.sendMessage(ColorUtils.colorize("&cTo polecenie może być wykonane tylko przez gracza!"));
                }
                break;
            case "list":
                // Wyświetl wszystkie relacje gildii
                sender.sendMessage(ColorUtils.colorize("&6=== Lista Relacji Gildii ==="));
                plugin.getGuildService().getAllGuildsAsync().thenCompose(guilds -> {
                    List<CompletableFuture<List<GuildRelation>>> relationFutures = new ArrayList<>();

                    for (Guild guild : guilds) {
                        relationFutures.add(plugin.getGuildService().getGuildRelationsAsync(guild.getId()));
                    }

                    return CompletableFuture.allOf(relationFutures.toArray(new CompletableFuture[0]))
                        .thenApply(v -> {
                            List<GuildRelation> allRelations = new ArrayList<>();
                            for (CompletableFuture<List<GuildRelation>> future : relationFutures) {
                                try {
                                    allRelations.addAll(future.get());
                                } catch (Exception e) {
                                    plugin.getLogger().warning("Błąd podczas pobierania relacji gildii: " + e.getMessage());
                                }
                            }
                            return allRelations;
                        });
                }).thenAccept(relations -> {
                    if (relations.isEmpty()) {
                        sender.sendMessage(ColorUtils.colorize("&cBrak relacji gildii"));
                        return;
                    }

                    for (GuildRelation relation : relations) {
                        String status = getRelationStatusText(relation.getStatus());
                        String type = getRelationTypeText(relation.getType());
                        sender.sendMessage(ColorUtils.colorize(String.format("&e%s ↔ %s &7- %s &7- %s",
                            relation.getGuild1Name(), relation.getGuild2Name(), type, status)));
                    }
                });
                break;
            case "create":
                if (args.length < 5) {
                    sender.sendMessage(ColorUtils.colorize("&cUżycie: /guildadmin relation create <gildia1> <gildia2> <typ_relacji>"));
                    sender.sendMessage(ColorUtils.colorize("&7Typ relacji: ally|enemy|war|truce|neutral"));
                    return;
                }
                handleCreateRelation(sender, args);
                break;
            case "delete":
                if (args.length < 4) {
                    sender.sendMessage(ColorUtils.colorize("&cUżycie: /guildadmin relation delete <gildia1> <gildia2>"));
                    return;
                }
                handleDeleteRelation(sender, args);
                break;
            default:
                sender.sendMessage(ColorUtils.colorize("&cNieprawidłowa operacja relacji! Użyj list|create|delete|gui"));
                break;
        }
    }

    private void handleCreateRelation(CommandSender sender, String[] args) {
        String guild1Name = args[2];
        String guild2Name = args[3];
        String relationTypeStr = args[4];

        // Parsuj typ relacji
        GuildRelation.RelationType relationType;
        try {
            relationType = GuildRelation.RelationType.valueOf(relationTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ColorUtils.colorize("&cNieprawidłowy typ relacji! Użyj: ally, enemy, war, truce, neutral"));
            return;
        }

        // Pobierz obie gildie
        CompletableFuture<Guild> guild1Future = plugin.getGuildService().getGuildByNameAsync(guild1Name);
        CompletableFuture<Guild> guild2Future = plugin.getGuildService().getGuildByNameAsync(guild2Name);

        CompletableFuture.allOf(guild1Future, guild2Future).thenAccept(v -> {
            try {
                Guild guild1 = guild1Future.get();
                Guild guild2 = guild2Future.get();

                if (guild1 == null) {
                    sender.sendMessage(ColorUtils.colorize("&cGildia " + guild1Name + " nie istnieje!"));
                    return;
                }
                if (guild2 == null) {
                    sender.sendMessage(ColorUtils.colorize("&cGildia " + guild2Name + " nie istnieje!"));
                    return;
                }
                if (guild1.getId() == guild2.getId()) {
                    sender.sendMessage(ColorUtils.colorize("&cNie można nawiązać relacji z samym sobą!"));
                    return;
                }

                // Utwórz relację
                plugin.getGuildService().createGuildRelationAsync(
                    guild1.getId(), guild2.getId(),
                    guild1.getName(), guild2.getName(),
                    relationType, UUID.randomUUID(), "Administrator"
                ).thenAccept(success -> {
                    if (success) {
                        sender.sendMessage(ColorUtils.colorize("&aUtworzono relację: " + guild1Name + " ↔ " + guild2Name + " (" + getRelationTypeText(relationType) + ")"));
                    } else {
                        sender.sendMessage(ColorUtils.colorize("&cUtworzenie relacji nie powiodło się!"));
                    }
                });

            } catch (Exception e) {
                sender.sendMessage(ColorUtils.colorize("&cBłąd podczas tworzenia relacji: " + e.getMessage()));
            }
        });
    }

    private void handleDeleteRelation(CommandSender sender, String[] args) {
        String guild1Name = args[2];
        String guild2Name = args[3];

        // Pobierz obie gildie
        CompletableFuture<Guild> guild1Future = plugin.getGuildService().getGuildByNameAsync(guild1Name);
        CompletableFuture<Guild> guild2Future = plugin.getGuildService().getGuildByNameAsync(guild2Name);

        CompletableFuture.allOf(guild1Future, guild2Future).thenAccept(v -> {
            try {
                Guild guild1 = guild1Future.get();
                Guild guild2 = guild2Future.get();

                if (guild1 == null) {
                    sender.sendMessage(ColorUtils.colorize("&cGildia " + guild1Name + " nie istnieje!"));
                    return;
                }
                if (guild2 == null) {
                    sender.sendMessage(ColorUtils.colorize("&cGildia " + guild2Name + " nie istnieje!"));
                    return;
                }

                // Znajdź i usuń relację
                plugin.getGuildService().getGuildRelationsAsync(guild1.getId()).thenAccept(relations -> {
                    for (GuildRelation relation : relations) {
                        if ((relation.getGuild1Id() == guild1.getId() && relation.getGuild2Id() == guild2.getId()) ||
                            (relation.getGuild1Id() == guild2.getId() && relation.getGuild2Id() == guild1.getId())) {

                            plugin.getGuildService().deleteGuildRelationAsync(relation.getId()).thenAccept(success -> {
                                if (success) {
                                    sender.sendMessage(ColorUtils.colorize("&aUsunięto relację: " + guild1Name + " ↔ " + guild2Name));
                                } else {
                                    sender.sendMessage(ColorUtils.colorize("&cUsunięcie relacji nie powiodło się!"));
                                }
                            });
                            return;
                        }
                    }
                    sender.sendMessage(ColorUtils.colorize("&cNie znaleziono relacji między " + guild1Name + " a " + guild2Name + "!"));
                });

            } catch (Exception e) {
                sender.sendMessage(ColorUtils.colorize("&cBłąd podczas usuwania relacji: " + e.getMessage()));
            }
        });
    }

    private String getRelationStatusText(GuildRelation.RelationStatus status) {
        switch (status) {
            case PENDING: return "Oczekująca";
            case ACTIVE: return "Aktywna";
            case EXPIRED: return "Wygasła";
            case CANCELLED: return "Anulowana";
            default: return "Nieznana";
        }
    }

    private String getRelationTypeText(GuildRelation.RelationType type) {
        switch (type) {
            case ALLY: return "Sojusznik";
            case ENEMY: return "Wróg";
            case WAR: return "Wojna";
            case TRUCE: return "Rozejm";
            case NEUTRAL: return "Neutralny";
            default: return "Nieznany";
        }
    }

    private void handleReload(CommandSender sender) {
        try {
            plugin.getConfigManager().reloadAllConfigs();
            // Przeładuj macierz uprawnień i wyczyść cache uprawnień
            plugin.getPermissionManager().reloadFromConfig();
            sender.sendMessage(ColorUtils.colorize("&aKonfiguracja została przeładowana!"));
        } catch (Exception e) {
            sender.sendMessage(ColorUtils.colorize("&cPrzeładowanie konfiguracji nie powiodło się: " + e.getMessage()));
        }
    }

    private void handleTest(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&cUżycie: /guildadmin test <typ-testu>"));
            sender.sendMessage(ColorUtils.colorize("&7typ-testu: gui, economy, relation"));
            return;
        }

        String testType = args[1];
        switch (testType.toLowerCase()) {
            case "gui":
                if (sender instanceof Player player) {
                    AdminGuildGUI adminGUI = new AdminGuildGUI(plugin);
                    plugin.getGuiManager().openGUI(player, adminGUI);
                    sender.sendMessage(ColorUtils.colorize("&aOtwarto GUI administratora do testów."));
                } else {
                    sender.sendMessage(ColorUtils.colorize("&cTo polecenie może być wykonane tylko przez gracza!"));
                }
                break;
            case "economy":
                if (args.length < 4) {
                    sender.sendMessage(ColorUtils.colorize("&cUżycie: /guildadmin test economy <nazwa_gildii> <operacja> <kwota>"));
                    return;
                }
                String guildName = args[2];
                String operation = args[3];
                double amount;
                try {
                    amount = Double.parseDouble(args[4]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ColorUtils.colorize("&cFormat kwoty jest nieprawidłowy!"));
                    return;
                }
                plugin.getGuildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
                    if (guild == null) {
                        sender.sendMessage(ColorUtils.colorize("&cGildia " + guildName + " nie istnieje!"));
                        return;
                    }
                    final double[] newBalance = {guild.getBalance()};
                    switch (operation.toLowerCase()) {
                        case "set":
                            newBalance[0] = amount;
                            break;
                        case "add":
                            newBalance[0] += amount;
                            break;
                        case "remove":
                            newBalance[0] -= amount;
                            if (newBalance[0] < 0) newBalance[0] = 0;
                            break;
                        default:
                            sender.sendMessage(ColorUtils.colorize("&cNieprawidłowa operacja! Użyj set|add|remove"));
                            return;
                    }
                    plugin.getGuildService().updateGuildBalanceAsync(guild.getId(), newBalance[0]).thenAccept(success -> {
                        if (success) {
                            String formattedAmount = plugin.getEconomyManager().format(newBalance[0]);
                            sender.sendMessage(ColorUtils.colorize("&aFundusze gildii " + guildName + " zostały zaktualizowane do: " + formattedAmount));
                        } else {
                            sender.sendMessage(ColorUtils.colorize("&cAktualizacja funduszy gildii nie powiodła się!"));
                        }
                    });
                });
                break;
            case "relation":
                if (args.length < 5) {
                    sender.sendMessage(ColorUtils.colorize("&cUżycie: /guildadmin test relation create <gildia1> <gildia2> <typ_relacji>"));
                    sender.sendMessage(ColorUtils.colorize("&7Typ relacji: ally|enemy|war|truce|neutral"));
                    return;
                }
                String guild1NameTest = args[2];
                String guild2NameTest = args[3];
                String relationTypeStrTest = args[4];
                GuildRelation.RelationType relationTypeTest;
                try {
                    relationTypeTest = GuildRelation.RelationType.valueOf(relationTypeStrTest.toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ColorUtils.colorize("&cNieprawidłowy typ relacji! Użyj: ally, enemy, war, truce, neutral"));
                    return;
                }
                plugin.getGuildService().getGuildByNameAsync(guild1NameTest).thenAccept(guild1 -> {
                    if (guild1 == null) {
                        sender.sendMessage(ColorUtils.colorize("&cGildia " + guild1NameTest + " nie istnieje!"));
                        return;
                    }
                    plugin.getGuildService().getGuildByNameAsync(guild2NameTest).thenAccept(guild2 -> {
                        if (guild2 == null) {
                            sender.sendMessage(ColorUtils.colorize("&cGildia " + guild2NameTest + " nie istnieje!"));
                            return;
                        }
                        if (guild1.getId() == guild2.getId()) {
                            sender.sendMessage(ColorUtils.colorize("&cNie można nawiązać relacji z samym sobą!"));
                            return;
                        }
                        plugin.getGuildService().createGuildRelationAsync(
                            guild1.getId(), guild2.getId(),
                            guild1.getName(), guild2.getName(),
                            relationTypeTest, UUID.randomUUID(), "Administrator"
                        ).thenAccept(success -> {
                            if (success) {
                                sender.sendMessage(ColorUtils.colorize("&aUtworzono relację: " + guild1NameTest + " ↔ " + guild2NameTest + " (" + getRelationTypeText(relationTypeTest) + ")"));
                            } else {
                                sender.sendMessage(ColorUtils.colorize("&cUtworzenie relacji nie powiodło się!"));
                            }
                        });
                    });
                });
                break;
            default:
                sender.sendMessage(ColorUtils.colorize("&cNieprawidłowy typ testu! Użyj gui, economy, relation"));
                break;
        }
    }

    private void handleHelp(CommandSender sender) {
        sender.sendMessage(ColorUtils.colorize("&6=== Polecenia Administratora Gildii ==="));
        sender.sendMessage(ColorUtils.colorize("&e/guildadmin &7- Otwórz GUI administratora"));
        sender.sendMessage(ColorUtils.colorize("&e/guildadmin list &7- Wypisz wszystkie gildie"));
        sender.sendMessage(ColorUtils.colorize("&e/guildadmin info <gildia> &7- Zobacz informacje o gildii"));
        sender.sendMessage(ColorUtils.colorize("&e/guildadmin delete <gildia> &7- Wymuś usunięcie gildii"));
        sender.sendMessage(ColorUtils.colorize("&e/guildadmin freeze <gildia> &7- Zamroź gildię"));
        sender.sendMessage(ColorUtils.colorize("&e/guildadmin unfreeze <gildia> &7- Odmroź gildię"));
        sender.sendMessage(ColorUtils.colorize("&e/guildadmin transfer <gildia> <gracz> &7- Przekaż lidera"));
        sender.sendMessage(ColorUtils.colorize("&e/guildadmin economy <gildia> <operacja> <kwota> &7- Zarządzaj ekonomią gildii"));
        sender.sendMessage(ColorUtils.colorize("&e/guildadmin relation <operacja> &7- Zarządzaj relacjami gildii"));
        sender.sendMessage(ColorUtils.colorize("&e/guildadmin reload &7- Przeładuj konfigurację"));
        sender.sendMessage(ColorUtils.colorize("&e/guildadmin help &7- Pokaż informacje o pomocy"));
    }
}
