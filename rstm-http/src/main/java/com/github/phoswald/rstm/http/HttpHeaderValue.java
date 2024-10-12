package com.github.phoswald.rstm.http;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/*
 * Implements a subset of RFC 9110 (HTTP Semantics), section 5.6.6. (Parameters)
 * See: https://httpwg.org/specs/rfc9110.html#rule.parameter
 *
 * TODO (correctness): support quoted parameters in HTTP header values
 */
public record HttpHeaderValue(String valueOnly, Map<String, String> parameters) {

    public Optional<String> parameter(String name) {
        return Optional.ofNullable(parameters.get(name));
    }

    public static HttpHeaderValue parse(String value) {
        Map<String, String> parameters = new LinkedHashMap<>();
        String valueOnly = value;
        int posSeparator = value.indexOf(";");
        if (posSeparator != -1) {
            valueOnly = value.substring(0, posSeparator);
            int pos = posSeparator + 1;
            while (pos < value.length()) {
                int posAssignment = value.indexOf("=", pos);
                posSeparator = value.indexOf(";", pos);
                if (posSeparator == -1) {
                    posSeparator = value.length();
                }
                if (pos + 1 < posAssignment && posAssignment + 1 < posSeparator) {
                    parameters.put( //
                            value.substring(pos, posAssignment).trim(),
                            value.substring(posAssignment + 1, posSeparator).trim());
                }
                pos = posSeparator + 1;
            }
        }
        return new HttpHeaderValue(valueOnly.trim(), parameters);
    }
}
