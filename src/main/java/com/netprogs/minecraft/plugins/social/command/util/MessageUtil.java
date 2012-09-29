package com.netprogs.minecraft.plugins.social.command.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;

import org.apache.commons.lang.StringUtils;
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

public class MessageUtil {

    public static void sendHeaderMessage(CommandSender receiver, String resource, int pageNumber, int maxPages) {

        ResourcesConfig resources = SocialNetworkPlugin.getResources();

        ChatColor SPACER_COLOR = ChatColor.YELLOW;
        ChatColor TITLE_COLOR = ChatColor.AQUA;

        // create our header
        String title = resources.getResource(resource);
        if (title == null) {
            SocialNetworkPlugin.logger().log(Level.SEVERE, "Could not find resource: " + resource);
            return;
        }

        title = " " + title + " ";

        if (pageNumber != 0 && maxPages != 0) {
            title += "(" + pageNumber + "/" + maxPages + ") ";
        }

        String headerSpacer = StringUtils.repeat("-", 55);

        int midPoint = ((headerSpacer.length() / 2) - (title.length() / 2));
        String start = headerSpacer.substring(0, midPoint);
        String middle = title;
        String end = headerSpacer.substring(midPoint + title.length());

        // combine it all into the final header
        String displayHeader = SPACER_COLOR + start + TITLE_COLOR + middle + SPACER_COLOR + end;

        // send the message
        displayHeader = displayHeader.replaceAll("(&([A-Fa-f0-9L-Ol-o]))", "\u00A7$2");
        receiver.sendMessage(displayHeader);
    }

    public static void sendHeaderMessage(CommandSender receiver, String resource) {

        sendHeaderMessage(receiver, resource, 0, 0);

        /*
        ResourcesConfig resources = SocialNetworkPlugin.getResources();

        ChatColor SPACER_COLOR = ChatColor.YELLOW;
        ChatColor TITLE_COLOR = ChatColor.AQUA;

        // create our header
        String title = resources.getResource(resource);
        if (title == null) {
            SocialNetworkPlugin.logger().log(Level.SEVERE, "Could not find resource: " + resource);
            return;
        }

        title = " " + title + " ";

        String headerSpacer = StringUtils.repeat("-", 52);

        int midPoint = ((headerSpacer.length() / 2) - (title.length() / 2));
        String start = headerSpacer.substring(0, midPoint);
        String middle = title;
        String end = headerSpacer.substring(midPoint + title.length());

        // combine it all into the final header
        String displayHeader = SPACER_COLOR + start + TITLE_COLOR + middle + SPACER_COLOR + end;

        // send the message
        displayHeader = displayHeader.replaceAll("(&([A-Fa-f0-9L-Ol-o]))", "\u00A7$2");
        receiver.sendMessage(displayHeader);
        */
    }

    public static void sendFooterMessage(CommandSender receiver, String resource) {

        ResourcesConfig resources = SocialNetworkPlugin.getResources();

        ChatColor FOOTER_COLOR = ChatColor.DARK_GRAY;

        // create our header
        String footer = resources.getResource(resource);
        if (footer == null) {
            SocialNetworkPlugin.logger().log(Level.SEVERE, "Could not find resource: " + resource);
            return;
        }

        footer = " " + footer + " ";

        String headerSpacer = StringUtils.repeat(" ", 65);

        int midPoint = ((headerSpacer.length() / 2) - (footer.length() / 2));
        String start = headerSpacer.substring(0, midPoint);
        String middle = footer;
        String end = headerSpacer.substring(midPoint + footer.length());

        // combine it all into the final header
        String displayFooter = FOOTER_COLOR + start + FOOTER_COLOR + middle + end;

        // send the message
        displayFooter = displayFooter.replaceAll("(&([A-Fa-f0-9L-Ol-o]))", "\u00A7$2");
        receiver.sendMessage(displayFooter);
    }

    public static void sendFooterLinesOnly(CommandSender receiver) {

        ChatColor SPACER_COLOR = ChatColor.YELLOW;
        String displayFooter = StringUtils.repeat("-", 50);

        receiver.sendMessage(SPACER_COLOR + displayFooter);
    }

