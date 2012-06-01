package com.netprogs.minecraft.plugins.social.command.perk;

import java.util.List;

import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.command.ISocialNetworkCommand;
import com.netprogs.minecraft.plugins.social.config.settings.ISocialNetworkSettings;
import com.netprogs.minecraft.plugins.social.storage.data.perk.IPersonPerkSettings;

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

public interface IPerkCommand<T extends ISocialNetworkSettings, P extends IPersonPerkSettings> extends
        ISocialNetworkCommand<T> {

    /**
     * Used to determine if the particular call being made should be pre-processed (check timer & pre-auth cost).
     * @param player
     * @param commandArguments
     * @return
     */
    public boolean allowPreProcessPerkCommand(Player player, List<String> commandArguments);

    /**
     * Used to determine if the particular call being made should be post-processed (set timer & charge cost).
     * @param player
     * @param commandArguments
     * @return
     */
    public boolean allowPostProcessPerkCommand(Player player, List<String> commandArguments);

    /**
     * Obtain the settings for this perk using the executing person and any arguments they're using. If you return NULL
     * from this call, the cooldown and perUseCosts will not be applied for this execution. If you want to handle them
     * yourself, you can return null also.
     * @param player
     * @param commandArguments
     * @return A ISocialNetworkSettings instance or NULL if one could not be obtained.
     */
    public T getProcessPerkSettings(SocialPerson person, List<String> commandArguments);
}
