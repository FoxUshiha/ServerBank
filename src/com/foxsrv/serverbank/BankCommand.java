package com.foxsrv.serverbank;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class BankCommand implements TabExecutor {

    private final ServerBankPlugin plugin;

    public BankCommand(ServerBankPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasAdmin(CommandSender sender) {
        // Console sempre pode. Jogadores precisam da permissão ou ser OP (default: op).
        if (sender instanceof ConsoleCommandSender) return true;
        return sender.hasPermission("serverbank.admin");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!hasAdmin(sender)) {
            sender.sendMessage(color("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "pay":
                return handlePay(sender, args, label);
            case "take":
                return handleTake(sender, args, label);
            case "collect":
                return handleCollect(sender, args, label);
            case "reload":
                // QoL: /bank reload recarrega config
                plugin.reloadLocalConfig();
                sender.sendMessage(color("&aConfiguration reloaded."));
                return true;
            default:
                sendUsage(sender, label);
                return true;
        }
    }

    private boolean handlePay(CommandSender sender, String[] args, String label) {
        if (args.length < 3) {
            sender.sendMessage(color("&eUsage: &f/" + label + " pay <player> <amount>"));
            return true;
        }

        String targetName = args[1];
        String rawAmount = args[2];

        BigDecimal amount = Util.parseAmount(rawAmount);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            sender.sendMessage(color("&cInvalid amount."));
            return true;
        }
        amount = Util.truncate4(amount);

        OfflinePlayer target = findOfflinePlayerByName(targetName);
        if (target == null) {
            sender.sendMessage(color("&cPlayer not found: &f" + targetName));
            return true;
        }

        OfflinePlayer serverBank = plugin.getServerBankPlayer();
        if (serverBank == null) {
            sender.sendMessage(color("&cServerUUID in config.yml is invalid."));
            return true;
        }

        double serverBalance = ServerBankPlugin.eco().getBalance(serverBank);
        if (BigDecimal.valueOf(serverBalance).compareTo(amount) < 0) {
            sender.sendMessage(color("&cInsufficient funds."));
            return true;
        }

        EconomyResponse w = ServerBankPlugin.eco().withdrawPlayer(serverBank, amount.doubleValue());
        if (!w.transactionSuccess()) {
            sender.sendMessage(color("&cFailed to withdraw from server account: &7" + w.errorMessage));
            return true;
        }

        EconomyResponse d = ServerBankPlugin.eco().depositPlayer(target, amount.doubleValue());
        if (!d.transactionSuccess()) {
            // Reverse para não “sumir” dinheiro
            ServerBankPlugin.eco().depositPlayer(serverBank, amount.doubleValue());
            sender.sendMessage(color("&cFailed to deposit to player: &7" + d.errorMessage));
            return true;
        }

        sender.sendMessage(color("&aPaid &f" + amount + " &ato &f" + getBestName(target) + "&a from server bank."));
        if (target.isOnline()) {
            Player p = target.getPlayer();
            if (p != null) {
                p.sendMessage(color("&aYou received &f" + amount + " &afrom the server bank."));
            }
        }
        return true;
    }

    private boolean handleTake(CommandSender sender, String[] args, String label) {
        if (args.length < 3) {
            sender.sendMessage(color("&eUsage: &f/" + label + " take <player> <amount>"));
            return true;
        }

        String targetName = args[1];
        String rawAmount = args[2];

        BigDecimal amount = Util.parseAmount(rawAmount);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            sender.sendMessage(color("&cInvalid amount."));
            return true;
        }
        amount = Util.truncate4(amount);

        OfflinePlayer target = findOfflinePlayerByName(targetName);
        if (target == null) {
            sender.sendMessage(color("&cPlayer not found: &f" + targetName));
            return true;
        }

        OfflinePlayer serverBank = plugin.getServerBankPlayer();
        if (serverBank == null) {
            sender.sendMessage(color("&cServerUUID in config.yml is invalid."));
            return true;
        }

        double playerBalance = ServerBankPlugin.eco().getBalance(target);
        if (BigDecimal.valueOf(playerBalance).compareTo(amount) < 0) {
            sender.sendMessage(color("&cThis user does not have enough money."));
            return true;
        }

        EconomyResponse w = ServerBankPlugin.eco().withdrawPlayer(target, amount.doubleValue());
        if (!w.transactionSuccess()) {
            sender.sendMessage(color("&cFailed to withdraw from player: &7" + w.errorMessage));
            return true;
        }

        EconomyResponse d = ServerBankPlugin.eco().depositPlayer(serverBank, amount.doubleValue());
        if (!d.transactionSuccess()) {
            // Tenta reverter caso depósito no servidor falhe (evita sumir dinheiro)
            ServerBankPlugin.eco().depositPlayer(target, amount.doubleValue());
            sender.sendMessage(color("&cFailed to deposit to server account: &7" + d.errorMessage));
            return true;
        }

        sender.sendMessage(color("&aTook &f" + amount + " &afrom &f" + getBestName(target) + "&a to server bank."));
        if (target.isOnline()) {
            Player p = target.getPlayer();
            if (p != null) {
                p.sendMessage(color("&c&l" + amount + " &chas been taken from your balance by the server bank."));
            }
        }
        return true;
    }

    private boolean handleCollect(CommandSender sender, String[] args, String label) {
        if (args.length < 2) {
            sender.sendMessage(color("&eUsage: &f/" + label + " collect <percent>"));
            sender.sendMessage(color("&7Examples: &f/" + label + " collect 100 &7(100% - zero everyone)"));
            sender.sendMessage(color("&7           &f/" + label + " collect 0.1 &7(0.1% from everyone)"));
            return true;
        }

        String rawPercent = args[1];
        Double percent = Util.parsePercent(rawPercent);
        if (percent == null || percent < 0) {
            sender.sendMessage(color("&cInvalid percent."));
            return true;
        }

        OfflinePlayer serverBank = plugin.getServerBankPlayer();
        if (serverBank == null) {
            sender.sendMessage(color("&cServerUUID in config.yml is invalid."));
            return true;
        }

        // percent é em % (ex.: 100 = 100%), logo fator = percent / 100
        BigDecimal factor = BigDecimal.valueOf(percent).divide(BigDecimal.valueOf(100), 12, RoundingMode.DOWN);

        // Lista de jogadores conhecidos (offline + online) sem duplicar
        Map<UUID, OfflinePlayer> allPlayers = new LinkedHashMap<>();
        for (Player p : Bukkit.getOnlinePlayers()) allPlayers.put(p.getUniqueId(), p);
        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) allPlayers.put(op.getUniqueId(), op);

        BigDecimal totalCollected = BigDecimal.ZERO.setScale(4, RoundingMode.DOWN);
        int affected = 0;

        for (OfflinePlayer op : allPlayers.values()) {
            try {
                double balD = ServerBankPlugin.eco().getBalance(op);
                if (balD <= 0) continue;

                BigDecimal balance = BigDecimal.valueOf(balD);
                // amount = balance * factor, truncado a 4 casas
                BigDecimal amount = Util.truncate4(balance.multiply(factor));
                if (amount.compareTo(BigDecimal.ZERO) <= 0) continue;

                // Segurança: nunca tentar sacar mais que o saldo (por arredondamentos)
                if (amount.compareTo(balance) > 0) {
                    amount = Util.truncate4(balance);
                    if (amount.compareTo(BigDecimal.ZERO) <= 0) continue;
                }

                EconomyResponse w = ServerBankPlugin.eco().withdrawPlayer(op, amount.doubleValue());
                if (!w.transactionSuccess()) {
                    // Falhou sacar do jogador -> não deposita, não cria dinheiro
                    continue;
                }

                EconomyResponse d = ServerBankPlugin.eco().depositPlayer(serverBank, amount.doubleValue());
                if (!d.transactionSuccess()) {
                    // Reverte o saque do jogador caso depósito falhe
                    ServerBankPlugin.eco().depositPlayer(op, amount.doubleValue());
                    continue;
                }

                totalCollected = totalCollected.add(amount).setScale(4, RoundingMode.DOWN);
                affected++;
            } catch (Throwable t) {
                // Não derrubar o comando por um jogador com problema
                plugin.getLogger().warning("Collect skipped for player UUID=" + op.getUniqueId() + ": " + t.getMessage());
            }
        }

        sender.sendMessage(color("&aCollected &f" + totalCollected + " &afrom &f" + affected + " &aplayers to the server bank."));
        return true;
    }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(color("&eUsage:"));
        sender.sendMessage(color("&f/" + label + " pay <player> <amount> &7- pay from server bank to a player"));
        sender.sendMessage(color("&f/" + label + " take <player> <amount> &7- take from a player to server bank"));
        sender.sendMessage(color("&f/" + label + " collect <percent> &7- collect percent from all players"));
        sender.sendMessage(color("&f/" + label + " reload &7- reload configuration"));
    }

    // ===== Busca por jogador (offline + online) pelo nome (case-insensitive) =====

    private OfflinePlayer findOfflinePlayerByName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);

        // Online primeiro (match exato priorizado)
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(name)) return p;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase(Locale.ROOT).startsWith(lower)) return p;
        }

        // Offline conhecidos pelo servidor
        OfflinePlayer bestPrefix = null;
        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            String n = op.getName();
            if (n == null) continue;
            if (n.equalsIgnoreCase(name)) return op;
            if (bestPrefix == null && n.toLowerCase(Locale.ROOT).startsWith(lower)) {
                bestPrefix = op;
            }
        }
        return (bestPrefix != null) ? bestPrefix : null;
    }

    private String getBestName(OfflinePlayer op) {
        return (op.getName() != null) ? op.getName() : op.getUniqueId().toString();
    }

    // ====== TAB COMPLETE ======

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!hasAdmin(sender)) return Collections.emptyList();

        if (args.length == 1) {
            return Arrays.asList("pay", "take", "collect", "reload").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if ((sub.equals("pay") || sub.equals("take"))) {
            if (args.length == 2) {
                String partial = args[1].toLowerCase(Locale.ROOT);
                // nomes online + offline (sem duplicar / nem nulos)
                LinkedHashSet<String> names = new LinkedHashSet<>();
                for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
                for (OfflinePlayer op : Bukkit.getOfflinePlayers()) if (op.getName() != null) names.add(op.getName());
                return names.stream()
                        .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(partial))
                        .limit(50)
                        .collect(Collectors.toList());
            } else if (args.length == 3) {
                return Collections.singletonList("<amount>");
            }
        } else if (sub.equals("collect")) {
            if (args.length == 2) {
                return Arrays.asList("100", "50", "10", "1", "0.5", "0.1");
            }
        }

        return Collections.emptyList();
    }
}
