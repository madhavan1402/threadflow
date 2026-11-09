import React, { useState, useEffect } from 'react';
import { warningsApi, BuyerDelayWarning } from '../api/warnings';

interface BuyerWarningPanelProps {
  orderId: number;
  poNumber: string;
}

export const BuyerWarningPanel: React.FC<BuyerWarningPanelProps> = ({ orderId, poNumber }) => {
  const [warnings, setWarnings] = useState<BuyerDelayWarning[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const [confirmingId, setConfirmingId] = useState<number | null>(null);
  const [confirmMessage, setConfirmMessage] = useState<string>('');
  const [customDraftMsg, setCustomDraftMsg] = useState<string>('');
  const [actionLoading, setActionLoading] = useState<boolean>(false);

  useEffect(() => {
    loadWarnings();
  }, [orderId]);

  const loadWarnings = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await warningsApi.getWarnings(orderId);
      setWarnings(data);
    } catch (err: any) {
      setError(err?.message || 'Failed to load delay warnings');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateDraft = async () => {
    setActionLoading(true);
    setError(null);
    try {
      await warningsApi.createDraft(orderId, customDraftMsg.trim() || undefined);
      setCustomDraftMsg('');
      await loadWarnings();
    } catch (err: any) {
      setError(err?.message || 'Failed to create warning draft');
    } finally {
      setActionLoading(false);
    }
  };

  const startConfirm = (warning: BuyerDelayWarning) => {
    setConfirmingId(warning.id);
    setConfirmMessage(warning.suggestedMessage);
  };

  const handleConfirm = async (warningId: number) => {
    if (!confirmMessage.trim()) return;
    setActionLoading(true);
    setError(null);
    try {
      await warningsApi.confirmWarning(orderId, warningId, confirmMessage.trim());
      setConfirmingId(null);
      setConfirmMessage('');
      await loadWarnings();
    } catch (err: any) {
      setError(err?.message || 'Failed to confirm warning for buyer release');
    } finally {
      setActionLoading(false);
    }
  };

  const handleDismiss = async (warningId: number) => {
    setActionLoading(true);
    setError(null);
    try {
      await warningsApi.dismissWarning(orderId, warningId);
      await loadWarnings();
    } catch (err: any) {
      setError(err?.message || 'Failed to dismiss warning');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="buyer-warning-panel" data-testid="buyer-warning-panel">
      <div className="panel-header">
        <div>
          <h4>Buyer Delay Advisories</h4>
          <p className="panel-desc">
            Merchandiser confirmation workflow for customer-facing delay warnings.
            Internal AI notes and calculations are never released until confirmed here.
          </p>
        </div>
        <button
          type="button"
          className="button button-small button-primary"
          onClick={handleCreateDraft}
          disabled={actionLoading || loading}
          data-testid="create-warning-draft-btn"
        >
          {actionLoading ? 'Generating...' : '+ Generate Draft from Delay Intelligence'}
        </button>
      </div>

      {error && (
        <div className="alert alert-danger" role="alert" data-testid="warning-panel-error">
          {error}
        </div>
      )}

      {loading ? (
        <p className="loading-text">Loading advisories...</p>
      ) : warnings.length === 0 ? (
        <div className="panel-empty" data-testid="no-warnings-message">
          <p>No delay advisories generated for PO {poNumber}.</p>
        </div>
      ) : (
        <div className="warnings-list" data-testid="warnings-list">
          {warnings.map((w) => (
            <div
              key={w.id}
              className={`warning-card warning-status-${w.status.toLowerCase()}`}
              data-testid={`merch-warning-card-${w.id}`}
            >
              <div className="warning-card-top">
                <div className="warning-tags">
                  <span className={`status-badge status-${w.status.toLowerCase()}`}>
                    {w.status.replace('_', ' ')}
                  </span>
                  <span className={`risk-badge risk-${w.riskStatus.toLowerCase()}`}>
                    {w.riskStatus}
                  </span>
                </div>
                <span className="warning-timestamp">
                  Created {new Date(w.createdAt).toLocaleDateString()}
                </span>
              </div>

              {/* Internal reasons (only visible to merchandiser) */}
              <div className="internal-reasons-box">
                <span className="internal-label">&#128274; Internal Cause (Hidden from Buyer):</span>
                <p className="internal-text">{w.internalReasons}</p>
              </div>

              {w.status === 'CONFIRMED' ? (
                <div className="confirmed-buyer-box">
                  <span className="buyer-label">&#128065; Released to Buyer:</span>
                  <p className="buyer-text">{w.confirmedMessage}</p>
                  <small className="confirmed-meta">
                    Confirmed by {w.confirmedByEmail || 'Merchandiser'} on {new Date(w.confirmedAt!).toLocaleString()}
                  </small>
                </div>
              ) : w.status === 'PENDING_CONFIRMATION' ? (
                <div className="pending-confirmation-box">
                  {confirmingId === w.id ? (
                    <div className="confirm-editor" data-testid="confirm-editor">
                      <label htmlFor={`edit-buyer-msg-${w.id}`}>
                        Customer-Facing Explanation (Edit before releasing to Buyer):
                      </label>
                      <textarea
                        id={`edit-buyer-msg-${w.id}`}
                        className="form-control"
                        rows={3}
                        value={confirmMessage}
                        onChange={(e) => setConfirmMessage(e.target.value)}
                        data-testid="confirm-message-textarea"
                      />
                      <div className="confirm-actions">
                        <button
                          type="button"
                          className="button button-small button-success"
                          onClick={() => handleConfirm(w.id)}
                          disabled={actionLoading || !confirmMessage.trim()}
                          data-testid="confirm-release-btn"
                        >
                          Confirm &amp; Release to Buyer
                        </button>
                        <button
                          type="button"
                          className="button button-small button-outline"
                          onClick={() => setConfirmingId(null)}
                        >
                          Cancel
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div>
                      <span className="suggested-label">Drafted Customer Notice:</span>
                      <p className="suggested-text">{w.suggestedMessage}</p>
                      <div className="draft-actions">
                        <button
                          type="button"
                          className="button button-small button-primary"
                          onClick={() => startConfirm(w)}
                          disabled={actionLoading}
                          data-testid={`start-confirm-btn-${w.id}`}
                        >
                          Review &amp; Confirm for Buyer
                        </button>
                        <button
                          type="button"
                          className="button button-small button-outline"
                          onClick={() => handleDismiss(w.id)}
                          disabled={actionLoading}
                          data-testid={`dismiss-btn-${w.id}`}
                        >
                          Dismiss (False Alarm)
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <div className="dismissed-box">
                  <p className="dismissed-text">Dismissed by merchandiser. Not visible to buyer.</p>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
