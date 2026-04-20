package net.nuggetmc.tplus.bot.combat;

import org.bukkit.util.Vector;

/**
 * Scratch-pad for multi-tick weapon behaviors (trident run-up, mace
 * airborne tracking). One instance per bot, stored on the Bot.
 */
public final class CombatState {

    public enum Phase { IDLE, CHARGING, AIRBORNE, RELEASE }

    private Phase phase = Phase.IDLE;
    private int phaseTicks = 0;
    private Vector chargeDirection;

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
        this.phaseTicks = 0;
    }

    public int tickPhase() {
        return ++phaseTicks;
    }

    public int getPhaseTicks() {
        return phaseTicks;
    }

    public Vector getChargeDirection() {
        return chargeDirection == null ? null : chargeDirection.clone();
    }

    public void setChargeDirection(Vector v) {
        this.chargeDirection = v == null ? null : v.clone();
    }

    public void reset() {
        phase = Phase.IDLE;
        phaseTicks = 0;
        chargeDirection = null;
    }
}
