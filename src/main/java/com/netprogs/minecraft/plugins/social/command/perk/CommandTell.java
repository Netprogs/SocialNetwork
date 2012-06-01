package com.netprogs.minecraft.plugins.social.command.perk;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommandType;
import com.netprogs.minecraft.plugins.social.command.exception.ArgumentsMissingException;
import com.netprogs.minecraft.plugins.social.command.exception.InvalidPermissionsException;
import com.netprogs.minecraft.plugins.social.command.exception.PlayerNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.exception.PlayerNotOnlineException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.exception.SenderNotPlayerException;
import com.netprogs.minecraft.plugins.social.command.help.HelpMessage;
import com.netprogs.minecraft.plugins.social.command.help.HelpSegment;
import com.netprogs.minecraft.plugins.social.command.util.MessageParameter;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.config.PluginConfig;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.perk.TellSettings;
import com.netprogs.minecraft.plugins.social.storage.SocialNetwork;
import com.netprogs.minecraft.plugins.social.storage.data.perk.IPersonPerkSettings;

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

/**
 * <pre>
 * Perk: Tell
 * 
 * Allow members to send private messages to each other.
 * 
 *  /s tell <player>  <message>   Sends a private message to another player, but only if they are online.
 * 
 * </pre>
 */
public class CommandTell extends PerkCommand<TellSettings, IPersonPerkSettings> {

    private final Logger logger = Logger.getLogger("Minecraft");

    public CommandTell() {
        super(SocialNetworkCommandType.tell);
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
        SocialPerson playerPerson = SocialNetwork.getInstance().getPerson(player.getName());
        if (playerPerson == null) {
            throw new SenderNotInNetworkException();
        }

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
        SocialPerson sendToPerson = SocialNetwork.getInstance().getPerson(sendPlayerName);
        if (sendToPerson == null) {
            throw new PlayerNotInNetworkException(sendPlayerName);
        }

        // make sure the teleport to person is online
        Player sendToPlayer = Bukkit.getServer().getPlayer(sendToPerson.getName());
        if (sendToPlayer == null) {
            throw new PlayerNotOnlineException(sendToPerson.getName());
        }

        // make sure the person is in your social groups
        TellSettings tellSettings = getPerkSettings(playerPerson, sendToPerson);
        if (tellSettings == null) {
            MessageUtil.sendPersonNotInPerksMessage(player, sendToPerson.getName());
            return false;
        }

        // If I have them on ignore...
        // Check to see if the person they're trying to tell is on their own ignore list
        if (playerPerson.isOnIgnore(sendToPerson.getName())) {
            MessageUtil.sendPlayerIgnoredMessage(player, sendToPerson.getName());
            return false;
        }

        // If they have me on ignore...
        // Check to see if the person they're sending a tell to is on their ignore list
        if (sendToPerson.isOnIgnore(playerPerson.getName())) {
            MessageUtil.sendSenderIgnoredMessage(player, sendToPerson.getName());
            return false;
        }

        MessageParameter senderName = new MessageParameter("<sender>", playerPerson.getName(), ChatColor.AQUA);
        MessageParameter playerName = new MessageParameter("<player>", sendToPerson.getName(), ChatColor.AQUA);
        MessageParameter messageName = new MessageParameter("<message>", message, ChatColor.GOLD);

        List<MessageParameter> requestParameters = new ArrayList<MessageParameter>();
        requestParameters.add(senderName);
        requestParameters.add(playerName);
        requestParameters.add(messageName);

        // send the tell
        MessageUtil.sendMessage(sendToPlayer, "social.perk.tell.send.completed.player", ChatColor.GREEN,
                requestParameters);

        // tell them it's been sent
        MessageUtil.sendMessage(player, "social.perk.tell.send.completed.sender", ChatColor.GREEN, requestParameters);

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
    public TellSettings getProcessPerkSettings(SocialPerson person, List<String> commandArguments) {

        // this command requires processing every time its used
        String sendPlayerName = commandArguments.get(0);

        SocialPerson sendToPerson = SocialNetwork.getInstance().getPerson(sendPlayerName);
        if (sendToPerson != null) {
            return getPerkSettings(person, sendToPerson);
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
        ResourcesConfig config = PluginConfig.getInstance().getConfig(ResourcesConfig.class);

        HelpMessage mainCommand = new HelpMessage();
        mainCommand.setCommand(getCommandType().toString());
        mainCommand.setArguments("<player> <message>");
        mainCommand.setDescription(config.getResource("social.perk.tell.help.send"));
        helpSegment.addEntry(mainCommand);

        return helpSegment;
    }
}
