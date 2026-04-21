package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import net.nuggetmc.tplus.bot.loadout.BotInventory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

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

    /**
     * @return true if the director handled combat this tick and the
     *         caller should NOT fall back to its default attack code.
     */
    public boolean tick(Bot bot, LivingEntity target) {
        if (target == null || !target.isValid()) return false;
        // Bots used for AI training rely on the legacy deterministic damage table; skip.
        if (bot.hasNeuralNetwork()) return false;

        // Passive behaviors run every tick regardless of weapon choice.
        elytra.tick(bot, target);
        totem.tick(bot, target);

        double distance = bot.getLocation().distance(target.getLocation());
        BotInventory inv = bot.getBotInventory();

        // Mid-attack commitment: stay on the chosen weapon.
        if (bot.getCombatState().getPhase() == CombatState.Phase.AIRBORNE && inv.hasMace()) {
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
                bot.getCombatState().setPhase(CombatState.Phase.AIRBORNE);
                selectType(inv, Material.MACE);
                mace.ticksFor(bot, target, distance);
                return true;
            }
        }
        if (bot.getCombatState().getPhase() == CombatState.Phase.CHARGING && inv.hasTrident()) {
            selectType(inv, Material.TRIDENT);
            trident.ticksFor(bot, target, distance);
            return true;
        }

        // Priority order (highest first):
        //  1. Crystal PvP at short range (huge burst)
        //  2. Anchor bomb in Nether at short range
        //  3. Mace smash (ground + mace + CD ready)
        //  4. Sword melee
        //  5. Wind charge BOOST (grounded + needs to traverse) — launches self upward+forward
        //  6. Trident momentum throw (mid range)
        //  7. Ender pearl (long range gap-close + traversal)
        //  8. Wind charge zone (combat throw at target)
        //  9. Cobweb utility (target fleeing)
        // 10. Healing

        World.Environment env = bot.getDimension();

        if (env != World.Environment.NETHER && inv.hasCrystalKit()
                && distance <= 6.0
                && bot.getBotCooldowns().ready(CrystalBehavior.COOLDOWN_KEY, bot.getAliveTicks())) {
            selectType(inv, Material.END_CRYSTAL);
            crystal.ticksFor(bot, target, distance);
            return true;
        }

        if (env == World.Environment.NETHER && inv.hasAnchorKit()
                && distance <= 5.0
                && bot.getBotCooldowns().ready(AnchorBombBehavior.COOLDOWN_KEY, bot.getAliveTicks())) {
            selectType(inv, Material.RESPAWN_ANCHOR);
            anchor.ticksFor(bot, target, distance);
            return true;
        }

        boolean grounded = bot.isBotOnGround();

        if (distance <= 3.5 && inv.hasMace() && grounded
                && bot.getBotCooldowns().ready(MaceBehavior.COOLDOWN_KEY, bot.getAliveTicks())) {
            selectType(inv, Material.MACE);
            mace.ticksFor(bot, target, distance);
            return true;
        }

        if (distance <= 3.5) {
            int sword = inv.findSword();
            int axe = inv.findAxe();
            int slot = sword >= 0 ? sword : axe;
            if (slot >= 0) bot.selectHotbarSlot(slot);
            melee.ticksFor(bot, target, distance);
            return true;
        }

        // Wind charge BOOST — pure-traversal launch. Throws straight down at the bot's feet so
        // the explosion knocks it upward; horizontal velocity from pathing carries it toward target.
        // Combines with elytra for sustained flight. Runs only when grounded AND there's distance to close.
        if (grounded && distance >= 12.0 && inv.hasWindCharge()
                && bot.getBotCooldowns().ready(WindChargeBehavior.BOOST_COOLDOWN_KEY, bot.getAliveTicks())) {
            selectType(inv, Material.WIND_CHARGE);
            if (windCharge.boostTowards(bot, target) > 0) return true;
        }

        if (distance >= 5.0 && distance <= 28.0 && inv.hasTrident()
                && bot.getBotCooldowns().ready(TridentBehavior.COOLDOWN_KEY, bot.getAliveTicks())) {
            selectType(inv, Material.TRIDENT);
            trident.ticksFor(bot, target, distance);
            return true;
        }

        if (distance >= 14.0 && distance <= 80.0 && inv.hasEnderPearl()
                && bot.getBotCooldowns().ready(EnderPearlBehavior.COOLDOWN_KEY, bot.getAliveTicks())) {
            pearl.ticksFor(bot, target, distance);
            return true;
        }

        if (distance >= 4.0 && inv.hasWindCharge()
                && bot.getBotCooldowns().ready(WindChargeBehavior.COOLDOWN_KEY, bot.getAliveTicks())) {
            selectType(inv, Material.WIND_CHARGE);
            windCharge.ticksFor(bot, target, distance);
            return true;
        }

        if (inv.hasCobweb() && distance <= 4.5
                && bot.getBotCooldowns().ready(UtilityBehavior.COOLDOWN_KEY, bot.getAliveTicks())) {
            if (utility.ticksFor(bot, target, distance) > 0) return true;
        }

        // Healing: if hurt, try to use a golden apple.
        if (bot.getBotHealth() < bot.getBotMaxHealth() * 0.4f && bot.getBotCooldowns().ready("heal", bot.getAliveTicks())) {
            if (tryHeal(bot)) {
                bot.getBotCooldowns().set("heal", 100, bot.getAliveTicks());
                return true;
            }
        }

        return false;
    }

    private boolean tryHeal(Bot bot) {
        int slot = bot.getBotInventory().findHealing();
        if (slot < 0) return false;
        ItemStack it = bot.getBotInventory().raw().getItem(slot);
        if (it == null) return false;
        bot.selectHotbarSlot(slot);
        if (it.getType() == Material.GOLDEN_APPLE || it.getType() == Material.ENCHANTED_GOLDEN_APPLE) {
            float max = bot.getBotMaxHealth();
            bot.getBukkitEntity().setHealth(Math.min(max, bot.getBotHealth() + 8.0f));
            int amt = it.getAmount();
            if (amt <= 1) bot.getBotInventory().raw().setItem(slot, new ItemStack(Material.AIR));
            else {
                it.setAmount(amt - 1);
                bot.getBotInventory().raw().setItem(slot, it);
            }
            return true;
        }
        return false;
    }

    private void selectType(BotInventory inv, Material type) {
        int slot = inv.findHotbar(type);
        if (slot >= 0) inv.getBot().selectHotbarSlot(slot);
    }
}
