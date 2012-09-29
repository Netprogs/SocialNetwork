package com.netprogs.minecraft.plugins.social.storage.data;

import java.util.ArrayList;
import java.util.List;

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

public class Network {

    // We only store here the names of each player that are in the network.
    // We use that name to reference their data files later on.
    private List<String> players = new ArrayList<String>();
    private List<String> excludedPlayers = new ArrayList<String>();
    private List<String> priests = new ArrayList<String>();
    private List<String> lawyers = new ArrayList<String>();

    public List<String> getPlayers() {
        return players;
    }

    public List<String> getPriests() {
        return priests;
    }

    public List<String> getLawyers() {
        return lawyers;
    }

    public List<String> getExcludedPlayers() {
        return excludedPlayers;
    }
}
