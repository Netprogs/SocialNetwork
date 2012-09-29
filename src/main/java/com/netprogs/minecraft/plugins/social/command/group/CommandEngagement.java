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
import com.netprogs.minecraft.plugins.social.config.settings.group.EngagementSettings;

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

public class CommandEngagement extends GroupCommand<EngagementSettings> {

    public CommandEngagement() {
        super(SocialNetworkCommandType.engagement);
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

        // Check to see if they allow same gender Marriage, which means the engagement won't be allowed
        if (!SocialNetworkPlugin.getSettings().isSameGenderMarriageAllowed()) {
            if (playerPerson.getGender() == receiverPerson.getGender()) {
                MessageUtil.sendMessage(playerPerson, "social.error.sameGenderDisabled.sender", ChatColor.RED);
                return false;
            }
        }

        // check to see if they're a child or parent of the person they're asking to marry
        if (playerPerson.isChildOf(receiverPerson) || playerPerson.isParentOf(receiverPerson)) {
            MessageUtil.sendMessage(playerPerson, "social." + getCommandType()
                    + ".cannotSendRequest.childParent.sender", ChatColor.GOLD);
            return false;
        }

        // Divorced couples remain on a "bitterness" phase for a period of time.
        // During this time they're not allowed to get engaged again. Check for that now.
        success = checkCommandTimer(playerPerson, SocialNetworkCommandType.divorce);
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

        // if they were divorced, now we can clear them since the bitterness is over
        if (playerPerson.getSocialStatus() == Status.divorced) {

            // remove the divorce
            playerPerson.endDivorce();

            // set them back to single for now
            playerPerson.setSocialStatus(Status.single);
        }

        // engagement can only be requested if the player is NOT married or already engaged
        if (playerPerson.getSocialStatus() == Status.engaged || playerPerson.getSocialStatus() == Status.married) {
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

        // Divorced couples remain on a "bitterness" phase for a period of time.
        // During this time they're not allowed to get engaged again. Check for that now.
        success = checkCommandTimer(acceptPerson, SocialNetworkCommandType.divorce);
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

        // engagement can only be requested if the player is NOT married or already engaged
        if (acceptPerson.getSocialStatus() == Status.engaged || acceptPerson.getSocialStatus() == Status.married) {
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

        return playerPerson.isEngagedTo(inGroupPerson);
    }

    /**
     * Adds the addPerson to the playerPerson's group.
     * @param playerPerson
     * @param addPerson
     */
    @Override
    protected void addPersonToGroup(SocialPerson playerPerson, SocialPerson addPerson) {

        // create and set the engagement to the playerPerson
        playerPerson.createEngagement(addPerson);

        // change their status
        playerPerson.setSocialStatus(Status.engaged);

        // check for a permissions update
        checkForPermissionsUpdate(playerPerson);

        // Charge the player for the cost of the engagement.
        // Ignore if they can't afford it (we already checked earlier)
        EngagementSettings settings = getCommandSettings();
        Player player = Bukkit.getServer().getPlayer(playerPerson.getName());
        if (player != null) {
            SocialNetworkPlugin.getVault().processCommandPurchase(player, settings.getPerUseCost());
        }

        // update the timer for the command
        int timer = settings.getEngagementPeriod();
        SocialNetworkPlugin.getTimerManager().updateCommandTimer(playerPerson.getName(),
                SocialNetworkCommandType.engagement, timer);
    }

    /**
     * Removes the removePerson from the playerPerson's group.
     * @param playerPerson
     * @param removePerson
     */
    @Override
    protected void removePersonFromGroup(SocialPerson playerPerson, SocialPerson removePerson) {

        // remove the engagement by setting it to NULL
        playerPerson.breakEngagement();

        // change their status
        playerPerson.setSocialStatus(Status.single);

        // check for a permissions update
        checkForPermissionsUpdate(playerPerson);
    }

    /**
     * We're overriding this method in order to reduce the scope of secondary commands.
     * Removing list.
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

        } else if (arguments.get(0).equals("ignore")) {

            handleReject(player, playerPerson, arguments, true);
            return true;

        } else if (arguments.get(0).equals("remove")) {

            handleRemove(player, playerPerson, arguments);
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

        // Engagement doesn't support list
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
                        config.getResource("social.engagement.help.accept"));
        MessageUtil.sendMessage(receiver, acceptCommand.display());

        HelpMessage rejectCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "reject", "<player>",
                        config.getResource("social.engagement.help.reject"));
        MessageUtil.sendMessage(receiver, rejectCommand.display());

        HelpMessage ignoreCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "ignore", "<player>",
                        config.getResource("social.engagement.help.ignore"));
        MessageUtil.sendMessage(receiver, ignoreCommand.display());
    }

    @Override
    public HelpSegment help() {

        HelpSegment helpSegment = new HelpSegment(getCommandType());
        ResourcesConfig config = SocialNetworkPlugin.getResources();

        HelpMessage mainCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "request", "<player>",
                        config.getResource("social.engagement.help.request"));
        helpSegment.addEntry(mainCommand);

        HelpMessage acceptCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "accept", "<player>",
                        config.getResource("social.engagement.help.accept"));
        helpSegment.addEntry(acceptCommand);

        HelpMessage rejectCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "reject", "<player>",
                        config.getResource("social.engagement.help.reject"));
        helpSegment.addEntry(rejectCommand);

        HelpMessage ignoreCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "ignore", "<player>",
                        config.getResource("social.engagement.help.ignore"));
        helpSegment.addEntry(ignoreCommand);

        HelpMessage removeCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "remove", "<player>",
                        config.getResource("social.engagement.help.remove"));
        helpSegment.addEntry(removeCommand);

        return helpSegment;
    }
}