    public static void sendMessage(CommandSender receiver, String message) {

        message = message.replaceAll("(&([A-Fa-f0-9L-Ol-o]))", "\u00A7$2");
        receiver.sendMessage(message);
    }

    public static void sendMessage(SocialPerson receiver, String message) {

        message = message.replaceAll("(&([A-Fa-f0-9L-Ol-o]))", "\u00A7$2");
        Player player = Bukkit.getServer().getPlayer(receiver.getName());
        if (player != null) {
            player.sendMessage(message);
        }
    }

    public static void sendMessage(SocialPerson receiver, String resource, ChatColor baseColor) {

        Player player = Bukkit.getServer().getPlayer(receiver.getName());
        if (player != null) {
            MessageUtil.sendMessage(player, resource, baseColor);
        }
    }

    public static void sendMessage(CommandSender receiver, String resource, ChatColor baseColor) {

        String requestSenderMessage = SocialNetworkPlugin.getResources().getResource(resource);
        if (requestSenderMessage == null) {
            SocialNetworkPlugin.logger().log(Level.SEVERE, "Could not find resource: " + resource);
            return;
        }

        requestSenderMessage = requestSenderMessage.replaceAll("(&([A-Fa-f0-9L-Ol-o]))", "\u00A7$2");
        receiver.sendMessage(baseColor + requestSenderMessage);
    }

    public static void sendMessage(CommandSender receiver, String resource, ChatColor baseColor,
            MessageParameter messageVariable) {

        String requestSenderMessage = SocialNetworkPlugin.getResources().getResource(resource);
        if (requestSenderMessage == null) {
            SocialNetworkPlugin.logger().log(Level.SEVERE, "Could not find resource: " + resource);
            return;
        }

        requestSenderMessage =
                requestSenderMessage.replaceAll(messageVariable.getKey(), messageVariable.getChatColor()
                        + messageVariable.getValue() + baseColor);

        requestSenderMessage = requestSenderMessage.replaceAll("(&([A-Fa-f0-9L-Ol-o]))", "\u00A7$2");

        receiver.sendMessage(baseColor + requestSenderMessage);
    }

    public static void sendMessage(CommandSender receiver, String resource, ChatColor baseColor,
            List<MessageParameter> messageVariables) {

        String requestSenderMessage = SocialNetworkPlugin.getResources().getResource(resource);
        if (requestSenderMessage == null) {
            SocialNetworkPlugin.logger().log(Level.SEVERE, "Could not find resource: " + resource);
            return;
        }

        for (MessageParameter messageVariable : messageVariables) {

            requestSenderMessage =
                    requestSenderMessage.replaceAll(messageVariable.getKey(), messageVariable.getChatColor()
                            + messageVariable.getValue() + baseColor);
        }

        requestSenderMessage = requestSenderMessage.replaceAll("(&([A-Fa-f0-9L-Ol-o]))", "\u00A7$2");
        receiver.sendMessage(baseColor + requestSenderMessage);
    }

    public static void sendGlobalMessage(String resource, ChatColor baseColor, List<MessageParameter> messageVariables) {

        String requestSenderMessage = SocialNetworkPlugin.getResources().getResource(resource);
        if (requestSenderMessage == null) {
            SocialNetworkPlugin.logger().log(Level.SEVERE, "Could not find resource: " + resource);
            return;
        }

        for (MessageParameter messageVariable : messageVariables) {

            requestSenderMessage =
                    requestSenderMessage.replaceAll(messageVariable.getKey(), messageVariable.getChatColor()
                            + messageVariable.getValue() + baseColor);
        }

        requestSenderMessage = requestSenderMessage.replaceAll("(&([A-Fa-f0-9L-Ol-o]))", "\u00A7$2");

        Bukkit.getServer().broadcastMessage(baseColor + requestSenderMessage);
    }

    public static void sendLoginMessageCountMessage(CommandSender sender, String messageType, int count) {

        MessageParameter numRequests = new MessageParameter("<messageCount>", Integer.toString(count), ChatColor.AQUA);
        MessageParameter messageName = new MessageParameter("<messageType>", messageType, ChatColor.GOLD);

        List<MessageParameter> requestParameters = new ArrayList<MessageParameter>();
        requestParameters.add(numRequests);
        requestParameters.add(messageName);

        sendMessage(sender, "social.login.availableMessages.sender", ChatColor.GOLD, requestParameters);
    }

