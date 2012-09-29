package com.netprogs.minecraft.plugins.social.config.settings.perk;

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

public class HealthRegenSettings extends PerkSettings {

    private int proximity;

    // This is the number of hearts to gain per server "tick". This number actually represents half a heart.
    // This can be a negative number also to create a "no regen" or "negative regen" effect for wanted cases.
    // If a negative is given, we do not allow it to reduce them below 1 heart to avoid death.
    private int heartsPerkTick;

    public int getProximity() {
        return proximity;
    }

    public int getHeartsPerkTick() {
        return heartsPerkTick;
    }
}
