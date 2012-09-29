package com.netprogs.minecraft.plugins.social.listener;

import java.util.Iterator;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

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

public class PlayerChatListener implements Listener {

    // 1.2.5 support.
    // public void onPlayerChat(PlayerChatEvent event) {

    // 1.3.1 supported only.

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChatEvent(AsyncPlayerChatEvent event) {

        // verify that the sender is actually a player
        if (event.getPlayer() instanceof Player) {

            // if the player has their chat disabled, this stops them from sending messages
            if (SocialNetworkPlugin.getChatManager().isDisabled(event.getPlayer())) {

                event.setCancelled(true);

            } else {

                // if they have their chat disabled, this stops them from receiving messages
                Iterator<String> disabledPlayers = SocialNetworkPlugin.getChatManager().getDisabledPlayers();
                while (disabledPlayers.hasNext()) {

                    String playerName = disabledPlayers.next();
                    Player player = Bukkit.getServer().getPlayer(playerName);
                    if (player != null) {
                        event.getRecipients().remove(player);
                    }
                }
            }
        }
    }
}
