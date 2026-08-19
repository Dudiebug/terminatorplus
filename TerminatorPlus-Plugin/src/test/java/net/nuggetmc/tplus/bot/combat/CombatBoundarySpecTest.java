package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.loadout.Cooldowns;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Executable policy checks for the combat-fix specification.
 * World-dependent behavior is covered by the Paper runtime harness.
 */
class CombatBoundarySpecTest {

    @Test
    void normalMeleeAcceptsThreePointFiveBlocksButNotBeyond() {
        assertEquals(3.5, MeleeBehavior.ATTACK_RANGE);
        assertTrue(3.5 <= MeleeBehavior.ATTACK_RANGE);
        assertFalse(3.5001 <= MeleeBehavior.ATTACK_RANGE);
    }

    @Test
    void tridentMeleeFallbackUsesNormalMeleeBoundary() {
        assertEquals(MeleeBehavior.ATTACK_RANGE, TridentBehavior.MELEE_FALLBACK_DISTANCE);
    }

    @Test
    void interceptWindowIsClosedAndOrdinaryRangeIsOutsideIt() {
        assertFalse(ComboBehavior.isInterceptRange(29.99));
        assertTrue(ComboBehavior.isInterceptRange(30.0));
        assertTrue(ComboBehavior.isInterceptRange(35.0));
        assertTrue(ComboBehavior.isInterceptRange(42.0));
        assertFalse(ComboBehavior.isInterceptRange(42.01));
        assertEquals(ComboBehavior.ComboType.WIND_PEARL_INTERCEPT,
                ComboBehavior.typeForDistance(35.0));
        assertEquals(ComboBehavior.ComboType.WIND_PEARL_ENGAGE,
                ComboBehavior.typeForDistance(12.0));
    }

    @Test
    void comboThrottleIsTenSecondsAndCooldownRegistryBlocksUntilExpiry() {
        assertEquals(200, ComboBehavior.COOLDOWN_TICKS);

        Cooldowns cooldowns = new Cooldowns();
        cooldowns.set(EnderPearlBehavior.COOLDOWN_KEY, ComboBehavior.COOLDOWN_TICKS, 100);

        assertFalse(cooldowns.ready(EnderPearlBehavior.COOLDOWN_KEY, 100));
        assertFalse(cooldowns.ready(EnderPearlBehavior.COOLDOWN_KEY, 299));
        assertTrue(cooldowns.ready(EnderPearlBehavior.COOLDOWN_KEY, 300));
    }
}
