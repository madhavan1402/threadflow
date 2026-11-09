import { useState, useMemo } from 'react';
import { Search, ArrowUpDown, ArrowUp, ArrowDown, ExternalLink } from 'lucide-react';
import type { ProductionOrder } from '../api/orders';
import type { ViewState } from '../App';
export type Order = ProductionOrder;

type SortField = 'poNumber' | 'buyer' | 'style' | 'quantity' | 'exFactory' | 'progress' | 'status';
type SortOrder = 'asc' | 'desc';

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
  errorMessage,
  onOpen,
}: {
  orders: ProductionOrder[];
  totalOrders: number;
  viewState: ViewState;
  query: string;
  statusFilter: string;
  onQueryChange: (value: string) => void;
  onStatusFilterChange: (value: string) => void;
  onRetry: () => void;
  onClearFilters: () => void;
  errorMessage?: string;
  onOpen: (id: number) => void;
}) {
  const [sortField, setSortField] = useState<SortField>('exFactory');
  const [sortOrder, setSortOrder] = useState<SortOrder>('asc');

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortOrder('asc');
    }
  };

  const sortedOrders = useMemo(() => {
    return [...orders].sort((a, b) => {
      let cmp = 0;
      if (sortField === 'poNumber') cmp = a.poNumber.localeCompare(b.poNumber);
      else if (sortField === 'buyer') cmp = a.buyer.buyerName.localeCompare(b.buyer.buyerName);
      else if (sortField === 'style') cmp = a.styleName.localeCompare(b.styleName);
      else if (sortField === 'quantity') cmp = a.totalQuantity - b.totalQuantity;
      else if (sortField === 'exFactory') cmp = new Date(a.exFactoryDate).getTime() - new Date(b.exFactoryDate).getTime();
      else if (sortField === 'status') cmp = a.status.localeCompare(b.status);
      else if (sortField === 'progress') {
        const progA = a.milestones.filter(m => m.status === 'COMPLETED').length / (a.milestones.length || 1);
        const progB = b.milestones.filter(m => m.status === 'COMPLETED').length / (b.milestones.length || 1);
        cmp = progA - progB;
      }
      return sortOrder === 'asc' ? cmp : -cmp;
    });
  }, [orders, sortField, sortOrder]);

  const renderSortIndicator = (field: SortField) => {
    if (sortField !== field) return <ArrowUpDown size={12} className="sort-icon-inactive" />;
    return sortOrder === 'asc' ? (
      <ArrowUp size={12} className="sort-icon-active" />
    ) : (
      <ArrowDown size={12} className="sort-icon-active" />
    );
  };

  return (
    <section className="order-section" id="orders">
      <div className="section-title-row">
        <div>
          <h2>Orders</h2>
          <p>Ex-factory plan and milestone progress</p>
        </div>
      </div>

      <div className="toolbar">
        <label className="search-field">
          <Search size={15} aria-hidden="true" />
          <span className="sr-only">Search orders</span>
          <input
            value={query}
            onChange={(event) => onQueryChange(event.target.value)}
            onBlur={() => onStatusFilterChange(statusFilter)}
            placeholder="Search PO or style"
          />
        </label>
        <span className="result-count">
          {orders.length} of {totalOrders} orders
        </span>
      </div>

      <div className="table-frame">
        <table className="order-table">
          <thead>
            <tr>
              <th onClick={() => handleSort('poNumber')} className="sortable-th">
                <div className="th-content">PO number {renderSortIndicator('poNumber')}</div>
              </th>
              <th onClick={() => handleSort('buyer')} className="sortable-th">
                <div className="th-content">Buyer {renderSortIndicator('buyer')}</div>
              </th>
              <th onClick={() => handleSort('style')} className="sortable-th">
                <div className="th-content">Style {renderSortIndicator('style')}</div>
              </th>
              <th onClick={() => handleSort('quantity')} className="sortable-th">
                <div className="th-content">Quantity {renderSortIndicator('quantity')}</div>
              </th>
              <th onClick={() => handleSort('exFactory')} className="sortable-th">
                <div className="th-content">Ex-factory {renderSortIndicator('exFactory')}</div>
              </th>
              <th onClick={() => handleSort('progress')} className="sortable-th">
                <div className="th-content">Milestone progress {renderSortIndicator('progress')}</div>
              </th>
              <th onClick={() => handleSort('status')} className="sortable-th">
                <div className="th-content">Status {renderSortIndicator('status')}</div>
              </th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {viewState === 'loading' && (
              <tr>
                <td colSpan={8} className="table-status-cell">
                  <div className="loading-spinner-wrap">
                    <span className="spinner-sm" />
                    <span>Loading orders...</span>
                  </div>
                </td>
              </tr>
            )}
            {viewState === 'error' && (
              <tr>
                <td colSpan={8} className="table-status-cell">
                  <span className="error-text">{errorMessage || 'Orders could not be loaded.'}</span>
                  <button className="state-action" onClick={onRetry}>
                    Retry
                  </button>
                </td>
              </tr>
            )}
            {viewState === 'empty' && (
              <tr>
                <td colSpan={8} className="table-status-cell">
                  <span>No orders match the current view.</span>
                  <button className="state-action" onClick={onClearFilters}>
                    Clear filters
                  </button>
                </td>
              </tr>
            )}
            {viewState === 'ready' &&
              sortedOrders.map((order) => {
                const completed = order.milestones.filter((m) => m.status === 'COMPLETED').length;
                const progress = order.milestones.length
                  ? Math.round((completed / order.milestones.length) * 100)
                  : 0;
                return (
                  <tr key={order.id} className="order-row" onClick={() => onOpen(order.id)}>
                    <td className="cell-po">
                      <strong>{order.poNumber}</strong>
                    </td>
                    <td>
                      <span className="cell-buyer">{order.buyer.buyerName}</span>
                      <span className="cell-country">{order.buyer.country}</span>
                    </td>
                    <td className="cell-style">{order.styleName}</td>
                    <td className="cell-qty">{order.totalQuantity.toLocaleString()} pcs</td>
                    <td className="cell-date">
                      <time dateTime={order.exFactoryDate}>{order.exFactoryDate}</time>
                    </td>
                    <td>
                      <div className="production-line">
                        <div className="line-bar-track">
                          <div className="line-bar-fill" style={{ width: `${progress}%` }} />
                        </div>
                        <div className="line-bar-meta">
                          <span>{completed} / {order.milestones.length}</span>
                          <b>{progress}%</b>
                        </div>
                      </div>
                    </td>
                    <td>
                      <span className={`status-pill status-${order.status.toLowerCase()}`}>
                        {order.status.replace('_', ' ')}
                      </span>
                    </td>
                    <td>
                      <button
                        className="state-action button-open-order"
                        onClick={(e) => {
                          e.stopPropagation();
                          onOpen(order.id);
                        }}
                        title={`Open PO ${order.poNumber}`}
                      >
                        <span>View</span>
                        <ExternalLink size={12} />
                      </button>
                    </td>
                  </tr>
                );
              })}
          </tbody>
        </table>
      </div>
    </section>
  );
}
