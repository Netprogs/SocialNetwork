package com.netprogs.minecraft.plugins.social.storage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.command.util.PlayerUtil;
import com.netprogs.minecraft.plugins.social.command.util.TimerManager;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.storage.data.Alert;
import com.netprogs.minecraft.plugins.social.storage.data.Person;
import com.netprogs.minecraft.plugins.social.storage.data.PersonSettings;
import com.netprogs.minecraft.plugins.social.storage.data.perk.IPersonPerkSettings;
import com.netprogs.minecraft.plugins.social.storage.driver.json.JsonPersonDataManager;
import com.netprogs.minecraft.plugins.social.storage.driver.json.JsonSocialNetworkDataManager;

import org.bukkit.entity.Player;

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

public class SocialNetworkStorage {

    private class PersonMapValue {

        public Person person;
        public SocialPerson socialPerson;

        public PersonMapValue(SocialPerson socialPerson, Person person) {
            this.person = person;
            this.socialPerson = socialPerson;
        }

        public Person getPerson() {
            return person;
        }

        public SocialPerson getSocialPerson() {
            return socialPerson;
        }
    }

    // This holds the list of all available players that are currently registered.
    private ISocialNetworkDataManager socialDataManager;

    // This holds the individual data for each player (Person). These are not loaded at startup.
    // Instead, we use the references from the socialDataManager to determine if the user is in the network, then we
    // lazy-load their data once they've been verified.
    private IPersonDataManager personDataManager;

    // List of the players that have used the network recently (since last restart)
    // The plug-in starts with this list as empty, but lazy-loads each player as they run commands for the first time.
    // If you reload the server, they won't be affected, the next command they run will load them back into this list.
    // This helps control memory a bit better since we'll only be storing those who are active.
    private Map<String, PersonMapValue> loadedPersonMap;

    public SocialNetworkStorage() {

        // create our data manager instances
        // later on we should be able to "hot-swap" these with other forms of data managers (mysql, sqlite etc)
        socialDataManager = new JsonSocialNetworkDataManager();
        personDataManager = new JsonPersonDataManager();
    }

    private Map<String, PersonMapValue> getSocialNetworkMap() {

        if (loadedPersonMap == null) {
            loadedPersonMap = new HashMap<String, PersonMapValue>();
            for (String personName : socialDataManager.getPlayers()) {

                // We start off placing NULL into the map for the person object. We'll lazy load this later.
                loadedPersonMap.put(personName.toLowerCase(), null);
            }
        }

        return loadedPersonMap;
    }

    // Used by PlayerJoinListener to create accounts upon login
    public synchronized SocialPerson addPerson(Player player) {

        // We need to check the excludedPlayers list and if they are here, return null
        if (SocialNetworkPlugin.getStorage().isExcludedPlayer(player.getName())) {
            return null;
        }

        // create the person data
        Person person = new Person();
        person.setName(player.getName());
        person.setDateJoined(System.currentTimeMillis());

        SocialPerson socialPerson = new SocialPerson(person);

        // create their data entry
        personDataManager.savePerson(person);

        // add their name to the player list
        socialDataManager.addPlayer(person.getName());

        // now wrap them in our PersonMapValue object
        PersonMapValue value = new PersonMapValue(socialPerson, person);

        // add to the map using lower case name as key
        getSocialNetworkMap().put(person.getName().toLowerCase(), value);

        // return the SocialPerson instance
        return socialPerson;
    }

    public void removePerson(SocialPerson socialPerson) {

        synchronized (socialPerson) {

            // get the person data
            Person person = getSocialNetworkMap().get(socialPerson.getName().toLowerCase()).getPerson();

            // remove all their relations to everyone and send alerts to each of them
            removeFromAllGroups(socialPerson);

            // delete their data file
            personDataManager.deletePerson(person);

            // remove from the data source
            socialDataManager.removePlayer(person.getName());

            // remove from the map
            getSocialNetworkMap().remove(person.getName().toLowerCase());
        }
    }

    public void savePerson(SocialPerson socialPerson) {

        synchronized (socialPerson) {

            // get the person data
            Person person = getSocialNetworkMap().get(socialPerson.getName().toLowerCase()).getPerson();

            // Saves to their data file
            personDataManager.savePerson(person);
        }
    }

