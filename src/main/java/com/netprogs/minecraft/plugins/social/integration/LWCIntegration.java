package com.netprogs.minecraft.plugins.social.integration;

import java.util.List;

import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Permission;
import com.griefcraft.model.Protection;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

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

public class LWCIntegration extends PluginIntegration {

    private LWC lwc;
    private boolean isPluginLoaded = false;

    public LWCIntegration(Plugin plugin, boolean isLoggingDebug) {
        super(plugin, isLoggingDebug);
    }

    @Override
    public void initialize() {

        isPluginLoaded = false;

        // try to find WorldGuard and verify that the plug-in found under that name actually is one
        Plugin loadedPlugin = Bukkit.getServer().getPluginManager().getPlugin("LWC");
        if ((loadedPlugin == null) || (!(loadedPlugin instanceof LWCPlugin))) {

            // not found, don't allow features using it to be enabled
            lwc = null;
            getPlugin().getLogger().info("Could not find LWC; features are disabled.");
            return;

        } else {

            // we found it, so now we can use it
            lwc = ((LWCPlugin) loadedPlugin).getLWC();
            getPlugin().getLogger().info("Found LWC; features can be enabled.");
        }

        isPluginLoaded = true;
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

    public void addPermission(String playerName, String memberName) {

        // go through each protection and add the member to it
        List<Protection> protections = lwc.getPhysicalDatabase().loadProtectionsByPlayer(playerName);
        for (Protection dbProtection : protections) {

            // pull the item from the LWC cache
            Protection protection = lwc.getProtectionCache().getProtectionById(dbProtection.getId());
            if (protection != null) {

                // create the permission
                Permission.Type type = Permission.Type.PLAYER;
                Permission permission = new Permission(memberName, type);
                permission.setAccess(Permission.Access.PLAYER);

                // add it to the protection and queue it to be saved
                protection.addPermission(permission);
                protection.save();
            }
        }
    }

    public void removePermission(String playerName, String memberName) {

        // go through each protection and remove the member from it
        List<Protection> protections = lwc.getPhysicalDatabase().loadProtectionsByPlayer(playerName);
        if (protections != null) {
            for (Protection dbProtection : protections) {

                // pull the item from the LWC cache
                Protection protection = lwc.getProtectionCache().getProtectionById(dbProtection.getId());
                if (protection != null) {

                    // remove it from the protection and queue it to be saved
                    Permission.Type type = Permission.Type.PLAYER;
                    protection.removePermissions(memberName, type);
                    protection.save();
                }
            }
        }
    }
}
