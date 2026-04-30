package net.nuggetmc.tplus.api.agent.legacyagent.ai.movement;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Family-specific reward shaping for movement-controller reinforcement.
 * Inputs are immutable snapshots captured on the server thread; scoring can run
 * asynchronously without touching Bukkit/NMS state.
 */
public final class MovementRewardProfile {
    private final String familyId;

    private MovementRewardProfile(String familyId) {
        this.familyId = MovementTrainingConfig.normalizeFamilyId(familyId);
    }

    public static MovementRewardProfile forFamily(String familyId) {
        return new MovementRewardProfile(familyId);
    }

    public RewardBreakdown score(MovementTrainingSnapshot movement, CombatTrainingSnapshot combat, CombatDeltas deltas) {
        MovementTrainingSnapshot m = movement == null ? MovementTrainingSnapshot.unavailable() : movement;
        CombatTrainingSnapshot c = combat == null ? CombatTrainingSnapshot.unavailable() : combat;
        CombatDeltas d = deltas == null ? CombatDeltas.ZERO : deltas;
        Map<String, Double> components = switch (familyId) {
            case "melee" -> melee(m, d);
            case "mace" -> mace(m, d);
            case "trident_ranged" -> tridentRanged(m, d);
            case "spear_melee" -> spearMelee(m, c, d);
            case "mobility" -> mobility(m, d);
            case "explosive_survival" -> explosiveSurvival(m, d);
            case "projectile_ranged" -> projectileRanged(m, d);
            default -> generalFallback(m, d);
        };

        double total = 0.0;
        for (double value : components.values()) {
            if (Double.isFinite(value)) total += value;
        }
        return new RewardBreakdown(familyId, total, components);
    }

    private static Map<String, Double> generalFallback(MovementTrainingSnapshot m, CombatDeltas d) {
        Map<String, Double> out = baseDamageSurvival(m, d, 1.8, 1.4);
        out.put("range_control", rangeScore(m, 1.5));
        out.put("low_fallback_rate", m.controllerFallback() ? -0.35 : 0.08);
        out.put("low_route_thrash", m.movementLocked() ? -0.02 : 0.05);
        out.put("low_oscillation", m.retreating() && m.circling() ? -0.08 : 0.04);
        return out;
    }

    private static Map<String, Double> melee(MovementTrainingSnapshot m, CombatDeltas d) {
        Map<String, Double> out = baseDamageSurvival(m, d, 2.2, 1.2);
        boolean meleeRange = m.horizontalDistance() <= 4.5;
        out.put("melee_threat_range", meleeRange ? 0.25 : -0.18);
        out.put("legal_sprint_hit_setup", m.legalSprintSetup() ? 0.22 : 0.0);
        out.put("legal_crit_setup", m.legalCritSetup() ? 0.18 : 0.0);
        out.put("hit_conversion", (d.swordDamage() + d.axeDamage() + d.spearDamage()) * 1.3);
        out.put("sweep_setup", m.wantsCritSetup() && meleeRange && !m.retreating() ? 0.08 : 0.0);
        out.put("jump_spam_penalty", m.justJumped() && !m.legalCritSetup() ? -0.14 : 0.0);
        out.put("backing_out_penalty", !meleeRange && m.retreating() ? -0.18 : 0.0);
        out.put("iframe_whiff_pressure", m.controllerFallback() && meleeRange ? -0.10 : 0.0);
        return out;
    }

    private static Map<String, Double> mace(MovementTrainingSnapshot m, CombatDeltas d) {
        Map<String, Double> out = baseDamageSurvival(m, d, 2.0, 1.5);
        out.put("phase_conversion", m.committed() ? 0.22 + m.commitProgress() * 0.18 : 0.0);
        out.put("airborne_tracking", m.falling() && m.horizontalDistance() <= 6.0 ? 0.16 : 0.0);
        out.put("smash_connect", d.maceDamage() * 2.6);
        out.put("commit_selection", "mace".equals(m.activeBranchFamily()) && m.committed() ? 0.20 : 0.0);
        out.put("failed_launch_penalty", m.plannedAction().contains("mace") && !m.committed() && m.controllerFallback() ? -0.22 : 0.0);
        out.put("landed_no_smash_penalty", !m.falling() && m.committed() && d.maceDamage() <= 0.0 ? -0.18 : 0.0);
        out.put("self_fall_damage_penalty", m.falling() ? -d.damageTaken() * 0.8 : 0.0);
        out.put("mid_commit_switch_penalty", m.committed() && !"mace".equals(m.activeBranchFamily()) ? -0.30 : 0.0);
        return out;
    }

