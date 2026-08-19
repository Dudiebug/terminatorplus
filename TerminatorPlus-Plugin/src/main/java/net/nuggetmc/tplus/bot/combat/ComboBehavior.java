package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.TerminatorPlus;
import net.nuggetmc.tplus.api.agent.legacyagent.LegacyUtils;
import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.WindCharge;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Owns the wind-charge/ender-pearl finisher as one bounded combat action.
 *
 * <p>The old PR implementation used delayed direct spawns and then forced a
 * pearl/charge collision with API calls that are not a reliable substitute for
 * vanilla projectile physics. This implementation keeps the useful intercept
 * calculation, but lets the server resolve the projectiles normally and only
 * observes their proximity for debug telemetry.</p>
 */
public final class ComboBehavior {

    public static final String COOLDOWN_KEY = "combo";
    /** High-impact movement finisher: at most once per ten seconds per bot. */
    public static final int COOLDOWN_TICKS = 200;
    private static final int PEARL_DELAY_TICKS = 2;

    private static final int MIN_RELEASE_TICKS = 4;
    private static final int MAX_RELEASE_TICKS = 28;
    private static final int MONITOR_GRACE_TICKS = 8;
    private static final int MAX_MONITOR_TICKS = 36;
    private static final double INTERCEPT_MIN_DISTANCE = 30.0;
    private static final double INTERCEPT_MAX_DISTANCE = 42.0;
    private static final double WIND_SPEED = 1.20;
    private static final double PEARL_SPEED = 1.95;
    private static final double TARGET_LEAD_FACTOR = 0.35;
    private static final double TARGET_LEAD_MAX_TICKS = 10.0;
    private static final double INTERCEPT_MIN_TICKS = 1.0;
    private static final double INTERCEPT_MAX_TICKS = 32.0;
    private static final double PEARL_GRAVITY_COMPENSATION = 0.03;
    private static final double CONTACT_RADIUS_SQ = 1.25 * 1.25;

    public enum ComboType {
        WIND_PEARL_ENGAGE,
        WIND_PEARL_INTERCEPT
    }

    private final Map<UUID, ActiveCombo> active = new HashMap<>();

