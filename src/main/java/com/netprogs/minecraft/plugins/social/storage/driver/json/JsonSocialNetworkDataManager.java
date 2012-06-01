package com.netprogs.minecraft.plugins.social.storage.driver.json;

import java.util.List;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.storage.ISocialNetworkDataManager;

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

public class JsonSocialNetworkDataManager implements ISocialNetworkDataManager {

    private SocialNetworkConfig config;

    public JsonSocialNetworkDataManager() {

        // create and run the JSON configuration loader
        config = new SocialNetworkConfig(SocialNetworkPlugin.instance.getDataFolder() + "/network.json");
        config.loadConfig();
    }

    @Override
    public List<String> getPlayers() {
        return config.getNetwork().getPlayers();
    }

    @Override
    public boolean hasPlayer(String playerName) {
        return config.getNetwork().getPlayers().contains(playerName);
    }

    @Override
    public void addPlayer(String playerName) {
        config.getNetwork().getPlayers().add(playerName);
        config.saveConfig();
    }

    @Override
    public void removePlayer(String playerName) {
        config.getNetwork().getPlayers().remove(playerName);
        config.saveConfig();
    }

    @Override
    public List<String> getPriests() {
        return config.getNetwork().getPriests();
    }

    @Override
    public boolean hasPriest(String playerName) {
        return config.getNetwork().getPriests().contains(playerName);
    }

    @Override
    public void addPriest(String playerName) {
        config.getNetwork().getPriests().add(playerName);
        config.saveConfig();
    }

    @Override
    public void removePriest(String playerName) {
        config.getNetwork().getPriests().remove(playerName);
        config.saveConfig();
    }

    @Override
    public List<String> getLawyers() {
        return config.getNetwork().getLawyers();
    }

    @Override
    public boolean hasLawyer(String playerName) {
        return config.getNetwork().getLawyers().contains(playerName);
    }

    @Override
    public void addLawyer(String playerName) {
        config.getNetwork().getLawyers().add(playerName);
        config.saveConfig();
    }

    @Override
    public void removeLawyer(String playerName) {
        config.getNetwork().getLawyers().remove(playerName);
        config.saveConfig();
    }
}
