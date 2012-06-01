package com.netprogs.minecraft.plugins.social.config.settings.group;

import java.util.List;

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

public abstract class GroupSettings implements ISocialNetworkSettings {

    //
    // The priority in which this group should behave in comparison to the others.
    //
    // This is used for when we look up the group settings for a particular perk when it's being executed.
    //
    // There is no way for a perk command to know which specific group it's running within. So we use this priority to
    // determine which perk setting from the list of groups can have should be used.
    //
    // If two group settings for the same perk have the same rating, it will pick based on the first index within the
    // loading list.
    //
    public enum Priority {
        HIGHEST, HIGH, HIGH_NORMAL, NORMAL, LOW_NORMAL, LOW, LOWEST
    }

    // the group priority
    private Priority priority;

    // This is the Permissions group that would be added to the user when this social group is active.
    private String permissionsGroup;

    // the initial cost collected when using this group
    private double perUseCost;

    // perks assigned to this setting group
    private List<String> perks;

    public List<String> getPerks() {
        return perks;
    }

    public void setPerks(List<String> perks) {
        this.perks = perks;
    }

    public String getPermissionsGroup() {
        return permissionsGroup;
    }

    public void setPermissionsGroup(String permissionsGroup) {
        this.permissionsGroup = permissionsGroup;
    }

    public double getPerUseCost() {
        return perUseCost;
    }

    public void setPerUseCost(double perUseCost) {
        this.perUseCost = perUseCost;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }
}
