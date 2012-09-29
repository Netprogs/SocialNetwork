package com.netprogs.minecraft.plugins.social.command.perk;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.SocialPerson.WaitState;
import com.netprogs.minecraft.plugins.social.command.IWaitCommand;
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
import com.netprogs.minecraft.plugins.social.config.settings.perk.GiftSettings;
import com.netprogs.minecraft.plugins.social.storage.IMessage;
import com.netprogs.minecraft.plugins.social.storage.data.Gift;
import com.netprogs.minecraft.plugins.social.storage.data.Gift.Type;
import com.netprogs.minecraft.plugins.social.storage.data.perk.IPersonPerkSettings;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

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
 * Perk: Gift
 * 
 * Allow members to send gifts to each other:
 * 
 *  /s gift cash    <amount> <player>   Sends the player the cash amount. 
 *  /s gift hand    <player>            Give the player the item in your hand. 
 *  /s gift yes                         Accept the gift being sent.
 *  /s gift no                          Don't accept the gift being sent.
 *  /s gift list                        List all the gifts available to you. Number, by player.
 *  /s gift open    <player>            Open all the gifts from the player. Will delete after opening.
 * 
 * </pre>
 */
public class CommandGift extends PerkCommand<GiftSettings, IPersonPerkSettings> implements IWaitCommand {

    public CommandGift() {
        super(SocialNetworkCommandType.gift);
    }

    @Override
    public boolean run(CommandSender sender, List<String> commandArguments) throws ArgumentsMissingException,
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

        // check arguments
        if (commandArguments.size() < 1 || commandArguments.size() > 4) {
            throw new ArgumentsMissingException();
        }

        // get the first parameter, this should be the command
        List<String> arguments = new ArrayList<String>(commandArguments);
        String command = arguments.remove(0);

        // dispatch our commands now
        if (command.equals("cash")) {

            return handlePrepareCashGift(player, playerPerson, arguments);

        } else if (command.equals("hand")) {

            return handlePrepareHandGift(player, playerPerson, arguments);

        } else if (command.equals("list")) {

            return handleListGifts(player, playerPerson, arguments);

        } else if (command.equals("read")) {

            return handleOpenGifts(player, playerPerson, arguments, true);

        } else if (command.equals("open")) {

            return handleOpenGifts(player, playerPerson, arguments, false);

        } else if (command.equals("yes") && playerPerson.getWaitState() != WaitState.notWaiting) {

            return handleSendGift(player, playerPerson, arguments);

        } else if (command.equals("no") && playerPerson.getWaitState() != WaitState.notWaiting) {

            return handleCancelGift(player, playerPerson, arguments);
        }

