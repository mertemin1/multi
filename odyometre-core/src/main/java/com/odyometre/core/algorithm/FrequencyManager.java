package com.odyometre.core.algorithm;

import java.util.List;
import java.util.Optional;

public class FrequencyManager {
    // ASHA standardına göre test edilecek frekans sırası
    public static final List<Integer> FREQUENCY_SEQUENCE = List.of(
            1000, 2000, 3000, 4000, 6000, 8000, 1000, 500, 250
    );

    /**
     * Saf Fonksiyon: Şu anki frekansın index'ini bulup bir sonrakini döner.
     * Yan etki yaratmamak için Optional kullanılmıştır.
     */
    public static Optional<Integer> getNextFrequency(int currentFrequency, boolean isFirst1000HzDone) {
        int index = -1;
        if (currentFrequency == 1000) {
            index = isFirst1000HzDone ? 6 : 0; // 6. index = 1000Hz (ikinci test)
        } else {
            index = FREQUENCY_SEQUENCE.indexOf(currentFrequency);
        }

        if (index == -1 || index == FREQUENCY_SEQUENCE.size() - 1) {
            return Optional.empty(); // Test dizilimi bitti
        }
        
        return Optional.of(FREQUENCY_SEQUENCE.get(index + 1));
    }
}
