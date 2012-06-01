package com.netprogs.minecraft.plugins.social.storage.driver.json;

import java.io.File;
import java.util.logging.Logger;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.config.PluginConfig;
import com.netprogs.minecraft.plugins.social.config.settings.SettingsConfig;
import com.netprogs.minecraft.plugins.social.storage.IPersonDataManager;
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

public class JsonPersonDataManager implements IPersonDataManager {

    private final Logger logger = Logger.getLogger("Minecraft");

    private static String DATA_FOLDER = SocialNetworkPlugin.instance.getDataFolder() + "/DataFiles/";

    @Override
    public Person loadPerson(String personName) {

        if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
            logger.info("loadPerson: " + DATA_FOLDER + personName);
        }

        // check to make sure they have a file, if not, they aren't in the network
        File checkFile = new File(DATA_FOLDER + personName);
        if (checkFile.exists()) {

            // now load their data
            PersonConfig config = new PersonConfig(DATA_FOLDER + personName);
            config.loadConfig();
            return config.getPerson();
        }

        if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
            logger.info("[" + personName + "] No file found.");
        }

        return null;
    }

    @Override
    public void savePerson(Person person) {

        // this will create the file if it doesn't already exist
        PersonConfig config = new PersonConfig(DATA_FOLDER + person.getName(), person);
        config.saveConfig();
    }

    @Override
    public void deletePerson(Person person) {

        // deletes the file
        File checkFile = new File(DATA_FOLDER + person.getName());
        checkFile.delete();
    }

    @Override
    public PersonSettings loadPersonSettings(String personName) {

        String FILE_NAME = DATA_FOLDER + personName + ".settings";

        if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
            logger.info("[" + personName + "] PersonSettings. Attempting to load: " + FILE_NAME);
        }

        // check to make sure they have a file, if not, they probably haven't saved any personal settings yet
        File checkFile = new File(FILE_NAME);
        if (checkFile.exists()) {

            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                logger.info("[" + personName + "] PersonSettings. Calling config...");
            }

            // now load their data
            PersonSettingsConfig config = new PersonSettingsConfig(FILE_NAME);
            config.loadConfig();

            return config.getPersonSettings();

        } else {

            // create an empty settings, save it, and return it
            PersonSettings personSettings = new PersonSettings();
            PersonSettingsConfig config = new PersonSettingsConfig(FILE_NAME, personSettings);
            config.saveConfig();
            return personSettings;
        }
    }

    @Override
    public void savePersonSettings(String personName, PersonSettings personSettings) {

        String FILE_NAME = DATA_FOLDER + personName + ".settings";

        if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
            logger.info("[" + personName + "] PersonSettings. Attempting to save: " + FILE_NAME);
        }

        // this will create the file if it doesn't already exist
        PersonSettingsConfig config = new PersonSettingsConfig(FILE_NAME, personSettings);
        config.saveConfig();
    }

    @Override
    public void deletePersonSettings(String personName) {

        String FILE_NAME = DATA_FOLDER + personName + ".settings";

        if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
            logger.info("[" + personName + "] PersonSettings. Attempting to save: " + FILE_NAME);
        }

        File checkFile = new File(FILE_NAME);
        checkFile.delete();
    }
}
