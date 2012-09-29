package com.netprogs.minecraft.plugins.social.listener;

import java.util.Map;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.command.util.MessageParameter;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.event.PlayerMessageCountEvent;
import com.netprogs.minecraft.plugins.social.storage.data.Alert;
import com.netprogs.minecraft.plugins.social.storage.data.Gift;
import com.netprogs.minecraft.plugins.social.storage.data.Request;
import com.netprogs.minecraft.plugins.social.storage.data.Sticky;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

        // verify that the sender is actually a player
        if (event.getPlayer() instanceof Player) {

            Player player = event.getPlayer();

            // check to see if they're part of the network
            SocialPerson playerPerson = SocialNetworkPlugin.getStorage().getPerson(player.getName());
            if (playerPerson == null) {

                // If no person object was found, it means they're a new player.
                // At this point we'll create them an account.
                if (SocialNetworkPlugin.getSettings().isAutoJoinOnLogin()) {
                    playerPerson = SocialNetworkPlugin.getStorage().addPerson(player);
                }
            }

            // Check for this to be NULL again. If it is, it means the player wishes to be excluded
            if (playerPerson == null) {
                return;
            }

            // enable their chat upon login (in case they forgot about it or crashed)
            SocialNetworkPlugin.getChatManager().enable(event.getPlayer());

            // We want to notify everyone that is in this players groups that they have logged in.
            // Make sure that the event timer for this has expired. This is used to avoid spamming the chat.
            long timeRemaining = SocialNetworkPlugin.getTimerManager().eventOnTimer(player.getName(), "LOGIN");
            if (timeRemaining <= 0) {

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
                            MessageUtil.sendMessage(notifyPlayer, "social.group.login", ChatColor.GREEN,
                                    new MessageParameter("<player>", playerPerson.getName(), ChatColor.AQUA));
                        }
                    }
                }

                // reset their timer for this notification
                long cooldown = SocialNetworkPlugin.getSettings().getLoginNotificationCooldown();
                SocialNetworkPlugin.getTimerManager().updateEventTimer(player.getName(), "LOGIN", cooldown);
            }

            // now let's check to see what's available to them.
            int alertsCount = playerPerson.getMessagesCount(Alert.class);
            int requestsCount = playerPerson.getMessagesCount(Request.class);

            // send out responses to the player
            MessageUtil.sendHeaderMessage(event.getPlayer(), "social.login.welcome.sender");

            ResourcesConfig resources = SocialNetworkPlugin.getResources();

            // Requests
            String requestSenderMessage = resources.getResource("social.message.requests");
            MessageUtil.sendLoginMessageCountMessage(event.getPlayer(), requestSenderMessage, requestsCount);

            // Alerts
            String alertSenderMessage = resources.getResource("social.message.alerts");
            MessageUtil.sendLoginMessageCountMessage(event.getPlayer(), alertSenderMessage, alertsCount);

            // now that we've created our initial response, fire off our event letting anyone else add to it
            PlayerMessageCountEvent countEvent = new PlayerMessageCountEvent(event.getPlayer(), playerPerson);
            Bukkit.getServer().getPluginManager().callEvent(countEvent);

            // check to see if we need to send out a gender choice reminder
            boolean genderChoiceRequired = SocialNetworkPlugin.getSettings().isGenderChoiceRequired();
            boolean genderChoiceReminderEnabled = SocialNetworkPlugin.getSettings().isGenderChoiceReminderEnabled();

            if (genderChoiceRequired && genderChoiceReminderEnabled && playerPerson.getGender() == null
                    && !playerPerson.isGenderChoiceRemindersIgnored()) {

                // now tell the user they need to use the /social gender <male/female> command
                MessageUtil.sendMessage(player, "social.gender.choose.reminder.sender", ChatColor.RED);
                MessageUtil.sendMessage(player, "social.gender.choose.commands.sender", ChatColor.GREEN);
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
        String stickySenderMessage = SocialNetworkPlugin.getResources().getResource("social.message.stickies");
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
        String giftSenderMessage = SocialNetworkPlugin.getResources().getResource("social.message.gifts");
        MessageUtil.sendLoginMessageCountMessage(event.getPlayer(), giftSenderMessage, count);
    }
}
