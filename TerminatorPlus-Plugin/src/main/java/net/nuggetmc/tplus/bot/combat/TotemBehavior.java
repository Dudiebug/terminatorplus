package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;

/**
 * Passive clutch swap: when the bot drops low on health and has a totem
 * of undying somewhere in its inventory, move it to the offhand so the
 * vanilla pop-on-fatal-damage effect can fire.
 */
public final class TotemBehavior {

    private static final float LOW_HEALTH_TRIGGER = 6.0f;

    public void tick(Bot bot, LivingEntity target) {
        if (bot.getBotHealth() > LOW_HEALTH_TRIGGER) return;
        if (!bot.getBotInventory().hasTotem()) return;
        bot.getBotInventory().equipOffhand(Material.TOTEM_OF_UNDYING);
    }
}
