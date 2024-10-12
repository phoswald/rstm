package com.github.phoswald.rstm.security.oidc;

import java.time.Instant;

record State(Provider provider, Instant expiry) { }
