package com.demo.bpm.repository;

import com.demo.bpm.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // Find all documents for a process instance
    Page<Document> findByProcessInstanceId(String processInstanceId, Pageable pageable);

    // Find specific document by process instance ID and type
    Optional<Document> findByProcessInstanceIdAndType(String processInstanceId, String type);

    // Find all documents of a specific type by business key
    Optional<Document> findByBusinessKeyAndType(String businessKey, String type);

    // Find all documents by business key
    Page<Document> findByBusinessKey(String businessKey, Pageable pageable);

    boolean existsByProcessInstanceIdAndType(String processInstanceId, String type);

    // Delete all documents for a process instance
    void deleteByProcessInstanceId(String processInstanceId);
}