    private static Map<String, Double> tridentRanged(MovementTrainingSnapshot m, CombatDeltas d) {
        Map<String, Double> out = baseDamageSurvival(m, d, 1.9, 1.2);
        boolean throwBand = m.horizontalDistance() >= 8.0 && m.horizontalDistance() <= 28.0;
        out.put("throw_range", throwBand ? 0.24 : -0.12);
        out.put("line_of_sight", m.controllerFallback() ? -0.08 : 0.10);
        out.put("charge_completion", m.committed() && m.plannedAction().contains("trident") ? 0.20 : 0.0);
        out.put("release_hit_rate", d.tridentDamage() * 2.2);
        out.put("anti_melee_collapse", m.horizontalDistance() < 4.0 ? -0.22 : 0.0);
        out.put("charging_out_of_range", m.committed() && !throwBand ? -0.20 : 0.0);
        out.put("over_retreating", m.horizontalDistance() > 32.0 && m.retreating() ? -0.18 : 0.0);
        return out;
    }

    private static Map<String, Double> spearMelee(MovementTrainingSnapshot m, CombatTrainingSnapshot c, CombatDeltas d) {
        Map<String, Double> out = baseDamageSurvival(m, d, 2.0, 1.2);
        boolean close = m.horizontalDistance() <= 4.5;
        out.put("close_trident_pressure", close ? 0.24 : -0.12);
        out.put("stable_melee_spacing", rangeScore(m, 1.2));
        out.put("hit_conversion", d.spearDamage() * 2.2 + d.tridentDamage() * 0.7);
        out.put("no_sword_fallback", "spear_melee".equals(c.loadoutFamily()) && close ? 0.12 : 0.0);
        out.put("ranged_at_point_blank_penalty", close && m.plannedAction().contains("trident") && m.committed() ? -0.20 : 0.0);
        return out;
    }

    private static Map<String, Double> mobility(MovementTrainingSnapshot m, CombatDeltas d) {
        Map<String, Double> out = baseDamageSurvival(m, d, 1.5, 1.3);
        boolean badClose = m.horizontalDistance() < 3.0;
        boolean farAway = m.horizontalDistance() > 36.0;
        out.put("gap_close", m.approachSpeed() > 0.08 && m.horizontalDistance() > m.desiredRange() ? 0.18 : 0.0);
        out.put("escape_bad_range", badClose && m.retreating() ? 0.18 : 0.0);
        out.put("vertical_setup", (m.falling() || m.justJumped()) && m.horizontalDistance() <= 16.0 ? 0.12 : 0.0);
        out.put("route_handoff_success", m.movementLocked() && "mobility".equals(m.activeBranchFamily()) ? 0.14 : 0.0);
        out.put("recoverable_velocity", m.falling() && m.horizontalDistance() <= 24.0 ? 0.06 : 0.0);
        out.put("stalling_penalty", m.approachSpeed() < 0.02 && !m.retreating() && !m.circling() ? -0.10 : 0.0);
        out.put("flying_away_penalty", farAway && m.retreating() ? -0.24 : 0.0);
        out.put("unsafe_landing_penalty", d.damageTaken() > 0.0 && (m.falling() || m.justJumped()) ? -d.damageTaken() * 0.8 : 0.0);
        out.put("lost_los_penalty", m.controllerFallback() ? -0.08 : 0.0);
        return out;
    }

