package com.github.phoswald.rstm.template;

import java.util.Map;
import java.util.regex.Pattern;

class HtmlGenerator {

    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z][a-z0-9-]*");

    private final StringBuilder buffer = new StringBuilder();

    String getOutput() {
        return buffer.toString();
    }

    void generateDocmentStart() {
        buffer.append("<!doctype html>\n");
    }

    void generateDocmentEnd() {
        buffer.append("\n");
    }

    void generateElementStart(String name, Map<String, String> attributes) {
        buffer.append("<");
        buffer.append(verifyElementName(name));
        for(Map.Entry<String, String> e : attributes.entrySet()) {
            String attribute = e.getKey();
            String value = e.getValue();
            buffer.append(" ");
            buffer.append(verifyAttributeName(attribute));
            buffer.append("=\"");
            if(value != null) {
                for(int i = 0; i < value.length(); i++) {
                    char c = value.charAt(i);
                    switch(c) {
                        case '<': buffer.append("&lt;"); break;
                        case '>': buffer.append("&gt;"); break;
                        case '&': buffer.append("&amp;"); break;
                        case '"': buffer.append("&quot;"); break;
                        default: buffer.append(c);
                    }
                }
            }
            buffer.append("\"");
        }
        buffer.append(">");
    }

    void generateElementEnd(String name) {
        buffer.append("</");
        buffer.append(verifyElementName(name));
        buffer.append(">");
    }

    private String verifyElementName(String name) {
        if(name == null || !NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid element name: " + name);
        }
        return name;
    }

    private String verifyAttributeName(String name) {
        if(name == null || !NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid attribute name: " + name);
        }
        return name;
    }

    void generateText(String text) {
        if(text != null) {
            for(int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                switch(c) {
                    case '<': buffer.append("&lt;"); break;
                    case '>': buffer.append("&gt;"); break;
                    case '&': buffer.append("&amp;"); break;
                    default: buffer.append(c);
                }
            }
        }
    }

    void generateComment(String comment) {
        buffer.append("<!--");
        if(comment != null) {
            if(comment.contains("--")) {
                throw new IllegalArgumentException("Invalid comment");
            }
            buffer.append(comment);
        }
        buffer.append("-->");
    }
}
