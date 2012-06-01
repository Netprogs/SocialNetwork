package com.netprogs.minecraft.plugins.social.config.settings.perk;

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

public abstract class PerkSettings implements IPerkSettings {

    private String name;
    private long coolDownPeriod;
    private double perUseCost;

    public PerkSettings() {
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public long getCoolDownPeriod() {
        return coolDownPeriod;
    }

    public void setCoolDownPeriod(long coolDownPeriod) {
        this.coolDownPeriod = coolDownPeriod;
    }

    @Override
    public double getPerUseCost() {
        return perUseCost;
    }

    public void setPerUseCost(double perUseCost) {
        this.perUseCost = perUseCost;
    }
}
