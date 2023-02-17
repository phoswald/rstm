package com.github.phoswald.rstm.template;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import java.time.LocalDate;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

class TemplateEngineTest {

    private final TemplateEngine testee = new TemplateEngine();

    @Test
    void compileAndExecute_valid_success() {
        SampleArguments arguments = new SampleArguments("world", LocalDate.of(2023, 2, 16), "message", //
                new SamplePair[] { new SamplePair("foo", "bar"), new SamplePair("bar", "baz") });

        Function<SampleArguments, String> template = testee.compile(SampleArguments.class, "sample");
        String html = template.apply(arguments);
        assertThat(html, startsWith("<!doctype html>\n<html lang=\"en\">\n"));
        assertThat(html, containsString("    <title>Sample Page</title>\n"));
        assertThat(html, containsString("    <h1>Hello, <span>world</span>!</h1>\n"));
        assertThat(html, containsString("    <p>The current date is <span>2023-02-16</span>.</p>\n"));
        assertThat(html, containsString("    <p>Optional: <span>message</span>.</p>\n"));
        assertThat(html, containsString("<li><span>foo</span>=<span>bar</span></li>")); // TOOD fix indent
        assertThat(html, containsString("<li><span>bar</span>=<span>baz</span></li>"));
        assertThat(html, containsString("    <!-- comment -->\n"));
        assertThat(html, endsWith("</html>\n"));
        assertThat(html, not(containsString("???")));
    }

    @Test
    void compileAndExecute_validEmpty_success() {
        SampleArguments arguments = new SampleArguments("", null, null, null);

        Function<SampleArguments, String> template = testee.compile(SampleArguments.class, "sample");
        String html = template.apply(arguments);
        assertThat(html, startsWith("<!doctype html>\n<html lang=\"en\">\n"));
        assertThat(html, containsString("    <title>Sample Page</title>\n"));
        assertThat(html, containsString("    <h1>Hello, <span></span>!</h1>\n"));
        assertThat(html, containsString("    <p>The current date is <span></span>.</p>\n"));
        assertThat(html, containsString("    <!-- comment -->\n"));
        assertThat(html, endsWith("</html>\n"));
        assertThat(html, not(containsString("<p>Optional:")));
        assertThat(html, not(containsString("<li>")));
        assertThat(html, not(containsString("???")));
    }
}
