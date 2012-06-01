package com.netprogs.minecraft.plugins.social.listener.perk;

import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.config.settings.perk.IPerkSettings;
import com.netprogs.minecraft.plugins.social.perk.PerkBase;
import com.netprogs.minecraft.plugins.social.storage.data.perk.IPersonPerkSettings;

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
 * Perk listener interface for providing access to perk settings and perk person settings within a listener class.
 */
public abstract class PerkListener<S extends IPerkSettings, P extends IPersonPerkSettings> extends PerkBase<S, P>
        implements IPerkListener<S, P> {

    private ListenerType listenerType;

    public PerkListener(ListenerType listenerType) {
        this.listenerType = listenerType;
    }

    /**
     * The command parameter following /social. This is used to relate settings and resources.
     * @return The listener type.
     */
    @Override
    public IListenerType getListenerType() {
        return listenerType;
    }

    // @Override
    // public S getPerkSettings(SocialPerson person) {
    //
    // return getPerkSettingsBase(person);
    // }

    /**
     * Gets the IPerkSettings based on only the person's highest priority social group.
     * @param person
     * @return
     */
    public S getPerkSettings(SocialPerson person) {

        return getPerkSettingsBase(person, null);
    }

    /**
     * Gets the IPerkSettings based on the person's highest priority social group that also contains the given member.
     * @param person
     * @param member
     * @return
     */
    public S getPerkSettings(SocialPerson person, SocialPerson member) {

        return getPerkSettingsBase(person, member);
    }

    // @Override
    public P getPersonPerkSettings(SocialPerson person) {

        return getPersonPerkSettingsBase(person);
    }

    // @Override
    public <U extends IPersonPerkSettings> void savePersonPerkSettings(SocialPerson person, U perkSettings) {

        savePersonPerkSettingsBase(person, perkSettings);
    }
}
