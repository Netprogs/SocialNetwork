package com.netprogs.minecraft.plugins.social.integration;

import java.util.logging.Level;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.command.ISocialNetworkCommand.ICommandType;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

/*
 * Copyright (C) 2012 Scott Milne
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

public class VaultIntegration extends PluginIntegration {

    private String basePermissionPath;
    private boolean isPluginLoaded = false;

    private Economy economy = null;
    private Permission permission = null;

    public VaultIntegration(Plugin plugin, String basePermissionPath, boolean isLoggingDebug) {
        super(plugin, isLoggingDebug);
        this.basePermissionPath = basePermissionPath;
    }

    @Override
    public void initialize() {

        isPluginLoaded = false;

        // first we need to check to see if Vault is actually installed
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            getPlugin().getLogger().log(Level.SEVERE, "Vault is not installed.");
            return;
        }

        // try to obtain the economy class from Vault
        RegisteredServiceProvider<Economy> economyProvider =
                Bukkit.getServer().getServicesManager().getRegistration(Economy.class);

        if (economyProvider != null) {

            economy = (Economy) economyProvider.getProvider();

            if (isLoggingDebug()) {
                getPlugin().getLogger().info("Vault:Economy integration successful.");
            }

        } else {

            getPlugin().getLogger().log(Level.SEVERE, "Could not obtain an Economy integration from Vault.");
            return;
        }

        // try to obtain the permission class from Vault
        RegisteredServiceProvider<Permission> permissionProvider =
                Bukkit.getServer().getServicesManager().getRegistration(Permission.class);

        if (permissionProvider != null) {

            permission = (Permission) permissionProvider.getProvider();

            if (isLoggingDebug()) {
                getPlugin().getLogger().info("Vault:Permission integration successful.");
            }

        } else {

            getPlugin().getLogger().log(Level.SEVERE, "Could not obtain a Permission integration from Vault.");
            return;
        }
        // set the isPluginLoaded flag
        isPluginLoaded = true;

        return;
    }

    @Override
    protected boolean isPluginLoaded() {
        return isPluginLoaded;
    }

    @Override
    protected boolean isPluginEnabled() {
        // we have to have this, so don't allow config to turn it off
        return true;
    }

    public String getBasePermissionPath() {
        return basePermissionPath;
    }

    /**
     * Since this is a required integration, we can safely expose the Economy instance within it.
     * @return
     */
    public Economy getEconomy() {
        return economy;
    }

    // TODO: Remove calling of Vault for permission. Just use player.hasPermission instead and place into main class.

    /**
     * Since this is a required integration, we can safely expose the Permission instance within it.
     * @return
     */
    public Permission getPermission() {
        return permission;
    }

    public boolean hasCommandPermission(CommandSender sender, ICommandType commandType) {
        return hasCommandPermission(sender, commandType.toString());
    }

    public boolean hasCommandPermission(CommandSender sender, String permissionPath) {

        String path = getBasePermissionPath() + "." + permissionPath;

        boolean hasPermission = permission.has(sender, path);
        if (hasPermission) {

            if (isLoggingDebug()) {
                getPlugin().getLogger().info(sender.getName() + " has the permission: " + path);
            }

        } else {

            if (isLoggingDebug()) {
                getPlugin().getLogger().info(sender.getName() + " does not have the permission: " + path);
            }
        }

        return hasPermission;
    }

    public boolean preAuthCommandPurchase(Player player, double price) {

        SocialNetworkPlugin.log("[PreAuthCommandPurchase] Found command price: " + price);

        // now check to see if they have enough money to run the perk
        if (!preAuth(player, price)) {

            // they don't, so tell them
            MessageUtil.sendNotEnoughFundsMessage(player, price);
            return false;
        }

        return true;
    }

    public boolean processCommandPurchase(Player player, double price) {

        // now check to see if they have enough money to run the perk
        if (preAuth(player, price)) {

            if (isLoggingDebug()) {
                getPlugin().getLogger().info("[processCommandPurchase] Charging command price: " + price);
            }

            // do the actual purchase now
            withdraw(player, price);

        } else {

            if (isLoggingDebug()) {
                getPlugin().getLogger().info("[processCommandPurchase] Not enough funds for command: " + price);
            }

            // Despite us checking earlier, they seem to have run out of money. Tell them.
            MessageUtil.sendNotEnoughFundsMessage(player, price);
            return false;
        }

        return true;
    }

    public boolean preAuth(Player player, double price) {

        if (isLoggingDebug()) {
            getPlugin().getLogger().info("[PreAuth] price: " + price);
        }

        // now check to see if they have enough money
        if (!economy.has(player.getName(), price)) {

            if (isLoggingDebug()) {
                getPlugin().getLogger().info("[PreAuth] Not enough funds: " + price);
            }

            return false;
        }

        return true;
    }

    public boolean withdraw(Player player, double payment) {

        // now check to see if they have enough money
        if (economy.has(player.getName(), payment)) {

            if (isLoggingDebug()) {
                getPlugin().getLogger().info("[withdraw] Charging: " + payment);
            }

            // do the actual withdraw now
            EconomyResponse response = economy.withdrawPlayer(player.getName(), payment);
            if (response.transactionSuccess()) {

                if (isLoggingDebug()) {
                    getPlugin().getLogger().info("[withdraw]: " + payment);
                }

            } else {

                if (isLoggingDebug()) {
                    getPlugin().getLogger().info("[withdraw] failed: " + response.errorMessage);
                }

                // Something went wrong when trying to get their money
                return false;
            }

        } else {

            if (isLoggingDebug()) {
                getPlugin().getLogger().info("[withdraw] Not enough funds: " + payment);
            }

            // They seem to have run out of money.
            return false;
        }

        return true;
    }

    public void deposit(Player player, double payment) {

        // put the payment onto the players account
        EconomyResponse response = economy.depositPlayer(player.getName(), payment);
        if (response.transactionSuccess()) {

            if (isLoggingDebug()) {
                getPlugin().getLogger().info("[deposit]: " + payment);
            }

        } else {

            if (isLoggingDebug()) {
                getPlugin().getLogger().info("[deposit] failed: " + response.errorMessage);
            }
        }
    }
}
