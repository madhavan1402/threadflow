export function StatusBadge({ status }: { status: string }) {
  return (
    <span className={`status-badge status-${status}`}>
      <span aria-hidden="true" />
      {status.replace('_', ' ').toLowerCase()}
    </span>
  );
}
