package com.github.phoswald.rstm.security;

import java.util.List;
import java.util.Optional;

/**
 * Interface for locally generating and verifying authentication tokens.
 *
 * This interface is not suitable for federated IDPs.
 */
public interface TokenProvider {

    Principal createPrincipal(String username, List<String> roles);

    Optional<Principal> authenticateWithToken(String token);
}
