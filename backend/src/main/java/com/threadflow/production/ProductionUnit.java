package com.threadflow.production;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "production_units")
public class ProductionUnit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 150) private String name;
    @Column(nullable = false, unique = true, length = 50) private String code;
    @Column(length = 500) private String description;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected ProductionUnit() {}

    public ProductionUnit(String name, String code, String description, boolean active) {
        this.name = name;
        this.code = code;
        this.description = description;
        this.active = active;
    }

    public void update(String name, String code, String description, boolean active) {
        this.name = name;
        this.code = code;
        this.description = description;
        this.active = active;
    }

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
}