package com.netprogs.minecraft.plugins.social.command.group;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.SocialPerson.Status;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommandType;
import com.netprogs.minecraft.plugins.social.command.help.HelpBook;
import com.netprogs.minecraft.plugins.social.command.help.HelpMessage;
import com.netprogs.minecraft.plugins.social.command.help.HelpSegment;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.group.AffairSettings;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

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

public class CommandAffair extends GroupCommand<AffairSettings> {

    public CommandAffair() {
        super(SocialNetworkCommandType.affair);
    }

    /**
     * Checks to see if playerPerson is allowed to send a request to receiverPerson.
     * Called from the sender of the request to see if they are allowed to send a request for this group.
     * @param playerPerson The person sending the request.
     * @param receiverPerson The person who is receiving the request.
     * @return
     */
    @Override
    protected boolean allowSendRequest(SocialPerson playerPerson, SocialPerson receiverPerson) {

        // Check to see if the sender can afford the affair.
        boolean success = checkCommandCost(playerPerson);
        if (!success) {
            MessageUtil.sendMessage(playerPerson, "social." + getCommandType() + ".cannotSendRequest.price.sender",
                    ChatColor.GOLD);
            return false;
        }

        // check to see if they've reached their maximum limit
        AffairSettings settings = getCommandSettings();
        if (settings.getMaximumAffairs() > 0 && playerPerson.getNumberAffairs() >= settings.getMaximumAffairs()) {
            MessageUtil.sendMessage(playerPerson, "social." + getCommandType() + ".cannotSendRequest.maximum.sender",
                    ChatColor.RED);
            return false;
        }

        // An affair can only be requested if the player is married or engaged
        if (playerPerson.getSocialStatus() != Status.engaged && playerPerson.getSocialStatus() != Status.married) {

            Player player = Bukkit.getServer().getPlayer(playerPerson.getName());
            if (player != null) {
                MessageUtil.sendMessage(player, "social." + getCommandType() + ".cannotSendRequest.sender",
                        ChatColor.RED);
            }

            return false;
        }

        return true;
    }

    /**
     * Checks to see if acceptPerson is still able to accept a request from the senderPerson.
     * Called when the request is received by the other person to determine if they are allowed to accept a request.
     * @param acceptPerson The person who is trying to accept the request.
     * @param senderPerson The person who sent them the request.
     * @return
     */
    @Override
    protected boolean allowAcceptRequest(SocialPerson acceptPerson, SocialPerson senderPerson) {

        // Check to see if the player can afford the affair.
        boolean success = checkCommandCost(acceptPerson);
        if (!success) {
            MessageUtil.sendMessage(acceptPerson, "social." + getCommandType() + ".cannotAcceptRequest.price.sender",
                    ChatColor.GOLD);
            return false;
        }

        // check to see if they've reached their maximum limit
        AffairSettings settings = getCommandSettings();
        if (settings.getMaximumAffairs() > 0 && acceptPerson.getNumberAffairs() >= settings.getMaximumAffairs()) {
            MessageUtil.sendMessage(acceptPerson, "social." + getCommandType() + ".cannotAcceptRequest.maximum.sender",
                    ChatColor.RED);
            return false;
        }

        // An affair can only be requested if the player is married or engaged
        if (acceptPerson.getSocialStatus() != Status.engaged || acceptPerson.getSocialStatus() != Status.married) {

            Player player = Bukkit.getServer().getPlayer(acceptPerson.getName());
            if (player != null) {
                MessageUtil.sendMessage(player, "social." + getCommandType() + ".cannotAcceptRequest.sender",
                        ChatColor.RED);
            }

            return false;
        }

        return true;
    }

    /**
     * Checks to see if inGroupPerson is within playerPerson's group already.
     * Called from the sender of the request to make sure the receiver isn't already in the group.
     * @param playerPerson
     * @param inGroupPerson
     * @return
     */
    @Override
    protected boolean personInGroup(SocialPerson playerPerson, SocialPerson inGroupPerson) {

        return playerPerson.isAffairWith(inGroupPerson);
    }

    @Override
    protected void addPersonToGroup(SocialPerson playerPerson, SocialPerson groupPerson) {

        // create and add a relationship to the playerPerson friend list
        playerPerson.addAffair(groupPerson);

        // check for a permissions update
        checkForPermissionsUpdate(playerPerson);

        // charge the user
        Player player = Bukkit.getServer().getPlayer(playerPerson.getName());
        if (player != null) {
            SocialNetworkPlugin.getVault().processCommandPurchase(player, getCommandSettings().getPerUseCost());
        }
    }

    /**
     * Removes the removePerson from the playerPerson's group.
     * @param playerPerson
     * @param removePerson
     */
    @Override
    protected void removePersonFromGroup(SocialPerson playerPerson, SocialPerson removePerson) {

        // remove them
        playerPerson.removeAffair(removePerson);

        // check for a permissions update
        checkForPermissionsUpdate(playerPerson);
    }

    /**
     * Handles the listing of a persons group.
     * @param player The bukkit player instance.
     * @param playerPerson The person who sent the request.
     */
    @Override
    protected void handleList(Player player, SocialPerson playerPerson) {

        // send back our member list
        displayGroupList(player, playerPerson.getAffairs());
    }

    /**
     * Provides the chance to display a help page to player who have been sent a request.
     * @param receiver The player to receive the help message.
     */
    @Override
    protected void displayRequestHelp(Player receiver) {

        ResourcesConfig config = SocialNetworkPlugin.getResources();

        HelpMessage acceptCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "accept", "<player>",
                        config.getResource("social.affair.help.accept"));
        MessageUtil.sendMessage(receiver, acceptCommand.display());

        HelpMessage rejectCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "reject", "<player>",
                        config.getResource("social.affair.help.reject"));
        MessageUtil.sendMessage(receiver, rejectCommand.display());

        HelpMessage ignoreCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "ignore", "<player>",
                        config.getResource("social.affair.help.ignore"));
        MessageUtil.sendMessage(receiver, ignoreCommand.display());
    }

    @Override
    public HelpSegment help() {

        HelpSegment helpSegment = new HelpSegment(getCommandType());

        ResourcesConfig config = SocialNetworkPlugin.getResources();

        HelpMessage mainCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "request", "<player>",
                        config.getResource("social.affair.help.request"));
        helpSegment.addEntry(mainCommand);

        HelpMessage acceptCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "accept", "<player>",
                        config.getResource("social.affair.help.accept"));
        helpSegment.addEntry(acceptCommand);

        HelpMessage acceptAllCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "acceptall", null,
                        config.getResource("social.affair.help.acceptall"));
        helpSegment.addEntry(acceptAllCommand);

        HelpMessage rejectCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "reject", "<player>",
                        config.getResource("social.affair.help.reject"));
        helpSegment.addEntry(rejectCommand);

        HelpMessage rejectAllCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "rejectall", null,
                        config.getResource("social.affair.help.rejectall"));
        helpSegment.addEntry(rejectAllCommand);

        HelpMessage ignoreCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "ignore", "<player>",
                        config.getResource("social.affair.help.ignore"));
        helpSegment.addEntry(ignoreCommand);

        HelpMessage ignoreAllCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "ignoreall", null,
                        config.getResource("social.affair.help.ignoreall"));
        helpSegment.addEntry(ignoreAllCommand);

        HelpMessage removeCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "remove", "<player>",
                        config.getResource("social.affair.help.remove"));
        helpSegment.addEntry(removeCommand);

        HelpMessage listCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "list", null,
                        config.getResource("social.affair.help.list"));
        helpSegment.addEntry(listCommand);

        return helpSegment;
    }
}
