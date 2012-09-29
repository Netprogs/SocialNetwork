package com.netprogs.minecraft.plugins.social.command.social;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommand;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommandType;
import com.netprogs.minecraft.plugins.social.command.exception.ArgumentsMissingException;
import com.netprogs.minecraft.plugins.social.command.exception.InvalidPermissionsException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotPlayerException;
import com.netprogs.minecraft.plugins.social.command.help.HelpBook;
import com.netprogs.minecraft.plugins.social.command.help.HelpMessage;
import com.netprogs.minecraft.plugins.social.command.help.HelpSegment;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.ISocialNetworkSettings;
import com.netprogs.minecraft.plugins.social.storage.SocialNetworkStorage;
import com.netprogs.minecraft.plugins.social.storage.data.Request;

import org.apache.commons.lang.StringUtils;
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

    private class RequestsView {

        private String requestTag;
        private List<String> playerNames = new ArrayList<String>();

        public RequestsView(String requestTag) {

            this.requestTag = requestTag;
        }

        public void addPlayer(String playerName) {
            playerNames.add(playerName);
        }

        public String toString() {

            StringBuffer stringBuffer = new StringBuffer();

            if (playerNames.size() > 0) {

                stringBuffer.append(ChatColor.AQUA);
                stringBuffer.append(requestTag);
                stringBuffer.append(": ");

                String firstPlayerName = playerNames.remove(0);
                stringBuffer.append(getPlayerNameDisplay(firstPlayerName));

                for (String playerName : playerNames) {
                    stringBuffer.append(", ");
                    stringBuffer.append(getPlayerNameDisplay(playerName));
                }
            }

            return stringBuffer.toString();
        }

        private String getPlayerNameDisplay(String playerName) {

            // check to see if they are online
            Player friendPlayer = getPlayer(playerName);
            if (friendPlayer != null) {
                return ChatColor.GREEN + playerName;
            } else {
                return ChatColor.GRAY + playerName;
            }
        }
    }

    public CommandRequests() {
        super(SocialNetworkCommandType.requests);
    }

    @Override
    public boolean run(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            InvalidPermissionsException, SenderNotPlayerException, SenderNotInNetworkException {

        // verify that the sender is actually a player
        verifySenderAsPlayer(sender);

        // check arguments
        if (arguments.size() > 1) {
            throw new ArgumentsMissingException();
        }

        // check permissions
        if (!hasCommandPermission(sender)) {
            throw new InvalidPermissionsException();
        }

        // get the social network data
        Player player = (Player) sender;
        SocialNetworkStorage socialConfig = SocialNetworkPlugin.getStorage();

        // check to see if the sender is part of the network
        SocialPerson person = socialConfig.getPerson(player.getName());
        if (person == null) {
            throw new SenderNotInNetworkException();
        }

        Map<String, List<Request>> requestMessages = person.getMessages(Request.class);

        // create the view objects
        ResourcesConfig resources = SocialNetworkPlugin.getResources();
        List<RequestsView> playerRequests = new ArrayList<RequestsView>();

        RequestsView friendView = new RequestsView(resources.getResource("social.requests.tag.friend.sender"));
        playerRequests.add(friendView);

        RequestsView relationView = new RequestsView(resources.getResource("social.requests.tag.relationship.sender"));
        playerRequests.add(relationView);

        RequestsView affairView = new RequestsView(resources.getResource("social.requests.tag.affair.sender"));
        playerRequests.add(affairView);

        RequestsView engagementView = new RequestsView(resources.getResource("social.requests.tag.engagement.sender"));
        playerRequests.add(engagementView);

        RequestsView marriageView = new RequestsView(resources.getResource("social.requests.tag.marriage.sender"));
        playerRequests.add(marriageView);

        RequestsView childView = new RequestsView(resources.getResource("social.requests.tag.child.sender"));
        playerRequests.add(childView);

        RequestsView divorceView = new RequestsView(resources.getResource("social.requests.tag.divorce.sender"));
        playerRequests.add(divorceView);

        // group them by type
        for (String playerName : requestMessages.keySet()) {
            List<Request> requests = requestMessages.get(playerName);
            for (Request request : requests) {
                if (request.getCommandType() == SocialNetworkCommandType.relationship) {
                    relationView.addPlayer(playerName);
                } else if (request.getCommandType() == SocialNetworkCommandType.friend) {
                    friendView.addPlayer(playerName);
                } else if (request.getCommandType() == SocialNetworkCommandType.engagement) {
                    engagementView.addPlayer(playerName);
                } else if (request.getCommandType() == SocialNetworkCommandType.affair) {
                    affairView.addPlayer(playerName);
                } else if (request.getCommandType() == SocialNetworkCommandType.marriage) {
                    marriageView.addPlayer(playerName);
                } else if (request.getCommandType() == SocialNetworkCommandType.child) {
                    childView.addPlayer(playerName);
                } else if (request.getCommandType() == SocialNetworkCommandType.divorce) {
                    divorceView.addPlayer(playerName);
                }
            }
        }

        // display the header
        MessageUtil.sendHeaderMessage(sender, "social.requests.header.sender");

        if (playerRequests.size() == 0) {

            MessageUtil.sendMessage(sender, "social.requests.noneAvailable.sender", ChatColor.RED);

        } else {

            // grab the sub list for displaying
            for (RequestsView item : playerRequests) {
                String displayView = item.toString();
                if (StringUtils.isNotEmpty(displayView)) {
                    player.sendMessage(displayView);
                }
            }
        }

        // send a footer
        MessageUtil.sendFooterLinesOnly(sender);

        return true;
    }

    @Override
    public HelpSegment help() {

        ResourcesConfig resources = SocialNetworkPlugin.getResources();
        HelpSegment helpSegment = new HelpSegment(getCommandType());

        HelpMessage mainCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), null, null,
                        resources.getResource("social.requests.help"));
        helpSegment.addEntry(mainCommand);

        return helpSegment;
    }
}
