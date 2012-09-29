package com.netprogs.minecraft.plugins.social.command.perk;

import java.util.List;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommandType;
import com.netprogs.minecraft.plugins.social.command.exception.ArgumentsMissingException;
import com.netprogs.minecraft.plugins.social.command.exception.InvalidPermissionsException;
import com.netprogs.minecraft.plugins.social.command.exception.PlayerNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.exception.PlayerNotOnlineException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotPlayerException;
import com.netprogs.minecraft.plugins.social.command.help.HelpBook;
import com.netprogs.minecraft.plugins.social.command.help.HelpMessage;
import com.netprogs.minecraft.plugins.social.command.help.HelpSegment;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.perk.TeleportSettings;
import com.netprogs.minecraft.plugins.social.storage.data.perk.PersonTeleportSettings;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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

/**
 * <pre>
 * Perk: Teleport
 * 
 * Allow members to use teleport commands:
 * 
 *  /s teleport             Teleports to thier home.
 *  /s teleport sethome     Sets their teleport home location.
 *  /s teleport tm <player> Teleports the <player> to the command caller.
 *  /s teleport ty <player> Teleports the command caller to the <player>.
 * 
 * </pre>
 */
public class CommandTeleport extends PerkCommand<TeleportSettings, PersonTeleportSettings> {

    public CommandTeleport() {
        super(SocialNetworkCommandType.teleport);
    }

