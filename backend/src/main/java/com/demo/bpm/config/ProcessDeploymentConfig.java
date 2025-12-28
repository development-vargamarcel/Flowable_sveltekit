package com.demo.bpm.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.List;

/**
 * Configuration to ensure all BPMN processes are properly deployed on application startup.
 * This addresses issues where processes may not be auto-deployed correctly.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ProcessDeploymentConfig {

    private final RepositoryService repositoryService;

    /**
     * Deploy all BPMN processes from the classpath on application startup.
     * This ensures all process definitions are available before the application accepts requests.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(1)  // Run early, before demo data initialization
    public void deployProcesses() {
        log.info("Starting process deployment...");

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:/processes/**/*.bpmn20.xml");

            if (resources.length == 0) {
                log.warn("No BPMN process files found in classpath:/processes/");
                return;
            }

            log.info("Found {} BPMN process file(s) to deploy", resources.length);

            // Create a single deployment for all processes
            DeploymentBuilder deployment = repositoryService.createDeployment()
                    .name("Application Processes - " + System.currentTimeMillis())
                    .enableDuplicateFiltering()
                    .category("application");

            int filesAdded = 0;
            for (Resource resource : resources) {
                try {
                    String filename = resource.getFilename();
                    log.info("Adding process file to deployment: {}", filename);
                    deployment.addInputStream(filename, resource.getInputStream());
                    filesAdded++;
                } catch (IOException e) {
                    log.error("Failed to read process file {}: {}", resource.getFilename(), e.getMessage());
                }
            }

            if (filesAdded > 0) {
                Deployment result = deployment.deploy();
                log.info("Successfully deployed {} process file(s) in deployment: {}",
                        filesAdded, result.getId());

                // Log all deployed process definitions
                List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
                        .deploymentId(result.getId())
                        .list();

                log.info("Deployed process definitions:");
                for (ProcessDefinition def : definitions) {
                    log.info("  - {} (key: {}, version: {})",
                            def.getName(), def.getKey(), def.getVersion());
                }
            } else {
                log.warn("No process files were added to deployment");
            }

            // Log summary of all process definitions
            long totalDefinitions = repositoryService.createProcessDefinitionQuery()
                    .latestVersion()
                    .count();
            log.info("Total process definitions available: {}", totalDefinitions);

        } catch (Exception e) {
            log.error("Failed to deploy processes: {}", e.getMessage(), e);
            // Don't throw exception to prevent application startup failure
            // The processes will need to be deployed manually
        }
    }
}