    public ComboBehavior(Plugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException(
                    "ComboBehavior requires a non-null Plugin — runTaskLater on a null plugin NPEs.");
        }
    }

    public ComboBehavior() {
        this(TerminatorPlus.getInstance());
    }

    /** PR #11's intended wind-pearl window. Ordinary pearls handle other ranges. */
    public static boolean isInterceptRange(double distance) {
        return distance >= INTERCEPT_MIN_DISTANCE && distance <= INTERCEPT_MAX_DISTANCE;
    }

    public static ComboType typeForDistance(double distance) {
        return isInterceptRange(distance)
                ? ComboType.WIND_PEARL_INTERCEPT
                : ComboType.WIND_PEARL_ENGAGE;
    }

    public boolean inProgress(Bot bot) {
        if (bot == null) return false;
        ActiveCombo run = active.get(bot.getUUID());
        if (run == null) return false;
        if (!bot.isBotAlive() || bot.getAliveTicks() > run.expiresAtTick) {
            cancel(run, "expired", true);
            return false;
        }
        return true;
    }

    /** Cancel a combo during bot removal or director cleanup. */
    public void clear(UUID botId) {
        if (botId == null) return;
        ActiveCombo run = active.get(botId);
        if (run != null) cancel(run, "cleanup", true);
    }

    /** Cancel when the director sees a changed target or world before movement runs. */
    public void tick(Bot bot, LivingEntity target) {
        if (bot == null) return;
        ActiveCombo run = active.get(bot.getUUID());
        if (run == null) return;
        if (!bot.isBotAlive() || target == null || !target.isValid()
                || !run.targetId.equals(target.getUniqueId())
                || target.getWorld() != bot.getLocation().getWorld()) {
            cancel(run, "target-invalid-or-changed", true);
        }
    }

    public boolean canCombo(Bot bot) {
        if (bot == null || inProgress(bot)) return false;
        int alive = bot.getAliveTicks();
        return bot.getBotCooldowns().ready(COOLDOWN_KEY, alive)
                && bot.getBotCooldowns().ready(WindChargeBehavior.COOLDOWN_KEY, alive)
                && bot.getBotCooldowns().ready(EnderPearlBehavior.COOLDOWN_KEY, alive)
                && !bot.getActionController().active();
    }

    public boolean start(Bot bot, LivingEntity target, ComboType type) {
        if (bot == null || target == null || type == null || !canCombo(bot)) return false;
        if (!bot.isBotAlive() || !target.isValid() || target.getWorld() != bot.getLocation().getWorld()) {
            CombatDebugger.log(bot, "wind-pearl-skip", "reason=target-invalid-or-world");
            return false;
        }
        if (!bot.isBotOnGround() || bot.getCombatState().getPhase() != CombatState.Phase.IDLE) {
            CombatDebugger.log(bot, "wind-pearl-skip", "reason=bot-not-grounded-or-idle");
            return false;
        }
        if (!bot.getBotInventory().hasWindCharge() || !bot.getBotInventory().hasEnderPearl()) {
            CombatDebugger.log(bot, "wind-pearl-skip", "reason=missing-combo-items");
            return false;
        }
        if (!hasTargetLineOfSight(bot, target)) {
            CombatDebugger.log(bot, "wind-pearl-skip", "reason=no-line-of-sight");
            return false;
        }

        Location windSpawn = bot.getLocation().clone()
                .add(0, bot.getBukkitEntity().getEyeHeight() - 0.1, 0);
        ComboPlan plan = type == ComboType.WIND_PEARL_INTERCEPT
                ? buildInterceptPlan(bot, target, windSpawn)
                : buildEngagePlan(bot, target, windSpawn);
        if (plan == null) return false;

        int previousSlot = bot.getBotInventory().getSelectedHotbarSlot();
        int windSlot = bot.getBotInventory().findMainInventory(Material.WIND_CHARGE);
        int selectedWindSlot = windSlot >= 0
                ? bot.getBotInventory().selectMainInventorySlot(windSlot)
                : previousSlot;
        if (windSlot >= 0 && selectedWindSlot < 0) {
            CombatDebugger.log(bot, "wind-pearl-skip", "reason=no-selectable-wind-charge");
            return false;
        }

        ActiveCombo run = new ActiveCombo(bot, target, type, previousSlot,
                bot.getAliveTicks() + plan.releaseTicks + plan.monitorTicks + 4, plan);
        active.put(bot.getUUID(), run);

        boolean started = bot.getActionController().start(bot, BotActionState.USING_WIND_CHARGE,
                plan.releaseTicks, selectedWindSlot, "wind-pearl-combo",
                () -> releasePearl(run));
        if (!started) {
            cancel(run, "action-busy", false);
            return false;
        }

        if (!bot.getBotInventory().decrementMaterialOrOffhand(Material.WIND_CHARGE)) {
            cancel(run, "no-wind-stack", true);
            return false;
        }

        try {
            bot.faceLocation(target.getLocation());
            bot.punch();
            run.charge = spawnCharge(bot, plan.windSpawn, plan.windAim);
            if (run.charge == null) {
                cancel(run, "wind-spawn-failed", true);
                return false;
            }
            plan.windSpawn.getWorld().playSound(plan.windSpawn,
                    Sound.ENTITY_WIND_CHARGE_THROW, 1f, 1.05f);
        } catch (RuntimeException ex) {
            CombatDebugger.log(bot, "wind-pearl-cancel", "reason=wind-spawn-exception type="
                    + ex.getClass().getSimpleName());
            cancel(run, "wind-spawn-exception", true);
            return false;
        }

        bot.getActionController().recordDirectShortcut(bot, BotActionState.USING_WIND_CHARGE,
                "wind-pearl-wind-spawn", selectedWindSlot);
        int alive = bot.getAliveTicks();
        bot.getBotCooldowns().set(COOLDOWN_KEY, COOLDOWN_TICKS, alive);
        bot.getBotCooldowns().set(WindChargeBehavior.COOLDOWN_KEY,
                WindChargeBehavior.COOLDOWN_TICKS, alive);
        // Keep the ordinary-pearl fallback from bypassing the combo throttle.
        bot.getBotCooldowns().set(EnderPearlBehavior.COOLDOWN_KEY, COOLDOWN_TICKS, alive);
        bot.getCombatState().markExecuted(alive);
        CombatDebugger.log(bot, "wind-pearl-start",
                "type=" + type.name()
                        + " dist=" + String.format("%.2f", bot.getLocation().distance(target.getLocation()))
                        + " release=" + plan.releaseTicks
                        + " monitor=" + plan.monitorTicks);
        return true;
    }

    private void releasePearl(ActiveCombo run) {
        Bot bot = run.bot;
        LivingEntity target = run.target;
        if (!isValid(run) || !target.isValid() || target.getWorld() != bot.getLocation().getWorld()) {
            cancel(run, "target-or-charge-invalid", false);
            return;
        }

        int pearlSlot = bot.getBotInventory().selectMaterial(Material.ENDER_PEARL);
        if (pearlSlot < 0) {
            cancel(run, "no-selectable-pearl", false);
            return;
        }

        Location spawn = bot.getLocation().clone()
                .add(0, bot.getBukkitEntity().getEyeHeight() - 0.1, 0);
        Vector velocity = run.type == ComboType.WIND_PEARL_INTERCEPT
                ? solvePearlInterceptVelocity(spawn, run.charge)
                : engageVelocity(spawn, target);
        if (velocity == null) {
            cancel(run, "no-pearl-trajectory", false);
            return;
        }

        EnderPearl pearl;
        try {
            bot.faceLocation(run.charge == null ? target.getLocation() : run.charge.getLocation());
            bot.punch();
            bot.getActionController().recordDirectShortcut(bot, BotActionState.USING_PEARL,
                    "wind-pearl-release", pearlSlot);
            pearl = spawn.getWorld().spawn(spawn, EnderPearl.class, p -> {
                p.setShooter(bot.getBukkitEntity());
                p.setVelocity(velocity);
            });
        } catch (RuntimeException ex) {
            CombatDebugger.log(bot, "wind-pearl-cancel", "reason=pearl-spawn-exception type="
                    + ex.getClass().getSimpleName());
            cancel(run, "pearl-spawn-exception", false);
            return;
        }

        if (!bot.getBotInventory().decrementMainInventorySlot(pearlSlot, 1)) {
            pearl.remove();
            cancel(run, "pearl-consume-failed", false);
            return;
        }

        spawn.getWorld().playSound(spawn, Sound.ENTITY_ENDER_PEARL_THROW, 1f, 1f);
        bot.getBotInventory().restoreSelectedSlotOrBestWeapon(run.previousSlot);
        bot.getBotCooldowns().set(EnderPearlBehavior.COOLDOWN_KEY,
                COOLDOWN_TICKS, bot.getAliveTicks());
        run.pearl = pearl;
        run.released = true;
        CombatDebugger.log(bot, "wind-pearl-throw",
                "type=" + run.type.name()
                        + " slot=" + pearlSlot
                        + " speed=" + String.format("%.2f", velocity.length()));
        monitor(run, run.plan.monitorTicks);
    }

    private void monitor(ActiveCombo run, int ticksLeft) {
        if (!active.containsKey(run.bot.getUUID())) return;
        if (run.pearl == null || !run.pearl.isValid() || run.charge == null || !run.charge.isValid()
                || run.pearl.getWorld() != run.charge.getWorld()) {
            finish(run, "projectile-ended");
            return;
        }

        double distanceSq = run.pearl.getLocation().distanceSquared(run.charge.getLocation());
        if (distanceSq <= CONTACT_RADIUS_SQ) {
            CombatDebugger.log(run.bot, "wind-pearl-contact-observed",
                    "dist=" + String.format("%.2f", Math.sqrt(distanceSq))
                            + " nativePhysics=true");
            finish(run, "contact-observed");
            return;
        }
        if (ticksLeft <= 0) {
            CombatDebugger.log(run.bot, "wind-pearl-miss",
                    "dist=" + String.format("%.2f", Math.sqrt(distanceSq)));
            finish(run, "monitor-expired");
            return;
        }

        run.monitorTask = run.bot.scheduleBotTask(() -> {
            run.monitorTask = null;
            monitor(run, ticksLeft - 1);
        }, 1L);
        if (run.monitorTask == null) finish(run, "bot-removed");
    }

    private boolean isValid(ActiveCombo run) {
        return active.get(run.bot.getUUID()) == run
                && run.bot.isBotAlive()
                && run.charge != null
                && run.charge.isValid();
    }

    private void finish(ActiveCombo run, String reason) {
        if (active.get(run.bot.getUUID()) != run) return;
        active.remove(run.bot.getUUID());
        cancelMonitor(run);
        run.bot.getBotInventory().restoreSelectedSlotOrBestWeapon(run.previousSlot);
        CombatDebugger.log(run.bot, "wind-pearl-end", "reason=" + reason + " released=" + run.released);
    }

    private void cancel(ActiveCombo run, String reason, boolean interruptAction) {
        if (active.get(run.bot.getUUID()) != run) return;
        active.remove(run.bot.getUUID());
        cancelMonitor(run);
        if (interruptAction) {
            run.bot.getActionController().interrupt(run.bot, "wind-pearl-" + reason);
        }
        run.bot.getBotInventory().restoreSelectedSlotOrBestWeapon(run.previousSlot);
        CombatDebugger.log(run.bot, "wind-pearl-cancel", "reason=" + reason);
    }

    private static void cancelMonitor(ActiveCombo run) {
        if (run.monitorTask != null && !run.monitorTask.isCancelled()) {
            run.monitorTask.cancel();
        }
        run.monitorTask = null;
    }

    private static ComboPlan buildEngagePlan(Bot bot, LivingEntity target, Location spawn) {
        Vector forward = horizontalTo(bot.getLocation(), target.getLocation());
        if (forward == null) return null;
        Location windSpawn = bot.getLocation().clone()
                .add(forward.clone().multiply(-0.8)).add(0, 0.4, 0);
        return new ComboPlan(windSpawn, forward.clone().multiply(-0.4).setY(-0.2),
                PEARL_DELAY_TICKS, 20);
    }

    private static ComboPlan buildInterceptPlan(Bot bot, LivingEntity target, Location windSpawn) {
        Vector roughContact = comboPoint(bot, target, 0.0);
        double roughWindTicks = roughContact.distance(windSpawn.toVector()) / WIND_SPEED;
        double targetLeadTicks = Math.min(TARGET_LEAD_MAX_TICKS,
                Math.max(0.0, roughWindTicks * TARGET_LEAD_FACTOR));
        Vector contactPoint = comboPoint(bot, target, targetLeadTicks);
        Vector windToContact = contactPoint.clone().subtract(windSpawn.toVector());
        double distance = windToContact.length();
        if (distance < 1.0e-6) return null;

        double windTravelTicks = distance / WIND_SPEED;
        double pearlTravelTicks = distance / PEARL_SPEED;
        int releaseTicks = clamp((int) Math.round(windTravelTicks - pearlTravelTicks),
                MIN_RELEASE_TICKS, MAX_RELEASE_TICKS);
        int monitorTicks = clamp((int) Math.ceil(pearlTravelTicks + MONITOR_GRACE_TICKS),
                8, MAX_MONITOR_TICKS);
        return new ComboPlan(windSpawn, windToContact.normalize().multiply(WIND_SPEED),
                releaseTicks, monitorTicks);
    }

    private static Vector engageVelocity(Location spawn, LivingEntity target) {
        Vector aimPoint = target.getEyeLocation().toVector()
                .add(target.getVelocity().clone().multiply(0.75));
        Vector velocity = aimPoint.subtract(spawn.toVector());
        if (velocity.lengthSquared() < 1.0e-6) return null;
        velocity.normalize().setY(velocity.getY() + 0.08).normalize();
        return velocity.multiply(2.2);
    }

    private static Vector solvePearlInterceptVelocity(Location spawn, WindCharge charge) {
        if (charge == null || !charge.isValid() || charge.getWorld() != spawn.getWorld()) return null;
        Vector origin = spawn.toVector();
        Vector chargePosition = charge.getLocation().toVector();
        Vector chargeVelocity = charge.getVelocity();
        double ticks = solveLinearInterceptTicks(origin, chargePosition, chargeVelocity, PEARL_SPEED);
        ticks = clamp(ticks, INTERCEPT_MIN_TICKS, INTERCEPT_MAX_TICKS);

        for (int i = 0; i < 3; i++) {
            Vector aimPoint = chargePosition.clone().add(chargeVelocity.clone().multiply(ticks));
            aimPoint.setY(aimPoint.getY() + gravityCompensation(ticks));
            ticks = clamp(aimPoint.distance(origin) / PEARL_SPEED,
                    INTERCEPT_MIN_TICKS, INTERCEPT_MAX_TICKS);
        }

        Vector aimPoint = chargePosition.clone().add(chargeVelocity.clone().multiply(ticks));
        aimPoint.setY(aimPoint.getY() + gravityCompensation(ticks));
        Vector velocity = aimPoint.subtract(origin);
        if (velocity.lengthSquared() < 1.0e-6) return null;
        return velocity.normalize().multiply(PEARL_SPEED);
    }

    static double solveLinearInterceptTicks(Vector origin, Vector targetPosition,
                                            Vector targetVelocity, double speed) {
        Vector delta = targetPosition.clone().subtract(origin);
        double a = targetVelocity.lengthSquared() - speed * speed;
        double b = 2.0 * delta.dot(targetVelocity);
        double c = delta.lengthSquared();
        if (c < 1.0e-6) return 0.0;
        if (Math.abs(a) < 1.0e-6) {
            if (Math.abs(b) < 1.0e-6) return Math.sqrt(c) / speed;
            double linear = -c / b;
            return linear > 0.0 && Double.isFinite(linear) ? linear : Math.sqrt(c) / speed;
        }

        double discriminant = b * b - 4.0 * a * c;
        if (discriminant < 0.0) return Math.sqrt(c) / speed;
        double root = Math.sqrt(discriminant);
        double t1 = (-b - root) / (2.0 * a);
        double t2 = (-b + root) / (2.0 * a);
        double best = Double.POSITIVE_INFINITY;
        if (t1 > 0.0 && Double.isFinite(t1)) best = t1;
        if (t2 > 0.0 && Double.isFinite(t2)) best = Math.min(best, t2);
        return Double.isFinite(best) ? best : Math.sqrt(c) / speed;
    }

    private static Vector comboPoint(Bot bot, LivingEntity target, double leadTicks) {
        Vector predictedEye = target.getEyeLocation().toVector()
                .add(target.getVelocity().clone().multiply(Math.max(0.0, leadTicks)));
        Vector fromTargetToBot = bot.getLocation().toVector().subtract(predictedEye).setY(0);
        if (fromTargetToBot.lengthSquared() > 1.0e-6) {
            fromTargetToBot.normalize().multiply(1.25);
        }
        return predictedEye.add(fromTargetToBot).add(new Vector(0, -0.25, 0));
    }

    private static WindCharge spawnCharge(Bot bot, Location spawn, Vector velocity) {
        return spawn.getWorld().spawn(spawn, WindCharge.class, w -> {
            w.setShooter(bot.getBukkitEntity());
            w.setVelocity(velocity.clone());
        });
    }

    private static boolean hasTargetLineOfSight(Bot bot, LivingEntity target) {
        Location eye = bot.getBukkitEntity().getEyeLocation();
        return LegacyUtils.checkFreeSpace(eye, target.getEyeLocation())
                || LegacyUtils.checkFreeSpace(eye, target.getLocation());
    }

    private static Vector horizontalTo(Location from, Location to) {
        Vector vector = to.toVector().subtract(from.toVector()).setY(0);
        if (vector.lengthSquared() < 1.0e-6) return null;
        return vector.normalize();
    }

    private static double gravityCompensation(double ticks) {
        return 0.5 * PEARL_GRAVITY_COMPENSATION * ticks * ticks;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    private record ComboPlan(Location windSpawn, Vector windAim, int releaseTicks, int monitorTicks) {}

    private static final class ActiveCombo {
        private final Bot bot;
        private final LivingEntity target;
        private final UUID targetId;
        private final ComboType type;
        private final int previousSlot;
        private final int expiresAtTick;
        private final ComboPlan plan;
        private WindCharge charge;
        private EnderPearl pearl;
        private BukkitTask monitorTask;
        private boolean released;

        private ActiveCombo(Bot bot, LivingEntity target, ComboType type, int previousSlot,
                            int expiresAtTick, ComboPlan plan) {
            this.bot = bot;
            this.target = target;
            this.targetId = target.getUniqueId();
            this.type = type;
            this.previousSlot = previousSlot;
            this.expiresAtTick = expiresAtTick;
            this.plan = plan;
        }
    }
}
