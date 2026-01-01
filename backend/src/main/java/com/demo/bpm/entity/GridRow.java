package com.demo.bpm.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "grid_rows")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GridRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "process_instance_id", nullable = false, length = 64)
    private String processInstanceId;

    @Column(name = "grid_name", nullable = false, length = 255)
    private String gridName;

    @Column(name = "row_index", nullable = false)
    private Integer rowIndex;

    // 30 VARCHAR columns
    @Column(name = "varchar_1", length = 4000)
    private String varchar1;

    @Column(name = "varchar_2", length = 4000)
    private String varchar2;

    @Column(name = "varchar_3", length = 4000)
    private String varchar3;

    @Column(name = "varchar_4", length = 4000)
    private String varchar4;

    @Column(name = "varchar_5", length = 4000)
    private String varchar5;

    @Column(name = "varchar_6", length = 4000)
    private String varchar6;

    @Column(name = "varchar_7", length = 4000)
    private String varchar7;

    @Column(name = "varchar_8", length = 4000)
    private String varchar8;

    @Column(name = "varchar_9", length = 4000)
    private String varchar9;

    @Column(name = "varchar_10", length = 4000)
    private String varchar10;

    @Column(name = "varchar_11", length = 4000)
    private String varchar11;

    @Column(name = "varchar_12", length = 4000)
    private String varchar12;

    @Column(name = "varchar_13", length = 4000)
    private String varchar13;

    @Column(name = "varchar_14", length = 4000)
    private String varchar14;

    @Column(name = "varchar_15", length = 4000)
    private String varchar15;

    @Column(name = "varchar_16", length = 4000)
    private String varchar16;

    @Column(name = "varchar_17", length = 4000)
    private String varchar17;

    @Column(name = "varchar_18", length = 4000)
    private String varchar18;

    @Column(name = "varchar_19", length = 4000)
    private String varchar19;

    @Column(name = "varchar_20", length = 4000)
    private String varchar20;

    @Column(name = "varchar_21", length = 4000)
    private String varchar21;

    @Column(name = "varchar_22", length = 4000)
    private String varchar22;

    @Column(name = "varchar_23", length = 4000)
    private String varchar23;

    @Column(name = "varchar_24", length = 4000)
    private String varchar24;

    @Column(name = "varchar_25", length = 4000)
    private String varchar25;

    @Column(name = "varchar_26", length = 4000)
    private String varchar26;

    @Column(name = "varchar_27", length = 4000)
    private String varchar27;

    @Column(name = "varchar_28", length = 4000)
    private String varchar28;

    @Column(name = "varchar_29", length = 4000)
    private String varchar29;

    @Column(name = "varchar_30", length = 4000)
    private String varchar30;

    // 30 FLOAT columns (using Double)
    @Column(name = "float_1")
    private Double float1;

    @Column(name = "float_2")
    private Double float2;

    @Column(name = "float_3")
    private Double float3;

    @Column(name = "float_4")
    private Double float4;

    @Column(name = "float_5")
    private Double float5;

    @Column(name = "float_6")
    private Double float6;

    @Column(name = "float_7")
    private Double float7;

    @Column(name = "float_8")
    private Double float8;

    @Column(name = "float_9")
    private Double float9;

    @Column(name = "float_10")
    private Double float10;

    @Column(name = "float_11")
    private Double float11;

    @Column(name = "float_12")
    private Double float12;

    @Column(name = "float_13")
    private Double float13;

    @Column(name = "float_14")
    private Double float14;

    @Column(name = "float_15")
    private Double float15;

    @Column(name = "float_16")
    private Double float16;

    @Column(name = "float_17")
    private Double float17;

    @Column(name = "float_18")
    private Double float18;

    @Column(name = "float_19")
    private Double float19;

    @Column(name = "float_20")
    private Double float20;

    @Column(name = "float_21")
    private Double float21;

    @Column(name = "float_22")
    private Double float22;

    @Column(name = "float_23")
    private Double float23;

    @Column(name = "float_24")
    private Double float24;

    @Column(name = "float_25")
    private Double float25;

    @Column(name = "float_26")
    private Double float26;

    @Column(name = "float_27")
    private Double float27;

    @Column(name = "float_28")
    private Double float28;

    @Column(name = "float_29")
    private Double float29;

    @Column(name = "float_30")
    private Double float30;

    // Audit columns
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods to get/set varchar by index
    public String getVarchar(int index) {
        return switch (index) {
            case 1 -> varchar1;
            case 2 -> varchar2;
            case 3 -> varchar3;
            case 4 -> varchar4;
            case 5 -> varchar5;
            case 6 -> varchar6;
            case 7 -> varchar7;
            case 8 -> varchar8;
            case 9 -> varchar9;
            case 10 -> varchar10;
            case 11 -> varchar11;
            case 12 -> varchar12;
            case 13 -> varchar13;
            case 14 -> varchar14;
            case 15 -> varchar15;
            case 16 -> varchar16;
            case 17 -> varchar17;
            case 18 -> varchar18;
            case 19 -> varchar19;
            case 20 -> varchar20;
            case 21 -> varchar21;
            case 22 -> varchar22;
            case 23 -> varchar23;
            case 24 -> varchar24;
            case 25 -> varchar25;
            case 26 -> varchar26;
            case 27 -> varchar27;
            case 28 -> varchar28;
            case 29 -> varchar29;
            case 30 -> varchar30;
            default -> throw new IllegalArgumentException("Invalid varchar index: " + index);
        };
    }

    public void setVarchar(int index, String value) {
        switch (index) {
            case 1 -> varchar1 = value;
            case 2 -> varchar2 = value;
            case 3 -> varchar3 = value;
            case 4 -> varchar4 = value;
            case 5 -> varchar5 = value;
            case 6 -> varchar6 = value;
            case 7 -> varchar7 = value;
            case 8 -> varchar8 = value;
            case 9 -> varchar9 = value;
            case 10 -> varchar10 = value;
            case 11 -> varchar11 = value;
            case 12 -> varchar12 = value;
            case 13 -> varchar13 = value;
            case 14 -> varchar14 = value;
            case 15 -> varchar15 = value;
            case 16 -> varchar16 = value;
            case 17 -> varchar17 = value;
            case 18 -> varchar18 = value;
            case 19 -> varchar19 = value;
            case 20 -> varchar20 = value;
            case 21 -> varchar21 = value;
            case 22 -> varchar22 = value;
            case 23 -> varchar23 = value;
            case 24 -> varchar24 = value;
            case 25 -> varchar25 = value;
            case 26 -> varchar26 = value;
            case 27 -> varchar27 = value;
            case 28 -> varchar28 = value;
            case 29 -> varchar29 = value;
            case 30 -> varchar30 = value;
            default -> throw new IllegalArgumentException("Invalid varchar index: " + index);
        }
    }

    // Helper methods to get/set float by index
    public Double getFloat(int index) {
        return switch (index) {
            case 1 -> float1;
            case 2 -> float2;
            case 3 -> float3;
            case 4 -> float4;
            case 5 -> float5;
            case 6 -> float6;
            case 7 -> float7;
            case 8 -> float8;
            case 9 -> float9;
            case 10 -> float10;
            case 11 -> float11;
            case 12 -> float12;
            case 13 -> float13;
            case 14 -> float14;
            case 15 -> float15;
            case 16 -> float16;
            case 17 -> float17;
            case 18 -> float18;
            case 19 -> float19;
            case 20 -> float20;
            case 21 -> float21;
            case 22 -> float22;
            case 23 -> float23;
            case 24 -> float24;
            case 25 -> float25;
            case 26 -> float26;
            case 27 -> float27;
            case 28 -> float28;
            case 29 -> float29;
            case 30 -> float30;
            default -> throw new IllegalArgumentException("Invalid float index: " + index);
        };
    }

    public void setFloat(int index, Double value) {
        switch (index) {
            case 1 -> float1 = value;
            case 2 -> float2 = value;
            case 3 -> float3 = value;
            case 4 -> float4 = value;
            case 5 -> float5 = value;
            case 6 -> float6 = value;
            case 7 -> float7 = value;
            case 8 -> float8 = value;
            case 9 -> float9 = value;
            case 10 -> float10 = value;
            case 11 -> float11 = value;
            case 12 -> float12 = value;
            case 13 -> float13 = value;
            case 14 -> float14 = value;
            case 15 -> float15 = value;
            case 16 -> float16 = value;
            case 17 -> float17 = value;
            case 18 -> float18 = value;
            case 19 -> float19 = value;
            case 20 -> float20 = value;
            case 21 -> float21 = value;
            case 22 -> float22 = value;
            case 23 -> float23 = value;
            case 24 -> float24 = value;
            case 25 -> float25 = value;
            case 26 -> float26 = value;
            case 27 -> float27 = value;
            case 28 -> float28 = value;
            case 29 -> float29 = value;
            case 30 -> float30 = value;
            default -> throw new IllegalArgumentException("Invalid float index: " + index);
        }
    }
}
