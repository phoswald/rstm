package com.github.phoswald.rstm.template;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class HtmlUtilTest {

    private final HtmlUtil testee = new HtmlUtil();
    private final StringBuilder buffer = new StringBuilder();

    @ParameterizedTest
    @ValueSource(strings = { "p", "h1", "some-other" })
    void generateElement_validName_success(String input) {
        testee.generateElementStart(buffer, input, emptyMap());
        testee.generateElementEnd(buffer, input);
        assertEquals("<" + input + "></" + input + ">", buffer.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = { "P", "H1", "1", "ö", "-" })
    @NullSource
    void generateElement_invalidName_success(String input) {
        assertThrows(IllegalArgumentException.class, () -> testee.generateElementStart(buffer, input, emptyMap()));
        assertThrows(IllegalArgumentException.class, () -> testee.generateElementEnd(buffer, input));
    }

    @ParameterizedTest
    @ValueSource(strings = { "x", "href", "some-other" })
    void generateElement_validAttributeName_success(String input) {
        testee.generateElementStart(buffer, "p", singletonMap(input, "text"));
        testee.generateElementEnd(buffer, "p");
        assertEquals("<p " + input + "=\"text\"></p>", buffer.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = { "X", "HREF", "1", "ö", "-" })
    @NullSource
    void generateElement_invalidAttributeName_success(String input) {
        assertThrows(IllegalArgumentException.class, () -> testee.generateElementStart(buffer, "p", singletonMap(input, "text")));
    }

    @ParameterizedTest
    @ValueSource(strings = { "text", " text ", "\ntext\n" })
    @NullSource
    void generateElement_validAttributeValue_success(String input) {
        testee.generateElementStart(buffer, "a", singletonMap("href", input));
        testee.generateElementEnd(buffer, "a");
        assertEquals("<a href=\"" + (input == null ? "" : input) + "\"></a>", buffer.toString());
    }

    @Test
    void generateAttribute_validAttributeValueSpecial_successEscaped() {
        testee.generateElementStart(buffer, "a", singletonMap("href", "a < b > c & d \" e ' f"));
        testee.generateElementEnd(buffer, "a");
        assertEquals("<a href=\"a &lt; b &gt; c &amp; d &quot; e ' f\"></a>", buffer.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = { "text", " text ", "\ntext\n" })
    @NullSource
    void generateText_valid_success(String input) {
        testee.generateText(buffer, input);
        assertEquals(input == null ? "" : input, buffer.toString());
    }

    @Test
    void generateText_validSpecial_successEscaped() {
        testee.generateText(buffer, "a < b > c & d \" e ' f");
        assertEquals("a &lt; b &gt; c &amp; d \" e ' f", buffer.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = { "comment", " comment ", " multi \n line ", "<!- -", "- ->", "" })
    @NullSource
    void generateComment_valid_success(String input) {
        testee.generateComment(buffer, input);
        assertEquals("<!--" + (input == null ? "" : input) + "-->", buffer.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = { "--", "<!--", "-->" })
    void generateComment_invalid_exception(String input) {
        assertThrows(IllegalArgumentException.class, () -> testee.generateComment(buffer, input));
    }
}
