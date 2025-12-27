package com.demo.bpm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BpmApplication {

    static {
        // Configure GraalVM JavaScript engine for Flowable script tasks
        // Required for Java 17+ since Nashorn was removed in Java 15
        // These properties enable JavaScript to interact with Java objects
        // (required for execution.getVariable, Java.to, etc. in BPMN script tasks)
        System.setProperty("polyglot.js.allowHostAccess", "true");
        System.setProperty("polyglot.js.allowHostClassLookup", "true");
        System.setProperty("polyglot.js.allowAllAccess", "true");
    }

    public static void main(String[] args) {
        SpringApplication.run(BpmApplication.class, args);
    }
}
