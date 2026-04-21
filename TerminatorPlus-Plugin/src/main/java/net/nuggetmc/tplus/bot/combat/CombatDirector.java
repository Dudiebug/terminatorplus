package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import net.nuggetmc.tplus.bot.loadout.BotInventory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

/**
 * Per-tick combat decision maker. Given a bot and its current target,
 * picks the best hotbar weapon and delegates the actual move to the
 * matching {@link WeaponBehavior}. Also runs passive behaviors
 * ({@link ElytraBehavior}, {@link TotemBehavior}) every tick.
 *
 * <p>One instance is shared across all bots; behaviors are stateless.
 */
public final class CombatDirector {

    private final MeleeBehavior melee = new MeleeBehavior();
    private final TridentBehavior trident = new TridentBehavior();
    private final MaceBehavior mace = new MaceBehavior();
    private final WindChargeBehavior windCharge = new WindChargeBehavior();
    private final ElytraBehavior elytra = new ElytraBehavior();
    private final EnderPearlBehavior pearl = new EnderPearlBehavior();
    private final TotemBehavior totem = new TotemBehavior();
    private final CrystalBehavior crystal = new CrystalBehavior();
    private final AnchorBombBehavior anchor = new AnchorBombBehavior();
    private final UtilityBehavior utility = new UtilityBehavior();
    private final ConsumableBehavior consumable = new ConsumableBehavior();
    private final ComboBehavior combo = new ComboBehavior();
    private final OpportunityScanner scanner = new OpportunityScanner();
    private final CombatSnapshot snapshot = new CombatSnapshot();

