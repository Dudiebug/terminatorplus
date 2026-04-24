package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.TerminatorPlus;
import net.nuggetmc.tplus.bot.Bot;
import net.nuggetmc.tplus.bot.loadout.BotInventory;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.SplashPotion;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.entity.WindCharge;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

/**
 * Reads a {@link CombatSnapshot} and identifies the highest-value play available
 * given the bot's current inventory, cooldowns, and the battlefield state.
 *
 * <p><b>Wiki coverage.</b> Every opportunity here maps to a documented
 * technique from the Minecraft Wiki "Tutorial: PvP (Java Edition)" page.
 * Mechanical defensive techniques (sprint-reset, jump-reset, hit-select,
 * p-crit, crit-deflect, s-tap) are NOT here. They belong in MeleeBehavior
 * and DefensiveReactions because they fire on every swing or every incoming
 * hit, not as composable opportunities.
 *
 * <p><b>Kit gating.</b> Every opportunity is strictly gated on inventory.
 * A bot with a stick falls straight through to the standard pipeline.
 * A bot with a full Vanilla PvP kit sees every opportunity. The bot uses
 * whatever it has and finds the smartest play for the current state.
 *
 * <p><b>Snapshot dependencies.</b> Some opportunities require fields not
 * in the original {@link CombatSnapshot}. Add these to the snapshot's
 * update() method:
 * <pre>
 *   targetSprintingAway   target velocity points away from bot AND |xz|>0.15
 *   targetDrawingBow      target.getActiveItem().getType() == Material.BOW
 *   targetDrinkingPotion  target.getActiveItem().getType() == Material.POTION
 *   targetInWater         target.isInWater()
 *   targetInCobweb        target.getLocation().getBlock().getType() == COBWEB
 *   botInLavaArea         lava block within 2 blocks of bot
 *   botOnFire             bot.getRemainingFireTicks() > 0
 *   targetThrowingPearl   nearby EnderPearl entity with target as shooter
 * </pre>
 * Every field is populated unconditionally by {@link CombatSnapshot#update}
 * every tick — the scanner reads them directly. (Earlier versions reflected
 * into CombatSnapshot so newly-added optional fields wouldn't break the
 * scanner's build; that indirection was dropped once the snapshot stabilized.)
 *
 * <p><b>Stateless.</b> Per-bot state lives on Bot (CombatState, Cooldowns).
 * The scanner instance can be shared across all bots.
 *
 * <p><b>Opportunity catalog (37 plays):</b>
 * <pre>
 *   TIER S -- game-enders
 *     1.  CRYSTAL_TRAP        airborne enemy + crystal kit
 *     2.  KNOCKUP_CRYSTAL     ground enemy + wind + crystal (chains into #1)
 *     3.  HIT_CRYSTAL         KB-sword + crystal (no wind needed)
 *     4.  LEDGE_CRYSTAL       enemy near void + crystal (push them off)
 *     5.  STUN_SLAM           shielded enemy + axe + mace (disable then smash)
 *     6.  AERIAL_STRIKE       open sky + wind + elytra + trident
 *     7.  PEARL_FLASH_CRYSTAL pearl + crystal (i-frame teleport into burst)
 *     8.  FACE_PLACE          enemy throwing pearl + obsidian + crystal
 *
 *   TIER A -- cobweb traps and tipped arrows
 *     9.  HEAD_WEB            target near wall + cobweb (head-locks them)
 *    10.  HIT_WEB             airborne target + cobweb at landing prediction
 *    11.  FOOT_PIN            sprinting-away target + cobweb at feet
 *    12.  WEB_BUBBLE          low HP + 2 cobwebs (self-encase to heal)
 *    13.  WEB_DRAIN           target in cobweb + lava bucket (deny escape)
 *    14.  TIPPED_HARMING      crossbow/bow + harming arrows + close
 *    15.  TIPPED_SLOWNESS     bow + slowness arrows + fleeing target
 *    16.  TIPPED_WEAKNESS     bow + weakness arrows + outgear scenario
 *    17.  TIPPED_SLOW_FALL    bow + slow-fall arrows + airborne target
 *    18.  TIPPED_POISON       bow + poison arrows + extended fight
 *    19.  CROSSBOW_PIERCE     piercing crossbow + shielded target
 *
 *   TIER B -- interrupts, splash potions, self-buffs
 *    20.  INTERRUPT_EAT       enemy eating + wind/projectile/splash
 *    21.  INTERRUPT_BOW       enemy drawing bow + wind/projectile
 *    22.  INTERRUPT_POTION    enemy drinking potion + wind/projectile
 *    23.  SPLASH_HEAL_SELF    bot low HP + healing splash potion
 *    24.  SPLASH_HARMING      target low + harming splash + close
 *    25.  SPLASH_POISON       extended fight + poison splash
 *    26.  SPLASH_WEAKNESS     close range + weakness splash
 *    27.  SPLASH_SLOWNESS     pursuing + slowness splash
 *    28.  STRENGTH_BUFF       pre-engage + strength potion
 *    29.  SPEED_BUFF          long-range engage + speed potion
 *    30.  FIRE_RES_BUFF       lava/fire nearby + fire-res potion
 *    31.  FIREWORK_BLAST      crossbow + firework rocket
 *
 *   TIER C -- terrain plays
 *    32.  LAVA_PIN            close + lava bucket + non-water target
 *    33.  FIRE_ZONE           flint+steel/fire charge area denial
 *    34.  WATER_DOUSE_SELF    bot on fire + water bucket
 *    35.  TNT_TRAP            target in cobweb + TNT + igniter
 *
 *   TIER D -- finisher and maintenance
 *    36.  FINISHER            target HP < 15% + closing tools
 *    37.  ARMOR_REPAIR        safe distance + XP bottle + low durability
 * </pre>
 */
public final class OpportunityScanner {

    // ==================================================================
    // Cooldown keys -- one per opportunity to allow independent gating
    // ==================================================================
    private static final String CRYSTAL_TRAP_CD     = "opp_crystal_trap";
    private static final String KNOCKUP_CD          = "opp_knockup";
    private static final String HIT_CRYSTAL_CD      = "opp_hit_crystal";
    private static final String LEDGE_CRYSTAL_CD    = "opp_ledge_crystal";
    private static final String STUN_SLAM_CD        = "opp_stun_slam";
    private static final String WIND_MACE_CD        = "opp_wind_mace";
    private static final String AERIAL_STRIKE_CD    = "opp_aerial_strike";
    private static final String PEARL_FLASH_CD      = "opp_pearl_flash";
    private static final String FACE_PLACE_CD       = "opp_face_place";

    private static final String HEAD_WEB_CD         = "opp_head_web";
    private static final String HIT_WEB_CD          = "opp_hit_web";
    private static final String FOOT_PIN_CD         = "opp_foot_pin";
    private static final String WEB_BUBBLE_CD       = "opp_web_bubble";
    private static final String WEB_DRAIN_CD        = "opp_web_drain";
    private static final String TIPPED_ARROW_CD     = "opp_tipped";
    private static final String CROSSBOW_PIERCE_CD  = "opp_crossbow_pierce";

    private static final String INTERRUPT_EAT_CD    = "opp_int_eat";
    private static final String INTERRUPT_BOW_CD    = "opp_int_bow";
    private static final String INTERRUPT_POTION_CD = "opp_int_pot";
    private static final String SPLASH_HEAL_CD      = "opp_splash_heal";
    private static final String SPLASH_HARM_CD      = "opp_splash_harm";
    private static final String SPLASH_POISON_CD    = "opp_splash_poison";
    private static final String SPLASH_WEAK_CD      = "opp_splash_weak";
    private static final String SPLASH_SLOW_CD      = "opp_splash_slow";
    private static final String STRENGTH_BUFF_CD    = "opp_strength";
    private static final String SPEED_BUFF_CD       = "opp_speed";
    private static final String FIRE_RES_CD         = "opp_fireres";
    private static final String FIREWORK_BLAST_CD   = "opp_firework";

    private static final String LAVA_PIN_CD         = "opp_lava_pin";
    private static final String FIRE_ZONE_CD        = "opp_fire_zone";
    private static final String WATER_DOUSE_CD      = "opp_water_douse";
    private static final String TNT_TRAP_CD         = "opp_tnt_trap";

    private static final String SWORD_CRIT_CD       = "opp_sword_crit";
    private static final String SPRINT_RESET_CD     = "opp_sprint_reset";
    private static final String PURSUE_GAP_CD       = "opp_pursue_gap";
    private static final String ARMOR_REPAIR_CD     = "opp_armor_repair";

    // ==================================================================
    // Tunable thresholds -- pulled from wiki guidance
    // ==================================================================
    private static final double CRYSTAL_TRAP_RANGE = CrystalBehavior.MAX_DISTANCE;
    private static final double MELEE_RANGE = 3.5;
    private static final double TIPPED_ARROW_MIN = 4.0;
    private static final double TIPPED_ARROW_MAX = 30.0;
    private static final double PIERCE_RANGE_MAX = 25.0;
    private static final double FINISHER_HP_FRAC = 0.15;
    private static final double ARMOR_REPAIR_SAFE_DIST = 15.0;
    private static final double ARMOR_DURABILITY_THRESHOLD = 0.30;

    private final Plugin plugin;

