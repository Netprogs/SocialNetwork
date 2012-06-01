package com.netprogs.minecraft.plugins.social;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import com.netprogs.minecraft.plugins.social.command.ISocialNetworkCommand.ICommandType;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommandType;
import com.netprogs.minecraft.plugins.social.config.PluginConfig;
import com.netprogs.minecraft.plugins.social.config.settings.SettingsConfig;
import com.netprogs.minecraft.plugins.social.config.settings.group.AffairSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.ChildSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.DivorceSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.EngagementSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.FriendSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.GroupSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.LawyerSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.MarriageSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.PriestSettings;
import com.netprogs.minecraft.plugins.social.config.settings.group.RelationshipSettings;
import com.netprogs.minecraft.plugins.social.config.settings.perk.IPerkSettings;
import com.netprogs.minecraft.plugins.social.event.PlayerMemberChangeEvent;
import com.netprogs.minecraft.plugins.social.event.PlayerMemberChangeEvent.Type;
import com.netprogs.minecraft.plugins.social.storage.IMessage;
import com.netprogs.minecraft.plugins.social.storage.data.Affair;
import com.netprogs.minecraft.plugins.social.storage.data.Alert;
import com.netprogs.minecraft.plugins.social.storage.data.Child;
import com.netprogs.minecraft.plugins.social.storage.data.Divorce;
import com.netprogs.minecraft.plugins.social.storage.data.Engagement;
import com.netprogs.minecraft.plugins.social.storage.data.Friend;
import com.netprogs.minecraft.plugins.social.storage.data.Marriage;
import com.netprogs.minecraft.plugins.social.storage.data.Person;
import com.netprogs.minecraft.plugins.social.storage.data.Relationship;
import com.netprogs.minecraft.plugins.social.storage.data.Request;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;

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

public class SocialPerson {

    private final Logger logger = Logger.getLogger("Minecraft");

    // use these for permissions also
    public enum Status {
        single, relationship, engaged, married, divorced
    }

    public enum Gender {
        male, female
    }

    public enum WaitState {
        notWaiting, waitGenderResponse, waitMarriageResponse, waitCashGiftVerification, waitHandGiftVerification
    }

    public enum WeddingVows {
        accepted, rejected
    }

    private Person person;

    // We use this to allow us to ignore the changes events during a large update
    // private transient boolean ignoreEvents;

    // Thread locking. These are set to be "fair" so first-come, first-serve.
    // Anyone can read. Locks reads while writing.
    private final ReentrantReadWriteLock rwMessageQueueLock = new ReentrantReadWriteLock(true);
    private final ReentrantReadWriteLock rwWaitLock = new ReentrantReadWriteLock(true);
    private final ReentrantReadWriteLock rwIgnoreLock = new ReentrantReadWriteLock(true);

    private final ReentrantReadWriteLock rwChildLock = new ReentrantReadWriteLock(true);
    private final ReentrantReadWriteLock rwFriendLock = new ReentrantReadWriteLock(true);
    private final ReentrantReadWriteLock rwAffairLock = new ReentrantReadWriteLock(true);
    private final ReentrantReadWriteLock rwRelationshipLock = new ReentrantReadWriteLock(true);

    private final ReentrantReadWriteLock rwNameLock = new ReentrantReadWriteLock(true);
    private final ReentrantReadWriteLock rwGenderLock = new ReentrantReadWriteLock(true);
    private final ReentrantReadWriteLock rwStatusLock = new ReentrantReadWriteLock(true);
    private final ReentrantReadWriteLock rwChildOfLock = new ReentrantReadWriteLock(true);
    private final ReentrantReadWriteLock rwEngagementLock = new ReentrantReadWriteLock(true);
    private final ReentrantReadWriteLock rwMarriageLock = new ReentrantReadWriteLock(true);
    private final ReentrantReadWriteLock rwWeddingLock = new ReentrantReadWriteLock(true);
    private final ReentrantReadWriteLock rwDivorceLock = new ReentrantReadWriteLock(true);
    private final ReentrantReadWriteLock rwLawyerLock = new ReentrantReadWriteLock(true);
    private final ReentrantReadWriteLock rwPriestLock = new ReentrantReadWriteLock(true);

    private final Map<String, SocialFriend> friends = new HashMap<String, SocialFriend>();
    private final Map<String, SocialAffair> affairs = new HashMap<String, SocialAffair>();
    private final Map<String, SocialChild> children = new HashMap<String, SocialChild>();
    private final Map<String, SocialRelationship> relationships = new HashMap<String, SocialRelationship>();

    private SocialEngagement socialEngagement;
    private SocialMarriage socialMarriage;
    private SocialDivorce socialDivorce;

    public SocialPerson(Person person) {

        this.person = person;

        generateSocialMappings();
    }

