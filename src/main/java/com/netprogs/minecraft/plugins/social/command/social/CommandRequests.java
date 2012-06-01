package com.netprogs.minecraft.plugins.social.command.social;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommand;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommandType;
import com.netprogs.minecraft.plugins.social.command.exception.ArgumentsMissingException;
import com.netprogs.minecraft.plugins.social.command.exception.InvalidPermissionsException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotPlayerException;
import com.netprogs.minecraft.plugins.social.command.help.HelpMessage;
import com.netprogs.minecraft.plugins.social.command.help.HelpSegment;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.config.PluginConfig;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.ISocialNetworkSettings;
import com.netprogs.minecraft.plugins.social.storage.SocialNetwork;
import com.netprogs.minecraft.plugins.social.storage.data.Request;

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

public class CommandRequests extends SocialNetworkCommand<ISocialNetworkSettings> {

    private final Logger logger = Logger.getLogger("Minecraft");

    public CommandRequests() {
        super(SocialNetworkCommandType.requests);
    }

    @Override
    public boolean run(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            InvalidPermissionsException, SenderNotPlayerException, SenderNotInNetworkException {

        // verify that the sender is actually a player
        verifySenderAsPlayer(sender);

        // check arguments
        if (arguments.size() != 0) {
            throw new ArgumentsMissingException();
        }

        // check permissions
        if (!hasCommandPermission(sender)) {
            throw new InvalidPermissionsException();
        }

        Player player = (Player) sender;

        // get the social network data
        SocialNetwork socialConfig = SocialNetwork.getInstance();

        // check to see if the sender is part of the network
        SocialPerson person = socialConfig.getPerson(player.getName());
        if (person == null) {
            throw new SenderNotInNetworkException();
        }

        // group them by type
        List<Request> friendRequests = new ArrayList<Request>();
        List<Request> relationshipRequests = new ArrayList<Request>();
        List<Request> engagementRequests = new ArrayList<Request>();
        List<Request> affairRequests = new ArrayList<Request>();
        List<Request> marriageRequests = new ArrayList<Request>();
        List<Request> childRequests = new ArrayList<Request>();
        List<Request> divorseRequests = new ArrayList<Request>();
        List<Request> priestAdminRequests = new ArrayList<Request>();
        List<Request> lawyerAdminRequests = new ArrayList<Request>();

        // get the list of requests from each player
        for (String key : person.getMessagePlayers(Request.class)) {
            for (Request request : person.getMessagesFrom(key, Request.class)) {
                if (request.getCommandType() == SocialNetworkCommandType.relationship) {
                    relationshipRequests.add(request);
                } else if (request.getCommandType() == SocialNetworkCommandType.friend) {
                    friendRequests.add(request);
                } else if (request.getCommandType() == SocialNetworkCommandType.engagement) {
                    engagementRequests.add(request);
                } else if (request.getCommandType() == SocialNetworkCommandType.affair) {
                    affairRequests.add(request);
                } else if (request.getCommandType() == SocialNetworkCommandType.marriage) {
                    marriageRequests.add(request);
                } else if (request.getCommandType() == SocialNetworkCommandType.child) {
                    childRequests.add(request);
                } else if (request.getCommandType() == SocialNetworkCommandType.divorce) {
                    divorseRequests.add(request);
                }
            }
        }

        // check to see if any of the lists got something added
        boolean hasRequests =
                (friendRequests.size() > 0) || (relationshipRequests.size() > 0) || (engagementRequests.size() > 0)
                        || (affairRequests.size() > 0) || (marriageRequests.size() > 0) || (childRequests.size() > 0)
                        || (divorseRequests.size() > 0);

        // send the header
        MessageUtil.sendHeaderMessage(sender, "social.requests.header.sender");

        // we don't have anything, let the player know
        if (!hasRequests) {
            MessageUtil.sendMessage(sender, "social.requests.noneAvailable.sender", ChatColor.RED);
            return false;
        }

        // still here, time to display what we have
        String friendTag =
                PluginConfig.getInstance().getConfig(ResourcesConfig.class)
                        .getResource("social.requests.tag.friend.sender");

        String relationshipTag =
                PluginConfig.getInstance().getConfig(ResourcesConfig.class)
                        .getResource("social.requests.tag.relationship.sender");

        String affairTag =
                PluginConfig.getInstance().getConfig(ResourcesConfig.class)
                        .getResource("social.requests.tag.affair.sender");

        String engagementTag =
                PluginConfig.getInstance().getConfig(ResourcesConfig.class)
                        .getResource("social.requests.tag.engagement.sender");

        String marriageTag =
                PluginConfig.getInstance().getConfig(ResourcesConfig.class)
                        .getResource("social.requests.tag.marriage.sender");

        String childTag =
                PluginConfig.getInstance().getConfig(ResourcesConfig.class)
                        .getResource("social.requests.tag.child.sender");

        String divorseTag =
                PluginConfig.getInstance().getConfig(ResourcesConfig.class)
                        .getResource("social.requests.tag.divorce.sender");

        String priestTag =
                PluginConfig.getInstance().getConfig(ResourcesConfig.class)
                        .getResource("social.requests.tag.priestadmin.sender");

        String lawyerTag =
                PluginConfig.getInstance().getConfig(ResourcesConfig.class)
                        .getResource("social.requests.tag.lawyeradmin.sender");

        // okay, now we'll display them
        displayRequests(player, friendTag, friendRequests);
        displayRequests(player, relationshipTag, relationshipRequests);
        displayRequests(player, engagementTag, engagementRequests);
        displayRequests(player, affairTag, affairRequests);
        displayRequests(player, marriageTag, marriageRequests);
        displayRequests(player, childTag, childRequests);
        displayRequests(player, divorseTag, divorseRequests);
        displayRequests(player, priestTag, priestAdminRequests);
        displayRequests(player, lawyerTag, lawyerAdminRequests);

        return true;
    }

    private void displayRequests(Player player, String prefix, List<Request> requests) {

        for (Request request : requests) {

            String displayRequest = "";

            // check to see if they are online
            Player friendPlayer = getPlayer(request.getPlayerName());
            if (friendPlayer != null) {
                displayRequest = ChatColor.GREEN + prefix;
            } else {
                displayRequest = ChatColor.GRAY + prefix;
            }

            displayRequest += ChatColor.WHITE + " " + request.getPlayerName();
            player.sendMessage(displayRequest);
        }
    }

    @Override
    public HelpSegment help() {

        ResourcesConfig config = PluginConfig.getInstance().getConfig(ResourcesConfig.class);

        HelpMessage mainCommand = new HelpMessage();
        mainCommand.setCommand(getCommandType().toString());
        mainCommand.setDescription(config.getResource("social.requests.help"));

        HelpSegment helpSegment = new HelpSegment(getCommandType());
        helpSegment.addEntry(mainCommand);

        return helpSegment;
    }
}
