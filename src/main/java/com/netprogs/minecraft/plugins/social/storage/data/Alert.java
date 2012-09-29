package com.netprogs.minecraft.plugins.social.storage.data;

import com.netprogs.minecraft.plugins.social.storage.IMessage;

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

public class Alert implements IMessage {

    public enum Type {
        deleted
    }

    // we can't save this type so we need to store it as a string instead and convert back later
    private String type;
    private transient Type alertType;

    private String alertMessage;
    private String playerName;

    public Alert(String playerName, Type alertType, String alertMessage) {
        this.playerName = playerName;
        this.alertMessage = alertMessage;
        this.alertType = alertType;
        this.type = alertType.toString();
    }

    public String getPlayerName() {
        return playerName;
    }

    public Type getAlertType() {
        if (type != null) {
            return Type.valueOf(type);
        }
        return alertType;
    }

    public String getAlertMessage() {
        return alertMessage;
    }
}
