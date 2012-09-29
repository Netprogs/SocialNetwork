package com.netprogs.minecraft.plugins.social.storage;

import com.netprogs.minecraft.plugins.social.storage.data.Person;
import com.netprogs.minecraft.plugins.social.storage.data.PersonSettings;

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

public interface IPersonDataManager {

    public Person loadPerson(String personName);

    public void savePerson(Person person);

    public void deletePerson(Person person);

    public PersonSettings loadPersonSettings(Person person);

    public void savePersonSettings(Person person, PersonSettings settings);

    public void deletePersonSettings(Person person);
}
