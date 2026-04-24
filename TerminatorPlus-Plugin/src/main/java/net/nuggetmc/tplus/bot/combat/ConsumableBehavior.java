package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import net.nuggetmc.tplus.bot.loadout.BotInventory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

/**
 * Reactive consumable logic: bots eat golden apples, drink potions, and throw splash
 * potions when combat situations call for it. Runs every tick as a passive behavior
 * ({@link CombatDirector} invokes {@code tick} alongside {@code elytra} and {@code totem}).
 *
 * <p>Priority order (first match wins; each has its own cooldown):
 * <ol>
 *   <li>Fire response — on fire &amp; no fire-res effect → drink fire-res potion</li>
 *   <li>Critical-HP healing (&lt; 30%) — enchanted apple preferred</li>
 *   <li>Low-HP healing (&lt; 50%) — regular apple / splash healing / drink healing</li>
 *   <li>Pre-combat strength buff — target present, no STRENGTH active → drink strength</li>
 *   <li>Offensive splash — target within 8 blocks → throw harming / poison / slowness</li>
 * </ol>
 *
 * <p>Effects apply INSTANTLY (no 32-tick vanilla eat animation) — bot combat flow can't
 * survive a 32-tick hold. Matches the pattern the old {@code CombatDirector.tryHeal()}
 * used before this class subsumed it.
 */
public final class ConsumableBehavior {

    // Cooldown keys registered on the bot's shared Cooldowns registry.
    private static final String CD_APPLE        = "consumable_apple";
    private static final String CD_DRINK_HEAL   = "consumable_drink_heal";
    private static final String CD_FIRE_RES     = "consumable_fire_res";
    private static final String CD_STRENGTH     = "consumable_strength";
    private static final String CD_SPLASH_HARM  = "consumable_splash_harm";

    // Gate thresholds.
    private static final float LOW_HP_RATIO  = 0.50f;
    private static final float CRIT_HP_RATIO = 0.30f;
    private static final int   FIRE_TICK_TRIGGER = 20;   // Must be burning for at least 1s
    private static final double SPLASH_MIN_RANGE = 2.0;  // Don't splash yourself
    private static final double SPLASH_MAX_RANGE = 8.0;  // Splash potions arc poorly beyond this

    // Cooldown values (ticks).
    private static final int CD_APPLE_TICKS       = 100;
    private static final int CD_DRINK_HEAL_TICKS  = 60;
    private static final int CD_FIRE_RES_TICKS    = 200;  // Re-check guard; the real gate is hasPotionEffect
    private static final int CD_STRENGTH_TICKS    = 200;  // Same — effect duration is the real gate
    private static final int CD_SPLASH_HARM_TICKS = 200;

    public void tick(Bot bot, LivingEntity target) {
        // Cheap pre-gate: bail if the bot has nothing to consume.
        BotInventory inv = bot.getBotInventory();
        if (!inv.hasAnyConsumable()) return;

        int aliveTicks = bot.getAliveTicks();
        Player bukkit = (Player) bot.getBukkitEntity();
        float hpRatio = bot.getBotHealth() / Math.max(1.0f, bot.getBotMaxHealth());
        CombatDebugger.log(bot, "consumable-tick",
                "hp=" + String.format("%.2f", hpRatio)
                        + " fireTicks=" + bukkit.getFireTicks()
                        + " target=" + (target != null));

        // 1. Fire response — burning and no fire-res active → drink fire-res.
        if (bukkit.getFireTicks() > FIRE_TICK_TRIGGER
                && !bukkit.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)
                && bot.getBotCooldowns().ready(CD_FIRE_RES, aliveTicks)) {
            if (tryDrinkFireRes(bot, inv, bukkit)) {
                CombatDebugger.log(bot, "consumable-use", "type=fire-res");
                bot.getBotCooldowns().set(CD_FIRE_RES, CD_FIRE_RES_TICKS, aliveTicks);
                return;
            }
        }

        // 2. Critical HP — try the strongest heal available.
        if (hpRatio < CRIT_HP_RATIO) {
            if (bot.getBotCooldowns().ready(CD_APPLE, aliveTicks)
                    && tryEatEnchantedApple(bot, inv, bukkit)) {
                CombatDebugger.log(bot, "consumable-use", "type=enchanted-gapple");
                bot.getBotCooldowns().set(CD_APPLE, CD_APPLE_TICKS, aliveTicks);
                return;
            }
        }