    /**
     * @return true if the director handled combat this tick and the
     *         caller should NOT fall back to its default attack code.
     */
    public boolean tick(Bot bot, LivingEntity target) {
        if (target == null || !target.isValid()) return false;
        // Bots used for AI training rely on the legacy deterministic damage table; skip.
        if (bot.hasNeuralNetwork()) return false;

        double distance = bot.getLocation().distance(target.getLocation());
        BotInventory inv = bot.getBotInventory();

        CombatDebugger.dirEntry(bot, distance, bot.getCombatState().getPhase(),
                bot.isBotOnGround(), bot.getVelocity().getY());

        // Passive behaviors run every tick regardless of weapon choice.
        elytra.tick(bot, target);
        totem.tick(bot, target);
        // Reactive consumable use (eat apples, drink potions, throw splash).
        // Totem clutch-swap runs first so a fatal hit's failsafe is never preempted.
        consumable.tick(bot, target);
        // Wind-charge self-propulsion: only fires in the 12–28 block approach zone,
        // not during combat, with a 4-tick windup and a 6s cooldown. Most calls no-op.
        windCharge.tickMovementBoost(bot, target, distance);

        // If a combo (wind+pearl) is mid-flight, let it finish — the scheduled
        // pearl task handles the second step; don't dispatch other weapons.
        if (combo.inProgress(bot)) {
            CombatDebugger.log(bot, "combo-in-progress");
            return true;
        }

        // Mid-attack commitment: stay on the chosen weapon.
        if (bot.getCombatState().getPhase() == CombatState.Phase.AIRBORNE && inv.hasMace()) {
            CombatDebugger.weaponPick(bot, "MACE(airborne-commit)", distance, true);
            selectType(inv, Material.MACE);
            mace.ticksFor(bot, target, distance);
            return true;
        }
        // Aerial dive: if falling freely with a mace, steer toward any target nearby below.
        if (bot.getCombatState().getPhase() == CombatState.Phase.IDLE
                && !bot.isBotOnGround() && bot.getVelocity().getY() < -0.3
                && inv.hasMace()) {
            Location botLoc    = bot.getLocation();
            Location targetLoc = target.getLocation();
            double dx = targetLoc.getX() - botLoc.getX();
            double dz = targetLoc.getZ() - botLoc.getZ();
            if (dx * dx + dz * dz <= 100.0 && targetLoc.getY() <= botLoc.getY() + 2.0) {
                CombatDebugger.macePhase(bot, CombatState.Phase.IDLE, CombatState.Phase.AIRBORNE);
                bot.getCombatState().setPhase(CombatState.Phase.AIRBORNE);
                CombatDebugger.weaponPick(bot, "MACE(aerial-dive)", distance, true);
                selectType(inv, Material.MACE);
                mace.ticksFor(bot, target, distance);
                return true;
            }
        }
        if (bot.getCombatState().getPhase() == CombatState.Phase.CHARGING && inv.hasTrident()) {
            CombatDebugger.weaponPick(bot, "TRIDENT(charging)", distance, true);
            selectType(inv, Material.TRIDENT);
            trident.ticksFor(bot, target, distance);
            return true;
        }

        // Read the battlefield once, reuse across the scanner and the pipeline.
        snapshot.update(bot, target);

        // Opportunity scanner first: every wiki-tier PvP play (crystals, cobwebs,
        // tipped arrows, interrupts, splash potions, combos) is evaluated here in
        // priority order. If anything fires, we're done this tick.
        if (scanner.scan(bot, target, snapshot, combo)) {
            return true;
        }

        // --- Standard priority pipeline (highest first) --------------------
        //  1. Crystal PvP at short range (huge burst)
        //  2. Anchor bomb in Nether at short range
        //  3. Mace smash (ground + mace + CD ready)
        //  4. Sword melee
        //  5. Trident momentum throw (mid range)
        //  6. Ender pearl gap-close (long range)
        //  7. Cobweb utility (target fleeing)
        //
        // Wind charges are intentionally absent from the standalone pipeline —
        // they are driven exclusively by OpportunityScanner (knockup-crystal,
        // interrupts, aerial strike) and ComboBehavior (engage/escape launches).
        // This stops the old "wind charge at any distance >= 4" spam behaviour.

        World.Environment env = bot.getDimension();

        if (env != World.Environment.NETHER && inv.hasCrystalKit()
                && distance <= 6.0
                && bot.getBotCooldowns().ready(CrystalBehavior.COOLDOWN_KEY, bot.getAliveTicks())) {
            CombatDebugger.weaponPick(bot, "END_CRYSTAL", distance, true);
            selectType(inv, Material.END_CRYSTAL);
            crystal.ticksFor(bot, target, distance);
            return true;
        }

        if (env == World.Environment.NETHER && inv.hasAnchorKit()
                && distance <= 5.0
                && bot.getBotCooldowns().ready(AnchorBombBehavior.COOLDOWN_KEY, bot.getAliveTicks())) {
            CombatDebugger.weaponPick(bot, "RESPAWN_ANCHOR", distance, true);
            selectType(inv, Material.RESPAWN_ANCHOR);
            anchor.ticksFor(bot, target, distance);
            return true;
        }

        boolean grounded = bot.isBotOnGround();

        // Short-range ground engagement. Sword / axe take priority: a normal
        // mace swing is 0.6 attack-speed / 3.6 DPS, worse than any sword.
        // The mace is only correct here when the bot has no real melee weapon
        // and can commit to a jump-smash (density + fall damage dominate).
        // An opportunistic aerial smash from altitude is still handled by the
        // aerial-dive branch above; this block governs ground combat only.
        if (distance <= 3.5) {
            int sword = inv.findSword();
            int axe = inv.findAxe();
            boolean hasRealMelee = sword >= 0 || axe >= 0;

            boolean maceSmashReady = inv.hasMace() && grounded && !hasRealMelee
                    && bot.getBotCooldowns().ready(MaceBehavior.COOLDOWN_KEY, bot.getAliveTicks());

            if (maceSmashReady) {
                CombatDebugger.weaponPick(bot, "MACE(smash-fallback)", distance, true);
                selectType(inv, Material.MACE);
                mace.ticksFor(bot, target, distance);
                return true;
            }

            int slot = sword >= 0 ? sword : axe;
            if (slot >= 0) {
                bot.selectHotbarSlot(slot);
                CombatDebugger.weaponPick(bot, sword >= 0 ? "SWORD" : "AXE", distance, true);
            } else {
                CombatDebugger.weaponPick(bot, "MELEE(empty)", distance, true);
            }
            melee.ticksFor(bot, target, distance);
            return true;
        }

        if (distance >= 5.0 && distance <= 28.0 && inv.hasTrident()
                && bot.getBotCooldowns().ready(TridentBehavior.COOLDOWN_KEY, bot.getAliveTicks())) {
            CombatDebugger.weaponPick(bot, "TRIDENT", distance, true);
            selectType(inv, Material.TRIDENT);
            trident.ticksFor(bot, target, distance);
            return true;
        }

        if (distance >= 28.0 && distance <= 64.0 && inv.hasEnderPearl()
                && bot.getBotCooldowns().ready(EnderPearlBehavior.COOLDOWN_KEY, bot.getAliveTicks())) {
            CombatDebugger.weaponPick(bot, "ENDER_PEARL", distance, true);
            pearl.ticksFor(bot, target, distance);
            return true;
        }

        if (inv.hasCobweb() && distance <= 4.5
                && bot.getBotCooldowns().ready(UtilityBehavior.COOLDOWN_KEY, bot.getAliveTicks())) {
            if (utility.ticksFor(bot, target, distance) > 0) {
                CombatDebugger.weaponPick(bot, "COBWEB", distance, true);
                return true;
            }
        }

        // Healing — subsumed by ConsumableBehavior.tick() which runs every tick
        // as a passive behavior (see above). Kept as an intentional no-op so the
        // fall-through to `return false` is obvious.

        CombatDebugger.dirNoop(bot, distance, "no-branch-matched");
        return false;
    }

    private void selectType(BotInventory inv, Material type) {
        int slot = inv.findHotbar(type);
        if (slot >= 0) inv.getBot().selectHotbarSlot(slot);
    }
}
