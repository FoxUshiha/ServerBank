package com.foxsrv.serverbank;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.logging.Level;

public class ServerBankPlugin extends JavaPlugin {

    private static Economy economy;
    private static ServerBankPlugin instance;

    private UUID serverBankUUID;

    @Override
    public void onEnable() {
        instance = this;

        // Vault obrigatório
        if (!setupEconomy()) {
            getLogger().severe("Vault ou Economy provider não encontrado. Desabilitando plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Config padrão
        saveDefaultConfig();
        reloadLocalConfig();

        // Registrar comando / tab-complete
        BankCommand bankCommand = new BankCommand(this);
        if (getCommand("bank") != null) {
            getCommand("bank").setExecutor(bankCommand);
            getCommand("bank").setTabCompleter(bankCommand);
        }

        getLogger().info("ServerBank habilitado. Conta do servidor em UUID: " + serverBankUUID);
    }

    @Override
    public void onDisable() {
        getLogger().info("ServerBank desabilitado.");
    }

    public void reloadLocalConfig() {
        reloadConfig();
        String raw = getConfig().getString("ServerUUID", "").trim();
        try {
            serverBankUUID = UUID.fromString(raw);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "ServerUUID inválido no config.yml: " + raw);
            // Mantém nulo para falhar rápido em uso
            serverBankUUID = null;
        }
    }

    public UUID getServerBankUUID() {
        return serverBankUUID;
    }

    public OfflinePlayer getServerBankPlayer() {
        if (serverBankUUID == null) return null;
        return Bukkit.getOfflinePlayer(serverBankUUID);
    }

    public static Economy eco() {
        return economy;
    }

    public static ServerBankPlugin getInstance() {
        return instance;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }
}
