import { useEffect, useState } from 'react';
import {
  Boxes,
  CheckCircle2,
  AlertTriangle,
  Clock3,
  TrendingUp,
  Plus,
  AlertCircle,
  Calendar,
  Zap,
  Activity,
} from 'lucide-react';
import { analyticsApi, type ProductionSummary, type OrderDelayRisk } from '../api/analytics';
import { productionApi, type ProductionUpdate } from '../api/production';
import { ordersApi, type ProductionOrder } from '../api/orders';

interface MerchandiserDashboardProps {
  onOpenOrder: (orderId: number) => void;
  onCreateOrder: () => void;
  pendingIssuesCount?: number;
}

export function MerchandiserDashboard({
  onOpenOrder,
  onCreateOrder,
  pendingIssuesCount = 0,
}: MerchandiserDashboardProps) {
  const [summary, setSummary] = useState<ProductionSummary | null>(null);
  const [risks, setRisks] = useState<OrderDelayRisk[]>([]);
  const [recentUpdates, setRecentUpdates] = useState<ProductionUpdate[]>([]);
  const [orders, setOrders] = useState<ProductionOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadDashboardData = async () => {
    setLoading(true);
    setError('');
    try {
      const [sumData, riskData, updatesData, ordersData] = await Promise.all([
        analyticsApi.summary(),
        analyticsApi.allOrderRisks(),
        productionApi.updates(),
        ordersApi.list('?size=20'),
      ]);
      setSummary(sumData);
      setRisks(riskData);
      setRecentUpdates(updatesData.slice(0, 5));
      setOrders(ordersData.content);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load merchandiser dashboard');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadDashboardData();
  }, []);

  if (loading) {
    return (
      <div className="dashboard-loading" role="status">
        <div className="dashboard-spinner" />
        <p>Assembling production intelligence &amp; analytics...</p>
      </div>
    );
  }

  if (error || !summary) {
    return (
      <div className="dashboard-error-panel" role="alert">
        <AlertTriangle size={24} className="error-icon" />
        <h3>Dashboard Analytics Unavailable</h3>
        <p>{error || 'Could not connect to production intelligence service.'}</p>
        <button type="button" className="button button-primary" onClick={loadDashboardData}>
          Retry Connection
        </button>
      </div>
    );
  }

  const totalActive = summary.totalOrders || 1;
  const onTrackPercent = Math.round((summary.onTrack / totalActive) * 100);
  const atRiskPercent = Math.round((summary.atRisk / totalActive) * 100);
  const delayedPercent = Math.round((summary.delayed / totalActive) * 100);

  // Total production shortfall across delayed and at-risk orders
  const totalShortfall = risks.reduce((acc, r) => acc + (r.productionShortfall || 0), 0);

  // Upcoming shipments sorted by ex-factory date
  const upcomingShipments = [...orders]
    .filter((o) => o.status !== 'COMPLETED' && o.status !== 'CANCELLED')
    .sort((a, b) => new Date(a.exFactoryDate).getTime() - new Date(b.exFactoryDate).getTime())
    .slice(0, 5);

  return (
    <div className="merchandiser-dashboard">
      {/* Top Banner with Quick Actions */}
      <div className="dashboard-hero-banner">
        <div>
          <span className="hero-tag">Tiruppur Knitwear Operations Hub</span>
          <h2 className="hero-title">Production Intelligence &amp; Overview</h2>
          <p className="hero-desc">
            Monitoring active cutting, knitting, dyeing, sewing, and packing milestones.
          </p>
        </div>
        <div className="hero-actions">
          {pendingIssuesCount > 0 && (
            <a href="#issues" className="button button-warning-outline" data-testid="dash-review-issues-btn">
              <AlertCircle size={15} />
              Review Tamil Issues ({pendingIssuesCount})
            </a>
          )}
          <button
            type="button"
            className="button button-primary"
            onClick={onCreateOrder}
            data-testid="dash-create-order-btn"
          >
            <Plus size={16} />
            New Production Order
          </button>
        </div>
      </div>

      {/* 4 Executive Metric Cards */}
      <div className="dashboard-metric-grid">
        <div className="exec-metric-card">
          <div className="metric-header">
            <span className="metric-label">Active Orders</span>
            <div className="metric-icon-wrap icon-neutral">
              <Boxes size={18} />
            </div>
          </div>
          <div className="metric-value-row">
            <strong className="metric-val">{summary.totalOrders}</strong>
            <span className="metric-chip chip-neutral">PO In-Flight</span>
          </div>
          <span className="metric-subtext">Across 6 production units</span>
        </div>

        <div className="exec-metric-card">
          <div className="metric-header">
            <span className="metric-label">On Track</span>
            <div className="metric-icon-wrap icon-success">
              <CheckCircle2 size={18} />
            </div>
          </div>
          <div className="metric-value-row">
            <strong className="metric-val val-success">{summary.onTrack}</strong>
            <span className="metric-chip chip-success">
              <TrendingUp size={12} /> {onTrackPercent}%
            </span>
          </div>
          <span className="metric-subtext">Meeting daily required pace</span>
        </div>

        <div className="exec-metric-card">
          <div className="metric-header">
            <span className="metric-label">At Risk</span>
            <div className="metric-icon-wrap icon-warning">
              <AlertTriangle size={18} />
            </div>
          </div>
          <div className="metric-value-row">
            <strong className="metric-val val-warning">{summary.atRisk}</strong>
            <span className="metric-chip chip-warning">{atRiskPercent}%</span>
          </div>
          <span className="metric-subtext">Buffer days diminishing</span>
        </div>

        <div className="exec-metric-card">
          <div className="metric-header">
            <span className="metric-label">Delayed Orders</span>
            <div className="metric-icon-wrap icon-danger">
              <Clock3 size={18} />
            </div>
          </div>
          <div className="metric-value-row">
            <strong className="metric-val val-danger">{summary.delayed}</strong>
            <span className="metric-chip chip-danger">{delayedPercent}%</span>
          </div>
          <span className="metric-subtext">
            {totalShortfall > 0 ? `${totalShortfall.toLocaleString()} pcs shortfall` : 'Action required'}
          </span>
        </div>
      </div>

      {/* Visual Analytics Row: Distribution Donut & Capacity Analysis */}
      <div className="dashboard-charts-grid">
        {/* Status Distribution Card */}
        <div className="dash-card">
          <div className="dash-card-header">
            <div>
              <h3 className="dash-card-title">Production Status Distribution</h3>
              <p className="dash-card-subtitle">Health across all scheduled export commitments</p>
            </div>
            <span className="status-total-badge">{summary.totalOrders} Total</span>
          </div>

          <div className="donut-chart-container">
            {/* SVG Segmented Donut Chart */}
            <svg viewBox="0 0 100 100" className="donut-chart-svg" aria-label="Order status chart">
              {/* Background circle */}
              <circle cx="50" cy="50" r="38" className="donut-bg" />
              {/* Segments: circum = 2 * PI * 38 = 238.76 */}
              <circle
                cx="50"
                cy="50"
                r="38"
                className="donut-segment segment-ontrack"
                strokeDasharray={`${(onTrackPercent / 100) * 238.76} 238.76`}
                strokeDashoffset="0"
              />
              <circle
                cx="50"
                cy="50"
                r="38"
                className="donut-segment segment-atrisk"
                strokeDasharray={`${(atRiskPercent / 100) * 238.76} 238.76`}
                strokeDashoffset={`${-((onTrackPercent / 100) * 238.76)}`}
              />
              <circle
                cx="50"
                cy="50"
                r="38"
                className="donut-segment segment-delayed"
                strokeDasharray={`${(delayedPercent / 100) * 238.76} 238.76`}
                strokeDashoffset={`${-(((onTrackPercent + atRiskPercent) / 100) * 238.76)}`}
              />
            </svg>
            <div className="donut-center-label">
              <strong>{onTrackPercent}%</strong>
              <span>On Schedule</span>
            </div>
          </div>

          {/* Chart Legend */}
          <div className="donut-legend">
            <div className="legend-item">
              <span className="legend-dot dot-ontrack" />
              <span className="legend-name">On Track</span>
              <strong className="legend-val">{summary.onTrack}</strong>
            </div>
            <div className="legend-item">
              <span className="legend-dot dot-atrisk" />
              <span className="legend-name">At Risk</span>
              <strong className="legend-val">{summary.atRisk}</strong>
            </div>
            <div className="legend-item">
              <span className="legend-dot dot-delayed" />
              <span className="legend-name">Delayed</span>
              <strong className="legend-val">{summary.delayed}</strong>
            </div>
            <div className="legend-item">
              <span className="legend-dot dot-completed" />
              <span className="legend-name">Completed</span>
              <strong className="legend-val">{summary.completed}</strong>
            </div>
          </div>
        </div>

        {/* Capacity & Production Pace Analysis */}
        <div className="dash-card">
          <div className="dash-card-header">
            <div>
              <h3 className="dash-card-title">Production Pace &amp; Shortfall</h3>
              <p className="dash-card-subtitle">Floor velocity vs required ship-date quotas</p>
            </div>
            <div className="pace-badge">
              <Zap size={14} /> Factory Velocity
            </div>
          </div>

          <div className="pace-summary-body">
            {risks.length === 0 ? (
              <p className="empty-subtext">No order risk velocity data recorded yet.</p>
            ) : (
              <div className="pace-bars-list">
                {risks.slice(0, 4).map((r) => {
                  const req = r.requiredDailyRate || 1;
                  const act = r.actualDailyRate || 0;
                  const ratio = Math.min(100, Math.round((act / req) * 100));
                  return (
                    <div key={r.orderId} className="pace-row" onClick={() => onOpenOrder(r.orderId)}>
                      <div className="pace-row-head">
                        <div>
                          <strong>{r.poNumber}</strong>
                          <span className="pace-style">{r.styleName}</span>
                        </div>
                        <div className="pace-figures">
                          <span className="pace-actual">{act.toFixed(0)} pcs/day</span>
                          <span className="pace-sep">/</span>
                          <span className="pace-target">{req.toFixed(0)} req</span>
                        </div>
                      </div>
                      <div className="pace-track">
                        <div
                          className={`pace-fill fill-${r.riskStatus.toLowerCase()}`}
                          style={{ width: `${ratio}%` }}
                        />
                      </div>
                      <div className="pace-row-foot">
                        <span className={`risk-tag tag-${r.riskStatus.toLowerCase()}`}>
                          {r.riskStatus}
                        </span>
                        <span className="pace-shortfall">
                          {r.productionShortfall > 0
                            ? `Shortfall: -${r.productionShortfall.toLocaleString()} pcs`
                            : 'On Target'}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Middle Row: Upcoming Ex-Factory Watchlist & Recent Floor Updates */}
      <div className="dashboard-tables-grid">
        {/* Upcoming Ex-Factory Deadlines */}
        <div className="dash-card">
          <div className="dash-card-header">
            <div>
              <h3 className="dash-card-title">Upcoming Ex-Factory Watchlist</h3>
              <p className="dash-card-subtitle">Shipments scheduled within the next 14 days</p>
            </div>
            <a href="#orders" className="dash-link">View All Orders &rarr;</a>
          </div>

          {upcomingShipments.length === 0 ? (
            <p className="empty-subtext">No pending shipments scheduled.</p>
          ) : (
            <div className="watchlist-table-wrap">
              <table className="watchlist-table">
                <thead>
                  <tr>
                    <th>PO &amp; Style</th>
                    <th>Buyer</th>
                    <th>Ex-Factory</th>
                    <th>Progress</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {upcomingShipments.map((order) => {
                    const completed = order.milestones.filter((m) => m.status === 'COMPLETED').length;
                    const percent = order.milestones.length
                      ? Math.round((completed / order.milestones.length) * 100)
                      : 0;
                    return (
                      <tr key={order.id} className="watchlist-row">
                        <td>
                          <strong>{order.poNumber}</strong>
                          <span className="cell-sub">{order.styleName}</span>
                        </td>
                        <td>
                          <span className="buyer-tag">{order.buyer.buyerName}</span>
                        </td>
                        <td>
                          <div className="date-cell">
                            <Calendar size={13} />
                            <span>{order.exFactoryDate}</span>
                          </div>
                        </td>
                        <td>
                          <div className="table-progress-wrap">
                            <div className="mini-progress-track">
                              <div
                                className="mini-progress-fill"
                                style={{ width: `${percent}%` }}
                              />
                            </div>
                            <span className="mini-percent">{percent}%</span>
                          </div>
                        </td>
                        <td>
                          <button
                            type="button"
                            className="button button-small button-outline"
                            onClick={() => onOpenOrder(order.id)}
                            title="Open order details"
                          >
                            Details &rarr;
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Live Shop Floor Submissions Feed */}
        <div className="dash-card">
          <div className="dash-card-header">
            <div>
              <h3 className="dash-card-title">Live Shop Floor Submissions</h3>
              <p className="dash-card-subtitle">Latest completed quantities from production units</p>
            </div>
            <div className="activity-pulse-tag">
              <Activity size={13} className="pulse-icon" /> Live
            </div>
          </div>

          {recentUpdates.length === 0 ? (
            <p className="empty-subtext">No production updates submitted yet.</p>
          ) : (
            <div className="floor-feed-list">
              {recentUpdates.map((update) => (
                <div key={update.id} className="floor-feed-item">
                  <div className="feed-item-top">
                    <span className="feed-po">{update.poNumber}</span>
                    <span className="feed-stage">{update.milestoneName}</span>
                    <span className="feed-qty">
                      +{update.completedQuantity.toLocaleString()} pcs
                    </span>
                  </div>
                  {update.issueNote && (
                    <div className="feed-issue-preview" title="Supervisor issue note">
                      <span className="issue-label">Note:</span> {update.issueNote}
                    </div>
                  )}
                  <div className="feed-item-footer">
                    <span className="feed-supervisor">{update.supervisorEmail}</span>
                    <span className="feed-time">{update.productionDate}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
