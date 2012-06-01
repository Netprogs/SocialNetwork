package com.netprogs.minecraft.plugins.social.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

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
 * Called when a players group changes. For example, they lose their last friend and drop out of the friends group.
 */
public class PlayerPermissionGroupChangeEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    public static enum Type {
        added, removed
    }

    private String playerName;
    private String groupType;
    private Type eventType;

    public PlayerPermissionGroupChangeEvent(String playerName, String groupType, Type eventType) {

        this.playerName = playerName;
        this.groupType = groupType;
        this.eventType = eventType;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getGroupType() {
        return groupType;
    }

    public Type getEventType() {
        return eventType;
    }
}