    public synchronized SocialPerson getPerson(String personName) {

        // Take the name given and look up their real name from Bukkit
        // This should allow people to use any nicknames also for sending requests.
        // Visually however, we're still going to use their real names.
        String playerName = PlayerUtil.getPlayerName(personName);
        if (playerName != null) {

            // Do the lazy load here. Check to see if they've been loaded from file yet, if not, so it now.
            if (getSocialNetworkMap().get(playerName.toLowerCase()) == null) {

                // load their data from the data manager
                Person person = personDataManager.loadPerson(playerName);
                if (person != null) {

                    // add their name to the data source if they're not there yet
                    // (case of new player being loaded for the first time)
                    if (!socialDataManager.hasPlayer(person.getName())) {
                        socialDataManager.addPlayer(person.getName());
                    }

                    // TODO: We should be able to remove this eventually and force people to update
                    // We now want to check to see if they have a valid join date
                    // This is only used for older versions that did not have this value previously.
                    if (person.getDateJoined() == 0) {
                        person.setDateJoined(System.currentTimeMillis());
                        personDataManager.savePerson(person);
                    }

                    // now wrap them in our SocialPerson object
                    SocialPerson socialPerson = new SocialPerson(person);
                    PersonMapValue value = new PersonMapValue(socialPerson, person);

                    // add to the map using lower case name as key
                    getSocialNetworkMap().put(person.getName().toLowerCase(), value);

                    // return the new instance
                    // SocialNetworkPlugin.log("getPerson returning: " + socialPerson.getName());
                    return socialPerson;
                }

            } else {

                // we found them in the network list, so return their object
                SocialPerson socialPerson = getSocialNetworkMap().get(playerName.toLowerCase()).getSocialPerson();
                // SocialNetworkPlugin.log("getPerson returning: " + socialPerson.getName());
                return socialPerson;
            }
        }

        // nothing found
        SocialNetworkPlugin.log("getPerson cannot find: " + personName);
        return null;
    }

    public boolean isExcludedPlayer(String playerName) {
        return socialDataManager.isExcludedPlayer(playerName);
    }

    public void addExcludedPlayer(SocialPerson socialPerson) {
        socialDataManager.addExcludedPlayer(socialPerson.getName());
    }

    public void removeExcludedPlayer(String playerName) {
        socialDataManager.removeExcludedPlayer(playerName);
    }

    public List<String> getPriests() {
        return new ArrayList<String>(socialDataManager.getPriests());
    }

    public boolean hasPriest(SocialPerson socialPerson) {
        return socialDataManager.hasPriest(socialPerson.getName());
    }

    public void addPriest(SocialPerson socialPerson) {
        socialDataManager.addPriest(socialPerson.getName());
    }

    public void removePriest(SocialPerson socialPerson) {
        socialDataManager.removePriest(socialPerson.getName());
    }

    public List<String> getLawyers() {
        return new ArrayList<String>(socialDataManager.getLawyers());
    }

    public boolean hasLawyer(SocialPerson socialPerson) {
        return socialDataManager.hasLawyer(socialPerson.getName());
    }

    public void addLawyer(SocialPerson socialPerson) {
        socialDataManager.addLawyer(socialPerson.getName());
    }

    public void removeLawyer(SocialPerson socialPerson) {
        socialDataManager.removeLawyer(socialPerson.getName());
    }

    public <P extends IPersonPerkSettings> P getPersonPerkSettings(SocialPerson socialPerson, String perkName) {

        Person person = getSocialNetworkMap().get(socialPerson.getName().toLowerCase()).getPerson();
        PersonSettings settings = personDataManager.loadPersonSettings(person);
        if (settings != null && settings.hasPerkSettings(perkName)) {
            return settings.getPerkSettings(perkName);
        }
        return null;
    }

    public <P extends IPersonPerkSettings> void setPersonPerkSettings(SocialPerson socialPerson, String perkName,
            P perkSettings) {

        Person person = getSocialNetworkMap().get(socialPerson.getName().toLowerCase()).getPerson();
        PersonSettings settings = personDataManager.loadPersonSettings(person);
        if (settings != null) {
            settings.setPerkSettings(perkName, perkSettings);
        } else {
            settings = new PersonSettings();
            settings.setPerkSettings(perkName, perkSettings);
        }

        // save it
        personDataManager.savePersonSettings(person, settings);
    }

    public int purgePlayers(int purgeDays) {

        SimpleDateFormat dayFormat = new SimpleDateFormat("D");
        dayFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        // convert the days into milliseconds
        long purgeDaysTime = purgeDays * 24L * 60L * 60L * 1000L;

        // get todays time and subtract the purge days
        long purgeTime = System.currentTimeMillis() - purgeDaysTime;

        SocialNetworkPlugin.logger().info("[PURGE] Starting the purge of old accounts...");
        SocialNetworkPlugin.logger().info("[PURGE] Purging accounts older than: " + TimerManager.formatDate(purgeTime));

        // get the list of players and for each one, determine when they last logged in
        int purgeCount = 0;
        for (String playerName : socialDataManager.getPlayers()) {

            // get the last login time and convert into days
            long lastLoginTime = PlayerUtil.getPlayerLastPlayed(playerName);

            SocialNetworkPlugin.logger().info(
                    "[PURGE] Checking " + playerName + ": " + TimerManager.formatDate(lastLoginTime));

            if (lastLoginTime < purgeTime) {

                // obtain their details, then delete their account
                SocialPerson purgePerson = getPerson(playerName);
                removePerson(purgePerson);

                SocialNetworkPlugin.logger().info(
                        "[PURGE] Purged account " + playerName + " with last login: "
                                + TimerManager.formatDate(lastLoginTime));
            }
        }

        SocialNetworkPlugin.logger().info("[PURGE] Purged " + purgeCount + " accounts.");

        return purgeCount;
    }

