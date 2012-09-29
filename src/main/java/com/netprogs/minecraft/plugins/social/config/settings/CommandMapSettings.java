package com.netprogs.minecraft.plugins.social.config.settings;

import java.util.HashMap;
import java.util.Map;

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

public class CommandMapSettings {

    private boolean enabled;

    private Map<String, String> commandMap;
    private transient Map<String, String> reverseCommandMap;

    public CommandMapSettings() {

        commandMap = new HashMap<String, String>();
        reverseCommandMap = new HashMap<String, String>();
    }

    public Map<String, String> getCommandMap() {
        return commandMap;
    }

    public String getCustomCommand(String actualCommand) {

        if (reverseCommandMap.size() == 0) {
            for (String customCommand : commandMap.keySet()) {
                reverseCommandMap.put(commandMap.get(customCommand), customCommand);
            }
        }

        return reverseCommandMap.get(actualCommand);
    }

    public boolean isEnabled() {
        return enabled;
    }
}
