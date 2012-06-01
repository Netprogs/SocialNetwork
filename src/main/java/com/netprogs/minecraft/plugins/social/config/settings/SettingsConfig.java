package com.netprogs.minecraft.plugins.social.config.settings;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.GsonBuilder;
import com.netprogs.minecraft.plugins.social.config.JsonConfiguration;
import com.netprogs.minecraft.plugins.social.config.JsonInterfaceAdapter;
import com.netprogs.minecraft.plugins.social.config.settings.group.AffairSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.ChildSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.DivorceSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.EngagementSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.FriendSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.GroupSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.LawyerSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.MarriageSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.PriestSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.RelationshipSettings;
import com.netprogs.minecraft.plugins.social.config.settings.perk.IPerkSettings;

public class SettingsConfig extends JsonConfiguration<Settings> {

    private final Logger logger = Logger.getLogger("Minecraft");

    // The social settings are the settings for the social network. All types end up here.
    private final Map<Class<? extends ISocialNetworkSettings>, ISocialNetworkSettings> settingsMap =
            new LinkedHashMap<Class<? extends ISocialNetworkSettings>, ISocialNetworkSettings>();

    // This holds the list of social group settings in order of priority
    private List<GroupSettings> settings = new ArrayList<GroupSettings>();

    // The perk settings are for configuring the perks themselves.
    private final Map<Class<? extends IPerkSettings>, Map<String, IPerkSettings>> perksMap =
            new HashMap<Class<? extends IPerkSettings>, Map<String, IPerkSettings>>();

    public SettingsConfig(String configFileName) {
        super(configFileName);

    }

    @Override
    protected void registerTypeAdapters(GsonBuilder builder) {

        // register the Message interface
        builder.registerTypeAdapter(IPerkSettings.class, new JsonInterfaceAdapter<IPerkSettings>()).create();
    }

    @Override
    protected void postLoad() {

        super.postLoad();

        // generate all the required data maps
        generateSocialMaps();
    }

    public boolean isLoggingDebug() {
        return getDataObject().isLoggingDebug();
    }

    public boolean isSameGenderMarriageAllowed() {
        return getDataObject().isSameGenderMarriageAllowed();
    }

    public boolean isGenderChoiceRequired() {
        return getDataObject().isGenderChoiceRequired();
    }

    public boolean isGlobalAnnouncePriestMarriages() {
        return getDataObject().isGlobalAnnouncePriestMarriages();
    }

    public GroupSettings[] getSocialNetworkGroupSettings() {
        return settings.toArray(new GroupSettings[settings.size()]);
    }

    public <T extends ISocialNetworkSettings> T getSocialNetworkSettings(Class<T> settingsClass) {

        return settingsClass.cast(settingsMap.get(settingsClass));
    }

    public Map<String, ? extends IPerkSettings> getPerkSettingsMap(Class<? extends IPerkSettings> settingsClass) {

        return perksMap.get(settingsClass);
    }

    private void generateSocialMaps() {

        if (!settingsMap.isEmpty() && !perksMap.isEmpty()) {
            return;
        }

        //
        // Load the social group data
        //

        AffairSettings affairSettings = getDataObject().getSocialGroupSettings().getAffairSettings();
        if (affairSettings != null) {
            // settingsMap.put(AffairSettings.class, affairSettings);
            settings.add(affairSettings);
        } else {
            logger.log(Level.SEVERE, "AffairSettings is invalid");
        }

        ChildSettings childSettings = getDataObject().getSocialGroupSettings().getChildSettings();
        if (childSettings != null) {
            // settingsMap.put(ChildSettings.class, childSettings);
            settings.add(childSettings);
        } else {
            logger.log(Level.SEVERE, "ChildSettings is invalid");
        }

        DivorceSettings divorceSettings = getDataObject().getSocialGroupSettings().getDivorceSettings();
        if (divorceSettings != null) {
            // settingsMap.put(DivorceSettings.class, divorceSettings);
            settings.add(divorceSettings);
        } else {
            logger.log(Level.SEVERE, "DivorceSettings is invalid");
        }

        EngagementSettings engagementSettings = getDataObject().getSocialGroupSettings().getEngagementSettings();
        if (engagementSettings != null) {
            // settingsMap.put(EngagementSettings.class, engagementSettings);
            settings.add(engagementSettings);
        } else {
            logger.log(Level.SEVERE, "EngagementSettings is invalid");
        }

        FriendSettings friendSettings = getDataObject().getSocialGroupSettings().getFriendSettings();
        if (friendSettings != null) {
            // settingsMap.put(FriendSettings.class, friendSettings);
            settings.add(friendSettings);
        } else {
            logger.log(Level.SEVERE, "FriendSettings is invalid");
        }

        LawyerSettings lawyerSettings = getDataObject().getSocialGroupSettings().getLawyerSettings();
        if (lawyerSettings != null) {
            // settingsMap.put(LawyerSettings.class, lawyerSettings);
            settings.add(lawyerSettings);
        } else {
            logger.log(Level.SEVERE, "LawyerSettings is invalid");
        }

        MarriageSettings marriageSettings = getDataObject().getSocialGroupSettings().getMarriageSettings();
        if (marriageSettings != null) {
            // settingsMap.put(MarriageSettings.class, marriageSettings);
            settings.add(marriageSettings);
        } else {
            logger.log(Level.SEVERE, "MarriageSettings is invalid");
        }

        PriestSettings priestSettings = getDataObject().getSocialGroupSettings().getPriestSettings();
        if (priestSettings != null) {
            // settingsMap.put(PriestSettings.class, priestSettings);
            settings.add(priestSettings);
        } else {
            logger.log(Level.SEVERE, "PriestSettings is invalid");
        }

        RelationshipSettings relationshipSettings = getDataObject().getSocialGroupSettings().getRelationshipSettings();
        if (relationshipSettings != null) {
            // settingsMap.put(RelationshipSettings.class, relationshipSettings);
            settings.add(relationshipSettings);
        } else {
            logger.log(Level.SEVERE, "RelationshipSettings is invalid");
        }

        // sort the list by priority and add them to the settings map
        Collections.sort(settings, new Comparator<GroupSettings>() {

            public int compare(GroupSettings o1, GroupSettings o2) {
                return o1.getPriority().compareTo(o2.getPriority());
            }
        });

        for (ISocialNetworkSettings groupSettings : settings) {
            settingsMap.put(groupSettings.getClass(), groupSettings);
        }

        //
        // Load the perk data.
        //
        // These will be our "pre-built" perks. Users will later be able to add their own through API.
        //
        // These should not be added to the social group perk settings map since they are perks themselves.
        //

        // load the command perk settings
        for (IPerkSettings perkSettings : getDataObject().getPerkSettings()) {

            if (!perksMap.containsKey(perkSettings.getClass())) {
                perksMap.put(perkSettings.getClass(), new HashMap<String, IPerkSettings>());
            }

            Map<String, IPerkSettings> perks = perksMap.get(perkSettings.getClass());
            perks.put(perkSettings.getName(), perkSettings);
        }
    }
}
