package com.netprogs.minecraft.plugins.social.command.perk;

import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.config.settings.perk.IPerkSettings;
import com.netprogs.minecraft.plugins.social.integration.VaultIntegration;
import com.netprogs.minecraft.plugins.social.perk.PerkBase;
import com.netprogs.minecraft.plugins.social.storage.data.perk.IPersonPerkSettings;

import org.bukkit.command.CommandSender;

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

public abstract class PerkCommand<T extends IPerkSettings, P extends IPersonPerkSettings> extends PerkBase<T, P>
        implements IPerkCommand<T, P> {

    // The command type is used for command, permissions and resource keys
    private ICommandType commandType;

    protected PerkCommand(ICommandType commandType) {

        this.commandType = commandType;
    }

    @Override
    public ICommandType getCommandType() {
        return commandType;
    }

    @Override
    public boolean hasCommandPermission(CommandSender sender) {
        return VaultIntegration.getInstance().hasCommandPermission(sender, commandType);
    }

    /**
     * Gets the IPerkSettings based on only the person's highest priority social group.
     * @param person
     * @return
     */
    public T getPerkSettings(SocialPerson person) {

        return getPerkSettingsBase(person, null);
    }

    /**
     * Gets the IPerkSettings based on the person's highest priority social group that also contains the given member.
     * @param person
     * @param member
     * @return
     */
    public T getPerkSettings(SocialPerson person, SocialPerson member) {

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
