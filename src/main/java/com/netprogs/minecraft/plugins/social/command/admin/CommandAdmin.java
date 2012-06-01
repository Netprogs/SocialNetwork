package com.netprogs.minecraft.plugins.social.command.admin;

import java.util.List;
import java.util.logging.Logger;

import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommand;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommandType;
import com.netprogs.minecraft.plugins.social.command.exception.ArgumentsMissingException;
import com.netprogs.minecraft.plugins.social.command.exception.InvalidPermissionsException;
import com.netprogs.minecraft.plugins.social.command.exception.PlayerNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.exception.PlayerNotOnlineException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotPlayerException;
import com.netprogs.minecraft.plugins.social.command.help.HelpMessage;
import com.netprogs.minecraft.plugins.social.command.help.HelpSegment;
import com.netprogs.minecraft.plugins.social.command.util.MessageParameter;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.config.PluginConfig;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.ISocialNetworkSettings;
import com.netprogs.minecraft.plugins.social.storage.SocialNetwork;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

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

public class CommandAdmin extends SocialNetworkCommand<ISocialNetworkSettings> {

    private final Logger logger = Logger.getLogger("Minecraft");

    public CommandAdmin() {
        super(SocialNetworkCommandType.admin);
    }

    @Override
    public boolean run(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            InvalidPermissionsException, SenderNotPlayerException, PlayerNotInNetworkException,
            PlayerNotOnlineException {

        // check permissions
        if (!hasCommandPermission(sender)) {
            throw new InvalidPermissionsException();
        }

        // check arguments
        if (arguments.size() == 0) {
            throw new ArgumentsMissingException();
        }

        // knock off the command from the list leaving only the parameters
        String command = arguments.remove(0);

        // check for each command and dispatch them as needed
        if (command.equals("priest")) {

            handlePriest(sender, arguments);

        } else if (command.equals("lawyer")) {

            handleLawyer(sender, arguments);
        }

        return true;
    }

    private void handlePriest(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            PlayerNotInNetworkException {

        if (arguments.size() == 0) {
            throw new ArgumentsMissingException();
        }

        if (arguments.get(0).equals("list")) {

            // send a header
            MessageUtil.sendHeaderMessage(sender, "social.admin.priest.list.header.sender");

            // send a response for each person with their message count
            List<String> priestPlayers = SocialNetwork.getInstance().getPriests();
            for (String playerName : priestPlayers) {
                sender.sendMessage(playerName);
            }

            if (priestPlayers.size() == 0) {
                MessageUtil.sendMessage(sender, "social.admin.priest.list.none.sender", ChatColor.GREEN);
            }

        } else {

            // We want to toggle the status of the given player

            // check to see if the requested person is in the network
            String playerName = arguments.get(0);
            SocialPerson playerPerson = SocialNetwork.getInstance().getPerson(playerName);
            if (playerPerson == null) {
                throw new PlayerNotInNetworkException(playerName);
            }

            // check to see if they're already a priest
            if (SocialNetwork.getInstance().hasPriest(playerPerson.getName())) {

                // toggle them off
                playerPerson.setPriest(false);
                SocialNetwork.getInstance().removePriest(playerPerson.getName());

                // tell the admin they got switched
                MessageUtil.sendMessage(sender, "social.admin.priest.remove.completed.sender", ChatColor.GREEN,
                        new MessageParameter("<player>", playerPerson.getName(), ChatColor.AQUA));

            } else {

                // toggle them on
                playerPerson.setPriest(true);
                SocialNetwork.getInstance().addPriest(playerPerson.getName());

                // tell the admin they got switched
                MessageUtil.sendMessage(sender, "social.admin.priest.add.completed.sender", ChatColor.GREEN,
                        new MessageParameter("<player>", playerPerson.getName(), ChatColor.AQUA));
            }

            // save the changes
            SocialNetwork.getInstance().savePerson(playerPerson);
        }
    }

    private void handleLawyer(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            PlayerNotInNetworkException {

        if (arguments.size() == 0) {
            throw new ArgumentsMissingException();
        }

        if (arguments.get(0).equals("list")) {

            // send a header
            MessageUtil.sendHeaderMessage(sender, "social.admin.lawyer.list.header.sender");

            // send a response for each person with their message count
            List<String> lawyerPlayers = SocialNetwork.getInstance().getLawyers();
            for (String playerName : lawyerPlayers) {
                sender.sendMessage(playerName);
            }

            if (lawyerPlayers.size() == 0) {
                MessageUtil.sendMessage(sender, "social.admin.lawyer.list.none.sender", ChatColor.GREEN);
            }

        } else {

            // We want to toggle the status of the given player

            // check to see if the requested person is in the network
            String playerName = arguments.get(0);
            SocialPerson playerPerson = SocialNetwork.getInstance().getPerson(playerName);
            if (playerPerson == null) {
                throw new PlayerNotInNetworkException(playerName);
            }

            // check to see if they're already a lawyer
            if (SocialNetwork.getInstance().hasLawyer(playerPerson.getName())) {

                // toggle them off
                playerPerson.setLawyer(false);
                SocialNetwork.getInstance().removeLawyer(playerPerson.getName());

                // tell the admin they got switched
                MessageUtil.sendMessage(sender, "social.admin.lawyer.remove.completed.sender", ChatColor.GREEN,
                        new MessageParameter("<player>", playerPerson.getName(), ChatColor.AQUA));

            } else {

                // toggle them on
                playerPerson.setLawyer(true);
                SocialNetwork.getInstance().addLawyer(playerPerson.getName());

                // tell the admin they got switched
                MessageUtil.sendMessage(sender, "social.admin.lawyer.add.completed.sender", ChatColor.GREEN,
                        new MessageParameter("<player>", playerPerson.getName(), ChatColor.AQUA));
            }

            // save the changes
            SocialNetwork.getInstance().savePerson(playerPerson);
        }
    }

    // "social.admin.priest.help.toggle": "Toggles the priest status for player.",
    // "social.admin.priest.help.list": "Lists current priests.",

    // "social.admin.priest.help.toggle": "Toggles the priest status for player.",
    // "social.admin.priest.help.list": "Lists current priests.",

    @Override
    public HelpSegment help() {

        HelpSegment helpSegment = new HelpSegment(getCommandType());
        ResourcesConfig config = PluginConfig.getInstance().getConfig(ResourcesConfig.class);

        HelpMessage priestCommand = new HelpMessage();
        priestCommand.setCommand(getCommandType().toString());
        priestCommand.setArguments("priest <player>");
        priestCommand.setDescription(config.getResource("social.admin.priest.help.toggle"));
        helpSegment.addEntry(priestCommand);

        HelpMessage priestListCommand = new HelpMessage();
        priestListCommand.setCommand(getCommandType().toString());
        priestListCommand.setArguments("priest list");
        priestListCommand.setDescription(config.getResource("social.admin.priest.help.list"));
        helpSegment.addEntry(priestListCommand);

        HelpMessage lawyerCommand = new HelpMessage();
        lawyerCommand.setCommand(getCommandType().toString());
        lawyerCommand.setArguments("lawyer <player>");
        lawyerCommand.setDescription(config.getResource("social.admin.lawyer.help.toggle"));
        helpSegment.addEntry(lawyerCommand);

        HelpMessage lawyerListCommand = new HelpMessage();
        lawyerListCommand.setCommand(getCommandType().toString());
        lawyerListCommand.setArguments("lawyer list");
        lawyerListCommand.setDescription(config.getResource("social.admin.lawyer.help.list"));
        helpSegment.addEntry(lawyerListCommand);

        return helpSegment;
    }
}
