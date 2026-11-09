import React, { useState, useEffect } from 'react';
import { auditApi, DateAuditRecord } from '../api/audit';
import { ProductionOrder } from '../api/orders';

interface OrderAuditHistoryModalProps {
  order: ProductionOrder;
  onClose: () => void;
  onDateUpdated: (newDate: string) => void;
}

export const OrderAuditHistoryModal: React.FC<OrderAuditHistoryModalProps> = ({
  order,
  onClose,
  onDateUpdated,
}) => {
  const [history, setHistory] = useState<DateAuditRecord[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const [newDate, setNewDate] = useState<string>(order.exFactoryDate);
  const [reason, setReason] = useState<string>('');
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    loadAuditHistory();
  }, [order.id]);

  const loadAuditHistory = async () => {
    setLoading(true);
    setError(null);
    try {
      const records = await auditApi.getAuditHistory(order.id);
      setHistory(records);
    } catch (err: any) {
      setError(err?.message || 'Failed to load audit history');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateDate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newDate) {
      setSubmitError('New ex-factory date is required.');
      return;
    }
    if (newDate === order.exFactoryDate) {
      setSubmitError('New ex-factory date must differ from the current date.');
      return;
    }
    if (!reason.trim()) {
      setSubmitError('Reason for date change is mandatory for audit compliance.');
      return;
    }

    setSubmitting(true);
    setSubmitError(null);
    try {
      await auditApi.changeExFactoryDate(order.id, newDate, reason.trim());
      setReason('');
      onDateUpdated(newDate);
      await loadAuditHistory();
    } catch (err: any) {
      setSubmitError(err?.message || 'Failed to update ex-factory date.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose} data-testid="audit-history-modal">
      <div className="modal-content audit-modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div>
            <h3>Ex-Factory Date Audit History</h3>
            <p className="modal-subtitle">
              Order: <strong>{order.poNumber}</strong> &bull; Current Ex-Factory Date: <strong>{order.exFactoryDate}</strong>
            </p>
          </div>
          <button type="button" className="modal-close-btn" onClick={onClose} data-testid="close-audit-modal">
            &times;
          </button>
        </div>

        <div className="modal-body">
          {/* Date Revision Form for Merchandiser */}
          <div className="audit-form-card">
            <h4>Record Ex-Factory Date Revision</h4>
            {submitError && (
              <div className="form-error-alert" role="alert" data-testid="audit-submit-error">
                {submitError}
              </div>
            )}
            <form onSubmit={handleUpdateDate} className="audit-change-form">
              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="new-ex-factory-date">New Ex-Factory Date *</label>
                  <input
                    id="new-ex-factory-date"
                    type="date"
                    className="form-control"
                    value={newDate}
                    onChange={(e) => setNewDate(e.target.value)}
                    data-testid="audit-new-date-input"
                  />
                </div>
                <div className="form-group flex-2">
                  <label htmlFor="audit-change-reason">Reason for Revision (Mandatory for Audit) *</label>
                  <input
                    id="audit-change-reason"
                    type="text"
                    className="form-control"
                    placeholder="e.g. Fabric lot delayed by knitting mill by 4 days"
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    data-testid="audit-reason-input"
                  />
                </div>
              </div>
              <div className="form-actions">
                <button
                  type="submit"
                  className="button button-primary"
                  disabled={submitting || !reason.trim() || newDate === order.exFactoryDate}
                  data-testid="submit-audit-date-change"
                >
                  {submitting ? 'Recording Audit...' : 'Update & Record Revision'}
                </button>
              </div>
            </form>
          </div>

          {/* Timeline of Revisions */}
          <div className="audit-timeline-container">
            <h4>Revision Log (Append-Only)</h4>
            {loading ? (
              <p className="loading-text">Loading audit trail...</p>
            ) : error ? (
              <div className="form-error-alert">{error}</div>
            ) : history.length === 0 ? (
              <p className="empty-text">No ex-factory date changes have been recorded for this order yet.</p>
            ) : (
              <div className="audit-timeline" data-testid="audit-timeline-list">
                {history.map((record) => (
                  <div key={record.id} className="audit-timeline-item" data-testid={`audit-record-${record.id}`}>
                    <div className="audit-timeline-badge">&#128336;</div>
                    <div className="audit-timeline-content">
                      <div className="audit-date-transition">
                        <span className="old-date">{record.previousExFactoryDate}</span>
                        <span className="date-arrow">&rarr;</span>
                        <span className="new-date">{record.newExFactoryDate}</span>
                      </div>
                      <p className="audit-reason">
                        <strong>Reason:</strong> {record.reason}
                      </p>
                      <div className="audit-meta">
                        <span>Changed by: {record.changedByEmail || 'Merchandiser'}</span>
                        <span>&bull;</span>
                        <span>{new Date(record.changedAt).toLocaleString()}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="modal-footer">
          <button type="button" className="button button-outline" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
