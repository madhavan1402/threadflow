import React, { useState, useEffect } from 'react';
import {
  buyerApi,
  getStoredBuyerToken,
  setStoredBuyerToken,
  clearStoredBuyerToken,
  BuyerOverview,
  BuyerOrderSummary,
  BuyerOrderDetail,
  BuyerApprovedPhoto,
} from '../api/buyer';

const DEMO_TOKENS = [
  { label: 'Company B (UK)', token: 'buyer-token-company-b' },
  { label: 'Company C (Germany)', token: 'buyer-token-company-c' },
];

export const BuyerPortal: React.FC = () => {
  const [tokenInput, setTokenInput] = useState<string>(getStoredBuyerToken() || 'buyer-token-company-b');
  const [activeToken, setActiveToken] = useState<string>(getStoredBuyerToken() || 'buyer-token-company-b');
  const [overview, setOverview] = useState<BuyerOverview | null>(null);
  const [orders, setOrders] = useState<BuyerOrderSummary[]>([]);
  const [selectedOrderId, setSelectedOrderId] = useState<number | null>(null);
  const [orderDetail, setOrderDetail] = useState<BuyerOrderDetail | null>(null);
  const [previewPhoto, setPreviewPhoto] = useState<BuyerApprovedPhoto | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState<string>('');

  useEffect(() => {
    if (!getStoredBuyerToken()) {
      setStoredBuyerToken('buyer-token-company-b');
    }
  }, []);

  useEffect(() => {
    if (!activeToken) {
      setOverview(null);
      setOrders([]);
      setSelectedOrderId(null);
      setOrderDetail(null);
      return;
    }
    loadPortalData(activeToken);
  }, [activeToken]);

  useEffect(() => {
    if (!selectedOrderId || !activeToken) {
      setOrderDetail(null);
      return;
    }
    loadOrderDetail(selectedOrderId, activeToken);
  }, [selectedOrderId, activeToken]);

  const loadPortalData = async (token: string) => {
    setLoading(true);
    setError(null);
    try {
      const [overviewData, ordersData] = await Promise.all([
        buyerApi.getOverview(token),
        buyerApi.getOrders(token),
      ]);
      setOverview(overviewData);
      setOrders(ordersData);
    } catch (err: any) {
      setError(err?.message || 'Failed to authenticate buyer portal. Please verify access token.');
      setOverview(null);
      setOrders([]);
    } finally {
      setLoading(false);
    }
  };

  const loadOrderDetail = async (orderId: number, token: string) => {
    setLoading(true);
    setError(null);
    try {
      const detail = await buyerApi.getOrderDetail(orderId, token);
      setOrderDetail(detail);
    } catch (err: any) {
      setError(err?.message || 'Failed to load order details.');
      setOrderDetail(null);
    } finally {
      setLoading(false);
    }
  };

  const handleApplyToken = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const trimmed = tokenInput.trim();
    if (!trimmed) return;
    setStoredBuyerToken(trimmed);
    setActiveToken(trimmed);
    setSelectedOrderId(null);
  };

  const handleSelectDemoToken = (token: string) => {
    setTokenInput(token);
    setStoredBuyerToken(token);
    setActiveToken(token);
    setSelectedOrderId(null);
  };

  const handleLogout = () => {
    clearStoredBuyerToken();
    setTokenInput('');
    setActiveToken('');
    setOverview(null);
    setOrders([]);
    setSelectedOrderId(null);
    setOrderDetail(null);
  };

  const filteredOrders = orders.filter((o) =>
    o.poNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
    o.styleName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="buyer-portal" data-testid="buyer-portal-root">
      {/* Portal Top Header */}
      <div className="portal-header">
        <div className="portal-header-left">
          <div className="portal-brand-badge">BUYER VISIBILITY</div>
          <h1 className="portal-title">Production Order Portal</h1>
          {overview && (
            <p className="portal-subtitle">
              Welcome <strong>{overview.buyerName}</strong> ({overview.country}) &bull; Tiruppur Knitwear Operations
            </p>
          )}
        </div>
        <div className="portal-header-right">
          <a href="#orders" className="button button-outline portal-back-link" data-testid="portal-back-to-workspace-btn">
            &larr; Factory Workspace
          </a>
          {activeToken && (
            <button
              type="button"
              className="button button-outline portal-logout-btn"
              onClick={handleLogout}
              data-testid="buyer-logout-btn"
            >
              Disconnect Token
            </button>
          )}
        </div>
      </div>

      {/* Token Switcher & Demo Presets Bar */}
      <div className="portal-auth-card">
        <form onSubmit={handleApplyToken} className="portal-auth-form">
          <label htmlFor="buyer-token-input" className="portal-token-label">
            Buyer Access Token:
          </label>
          <div className="portal-token-input-group">
            <input
              id="buyer-token-input"
              type="text"
              className="portal-token-input"
              value={tokenInput}
              onChange={(e) => setTokenInput(e.target.value)}
              placeholder="e.g. buyer-token-company-b"
              data-testid="buyer-token-input"
            />
            <button
              type="submit"
              className="button button-primary"
              disabled={!tokenInput.trim() || loading}
              data-testid="buyer-token-apply-btn"
            >
              Access Portal
            </button>
          </div>
        </form>

        <div className="portal-presets">
          <span className="presets-label">Demo Presets:</span>
          {DEMO_TOKENS.map((demo) => (
            <button
              key={demo.token}
              type="button"
              className={`preset-chip ${activeToken === demo.token ? 'preset-chip-active' : ''}`}
              onClick={() => handleSelectDemoToken(demo.token)}
              data-testid={`demo-preset-${demo.token}`}
            >
              {demo.label}
            </button>
          ))}
        </div>
      </div>

      {/* Error state with retry */}
      {error && (
        <div className="portal-alert portal-alert-danger" role="alert" data-testid="portal-error-alert">
          <div className="portal-alert-content">
            <strong>Portal Access Error:</strong> {error}
          </div>
          <button
            type="button"
            className="button button-small button-outline"
            onClick={() => (selectedOrderId ? loadOrderDetail(selectedOrderId, activeToken) : loadPortalData(activeToken))}
            data-testid="portal-retry-btn"
          >
            Retry
          </button>
        </div>
      )}

      {/* Loading indicator */}
      {loading && !overview && (
        <div className="portal-loading" data-testid="portal-loading">
          <div className="portal-spinner" />
          <p>Connecting to secure buyer portal...</p>
        </div>
      )}

      {/* Overview Metrics Bar */}
      {overview && (
        <div className="portal-metric-strip" data-testid="portal-metric-strip">
          <div className="portal-metric-card">
            <div className="portal-metric-label">Active Orders</div>
            <div className="portal-metric-value">{overview.totalOrders}</div>
          </div>
          <div className="portal-metric-card">
            <div className="portal-metric-label">In Production</div>
            <div className="portal-metric-value portal-metric-highlight">{overview.inProgressOrders}</div>
          </div>
          <div className="portal-metric-card">
            <div className="portal-metric-label">Completed</div>
            <div className="portal-metric-value portal-metric-success">{overview.completedOrders}</div>
          </div>
          <div className="portal-metric-card">
            <div className="portal-metric-label">Confirmed Delay Alerts</div>
            <div className={`portal-metric-value ${overview.confirmedWarningsCount > 0 ? 'portal-metric-warning' : ''}`}>
              {overview.confirmedWarningsCount}
            </div>
          </div>
        </div>
      )}

      {/* Detail View vs List View */}
      {selectedOrderId && orderDetail ? (
        <div className="portal-order-detail" data-testid="portal-order-detail">
          <div className="portal-detail-toolbar">
            <button
              type="button"
              className="button button-outline"
              onClick={() => setSelectedOrderId(null)}
              data-testid="portal-back-to-orders-btn"
            >
              &larr; Back to Orders List
            </button>
            <span className="portal-detail-badge">PO: {orderDetail.poNumber}</span>
          </div>

          {/* Confirmed Delay Warnings Alert Banner */}
          {orderDetail.confirmedWarnings.length > 0 && (
            <div className="portal-delay-warning-box" data-testid="portal-confirmed-warning-box">
              <div className="warning-box-header">
                <span className="warning-icon">&#9888;</span>
                <span className="warning-title">Important Schedule Advisory from Merchandising</span>
              </div>
              {orderDetail.confirmedWarnings.map((warn) => (
                <div key={warn.id} className="warning-item" data-testid={`warning-item-${warn.id}`}>
                  <div className="warning-item-meta">
                    <span className={`risk-tag risk-${warn.riskStatus.toLowerCase()}`}>{warn.riskStatus}</span>
                    <span className="warning-date">
                      Confirmed on {new Date(warn.confirmedAt).toLocaleDateString()}
                    </span>
                  </div>
                  <p className="warning-message">{warn.message}</p>
                </div>
              ))}
            </div>
          )}

          {/* Order Header Summary */}
          <div className="portal-detail-summary-card">
            <div className="summary-col">
              <span className="summary-label">Style Name</span>
              <span className="summary-value">{orderDetail.styleName}</span>
            </div>
            <div className="summary-col">
              <span className="summary-label">Order Quantity</span>
              <span className="summary-value">{orderDetail.totalQuantity.toLocaleString()} pcs</span>
            </div>
            <div className="summary-col">
              <span className="summary-label">Confirmed Ex-Factory Date</span>
              <span className="summary-value">{orderDetail.exFactoryDate}</span>
            </div>
            <div className="summary-col">
              <span className="summary-label">Status</span>
              <span className="status-pill">{orderDetail.status}</span>
            </div>
          </div>

          {/* Progress Bar */}
          <div className="portal-progress-card">
            <div className="progress-header">
              <span className="progress-label">Overall Completion</span>
              <span className="progress-percent">{orderDetail.progressPercent}%</span>
            </div>
            <div className="progress-track">
              <div
                className="progress-fill"
                style={{ width: `${orderDetail.progressPercent}%` }}
                data-testid="portal-progress-fill"
              />
            </div>
          </div>

          {/* Milestone Schedule Timeline */}
          <div className="portal-section-card">
            <h3 className="portal-section-title">Production Milestone Schedule</h3>
            <div className="portal-table-wrapper">
              <table className="portal-table" data-testid="portal-milestones-table">
                <thead>
                  <tr>
                    <th>Seq</th>
                    <th>Stage / Milestone</th>
                    <th>Planned Start</th>
                    <th>Planned Target</th>
                    <th>Actual Completed</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {orderDetail.milestones.map((m) => (
                    <tr key={m.id}>
                      <td>{m.sequenceNumber}</td>
                      <td><strong>{m.milestoneName}</strong></td>
                      <td>{m.plannedStartDate}</td>
                      <td>{m.plannedEndDate}</td>
                      <td>{m.actualCompletionDate || '—'}</td>
                      <td>
                        <span className={`milestone-badge milestone-${m.status.toLowerCase()}`}>
                          {m.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Approved Production Photos Gallery */}
          <div className="portal-section-card">
            <h3 className="portal-section-title">
              Approved Factory Production Photos ({orderDetail.approvedPhotos.length})
            </h3>
            {orderDetail.approvedPhotos.length === 0 ? (
              <p className="portal-empty-text">No factory inspection photos have been approved for release yet.</p>
            ) : (
              <div className="portal-photo-grid" data-testid="portal-photo-grid">
                {orderDetail.approvedPhotos.map((photo) => {
                  const url = buyerApi.photoContentUrl(orderDetail.id, photo.id, activeToken);
                  return (
                    <div
                      key={photo.id}
                      className="portal-photo-card"
                      onClick={() => setPreviewPhoto(photo)}
                      data-testid={`photo-card-${photo.id}`}
                    >
                      <img src={url} alt={photo.originalFilename} className="portal-photo-img" />
                      <div className="portal-photo-info">
                        <span className="photo-name">{photo.originalFilename}</span>
                        <span className="photo-size">{(photo.fileSize / 1024).toFixed(1)} KB</span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      ) : (
        /* Orders List View */
        overview && (
          <div className="portal-orders-list-card">
            <div className="portal-list-toolbar">
              <h2 className="portal-list-title">Export Orders ({orders.length})</h2>
              <input
                type="text"
                className="portal-search-input"
                placeholder="Search PO or style name..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                data-testid="buyer-search-input"
              />
            </div>

            {filteredOrders.length === 0 ? (
              <div className="portal-empty-state" data-testid="portal-empty-orders">
                <p>No orders found matching your search or buyer account.</p>
              </div>
            ) : (
              <div className="portal-table-wrapper">
                <table className="portal-table" data-testid="buyer-orders-table">
                  <thead>
                    <tr>
                      <th>PO Number</th>
                      <th>Style Name</th>
                      <th>Quantity</th>
                      <th>Ex-Factory Date</th>
                      <th>Status</th>
                      <th>Progress</th>
                      <th>Confirmed Alerts</th>
                      <th>Approved Photos</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredOrders.map((order) => (
                      <tr key={order.id} data-testid={`buyer-order-row-${order.id}`}>
                        <td><strong>{order.poNumber}</strong></td>
                        <td>{order.styleName}</td>
                        <td>{order.totalQuantity.toLocaleString()} pcs</td>
                        <td>{order.exFactoryDate}</td>
                        <td>
                          <span className={`status-pill status-${order.status.toLowerCase()}`}>
                            {order.status}
                          </span>
                        </td>
                        <td>
                          <div className="mini-progress-track">
                            <div
                              className="mini-progress-fill"
                              style={{ width: `${order.progressPercent}%` }}
                            />
                          </div>
                          <span className="mini-progress-text">{order.progressPercent}%</span>
                        </td>
                        <td>
                          {order.confirmedWarningsCount > 0 ? (
                            <span className="alert-count-chip alert-count-warning" data-testid={`order-alert-chip-${order.id}`}>
                              {order.confirmedWarningsCount} alert{order.confirmedWarningsCount > 1 ? 's' : ''}
                            </span>
                          ) : (
                            <span className="alert-count-chip alert-count-none">None</span>
                          )}
                        </td>
                        <td>
                          <span className="photo-count-badge">{order.approvedPhotosCount} photos</span>
                        </td>
                        <td>
                          <button
                            type="button"
                            className="button button-small button-outline"
                            onClick={() => setSelectedOrderId(order.id)}
                            data-testid={`view-order-btn-${order.id}`}
                          >
                            View Order &rarr;
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )
      )}

      {/* Photo Preview Modal */}
      {previewPhoto && selectedOrderId && (
        <div className="portal-modal-backdrop" onClick={() => setPreviewPhoto(null)}>
          <div className="portal-modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h4>{previewPhoto.originalFilename}</h4>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => setPreviewPhoto(null)}
              >
                &times;
              </button>
            </div>
            <div className="modal-body">
              <img
                src={buyerApi.photoContentUrl(selectedOrderId, previewPhoto.id, activeToken)}
                alt={previewPhoto.originalFilename}
                className="modal-preview-img"
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
