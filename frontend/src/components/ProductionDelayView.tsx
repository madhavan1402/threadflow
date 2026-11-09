import { useEffect, useState } from 'react';
import { analyticsApi, type OrderDelayRisk, type ProductionSummary, type RiskStatus } from '../api/analytics';

const statusNames: Record<RiskStatus, string> = {
  ON_TRACK: 'On track',
  AT_RISK: 'At risk',
  DELAYED: 'Delayed',
  COMPLETED: 'Completed',
};

function formatRate(rate: number | null) {
  return rate === null ? '—' : `${rate.toLocaleString(undefined, { maximumFractionDigits: 2 })} pcs/day`;
}

function RiskStatusMark({ status }: { status: RiskStatus }) {
  return <span className={`delay-status delay-status-${status.toLowerCase()}`}>{statusNames[status]}</span>;
}

function ProgressBar({ risk }: { risk: OrderDelayRisk }) {
  const percentage = Math.min(100, Math.max(0, risk.completionPercent));
  return (
    <div className="delay-progress" role="progressbar" aria-label={`${risk.poNumber} production completion`}
      aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(percentage)}>
      <span className={`delay-progress-value delay-progress-${risk.riskStatus.toLowerCase()}`}
        style={{ width: `${percentage}%` }} />
    </div>
  );
}

function RiskFigures({ risk }: { risk: OrderDelayRisk }) {
  return (
    <dl className="delay-figures">
      <div><dt>Remaining</dt><dd>{risk.remainingQuantity.toLocaleString()} pcs</dd></div>
      <div><dt>Required pace</dt><dd>{formatRate(risk.requiredDailyRate)}</dd></div>
      <div><dt>Actual pace</dt><dd>{formatRate(risk.actualDailyRate)}</dd></div>
      <div><dt>Estimated completion</dt><dd>{risk.estimatedCompletionDate || 'Not estimable'}</dd></div>
      <div><dt>Production gap</dt><dd>{risk.productionShortfall.toLocaleString()} pcs</dd></div>
    </dl>
  );
}

function RiskExplanation({ reasons }: { reasons: string[] }) {
  return <ul className="delay-reasons">{reasons.map((reason, index) => <li key={`${index}-${reason}`}>{reason}</li>)}</ul>;
}

export function ProductionDelaySummary({ onOpenOrder }: { onOpenOrder: (orderId: number) => void }) {
  const [summary, setSummary] = useState<ProductionSummary | null>(null);
  const [risks, setRisks] = useState<OrderDelayRisk[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const [nextSummary, nextRisks] = await Promise.all([analyticsApi.summary(), analyticsApi.allOrderRisks()]);
      setSummary(nextSummary);
      setRisks(nextRisks);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Production risk could not be loaded.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void load(); }, []);

  if (loading) return <section className="delay-dashboard-state" role="status">Loading production outlook...</section>;
  if (error) return <section className="delay-dashboard-state" role="alert">
    <h2>Production outlook unavailable</h2><p>{error}</p>
    <button className="state-action" type="button" onClick={() => void load()}>Retry</button>
  </section>;
  if (!summary || summary.totalOrders === 0) return <section className="delay-dashboard-state" role="status">
    <h2>No orders available for delay analysis</h2><p>Non-cancelled production orders will appear here.</p>
    <button className="state-action" type="button" onClick={() => void load()}>Refresh</button>
  </section>;

  const metrics = [
    { label: 'On track', value: summary.onTrack, status: 'on-track' },
    { label: 'At risk', value: summary.atRisk, status: 'at-risk' },
    { label: 'Delayed', value: summary.delayed, status: 'delayed' },
    { label: 'Completed', value: summary.completed, status: 'completed' },
  ];

  return (
    <section className="delay-dashboard" aria-labelledby="delay-dashboard-title">
      <div className="section-heading">
        <div><p>Ex-factory outlook · {summary.calculatedForDate}</p><h2 id="delay-dashboard-title">Production delay intelligence</h2></div>
        <button className="state-action" type="button" onClick={() => void load()}>Refresh</button>
      </div>
      <div className="delay-summary-counts" aria-label="Production risk totals">
        {metrics.map(metric => <article className={`delay-summary-count delay-count-${metric.status}`} key={metric.status}>
          <span>{metric.label}</span><strong>{metric.value}</strong>
        </article>)}
      </div>
      <div className="delay-order-list">
        {risks.map(risk => (
          <article className="delay-order-row" key={risk.orderId}>
            <div className="delay-order-heading">
              <div><strong>{risk.poNumber}</strong><span>{risk.buyerName} · {risk.styleName}</span></div>
              <RiskStatusMark status={risk.riskStatus} />
            </div>
            <div className="delay-progress-label"><span>{risk.completedQuantity.toLocaleString()} / {risk.totalQuantity.toLocaleString()} pcs</span><strong>{Math.round(risk.completionPercent)}%</strong></div>
            <ProgressBar risk={risk} />
            <RiskFigures risk={risk} />
            <RiskExplanation reasons={risk.reasons} />
            <button className="state-action" type="button" onClick={() => onOpenOrder(risk.orderId)}>Order details</button>
          </article>
        ))}
      </div>
    </section>
  );
}

export function OrderDelayRiskPanel({ orderId }: { orderId: number }) {
  const [risk, setRisk] = useState<OrderDelayRisk | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      setRisk(await analyticsApi.orderRisk(orderId));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Order risk could not be loaded.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void load(); }, [orderId]);

  if (loading) return <section className="delay-order-detail" role="status">Loading production outlook...</section>;
  if (error) return <section className="delay-order-detail" role="alert">
    <div className="section-heading"><div><p>Production intelligence</p><h2>Outlook unavailable</h2></div></div>
    <p>{error}</p><button className="state-action" type="button" onClick={() => void load()}>Retry</button>
  </section>;
  if (!risk) return null;

  return (
    <section className="delay-order-detail" aria-labelledby="order-delay-title">
      <div className="section-heading">
        <div><p>Final-stage pace · calculated {risk.calculatedForDate}</p><h2 id="order-delay-title">Production outlook</h2></div>
        <RiskStatusMark status={risk.riskStatus} />
      </div>
      <div className="delay-progress-label"><span>{risk.completedQuantity.toLocaleString()} / {risk.totalQuantity.toLocaleString()} pcs completed</span><strong>{Math.round(risk.completionPercent)}%</strong></div>
      <ProgressBar risk={risk} />
      <RiskFigures risk={risk} />
      <RiskExplanation reasons={risk.reasons} />
      {risk.reportedCompletedQuantity > risk.totalQuantity &&
        <p className="delay-overreport">Raw final-stage reports: {risk.reportedCompletedQuantity.toLocaleString()} pcs.</p>}
    </section>
  );
}