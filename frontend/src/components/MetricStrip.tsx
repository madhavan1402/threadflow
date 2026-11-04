import { AlertTriangle, Boxes, CheckCircle2, Clock3 } from 'lucide-react';

export function MetricStrip({
  total,
  counts,
}: {
  total: number;
  counts: { green: number; amber: number; red: number };
}) {
  const metrics = [
    { label: 'Active orders', value: total, detail: 'Across 6 production units', icon: Boxes, tone: 'neutral' },
    { label: 'On track', value: counts.green, detail: `${Math.round(counts.green / total * 100)}% of active orders`, icon: CheckCircle2, tone: 'green' },
    { label: 'At risk', value: counts.amber, detail: 'Needs review today', icon: AlertTriangle, tone: 'amber' },
    { label: 'Delayed', value: counts.red, detail: 'Action required', icon: Clock3, tone: 'red' },
  ];
  return (
    <section className="metric-strip" id="production" aria-label="Production summary">
      {metrics.map((metric) => {
        const Icon = metric.icon;
        return (
          <article className="metric" key={metric.label}>
            <Icon className={`metric-glyph metric-${metric.tone}`} size={17} aria-hidden="true" />
            <p>{metric.label}</p>
            <strong>{metric.value}</strong>
            <span>{metric.detail}</span>
          </article>
        );
      })}
    </section>
  );
}
