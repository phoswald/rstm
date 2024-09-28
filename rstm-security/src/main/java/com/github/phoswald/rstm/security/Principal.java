package com.github.phoswald.rstm.security;

import java.util.List;

public record Principal(String name, List<String> roles, String token) { }
