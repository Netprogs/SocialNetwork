package com.netprogs.minecraft.plugins.social.config.settings;

import com.netprogs.minecraft.plugins.social.config.settings.group.AffairSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.ChildSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.DivorceSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.EngagementSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.FriendSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.LawyerSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.MarriageSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.PriestSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.RelationshipSettings;

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

public class SocialGroupSettings {

    private ChildSettings childSettings;
    private FriendSettings friendSettings;
    private RelationshipSettings relationshipSettings;
    private AffairSettings affairSettings;

    private EngagementSettings engagementSettings;
    private DivorceSettings divorceSettings;
    private MarriageSettings marriageSettings;

    private LawyerSettings lawyerSettings;
    private PriestSettings priestSettings;

    public ChildSettings getChildSettings() {
        return childSettings;
    }

    public void setChildSettings(ChildSettings childSettings) {
        this.childSettings = childSettings;
    }

    public FriendSettings getFriendSettings() {
        return friendSettings;
    }

    public void setFriendSettings(FriendSettings friendSettings) {
        this.friendSettings = friendSettings;
    }

    public DivorceSettings getDivorceSettings() {
        return divorceSettings;
    }

    public void setDivorceSettings(DivorceSettings divorceSettings) {
        this.divorceSettings = divorceSettings;
    }

    public MarriageSettings getMarriageSettings() {
        return marriageSettings;
    }

    public void setMarriageSettings(MarriageSettings marriageSettings) {
        this.marriageSettings = marriageSettings;
    }

    public LawyerSettings getLawyerSettings() {
        return lawyerSettings;
    }

    public void setLawyerSettings(LawyerSettings lawyerSettings) {
        this.lawyerSettings = lawyerSettings;
    }

    public PriestSettings getPriestSettings() {
        return priestSettings;
    }

    public void setPriestSettings(PriestSettings priestSettings) {
        this.priestSettings = priestSettings;
    }

    public RelationshipSettings getRelationshipSettings() {
        return relationshipSettings;
    }

    public void setRelationshipSettings(RelationshipSettings relationshipSettings) {
        this.relationshipSettings = relationshipSettings;
    }

    public EngagementSettings getEngagementSettings() {
        return engagementSettings;
    }

    public void setEngagementSettings(EngagementSettings engagementSettings) {
        this.engagementSettings = engagementSettings;
    }

    public AffairSettings getAffairSettings() {
        return affairSettings;
    }

    public void setAffairSettings(AffairSettings affairSettings) {
        this.affairSettings = affairSettings;
    }
}
