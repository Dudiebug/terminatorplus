package net.nuggetmc.tplus.api.agent.legacyagent.ai.movement;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

public final class MovementNetworkShape {
    public static final int OBSERVATION_SCHEMA_VERSION = 2;
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
    public static final List<String> INPUT_FIELDS = List.of(
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
            "weaponRange",
            "branchFamily." + BRANCH_FAMILY_IDS.get(0),
            "branchFamily." + BRANCH_FAMILY_IDS.get(1),
            "branchFamily." + BRANCH_FAMILY_IDS.get(2),
            "branchFamily." + BRANCH_FAMILY_IDS.get(3),
            "branchFamily." + BRANCH_FAMILY_IDS.get(4),
            "branchFamily." + BRANCH_FAMILY_IDS.get(5),
            "branchFamily." + BRANCH_FAMILY_IDS.get(6),
            "branchFamily." + BRANCH_FAMILY_IDS.get(7)
    );
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
