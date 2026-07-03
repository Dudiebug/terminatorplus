package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.WindCharge;
import org.bukkit.util.Vector;

/**
 * Wind charges have two roles for bots:
 * <ul>
 *   <li><b>Combat zoning</b> ({@link #ticksFor}): lobbed at the target's eye for
 *       knockback when no trident is available. Runs in the combat dispatch path
 *       at 4–30 block range.</li>
 *   <li><b>Self-propulsion</b> ({@link #tickMovementBoost}): deliberately thrown to
 *       launch the bot in a calculated direction (up over a ledge, down onto a
 *       target, or forward across a flat gap). Runs only OUTSIDE combat range,
 *       uses a short windup so the throw reads as a deliberate build, and is
 *       paced by a long cooldown (~6s) so the bot isn't constantly blasting itself.</li>
 * </ul>
 */
public final class WindChargeBehavior implements WeaponBehavior {

    // -- Combat (offensive) wind charge --
    public static final String COOLDOWN_KEY = "windcharge";
    private static final int COOLDOWN = 55;
    private static final double MIN_DISTANCE = 4.0;
    private static final double MAX_DISTANCE = 30.0;
    private static final double SPEED = 1.6;

    // -- Wind charge + ender pearl combo --
    public static final String PEARL_COMBO_COOLDOWN_KEY = "wind_pearl_combo";
    private static final int PEARL_COMBO_COOLDOWN = 100;
    private static final int PEARL_COMBO_PEARL_COOLDOWN = 60;
    private static final int PEARL_COMBO_RELEASE_TICKS = 4;
    private static final int PEARL_COMBO_MONITOR_TICKS = 14;
    private static final double PEARL_COMBO_MIN_DISTANCE = 30.0;
    private static final double PEARL_COMBO_MAX_DISTANCE = 42.0;
    private static final double PEARL_COMBO_WIND_SPEED = 0.95;
    private static final double PEARL_COMBO_PEARL_SPEED = 1.9;
    private static final double PEARL_COMBO_LEAD_TICKS = 2.0;
    private static final double PEARL_COMBO_CONTACT_RADIUS_SQ = 1.35 * 1.35;

    // -- Self-propulsion wind charge --
    public static final String BOOST_COOLDOWN_KEY = "windcharge_boost";
    /** Long cooldown — this is a deliberate strategic tool, not a constant boost. */
    private static final int BOOST_COOLDOWN = 120;
    /** Windup ticks between "decide to boost" and "spawn the charge" — the visible build. */
    private static final int BOOST_WINDUP_TICKS = 4;
    /** Below this, the bot is in combat range — let melee/mace/trident work without wind-charge interference. */
    private static final double BOOST_MIN_DISTANCE = 12.0;
    /** Above this, pearls take over (see {@link EnderPearlBehavior}). */
    private static final double BOOST_MAX_DISTANCE = 28.0;
    /** Vertical delta that triggers an up/down launch instead of a horizontal one. */
    private static final double VERTICAL_TRIGGER = 3.0;

