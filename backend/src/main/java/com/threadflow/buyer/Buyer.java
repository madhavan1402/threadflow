package com.threadflow.buyer;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "buyers")
public class Buyer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "buyer_name", nullable = false) private String buyerName;
    @Column(nullable = false) private String country;
    @Column(name = "contact_email") private String contactEmail;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Buyer() {}
    public Buyer(String buyerName, String country, String contactEmail) {
        this.buyerName = buyerName;
        this.country = country;
        this.contactEmail = contactEmail;
    }
    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getBuyerName() { return buyerName; }
    public String getCountry() { return country; }
    public String getContactEmail() { return contactEmail; }
}
