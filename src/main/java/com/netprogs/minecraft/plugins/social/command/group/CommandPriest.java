package com.netprogs.minecraft.plugins.social.command.group;

import java.util.ArrayList;
import java.util.List;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.SocialPerson.Status;
import com.netprogs.minecraft.plugins.social.SocialPerson.WaitState;
import com.netprogs.minecraft.plugins.social.SocialPerson.WeddingVows;
import com.netprogs.minecraft.plugins.social.command.IWaitCommand;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommand;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommandType;
import com.netprogs.minecraft.plugins.social.command.exception.ArgumentsMissingException;
import com.netprogs.minecraft.plugins.social.command.exception.InvalidPermissionsException;
import com.netprogs.minecraft.plugins.social.command.exception.PlayerNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.exception.PlayerNotOnlineException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotPlayerException;
import com.netprogs.minecraft.plugins.social.command.help.HelpBook;
import com.netprogs.minecraft.plugins.social.command.help.HelpMessage;
import com.netprogs.minecraft.plugins.social.command.help.HelpSegment;
import com.netprogs.minecraft.plugins.social.command.util.MessageParameter;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.SettingsConfig;
import com.netprogs.minecraft.plugins.social.config.settings.group.PriestSettings;
import com.netprogs.minecraft.plugins.social.storage.SocialNetworkStorage;
import com.netprogs.minecraft.plugins.social.storage.data.Wedding;

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

public class CommandPriest extends SocialNetworkCommand<PriestSettings> implements IWaitCommand {

    public CommandPriest() {
        super(SocialNetworkCommandType.priest);
    }