    @Override
    public int ticksFor(Bot bot, LivingEntity target, double distance) {
        int alive = bot.getAliveTicks();
        if (distance < MIN_DISTANCE || distance > MAX_DISTANCE) {
            CombatDebugger.log(bot, "wind-skip", "reason=range dist=" + String.format("%.2f", distance));
            return 0;
        }
        if (!bot.getBotCooldowns().ready(COOLDOWN_KEY, alive)) {
            CombatDebugger.log(bot, "wind-skip", "reason=cooldown left=" + bot.getBotCooldowns().remaining(COOLDOWN_KEY, alive));
            return 0;
        }
        int slot = bot.getBotInventory().findMainInventory(Material.WIND_CHARGE);
        if (slot < 0) {
            CombatDebugger.log(bot, "wind-skip", "reason=no-wind-charge");
            return 0;
        }
        slot = bot.getBotInventory().selectMainInventorySlot(slot);
        if (slot < 0) {
            CombatDebugger.log(bot, "wind-skip", "reason=no-selectable-slot");
            return 0;
        }

        Location spawn = bot.getLocation().add(0, bot.getBukkitEntity().getEyeHeight() - 0.1, 0);
        Vector aim = target.getEyeLocation().toVector().subtract(spawn.toVector()).normalize();

        bot.faceLocation(target.getLocation());
        bot.punch();
        bot.getActionController().recordDirectShortcut(bot, BotActionState.USING_WIND_CHARGE,
                "direct-windcharge-spawn", slot);

        spawn.getWorld().spawn(spawn, WindCharge.class, w -> {
            w.setVelocity(aim.multiply(SPEED));
            w.setShooter(bot.getBukkitEntity());
        });

        spawn.getWorld().playSound(spawn, Sound.ENTITY_WIND_CHARGE_THROW, 1f, 1f);
        CombatDebugger.log(bot, "wind-throw", "mode=combat dist=" + String.format("%.2f", distance));
        bot.getBotInventory().decrementMainInventorySlot(slot, 1);
        bot.getBotCooldowns().set(COOLDOWN_KEY, COOLDOWN, alive);
        return COOLDOWN;
    }

    /**
     * Self-boost: plans a calculated wind-charge throw and fires it after a short windup.
     * Called every tick by {@link CombatDirector} but the vast majority of ticks no-op
     * quickly — the real throw only happens 10 times per minute or so.
     *
     * <p>Flow:
     * <ol>
     *   <li>If a plan is pending, check if the windup has elapsed. If so, spawn the
     *       charge and clear the plan. Otherwise do nothing (let it wind up).</li>
     *   <li>If no plan: guard on distance (12–28), on-ground, idle combat state, and
     *       cooldown. If all pass, compute an aim direction based on target geometry
     *       and record a plan that'll fire in {@link #BOOST_WINDUP_TICKS} ticks.</li>
     * </ol>
     */
    public void tickMovementBoost(Bot bot, LivingEntity target, double distance) {
        WindChargeMovePlan plan = bot.pendingWindChargePlan;
        if (plan != null) {
            // Abort the throw if the situation changed mid-windup: bot took off, got
            // knocked into a combat action (mace jump, trident charge), or similar.
            // Firing the wind charge now would disrupt that higher-priority action.
            boolean stillValid = bot.getCombatState().getPhase() == CombatState.Phase.IDLE
                    && bot.isBotOnGround();
            if (!stillValid) {
                CombatDebugger.log(bot, "wind-boost-cancel", "reason=state-changed");
                bot.pendingWindChargePlan = null;
                return;
            }
            if (bot.getAliveTicks() >= plan.fireAtTick) {
                CombatDebugger.log(bot, "wind-boost-fire", "mode=" + plan.mode.name());
                executePlan(bot, plan);
                bot.pendingWindChargePlan = null;
            }
            return;
        }

        if (tryPearlCombo(bot, target, distance)) return;

        if (distance < BOOST_MIN_DISTANCE || distance > BOOST_MAX_DISTANCE) return;
        if (!bot.isBotOnGround()) return;
        if (!bot.getBotInventory().hasWindCharge()) return;
        // Only boost when NOT mid-combat-action — don't interrupt a mace jump, trident charge, etc.
        if (bot.getCombatState().getPhase() != CombatState.Phase.IDLE) return;
        if (!bot.getBotCooldowns().ready(BOOST_COOLDOWN_KEY, bot.getAliveTicks())) return;

        Vector toTarget = target.getLocation().toVector().subtract(bot.getLocation().toVector());
        double dy = toTarget.getY();
        Vector horiz = toTarget.clone().setY(0);
        if (horiz.lengthSquared() < 0.01) return;
        horiz.normalize();

        Vector placement;
        Vector velocity;
        WindChargeMovePlan.Mode mode;
        if (dy >= VERTICAL_TRIGGER) {
            // Target is high above — place below bot so the explosion launches it UP.
            placement = new Vector(0, -0.8, 0);
            velocity = new Vector(0, -0.4, 0);
            mode = WindChargeMovePlan.Mode.LAUNCH_UP;
        } else if (dy <= -VERTICAL_TRIGGER) {
            // Target is far below — place above bot's head so the explosion launches it DOWN.
            placement = new Vector(0, 2.0, 0);
            velocity = new Vector(0, 0.4, 0);
            mode = WindChargeMovePlan.Mode.LAUNCH_DOWN;
        } else {
            // Roughly level — place behind bot so the explosion launches it FORWARD.
            placement = horiz.clone().multiply(-0.7);
            placement.setY(0.4);
            velocity = horiz.clone().multiply(-0.5);
            velocity.setY(-0.3);
            mode = WindChargeMovePlan.Mode.LAUNCH_FORWARD;
        }

        // Face the target now so the windup animation reads correctly.
        bot.faceLocation(target.getLocation());

        bot.pendingWindChargePlan = new WindChargeMovePlan(
                bot.getAliveTicks() + BOOST_WINDUP_TICKS,
                placement,
                velocity,
                mode
        );
        CombatDebugger.log(bot, "wind-boost-plan",
                "mode=" + mode.name() + " dist=" + String.format("%.2f", distance) + " dy=" + String.format("%.2f", dy));

        // Audible wind-up so players can hear/see the bot is building the throw.
        bot.getLocation().getWorld().playSound(bot.getLocation(),
                Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 2.0f);

    }

