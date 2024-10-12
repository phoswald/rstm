package com.github.phoswald.rstm.security.oidc;

import com.github.phoswald.rstm.security.jwt.JwtKeySet;

record State(Provider provider, Configuration config, JwtKeySet keySet) { }