    @Override
    public boolean run(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            InvalidPermissionsException, SenderNotPlayerException, PlayerNotInNetworkException,
            SenderNotInNetworkException, PlayerNotOnlineException {

        // verify that the sender is actually a player
        if (!(sender instanceof Player)) {
            throw new SenderNotPlayerException();
        }

        // check permissions
        if (!hasCommandPermission(sender)) {
            throw new InvalidPermissionsException();
        }

        Player player = (Player) sender;

        // make sure the sender is in the network
        SocialPerson playerPerson = SocialNetworkPlugin.getStorage().getPerson(player.getName());
        if (playerPerson == null) {
            throw new SenderNotInNetworkException();
        }

        // attempt to run the secondary commands first
        boolean commandHandled = handleSecondaryCommands(player, playerPerson, arguments);

        // there were none, so lets try the main friend request command
        if (!commandHandled) {

            // check arguments
            if (arguments.size() != 0) {
                throw new ArgumentsMissingException();
            }

            PersonTeleportSettings personSettings = getPersonPerkSettings(playerPerson);

            int locX = personSettings.getBlockX();
            int locY = personSettings.getBlockY();
            int locZ = personSettings.getBlockZ();
            String world = personSettings.getWorld();

            SocialNetworkPlugin
                    .log("CommandTeleportHome. Load: [X, Y, Z] = [" + locX + ", " + locY + ", " + locZ + "]");

            if (locX > 0 && locY > 0 && locZ > 0 && world != null) {

                Location location = new Location(Bukkit.getWorld(world), locX, locY, locZ);
                player.teleport(location);

            } else {

                MessageUtil.sendMessage(player, "social.perk.teleport.sethome.required.sender", ChatColor.GREEN);
            }
        }

        return true;
    }

    private boolean handleSecondaryCommands(Player player, SocialPerson playerPerson, List<String> arguments)
            throws ArgumentsMissingException, PlayerNotInNetworkException, PlayerNotOnlineException,
            SenderNotInNetworkException {

        // need to make sure we have at least one argument (the secondary command)
        if (arguments.size() >= 1) {

            if (arguments.get(0).equals("sethome")) {

                PersonTeleportSettings personSettings = getPersonPerkSettings(playerPerson);

                personSettings.setBlockX(player.getLocation().getBlockX());
                personSettings.setBlockY(player.getLocation().getBlockY());
                personSettings.setBlockZ(player.getLocation().getBlockZ());
                personSettings.setWorld("world");

                SocialNetworkPlugin.log("CommandTeleportHome. Save: [X, Y, Z] = [" + player.getLocation().getBlockX()
                        + ", " + player.getLocation().getBlockX() + ", " + player.getLocation().getBlockX() + "]");

                // save the changes
                savePersonPerkSettings(playerPerson, personSettings);

                // send the message
                MessageUtil.sendMessage(player, "social.perk.teleport.sethome.completed.sender", ChatColor.GREEN);

                return true;
            } else if (arguments.get(0).equals("tm")) {

                // check arguments
                if (arguments.size() != 2) {
                    throw new ArgumentsMissingException();
                }

                // make sure the teleport to person is in the network
                String teleportToMePersonName = arguments.get(1);
                SocialPerson teleportToMePerson = SocialNetworkPlugin.getStorage().getPerson(teleportToMePersonName);
                if (teleportToMePerson == null) {
                    throw new PlayerNotInNetworkException(teleportToMePersonName);
                }

                // make sure the teleport to person is online
                Player teleportToMePlayer = Bukkit.getServer().getPlayer(teleportToMePerson.getName());
                if (teleportToMePlayer == null) {
                    throw new PlayerNotOnlineException(teleportToMePerson.getName());
                }

                // make sure the person is in your social groups
                TeleportSettings teleportSettings = getPerkSettings(playerPerson, teleportToMePerson);
                if (teleportSettings == null) {
                    MessageUtil.sendPersonNotInPerksMessage(player, teleportToMePersonName);
                    return true;
                }

                // If I have them on ignore...
                // Check to see if the person they're trying to teleport is on their own ignore list
                if (playerPerson.isOnIgnore(teleportToMePerson)) {
                    MessageUtil.sendPlayerIgnoredMessage(player, teleportToMePerson.getName());
                    return true;
                }

                // If they have me on ignore...
                // Check to see if the person they're trying to teleport has them on their ignore list
                if (teleportToMePerson.isOnIgnore(playerPerson)) {
                    MessageUtil.sendSenderIgnoredMessage(player, teleportToMePerson.getName());
                    return true;
                }

                // finally, make them teleport to me
                teleportToMePlayer.teleport(player);

                SocialNetworkPlugin.log("Teleporting " + teleportToMePlayer.getName() + " to " + player.getName());

                return true;

            } else if (arguments.get(0).equals("ty")) {

                // check arguments
                if (arguments.size() != 2) {
                    throw new ArgumentsMissingException();
                }

                // make sure the A player is in the network
                String teleportToPersonName = arguments.get(1);
                SocialPerson teleportToPerson = SocialNetworkPlugin.getStorage().getPerson(teleportToPersonName);
                if (teleportToPerson == null) {
                    throw new PlayerNotInNetworkException(teleportToPersonName);
                }

                // make sure the teleport to person is online
                Player teleportToPlayer = Bukkit.getServer().getPlayer(teleportToPerson.getName());
                if (teleportToPlayer == null) {
                    throw new PlayerNotOnlineException(teleportToPerson.getName());
                }

                // make sure the person is in your social groups
                TeleportSettings teleportSettings = getPerkSettings(playerPerson, teleportToPerson);
                if (teleportSettings == null) {
                    MessageUtil.sendPersonNotInGroupMessage(player, teleportToPersonName);
                    return true;
                }

                // If I have them on ignore...
                // Check to see if the person they're trying to teleport is on their own ignore list
                if (playerPerson.isOnIgnore(teleportToPerson)) {
                    MessageUtil.sendPlayerIgnoredMessage(player, teleportToPerson.getName());
                    return true;
                }

                // If they have me on ignore...
                // Check to see if the person they're trying to teleport has them on their ignore list
                if (teleportToPerson.isOnIgnore(playerPerson)) {
                    MessageUtil.sendSenderIgnoredMessage(player, teleportToPerson.getName());
                    return true;
                }

                SocialNetworkPlugin.log("Teleporting " + player.getName() + " to " + teleportToPlayer.getName());

                // finally, teleport to them
                player.teleport(teleportToPlayer);

                return true;
            }
        }

        return false;
    }

    /**
     * Used to determine if the particular call being made should be pre-processed (check timer & pre-auth cost).
     * @param player
     * @param commandArguments
     * @return
     */
    @Override
    public boolean allowPreProcessPerkCommand(Player player, List<String> commandArguments) {
        return true;
    }

    /**
     * Used to determine if the particular call being made should be post-processed (set timer & charge cost).
     * @param player
     * @param commandArguments
     * @return
     */
    @Override
    public boolean allowPostProcessPerkCommand(Player player, List<String> commandArguments) {
        return true;
    }

    /**
     * Obtain the settings for this perk using the executing person and any arguments they're using. If you return NULL
     * from this call, the cooldown and perUseCosts will not be applied for this execution. If you want to handle them
     * yourself, you can return null also.
     * @param player
     * @param commandArguments
     * @return A ISocialNetworkSettings instance or NULL if one could not be obtained.
     * @throws ArgumentsMissingException
     * @throws PlayerNotInNetworkException
     */
    @Override
    public TeleportSettings getProcessPerkSettings(SocialPerson person, List<String> commandArguments) {

        // this command requires processing when teleporting players to/from the other
        if (commandArguments.size() > 0
                && (commandArguments.get(0).equals("tm") || commandArguments.get(0).equals("ty"))) {

            // check arguments
            if (commandArguments.size() == 2) {

                // make sure the teleport to person is in the network
                String teleportToMePersonName = commandArguments.get(1);
                SocialPerson teleportToMePerson = SocialNetworkPlugin.getStorage().getPerson(teleportToMePersonName);
                if (teleportToMePerson != null) {
                    return getPerkSettings(person, teleportToMePerson);
                }
            }

        } else if (commandArguments.size() == 0
                || (commandArguments.size() > 0 && commandArguments.get(0).equals("sethome"))) {

            // If were here, they're trying to use commands that require pre/post processing, but do not use another
            // player. In this case, we're going to have to use the priority list alone to obtain the group settings.
            return getPerkSettings(person);
        }

        // don't bother getting the system to process anything using the settings
        return null;
    }

    @Override
    public HelpSegment help() {

        HelpSegment helpSegment = new HelpSegment(getCommandType());
        ResourcesConfig config = SocialNetworkPlugin.getResources();

        HelpMessage mainCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), null, null,
                        config.getResource("social.perk.teleport.help"));
        helpSegment.addEntry(mainCommand);

        HelpMessage setHomeCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "sethome", null,
                        config.getResource("social.perk.teleport.help.sethome"));
        helpSegment.addEntry(setHomeCommand);

        HelpMessage tyCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "ty", "<player>",
                        config.getResource("social.perk.teleport.help.ty"));
        helpSegment.addEntry(tyCommand);

        HelpMessage tmCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "tm", "<player>",
                        config.getResource("social.perk.teleport.help.tm"));
        helpSegment.addEntry(tmCommand);

        return helpSegment;
    }
}