    private boolean tryPearlCombo(Bot bot, LivingEntity target, double distance) {
        int alive = bot.getAliveTicks();
        if (distance < PEARL_COMBO_MIN_DISTANCE || distance > PEARL_COMBO_MAX_DISTANCE) return false;
        if (!bot.isBotOnGround()) return false;
        if (bot.getCombatState().getPhase() != CombatState.Phase.IDLE) return false;
        if (!bot.getBotCooldowns().ready(PEARL_COMBO_COOLDOWN_KEY, alive)) return false;
        if (!bot.getBotCooldowns().ready(COOLDOWN_KEY, alive)) return false;
        if (!bot.getBotCooldowns().ready(EnderPearlBehavior.COOLDOWN_KEY, alive)) return false;
        if (!bot.getBotInventory().hasEnderPearl()) return false;
        if (bot.getBotInventory().findMainInventory(Material.WIND_CHARGE) < 0) return false;
        if (!bot.getActionController().canStart(bot, BotActionState.USING_PEARL, "wind-pearl-combo")) return false;

        int previousSlot = bot.getBotInventory().getSelectedHotbarSlot();
        int windSlot = bot.getBotInventory().selectMaterial(Material.WIND_CHARGE);
        if (windSlot < 0) {
            CombatDebugger.log(bot, "wind-pearl-skip", "reason=no-selectable-wind");
            bot.getBotInventory().restoreSelectedSlotOrBestWeapon(previousSlot);
            return false;
        }

        Location windSpawn = bot.getLocation().add(0, bot.getBukkitEntity().getEyeHeight() - 0.1, 0);
        Vector windAim = comboPoint(bot, target).subtract(windSpawn.toVector());
        if (windAim.lengthSquared() < 1.0e-6) {
            CombatDebugger.log(bot, "wind-pearl-skip", "reason=zero-wind-aim");
            bot.getBotInventory().restoreSelectedSlotOrBestWeapon(previousSlot);
            return false;
        }
        WindCharge charge = spawnComboCharge(bot, windSpawn, windAim.normalize());
        if (!bot.getBotInventory().decrementMainInventorySlot(windSlot, 1)) {
            CombatDebugger.log(bot, "wind-pearl-cancel", "reason=no-wind-stack");
            charge.remove();
            bot.getBotInventory().restoreSelectedSlotOrBestWeapon(previousSlot);
            return false;
        }

        int pearlSlot = bot.getBotInventory().selectMaterial(Material.ENDER_PEARL);
        if (pearlSlot < 0) {
            CombatDebugger.log(bot, "wind-pearl-cancel", "reason=no-selectable-pearl");
            charge.remove();
            bot.getBotInventory().restoreSelectedSlotOrBestWeapon(previousSlot);
            return false;
        }

        final int selectedPearlSlot = pearlSlot;
        boolean started = bot.getActionController().start(bot, BotActionState.USING_PEARL,
                PEARL_COMBO_RELEASE_TICKS, selectedPearlSlot, "wind-pearl-combo", () -> {
                    releasePearlAtCharge(bot, target, charge, selectedPearlSlot, previousSlot, distance);
                });
        if (!started) {
            CombatDebugger.log(bot, "wind-pearl-cancel",
                    "reason=action-busy active=" + bot.getActionController().state());
            if (charge.isValid()) charge.remove();
            bot.getBotInventory().restoreSelectedSlotOrBestWeapon(previousSlot);
            return false;
        }

        bot.faceLocation(target.getLocation());
        bot.punch();
        windSpawn.getWorld().playSound(windSpawn, Sound.ENTITY_WIND_CHARGE_THROW, 1f, 1.05f);
        bot.getBotCooldowns().set(PEARL_COMBO_COOLDOWN_KEY, PEARL_COMBO_COOLDOWN, alive);
        bot.getBotCooldowns().set(COOLDOWN_KEY, COOLDOWN, alive);
        bot.getBotCooldowns().set(EnderPearlBehavior.COOLDOWN_KEY, PEARL_COMBO_PEARL_COOLDOWN, alive);
        bot.getCombatState().markExecuted(alive);
        CombatDebugger.log(bot, "wind-pearl-start",
                "dist=" + String.format("%.2f", distance)
                        + " windSlot=" + windSlot
                        + " pearlSlot=" + selectedPearlSlot);
        return true;
    }

