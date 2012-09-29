package com.netprogs.minecraft.plugins.social.command.help;

import java.util.ArrayList;
import java.util.List;

import com.netprogs.minecraft.plugins.social.command.ISocialNetworkCommand;
import com.netprogs.minecraft.plugins.social.config.settings.ISocialNetworkSettings;

import org.apache.commons.lang.StringUtils;

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

public class HelpPage {

    private String title;

    private List<ISocialNetworkCommand<? extends ISocialNetworkSettings>> commands =
            new ArrayList<ISocialNetworkCommand<? extends ISocialNetworkSettings>>();

    public HelpPage() {
        this.title = StringUtils.EMPTY;
    }

    public HelpPage(String title) {
        this.title = title;
    }

    public List<ISocialNetworkCommand<? extends ISocialNetworkSettings>> getCommands() {
        return commands;
    }

    public void addCommand(ISocialNetworkCommand<? extends ISocialNetworkSettings> command) {
        this.commands.add(command);
    }

    public String getTitle() {
        return title;
    }
}
