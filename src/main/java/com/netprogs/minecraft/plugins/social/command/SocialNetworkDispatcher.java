package com.netprogs.minecraft.plugins.social.command;

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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.SocialPerson.WaitState;
import com.netprogs.minecraft.plugins.social.command.ISocialNetworkCommand.ICommandType;
import com.netprogs.minecraft.plugins.social.command.admin.CommandAdmin;
import com.netprogs.minecraft.plugins.social.command.exception.ArgumentsMissingException;
import com.netprogs.minecraft.plugins.social.command.exception.InvalidPermissionsException;
import com.netprogs.minecraft.plugins.social.command.exception.PlayerNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.exception.PlayerNotOnlineException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotPlayerException;
import com.netprogs.minecraft.plugins.social.command.group.CommandAffair;
import com.netprogs.minecraft.plugins.social.command.group.CommandChild;
import com.netprogs.minecraft.plugins.social.command.group.CommandDivorce;
import com.netprogs.minecraft.plugins.social.command.group.CommandEngagement;
import com.netprogs.minecraft.plugins.social.command.group.CommandFriend;
import com.netprogs.minecraft.plugins.social.command.group.CommandLawyer;
import com.netprogs.minecraft.plugins.social.command.group.CommandMarriage;
import com.netprogs.minecraft.plugins.social.command.group.CommandPriest;
import com.netprogs.minecraft.plugins.social.command.group.CommandRelationship;
import com.netprogs.minecraft.plugins.social.command.help.HelpBook;
import com.netprogs.minecraft.plugins.social.command.help.HelpPage;
import com.netprogs.minecraft.plugins.social.command.perk.CommandGift;
import com.netprogs.minecraft.plugins.social.command.perk.CommandSticky;
import com.netprogs.minecraft.plugins.social.command.perk.CommandTeleport;
import com.netprogs.minecraft.plugins.social.command.perk.CommandTell;
import com.netprogs.minecraft.plugins.social.command.perk.IPerkCommand;
import com.netprogs.minecraft.plugins.social.command.social.CommandAlerts;
import com.netprogs.minecraft.plugins.social.command.social.CommandHelp;
import com.netprogs.minecraft.plugins.social.command.social.CommandIgnore;
import com.netprogs.minecraft.plugins.social.command.social.CommandJoin;
import com.netprogs.minecraft.plugins.social.command.social.CommandOnline;
import com.netprogs.minecraft.plugins.social.command.social.CommandQuit;
import com.netprogs.minecraft.plugins.social.command.social.CommandRequests;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.command.util.TimerUtil;
import com.netprogs.minecraft.plugins.social.config.PluginConfig;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.ISocialNetworkSettings;
import com.netprogs.minecraft.plugins.social.config.settings.SettingsConfig;
import com.netprogs.minecraft.plugins.social.config.settings.perk.IPerkSettings;
import com.netprogs.minecraft.plugins.social.integration.VaultIntegration;
import com.netprogs.minecraft.plugins.social.storage.SocialNetwork;
import com.netprogs.minecraft.plugins.social.storage.data.perk.IPersonPerkSettings;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Dispatches all incoming commands off to their related {@link ISocialNetworkCommand} instance to handle.
 * @author Scott Milne
 */
public class SocialNetworkDispatcher implements CommandExecutor {

    private final Logger logger = Logger.getLogger("Minecraft");

    private final Map<ICommandType, ISocialNetworkCommand<? extends ISocialNetworkSettings>> commands =
            new HashMap<ICommandType, ISocialNetworkCommand<? extends ISocialNetworkSettings>>();

