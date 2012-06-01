package com.netprogs.minecraft.plugins.social.storage.data;

import com.netprogs.minecraft.plugins.social.SocialPerson;

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

/**
 * <pre>
 * This provides attributes for a person that is in held in a group list.
 * 
 * Since we don't actually need that much information about the person in a group (since we can just reference their
 * {@link SocialPerson} object, then this class will basically contain only information that is needed to manage the group
 * references.
 * </pre>
 */
public abstract class GroupMember {

    // This name is used for looking up their actual Person object
    private String playerName;

    public GroupMember(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }
}
