package com.netprogs.minecraft.plugins.social.listener.perk;

import java.util.List;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.config.settings.perk.HealthRegenSettings;
import com.netprogs.minecraft.plugins.social.storage.data.perk.IPersonPerkSettings;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityRegainHealthEvent;

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

public class HealthRegenListener extends PerkListener<HealthRegenSettings, IPersonPerkSettings> {

    public HealthRegenListener() {
        super(ListenerType.healthregen);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityRegainHealthEvent(EntityRegainHealthEvent event) {

        // if this event has already been cancelled, let's get out of here
        if (event.isCancelled()) {
            return;
        }

        if ((event.getEntity() instanceof Player)) {

            // get the person instance
            Player regenPlayer = (Player) event.getEntity();
            SocialPerson regenPerson = SocialNetworkPlugin.getStorage().getPerson(regenPlayer.getName());
            if (regenPerson != null) {

                // Let's do a quick check to see if they have any group members at all
                // If not, then no point in going any further
                if (!regenPerson.hasGroupMembers()) {

                    // SocialNetworkPlugin.log("HealthRegen: Player does not have any group members.");
                    return;
                }

                // Get the settings for this perk.
                HealthRegenSettings regenSettings = getPerkSettings(regenPerson);

                // If it returns null, that means the player does not have this peak assigned to any of their groups
                if (regenSettings == null) {

                    // SocialNetworkPlugin.log("HealthRegen: Player not in a social group containing this perk.");
                    return;

                } else {

                    int proximity = regenSettings.getProximity();
                    double proximityRadius = proximity / 2D;

                    int heartsPerkTick = regenSettings.getHeartsPerkTick();

                    // If heartsPerkTick is zero, then don't continue at all
                    // If a negative or zero is given, do not allow it to reduce them below 1 heart to avoid death.
                    if ((heartsPerkTick == 0) || (heartsPerkTick < 0 && regenPlayer.getHealth() <= 2)) {
                        return;
                    }

                    // Go through the list of all the nearby entities and see if any of them are players also
                    List<Entity> nearByEntities =
                            regenPlayer.getNearbyEntities(proximityRadius, proximityRadius, proximityRadius);

                    boolean hasNearGroupMembers = false;
                    for (Entity entity : nearByEntities) {

                        if (entity instanceof Player) {

                            Player nearByPlayer = (Player) entity;
                            SocialPerson nearByPerson =
                                    SocialNetworkPlugin.getStorage().getPerson(nearByPlayer.getName());

                            // Check to see if this player is in a group with the regen person
                            // If they are, then we can allow the regen person to gain the bonus
                            if (nearByPerson != null && regenPerson.hasGroupMemberWithPerk(nearByPerson, regenSettings)) {
                                hasNearGroupMembers = true;
                                break;
                            }
                        }
                    }

                    // if they had at least one group member near by, then give them their bonus also
                    if (hasNearGroupMembers) {
                        event.setAmount(event.getAmount() + heartsPerkTick);
                    }
                }
            }
        }
    }
}
