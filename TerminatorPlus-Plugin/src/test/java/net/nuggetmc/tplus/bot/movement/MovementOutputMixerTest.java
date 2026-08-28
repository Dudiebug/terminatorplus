package net.nuggetmc.tplus.bot.movement;

import net.nuggetmc.tplus.bot.combat.CombatIntent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MovementOutputMixerTest {

    @Test
    void neuralResidualCannotCancelBaselineSprint() {
        MovementOutput baseline = new MovementOutput(.85, .15, 0, .65, 0, 0, .65, 0);
        MovementOutputMixer mixer = new MovementOutputMixer();

        MovementOutputMixer.MixResult result = mixer.mix(
                CombatIntent.DEFAULT, baseline, MovementOutput.ZERO, true);

        assertEquals(.65, result.output().sprintDesire(), 1.0e-12);
    }
}
