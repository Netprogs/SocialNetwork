package com.netprogs.minecraft.plugins.social.listener.perk;

import java.util.logging.Logger;

import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.config.PluginConfig;
import com.netprogs.minecraft.plugins.social.config.settings.SettingsConfig;
import com.netprogs.minecraft.plugins.social.config.settings.perk.LWCSettings;
import com.netprogs.minecraft.plugins.social.event.PlayerMemberChangeEvent;
import com.netprogs.minecraft.plugins.social.event.PlayerMemberChangeEvent.Type;
import com.netprogs.minecraft.plugins.social.integration.LWCIntegration;
import com.netprogs.minecraft.plugins.social.storage.SocialNetwork;
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

public class LWCListener extends PerkListener<LWCSettings, IPersonPerkSettings> {

    private final Logger logger = Logger.getLogger("Minecraft");

    public LWCListener() {
        super(ListenerType.lwc);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMemberChangeEvent(PlayerMemberChangeEvent event) {

        // Something changed with your group members. Let's check to see if anyone left.
        SocialPerson playerPerson = SocialNetwork.getInstance().getPerson(event.getPlayerName());
        SocialPerson memberPerson = SocialNetwork.getInstance().getPerson(event.getMemberName());
        if (playerPerson != null && memberPerson != null) {

            if (event.getEventType() == Type.postAdd) {

                // get the settings for this perk to verify that the member belongs to a social group of player
                LWCSettings lwcSettings = getPerkSettings(playerPerson, memberPerson);
                if (lwcSettings != null) {

                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("LWC: " + playerPerson.getName() + " is adding " + memberPerson.getName()
                                + " as Member.");
                    }

                    // add them to the players lwc
                    LWCIntegration.getInstance().addPermission(playerPerson.getName(), memberPerson.getName());
                }

            } else if (event.getEventType() == Type.postRemove) {

                // If the settings return NULL, that means the player no longer has the member in any of their social
                // groups that have this particular perk; so remove them as a member of the region
                LWCSettings lwcSettings = getPerkSettings(playerPerson, memberPerson);
                if (lwcSettings == null) {

                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("LWC: " + playerPerson.getName() + " is removing " + memberPerson.getName()
                                + " as Member.");
                    }

                    // add them to the players lwc as a member
                    LWCIntegration.getInstance().removePermission(playerPerson.getName(), memberPerson.getName());
                }
            }
        }
    }
}
