package com.netprogs.minecraft.plugins.social.command.group;

import java.util.List;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.SocialPerson.Status;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommandType;
import com.netprogs.minecraft.plugins.social.command.exception.ArgumentsMissingException;
import com.netprogs.minecraft.plugins.social.command.exception.InvalidPermissionsException;
import com.netprogs.minecraft.plugins.social.command.exception.PlayerNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.help.HelpBook;
import com.netprogs.minecraft.plugins.social.command.help.HelpMessage;
import com.netprogs.minecraft.plugins.social.command.help.HelpSegment;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.group.DivorceSettings;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

public class CommandDivorce extends GroupCommand<DivorceSettings> {

    public CommandDivorce() {
        super(SocialNetworkCommandType.divorce);
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

        Player player = Bukkit.getServer().getPlayer(playerPerson.getName());
        if (player == null) {
            return false;
        }

        boolean success = true;

        // Married couples remain on a "honeymoon" phase for a period of time.
        // During this time they're not allowed to get divorced. Check for that now.
        success = checkCommandTimer(playerPerson, SocialNetworkCommandType.marriage);
        if (!success) {
            return false;
        }

        // Also, check to see if they can afford the divorce.
        success = checkCommandCost(playerPerson);
        if (!success) {
            MessageUtil.sendMessage(playerPerson, "social." + getCommandType() + ".cannotSendRequest.price.sender",
                    ChatColor.GOLD);
            return false;
        }

        // check to see if they're married to the person they're asking to divorce
        boolean marriedToPerson = false;
        if (playerPerson.getMarriage() != null) {
            marriedToPerson = playerPerson.getMarriage().getPlayerName().equalsIgnoreCase(receiverPerson.getName());
        }

        // divorce can only be requested if the player IS married
        if (playerPerson.getSocialStatus() != Status.married || !marriedToPerson) {
            MessageUtil.sendMessage(player, "social." + getCommandType() + ".cannotSendRequest.sender", ChatColor.RED);
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

        boolean success = true;

        // Married couples remain on a "honeymoon" phase for a period of time.
        // During this time they're not allowed to get divorced. Check for that now.
        success = checkCommandTimer(acceptPerson, SocialNetworkCommandType.marriage);
        if (!success) {
            return false;
        }

        // Also, check to see if they can afford the divorce.
        success = checkCommandCost(acceptPerson);
        if (!success) {
            MessageUtil.sendMessage(acceptPerson, "social." + getCommandType() + ".cannotAcceptRequest.price.sender",
                    ChatColor.GOLD);
            return false;
        }

        // check to see if they're married to the person they're asking to divorce
        boolean marriedToPerson = false;
        if (acceptPerson.getMarriage() != null) {
            marriedToPerson = acceptPerson.getMarriage().getPlayerName().equalsIgnoreCase(senderPerson.getName());
        }

        // divorce can only be accepted if the player is married
        if (acceptPerson.getSocialStatus() != Status.married || !marriedToPerson) {
            MessageUtil.sendMessage(acceptPerson, "social." + getCommandType() + ".cannotAcceptRequest.sender",
                    ChatColor.RED);
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

        return playerPerson.isDivorcedFrom(inGroupPerson);
    }

    /**
     * Adds the addPerson to the playerPerson's group.
     * @param playerPerson
     * @param addPerson
     */
    @Override
    protected void addPersonToGroup(SocialPerson playerPerson, SocialPerson addPerson) {

        // create and set the divorce to the playerPerson
        playerPerson.createDivorce(addPerson);

        // remove their marriage
        playerPerson.breakMarriage();

        // change their status
        playerPerson.setSocialStatus(Status.divorced);

        // check for a permissions update
        checkForPermissionsUpdate(playerPerson);

        // get the command settings
        DivorceSettings settings = getCommandSettings();
        int timer = settings.getBitternessPeriod();

        // update the timer for the command
        SocialNetworkPlugin.getTimerManager().updateCommandTimer(playerPerson.getName(),
                SocialNetworkCommandType.divorce, timer);

        // Charge the player for the cost of the divorce.
        // Ignore if they can't afford it (we already checked earlier)
        Player player = Bukkit.getServer().getPlayer(playerPerson.getName());
        if (player != null) {
            SocialNetworkPlugin.getVault().processCommandPurchase(player, settings.getPerUseCost());
        }
    }

    /**
     * Removes the removePerson from the playerPerson's group.
     * @param playerPerson
     * @param removePerson
     */
    @Override
    protected void removePersonFromGroup(SocialPerson playerPerson, SocialPerson removePerson) {

        // Divorce doesn't have a remove. See handleSecondaryCommands() below.
    }

    /**
     * We're overriding this method in order to reduce the scope of secondary commands.
     */
    @Override
    protected boolean handleSecondaryCommands(Player player, SocialPerson playerPerson, List<String> arguments)
            throws ArgumentsMissingException, InvalidPermissionsException, PlayerNotInNetworkException {

        if (arguments.get(0).equals("accept")) {
            handleAccept(player, playerPerson, arguments);
            return true;

        } else if (arguments.get(0).equals("reject")) {
            handleReject(player, playerPerson, arguments, false);
            return true;
        }

        return false;
    }

    /**
     * Handles the listing of a persons group.
     * @param player The bukkit player instance.
     * @param playerPerson The person who sent the request.
     */
    @Override
    protected void handleList(Player player, SocialPerson playerPerson) {

        // Divorce doesn't support list
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
                        config.getResource("social.divorce.help.accept"));
        MessageUtil.sendMessage(receiver, acceptCommand.display());

        HelpMessage rejectCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "reject", "<player>",
                        config.getResource("social.divorce.help.reject"));
        MessageUtil.sendMessage(receiver, rejectCommand.display());
    }

    @Override
    public HelpSegment help() {

        HelpSegment helpSegment = new HelpSegment(getCommandType());
        ResourcesConfig config = SocialNetworkPlugin.getResources();

        HelpMessage mainCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "request", "<player>",
                        config.getResource("social.divorce.help.request"));
        helpSegment.addEntry(mainCommand);

        HelpMessage acceptCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "accept", "<player>",
                        config.getResource("social.divorce.help.accept"));
        helpSegment.addEntry(acceptCommand);

        HelpMessage rejectCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "reject", "<player>",
                        config.getResource("social.divorce.help.reject"));
        helpSegment.addEntry(rejectCommand);

        return helpSegment;
    }
}
