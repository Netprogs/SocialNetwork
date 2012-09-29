package com.netprogs.minecraft.plugins.social.listener.perk;

import java.util.ArrayList;
import java.util.List;

import com.netprogs.minecraft.plugins.social.SocialNetworkPlugin;
import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.command.util.MessageParameter;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.config.settings.perk.BonusExperienceSettings;
import com.netprogs.minecraft.plugins.social.storage.data.perk.IPersonPerkSettings;

import org.bukkit.ChatColor;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;

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

public class BonusExperienceListener extends PerkListener<BonusExperienceSettings, IPersonPerkSettings> {

    public BonusExperienceListener() {
        super(ListenerType.bonusxp);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeathEvent(EntityDeathEvent event) {

        // make sure the killer is a player and the entity being killed is a creature
        if ((event.getEntity().getKiller() instanceof Player) && (event.getEntity() instanceof Creature)) {

            // get the killer person instance
            Player killerPlayer = event.getEntity().getKiller();
            SocialPerson killerPerson = SocialNetworkPlugin.getStorage().getPerson(killerPlayer.getName());

            if (killerPerson != null) {

                // Let's do a quick check to see if they have any group members at all
                // If not, then no point in going any further
                if (!killerPerson.hasGroupMembers()) {

                    SocialNetworkPlugin.log("onEntityDeathEvent: Player does not have any group members.");
                    return;
                }

                // Get the settings for this perk.
                BonusExperienceSettings bonusSettings = getPerkSettings(killerPerson);

                // If it returns null, that means the player does not have this peak assigned to any of their groups
                if (bonusSettings == null) {

                    SocialNetworkPlugin.log("onEntityDeathEvent: Player not in a social group containing this perk.");
                    return;

                } else {

                    int proximity = bonusSettings.getProximity();
                    int flatBonus = bonusSettings.getFlatBonus();
                    double proximityRadius = proximity / 2D;

                    double percentBonus = 0;
                    if (bonusSettings.getPercentBonus() != 0) {
                        percentBonus = bonusSettings.getPercentBonus() / 100D;
                    }

                    // get the original XP drop
                    int droppedXp = event.getDroppedExp();

                    SocialNetworkPlugin.log("proximity: " + proximity);
                    SocialNetworkPlugin.log("proximityRadius: " + proximityRadius);
                    SocialNetworkPlugin.log("flatBonus: " + flatBonus);
                    SocialNetworkPlugin.log("percentBonus: " + percentBonus);
                    SocialNetworkPlugin.log("droppedXp: " + droppedXp);

                    // calculate the XP bonus that will be gained
                    int bonusXp = 0;

                    // if a percentage bonus is given, apply it now
                    if (percentBonus != 0) {

                        int percentXp = (int) Math.floor(droppedXp / percentBonus);
                        bonusXp += percentXp;

                        SocialNetworkPlugin.log("percentXp: " + percentXp);
                    }

                    // add the flat XP bonus
                    bonusXp += flatBonus;

                    SocialNetworkPlugin.log("bonusXp: " + bonusXp);

                    // combine the two xp values
                    droppedXp = droppedXp + bonusXp;

                    // create the message parameters to be sent to all the players
                    MessageParameter bonusParam =
                            new MessageParameter("<bonus>", Integer.toString(bonusXp), ChatColor.AQUA);
                    MessageParameter totalParam =
                            new MessageParameter("<total>", Integer.toString(droppedXp), ChatColor.AQUA);

                    List<MessageParameter> requestParameters = new ArrayList<MessageParameter>();
                    requestParameters.add(bonusParam);
                    requestParameters.add(totalParam);

                    // Go through the list of all the nearby entities and see if any of them are players also
                    List<Entity> nearByEntities =
                            killerPlayer.getNearbyEntities(proximityRadius, proximityRadius, proximityRadius);

                    // boolean hasNearGroupMembers = false;
                    for (Entity entity : nearByEntities) {

                        if (entity instanceof Player) {

                            Player nearByPlayer = (Player) entity;
                            SocialPerson nearByPerson =
                                    SocialNetworkPlugin.getStorage().getPerson(nearByPlayer.getName());

                            // Check to see if any of them are in any of the killers groups and give them the bonus
                            if (nearByPerson != null
                                    && killerPerson.hasGroupMemberWithPerk(nearByPerson, bonusSettings)) {

                                nearByPlayer.giveExp(droppedXp);

                                SocialNetworkPlugin.log("xp: " + droppedXp + " to: " + nearByPlayer.getName());

                                MessageUtil.sendMessage(nearByPlayer, "social.perk.bonusxp.gained.player",
                                        ChatColor.GREEN, requestParameters);

                                // hasNearGroupMembers = true;
                            }
                        }
                    }

                    //
                    // FIX: Instead of giving the killer their xp ourselves, we'll just allow Bukkit
                    // to continue on with whatever it needs to do next and not get in the way.
                    //
                    // // if they had at least one group member near by, then give the killer their bonus
                    // if (hasNearGroupMembers) {
                    //
                    // killerPlayer.giveExp(droppedXp);
                    //
                    // SocialNetworkPlugin.log("xp: " + droppedXp + " to: " + killerPlayer.getName());
                    //
                    // MessageUtil.sendMessage(killerPlayer, "social.perk.bonusxp.gained.player", ChatColor.GREEN,
                    // requestParameters);
                    // }
                    //
                    // // set the xp for this event to 0 since we've already handled it above
                    // event.setDroppedExp(0);
                }
            }
        }
    }
}
