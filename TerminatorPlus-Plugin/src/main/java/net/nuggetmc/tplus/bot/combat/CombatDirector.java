package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import net.nuggetmc.tplus.bot.loadout.BotInventory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

/**
 * Per-tick combat decision maker. Given a bot and its current target, picks the
 * best weapon and delegates the actual move to the matching behavior.
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

    public boolean tick(Bot bot, LivingEntity target) {
        if (target == null || !target.isValid()) return false;
        if (bot.hasNeuralNetwork()) return false;

        double distance = bot.getLocation().distance(target.getLocation());
        BotInventory inv = bot.getBotInventory();
        int alive = bot.getAliveTicks();
        // Tracked for dir-noop telemetry so post-fight log inspection
        // can tell *which* branch almost matched before we fell through.
        String lastBranch = "none";

        CombatDebugger.dirEntry(bot, distance, bot.getCombatState().getPhase(),
                bot.isBotOnGround(), bot.getVelocity().getY());
        CombatDebugger.inventorySnapshot(bot);
        if (CombatDebugger.isOn(bot)) {
            CombatDebugger.log(bot, "dir-ready",
                    "kit[mace=" + inv.hasMace()
                            + " trident=" + inv.hasTrident()
                            + " pearl=" + inv.hasEnderPearl()
                            + " wind=" + inv.hasWindCharge()
                            + " crystal=" + inv.hasCrystalKit()
                            + " anchor=" + inv.hasAnchorKit()
                            + " cobweb=" + inv.hasCobweb()
                            + "] cd[mace=" + bot.getBotCooldowns().ready(MaceBehavior.COOLDOWN_KEY, alive)
                            + " trident=" + bot.getBotCooldowns().ready(TridentBehavior.COOLDOWN_KEY, alive)
                            + " pearl=" + bot.getBotCooldowns().ready(EnderPearlBehavior.COOLDOWN_KEY, alive)
                            + " wind=" + bot.getBotCooldowns().ready(WindChargeBehavior.COOLDOWN_KEY, alive)
                            + " crystal=" + bot.getBotCooldowns().ready(CrystalBehavior.COOLDOWN_KEY, alive)
                            + " anchor=" + bot.getBotCooldowns().ready(AnchorBombBehavior.COOLDOWN_KEY, alive)
                            + " cobweb=" + bot.getBotCooldowns().ready(UtilityBehavior.COOLDOWN_KEY, alive)
                            + "]");
        }

        elytra.tick(bot, target);
        totem.tick(bot, target);
        consumable.tick(bot, target);
        windCharge.tickMovementBoost(bot, target, distance);

        if (combo.inProgress(bot)) {
            CombatDebugger.log(bot, "combo-in-progress");
            return true;
        }

        lastBranch = "mace-charging";
        if (bot.getCombatState().getPhase() == CombatState.Phase.MACE_CHARGING && inv.hasMace()) {
            CombatDebugger.weaponPick(bot, "MACE(charging)", distance, true);
            selectType(inv, Material.MACE);
            mace.ticksFor(bot, target, distance);
            return true;
        }

        lastBranch = "airborne-mace";
        if (bot.getCombatState().getPhase() == CombatState.Phase.AIRBORNE && inv.hasMace()) {
            CombatDebugger.weaponPick(bot, "MACE(airborne-commit)", distance, true);
            selectType(inv, Material.MACE);
            mace.ticksFor(bot, target, distance);
            return true;
        }

        lastBranch = "aerial-dive";
        if (bot.getCombatState().getPhase() == CombatState.Phase.IDLE
                && !bot.isBotOnGround() && bot.getVelocity().getY() < -0.3
                && inv.hasMace()) {
            Location botLoc = bot.getLocation();
            Location targetLoc = target.getLocation();
            double dx = targetLoc.getX() - botLoc.getX();
            double dz = targetLoc.getZ() - botLoc.getZ();
            if (dx * dx + dz * dz <= 100.0 && targetLoc.getY() <= botLoc.getY() + 2.0) {
                CombatDebugger.macePhase(bot, CombatState.Phase.IDLE, CombatState.Phase.AIRBORNE);
                bot.getCombatState().setPhase(CombatState.Phase.AIRBORNE);
                bot.getCombatState().setPhaseStartY(botLoc.getY());
                CombatDebugger.weaponPick(bot, "MACE(aerial-dive)", distance, true);
                selectType(inv, Material.MACE);
                mace.ticksFor(bot, target, distance);
                return true;
            }
        }

        lastBranch = "charging-trident";
        if (bot.getCombatState().getPhase() == CombatState.Phase.CHARGING && inv.hasTrident()) {
            CombatDebugger.weaponPick(bot, "TRIDENT(charging)", distance, true);
            selectType(inv, Material.TRIDENT);
            trident.ticksFor(bot, target, distance);
            return true;
        }

        snapshot.update(bot, target);
        if (CombatDebugger.isOn(bot)) {
            CombatDebugger.log(bot, "snapshot",
                    "botHp=" + String.format("%.2f", snapshot.botHpFraction)
                            + " tgtHp=" + String.format("%.2f", snapshot.targetHpFraction)
                            + " targetAir=" + snapshot.targetAirborne
                            + " targetBlocking=" + snapshot.targetBlocking
                            + " targetEating=" + snapshot.targetEating
                            + " openSky=" + snapshot.openSkyAboveBot
                            + " targetAway=" + snapshot.targetSprintingAway
                            + " targetBow=" + snapshot.targetDrawingBow
                            + " targetPotion=" + snapshot.targetDrinkingPotion
                            + " targetCobweb=" + snapshot.targetInCobweb
                            + " botOnFire=" + snapshot.botOnFire);
        }

        lastBranch = "scanner";
        if (scanner.scan(bot, target, snapshot, combo)) {
            CombatDebugger.log(bot, "scanner-hit");
            return true;
        }
        if (CombatDebugger.isOn(bot)) {
            CombatDebugger.log(bot, "scanner-miss",
                    "dist=" + String.format("%.2f", distance)
                            + " targetAir=" + snapshot.targetAirborne
                            + " targetBlocking=" + snapshot.targetBlocking
                            + " targetNearWall=" + snapshot.targetNearWall
                            + " targetAway=" + snapshot.targetSprintingAway);
        }

        int shieldAxe = inv.findAxe();
        if (snapshot.targetBlocking && shieldAxe >= 0 && distance <= MeleeBehavior.ATTACK_RANGE) {
            inv.selectMainInventorySlot(shieldAxe);
            CombatDebugger.weaponPick(bot, "AXE(shield)", distance, true);
            if (!BotCombatTiming.shouldPlanNormalMelee(bot, target)) {
                CombatDebugger.log(bot, "opp-skip",
                        "name=DIRECTOR_SHIELD_MELEE reason=charge charge="
                                + String.format("%.3f", BotCombatTiming.charge(bot)));
                return false;
            }
            melee.ticksFor(bot, target, distance);
            return true;
        }

        World.Environment env = bot.getDimension();
        lastBranch = "crystal";
        if (env != World.Environment.NETHER && inv.hasCrystalKit()
                && distance >= CrystalBehavior.MIN_DISTANCE && distance <= CrystalBehavior.MAX_DISTANCE
                && bot.getBotCooldowns().ready(CrystalBehavior.COOLDOWN_KEY, alive)) {
            CombatDebugger.weaponPick(bot, "END_CRYSTAL", distance, true);
            selectType(inv, Material.END_CRYSTAL);
            crystal.ticksFor(bot, target, distance);
            return true;
        }

        lastBranch = "anchor";
        if (env != World.Environment.NETHER && inv.hasAnchorKit()
                && distance >= AnchorBombBehavior.MIN_DISTANCE && distance <= AnchorBombBehavior.MAX_DISTANCE
                && bot.getBotCooldowns().ready(AnchorBombBehavior.COOLDOWN_KEY, alive)) {
            CombatDebugger.weaponPick(bot, "RESPAWN_ANCHOR", distance, true);
            selectType(inv, Material.RESPAWN_ANCHOR);
            anchor.ticksFor(bot, target, distance);
            return true;
        }

        boolean grounded = bot.isBotOnGround();
        int sword = inv.findSword();
        int axe = inv.findAxe();
        int tridentMelee = inv.findHotbar(Material.TRIDENT);
        boolean onlyTridentMelee = sword < 0 && axe < 0 && tridentMelee >= 0
                && distance <= TridentBehavior.MELEE_FALLBACK_DISTANCE;

        lastBranch = "melee";
        if (distance <= 3.5 || onlyTridentMelee) {
            if (snapshot.targetBlocking && axe >= 0) {
                inv.selectMainInventorySlot(axe);
                CombatDebugger.weaponPick(bot, "AXE(shield)", distance, true);
                if (!BotCombatTiming.shouldPlanNormalMelee(bot, target)) {
                    CombatDebugger.log(bot, "opp-skip",
                            "name=DIRECTOR_MELEE_SHIELD reason=charge charge="
                                    + String.format("%.3f", BotCombatTiming.charge(bot)));
                    return false;
                }
                melee.ticksFor(bot, target, distance);
                return true;
            }

            boolean hasSwordOrAxe = sword >= 0 || axe >= 0;
            boolean maceSmashReady = inv.hasMace() && !hasSwordOrAxe && !snapshot.targetBlocking
                    && snapshot.openSkyAboveBot && inv.hasWindCharge() && grounded
                    && bot.getBotCooldowns().ready(MaceBehavior.COOLDOWN_KEY, alive)
                    && bot.getBotCooldowns().ready(WindChargeBehavior.COOLDOWN_KEY, alive);

            if (maceSmashReady) {
                CombatDebugger.weaponPick(bot, "MACE(smash)", distance, true);
                selectType(inv, Material.MACE);
                mace.ticksFor(bot, target, distance);
                return true;
            }

            int slot = sword >= 0 ? sword : axe;
            String pickLabel = sword >= 0 ? "SWORD" : axe >= 0 ? "AXE" : null;
            if (slot < 0 && tridentMelee >= 0) {
                slot = tridentMelee;
                pickLabel = "TRIDENT(melee)";
            }
            if (slot >= 0) {
                inv.selectMainInventorySlot(slot);
                CombatDebugger.weaponPick(bot, pickLabel, distance, true);
            } else {
                CombatDebugger.weaponPick(bot, "MELEE(empty)", distance, true);
            }
            if (!BotCombatTiming.shouldPlanNormalMelee(bot, target)) {
                CombatDebugger.log(bot, "opp-skip",
                        "name=DIRECTOR_MELEE reason=charge charge="
                                + String.format("%.3f", BotCombatTiming.charge(bot)));
                return false;
            }
            melee.ticksFor(bot, target, distance);
            return true;
        }

        lastBranch = "trident";
        if (distance >= TridentBehavior.MIN_DISTANCE && distance <= TridentBehavior.MAX_DISTANCE && inv.hasTrident()
                && bot.getBotCooldowns().ready(TridentBehavior.COOLDOWN_KEY, alive)) {
            CombatDebugger.weaponPick(bot, "TRIDENT", distance, true);
            selectType(inv, Material.TRIDENT);
            trident.ticksFor(bot, target, distance);
            return true;
        }

        lastBranch = "pearl";
        if (distance >= 28.0 && distance <= 64.0 && inv.hasEnderPearl()
                && bot.getBotCooldowns().ready(EnderPearlBehavior.COOLDOWN_KEY, alive)) {
            CombatDebugger.weaponPick(bot, "ENDER_PEARL", distance, true);
            pearl.ticksFor(bot, target, distance);
            return true;
        }

        lastBranch = "cobweb";
        if (inv.hasCobweb() && distance <= 4.5
                && bot.getBotCooldowns().ready(UtilityBehavior.COOLDOWN_KEY, alive)) {
            if (utility.ticksFor(bot, target, distance) > 0) {
                CombatDebugger.weaponPick(bot, "COBWEB", distance, true);
                return true;
            }
        }

        CombatDebugger.dirNoop(bot, distance, "no-branch-matched", lastBranch);
        return false;
    }

    private void selectType(BotInventory inv, Material type) {
        inv.selectMaterial(type);
    }
}
