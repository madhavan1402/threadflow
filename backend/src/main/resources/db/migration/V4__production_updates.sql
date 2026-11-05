CREATE TABLE supervisor_order_assignments (
    supervisor_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (supervisor_id, order_id),
    CONSTRAINT fk_assignment_supervisor FOREIGN KEY (supervisor_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_order FOREIGN KEY (order_id) REFERENCES production_orders(id) ON DELETE CASCADE
);
CREATE INDEX idx_assignment_order ON supervisor_order_assignments(order_id);

CREATE TABLE production_updates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    milestone_id BIGINT NOT NULL,
    supervisor_id BIGINT NOT NULL,
    production_date DATE NOT NULL,
    completed_quantity INT NOT NULL,
    issue_note TEXT NULL,
    idempotency_key VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_production_update_key UNIQUE (idempotency_key),
    CONSTRAINT fk_update_order FOREIGN KEY (order_id) REFERENCES production_orders(id),
    CONSTRAINT fk_update_milestone FOREIGN KEY (milestone_id) REFERENCES order_milestones(id),
    CONSTRAINT fk_update_supervisor FOREIGN KEY (supervisor_id) REFERENCES users(id),
    CONSTRAINT chk_update_quantity CHECK (completed_quantity > 0)
);
CREATE INDEX idx_updates_order_date ON production_updates(order_id, production_date);
CREATE INDEX idx_updates_milestone ON production_updates(milestone_id);
CREATE INDEX idx_updates_supervisor ON production_updates(supervisor_id);
CREATE INDEX idx_updates_created ON production_updates(created_at);

CREATE TABLE production_photos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    production_update_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(1024) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_production_photo_stored UNIQUE (stored_filename),
    CONSTRAINT fk_photo_update FOREIGN KEY (production_update_id) REFERENCES production_updates(id) ON DELETE CASCADE,
    CONSTRAINT chk_photo_size CHECK (file_size > 0)
);
