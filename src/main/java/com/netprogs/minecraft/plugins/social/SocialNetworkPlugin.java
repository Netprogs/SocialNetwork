package com.netprogs.minecraft.plugins.social;

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

import java.io.File;
import java.util.logging.Logger;

import com.netprogs.minecraft.plugins.social.command.SocialNetworkDispatcher;
import com.netprogs.minecraft.plugins.social.config.PluginConfig;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.SettingsConfig;
import com.netprogs.minecraft.plugins.social.integration.LWCIntegration;
import com.netprogs.minecraft.plugins.social.integration.VaultIntegration;
import com.netprogs.minecraft.plugins.social.integration.WorldGuardIntegration;
import com.netprogs.minecraft.plugins.social.listener.PlayerJoinListener;
import com.netprogs.minecraft.plugins.social.listener.perk.LWCListener;
import com.netprogs.minecraft.plugins.social.listener.perk.PlayerDamageListener;
import com.netprogs.minecraft.plugins.social.listener.perk.WorldGuardListener;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

public class SocialNetworkPlugin extends JavaPlugin {

    private final Logger logger = Logger.getLogger("Minecraft");

    // expose the instance of this class as a global so we can better access it's methods
    public static SocialNetworkPlugin instance;

    private String pluginName;
    private File pluginFolder;

    public SocialNetworkPlugin() {
        instance = this;
    }

    public void onEnable() {

        // report that this plug in is being loaded
        PluginDescriptionFile pdfFile = getDescription();

        pluginName = getDescription().getName();
        pluginFolder = getDataFolder();

        // load the rank data from the XML file
        loadConfigurations();

        // check to make sure Vault is installed
        VaultIntegration.getInstance().initialize(this);
        if (!VaultIntegration.getInstance().isEnabled()) {
            logger.info("[" + pdfFile.getName() + "] v" + pdfFile.getVersion() + " has been disabled.");
            return;
        }

        // if WorldGuard is available, attach the listener for it
        if (isPluginAvailable("WorldGuard")) {

            WorldGuardIntegration.getInstance().initialize(this);
            if (WorldGuardIntegration.getInstance().isEnabled()) {
                getServer().getPluginManager().registerEvents(new WorldGuardListener(), this);
            }

        } else {

            logger.info("[" + pdfFile.getName() + "] " + "Could not find WorldGuard; features are disabled.");
        }

        // if LWC is available, attach the listener for it
        if (isPluginAvailable("LWC")) {

            LWCIntegration.getInstance().initialize(this);
            if (LWCIntegration.getInstance().isEnabled()) {
                getServer().getPluginManager().registerEvents(new LWCListener(), this);
            }

        } else {

            logger.info("[" + pdfFile.getName() + "] " + "Could not find LWC; features are disabled.");
        }

        // attach to the "social" command
        getCommand("social").setExecutor(new SocialNetworkDispatcher());

        // attach the events to our listeners
        getServer().getPluginManager().registerEvents(new PlayerDamageListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);

        // Okay, we're done
        logger.info("[" + pdfFile.getName() + "] v" + pdfFile.getVersion() + " has been enabled.");
    }

    public void loadConfigurations() {

        PluginConfig.getInstance().reset();
        PluginConfig.getInstance().register(new SettingsConfig(getDataFolder() + "/config.json"));
        PluginConfig.getInstance().register(new ResourcesConfig(getDataFolder() + "/resources.json"));
    }

    public void onDisable() {

        PluginDescriptionFile pdfFile = getDescription();
        this.logger.info("[" + pdfFile.getName() + "] has been disabled.");
    }

    public String getPluginName() {
        return pluginName;
    }

    public File getPluginFolder() {
        return pluginFolder;
    }

    private boolean isPluginAvailable(String name) {

        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin(name);
        if (plugin == null) {
            return false;
        }
        return true;
    }
}
