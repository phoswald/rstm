package com.github.phoswald.rstm.template;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TemplateEngineTest {

    private final TemplateEngine testee = new TemplateEngine();

    @Test
    void compileAndExecute_validObject_success() {
        SampleArguments arguments = new SampleArguments("world", LocalDate.of(2023, 2, 16), "message");

        Template<SampleArguments> template = testee.compile(SampleArguments.class, "sample");
        String html = template.evaluate(arguments);

        assertThat(html, startsWith("<!doctype html>\n<html lang=\"en\">\n"));
        assertThat(html, containsString("\n    <title>Sample Page</title>\n"));
        assertThat(html, containsString("\n    <h1>Hello, <span>world</span>!</h1>\n"));
        assertThat(html, containsString("\n    <p>The current date is <span>2023-02-16</span>.</p>\n"));
        assertThat(html, containsString("\n    <p>Optional: <span>message</span>.</p>\n"));
        assertThat(html, containsString("\n    <!-- comment -->\n"));
        assertThat(html, endsWith("\n</html>\n"));
        assertThat(html, not(containsString("???")));
    }

    @Test
    void compileAndExecute_validObjectEmpty_success() {
        SampleArguments arguments = new SampleArguments("", null, null);

        Template<SampleArguments> template = testee.compile(SampleArguments.class, "sample");
        String html = template.evaluate(arguments);

        assertThat(html, startsWith("<!doctype html>\n<html lang=\"en\">\n"));
        assertThat(html, containsString("\n    <title>Sample Page</title>\n"));
        assertThat(html, containsString("\n    <h1>Hello, <span></span>!</h1>\n"));
        assertThat(html, containsString("\n    <p>The current date is <span></span>.</p>\n"));
        assertThat(html, containsString("\n    <!-- comment -->\n"));
        assertThat(html, endsWith("\n</html>\n"));
        assertThat(html, not(containsString("<p>Optional:")));
        assertThat(html, not(containsString("???")));
    }

    @Test
    void compileAndExecute_validObjectWithLocale_success() {
        SampleArguments arguments = new SampleArguments("", null, null);

        Template<SampleArguments> template = testee.compile(SampleArguments.class, "sample");
        String html = template.evaluate(arguments, Locale.GERMANY);

        assertThat(html, startsWith("<!doctype html>\n<html lang=\"en\">\n"));
        assertThat(html, containsString("\n    <title>Beispiel Seite</title>\n"));
        assertThat(html, endsWith("\n</html>\n"));
        assertThat(html, not(containsString("???")));
    }

    @Test
    void compileAndExecute_validArrayObject_success() {
        SampleArrayArguments arguments = new SampleArrayArguments( //
                new SamplePair[] { new SamplePair("foo", "bar"), new SamplePair("bar", "baz") });

        Template<SampleArrayArguments> template = testee.compile(SampleArrayArguments.class, "sample-collection");
        String html = template.evaluate(arguments);

        assertHtmlCollection(html);
    }

    @Test
    void compileAndExecute_validArrayObjectEmpty_success() {
        SampleArrayArguments arguments = new SampleArrayArguments(null);

        Template<SampleArrayArguments> template = testee.compile(SampleArrayArguments.class, "sample-collection");
        String html = template.evaluate(arguments);

        assertHtmlCollectionEmpty(html);
    }

    @Test
    void compileAndExecute_validListObject_success() {
        SampleListArguments arguments = new SampleListArguments( //
                List.of(new SamplePair("foo", "bar"), new SamplePair("bar", "baz")));

        Template<SampleListArguments> template = testee.compile(SampleListArguments.class, "sample-collection");
        String html = template.evaluate(arguments);

        assertHtmlCollection(html);
    }

    @Test
    void compileAndExecute_validListObjectEmpty_success() {
        SampleListArguments arguments = new SampleListArguments(null);

        Template<SampleListArguments> template = testee.compile(SampleListArguments.class, "sample-collection");
        String html = template.evaluate(arguments);

        assertHtmlCollectionEmpty(html);
    }

    @Test
    void compileAndExecute_validMapString_success() {
        Map<String, String> collection = new LinkedHashMap<>();
        collection.put("foo", "bar");
        collection.put("bar", "baz");
        SampleMapArguments arguments = new SampleMapArguments(collection);

        Template<SampleMapArguments> template = testee.compile(SampleMapArguments.class, "sample-collection");
        String html = template.evaluate(arguments);

        assertHtmlCollection(html);
    }

    @Test
    void compileAndExecute_validMapEmpty_success() {
        SampleMapArguments arguments = new SampleMapArguments(null);

        Template<SampleMapArguments> template = testee.compile(SampleMapArguments.class, "sample-collection");
        String html = template.evaluate(arguments);

        assertHtmlCollectionEmpty(html);
    }

    private void assertHtmlCollection(String html) {
        assertThat(html, startsWith("<!doctype html>\n<html lang=\"en\">\n"));
        assertThat(html, containsString("\n    <ul>\n"));
        assertThat(html, containsString("<li><span>foo</span>=<span>bar</span></li>")); // TOOD fix indent
        assertThat(html, containsString("<li><span>bar</span>=<span>baz</span></li>"));
        assertThat(html, endsWith("\n</html>\n"));
        assertThat(html, not(containsString("???")));
    }

    private void assertHtmlCollectionEmpty(String html) {
        assertThat(html, startsWith("<!doctype html>\n<html lang=\"en\">\n"));
        assertThat(html, containsString("\n    <ul>\n"));
        assertThat(html, endsWith("\n</html>\n"));
        assertThat(html, not(containsString("<li>")));
        assertThat(html, not(containsString("???")));
    }
}
