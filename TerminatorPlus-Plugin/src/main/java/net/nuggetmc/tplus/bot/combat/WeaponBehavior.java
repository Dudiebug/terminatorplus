package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.entity.LivingEntity;

/**
 * Strategy for how a bot should use a particular weapon against a target.
 * A single behavior instance is shared across all bots and must be
 * stateless — per-bot state lives on the bot (inventory, cooldowns,
 * combat state machine).
 *
 * <p>{@link #ticksFor(Bot, LivingEntity, double)} is called every tick
 * while the bot's combat director has chosen this weapon. The behavior
 * is responsible for its own cooldown management via {@link Bot#getCooldowns()}.
 */
public interface WeaponBehavior {

    /**
     * @return how many ticks to delay before switching away and
     *         re-evaluating the target situation. Return 0 to let the
     *         director re-evaluate next tick.
     */
    int ticksFor(Bot bot, LivingEntity target, double distance);
}
