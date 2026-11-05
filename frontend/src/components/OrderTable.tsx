import { Search } from 'lucide-react';
import type { ProductionOrder } from '../api/orders';
import type { ViewState } from '../App';
export type Order = ProductionOrder;

export function OrderTable({
  orders, totalOrders, viewState, query, statusFilter, onQueryChange,
  onStatusFilterChange, onRetry, onClearFilters, errorMessage, onOpen,
}: {
  orders: ProductionOrder[]; totalOrders: number; viewState: ViewState; query: string; statusFilter: string;
  onQueryChange: (value: string) => void; onStatusFilterChange: (value: string) => void;
  onRetry: () => void; onClearFilters: () => void; errorMessage?: string; onOpen: (id: number) => void;
}) {
  return (
    <section className="order-section" id="orders">
      <div className="section-title-row"><div><h2>Orders</h2><p>Ex-factory plan and milestone progress</p></div></div>
      <div className="toolbar">
        <label className="search-field"><Search size={15} aria-hidden="true" /><span className="sr-only">Search orders</span>
          <input value={query} onChange={event => onQueryChange(event.target.value)} onBlur={() => onStatusFilterChange(statusFilter)} placeholder="Search PO or style" /></label>
        <span className="result-count">{orders.length} of {totalOrders} orders</span>
      </div>
      <div className="table-frame">
        <table className="order-table">
          <thead><tr><th>PO number</th><th>Buyer</th><th>Style</th><th>Quantity</th><th>Ex-factory</th><th>Milestone progress</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>
            {viewState === 'loading' && <tr><td colSpan={8}>Loading orders...</td></tr>}
            {viewState === 'error' && <tr><td colSpan={8}>{errorMessage || 'Orders could not be loaded.'} <button className="state-action" onClick={onRetry}>Retry</button></td></tr>}
            {viewState === 'empty' && <tr><td colSpan={8}>No orders match the current view. <button className="state-action" onClick={onClearFilters}>Clear filters</button></td></tr>}
            {viewState === 'ready' && orders.map(order => {
              const completed = order.milestones.filter(m => m.status === 'COMPLETED').length;
              const progress = order.milestones.length ? Math.round(completed / order.milestones.length * 100) : 0;
              return (
                <tr key={order.id}>
                  <td><strong>{order.poNumber}</strong></td><td>{order.buyer.buyerName}</td><td>{order.styleName}</td>
                  <td>{order.totalQuantity.toLocaleString()}</td><td>{order.exFactoryDate}</td>
                  <td><div className="production-line"><span>{completed} / {order.milestones.length}</span><b>{progress}%</b></div></td>
                  <td>{order.status}</td><td><button className="state-action" onClick={() => onOpen(order.id)}>View</button></td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}
