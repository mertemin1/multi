package com.odyometre.core.model;

/**
 * Hughson-Westlake algoritması test ayarları.
 */
public record TestConfig(
        int startIntensityDb,
        int stepDownDb,
        int stepUpDb,
        int minDb,
        int maxDb,
        int thresholdCriteriaCount,
        int thresholdCriteriaTrials
) {
    public static TestConfig defaultASHA() {
        return new TestConfig(30, 10, 5, -10, 120, 2, 3); 
        // Başlangıç: 30dB, Aşağı: 10, Yukarı: 5, Max: 120, Min: -10
        // Eşik kriteri: 3 yükseliş denemesinin 2'sinde yanıt alınması
    }
}
