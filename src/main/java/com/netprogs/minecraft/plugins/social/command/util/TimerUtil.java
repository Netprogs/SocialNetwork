package com.netprogs.minecraft.plugins.social.command.util;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import com.netprogs.minecraft.plugins.social.command.ISocialNetworkCommand.ICommandType;
import com.netprogs.minecraft.plugins.social.config.PluginConfig;
import com.netprogs.minecraft.plugins.social.config.settings.SettingsConfig;

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
 * All timers are lost after a server restart. I don't really see the advantage of persisting these.
 * However, I'm not against the idea should people want it.
 */
public class TimerUtil {

    private static final Logger logger = Logger.getLogger("Minecraft");

    // Map<PlayerName, Map<CommandType, TimeInSeconds>>
    private static final Map<String, Map<ICommandType, Long>> commandTimers =
            new HashMap<String, Map<ICommandType, Long>>();

    // Map<PlayerName, Map<StringEventName, TimeInSeconds>>
    private static final Map<String, Map<String, Long>> eventTimers = new HashMap<String, Map<String, Long>>();

    private final static ReentrantReadWriteLock rwCommandLock = new ReentrantReadWriteLock(true);
    private final static ReentrantReadWriteLock rwEventLock = new ReentrantReadWriteLock(true);

    /**
     * Cleans up old timers to let them get GC and reduce memory stamp.
     */
    private static void cleanCommandTimers() {

        for (Iterator<String> it = commandTimers.keySet().iterator(); it.hasNext();) {

            String playerName = it.next();
            Map<ICommandType, Long> playerMap = commandTimers.get(playerName);

            // get the timer map for the player
            for (Iterator<ICommandType> playerMapIterator = playerMap.keySet().iterator(); playerMapIterator.hasNext();) {

                ICommandType commandType = playerMapIterator.next();
                long lastCommandTime = playerMap.get(commandType);

                if (lastCommandTime <= System.currentTimeMillis()) {
                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("Removing expired command timer: [" + playerName + ", " + commandType + "]");
                    }
                    playerMapIterator.remove();
                }
            }
        }
    }

    /**
     * Cleans up old timers to let them get GC and reduce memory stamp.
     */
    private static void cleanEventTimers() {

        for (Iterator<String> it = eventTimers.keySet().iterator(); it.hasNext();) {

            String playerName = it.next();
            Map<String, Long> playerMap = eventTimers.get(playerName);

            // get the timer map for the player
            for (Iterator<String> playerMapIterator = playerMap.keySet().iterator(); playerMapIterator.hasNext();) {

                String eventType = playerMapIterator.next();
                long lastCommandTime = playerMap.get(eventType);

                if (lastCommandTime <= System.currentTimeMillis()) {
                    logger.info("Removing expired event timer: [" + playerName + ", " + eventType + "]");
                    playerMapIterator.remove();
                }
            }
        }
    }

    /**
     * Determines if the provided command is on timer for the user.
     * @param player The name of the player running the command.
     * @param socialCommand The command being run.
     * @return Amount of time remaining. If 0, means it's not on timer.
     */
    public static long commandOnTimer(String playerName, ICommandType commandType) {

        Lock lock = rwCommandLock.readLock();
        lock.lock();
        try {

            // check the timer map to see if they have one there already
            Map<ICommandType, Long> timerInfo = commandTimers.get(playerName);
            if (timerInfo != null && timerInfo.containsKey(commandType)) {

                long lastCommandTime = timerInfo.get(commandType);

                if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                    logger.info("commandOnTimer, lastCommandTime: " + formatTime(lastCommandTime) + " > "
                            + formatTime(System.currentTimeMillis()));
                }

                // check to see if they're allowed to use the command
                if (lastCommandTime > System.currentTimeMillis()) {

                    long remaining = (lastCommandTime - System.currentTimeMillis());

                    // String timeRemaining = formatTime(remaining);
                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("commandOnTimer, timeRemaining: " + formatTime(remaining));
                    }

                    // It's on timer, return the time
                    return remaining;
                }
            }

            return 0L;

        } finally {
            lock.unlock();
        }
    }

    /**
     * Updates the command timer for the player.
     * @param playerName The name of the player running the command.
     * @param commandType The command type.
     * @param timer The new timer period to assign (in seconds).
     */
    public static void updateCommandTimer(String playerName, ICommandType commandType, long timer) {

        Lock lock = rwCommandLock.writeLock();
        lock.lock();
        try {

            // clean out old timers
            cleanCommandTimers();

            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                logger.info("updateCommandTimer: " + commandType + " " + timer);
            }

            // check the timer map to see if they have one there already
            Map<ICommandType, Long> timerInfo = commandTimers.get(playerName);
            if (timerInfo == null) {

                // nope, let's create it
                timerInfo = new HashMap<ICommandType, Long>();
                commandTimers.put(playerName, timerInfo);

                if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                    logger.info("Created new timer entry for: " + commandType);
                }
            }

            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                logger.info("Updating timer for command: " + commandType + " to: "
                        + formatTime(System.currentTimeMillis() + (timer * 1000)));
            }

            // now update the cooldown
            timerInfo.put(commandType, (System.currentTimeMillis() + (timer * 1000)));

        } finally {
            lock.unlock();
        }
    }

    /**
     * Determines if the provided command is on timer for the user.
     * @param player The name of the player running the command.
     * @param socialCommand The command being run.
     * @return Amount of time remaining. If 0, means it's not on timer.
     */
    public static long eventOnTimer(String playerName, String eventType) {

        Lock lock = rwEventLock.readLock();
        lock.lock();
        try {

            // check the timer map to see if they have one there already
            Map<String, Long> timerInfo = eventTimers.get(playerName);
            if (timerInfo != null && timerInfo.containsKey(eventType)) {

                long lastCommandTime = timerInfo.get(eventType);

                if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                    logger.info("eventOnTimer, lastCommandTime: " + formatTime(lastCommandTime) + " > "
                            + formatTime(System.currentTimeMillis()));
                }

                // check to see if they're allowed to use the command
                if (lastCommandTime > System.currentTimeMillis()) {

                    long remaining = (lastCommandTime - System.currentTimeMillis());

                    // String timeRemaining = formatTime(remaining);
                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("eventOnTimer, timeRemaining: " + formatTime(remaining));
                    }

                    // It's on timer, return the time
                    return remaining;
                }
            }

            return 0L;

        } finally {
            lock.unlock();
        }
    }

    /**
     * Updates the command timer for the player.
     * @param playerName The name of the player running the command.
     * @param commandType The command type.
     * @param timer The new timer period to assign (in seconds).
     */
    public static void updateEventTimer(String playerName, String eventType, long timer) {

        Lock lock = rwEventLock.writeLock();
        lock.lock();
        try {
            // clean out old timers
            cleanEventTimers();

            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                logger.info("updateEventTimer: " + eventType + " " + timer);
            }

            // check the timer map to see if they have one there already
            Map<String, Long> timerInfo = eventTimers.get(playerName);
            if (timerInfo == null) {

                // nope, let's create it
                timerInfo = new HashMap<String, Long>();
                eventTimers.put(playerName, timerInfo);

                if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                    logger.info("Created new timer entry for: " + eventType);
                }
            }

            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                logger.info("Updating timer for command: " + eventType + " to: "
                        + formatTime(System.currentTimeMillis() + (timer * 1000)));
            }

            // now update the cooldown
            timerInfo.put(eventType, (System.currentTimeMillis() + (timer * 1000)));

        } finally {
            lock.unlock();
        }
    }

    public static String formatTime(long time) {

        SimpleDateFormat hourFormat = new SimpleDateFormat("HH");
        hourFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        SimpleDateFormat minFormat = new SimpleDateFormat("mm");
        minFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        SimpleDateFormat secFormat = new SimpleDateFormat("ss");
        secFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        String timeRemaining = hourFormat.format(time) + " hours, ";
        timeRemaining += minFormat.format(time) + " minutes, ";
        timeRemaining += secFormat.format(time) + " seconds";

        return timeRemaining;
    }
}
