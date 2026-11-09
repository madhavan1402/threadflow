CREATE TABLE production_units (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(500) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_production_units_name UNIQUE (name),
    CONSTRAINT uk_production_units_code UNIQUE (code),
    CONSTRAINT chk_production_unit_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_production_unit_code CHECK (CHAR_LENGTH(TRIM(code)) > 0)
);

ALTER TABLE production_orders ADD COLUMN production_unit_id BIGINT NULL;
ALTER TABLE production_orders
    ADD CONSTRAINT fk_orders_production_unit FOREIGN KEY (production_unit_id) REFERENCES production_units(id);
CREATE INDEX idx_orders_production_unit ON production_orders(production_unit_id);

ALTER TABLE supervisor_order_assignments RENAME TO supervisor_order_assignments_v4;
CREATE TABLE supervisor_order_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    supervisor_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    assigned_by BIGINT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    active_supervisor_id BIGINT GENERATED ALWAYS AS (CASE WHEN active = TRUE THEN supervisor_id ELSE NULL END),
    active_order_id BIGINT GENERATED ALWAYS AS (CASE WHEN active = TRUE THEN order_id ELSE NULL END),
    PRIMARY KEY (id),
    CONSTRAINT uk_active_order_assignment UNIQUE (active_supervisor_id, active_order_id),
    CONSTRAINT fk_assignment_v5_supervisor FOREIGN KEY (supervisor_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_v5_order FOREIGN KEY (order_id) REFERENCES production_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_assignment_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id),
    CONSTRAINT chk_order_assignment_revoked CHECK (active = FALSE OR revoked_at IS NULL)
);
INSERT INTO supervisor_order_assignments (supervisor_id, order_id, assigned_at, active)
    SELECT supervisor_id, order_id, created_at, TRUE FROM supervisor_order_assignments_v4;
DROP TABLE supervisor_order_assignments_v4;
CREATE INDEX idx_order_assignment_active ON supervisor_order_assignments(supervisor_id, order_id, active);
CREATE INDEX idx_order_assignment_order_active ON supervisor_order_assignments(order_id, active);

CREATE TABLE supervisor_unit_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    supervisor_id BIGINT NOT NULL,
    production_unit_id BIGINT NOT NULL,
    assigned_by BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    active_supervisor_id BIGINT GENERATED ALWAYS AS (CASE WHEN active = TRUE THEN supervisor_id ELSE NULL END),
    active_unit_id BIGINT GENERATED ALWAYS AS (CASE WHEN active = TRUE THEN production_unit_id ELSE NULL END),
    PRIMARY KEY (id),
    CONSTRAINT uk_active_unit_assignment UNIQUE (active_supervisor_id, active_unit_id),
    CONSTRAINT fk_unit_assignment_supervisor FOREIGN KEY (supervisor_id) REFERENCES users(id),
    CONSTRAINT fk_unit_assignment_unit FOREIGN KEY (production_unit_id) REFERENCES production_units(id),
    CONSTRAINT fk_unit_assignment_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id),
    CONSTRAINT chk_unit_assignment_revoked CHECK (active = FALSE OR revoked_at IS NULL)
);
CREATE INDEX idx_unit_assignment_supervisor_active ON supervisor_unit_assignments(supervisor_id, active);
CREATE INDEX idx_unit_assignment_unit_active ON supervisor_unit_assignments(production_unit_id, active);
CREATE INDEX idx_unit_assignment_pair_active ON supervisor_unit_assignments(supervisor_id, production_unit_id, active);

CREATE INDEX idx_order_assignment_pair_time ON supervisor_order_assignments(supervisor_id, order_id, assigned_at);