    private void generateSocialMappings() {

        for (Friend friend : person.getFriends().values()) {
            friends.put(friend.getPlayerName(), new SocialFriend(friend));
        }

        for (Child child : person.getChildren().values()) {
            children.put(child.getPlayerName(), new SocialChild(child));
        }

        for (Affair affair : person.getAffairs().values()) {
            affairs.put(affair.getPlayerName(), new SocialAffair(affair));
        }

        for (Relationship relationship : person.getRelationships().values()) {
            relationships.put(relationship.getPlayerName(), new SocialRelationship(relationship));
        }

        if (person.getEngagement() != null) {
            socialEngagement = new SocialEngagement(person.getEngagement());
        }

        if (person.getMarriage() != null) {
            socialMarriage = new SocialMarriage(person.getMarriage());
        }

        if (person.getDivorce() != null) {
            socialDivorce = new SocialDivorce(person.getDivorce());
        }

    }

    /**
     * Returns a read-only copy of the friends list.
     * @return
     */
    public List<SocialFriend> getFriends() {
        Lock lock = rwFriendLock.readLock();
        lock.lock();
        try {
            return new ArrayList<SocialFriend>(friends.values());
        } finally {
            lock.unlock();
        }
    }

    public void addFriend(String memberName) {
        Lock lock = rwFriendLock.writeLock();
        lock.lock();
        try {
            firePlayerMemberChangeEvent(memberName, SocialNetworkCommandType.friend, Type.preAdd, false);

            Friend friend = new Friend(memberName);
            person.getFriends().put(memberName, friend);
            friends.put(memberName, new SocialFriend(friend));

            firePlayerMemberChangeEvent(memberName, SocialNetworkCommandType.friend, Type.postAdd, false);

        } finally {
            lock.unlock();
        }
    }

    public void removeFriend(String memberName) {
        Lock lock = rwFriendLock.writeLock();
        lock.lock();
        try {
            firePlayerMemberChangeEvent(memberName, SocialNetworkCommandType.friend, Type.preRemove, (person
                    .getFriends().size() == 0));

            person.getFriends().remove(memberName);
            friends.remove(memberName);

            firePlayerMemberChangeEvent(memberName, SocialNetworkCommandType.friend, Type.postRemove, (person
                    .getFriends().size() == 0));
        } finally {
            lock.unlock();
        }
    }

    public boolean isFriendWith(String memberName) {
        return friends.containsKey(memberName);
    }

    public int getNumberFriends() {
        return friends.values().size();
    }

    public List<SocialAffair> getAffairs() {
        Lock lock = rwAffairLock.readLock();
        lock.lock();
        try {
            return new ArrayList<SocialAffair>(affairs.values());
        } finally {
            lock.unlock();
        }
    }

    public void addAffair(String memberName) {
        Lock lock = rwAffairLock.writeLock();
        lock.lock();
        try {
            firePlayerMemberChangeEvent(memberName, SocialNetworkCommandType.affair, Type.preAdd, false);

            Affair affair = new Affair(memberName);
            person.getAffairs().put(memberName, affair);
            affairs.put(memberName, new SocialAffair(affair));

            firePlayerMemberChangeEvent(memberName, SocialNetworkCommandType.affair, Type.postAdd, false);

        } finally {
            lock.unlock();
        }
    }

    public void removeAffair(String memberName) {
        Lock lock = rwAffairLock.writeLock();
        lock.lock();
        try {
            firePlayerMemberChangeEvent(memberName, SocialNetworkCommandType.affair, Type.preRemove, (person
                    .getAffairs().size() == 0));

            person.getAffairs().remove(memberName);
            affairs.remove(memberName);

            firePlayerMemberChangeEvent(memberName, SocialNetworkCommandType.affair, Type.postRemove, (person
                    .getAffairs().size() == 0));
        } finally {
            lock.unlock();
        }
    }

    public boolean isAffairWith(String memberName) {
        return affairs.containsKey(memberName);
    }

    public int getNumberAffairs() {
        return affairs.values().size();
    }

    public List<SocialChild> getChildren() {
        Lock lock = rwChildLock.readLock();
        lock.lock();
        try {
            return new ArrayList<SocialChild>(children.values());
        } finally {
            lock.unlock();
        }
    }

    public void addChild(String memberName) {
        Lock lock = rwChildLock.writeLock();
        lock.lock();
        try {
            firePlayerMemberChangeEvent(memberName, SocialNetworkCommandType.child, Type.preAdd, false);

            Child child = new Child(memberName);
            person.getChildren().put(memberName, child);
            children.put(memberName, new SocialChild(child));

            firePlayerMemberChangeEvent(memberName, SocialNetworkCommandType.child, Type.postAdd, false);
        } finally {
            lock.unlock();
        }
    }

    public void removeChild(String memberName) {
        Lock lock = rwChildLock.writeLock();
        lock.lock();
        try {
            firePlayerMemberChangeEvent(memberName, SocialNetworkCommandType.child, Type.preRemove, (person
                    .getChildren().size() == 0));

            person.getChildren().remove(memberName);
            children.remove(memberName);

            firePlayerMemberChangeEvent(memberName, SocialNetworkCommandType.child, Type.postRemove, (person
                    .getChildren().size() == 0));
        } finally {
            lock.unlock();
        }
    }

