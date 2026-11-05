package com.threadflow.production;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "production_photos")
public class ProductionPhoto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "production_update_id", nullable = false) private ProductionUpdate productionUpdate;
    @Column(name = "original_filename", nullable = false) private String originalFilename;
    @Column(name = "stored_filename", nullable = false) private String storedFilename;
    @Column(name = "content_type", nullable = false) private String contentType;
    @Column(name = "file_size", nullable = false) private Long fileSize;
    @Column(name = "storage_path", nullable = false) private String storagePath;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected ProductionPhoto() {}
    public ProductionPhoto(ProductionUpdate update, String originalFilename, String storedFilename,
                           String contentType, long fileSize, String storagePath) {
        this.productionUpdate = update; this.originalFilename = originalFilename; this.storedFilename = storedFilename;
        this.contentType = contentType; this.fileSize = fileSize; this.storagePath = storagePath;
    }
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public Long getId() { return id; }
    public ProductionUpdate getProductionUpdate() { return productionUpdate; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public Long getFileSize() { return fileSize; }
    public String getStoragePath() { return storagePath; }
}
