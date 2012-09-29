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
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotPlayerException;
import com.netprogs.minecraft.plugins.social.command.help.HelpBook;
import com.netprogs.minecraft.plugins.social.command.help.HelpMessage;
import com.netprogs.minecraft.plugins.social.command.help.HelpSegment;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.ISocialNetworkSettings;

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

public class CommandMute extends SocialNetworkCommand<ISocialNetworkSettings> {

    public CommandMute() {
        super(SocialNetworkCommandType.mute);
    }

    @Override
    public boolean run(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            InvalidPermissionsException, SenderNotPlayerException, PlayerNotInNetworkException,
            PlayerNotOnlineException {

        // verify that the sender is actually a player
        verifySenderAsPlayer(sender);

        // check permissions
        if (!hasCommandPermission(sender)) {
            throw new InvalidPermissionsException();
        }

        // check arguments
        if (arguments.size() == 0) {
            throw new ArgumentsMissingException();
        }

        Player player = (Player) sender;

        // knock off the command from the list leaving only the parameters
        String command = arguments.remove(0);

        // check to see if the requested person is in the network
        SocialPerson playerPerson = SocialNetworkPlugin.getStorage().getPerson(player.getName());
        if (playerPerson == null) {
            throw new PlayerNotInNetworkException(player.getName());
        }

        // check for each command and dispatch them as needed
        if (command.equals("login")) {

            handleMuteLogin(sender, playerPerson, arguments);

        } else if (command.equals("status")) {

            handleMuteStatus(sender, playerPerson, arguments);

        } else if (command.equals("gender")) {

            handleMuteGender(sender, playerPerson, arguments);
        }

        return true;
    }

    private void handleMuteLogin(CommandSender sender, SocialPerson playerPerson, List<String> arguments)
            throws ArgumentsMissingException, PlayerNotInNetworkException {

        // toggle their gender mute flag
        boolean ignored = playerPerson.isLoginUpdatesIgnored();
        if (ignored) {

            playerPerson.setLoginUpdatesIgnored(false);
            MessageUtil.sendMessage(sender, "social.mute.login.off.sender", ChatColor.GREEN);

        } else {

            playerPerson.setLoginUpdatesIgnored(true);
            MessageUtil.sendMessage(sender, "social.mute.login.on.sender", ChatColor.GREEN);
        }

        SocialNetworkPlugin.getStorage().savePerson(playerPerson);
    }

    private void handleMuteStatus(CommandSender sender, SocialPerson playerPerson, List<String> arguments)
            throws ArgumentsMissingException, PlayerNotInNetworkException {

        // toggle their gender mute flag
        boolean ignored = playerPerson.isStatusUpdatesIgnored();
        if (ignored) {

            playerPerson.setStatusUpdatesIgnored(false);
            MessageUtil.sendMessage(sender, "social.mute.status.off.sender", ChatColor.GREEN);

        } else {

            playerPerson.setStatusUpdatesIgnored(true);
            MessageUtil.sendMessage(sender, "social.mute.status.on.sender", ChatColor.GREEN);
        }

        SocialNetworkPlugin.getStorage().savePerson(playerPerson);
    }

    private void handleMuteGender(CommandSender sender, SocialPerson playerPerson, List<String> arguments)
            throws ArgumentsMissingException, PlayerNotInNetworkException {

        // toggle their gender mute flag
        boolean ignored = playerPerson.isGenderChoiceRemindersIgnored();
        if (ignored) {

            playerPerson.setGenderChoiceRemindersIgnored(false);
            MessageUtil.sendMessage(sender, "social.mute.gender.off.sender", ChatColor.GREEN);

        } else {

            playerPerson.setGenderChoiceRemindersIgnored(true);
            MessageUtil.sendMessage(sender, "social.mute.gender.on.sender", ChatColor.GREEN);
        }

        SocialNetworkPlugin.getStorage().savePerson(playerPerson);
    }

    @Override
    public HelpSegment help() {

        HelpSegment helpSegment = new HelpSegment(getCommandType());
        ResourcesConfig config = SocialNetworkPlugin.getResources();

        HelpMessage loginCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "login", null,
                        config.getResource("social.mute.login.help.toggle"));
        helpSegment.addEntry(loginCommand);

        HelpMessage statusCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "status", null,
                        config.getResource("social.mute.status.help.toggle"));
        helpSegment.addEntry(statusCommand);

        HelpMessage genderCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "gender", null,
                        config.getResource("social.mute.gender.help.toggle"));
        helpSegment.addEntry(genderCommand);

        return helpSegment;
    }
}
