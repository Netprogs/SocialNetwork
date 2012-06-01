package com.netprogs.minecraft.plugins.social.config.settings.perk;

import com.netprogs.minecraft.plugins.social.config.settings.ISocialNetworkSettings;

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

public interface IPerkSettings extends ISocialNetworkSettings {

    /**
     * The name of the perk. Used to relate back to the social group perk settings.
     * @return Name of the perk.
     */
    String getName();

    /**
     * The length of time (in seconds) we want to wait between excecutions of this perk.
     * @return
     */
    long getCoolDownPeriod();

    /**
     * The cost of executing this perk;
     * @return
     */
    double getPerUseCost();
}