    public SocialNetworkDispatcher() {

        createCommandMap();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {

        // first thing we want to do is check for who's sending this request
        if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
            StringWriter argumentList = new StringWriter();
            for (String argument : arguments) {
                argumentList.append(argument);
                argumentList.append(" ");
            }
            logger.info("Incoming command: " + argumentList.toString());
        }

        try {

            // if nothing given, don't continue
            if (arguments.length == 0) {
                throw new ArgumentsMissingException();
            }

            // Grab the first argument, this should be our command.
            SocialNetworkCommandType requestedCommand = null;
            if (SocialNetworkCommandType.contains(arguments[0].toLowerCase())) {

                // we got a match, so set it
                requestedCommand = SocialNetworkCommandType.valueOf(arguments[0].toLowerCase());

            } else {

                // If we're here, the command wasn't given enough information.
                MessageUtil.sendMessage(sender, "social.error.unknownArguments", ChatColor.RED);
                return true;
            }

            // put the rest into a list
            List<String> commandArguments = new ArrayList<String>();
            for (int i = 1; i < arguments.length; i++) {
                commandArguments.add(arguments[i]);
            }

            // check to see if we need to wait for the user to respond to a question from us
            boolean processRequest = processWaitCommand(sender, requestedCommand);
            if (!processRequest) {

                // the user sent a command we're not waiting on, so cancel the command
                return true;
            }

            // process the rest of the commands
            if (commands.containsKey(requestedCommand)) {

                ISocialNetworkCommand<? extends ISocialNetworkSettings> socialCommand = commands.get(requestedCommand);

                // try to run the command
                try {

                    boolean success = false;

                    // List<Class<? extends GroupSettings>> socialGroups = getPreCommandSocialGroups(sender);

                    // do any pre-process work needed
                    success = preProcessCommand(sender, socialCommand, commandArguments);

                    // okay, run the command now if pre-process was successful
                    if (success) {
                        success = socialCommand.run(sender, commandArguments);
                    }

                    // do the post-process work if successfully executed
                    if (success) {
                        postProcessCommand(sender, socialCommand, commandArguments);
                    }

                } catch (SenderNotInNetworkException exception) {

                    // If we're here, the sender wasn't in the network
                    MessageUtil.sendSenderNotInNetworkMessage(sender);

                } catch (PlayerNotInNetworkException exception) {

                    // If we're here, the player being interacted with is not in the network
                    MessageUtil.sendPlayerNotInNetworkMessage(sender, exception.getPlayerName());

                } catch (SenderNotPlayerException exception) {

                    // If we're here, the command wasn't sent from a player and the command needed them to be one.
                    MessageUtil.sendSenderNotPlayerMessage(sender);

                } catch (ArgumentsMissingException exception) {

                    // If we're here, the command wasn't given enough information.
                    MessageUtil.sendUnknownArgumentsMessage(sender);

                } catch (InvalidPermissionsException exception) {

                    // If we're here, the sender requesting the command did not have permission to do so
                    MessageUtil.sendInvalidPermissionsMessage(sender);

                } catch (PlayerNotOnlineException exception) {

                    // If we're here, the sender requested an action with a player that was off-line
                    MessageUtil.sendPlayerNotOnlineMessage(sender, exception.getPlayerName());
                }

                // we've handled this command in one form or another
                return true;
            }

            // Send all help messages if none matched
            HelpBook.sendHelpPage(sender, 1);

        } catch (ArgumentsMissingException exception) {

            // If we're here, the command wasn't given enough information.
            MessageUtil.sendMessage(sender, "social.error.unknownArguments", ChatColor.RED);
        }

        return true;
    }