    public boolean isParentOf(String memberName) {
        return children.containsKey(memberName);
    }

    public int getNumberChildren() {
        return children.values().size();
    }

    public List<SocialRelationship> getRelationships() {
        Lock lock = rwRelationshipLock.readLock();
        lock.lock();
        try {
            return new ArrayList<SocialRelationship>(relationships.values());
        } finally {
            lock.unlock();
        }
    }

    public void addRelationship(String memberName) {
        Lock lock = rwRelationshipLock.writeLock();
        lock.lock();
        try {
            firePlayerMemberChangeEvent(memberName, SocialNetworkCommandType.relationship, Type.preAdd, false);

            Relationship relationship = new Relationship(memberName);
            person.getRelationships().put(memberName, relationship);
            relationships.put(memberName, new SocialRelationship(relationship));

            firePlayerMemberChangeEvent(memberName, SocialNetworkCommandType.relationship, Type.postAdd, false);
        } finally {
            lock.unlock();
        }
    }

    public void removeRelationship(String memberName) {
        Lock lock = rwRelationshipLock.writeLock();
        lock.lock();
        try {
            firePlayerMemberChangeEvent(memberName, SocialNetworkCommandType.relationship, Type.preRemove, (person
                    .getRelationships().size() == 0));

            person.getRelationships().remove(memberName);
            relationships.remove(memberName);

            firePlayerMemberChangeEvent(memberName, SocialNetworkCommandType.relationship, Type.postRemove, (person
                    .getRelationships().size() == 0));
        } finally {
            lock.unlock();
        }
    }

    public boolean isRelationshipWith(String memberName) {
        return relationships.containsKey(memberName);
    }

    public int getNumberRelationships() {
        return relationships.values().size();
    }

    public SocialEngagement getEngagement() {
        Lock lock = rwEngagementLock.readLock();
        lock.lock();
        try {
            return socialEngagement;
        } finally {
            lock.unlock();
        }
    }

    public String getChildOf() {
        Lock lock = rwChildOfLock.readLock();
        lock.lock();
        try {
            return person.getChildOf();
        } finally {
            lock.unlock();
        }
    }

    public void createChildOf(String childOf) {
        Lock lock = rwChildOfLock.writeLock();
        lock.lock();
        try {
            if (childOf != null) {
                firePlayerMemberChangeEvent(childOf, SocialNetworkCommandType.child, Type.preAdd, false);
                person.setChildOf(childOf);
                firePlayerMemberChangeEvent(childOf, SocialNetworkCommandType.child, Type.postAdd, false);
            }
        } finally {
            lock.unlock();
        }
    }

    public void breakChildOf() {
        Lock lock = rwChildOfLock.writeLock();
        lock.lock();
        try {

            String childOf = StringUtils.EMPTY;
            if (person.getChildOf() != null) {
                childOf = person.getChildOf();
                firePlayerMemberChangeEvent(childOf, SocialNetworkCommandType.child, Type.preRemove, true);
            }

            person.setChildOf(null);

            if (StringUtils.isNotEmpty(childOf)) {
                firePlayerMemberChangeEvent(childOf, SocialNetworkCommandType.child, Type.postRemove, true);
            }

        } finally {
            lock.unlock();
        }
    }

    public void createEngagement(String memberName) {
        Lock lock = rwEngagementLock.writeLock();
        lock.lock();
        try {
            firePlayerMemberChangeEvent(memberName, SocialNetworkCommandType.engagement, Type.preAdd, false);

            Engagement engagement = new Engagement(memberName);
            socialEngagement = new SocialEngagement(engagement);
            person.setEngagement(engagement);

            firePlayerMemberChangeEvent(memberName, SocialNetworkCommandType.engagement, Type.postAdd, false);

        } finally {
            lock.unlock();
        }
    }

    public void breakEngagement() {
        Lock lock = rwEngagementLock.writeLock();
        lock.lock();
        try {

            String engagementName = StringUtils.EMPTY;
            if (person.getEngagement() != null) {
                engagementName = person.getEngagement().getPlayerName();
                firePlayerMemberChangeEvent(engagementName, SocialNetworkCommandType.engagement, Type.preRemove, true);
            }

            person.setEngagement(null);
            socialEngagement = null;

            if (StringUtils.isNotEmpty(engagementName)) {
                firePlayerMemberChangeEvent(engagementName, SocialNetworkCommandType.engagement, Type.postRemove, true);
            }
        } finally {
            lock.unlock();
        }
    }

    public SocialMarriage getMarriage() {
        Lock lock = rwMarriageLock.readLock();
        lock.lock();
        try {
            return socialMarriage;
        } finally {
            lock.unlock();
        }
    }

