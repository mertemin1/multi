package com.odyometre.core.algorithm;

import com.odyometre.core.model.TestConfig;
import com.odyometre.core.model.TestState;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HughsonWestlakeEngine {

    /**
     * Saf Fonksiyon (Pure Function): Mevcut test durumunu ve hastanın yanıtını alıp, tamamen yeni bir durumu döner.
     * Dışarıdaki hiçbir değişkeni değiştirmez (Side-effect free).
     */
    public static TestState applyResponse(TestState currentState, boolean heard, TestConfig config) {
        // Eğer eşik zaten bulunduysa durum değişmeden geri döner
        if (currentState.threshold().isPresent()) {
            return currentState;
        }

        int nextIntensity = currentState.currentIntensityDb();
        boolean nextIsAscending = currentState.isAscending();
        
        // Değiştirilemez map'lerin geçici kopyasını al (yeni TestState'e geçerken tekrar immutable olacak)
        Map<Integer, Integer> newResponseCounts = new HashMap<>(currentState.ascentResponseCounts());
        Map<Integer, Integer> newTrialCounts = new HashMap<>(currentState.ascentTrialCounts());
        Optional<Integer> nextThreshold = Optional.empty();

        if (heard) {
            if (currentState.isAscending()) {
                // Yükselişteyken duydu -> Bu dB seviyesi için bir yanıt kaydediyoruz.
                int currentDb = currentState.currentIntensityDb();
                int responses = newResponseCounts.getOrDefault(currentDb, 0) + 1;
                int trials = newTrialCounts.getOrDefault(currentDb, 0) + 1;
                
                newResponseCounts.put(currentDb, responses);
                newTrialCounts.put(currentDb, trials);

                if (responses >= config.thresholdCriteriaCount()) {
                    // Eşik bulundu! İstenen kriter sağlandı (Örn: 3 denemenin 2'sinde duydu).
                    nextThreshold = Optional.of(currentDb);
                } else {
                    // Eşik henüz onaylanmadı, duyduğu için tekrar iniyoruz.
                    nextIntensity = currentDb - config.stepDownDb();
                    nextIsAscending = false; // İniş (descending) moduna dön
                }
            } else {
                // Zaten iniyorken duydu -> İnmeye devam (10 dB down)
                nextIntensity = currentState.currentIntensityDb() - config.stepDownDb();
            }
        } else {
            // Duymadı
            if (currentState.isAscending()) {
                // Yükselişteyken duymadı -> Deneme sayısını (trial) artır ama Response artmaz
                int currentDb = currentState.currentIntensityDb();
                newTrialCounts.put(currentDb, newTrialCounts.getOrDefault(currentDb, 0) + 1);
            }
            // Duymadığı için 5 dB yukarı çıkıyoruz ve ascending moduna giriyoruz
            nextIntensity = currentState.currentIntensityDb() + config.stepUpDb();
            nextIsAscending = true;
        }

        // Fiziksel limitleri kontrol et (IEC 60645-1 vb. gereksinimler)
        if (nextIntensity > config.maxDb()) {
            nextIntensity = config.maxDb();
        } else if (nextIntensity < config.minDb()) {
            nextIntensity = config.minDb();
        }

        return new TestState(
                currentState.frequency(),
                nextIntensity,
                nextIsAscending,
                newResponseCounts,
                newTrialCounts,
                nextThreshold
        );
    }
}
