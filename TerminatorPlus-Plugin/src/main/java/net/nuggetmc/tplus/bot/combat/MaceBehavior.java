package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import net.nuggetmc.tplus.bot.loadout.BotInventory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

/**
 * Jump-smash behavior for the 1.21 mace. The bot fires a wind charge down at
 * its own feet and launches upward on the same tick; the vertical impulse puts
 * the bot 8–10 blocks above the ground, well above the target, and the mace
 * dive that follows slams the target with full fall-damage + density bonus.
 */
public final class MaceBehavior implements WeaponBehavior {

    public static final String COOLDOWN_KEY = "mace";
    private static final double MIN_DISTANCE = 0.5;
    private static final double MAX_DISTANCE = 6.0;
    private static final double ATTACK_RANGE = 3.5;
    private static final int JUMP_COOLDOWN = 55;

    /**
     * Total upward impulse (Y component of initial velocity). At vanilla gravity
     * (-0.08 b/t after the first tick) and no drag, {@code 2.0} sustains positive
     * vy for ~25 ticks and produces a practical peak of ~8–10 blocks before the
     * dive reacquires the target. Big enough that the wind-charge detonation at
     * the feet reads as the cause of the launch, small enough that airtime
     * (≈36 ticks to apex + descent) still clears the 33-tick mace recharge.
     */
    private static final double JUMP_Y = 2.0;
    /** Horizontal impulse blended with target direction. */
    private static final double JUMP_XZ = 0.25;

    @Override
    public int ticksFor(Bot bot, LivingEntity target, double distance) {
        CombatState state = bot.getCombatState();
        Location targetLoc = target.getLocation();
        bot.faceLocation(targetLoc);

        if (distance < MIN_DISTANCE) {
            state.reset();
            return 0;
        }
        // During an aerial dive the bot may be far above the target; let it keep tracking.
        if (distance > MAX_DISTANCE && state.getPhase() != CombatState.Phase.AIRBORNE) {
            state.reset();
            return 0;
        }

        Vector botPos = bot.getLocation().toVector();
        Vector toTarget = targetLoc.toVector().subtract(botPos);

        switch (state.getPhase()) {
            case CHARGING:
            case IDLE:
            case RELEASE: {
                if (!bot.getBotCooldowns().ready(COOLDOWN_KEY, bot.getAliveTicks())) {
                    int left = bot.getBotCooldowns().remaining(COOLDOWN_KEY, bot.getAliveTicks());
                    CombatDebugger.maceCd(bot, left);
                    // Stay close but don't jump-smash. Fall through to melee on the sword if lateral.
                    if (distance <= ATTACK_RANGE && BotCombatTiming.canSwing(bot, target)) {
                        doAttack(bot, target);
                    }
                    return 0;
                }
                if (!bot.isBotOnGround()) {
                    CombatDebugger.log(bot, "mace-skip", "reason=airborne-no-commit");
                    return 0;
                }

                Vector horiz = toTarget.clone();
                horiz.setY(0);
                if (horiz.lengthSquared() > 1.0e-6) horiz.normalize().multiply(JUMP_XZ);
                Vector launch = horiz.setY(JUMP_Y);

                // Wind-charge-at-feet visual effect. Fires BEFORE bot.jump() so
                // the explosion particles/sound read as the cause of the launch.
                // The wind charge is thrown from eye height straight down so it
                // bursts at the bot's feet on the next tick; spawning it right
                // at feet level would have it collide with the ground on spawn
                // and fizzle silently. We intentionally do NOT use the vanilla
                // knockback physics for the launch — bot.jump()'s direct velocity
                // set is what carries the bot; the wind charge is purely for
                // player-readable cause-and-effect.
                //
                // If the bot has no wind charges (non-mace kits that happen to
                // satisfy the CombatDirector smash gate), we just skip the throw
                // and go straight to the jump. Consume is best-effort — the
                // mace jump still fires without it.
                throwWindChargeAtFeet(bot);

                bot.jump(launch);
                bot.getBotCooldowns().set(COOLDOWN_KEY, JUMP_COOLDOWN, bot.getAliveTicks());
                CombatDebugger.macePhase(bot, state.getPhase(), CombatState.Phase.AIRBORNE);
                state.setPhase(CombatState.Phase.AIRBORNE);
                bot.getLocation().getWorld().playSound(bot.getLocation(), Sound.ENTITY_PLAYER_BIG_FALL, 0.3f, 1.6f);
                return 0;
            }
            case AIRBORNE: {
                // Track the target on the way down; accelerate harder during a fast dive.
                Vector vel = bot.getVelocity();
                if (vel.getY() < -0.2) {
                    Vector horiz = toTarget.clone();
                    horiz.setY(0);
                    if (horiz.lengthSquared() > 1.0e-6) {
                        double speed = vel.getY() < -0.6 ? 0.28 : 0.15;
                        horiz.normalize().multiply(speed);
                        bot.walk(horiz);
                    }
                }
                // Fire the smash WHILE STILL AIRBORNE so fallDistance > 0 when
                // Player.attack() reads it — that's what makes the hit a crit
                // AND what triggers the mace smash's density/fall-damage bonus.
                // Waiting for bot.isBotOnGround() means fallDistance is already
                // reset to 0 by then, downgrading the smash to a normal 6-dmg
                // mace swing.
                if (distance <= ATTACK_RANGE && !bot.isBotOnGround() && vel.getY() < -0.3) {
                    boolean iframes = BotCombatTiming.targetHasIFrames(target);
                    CombatDebugger.maceSmash(bot, vel.getY(), iframes, bot.isBotOnGround());
                    if (!iframes) {
                        doAttack(bot, target);
                    }
                    CombatDebugger.macePhase(bot, state.getPhase(), CombatState.Phase.IDLE);
                    state.reset();
                    return 0;
                }
                // Safety reset: if the bot has landed without firing (target
                // ran out of range, grazed the ground, etc.), drop back to
                // IDLE so the pipeline can try a different weapon. Without
                // this the bot ticks forever in AIRBORNE swinging with stale
                // items in hand.
                if (bot.isBotOnGround()) {
                    CombatDebugger.macePhase(bot, state.getPhase(), CombatState.Phase.IDLE);
                    state.reset();
                }
                return 0;
            }
            default:
                return 0;
        }
    }

