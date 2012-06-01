package com.netprogs.minecraft.plugins.social.storage;

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

public interface ISocialNetworkDataManager {

    public List<String> getPlayers();

    public boolean hasPlayer(String playerName);

    public void addPlayer(String playerName);

    public void removePlayer(String playerName);

    public List<String> getPriests();

    public boolean hasPriest(String playerName);

    public void addPriest(String playerName);

    public void removePriest(String playerName);

    public List<String> getLawyers();

    public boolean hasLawyer(String playerName);

    public void addLawyer(String playerName);

    public void removeLawyer(String playerName);
}
