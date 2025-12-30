package com.github.phoswald.rstm.http.health;

public interface HealthCheckFunction {

    boolean invoke() throws Exception;
}
