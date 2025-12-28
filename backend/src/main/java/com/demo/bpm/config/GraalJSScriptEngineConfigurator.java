package com.demo.bpm.config;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Engine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom configuration for GraalVM JavaScript engine with Flowable.
 *
 * This configurator ensures that the GraalVM JavaScript engine is properly
 * initialized and configured for use with Flowable BPMN script tasks.
 *
 * Note: The main ScriptingEngines bean is provided by FlowableScriptingConfig.
 * This class provides the shared GraalVM polyglot Engine bean.
 */
@Slf4j
@Configuration
public class GraalJSScriptEngineConfigurator {

    /**
     * Creates a pre-configured GraalVM polyglot engine for optimal performance.
     * The engine is shared across all script executions.
     */
    @Bean
    public Engine graalEngine() {
        log.info("Creating shared GraalVM polyglot engine...");
        Engine engine = Engine.newBuilder()
            .option("engine.WarnInterpreterOnly", "false")
            .build();
        log.info("GraalVM polyglot engine created successfully");
        return engine;
    }
}
