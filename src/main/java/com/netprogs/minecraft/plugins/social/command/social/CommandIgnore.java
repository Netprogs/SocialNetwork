package com.netprogs.minecraft.plugins.social.command.social;

import java.util.List;

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

public class CommandIgnore extends SocialNetworkCommand<ISocialNetworkSettings> {

    public CommandIgnore() {
        super(SocialNetworkCommandType.ignore);
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

        // attempt to run the secondary commands first
        boolean commandHandled = handleSecondaryCommands(player, playerPerson, arguments);

        // there were none, so lets try the main friend request command
        if (!commandHandled) {

            // check arguments
            if (arguments.size() != 1) {
                throw new ArgumentsMissingException();
            }

            // get our arguments
            String ignorePlayerName = arguments.get(0);

            // make sure the player is in the network
            SocialPerson ignorePerson = SocialNetworkPlugin.getStorage().getPerson(ignorePlayerName);
            if (ignorePerson == null) {
                throw new PlayerNotInNetworkException(ignorePlayerName);
            }

            // add them to the ignore list
            playerPerson.addIgnore(ignorePerson);
            SocialNetworkPlugin.getStorage().savePerson(playerPerson);

            // tell them they've added the person
            MessageUtil.sendMessage(player, "social.ignore.add.completed.sender", ChatColor.GREEN,
                    new MessageParameter("<player>", ignorePerson.getName(), ChatColor.AQUA));
        }

        return true;
    }

    private boolean handleSecondaryCommands(Player player, SocialPerson playerPerson, List<String> arguments)
            throws ArgumentsMissingException, PlayerNotInNetworkException, PlayerNotOnlineException {

        // need to make sure we have at least one argument (the secondary command)
        if (arguments.size() >= 1) {

            if (arguments.get(0).equals("remove")) {

                // check arguments
                if (arguments.size() != 2) {
                    throw new ArgumentsMissingException();
                }

                String ignorePersonName = arguments.get(1);

                // make sure the player is in the network
                SocialPerson ignorePerson = SocialNetworkPlugin.getStorage().getPerson(ignorePersonName);
                if (ignorePerson == null) {
                    throw new PlayerNotInNetworkException(ignorePersonName);
                }

                // remove them from the ignore list
                playerPerson.removeIgnore(ignorePerson);
                SocialNetworkPlugin.getStorage().savePerson(playerPerson);

                // tell them they've removed the person
                MessageUtil.sendMessage(player, "social.ignore.remove.completed.sender", ChatColor.GREEN,
                        new MessageParameter("<player>", ignorePerson.getName(), ChatColor.AQUA));

                return true;

            } else if (arguments.get(0).equals("list")) {

                // send a header
                MessageUtil.sendHeaderMessage(player, "social.ignore.list.header");

                // send a response for each person with their message count
                List<String> ignoredPlayers = playerPerson.getIgnoredPlayers();
                for (String playerName : ignoredPlayers) {
                    player.sendMessage(playerName);
                }

                if (ignoredPlayers.size() == 0) {
                    MessageUtil.sendMessage(player, "social.ignore.list.none", ChatColor.GREEN);
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public HelpSegment help() {

        ResourcesConfig config = SocialNetworkPlugin.getResources();
        HelpSegment helpSegment = new HelpSegment(getCommandType());

        HelpMessage mainCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), null, "<player>",
                        config.getResource("social.ignore.help"));
        helpSegment.addEntry(mainCommand);

        HelpMessage removeCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "remove", "<player>",
                        config.getResource("social.ignore.help.remove"));
        helpSegment.addEntry(removeCommand);

        HelpMessage listCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "list", null,
                        config.getResource("social.ignore.help.list"));
        helpSegment.addEntry(listCommand);

        return helpSegment;
    }
}
