import java.util.HashMap;
import java.util.Map;

/**
 * Hughson-Westlake prosedürünün basitleştirilmiş, pure function çekirdeği.
 *
 * Bu sınıf yalnızca immutable {@link HughsonWestlakeState} alır ve
 * yeni bir {@link HughsonWestlakeState} döndürür. Böylece
 * Fonksiyonel Programlama (FP) yaklaşımıyla, yan etkisiz
 * medikal hesaplama katmanı elde edilir.
 */
public final class HughsonWestlakeEngine {

    private HughsonWestlakeEngine() {
        // statik yardımcı sınıf
    }

    /**
     * Hastadan gelen yanıtı ("duydu" / "duymadı") işleyip,
     * bir sonraki adımın durumunu üretir.
     *
     * Varsayılan kurallar (öğretim amaçlı sadeleştirilmiş sürüm):
     *  - Eğer hasta DUYMUŞSA -> seviyeyi 10 dB azalt.
     *  - Eğer hasta DUYMAMIŞSA -> seviyeyi 5 dB artır.
     *  - Aynı frekansta artan yönde (5 dB adımlarıyla) en az 2 pozitif yanıt
     *    alınırsa, o seviye eşik olarak kaydedilir ve bir sonraki frekansa geçilir.
     */
    public static HughsonWestlakeState applyResponse(HughsonWestlakeState state, boolean heard) {
        if (state.isFinished()) {
            return state;
        }

        int currentFreqIndex = state.getFrequencyIndex();
        int[] freqs = state.getFrequencies();
        int currentFreq = freqs[currentFreqIndex];

        int level = state.getLevelDb();
        int ascendingPositiveCount = state.getAscendingPositiveCount();
        Map<Integer, Integer> thresholdsCopy = new HashMap<>(state.getThresholds());

        if (heard) {
            // Hasta duydu -> 10 dB aşağı
            int newLevel = Math.max(HughsonWestlakeState.MIN_DB, level - 10);

            // Artan yönde en az 2 kez DUYDU ise bu seviyeyi eşik kabul et
            int newAscendingCount = ascendingPositiveCount + 1;
            if (newAscendingCount >= 2) {
                thresholdsCopy.put(currentFreq, newLevel);

                int nextIndex = currentFreqIndex + 1;
                if (nextIndex >= freqs.length) {
                    // Tüm frekanslar tamamlandı
                    return state.with(
                            currentFreqIndex,
                            newLevel,
                            thresholdsCopy,
                            newAscendingCount,
                            true
                    );
                } else {
                    // Sonraki frekansa 40 dB HL seviyesinden başla
                    return state.with(
                            nextIndex,
                            40,
                            thresholdsCopy,
                            0,
                            false
                    );
                }
            } else {
                // Aynı frekansta artan pozitif sayısını güncelle
                return state.with(
                        currentFreqIndex,
                        newLevel,
                        thresholdsCopy,
                        newAscendingCount,
                        false
                );
            }
        } else {
            // Hasta duymadı -> 5 dB yukarı
            int newLevel = Math.min(HughsonWestlakeState.MAX_DB, level + 5);

            // Negatif yanıt aldığımız için artan pozitif sayacı sıfırlanır
            return state.with(
                    currentFreqIndex,
                    newLevel,
                    thresholdsCopy,
                    0,
                    false
            );
        }
    }
}

