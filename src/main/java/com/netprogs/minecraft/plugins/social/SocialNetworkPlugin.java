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
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.netprogs.minecraft.plugins.social.command.SocialNetworkDispatcher;
import com.netprogs.minecraft.plugins.social.command.help.HelpBook;
import com.netprogs.minecraft.plugins.social.command.util.ChatManager;
import com.netprogs.minecraft.plugins.social.command.util.TimerManager;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.SettingsConfig;
import com.netprogs.minecraft.plugins.social.integration.LWCIntegration;
import com.netprogs.minecraft.plugins.social.integration.VaultIntegration;
import com.netprogs.minecraft.plugins.social.integration.WorldGuardIntegration;
import com.netprogs.minecraft.plugins.social.listener.CommandPreprocessListener;
import com.netprogs.minecraft.plugins.social.listener.PlayerChatListener;
import com.netprogs.minecraft.plugins.social.listener.PlayerJoinListener;
import com.netprogs.minecraft.plugins.social.listener.PlayerMoveListener;
import com.netprogs.minecraft.plugins.social.listener.PlayerQuitListener;
import com.netprogs.minecraft.plugins.social.listener.perk.BonusExperienceListener;
import com.netprogs.minecraft.plugins.social.listener.perk.FoodShareListener;
import com.netprogs.minecraft.plugins.social.listener.perk.HealthRegenListener;
import com.netprogs.minecraft.plugins.social.listener.perk.LWCListener;
import com.netprogs.minecraft.plugins.social.listener.perk.PlayerDamageListener;
import com.netprogs.minecraft.plugins.social.listener.perk.WorldGuardListener;
import com.netprogs.minecraft.plugins.social.storage.SocialNetworkStorage;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

public class SocialNetworkPlugin extends JavaPlugin {

    // expose the instance of this class as a global so we can better access it's methods
    public static SocialNetworkPlugin instance;

    // used for sending completely anonymous data to http://mcstats.org for usage tracking
    private Metrics metrics;

    // used to hold all the plug-in settings
    private SettingsConfig settingsConfig;

    // used to hold all the plug-in resources
    private ResourcesConfig resourcesConfig;

    // used to manage the social network data storage
    private SocialNetworkStorage storage;

    // used to manage command/event timers
    private TimerManager timerManager;

    // used to maintain the list of players how have their chat turned off
    private ChatManager chatManager;

    // used to create the help pages for the plug-in
    private HelpBook helpBook;

    // used to access Vault
    private VaultIntegration vault;

    // used to access WorldGuard
    private WorldGuardIntegration worldGuard;

    // used to access LWC
    private LWCIntegration lwc;

    public SocialNetworkPlugin() {

        instance = this;
    }

    public void onEnable() {

        // report that this plug in is being loaded
        PluginDescriptionFile pdfFile = getDescription();

        // create the settings configuration object
        settingsConfig = new SettingsConfig(getDataFolder() + "/config.json");
        settingsConfig.loadConfig();

        // create the resources configuration object
        resourcesConfig = new ResourcesConfig(getDataFolder() + "/resources.json");
        resourcesConfig.loadConfig();

        // create the help book instance
        helpBook = new HelpBook();

        // create the vault integration object
        vault = new VaultIntegration(this, "social", settingsConfig.isLoggingDebug());
        vault.initialize();

        // check to make sure Vault is enabled
        if (!vault.isEnabled()) {
            getLogger().info("Disabled v" + pdfFile.getVersion());
            return;
        }

        // if WorldGuard is available, attach the listener for it
        if (isPluginAvailable("WorldGuard")) {

            worldGuard = new WorldGuardIntegration(this, settingsConfig.isLoggingDebug());
            worldGuard.initialize();

            if (worldGuard.isEnabled()) {
                getServer().getPluginManager().registerEvents(new WorldGuardListener(), this);
            }

        } else {

            getLogger().info("Could not find WorldGuard; features are disabled.");
        }

        // if LWC is available, attach the listener for it
        if (isPluginAvailable("LWC")) {

            lwc = new LWCIntegration(this, settingsConfig.isLoggingDebug());
            lwc.initialize();

            if (lwc.isEnabled()) {
                getServer().getPluginManager().registerEvents(new LWCListener(), this);
            }

        } else {

            getLogger().info("Could not find LWC; features are disabled.");
        }

        // register the command preprocessor for allowing custom commands
        getServer().getPluginManager().registerEvents(new CommandPreprocessListener(), this);

        // attach to the "social" command
        getCommand("social").setExecutor(new SocialNetworkDispatcher(this));

        // attach the events to our listeners
        getServer().getPluginManager().registerEvents(new PlayerDamageListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerChatListener(), this);
        getServer().getPluginManager().registerEvents(new BonusExperienceListener(), this);
        getServer().getPluginManager().registerEvents(new HealthRegenListener(), this);
        getServer().getPluginManager().registerEvents(new FoodShareListener(), this);

        // create the timer manager instance
        timerManager = new TimerManager(this, settingsConfig.isLoggingDebug());

        // create the chat manager instance
        chatManager = new ChatManager();

        // create the storage manager instance
        storage = new SocialNetworkStorage();

        // start up the metrics engine
        try {
            metrics = new Metrics(this);
            metrics.start();
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Error while enabling Metrics.");
        }

        // Okay, we're done
        getLogger().info("Enabled v" + pdfFile.getVersion());
    }

    public void onDisable() {

        PluginDescriptionFile pdfFile = getDescription();
        getLogger().info("Disabled v" + pdfFile.getVersion());

        // clear out all the static references to avoid leaks
        instance = null;
    }

    public static File getFolder() {
        return instance.getDataFolder();
    }

    public static String getPluginName() {

        PluginDescriptionFile pdfFile = instance.getDescription();
        return pdfFile.getName();
    }

    public static Logger logger() {
        return instance.getLogger();
    }

    public static void log(String logMessage) {

        if (instance.settingsConfig.isLoggingDebug()) {
            instance.getLogger().info(logMessage);
        }
    }

    public static VaultIntegration getVault() {
        return instance.vault;
    }

    public static WorldGuardIntegration getWorldGuard() {
        return instance.worldGuard;
    }

    public static LWCIntegration getLwc() {
        return instance.lwc;
    }

    public static SettingsConfig getSettings() {
        return instance.settingsConfig;
    }

    public static ResourcesConfig getResources() {
        return instance.resourcesConfig;
    }

    public static SocialNetworkStorage getStorage() {
        return instance.storage;
    }

    public static TimerManager getTimerManager() {
        return instance.timerManager;
    }

    public static ChatManager getChatManager() {
        return instance.chatManager;
    }

    public static HelpBook getHelpBook() {
        return instance.helpBook;
    }

    private boolean isPluginAvailable(String name) {

        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin(name);
        if (plugin == null) {
            return false;
        }
        return true;
    }
}
