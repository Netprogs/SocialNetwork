package com.netprogs.minecraft.plugins.social.listener.perk;

import java.util.List;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.config.settings.perk.FoodShareSettings;
import com.netprogs.minecraft.plugins.social.storage.data.perk.IPersonPerkSettings;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.FoodLevelChangeEvent;

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

public class FoodShareListener extends PerkListener<FoodShareSettings, IPersonPerkSettings> {

    public FoodShareListener() {
        super(ListenerType.foodshare);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onFoodLevelChangeEvent(FoodLevelChangeEvent event) {

        // if this event has already been cancelled, let's get out of here
        if (event.isCancelled()) {
            return;
        }

        if ((event.getEntity() instanceof Player)) {

            Player sharePlayer = (Player) event.getEntity();

            // calculate the food bonus
            int bonusFoodLevel = event.getFoodLevel() - sharePlayer.getFoodLevel();

            // only give the bonus if it's a gain
            if (bonusFoodLevel <= 0) {
                // SocialNetworkPlugin.log("No Gain.");
                return;
            }

            // get the person instance
            SocialPerson sharePerson = SocialNetworkPlugin.getStorage().getPerson(sharePlayer.getName());
            if (sharePerson != null) {

                // Let's do a quick check to see if they have any group members at all
                // If not, then no point in going any further
                if (!sharePerson.hasGroupMembers()) {
                    // SocialNetworkPlugin.log("FoodShare: Player does not have any group members.");
                    return;
                }

                // Get the settings for this perk.
                FoodShareSettings foodShareSettings = getPerkSettings(sharePerson);

                // If it returns null, that means the player does not have this peak assigned to any of their groups
                if (foodShareSettings == null) {

                    // SocialNetworkPlugin.log("FoodShare: Player not in a social group containing this perk.");
                    return;

                } else {

                    int proximity = foodShareSettings.getProximity();
                    double proximityRadius = proximity / 2D;

                    // SocialNetworkPlugin.log("playerLevel: " + sharePlayer.getFoodLevel());
                    // SocialNetworkPlugin.log("eventLevel: " + event.getFoodLevel());
                    // SocialNetworkPlugin.log("bonusFoodLevel: " + bonusFoodLevel);

                    // Go through the list of all the nearby entities and see if any of them are players also
                    List<Entity> nearByEntities =
                            sharePlayer.getNearbyEntities(proximityRadius, proximityRadius, proximityRadius);

                    for (Entity entity : nearByEntities) {

                        if (entity instanceof Player) {

                            Player nearByPlayer = (Player) entity;
                            SocialPerson nearByPerson =
                                    SocialNetworkPlugin.getStorage().getPerson(nearByPlayer.getName());

                            // Check to see if this player is in a group with the share person
                            // If they are, then give them the same amount of food level change
                            if (nearByPerson != null
                                    && sharePerson.hasGroupMemberWithPerk(nearByPerson, foodShareSettings)) {

                                // set their food level
                                nearByPlayer.setFoodLevel(nearByPlayer.getFoodLevel() + bonusFoodLevel);
                            }
                        }
                    }
                }
            }
        }
    }
}
