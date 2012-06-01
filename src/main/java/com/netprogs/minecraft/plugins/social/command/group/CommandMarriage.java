package com.netprogs.minecraft.plugins.social.command.group;

import java.util.List;
import java.util.logging.Logger;

import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.SocialPerson.Status;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommandType;
import com.netprogs.minecraft.plugins.social.command.exception.ArgumentsMissingException;
import com.netprogs.minecraft.plugins.social.command.exception.InvalidPermissionsException;
import com.netprogs.minecraft.plugins.social.command.exception.PlayerNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.help.HelpMessage;
import com.netprogs.minecraft.plugins.social.command.help.HelpSegment;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.command.util.TimerUtil;
import com.netprogs.minecraft.plugins.social.config.PluginConfig;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.group.MarriageSettings;
import com.netprogs.minecraft.plugins.social.integration.VaultIntegration;

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

public class CommandMarriage extends GroupCommand<MarriageSettings> {

    private final Logger logger = Logger.getLogger("Minecraft");

    public CommandMarriage() {
        super(SocialNetworkCommandType.marriage);
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

        // Check to see if the sender can afford the cost.
        boolean success = checkCommandCost(playerPerson);
        if (!success) {
            MessageUtil.sendMessage(playerPerson, "social." + getCommandType() + ".cannotSendRequest.price.sender",
                    ChatColor.GOLD);
            return false;
        }

        // check to see if they're engaged to the person they're asking to marry
        boolean engagedToPerson = false;
        if (playerPerson.getEngagement() != null) {
            engagedToPerson = playerPerson.getEngagement().getPlayerName().equalsIgnoreCase(receiverPerson.getName());
        }

        // marriage can only be requested if the player is engaged
        if (playerPerson.getSocialStatus() != Status.engaged || !engagedToPerson) {
            MessageUtil.sendMessage(player, "social." + getCommandType() + ".cannotSendRequest.sender", ChatColor.RED);
            return false;
        }

        // Engaged couples remain on an engagement phase for a period of time.
        // During this time they're not allowed to get married. Check for that now.
        success = checkCommandTimer(playerPerson, SocialNetworkCommandType.engagement);
        if (!success) {
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

        // Check to see if the player can afford the cost.
        boolean success = checkCommandCost(acceptPerson);
        if (!success) {
            MessageUtil.sendMessage(acceptPerson, "social." + getCommandType() + ".cannotAcceptRequest.price.sender",
                    ChatColor.GOLD);
            return false;
        }

        // check to see if they're engaged to the person they're asking to marry
        boolean engagedToPerson = acceptPerson.getEngagement().getPlayerName().equalsIgnoreCase(senderPerson.getName());

        // marriage can only be requested if the player is engaged
        if (acceptPerson.getSocialStatus() != Status.engaged || !engagedToPerson) {
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

        // if the marriage object is valid and it's player is the same as the groupPerson given, then we have a match
        if (playerPerson.getMarriage() != null
                && playerPerson.getMarriage().getPlayerName().equalsIgnoreCase(inGroupPerson.getName())) {
            return true;
        }

        return false;
    }

    /**
     * Adds the addPerson to the playerPerson's group.
     * @param playerPerson
     * @param addPerson
     */
    @Override
    protected void addPersonToGroup(SocialPerson playerPerson, SocialPerson addPerson) {

        // create and set the marriage to the playerPerson
        playerPerson.createMarriage(addPerson.getName());

        // remove their engagement
        playerPerson.breakEngagement();

        // change their status
        playerPerson.setSocialStatus(Status.married);

        // check for a permissions update
        checkForPermissionsUpdate(playerPerson);

        // get the command settings
        MarriageSettings settings = getCommandSettings();
        int timer = settings.getHoneymoonPeriod();

        // update the timer for the command
        TimerUtil.updateCommandTimer(playerPerson.getName(), SocialNetworkCommandType.marriage, timer);

        // Charge the player for the cost of the divorce.
        // Ignore if they can't afford it (we already checked earlier)
        Player player = Bukkit.getServer().getPlayer(playerPerson.getName());
        if (player != null) {
            VaultIntegration.getInstance().processCommandPurchase(player, settings.getPerUseCost());
        }
    }

    @Override
    protected void removePersonFromGroup(SocialPerson playerPerson, SocialPerson groupPerson) {

        // Marriage doesn't support remove. See handleSecondaryCommands() below.
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

        // Marriage doesn't support list
    }

    @Override
    public HelpSegment help() {

        HelpSegment helpSegment = new HelpSegment(getCommandType());
        ResourcesConfig config = PluginConfig.getInstance().getConfig(ResourcesConfig.class);

        HelpMessage mainCommand = new HelpMessage();
        mainCommand.setCommand(getCommandType().toString());
        mainCommand.setArguments("request <player>");
        mainCommand.setDescription(config.getResource("social.marriage.help.request"));
        helpSegment.addEntry(mainCommand);

        HelpMessage acceptCommand = new HelpMessage();
        acceptCommand.setCommand(getCommandType().toString());
        acceptCommand.setArguments("accept <player>");
        acceptCommand.setDescription(config.getResource("social.marriage.help.accept"));
        helpSegment.addEntry(acceptCommand);

        HelpMessage rejectCommand = new HelpMessage();
        rejectCommand.setCommand(getCommandType().toString());
        rejectCommand.setArguments("reject <player>");
        rejectCommand.setDescription(config.getResource("social.marriage.help.reject"));
        helpSegment.addEntry(rejectCommand);

        return helpSegment;
    }
}
