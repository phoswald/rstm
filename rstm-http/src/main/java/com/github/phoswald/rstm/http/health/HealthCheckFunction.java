package com.github.phoswald.rstm.http.health;

@FunctionalInterface
public interface HealthCheckFunction {

    boolean invoke() throws Exception;
}
