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

public class Gift implements IMessage {

    public enum Type {
        cash, item
    }

    private String senderPlayerName;
    private String receiverPlayerName;
    private Type type;
    private double amount;
    private int itemId;
    private int itemCount;

    public Gift(Type type, String senderPlayerName, String receiverPlayerName, double amount) {
        this.type = type;
        this.senderPlayerName = senderPlayerName;
        this.receiverPlayerName = receiverPlayerName;
        this.amount = amount;
    }

    public Gift(Type type, String senderPlayerName, String receiverPlayerName, int itemId, int itemCount) {
        this.type = type;
        this.senderPlayerName = senderPlayerName;
        this.receiverPlayerName = receiverPlayerName;
        this.itemId = itemId;
        this.itemCount = itemCount;
    }

    public double getAmount() {
        return amount;
    }

    public int getItemId() {
        return itemId;
    }

    public int getItemCount() {
        return itemCount;
    }

    public Type getType() {
        return type;
    }

    public String getSenderPlayerName() {
        return senderPlayerName;
    }

    public String getReceiverPlayerName() {
        return receiverPlayerName;
    }
}