    private static Vector comboPoint(Bot bot, LivingEntity target) {
        Vector fromTargetToBot = bot.getLocation().toVector().subtract(target.getEyeLocation().toVector()).setY(0);
        if (fromTargetToBot.lengthSquared() > 1.0e-6) {
            fromTargetToBot.normalize().multiply(1.25);
        }
        return target.getEyeLocation().toVector().add(fromTargetToBot).add(new Vector(0, -0.25, 0));
    }

    private static WindCharge spawnComboCharge(Bot bot, Location spawn, Vector aim) {
        return spawn.getWorld().spawn(spawn, WindCharge.class, w -> {
            w.setShooter(bot.getBukkitEntity());
            w.setVelocity(aim.multiply(PEARL_COMBO_WIND_SPEED));
        });
    }

    private static void releasePearlAtCharge(
            Bot bot,
            LivingEntity target,
            WindCharge charge,
            int plannedPearlSlot,
            int restoreSlot,
            double plannedDistance
    ) {
        if (!target.isValid() || !charge.isValid()) {
            CombatDebugger.log(bot, "wind-pearl-cancel", "reason=target-or-charge-invalid slot=" + plannedPearlSlot);
            bot.getBotInventory().restoreSelectedSlotOrBestWeapon(restoreSlot);
            return;
        }

        int pearlSlot = bot.getBotInventory().selectMaterial(Material.ENDER_PEARL);
        if (pearlSlot < 0) {
            CombatDebugger.log(bot, "wind-pearl-cancel", "reason=no-pearl slot=" + plannedPearlSlot);
            bot.getBotInventory().restoreSelectedSlotOrBestWeapon(restoreSlot);
            return;
        }

        Location spawn = bot.getLocation().add(0, bot.getBukkitEntity().getEyeHeight() - 0.1, 0);
        Vector contactPoint = charge.getLocation().toVector()
                .add(charge.getVelocity().clone().multiply(PEARL_COMBO_LEAD_TICKS));
        Vector aim = contactPoint.subtract(spawn.toVector());
        if (aim.lengthSquared() < 1.0e-6) {
            CombatDebugger.log(bot, "wind-pearl-cancel", "reason=zero-pearl-aim slot=" + pearlSlot);
            bot.getBotInventory().restoreSelectedSlotOrBestWeapon(restoreSlot);
            return;
        }

        bot.faceLocation(charge.getLocation());
        bot.punch();
        bot.getActionController().recordDirectShortcut(bot, BotActionState.USING_PEARL,
                "wind-pearl-release", pearlSlot);
        EnderPearl pearl = spawn.getWorld().spawn(spawn, EnderPearl.class, p -> {
            p.setShooter(bot.getBukkitEntity());
            p.setVelocity(aim.normalize().multiply(PEARL_COMBO_PEARL_SPEED));
            p.setHasLeftShooter(true);
            p.setHasBeenShot(true);
        });

        spawn.getWorld().playSound(spawn, Sound.ENTITY_ENDER_PEARL_THROW, 1f, 1f);
        CombatDebugger.log(bot, "wind-pearl-throw",
                "dist=" + String.format("%.2f", plannedDistance)
                        + " slot=" + pearlSlot
                        + " chargeValid=" + charge.isValid());
        bot.getBotInventory().decrementMainInventorySlot(pearlSlot, 1);
        bot.getBotInventory().restoreSelectedSlotOrBestWeapon(restoreSlot);
        monitorPearlCombo(bot, pearl, charge, PEARL_COMBO_MONITOR_TICKS);
    }

