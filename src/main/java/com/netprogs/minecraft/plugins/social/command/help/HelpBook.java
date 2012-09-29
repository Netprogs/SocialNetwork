package com.netprogs.minecraft.plugins.social.command.help;

import java.util.ArrayList;
import java.util.List;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.command.ISocialNetworkCommand;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommandType;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.CommandMapSettings;
import com.netprogs.minecraft.plugins.social.config.settings.ISocialNetworkSettings;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

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
 * A {@link HelpBook} consists of instances of {@link HelpPage}.
 * {@link HelpPage} consists of instances of {@link HelpSegment}. (parts of a single page).
 * 
 * You then add entries to the HelpSegments using {@link HelpMessage} and {@link HelpText}.
 * </pre>
 * @author Scott
 */
public class HelpBook {

    public final static ChatColor COMMAND_COLOR = ChatColor.AQUA;
    public final static ChatColor DESCRIPTION_COLOR = ChatColor.YELLOW;

    public final static ChatColor SPACER_COLOR = ChatColor.YELLOW;

    public final static ChatColor SEGMENT_TITLE_COLOR = ChatColor.LIGHT_PURPLE;

    public final static String TITLE_COLOR = "&a";
    public final static String PAGE_TITLE_COLOR = "&d";
    public final static String PARAMS_COLOR = "&3";

    private List<HelpPage> helpPages;

    public HelpBook() {

        helpPages = new ArrayList<HelpPage>();
    }

    public static HelpMessage generateHelpMessage(String mainCommand, String subCommand, String arguments,
            String description) {

        CommandMapSettings customMapSettings = SocialNetworkPlugin.getSettings().getCommandMapSettings();

        String command = "";
        if (StringUtils.isNotEmpty(mainCommand)) {
            command += mainCommand;
        }

        if (StringUtils.isNotEmpty(subCommand)) {
            if (StringUtils.isNotEmpty(command)) {
                command += " ";
            }
            command += subCommand;
        }

        String customRequestCommand = customMapSettings.getCustomCommand(command);

        HelpMessage helpMessage = new HelpMessage();

        if (customRequestCommand != null && customMapSettings.isEnabled()) {

            helpMessage.setCommand(customRequestCommand);

        } else {

            helpMessage.setCommand(command);
        }

        helpMessage.setArguments(arguments);
        helpMessage.setDescription(description);

        return helpMessage;
    }

    public void addPage(HelpPage page) {
        helpPages.add(page);
    }

    public boolean sendHelpPage(CommandSender sender, String pluginName, int pageNumber) {

        SocialPerson senderPerson = SocialNetworkPlugin.getStorage().getPerson(sender.getName());

        // Go through the pages and for each segment within it, determine if the user is allowed to use that command
        // Each segment is given the permission so we just use that.
        // If by the time we're done with the page, nothing is left in it, then we won't add it to the final list.
        List<HelpPage> availableHelpPages = new ArrayList<HelpPage>();
        for (HelpPage helpPage : helpPages) {

            HelpPage newHelpPage = new HelpPage(helpPage.getTitle());

            for (ISocialNetworkCommand<? extends ISocialNetworkSettings> helpCommand : helpPage.getCommands()) {

                // if they're allowed access, add the segment
                if (SocialNetworkPlugin.getVault().hasCommandPermission(sender, helpCommand.getCommandType())) {
                    newHelpPage.addCommand(helpCommand);
                }

                if (senderPerson != null) {
                    if (helpCommand.getCommandType() == SocialNetworkCommandType.priest && senderPerson.isPriest()) {

                        newHelpPage.addCommand(helpCommand);
                    }
                }

                if (senderPerson != null) {
                    if (helpCommand.getCommandType() == SocialNetworkCommandType.lawyer && senderPerson.isLawyer()) {

                        newHelpPage.addCommand(helpCommand);
                    }
                }
            }

            // if there are any segments left, add the page to the pages list
            if (newHelpPage.getCommands().size() > 0) {
                availableHelpPages.add(newHelpPage);
            }
        }

        // create and send the help title
        String header = createHeader(pluginName, availableHelpPages, pageNumber);
        sendMessage(sender, header);

        // get the resources
        ResourcesConfig resources = SocialNetworkPlugin.getResources();

        // check to see if the user has any pages available to them
        if (availableHelpPages.size() == 0) {

            String helpTitle = resources.getResource("social.help.noneAvailable");
            sendMessage(sender, ChatColor.RED + helpTitle);
            return false;
        }

        // check to make sure the page number is valid
        if (pageNumber <= 0 || pageNumber > availableHelpPages.size()) {

            String helpTitle = resources.getResource("social.help.wrongPage");
            sendMessage(sender, ChatColor.RED + helpTitle);
            return false;
        }

        HelpPage helpPage = availableHelpPages.get(pageNumber - 1);
        generateHelpMessages(sender, helpPage);

        // send the footer
        MessageUtil.sendFooterMessage(sender, "social.help.footer");

        return true;
    }

    private void generateHelpMessages(CommandSender sender, HelpPage helpPage) {

        // if there was a title, display it now
        if (!StringUtils.isEmpty(helpPage.getTitle())) {
            sendMessage(sender, PAGE_TITLE_COLOR + helpPage.getTitle());
        }

        // now display every segment that was added to this page
        for (ISocialNetworkCommand<? extends ISocialNetworkSettings> helpCommand : helpPage.getCommands()) {

            HelpSegment helpSegment = helpCommand.help();

            // if there was a title, display it now
            if (!StringUtils.isEmpty(helpSegment.getTitle())) {
                sendMessage(sender, SEGMENT_TITLE_COLOR + helpSegment.getTitle());
            }

            // display every entry in the segment
            for (IHelpEntry helpEntry : helpSegment.getEntries()) {
                sendMessage(sender, helpEntry.display());
            }
        }
    }

    private void sendMessage(CommandSender receiver, String message) {

        message = message.replaceAll("(&([A-Fa-f0-9L-Ol-o]))", "\u00A7$2");
        receiver.sendMessage(message);
    }

    private String createHeader(String pluginName, List<HelpPage> helpPageList, int pageNumber) {

        ResourcesConfig resources = SocialNetworkPlugin.getResources();

        // create our header
        String helpTitle = resources.getResource("social.help.header");
        helpTitle = " " + helpTitle + " ";
        helpTitle = helpTitle.replaceAll("<plugin>", pluginName);

        if (helpPageList.size() > 0) {
            helpTitle += " (" + pageNumber + "/" + helpPageList.size() + ") ";
        }

        String headerSpacer = StringUtils.repeat("-", 52);

        int midPoint = ((headerSpacer.length() / 2) - (helpTitle.length() / 2));
        String start = headerSpacer.substring(0, midPoint);
        String middle = helpTitle;
        String end = headerSpacer.substring(midPoint + helpTitle.length());

        // combine it all into the final header
        String displayHeader = SPACER_COLOR + start + TITLE_COLOR + middle + SPACER_COLOR + end;

        return displayHeader;
    }
}
