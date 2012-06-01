package com.netprogs.minecraft.plugins.social.perk;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.config.PluginConfig;
import com.netprogs.minecraft.plugins.social.config.settings.SettingsConfig;
import com.netprogs.minecraft.plugins.social.config.settings.group.GroupSettings;
import com.netprogs.minecraft.plugins.social.config.settings.perk.IPerkSettings;
import com.netprogs.minecraft.plugins.social.storage.SocialNetwork;
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

public abstract class PerkBase<S extends IPerkSettings, P extends IPersonPerkSettings> {

    private final Logger logger = Logger.getLogger("Minecraft");

    @SuppressWarnings("unchecked")
    protected Class<S> getPerkSettingsClassBase() {

        // get the sub-class type
        ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
        Class<S> classObject = (Class<S>) genericSuperclass.getActualTypeArguments()[0];

        return classObject;
    }

    protected S getPerkSettingsBase(SocialPerson person, SocialPerson member) {

        //
        // A perk command or listener do not know which group they are being run for since these commands and listeners
        // are not being run within a group scope.
        //
        // Because of that, we need to be able to determine which group settings to apply to the execution.
        //
        // We are accomplishing this by assigning a "priority" to each group. Then we get a list of the available groups
        // in the system ordered by that priority list.
        //
        // From there, we get a list of groups the user is currently within.
        //
        // We then compare the groups the user belongs to against the system groups in the priority order.
        // The first one we come that matches, will be considered the settings to use for execution.
        //

        // get the settings
        SettingsConfig settingsConfig = PluginConfig.getInstance().getConfig(SettingsConfig.class);

        // get the list of group settings the person belongs to
        List<GroupSettings> personGroupSettings = person.getGroupSettings();

        // get the settings class for this perk from the generic template
        Class<S> classObject = (Class<S>) getPerkSettingsClassBase();

        if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
            logger.info("------------------------ getPerkSettings() START ---------------------");
        }

        // use that to lookup our settings instance list for this perk
        Map<String, ? extends IPerkSettings> perkSettingsMap = settingsConfig.getPerkSettingsMap(classObject);
        if (perkSettingsMap != null) {

            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                for (GroupSettings groupSettings : settingsConfig.getSocialNetworkGroupSettings()) {
                    logger.info("groupSettings: " + groupSettings.getClass().getSimpleName());
                }
            }

            // The social network group settings list has been sorted by priority as defined in the configuration file.
            // Let's check each one in order against the list of what the user currently belongs to.
            for (GroupSettings groupSettings : settingsConfig.getSocialNetworkGroupSettings()) {

                if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                    logger.info("Checking player: " + person.getName() + " for groupSettings: "
                            + groupSettings.getClass().getSimpleName());
                }

                // check each of the persons current groups to see if we have one
                if (personGroupSettings.contains(groupSettings)) {

                    // Now we have a settings group from the configuration, in the correct priority level, let's
                    // check to see which perks it contains.
                    //
                    // Our perk map contains only settings of this command type, so all we need to do is match the
                    // names of those against the one's in the social group. The social group should only ever have
                    // one of any particular perk within it.
                    for (String perkName : groupSettings.getPerks()) {

                        if (perkSettingsMap.containsKey(perkName)) {

                            IPerkSettings perkSettings = perkSettingsMap.get(perkName);

                            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                                logger.info("Player: " + person.getName() + " has groupSettings: "
                                        + groupSettings.getClass().getSimpleName() + " with perk: "
                                        + perkSettings.getName());
                            }

                            // check to see if the member also has this perk by checking all their groups
                            // (could end up matching this same group, but that is fine)
                            if (member != null) {

                                if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                                    logger.info("Checking to see if member " + member.getName() + " belongs to any of "
                                            + person.getName() + " groups with a perk: " + perkSettings.getName());
                                }

                                if (person.isPerkMember(perkSettings, member)) {

                                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {

                                        logger.info("Match found. Using perk settings: " + perkSettings.getName()
                                                + " found in group: " + groupSettings.getClass().getSimpleName());

                                        logger.info("------------------------ getPerkSettings() END ---------------------");
                                    }

                                    return classObject.cast(perkSettings);
                                }

                            } else {

                                if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                                    logger.info("------------------------ getPerkSettings() END ---------------------");
                                }

                                // No member was given, so let's just return the one we found
                                // This would be used for cases where the perk only needs a config for the executing
                                // player and not related to any secondary interacting player.
                                return classObject.cast(perkSettings);
                            }
                        }
                    }

                } else {

                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("Player: " + person.getName() + " does NOT have groupSettings: "
                                + groupSettings.getClass().getSimpleName());
                    }
                }
            }
        }

        if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
            logger.info("------------------------ getPerkSettings() END: NO MATCH ---------------------");
        }

        // we'll return null if nothing at all is found
        return null;
    }

    @SuppressWarnings("unchecked")
    protected P getPersonPerkSettingsBase(SocialPerson person) {

        // request their settings for this perk from the SocialNetwork data controller
        P settings =
                SocialNetwork.getInstance().getPersonPerkSettings(person.getName(),
                        getPerkSettingsClassBase().getSimpleName());

        if (settings == null) {

            // they may not have saved anything yet, so we'll make an empty one to return

            // get the sub-class type
            ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
            Class<P> classObject = (Class<P>) genericSuperclass.getActualTypeArguments()[1];
            try {
                settings = (P) classObject.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return settings;
    }

    protected <U extends IPersonPerkSettings> void savePersonPerkSettingsBase(SocialPerson person, U perkSettings) {

        // ask the SocialNetwork data controller to save the settings
        SocialNetwork.getInstance().setPersonPerkSettings(person.getName(), getPerkSettingsClassBase().getSimpleName(),
                perkSettings);
    }
}