        // if we're still here, none of the attempted commands worked
        throw new ArgumentsMissingException();
    }

    /**
     * /s gift cash <amount> <player> Sends the player the cash amount.
     * @param playerPerson
     * @param arguments
     * @return
     */
    private boolean handlePrepareCashGift(Player player, SocialPerson playerPerson, List<String> arguments)
            throws ArgumentsMissingException, PlayerNotInNetworkException {

        if (arguments.size() != 2) {
            throw new ArgumentsMissingException();
        }

        String amountString = arguments.get(0);
        String receivePlayerName = arguments.get(1);

        // validate the numeric amount
        double amount = 0;
        try {
            amount = Double.parseDouble(amountString);
        } catch (NumberFormatException ne) {
            throw new ArgumentsMissingException();
        }

        // now check to see if they have enough money
        boolean auth = preAuthCashGift(player, amount);
        if (!auth) {
            // error messages handled above
            return false;
        }

        // check to see if we have a valid person to send to (will display messages if not)
        SocialPerson receivePerson = validateReceivePerson(player, playerPerson, receivePlayerName);
        if (receivePerson != null) {

            // check to see if we've reached our limit on how many gifts we can send them
            GiftSettings giftSettings = getPerkSettings(playerPerson, receivePerson);
            if (giftSettings != null) {

                List<Gift> gifts = receivePerson.getMessagesFrom(playerPerson, Gift.class);
                if (gifts.size() >= giftSettings.getMaximumNumber()) {

                    MessageUtil.sendMessage(player, "social.perk.gift.limitReached", ChatColor.RED,
                            new MessageParameter("<player>", receivePerson.getName(), ChatColor.AQUA));

                    return false;
                }
            }

            // create the gift message
            Gift giftData =
                    new Gift(Type.cash, playerPerson.getName(), receivePlayerName, player.getWorld().getName(), amount);

            NumberFormat formatter = new DecimalFormat("###.00");
            String itemDisplay = formatter.format(amount);

            // send out a verification message. Response is handled
            sendGiftVerification(player, playerPerson, receivePerson, itemDisplay, WaitState.waitCashGiftVerification,
                    giftData);

            // save their data in case they lose connection mid-way through a send
            SocialNetworkPlugin.getStorage().savePerson(receivePerson);

            return true;
        }

        // response messages already displayed
        return false;
    }

    /**
     * /s gift hand <player> Send the player the item in your hand.
     * @param playerPerson
     * @param arguments
     * @return
     * @throws PlayerNotInNetworkException
     */
    private boolean handlePrepareHandGift(Player player, SocialPerson playerPerson, List<String> arguments)
            throws ArgumentsMissingException, PlayerNotInNetworkException {

        if (arguments.size() != 1) {
            throw new ArgumentsMissingException();
        }

        String receivePlayerName = arguments.get(0);

        // get the item in their hand
        ItemStack itemStack = player.getItemInHand();
        if (itemStack != null) {

            // check to see if we have a valid person to send to (will display messages if not)
            SocialPerson receivePerson = validateReceivePerson(player, playerPerson, receivePlayerName);
            if (receivePerson != null) {

                // check to see if we've reached our limit on how many gifts we can send them
                GiftSettings giftSettings = getPerkSettings(playerPerson, receivePerson);
                if (giftSettings != null) {

                    List<Gift> gifts = receivePerson.getMessagesFrom(playerPerson, Gift.class);
                    if (gifts.size() >= giftSettings.getMaximumNumber()) {

                        MessageUtil.sendMessage(player, "social.perk.gift.limitReached", ChatColor.RED,
                                new MessageParameter("<player>", receivePerson.getName(), ChatColor.AQUA));

                        return false;
                    }
                }

                Gift giftData =
                        new Gift(Type.item, playerPerson.getName(), receivePlayerName, player.getWorld().getName(),
                                itemStack.getTypeId(), itemStack.getAmount());

                // take the item from their inventory, if they choose not to send it, we'll give it back then
                PlayerInventory inventory = player.getInventory();
                if (inventory.contains(itemStack)) {
                    inventory.removeItem(itemStack);
                } else {
                    return false;
                }

                // get the item display name
                Material material = Material.getMaterial(itemStack.getTypeId());
                String itemDisplay = itemStack.getAmount() + " " + material.name();

                // send out a verification message. Response is handled
                sendGiftVerification(player, playerPerson, receivePerson, itemDisplay,
                        WaitState.waitCashGiftVerification, giftData);

                // save their data in case they lose connection mid-way through a send
                SocialNetworkPlugin.getStorage().savePerson(receivePerson);

                return true;
            }
        }

        return false;
    }

    /**
     * /s gift list List all the gifts available to you. Number, by player.
     * @param playerPerson
     * @param arguments
     * @return
     */
    private boolean handleListGifts(Player player, SocialPerson playerPerson, List<String> arguments) {

        // send a header
        MessageUtil.sendHeaderMessage(player, "social.perk.gift.list.header.sender");

        // send a response for each person with their message count
        Set<String> messagePlayers = playerPerson.getMessagePlayers(Gift.class);
        for (String playerName : messagePlayers) {

            int count = playerPerson.getMessageCountFrom(playerName, Gift.class);

            MessageParameter numRequests = new MessageParameter("<number>", Integer.toString(count), ChatColor.GOLD);
            MessageParameter messageName = new MessageParameter("<player>", playerName, ChatColor.AQUA);

            List<MessageParameter> requestParameters = new ArrayList<MessageParameter>();
            requestParameters.add(numRequests);
            requestParameters.add(messageName);

            MessageUtil.sendMessage(player, "social.perk.gift.list.item.sender", ChatColor.GREEN, requestParameters);
        }

        if (messagePlayers.size() == 0) {
            MessageUtil.sendMessage(player, "social.perk.gift.none.sender", ChatColor.GREEN);
        }

        return true;
    }

    /**
     * /s gift open <player> Open all the gifts from the player. Will delete after opening if readOnly is false.
     * @param playerPerson
     * @param arguments
     * @param readOnly If you only want to display the items instead of opening and receiving them.
     * @return
     */
    private boolean handleOpenGifts(Player player, SocialPerson playerPerson, List<String> arguments, boolean readOnly)
            throws ArgumentsMissingException, PlayerNotInNetworkException {

        // check arguments
        if (arguments.size() != 1) {
            throw new ArgumentsMissingException();
        }

        String messagePlayerName = arguments.get(0);

        // make sure the player is in the network
        SocialPerson messagePerson = SocialNetworkPlugin.getStorage().getPerson(messagePlayerName);
        if (messagePerson == null) {
            throw new PlayerNotInNetworkException(messagePlayerName);
        }

        // now read their messages
        List<Gift> gifts = playerPerson.getMessagesFrom(messagePerson, Gift.class);

        // send a header
        MessageUtil.sendHeaderMessage(player, "social.perk.gift.read.header.sender");

        int openCount = 0;

        // send the gifts
        for (Gift playerGift : gifts) {

            // check to see if we should limit the gift to their world
            boolean sameWorld = true;
            if (!SocialNetworkPlugin.getSettings().isMultiWorldGiftsAllowed() && playerGift.getSenderWorld() != null) {
                sameWorld = player.getWorld().getName().equals(playerGift.getSenderWorld());
            }

            // create the display parameters
            MessageParameter messageName =
                    new MessageParameter("<player>", playerGift.getSenderPlayerName(), ChatColor.AQUA);

            MessageParameter messageWorld =
                    new MessageParameter("<world>", playerGift.getSenderWorld(), ChatColor.AQUA);

            MessageParameter giftMessage = null;

            if (playerGift.getType() == Type.cash) {

                // give the player the cash gift
                if (!readOnly && sameWorld) {
                    SocialNetworkPlugin.getVault().getEconomy().depositPlayer(player.getName(), playerGift.getAmount());
                }

                // get the display name of the item received
                NumberFormat formatter = new DecimalFormat("###.00");
                String itemDisplay = formatter.format(playerGift.getAmount());
                giftMessage = new MessageParameter("<gift>", itemDisplay, ChatColor.GREEN);

            } else {

                // Put it into their inventory
                ItemStack itemStack = new ItemStack(playerGift.getItemId(), playerGift.getItemCount());

                if (!readOnly && sameWorld) {
                    PlayerInventory inventory = player.getInventory();
                    inventory.addItem(itemStack);
                }

                // get the display name of the item received
                Material material = Material.getMaterial(itemStack.getTypeId());
                String itemDisplay = itemStack.getAmount() + " " + material.name();
                giftMessage = new MessageParameter("<gift>", itemDisplay, ChatColor.GREEN);
            }

            // tell them what they got
            List<MessageParameter> msgParameters = new ArrayList<MessageParameter>();
            msgParameters.add(messageName);
            msgParameters.add(giftMessage);
            msgParameters.add(messageWorld);

            if (!readOnly && sameWorld) {
                MessageUtil.sendMessage(player, "social.perk.gift.open.item.sender", ChatColor.GREEN, msgParameters);
            } else {
                MessageUtil.sendMessage(player, "social.perk.gift.read.item.sender", ChatColor.GREEN, msgParameters);
            }

            // now remove the gift so they don't get it again
            if (!readOnly && sameWorld) {
                openCount++;
                playerPerson.removeMessage(messagePerson, playerGift);
            }
        }

        if (gifts.size() == 0) {

            MessageUtil.sendMessage(player, "social.perk.gift.none.sender", ChatColor.GREEN);

        } else {

            MessageUtil.sendFooterLinesOnly(player);

            if (!SocialNetworkPlugin.getSettings().isMultiWorldGiftsAllowed()) {

                if (!readOnly && openCount == 0) {
                    MessageUtil.sendMessage(player, "social.perk.gift.multiWorld.noneAvailable", ChatColor.RED);
                }

                MessageUtil.sendMessage(player, "social.perk.gift.multiWorld.notice", ChatColor.GOLD);
            }

            // save the person
            if (!readOnly) {
                SocialNetworkPlugin.getStorage().savePerson(playerPerson);
            }
        }

        return true;
    }

    /**
     * /s gift yes Accept the gift being sent.
     * @param playerPerson
     * @param arguments
     * @return
     */
    private boolean handleSendGift(Player player, SocialPerson playerPerson, List<String> arguments) {

        // They've chosen to send the gift, so let's send it

        // get the gift and determine the type
        Gift gift = playerPerson.getWaitData();
        if (gift.getType() == Type.cash) {

            // now remove the money from their account
            boolean auth = processCashGift(player, gift.getAmount());
            if (!auth) {
                // error messages handled above
                return false;
            }

        } else if (gift.getType() == Type.item) {

            // we've already got the item in the message, so we'll place it in the message queue
            // when the other player receives it, we'll move it into their inventory
        }

        // place this message onto the other person's list
        SocialPerson sendToPerson = SocialNetworkPlugin.getStorage().getPerson(gift.getReceiverPlayerName());
        if (sendToPerson != null) {
            sendToPerson.addMessage(playerPerson, gift);
            SocialNetworkPlugin.getStorage().savePerson(sendToPerson);
        }

        // tell them it's been sent
        MessageUtil.sendMessage(player, "social.perk.gift.send.completed.sender", ChatColor.GREEN);

        // Cancel the wait, except the wait data. We need that later in getProcessPerkSettings() during postProcess.
        // We'll clear out the object after we're done with it then.
        playerPerson.waitOn(WaitState.notWaiting, null, gift);

        return true;
    }

    /**
     * /s gift no Don't accept the gift being sent.
     * @param playerPerson
     * @param arguments
     * @return
     */
    private boolean handleCancelGift(Player player, SocialPerson playerPerson, List<String> arguments) {

        // They've chosen to cancel the gift

        // if it was an item gift, give it back to them
        Gift gift = playerPerson.getWaitData();
        if (gift.getType() == Type.item) {

            // Put it back into their inventory
            ItemStack itemStack = new ItemStack(gift.getItemId(), gift.getItemCount());
            PlayerInventory inventory = player.getInventory();
            inventory.addItem(itemStack);
        }

        // cancel the wait
        playerPerson.waitOn(WaitState.notWaiting, null, null);

        // tell them it was cancelled
        MessageUtil.sendMessage(player, "social.perk.gift.send.cancelled.sender", ChatColor.GREEN);

        return false;
    }

    private SocialPerson validateReceivePerson(Player player, SocialPerson playerPerson, String receivePlayerName)
            throws PlayerNotInNetworkException {

        // make sure the player is in the network
        SocialPerson sendToPerson = SocialNetworkPlugin.getStorage().getPerson(receivePlayerName);
        if (sendToPerson == null) {
            throw new PlayerNotInNetworkException(receivePlayerName);
        }

        // make sure the person is in your social groups
        GiftSettings giftSettings = getPerkSettings(playerPerson, sendToPerson);
        if (giftSettings == null) {
            MessageUtil.sendPersonNotInPerksMessage(player, sendToPerson.getName());
            return null;
        }

        // If I have them on ignore...
        // Check to see if the person they're trying to contact is on their own ignore list
        if (playerPerson.isOnIgnore(sendToPerson)) {
            MessageUtil.sendPlayerIgnoredMessage(player, sendToPerson.getName());
            return null;
        }

        // If they have me on ignore...
        // Check to see if the person they're trying to contact has them on their ignore list
        if (sendToPerson.isOnIgnore(playerPerson)) {
            MessageUtil.sendSenderIgnoredMessage(player, sendToPerson.getName());
            return null;
        }

        return sendToPerson;
    }

    private <U extends IMessage> void sendGiftVerification(Player player, SocialPerson playerPerson,
            SocialPerson receivePerson, String itemDisplay, WaitState waitState, U waitData) {

        // okay, we need to ask them for their gender
        // first we'll create their account in the network, and assign it waitState.waitGenderResponse
        playerPerson.waitOn(waitState, SocialNetworkCommandType.gift, waitData);

        // now tell the user they need to use the /join <male/female> command
        MessageParameter numRequests = new MessageParameter("<item>", itemDisplay, ChatColor.GOLD);
        MessageParameter messageName = new MessageParameter("<player>", receivePerson.getName(), ChatColor.AQUA);

        List<MessageParameter> requestParameters = new ArrayList<MessageParameter>();
        requestParameters.add(numRequests);
        requestParameters.add(messageName);

        MessageUtil.sendMessage(player, "social.perk.gift.verifyRequired.label.sender", ChatColor.GREEN,
                requestParameters);

        MessageUtil.sendMessage(player, "social.perk.gift.verifyRequired.commands.sender", ChatColor.GREEN);
    }

    private boolean preAuthCashGift(Player player, double price) {

        SocialNetworkPlugin.log("[PreAuth] Found gift cash: " + price);

        // now check to see if they have enough money to run the perk
        if (!SocialNetworkPlugin.getVault().getEconomy().has(player.getName(), price)) {

            SocialNetworkPlugin.log("[PreAuth] Not enough funds for gift: " + price);

            NumberFormat formatter = new DecimalFormat("###.00");
            String itemDisplay = formatter.format(price);

            // they don't, so tell them
            MessageUtil.sendMessage(player, "social.perk.gift.notEnoughFunds", ChatColor.RED, new MessageParameter(
                    "<price>", itemDisplay, ChatColor.GOLD));

            return false;
        }

        return true;
    }

    private boolean processCashGift(Player player, double price) {

        // now check to see if they have enough money to pay for the gift
        if (SocialNetworkPlugin.getVault().getEconomy().has(player.getName(), price)) {

            SocialNetworkPlugin.log("[purchase] Charging gift price: " + price);

            // do the actual purchase now
            SocialNetworkPlugin.getVault().getEconomy().withdrawPlayer(player.getName(), price);

        } else {

            SocialNetworkPlugin.log("[purchase] Not enough funds for gift: " + price);

            NumberFormat formatter = new DecimalFormat("###.00");
            String itemDisplay = formatter.format(price);

            // Despite us checking earlier, they seem to have run out of money. Tell them.
            MessageUtil.sendMessage(player, "social.perk.gift.notEnoughFunds", ChatColor.RED, new MessageParameter(
                    "<price>", itemDisplay, ChatColor.GOLD));

            return false;
        }

        return true;
    }

    /**
     * Used to determine if the particular call being made should be pre-processed (check timer & pre-auth cost).
     * @param player
     * @param commandArguments
     * @return
     */
    @Override
    public boolean allowPreProcessPerkCommand(Player player, List<String> commandArguments) {

        // only charge/timer for sending a gift
        if (commandArguments.size() >= 1) {
            if (commandArguments.get(0).equals("yes")) {
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
        if (commandArguments.size() >= 1) {
            if (commandArguments.get(0).equals("yes")) {
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
    public GiftSettings getProcessPerkSettings(SocialPerson person, List<String> commandArguments) {

        // this command requires processing when sending a player a gift
        String command = commandArguments.get(0);
        if (command.equals("yes")) {

            // get the gift and determine the type
            Gift gift = person.getWaitData();
            SocialPerson sendToPerson = SocialNetworkPlugin.getStorage().getPerson(gift.getReceiverPlayerName());
            if (sendToPerson != null) {
                return getPerkSettings(person, sendToPerson);
            }

            // if their wait state has already been cleared, we need to clear out the object now that we're done with it
            if (person.getWaitState() == WaitState.notWaiting) {
                person.waitOn(WaitState.notWaiting, null, null);
            }
        }

        return null;
    }

    /**
     * This determines if we're waiting for a response from the user for this command.
     */
    @Override
    public boolean isValidWaitReponse(CommandSender sender, SocialPerson person) {

        // get the wait state from the person
        WaitState waitState = person.getWaitState();

        // we're looking for a gift wait, so check for it
        if (waitState != WaitState.waitCashGiftVerification && waitState != WaitState.waitHandGiftVerification) {

            // not what we're looking for, so display the wait help
            displayWaitHelp(sender);

            return false;
        }

        return true;
    }

    @Override
    public void displayWaitHelp(CommandSender sender) {

        // send the error message saying we're still waiting
        MessageUtil.sendMessage(sender, "social.wait.waitingForResponse.sender", ChatColor.GOLD);

        // the requested command wasn't for setting gender, so let's remind them they have to
        MessageUtil.sendMessage(sender, "social.perk.gift.verifyRequired.label.sender", ChatColor.GREEN);
        MessageUtil.sendMessage(sender, "social.perk.gift.verifyRequired.commands.sender", ChatColor.GREEN);
    }

    /**
     * /s gift cash <amount> <player> Send cash to player.
     * /s gift hand <player> Send in-hand item to player.
     * /s gift list List all the gifts available to you. Number, by player.
     * /s gift open <player> Open all the gifts from the player. Will delete after opening.
     */
    @Override
    public HelpSegment help() {

        HelpSegment helpSegment = new HelpSegment(getCommandType());
        ResourcesConfig config = SocialNetworkPlugin.getResources();

        HelpMessage cashCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "cash", "<amount> <player>",
                        config.getResource("social.perk.gift.help.send.cash"));
        helpSegment.addEntry(cashCommand);

        HelpMessage handCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "hand", "<player>",
                        config.getResource("social.perk.gift.help.send.item"));
        helpSegment.addEntry(handCommand);

        HelpMessage listCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "list", null,
                        config.getResource("social.perk.gift.help.list"));
        helpSegment.addEntry(listCommand);

        HelpMessage readCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "read", "<player>",
                        config.getResource("social.perk.gift.help.read"));
        helpSegment.addEntry(readCommand);

        HelpMessage openCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "open", "<player>",
                        config.getResource("social.perk.gift.help.open"));
        helpSegment.addEntry(openCommand);

        return helpSegment;
    }
}
