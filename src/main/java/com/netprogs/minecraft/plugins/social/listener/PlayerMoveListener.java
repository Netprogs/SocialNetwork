package com.netprogs.minecraft.plugins.social.listener;

import java.util.HashSet;
import java.util.Set;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.SocialPerson.WaitState;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommandType;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

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

public class PlayerMoveListener implements Listener {

    private final Set<String> genderMessageQueue = new HashSet<String>();

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMoveEvent(PlayerMoveEvent event) {

        // if already cancelled, that's fine
        if (event.isCancelled()) {
            return;
        }

        // now we want to check our settings to see if we need to stop movement until they choose a gender
        boolean genderChoiceRequired = SocialNetworkPlugin.getSettings().isGenderChoiceRequired();
        boolean genderChoiceFreezeEnabled = SocialNetworkPlugin.getSettings().isGenderChoiceFreezeEnabled();
        if (genderChoiceRequired && genderChoiceFreezeEnabled) {

            Player player = event.getPlayer();

            // Check to see if the same gender flag is true.
            // If it's true, then we'll stop the player from moving until they choose a gender.
            SocialPerson playerPerson = SocialNetworkPlugin.getStorage().getPerson(player.getName());
            if (playerPerson != null) {

                if (playerPerson.getGender() == null) {

                    // okay, we need to ask them for their gender
                    playerPerson.waitOn(WaitState.waitGenderResponse, SocialNetworkCommandType.gender);

                    // only give them the message every 5 seconds to avoid spamming
                    if (!genderMessageQueue.contains(player.getName())) {

                        // now tell the user they need to use the /join <male/female> command
                        MessageUtil.sendMessage(player, "social.gender.choose.initial.sender", ChatColor.GREEN);
                        MessageUtil.sendMessage(player, "social.gender.choose.commands.sender", ChatColor.GREEN);

                        // set a delayed scheduled event to remove their name from the list so it can be shown again
                        final String playerName = player.getName();
                        genderMessageQueue.add(player.getName());
                        Bukkit.getScheduler().scheduleSyncDelayedTask(SocialNetworkPlugin.instance, new Runnable() {
                            public void run() {
                                genderMessageQueue.remove(playerName);
                            }
                        }, 100L);
                    }

                    // cancel the movement event by teleporting them back into the same spot again
                    player.teleport(player.getLocation());

                    // this seems to cause excessive calling of the movement event, so instead using teleport above
                    // event.setCancelled(true);
                }
            }
        }
    }
}
