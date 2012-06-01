package com.netprogs.minecraft.plugins.social.listener.perk;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.command.util.MessageParameter;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.command.util.TimerUtil;
import com.netprogs.minecraft.plugins.social.config.PluginConfig;
import com.netprogs.minecraft.plugins.social.config.settings.SettingsConfig;
import com.netprogs.minecraft.plugins.social.config.settings.perk.PlayerDamageSettings;
import com.netprogs.minecraft.plugins.social.event.PlayerMemberChangeEvent;
import com.netprogs.minecraft.plugins.social.event.PlayerMemberChangeEvent.Type;
import com.netprogs.minecraft.plugins.social.storage.SocialNetwork;
import com.netprogs.minecraft.plugins.social.storage.data.perk.PersonPlayerDamageSettings;

import org.bukkit.ChatColor;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

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

public class PlayerDamageListener extends PerkListener<PlayerDamageSettings, PersonPlayerDamageSettings> {

    private final Logger logger = Logger.getLogger("Minecraft");

    public PlayerDamageListener() {
        super(ListenerType.damage);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMemberChangeEvent(PlayerMemberChangeEvent event) {

        // Something changed with your group members. Let's check to see if anyone left.

        // if the event is a remove, add a timer for a combination of this perk and the player
        if (event.getEventType() == Type.preRemove) {

            SocialPerson targetPerson = SocialNetwork.getInstance().getPerson(event.getPlayerName());
            SocialPerson damagerPerson = SocialNetwork.getInstance().getPerson(event.getMemberName());
            if (targetPerson != null && damagerPerson != null) {

                // start with a 30 second default. If there are valid settings found, we'll reassign
                long coolDownPeriod = 30L;

                // get the settings for this perk
                PlayerDamageSettings playerDamageSettings = getPerkSettings(targetPerson, damagerPerson);
                if (playerDamageSettings != null) {

                    // we have a valid settings, so grab the cool down from it
                    coolDownPeriod = playerDamageSettings.getCoolDownPeriod();

                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("PlayerDamage_Timer: Found group, setting timer to: " + coolDownPeriod);
                    }

                } else if (playerDamageSettings == null && !event.isGroupEmpty()) {

                    // A NULL settings means the player does not belong to any groups this perk is currently assigned to
                    //
                    // If the group is empty, it means the player that was removed was the last one making the settings
                    // above return as NULL because the player no longer belongs to that group.
                    //
                    // For that case, we want to use the default timer so allow that dropped person to be protected for
                    // a period of time regardless of recently being dropped from the group.
                    //
                    // If the settings are null and the group is not empty, then don't bother adding the default counter
                    // This way we can avoid making a timer entry when one isn't needed.
                    coolDownPeriod = 0;

                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("PlayerDamage_Timer: No group, but group is empty. Setting timer to 0.");
                    }

                } else {

                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("PlayerDamage_Timer: No group, but group is empty. Setting timer to default: 30");
                    }
                }

                // set the timer
                if (coolDownPeriod != 0) {
                    String timerKey = getListenerType() + "_" + event.getMemberName();
                    TimerUtil.updateEventTimer(event.getPlayerName(), timerKey, coolDownPeriod);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

        // if this event has already been cancelled, let's get out of here
        if (event.isCancelled()) {
            return;
        }

        // check early to see what's being damaged
        if (event.getEntityType() != EntityType.PLAYER && !(event.getEntity() instanceof Tameable)) {

            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                logger.info("PlayerDamage_Entity: not using target");
            }

            return;
        }

        // check early to see what's doing the damage
        if (!(event.getDamager() instanceof Player) && !(event.getDamager() instanceof Projectile)
                && !(event.getDamager() instanceof Tameable)) {

            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                logger.info("PlayerDamage_Entity: not using damager");
            }

            return;
        }

        // Let's find out what's getting hit first.
        Player target = null;

        // checking to see if it's a player
        if (event.getEntityType() == EntityType.PLAYER) {

            // looks like the player is being hit
            target = (Player) event.getEntity();
        }

        // still NULL, so check to see if it's a Tameable creature
        if (target == null && event.getEntity() instanceof Tameable) {

            // get the creature
            Tameable tameable = (Tameable) event.getEntity();

            // check to see if the owner of this creature is a player
            if (tameable.getOwner() != null && tameable.getOwner() instanceof Player) {

                // it is, so the target must be their pet
                target = (Player) tameable.getOwner();
            }
        }

