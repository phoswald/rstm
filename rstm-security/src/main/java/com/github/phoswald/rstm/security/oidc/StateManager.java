package com.github.phoswald.rstm.security.oidc;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.phoswald.rstm.security.jwt.JwtKeySet;

class StateManager {

    private final SecureRandom random = new SecureRandom();
    private final Map<String, StateInfo> states = new ConcurrentHashMap<>();

    String create(ProviderInfo providerInfo, ConfigurationResponse config, JwtKeySet keySet) {
        // TODO: evict expired items
        String state = createState();
        states.put(state, new StateInfo(providerInfo, config, keySet));
        return state;
    }

    StateInfo consume(String state) {
        return states.remove(state);
    }

    private String createState() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
