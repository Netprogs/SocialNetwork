package com.netprogs.minecraft.plugins.social.listener;

import java.util.Map;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.command.util.MessageParameter;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

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

public class PlayerQuitListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuitEvent(PlayerQuitEvent event) {

        // verify that the sender is actually a player
        if (event.getPlayer() instanceof Player) {

            Player player = event.getPlayer();

            // We want to notify everyone that is in this players groups that they have logged out.
            // Make sure that the event timer for this has expired. This is used to avoid spamming the chat.
            long timeRemaining = SocialNetworkPlugin.getTimerManager().eventOnTimer(player.getName(), "LOGIN");
            if (timeRemaining <= 0) {

                SocialPerson playerPerson = SocialNetworkPlugin.getStorage().getPerson(player.getName());
                if (playerPerson != null) {

                    // Get the list of all unique player among all their groups
                    // Then for each of those report that this person has logged in
                    Map<String, SocialPerson> notifyPlayers =
                            SocialNetworkPlugin.getStorage().getNotificationPlayers(playerPerson);

                    for (String notifyPlayerName : notifyPlayers.keySet()) {

                        // check to see if this person wants to receive login notifications
                        SocialPerson notifySocialPerson = notifyPlayers.get(notifyPlayerName);
                        if (!notifySocialPerson.isLoginUpdatesIgnored()) {

                            Player notifyPlayer = Bukkit.getPlayer(notifyPlayerName);
                            if (notifyPlayer != null) {
                                MessageUtil.sendMessage(notifyPlayer, "social.group.logout", ChatColor.GREEN,
                                        new MessageParameter("<player>", playerPerson.getName(), ChatColor.AQUA));
                            }
                        }
                    }

                    // reset their timer for this notification
                    long cooldown = SocialNetworkPlugin.getSettings().getLoginNotificationCooldown();
                    SocialNetworkPlugin.getTimerManager().updateEventTimer(player.getName(), "LOGIN", cooldown);
                }
            }
        }
    }
}
