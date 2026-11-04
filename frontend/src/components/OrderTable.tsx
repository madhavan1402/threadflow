import { MoreHorizontal, Search } from 'lucide-react';
import { useState } from 'react';
import type { ViewState } from '../App';
import { StatusBadge } from './StatusBadge';

export type Order = {
  po: string;
  buyer: string;
  style: string;
  unit: string;
  stage: string;
  completed: number;
  total: number;
  due: string;
  status: 'green' | 'amber' | 'red';
  note: string;
};

export function OrderTable({
  orders,
  totalOrders,
  viewState,
  query,
  statusFilter,
  onQueryChange,
  onStatusFilterChange,
  onRetry,
  onClearFilters,
}: {
  orders: Order[];
  totalOrders: number;
  viewState: ViewState;
  query: string;
  statusFilter: 'all' | Order['status'];
  onQueryChange: (value: string) => void;
  onStatusFilterChange: (value: 'all' | Order['status']) => void;
  onRetry: () => void;
  onClearFilters: () => void;
}) {
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  return (
    <section className="order-section" id="orders">
      <div className="section-title-row">
        <div>
          <h2>Orders</h2>
          <p>Current production progress and ex-factory dates</p>
        </div>
      </div>
      <div className="order-tabs" role="tablist" aria-label="Order status">
        <button className={`order-tab ${statusFilter === 'all' ? 'active' : ''}`} type="button" onClick={() => onStatusFilterChange('all')}>All orders</button>
        <button className={`order-tab ${statusFilter === 'green' ? 'active' : ''}`} type="button" onClick={() => onStatusFilterChange('green')}>On track</button>
        <button className={`order-tab ${statusFilter === 'amber' ? 'active' : ''}`} type="button" onClick={() => onStatusFilterChange('amber')}>At risk</button>
        <button className={`order-tab ${statusFilter === 'red' ? 'active' : ''}`} type="button" onClick={() => onStatusFilterChange('red')}>Delayed</button>
      </div>
      <div className="toolbar">
        <label className="search-field">
          <Search size={15} aria-hidden="true" />
          <span className="sr-only">Search orders</span>
          <input value={query} onChange={(event) => onQueryChange(event.target.value)} placeholder="Search PO, buyer or style" />
        </label>
        <span className="result-count">{orders.length} of {totalOrders} orders</span>
      </div>
      <div className="table-frame">
        <table className="order-table">
          <thead>
            <tr>
              <th>Order / style</th>
              <th>Buyer</th>
              <th>Stage</th>
              <th>Production</th>
              <th>Ex-factory</th>
              <th>Status</th>
              <th><span className="sr-only">Actions</span></th>
            </tr>
          </thead>
          <tbody>
            {viewState === 'loading' && <tr className="loading-row"><td colSpan={7}><span className="loading-bar wide" /> <span className="loading-bar medium" /></td></tr>}
            {viewState === 'error' && <tr className="error-row"><td colSpan={7}>Production snapshot could not be loaded.<br /><button className="state-action" type="button" onClick={onRetry}>Retry</button></td></tr>}
            {viewState === 'empty' && <tr className="empty-row"><td colSpan={7}>No orders match the current view.<br /><button className="state-action" type="button" onClick={onClearFilters}>Clear filters</button></td></tr>}
            {viewState === 'ready' && orders.map((order) => {
              const percentage = Math.round(order.completed / order.total * 100);
              return (
                <tr key={order.po}>
                  <td className="identity"><strong>{order.po}</strong><span>{order.style}</span></td>
                  <td className="buyer"><strong>{order.buyer}</strong><span>{order.unit}</span></td>
                  <td>{order.stage}</td>
                  <td>
                    <div className="production-line"><span>{order.completed.toLocaleString()} / {order.total.toLocaleString()}</span><b>{percentage}%</b></div>
                    <div className="progress-track"><span className={order.status} style={{ width: `${percentage}%` }} /></div>
                  </td>
                  <td className="two-line" title={order.note}><strong>{order.due}</strong></td>
                  <td><StatusBadge status={order.status} /></td>
                  <td>
                    <button
                      className="row-action"
                      type="button"
                      aria-label={`View actions for ${order.po}`}
                      aria-expanded={selectedOrder?.po === order.po}
                      onClick={() => setSelectedOrder(selectedOrder?.po === order.po ? null : order)}
                    >
                      <MoreHorizontal size={17} />
                    </button>
                    {selectedOrder?.po === order.po && (
                      <div className="row-menu" role="dialog" aria-label={`Actions for ${order.po}`}>
                        <strong>{order.po}</strong>
                        <span>Production note</span>
                        <p>{order.note}</p>
                      </div>
                    )}
                  </td>
                </tr>
              );
            })}
            {viewState === 'ready' && orders.length === 0 && <tr className="empty-row"><td colSpan={7}>No orders match your search.<br /><button className="state-action" type="button" onClick={onClearFilters}>Clear filters</button></td></tr>}
          </tbody>
        </table>
        <div className="table-footer"><span>Showing {orders.length} of {totalOrders} orders</span><span>Sample snapshot</span></div>
      </div>
    </section>
  );
}
