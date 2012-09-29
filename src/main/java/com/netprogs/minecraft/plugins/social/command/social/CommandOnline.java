package com.netprogs.minecraft.plugins.social.command.social;

import java.util.List;
import java.util.Map;

import com.netprogs.minecraft.plugins.social.SocialDivorce;
import com.netprogs.minecraft.plugins.social.SocialEngagement;
import com.netprogs.minecraft.plugins.social.SocialGroupMember;
import com.netprogs.minecraft.plugins.social.SocialMarriage;
import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommand;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommandType;
import com.netprogs.minecraft.plugins.social.command.exception.ArgumentsMissingException;
import com.netprogs.minecraft.plugins.social.command.exception.InvalidPermissionsException;
import com.netprogs.minecraft.plugins.social.command.exception.PlayerNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotPlayerException;
import com.netprogs.minecraft.plugins.social.command.help.HelpBook;
import com.netprogs.minecraft.plugins.social.command.help.HelpMessage;
import com.netprogs.minecraft.plugins.social.command.help.HelpSegment;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.ISocialNetworkSettings;
import com.netprogs.minecraft.plugins.social.storage.SocialNetworkStorage;

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

public class CommandOnline extends SocialNetworkCommand<ISocialNetworkSettings> {

    public CommandOnline() {
        super(SocialNetworkCommandType.online);
    }

    @Override
    public boolean run(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            InvalidPermissionsException, SenderNotPlayerException, PlayerNotInNetworkException,
            SenderNotInNetworkException {

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
        SocialNetworkStorage socialConfig = SocialNetworkPlugin.getStorage();

        // check to see if the player is in the network
        SocialPerson person = socialConfig.getPerson(player.getName());
        if (person == null) {
            throw new SenderNotInNetworkException();
        }

        // group them by type
        SocialEngagement engagement = person.getEngagement();
        SocialMarriage marriage = person.getMarriage();
        SocialDivorce divorce = person.getDivorce();
        String childOf = person.getChildOf();

        // check to see if any of the lists got something added
        boolean hasMembersOnline =
                (person.getNumberFriends() > 0) || (person.getNumberRelationships() > 0)
                        || (person.getNumberFriends() > 0) || (person.getNumberChildren() > 0);

        // send the header
        MessageUtil.sendHeaderMessage(sender, "social.online.header.sender");

        // we don't have anything, let the player know
        if (!hasMembersOnline) {
            MessageUtil.sendMessage(sender, "social.online.noneAvailable.sender", ChatColor.RED);
            return false;
        }

        ResourcesConfig resources = SocialNetworkPlugin.getResources();

        // Friends
        String friendTag = resources.getResource("social.online.tag.friend.sender");

        displayPlayers(player, friendTag, person.getFriends());

        // Relationships
        String relationshipTag = resources.getResource("social.online.tag.relationship.sender");

        displayPlayers(player, relationshipTag, person.getRelationships());

        // Affairs
        String affairTag = resources.getResource("social.online.tag.affair.sender");

        displayPlayers(player, affairTag, person.getAffairs());

        // Children
        String childTag = resources.getResource("social.online.tag.child.sender");

        displayPlayers(player, childTag, person.getChildren());

        // Display the person that is your parent
        String parentTag = resources.getResource("social.online.tag.parent.sender");

        if (childOf != null) {
            displayPlayer(player, parentTag, childOf);
        }

        // Display person engaged to
        String engagementTag = resources.getResource("social.online.tag.engagement.sender");

        if (engagement != null) {
            displayPlayer(player, engagementTag, engagement.getPlayerName());
        }

        // Display person married to
        String marriageTag = resources.getResource("social.online.tag.marriage.sender");

        if (marriage != null) {
            displayPlayer(player, marriageTag, marriage.getPlayerName());
        }

        // Display person divorced from
        String divorseTag = resources.getResource("social.online.tag.divorce.sender");

        if (divorce != null) {
            displayPlayer(player, divorseTag, divorce.getPlayerName());
        }

        return true;
    }

    private void displayPlayers(Player player, String groupPrefix, Map<String, ? extends SocialGroupMember> members) {

        String displayPlayers = "";
        if (members.size() > 0) {

            displayPlayers = ChatColor.GREEN + groupPrefix + " " + ChatColor.WHITE;

            // check to see if they are online
            for (SocialGroupMember member : members.values()) {
                Player friendPlayer = getPlayer(member.getPlayerName());
                if (friendPlayer != null) {
                    displayPlayers += member.getPlayerName() + ", ";
                }
            }

            if (displayPlayers.endsWith(", ")) {
                displayPlayers = displayPlayers.substring(0, displayPlayers.length() - 2);
            }
        }

        if (!displayPlayers.equals("")) {
            player.sendMessage(displayPlayers);
        }
    }

    private void displayPlayer(Player player, String prefix, String memberName) {

        String displayRequest = "";

        // check to see if they are online
        Player friendPlayer = getPlayer(memberName);
        if (friendPlayer != null) {
            displayRequest = ChatColor.GREEN + prefix;
            displayRequest += ChatColor.WHITE + " " + memberName;
            player.sendMessage(displayRequest);
        }
    }

    @Override
    public HelpSegment help() {

        ResourcesConfig config = SocialNetworkPlugin.getResources();

        HelpSegment helpSegment = new HelpSegment(getCommandType());

        HelpMessage mainCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), null, null,
                        config.getResource("social.online.help"));
        helpSegment.addEntry(mainCommand);

        return helpSegment;
    }
}