    @Override
    public boolean run(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            InvalidPermissionsException, SenderNotPlayerException, PlayerNotInNetworkException,
            PlayerNotOnlineException {

        // attempt to run the secondary commands first, but only players can respond to a priest
        boolean commandHandled = false;
        if (sender instanceof Player) {

            Player player = (Player) sender;

            // players can respond to requests from this command without having permissions to use it
            commandHandled = handleSecondaryCommands(player, arguments);

            // since we have a player, if the command wasn't handled, let's check to make sure they're a priest
            if (!commandHandled) {
                SocialPerson playerPerson = SocialNetworkPlugin.getStorage().getPerson(player.getName());
                if (playerPerson != null && !playerPerson.isPriest()) {

                    // If they didn't have priest assigned to them, check the permissions to see if they have it there
                    if (!hasCommandPermission(sender)) {
                        throw new InvalidPermissionsException();
                    }
                }
            }
        }

        // there were none, so lets try the main priest request command
        if (!commandHandled) {

            // check arguments
            if (arguments.size() != 2) {
                throw new ArgumentsMissingException();
            }

            // check to see if we have a player, if they are, check to make sure they're a lawyer
            if (sender instanceof Player) {

                Player player = (Player) sender;
                SocialPerson playerPerson = SocialNetworkPlugin.getStorage().getPerson(player.getName());

                // since we have a player, let's check to make sure they're a lawyer
                if (playerPerson != null && !playerPerson.isPriest()) {

                    // If they didn't have priest assigned to them, check the permissions to see if they have it there
                    if (!hasCommandPermission(sender)) {
                        throw new InvalidPermissionsException();
                    }
                }
            }

            // get the social network data
            PriestSettings settings = getCommandSettings();

            // make sure the A player is in the network
            String aPlayerName = arguments.get(0);
            SocialPerson aPlayerPerson = SocialNetworkPlugin.getStorage().getPerson(aPlayerName);
            if (aPlayerPerson == null) {
                throw new PlayerNotInNetworkException(aPlayerName);
            }

            // make sure the B player is in the network
            String bPlayerName = arguments.get(1);
            SocialPerson bPlayerPerson = SocialNetworkPlugin.getStorage().getPerson(bPlayerName);
            if (bPlayerPerson == null) {
                throw new PlayerNotInNetworkException(bPlayerName);
            }

            Player aPlayer = getPlayer(aPlayerPerson.getName());
            Player bPlayer = getPlayer(bPlayerPerson.getName());

            if (aPlayer != null && bPlayer != null) {

                // check to see if the couple can afford this request
                double price = settings.getPerUseCost();
                boolean authorized = true;
                authorized &= SocialNetworkPlugin.getVault().preAuthCommandPurchase(aPlayer, price);
                authorized &= SocialNetworkPlugin.getVault().preAuthCommandPurchase(bPlayer, price);
                if (!authorized) {
                    MessageUtil.sendMessage(sender, "social.priest.request.coupleCannotAfford.sender", ChatColor.GOLD);
                    return false;
                }

                // Divorced couples remain on a "bitterness" phase for a period of time.
                // During this time they're not allowed to get married again. Check for that now.
                long timeRemaining =
                        SocialNetworkPlugin.getTimerManager().commandOnTimer(aPlayerPerson.getName(),
                                SocialNetworkCommandType.divorce);

                if (timeRemaining > 0) {

                    // tell the user how much time remains
                    Player player = Bukkit.getServer().getPlayer(aPlayerPerson.getName());
                    if (player != null) {
                        MessageUtil.sendMessage(sender, "social.priest.request.coupleOnTimer.sender", ChatColor.GOLD,
                                MessageUtil.createCoolDownFormatting(timeRemaining));
                    }

                    return false;
                }

                // Engaged couples remain on an engagement phase for a period of time.
                // During this time they're not allowed to get married. Check for that now.
                timeRemaining =
                        SocialNetworkPlugin.getTimerManager().commandOnTimer(aPlayerPerson.getName(),
                                SocialNetworkCommandType.engagement);

                if (timeRemaining > 0) {

                    // tell the user how much time remains
                    Player player = Bukkit.getServer().getPlayer(aPlayerPerson.getName());
                    if (player != null) {
                        MessageUtil.sendMessage(sender, "social.priest.request.coupleOnTimer.sender", ChatColor.GOLD,
                                MessageUtil.createCoolDownFormatting(timeRemaining));
                    }
                    return false;
                }

                // Check to see if they allow same gender Marriage
                if (!SocialNetworkPlugin.getSettings().isSameGenderMarriageAllowed()) {
                    if (aPlayerPerson.getGender() == bPlayerPerson.getGender()) {
                        MessageUtil.sendMessage(sender, "social.error.sameGenderDisabled.sender", ChatColor.RED);
                        return false;
                    }
                }

                boolean engagedToPersonB = false;
                if (aPlayerPerson.getEngagement() != null) {

                    // check to see if they're engaged to the person they're asking to marry
                    engagedToPersonB =
                            aPlayerPerson.getEngagement().getPlayerName().equalsIgnoreCase(bPlayer.getName());
                }

                // marriage can only be requested if the player IS engaged
                if (aPlayerPerson.getSocialStatus() != Status.engaged || !engagedToPersonB) {

                    MessageUtil.sendMessage(sender, "social.priest.request.coupleNotEngaged.sender", ChatColor.RED);
                    return false;
                }

                //
                // Okay, we have everyone now. Let's send them each a message and force them to answer
                //

                // make it so we won't accept any commands other than our accept/reject
                Wedding aPlayerWaitData =
                        new Wedding(sender.getName(), aPlayerPerson.getName(), bPlayerPerson.getName());
                aPlayerPerson.setWeddingVows(null);
                aPlayerPerson.waitOn(WaitState.waitMarriageResponse, SocialNetworkCommandType.priest, aPlayerWaitData);
                sendPlayerRequestMessage(sender, aPlayer);

                // make it so we won't accept any commands other than our accept/reject
                Wedding bPlayerWaitData =
                        new Wedding(sender.getName(), bPlayerPerson.getName(), aPlayerPerson.getName());
                bPlayerPerson.setWeddingVows(null);
                bPlayerPerson.waitOn(WaitState.waitMarriageResponse, SocialNetworkCommandType.priest, bPlayerWaitData);
                sendPlayerRequestMessage(sender, bPlayer);

                // save the changes to disk
                SocialNetworkPlugin.getStorage().savePerson(aPlayerPerson);
                SocialNetworkPlugin.getStorage().savePerson(bPlayerPerson);

            } else {

                if (aPlayer == null) {
                    throw new PlayerNotOnlineException(aPlayerName);
                }

                if (bPlayer == null) {
                    throw new PlayerNotOnlineException(bPlayerName);
                }

                return false;
            }
        }

        return true;
    }

    private void sendPlayerRequestMessage(CommandSender sender, Player player) {

        // now tell the user they need to use the /join <male/female> command
        MessageParameter priestParam = new MessageParameter("<priest>", sender.getName(), ChatColor.AQUA);
        MessageParameter playerParam = new MessageParameter("<player>", player.getName(), ChatColor.AQUA);

        List<MessageParameter> messageVariables = new ArrayList<MessageParameter>();
        messageVariables.add(priestParam);
        messageVariables.add(playerParam);

        // send accept/reject message to the player
        MessageUtil.sendMessage(player, "social.priest.request.question.player", ChatColor.GREEN, messageVariables);
        MessageUtil.sendMessage(player, "social.priest.request.commands.player", ChatColor.GREEN);

        // send a message to priest saying the player was contacted
        MessageUtil.sendMessage(sender, "social.priest.request.waiting.sender", ChatColor.GREEN, messageVariables);
    }

    private boolean handleSecondaryCommands(Player player, List<String> arguments) throws ArgumentsMissingException,
            PlayerNotInNetworkException {

        // get the social network data
        SocialNetworkStorage socialConfig = SocialNetworkPlugin.getStorage();
        SettingsConfig settingsConfig = SocialNetworkPlugin.getSettings();

        // need to make sure we have at least one argument (the command parameter)
        if (arguments.size() == 1) {

            /**
             * First one in, does the changes. If the second one in rejects, then revert the changes.
             * Don't send out a completed message until we hear back from the second one.
             * Don't save the changes until we hear from the second one?
             */

            SocialNetworkPlugin.log("Processing player marriage acceptance.");

            // we only want to run this if the player is in the network
            SocialPerson playerPerson = SocialNetworkPlugin.getStorage().getPerson(player.getName());
            if (playerPerson != null) {

                if (arguments.get(0).equals("list")) {

                    // List all the available priests and if they're online
                    MessageUtil.sendHeaderMessage(player, "social.admin.priest.list.header.sender");

                    // check to see if they have anyone
                    List<String> priestPlayers = SocialNetworkPlugin.getStorage().getPriests();
                    if (priestPlayers.size() == 0) {

                        MessageUtil.sendMessage(player, "social.priest.list.noPeople.sender", ChatColor.GREEN);

                    } else {

                        ResourcesConfig config = SocialNetworkPlugin.getResources();
                        String onlineTag = config.getResource("social.list.tag.online.sender");
                        String offlineTag = config.getResource("social.list.tag.offline.sender");

                        String onlineList = ChatColor.GREEN + onlineTag + " " + ChatColor.WHITE;
                        String offlineList = ChatColor.GRAY + offlineTag + " " + ChatColor.WHITE;

                        // go through priest list and display their names and online status
                        for (String playerName : priestPlayers) {

                            // check to see if they are online
                            Player groupPlayer = getPlayer(playerName);
                            if (groupPlayer != null) {
                                onlineList += playerName + ", ";
                            } else {
                                offlineList += playerName + ", ";
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

                    return true;

                } else if (arguments.get(0).equals("accept")) {

                    // check to make sure they're engaged still
                    if (playerPerson.getSocialStatus() != Status.engaged) {
                        MessageUtil.sendMessage(player, "social.priest.request.coupleNotEngaged.sender", ChatColor.RED);
                        return true;
                    }

                    SocialNetworkPlugin.log("Processing " + playerPerson.getName() + " accept.");

                    // Check to see if the other person has rejected their vows
                    // If it's NULL, we got here first and we'll assume it's okay to add them
                    // When the next one comes in, if they reject, we'll delete then
                    // If they responded first, but rejected, then we'll just move on

                    // get your spouse
                    String spouseName = playerPerson.getEngagement().getPlayerName();
                    SocialPerson spousePerson = socialConfig.getPerson(spouseName);
                    if (spousePerson == null) {
                        throw new PlayerNotInNetworkException(spouseName);
                    }

                    // Check to see if they've accepted their vows
                    if (spousePerson.getWeddingVows() == null || spousePerson.getWeddingVows() == WeddingVows.accepted) {

                        // Player got here first. Create the marriage
                        if (spousePerson.getWeddingVows() == WeddingVows.accepted) {
                            SocialNetworkPlugin.log("Spouse " + spousePerson.getName() + " has already accepted.");
                        } else {
                            SocialNetworkPlugin.log("Player " + spousePerson.getName() + " has accepted.");
                        }

                        // get the priest first
                        Wedding wedding = playerPerson.getWaitData();
                        Player priestPlayer = getPlayer(wedding.getPriest());

                        // We got here first, so create the marriage
                        playerPerson.createMarriage(spousePerson);

                        // remove their engagement
                        playerPerson.breakEngagement();

                        // change their status
                        playerPerson.setSocialStatus(Status.married);

                        // set vows as accepted
                        playerPerson.setWeddingVows(WeddingVows.accepted);

                        // remove the wait from this player
                        playerPerson.waitOn(WaitState.notWaiting, null);

                        // save the changes
                        socialConfig.savePerson(playerPerson);

                        // tell the player they accepted
                        MessageUtil.sendMessage(player, "social.priest.accept.sender", ChatColor.GREEN);

                        // tell the spouse they got accepted
                        Player spousePlayer = getPlayer(spousePerson.getName());
                        if (spousePlayer != null) {
                            MessageUtil.sendMessage(spousePlayer, "social.priest.accept.player", ChatColor.GREEN,
                                    new MessageParameter("<player>", playerPerson.getName(), ChatColor.AQUA));
                        }

                        // send a message to the priest
                        if (priestPlayer != null) {
                            MessageUtil.sendMessage(priestPlayer, "social.priest.accept.player", ChatColor.GREEN,
                                    new MessageParameter("<player>", playerPerson.getName(), ChatColor.AQUA));
                        }

                        // both parties have accepted their vows, so let's do the final work
                        if (spousePerson.getWeddingVows() == WeddingVows.accepted) {

                            // check for a permissions update
                            checkForPermissionsUpdate(playerPerson);
                            checkForPermissionsUpdate(spousePerson);

                            PriestSettings settings = getCommandSettings();

                            // update the timer for the marriage
                            int timer = settings.getHoneymoonPeriod();

                            SocialNetworkPlugin.getTimerManager().updateCommandTimer(playerPerson.getName(),
                                    SocialNetworkCommandType.marriage, timer);

                            SocialNetworkPlugin.getTimerManager().updateCommandTimer(spousePerson.getName(),
                                    SocialNetworkCommandType.marriage, timer);

                            // now charge for our services
                            SocialNetworkPlugin.getVault().processCommandPurchase(player, settings.getPerUseCost());
                            SocialNetworkPlugin.getVault().processCommandPurchase(spousePlayer,
                                    settings.getPerUseCost());

                            MessageParameter priestParam =
                                    new MessageParameter("<playerA>", playerPerson.getName(), ChatColor.AQUA);
                            MessageParameter playerParam =
                                    new MessageParameter("<playerB>", spousePerson.getName(), ChatColor.AQUA);

                            List<MessageParameter> messageVariables = new ArrayList<MessageParameter>();
                            messageVariables.add(priestParam);
                            messageVariables.add(playerParam);

                            // If they have global announcement on, send it out
                            if (settingsConfig.isGlobalAnnouncePriestMarriages()) {

                                // send out the global message
                                MessageUtil.sendGlobalMessage("social.priest.completed.global", ChatColor.GOLD,
                                        messageVariables);

                            } else {

                                // send a message to the priest
                                if (priestPlayer != null) {
                                    MessageUtil.sendMessage(priestPlayer, "social.priest.completed.global",
                                            ChatColor.GOLD, messageVariables);
                                }

                                // send to the players involved
                                MessageUtil.sendMessage(player, "social.priest.completed.global", ChatColor.GOLD,
                                        messageVariables);

                                MessageUtil.sendMessage(spousePlayer, "social.priest.completed.global", ChatColor.GOLD,
                                        messageVariables);
                            }
                        }

                    } else if (spousePerson.getWeddingVows() == WeddingVows.rejected) {

                        // If the spouse rejected the player, change the player settings
                        SocialNetworkPlugin.log("Spouse " + spousePerson.getName()
                                + " has already rejected. Doing cleanup.");
                    }

                    return true;

                } else if (arguments.get(0).equals("reject")) {

                    // check to make sure they're engaged still
                    if (playerPerson.getSocialStatus() != Status.engaged) {
                        MessageUtil.sendMessage(player, "social.priest.request.coupleNotEngaged.sender", ChatColor.RED);
                        return true;
                    }

                    SocialNetworkPlugin.log("Processing " + playerPerson.getName() + " reject.");

                    // Check to see if the other person was there first and if they accepted.
                    // If they accepted, do cleanup
                    // If they rejected also, we're fine

                    // get your spouse
                    String spouseName = playerPerson.getEngagement().getPlayerName();
                    SocialPerson spousePerson = socialConfig.getPerson(spouseName);
                    if (spousePerson == null) {
                        throw new PlayerNotInNetworkException(spouseName);
                    }

                    // Check to see if spouse rejected their vows first
                    if (spousePerson.getWeddingVows() == null || spousePerson.getWeddingVows() == WeddingVows.rejected) {

                        // Player got here first
                        if (spousePerson.getWeddingVows() == WeddingVows.rejected) {
                            SocialNetworkPlugin.log("Spouse " + spousePerson.getName() + " has already rejected.");
                        } else {
                            SocialNetworkPlugin.log("Player " + playerPerson.getName() + " has rejected.");
                        }

                    } else if (spousePerson.getWeddingVows() == WeddingVows.accepted) {

                        // Spouse accepted theirs first.
                        SocialNetworkPlugin.log("Spouse " + spousePerson.getName()
                                + " has already accepted. Doing cleanup.");
                    }

                    // get the priest first
                    Wedding wedding = playerPerson.getWaitData();
                    Player priestPlayer = getPlayer(wedding.getPriest());

                    // remove the marriage if it was set
                    playerPerson.breakMarriage();
                    spousePerson.breakMarriage();

                    // remove their engagement (spouse already got theirs removed)
                    playerPerson.breakEngagement();
                    spousePerson.breakEngagement();

                    // remove the wait from this player (spouse was already removed during accept)
                    playerPerson.waitOn(WaitState.notWaiting, null);
                    spousePerson.waitOn(WaitState.notWaiting, null);

                    // change their status to single
                    playerPerson.setSocialStatus(Status.single);
                    spousePerson.setSocialStatus(Status.single);

                    // set vows as null
                    playerPerson.setWeddingVows(null);
                    spousePerson.setWeddingVows(null);

                    // save the changes
                    socialConfig.savePerson(playerPerson);
                    socialConfig.savePerson(spousePerson);

                    // check for a permissions update
                    checkForPermissionsUpdate(playerPerson);
                    checkForPermissionsUpdate(spousePerson);

                    // tell the player they rejected
                    MessageUtil.sendMessage(player, "social.priest.reject.sender", ChatColor.GREEN);

                    // tell the spouse they got rejected
                    Player spousePlayer = getPlayer(spousePerson.getName());
                    if (spousePlayer != null) {
                        MessageUtil.sendMessage(spousePlayer, "social.priest.reject.player", ChatColor.GREEN,
                                new MessageParameter("<player>", playerPerson.getName(), ChatColor.AQUA));
                    }

                    // send a message to the priest
                    if (priestPlayer != null) {
                        MessageUtil.sendMessage(priestPlayer, "social.priest.reject.player", ChatColor.GREEN,
                                new MessageParameter("<player>", playerPerson.getName(), ChatColor.AQUA));
                    }

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * This determines if we're waiting for a response from the user for this command.
     */
    @Override
    public boolean isValidWaitReponse(CommandSender sender, SocialPerson person) {

        // get the wait state from the person
        WaitState waitState = person.getWaitState();

        // we're looking for a gender wait, so check for it
        if (waitState != WaitState.waitMarriageResponse) {

            // not what we're looking for, so display the wait help
            displayWaitHelp(sender);

            return false;
        }

        return true;
    }

    @Override
    public void displayWaitHelp(CommandSender sender) {

        // lookup the person's information in the network
        SocialPerson playerPerson = SocialNetworkPlugin.getStorage().getPerson(sender.getName());
        if (playerPerson != null) {

            Wedding wedding = playerPerson.getWaitData();

            // send the error message saying we're still waiting
            MessageUtil.sendMessage(sender, "social.wait.waitingForResponse.sender", ChatColor.RED);

            // now tell the user they need to use the /join <male/female> command
            MessageParameter priestParam = new MessageParameter("<priest>", wedding.getPriest(), ChatColor.AQUA);
            MessageParameter playerParam = new MessageParameter("<player>", wedding.getSpouseName(), ChatColor.AQUA);

            List<MessageParameter> messageVariables = new ArrayList<MessageParameter>();
            messageVariables.add(priestParam);
            messageVariables.add(playerParam);

            MessageUtil.sendMessage(sender, "social.priest.request.question.player", ChatColor.GREEN, messageVariables);
            MessageUtil.sendMessage(sender, "social.priest.request.commands.player", ChatColor.GREEN);
        }
    }

    @Override
    public HelpSegment help() {

        HelpSegment helpSegment = new HelpSegment(getCommandType());
        ResourcesConfig config = SocialNetworkPlugin.getResources();

        HelpMessage mainCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "request", "<playerA> <playerB>",
                        config.getResource("social.priest.help.request"));
        helpSegment.addEntry(mainCommand);

        HelpMessage listCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "list", null,
                        config.getResource("social.priest.help.list"));
        helpSegment.addEntry(listCommand);

        HelpMessage acceptCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "accept", null,
                        config.getResource("social.priest.help.accept"));
        helpSegment.addEntry(acceptCommand);

        HelpMessage rejectCommand =
                HelpBook.generateHelpMessage(getCommandType().toString(), "reject", null,
                        config.getResource("social.priest.help.reject"));
        helpSegment.addEntry(rejectCommand);

        return helpSegment;
    }
}
