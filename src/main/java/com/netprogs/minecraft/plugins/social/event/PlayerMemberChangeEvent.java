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
 * Called when a member within a player group has changed. Either added or removed.
 * This event is fired AFTER the person is ADDED to the group.
 * This event is fired BEFORE the person is REMOVED from the group.
 */
public class PlayerMemberChangeEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    public static enum Type {
        preAdd, postAdd, preRemove, postRemove
    }

    private String playerName;
    private String memberName;
    private String groupType;
    private Type eventType;
    private boolean groupEmpty;

    public PlayerMemberChangeEvent(String playerName, String memberName, String groupType, Type eventType,
            boolean groupEmpty) {

        this.playerName = playerName;
        this.memberName = memberName;
        this.groupType = groupType;
        this.eventType = eventType;
        this.groupEmpty = groupEmpty;
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

    public String getMemberName() {
        return memberName;
    }

    public boolean isGroupEmpty() {
        return groupEmpty;
    }
}
