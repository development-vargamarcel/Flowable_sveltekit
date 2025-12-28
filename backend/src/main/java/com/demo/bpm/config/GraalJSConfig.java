package com.demo.bpm.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
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

        try {
            // Use thread context classloader to ensure proper resolution in Spring Boot
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            ScriptEngineManager manager = new ScriptEngineManager(contextClassLoader);

            // Log all available script engines
            logAvailableEngines(manager);

            // Try to find JavaScript engine with multiple name attempts
            ScriptEngine engine = findJavaScriptEngine(manager);

            if (engine != null) {
                log.info("GraalVM JavaScript engine successfully loaded!");
                try {
                    log.info("  Engine Name: {}", engine.getFactory().getEngineName());
                    log.info("  Engine Version: {}", engine.getFactory().getEngineVersion());
                    log.info("  Language: {} v{}",
                        engine.getFactory().getLanguageName(),
                        engine.getFactory().getLanguageVersion());
                } catch (Exception e) {
                    log.warn("Could not retrieve engine details: {}", e.getMessage());
                }

                // Test the engine with a simple script
                testEngineWithScript(engine);
            } else {
                log.warn("========================================");
                log.warn("JavaScript engine not found!");
                log.warn("BPMN script tasks may fail at runtime.");
                log.warn("========================================");
                log.warn("Troubleshooting steps:");
                log.warn("1. Ensure org.graalvm.js:js is in dependencies");
                log.warn("2. Ensure org.graalvm.js:js-scriptengine is in dependencies");
                log.warn("3. Ensure org.graalvm.sdk:graal-sdk is in dependencies");
                log.warn("4. Check that all dependencies use the same version (22.3.3)");
            }
        } catch (Exception e) {
            // Don't fail application startup - JavaScript engine may still work at runtime
            log.warn("Error during JavaScript engine verification (app will continue): {}", e.getMessage());
            log.debug("Full stack trace:", e);
        }
    }

    private void logAvailableEngines(ScriptEngineManager manager) {
        try {
            List<ScriptEngineFactory> factories = manager.getEngineFactories();
            if (factories.isEmpty()) {
                log.warn("No script engines found in ScriptEngineManager!");
            } else {
                log.info("Available script engines ({}):", factories.size());
                for (ScriptEngineFactory factory : factories) {
                    try {
                        log.info("  - {} v{} [names: {}, extensions: {}]",
                            factory.getEngineName(),
                            factory.getEngineVersion(),
                            factory.getNames(),
                            factory.getExtensions());
                    } catch (Exception e) {
                        log.info("  - (engine details unavailable: {})", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not enumerate script engines: {}", e.getMessage());
        }
    }

    private ScriptEngine findJavaScriptEngine(ScriptEngineManager manager) {
        // Try by name
        for (String name : JS_ENGINE_NAMES) {
            try {
                ScriptEngine engine = manager.getEngineByName(name);
                if (engine != null) {
                    log.info("Found JavaScript engine using name: '{}'", name);

                    // Ensure 'javascript' name is registered if it wasn't
                    if (!"javascript".equals(name)) {
                        try {
                            log.info("Registering engine under 'javascript' name");
                            manager.registerEngineName("javascript", engine.getFactory());
                        } catch (Exception e) {
                            log.debug("Could not register engine under 'javascript' name: {}", e.getMessage());
                        }
                    }

                    return engine;
                }
            } catch (Exception e) {
                log.debug("Error trying engine name '{}': {}", name, e.getMessage());
            }
        }

        // Try by MIME type
        try {
            ScriptEngine engine = manager.getEngineByMimeType("application/javascript");
            if (engine != null) {
                log.info("Found JavaScript engine using MIME type");
                return engine;
            }
        } catch (Exception e) {
            log.debug("Error trying MIME type lookup: {}", e.getMessage());
        }

        // Try by extension
        try {
            ScriptEngine engine = manager.getEngineByExtension("js");
            if (engine != null) {
                log.info("Found JavaScript engine using extension");
                return engine;
            }
        } catch (Exception e) {
            log.debug("Error trying extension lookup: {}", e.getMessage());
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
        } catch (Exception e) {
            log.warn("JavaScript engine test failed: {}", e.getMessage());
            log.debug("Script tasks may not work correctly", e);
        }
    }
}
