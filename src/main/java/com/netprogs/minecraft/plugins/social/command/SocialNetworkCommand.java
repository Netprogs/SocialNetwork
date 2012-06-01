package com.netprogs.minecraft.plugins.social.command;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.logging.Logger;

import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotPlayerException;
import com.netprogs.minecraft.plugins.social.command.group.GroupCommand;
import com.netprogs.minecraft.plugins.social.config.PluginConfig;
import com.netprogs.minecraft.plugins.social.config.settings.ISocialNetworkSettings;
import com.netprogs.minecraft.plugins.social.config.settings.SettingsConfig;
import com.netprogs.minecraft.plugins.social.config.settings.group.GroupSettings;
import com.netprogs.minecraft.plugins.social.event.PlayerPermissionGroupChangeEvent;
import com.netprogs.minecraft.plugins.social.integration.VaultIntegration;

import net.milkbowl.vault.permission.plugins.Permission_PermissionsBukkit;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

public abstract class SocialNetworkCommand<T extends ISocialNetworkSettings> implements ISocialNetworkCommand<T> {

    private final Logger logger = Logger.getLogger("Minecraft");

    // The command type is used for command, permissions and resource keys
    private ICommandType commandType;

    protected SocialNetworkCommand(ICommandType commandType) {

        this.commandType = commandType;
    }

    public void verifySenderAsPlayer(CommandSender sender) throws SenderNotPlayerException {

        if (!(sender instanceof Player)) {
            throw new SenderNotPlayerException();
        }
    }

    public Player getPlayer(String playerName) {
        return Bukkit.getServer().getPlayer(playerName);
    }

    @Override
    public boolean hasCommandPermission(CommandSender sender) {
        return VaultIntegration.getInstance().hasCommandPermission(sender, commandType);
    }

    @Override
    public ICommandType getCommandType() {
        return commandType;
    }

    // @Override
    @SuppressWarnings("unchecked")
    public T getCommandSettings() {

        // get the sub-class type
        ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
        Class<T> classObject = (Class<T>) genericSuperclass.getActualTypeArguments()[0];

        // use that to obtain the settings for this instance
        SettingsConfig settingsConfig = PluginConfig.getInstance().getConfig(SettingsConfig.class);
        return (T) settingsConfig.getSocialNetworkSettings(classObject);
    }

    /**
     * Checks to see if we need to update the permissions groups because the user may have been added or removed to a
     * related social group.
     * @param person The person.
     * @param groupSettingsClass The settings class that called this method.
     */
    protected void checkForPermissionsUpdate(SocialPerson person) {

        Player player = Bukkit.getServer().getPlayer(person.getName());
        if (player == null) {
            // they're off-line
            return;
        }

        // check to make sure we're only using group commands since perks don't support permissions groups
        if (!(this instanceof GroupCommand)) {
            return;
        }

        // down-cast it, we only want to use it for the permission name
        GroupSettings groupSettings = (GroupSettings) getCommandSettings();

        // get the list of current groups the player belongs to
        List<GroupSettings> personGroupSettings = person.getGroupSettings();

        //
        // The Vault::Permission_PermissionsBukkit version of this checks for world to BE NULL.
        //
        // So, for now, pass NULL for world for that Permission implementation. I'll come back to this later.
        //
        String world = player.getWorld().getName();
        if ((VaultIntegration.getInstance().getPermission() instanceof Permission_PermissionsBukkit)) {
            world = null;
        }

        // Try getting the group settings for the one that has triggered this call
        // If it's NULL that means the configuration didn't have anything related, so we won't bother with it
        if (!StringUtils.isEmpty(groupSettings.getPermissionsGroup())) {

            // Check to see if the players groups still contain the one that triggered the event.
            // If it's not in the players social groups, that means we should remove their related permissions group
            if (!personGroupSettings.contains(groupSettings)) {

                if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                    logger.info("Removing group: " + player.getName() + ", " + groupSettings.getPermissionsGroup());
                }

                boolean removed =
                        VaultIntegration.getInstance().getPermission()
                                .playerRemoveGroup(world, player.getName(), groupSettings.getPermissionsGroup());

                if (removed) {

                    // create the group change event and fire it off
                    PlayerPermissionGroupChangeEvent event =
                            new PlayerPermissionGroupChangeEvent(player.getName(), getCommandType().toString(),
                                    PlayerPermissionGroupChangeEvent.Type.removed);

                    Bukkit.getServer().getPluginManager().callEvent(event);
                }

            } else {

                // check to see if they're already in this group
                boolean playerInGroup = playerInPermissionGroup(player.getName(), groupSettings.getPermissionsGroup());
                if (!playerInGroup) {

                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("Adding group: " + player.getName() + ", " + groupSettings.getPermissionsGroup());
                    }

                    // they're not, so lets add them
                    boolean added =
                            VaultIntegration.getInstance().getPermission()
                                    .playerAddGroup(world, player.getName(), groupSettings.getPermissionsGroup());

                    if (added) {

                        // create the group change event and fire it off
                        PlayerPermissionGroupChangeEvent event =
                                new PlayerPermissionGroupChangeEvent(player.getName(), getCommandType().toString(),
                                        PlayerPermissionGroupChangeEvent.Type.added);

                        Bukkit.getServer().getPluginManager().callEvent(event);
                    }

                } else {

                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("Remaining in group: " + player.getName() + ", "
                                + groupSettings.getPermissionsGroup());
                    }
                }
            }
        }
    }

    private boolean playerInPermissionGroup(String playerName, String groupName) {

        final String nullString = null;
        String[] groupList = VaultIntegration.getInstance().getPermission().getPlayerGroups(nullString, playerName);
        for (String group : groupList) {
            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                logger.info("playerGroup: " + group);
            }
            if (group.equalsIgnoreCase(groupName)) {
                if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                    logger.info("Matched playerGroup: " + group);
                }
                return true;
            }
        }

        return false;
    }
}
