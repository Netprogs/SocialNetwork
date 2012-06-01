package com.netprogs.minecraft.plugins.social.command;

import java.util.List;

import com.netprogs.minecraft.plugins.social.command.exception.ArgumentsMissingException;
import com.netprogs.minecraft.plugins.social.command.exception.InvalidPermissionsException;
import com.netprogs.minecraft.plugins.social.command.exception.PlayerNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.exception.PlayerNotOnlineException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotPlayerException;
import com.netprogs.minecraft.plugins.social.command.help.HelpSegment;
import com.netprogs.minecraft.plugins.social.config.settings.ISocialNetworkSettings;

import org.bukkit.command.CommandSender;

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

public interface ISocialNetworkCommand<T extends ISocialNetworkSettings> {

    /**
     * You must extend this with an enum or another class to hold the command types for this command.
     * This is used to relate commands, permissions, settings and resources.
     */
    public interface ICommandType {
    }

    /**
     * Executes the command.
     * @param sender
     * @param arguments
     * @return True if the run was successful. False otherwise.
     * @throws ArgumentsMissingException
     * @throws InvalidPermissionsException
     * @throws SenderNotPlayerException
     * @throws PlayerNotInNetworkException
     * @throws SenderNotInNetworkException
     */
    public boolean run(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            InvalidPermissionsException, SenderNotPlayerException, PlayerNotInNetworkException,
            SenderNotInNetworkException, PlayerNotOnlineException;

    /**
     * Used to determine if the sender has permission to execute this command.
     * @param sender The sender (player)
     * @return True if they have access. False otherwise.
     */
    public boolean hasCommandPermission(CommandSender sender);

    /**
     * The command parameter following /social. This is used to relate commands, permissions, settings and resources.
     * @return The command type.
     */
    public ICommandType getCommandType();

    /**
     * The command settings instance to hold configuration information for the command.
     * @return The command settings instance.
     */
    // public T getCommandSettings();

    /**
     * Produces a {@link Help} instance containing the details of the command.
     * @return Help details.
     */
    public HelpSegment help();
}