    private static void monitorPearlCombo(Bot bot, EnderPearl pearl, WindCharge charge, int ticksLeft) {
        if (pearl == null || charge == null) return;
        if (!pearl.isValid() || !charge.isValid()) return;
        if (pearl.getWorld() != charge.getWorld()) return;

        double distanceSq = pearl.getLocation().distanceSquared(charge.getLocation());
        if (distanceSq <= PEARL_COMBO_CONTACT_RADIUS_SQ) {
            resolvePearlCombo(bot, pearl, charge, distanceSq);
            return;
        }

        if (ticksLeft <= 0) {
            CombatDebugger.log(bot, "wind-pearl-miss",
                    "dist=" + String.format("%.2f", Math.sqrt(distanceSq)));
            return;
        }
        bot.scheduleBotTask(() -> monitorPearlCombo(bot, pearl, charge, ticksLeft - 1), 1L);
    }

    private static void resolvePearlCombo(Bot bot, EnderPearl pearl, WindCharge charge, double distanceSq) {
        boolean apiContact = false;
        try {
            pearl.hitEntity(charge);
            apiContact = true;
        } catch (RuntimeException ex) {
            CombatDebugger.log(bot, "wind-pearl-contact-fallback", "reason=" + ex.getClass().getSimpleName());
        }
        if (charge.isValid()) {
            charge.explode();
        }
        CombatDebugger.log(bot, "wind-pearl-contact",
                "apiContact=" + apiContact + " dist=" + String.format("%.2f", Math.sqrt(distanceSq)));
    }

    private static void executePlan(Bot bot, WindChargeMovePlan plan) {
        if (!bot.getBotInventory().decrementMaterialOrOffhand(Material.WIND_CHARGE)) {
            CombatDebugger.log(bot, "wind-boost-cancel", "reason=no-wind-charge");
            return;
        }
        Location spawn = bot.getLocation().add(plan.placementOffset);
        Vector velocity = plan.velocity.clone();
        bot.punch();
        bot.getActionController().recordDirectShortcut(bot, BotActionState.USING_WIND_CHARGE,
                "direct-windcharge-boost-spawn", bot.getBotInventory().findMainInventory(Material.WIND_CHARGE));
        spawn.getWorld().spawn(spawn, WindCharge.class, w -> {
            w.setShooter(bot.getBukkitEntity());
            w.setVelocity(velocity);
        });
        spawn.getWorld().playSound(spawn, Sound.ENTITY_WIND_CHARGE_THROW, 1f, 1f);
        bot.getBotCooldowns().set(BOOST_COOLDOWN_KEY, BOOST_COOLDOWN, bot.getAliveTicks());
    }
}