    private void createCommandMap() {

        //
        // Help
        //

        CommandHelp help = new CommandHelp();
        commands.put(SocialNetworkCommandType.help, help);

        //
        // Social
        //

        CommandJoin join = new CommandJoin();
        commands.put(SocialNetworkCommandType.join, join);

        CommandQuit quit = new CommandQuit();
        commands.put(SocialNetworkCommandType.quit, quit);

        CommandOnline online = new CommandOnline();
        commands.put(SocialNetworkCommandType.online, online);

        CommandRequests requests = new CommandRequests();
        commands.put(SocialNetworkCommandType.requests, requests);

        CommandAlerts alerts = new CommandAlerts();
        commands.put(SocialNetworkCommandType.alerts, alerts);

        CommandIgnore ignore = new CommandIgnore();
        commands.put(SocialNetworkCommandType.ignore, ignore);

        //
        // Groups
        //

        CommandFriend friend = new CommandFriend();
        commands.put(SocialNetworkCommandType.friend, friend);

        CommandRelationship relationship = new CommandRelationship();
        commands.put(SocialNetworkCommandType.relationship, relationship);

        CommandEngagement engagement = new CommandEngagement();
        commands.put(SocialNetworkCommandType.engagement, engagement);

        CommandAffair affair = new CommandAffair();
        commands.put(SocialNetworkCommandType.affair, affair);

        CommandChild child = new CommandChild();
        commands.put(SocialNetworkCommandType.child, child);

        CommandMarriage marriage = new CommandMarriage();
        commands.put(SocialNetworkCommandType.marriage, marriage);

        CommandDivorce divorce = new CommandDivorce();
        commands.put(SocialNetworkCommandType.divorce, divorce);

        //
        // Jobs
        //

        CommandPriest priest = new CommandPriest();
        commands.put(SocialNetworkCommandType.priest, priest);

        CommandLawyer lawyer = new CommandLawyer();
        commands.put(SocialNetworkCommandType.lawyer, lawyer);

        //
        // Admin
        //

        CommandAdmin admin = new CommandAdmin();
        commands.put(SocialNetworkCommandType.admin, admin);

        //
        // Perks
        //

        CommandTeleport teleport = new CommandTeleport();
        commands.put(teleport.getCommandType(), teleport);

        CommandSticky sticky = new CommandSticky();
        commands.put(sticky.getCommandType(), sticky);

        CommandTell tell = new CommandTell();
        commands.put(tell.getCommandType(), tell);

        CommandGift gift = new CommandGift();
        commands.put(gift.getCommandType(), gift);

        //
        // Help Pages
        //
        ResourcesConfig config = PluginConfig.getInstance().getConfig(ResourcesConfig.class);

        // Page 1
        HelpPage baseHelpPage = new HelpPage();
        baseHelpPage.addSegment(help.help());
        baseHelpPage.addSegment(join.help());
        baseHelpPage.addSegment(quit.help());
        baseHelpPage.addSegment(online.help());
        baseHelpPage.addSegment(requests.help());
        baseHelpPage.addSegment(alerts.help());
        baseHelpPage.addSegment(ignore.help());
        HelpBook.addPage(baseHelpPage);

        // Page 2
        HelpPage friendHelpPage = new HelpPage();
        friendHelpPage.addSegment(friend.help());
        HelpBook.addPage(friendHelpPage);

        // Page 3
        HelpPage relationshipHelpPage = new HelpPage();
        relationshipHelpPage.addSegment(relationship.help());
        HelpBook.addPage(relationshipHelpPage);

        // Page 4
        HelpPage childHelpPage = new HelpPage();
        childHelpPage.addSegment(child.help());
        HelpBook.addPage(childHelpPage);

        // Page 5
        HelpPage engagementHelpPage = new HelpPage();
        engagementHelpPage.addSegment(engagement.help());
        HelpBook.addPage(engagementHelpPage);

        // Page 6
        HelpPage affairHelpPage = new HelpPage();
        affairHelpPage.addSegment(affair.help());
        HelpBook.addPage(affairHelpPage);

        // Page 7
        HelpPage mdHelpPage = new HelpPage();
        mdHelpPage.addSegment(marriage.help());
        mdHelpPage.addSegment(divorce.help());
        HelpBook.addPage(mdHelpPage);

        // Page 8
        HelpPage jobsHelpPage = new HelpPage();
        jobsHelpPage.addSegment(priest.help());
        jobsHelpPage.addSegment(lawyer.help());
        HelpBook.addPage(jobsHelpPage);

        // Page 9, Perks. Break apart as needed.
        HelpPage perks1HelpPage = new HelpPage(config.getResource("social.help.perks.title"));
        perks1HelpPage.addSegment(teleport.help());
        perks1HelpPage.addSegment(sticky.help());
        perks1HelpPage.addSegment(tell.help());
        perks1HelpPage.addSegment(gift.help());
        HelpBook.addPage(perks1HelpPage);

        // leave as last
        HelpPage adminHelpPage = new HelpPage();
        adminHelpPage.addSegment(admin.help());
        HelpBook.addPage(adminHelpPage);
    }

