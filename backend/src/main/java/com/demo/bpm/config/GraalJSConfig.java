package com.demo.bpm.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;

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
public class GraalJSConfig {

    @PostConstruct
    public void verifyGraalJS() {
        // Verify that the GraalVM JavaScript engine is available
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");

        if (engine != null) {
            log.info("GraalVM JavaScript engine successfully loaded: {}", engine.getFactory().getEngineName());
            log.info("JavaScript engine version: {}", engine.getFactory().getEngineVersion());
        } else {
            log.error("JavaScript engine not found! BPMN script tasks will fail.");
            log.error("Ensure org.graalvm.js:js-scriptengine dependency is included.");
        }
    }
}
