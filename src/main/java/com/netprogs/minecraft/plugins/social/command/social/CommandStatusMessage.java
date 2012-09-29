package com.netprogs.minecraft.plugins.social.command.social;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommand;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommandType;
import com.netprogs.minecraft.plugins.social.command.exception.ArgumentsMissingException;
import com.netprogs.minecraft.plugins.social.command.exception.InvalidPermissionsException;
import com.netprogs.minecraft.plugins.social.command.exception.PlayerNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.exception.PlayerNotOnlineException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotPlayerException;
import com.netprogs.minecraft.plugins.social.command.help.HelpBook;
import com.netprogs.minecraft.plugins.social.command.help.HelpMessage;
import com.netprogs.minecraft.plugins.social.command.help.HelpSegment;
import com.netprogs.minecraft.plugins.social.command.util.MessageParameter;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.ISocialNetworkSettings;
import com.netprogs.minecraft.plugins.social.storage.SocialNetworkStorage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/*
 * "Social Network" is a Craftbukkit Minecraft server modification plug-in. It attempts to add a 
 * social environment to your server by allowing players to be placed into different types of social groups.
 * 
 * Copyright (C) 2012 Scott Milne
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

public class CommandStatusMessage extends SocialNetworkCommand<ISocialNetworkSettings> {

    public CommandStatusMessage() {
        super(SocialNetworkCommandType.status);
    }

    @Override
    public boolean run(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            InvalidPermissionsException, SenderNotPlayerException, SenderNotInNetworkException,
            PlayerNotInNetworkException, PlayerNotOnlineException {

        // verify that the sender is actually a player
        verifySenderAsPlayer(sender);

        // check permissions
        if (!hasCommandPermission(sender)) {
            throw new InvalidPermissionsException();
        }

        Player player = (Player) sender;

        // get the social network data
        SocialNetworkStorage socialConfig = SocialNetworkPlugin.getStorage();

        // check to see if the person is already there, if not, then start to add them
        SocialPerson playerPerson = socialConfig.getPerson(player.getName());
        if (playerPerson == null) {
            throw new SenderNotInNetworkException();
        }

        // check arguments
        if (arguments.size() == 0) {
            throw new ArgumentsMissingException();
        }

        // the message is everything else
        String message = "";
        for (int i = 0; i < arguments.size(); i++) {
            message += arguments.get(i) + " ";
        }

        // update their status message and save the changes
        playerPerson.setStatusMessage(message);
        SocialNetworkPlugin.getStorage().savePerson(playerPerson);

        // tell them they've updated their message
        MessageUtil.sendMessage(player, "social.statusmessage.completed", ChatColor.GREEN);

        // We want to notify everyone that is in this players groups that they updated their status message.
        // Make sure that the event timer for this has expired. This is used to avoid spamming the chat.
        long timeRemaining = SocialNetworkPlugin.getTimerManager().eventOnTimer(player.getName(), "STATUS");
        if (timeRemaining <= 0) {

            // Get the list of all unique player among all their groups
            // Then for each of those report that this person has logged in
            Map<String, SocialPerson> notifyPlayers =
                    SocialNetworkPlugin.getStorage().getNotificationPlayers(playerPerson);

            MessageParameter playerParam = new MessageParameter("<player>", playerPerson.getName(), ChatColor.AQUA);
            MessageParameter statusParam = new MessageParameter("<status>", message, ChatColor.RESET);

            List<MessageParameter> messageVariables = new ArrayList<MessageParameter>();
            messageVariables.add(playerParam);
            messageVariables.add(statusParam);

            for (String notifyPlayerName : notifyPlayers.keySet()) {

                // check to see if this person wants to receive login notifications
                SocialPerson notifySocialPerson = notifyPlayers.get(notifyPlayerName);
                if (!notifySocialPerson.isLoginUpdatesIgnored()) {

                    Player notifyPlayer = Bukkit.getPlayer(notifyPlayerName);
                    if (notifyPlayer != null) {
                        MessageUtil.sendMessage(notifyPlayer, "social.group.status", ChatColor.GREEN, messageVariables);
                    }
                }
            }

            // reset their timer for this notification
            long cooldown = SocialNetworkPlugin.getSettings().getLoginNotificationCooldown();
            SocialNetworkPlugin.getTimerManager().updateEventTimer(player.getName(), "STATUS", cooldown);
        }

        return true;
    }

    @Override
    public HelpSegment help() {

        ResourcesConfig config = SocialNetworkPlugin.getResources();
        HelpSegment helpSegment = new HelpSegment(getCommandType());

        HelpMessage mainCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), null, "<message>",
                        config.getResource("social.statusmessage.help"));
        helpSegment.addEntry(mainCommand);

        return helpSegment;
    }
}
