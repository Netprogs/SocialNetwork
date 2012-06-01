package com.netprogs.minecraft.plugins.social.integration;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.netprogs.minecraft.plugins.social.command.ISocialNetworkCommand.ICommandType;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.config.PluginConfig;
import com.netprogs.minecraft.plugins.social.config.settings.SettingsConfig;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;

/*
 * Copyright (C) 2012 Scott Milne
 * 
 * "Social Network" is a Craftbukkit Minecraft server modification plug-in. It attempts to add a 
 * social environment to your server by allowing players to be placed into different types of social groups.
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

    private final Logger logger = Logger.getLogger("Minecraft");

    private boolean isPluginLoaded = false;

    private PluginDescriptionFile pdfFile;
    private Economy economy = null;
    private Permission permission = null;

    private static final VaultIntegration SINGLETON = new VaultIntegration();

    public static VaultIntegration getInstance() {
        return SINGLETON;
    }

    @Override
    public void initialize(Plugin plugin) {

        isPluginLoaded = false;

        // get the plug-in description file
        pdfFile = plugin.getDescription();

        // first we need to check to see if Vault is actually installed
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            logger.log(Level.SEVERE, getPluginName() + "Vault is not installed.");
            return;
        }

        // try to obtain the economy class from Vault
        RegisteredServiceProvider<Economy> economyProvider =
                Bukkit.getServer().getServicesManager().getRegistration(Economy.class);

        if (economyProvider != null) {

            economy = (Economy) economyProvider.getProvider();
            logger.info(getPluginName() + "Vault:Economy integration successful.");

        } else {

            logger.log(Level.SEVERE, getPluginName() + "Could not obtain an Economy integration from Vault.");
            return;
        }

        // try to obtain the permission class from Vault
        RegisteredServiceProvider<Permission> permissionProvider =
                Bukkit.getServer().getServicesManager().getRegistration(Permission.class);

        if (permissionProvider != null) {

            permission = (Permission) permissionProvider.getProvider();
            logger.info(getPluginName() + "Vault:Permission integration successful.");

        } else {

            logger.log(Level.SEVERE, getPluginName() + "Could not obtain a Permission integration from Vault.");
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

    private String getPluginName() {
        return "[" + pdfFile.getName() + "] ";
    }

    /**
     * Since this is a required integration, we can safely expose the Economy instance within it.
     * @return
     */
    public Economy getEconomy() {
        return economy;
    }

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

    public boolean hasCommandPermission(CommandSender sender, ICommandType commandType, String subCommandPath) {
        return hasCommandPermission(sender, commandType + subCommandPath);
    }

    public boolean hasCommandPermission(CommandSender sender, String permissionPath) {

        String path = "social." + permissionPath;

        boolean hasPermission = permission.has(sender, path);
        if (hasPermission) {

            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                logger.info(sender.getName() + " has the permission: " + path);
            }

        } else {

            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                logger.info(sender.getName() + " does not have the permission: " + path);
            }
        }

        return hasPermission;
    }

    public boolean preAuthCommandPurchase(Player player, double price) {

        if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
            logger.info("[PreAuth] Found command price: " + price);
        }

        // now check to see if they have enough money to run the perk
        if (!economy.has(player.getName(), price)) {

            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                logger.info("[PreAuth] Not enough funds for command: " + price);
            }

            // they don't, so tell them
            MessageUtil.sendNotEnoughFundsMessage(player, price);
            return false;
        }

        return true;
    }

    public boolean processCommandPurchase(Player player, double price) {

        // now check to see if they have enough money to run the perk
        if (economy.has(player.getName(), price)) {

            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                logger.info("[purchase] Charging command price: " + price);
            }

            // do the actual purchase now
            economy.withdrawPlayer(player.getName(), price);

        } else {

            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                logger.info("[purchase] Not enough funds for command: " + price);
            }

            // Despite us checking earlier, they seem to have run out of money. Tell them.
            MessageUtil.sendNotEnoughFundsMessage(player, price);
            return false;
        }

        return true;
    }
}
