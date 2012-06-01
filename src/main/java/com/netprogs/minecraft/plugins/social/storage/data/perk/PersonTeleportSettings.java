package com.netprogs.minecraft.plugins.social.storage.data.perk;

import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommandType;

/*
 * Copyright (C) 2012 Scott Milne
 * 
 * "Social Network" is a Craftbukkit Minecraft server modification plug-in. It attempts to add a 
 * social environment to your server by allowing players to be placed into different types of social groups.
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

public class PersonTeleportSettings implements IPersonPerkSettings {

    private String world;
    private int blockX;
    private int blockY;
    private int blockZ;

    /**
     * The name of the perk. Used to relate back to the social group perk settings.
     * @return Name of the perk.
     */
    public String getName() {
        return SocialNetworkCommandType.teleport.toString();
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public int getBlockX() {
        return blockX;
    }

    public void setBlockX(int blockX) {
        this.blockX = blockX;
    }

    public int getBlockY() {
        return blockY;
    }

    public void setBlockY(int blockY) {
        this.blockY = blockY;
    }

    public int getBlockZ() {
        return blockZ;
    }

    public void setBlockZ(int blockZ) {
        this.blockZ = blockZ;
    }
}
