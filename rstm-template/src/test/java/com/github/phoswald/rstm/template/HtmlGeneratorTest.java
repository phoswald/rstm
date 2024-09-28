package com.github.phoswald.rstm.template;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class HtmlGeneratorTest {

    private final HtmlGenerator testee = new HtmlGenerator();

    @ParameterizedTest
    @ValueSource(strings = { "p", "h1", "some-other" })
    void generateElement_validName_success(String input) {
        testee.generateElementStart(input, Map.of());
        testee.generateElementEnd(input);
        assertEquals("<" + input + "></" + input + ">", testee.getOutput());
    }

    @ParameterizedTest
    @ValueSource(strings = { "P", "H1", "1", "ö", "-" })
    @NullSource
    void generateElement_invalidName_success(String input) {
        assertThrows(IllegalArgumentException.class, () -> testee.generateElementStart(input, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> testee.generateElementEnd(input));
    }

    @ParameterizedTest
    @ValueSource(strings = { "x", "href", "some-other" })
    void generateElement_validAttributeName_success(String input) {
        testee.generateElementStart("p", Map.of(input, "text"));
        testee.generateElementEnd("p");
        assertEquals("<p " + input + "=\"text\"></p>", testee.getOutput());
    }

    @ParameterizedTest
    @ValueSource(strings = { "X", "HREF", "1", "ö", "-" })
    @NullSource
    void generateElement_invalidAttributeName_success(String input) {
        assertThrows(IllegalArgumentException.class, () -> testee.generateElementStart("p", singletonMap(input, "text"))); // Cannot use Map.of() for null value
    }

    @ParameterizedTest
    @ValueSource(strings = { "text", " text ", "\ntext\n" })
    @NullSource
    void generateElement_validAttributeValue_success(String input) {
        testee.generateElementStart("a", singletonMap("href", input)); // Cannot use Map.of() for null value
        testee.generateElementEnd("a");
        assertEquals("<a href=\"" + (input == null ? "" : input) + "\"></a>", testee.getOutput());
    }

    @Test
    void generateAttribute_validAttributeValueSpecial_successEscaped() {
        testee.generateElementStart("a", Map.of("href", "a < b > c & d \" e ' f"));
        testee.generateElementEnd("a");
        assertEquals("<a href=\"a &lt; b &gt; c &amp; d &quot; e ' f\"></a>", testee.getOutput());
    }

    @ParameterizedTest
    @ValueSource(strings = { "text", " text ", "\ntext\n" })
    @NullSource
    void generateText_valid_success(String input) {
        testee.generateText(input);
        assertEquals(input == null ? "" : input, testee.getOutput());
    }

    @Test
    void generateText_validSpecial_successEscaped() {
        testee.generateText("a < b > c & d \" e ' f");
        assertEquals("a &lt; b &gt; c &amp; d \" e ' f", testee.getOutput());
    }

    @ParameterizedTest
    @ValueSource(strings = { "comment", " comment ", " multi \n line ", "<!- -", "- ->", "" })
    @NullSource
    void generateComment_valid_success(String input) {
        testee.generateComment(input);
        assertEquals("<!--" + (input == null ? "" : input) + "-->", testee.getOutput());
    }

    @ParameterizedTest
    @ValueSource(strings = { "--", "<!--", "-->" })
    void generateComment_invalid_exception(String input) {
        assertThrows(IllegalArgumentException.class, () -> testee.generateComment(input));
    }
}
