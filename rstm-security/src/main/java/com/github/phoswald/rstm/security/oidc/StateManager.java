package com.github.phoswald.rstm.security.oidc;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.phoswald.rstm.security.jwt.JwtKeySet;

class StateManager {

    private final SecureRandom random = new SecureRandom();
    private final Map<String, State> states = new ConcurrentHashMap<>();

    String create(Provider provider, Configuration config, JwtKeySet keySet) {
        // TODO (optimize): evict expired items
        String stateId = createState();
        states.put(stateId, new State(provider, config, keySet));
        return stateId;
    }

    State consume(String state) {
        // TODO (correctness): check expired items
        return states.remove(state);
    }

    private String createState() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
