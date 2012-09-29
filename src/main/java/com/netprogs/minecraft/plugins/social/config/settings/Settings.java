package com.netprogs.minecraft.plugins.social.config.settings;

import java.util.ArrayList;
import java.util.List;

import com.netprogs.minecraft.plugins.social.config.settings.perk.IPerkSettings;

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

public class Settings {

    private boolean autoJoinOnLogin;

    private boolean genderChoiceRequired;
    private boolean genderChoiceFreezeEnabled;
    private boolean genderChoiceReminderEnabled;

    private boolean sameGenderMarriageAllowed;

    private boolean multiWorldGiftsAllowed;

    private boolean globalAnnouncePriestMarriages;

    private long loginNotificationCooldown;
    private long statusMessageNotificationCooldown;

    private boolean loggingDebug;

    private SocialGroupSettings groupSettings;

    private List<? extends IPerkSettings> perkSettings = new ArrayList<IPerkSettings>();

    private CommandMapSettings commandMapSettings;

    public boolean isLoggingDebug() {
        return loggingDebug;
    }

    public void setLoggingDebug(boolean loggingDebug) {
        this.loggingDebug = loggingDebug;
    }

    public SocialGroupSettings getSocialGroupSettings() {
        return groupSettings;
    }

    public void setSocialGroupSettings(SocialGroupSettings socialSettings) {
        this.groupSettings = socialSettings;
    }

    public boolean isGenderChoiceRequired() {
        return genderChoiceRequired;
    }

    public void setGenderChoiceRequired(boolean genderChoiceRequired) {
        this.genderChoiceRequired = genderChoiceRequired;
    }

    public boolean isSameGenderMarriageAllowed() {
        return sameGenderMarriageAllowed;
    }

    public void setSameGenderMarriageAllowed(boolean sameGenderMarriageAllowed) {
        this.sameGenderMarriageAllowed = sameGenderMarriageAllowed;
    }

    public boolean isGlobalAnnouncePriestMarriages() {
        return globalAnnouncePriestMarriages;
    }

    public void setGlobalAnnouncePriestMarriages(boolean globalAnnouncePriestMarriages) {
        this.globalAnnouncePriestMarriages = globalAnnouncePriestMarriages;
    }

    @SuppressWarnings("unchecked")
    public <U extends IPerkSettings> List<U> getPerkSettings() {
        return (List<U>) perkSettings;
    }

    public <U extends IPerkSettings> void setPerkSettings(List<U> perkSettings) {
        this.perkSettings = perkSettings;
    }

    public CommandMapSettings getCommandMapSettings() {
        return commandMapSettings;
    }

    public void setCommandMapSettings(CommandMapSettings commandMapSettings) {
        this.commandMapSettings = commandMapSettings;
    }

    public long getStatusMessageNotificationCooldown() {
        return statusMessageNotificationCooldown;
    }

    public void setStatusMessageNotificationCooldown(long statusMessageNotificationCooldown) {
        this.statusMessageNotificationCooldown = statusMessageNotificationCooldown;
    }

    public long getLoginNotificationCooldown() {
        return loginNotificationCooldown;
    }

    public void setLoginNotificationCooldown(long loginNotificationCooldown) {
        this.loginNotificationCooldown = loginNotificationCooldown;
    }

    public boolean isGenderChoiceFreezeEnabled() {
        return genderChoiceFreezeEnabled;
    }

    public void setGenderChoiceFreezeEnabled(boolean genderChoiceFreezeEnabled) {
        this.genderChoiceFreezeEnabled = genderChoiceFreezeEnabled;
    }

    public boolean isGenderChoiceReminderEnabled() {
        return genderChoiceReminderEnabled;
    }

    public void setGenderChoiceReminderEnabled(boolean genderChoiceReminderEnabled) {
        this.genderChoiceReminderEnabled = genderChoiceReminderEnabled;
    }

    public boolean isAutoJoinOnLogin() {
        return autoJoinOnLogin;
    }

    public void setAutoJoinOnLogin(boolean autoJoinOnLogin) {
        this.autoJoinOnLogin = autoJoinOnLogin;
    }

    public boolean isMultiWorldGiftsAllowed() {
        return multiWorldGiftsAllowed;
    }

    public void setMultiWorldGiftsAllowed(boolean multiWorldGiftsAllowed) {
        this.multiWorldGiftsAllowed = multiWorldGiftsAllowed;
    }
}
