package com.netprogs.minecraft.plugins.social.command.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.entity.Player;

/*
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

/**
 * This class maintains a list of players that currently have their chat disabled.
 * 
 * This is a memory-only storage of players and will not be saved after server shutdown.
 * 
 * This is combined with the @PlayerChatListener to block incoming and outgoing chat messages for players.
 * 
 */
public class ChatManager {

    private final Set<String> disabledPlayers = new HashSet<String>();

    public void enable(Player player) {

        disabledPlayers.remove(player.getName());
    }

    public void disable(Player player) {

        disabledPlayers.add(player.getName());
    }

    public boolean isDisabled(Player player) {

        return disabledPlayers.contains(player.getName());
    }

    public Iterator<String> getDisabledPlayers() {
        return disabledPlayers.iterator();
    }
}
