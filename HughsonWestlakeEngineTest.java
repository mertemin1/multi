import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HughsonWestlakeEngine icin basit birim testleri.
 *
 * Bu testler, resmi biyomedikal kurallardan bagimsiz olarak
 * mantiksal akisi doğrulamak icin ornek niteligindedir.
 */
public class HughsonWestlakeEngineTest {

    @Test
    void heardResponseShouldDecreaseLevelBy10dB() {
        int[] freqs = {1000};
        HughsonWestlakeState state = HughsonWestlakeState.start(HughsonWestlakeState.Ear.RIGHT, freqs);

        HughsonWestlakeState next = HughsonWestlakeEngine.applyResponse(state, true);

        assertEquals(state.getLevelDb() - 10, next.getLevelDb());
        assertFalse(next.isFinished());
    }

    @Test
    void notHeardResponseShouldIncreaseLevelBy5dB() {
        int[] freqs = {1000};
        HughsonWestlakeState state = HughsonWestlakeState.start(HughsonWestlakeState.Ear.RIGHT, freqs);

        HughsonWestlakeState next = HughsonWestlakeEngine.applyResponse(state, false);

        assertEquals(state.getLevelDb() + 5, next.getLevelDb());
        assertFalse(next.isFinished());
    }

    @Test
    void twoAscendingHeardResponsesShouldRecordThresholdAndMoveToNextFrequency() {
        int[] freqs = {1000, 2000};
        HughsonWestlakeState state = HughsonWestlakeState.start(HughsonWestlakeState.Ear.RIGHT, freqs);

        // Basit senaryo: iki kez DUYDU kabul edelim
        HughsonWestlakeState step1 = HughsonWestlakeEngine.applyResponse(state, true);
        HughsonWestlakeState step2 = HughsonWestlakeEngine.applyResponse(step1, true);

        // 1000 Hz icin esik kaydedilmis olmali
        Map<Integer, Integer> thresholds = step2.getThresholds();
        assertTrue(thresholds.containsKey(1000));

        // Sonraki frekansa gecilmeli
        assertEquals(1, step2.getFrequencyIndex());
        assertEquals(2000, step2.getCurrentFrequency());
        assertFalse(step2.isFinished());
    }

    @Test
    void engineShouldEventuallyFinishAfterLastFrequency() {
        int[] freqs = {1000};
        HughsonWestlakeState state = HughsonWestlakeState.start(HughsonWestlakeState.Ear.RIGHT, freqs);

        HughsonWestlakeState s1 = HughsonWestlakeEngine.applyResponse(state, true);
        HughsonWestlakeState s2 = HughsonWestlakeEngine.applyResponse(s1, true);

        assertTrue(s2.isFinished());
        assertTrue(s2.getThresholds().containsKey(1000));
    }
}

