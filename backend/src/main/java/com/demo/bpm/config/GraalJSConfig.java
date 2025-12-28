package com.demo.bpm.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.List;

/**
 * Configuration for GraalVM JavaScript engine.
 *
 * Required for Java 17+ since Nashorn was removed in Java 15.
 * This enables JavaScript scripting in Flowable BPMN processes.
 *
 * Note: The GraalVM polyglot properties are set in BpmApplication's static block
 * to ensure they're configured before any Spring beans are initialized.
 */
@Slf4j
@Configuration
@Order(1) // Ensure this runs early
public class GraalJSConfig {

    private static final String[] JS_ENGINE_NAMES = {
        "javascript", "JavaScript", "js", "JS", "graal.js", "Graal.js", "GraalJS"
    };

    @PostConstruct
    public void verifyAndConfigureGraalJS() {
        log.info("Verifying GraalVM JavaScript engine availability...");

        // Use thread context classloader to ensure proper resolution in Spring Boot
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ScriptEngineManager manager = new ScriptEngineManager(contextClassLoader);

        // Log all available script engines
        logAvailableEngines(manager);

        // Try to find JavaScript engine with multiple name attempts
        ScriptEngine engine = findJavaScriptEngine(manager);

        if (engine != null) {
            log.info("GraalVM JavaScript engine successfully loaded!");
            log.info("  Engine Name: {}", engine.getFactory().getEngineName());
            log.info("  Engine Version: {}", engine.getFactory().getEngineVersion());
            log.info("  Language: {} v{}",
                engine.getFactory().getLanguageName(),
                engine.getFactory().getLanguageVersion());

            // Test the engine with a simple script
            testEngineWithScript(engine);
        } else {
            log.error("========================================");
            log.error("CRITICAL: JavaScript engine not found!");
            log.error("BPMN script tasks will fail at runtime.");
            log.error("========================================");
            log.error("Troubleshooting steps:");
            log.error("1. Ensure org.graalvm.polyglot:polyglot is in dependencies");
            log.error("2. Ensure org.graalvm.polyglot:js-community is in dependencies");
            log.error("3. Ensure org.graalvm.js:js-scriptengine is in dependencies");
            log.error("4. Check that all dependencies use the same version (23.0.0)");
        }
    }

    private void logAvailableEngines(ScriptEngineManager manager) {
        List<ScriptEngineFactory> factories = manager.getEngineFactories();
        if (factories.isEmpty()) {
            log.warn("No script engines found in ScriptEngineManager!");
        } else {
            log.info("Available script engines ({}):", factories.size());
            for (ScriptEngineFactory factory : factories) {
                log.info("  - {} v{} [names: {}, extensions: {}]",
                    factory.getEngineName(),
                    factory.getEngineVersion(),
                    factory.getNames(),
                    factory.getExtensions());
            }
        }
    }

    private ScriptEngine findJavaScriptEngine(ScriptEngineManager manager) {
        for (String name : JS_ENGINE_NAMES) {
            ScriptEngine engine = manager.getEngineByName(name);
            if (engine != null) {
                log.info("Found JavaScript engine using name: '{}'", name);

                // Ensure 'javascript' name is registered if it wasn't
                if (!"javascript".equals(name)) {
                    log.info("Registering engine under 'javascript' name");
                    manager.registerEngineName("javascript", engine.getFactory());
                }

                return engine;
            }
        }

        // Try by MIME type
        ScriptEngine engine = manager.getEngineByMimeType("application/javascript");
        if (engine != null) {
            log.info("Found JavaScript engine using MIME type");
            return engine;
        }

        // Try by extension
        engine = manager.getEngineByExtension("js");
        if (engine != null) {
            log.info("Found JavaScript engine using extension");
            return engine;
        }

        return null;
    }

    private void testEngineWithScript(ScriptEngine engine) {
        try {
            // Test basic JavaScript execution
            Object result = engine.eval("1 + 1");
            log.info("JavaScript engine test (1+1): {}", result);

            // Test Java interop (critical for Flowable)
            engine.put("testVar", 42);
            Object retrieved = engine.eval("testVar");
            log.info("JavaScript engine Java interop test: {}", retrieved);

            log.info("JavaScript engine is fully operational!");
        } catch (ScriptException e) {
            log.error("JavaScript engine test failed: {}", e.getMessage());
            log.error("Script tasks may not work correctly", e);
        }
    }
}
