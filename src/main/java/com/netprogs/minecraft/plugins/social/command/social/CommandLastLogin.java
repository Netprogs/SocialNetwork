package com.netprogs.minecraft.plugins.social.command.social;

import java.util.ArrayList;
import java.util.List;

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
import com.netprogs.minecraft.plugins.social.command.util.MessageParameter;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.command.util.PlayerUtil;
import com.netprogs.minecraft.plugins.social.command.util.TimerManager;
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

public class CommandLastLogin extends SocialNetworkCommand<ISocialNetworkSettings> {

    public CommandLastLogin() {
        super(SocialNetworkCommandType.lastlogin);
    }

    @Override
    public boolean run(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            InvalidPermissionsException, SenderNotPlayerException, PlayerNotInNetworkException,
            SenderNotInNetworkException {

        // verify that the sender is actually a player
        verifySenderAsPlayer(sender);

        // check arguments
        if (arguments.size() != 1) {
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

        // get our group person
        String groupPlayerName = arguments.get(0);
        SocialPerson groupPerson = SocialNetworkPlugin.getStorage().getPerson(groupPlayerName);
        if (groupPerson == null) {
            throw new PlayerNotInNetworkException(groupPlayerName);
        }

        // Now we need to check to see if the caller has the given player as part of their social group
        boolean isGroupMember = person.hasGroupMember(groupPerson);
        if (isGroupMember) {

            long lastLogin = PlayerUtil.getPlayerLastPlayed(groupPerson.getName());
            String dateTime = TimerManager.formatDate(lastLogin);

            MessageParameter messageName = new MessageParameter("<player>", groupPerson.getName(), ChatColor.AQUA);
            MessageParameter messageDateTime = new MessageParameter("<date>", dateTime, ChatColor.GREEN);

            List<MessageParameter> requestParameters = new ArrayList<MessageParameter>();
            requestParameters.add(messageName);
            requestParameters.add(messageDateTime);

            MessageUtil.sendMessage(player, "social.lastlogin.completed", ChatColor.GREEN, requestParameters);

        } else {

            MessageUtil.sendPersonNotInGroupMessage(player, groupPlayerName);
        }

        return true;
    }

    @Override
    public HelpSegment help() {

        ResourcesConfig config = SocialNetworkPlugin.getResources();

        HelpSegment helpSegment = new HelpSegment(getCommandType());

        HelpMessage mainCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), null, "<player>",
                        config.getResource("social.lastlogin.help"));
        helpSegment.addEntry(mainCommand);

        return helpSegment;
    }
}
