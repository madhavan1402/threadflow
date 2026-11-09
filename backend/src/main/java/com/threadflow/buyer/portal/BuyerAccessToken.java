package com.threadflow.buyer.portal;

import com.threadflow.buyer.Buyer;
import com.threadflow.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "buyer_access_tokens")
public class BuyerAccessToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Buyer buyer;

    @Column(name = "token_value", nullable = false, unique = true, length = 128)
    private String tokenValue;

    @Column(name = "token_name", nullable = false, length = 100)
    private String tokenName;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BuyerAccessToken() {}

    public BuyerAccessToken(Buyer buyer, String tokenValue, String tokenName,
                            Instant expiresAt, User createdBy) {
        this.buyer = buyer;
        this.tokenValue = tokenValue;
        this.tokenName = tokenName;
        this.expiresAt = expiresAt;
        this.createdBy = createdBy;
    }

    public BuyerAccessToken(Buyer buyer, String tokenValue, String tokenName, Instant expiresAt) {
        this(buyer, tokenValue, tokenName, expiresAt, null);
    }

    public boolean isValid() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }

    public void revoke() {
        this.revokedAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Buyer getBuyer() { return buyer; }
    public String getTokenValue() { return tokenValue; }
    public String getTokenName() { return tokenName; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public User getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
