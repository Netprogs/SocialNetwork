package com.netprogs.minecraft.plugins.social.integration;

import java.util.Map;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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

public class WorldGuardIntegration extends PluginIntegration {

    private boolean isPluginLoaded = false;

    private WorldGuardPlugin worldGuard;

    public WorldGuardIntegration(Plugin plugin, boolean isLoggingDebug) {
        super(plugin, isLoggingDebug);
    }

    @Override
    public void initialize() {

        isPluginLoaded = false;

        // try to find WorldGuard and verify that the plug-in found under that name actually is one
        Plugin loadedPlugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
        if ((loadedPlugin == null) || (!(loadedPlugin instanceof WorldGuardPlugin))) {

            // not found, don't allow features using it to be enabled
            worldGuard = null;
            getPlugin().getLogger().info("Could not find WorldGuard; features are disabled.");
            return;

        } else {

            // we found it, so now we can use it
            worldGuard = (WorldGuardPlugin) loadedPlugin;
            getPlugin().getLogger().info("Found WorldGuard; features can be enabled.");
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

    public void addMemberToRegion(String playerName, String memberName) {

        updateRegions(playerName, memberName, false, false);
    }

    public void removeMemberFromRegion(String playerName, String memberName) {

        updateRegions(playerName, memberName, true, false);
    }

    public void addOwnerToRegion(String playerName, String memberName) {

        updateRegions(playerName, memberName, false, true);
    }

    public void removeOwnerFromRegion(String playerName, String memberName) {

        updateRegions(playerName, memberName, true, true);
    }

    private void updateRegions(String playerName, String memberName, boolean remove, boolean memberAsOwner) {

        // get the region manager
        Player player = Bukkit.getPlayer(playerName);
        RegionManager regionManager = worldGuard.getRegionManager(player.getWorld());

        // get the list of regions from the manager
        Map<String, ProtectedRegion> regionMap = regionManager.getRegions();

        // For each of the regions in this map, check to see which one's are owned by the localPlayer.
        // Then for each of those, add the member to the members list.
        // Options granted to the member past that point is up to the region flags.
        for (ProtectedRegion region : regionMap.values()) {

            // skip it if it has nobody
            if (!region.hasMembersOrOwners()) {
                continue;
            }

            // check to see if the player owns this region
            if (region.isOwner(playerName)) {

                if (memberAsOwner) {

                    // get the list of owners and adjust as needed
                    DefaultDomain domain = region.getOwners();
                    if (remove) {
                        domain.removePlayer(memberName);
                    } else {
                        domain.addPlayer(memberName);
                    }

                    // update the member list
                    region.setOwners(domain);

                    // and save it
                    try {
                        regionManager.save();
                    } catch (ProtectionDatabaseException e) {
                        e.printStackTrace();
                    }

                } else {

                    // get the list of members and adjust as needed
                    DefaultDomain domain = region.getMembers();

                    if (remove) {
                        domain.removePlayer(memberName);
                    } else {
                        domain.addPlayer(memberName);
                    }

                    // update the member list
                    region.setMembers(domain);

                    // and save it
                    try {
                        regionManager.save();
                    } catch (ProtectionDatabaseException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