        // 3. Low HP — try progressively cheaper heals until one succeeds.
        if (hpRatio < LOW_HP_RATIO) {
            if (bot.getBotCooldowns().ready(CD_APPLE, aliveTicks)
                    && tryEatGoldenApple(bot, inv, bukkit)) {
                CombatDebugger.log(bot, "consumable-use", "type=gapple");
                bot.getBotCooldowns().set(CD_APPLE, CD_APPLE_TICKS, aliveTicks);
                return;
            }
            if (bot.getBotCooldowns().ready(CD_DRINK_HEAL, aliveTicks)) {
                if (trySplashSelfHeal(bot, inv, bukkit)) {
                    CombatDebugger.log(bot, "consumable-use", "type=splash-heal");
                    bot.getBotCooldowns().set(CD_DRINK_HEAL, CD_DRINK_HEAL_TICKS, aliveTicks);
                    return;
                }
                if (tryDrinkHealing(bot, inv, bukkit)) {
                    CombatDebugger.log(bot, "consumable-use", "type=drink-heal");
                    bot.getBotCooldowns().set(CD_DRINK_HEAL, CD_DRINK_HEAL_TICKS, aliveTicks);
                    return;
                }
            }
        }

        // 4. Pre-combat strength buff — target present, no STRENGTH active.
        if (target != null
                && !bukkit.hasPotionEffect(PotionEffectType.STRENGTH)
                && bot.getBotCooldowns().ready(CD_STRENGTH, aliveTicks)) {
            if (tryDrinkStrength(bot, inv, bukkit)) {
                CombatDebugger.log(bot, "consumable-use", "type=strength");
                bot.getBotCooldowns().set(CD_STRENGTH, CD_STRENGTH_TICKS, aliveTicks);
                return;
            }
        }

