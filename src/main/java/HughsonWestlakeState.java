import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Hughson-Westlake odyometri testi için immutable durum nesnesi.
 * Bu sınıf yan etkisiz (pure function) algoritma adımlarında
 * giriş/çıkış olarak kullanılır.
 */
public final class HughsonWestlakeState {

    public enum Ear {
        RIGHT, LEFT
    }

    // Test edilen kulak
    private final Ear ear;

    // Standart frekanslar (Hz)
    private final int[] frequencies;

    // Mevcut frekans index'i (0..frequencies.length-1)
    private final int frequencyIndex;

    // Mevcut uyarım şiddeti (dB HL)
    private final int levelDb;

    // Her frekans için saptanan eşik değerleri (dB HL)
    private final Map<Integer, Integer> thresholds;

    // İlgili frekans ve seviyede, artan 5 dB adımlarındaki toplam yanıt sayısı
    private final int ascendingPositiveCount;

    // Test tamamlandı mı?
    private final boolean finished;

    // Dışarıdan yeni bir state üretmek için kullanılacak alt ve üst sınırlar
    public static final int MIN_DB = -10;
    public static final int MAX_DB = 120;

    private HughsonWestlakeState(
            Ear ear,
            int[] frequencies,
            int frequencyIndex,
            int levelDb,
            Map<Integer, Integer> thresholds,
            int ascendingPositiveCount,
            boolean finished
    ) {
        this.ear = ear;
        this.frequencies = frequencies;
        this.frequencyIndex = frequencyIndex;
        this.levelDb = levelDb;
        this.thresholds = Collections.unmodifiableMap(thresholds);
        this.ascendingPositiveCount = ascendingPositiveCount;
        this.finished = finished;
    }

    /**
     * Yeni bir test başlatmak için fabrika metodu.
     * Varsayılan başlangıç seviyesi 30 dB HL olarak alınmıştır.
     */
    public static HughsonWestlakeState start(Ear ear, int[] frequencies) {
        return new HughsonWestlakeState(
                ear,
                frequencies.clone(),
                0,
                30,
                new HashMap<>(),
                0,
                false
        );
    }

    /**
     * Dahili olarak yeni immutable state oluşturmak için yardımcı metot.
     */
    public HughsonWestlakeState with(
            int newFrequencyIndex,
            int newLevelDb,
            Map<Integer, Integer> newThresholds,
            int newAscendingPositiveCount,
            boolean newFinished
    ) {
        return new HughsonWestlakeState(
                this.ear,
                this.frequencies,
                newFrequencyIndex,
                newLevelDb,
                newThresholds,
                newAscendingPositiveCount,
                newFinished
        );
    }

    public Ear getEar() {
        return ear;
    }

    public int[] getFrequencies() {
        return frequencies.clone();
    }

    public int getFrequencyIndex() {
        return frequencyIndex;
    }

    public int getCurrentFrequency() {
        return frequencies[frequencyIndex];
    }

    public int getLevelDb() {
        return levelDb;
    }

    public Map<Integer, Integer> getThresholds() {
        return thresholds;
    }

    public int getAscendingPositiveCount() {
        return ascendingPositiveCount;
    }

    public boolean isFinished() {
        return finished;
    }
}

