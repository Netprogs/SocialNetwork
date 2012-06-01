package com.netprogs.minecraft.plugins.social.command.group;

import java.util.logging.Logger;

import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.SocialPerson.Status;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommandType;
import com.netprogs.minecraft.plugins.social.command.help.HelpMessage;
import com.netprogs.minecraft.plugins.social.command.help.HelpSegment;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.config.PluginConfig;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.group.AffairSettings;
import com.netprogs.minecraft.plugins.social.integration.VaultIntegration;

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

    private final Logger logger = Logger.getLogger("Minecraft");

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

        return playerPerson.isAffairWith(inGroupPerson.getName());
    }

    @Override
    protected void addPersonToGroup(SocialPerson playerPerson, SocialPerson groupPerson) {

        // create and add a relationship to the playerPerson friend list
        playerPerson.addAffair(groupPerson.getName());

        // check for a permissions update
        checkForPermissionsUpdate(playerPerson);

        // charge the user
        Player player = Bukkit.getServer().getPlayer(playerPerson.getName());
        if (player != null) {
            VaultIntegration.getInstance().processCommandPurchase(player, getCommandSettings().getPerUseCost());
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
        playerPerson.removeAffair(removePerson.getName());

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

    @Override
    public HelpSegment help() {

        HelpSegment helpSegment = new HelpSegment(getCommandType());

        ResourcesConfig config = PluginConfig.getInstance().getConfig(ResourcesConfig.class);

        HelpMessage mainCommand = new HelpMessage();
        mainCommand.setCommand(getCommandType().toString());
        mainCommand.setArguments("request <player>");
        mainCommand.setDescription(config.getResource("social.affair.help.request"));
        helpSegment.addEntry(mainCommand);

        HelpMessage acceptCommand = new HelpMessage();
        acceptCommand.setCommand(getCommandType().toString());
        acceptCommand.setArguments("accept <player>");
        acceptCommand.setDescription(config.getResource("social.affair.help.accept"));
        helpSegment.addEntry(acceptCommand);

        HelpMessage rejectCommand = new HelpMessage();
        rejectCommand.setCommand(getCommandType().toString());
        rejectCommand.setArguments("reject <player>");
        rejectCommand.setDescription(config.getResource("social.affair.help.reject"));
        helpSegment.addEntry(rejectCommand);

        HelpMessage ignoreCommand = new HelpMessage();
        ignoreCommand.setCommand(getCommandType().toString());
        ignoreCommand.setArguments("ignore <player>");
        ignoreCommand.setDescription(config.getResource("social.affair.help.ignore"));
        helpSegment.addEntry(ignoreCommand);

        HelpMessage removeCommand = new HelpMessage();
        removeCommand.setCommand(getCommandType().toString());
        removeCommand.setArguments("remove <player>");
        removeCommand.setDescription(config.getResource("social.affair.help.remove"));
        helpSegment.addEntry(removeCommand);

        HelpMessage listCommand = new HelpMessage();
        listCommand.setCommand(getCommandType().toString());
        listCommand.setArguments("list");
        listCommand.setDescription(config.getResource("social.affair.help.list"));
        helpSegment.addEntry(listCommand);

        return helpSegment;
    }
}
