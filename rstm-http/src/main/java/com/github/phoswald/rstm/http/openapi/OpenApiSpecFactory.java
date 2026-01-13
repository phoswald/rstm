package com.github.phoswald.rstm.http.openapi;

import static com.github.phoswald.rstm.http.HttpConstants.CONTENT_TYPE_JSON;
import static com.github.phoswald.rstm.http.HttpConstants.CONTENT_TYPE_XML;
import static java.util.function.UnaryOperator.identity;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.phoswald.rstm.databind.Databinder;
import com.github.phoswald.rstm.databind.FieldMetadata;
import com.github.phoswald.rstm.databind.Kind;
import com.github.phoswald.rstm.http.HttpMethod;
import com.github.phoswald.rstm.http.server.HttpFilter;
import com.github.phoswald.rstm.http.server.RouteMetadata;

class OpenApiSpecFactory {

    private static final String SPEC_VERSION = "3.0.0";
    private static final Databinder BINDER = new Databinder();

    private final OpenApiConfig config;
    private final List<RouteMetadata> routeMetadata;
    private final Set<Class<?>> schemaClasses;
    private final Set<Class<?>> schemaClassesXml;

    OpenApiSpecFactory(OpenApiConfig config, HttpFilter filter) {
        this.config = config;
        this.routeMetadata = filter.createMetadata();
        this.schemaClasses = findSchemaClasses(List.of(CONTENT_TYPE_JSON, CONTENT_TYPE_XML));
        this.schemaClassesXml = findSchemaClasses(List.of(CONTENT_TYPE_XML));
    }

    String generateOpenApiSpecJson() {
        return BINDER.toJson(generateOpenApiSpec());
    }

    OpenApiSpec generateOpenApiSpec() {
        return new OpenApiSpecBuilder()
                .openapi(SPEC_VERSION)
                .info(new OpenApiInfo(config.title(), config.description(), config.version()))
                .servers(config.urls().stream().map(OpenApiServer::new).toList())
                .security(List.of(
                        new OpenApiSecurityBuilder().BasicAuth(List.of()).build(),
                        new OpenApiSecurityBuilder().BearerAuth(List.of()).build()))
                .paths(findPaths().stream().collect(toUniqueOrderedMap(identity(), this::createPath)))
                .components(new OpenApiComponentsBuilder()
                        .securitySchemes(new OpenApiSecuritySchemesBuilder()
                                .BasicAuth(new OpenApiSecurityScheme("http", "basic"))
                                .BearerAuth(new OpenApiSecurityScheme("http", "bearer"))
                                .build())
                        .schemas(schemaClasses.stream().collect(toUniqueOrderedMap(Class::getSimpleName, this::createSchema)))
                        .build())
                .build();
    }

    private List<String> findPaths() {
        return routeMetadata.stream()
                .map(RouteMetadata::route)
                .distinct()
                .toList();
    }

    private OpenApiPath createPath(String path) {
        return new OpenApiPathBuilder()
                .get(createOperation(path, HttpMethod.GET))
                .post(createOperation(path, HttpMethod.POST))
                .put(createOperation(path, HttpMethod.PUT))
                .delete(createOperation(path, HttpMethod.DELETE))
                .build();
    }

    private OpenApiOperation createOperation(String path, HttpMethod method) {
        return routeMetadata.stream()
                .filter(metadata -> metadata.route().equals(path))
                .filter(metadata -> metadata.method() == method)
                .findFirst()
                .map(this::createOperation)
                .orElse(null);
    }

    private OpenApiOperation createOperation(RouteMetadata metadata) {
        List<OpenApiParameter> parameters = null;
        if(!metadata.pathParams().isEmpty()) {
            parameters = metadata.pathParams().stream()
                    .map(this::createParameter)
                    .toList();
        }
        OpenApiRequestBody requestBody = null;
        if(metadata.requestClass() != null) {
            requestBody = new OpenApiRequestBodyBuilder()
                    .content(Map.of(metadata.contentType(), createContent(metadata.contentType(), metadata.requestClass())))
                    .build();
        }
        String status = "200";
        OpenApiResponseBody responseBody;
        if(metadata.responseClass() == String.class && isJsonOrXml(metadata.contentType())) {
            // special case: JSON or XML with String indicates no content at all; but text/plain or text/html with String indicates textual content!
            status = "204";
            responseBody = new OpenApiResponseBodyBuilder()
                    .description("((mandatory description))")
                    .build();
        } else if(metadata.responseClass() != null) {
            responseBody = new OpenApiResponseBodyBuilder()
                    .description("((mandatory description))")
                    .content(Map.of(metadata.contentType(), createContent(metadata.contentType(), metadata.responseClass())))
                    .build();
        } else {
            responseBody = new OpenApiResponseBodyBuilder()
                    .description("((mandatory description))")
                    .build();
        }
        return new OpenApiOperationBuilder()
                .description("((mandatory description))")
                .parameters(parameters)
                .requestBody(requestBody)
                .responses(Map.of(status, responseBody))
                .build();
    }

