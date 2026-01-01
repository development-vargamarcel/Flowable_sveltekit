package com.demo.bpm.repository;

import com.demo.bpm.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByProcessInstanceId(String processInstanceId);

    Optional<Document> findByBusinessKey(String businessKey);

    List<Document> findByProcessDefinitionKey(String processDefinitionKey);

    boolean existsByProcessInstanceId(String processInstanceId);
}