    private static Map<String, Double> explosiveSurvival(MovementTrainingSnapshot m, CombatDeltas d) {
        Map<String, Double> out = baseDamageSurvival(m, d, 1.6, 1.9);
        boolean safeBlastBand = m.horizontalDistance() >= 5.0 && m.horizontalDistance() <= 11.0;
        out.put("target_explosive_damage", d.explosiveDamage() * 2.8);
        out.put("safe_blast_spacing", safeBlastBand ? 0.24 : -0.12);
        out.put("post_setup_escape", m.plannedAction().contains("crystal") || m.plannedAction().contains("anchor")
                ? (m.retreating() ? 0.18 : -0.16) : 0.0);
        out.put("low_self_damage", -d.damageTaken() * 1.2);
        out.put("unsafe_radius_penalty", m.horizontalDistance() < 4.0 && d.explosiveDamage() > 0.0 ? -0.35 : 0.0);
        out.put("no_retreat_after_place", m.wantsHoldPosition() && !m.retreating() ? -0.16 : 0.0);
        return out;
    }

    private static Map<String, Double> projectileRanged(MovementTrainingSnapshot m, CombatDeltas d) {
        Map<String, Double> out = baseDamageSurvival(m, d, 1.6, 1.1);
        boolean band = m.horizontalDistance() >= 6.0 && m.horizontalDistance() <= 24.0;
        out.put("line_of_sight_control", m.controllerFallback() ? -0.08 : 0.12);
        out.put("lateral_strafe", m.circling() ? 0.16 : 0.0);
        out.put("projectile_success", d.projectileDamage() * 2.0);
        out.put("useful_distance_band", band ? 0.18 : -0.10);
        out.put("waste_penalty", m.controllerFallback() && m.plannedAction().matches(".*(projectile|arrow|splash|firework).*") ? -0.16 : 0.0);
        return out;
    }

    private static Map<String, Double> baseDamageSurvival(MovementTrainingSnapshot m, CombatDeltas d, double dealtWeight, double takenWeight) {
        Map<String, Double> out = new LinkedHashMap<>();
        out.put("damage_delta", d.damageDealt() * dealtWeight - d.damageTaken() * takenWeight);
        out.put("survival", m.available() ? 0.08 : -0.25);
        return out;
    }

    private static double rangeScore(MovementTrainingSnapshot m, double weight) {
        double desired = Math.max(1.0, m.desiredRange());
        double error = Math.min(1.0, Math.abs(m.horizontalDistance() - desired) / desired);
        return (1.0 - error) * weight * 0.18 - error * weight * 0.08;
    }

    public record CombatDeltas(
            double damageDealt,
            double damageTaken,
            double swordDamage,
            double axeDamage,
            double maceDamage,
            double tridentDamage,
            double spearDamage,
            double projectileDamage,
            double explosiveDamage
    ) {
        public static final CombatDeltas ZERO = new CombatDeltas(0, 0, 0, 0, 0, 0, 0, 0, 0);

        public static CombatDeltas between(CombatTrainingSnapshot previous, CombatTrainingSnapshot current) {
            CombatTrainingSnapshot p = previous == null ? CombatTrainingSnapshot.unavailable() : previous;
            CombatTrainingSnapshot c = current == null ? CombatTrainingSnapshot.unavailable() : current;
            return new CombatDeltas(
                    nonNegative(c.damageDealt() - p.damageDealt()),
                    nonNegative(c.damageTaken() - p.damageTaken()),
                    nonNegative(c.swordDamage() - p.swordDamage()),
                    nonNegative(c.axeDamage() - p.axeDamage()),
                    nonNegative(c.maceDamage() - p.maceDamage()),
                    nonNegative(c.tridentDamage() - p.tridentDamage()),
                    nonNegative(c.spearDamage() - p.spearDamage()),
                    nonNegative(c.projectileDamage() - p.projectileDamage()),
                    nonNegative(c.explosiveDamage() - p.explosiveDamage())
            );
        }

        private static double nonNegative(double value) {
            return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
        }
    }

    public record RewardBreakdown(String familyId, double total, Map<String, Double> components) {
        public RewardBreakdown {
            familyId = familyId == null || familyId.isBlank()
                    ? MovementBrainBank.FALLBACK_BRAIN_NAME
                    : familyId.toLowerCase(Locale.ROOT);
            Map<String, Double> copy = new LinkedHashMap<>();
            if (components != null) {
                components.forEach((key, value) -> {
                    if (key != null && value != null && Double.isFinite(value)) {
                        copy.put(key, value);
                    }
                });
            }
            components = Collections.unmodifiableMap(copy);
        }
    }
}
