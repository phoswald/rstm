package com.github.phoswald.rstm.http.openapi;

import java.util.List;
import java.util.Map;

import com.github.phoswald.record.builder.RecordBuilder;

@RecordBuilder
record OpenApiSpec(
        String openapi,
        OpenApiInfo info,
        List<OpenApiServer> servers,
        List<OpenApiSecurity> security,
        Map<String, OpenApiPath> paths,
        OpenApiComponents components
) { }

record OpenApiInfo(String title, String description, String version) { }

record OpenApiServer(String url) { }

// this should actually be a map
@RecordBuilder
record OpenApiSecurity(
        List<String> BasicAuth,
        List<String> BearerAuth
) { }

@RecordBuilder
record OpenApiPath(
        OpenApiOperation get,
        OpenApiOperation post,
        OpenApiOperation put,
        OpenApiOperation delete
) { }

@RecordBuilder
record OpenApiOperation(
        String description,
        List<OpenApiParameter> parameters,
        OpenApiRequestBody requestBody,
        Map<String, OpenApiResponseBody> responses
) { }

@RecordBuilder
record OpenApiParameter(
        String name,
        String in,
        boolean required,
        OpenApiSchema schema
) { }

@RecordBuilder
record OpenApiRequestBody(
        Map<String, OpenApiContent> content
) { }

@RecordBuilder
record OpenApiResponseBody(
        String description,
        Map<String, OpenApiContent> content
) { }

record OpenApiContent(OpenApiSchema schema) { }

@RecordBuilder
record OpenApiComponents(
        OpenApiSecuritySchemes securitySchemes,
        Map<String, OpenApiSchema> schemas
) { }

// this should actually be a map
@RecordBuilder
record OpenApiSecuritySchemes(
        OpenApiSecurityScheme BasicAuth,
        OpenApiSecurityScheme BearerAuth
) { }

record OpenApiSecurityScheme(String type, String scheme) { }

@RecordBuilder
record OpenApiSchema(
        String $ref,
        String type,
        Map<String, OpenApiSchema> properties,
        OpenApiSchema items,
        OpenApiSchemaXml xml
) { }

record OpenApiSchemaXml(String name) { }
