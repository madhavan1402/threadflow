CREATE TABLE buyers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    buyer_name VARCHAR(255) NOT NULL,
    country VARCHAR(100) NOT NULL,
    contact_email VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_buyers_name_country UNIQUE (buyer_name, country),
    CONSTRAINT chk_buyers_name CHECK (CHAR_LENGTH(TRIM(buyer_name)) > 0),
    CONSTRAINT chk_buyers_country CHECK (CHAR_LENGTH(TRIM(country)) > 0),
    CONSTRAINT chk_buyers_email CHECK (contact_email IS NULL OR CHAR_LENGTH(contact_email) > 0)
);

CREATE TABLE production_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    buyer_id BIGINT NOT NULL,
    po_number VARCHAR(100) NOT NULL,
    style_name VARCHAR(255) NOT NULL,
    product_description TEXT NULL,
    total_quantity INT NOT NULL,
    ex_factory_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PLANNED',
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_production_orders_po_number UNIQUE (po_number),
    CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id) REFERENCES buyers(id),
    CONSTRAINT fk_orders_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_order_quantity CHECK (total_quantity > 0),
    CONSTRAINT chk_order_status CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);
CREATE INDEX idx_orders_buyer ON production_orders(buyer_id);
CREATE INDEX idx_orders_status ON production_orders(status);
CREATE INDEX idx_orders_ex_factory ON production_orders(ex_factory_date);
CREATE INDEX idx_orders_po_style ON production_orders(po_number, style_name);

CREATE TABLE order_variants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    color VARCHAR(100) NOT NULL,
    size VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_order_variant UNIQUE (order_id, color, size),
    CONSTRAINT fk_variants_order FOREIGN KEY (order_id) REFERENCES production_orders(id) ON DELETE CASCADE,
    CONSTRAINT chk_variant_quantity CHECK (quantity > 0)
);
CREATE INDEX idx_variants_order ON order_variants(order_id);

CREATE TABLE milestone_templates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    template_name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_milestone_templates_name UNIQUE (template_name)
);

CREATE TABLE template_milestones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    template_id BIGINT NOT NULL,
    milestone_name VARCHAR(255) NOT NULL,
    sequence_number INT NOT NULL,
    default_duration_days INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_template_milestone_sequence UNIQUE (template_id, sequence_number),
    CONSTRAINT fk_template_milestones_template FOREIGN KEY (template_id) REFERENCES milestone_templates(id) ON DELETE CASCADE,
    CONSTRAINT chk_template_sequence CHECK (sequence_number > 0),
    CONSTRAINT chk_template_duration CHECK (default_duration_days >= 0)
);
CREATE INDEX idx_template_milestones_template ON template_milestones(template_id);

CREATE TABLE order_milestones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    milestone_name VARCHAR(255) NOT NULL,
    sequence_number INT NOT NULL,
    planned_start_date DATE NOT NULL,
    planned_end_date DATE NOT NULL,
    actual_start_date DATE NULL,
    actual_completion_date DATE NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED',
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_order_milestone_sequence UNIQUE (order_id, sequence_number),
    CONSTRAINT fk_order_milestones_order FOREIGN KEY (order_id) REFERENCES production_orders(id) ON DELETE CASCADE,
    CONSTRAINT chk_milestone_sequence CHECK (sequence_number > 0),
    CONSTRAINT chk_milestone_dates CHECK (planned_end_date >= planned_start_date),
    CONSTRAINT chk_milestone_actual_dates CHECK (
        actual_start_date IS NULL OR actual_completion_date IS NULL OR actual_completion_date >= actual_start_date
    ),
    CONSTRAINT chk_order_milestone_status CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'BLOCKED'))
);
CREATE INDEX idx_order_milestones_order ON order_milestones(order_id);
