package com.demo.bpm.service;

import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private HistoryService historyService;

    @Mock
    private HistoricProcessInstanceQuery historicProcessInstanceQuery;

    @InjectMocks
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        lenient().when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicProcessInstanceQuery);
    }

    @Test
    void getProcessCompletionTrend_shouldReturnTrendMetrics() {
        // Arrange
        when(historicProcessInstanceQuery.finished()).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.finishedAfter(any(Date.class))).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.orderByProcessInstanceEndTime()).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.asc()).thenReturn(historicProcessInstanceQuery);

        HistoricProcessInstance instance1 = mock(HistoricProcessInstance.class);
        // Today
        when(instance1.getEndTime()).thenReturn(Date.from(Instant.now()));

        HistoricProcessInstance instance2 = mock(HistoricProcessInstance.class);
        // Yesterday
        when(instance2.getEndTime()).thenReturn(Date.from(Instant.now().minusSeconds(86400)));

        when(historicProcessInstanceQuery.list()).thenReturn(List.of(instance1, instance2));

        // Act
        List<AnalyticsService.TrendMetric> result = analyticsService.getProcessCompletionTrend(7);

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(7, result.size());

        // Verify we have at least 2 completions counted (might be in same day or different depending on run time)
        long totalCount = result.stream().mapToLong(AnalyticsService.TrendMetric::getCount).sum();
        assertEquals(2, totalCount);
    }
}
