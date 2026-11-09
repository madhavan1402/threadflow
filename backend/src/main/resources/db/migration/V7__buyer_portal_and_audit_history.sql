CREATE TABLE buyer_access_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    buyer_id BIGINT NOT NULL,
    token_value VARCHAR(128) NOT NULL,
    token_name VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_buyer_token_value UNIQUE (token_value),
    CONSTRAINT fk_buyer_token_buyer FOREIGN KEY (buyer_id) REFERENCES buyers(id) ON DELETE CASCADE,
    CONSTRAINT fk_buyer_token_user FOREIGN KEY (created_by) REFERENCES users(id)
);
CREATE INDEX idx_buyer_tokens_lookup ON buyer_access_tokens(token_value, revoked_at, expires_at);
CREATE INDEX idx_buyer_tokens_buyer ON buyer_access_tokens(buyer_id);

CREATE TABLE order_date_audit_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    previous_ex_factory_date DATE NOT NULL,
    new_ex_factory_date DATE NOT NULL,
    reason TEXT NOT NULL,
    changed_by BIGINT NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_audit_order FOREIGN KEY (order_id) REFERENCES production_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_audit_user FOREIGN KEY (changed_by) REFERENCES users(id)
);
CREATE INDEX idx_order_audit_order_date ON order_date_audit_history(order_id, changed_at);

CREATE TABLE buyer_delay_warnings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_CONFIRMATION',
    risk_status VARCHAR(32) NOT NULL,
    internal_reasons TEXT NULL,
    suggested_message TEXT NOT NULL,
    confirmed_message TEXT NULL,
    confirmed_by BIGINT NULL,
    confirmed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_warning_order FOREIGN KEY (order_id) REFERENCES production_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_warning_user FOREIGN KEY (confirmed_by) REFERENCES users(id),
    CONSTRAINT chk_warning_status CHECK (status IN ('PENDING_CONFIRMATION', 'CONFIRMED', 'DISMISSED'))
);
CREATE INDEX idx_warning_order_status ON buyer_delay_warnings(order_id, status);

ALTER TABLE production_photos ADD COLUMN approved_for_buyer BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE production_photos ADD COLUMN approved_by BIGINT NULL;
ALTER TABLE production_photos ADD COLUMN approved_at TIMESTAMP NULL;
ALTER TABLE production_photos ADD CONSTRAINT fk_photo_approved_by FOREIGN KEY (approved_by) REFERENCES users(id);
CREATE INDEX idx_photos_buyer_approved ON production_photos(production_update_id, approved_for_buyer);
