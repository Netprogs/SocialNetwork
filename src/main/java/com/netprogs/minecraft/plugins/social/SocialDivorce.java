package com.netprogs.minecraft.plugins.social;

import com.netprogs.minecraft.plugins.social.storage.data.Divorce;

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

public class SocialDivorce extends SocialGroupMember {

    // private final ReentrantReadWriteLock rwMemberLock = new ReentrantReadWriteLock(true);

    public SocialDivorce(Divorce divorce) {
        super(divorce.getPlayerName());
        // this.person = person;
    }

    // public Iterator<Friend> getFriends() {
    // Lock lock = rwFriendLock.readLock();
    // lock.lock();
    // try {
    // // return person.getFriends().toArray(new Friend[person.getFriends().size()]);
    // return person.getFriends().iterator();
    // } finally {
    // lock.unlock();
    // }
    // }
}
