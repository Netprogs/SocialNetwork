package com.netprogs.minecraft.plugins.social.command.perk;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import com.netprogs.minecraft.plugins.social.command.util.MessageParameter;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.perk.StickySettings;
import com.netprogs.minecraft.plugins.social.storage.data.Sticky;
import com.netprogs.minecraft.plugins.social.storage.data.perk.IPersonPerkSettings;

import org.bukkit.ChatColor;
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
 * Perk: Sticky
 * 
 * Allow members to use sticky commands:
 * 
 *  /s sticky <player>  <message>   Sends a "sticky note" to another player. These are stored and can be read later.
 *  /s sticky list                  Lists all available stickies, number by player.
 *  /s sticky read      <player>    Reads all the stickies from the player.
 *  /s sticky delete    <player>    Deletes the stickies from the player.
 * 
 * </pre>
 */
public class CommandSticky extends PerkCommand<StickySettings, IPersonPerkSettings> {

    public CommandSticky() {
        super(SocialNetworkCommandType.sticky);
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
            if (arguments.size() < 2) {
                throw new ArgumentsMissingException();
            }

            // get our arguments
            String sendPlayerName = arguments.get(0);

            // the message is everything else
            String message = "";
            for (int i = 1; i < arguments.size(); i++) {
                message += arguments.get(i) + " ";
            }

            // make sure the player is in the network
            SocialPerson sendToPerson = SocialNetworkPlugin.getStorage().getPerson(sendPlayerName);
            if (sendToPerson == null) {
                throw new PlayerNotInNetworkException(sendPlayerName);
            }

            // make sure the person is in your social groups
            StickySettings stickySettings = getPerkSettings(playerPerson, sendToPerson);
            if (stickySettings == null) {
                MessageUtil.sendPersonNotInPerksMessage(player, sendToPerson.getName());
                return false;
            }

            // If I have them on ignore...
            // Check to see if the person they're trying to contact is on their own ignore list
            if (playerPerson.isOnIgnore(sendToPerson)) {
                MessageUtil.sendPlayerIgnoredMessage(player, sendToPerson.getName());
                return false;
            }

            // If they have me on ignore...
            // Check to see if the person they're trying to contact has them on their ignore list
            if (sendToPerson.isOnIgnore(playerPerson)) {
                MessageUtil.sendSenderIgnoredMessage(player, sendToPerson.getName());
                return false;
            }

            // check to see if we've reached out limit on how many stickies we can send them
            if (stickySettings != null) {

                List<Sticky> stickies = sendToPerson.getMessagesFrom(playerPerson, Sticky.class);
                if (stickies.size() >= stickySettings.getMaximumNumber()) {

                    MessageUtil.sendMessage(player, "social.perk.sticky.limitReached", ChatColor.RED,
                            new MessageParameter("<player>", sendToPerson.getName(), ChatColor.AQUA));

                    return false;
                }
            }

            // Okay, lets send them a sticky
            Sticky sticky = new Sticky(playerPerson.getName(), message);

            // and throw it onto the message queue
            sendToPerson.addMessage(playerPerson, sticky);
            SocialNetworkPlugin.getStorage().savePerson(sendToPerson);

            // tell them it's been sent
            MessageUtil.sendMessage(player, "social.perk.sticky.send.completed.sender", ChatColor.GREEN);
        }

