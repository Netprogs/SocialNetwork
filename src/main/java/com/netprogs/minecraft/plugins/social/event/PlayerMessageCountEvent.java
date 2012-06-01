package com.netprogs.minecraft.plugins.social.event;

import com.netprogs.minecraft.plugins.social.SocialPerson;

import org.bukkit.entity.Player;
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
 * Called when a player first logs in. This allows you a chance to add to the entrance text sent out saying what
 * messages they have available to them.
 */
public class PlayerMessageCountEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private Player player;
    private SocialPerson playerPerson;

    public PlayerMessageCountEvent(Player player, SocialPerson playerPerson) {
        this.player = player;
        this.playerPerson = playerPerson;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public SocialPerson getPlayerPerson() {
        return playerPerson;
    }

    public Player getPlayer() {
        return player;
    }
}
