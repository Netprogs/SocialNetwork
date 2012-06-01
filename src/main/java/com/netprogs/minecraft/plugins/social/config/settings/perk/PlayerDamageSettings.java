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

public class PlayerDamageSettings extends PerkSettings {

    private boolean damageAllowedFromPlayer;
    private boolean damageAllowedFromProjectile;
    private boolean damageAllowedFromTameable;

    public boolean isDamageAllowedFromPlayer() {
        return damageAllowedFromPlayer;
    }

    public void setDamageAllowedFromPlayer(boolean damageAllowedFromPlayer) {
        this.damageAllowedFromPlayer = damageAllowedFromPlayer;
    }

    public boolean isDamageAllowedFromProjectile() {
        return damageAllowedFromProjectile;
    }

    public void setDamageAllowedFromProjectile(boolean damageAllowedFromProjectile) {
        this.damageAllowedFromProjectile = damageAllowedFromProjectile;
    }

    public boolean isDamageAllowedFromTameable() {
        return damageAllowedFromTameable;
    }

    public void setDamageAllowedFromTameable(boolean damageAllowedFromTameable) {
        this.damageAllowedFromTameable = damageAllowedFromTameable;
    }
}
