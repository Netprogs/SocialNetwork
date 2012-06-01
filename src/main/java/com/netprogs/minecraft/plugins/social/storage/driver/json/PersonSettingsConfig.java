package com.netprogs.minecraft.plugins.social.storage.driver.json;

import com.google.gson.GsonBuilder;
import com.netprogs.minecraft.plugins.social.config.JsonConfiguration;
import com.netprogs.minecraft.plugins.social.config.JsonInterfaceAdapter;
import com.netprogs.minecraft.plugins.social.storage.data.PersonSettings;
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

public class PersonSettingsConfig extends JsonConfiguration<PersonSettings> {

    public PersonSettingsConfig(String configFileName) {
        super(configFileName, true);
    }

    public PersonSettingsConfig(String configFileName, PersonSettings personSettings) {
        super(configFileName, true);
        setDataObject(personSettings);
    }

    public PersonSettings getPersonSettings() {
        return getDataObject();
    }

    @Override
    protected void registerTypeAdapters(GsonBuilder builder) {

        // register the person perk settings interface
        builder.registerTypeAdapter(IPersonPerkSettings.class, new JsonInterfaceAdapter<IPersonPerkSettings>())
                .create();
    }
}