package com.netprogs.minecraft.plugins.social.command.group;

import java.util.List;
import java.util.logging.Logger;

import com.netprogs.minecraft.plugins.social.SocialGroupMember;
import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommand;
import com.netprogs.minecraft.plugins.social.command.exception.ArgumentsMissingException;
import com.netprogs.minecraft.plugins.social.command.exception.InvalidPermissionsException;
import com.netprogs.minecraft.plugins.social.command.exception.PlayerNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotPlayerException;
import com.netprogs.minecraft.plugins.social.command.util.MessageParameter;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.command.util.TimerUtil;
import com.netprogs.minecraft.plugins.social.config.PluginConfig;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.ISocialNetworkSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.GroupSettings;
import com.netprogs.minecraft.plugins.social.integration.VaultIntegration;
import com.netprogs.minecraft.plugins.social.storage.SocialNetwork;

import org.bukkit.Bukkit;
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

public abstract class GroupCommand<T extends ISocialNetworkSettings> extends SocialNetworkCommand<T> {

    private final Logger logger = Logger.getLogger("Minecraft");

    protected GroupCommand(ICommandType commandType) {
        super(commandType);
    }

    @Override
    public boolean run(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            InvalidPermissionsException, SenderNotInNetworkException, SenderNotPlayerException,
            PlayerNotInNetworkException {

        // verify that the sender is actually a player
        verifySenderAsPlayer(sender);

        // check arguments
        if (arguments.size() == 0) {
            throw new ArgumentsMissingException();
        }

        // check permissions
        if (!hasCommandPermission(sender)) {
            throw new InvalidPermissionsException();
        }

        Player player = (Player) sender;

        // get the social network data
        SocialNetwork socialConfig = SocialNetwork.getInstance();

        // make sure the sender is in the network
        SocialPerson playerPerson = socialConfig.getPerson(player.getName());
        if (playerPerson == null) {
            throw new SenderNotInNetworkException();
        }

        // attempt to run the secondary commands first
        boolean commandHandled = handleSecondaryCommands(player, playerPerson, arguments);

        // there were none, so lets try the main request command
        if (!commandHandled) {
            return handleSenderCommands(player, playerPerson, arguments);
        }

        return true;
    }

    /**
     * Handles the commands from the sender of the group request.
     * @param player
     * @param playerPerson
     * @param arguments
     * @return
     * @throws ArgumentsMissingException
     * @throws PlayerNotInNetworkException
     */
    protected boolean handleSenderCommands(Player player, SocialPerson playerPerson, List<String> arguments)
            throws ArgumentsMissingException, PlayerNotInNetworkException {

        if (arguments.get(0).equals("request")) {
            handleRequest(player, playerPerson, arguments);
            return true;
        }

        // return false;
        throw new ArgumentsMissingException();
    }

    /**
     * Handles all secondary group commands (accept/reject/ignore/remove).
     * @param player The player instance of the person receiving the request and running these commands.
     * @param receiverPerson The person who is receiving the request and is now using these commands to handle it.
     * @param arguments The additional arguments passed into these commands.
     * @return True if we've found and handled a command. False otherwise.
     * @throws ArgumentsMissingException
     * @throws InvalidPermissionsException
     * @throws PlayerNotInNetworkException
     */
    protected boolean handleSecondaryCommands(Player player, SocialPerson receiverPerson, List<String> arguments)
            throws ArgumentsMissingException, InvalidPermissionsException, PlayerNotInNetworkException {

        // If were here, it means we're receiving a request from someone else and running commands to handle that

        if (arguments.get(0).equals("accept")) {
            handleAccept(player, receiverPerson, arguments);
            return true;

        } else if (arguments.get(0).equals("reject")) {
            handleReject(player, receiverPerson, arguments, false);
            return true;

        } else if (arguments.get(0).equals("ignore")) {
            handleReject(player, receiverPerson, arguments, true);
            return true;

        } else if (arguments.get(0).equals("remove")) {
            handleRemove(player, receiverPerson, arguments);
            return true;

        } else if (arguments.get(0).equals("list")) {
            handleList(player, receiverPerson, arguments);
            return true;
        }

        return false;
    }

    /**
     * Sends out a group request.
     * @param player
     * @param receiverPerson
     * @param arguments
     * @throws ArgumentsMissingException
     * @throws PlayerNotInNetworkException
     */
    protected boolean handleRequest(Player player, SocialPerson playerPerson, List<String> arguments)
            throws ArgumentsMissingException, PlayerNotInNetworkException {

        // check to make sure we have a player name given
        if (arguments.size() < 2) {
            throw new ArgumentsMissingException();
        }

        // get the social network data
        SocialNetwork socialConfig = SocialNetwork.getInstance();

        // if we're here, we want to process a group request
        String personName = arguments.get(1);

        // make sure they can't send things to themselves
        if (player.getName().equalsIgnoreCase((personName))) {
            MessageUtil.sendMessage(player, "social.error.cannotSendSelf.sender", ChatColor.RED);
            return false;
        }

        // check to see if the person is part of the network
        SocialPerson groupPerson = socialConfig.getPerson(personName);
        if (groupPerson != null) {

            // If I have them on ignore...
            // Check to see if the person they're sending a request to is on their ignore list
            if (playerPerson.isOnIgnore(groupPerson.getName())) {
                MessageUtil.sendPlayerIgnoredMessage(player, groupPerson.getName());
                return false;
            }

            // If they have me on ignore...
            // Check to see if the person having a request sent to has the sender on ignore
            if (groupPerson.isOnIgnore(playerPerson.getName())) {
                MessageUtil.sendSenderIgnoredMessage(player, groupPerson.getName());
                return false;
            }

            // Check to see if we're allowed to send a request. Error messages will be handled in the method.
            boolean allowSendRequest = allowSendRequest(playerPerson, groupPerson);
            if (!allowSendRequest) {
                // we're letting the sub-class handle player messages
                return false;
            }

            // now check to see if requested person is already in this group
            boolean alreadyInList = personInGroup(playerPerson, groupPerson);
            if (alreadyInList) {
                MessageUtil.sendMessage(player, "social." + getCommandType() + ".request.alreadyInGroup.sender",
                        ChatColor.GREEN, new MessageParameter("<player>", groupPerson.getName(), ChatColor.AQUA));
                return false;
            }

            // send the person request to the queue for the group
            boolean requestSent = groupPerson.addRequest(playerPerson, getCommandType());

            // if it was sent, tell the player
            if (requestSent) {

                // save the person
                socialConfig.savePerson(groupPerson);

                // tell the player
                MessageUtil.sendMessage(player, "social." + getCommandType() + ".request.completed.sender",
                        ChatColor.GREEN, new MessageParameter("<player>", groupPerson.getName(), ChatColor.AQUA));

                // tell the person
                Player groupPlayer = getPlayer(groupPerson.getName());
                if (groupPlayer != null) {
                    String resource = "social." + getCommandType() + ".request.completed.player";
                    MessageUtil.sendMessage(groupPlayer, resource, ChatColor.GREEN, new MessageParameter("<player>",
                            player.getName(), ChatColor.AQUA));
                }

            } else {

                // it wasn't sent because the player already has one
                String resource = "social." + getCommandType() + ".request.alreadyRequested.sender";
                MessageUtil.sendMessage(player, resource, ChatColor.GREEN,
                        new MessageParameter("<player>", groupPerson.getName(), ChatColor.AQUA));

                return false;
            }

        } else {

            // player isn't in the network, report it
            throw new PlayerNotInNetworkException(personName);
        }

        return true;
    }

    /**
     * Accept a request. Both parties are notified.
     * @param player
     * @param receiverPerson
     * @param arguments
     * @throws ArgumentsMissingException
     * @throws PlayerNotInNetworkException
     */
    protected void handleAccept(Player player, SocialPerson receiverPerson, List<String> arguments)
            throws ArgumentsMissingException, PlayerNotInNetworkException {

        // check to make sure we have a player name given
        if (arguments.size() < 2) {
            throw new ArgumentsMissingException();
        }

        // get the social network data
        SocialNetwork socialConfig = SocialNetwork.getInstance();

        String personName = arguments.get(1);

        // make sure the person given is in the network and has sent us a request
        SocialPerson senderPerson = socialConfig.getPerson(personName);
        if (senderPerson != null) {

            // Check to see if there is a request waiting from the sender person.
            if (!receiverPerson.hasRequest(senderPerson, getCommandType())) {
                MessageUtil.sendMessage(player, "social." + getCommandType() + ".accept.playerNoRequest",
                        ChatColor.GREEN, new MessageParameter("<player>", senderPerson.getName(), ChatColor.AQUA));
                return;
            }

            // Check to see if we're allowed to accept a request.
            boolean allowAcceptRequest = allowAcceptRequest(receiverPerson, senderPerson);
            if (!allowAcceptRequest) {

                // we're letting the sub-class handle player messages and will just remove the request from the queue
                receiverPerson.removeRequest(senderPerson, getCommandType());
                socialConfig.savePerson(receiverPerson);
                return;
            }

            // handle the accept
            boolean alreadyInList = personInGroup(receiverPerson, senderPerson);
            if (!alreadyInList) {
                handleAccept(receiverPerson, senderPerson);
            }

            // now remove the request from the queue
            receiverPerson.removeRequest(senderPerson, getCommandType());

            // and save both players
            socialConfig.savePerson(receiverPerson);
            socialConfig.savePerson(senderPerson);

            // send a notice to the person to tell them you've accepted
            Player senderPlayer = getPlayer(senderPerson.getName());
            if (senderPlayer != null) {
                MessageUtil.sendMessage(senderPlayer, "social." + getCommandType() + ".accept.completed.player",
                        ChatColor.GREEN, new MessageParameter("<player>", receiverPerson.getName(), ChatColor.AQUA));
            }

            // send a notice to the player to tell them we've processed the accepted request
            MessageUtil.sendMessage(player, "social." + getCommandType() + ".accept.completed.sender", ChatColor.GREEN,
                    new MessageParameter("<player>", senderPerson.getName(), ChatColor.AQUA));

        } else {

            // can't find the person in the network
            throw new PlayerNotInNetworkException(personName);
        }
    }

    /**
     * Reject a request. Both parties are notified if silent is false. If true, only the sender gets notified.
     * @param player
     * @param playerPerson
     * @param arguments
     * @param silent
     * @throws ArgumentsMissingException
     * @throws PlayerNotInNetworkException
     */
    protected void handleReject(Player player, SocialPerson receiverPerson, List<String> arguments, boolean silent)
            throws ArgumentsMissingException, PlayerNotInNetworkException {

        // check to make sure we have a player name given
        if (arguments.size() < 2) {
            throw new ArgumentsMissingException();
        }

        // get the social network data
        SocialNetwork socialConfig = SocialNetwork.getInstance();

        String personName = arguments.get(1);

        // make sure the person given is in the network
        SocialPerson senderPerson = socialConfig.getPerson(personName);
        if (senderPerson != null) {

            // check to see if there is a request waiting from the sender person
            if (!receiverPerson.hasRequest(senderPerson, getCommandType())) {

                // tell the sender there was no request from that person
                MessageUtil.sendMessage(player, "social." + getCommandType() + ".accept.playerNoRequest",
                        ChatColor.GREEN, new MessageParameter("<player>", senderPerson.getName(), ChatColor.AQUA));

                return;
            }

            // handle the rejection
            handleReject(receiverPerson, senderPerson);

            // remove the request from the players queue
            receiverPerson.removeRequest(senderPerson, getCommandType());
            socialConfig.savePerson(receiverPerson);

            // send a notice to the person to tell them you've rejected
            if (!silent) {

                Player senderPlayer = getPlayer(senderPerson.getName());
                if (senderPlayer != null) {
                    String resource = "social." + getCommandType() + ".reject.completed.player";
                    MessageUtil.sendMessage(senderPlayer, resource, ChatColor.GREEN, new MessageParameter("<player>",
                            receiverPerson.getName(), ChatColor.AQUA));
                }
            }

            // send a notice to the player to tell them we've processed the rejected request
            MessageUtil.sendMessage(player, "social." + getCommandType() + ".reject.completed.sender", ChatColor.GREEN,
                    new MessageParameter("<player>", senderPerson.getName(), ChatColor.AQUA));

        } else {

            // can't find the person in the network
            throw new PlayerNotInNetworkException(personName);
        }
    }

    /**
     * Removes a person from the group list. Also removes you from theirs.
     * @param player
     * @param playerPerson
     * @param arguments
     * @throws ArgumentsMissingException
     * @throws PlayerNotInNetworkException
     */
    protected void handleRemove(Player player, SocialPerson playerPerson, List<String> arguments)
            throws ArgumentsMissingException, PlayerNotInNetworkException {

        // check to make sure we have a player name given
        if (arguments.size() < 2) {
            throw new ArgumentsMissingException();
        }

        // get the social network data
        SocialNetwork socialConfig = SocialNetwork.getInstance();

        String personName = arguments.get(1);

        // make sure the person given is in the network
        SocialPerson removePerson = socialConfig.getPerson(personName);
        if (removePerson != null) {

            // check to make sure they're in the group
            if (!personInGroup(playerPerson, removePerson)) {

                MessageUtil.sendMessage(player, "social." + getCommandType() + ".notInGroup.sender", ChatColor.GREEN,
                        new MessageParameter("<player>", removePerson.getName(), ChatColor.AQUA));
                return;
            }

            // handle the actual removal
            handleRemove(playerPerson, removePerson);

            // save the changes
            socialConfig.savePerson(playerPerson);
            socialConfig.savePerson(removePerson);

            // send a notice to the player to tell them we've processed the remove
            MessageUtil.sendMessage(player, "social." + getCommandType() + ".remove.completed.sender", ChatColor.GREEN,
                    new MessageParameter("<player>", removePerson.getName(), ChatColor.AQUA));

        } else {

            // can't find the person in the network
            throw new PlayerNotInNetworkException(personName);
        }
    }

    /**
     * Produces a list of all their current people in the group.
     * @param player
     * @param playerPerson
     * @param arguments
     * @throws ArgumentsMissingException
     * @throws PlayerNotInNetworkException
     */
    protected void handleList(Player player, SocialPerson playerPerson, List<String> arguments)
            throws ArgumentsMissingException, PlayerNotInNetworkException {

        // pass off the rest of the work to the sub-classes
        handleList(player, playerPerson);
    }

    /**
     * Checks the cooldown of the provided command to make sure the user can use it.
     * @param playerPerson The person you wish to check a timer for.
     * @param playerPerson The command type to check a timer for.
     * @return
     */
    protected boolean checkCommandTimer(SocialPerson playerPerson, ICommandType commandType) {

        Player player = Bukkit.getServer().getPlayer(playerPerson.getName());
        if (player == null) {
            return false;
        }

        // Some commands will set a timer for their usage. We're going to check for that here based on the command type.
        // If no timer is set by this command, the timeRemaining will be 0.
        long timeRemaining = TimerUtil.commandOnTimer(playerPerson.getName(), commandType);
        if (timeRemaining > 0) {
            MessageUtil.sendMessage(player, "social." + commandType + ".cannotSendRequest.ontimer.sender",
                    ChatColor.GOLD, MessageUtil.createCoolDownFormatting(timeRemaining));
            return false;
        }

        return true;
    }

    /**
     * Checks the cost of the provided command to make sure the user can use it.
     * @param playerPerson The person you wish to check a timer for.
     * @return
     */
    protected boolean checkCommandCost(SocialPerson playerPerson) {

        Player player = Bukkit.getServer().getPlayer(playerPerson.getName());
        if (player == null) {
            return false;
        }

        // check to make sure we're only using group commands since perks don't support these payment scheme
        if (!(this instanceof GroupCommand)) {
            return false;
        }

        // check to see if they can afford the cost
        GroupSettings settings = (GroupSettings) getCommandSettings();
        double price = settings.getPerUseCost();
        boolean authorized = VaultIntegration.getInstance().preAuthCommandPurchase(player, price);
        if (!authorized) {
            return false;
        }

        return true;
    }

    /**
     * Checks to see if playerPerson is allowed to send a request to receiverPerson.
     * Called from the sender of the request to see if they are allowed to send a request for this group.
     * @param playerPerson The person sending the request.
     * @param receiverPerson The person who is receiving the request.
     * @return
     */
    protected boolean allowSendRequest(SocialPerson playerPerson, SocialPerson receiverPerson) {
        // override this in sub-class as needed
        return true;
    }

    /**
     * Checks to see if acceptPerson is still able to accept a request from the senderPerson.
     * Called when the request is received by the other person to determine if they are allowed to accept a request.
     * @param acceptPerson The person who is trying to accept the request.
     * @param senderPerson The person who sent them the request.
     * @return
     */
    protected boolean allowAcceptRequest(SocialPerson acceptPerson, SocialPerson senderPerson) {
        // override this in sub-class as needed
        return true;
    }

    /**
     * Handles the actual accept.
     * @param receiverPerson The person who is accepting the request.
     * @param senderPerson The person who sent the request.
     * @throws ArgumentsMissingException
     * @throws PlayerNotInNetworkException
     */
    protected void handleAccept(SocialPerson receiverPerson, SocialPerson senderPerson)
            throws ArgumentsMissingException, PlayerNotInNetworkException {

        // add sender to receiver list
        addPersonToGroup(receiverPerson, senderPerson);

        // add receiver to sender list
        addPersonToGroup(senderPerson, receiverPerson);
    }

    /**
     * Handles the actual reject.
     * @param receiverPerson The person who is rejecting the request.
     * @param senderPerson The person who sent the request.
     * @throws ArgumentsMissingException
     * @throws PlayerNotInNetworkException
     */
    protected void handleReject(SocialPerson receiverPerson, SocialPerson senderPerson)
            throws ArgumentsMissingException, PlayerNotInNetworkException {

        // This base implementation does not need to do anything during rejection
    }

    /**
     * Handles the actual remove.
     * @param playerPerson The person who requested the removal.
     * @param removePerson The person who is going to be removed.
     * @throws ArgumentsMissingException
     * @throws PlayerNotInNetworkException
     */
    protected void handleRemove(SocialPerson playerPerson, SocialPerson removePerson) throws ArgumentsMissingException,
            PlayerNotInNetworkException {

        // remove removePerson from playerPerson list
        removePersonFromGroup(playerPerson, removePerson);

        // remove playerPerson from removePerson list
        removePersonFromGroup(removePerson, playerPerson);
    }

    /**
     * Displays the group member list as obtained from the sub-class.
     * @param player The player to send response to.
     * @param memberList The list of people in the group.
     */
    protected void displayGroupList(Player player, List<? extends SocialGroupMember> memberList) {

        String onlineTag =
                PluginConfig.getInstance().getConfig(ResourcesConfig.class)
                        .getResource("social.list.tag.online.sender");

        String offlineTag =
                PluginConfig.getInstance().getConfig(ResourcesConfig.class)
                        .getResource("social.list.tag.offline.sender");

        // send the header
        MessageUtil.sendHeaderMessage(player, "social." + getCommandType() + ".list.header.sender");

        // check to see if they have anyone
        if (memberList.size() == 0) {
            MessageUtil.sendMessage(player, "social." + getCommandType() + ".list.noPeople.sender", ChatColor.RED);
            return;
        }

        String onlineList = ChatColor.GREEN + onlineTag + " " + ChatColor.WHITE;
        String offlineList = ChatColor.GRAY + offlineTag + " " + ChatColor.WHITE;

        // go through your entire list and display their names and online status
        for (SocialGroupMember currentPerson : memberList) {

            // check to see if they are online
            Player groupPlayer = getPlayer(currentPerson.getPlayerName());
            if (groupPlayer != null) {
                onlineList += currentPerson.getPlayerName() + ", ";
            } else {
                offlineList += currentPerson.getPlayerName() + ", ";
            }
        }

        if (onlineList.endsWith(", ")) {
            onlineList = onlineList.substring(0, onlineList.length() - 2);
        }
        if (offlineList.endsWith(", ")) {
            offlineList = offlineList.substring(0, offlineList.length() - 2);
        }

        player.sendMessage(onlineList);
        player.sendMessage(offlineList);
    }

    /**
     * Handles the listing of a persons group.
     * @param player The bukkit player instance.
     * @param playerPerson The person who sent the request.
     */
    protected abstract void handleList(Player player, SocialPerson playerPerson);

    /**
     * Checks to see if inGroupPerson is within playerPerson's group already.
     * Called from the sender of the request to make sure the receiver isn't already in the group.
     * @param playerPerson
     * @param inGroupPerson
     * @return
     */
    protected abstract boolean personInGroup(SocialPerson playerPerson, SocialPerson inGroupPerson);

    /**
     * Adds the addPerson to the playerPerson's group.
     * @param playerPerson
     * @param addPerson
     */
    protected abstract void addPersonToGroup(SocialPerson playerPerson, SocialPerson addPerson);

    /**
     * Removes the removePerson from the playerPerson's group.
     * @param playerPerson
     * @param removePerson
     */
    protected abstract void removePersonFromGroup(SocialPerson playerPerson, SocialPerson removePerson);
}