    private OpenApiParameter createParameter(String name) {
        return new OpenApiParameterBuilder()
                .name(name)
                .in("path")
                .required(true)
                .schema(createSchemaSimple(String.class))
                .build();
    }

    private OpenApiContent createContent(String contentType, Class<?> clazz) {
        if(isJsonOrXml(contentType) && clazz.isRecord()) {
            return new OpenApiContent(createSchemaReference(clazz));
        } else {
            return new OpenApiContent(null);
        }
    }

    private Set<Class<?>> findSchemaClasses(List<String> contentTypes) {
        Set<Class<?>> result = new LinkedHashSet<>();
        routeMetadata.stream()
                .filter(metadata -> metadata.contentType() != null && contentTypes.contains(metadata.contentType()))
                .flatMap(metadata -> Stream.of(metadata.requestClass(), metadata.responseClass()))
                .filter(clazz -> clazz != null && clazz.isRecord())
                .forEach(clazz -> findSchemaClasses(result, clazz));
        return result;
    }

    private void findSchemaClasses(Set<Class<?>> result, Class<?> current) {
        if(result.add(current)) {
            Map<String, FieldMetadata> fields = BINDER.createMetadata(current);
            fields.values().stream()
                    .filter(field -> field.clazz().isRecord()) // kind can be RECORD, LIST, MAP
                    .forEach(field -> findSchemaClasses(result, field.clazz()));
        }
    }

    private OpenApiSchema createSchemaReference(Class<?> clazz) {
        return new OpenApiSchemaBuilder().$ref("#/components/schemas/" + clazz.getSimpleName()).build();
    }

    private OpenApiSchema createSchema(Class<?> clazz) {
        Map<String, FieldMetadata> fields = BINDER.createMetadata(clazz);
        return new OpenApiSchemaBuilder()
                .type("object")
                .properties(fields.values().stream().collect(toUniqueOrderedMap(FieldMetadata::name, this::createSchemaField)))
                .xml(schemaClassesXml.contains(clazz) ? new OpenApiSchemaXml(getXmlElementName(clazz)) : null)
                .build();
    }

    private OpenApiSchema createSchemaField(FieldMetadata field) {
        return switch(field.kind()) {
            case Kind.PRIMITIVE, Kind.SIMPLE -> createSchemaSimple(field.clazz());
            case Kind.RECORD -> createSchemaReference(field.clazz());
            case Kind.LIST -> new OpenApiSchemaBuilder()
                    .type("array")
                    .items(createSchemaReference(field.clazz()))
                    .build();
            case Kind.MAP -> throw new IllegalArgumentException("Unsupported field type: Map of " + field.clazz());
        };
    }

    private OpenApiSchema createSchemaSimple(Class<?> clazz) {
        String type = null;
        if(clazz == Boolean.class || clazz == boolean.class) {
            type = "boolean";
        } else if(clazz == Integer.class || clazz == int.class || clazz == Long.class || clazz == long.class) {
            type = "integer";
        } else if(clazz == Float.class || clazz == float.class || clazz == Double.class || clazz == double.class) {
            type = "integer";
        } else if(clazz == String.class || clazz == Instant.class) {
            type = "string";
        }
        return new OpenApiSchemaBuilder().type(type).build();
    }

    private String getXmlElementName(Class<?> clazz) {
        String name = clazz.getSimpleName();
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

    private boolean isJsonOrXml(String contentType) {
        return CONTENT_TYPE_JSON.equals(contentType) || CONTENT_TYPE_XML.equals(contentType);
    }

    private static <T, K, V> Collector<T, ?, Map<K, V>> toUniqueOrderedMap(Function<T, K> keyExtractor, Function<T, V> valueExtractor) {
        BinaryOperator<V> mergeFunction = (_, _) -> {
            throw new UnsupportedOperationException();
        };
        return Collectors.toMap(keyExtractor, valueExtractor, mergeFunction, LinkedHashMap::new);
    }
}
