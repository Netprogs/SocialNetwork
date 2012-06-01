package com.netprogs.minecraft.plugins.social.listener;

import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.config.PluginConfig;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.event.PlayerMessageCountEvent;
import com.netprogs.minecraft.plugins.social.storage.SocialNetwork;
import com.netprogs.minecraft.plugins.social.storage.data.Alert;
import com.netprogs.minecraft.plugins.social.storage.data.Gift;
import com.netprogs.minecraft.plugins.social.storage.data.Request;
import com.netprogs.minecraft.plugins.social.storage.data.Sticky;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

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

public class PlayerJoinListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoinEvent(PlayerJoinEvent event) {

        // When the player logs in, check to see what messages they have waiting for them

        // We only want to check for our types. Other people may be placing messages into the queue, but we don't want
        // to display them here.

        // verify that the sender is actually a player
        if (event.getPlayer() instanceof Player) {

            // check to see if they're part of the network
            SocialPerson playerPerson = SocialNetwork.getInstance().getPerson(event.getPlayer().getName());
            if (playerPerson != null) {

                // now let's check to see what's available to them.
                int alertsCount = playerPerson.getMessagesCount(Alert.class);
                int requestsCount = playerPerson.getMessagesCount(Request.class);

                // send out responses to the player
                MessageUtil.sendHeaderMessage(event.getPlayer(), "social.login.welcome.sender");

                // Requests
                String requestSenderMessage =
                        PluginConfig.getInstance().getConfig(ResourcesConfig.class)
                                .getResource("social.message.requests");
                MessageUtil.sendLoginMessageCountMessage(event.getPlayer(), requestSenderMessage, requestsCount);

                // Alerts
                String alertSenderMessage =
                        PluginConfig.getInstance().getConfig(ResourcesConfig.class)
                                .getResource("social.message.alerts");
                MessageUtil.sendLoginMessageCountMessage(event.getPlayer(), alertSenderMessage, alertsCount);

                // now that we've created our initial response, fire off our event letting anyone else add to it
                PlayerMessageCountEvent countEvent = new PlayerMessageCountEvent(event.getPlayer(), playerPerson);
                Bukkit.getServer().getPluginManager().callEvent(countEvent);
            }
        }
    }

    /**
     * We're listening for this event so we can add a count message when the user logs in.
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerStickyMessageCountEvent(PlayerMessageCountEvent event) {

        SocialPerson person = event.getPlayerPerson();

        int count = person.getMessagesCount(Sticky.class);

        String stickySenderMessage =
                PluginConfig.getInstance().getConfig(ResourcesConfig.class).getResource("social.message.stickies");

        MessageUtil.sendLoginMessageCountMessage(event.getPlayer(), stickySenderMessage, count);
    }

    /**
     * We're listening for this event so we can add a count message when the user logs in.
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerGiftMessageCountEvent(PlayerMessageCountEvent event) {

        SocialPerson person = event.getPlayerPerson();

        int count = person.getMessagesCount(Gift.class);

        String giftSenderMessage =
                PluginConfig.getInstance().getConfig(ResourcesConfig.class).getResource("social.message.gifts");

        MessageUtil.sendLoginMessageCountMessage(event.getPlayer(), giftSenderMessage, count);
    }
}
