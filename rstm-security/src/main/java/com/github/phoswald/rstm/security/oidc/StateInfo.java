package com.github.phoswald.rstm.security.oidc;

import com.github.phoswald.rstm.security.jwt.JwtKeySet;

record StateInfo(ProviderInfo providerInfo, ConfigurationResponse config, JwtKeySet keySet) { }
