package com.netprogs.minecraft.plugins.social.command.social;

import java.util.List;

import com.mysql.jdbc.StringUtils;
import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommand;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommandType;
import com.netprogs.minecraft.plugins.social.command.exception.ArgumentsMissingException;
import com.netprogs.minecraft.plugins.social.command.exception.InvalidPermissionsException;
import com.netprogs.minecraft.plugins.social.command.exception.PlayerNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotPlayerException;
import com.netprogs.minecraft.plugins.social.command.help.HelpBook;
import com.netprogs.minecraft.plugins.social.command.help.HelpMessage;
import com.netprogs.minecraft.plugins.social.command.help.HelpSegment;
import com.netprogs.minecraft.plugins.social.command.util.MessageParameter;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.command.util.PlayerUtil;
import com.netprogs.minecraft.plugins.social.command.util.TimerManager;
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

public class CommandProfile extends SocialNetworkCommand<ISocialNetworkSettings> {

    public CommandProfile() {
        super(SocialNetworkCommandType.profile);
    }

    @Override
    public boolean run(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            InvalidPermissionsException, SenderNotPlayerException, PlayerNotInNetworkException {

        // verify that the sender is actually a player
        verifySenderAsPlayer(sender);

        // check permissions
        if (!hasCommandPermission(sender)) {
            throw new InvalidPermissionsException();
        }

        Player player = (Player) sender;

        // check arguments
        String profilePlayerName;

        if (arguments.size() != 1) {

            // if none provided, use yourself
            profilePlayerName = sender.getName();

        } else {

            // get from arguments
            profilePlayerName = arguments.get(0);
        }

        // make sure the player is in the network
        SocialPerson profilePerson = SocialNetworkPlugin.getStorage().getPerson(profilePlayerName);
        if (profilePerson == null) {
            throw new PlayerNotInNetworkException(profilePlayerName);
        }

        // Display the header
        MessageUtil.sendHeaderMessage(player, "social.profile.header");

        // Display name
        MessageUtil.sendMessage(player, "social.profile.label.name", ChatColor.GREEN, new MessageParameter("<player>",
                profilePerson.getName(), ChatColor.AQUA));

        // Display gender
        if (profilePerson.getGender() != null) {
            MessageUtil.sendMessage(player, "social.profile.label.gender", ChatColor.GREEN, new MessageParameter(
                    "<gender>", profilePerson.getGenderDisplay(), ChatColor.AQUA));
        }

        // Display the date they joined
        MessageUtil.sendMessage(player, "social.profile.label.joindate", ChatColor.GREEN, new MessageParameter(
                "<date>", TimerManager.formatDate(profilePerson.getDateJoined()), ChatColor.AQUA));

        // Display the date they last logged in
        long lastLogin = PlayerUtil.getPlayerLastPlayed(profilePerson.getName());
        MessageUtil.sendMessage(player, "social.profile.label.lastlogin", ChatColor.GREEN, new MessageParameter(
                "<date>", TimerManager.formatDate(lastLogin), ChatColor.AQUA));

        // Display the number of friends
        MessageUtil.sendMessage(player, "social.profile.label.group.friend", ChatColor.GREEN, new MessageParameter(
                "<count>", Integer.toString(profilePerson.getFriends().size()), ChatColor.AQUA));

        // Display the number of relationships
        MessageUtil.sendMessage(player, "social.profile.label.group.relationship", ChatColor.GREEN,
                new MessageParameter("<count>", Integer.toString(profilePerson.getRelationships().size()),
                        ChatColor.AQUA));

        // Display the number of children
        MessageUtil.sendMessage(player, "social.profile.label.group.child", ChatColor.GREEN, new MessageParameter(
                "<count>", Integer.toString(profilePerson.getChildren().size()), ChatColor.AQUA));

        // TODO: Add childOf

        // Hide affairs
        // Display the number of affairs
        // MessageUtil.sendMessage(player, "social.profile.label.group.affair", ChatColor.GREEN, new MessageParameter(
        // "<count>", Integer.toString(profilePerson.getFriends().size()), ChatColor.AQUA));

        // Get their parent, if any
        if (profilePerson.getChildOf() != null) {
            MessageUtil.sendMessage(player, "social.profile.label.group.childOf", ChatColor.GREEN,
                    new MessageParameter("<player>", profilePerson.getChildOf(), ChatColor.AQUA));
        }

        // Display Engagement
        if (profilePerson.getEngagement() != null) {
            MessageUtil.sendMessage(player, "social.profile.label.group.engagement", ChatColor.GREEN,
                    new MessageParameter("<player>", profilePerson.getEngagement().getPlayerName(), ChatColor.AQUA));
        }

        // Display Marriage
        if (profilePerson.getMarriage() != null) {
            MessageUtil.sendMessage(player, "social.profile.label.group.marriage", ChatColor.GREEN,
                    new MessageParameter("<player>", profilePerson.getMarriage().getPlayerName(), ChatColor.AQUA));
        }

        // Display Divorce
        if (profilePerson.getDivorce() != null) {
            MessageUtil.sendMessage(player, "social.profile.label.group.divorce", ChatColor.GREEN,
                    new MessageParameter("<player>", profilePerson.getDivorce().getPlayerName(), ChatColor.AQUA));
        }

        // Display their status message
        String statusMessage = profilePerson.getStatusMessage();
        if (!StringUtils.isEmptyOrWhitespaceOnly(statusMessage)) {
            MessageUtil.sendMessage(player, "social.profile.label.status", ChatColor.GREEN, new MessageParameter(
                    "<message>", statusMessage, ChatColor.AQUA));
        }

        // returning false so that we don't so any post-command processing
        return false;
    }

    @Override
    public HelpSegment help() {

        ResourcesConfig config = SocialNetworkPlugin.getResources();
        HelpSegment helpSegment = new HelpSegment(getCommandType());

        HelpMessage mainCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), null, "[player]",
                        config.getResource("social.profile.help"));
        helpSegment.addEntry(mainCommand);

        return helpSegment;
    }
}
