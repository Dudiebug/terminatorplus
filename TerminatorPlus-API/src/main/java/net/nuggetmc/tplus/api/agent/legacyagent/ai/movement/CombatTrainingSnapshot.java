package net.nuggetmc.tplus.api.agent.legacyagent.ai.movement;

/**
 * Read-only combat outcome counters for movement-controller GA fitness.
 * Movement networks still do not execute combat; these values only tell the
 * trainer whether movement helped CombatDirector produce real damage.
 */
public record CombatTrainingSnapshot(
        boolean available,
        String loadout,
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
    public static CombatTrainingSnapshot unavailable() {
        return new CombatTrainingSnapshot(
                false,
                "",
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0
        );
    }
}
