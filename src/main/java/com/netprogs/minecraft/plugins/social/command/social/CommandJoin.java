package com.netprogs.minecraft.plugins.social.command.social;

import java.util.List;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommand;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommandType;
import com.netprogs.minecraft.plugins.social.command.exception.ArgumentsMissingException;
import com.netprogs.minecraft.plugins.social.command.exception.InvalidPermissionsException;
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

public class CommandJoin extends SocialNetworkCommand<ISocialNetworkSettings> {

    public CommandJoin() {
        super(SocialNetworkCommandType.join);
    }

    public boolean run(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            InvalidPermissionsException, SenderNotPlayerException {

        ResourcesConfig resources = SocialNetworkPlugin.getResources();

        // verify that the sender is actually a player
        verifySenderAsPlayer(sender);

        // check permissions
        if (!hasCommandPermission(sender)) {
            throw new InvalidPermissionsException();
        }

        Player player = (Player) sender;

        // check arguments
        if (arguments.size() != 0) {
            throw new ArgumentsMissingException();
        }

        // get the social network data
        SocialNetworkStorage socialConfig = SocialNetworkPlugin.getStorage();

        // check to see if the person is already there, if not, then start to add them
        SocialPerson person = socialConfig.getPerson(player.getName());
        if (person != null) {

            String alreadyJoined = resources.getResource("social.error.alreadyJoined.sender");

            sender.sendMessage(ChatColor.RED + alreadyJoined);

            return false;
        }

        // if they had previously quit the network, then remove them from the excluded list
        if (SocialNetworkPlugin.getStorage().isExcludedPlayer(player.getName())) {

            // now that they're asking to re-join, remove them from the excluded players list
            SocialNetworkPlugin.getStorage().removeExcludedPlayer(player.getName());

            // create the person instance and add to the network
            socialConfig.addPerson(player);

            // gender isn't required, so send the welcome message now
            MessageUtil.sendMessage(sender, "social.rejoin.completed.sender", ChatColor.GOLD);

        } else {

            // create the person instance and add to the network
            socialConfig.addPerson(player);

            // gender isn't required, so send the welcome message now
            MessageUtil.sendMessage(sender, "social.join.completed.sender", ChatColor.GOLD);
        }

        return true;
    }

    @Override
    public HelpSegment help() {

        ResourcesConfig config = SocialNetworkPlugin.getResources();
        HelpSegment helpSegment = new HelpSegment(getCommandType());

        if (SocialNetworkPlugin.getSettings().isAutoJoinOnLogin()) {

            HelpMessage mainCommand =
                    HelpBook.generateHelpMessage(getCommandType().toString(), null, null,
                            config.getResource("social.rejoin.help"));
            helpSegment.addEntry(mainCommand);

        } else {

            HelpMessage mainCommand =
                    HelpBook.generateHelpMessage(getCommandType().toString(), null, null,
                            config.getResource("social.join.help"));
            helpSegment.addEntry(mainCommand);
        }

        return helpSegment;
    }
}
