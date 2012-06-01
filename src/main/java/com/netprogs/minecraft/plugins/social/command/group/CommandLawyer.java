package com.netprogs.minecraft.plugins.social.command.group;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.SocialPerson.Status;
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
import com.netprogs.minecraft.plugins.social.command.util.TimerUtil;
import com.netprogs.minecraft.plugins.social.config.PluginConfig;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.group.LawyerSettings;
import com.netprogs.minecraft.plugins.social.integration.VaultIntegration;
import com.netprogs.minecraft.plugins.social.storage.SocialNetwork;

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

public class CommandLawyer extends SocialNetworkCommand<LawyerSettings> {

    private final Logger logger = Logger.getLogger("Minecraft");

    public CommandLawyer() {
        super(SocialNetworkCommandType.lawyer);
    }

    @Override
    public boolean run(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            InvalidPermissionsException, SenderNotPlayerException, PlayerNotInNetworkException,
            PlayerNotOnlineException {

        // attempt to run the secondary commands first, but only players can respond to a priest
        boolean commandHandled = false;
        if (sender instanceof Player) {

            Player player = (Player) sender;

            // players can respond to requests from this command without having permissions to use it
            commandHandled = handleSecondaryCommands(player, arguments);

            // since we have a player, if the command wasn't handled, let's check to make sure they're a priest
            if (!commandHandled) {
                SocialPerson playerPerson = SocialNetwork.getInstance().getPerson(player.getName());
                if (playerPerson != null && !playerPerson.isPriest()) {

                    // If they didn't have priest assigned to them, check the permissions to see if they have it there
                    if (!hasCommandPermission(sender)) {
                        throw new InvalidPermissionsException();
                    }
                }
            }
        }

        // there were none, so lets try the main priest request command
        if (!commandHandled) {

            // check arguments
            if (arguments.size() != 2) {
                throw new ArgumentsMissingException();
            }

            // check to see if we have a player, if they are, check to make sure they're a lawyer
            if (sender instanceof Player) {

                Player player = (Player) sender;
                SocialPerson playerPerson = SocialNetwork.getInstance().getPerson(player.getName());

                // since we have a player, let's check to make sure they're a lawyer
                if (playerPerson != null && !playerPerson.isLawyer()) {

                    // If they didn't have lawyer assigned to them, check the permissions to see if they have it there
                    if (!hasCommandPermission(sender)) {
                        throw new InvalidPermissionsException();
                    }
                }
            }

            // get the command settings
            LawyerSettings settings = getCommandSettings();

            // make sure the A player is in the network
            String aPlayerName = arguments.get(0);
            SocialPerson aPlayerPerson = SocialNetwork.getInstance().getPerson(aPlayerName);
            if (aPlayerPerson == null) {
                throw new PlayerNotInNetworkException(aPlayerName);
            }

            // make sure the B player is in the network
            String bPlayerName = arguments.get(1);
            SocialPerson bPlayerPerson = SocialNetwork.getInstance().getPerson(bPlayerName);
            if (bPlayerPerson == null) {
                throw new PlayerNotInNetworkException(bPlayerName);
            }

            Player aPlayer = getPlayer(aPlayerPerson.getName());
            Player bPlayer = getPlayer(bPlayerPerson.getName());

            if (aPlayer != null && bPlayer != null) {

                // check to see if the couple can afford this request
                double price = settings.getPerUseCost();
                boolean authorized = true;
                authorized &= VaultIntegration.getInstance().preAuthCommandPurchase(aPlayer, price);
                authorized &= VaultIntegration.getInstance().preAuthCommandPurchase(bPlayer, price);
                if (!authorized) {
                    MessageUtil.sendMessage(sender, "social.lawyer.request.coupleCannotAfford.sender", ChatColor.GOLD);
                    return false;
                }

                // Married couples remain on a "honeymoon" phase for a period of time.
                // During this time they're not allowed to get divorced. Check for that now.
                long timeRemaining =
                        TimerUtil.commandOnTimer(aPlayerPerson.getName(), SocialNetworkCommandType.marriage);
                if (timeRemaining > 0) {

                    // tell the user how much time remains
                    Player player = Bukkit.getServer().getPlayer(aPlayerPerson.getName());
                    if (player != null) {
                        MessageUtil.sendMessage(sender, "social.lawyer.request.coupleOnTimer.sender", ChatColor.GOLD,
                                MessageUtil.createCoolDownFormatting(timeRemaining));
                    }

                    return false;
                }

                boolean marriedToPersonB = false;
                if (aPlayerPerson.getMarriage() != null) {

                    // check to see if they're engaged to the person they're asking to marry
                    marriedToPersonB = aPlayerPerson.getMarriage().getPlayerName().equalsIgnoreCase(bPlayer.getName());
                }

                // divorce can only be requested if the players are married to each other
                if (aPlayerPerson.getSocialStatus() != Status.married || !marriedToPersonB) {

                    MessageUtil.sendMessage(sender, "social.divorce.request.coupleNotMarried.sender", ChatColor.RED);
                    return false;
                }

                // create and set the divorce to the playerPerson
                aPlayerPerson.createDivorce(bPlayerPerson.getName());
                bPlayerPerson.createDivorce(aPlayerPerson.getName());

                // remove their marriage
                aPlayerPerson.breakMarriage();
                bPlayerPerson.breakMarriage();

                // change their status
                aPlayerPerson.setSocialStatus(Status.divorced);
                bPlayerPerson.setSocialStatus(Status.divorced);

                // save the changes to disk
                SocialNetwork.getInstance().savePerson(aPlayerPerson);
                SocialNetwork.getInstance().savePerson(bPlayerPerson);

                // check for a permissions update
                checkForPermissionsUpdate(aPlayerPerson);
                checkForPermissionsUpdate(bPlayerPerson);

                // update the timer for the divorce
                int timer = settings.getBitternessPeriod();
                TimerUtil.updateCommandTimer(aPlayerPerson.getName(), SocialNetworkCommandType.divorce, timer);
                TimerUtil.updateCommandTimer(bPlayerPerson.getName(), SocialNetworkCommandType.divorce, timer);

                // now charge for our services
                VaultIntegration.getInstance().processCommandPurchase(aPlayer, settings.getPerUseCost());
                VaultIntegration.getInstance().processCommandPurchase(bPlayer, settings.getPerUseCost());

                // send messages to the players
                MessageUtil.sendMessage(aPlayer, "social.lawyer.completed.player", ChatColor.GREEN,
                        new MessageParameter("<player>", bPlayerPerson.getName(), ChatColor.AQUA));

                MessageUtil.sendMessage(bPlayer, "social.lawyer.completed.player", ChatColor.GREEN,
                        new MessageParameter("<player>", aPlayerPerson.getName(), ChatColor.AQUA));

                // send message to the lawyer
                MessageParameter priestParam =
                        new MessageParameter("<playerA>", aPlayerPerson.getName(), ChatColor.AQUA);
                MessageParameter playerParam =
                        new MessageParameter("<playerB>", bPlayerPerson.getName(), ChatColor.AQUA);

                List<MessageParameter> messageVariables = new ArrayList<MessageParameter>();
                messageVariables.add(priestParam);
                messageVariables.add(playerParam);

                // and finally, we're done
                MessageUtil.sendMessage(sender, "social.lawyer.completed.sender", ChatColor.GREEN, messageVariables);

            } else {

                if (aPlayer == null) {
                    throw new PlayerNotOnlineException(aPlayerName);
                }

                if (bPlayer == null) {
                    throw new PlayerNotOnlineException(bPlayerName);
                }

                return false;
            }
        }

        return true;
    }

    private boolean handleSecondaryCommands(Player player, List<String> arguments) throws ArgumentsMissingException,
            PlayerNotInNetworkException {

        // need to make sure we have at least one argument (the command parameter)
        if (arguments.size() == 1) {

            // we only want to run this if the player is in the network
            SocialPerson playerPerson = SocialNetwork.getInstance().getPerson(player.getName());
            if (playerPerson != null) {

                if (arguments.get(0).equals("list")) {

                    // List all the available priests and if they're online
                    MessageUtil.sendHeaderMessage(player, "social.admin.lawyer.list.header.sender");

                    // check to see if they have anyone
                    List<String> lawyerPlayers = SocialNetwork.getInstance().getLawyers();
                    if (lawyerPlayers.size() == 0) {

                        MessageUtil.sendMessage(player, "social.lawyer.list.noPeople.sender", ChatColor.GREEN);

                    } else {

                        String onlineTag =
                                PluginConfig.getInstance().getConfig(ResourcesConfig.class)
                                        .getResource("social.list.tag.online.sender");

                        String offlineTag =
                                PluginConfig.getInstance().getConfig(ResourcesConfig.class)
                                        .getResource("social.list.tag.offline.sender");

                        String onlineList = ChatColor.GREEN + onlineTag + " " + ChatColor.WHITE;
                        String offlineList = ChatColor.GRAY + offlineTag + " " + ChatColor.WHITE;

                        // go through priest list and display their names and online status
                        for (String playerName : lawyerPlayers) {

                            // check to see if they are online
                            Player groupPlayer = getPlayer(playerName);
                            if (groupPlayer != null) {
                                onlineList += playerName + ", ";
                            } else {
                                offlineList += playerName + ", ";
                            }
                        }

                        if (onlineList.endsWith(", ")) {
                            onlineList = onlineList.substring(0, onlineList.length() - 2);
                        }
                        if (offlineList.endsWith(", ")) {
                            offlineList = offlineList.substring(0, offlineList.length() - 2);
                        }

                        player.sendMessage(onlineList);
                        player.sendMessage(offlineList);
                    }

                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public HelpSegment help() {

        HelpSegment helpSegment = new HelpSegment(getCommandType());
        ResourcesConfig config = PluginConfig.getInstance().getConfig(ResourcesConfig.class);

        HelpMessage mainCommand = new HelpMessage();
        mainCommand.setCommand(getCommandType().toString());
        mainCommand.setArguments("<playerA> <playerB>");
        mainCommand.setDescription(config.getResource("social.lawyer.help.request"));
        helpSegment.addEntry(mainCommand);

        HelpMessage listCommand = new HelpMessage();
        listCommand.setCommand(getCommandType().toString());
        listCommand.setArguments("list");
        listCommand.setDescription(config.getResource("social.lawyer.help.list"));
        helpSegment.addEntry(listCommand);

        HelpMessage acceptCommand = new HelpMessage();
        acceptCommand.setCommand(getCommandType().toString());
        acceptCommand.setArguments("accept");
        acceptCommand.setDescription(config.getResource("social.lawyer.help.accept"));
        helpSegment.addEntry(acceptCommand);

        HelpMessage rejectCommand = new HelpMessage();
        rejectCommand.setCommand(getCommandType().toString());
        rejectCommand.setArguments("reject");
        rejectCommand.setDescription(config.getResource("social.lawyer.help.reject"));
        helpSegment.addEntry(rejectCommand);

        return helpSegment;
    }
}
