package com.netprogs.minecraft.plugins.social.command.social;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommand;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommandType;
import com.netprogs.minecraft.plugins.social.command.exception.ArgumentsMissingException;
import com.netprogs.minecraft.plugins.social.command.exception.InvalidPermissionsException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotPlayerException;
import com.netprogs.minecraft.plugins.social.command.help.HelpMessage;
import com.netprogs.minecraft.plugins.social.command.help.HelpSegment;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.config.PluginConfig;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.ISocialNetworkSettings;
import com.netprogs.minecraft.plugins.social.storage.SocialNetwork;
import com.netprogs.minecraft.plugins.social.storage.data.Alert;

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

public class CommandAlerts extends SocialNetworkCommand<ISocialNetworkSettings> {

    private final Logger logger = Logger.getLogger("Minecraft");

    public CommandAlerts() {
        super(SocialNetworkCommandType.alerts);
    }

    @Override
    public boolean run(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            InvalidPermissionsException, SenderNotPlayerException, SenderNotInNetworkException {

        // verify that the sender is actually a player
        verifySenderAsPlayer(sender);

        // check arguments
        if (arguments.size() != 0) {
            throw new ArgumentsMissingException();
        }

        // check permissions
        if (!hasCommandPermission(sender)) {
            throw new InvalidPermissionsException();
        }

        Player player = (Player) sender;

        // get the social network data
        SocialNetwork socialConfig = SocialNetwork.getInstance();

        // check to see if the sender is part of the network
        SocialPerson person = socialConfig.getPerson(player.getName());
        if (person == null) {
            throw new SenderNotInNetworkException();
        }

        // group them by type
        List<Alert> quitAlerts = new ArrayList<Alert>();

        // get the list of requests from each player
        for (String key : person.getMessagePlayers(Alert.class)) {
            for (Alert alert : person.getMessagesFrom(key, Alert.class)) {
                if (alert.getAlertType() == Alert.Type.quit) {
                    quitAlerts.add(alert);
                    person.removeMessage(key, alert);
                }
            }
        }

        // save the person to remove the alerts
        SocialNetwork.getInstance().savePerson(person);

        // check to see if any of the lists got something added
        boolean hasRequests = (quitAlerts.size() > 0);

        // send the header
        MessageUtil.sendHeaderMessage(sender, "social.alerts.header.sender");

        // we don't have anything, let the player know
        if (!hasRequests) {
            MessageUtil.sendMessage(sender, "social.alerts.noneAvailable.sender", ChatColor.RED);
            return false;
        }

        // still here, time to display what we have
        String quitTag =
                PluginConfig.getInstance().getConfig(ResourcesConfig.class)
                        .getResource("social.alerts.tag.quit.sender");

        // okay, now we'll display them
        displayRequests(player, quitTag, quitAlerts);

        return true;
    }

    private void displayRequests(Player player, String prefix, List<Alert> alerts) {

        for (Alert alert : alerts) {
            String displayRequest = ChatColor.RED + prefix;
            displayRequest += ChatColor.GREEN + " " + alert.getPlayerName();
            displayRequest += ChatColor.WHITE + " " + alert.getAlertMessage();
            player.sendMessage(displayRequest);
        }
    }

    @Override
    public HelpSegment help() {

        ResourcesConfig config = PluginConfig.getInstance().getConfig(ResourcesConfig.class);

        HelpMessage mainCommand = new HelpMessage();
        mainCommand.setCommand(getCommandType().toString());
        mainCommand.setDescription(config.getResource("social.alerts.help"));

        HelpSegment helpSegment = new HelpSegment(getCommandType());
        helpSegment.addEntry(mainCommand);

        return helpSegment;
    }
}
