package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.entity.LivingEntity;

public final class MeleeBehavior implements WeaponBehavior {

    @Override
    public int ticksFor(Bot bot, LivingEntity target, double distance) {
        if (distance > 4.0) return 0;
        if (bot.tickDelay(3)) {
            bot.attack(target);
        }
        return 0;
    }
}