        // 5. Offensive splash — target in range, cooldown ready.
        if (target != null && bot.getBotCooldowns().ready(CD_SPLASH_HARM, aliveTicks)) {
            double dist = bot.getLocation().distance(target.getLocation());
            if (dist >= SPLASH_MIN_RANGE && dist <= SPLASH_MAX_RANGE) {
                if (tryThrowSplashHarming(bot, inv, target)) {
                    CombatDebugger.log(bot, "consumable-use", "type=splash-offense");
                    bot.getBotCooldowns().set(CD_SPLASH_HARM, CD_SPLASH_HARM_TICKS, aliveTicks);
                    return;
                }
            }
        }
        CombatDebugger.log(bot, "consumable-noop");
    }

    // -------------------------------------------------------------------------
    // Individual actions. Each returns true when it actually consumed an item.
    // -------------------------------------------------------------------------

    private boolean tryEatEnchantedApple(Bot bot, BotInventory inv, Player bukkit) {
        int slot = inv.findHotbar(Material.ENCHANTED_GOLDEN_APPLE);
        if (slot < 0) return false;
        int previousSlot = inv.getSelectedHotbarSlot();
        slot = inv.selectMainInventorySlot(slot);
        if (slot < 0) return false;
        float max = bot.getBotMaxHealth();
        bukkit.setHealth(Math.min(max, bot.getBotHealth() + 4.0f));
        bukkit.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 3));
        bukkit.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 400, 1));
        bukkit.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 6000, 0));
        bukkit.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 6000, 0));
        decrementSlot(inv, slot);
        inv.restoreSelectedSlotOrBestWeapon(previousSlot);
        bukkit.getWorld().playSound(bukkit.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.5f, 1.0f);
        return true;
    }

    private boolean tryEatGoldenApple(Bot bot, BotInventory inv, Player bukkit) {
        int slot = inv.findHotbar(Material.GOLDEN_APPLE);
        if (slot < 0) {
            // Fall back to enchanted if nothing better — rare but valid.
            slot = inv.findHotbar(Material.ENCHANTED_GOLDEN_APPLE);
            if (slot < 0) return false;
            return tryEatEnchantedApple(bot, inv, bukkit);
        }
        int previousSlot = inv.getSelectedHotbarSlot();
        slot = inv.selectMainInventorySlot(slot);
        if (slot < 0) return false;
        float max = bot.getBotMaxHealth();
        bukkit.setHealth(Math.min(max, bot.getBotHealth() + 4.0f));
        bukkit.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 0));
        bukkit.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
        decrementSlot(inv, slot);
        inv.restoreSelectedSlotOrBestWeapon(previousSlot);
        bukkit.getWorld().playSound(bukkit.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.5f, 1.0f);
        return true;
    }

    private boolean tryDrinkHealing(Bot bot, BotInventory inv, Player bukkit) {
        int slot = inv.findHealingPotion();
        if (slot < 0) return false;
        ItemStack stack = inv.raw().getItem(slot);
        if (stack == null) return false;
        PotionType pt = (stack.getItemMeta() instanceof PotionMeta pm) ? pm.getBasePotionType() : null;
        float heal = (pt == PotionType.STRONG_HEALING) ? 12.0f : 6.0f;
        int previousSlot = inv.getSelectedHotbarSlot();
        slot = inv.selectMainInventorySlot(slot);
        if (slot < 0) return false;
        float max = bot.getBotMaxHealth();
        bukkit.setHealth(Math.min(max, bot.getBotHealth() + heal));
        decrementSlot(inv, slot);
        inv.restoreSelectedSlotOrBestWeapon(previousSlot);
        bukkit.getWorld().playSound(bukkit.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.8f, 1.0f);
        return true;
    }

    private boolean trySplashSelfHeal(Bot bot, BotInventory inv, Player bukkit) {
        int slot = inv.findSplashHealing();
        if (slot < 0) return false;
        ItemStack stack = inv.raw().getItem(slot);
        if (stack == null) return false;
        // Throw at own feet: tiny downward velocity so it lands immediately.
        int previousSlot = inv.getSelectedHotbarSlot();
        slot = inv.selectMainInventorySlot(slot);
        if (slot < 0) return false;
        bukkit.swingMainHand();
        Location eye = bukkit.getEyeLocation();
        ItemStack splash = stack.clone();
        splash.setAmount(1);
        bukkit.getWorld().spawn(eye, ThrownPotion.class, p -> {
            p.setShooter(bukkit);
            p.setItem(splash);
            p.setVelocity(new Vector(0, -0.4, 0));
        });
        // Apply instant-heal immediately too, mirroring tryHeal's no-animation style. The thrown
        // potion still shatters visually; the double-heal is small (splash radius effect stacks
        // once on self), kept for reliability in case the entity despawns before impact.
        PotionType pt = (stack.getItemMeta() instanceof PotionMeta pm) ? pm.getBasePotionType() : null;
        float heal = (pt == PotionType.STRONG_HEALING) ? 12.0f : 6.0f;
        float max = bot.getBotMaxHealth();
        bukkit.setHealth(Math.min(max, bot.getBotHealth() + heal));
        decrementSlot(inv, slot);
        inv.restoreSelectedSlotOrBestWeapon(previousSlot);
        return true;
    }

    private boolean tryDrinkFireRes(Bot bot, BotInventory inv, Player bukkit) {
        int slot = inv.findFireResPotion();
        if (slot < 0) return false;
        ItemStack stack = inv.raw().getItem(slot);
        if (stack == null) return false;
        PotionType pt = (stack.getItemMeta() instanceof PotionMeta pm) ? pm.getBasePotionType() : null;
        int duration = (pt == PotionType.LONG_FIRE_RESISTANCE) ? 9600 : 3600;
        int previousSlot = inv.getSelectedHotbarSlot();
        slot = inv.selectMainInventorySlot(slot);
        if (slot < 0) return false;
        bukkit.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0));
        bukkit.setFireTicks(0);
        decrementSlot(inv, slot);
        inv.restoreSelectedSlotOrBestWeapon(previousSlot);
        bukkit.getWorld().playSound(bukkit.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.8f, 1.0f);
        return true;
    }

    private boolean tryDrinkStrength(Bot bot, BotInventory inv, Player bukkit) {
        int slot = inv.findStrengthPotion();
        if (slot < 0) return false;
        ItemStack stack = inv.raw().getItem(slot);
        if (stack == null) return false;
        PotionType pt = (stack.getItemMeta() instanceof PotionMeta pm) ? pm.getBasePotionType() : null;
        int duration;
        int amplifier;
        if (pt == PotionType.STRONG_STRENGTH) {
            duration = 1800;
            amplifier = 1;
        } else {
            duration = 3600;
            amplifier = 0;
        }
        int previousSlot = inv.getSelectedHotbarSlot();
        slot = inv.selectMainInventorySlot(slot);
        if (slot < 0) return false;
        bukkit.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, amplifier));
        decrementSlot(inv, slot);
        inv.restoreSelectedSlotOrBestWeapon(previousSlot);
        bukkit.getWorld().playSound(bukkit.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.8f, 1.0f);
        return true;
    }

    private boolean tryThrowSplashHarming(Bot bot, BotInventory inv, LivingEntity target) {
        int slot = inv.findSplashHarming();
        if (slot < 0) return false;
        ItemStack stack = inv.raw().getItem(slot);
        if (stack == null) return false;
        Player bukkit = (Player) bot.getBukkitEntity();
        int previousSlot = inv.getSelectedHotbarSlot();
        slot = inv.selectMainInventorySlot(slot);
        if (slot < 0) return false;
        bot.faceLocation(target.getLocation());
        bukkit.swingMainHand();
        Location eye = bukkit.getEyeLocation();
        Vector aim = target.getEyeLocation().toVector()
                .subtract(eye.toVector())
                .normalize()
                .multiply(0.5);
        aim.setY(aim.getY() + 0.1); // Slight arc so it lands near the target's feet.
        ItemStack splash = stack.clone();
        splash.setAmount(1);
        bukkit.getWorld().spawn(eye, ThrownPotion.class, p -> {
            p.setShooter(bukkit);
            p.setItem(splash);
            p.setVelocity(aim);
        });
        decrementSlot(inv, slot);
        inv.restoreSelectedSlotOrBestWeapon(previousSlot);
        return true;
    }

    private static void decrementSlot(BotInventory inv, int slot) {
        inv.decrementMainInventorySlot(slot, 1);
    }
}
