package com.netprogs.minecraft.plugins.social.command.social;

import java.util.List;
import java.util.logging.Logger;

import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.SocialPerson.Gender;
import com.netprogs.minecraft.plugins.social.SocialPerson.WaitState;
import com.netprogs.minecraft.plugins.social.command.IWaitCommand;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommand;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommandType;
import com.netprogs.minecraft.plugins.social.command.exception.ArgumentsMissingException;
import com.netprogs.minecraft.plugins.social.command.exception.InvalidPermissionsException;
import com.netprogs.minecraft.plugins.social.command.exception.PlayerNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotPlayerException;
import com.netprogs.minecraft.plugins.social.command.help.HelpMessage;
import com.netprogs.minecraft.plugins.social.command.help.HelpSegment;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.config.PluginConfig;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.ISocialNetworkSettings;
import com.netprogs.minecraft.plugins.social.config.settings.SettingsConfig;
import com.netprogs.minecraft.plugins.social.storage.SocialNetwork;

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

public class CommandJoin extends SocialNetworkCommand<ISocialNetworkSettings> implements IWaitCommand {

    private final Logger logger = Logger.getLogger("Minecraft");

    public CommandJoin() {
        super(SocialNetworkCommandType.join);
    }

    public boolean run(CommandSender sender, List<String> arguments) throws ArgumentsMissingException,
            InvalidPermissionsException, SenderNotPlayerException {

        // verify that the sender is actually a player
        verifySenderAsPlayer(sender);

        // check permissions
        if (!hasCommandPermission(sender)) {
            throw new InvalidPermissionsException();
        }

        Player player = (Player) sender;

        // attempt to run the secondary commands first
        boolean commandHandled = handleSecondaryCommands(player, arguments);

        // there were none, so lets try the main friend request command
        if (!commandHandled) {

            // check arguments
            if (arguments.size() != 0) {
                throw new ArgumentsMissingException();
            }

            // get the social network data
            SocialNetwork socialConfig = SocialNetwork.getInstance();

            // check to see if the person is already there, if not, then start to add them
            SocialPerson person = socialConfig.getPerson(player.getName());
            if (person != null) {

                String alreadyJoined =
                        PluginConfig.getInstance().getConfig(ResourcesConfig.class)
                                .getResource("social.error.alreadyJoined.sender");

                sender.sendMessage(ChatColor.RED + alreadyJoined);

                return false;
            }

            // create the person instance and add to the network
            SocialPerson playerPerson = socialConfig.addPerson(player.getName());

            // now, check the setting to see if we should be asking for them to choose a gender
            SettingsConfig settingsConfig = PluginConfig.getInstance().getConfig(SettingsConfig.class);
            if (settingsConfig.isGenderChoiceRequired()) {

                // okay, we need to ask them for their gender
                // first we'll create their account in the network, and assign it waitState.waitGenderResponse
                playerPerson.waitOn(WaitState.waitGenderResponse, SocialNetworkCommandType.join);

                // now tell the user they need to use the /join <male/female> command
                MessageUtil.sendMessage(sender, "social.join.genderRequired.label.sender", ChatColor.GREEN);
                MessageUtil.sendMessage(sender, "social.join.genderRequired.commands.sender", ChatColor.GREEN);

            } else {

                // gender isn't required, so send the welcome message now
                MessageUtil.sendMessage(sender, "social.join.completed.sender", ChatColor.GOLD);
            }
        }

        return true;
    }

    private boolean handleSecondaryCommands(Player player, List<String> arguments) throws ArgumentsMissingException {

        // get the social network data
        SocialNetwork socialConfig = SocialNetwork.getInstance();

        // need to make sure we have at least one argument (the command parameter)
        if (arguments.size() >= 1) {

            // we only want to run this if the player is in the network
            SocialPerson playerPerson = socialConfig.getPerson(player.getName());
            if (playerPerson != null) {

                if (arguments.get(0).equals("male")) {

                    handleMale(player, playerPerson, arguments);
                    return true;

                } else if (arguments.get(0).equals("female")) {

                    handleFemale(player, playerPerson, arguments);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Assigns the player as being a Male.
     * @param player
     * @param playerPerson
     * @param arguments
     * @throws ArgumentsMissingException
     * @throws PlayerNotInNetworkException
     */
    private void handleMale(Player player, SocialPerson playerPerson, List<String> arguments)
            throws ArgumentsMissingException {

        // check arguments
        if (arguments.size() == 0) {
            throw new ArgumentsMissingException();
        }

        // get the social network data
        SocialNetwork socialConfig = SocialNetwork.getInstance();

        // check to see if the person is already there, if not, then start to add them
        SocialPerson person = socialConfig.getPerson(player.getName());
        if (person != null) {

            // set their gender and remove their wait state
            person.setGender(Gender.male);
            person.waitOn(WaitState.notWaiting, null);

            // save the changes to disk
            socialConfig.savePerson(person);

            // send the welcome message now
            MessageUtil.sendMessage(player, "social.join.completed.sender", ChatColor.GOLD);
        }
    }

    /**
     * Assigns the player as being a Male.
     * @param player
     * @param playerPerson
     * @param arguments
     * @throws ArgumentsMissingException
     * @throws PlayerNotInNetworkException
     */
    private void handleFemale(Player player, SocialPerson playerPerson, List<String> arguments)
            throws ArgumentsMissingException {

        // check arguments
        if (arguments.size() == 0) {
            throw new ArgumentsMissingException();
        }

        // get the social network data
        SocialNetwork socialConfig = SocialNetwork.getInstance();

        // check to see if the person is already there, if not, then start to add them
        SocialPerson person = socialConfig.getPerson(player.getName());
        if (person != null) {

            // set their gender and remove their wait state
            person.setGender(Gender.female);
            person.waitOn(WaitState.notWaiting, null);

            // save the changes to disk
            socialConfig.savePerson(person);

            // send the welcome message now
            MessageUtil.sendMessage(player, "social.join.completed.sender", ChatColor.GOLD);
        }
    }

    /**
     * This determines if we're waiting for a response from the user for this command.
     */
    @Override
    public boolean isValidWaitReponse(CommandSender sender, SocialPerson person) {

        // get the wait state from the person
        WaitState waitState = person.getWaitState();

        // we're looking for a gender wait, so check for it
        if (waitState != WaitState.waitGenderResponse) {

            // not what we're looking for, so display the wait help
            displayWaitHelp(sender);

            return false;
        }

        return true;
    }

    @Override
    public void displayWaitHelp(CommandSender sender) {

        // send the error message saying we're still waiting
        MessageUtil.sendMessage(sender, "social.wait.waitingForResponse.sender", ChatColor.RED);

        // the requested command wasn't for setting gender, so let's remind them they have to
        MessageUtil.sendMessage(sender, "social.join.genderRequired.label.sender", ChatColor.GREEN);
        MessageUtil.sendMessage(sender, "social.join.genderRequired.commands.sender", ChatColor.GREEN);
    }

    @Override
    public HelpSegment help() {

        ResourcesConfig config = PluginConfig.getInstance().getConfig(ResourcesConfig.class);

        HelpMessage mainCommand = new HelpMessage();
        mainCommand.setCommand(getCommandType().toString());
        mainCommand.setDescription(config.getResource("social.join.help"));

        HelpSegment helpSegment = new HelpSegment(getCommandType());
        helpSegment.addEntry(mainCommand);

        return helpSegment;
    }
}
