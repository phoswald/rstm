package com.github.phoswald.rstm.security.oidc;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

class StateManager {

    private final Duration lifetime;
    private final Supplier<Instant> clock;
    private final RandomGenerator random;
    private final Map<String, State> states = new LinkedHashMap<String, State>();

    StateManager(Duration lifetime, Supplier<Instant> clock, RandomGenerator random) {
        this.lifetime = lifetime;
        this.clock = clock;
        this.random = random;
    }

    synchronized String create(Provider provider) {
        removeExpiredStates();
        String stateId = createState();
        states.put(stateId, new State(provider, clock.get().plus(lifetime)));
        return stateId;
    }

    synchronized State consume(String state) {
        removeExpiredStates();
        return states.remove(state);
    }

    private void removeExpiredStates() {
        new ArrayList<>(states.entrySet()).stream() // copy collection, cannot remove from stream
                .takeWhile(e -> e.getValue().expiry().isBefore(clock.get())) // iteration in chronological order
                .forEach(e -> states.remove(e.getKey()));
    }

    private String createState() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
