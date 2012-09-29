package com.netprogs.minecraft.plugins.social.command.admin;

import java.util.List;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.SocialPerson.WaitState;
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
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.ISocialNetworkSettings;

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

        } else if (command.equals("purge")) {

            handlePurge(sender, arguments);

        } else if (command.equals("clear")) {

            handleClear(sender, arguments);

        } else if (command.equals("remove")) {

            handleRemove(sender, arguments);

        } else if (command.equals("reload")) {

            handleReload(sender, arguments);

        } else if (command.equals("gender")) {

            handleGender(sender, arguments);
        }

        return true;
    }

    private void handlePurge(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            PlayerNotInNetworkException {

        if (arguments.size() != 1) {
            throw new ArgumentsMissingException();
        }

        int purgeDays = 0;
        if (arguments.size() > 0) {

            try {
                purgeDays = Integer.valueOf(Integer.parseInt(arguments.get(0)));
            } catch (Exception e) {
                // don't bother reporting it, just assume page 1
            }
        }

        if (purgeDays > 0) {

            // get the list of all players
            int purgeCount = SocialNetworkPlugin.getStorage().purgePlayers(purgeDays);

            // tell them it's done
            MessageUtil.sendMessage(sender, "social.admin.purge.completed.sender", ChatColor.GREEN,
                    new MessageParameter("<count>", Integer.toString(purgeCount), ChatColor.AQUA));
        }
    }

    private void handleGender(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            PlayerNotInNetworkException {

        if (arguments.size() != 2) {
            throw new ArgumentsMissingException();
        }

        // check to see if the requested person is in the network
        String playerName = arguments.get(0);
        SocialPerson playerPerson = SocialNetworkPlugin.getStorage().getPerson(playerName);
        if (playerPerson == null) {
            throw new PlayerNotInNetworkException(playerName);
        }

        String playerGender = arguments.get(1);
        if (!SocialPerson.Gender.male.toString().equalsIgnoreCase(playerGender)
                && !SocialPerson.Gender.female.toString().equalsIgnoreCase(playerGender)) {

            MessageUtil.sendMessage(sender, "social.admin.gender.invalid.sender", ChatColor.RED);
            return;
        }

        // change their gender
        SocialPerson.Gender gender = SocialPerson.Gender.valueOf(playerGender);
        playerPerson.setGender(gender);
        SocialNetworkPlugin.getStorage().savePerson(playerPerson);

        // tell the admin they got updated
        MessageUtil.sendMessage(sender, "social.admin.gender.completed.sender", ChatColor.GREEN);
    }

    private void handleClear(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            PlayerNotInNetworkException {

        if (arguments.size() != 1) {
            throw new ArgumentsMissingException();
        }

        // check to see if the requested person is in the network
        String playerName = arguments.get(0);
        SocialPerson playerPerson = SocialNetworkPlugin.getStorage().getPerson(playerName);
        if (playerPerson == null) {
            throw new PlayerNotInNetworkException(playerName);
        }

        // remove their timers
        SocialNetworkPlugin.getTimerManager().removeTimers(playerPerson.getName());

        // remove their wait command
        playerPerson.waitOn(WaitState.notWaiting, null);
        SocialNetworkPlugin.getStorage().savePerson(playerPerson);

        // tell the admin they got reset
        MessageUtil.sendMessage(sender, "social.admin.clear.completed.sender", ChatColor.GREEN);
    }

    private void handleRemove(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            PlayerNotInNetworkException {

        if (arguments.size() != 0) {
            throw new ArgumentsMissingException();
        }

        // check to see if the requested person is in the network
        String playerName = arguments.get(0);
        SocialPerson playerPerson = SocialNetworkPlugin.getStorage().getPerson(playerName);
        if (playerPerson == null) {
            throw new PlayerNotInNetworkException(playerName);
        }

        // remove their timers
        SocialNetworkPlugin.getTimerManager().removeTimers(playerPerson.getName());

        // remove their wait command
        playerPerson.waitOn(WaitState.notWaiting, null);

        // remove the person from the network
        SocialNetworkPlugin.getStorage().removePerson(playerPerson);

        // tell the admin they got reset
        MessageUtil.sendMessage(sender, "social.admin.remove.completed.sender", ChatColor.GREEN);
    }

    private void handleReload(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            PlayerNotInNetworkException {

        // reload the configurations
        SocialNetworkPlugin.getSettings().reloadConfig();
        SocialNetworkPlugin.getResources().reloadConfig();

        // tell the admin they got reset
        MessageUtil.sendMessage(sender, "social.admin.reload.completed.sender", ChatColor.GREEN);
    }

    private void handlePriest(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            PlayerNotInNetworkException {

        if (arguments.size() != 1) {
            throw new ArgumentsMissingException();
        }

        if (arguments.get(0).equals("list")) {

            // send a header
            MessageUtil.sendHeaderMessage(sender, "social.admin.priest.list.header.sender");

            // send a response for each person with their message count
            List<String> priestPlayers = SocialNetworkPlugin.getStorage().getPriests();
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
            SocialPerson playerPerson = SocialNetworkPlugin.getStorage().getPerson(playerName);
            if (playerPerson == null) {
                throw new PlayerNotInNetworkException(playerName);
            }

            // check to see if they're already a priest
            if (SocialNetworkPlugin.getStorage().hasPriest(playerPerson)) {

                // toggle them off
                playerPerson.setPriest(false);
                SocialNetworkPlugin.getStorage().removePriest(playerPerson);

                // tell the admin they got switched
                MessageUtil.sendMessage(sender, "social.admin.priest.remove.completed.sender", ChatColor.GREEN,
                        new MessageParameter("<player>", playerPerson.getName(), ChatColor.AQUA));

            } else {

                // toggle them on
                playerPerson.setPriest(true);
                SocialNetworkPlugin.getStorage().addPriest(playerPerson);

                // tell the admin they got switched
                MessageUtil.sendMessage(sender, "social.admin.priest.add.completed.sender", ChatColor.GREEN,
                        new MessageParameter("<player>", playerPerson.getName(), ChatColor.AQUA));
            }

            // save the changes
            SocialNetworkPlugin.getStorage().savePerson(playerPerson);
        }
    }

    private void handleLawyer(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            PlayerNotInNetworkException {

        if (arguments.size() != 1) {
            throw new ArgumentsMissingException();
        }

        if (arguments.get(0).equals("list")) {

            // send a header
            MessageUtil.sendHeaderMessage(sender, "social.admin.lawyer.list.header.sender");

            // send a response for each person with their message count
            List<String> lawyerPlayers = SocialNetworkPlugin.getStorage().getLawyers();
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
            SocialPerson playerPerson = SocialNetworkPlugin.getStorage().getPerson(playerName);
            if (playerPerson == null) {
                throw new PlayerNotInNetworkException(playerName);
            }

            // check to see if they're already a lawyer
            if (SocialNetworkPlugin.getStorage().hasLawyer(playerPerson)) {

                // toggle them off
                playerPerson.setLawyer(false);
                SocialNetworkPlugin.getStorage().removeLawyer(playerPerson);

                // tell the admin they got switched
                MessageUtil.sendMessage(sender, "social.admin.lawyer.remove.completed.sender", ChatColor.GREEN,
                        new MessageParameter("<player>", playerPerson.getName(), ChatColor.AQUA));

            } else {

                // toggle them on
                playerPerson.setLawyer(true);
                SocialNetworkPlugin.getStorage().addLawyer(playerPerson);

                // tell the admin they got switched
                MessageUtil.sendMessage(sender, "social.admin.lawyer.add.completed.sender", ChatColor.GREEN,
                        new MessageParameter("<player>", playerPerson.getName(), ChatColor.AQUA));
            }

            // save the changes
            SocialNetworkPlugin.getStorage().savePerson(playerPerson);
        }
    }

    @Override
    public HelpSegment help() {

        HelpSegment helpSegment = new HelpSegment(getCommandType());
        ResourcesConfig config = SocialNetworkPlugin.getResources();

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

        HelpMessage genderListCommand = new HelpMessage();
        genderListCommand.setCommand(getCommandType().toString());
        genderListCommand.setArguments("gender <player> <male|female>");
        genderListCommand.setDescription(config.getResource("social.admin.gender.help"));
        helpSegment.addEntry(genderListCommand);

        HelpMessage clearTimerListCommand = new HelpMessage();
        clearTimerListCommand.setCommand(getCommandType().toString());
        clearTimerListCommand.setArguments("clear <player>");
        clearTimerListCommand.setDescription(config.getResource("social.admin.clear.help"));
        helpSegment.addEntry(clearTimerListCommand);

        HelpMessage removeListCommand = new HelpMessage();
        removeListCommand.setCommand(getCommandType().toString());
        removeListCommand.setArguments("remove <player>");
        removeListCommand.setDescription(config.getResource("social.admin.remove.help"));
        helpSegment.addEntry(removeListCommand);

        HelpMessage reloadCommand = new HelpMessage();
        reloadCommand.setCommand(getCommandType().toString());
        reloadCommand.setArguments("reload");
        reloadCommand.setDescription(config.getResource("social.admin.reload.help"));
        helpSegment.addEntry(reloadCommand);

        HelpMessage purgeCommand = new HelpMessage();
        purgeCommand.setCommand(getCommandType().toString());
        purgeCommand.setArguments("purge <days>");
        purgeCommand.setDescription(config.getResource("social.admin.purge.help"));
        helpSegment.addEntry(purgeCommand);

        return helpSegment;
    }
}