        // no valid target, give up
        if (target == null) {
            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                logger.info("PlayerDamage_Entity: non-usable target");
            }
            return;
        }

        // Check right away to see if we should be handling this event.
        // We don't want to be here any longer than we have to.
        if (!SocialNetwork.getInstance().isSocialNetworkPlayer(target)) {
            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                logger.info("PlayerDamage_Entity: target not in network");
            }
            return;
        }

        // Now we want to find out who/what is doing the damage
        Tameable tameable = null;
        Player damager = null;

        boolean projectileDmg = false;
        boolean tameableDmg = false;
        boolean playerDmg = false;

        // let's see if it's a projectile
        if (event.getDamager() instanceof Projectile) {

            // let's see if a player used it
            Projectile p = (Projectile) event.getDamager();
            if (p.getShooter() instanceof Player) {
                damager = (Player) p.getShooter();
                projectileDmg = true;
            }
        }

        // no luck yet, so check to see if the damage is being caused by a tameable creature
        if (damager == null && event.getDamager() instanceof Tameable) {

            tameable = (Tameable) event.getDamager();
            if (tameable.getOwner() != null && tameable.getOwner() instanceof Player) {
                damager = (Player) tameable.getOwner();
                tameableDmg = true;
            }
        }

        // final check, let's see if it's another player
        if (damager == null && event.getDamager() instanceof Player) {
            damager = (Player) event.getDamager();
            playerDmg = true;
        }

        // no valid damager, give up
        if (damager == null) {
            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                logger.info("PlayerDamage_Entity: non-usable damager");
            }
            return;
        }

        // At this point, both the target and the damager as player instances. So let's load their network data.

        // first, lets check to see if this event is still on timer
        String timerKey = getListenerType() + "_" + damager.getName();
        long timeRemaining = TimerUtil.eventOnTimer(target.getName(), timerKey);
        if (timeRemaining > 0) {

            if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                logger.info("PlayerDamage_Timer: " + target.getName() + " on timer for: "
                        + TimerUtil.formatTime(timeRemaining));
            }

            // Send the user a message saying they can't hurt the other person for the timeRemaining
            List<MessageParameter> parameters = new ArrayList<MessageParameter>();

            MessageParameter playerParam = new MessageParameter("<player>", damager.getName(), ChatColor.AQUA);
            parameters.add(playerParam);

            parameters.addAll(MessageUtil.createCoolDownFormatting(timeRemaining));

            MessageUtil.sendMessage(damager, "social.perk.damage.ontimer.sender", ChatColor.RED, parameters);

            // cancel the event
            event.setCancelled(true);
            return;
        }

        // Check to make sure they both loaded properly.
        // The above can also return NULL if one or both of them are not in the network.
        SocialPerson targetPerson = SocialNetwork.getInstance().getPerson(target.getName());
        SocialPerson damagePerson = SocialNetwork.getInstance().getPerson(damager.getName());
        if (damagePerson != null && targetPerson != null) {

            // Get the settings for this perk.
            PlayerDamageSettings playerDamageSettings = getPerkSettings(targetPerson, damagePerson);

            // If it returns null, that means the player does not have this peak assigned to any of their current groups
            if (playerDamageSettings == null) {
                if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                    logger.info("PlayerDamage_SettingsCheck: Players not in a social group containing this perk.");
                }
                return;

            } else {

                // if they're allowing player damage and it was coming from them, don't continue
                if (playerDamageSettings.isDamageAllowedFromPlayer() && playerDmg) {
                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("PlayerDamage_SettingsCheck: Player Damage allowed");
                    }
                    return;
                }

                // if they're allowing tameable damage and it was coming from them, don't continue
                if (playerDamageSettings.isDamageAllowedFromTameable() && tameableDmg) {
                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("PlayerDamage_SettingsCheck: Tameable Damage allowed");
                    }
                    return;
                }

                // if they're allowing projectile damage and it was coming from them, don't continue
                if (playerDamageSettings.isDamageAllowedFromProjectile() && projectileDmg) {
                    if (PluginConfig.getInstance().getConfig(SettingsConfig.class).isLoggingDebug()) {
                        logger.info("PlayerDamage_SettingsCheck: Projectile Damage allowed");
                    }
                    return;
                }

                // Check to make sure the person firing the event is in the network.
                // Don't bother returning anything if they're not.

                // PersonPlayerDamageSettings personSettings = getPersonPerkSettings(playerPerson);

                // If one of them has it, the other one does. So lets kill the damage.
                event.setCancelled(true);

                // If the original damager was a creature, tell it to stop
                if (tameable != null && tameable instanceof Creature) {
                    Creature creature = (Creature) tameable;
                    if (creature.getTarget().equals(target)) {
                        creature.setTarget(null);
                    }
                }
            }
        }
    }
}
