package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

/**
 * Cooldown-aware melee. Swings only when:
 * <ul>
 *   <li>attack-strength charge &gt;= 0.95 (full-damage + crit/sweep eligible), and</li>
 *   <li>the target isn't deep in an i-frame window that would swallow the hit, and</li>
 *   <li>the currently-held item is an actual melee weapon — otherwise a scanner
 *       play that left a cobweb / wind charge / pearl / crystal in hand last
 *       tick would make the bot "swing" that utility item, wasting the swing
 *       cooldown and producing melee-hit log lines with w=AIR / COBWEB / etc.</li>
 * </ul>
 *
 * <p>Only reached via {@link CombatDirector#tick}, which short-circuits for neural-network
 * training bots — training still uses the deterministic damage table.
 */
public final class MeleeBehavior implements WeaponBehavior {

    private static final double ATTACK_RANGE = 4.0;

    @Override
    public int ticksFor(Bot bot, LivingEntity target, double distance) {
        if (distance > ATTACK_RANGE) {
            CombatDebugger.log(bot, "melee-oor", "dist=" + String.format("%.2f", distance));
            return 0;
        }
        ItemStack held = bot.getBotInventory().getSelected();
        if (!isMeleeWeapon(held)) {
            CombatDebugger.log(bot, "melee-skip", "reason=non-melee-held held=" + held.getType().name());
            return 0;
        }
        float charge = bot.getAttackStrengthScale(0.0f);
        boolean iframes = BotCombatTiming.targetHasIFrames(target);
        CombatDebugger.meleeTry(bot, charge, iframes, distance);
        if (!BotCombatTiming.canSwing(bot, target)) {
            return 0;
        }
        bot.attack(target);
        CombatDebugger.meleeHit(bot, held.getType().name());
        return 0;
    }

    private static boolean isMeleeWeapon(ItemStack stack) {
        if (stack == null) return false;
        Material m = stack.getType();
        if (m == Material.AIR) return false;
        if (m == Material.MACE || m == Material.TRIDENT) return true;
        String name = m.name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE");
    }
}
