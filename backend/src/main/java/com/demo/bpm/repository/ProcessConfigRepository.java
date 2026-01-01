package com.demo.bpm.repository;

import com.demo.bpm.entity.ProcessConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessConfigRepository extends JpaRepository<ProcessConfig, Long> {

    Optional<ProcessConfig> findByProcessDefinitionKey(String processDefinitionKey);

    boolean existsByProcessDefinitionKey(String processDefinitionKey);
}
