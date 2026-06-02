package com.odyometre.core.algorithm;

import com.odyometre.core.model.TestConfig;
import com.odyometre.core.model.TestState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HughsonWestlakeEngineTest {

    private final TestConfig config = TestConfig.defaultASHA();

    @Test
    void testInitialDescentWhenHeard() {
        TestState initial = TestState.initial(1000, 30);
        TestState next = HughsonWestlakeEngine.applyResponse(initial, true, config);
        
        assertEquals(20, next.currentIntensityDb(), "Duyduğunda 10 dB aşağı inmelidir.");
        assertFalse(next.isAscending(), "Hala descending (iniş) modunda kalmalıdır.");
    }

    @Test
    void testAscentWhenNotHeard() {
        TestState state = TestState.initial(1000, 20);
        TestState next = HughsonWestlakeEngine.applyResponse(state, false, config);
        
        assertEquals(25, next.currentIntensityDb(), "Duymadığında 5 dB yukarı çıkmalıdır.");
        assertTrue(next.isAscending(), "Ascending (çıkış) moduna geçmelidir.");
    }

    @Test
    void testThresholdDetectedAfterTwoAscentResponses() {
        TestState state = TestState.initial(1000, 30);
        // İniş: 30 duydu -> 20
        state = HughsonWestlakeEngine.applyResponse(state, true, config);
        // İniş: 20 duydu -> 10
        state = HughsonWestlakeEngine.applyResponse(state, true, config);
        // İniş: 10 duymadı -> 15 (Ascent başlar)
        state = HughsonWestlakeEngine.applyResponse(state, false, config);
        
        // Yükseliş: 15 duydu -> Ascent Response 1. Sonra 10 iner -> 5
        state = HughsonWestlakeEngine.applyResponse(state, true, config);
        
        assertTrue(state.threshold().isEmpty(), "Henüz 1 kere onaylandı, eşik bulunmamalı.");
        assertEquals(5, state.currentIntensityDb());
        assertFalse(state.isAscending());

        // İniş: 5 duymadı -> 10 (Ascent başlar)
        state = HughsonWestlakeEngine.applyResponse(state, false, config);
        // Yükseliş: 10 duymadı -> 15
        state = HughsonWestlakeEngine.applyResponse(state, false, config);
        
        // Yükseliş: 15 duydu -> Ascent Response 2.
        state = HughsonWestlakeEngine.applyResponse(state, true, config);

        assertTrue(state.threshold().isPresent(), "2 defa yükselişte 15 dB de duyulduğu için eşik bulunmalı.");
        assertEquals(15, state.threshold().get());
    }
}
