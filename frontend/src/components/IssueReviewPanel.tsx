import { useEffect, useState } from 'react';
import {
  issuesApi,
  type ClassificationResponse,
  type IssueCategory,
  ISSUE_CATEGORY_LABELS,
} from '../api/issues';

const CATEGORIES: IssueCategory[] = [
  'MACHINE_BREAKDOWN',
  'MATERIAL_SHORTAGE',
  'LABOUR_SHORTAGE',
  'QUALITY_ISSUE',
  'PRINTING_DELAY',
  'WASHING_DELAY',
  'POWER_FAILURE',
  'OTHER',
];

export function IssueReviewPanel() {
  const [issues, setIssues] = useState<ClassificationResponse[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState('');
  const [actionSuccess, setActionSuccess] = useState('');

  // Form states for manual review
  const [formCategory, setFormCategory] = useState<IssueCategory | ''>('');
  const [formTranslation, setFormTranslation] = useState('');

  const loadIssues = async () => {
    setLoading(true);
    setError('');
    setActionError('');
    setActionSuccess('');
    try {
      const data = await issuesApi.pendingReview();
      setIssues(data);
      if (data.length > 0) {
        setSelectedId((current) =>
          current && data.some((i) => i.issueId === current) ? current : data[0].issueId
        );
      } else {
        setSelectedId(null);
      }
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Pending issues could not be loaded.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadIssues();
  }, []);

  const selectedIssue = issues.find((issue) => issue.issueId === selectedId) || null;

  useEffect(() => {
    if (selectedIssue) {
      setFormCategory(selectedIssue.reviewedCategory || selectedIssue.predictedCategory || '');
      setFormTranslation(selectedIssue.reviewedTranslation || selectedIssue.englishTranslation || '');
      setActionError('');
      setActionSuccess('');
    }
  }, [selectedIssue?.issueId]);

  const handleAcceptAi = async () => {
    if (!selectedIssue) return;
    setSubmitting(true);
    setActionError('');
    setActionSuccess('');
    try {
      const updated = await issuesApi.review(selectedIssue.issueId, {
        acceptAiClassification: true,
      });
      setActionSuccess('AI classification accepted.');
      setIssues((current) =>
        current.map((i) => (i.issueId === updated.issueId ? updated : i))
      );
    } catch (cause) {
      setActionError(cause instanceof Error ? cause.message : 'Could not accept AI classification.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleManualReview = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedIssue) return;
    if (!formCategory) {
      setActionError('Please select a category for this issue.');
      return;
    }
    setSubmitting(true);
    setActionError('');
    setActionSuccess('');
    try {
      const updated = await issuesApi.review(selectedIssue.issueId, {
        acceptAiClassification: false,
        category: formCategory,
        englishTranslation: formTranslation,
      });
      setActionSuccess('Manual review saved.');
      setIssues((current) =>
        current.map((i) => (i.issueId === updated.issueId ? updated : i))
      );
    } catch (cause) {
      setActionError(cause instanceof Error ? cause.message : 'Could not save review.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleReclassify = async () => {
    if (!selectedIssue) return;
    setSubmitting(true);
    setActionError('');
    setActionSuccess('');
    try {
      const updated = await issuesApi.reclassify(selectedIssue.issueId);
      setActionSuccess('Issue reclassification triggered.');
      setIssues((current) =>
        current.map((i) => (i.issueId === updated.issueId ? updated : i))
      );
    } catch (cause) {
      setActionError(cause instanceof Error ? cause.message : 'Could not reclassify issue.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <section className="empty-panel" role="status">
        <h2>Loading pending issue reviews...</h2>
        <p>Retrieving production floor issues requiring merchandiser attention.</p>
      </section>
    );
  }

  if (error) {
    return (
      <section className="empty-panel" role="alert">
        <h2>Issue reviews unavailable</h2>
        <p>{error}</p>
        <button className="state-action" type="button" onClick={() => void loadIssues()}>
          Retry
        </button>
      </section>
    );
  }

  if (issues.length === 0) {
    return (
      <section className="empty-panel" role="status">
        <h2>No issues pending review</h2>
        <p>Supervisor production issues from your orders will appear here for review.</p>
        <button className="state-action" type="button" onClick={() => void loadIssues()}>
          Refresh
        </button>
      </section>
    );
  }

  return (
    <div className="issue-review-workspace">
      <div className="issue-review-layout">
        {/* Left Column: Queue List */}
        <section className="issue-queue" aria-label="Pending issues queue">
          <div className="section-heading">
            <div>
              <p>Supervisor floor issues</p>
              <h2>Pending queue ({issues.length})</h2>
            </div>
            <button className="state-action" type="button" onClick={() => void loadIssues()}>
              Refresh
            </button>
          </div>
          <div className="issue-queue-list">
            {issues.map((item) => {
              const isSelected = item.issueId === selectedId;
              return (
                <article
                  key={item.issueId}
                  className={`issue-card ${isSelected ? 'selected' : ''}`}
                  onClick={() => setSelectedId(item.issueId)}
                  tabIndex={0}
                  role="button"
                  aria-pressed={isSelected}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      setSelectedId(item.issueId);
                    }
                  }}
                >
                  <div className="issue-card-header">
                    <strong>{item.poNumber}</strong>
                    <span className={`issue-status-badge status-${item.status.toLowerCase()}`}>
                      {item.status.replace(/_/g, ' ')}
                    </span>
                  </div>
                  <p className="issue-card-style">{item.styleName}</p>
                  <p className="issue-card-preview">{item.originalNote}</p>
                  <div className="issue-card-meta">
                    <small>Supervisor #{item.supervisorId}</small>
                    {item.predictedCategory && (
                      <span className="issue-card-cat">
                        {ISSUE_CATEGORY_LABELS[item.predictedCategory]}
                      </span>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
        </section>

        {/* Right Column: Detail & Review */}
        {selectedIssue && (
          <section className="issue-detail" aria-label="Issue review detail">
            <div className="section-heading">
              <div>
                <p>Order {selectedIssue.poNumber} · {selectedIssue.styleName}</p>
                <h2>Review issue #{selectedIssue.issueId}</h2>
              </div>
              <span className={`issue-status-badge status-${selectedIssue.status.toLowerCase()}`}>
                {selectedIssue.status.replace(/_/g, ' ')}
              </span>
            </div>

            {actionError && (
              <div className="login-alert" role="alert">
                {actionError}
              </div>
            )}
            {actionSuccess && (
              <div className="success-alert" role="status">
                {actionSuccess}
              </div>
            )}

            {/* Original Floor Note */}
            <div className="issue-detail-card original-note-card">
              <div className="card-title-row">
                <h3>Original supervisor note</h3>
                <span className="lang-tag">
                  Detected: {selectedIssue.detectedLanguage || 'Pending'}
                </span>
              </div>
              <blockquote className="original-note-quote">
                {selectedIssue.originalNote}
              </blockquote>
            </div>

            {/* AI Classification Card */}
            <div className="issue-detail-card ai-classification-card">
              <div className="card-title-row">
                <h3>AI classification</h3>
                {selectedIssue.confidence !== null && selectedIssue.confidence !== undefined && (
                  <span className="confidence-pill">
                    {(Number(selectedIssue.confidence) * 100).toFixed(0)}% confidence
                  </span>
                )}
              </div>

              {selectedIssue.status === 'AI_UNAVAILABLE' && (
                <div className="warning-banner" role="status">
                  <p>AI service is currently unavailable or unconfigured. Manual classification is required.</p>
                  <button
                    className="state-action"
                    type="button"
                    disabled={submitting}
                    onClick={() => void handleReclassify()}
                  >
                    Retry AI classification
                  </button>
                </div>
              )}

              {selectedIssue.status === 'CLASSIFICATION_FAILED' && (
                <div className="warning-banner" role="status">
                  <p>AI classification encountered an error. You may retry or classify manually.</p>
                  <button
                    className="state-action"
                    type="button"
                    disabled={submitting}
                    onClick={() => void handleReclassify()}
                  >
                    Retry AI classification
                  </button>
                </div>
              )}

              {selectedIssue.predictedCategory && (
                <div className="ai-prediction-content">
                  <div className="ai-prediction-field">
                    <span className="field-label">Predicted category:</span>
                    <strong>{ISSUE_CATEGORY_LABELS[selectedIssue.predictedCategory]}</strong>
                  </div>
                  {selectedIssue.englishTranslation && (
                    <div className="ai-prediction-field">
                      <span className="field-label">English translation:</span>
                      <p className="ai-translation-text">{selectedIssue.englishTranslation}</p>
                    </div>
                  )}
                  <button
                    className="primary-action accept-ai-btn"
                    type="button"
                    disabled={submitting}
                    onClick={() => void handleAcceptAi()}
                  >
                    {submitting ? 'Processing...' : 'Accept AI classification'}
                  </button>
                </div>
              )}
            </div>

            {/* Manual Review / Correction Card */}
            <div className="issue-detail-card manual-review-card">
              <h3>Manual review & correction</h3>
              <form onSubmit={handleManualReview} className="manual-review-form">
                <label>
                  Issue category
                  <select
                    value={formCategory}
                    onChange={(e) => setFormCategory(e.target.value as IssueCategory)}
                    aria-label="Issue category"
                  >
                    <option value="">Select category</option>
                    {CATEGORIES.map((cat) => (
                      <option key={cat} value={cat}>
                        {ISSUE_CATEGORY_LABELS[cat]}
                      </option>
                    ))}
                  </select>
                </label>

                <label>
                  English translation
                  <textarea
                    value={formTranslation}
                    onChange={(e) => setFormTranslation(e.target.value)}
                    rows={3}
                    maxLength={5000}
                    placeholder="Enter or adjust English translation"
                    aria-label="English translation"
                  />
                </label>

                <div className="form-actions">
                  <button
                    className="primary-action"
                    type="submit"
                    disabled={submitting || !formCategory}
                  >
                    {submitting ? 'Saving...' : 'Save review'}
                  </button>
                  {selectedIssue.status !== 'REVIEWED' && (
                    <button
                      className="state-action"
                      type="button"
                      disabled={submitting}
                      onClick={() => void handleReclassify()}
                    >
                      Reclassify
                    </button>
                  )}
                </div>
              </form>
            </div>

            {/* Review History */}
            {selectedIssue.reviewHistory && selectedIssue.reviewHistory.length > 0 && (
              <div className="issue-detail-card review-history-card">
                <h3>Review history</h3>
                <div className="review-history-list">
                  {selectedIssue.reviewHistory.map((history) => (
                    <div key={history.id} className="history-item">
                      <div className="history-header">
                        <span className={`decision-badge decision-${history.decision.toLowerCase()}`}>
                          {history.decision === 'ACCEPTED_AI' ? 'Accepted AI' : 'Corrected'}
                        </span>
                        <time>{new Date(history.reviewedAt).toLocaleString()}</time>
                      </div>
                      <div className="history-body">
                        <p>
                          <strong>Category:</strong> {ISSUE_CATEGORY_LABELS[history.category]}
                        </p>
                        {history.englishTranslation && (
                          <p>
                            <strong>Translation:</strong> {history.englishTranslation}
                          </p>
                        )}
                        <small>Reviewer: {history.reviewerEmail}</small>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </section>
        )}
      </div>
    </div>
  );
}