    public void createMarriage(String memberName) {
        Lock lock = rwMarriageLock.writeLock();
        lock.lock();
        try {
            firePlayerMemberChangeEvent(memberName, SocialNetworkCommandType.marriage, Type.preAdd, false);

            Marriage marriage = new Marriage(memberName);
            socialMarriage = new SocialMarriage(marriage);
            person.setMarriage(marriage);

            firePlayerMemberChangeEvent(memberName, SocialNetworkCommandType.marriage, Type.postAdd, false);

        } finally {
            lock.unlock();
        }
    }

    public void breakMarriage() {
        Lock lock = rwMarriageLock.writeLock();
        lock.lock();
        try {

            String spouseName = StringUtils.EMPTY;
            if (person.getMarriage() != null) {
                spouseName = person.getMarriage().getPlayerName();
                firePlayerMemberChangeEvent(spouseName, SocialNetworkCommandType.marriage, Type.preRemove, true);
            }

            person.setMarriage(null);
            socialMarriage = null;

            if (StringUtils.isNotEmpty(spouseName)) {
                firePlayerMemberChangeEvent(spouseName, SocialNetworkCommandType.marriage, Type.postRemove, true);
            }

        } finally {
            lock.unlock();
        }
    }

    public SocialDivorce getDivorce() {
        Lock lock = rwDivorceLock.readLock();
        lock.lock();
        try {
            return socialDivorce;
        } finally {
            lock.unlock();
        }
    }

    public void createDivorce(String memberName) {
        Lock lock = rwDivorceLock.writeLock();
        lock.lock();
        try {
            firePlayerMemberChangeEvent(memberName, SocialNetworkCommandType.divorce, Type.preAdd, false);

            Divorce divorce = new Divorce(memberName);
            socialDivorce = new SocialDivorce(divorce);
            person.setDivorce(divorce);

            firePlayerMemberChangeEvent(memberName, SocialNetworkCommandType.divorce, Type.postAdd, false);

        } finally {
            lock.unlock();
        }
    }

    public void endDivorce() {
        Lock lock = rwDivorceLock.writeLock();
        lock.lock();
        try {
            String spouseName = StringUtils.EMPTY;
            if (person.getDivorce() != null) {
                spouseName = person.getDivorce().getPlayerName();
                firePlayerMemberChangeEvent(spouseName, SocialNetworkCommandType.divorce, Type.preRemove, true);
            }

            person.setDivorce(null);
            socialDivorce = null;

            if (StringUtils.isNotEmpty(spouseName)) {
                firePlayerMemberChangeEvent(spouseName, SocialNetworkCommandType.divorce, Type.postRemove, true);
            }

        } finally {
            lock.unlock();
        }
    }

    public Status getSocialStatus() {
        Lock lock = rwStatusLock.readLock();
        lock.lock();
        try {
            return person.getSocialStatus();
        } finally {
            lock.unlock();
        }
    }

    public void setSocialStatus(Status socialStatus) {
        Lock lock = rwRelationshipLock.writeLock();
        lock.lock();
        try {
            person.setSocialStatus(socialStatus);
        } finally {
            lock.unlock();
        }
    }

    public String getName() {
        Lock lock = rwNameLock.readLock();
        lock.lock();
        try {
            return person.getName();
        } finally {
            lock.unlock();
        }
    }

    public Gender getGender() {
        Lock lock = rwGenderLock.readLock();
        lock.lock();
        try {
            return person.getGender();
        } finally {
            lock.unlock();
        }
    }

    public void setGender(Gender gender) {
        Lock lock = rwGenderLock.writeLock();
        lock.lock();
        try {
            person.setGender(gender);
        } finally {
            lock.unlock();
        }
    }

    public void waitOn(WaitState waitState, ICommandType waitCommand) {
        waitOn(waitState, waitCommand, null);
    }

    public <U extends IMessage> void waitOn(WaitState waitState, ICommandType waitCommand, U waitData) {
        Lock lock = rwWaitLock.writeLock();
        lock.lock();
        try {
            person.setWaitState(waitState);
            person.setWaitCommand(waitCommand);
            person.setWaitData(waitData);
        } finally {
            lock.unlock();
        }
    }

    public ICommandType getWaitCommand() {
        Lock lock = rwWaitLock.readLock();
        lock.lock();
        try {
            return person.getWaitCommand();
        } finally {
            lock.unlock();
        }
    }

    public WaitState getWaitState() {
        Lock lock = rwRelationshipLock.readLock();
        lock.lock();
        try {
            return person.getWaitState();
        } finally {
            lock.unlock();
        }
    }

    public <U extends IMessage> U getWaitData() {
        Lock lock = rwRelationshipLock.readLock();
        lock.lock();
        try {
            return person.getWaitData();
        } finally {
            lock.unlock();
        }
    }

