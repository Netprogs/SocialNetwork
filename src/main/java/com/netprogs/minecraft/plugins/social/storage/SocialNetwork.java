package com.netprogs.minecraft.plugins.social.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.config.PluginConfig;
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

public class SocialNetwork {

    private final Logger logger = Logger.getLogger("Minecraft");

    private static final SocialNetwork SINGLETON = new SocialNetwork();

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

    public static SocialNetwork getInstance() {
        return SINGLETON;
    }

    private SocialNetwork() {

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

    // Used by listeners to determine right away if they're handling a player from the network
    public boolean isSocialNetworkPlayer(Player player) {
        return getSocialNetworkMap().containsKey(player.getName().toLowerCase());
    }

    // Used by CommandJoin
    public synchronized SocialPerson addPerson(String playerName) {

        // create the person data
        Person person = new Person();
        person.setName(playerName);

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

    // Used by CommandQuit
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

        // Do the lazy load here. Check to see if they've been loaded from file yet, if not, so it now.
        if (getSocialNetworkMap().get(personName.toLowerCase()) == null) {

            // load their data from the data manager
            Person person = personDataManager.loadPerson(personName);
            if (person != null) {

                // add their name to the data source if they're not there yet
                // (case of new player being loaded for the first time)
                if (!socialDataManager.hasPlayer(person.getName())) {
                    socialDataManager.addPlayer(person.getName());
                }

                // now wrap them in our SocialPerson object
                SocialPerson socialPerson = new SocialPerson(person);
                PersonMapValue value = new PersonMapValue(socialPerson, person);

                // add to the map using lower case name as key
                getSocialNetworkMap().put(person.getName().toLowerCase(), value);

                // return the new instance
                return getSocialNetworkMap().get(personName.toLowerCase()).getSocialPerson();
            }

        } else {

            // we found them in the network list, so return their object
            return getSocialNetworkMap().get(personName.toLowerCase()).getSocialPerson();
        }

        // nothing found
        return null;
    }

    public List<String> getPriests() {
        return new ArrayList<String>(socialDataManager.getPriests());
    }

    public boolean hasPriest(String playerName) {
        return socialDataManager.hasPriest(playerName);
    }

    public void addPriest(String playerName) {
        socialDataManager.addPriest(playerName);
    }

    public void removePriest(String playerName) {
        socialDataManager.removePriest(playerName);
    }

    public List<String> getLawyers() {
        return new ArrayList<String>(socialDataManager.getLawyers());
    }

    public boolean hasLawyer(String playerName) {
        return socialDataManager.hasLawyer(playerName);
    }

    public void addLawyer(String playerName) {
        socialDataManager.addLawyer(playerName);
    }

    public void removeLawyer(String playerName) {
        socialDataManager.removeLawyer(playerName);
    }

    public <P extends IPersonPerkSettings> P getPersonPerkSettings(String personName, String perkName) {

        PersonSettings settings = personDataManager.loadPersonSettings(personName);
        if (settings != null && settings.hasPerkSettings(perkName)) {
            return settings.getPerkSettings(perkName);
        }
        return null;
    }

    public <P extends IPersonPerkSettings> void setPersonPerkSettings(String personName, String perkName, P perkSettings) {

        PersonSettings settings = personDataManager.loadPersonSettings(personName);
        if (settings != null) {
            settings.setPerkSettings(perkName, perkSettings);
        } else {
            settings = new PersonSettings();
            settings.setPerkSettings(perkName, perkSettings);
        }

        // save it
        personDataManager.savePersonSettings(personName, settings);
    }

    private void removeFromAllGroups(SocialPerson socialPerson) {

        Map<String, SocialPerson> notifyPlayers = new HashMap<String, SocialPerson>();

        // go through each group type and remove this user from their lists
        Person person = getSocialNetworkMap().get(socialPerson.getName().toLowerCase()).getPerson();

        for (String memberName : person.getFriends().keySet()) {
            SocialPerson groupPerson = getPerson(memberName);
            if (groupPerson != null) {
                groupPerson.removeFriend(person.getName());
                notifyPlayers.put(groupPerson.getName(), groupPerson);
            }
        }

        for (String memberName : person.getAffairs().keySet()) {
            SocialPerson groupPerson = getPerson(memberName);
            if (groupPerson != null) {
                groupPerson.removeAffair(person.getName());
                notifyPlayers.put(groupPerson.getName(), groupPerson);
            }
        }

        for (String memberName : person.getRelationships().keySet()) {
            SocialPerson groupPerson = getPerson(memberName);
            if (groupPerson != null) {
                groupPerson.removeRelationship(person.getName());
                notifyPlayers.put(groupPerson.getName(), groupPerson);
            }
        }

        for (String memberName : person.getRelationships().keySet()) {
            SocialPerson groupPerson = getPerson(memberName);
            if (groupPerson != null) {
                groupPerson.removeRelationship(person.getName());
                notifyPlayers.put(groupPerson.getName(), groupPerson);
            }
        }

        for (String memberName : person.getChildren().keySet()) {
            SocialPerson groupPerson = getPerson(memberName);
            if (groupPerson != null) {
                groupPerson.breakChildOf();
                notifyPlayers.put(groupPerson.getName(), groupPerson);
            }
        }

        if (person.getChildOf() != null) {
            SocialPerson groupPerson = getPerson(person.getChildOf());
            if (groupPerson != null) {
                groupPerson.removeChild(person.getName());
                notifyPlayers.put(groupPerson.getName(), groupPerson);
            }
        }

        if (person.getEngagement() != null) {
            SocialPerson groupPerson = getPerson(person.getEngagement().getPlayerName());
            if (groupPerson != null) {
                groupPerson.breakEngagement();
                notifyPlayers.put(groupPerson.getName(), groupPerson);
            }
        }

        if (person.getDivorce() != null) {
            SocialPerson groupPerson = getPerson(person.getDivorce().getPlayerName());
            if (groupPerson != null) {
                groupPerson.endDivorce();
                notifyPlayers.put(groupPerson.getName(), groupPerson);
            }
        }

        if (person.getMarriage() != null) {
            SocialPerson groupPerson = getPerson(person.getMarriage().getPlayerName());
            if (groupPerson != null) {
                groupPerson.breakMarriage();
                notifyPlayers.put(groupPerson.getName(), groupPerson);
            }
        }

        // now, for each person in the map, send them an alert saying this person quit
        ResourcesConfig resources = PluginConfig.getInstance().getConfig(ResourcesConfig.class);
        String alertMessage = resources.getResource("social.alert.playerLeftNetwork");

        for (SocialPerson memberPerson : notifyPlayers.values()) {

            // remove all pending messages
            memberPerson.removeMessagesFrom(socialPerson.getName());

            // add the alert
            memberPerson.addAlert(socialPerson, Alert.Type.quit, alertMessage);

            // save the alert and the changes from above
            savePerson(memberPerson);
        }
    }
}
