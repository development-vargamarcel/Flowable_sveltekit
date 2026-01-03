package com.demo.bpm.repository;

import com.demo.bpm.entity.GridRow;
import com.demo.bpm.entity.GridRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GridRowRepository extends JpaRepository<GridRow, Long> {

    Page<GridRow> findByDocumentIdAndGridNameOrderByRowIndex(Long documentId, String gridName, Pageable pageable);

    List<GridRow> findByProcessInstanceIdAndGridNameOrderByRowIndex(String processInstanceId, String gridName);

    List<GridRow> findByDocumentIdOrderByGridNameAscRowIndexAsc(Long documentId);

    List<GridRow> findByProcessInstanceIdOrderByGridNameAscRowIndexAsc(String processInstanceId);

    @Modifying
    @Query("DELETE FROM GridRow gr WHERE gr.document.id = :documentId AND gr.gridName = :gridName")
    void deleteByDocumentIdAndGridName(@Param("documentId") Long documentId, @Param("gridName") String gridName);

    @Modifying
    @Query("DELETE FROM GridRow gr WHERE gr.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);

    long countByDocumentIdAndGridName(Long documentId, String gridName);
}
