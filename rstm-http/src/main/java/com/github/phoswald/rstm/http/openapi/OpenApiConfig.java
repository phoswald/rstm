package com.github.phoswald.rstm.http.openapi;

import java.util.List;

import com.github.phoswald.record.builder.RecordBuilder;

@RecordBuilder
public record OpenApiConfig(
        String title,
        String description,
        String version,
        List<String>urls
) {

    public static OpenApiConfigBuilder builder() {
        return new OpenApiConfigBuilder();
    }
}
