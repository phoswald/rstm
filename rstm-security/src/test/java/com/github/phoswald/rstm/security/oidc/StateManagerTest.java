package com.github.phoswald.rstm.security.oidc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class StateManagerTest {

    private Instant now = Instant.now();
    private long random = 1; // RandomGenerator produces a sequence of longs
    private final Duration lifetime = Duration.ofMinutes(5);
    private final StateManager testee = new StateManager(lifetime, () -> now, () -> random++);

    @Test
    void createRandomState() {
        Provider provider = Provider.builder().id("1").build();
        assertEquals("01000000000000000200000000000000", testee.create(provider));
        assertEquals("03000000000000000400000000000000", testee.create(provider));
    }

    @Test
    void createAndConsumeOnce() {
        Provider provider = Provider.builder().id("1").build();
        String state1 = testee.create(provider);
        String state2 = testee.create(provider);
        assertNotNull(testee.consume(state1));
        assertNotNull(testee.consume(state2));
        assertNull(testee.consume(state2));
        assertNull(testee.consume("other"));
    }

    @Test
    void testStateManager() {
        Provider provider = Provider.builder().id("1").build();
        String state1 = testee.create(provider); advanceTime(Duration.ofMinutes(1));
        String state2 = testee.create(provider); advanceTime(Duration.ofMinutes(1));
        String state3 = testee.create(provider); advanceTime(Duration.ofMinutes(1));
        String state4 = testee.create(provider); advanceTime(Duration.ofMinutes(1));
        String state5 = testee.create(provider); advanceTime(Duration.ofMinutes(1));
        String state6 = testee.create(provider); advanceTime(Duration.ofMinutes(1));
        String state7 = testee.create(provider); advanceTime(Duration.ofMinutes(1));
        assertNull(testee.consume(state1)); // expired
        assertNotNull(testee.consume(state3));
        assertNull(testee.consume(state2)); // expired
        assertNotNull(testee.consume(state6)); // out of order
        assertNotNull(testee.consume(state5)); // out of order
        assertNotNull(testee.consume(state4));
        assertNotNull(testee.consume(state7));
    }

    void advanceTime(Duration elapse) {
        now = now.plus(elapse);
    }
}