    public OpportunityScanner(Plugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException(
                "OpportunityScanner requires a non-null Plugin — its schedulers would NPE otherwise.");
        }
        this.plugin = plugin;
    }

    /**
     * Use the {@link TerminatorPlus#getInstance()} singleton rather than looking
     * the plugin up by name — {@code Bukkit.getPluginManager().getPlugin("TerminatorPlus")}
     * returned {@code null} if the plugin was renamed or shaded under a different
     * key, which later NPE'd inside {@code runTaskLater}.
     */
    public OpportunityScanner() {
        this(TerminatorPlus.getInstance());
    }

    /**
     * Scan for the best available opportunity.
     *
     * @return true if an opportunity was executed (caller should return true
     *         from CombatDirector.tick). False means fall through to the
     *         standard pipeline.
     */
    public boolean scan(Bot bot, LivingEntity target, CombatSnapshot snap, ComboBehavior combo) {
        BotInventory inv = bot.getBotInventory();
        int alive = bot.getAliveTicks();
        CombatDebugger.log(bot, "opp-scan",
                "dist=" + String.format("%.2f", snap.distance)
                        + " botHp=" + String.format("%.2f", snap.botHpFraction)
                        + " targetHp=" + String.format("%.2f", snap.targetHpFraction));

        // ===============================================================
        // TIER S -- game-ending plays
        // ===============================================================

        // 1. CRYSTAL_TRAP -- target airborne, drop a crystal under them.
        //    Wiki "Hit crystal": exploit airborne enemies for max damage.
        if (snap.targetAirborne && snap.targetRising
                && snap.distance <= CRYSTAL_TRAP_RANGE
                && inv.hasCrystalKit()
                && bot.getBotCooldowns().ready(CRYSTAL_TRAP_CD, alive)) {
            if (executeCrystalTrap(bot, target)) {
                bot.getBotCooldowns().set(CRYSTAL_TRAP_CD, 40, alive);
                return true;
            }
        }

        // 2. KNOCKUP_CRYSTAL -- wind charge them up, scanner re-runs into #1.
        if (snap.botOnGround && !snap.targetAirborne
                && snap.distance <= 4.0
                && inv.hasWindCharge() && inv.hasCrystalKit()
                && bot.getBotCooldowns().ready(KNOCKUP_CD, alive)
                && bot.getBotCooldowns().ready(WindChargeBehavior.COOLDOWN_KEY, alive)) {
            if (executeKnockupCrystal(bot, target)) {
                bot.getBotCooldowns().set(KNOCKUP_CD, 60, alive);
                bot.getBotCooldowns().set(WindChargeBehavior.COOLDOWN_KEY, 55, alive);
                return true;
            }
        }

        // 3. HIT_CRYSTAL -- KB-sword sprint hit launches them, then crystal.
        //    Wiki "Hit crystal": "best to use a sword enchanted with KB1"
        //    Same outcome as #2 without spending wind charges.
        if (snap.botOnGround && !snap.targetAirborne
                && snap.distance >= 1.5 && snap.distance <= MELEE_RANGE
                && inv.hasCrystalKit() && hasKnockbackSword(inv)
                && bot.getBotCooldowns().ready(HIT_CRYSTAL_CD, alive)) {
            if (executeHitCrystal(bot, target)) {
                bot.getBotCooldowns().set(HIT_CRYSTAL_CD, 50, alive);
                return true;
            }
        }

        // 4. LEDGE_CRYSTAL -- target near a void/drop, push them off.
        //    Wiki "Ledge crystal": "Running off of a ledge ... putting down
        //    an obsidian and spamming crystals into the obsidian"
        if (snap.targetOverVoid && snap.distance <= 5.0
                && inv.hasCrystalKit()
                && bot.getBotCooldowns().ready(LEDGE_CRYSTAL_CD, alive)) {
            if (executeCrystalTrap(bot, target)) {
                bot.getBotCooldowns().set(LEDGE_CRYSTAL_CD, 50, alive);
                return true;
            }
        }

        // 5. STUN_SLAM -- shielded target + axe + mace.
        //    Wiki "Stun-slamming": axe-disable the shield, then mace smash.
        if (snap.targetBlocking && snap.distance <= MELEE_RANGE
                && inv.hasMace() && hasAxe(inv) && snap.botOnGround
                && bot.getBotCooldowns().ready(STUN_SLAM_CD, alive)
                && bot.getBotCooldowns().ready(MaceBehavior.COOLDOWN_KEY, alive)) {
            if (executeStunSlam(bot, target)) {
                bot.getBotCooldowns().set(STUN_SLAM_CD, 90, alive);
                return true;
            }
        }

        // 5b. WIND_MACE_SMASH -- mace + wind charge + open sky.
        //     The wiki "Mace PvP" core play: the bot fires a wind charge under
        //     itself and jumps on the same tick, which blasts it ~5 blocks up.
        //     It immediately swaps to the mace so the mace-cooldown ticks
        //     during the ascent; by the time the bot peaks and falls, the
        //     mace is ready for a full-fall smash. Gated on openSkyAboveBot —
        //     if there's a ceiling, the bot can't gain altitude and would
        //     faceplant its own blast, so it falls through to normal melee.
        if (snap.botOnGround && snap.openSkyAboveBot
                && inv.hasMace() && inv.hasWindCharge()
                && snap.distance <= 5.0
                && bot.getBotCooldowns().ready(WIND_MACE_CD, alive)
                && bot.getBotCooldowns().ready(MaceBehavior.COOLDOWN_KEY, alive)
                && bot.getBotCooldowns().ready(WindChargeBehavior.COOLDOWN_KEY, alive)) {
            if (executeWindMaceSmash(bot, target)) {
                // Long cooldown so this doesn't dominate every fight; mace
                // cooldown (see MaceBehavior.JUMP_COOLDOWN) gates the smash
                // window itself, but we space out the wind-launch setup.
                bot.getBotCooldowns().set(WIND_MACE_CD, 160, alive);
                bot.getBotCooldowns().set(WindChargeBehavior.COOLDOWN_KEY, 55, alive);
                return true;
            }
        }

        // 6. AERIAL_STRIKE -- open sky + wind + elytra + trident.
        if (snap.botOnGround && snap.openSkyAboveBot
                && inv.hasWindCharge() && inv.hasElytra() && inv.hasTrident()
                && snap.distance >= 8.0 && snap.distance <= 40.0
                && bot.getBotCooldowns().ready(AERIAL_STRIKE_CD, alive)
                && bot.getBotCooldowns().ready(ComboBehavior.COOLDOWN_KEY, alive)) {
            if (executeAerialStrike(bot, target, combo)) {
                bot.getBotCooldowns().set(AERIAL_STRIKE_CD, 80, alive);
                return true;
            }
        }

        // 7. PEARL_FLASH_CRYSTAL -- pearl in for i-frames, crystal at feet.
        //    Wiki "Pearl flash/abuse": "use it to instantly perform a hit
        //    crystal on your opponent"
        if (inv.hasEnderPearl() && inv.hasCrystalKit()
                && snap.distance >= 8.0 && snap.distance <= 30.0
                && bot.getBotCooldowns().ready(PEARL_FLASH_CD, alive)
                && bot.getBotCooldowns().ready(EnderPearlBehavior.COOLDOWN_KEY, alive)) {
            if (executePearlFlashCrystal(bot, target)) {
                bot.getBotCooldowns().set(PEARL_FLASH_CD, 100, alive);
                bot.getBotCooldowns().set(EnderPearlBehavior.COOLDOWN_KEY, 80, alive);
                return true;
            }
        }

        // 8. FACE_PLACE -- target threw pearl at us, crystal-shield in their path.
        //    Wiki "Face Placing": "to counter pearl flashing as crystals
        //    can catch your opponent's incoming pearl"
        if (snap.targetThrowingPearl
                && inv.hasCrystalKit() && snap.botOnGround
                && bot.getBotCooldowns().ready(FACE_PLACE_CD, alive)) {
            if (executeFacePlace(bot, target)) {
                bot.getBotCooldowns().set(FACE_PLACE_CD, 60, alive);
                return true;
            }
        }

        // ===============================================================
        // TIER A -- cobweb traps and tipped arrows
        // ===============================================================

        // 9. HEAD_WEB -- target cornered + cobweb at head height.
        //    Wiki "Head web": "place a cobweb in their head (third block up)"
        if (snap.targetNearWall && snap.distance <= 4.0
                && inv.hasCobweb()
                && bot.getBotCooldowns().ready(HEAD_WEB_CD, alive)) {
            if (executeHeadWeb(bot, target)) {
                bot.getBotCooldowns().set(HEAD_WEB_CD, 60, alive);
                return true;
            }
        }

        // 10. HIT_WEB -- airborne target + cobweb at landing prediction.
        //    Wiki "Hit web": suspend them mid-flight for a free combo.
        if (snap.targetAirborne && snap.distance <= 5.0
                && inv.hasCobweb()
                && bot.getBotCooldowns().ready(HIT_WEB_CD, alive)) {
            if (executeHitWeb(bot, target)) {
                bot.getBotCooldowns().set(HIT_WEB_CD, 30, alive);
                return true;
            }
        }

        // 11. FOOT_PIN -- target sprinting away + cobweb at feet.
        if (snap.targetSprintingAway && snap.distance <= 5.0
                && inv.hasCobweb()
                && bot.getBotCooldowns().ready(FOOT_PIN_CD, alive)) {
            if (executeFootPin(bot, target)) {
                bot.getBotCooldowns().set(FOOT_PIN_CD, 40, alive);
                return true;
            }
        }

        // 12. WEB_BUBBLE -- bot low HP + at least 2 cobwebs, encase to heal.
        //    Wiki "Bubbling/web bubbling": "place two cobwebs where you're
        //    standing to prevent the opponent from attacking with their melee
        //    weapon, allowing for the user to heal."
        if (snap.botHpFraction < 0.35f && snap.distance <= 4.0
                && countItem(inv, Material.COBWEB) >= 2
                && bot.getBotCooldowns().ready(WEB_BUBBLE_CD, alive)) {
            if (executeWebBubble(bot)) {
                bot.getBotCooldowns().set(WEB_BUBBLE_CD, 100, alive);
                return true;
            }
        }

        // 13. WEB_DRAIN -- target stuck in cobweb + lava to deny their water.
        //    Wiki "Draining": "spamming lava ... If they use water to escape,
        //    the water will be destroyed and they will be trapped"
        if (snap.targetInCobweb && snap.distance <= 5.0
                && hasItem(inv, Material.LAVA_BUCKET)
                && bot.getBotCooldowns().ready(WEB_DRAIN_CD, alive)) {
            if (executeWebDrain(bot, target)) {
                bot.getBotCooldowns().set(WEB_DRAIN_CD, 60, alive);
                return true;
            }
        }

        // 14-18. TIPPED ARROWS -- pick the best type for the situation.
        //    Wiki: tipped arrows are "one of the most powerful items in the game".
        if ((inv.hasCrossbow() || inv.hasBow()) && hasAnyTippedArrow(inv)
                && snap.distance >= TIPPED_ARROW_MIN && snap.distance <= TIPPED_ARROW_MAX
                && bot.getBotCooldowns().ready(TIPPED_ARROW_CD, alive)) {
            PotionType chosen = pickBestTippedArrow(inv, snap, target);
            if (chosen != null && executeTippedArrow(bot, target, chosen)) {
                bot.getBotCooldowns().set(TIPPED_ARROW_CD, 30, alive);
                return true;
            }
        }

        // 19. CROSSBOW_PIERCE -- piercing crossbow against shielded target.
        //    Wiki: piercing arrows "ignore shields". Hard counter.
        if (snap.targetBlocking && snap.distance >= 4.0 && snap.distance <= PIERCE_RANGE_MAX
                && hasPiercingCrossbow(inv)
                && bot.getBotCooldowns().ready(CROSSBOW_PIERCE_CD, alive)) {
            if (executePiercingCrossbow(bot, target)) {
                bot.getBotCooldowns().set(CROSSBOW_PIERCE_CD, 25, alive);
                return true;
            }
        }

        // ===============================================================
        // TIER B -- interrupts, splash potions, self-buffs
        // ===============================================================

        // 20. INTERRUPT_EAT -- target eating gapple, deny the heal.
        if (snap.targetEating && snap.distance <= 14.0
                && bot.getBotCooldowns().ready(INTERRUPT_EAT_CD, alive)) {
            if (tryInterrupt(bot, target, inv, INTERRUPT_EAT_CD, alive, 30)) return true;
        }

        // 21. INTERRUPT_BOW -- target drawing a bow at us.
        if (snap.targetDrawingBow && snap.distance <= 24.0
                && bot.getBotCooldowns().ready(INTERRUPT_BOW_CD, alive)) {
            if (tryInterrupt(bot, target, inv, INTERRUPT_BOW_CD, alive, 25)) return true;
        }

        // 22. INTERRUPT_POTION -- target drinking a potion.
        if (snap.targetDrinkingPotion && snap.distance <= 14.0
                && bot.getBotCooldowns().ready(INTERRUPT_POTION_CD, alive)) {
            if (tryInterrupt(bot, target, inv, INTERRUPT_POTION_CD, alive, 30)) return true;
        }

        // 23. SPLASH_HEAL_SELF -- bot low HP + healing splash in inventory.
        //    Wiki Pot PvP: splash potions of healing for instant regen.
        if (snap.botHpFraction < 0.4f && hasSplashHealing(inv)
                && bot.getBotCooldowns().ready(SPLASH_HEAL_CD, alive)) {
            if (executeSplashHealSelf(bot)) {
                bot.getBotCooldowns().set(SPLASH_HEAL_CD, 20, alive);
                return true;
            }
        }

        // 24. SPLASH_HARMING -- close range + harming splash + low-HP target.
        if (snap.distance <= 4.0 && snap.targetHpFraction < 0.5
                && hasSplashOf(inv, PotionType.HARMING)
                && bot.getBotCooldowns().ready(SPLASH_HARM_CD, alive)) {
            if (executeSplashAtTarget(bot, target, PotionType.HARMING)) {
                bot.getBotCooldowns().set(SPLASH_HARM_CD, 30, alive);
                return true;
            }
        }

        // 25. SPLASH_POISON -- extended fight, target healthy, poison ticks.
        //    Wiki: "Poison constantly damages your opponent through their armor"
        if (snap.distance <= 5.0 && snap.targetHpFraction > 0.5
                && !targetHasEffect(target, PotionEffectType.POISON)
                && hasSplashOf(inv, PotionType.POISON)
                && bot.getBotCooldowns().ready(SPLASH_POISON_CD, alive)) {
            if (executeSplashAtTarget(bot, target, PotionType.POISON)) {
                bot.getBotCooldowns().set(SPLASH_POISON_CD, 200, alive);
                return true;
            }
        }

        // 26. SPLASH_WEAKNESS -- close, deny their melee output.
        //    Wiki: "outright prevent an opponent from dealing any damage"
        if (snap.distance <= 5.0
                && !targetHasEffect(target, PotionEffectType.WEAKNESS)
                && hasSplashOf(inv, PotionType.WEAKNESS)
                && bot.getBotCooldowns().ready(SPLASH_WEAK_CD, alive)) {
            if (executeSplashAtTarget(bot, target, PotionType.WEAKNESS)) {
                bot.getBotCooldowns().set(SPLASH_WEAK_CD, 600, alive);
                return true;
            }
        }

        // 27. SPLASH_SLOWNESS -- pursuing fleeing target.
        if (snap.targetSprintingAway && snap.distance <= 6.0
                && !targetHasEffect(target, PotionEffectType.SLOWNESS)
                && hasSplashOf(inv, PotionType.SLOWNESS)
                && bot.getBotCooldowns().ready(SPLASH_SLOW_CD, alive)) {
            if (executeSplashAtTarget(bot, target, PotionType.SLOWNESS)) {
                bot.getBotCooldowns().set(SPLASH_SLOW_CD, 400, alive);
                return true;
            }
        }

        // 28. STRENGTH_BUFF -- pre-engage drink Strength.
        //    Wiki: "Strength II can be used to out-damage golden apples"
        PotionType strength = pickBestDrinkable(inv, PotionType.STRONG_STRENGTH, PotionType.STRENGTH);
        if (snap.distance >= 6.0 && snap.distance <= 20.0
                && !botHasEffect(bot, PotionEffectType.STRENGTH)
                && strength != null
                && bot.getBotCooldowns().ready(STRENGTH_BUFF_CD, alive)) {
            if (executeDrinkPotion(bot, strength)) {
                bot.getBotCooldowns().set(STRENGTH_BUFF_CD, 1800, alive);
                return true;
            }
        }

        // 29. SPEED_BUFF -- long range engage, drink Speed.
        PotionType speed = pickBestDrinkable(inv, PotionType.STRONG_SWIFTNESS, PotionType.SWIFTNESS);
        if (snap.distance >= 10.0
                && !botHasEffect(bot, PotionEffectType.SPEED)
                && speed != null
                && bot.getBotCooldowns().ready(SPEED_BUFF_CD, alive)) {
            if (executeDrinkPotion(bot, speed)) {
                bot.getBotCooldowns().set(SPEED_BUFF_CD, 1800, alive);
                return true;
            }
        }

        // 30. FIRE_RES_BUFF -- standing in/near lava, drink Fire Resistance.
        PotionType fireRes = pickBestDrinkable(inv, PotionType.LONG_FIRE_RESISTANCE, PotionType.FIRE_RESISTANCE);
        if ((snap.botInLavaArea || snap.botOnFire)
                && !botHasEffect(bot, PotionEffectType.FIRE_RESISTANCE)
                && fireRes != null
                && bot.getBotCooldowns().ready(FIRE_RES_CD, alive)) {
            if (executeDrinkPotion(bot, fireRes)) {
                bot.getBotCooldowns().set(FIRE_RES_CD, 600, alive);
                return true;
            }
        }

        // 31. FIREWORK_BLAST -- crossbow + firework rocket.
        //    Wiki: "an effective way to drain your opponent's armor durability"
        if (snap.distance >= 5.0 && snap.distance <= 20.0
                && inv.hasCrossbow() && inv.hasFirework()
                && bot.getBotCooldowns().ready(FIREWORK_BLAST_CD, alive)) {
            if (executeFireworkBlast(bot, target)) {
                bot.getBotCooldowns().set(FIREWORK_BLAST_CD, 35, alive);
                return true;
            }
        }

        // ===============================================================
        // TIER C -- terrain plays
        // ===============================================================

        // 32. LAVA_PIN -- close + lava bucket + non-water target.
        //    Wiki: "double click with your lava under your opponent so that
        //    you pick it back up but still ignite them"
        if (snap.distance <= MELEE_RANGE && snap.botOnGround
                && hasItem(inv, Material.LAVA_BUCKET)
                && !snap.targetInWater
                && bot.getBotCooldowns().ready(LAVA_PIN_CD, alive)) {
            if (executeLavaPin(bot, target)) {
                bot.getBotCooldowns().set(LAVA_PIN_CD, 80, alive);
                return true;
            }
        }

        // 33. FIRE_ZONE -- area denial with flint+steel/fire charge.
        if (snap.distance <= 4.5 && snap.botOnGround
                && (hasItem(inv, Material.FLINT_AND_STEEL) || hasItem(inv, Material.FIRE_CHARGE))
                && !snap.targetInWater
                && bot.getBotCooldowns().ready(FIRE_ZONE_CD, alive)) {
            if (executeFireZone(bot, target, inv)) {
                bot.getBotCooldowns().set(FIRE_ZONE_CD, 60, alive);
                return true;
            }
        }

        // 34. WATER_DOUSE_SELF -- bot on fire + water bucket.
        if (snap.botOnFire && hasItem(inv, Material.WATER_BUCKET)
                && bot.getBotCooldowns().ready(WATER_DOUSE_CD, alive)) {
            if (executeWaterDouse(bot)) {
                bot.getBotCooldowns().set(WATER_DOUSE_CD, 40, alive);
                return true;
            }
        }

        // 35. TNT_TRAP -- target stuck in cobweb + TNT + ignition source.
        if (snap.targetInCobweb && snap.distance <= 5.0
                && hasItem(inv, Material.TNT)
                && (hasItem(inv, Material.FLINT_AND_STEEL) || hasItem(inv, Material.FIRE_CHARGE))
                && bot.getBotCooldowns().ready(TNT_TRAP_CD, alive)) {
            if (executeTntTrap(bot, target)) {
                bot.getBotCooldowns().set(TNT_TRAP_CD, 100, alive);
                return true;
            }
        }

        // 35b. Sword/axe fundamentals for default kits.
        if (hasSwordOrAxe(inv) && snap.botOnGround && snap.distance <= 3.2
                && bot.getBotCooldowns().ready(SWORD_CRIT_CD, alive)
                && BotCombatTiming.chargeReady(bot)
                && !BotCombatTiming.targetHasIFrames(target)) {
            if (executeSwordCritSetup(bot, target)) {
                bot.getBotCooldowns().set(SWORD_CRIT_CD, 18, alive);
                return true;
            }
        }

        if (hasSwordOrAxe(inv) && snap.distance <= MELEE_RANGE
                && bot.getBotCooldowns().ready(SPRINT_RESET_CD, alive)
                && !BotCombatTiming.isCritWindow(bot)
                && !(snap.botOnGround && snap.distance <= 3.2 && BotCombatTiming.chargeReady(bot))) {
            if (executeSprintReset(bot, target)) {
                bot.getBotCooldowns().set(SPRINT_RESET_CD, 12, alive);
                return true;
            }
        }

        if (hasSwordOrAxe(inv) && snap.targetSprintingAway
                && snap.distance > MELEE_RANGE && snap.distance <= 8.0
                && bot.getBotCooldowns().ready(PURSUE_GAP_CD, alive)) {
            if (executePursueGapClose(bot, target)) {
                bot.getBotCooldowns().set(PURSUE_GAP_CD, 10, alive);
                return true;
            }
        }

        // ===============================================================
        // TIER D -- finisher and maintenance
        // ===============================================================

        // 36. FINISHER -- target critically low, override range checks.
        if (snap.targetHpFraction < FINISHER_HP_FRAC && snap.distance >= 5.0) {
            if (executeFinisher(bot, target, snap, combo)) return true;
        }

        // 37. ARMOR_REPAIR -- safe distance + XP bottle + low durability.
        //    Wiki "XP management": "back off way before your armor breaks"
        if (snap.distance >= ARMOR_REPAIR_SAFE_DIST && hasItem(inv, Material.EXPERIENCE_BOTTLE)
                && armorNeedsRepair(bot)
                && bot.getBotCooldowns().ready(ARMOR_REPAIR_CD, alive)) {
            if (executeArmorRepair(bot)) {
                bot.getBotCooldowns().set(ARMOR_REPAIR_CD, 10, alive);
                return true;
            }
        }

        // No opportunity matched -- fall through to standard pipeline.
        CombatDebugger.log(bot, "opp-scan", "result=none");
        return false;
    }

    // ==================================================================
    // EXECUTORS -- TIER S
    // ==================================================================

    private boolean executeCrystalTrap(Bot bot, LivingEntity target) {
        CombatDebugger.log(bot, "opp-attempt", "name=CRYSTAL_TRAP");
        World world = target.getWorld();
        Location targetLoc = target.getLocation();
        Block below = world.getBlockAt(targetLoc.getBlockX(),
                targetLoc.getBlockY() - 1, targetLoc.getBlockZ());

        boolean needsPlace = !isHostBlock(below.getType());
        if (needsPlace) {
            if (!below.getType().isAir()) return false;
            if (!hasItem(bot.getBotInventory(), Material.OBSIDIAN)) return false;
        }

        Location crystalSpawn = below.getLocation().add(0.5, 1.0, 0.5);
        if (!crystalSpawn.getBlock().getType().isAir()
                || !crystalSpawn.clone().add(0, 1, 0).getBlock().getType().isAir()) return false;
        if (!isSafeCrystalSpawn(bot, crystalSpawn) || !hasClearLine(bot, crystalSpawn)) return false;

        if (needsPlace) {
            below.setType(Material.OBSIDIAN);
            consumeOne(bot, Material.OBSIDIAN);
        }

        bot.faceLocation(crystalSpawn);
        bot.punch();

        EnderCrystal crystal = world.spawn(crystalSpawn, EnderCrystal.class,
                c -> c.setShowingBottom(false));
        crystal.remove();
        world.createExplosion(crystalSpawn, 6.0f, false, true, bot.getBukkitEntity());
        world.playSound(crystalSpawn, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

        consumeOne(bot, Material.END_CRYSTAL);
        selectMaterial(bot, Material.END_CRYSTAL);
        return true;
    }

    private boolean executeKnockupCrystal(Bot bot, LivingEntity target) {
        CombatDebugger.log(bot, "opp-attempt", "name=KNOCKUP_CRYSTAL");
        Location spawn = bot.getLocation().add(0, bot.getBukkitEntity().getEyeHeight() - 0.1, 0);
        Vector aim = target.getLocation().toVector()
                .add(new Vector(0, 0.3, 0))
                .subtract(spawn.toVector()).normalize();

        selectMaterial(bot, Material.WIND_CHARGE);
        bot.faceLocation(target.getLocation());
        bot.punch();

        spawn.getWorld().spawn(spawn, WindCharge.class, w -> {
            w.setVelocity(aim.multiply(1.6));
            w.setShooter(bot.getBukkitEntity());
        });
        spawn.getWorld().playSound(spawn, Sound.ENTITY_WIND_CHARGE_THROW, 1f, 1f);
        consumeOne(bot, Material.WIND_CHARGE);
        return true;
    }

    /**
     * Hit-crystal without wind charges: sprint-KB the target with the KB-sword
     * to launch them, then on the next tick the CRYSTAL_TRAP opportunity catches
     * them airborne. Two-tick chain handled implicitly by the scanner re-running.
     */
    private boolean executeHitCrystal(Bot bot, LivingEntity target) {
        CombatDebugger.log(bot, "opp-attempt", "name=HIT_CRYSTAL");
        int kbSwordSlot = findKnockbackSwordSlot(bot.getBotInventory());
        if (kbSwordSlot < 0) return false;
        if (selectSlot(bot, kbSwordSlot) < 0) return false;
        bot.faceLocation(target.getLocation());
        return tryMeleeAttack(bot, target);
    }

    private boolean executeStunSlam(Bot bot, LivingEntity target) {
        CombatDebugger.log(bot, "opp-attempt", "name=STUN_SLAM");
        // Step 1: hotkey to axe and disable shield via single hit.
        int axeSlot = findAxeSlot(bot.getBotInventory());
        if (axeSlot < 0) return false;
        if (selectSlot(bot, axeSlot) < 0) return false;
        bot.faceLocation(target.getLocation());
        if (!tryMeleeAttack(bot, target)) return false;

        // Step 2: hotkey to mace. CombatDirector pipeline picks up mace on
        //         the next tick and fires the standard MaceBehavior dive.
        int maceSlot = bot.getBotInventory().findHotbar(Material.MACE);
        if (maceSlot >= 0) selectSlot(bot, maceSlot);
        bot.getCombatState().setPhase(CombatState.Phase.IDLE);
        return true;
    }

    /**
     * Wind-launched mace smash — Scenario A: mace stays in main hand the
     * whole time, so the 33.3-tick mace attack-strength recharge is the
     * ONLY cooldown clock running. Single-tick sequence:
     *   1) Put the mace in hand FIRST. This is the one-and-only item
     *      switch in the play; vanilla Player resets attackStrengthTicker
     *      on every item switch, so any later swap would restart the crit
     *      recharge and the smash would land as a normal 6-dmg swing.
     *   2) Consume one wind charge from anywhere in the inventory and
     *      play the throw sound (no WindCharge entity is spawned — the
     *      physical charge can't produce upward knockback from feet-level
     *      origin because vanilla's (target-origin).normalize() factor is
     *      zero at the bot's own position).
     *   3) Directly bot.setVelocity to launch (~1.5 Y). Air time under
     *      vanilla gravity gives ~36 ticks which clears the 33-tick mace
     *      recharge window — the smash on impact is crit-eligible.
     *   4) CombatState.AIRBORNE so MaceBehavior's dive path takes over
     *      (track + steer + fire impact while still airborne so
     *      fallDistance > 0).
     */
    private boolean executeWindMaceSmash(Bot bot, LivingEntity target) {
        CombatDebugger.log(bot, "opp-attempt", "name=WIND_MACE_SMASH");
        // Step 1: mace in hand, one switch only.
        int maceSlot = bot.getBotInventory().findHotbar(Material.MACE);
        if (maceSlot < 0) return false;
        if (selectSlot(bot, maceSlot) < 0) return false;

        // Step 2: pay the wind-charge cost, no entity spawn, no slot swap.
        bot.faceLocation(target.getLocation());
        bot.punch();
        bot.getLocation().getWorld().playSound(bot.getLocation(),
                Sound.ENTITY_WIND_CHARGE_THROW, 1f, 1.1f);
        consumeOne(bot, Material.WIND_CHARGE);

        // Step 3: direct launch, horizontal bias toward the target.
        Vector toTarget = target.getLocation().toVector()
                .subtract(bot.getLocation().toVector()).setY(0);
        if (toTarget.lengthSquared() > 1.0e-6) {
            toTarget.normalize().multiply(0.25);
        } else {
            toTarget.setX(0).setZ(0);
        }
        bot.setVelocity(new Vector(toTarget.getX(), 1.5, toTarget.getZ()));

        // Step 4: let MaceBehavior's AIRBORNE path finish the play.
        bot.getCombatState().setPhase(CombatState.Phase.AIRBORNE);
        bot.getBotCooldowns().set(MaceBehavior.COOLDOWN_KEY, 55, bot.getAliveTicks());
        return true;
    }

    private boolean executeAerialStrike(Bot bot, LivingEntity target, ComboBehavior combo) {
        CombatDebugger.log(bot, "opp-attempt", "name=AERIAL_STRIKE");
        Location feet = bot.getLocation();
        Vector toTarget = target.getLocation().toVector().subtract(feet.toVector());
        toTarget.setY(0);
        if (toTarget.lengthSquared() < 0.001) return false;
        toTarget.normalize();

        Vector spawnOffset = toTarget.clone().multiply(-0.5).setY(0.3);
        Location spawnLoc = feet.clone().add(spawnOffset);
        Vector chargeVel = toTarget.clone().multiply(-0.8).setY(-1.5);

        selectMaterial(bot, Material.WIND_CHARGE);
        bot.punch();

        spawnLoc.getWorld().spawn(spawnLoc, WindCharge.class, w -> {
            w.setVelocity(chargeVel);
            w.setShooter(bot.getBukkitEntity());
        });
        spawnLoc.getWorld().playSound(feet, Sound.ENTITY_WIND_CHARGE_THROW, 1f, 1.2f);
        consumeOne(bot, Material.WIND_CHARGE);

        bot.getBotCooldowns().set(WindChargeBehavior.COOLDOWN_KEY, 55, bot.getAliveTicks());
        bot.getBotCooldowns().set(ComboBehavior.COOLDOWN_KEY, 100, bot.getAliveTicks());
        return true;
    }

    /**
     * Pearl-flash crystal: pearl in to gain i-frames, then let CRYSTAL_TRAP
     * fire next tick on the now-close target. The i-frame window protects
     * us from the explosion damage.
     */
    private boolean executePearlFlashCrystal(Bot bot, LivingEntity target) {
        CombatDebugger.log(bot, "opp-attempt", "name=PEARL_FLASH_CRYSTAL");
        int pearlSlot = bot.getBotInventory().findHotbar(Material.ENDER_PEARL);
        if (pearlSlot < 0) return false;
        pearlSlot = selectSlot(bot, pearlSlot);
        if (pearlSlot < 0) return false;
        bot.faceLocation(target.getLocation());

        Location spawn = bot.getLocation().add(0, bot.getBukkitEntity().getEyeHeight() - 0.1, 0);
        Vector aim = target.getLocation().toVector().subtract(spawn.toVector()).normalize();

        bot.punch();
        spawn.getWorld().spawn(spawn, EnderPearl.class, p -> {
            p.setShooter(bot.getBukkitEntity());
            p.setVelocity(aim.multiply(1.8));
        });
        spawn.getWorld().playSound(spawn, Sound.ENTITY_ENDER_PEARL_THROW, 1f, 1f);
        consumeOne(bot, Material.ENDER_PEARL);
        return true;
    }

    /**
     * Face place: target threw a pearl at us. Place obsidian one block in front
     * and spawn a crystal on it. Their pearl lands into the explosion or
     * touches the crystal, popping them.
     */
    private boolean executeFacePlace(Bot bot, LivingEntity target) {
        CombatDebugger.log(bot, "opp-attempt", "name=FACE_PLACE");
        Location feet = bot.getLocation();
        Vector forward = target.getLocation().toVector().subtract(feet.toVector());
        forward.setY(0);
        if (forward.lengthSquared() < 0.001) return false;
        forward.normalize();

        Block obsBlock = feet.clone().add(forward).getBlock();
        if (!obsBlock.getType().isAir()) return false;
        if (!hasItem(bot.getBotInventory(), Material.OBSIDIAN)) return false;

        Location crystalSpawn = obsBlock.getLocation().add(0.5, 1.0, 0.5);
        if (!crystalSpawn.getBlock().getType().isAir()
                || !crystalSpawn.clone().add(0, 1, 0).getBlock().getType().isAir()) return false;
        if (!isSafeCrystalSpawn(bot, crystalSpawn) || !hasClearLine(bot, crystalSpawn)) return false;

        obsBlock.setType(Material.OBSIDIAN);
        consumeOne(bot, Material.OBSIDIAN);

        World world = feet.getWorld();
        EnderCrystal crystal = world.spawn(crystalSpawn, EnderCrystal.class,
                c -> c.setShowingBottom(false));
        crystal.remove();
        world.createExplosion(crystalSpawn, 6.0f, false, true, bot.getBukkitEntity());
        world.playSound(crystalSpawn, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
        consumeOne(bot, Material.END_CRYSTAL);
        return true;
    }

    // ==================================================================
    // EXECUTORS -- TIER A (cobwebs, tipped arrows, crossbow)
    // ==================================================================

    private boolean executeHeadWeb(Bot bot, LivingEntity target) {
        CombatDebugger.log(bot, "opp-attempt", "name=HEAD_WEB");
        World world = target.getWorld();
        Location targetLoc = target.getLocation();
        Block headBlock = world.getBlockAt(targetLoc.getBlockX(),
                targetLoc.getBlockY() + 2, targetLoc.getBlockZ());
        if (!headBlock.getType().isAir()) return false;

        int slot = bot.getBotInventory().findHotbar(Material.COBWEB);
        if (slot >= 0) selectSlot(bot, slot);
        bot.faceLocation(headBlock.getLocation());
        bot.punch();

        headBlock.setType(Material.COBWEB);
        consumeOne(bot, Material.COBWEB);
        return true;
    }

    private boolean executeHitWeb(Bot bot, LivingEntity target) {
        CombatDebugger.log(bot, "opp-attempt", "name=HIT_WEB");
        Vector vel = target.getVelocity();
        Location predicted = target.getLocation().clone().add(vel.clone().multiply(5));
        predicted.setY(target.getLocation().getY() + 1);

        Block place = predicted.getBlock();
        if (!place.getType().isAir()) return false;

        int slot = bot.getBotInventory().findHotbar(Material.COBWEB);
        if (slot >= 0) selectSlot(bot, slot);
        place.setType(Material.COBWEB);
        consumeOne(bot, Material.COBWEB);
        return true;
    }

    private boolean executeFootPin(Bot bot, LivingEntity target) {
        CombatDebugger.log(bot, "opp-attempt", "name=FOOT_PIN");
        Block at = target.getLocation().getBlock();
        if (!at.getType().isAir()) {
            at = at.getRelative(0, 1, 0);
            if (!at.getType().isAir()) return false;
        }
        int slot = bot.getBotInventory().findHotbar(Material.COBWEB);
        if (slot >= 0) selectSlot(bot, slot);
        at.setType(Material.COBWEB);
        consumeOne(bot, Material.COBWEB);
        return true;
    }

    /**
     * Web bubble: place cobwebs at own feet AND eye level, then heal.
     * Wiki: "place two cobwebs where you're standing."
     */
    private boolean executeWebBubble(Bot bot) {
        CombatDebugger.log(bot, "opp-attempt", "name=WEB_BUBBLE");
        Block feet = bot.getLocation().getBlock();
        Block head = feet.getRelative(0, 1, 0);
        if (!feet.getType().isAir() && !head.getType().isAir()) return false;

        int slot = bot.getBotInventory().findHotbar(Material.COBWEB);
        if (slot >= 0) selectSlot(bot, slot);

        if (feet.getType().isAir()) {
            feet.setType(Material.COBWEB);
            consumeOne(bot, Material.COBWEB);
        }
        if (head.getType().isAir()) {
            head.setType(Material.COBWEB);
            consumeOne(bot, Material.COBWEB);
        }
        return true;
    }

    /**
     * Web drain: pour lava on top of a cobweb the target is in.
     * Per wiki, this destroys their water if they try to escape.
     */
    private boolean executeWebDrain(Bot bot, LivingEntity target) {
        CombatDebugger.log(bot, "opp-attempt", "name=WEB_DRAIN");
        Block web = target.getLocation().getBlock();
        Block above = web.getRelative(0, 1, 0);
        if (!above.getType().isAir()) return false;

        int slot = bot.getBotInventory().findHotbar(Material.LAVA_BUCKET);
        if (slot >= 0) selectSlot(bot, slot);
        bot.faceLocation(above.getLocation());
        bot.punch();

        above.setType(Material.LAVA);
        scheduleBlockClear(above, Material.AIR, 6L);
        return true;
    }

    private boolean executeTippedArrow(Bot bot, LivingEntity target, PotionType type) {
        CombatDebugger.log(bot, "opp-attempt", "name=TIPPED_ARROW type=" + type.name());
        boolean crossbow = bot.getBotInventory().hasCrossbow();
        Material weapon = crossbow ? Material.CROSSBOW : Material.BOW;
        int weaponSlot = bot.getBotInventory().findHotbar(weapon);
        if (weaponSlot < 0) return false;

        if (selectSlot(bot, weaponSlot) < 0) return false;
        bot.faceLocation(target.getLocation());

        Location spawn = bot.getLocation().add(0, bot.getBukkitEntity().getEyeHeight() - 0.1, 0);
        Vector aim = target.getEyeLocation().toVector().subtract(spawn.toVector()).normalize();

        bot.punch();
        spawn.getWorld().spawn(spawn, Arrow.class, a -> {
            a.setShooter(bot.getBukkitEntity());
            a.setVelocity(aim.multiply(crossbow ? 3.0 : 2.5));
            a.setBasePotionType(type);
            a.setCritical(true);
        });
        spawn.getWorld().playSound(spawn,
                crossbow ? Sound.ITEM_CROSSBOW_SHOOT : Sound.ENTITY_ARROW_SHOOT, 1f, 1f);

        consumeTippedArrow(bot, type);
        return true;
    }

    private boolean executePiercingCrossbow(Bot bot, LivingEntity target) {
        CombatDebugger.log(bot, "opp-attempt", "name=CROSSBOW_PIERCE");
        int slot = bot.getBotInventory().findHotbar(Material.CROSSBOW);
        if (slot < 0) return false;
        if (selectSlot(bot, slot) < 0) return false;
        bot.faceLocation(target.getLocation());

        Location spawn = bot.getLocation().add(0, bot.getBukkitEntity().getEyeHeight() - 0.1, 0);
        Vector aim = target.getEyeLocation().toVector().subtract(spawn.toVector()).normalize();

        bot.punch();
        spawn.getWorld().spawn(spawn, Arrow.class, a -> {
            a.setShooter(bot.getBukkitEntity());
            a.setVelocity(aim.multiply(3.0));
            a.setPierceLevel(4);
            a.setCritical(true);
        });
        spawn.getWorld().playSound(spawn, Sound.ITEM_CROSSBOW_SHOOT, 1f, 1f);
        consumeArrow(bot);
        return true;
    }

    // ==================================================================
    // EXECUTORS -- TIER B (interrupts, splash potions, buffs)
    // ==================================================================

    /**
     * Interrupt the target with the cheapest available denial:
     * wind charge first, arrow second, harming splash third.
     * Returns false only if the bot has no interrupt tools at all.
     */
    private boolean tryInterrupt(Bot bot, LivingEntity target, BotInventory inv,
                                 String cdKey, int alive, int cdTicks) {
        CombatDebugger.log(bot, "opp-attempt", "name=INTERRUPT cdKey=" + cdKey);
        if (inv.hasWindCharge()
                && bot.getBotCooldowns().ready(WindChargeBehavior.COOLDOWN_KEY, alive)) {
            if (executeKnockupCrystal(bot, target)) {
                CombatDebugger.log(bot, "opp-fire", "name=INTERRUPT via=wind-charge");
                bot.getBotCooldowns().set(cdKey, cdTicks, alive);
                bot.getBotCooldowns().set(WindChargeBehavior.COOLDOWN_KEY, 55, alive);
                return true;
            }
        }
        if ((inv.hasCrossbow() || inv.hasBow()) && hasArrow(inv)) {
            if (executeQuickArrow(bot, target)) {
                CombatDebugger.log(bot, "opp-fire", "name=INTERRUPT via=quick-arrow");
                bot.getBotCooldowns().set(cdKey, cdTicks, alive);
                return true;
            }
        }
        if (hasSplashOf(inv, PotionType.HARMING)) {
            if (executeSplashAtTarget(bot, target, PotionType.HARMING)) {
                CombatDebugger.log(bot, "opp-fire", "name=INTERRUPT via=splash-harm");
                bot.getBotCooldowns().set(cdKey, cdTicks, alive);
                return true;
            }
        }
        CombatDebugger.log(bot, "opp-skip", "name=INTERRUPT reason=no-executor");
        return false;
    }

    private boolean executeQuickArrow(Bot bot, LivingEntity target) {
        CombatDebugger.log(bot, "opp-attempt", "name=QUICK_ARROW");
        boolean crossbow = bot.getBotInventory().hasCrossbow();
        Material weapon = crossbow ? Material.CROSSBOW : Material.BOW;
        int slot = bot.getBotInventory().findHotbar(weapon);
        if (slot < 0) return false;
        if (selectSlot(bot, slot) < 0) return false;
        bot.faceLocation(target.getLocation());

        Location spawn = bot.getLocation().add(0, bot.getBukkitEntity().getEyeHeight() - 0.1, 0);
        Vector aim = target.getEyeLocation().toVector().subtract(spawn.toVector()).normalize();
        bot.punch();
        spawn.getWorld().spawn(spawn, Arrow.class, a -> {
            a.setShooter(bot.getBukkitEntity());
            a.setVelocity(aim.multiply(crossbow ? 3.0 : 2.5));
        });
        spawn.getWorld().playSound(spawn,
                crossbow ? Sound.ITEM_CROSSBOW_SHOOT : Sound.ENTITY_ARROW_SHOOT, 1f, 1f);
        consumeArrow(bot);
        return true;
    }

    private boolean executeSplashHealSelf(Bot bot) {
        CombatDebugger.log(bot, "opp-attempt", "name=SPLASH_HEAL_SELF");
        PotionType type = hasSplashOf(bot.getBotInventory(), PotionType.STRONG_HEALING)
                ? PotionType.STRONG_HEALING : PotionType.HEALING;
        if (!hasSplashOf(bot.getBotInventory(), type)) return false;

        Location feet = bot.getLocation();
        feet.getWorld().spawn(feet.clone().add(0, 1.5, 0), SplashPotion.class, s -> {
            s.setItem(splashItem(type));
            s.setShooter(bot.getBukkitEntity());
            s.setVelocity(new Vector(0, -0.3, 0));
        });
        feet.getWorld().playSound(feet, Sound.ENTITY_SPLASH_POTION_THROW, 1f, 1f);
        consumeOnePotion(bot, type);
        return true;
    }

    private boolean executeSplashAtTarget(Bot bot, LivingEntity target, PotionType type) {
        CombatDebugger.log(bot, "opp-attempt", "name=SPLASH_AT_TARGET type=" + type.name());
        if (!hasSplashOf(bot.getBotInventory(), type)) return false;

        Location spawn = bot.getLocation().add(0, bot.getBukkitEntity().getEyeHeight() - 0.1, 0);
        Vector aim = target.getLocation().clone().add(0, 0.5, 0).toVector()
                .subtract(spawn.toVector()).normalize();
        bot.faceLocation(target.getLocation());
        bot.punch();

        spawn.getWorld().spawn(spawn, SplashPotion.class, s -> {
            s.setItem(splashItem(type));
            s.setShooter(bot.getBukkitEntity());
            s.setVelocity(aim.multiply(0.7));
        });
        spawn.getWorld().playSound(spawn, Sound.ENTITY_SPLASH_POTION_THROW, 1f, 1f);
        consumeOnePotion(bot, type);
        return true;
    }

    private boolean executeDrinkPotion(Bot bot, PotionType type) {
        CombatDebugger.log(bot, "opp-attempt", "name=DRINK_POTION type=" + type.name());
        if (!hasDrinkable(bot.getBotInventory(), type)) return false;

        for (PotionEffect eff : potionEffects(type)) {
            bot.getBukkitEntity().addPotionEffect(eff, true);
        }
        consumeOneDrinkable(bot, type);

        Location loc = bot.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_DRINK, 1f, 1f);
        return true;
    }

    private boolean executeFireworkBlast(Bot bot, LivingEntity target) {
        CombatDebugger.log(bot, "opp-attempt", "name=FIREWORK_BLAST");
        int slot = bot.getBotInventory().findHotbar(Material.CROSSBOW);
        if (slot < 0) return false;
        if (selectSlot(bot, slot) < 0) return false;
        bot.faceLocation(target.getLocation());

        Location spawn = bot.getLocation().add(0, bot.getBukkitEntity().getEyeHeight() - 0.1, 0);
        Vector aim = target.getLocation().toVector().subtract(spawn.toVector()).normalize();

        bot.punch();
        spawn.getWorld().spawn(spawn, Firework.class, f -> {
            f.setShooter(bot.getBukkitEntity());
            f.setVelocity(aim.multiply(2.0));
            f.setShotAtAngle(true);
            f.setTicksToDetonate(15);
        });
        spawn.getWorld().playSound(spawn, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);
        consumeOne(bot, Material.FIREWORK_ROCKET);
        return true;
    }

    // ==================================================================
    // EXECUTORS -- TIER C (terrain plays)
    // ==================================================================

    /**
     * Lava pin: place lava under target then auto-pickup 4 ticks later
     * (the "double click" technique from the wiki). Target gets ignited
     * but no permanent lava block remains.
     */
    private boolean executeLavaPin(Bot bot, LivingEntity target) {
        CombatDebugger.log(bot, "opp-attempt", "name=LAVA_PIN");
        Block targetFeet = target.getLocation().getBlock();
        if (!targetFeet.getType().isAir()) return false;

        int slot = bot.getBotInventory().findHotbar(Material.LAVA_BUCKET);
        if (slot >= 0) selectSlot(bot, slot);
        bot.faceLocation(targetFeet.getLocation());
        bot.punch();

        targetFeet.setType(Material.LAVA);
        scheduleBlockClear(targetFeet, Material.AIR, 4L);
        return true;
    }

    private boolean executeFireZone(Bot bot, LivingEntity target, BotInventory inv) {
        CombatDebugger.log(bot, "opp-attempt", "name=FIRE_ZONE");
        Block at = target.getLocation().getBlock();
        if (!at.getType().isAir()) return false;

        Material igniter = hasItem(inv, Material.FLINT_AND_STEEL)
                ? Material.FLINT_AND_STEEL : Material.FIRE_CHARGE;
        int slot = inv.findHotbar(igniter);
        if (slot >= 0) selectSlot(bot, slot);

        bot.faceLocation(at.getLocation());
        bot.punch();
        at.setType(Material.FIRE);
        if (igniter == Material.FIRE_CHARGE) consumeOne(bot, Material.FIRE_CHARGE);
        return true;
    }

    /**
     * Water douse: place water at own feet, auto-pickup 10 ticks later.
     * Long enough to extinguish fire damage, short enough to keep the bucket.
     */
    private boolean executeWaterDouse(Bot bot) {
        CombatDebugger.log(bot, "opp-attempt", "name=WATER_DOUSE_SELF");
        Block feet = bot.getLocation().getBlock();
        // Feet occupied: can't place water here, let the next priority fire.
        if (!feet.getType().isAir()) return false;

        int slot = bot.getBotInventory().findHotbar(Material.WATER_BUCKET);
        if (slot >= 0) selectSlot(bot, slot);
        bot.punch();
        feet.setType(Material.WATER);
        scheduleBlockClear(feet, Material.AIR, 10L);
        return true;
    }

    private boolean executeTntTrap(Bot bot, LivingEntity target) {
        CombatDebugger.log(bot, "opp-attempt", "name=TNT_TRAP");
        World world = target.getWorld();
        Location at = target.getLocation();
        Block place = world.getBlockAt(at.getBlockX(), at.getBlockY(), at.getBlockZ());

        int tntSlot = bot.getBotInventory().findHotbar(Material.TNT);
        if (tntSlot >= 0) selectSlot(bot, tntSlot);
        bot.faceLocation(place.getLocation());
        bot.punch();

        place.setType(Material.TNT);
        consumeOne(bot, Material.TNT);

        Block above = place.getRelative(0, 1, 0);
        if (above.getType().isAir()) above.setType(Material.FIRE);
        if (hasItem(bot.getBotInventory(), Material.FIRE_CHARGE)) {
            consumeOne(bot, Material.FIRE_CHARGE);
        }
        return true;
    }

    private boolean executeSwordCritSetup(Bot bot, LivingEntity target) {
        CombatDebugger.log(bot, "opp-attempt", "name=SWORD_CRIT_SETUP");
        int slot = findSwordAxeSlot(bot.getBotInventory());
        if (slot < 0 || selectSlot(bot, slot) < 0) return false;
        bot.faceLocation(target.getLocation());
        bot.jump();
        return true;
    }

    private boolean executeSprintReset(Bot bot, LivingEntity target) {
        CombatDebugger.log(bot, "opp-attempt", "name=SPRINT_RESET");
        int slot = findSwordAxeSlot(bot.getBotInventory());
        if (slot < 0 || selectSlot(bot, slot) < 0) return false;
        bot.faceLocation(target.getLocation());
        boolean wasSprinting = bot.isSprinting();
        bot.setSprinting(true);
        boolean hit = tryMeleeAttack(bot, target);
        bot.setSprinting(wasSprinting);
        return hit;
    }

    private boolean executePursueGapClose(Bot bot, LivingEntity target) {
        CombatDebugger.log(bot, "opp-attempt", "name=PURSUE_GAP_CLOSE");
        int slot = findSwordAxeSlot(bot.getBotInventory());
        if (slot >= 0) selectSlot(bot, slot);
        Vector toTarget = target.getLocation().toVector().subtract(bot.getLocation().toVector()).setY(0);
        if (toTarget.lengthSquared() < 1.0e-6) return false;
        toTarget.normalize();
        bot.faceLocation(target.getLocation());
        if (bot.isBotOnGround()) {
            bot.jump(toTarget.multiply(0.35).setY(0.42));
        } else {
            bot.walk(toTarget.multiply(0.28));
        }
        return true;
    }

    // ==================================================================
    // EXECUTORS -- TIER D (finisher and maintenance)
    // ==================================================================

    private boolean executeFinisher(Bot bot, LivingEntity target,
                                     CombatSnapshot snap, ComboBehavior combo) {
        CombatDebugger.log(bot, "opp-attempt", "name=FINISHER");
        BotInventory inv = bot.getBotInventory();
        int alive = bot.getAliveTicks();

        if (snap.distance >= 18.0 && inv.hasWindCharge() && inv.hasEnderPearl()
                && combo.canCombo(bot)
                && bot.getBotCooldowns().ready(EnderPearlBehavior.COOLDOWN_KEY, alive)) {
            return combo.start(bot, target, ComboBehavior.ComboType.WIND_PEARL_ENGAGE);
        }

        if (snap.distance >= 8.0 && snap.distance <= 28.0 && inv.hasTrident()
                && bot.getBotCooldowns().ready(TridentBehavior.COOLDOWN_KEY, alive)) {
            int slot = inv.findHotbar(Material.TRIDENT);
            if (slot < 0 || selectSlot(bot, slot) < 0) return false;
            new TridentBehavior().ticksFor(bot, target, snap.distance);
            return true;
        }

        if (snap.distance >= 10.0 && inv.hasEnderPearl()
                && bot.getBotCooldowns().ready(EnderPearlBehavior.COOLDOWN_KEY, alive)) {
            return executeFinisherPearl(bot, target);
        }
        if (snap.distance <= MeleeBehavior.ATTACK_RANGE) {
            int slot = findSwordAxeSlot(inv);
            if (slot < 0) slot = inv.findHotbar(Material.TRIDENT);
            if (slot >= 0 && selectSlot(bot, slot) >= 0) {
                bot.faceLocation(target.getLocation());
                return tryMeleeAttack(bot, target);
            }
        }
        return false;
    }

    private boolean executeFinisherPearl(Bot bot, LivingEntity target) {
        int slot = bot.getBotInventory().findHotbar(Material.ENDER_PEARL);
        if (slot < 0) return false;
        slot = selectSlot(bot, slot);
        if (slot < 0) return false;

        Location spawn = bot.getLocation().add(0, bot.getBukkitEntity().getEyeHeight() - 0.1, 0);
        Vector aim = target.getEyeLocation().toVector().subtract(spawn.toVector()).normalize();
        bot.faceLocation(target.getLocation());
        bot.punch();
        spawn.getWorld().spawn(spawn, EnderPearl.class, p -> {
            p.setShooter(bot.getBukkitEntity());
            p.setVelocity(aim.multiply(2.0));
        });
        spawn.getWorld().playSound(spawn, Sound.ENTITY_ENDER_PEARL_THROW, 1f, 1f);
        bot.getBotInventory().decrementMainInventorySlot(slot, 1);
        bot.getBotCooldowns().set(EnderPearlBehavior.COOLDOWN_KEY, 60, bot.getAliveTicks());
        return true;
    }

    private boolean executeArmorRepair(Bot bot) {
        CombatDebugger.log(bot, "opp-attempt", "name=ARMOR_REPAIR");
        int slot = bot.getBotInventory().findHotbar(Material.EXPERIENCE_BOTTLE);
        if (slot >= 0) selectSlot(bot, slot);

        Location feet = bot.getLocation();
        feet.getWorld().spawn(feet.clone().add(0, 1.5, 0), ThrownExpBottle.class, e -> {
            e.setShooter(bot.getBukkitEntity());
            e.setVelocity(new Vector(0, 0.2, 0));
        });
        feet.getWorld().playSound(feet, Sound.ENTITY_EXPERIENCE_BOTTLE_THROW, 1f, 1f);
        consumeOne(bot, Material.EXPERIENCE_BOTTLE);
        return true;
    }

    // ==================================================================
    // INVENTORY HELPERS (kit gating primitives)
    // ==================================================================

    private static boolean isHostBlock(Material type) {
        return type == Material.OBSIDIAN || type == Material.BEDROCK
                || type == Material.CRYING_OBSIDIAN || type == Material.GLOWSTONE;
    }

    private static boolean hasItem(BotInventory inv, Material type) {
        return inv.findHotbar(type) >= 0;
    }

    private static int countItem(BotInventory inv, Material type) {
        PlayerInventory raw = inv.raw();
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack it = raw.getItem(i);
            if (it != null && it.getType() == type) count += it.getAmount();
        }
        return count;
    }

    private static boolean hasArrow(BotInventory inv) {
        return inv.findHotbar(Material.ARROW) >= 0
                || inv.findHotbar(Material.TIPPED_ARROW) >= 0
                || inv.findHotbar(Material.SPECTRAL_ARROW) >= 0;
    }

    private static void consumeArrow(Bot bot) {
        BotInventory inv = bot.getBotInventory();
        for (Material type : new Material[]{Material.ARROW, Material.SPECTRAL_ARROW, Material.TIPPED_ARROW}) {
            int slot = inv.findMainInventory(type);
            if (slot >= 0) {
                inv.decrementMainInventorySlot(slot, 1);
                return;
            }
        }
    }

    private static final Material[] AXES = {
            Material.NETHERITE_AXE, Material.DIAMOND_AXE, Material.IRON_AXE,
            Material.STONE_AXE, Material.GOLDEN_AXE, Material.WOODEN_AXE
    };

    private static boolean hasAxe(BotInventory inv) {
        for (Material m : AXES) {
            if (inv.findHotbar(m) >= 0) return true;
        }
        return false;
    }

    private static boolean hasSwordOrAxe(BotInventory inv) {
        return findSwordAxeSlot(inv) >= 0;
    }

    private static int findSwordAxeSlot(BotInventory inv) {
        int sword = inv.findSword();
        if (sword >= 0) return sword;
        return inv.findAxe();
    }

    private static int findAxeSlot(BotInventory inv) {
        for (Material m : AXES) {
            int s = inv.findHotbar(m);
            if (s >= 0) return s;
        }
        return -1;
    }

    private static boolean hasKnockbackSword(BotInventory inv) {
        return findKnockbackSwordSlot(inv) >= 0;
    }

    private static int findKnockbackSwordSlot(BotInventory inv) {
        PlayerInventory raw = inv.raw();
        // Prefer a sword carrying Knockback enchantment.
        for (int i = 0; i < 9; i++) {
            ItemStack it = raw.getItem(i);
            if (it == null) continue;
            if (!it.getType().name().endsWith("_SWORD")) continue;
            if (it.containsEnchantment(Enchantment.KNOCKBACK)) return i;
        }
        // Fallback: any sword (sprint-KB still launches the target).
        for (int i = 0; i < 9; i++) {
            ItemStack it = raw.getItem(i);
            if (it != null && it.getType().name().endsWith("_SWORD")) return i;
        }
        return -1;
    }

    private static boolean hasPiercingCrossbow(BotInventory inv) {
        PlayerInventory raw = inv.raw();
        for (int i = 0; i < 9; i++) {
            ItemStack it = raw.getItem(i);
            if (it != null && it.getType() == Material.CROSSBOW
                    && it.containsEnchantment(Enchantment.PIERCING)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnyTippedArrow(BotInventory inv) {
        PlayerInventory raw = inv.raw();
        for (int i = 0; i < 36; i++) {
            ItemStack it = raw.getItem(i);
            if (it != null && it.getType() == Material.TIPPED_ARROW) return true;
        }
        return false;
    }

    /**
     * Pick the most useful tipped arrow type for the current situation,
     * but only choose a type the bot actually carries. Returns null if no
     * tipped arrow in the inventory fits the scenario.
     */
    private static PotionType pickBestTippedArrow(BotInventory inv, CombatSnapshot snap, LivingEntity target) {
        boolean hasHarming  = findTippedArrow(inv, PotionType.HARMING) != null
                           || findTippedArrow(inv, PotionType.STRONG_HARMING) != null;
        boolean hasSlowness = findTippedArrow(inv, PotionType.SLOWNESS) != null
                           || findTippedArrow(inv, PotionType.STRONG_SLOWNESS) != null;
        boolean hasWeakness = findTippedArrow(inv, PotionType.WEAKNESS) != null;
        boolean hasSlowFall = findTippedArrow(inv, PotionType.SLOW_FALLING) != null;
        boolean hasPoison   = findTippedArrow(inv, PotionType.POISON) != null
                           || findTippedArrow(inv, PotionType.STRONG_POISON) != null;

        // Wiki: "Harming II ... most useful in combination with crossbows,
        //       and in combination with uncharged bow shots"
        if (hasHarming && snap.distance <= 12.0 && snap.targetHpFraction < 0.7) return PotionType.HARMING;
        // Wiki: "Slow falling makes it very difficult and slow for your opponent to crit"
        if (hasSlowFall && snap.targetAirborne) return PotionType.SLOW_FALLING;
        // Wiki: "Slowness ... if you are to give chase"
        if (hasSlowness && snap.targetSprintingAway) return PotionType.SLOWNESS;
        // Wiki: "weakness is great for extended trades"
        if (hasWeakness && snap.distance <= 8.0) return PotionType.WEAKNESS;
        // Wiki: "deal light chip damage"
        if (hasPoison && !targetHasEffect(target, PotionEffectType.POISON)) return PotionType.POISON;

        return null;
    }

    private static ItemStack findTippedArrow(BotInventory inv, PotionType type) {
        PlayerInventory raw = inv.raw();
        for (int i = 0; i < 36; i++) {
            ItemStack it = raw.getItem(i);
            if (it == null || it.getType() != Material.TIPPED_ARROW) continue;
            if (it.getItemMeta() instanceof PotionMeta pm && pm.getBasePotionType() == type) {
                return it;
            }
        }
        return null;
    }

    private static void consumeTippedArrow(Bot bot, PotionType type) {
        PlayerInventory raw = bot.getBotInventory().raw();
        for (int i = 0; i < 36; i++) {
            ItemStack it = raw.getItem(i);
            if (it == null || it.getType() != Material.TIPPED_ARROW) continue;
            if (it.getItemMeta() instanceof PotionMeta pm && pm.getBasePotionType() == type) {
                bot.getBotInventory().decrementMainInventorySlot(i, 1);
                return;
            }
        }
    }

    // -- Potion helpers --

    private static boolean hasSplashHealing(BotInventory inv) {
        return hasSplashOf(inv, PotionType.HEALING) || hasSplashOf(inv, PotionType.STRONG_HEALING);
    }

    private static boolean hasSplashOf(BotInventory inv, PotionType type) {
        return findSplashOf(inv, type) != null;
    }

    private static ItemStack findSplashOf(BotInventory inv, PotionType type) {
        PlayerInventory raw = inv.raw();
        for (int i = 0; i < 36; i++) {
            ItemStack it = raw.getItem(i);
            if (it == null || it.getType() != Material.SPLASH_POTION) continue;
            if (it.getItemMeta() instanceof PotionMeta pm && pm.getBasePotionType() == type) {
                return it;
            }
        }
        return null;
    }

    private static void consumeOnePotion(Bot bot, PotionType type) {
        PlayerInventory raw = bot.getBotInventory().raw();
        for (int i = 0; i < 36; i++) {
            ItemStack it = raw.getItem(i);
            if (it == null || it.getType() != Material.SPLASH_POTION) continue;
            if (it.getItemMeta() instanceof PotionMeta pm && pm.getBasePotionType() == type) {
                bot.getBotInventory().decrementMainInventorySlot(i, 1);
                return;
            }
        }
    }

    private static boolean hasDrinkable(BotInventory inv, PotionType type) {
        return findDrinkable(inv, type) != null;
    }

    private static PotionType pickBestDrinkable(BotInventory inv, PotionType... types) {
        for (PotionType type : types) {
            if (hasDrinkable(inv, type)) return type;
        }
        return null;
    }

    private static ItemStack findDrinkable(BotInventory inv, PotionType type) {
        PlayerInventory raw = inv.raw();
        for (int i = 0; i < 36; i++) {
            ItemStack it = raw.getItem(i);
            if (it == null || it.getType() != Material.POTION) continue;
            if (it.getItemMeta() instanceof PotionMeta pm && pm.getBasePotionType() == type) {
                return it;
            }
        }
        return null;
    }

    private static void consumeOneDrinkable(Bot bot, PotionType type) {
        PlayerInventory raw = bot.getBotInventory().raw();
        for (int i = 0; i < 36; i++) {
            ItemStack it = raw.getItem(i);
            if (it == null || it.getType() != Material.POTION) continue;
            if (it.getItemMeta() instanceof PotionMeta pm && pm.getBasePotionType() == type) {
                bot.getBotInventory().decrementMainInventorySlot(i, 1);
                return;
            }
        }
    }

    private static ItemStack splashItem(PotionType type) {
        ItemStack it = new ItemStack(Material.SPLASH_POTION);
        if (it.getItemMeta() instanceof PotionMeta pm) {
            pm.setBasePotionType(type);
            it.setItemMeta(pm);
        }
        return it;
    }

    /**
     * Vanilla-approximate effect ladder for self-drink potions.
     * Durations in ticks (3600t = 3min, 1800t = 1:30).
     */
    private static PotionEffect[] potionEffects(PotionType type) {
        return switch (type) {
            case STRENGTH -> new PotionEffect[]{
                    new PotionEffect(PotionEffectType.STRENGTH, 3600, 0)};
            case STRONG_STRENGTH -> new PotionEffect[]{
                    new PotionEffect(PotionEffectType.STRENGTH, 1800, 1)};
            case SWIFTNESS -> new PotionEffect[]{
                    new PotionEffect(PotionEffectType.SPEED, 3600, 0)};
            case STRONG_SWIFTNESS -> new PotionEffect[]{
                    new PotionEffect(PotionEffectType.SPEED, 1800, 1)};
            case FIRE_RESISTANCE -> new PotionEffect[]{
                    new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 3600, 0)};
            case REGENERATION -> new PotionEffect[]{
                    new PotionEffect(PotionEffectType.REGENERATION, 900, 0)};
            default -> new PotionEffect[0];
        };
    }

    private static boolean targetHasEffect(LivingEntity target, PotionEffectType type) {
        return target.hasPotionEffect(type);
    }

    private static boolean botHasEffect(Bot bot, PotionEffectType type) {
        return bot.getBukkitEntity().hasPotionEffect(type);
    }

    private static boolean isSafeCrystalSpawn(Bot bot, Location spawn) {
        return bot.getLocation().distance(spawn) >= CrystalBehavior.MIN_DISTANCE;
    }

    private static boolean hasClearLine(Bot bot, Location target) {
        Location eye = bot.getBukkitEntity().getEyeLocation();
        Vector direction = target.toVector().subtract(eye.toVector());
        double length = direction.length();
        if (length < 1.0e-6) return true;
        direction.normalize();
        return eye.getWorld().rayTraceBlocks(eye, direction, length, FluidCollisionMode.NEVER, true) == null;
    }

    // ==================================================================
    // MISC HELPERS
    // ==================================================================

    private static int selectSlot(Bot bot, int slot) {
        return bot.getBotInventory().selectMainInventorySlot(slot);
    }

    private static int selectMaterial(Bot bot, Material type) {
        return bot.getBotInventory().selectMaterial(type);
    }

    private static boolean tryMeleeAttack(Bot bot, LivingEntity target) {
        if (!BotCombatTiming.canSwing(bot, target)) return false;
        bot.attack(target);
        return true;
    }

    private static boolean armorNeedsRepair(Bot bot) {
        PlayerInventory inv = bot.getBukkitEntity().getInventory();
        ItemStack[] armor = {inv.getHelmet(), inv.getChestplate(),
                inv.getLeggings(), inv.getBoots()};
        for (ItemStack piece : armor) {
            if (piece == null || piece.getType().isAir()) continue;
            int max = piece.getType().getMaxDurability();
            if (max <= 0) continue;
            int damage = (piece.getItemMeta() instanceof Damageable d) ? d.getDamage() : 0;
            double durability = 1.0 - ((double) damage / max);
            if (durability < ARMOR_DURABILITY_THRESHOLD) return true;
        }
        return false;
    }

    private static void consumeOne(Bot bot, Material type) {
        bot.getBotInventory().decrementMaterialOrOffhand(type);
    }

    /**
     * Schedule a block to be cleared (or replaced) after a delay. Used by
     * lava pin, water douse, and web drain to simulate the "double click"
     * pickup behavior. Silently no-ops if the plugin reference is null.
     */
    private void scheduleBlockClear(Block block, Material replacement, long delayTicks) {
        if (plugin == null) return;
        Material original = block.getType();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Only clear if the block is still what we placed (avoid clobbering
            // something a player or another bot placed in the meantime).
            if (block.getType() == original) {
                block.setType(replacement);
            }
        }, delayTicks);
    }
}

