package com.github.phoswald.rstm.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

class ConfigProviderTest {

    private final ConfigProvider testee = new ConfigProvider();

    @Test
    void getConfigProperty_environmentVariable_success() {
        assertThat(testee.getConfigProperty("path").get(), containsString("/usr/bin"));
    }

    @Test
    void getConfigProperty_systemProperty_success() {
        assertThat(testee.getConfigProperty("java.runtime.name").get(), equalTo("OpenJDK Runtime Environment"));
    }
}
