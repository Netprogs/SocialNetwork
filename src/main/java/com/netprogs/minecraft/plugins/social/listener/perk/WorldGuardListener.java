package com.netprogs.minecraft.plugins.social.listener.perk;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.config.settings.perk.WorldGuardSettings;
import com.netprogs.minecraft.plugins.social.event.PlayerMemberChangeEvent;
import com.netprogs.minecraft.plugins.social.event.PlayerMemberChangeEvent.Type;
import com.netprogs.minecraft.plugins.social.storage.data.perk.IPersonPerkSettings;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

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

public class WorldGuardListener extends PerkListener<WorldGuardSettings, IPersonPerkSettings> {

    public WorldGuardListener() {
        super(ListenerType.worldguard);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMemberChangeEvent(PlayerMemberChangeEvent event) {

        // if (SocialNetworkPlugin.getSettings().isLoggingDebug()) {
        // logger.info("WG: listener " + event.getPlayerName() + " " + event.getEventType() + " "
        // + event.getMemberName() + " to/from " + event.getGroupType());
        // }

        // Something changed with your group members. Let's check to see if anyone left.
        SocialPerson playerPerson = SocialNetworkPlugin.getStorage().getPerson(event.getPlayerName());
        SocialPerson memberPerson = SocialNetworkPlugin.getStorage().getPerson(event.getMemberName());
        if (playerPerson != null && memberPerson != null) {

            if (event.getEventType() == Type.postAdd) {

                // get the settings for this perk to verify that the member belongs to a social group of player
                WorldGuardSettings worldGuardSettings = getPerkSettings(playerPerson, memberPerson);
                if (worldGuardSettings != null) {

                    SocialNetworkPlugin.log("WG: " + playerPerson.getName() + " is adding " + memberPerson.getName()
                            + " as Member.");

                    // add them to the players region
                    SocialNetworkPlugin.getWorldGuard().addMemberToRegion(playerPerson.getName(),
                            memberPerson.getName());
                }

            } else if (event.getEventType() == Type.postRemove) {

                // If the settings return NULL, that means the player no longer has the member in any of their social
                // groups that have this particular perk; so remove them as a member of the region
                WorldGuardSettings worldGuardSettings = getPerkSettings(playerPerson, memberPerson);
                if (worldGuardSettings == null) {

                    SocialNetworkPlugin.log("WG: " + playerPerson.getName() + " is removing " + memberPerson.getName()
                            + " as Member.");

                    // add them to the players region as a member
                    SocialNetworkPlugin.getWorldGuard().removeMemberFromRegion(playerPerson.getName(),
                            memberPerson.getName());
                }
            }
        }
    }
}