    public Map<String, SocialPerson> getNotificationPlayers(SocialPerson socialPerson) {
        return getNotificationPlayers(socialPerson, false);
    }

    private Map<String, SocialPerson> getNotificationPlayers(SocialPerson socialPerson, boolean remove) {

        Map<String, SocialPerson> notifyPlayers = new HashMap<String, SocialPerson>();

        // go through each group type and remove this user from their lists
        Person person = getSocialNetworkMap().get(socialPerson.getName().toLowerCase()).getPerson();

        for (String memberName : person.getFriends().keySet()) {
            SocialPerson groupPerson = getPerson(memberName);
            if (groupPerson != null) {
                if (remove) {
                    groupPerson.removeFriend(socialPerson);
                }
                notifyPlayers.put(groupPerson.getName(), groupPerson);
            }
        }

        for (String memberName : person.getAffairs().keySet()) {
            SocialPerson groupPerson = getPerson(memberName);
            if (groupPerson != null) {
                if (remove) {
                    groupPerson.removeAffair(socialPerson);
                }
                notifyPlayers.put(groupPerson.getName(), groupPerson);
            }
        }

        for (String memberName : person.getRelationships().keySet()) {
            SocialPerson groupPerson = getPerson(memberName);
            if (groupPerson != null) {
                if (remove) {
                    groupPerson.removeRelationship(socialPerson);
                }
                notifyPlayers.put(groupPerson.getName(), groupPerson);
            }
        }

        for (String memberName : person.getRelationships().keySet()) {
            SocialPerson groupPerson = getPerson(memberName);
            if (groupPerson != null) {
                if (remove) {
                    groupPerson.removeRelationship(socialPerson);
                }
                notifyPlayers.put(groupPerson.getName(), groupPerson);
            }
        }

        for (String memberName : person.getChildren().keySet()) {
            SocialPerson groupPerson = getPerson(memberName);
            if (groupPerson != null) {
                if (remove) {
                    groupPerson.breakChildOf();
                }
                notifyPlayers.put(groupPerson.getName(), groupPerson);
            }
        }

        if (person.getChildOf() != null) {
            SocialPerson groupPerson = getPerson(person.getChildOf());
            if (groupPerson != null) {
                if (remove) {
                    groupPerson.removeChild(socialPerson);
                }
                notifyPlayers.put(groupPerson.getName(), groupPerson);
            }
        }

        if (person.getEngagement() != null) {
            SocialPerson groupPerson = getPerson(person.getEngagement().getPlayerName());
            if (groupPerson != null) {
                if (remove) {
                    groupPerson.breakEngagement();
                }
                notifyPlayers.put(groupPerson.getName(), groupPerson);
            }
        }

        if (person.getDivorce() != null) {
            SocialPerson groupPerson = getPerson(person.getDivorce().getPlayerName());
            if (groupPerson != null) {
                if (remove) {
                    groupPerson.endDivorce();
                }
                notifyPlayers.put(groupPerson.getName(), groupPerson);
            }
        }

        if (person.getMarriage() != null) {
            SocialPerson groupPerson = getPerson(person.getMarriage().getPlayerName());
            if (groupPerson != null) {
                if (remove) {
                    groupPerson.breakMarriage();
                }
                notifyPlayers.put(groupPerson.getName(), groupPerson);
            }
        }

        return notifyPlayers;
    }

    private void removeFromAllGroups(SocialPerson socialPerson) {

        // get the list of all unique player among all your groups
        Map<String, SocialPerson> notifyPlayers = getNotificationPlayers(socialPerson, true);

        // now, for each person in the map, send them an alert saying this person quit
        ResourcesConfig resources = SocialNetworkPlugin.getResources();
        String alertMessage = resources.getResource("social.alert.playerDeleted");
        alertMessage = alertMessage.replaceAll("<player>", socialPerson.getName());

        for (SocialPerson memberPerson : notifyPlayers.values()) {

            // remove all pending messages
            memberPerson.removeMessagesFrom(socialPerson);

            // add the alert
            memberPerson.addAlert(socialPerson, Alert.Type.deleted, alertMessage);

            // save the alert and the changes from above
            savePerson(memberPerson);
        }
    }
}
