package com.github.phoswald.rstm.template;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

class TemplateEngineTest {

    private final TemplateEngine testee = new TemplateEngine();

    @Test
    void compileAndExecute_validObject_success() {
        SampleArguments arguments = new SampleArguments("world", LocalDate.of(2023, 2, 16), "message");

        Function<SampleArguments, String> template = testee.compile(SampleArguments.class, "sample");
        String html = template.apply(arguments);
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

        Function<SampleArguments, String> template = testee.compile(SampleArguments.class, "sample");
        String html = template.apply(arguments);
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
    void compileAndExecute_validArrayObject_success() {
        SampleArrayArguments arguments = new SampleArrayArguments( //
                new SamplePair[] { new SamplePair("foo", "bar"), new SamplePair("bar", "baz") });

        Function<SampleArrayArguments, String> template = testee.compile(SampleArrayArguments.class, "sample-collection");
        String html = template.apply(arguments);
        assertThat(html, startsWith("<!doctype html>\n<html lang=\"en\">\n"));
        assertThat(html, containsString("\n    <ul>\n"));
        assertThat(html, containsString("<li><span>foo</span>=<span>bar</span></li>")); // TOOD fix indent
        assertThat(html, containsString("<li><span>bar</span>=<span>baz</span></li>"));
        assertThat(html, endsWith("\n</html>\n"));
        assertThat(html, not(containsString("???")));
    }

    @Test
    void compileAndExecute_validArrayObjectEmpty_success() {
        SampleArrayArguments arguments = new SampleArrayArguments( //
                new SamplePair[] { });

        Function<SampleArrayArguments, String> template = testee.compile(SampleArrayArguments.class, "sample-collection");
        String html = template.apply(arguments);
        assertThat(html, startsWith("<!doctype html>\n<html lang=\"en\">\n"));
        assertThat(html, containsString("\n    <ul>\n"));
        assertThat(html, endsWith("\n</html>\n"));
        assertThat(html, not(containsString("<li>")));
        assertThat(html, not(containsString("???")));
    }

    @Test
    void compileAndExecute_validListObject_success() {
        SampleListArguments arguments = new SampleListArguments( //
                Arrays.asList(new SamplePair("foo", "bar"), new SamplePair("bar", "baz")));

        Function<SampleListArguments, String> template = testee.compile(SampleListArguments.class, "sample-collection");
        String html = template.apply(arguments);
        assertThat(html, startsWith("<!doctype html>\n<html lang=\"en\">\n"));
        assertThat(html, containsString("\n    <ul>\n"));
        assertThat(html, containsString("<li><span>foo</span>=<span>bar</span></li>")); // TOOD fix indent
        assertThat(html, containsString("<li><span>bar</span>=<span>baz</span></li>"));
        assertThat(html, endsWith("\n</html>\n"));
        assertThat(html, not(containsString("???")));
    }

    @Test
    void compileAndExecute_validListObjectEmpty_success() {
        SampleListArguments arguments = new SampleListArguments(Collections.emptyList());

        Function<SampleListArguments, String> template = testee.compile(SampleListArguments.class, "sample-collection");
        String html = template.apply(arguments);
        assertThat(html, startsWith("<!doctype html>\n<html lang=\"en\">\n"));
        assertThat(html, containsString("\n    <ul>\n"));
        assertThat(html, endsWith("\n</html>\n"));
        assertThat(html, not(containsString("<li>")));
        assertThat(html, not(containsString("???")));
    }
}
