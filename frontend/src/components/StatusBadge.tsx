import type { Order } from './OrderTable';

const labels = {
  green: 'On track',
  amber: 'At risk',
  red: 'Delayed',
};

export function StatusBadge({ status }: { status: Order['status'] }) {
  return (
    <span className={`status-badge status-${status}`}>
      <span aria-hidden="true" />
      {labels[status]}
    </span>
  );
}
