CREATE TABLE production_issue_classifications (
    production_update_id BIGINT NOT NULL,
    detected_language VARCHAR(16) NULL,
    english_translation TEXT NULL,
    predicted_category VARCHAR(32) NULL,
    confidence DECIMAL(4,3) NULL,
    classification_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    provider_id VARCHAR(100) NULL,
    model_id VARCHAR(255) NULL,
    reviewed_category VARCHAR(32) NULL,
    reviewed_translation TEXT NULL,
    reviewer_id BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (production_update_id),
    CONSTRAINT fk_issue_classification_update FOREIGN KEY (production_update_id)
        REFERENCES production_updates(id) ON DELETE CASCADE,
    CONSTRAINT fk_issue_classification_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
    CONSTRAINT chk_issue_classification_status CHECK (classification_status IN
        ('PENDING', 'PENDING_REVIEW', 'AI_UNAVAILABLE', 'CLASSIFICATION_FAILED', 'REVIEWED')),
    CONSTRAINT chk_issue_predicted_category CHECK (predicted_category IS NULL OR predicted_category IN
        ('MACHINE_BREAKDOWN', 'MATERIAL_SHORTAGE', 'LABOUR_SHORTAGE', 'QUALITY_ISSUE',
         'PRINTING_DELAY', 'WASHING_DELAY', 'POWER_FAILURE', 'OTHER')),
    CONSTRAINT chk_issue_reviewed_category CHECK (reviewed_category IS NULL OR reviewed_category IN
        ('MACHINE_BREAKDOWN', 'MATERIAL_SHORTAGE', 'LABOUR_SHORTAGE', 'QUALITY_ISSUE',
         'PRINTING_DELAY', 'WASHING_DELAY', 'POWER_FAILURE', 'OTHER')),
    CONSTRAINT chk_issue_confidence CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1))
);
CREATE INDEX idx_issue_classification_status ON production_issue_classifications(classification_status, created_at);
CREATE INDEX idx_issue_classification_reviewer ON production_issue_classifications(reviewer_id, reviewed_at);

CREATE TABLE production_issue_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    production_update_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    decision VARCHAR(24) NOT NULL,
    category VARCHAR(32) NOT NULL,
    english_translation TEXT NULL,
    reviewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_issue_review_classification FOREIGN KEY (production_update_id)
        REFERENCES production_issue_classifications(production_update_id) ON DELETE CASCADE,
    CONSTRAINT fk_issue_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
    CONSTRAINT chk_issue_review_decision CHECK (decision IN ('ACCEPTED_AI', 'CORRECTED')),
    CONSTRAINT chk_issue_review_category CHECK (category IN
        ('MACHINE_BREAKDOWN', 'MATERIAL_SHORTAGE', 'LABOUR_SHORTAGE', 'QUALITY_ISSUE',
         'PRINTING_DELAY', 'WASHING_DELAY', 'POWER_FAILURE', 'OTHER'))
);
CREATE INDEX idx_issue_reviews_history ON production_issue_reviews(production_update_id, reviewed_at);