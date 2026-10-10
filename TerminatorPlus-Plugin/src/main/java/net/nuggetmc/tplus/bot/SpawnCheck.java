package net.nuggetmc.tplus.bot;

/**
 * Result of validating a {@code /bot setspawn} location.
 *
 * @param problem    why the location was refused, or {@link Problem#NONE}
 * @param dropHeight how far a bot falls before landing; 0 when standing on ground
 */
public record SpawnCheck(Problem problem, double dropHeight) {

    public enum Problem {
        NONE,
        OUTSIDE_WORLD,
        OBSTRUCTED,
        NO_GROUND,
        UNSAFE_LANDING
    }

    static SpawnCheck refused(Problem problem) {
        return new SpawnCheck(problem, 0.0);
    }

    public boolean accepted() {
        return problem == Problem.NONE;
    }
}