    private boolean processWaitCommand(CommandSender sender, SocialNetworkCommandType requestedCommand) {

        // first we want to check to see if we need to process any waiting commands
        // if the user is in a waiting state, they cannot do any other commands until they respond

        if ((sender instanceof Player)) {

            Player player = (Player) sender;

            // if the user is in a waiting state, then check to see if this requested command will handle it
            SocialPerson person = SocialNetwork.getInstance().getPerson(player.getName());
            if (person != null && person.getWaitState() != null && person.getWaitState() != WaitState.notWaiting) {

                if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                    logger.info("WaitState: " + person.getWaitState());
                    logger.info("WaitCommand: " + person.getWaitCommand());
                    logger.info("Requested Command: " + requestedCommand);
                }

                // get the command that is being requested and check to see if it is a wait command
                ISocialNetworkCommand<? extends ISocialNetworkSettings> socialCommand = commands.get(requestedCommand);
                if (socialCommand instanceof IWaitCommand) {

                    // we got a wait command, so now let's check to see if it wants to handle the command
                    boolean isValidWaitResponse = ((IWaitCommand) socialCommand).isValidWaitReponse(sender, person);

                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("Requested command is an IWaitCommand");
                        logger.info("isValidWaitResponse: " + isValidWaitResponse);
                    }

                    return isValidWaitResponse;

                } else {

                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("Requested command is NOT an IWaitCommand. Using person WaitCommand for help page.");
                    }

                    // The command requested wasn't an IWaitCommand, so let's use the person's getWaitCommand() and look
                    // it up so we can provide a help page.
                    if (person.getWaitCommand() != null) {

                        ISocialNetworkCommand<? extends ISocialNetworkSettings> waitCommand =
                                commands.get(person.getWaitCommand());

                        if (waitCommand instanceof IWaitCommand) {
                            ((IWaitCommand) waitCommand).displayWaitHelp(sender);
                        }
                    }

                    // cancel the command request, we didn't get anything useful
                    return false;
                }
            }
        }

        // player isn't waiting on anything, continue on
        return true;
    }

    private boolean preProcessCommand(CommandSender sender,
            ISocialNetworkCommand<? extends ISocialNetworkSettings> socialCommand, List<String> commandArguments)
            throws SenderNotPlayerException, SenderNotInNetworkException {

        // check to see if the command is a perk, and do any preliminary work needed for it
        boolean success = preProcessPerkCommand(sender, socialCommand, commandArguments);

        return success;
    }

    private boolean postProcessCommand(CommandSender sender,
            ISocialNetworkCommand<? extends ISocialNetworkSettings> socialCommand, List<String> commandArguments)
            throws SenderNotInNetworkException, SenderNotPlayerException {

        // check to see if this was a perk command and do any post work needed for it
        postProcessPerkCommand(sender, socialCommand, commandArguments);

        return true;
    }

    private boolean preProcessPerkCommand(CommandSender sender,
            ISocialNetworkCommand<? extends ISocialNetworkSettings> socialCommand, List<String> commandArguments)
            throws SenderNotInNetworkException, SenderNotPlayerException {

        // At this point we need to check the command to see if it is a perk, and if it is, only allow the
        // player to use it if they belong to a group that has it assigned to.
        if (socialCommand instanceof IPerkCommand) {

            // make sure we're working with a player
            if (!(sender instanceof Player)) {
                throw new SenderNotPlayerException();
            }

            @SuppressWarnings("unchecked")
            IPerkCommand<? extends IPerkSettings, ? extends IPersonPerkSettings> perkCommand =
                    (IPerkCommand<? extends IPerkSettings, ? extends IPersonPerkSettings>) socialCommand;

            Player player = (Player) sender;
            SocialPerson person = getSenderPerson(sender);

            // ask the perk command to see if we should do the pre-process
            if (!perkCommand.allowPreProcessPerkCommand(player, commandArguments)) {
                // although we haven't done anything, tell the response we have
                return true;
            }

            //
            // Step 1: Check to see if their current social groups allow them to run this perk.
            //

            IPerkSettings perkSettings = perkCommand.getProcessPerkSettings(person, commandArguments);
            if (perkSettings != null) {

                //
                // Step 2: Check to see if the perk is currently on cooldown.
                //

                // Check to see if this command is on timer for the player. If so, cancel the command by returning
                // false.
                long remaining = TimerUtil.commandOnTimer(player.getName(), socialCommand.getCommandType());
                if (remaining > 0) {

                    // tell the user how much time remains
                    MessageUtil.sendCommandOnCooldownMessage(player, remaining);

                    // cancel the command
                    return false;
                }

                //
                // Step 3: Check to see if they have enough money to pay for the perk usage (pre-auth only)
                //

                double price = perkSettings.getPerUseCost();
                boolean authorized = VaultIntegration.getInstance().preAuthCommandPurchase(player, price);
                if (!authorized) {
                    return false;
                }

            } else {

                // if were here, nothing was found, which means they can't run this command, so tell them
                MessageUtil.sendInvalidPerkMessage(sender);
                return false;
            }
        }

        // wasn't a perk instance or was good to run
        return true;
    }

    private boolean postProcessPerkCommand(CommandSender sender,
            ISocialNetworkCommand<? extends ISocialNetworkSettings> socialCommand, List<String> commandArguments)
            throws SenderNotInNetworkException, SenderNotPlayerException {

        // Get their social data
        // Person person = getSenderPerson(sender);

        // get the cooldown settings
        long cooldown = 0L;

        // if an Perk, grab from the perk settings
        if (socialCommand instanceof IPerkCommand) {

            // make sure we're working with a player
            if (!(sender instanceof Player)) {
                throw new SenderNotPlayerException();
            }

            @SuppressWarnings("unchecked")
            IPerkCommand<? extends IPerkSettings, ? extends IPersonPerkSettings> perkCommand =
                    (IPerkCommand<? extends IPerkSettings, ? extends IPersonPerkSettings>) socialCommand;

            Player player = (Player) sender;
            SocialPerson person = getSenderPerson(sender);

            // ask the perk command to see if we should do the pre-process
            if (!perkCommand.allowPostProcessPerkCommand(player, commandArguments)) {
                // although we haven't done anything, tell the response we have
                return true;
            }

            //
            // Step 1: Check to see if their current social groups allow them to run this perk.
            //
            IPerkSettings perkSettings = perkCommand.getProcessPerkSettings(person, commandArguments);
            if (perkSettings != null) {

                //
                // Step 2: Create or update the cooldown timer for the perk command.
                //

                // get the cooldown
                cooldown = perkSettings.getCoolDownPeriod();

                if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                    logger.info("Using perk cooldown: " + TimerUtil.formatTime(cooldown));
                }

                // update the timer for the command
                TimerUtil.updateCommandTimer(player.getName(), socialCommand.getCommandType(), cooldown);

                //
                // Step 3: Charge the user for the price of the usage.
                //

                double price = perkSettings.getPerUseCost();
                boolean successful = VaultIntegration.getInstance().processCommandPurchase(player, price);
                if (!successful) {
                    return false;
                }
            }
        }

        // either not a perk, or we've completed our work on it successfully
        return true;
    }

    private SocialPerson getSenderPerson(CommandSender sender) throws SenderNotInNetworkException,
            SenderNotPlayerException {

        // get the social network data
        SocialNetwork socialConfig = SocialNetwork.getInstance();

        if ((sender instanceof Player)) {

            Player player = (Player) sender;

            // Get their social data
            SocialPerson person = socialConfig.getPerson(player.getName());
            if (person != null) {
                return person;
            } else {
                throw new SenderNotInNetworkException();
            }

        } else {

            throw new SenderNotPlayerException();
        }
    }
}
