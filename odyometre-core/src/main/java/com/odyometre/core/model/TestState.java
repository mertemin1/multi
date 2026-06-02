package com.odyometre.core.model;

import java.util.Map;
import java.util.Optional;

/**
 * Testin mevcut durumunu tutan değiştirilemez (immutable) veri yapısı.
 */
public record TestState(
        int frequency,
        int currentIntensityDb,
        boolean isAscending, // True if we are in the 5 dB UP phase, False if in the 10 dB DOWN phase
        Map<Integer, Integer> ascentResponseCounts,
        Map<Integer, Integer> ascentTrialCounts,
        Optional<Integer> threshold
) {
    public TestState {
        // Defensive copy to ensure Maps are truly immutable even if mutable maps are passed
        ascentResponseCounts = ascentResponseCounts == null ? Map.of() : Map.copyOf(ascentResponseCounts);
        ascentTrialCounts = ascentTrialCounts == null ? Map.of() : Map.copyOf(ascentTrialCounts);
    }
    
    // Initial state factory method
    public static TestState initial(int frequency, int startIntensityDb) {
        return new TestState(
            frequency,
            startIntensityDb,
            false, // Start with descending
            Map.of(),
            Map.of(),
            Optional.empty()
        );
    }
}
