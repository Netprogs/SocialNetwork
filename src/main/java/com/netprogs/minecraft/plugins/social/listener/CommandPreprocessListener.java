package com.netprogs.minecraft.plugins.social.listener;

import java.util.ArrayList;
import java.util.List;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.config.settings.CommandMapSettings;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/*
 * Copyright (C) 2012 Scott Milne
 * 
 * "Social Network" is a Craftbukkit Minecraft server modification plug-in. It attempts to add a 
 * social environment to your server by allowing players to be placed into different types of social groups.
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

public class CommandPreprocessListener implements Listener {

    private final List<String> socialCommands = new ArrayList<String>();

    public CommandPreprocessListener() {

        // create the list of base commands this plug-in uses
        socialCommands.add("social");
        socialCommands.add("s");
        socialCommands.add("soc");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {

        // if this event has already been cancelled, don't bother continuing
        if (event.isCancelled()) {
            return;
        }

        CommandMapSettings settings = SocialNetworkPlugin.getSettings().getCommandMapSettings();

        // check to see if custom mapping is enabled
        if (!settings.isEnabled()) {
            return;
        }

        // get the message the player is attempting to execute
        String message = event.getMessage();

        // get the command portion
        String command = message.substring(1).split(" ")[0];

        // make sure we're only overriding our own commands
        if (socialCommands.contains(command.toLowerCase())) {

            // get the remaining parameters of the command
            String parameters = message.substring(command.length() + 1).trim();

            // now check the custom map for matches to this
            for (String customCommand : settings.getCommandMap().keySet()) {

                if (parameters.startsWith(customCommand)) {

                    // replace the original command with the custom one
                    message = message.replace(customCommand, settings.getCommandMap().get(customCommand));

                    // and tell the event to use the new one instead
                    event.setMessage(message);
                    return;
                }
            }
        }
    }
}