    private void doAttack(Bot bot, LivingEntity target) {
        bot.punch();
        if (bot.getBukkitEntity() instanceof Player attacker) {
            // Use vanilla attack so 1.21 mace fall-damage scaling + density bonuses apply.
            attacker.attack(target);
        } else {
            bot.attack(target);
        }
    }

    /**
     * Spawn a wind-charge entity at the bot's eye level aimed straight down at
     * its own feet, consume one wind charge from inventory, and play the throw
     * sound. Visual only — the actual vertical impulse is driven by
     * {@link Bot#jump(Vector)} below.
     *
     * <p>Spawning at eye height + 0.5 blocks forward, with a solid downward
     * velocity, means the wind charge is visible to spectators for ~2 ticks
     * before it bursts at ground level next to the bot's feet. Spawning
     * directly at feet level caused the wind charge to intersect the ground
     * block on spawn, which vanilla handles by silently deleting the entity
     * (no burst, no sound).
     */
    private static void throwWindChargeAtFeet(Bot bot) {
        BotInventory inv = bot.getBotInventory();
        if (!inv.hasWindCharge()) {
            // No wind charge available — the mace smash still fires below, just
            // without the visual. Use the existing big-fall sound as a fallback
            // audio cue so there's still a cause-and-effect read for spectators.
            return;
        }

        Location botLoc = bot.getLocation();
        Location spawn = botLoc.clone().add(0, bot.getBukkitEntity().getEyeHeight() - 0.1, 0);
        Vector downward = new Vector(0, -1.5, 0);

        spawn.getWorld().spawn(spawn, WindCharge.class, w -> {
            w.setShooter(bot.getBukkitEntity());
            w.setVelocity(downward);
        });
        botLoc.getWorld().playSound(botLoc, Sound.ENTITY_WIND_CHARGE_THROW, 1f, 1.1f);
        consumeOneWindCharge(inv);
    }

    /**
     * Pull one wind charge from the bot's inventory. Mace loadouts store the
     * wind-charge stack in the offhand (see {@code BotInventory.autoEquip}),
     * so we check offhand first; otherwise scan the hotbar + storage.
     */
    private static void consumeOneWindCharge(BotInventory inv) {
        PlayerInventory raw = inv.raw();
        ItemStack off = raw.getItemInOffHand();
        if (off != null && off.getType() == Material.WIND_CHARGE) {
            int amt = off.getAmount();
            if (amt <= 1) {
                inv.getBot().setItemOffhand(new ItemStack(Material.AIR));
            } else {
                off.setAmount(amt - 1);
                inv.getBot().setItemOffhand(off);
            }
            return;
        }
        for (int i = 0; i < 36; i++) {
            ItemStack it = raw.getItem(i);
            if (it == null || it.getType() != Material.WIND_CHARGE) continue;
            int amt = it.getAmount();
            if (amt <= 1) {
                raw.setItem(i, new ItemStack(Material.AIR));
            } else {
                it.setAmount(amt - 1);
                raw.setItem(i, it);
            }
            return;
        }
    }
}