    public WeddingVows getWeddingVows() {
        Lock lock = rwWeddingLock.readLock();
        lock.lock();
        try {
            return person.getWeddingVows();
        } finally {
            lock.unlock();
        }
    }

    public void setWeddingVows(WeddingVows weddingVows) {
        Lock lock = rwWeddingLock.writeLock();
        lock.lock();
        try {
            person.setWeddingVows(weddingVows);
        } finally {
            lock.unlock();
        }
    }

    public boolean isLawyer() {
        Lock lock = rwLawyerLock.readLock();
        lock.lock();
        try {
            return person.isLawyer();
        } finally {
            lock.unlock();
        }
    }

    public void setLawyer(boolean lawyer) {
        Lock lock = rwLawyerLock.writeLock();
        lock.lock();
        try {
            person.setLawyer(lawyer);
        } finally {
            lock.unlock();
        }
    }

    public boolean isPriest() {
        Lock lock = rwPriestLock.readLock();
        lock.lock();
        try {
            return person.isPriest();
        } finally {
            lock.unlock();
        }
    }

    public void setPriest(boolean priest) {
        Lock lock = rwPriestLock.writeLock();
        lock.lock();
        try {
            person.setPriest(priest);
        } finally {
            lock.unlock();
        }
    }

    public void addIgnore(String playerName) {
        Lock lock = rwIgnoreLock.writeLock();
        lock.lock();
        try {
            person.getIgnoreList().add(playerName);
            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                logger.info("[" + person.getName() + "] is now ignoring " + playerName);
            }
        } finally {
            lock.unlock();
        }
    }

    public void removeIgnore(String playerName) {
        Lock lock = rwIgnoreLock.writeLock();
        lock.lock();
        try {
            person.getIgnoreList().remove(playerName);
            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                logger.info("[" + person.getName() + "] is no longer ignoring " + playerName);
            }
        } finally {
            lock.unlock();
        }
    }

    public List<String> getIgnoredPlayers() {
        Lock lock = rwIgnoreLock.writeLock();
        lock.lock();
        try {
            return new ArrayList<String>(person.getIgnoreList());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Checks to see if the given player name is on the ignore list of the person.
     * @param playerName The name to check to see if they're on ignore by this person.
     * @return True if ignored, false otherwise.
     */
    public boolean isOnIgnore(String playerName) {
        Lock lock = rwIgnoreLock.writeLock();
        lock.lock();
        try {
            boolean isOnIgnore = person.getIgnoreList().contains(playerName);
            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                logger.info("[" + person.getName() + "] is ignoring " + playerName + ":" + isOnIgnore);
            }
            return isOnIgnore;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns a read-only copy of the messages for the person. Adding/removing to this list will not affect the person.
     * @param fromPlayerName
     * @param messageClass
     * @return
     */
    @SuppressWarnings("unchecked")
    public <U extends IMessage> List<U> getMessagesFrom(String fromPlayerName, Class<U> messageClass) {

        Lock lock = rwMessageQueueLock.readLock();
        lock.lock();
        try {

            String className = messageClass.getCanonicalName();
            if (person.getMessageQueue().containsKey(className)) {
                if (person.getMessageQueue().get(className).containsKey(fromPlayerName)) {

                    // send back a copy of the items instead of the original list
                    List<U> messages = (List<U>) person.getMessageQueue().get(className).get(fromPlayerName);
                    return new ArrayList<U>(messages);
                }
            }

            // return an empty list
            return Collections.emptyList();

        } finally {
            lock.unlock();
        }
    }

    public <U extends IMessage> List<String> getMessagePlayers(Class<U> messageClass) {

        Lock lock = rwMessageQueueLock.readLock();
        lock.lock();
        try {
            String className = messageClass.getCanonicalName();
            if (person.getMessageQueue().get(className) != null) {
                return new ArrayList<String>(person.getMessageQueue().get(className).keySet());
            }

            // return an empty list
            return Collections.emptyList();

        } finally {
            lock.unlock();
        }
    }

    public <U extends IMessage> int getMessagesCount(Class<U> messageClass) {

        Lock lock = rwMessageQueueLock.readLock();
        lock.lock();
        try {

            int count = 0;
            String className = messageClass.getCanonicalName();
            if (person.getMessageQueue().containsKey(className)) {
                Set<String> senders = person.getMessageQueue().get(className).keySet();
                for (String sender : senders) {
                    count += person.getMessageQueue().get(className).get(sender).size();
                }
            }

            return count;

        } finally {
            lock.unlock();
        }
    }

    public <U extends IMessage> int getMessageCountFrom(String fromPlayerName, Class<U> messageClass) {

        Lock lock = rwMessageQueueLock.readLock();
        lock.lock();
        try {

            String className = messageClass.getCanonicalName();
            if (person.getMessageQueue().containsKey(className)) {
                if (person.getMessageQueue().get(className).containsKey(fromPlayerName)) {
                    return person.getMessageQueue().get(className).get(fromPlayerName).size();
                }
            }

            return 0;

        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public <U extends IMessage> void addMessage(String fromPlayerName, U message) {

        Lock lock = rwMessageQueueLock.writeLock();
        lock.lock();
        try {

            // check to see if this message type has a queue entry, if not, make one
            String className = message.getClass().getCanonicalName();

            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                logger.info("Adding message queue entry: [" + fromPlayerName + ", " + message + "]");
            }

            // If there isn't an entry for this message type, then create one
            Map<String, List<? extends IMessage>> playerMessageMap = person.getMessageQueue().get(className);
            if (playerMessageMap == null) {

                if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                    logger.info("Creating new message queue entry: [" + fromPlayerName + ", " + message + "]");
                }

                List<? extends IMessage> messageList = new ArrayList<U>();
                playerMessageMap = new HashMap<String, List<? extends IMessage>>();
                playerMessageMap.put(fromPlayerName, messageList);
                person.getMessageQueue().put(className, playerMessageMap);
            }

            // add the message
            List<U> playerList = (List<U>) playerMessageMap.get(fromPlayerName);
            playerList.add(message);

        } finally {
            lock.unlock();
        }
    }

    public <U extends IMessage> void removeMessage(String fromPlayerName, U message) {

        Lock lock = rwMessageQueueLock.writeLock();
        lock.lock();
        try {
            String className = message.getClass().getCanonicalName();

            // check to make sure there is a message of this type in the queue
            if (person.getMessageQueue().containsKey(className)) {

                // check to see if that list of types contains an entry for the player
                if (person.getMessageQueue().get(className).containsKey(fromPlayerName)) {

                    // remove the message from the list
                    person.getMessageQueue().get(className).get(fromPlayerName).remove(message);

                    // if the list is empty now, remove it from the map, just to save object memory
                    if (person.getMessageQueue().get(className).get(fromPlayerName).size() == 0) {
                        person.getMessageQueue().get(className).remove(fromPlayerName);
                    }

                    // if the message type list is empty, remove it also, just to save object memory
                    if (person.getMessageQueue().get(className).keySet().size() == 0) {
                        person.getMessageQueue().remove(className);
                    }
                }
            }

        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes all the messages of every type of the given person.
     * @param fromPlayerName
     * @param messageClass
     * @return
     */
    public void removeMessagesFrom(String fromPlayerName) {

        Lock lock = rwMessageQueueLock.readLock();
        lock.lock();
        try {

            // go through every class in the message queue and remove the player list from it
            for (String className : person.getMessageQueue().keySet()) {

                // remove the person
                person.getMessageQueue().get(className).remove(fromPlayerName);

                // if the message type list is empty, remove it also, just to save object memory
                if (person.getMessageQueue().get(className).keySet().size() == 0) {
                    person.getMessageQueue().remove(className);
                }
            }

        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns all the messages of the given type for the given player name.
     * This version does not create a copy allowing us to work with it internally.
     * @param playerFrom
     * @param classObject
     * @return
     */
    @SuppressWarnings("unchecked")
    private <U extends IMessage> List<U> getMessagesFrom(SocialPerson playerFrom, Class<U> classObject) {

        Lock lock = rwMessageQueueLock.readLock();
        lock.lock();
        try {

            // get the list of requests for the player
            String className = classObject.getCanonicalName();
            if (person.getMessageQueue().containsKey(className)) {
                if (person.getMessageQueue().get(className).containsKey(playerFrom.getName())) {
                    return (List<U>) person.getMessageQueue().get(className).get(playerFrom.getName());
                }
            }

            // return an empty list
            return Collections.emptyList();

        } finally {
            lock.unlock();
        }
    }

    public boolean addRequest(SocialPerson playerFrom, ICommandType requestType) {

        // get the request messages from the player
        List<Request> requests = getMessagesFrom(playerFrom, Request.class);

        // now we have a list of requests
        boolean hasRequestFromPlayer = false;

        // check to see if we already have a request from that player
        for (Request request : requests) {
            if (request.getCommandType() == requestType) {
                hasRequestFromPlayer = true;
                break;
            }
        }

        // if not already there, add them now
        if (!hasRequestFromPlayer) {

            // create the request message
            Request request = new Request(playerFrom.getName(), requestType);

            // and throw it onto the message queue
            addMessage(playerFrom.getName(), request);

            return true;
        }

        return false;
    }

    public boolean removeRequest(SocialPerson playerFrom, ICommandType requestType) {

        // get the request messages from the player
        List<Request> requests = getMessagesFrom(playerFrom, Request.class);

        // check to see if the person has sent them a request of this type
        for (Request request : requests) {

            if (request.getCommandType() == requestType) {

                // okay, now we want to remove this from the list
                removeMessage(playerFrom.getName(), request);

                return true;
            }
        }

        return false;
    }

    public boolean hasRequest(SocialPerson playerFrom, ICommandType requestType) {

        // get the request messages from the player
        List<Request> requests = getMessagesFrom(playerFrom, Request.class);

        // check to see if the person has sent them a request of this type
        for (Request request : requests) {

            if (request.getCommandType() == requestType) {
                return true;
            }
        }

        return false;
    }

    public boolean addAlert(SocialPerson playerFrom, Alert.Type alertType, String alertMessage) {

        // get the alert messages from the player
        List<Alert> alerts = getMessagesFrom(playerFrom, Alert.class);

        // now we have a list of alerts
        boolean hasAlertFromPlayer = false;

        // check to see if we already have a alert from that player
        for (Alert alert : alerts) {
            if (alert.getAlertType() == alertType) {
                hasAlertFromPlayer = true;
                break;
            }
        }

        // if not already there, add them now
        if (!hasAlertFromPlayer) {

            // create the alert message
            Alert alert = new Alert(playerFrom.getName(), alertType, alertMessage);

            // and throw it onto the message queue
            addMessage(playerFrom.getName(), alert);

            return true;
        }

        return false;
    }

    public boolean removeAlert(SocialPerson playerFrom, Alert.Type alertType) {

        // get the alert messages from the player
        List<Alert> alerts = getMessagesFrom(playerFrom, Alert.class);

        // check to see if the person has sent them a alert of this type
        for (Alert alert : alerts) {

            if (alert.getAlertType() == alertType) {

                // okay, now we want to remove this from the list
                removeMessage(playerFrom.getName(), alert);

                return true;
            }
        }

        return false;
    }

    public boolean hasAlert(SocialPerson playerFrom, Alert.Type alertType) {

        // get the alert messages from the player
        List<Alert> alerts = getMessagesFrom(playerFrom, Alert.class);

        // check to see if the person has sent them a alert of this type
        for (Alert alert : alerts) {

            if (alert.getAlertType() == alertType) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks to see if inGroupPerson is in playerPerson's social groups.
     * @param playerPerson The person to search within.
     * @param memberPerson The person to search for.
     * @return True if found, false if not.
     */
    public <U extends IPerkSettings> boolean isPerkMember(U perkSettings, SocialPerson memberPerson) {

        if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
            logger.info("Calling isPerkMember for: " + perkSettings.getName() + ", " + memberPerson.getName());
        }

        // get the settings
        SettingsConfig settingsConfig = PluginConfig.getInstance().getConfig(SettingsConfig.class);

        // go through each perk assigned to this group and see if any of them match the one from the perk settings given
        if (isFriendWith(memberPerson.getName())) {
            FriendSettings settings = settingsConfig.getSocialNetworkSettings(FriendSettings.class);
            for (String perkName : settings.getPerks()) {
                if (perkSettings.getName().equals(perkName)) {
                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("Friend has perk: " + perkName);
                    }
                    return true;
                }
            }
        }

        // go through each perk assigned to this group and see if any of them match the one from the perk settings given
        if (isAffairWith(memberPerson.getName())) {
            AffairSettings settings = settingsConfig.getSocialNetworkSettings(AffairSettings.class);
            for (String perkName : settings.getPerks()) {
                if (perkSettings.getName().equals(perkName)) {
                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("Affair has perk: " + perkName);
                    }
                    return true;
                }
            }
        }

        // go through each perk assigned to this group and see if any of them match the one from the perk settings given
        if (isRelationshipWith(memberPerson.getName())) {
            RelationshipSettings settings = settingsConfig.getSocialNetworkSettings(RelationshipSettings.class);
            for (String perkName : settings.getPerks()) {
                if (perkSettings.getName().equals(perkName)) {
                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("Relationship has perk: " + perkName);
                    }
                    return true;
                }
            }
        }

        // go through each perk assigned to this group and see if any of them match the one from the perk settings given
        if (getChildOf() != null && getChildOf().equals(memberPerson.getName())) {
            ChildSettings settings = settingsConfig.getSocialNetworkSettings(ChildSettings.class);
            for (String perkName : settings.getPerks()) {
                if (perkSettings.getName().equals(perkName)) {
                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("Child has perk: " + perkName);
                    }
                    return true;
                }
            }
        }

        // go through each perk assigned to this group and see if any of them match the one from the perk settings given
        if (getEngagement() != null && getEngagement().getPlayerName().equals(memberPerson.getName())) {
            EngagementSettings settings = settingsConfig.getSocialNetworkSettings(EngagementSettings.class);
            for (String perkName : settings.getPerks()) {
                if (perkSettings.getName().equals(perkName)) {
                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("Engagement has perk: " + perkName);
                    }
                    return true;
                }
            }
        }

        // go through each perk assigned to this group and see if any of them match the one from the perk settings given
        if (getMarriage() != null && getMarriage().getPlayerName().equals(memberPerson.getName())) {
            MarriageSettings settings = settingsConfig.getSocialNetworkSettings(MarriageSettings.class);
            for (String perkName : settings.getPerks()) {
                if (perkSettings.getName().equals(perkName)) {
                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("Marriage has perk: " + perkName);
                    }
                    return true;
                }
            }
        }

        // go through each perk assigned to this group and see if any of them match the one from the perk settings given
        if (getDivorce() != null && getDivorce().getPlayerName().equals(memberPerson.getName())) {
            DivorceSettings settings = settingsConfig.getSocialNetworkSettings(DivorceSettings.class);
            for (String perkName : settings.getPerks()) {
                if (perkSettings.getName().equals(perkName)) {
                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("Divorce has perk: " + perkName);
                    }
                    return true;
                }
            }
        }

        if (isLawyer()) {
            LawyerSettings settings = settingsConfig.getSocialNetworkSettings(LawyerSettings.class);
            for (String perkName : settings.getPerks()) {
                if (perkSettings.getName().equals(perkName)) {
                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("Lawyer has perk: " + perkName);
                    }
                    return true;
                }
            }
        }

        if (isPriest()) {
            PriestSettings settings = settingsConfig.getSocialNetworkSettings(PriestSettings.class);
            for (String perkName : settings.getPerks()) {
                if (perkSettings.getName().equals(perkName)) {
                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("Priest has perk: " + perkName);
                    }
                    return true;
                }
            }
        }

        if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
            logger.info("NOMATCH isPerkMember for: " + perkSettings.getName() + ", " + memberPerson.getName());
        }

        return false;
    }

    /**
     * Gets a specific group settings instance. If the user does not belong to that group, it will return NULL.
     * @param classObject The class of the settings to request.
     * @return NULL if user does not belong to that group.
     */
    @SuppressWarnings("unchecked")
    public <T extends GroupSettings> T getGroupSettings(Class<T> classObject) {

        return (T) buildGroupSettingsMap().get(classObject);
    }

    /**
     * @return A list of group settings for the groups the user currently belongs to.
     */
    public List<GroupSettings> getGroupSettings() {

        Map<Class<? extends GroupSettings>, ? extends GroupSettings> settingsMap = buildGroupSettingsMap();

        List<GroupSettings> list = new ArrayList<GroupSettings>();
        list.addAll(settingsMap.values());
        return list;
    }

    private Map<Class<? extends GroupSettings>, ? extends GroupSettings> buildGroupSettingsMap() {

        Map<Class<? extends GroupSettings>, GroupSettings> settingsMap =
                new HashMap<Class<? extends GroupSettings>, GroupSettings>();

        // get the settings
        SettingsConfig settingsConfig = PluginConfig.getInstance().getConfig(SettingsConfig.class);

        if (person.getFriends().size() > 0) {
            FriendSettings settings = settingsConfig.getSocialNetworkSettings(FriendSettings.class);
            settingsMap.put(FriendSettings.class, settings);
        }

        if (person.getAffairs().size() > 0) {
            AffairSettings settings = settingsConfig.getSocialNetworkSettings(AffairSettings.class);
            settingsMap.put(AffairSettings.class, settings);
        }

        if (person.getRelationships().size() > 0) {
            RelationshipSettings settings = settingsConfig.getSocialNetworkSettings(RelationshipSettings.class);
            settingsMap.put(RelationshipSettings.class, settings);
        }

        if (person.getChildOf() != null) {
            ChildSettings settings = settingsConfig.getSocialNetworkSettings(ChildSettings.class);
            settingsMap.put(ChildSettings.class, settings);
        }

        if (person.getEngagement() != null) {
            EngagementSettings settings = settingsConfig.getSocialNetworkSettings(EngagementSettings.class);
            settingsMap.put(EngagementSettings.class, settings);
        }

        if (person.getDivorce() != null) {
            DivorceSettings settings = settingsConfig.getSocialNetworkSettings(DivorceSettings.class);
            settingsMap.put(DivorceSettings.class, settings);
        }

        if (person.getMarriage() != null) {
            MarriageSettings settings = settingsConfig.getSocialNetworkSettings(MarriageSettings.class);
            settingsMap.put(MarriageSettings.class, settings);
        }

        if (person.isLawyer()) {
            LawyerSettings settings = settingsConfig.getSocialNetworkSettings(LawyerSettings.class);
            settingsMap.put(LawyerSettings.class, settings);
        }

        if (person.isPriest()) {
            PriestSettings settings = settingsConfig.getSocialNetworkSettings(PriestSettings.class);
            settingsMap.put(PriestSettings.class, settings);
        }

        return settingsMap;
    }

    private void firePlayerMemberChangeEvent(String memberName, ICommandType groupType, Type eventType,
            boolean groupEmpty) {

        // create the event
        PlayerMemberChangeEvent event =
                new PlayerMemberChangeEvent(this.getName(), memberName, groupType.toString(), eventType, groupEmpty);

        // and fire it off
        Bukkit.getServer().getPluginManager().callEvent(event);
    }
}
