package com.github.phoswald.rstm.template;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateEngine {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<String, ResourceBundle> resourcesMap = new ConcurrentHashMap<>();

    public <T> Template<T> compile(Class<T> argumentClass, String templateName) {
        logger.info("Compiling template '{}' for {}.", templateName, argumentClass);
        String xhtml = loadTemplate(templateName);
        ResourceBundle resources = findResourceBundle(templateName, Locale.ROOT);
        Template<T> template = parseTemplate(argumentClass, templateName, xhtml, resources);
        return template;
    }

    private String loadTemplate(String templateName) {
        try (InputStream input = getClass().getResourceAsStream("/templates/" + templateName + ".xhtml")) {
            StringWriter buffer = new StringWriter();
            new InputStreamReader(input, StandardCharsets.UTF_8).transferTo(buffer);
            return buffer.toString();

        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load XMTHL template " + templateName, e);
        }
    }

    ResourceBundle findResourceBundle(String templateName, Locale locale) {
        return resourcesMap.computeIfAbsent(templateName + ":" + locale, k -> loadResourceBundle(templateName, locale));
    }

    private ResourceBundle loadResourceBundle(String templateName, Locale locale) {
        try {
            return ResourceBundle.getBundle("templates." + templateName, locale);
        } catch (MissingResourceException e) {
            try {
                return new PropertyResourceBundle(new StringReader(""));
            } catch (IOException e2) {
                throw new UncheckedIOException(e2);
            }
        }
    }

    private <T> TemplateInstance<T> parseTemplate(Class<T> argumentClass, String templateName, String xhtml, ResourceBundle resources) {
        try {
            TemplateCompilation<?> compilation = new TemplateCompilation<>(argumentClass, resources);
            XHtmlParser parser = new XHtmlParser(xhtml);
            return new TemplateInstance<>(this, templateName, argumentClass, parser.parseDocument(compilation));

        } catch (XMLStreamException e) {
            throw new IllegalArgumentException("Failed to parse XHTML template " + templateName, e);
        }
    }

    <T> String evaluateTemplate(TemplateInstance<T> template, TemplateArgument<T> argument) {
        logger.debug("Evaluating template '{}' for {} and {}.", template.name(), template.argumentClass(), argument.locale());

        HtmlGenerator generator = new HtmlGenerator();
        template.documentNode().evaluateNode(generator, argument);
        return generator.getOutput();
    }
}
