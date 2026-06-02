package com.odyometre.core.algorithm;

import com.odyometre.core.model.TestConfig;
import com.odyometre.core.model.TestState;
import net.jqwik.api.*;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HughsonWestlakePropertyTest {

    private final TestConfig config = TestConfig.defaultASHA();

    @Property
    void nextIntensityAlwaysWithinLimits(
            @ForAll("validStates") TestState state,
            @ForAll boolean heard) {
        
        TestState next = HughsonWestlakeEngine.applyResponse(state, heard, config);
        
        assertTrue(next.currentIntensityDb() >= config.minDb(), "Şiddet minDb altına düşemez. Geçerli dB: " + next.currentIntensityDb());
        assertTrue(next.currentIntensityDb() <= config.maxDb(), "Şiddet maxDb üstüne çıkamaz. Geçerli dB: " + next.currentIntensityDb());
    }

    @Provide
    Arbitrary<TestState> validStates() {
        return Combinators.combine(
                Arbitraries.integers().between(250, 8000), // frequency
                Arbitraries.integers().between(-10, 120),  // intensity
                Arbitraries.of(true, false)                // isAscending
        ).as((freq, intensity, asc) -> new TestState(freq, intensity, asc, Map.of(), Map.of(), Optional.empty()));
    }
}