        return true;
    }

    private boolean handleSecondaryCommands(Player player, SocialPerson playerPerson, List<String> arguments)
            throws ArgumentsMissingException, PlayerNotInNetworkException, PlayerNotOnlineException {

        // need to make sure we have at least one argument (the secondary command)
        if (arguments.size() >= 1) {

            if (arguments.get(0).equals("list")) {

                // send a header
                MessageUtil.sendHeaderMessage(player, "social.perk.sticky.list.header.sender");

                // send a response for each person with their message count
                Set<String> messagePlayers = playerPerson.getMessagePlayers(Sticky.class);
                for (String playerName : messagePlayers) {

                    int count = playerPerson.getMessageCountFrom(playerName, Sticky.class);

                    MessageParameter numRequests =
                            new MessageParameter("<number>", Integer.toString(count), ChatColor.GOLD);
                    MessageParameter messageName = new MessageParameter("<player>", playerName, ChatColor.AQUA);

                    List<MessageParameter> requestParameters = new ArrayList<MessageParameter>();
                    requestParameters.add(numRequests);
                    requestParameters.add(messageName);

                    MessageUtil.sendMessage(player, "social.perk.sticky.list.item.sender", ChatColor.GREEN,
                            requestParameters);
                }

                if (messagePlayers.size() == 0) {
                    MessageUtil.sendMessage(player, "social.perk.sticky.none.sender", ChatColor.GREEN);
                }

                return true;

            } else if (arguments.get(0).equals("read")) {

                // check arguments
                if (arguments.size() != 2) {
                    throw new ArgumentsMissingException();
                }

                String messagePlayerName = arguments.get(1);

                // make sure the player is in the network
                SocialPerson messagePerson = SocialNetworkPlugin.getStorage().getPerson(messagePlayerName);
                if (messagePerson == null) {
                    throw new PlayerNotInNetworkException(messagePlayerName);
                }

                // now read their messages
                List<Sticky> stickies = playerPerson.getMessagesFrom(messagePerson, Sticky.class);

                // send a header
                MessageUtil.sendHeaderMessage(player, "social.perk.sticky.read.header.sender");

                // send the stickies
                for (Sticky playerSticky : stickies) {

                    MessageParameter messageName =
                            new MessageParameter("<player>", playerSticky.getPlayerName(), ChatColor.AQUA);

                    MessageParameter message =
                            new MessageParameter("<message>", playerSticky.getMessage(), ChatColor.GREEN);

                    List<MessageParameter> requestParameters = new ArrayList<MessageParameter>();
                    requestParameters.add(messageName);
                    requestParameters.add(message);

                    MessageUtil.sendMessage(player, "social.perk.sticky.read.item.sender", ChatColor.GREEN,
                            requestParameters);
                }

                if (stickies.size() == 0) {
                    MessageUtil.sendMessage(player, "social.perk.sticky.none.sender", ChatColor.GREEN);
                }

                return true;

            } else if (arguments.get(0).equals("delete")) {

                // check arguments
                if (arguments.size() != 2) {
                    throw new ArgumentsMissingException();
                }

                String messagePlayerName = arguments.get(1);

                // make sure the player is in the network
                SocialPerson messagePerson = SocialNetworkPlugin.getStorage().getPerson(messagePlayerName);
                if (messagePerson == null) {
                    throw new PlayerNotInNetworkException(messagePlayerName);
                }

                // get their messages and delete each one
                List<Sticky> stickies = playerPerson.getMessagesFrom(messagePerson, Sticky.class);
                for (Sticky playerSticky : stickies) {
                    playerPerson.removeMessage(messagePerson, playerSticky);
                }

                // save the person
                SocialNetworkPlugin.getStorage().savePerson(playerPerson);

                // tell them we're done
                MessageUtil.sendMessage(player, "social.perk.sticky.delete.completed.sender", ChatColor.GREEN,
                        new MessageParameter("<player>", messagePerson.getName(), ChatColor.AQUA));

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

        // only charge/timer for sending
        if (commandArguments.size() == 2) {
            if (!commandArguments.get(0).equals("list") && !commandArguments.get(0).equals("read")
                    && !commandArguments.get(0).equals("delete")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Used to determine if the particular call being made should be post-processed (set timer & charge cost).
     * @param player
     * @param commandArguments
     * @return
     */
    @Override
    public boolean allowPostProcessPerkCommand(Player player, List<String> commandArguments) {

        // only charge/timer for sending
        if (commandArguments.size() == 2) {
            if (!commandArguments.get(0).equals("list") && !commandArguments.get(0).equals("read")
                    && !commandArguments.get(0).equals("delete")) {
                return true;
            }
        }

        return false;
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
    public StickySettings getProcessPerkSettings(SocialPerson person, List<String> commandArguments) {

        // this command requires processing when sending a player an actual sticky
        String command = commandArguments.get(0);
        if (!command.equals("list") && !command.equals("read") && !command.equals("delete")) {

            // if here, the first command should be the receiver name
            String sendPlayerName = command;

            SocialPerson sendToPerson = SocialNetworkPlugin.getStorage().getPerson(sendPlayerName);
            if (sendToPerson != null) {
                return getPerkSettings(person, sendToPerson);
            }
        }

        return null;
    }

    /**
     * Sticky Help:
     * /s sticky <player> <message> Sends a "sticky note" to another player.
     * /s sticky list Lists all available stickies by player.
     * /s sticky read <player> Reads all the stickies from the player.
     * /s sticky delete <player> Deletes the stickies from the player.
     */
    @Override
    public HelpSegment help() {

        HelpSegment helpSegment = new HelpSegment(getCommandType());
        ResourcesConfig config = SocialNetworkPlugin.getResources();

        HelpMessage mainCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), null, "<player> <message>",
                        config.getResource("social.perk.sticky.help.send"));
        helpSegment.addEntry(mainCommand);

        HelpMessage listCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "list", null,
                        config.getResource("social.perk.sticky.help.list"));
        helpSegment.addEntry(listCommand);

        HelpMessage readCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "read", "<player>",
                        config.getResource("social.perk.sticky.help.read"));
        helpSegment.addEntry(readCommand);

        HelpMessage deleteCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "delete", "<player>",
                        config.getResource("social.perk.sticky.help.delete"));
        helpSegment.addEntry(deleteCommand);

        return helpSegment;
    }
}
