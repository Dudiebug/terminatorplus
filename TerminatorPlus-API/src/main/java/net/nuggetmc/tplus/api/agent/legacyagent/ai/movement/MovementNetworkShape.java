package net.nuggetmc.tplus.api.agent.legacyagent.ai.movement;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public final class MovementNetworkShape {
    public static final int OBSERVATION_SCHEMA_VERSION = 3;
    public static final int ACTION_SCHEMA_VERSION = 1;
    public static final List<String> BRANCH_FAMILY_IDS = List.of(
            "general_fallback",
            "melee",
            "mace",
            "trident_ranged",
            "spear_melee",
            "mobility",
            "explosive_survival",
            "projectile_ranged"
    );
    public static final List<String> MOVEMENT_OBJECTIVE_IDS = List.of(
            "approach",
            "retreat",
            "hold",
            "orbit",
            "disengage",
            "vertical_setup",
            "explosive_escape",
            "ranged_los",
            "shield_pressure",
            "cobweb_pressure",
            "pearl_engage",
            "pearl_disengage",
            "stuck_recovery"
    );
    public static final List<String> COMBAT_ACTION_CATEGORY_IDS = List.of(
            "none",
            "melee",
            "shield_axe",
            "mace_charge",
            "mace_airborne",
            "mace_smash",
            "trident_charge",
            "trident_throw",
            "crystal",
            "anchor",
            "pearl",
            "cobweb",
            "scanner_punish",
            "combo",
            "aerial_dive"
    );
    public static final List<String> INPUT_FIELDS = buildInputFields();
    public static final List<String> OUTPUT_FIELDS = List.of(
            "forwardPressure",
            "strafePressure",
            "jumpDesire",
            "sprintDesire",
            "retreatDesire",
            "facingAdjustment",
            "urgency",
            "holdPosition"
    );
    public static final int INPUT_COUNT = INPUT_FIELDS.size();
    public static final int OUTPUT_COUNT = OUTPUT_FIELDS.size();
    public static final int[] DEFAULT_LAYERS = {INPUT_COUNT, 32, 16, OUTPUT_COUNT};
    public static final String OBSERVATION_SCHEMA_HASH = sha256(
            "MovementInput:v" + OBSERVATION_SCHEMA_VERSION + ":" + String.join(",", INPUT_FIELDS));
    public static final String ACTION_SCHEMA_HASH = sha256(
            "MovementOutput:v" + ACTION_SCHEMA_VERSION + ":" + String.join(",", OUTPUT_FIELDS));

    private MovementNetworkShape() {
    }

    private static List<String> buildInputFields() {
        List<String> fields = new ArrayList<>(96);
        fields.addAll(List.of(
                "relX",
                "relY",
                "relZ",
                "distance",
                "horizontalDistance",
                "botVelX",
                "botVelY",
                "botVelZ",
                "botHorizontalSpeed",
                "targetVelX",
                "targetVelY",
                "targetVelZ",
                "targetHorizontalSpeed",
                "facingDot",
                "facingCross",
                "grounded",
                "falling",
                "sprinting",
                "recentlyJumped",
                "reachable",
                "obstructed",
                "desiredRange",
                "rangeUrgency",
                "wantsCritSetup",
                "wantsSprintHit",
                "wantsHoldPosition",
                "isCommitted",
                "commitProgress",
                "weaponRange"
        ));
        for (String family : BRANCH_FAMILY_IDS) fields.add("branchFamily." + family);
        for (String objective : MOVEMENT_OBJECTIVE_IDS) fields.add("movementObjective." + objective);
        for (String category : COMBAT_ACTION_CATEGORY_IDS) fields.add("combatActionCategory." + category);
        fields.addAll(List.of(
                "minSafeRange",
                "maxUsefulRange",
                "rangeErrorSigned",
                "commitTicksRemaining",
                "botAttackStrength",
                "targetHasIFrames",
                "botHealthFraction",
                "targetHealthFraction",
                "healthAdvantage",
                "targetBlocking",
                "targetAirborne",
                "targetRising",
                "targetSprintingAway",
                "targetInCobweb",
                "targetOverVoid",
                "openSkyAboveBot",
                "botInLavaArea",
                "botOnFire",
                "needsLineOfSight",
                "localLavaHazard",
                "localFireHazard",
                "baseline.forwardPressure",
                "baseline.strafePressure",
                "baseline.jumpDesire",
                "baseline.sprintDesire",
                "baseline.retreatDesire",
                "baseline.facingAdjustment",
                "baseline.urgency",
                "baseline.holdPosition"
        ));
        return List.copyOf(fields);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