    /*
     * You tried to do something with someone that has you on their ignore list.
     */
    public static void sendSenderIgnoredMessage(CommandSender sender, String playerName) {

        MessageUtil.sendMessage(sender, "social.ignore.playerIgnored.sender", ChatColor.RED, new MessageParameter(
                "<player>", playerName, ChatColor.AQUA));
    }

    /*
     * You tried to do something with someone on your own ignore list.
     */
    public static void sendPlayerIgnoredMessage(CommandSender sender, String playerName) {

        MessageUtil.sendMessage(sender, "social.ignore.playerIgnored.player", ChatColor.RED, new MessageParameter(
                "<player>", playerName, ChatColor.AQUA));
    }

    public static void sendInvalidPermissionsMessage(CommandSender sender) {

        sendMessage(sender, "social.error.invalidPermissions", ChatColor.RED);
    }

    public static void sendInvalidPerkMessage(CommandSender sender) {

        sendMessage(sender, "social.error.invalidPerk", ChatColor.RED);
    }

    public static void sendSenderNotPlayerMessage(CommandSender sender) {

        sendMessage(sender, "social.error.senderNotPlayer", ChatColor.RED);
    }

    public static void sendUnknownArgumentsMessage(CommandSender sender) {

        sendMessage(sender, "social.error.unknownArguments", ChatColor.RED);
    }

    public static void sendSenderNotInNetworkMessage(CommandSender sender) {

        sendMessage(sender, "social.error.senderNotInNetwork", ChatColor.RED);
    }

    public static void sendPlayerNotInNetworkMessage(CommandSender sender, String playerNotInNetworkName) {

        sendMessage(sender, "social.error.playerNotInNetwork", ChatColor.RED, new MessageParameter("<player>",
                playerNotInNetworkName, ChatColor.AQUA));
    }

    public static void sendPlayerNotOnlineMessage(CommandSender sender, String offlinePlayerName) {

        sendMessage(sender, "social.error.offlinePlayer", ChatColor.RED, new MessageParameter("<player>",
                offlinePlayerName, ChatColor.AQUA));
    }

    public static void sendPersonNotInGroupMessage(CommandSender sender, String personName) {

        sendMessage(sender, "social.error.personNotInGroup", ChatColor.RED, new MessageParameter("<player>",
                personName, ChatColor.AQUA));
    }

    public static void sendPersonNotInPerksMessage(CommandSender sender, String personName) {

        sendMessage(sender, "social.error.personNotInPerks", ChatColor.RED, new MessageParameter("<player>",
                personName, ChatColor.AQUA));
    }

    public static void sendNotEnoughFundsMessage(CommandSender sender, double price) {

        NumberFormat formatter = new DecimalFormat("###.00");
        String itemDisplay = formatter.format(price);

        MessageUtil.sendMessage(sender, "social.error.notEnoughFunds", ChatColor.RED, new MessageParameter("<price>",
                itemDisplay, ChatColor.GOLD));
    }

    public static void sendCommandOnCooldownMessage(CommandSender sender, long timeRemaining) {

        MessageUtil.sendMessage(sender, "social.error.commandOnCooldown", ChatColor.RED,
                createCoolDownFormatting(timeRemaining));
    }

    public static List<MessageParameter> createCoolDownFormatting(long timeRemaining) {

        SimpleDateFormat hourFormat = new SimpleDateFormat("HH");
        hourFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        SimpleDateFormat minFormat = new SimpleDateFormat("mm");
        minFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        SimpleDateFormat secFormat = new SimpleDateFormat("ss");
        secFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        MessageParameter hour = new MessageParameter("<hours>", hourFormat.format(timeRemaining), ChatColor.GOLD);
        MessageParameter min = new MessageParameter("<minutes>", minFormat.format(timeRemaining), ChatColor.GOLD);
        MessageParameter sec = new MessageParameter("<seconds>", secFormat.format(timeRemaining), ChatColor.GOLD);

        List<MessageParameter> params = new ArrayList<MessageParameter>();
        params.add(hour);
        params.add(min);
        params.add(sec);

        return params;
    }
}
