import { useEffect, useState } from 'react';
import { Sidebar } from './components/Sidebar';
import { LoginScreen } from './components/LoginScreen';
import { OrderTable, type Order } from './components/OrderTable';
import { useAuth } from './auth/AuthProvider';
import { getDefaultWorkspacePath, isPathWorkspaceAllowed } from './auth/auth';
import { ordersApi, type Buyer, type Template } from './api/orders';

export type ViewState = 'ready' | 'loading' | 'empty' | 'error';

function App() {
  const { user, loading, logout } = useAuth();
  const [orders, setOrders] = useState<Order[]>([]);
  const [viewState, setViewState] = useState<ViewState>('ready');
  const [error, setError] = useState('');
  const [query, setQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<'all' | string>('all');
  const [buyerId, setBuyerId] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [buyers, setBuyerList] = useState<Buyer[]>([]);
  const [templates, setTemplates] = useState<Template[]>([]);
  const [createOpen, setCreateOpen] = useState(false);
  const [detailId, setDetailId] = useState<number | null>(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const isMerchandiser = !!user?.roles.includes('MERCHANDISER');

  const loadOrders = async () => {
    if (!isMerchandiser) return;
    setViewState('loading');
    setError('');
    const params = new URLSearchParams();
    if (query) params.set('search', query);
    if (statusFilter !== 'all') params.set('status', statusFilter);
    if (buyerId) params.set('buyer', buyerId);
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    try {
      const page = await ordersApi.list(params.size ? `?${params}` : '');
      setOrders(page.content);
      setViewState(page.content.length ? 'ready' : 'empty');
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Orders could not be loaded.');
      setViewState('error');
    }
  };

  useEffect(() => {
    if (!user) return;
    if (window.location.pathname === '/') window.history.replaceState({}, '', getDefaultWorkspacePath(user.roles));
    if (isMerchandiser) {
      void loadOrders();
      void ordersApi.buyers().then(setBuyerList).catch(() => setBuyerList([]));
      void ordersApi.templates().then(setTemplates).catch(() => setTemplates([]));
    }
  }, [user]);

  useEffect(() => {
    if (!loading && !user && window.location.pathname !== '/login') window.history.replaceState({}, '', '/login');
  }, [loading, user]);

  if (loading) return <main className="auth-loading" role="status">Opening ThreadFlow workspace...</main>;
  if (!user) return <LoginScreen />;
  if (!isPathWorkspaceAllowed(window.location.pathname, user.roles)) {
    return (
      <main className="access-denied" role="alert">
        <h1>Workspace access denied</h1>
        <p>Your account does not have permission to open this workspace.</p>
        <button className="login-submit" type="button" onClick={() => {
          window.history.pushState({}, '', getDefaultWorkspacePath(user.roles));
        }}>Return to my workspace</button>
      </main>
    );
  }

  const counts = {
    inProgress: orders.filter(order => order.status === 'IN_PROGRESS').length,
    completed: orders.filter(order => order.status === 'COMPLETED').length,
  };

  return (
    <div className="app-shell">
      <Sidebar menuOpen={menuOpen} onClose={() => setMenuOpen(false)} roles={user.roles}
        email={user.email} onLogout={() => { void logout(); }} activeHash={window.location.hash || '#orders'} />
      <div className="workspace">
        <main className="workspace-main" id="orders">
          <div className="title-row">
            <div>
              <p className="title-context">{isMerchandiser ? 'Merchandising workspace · Tiruppur' : 'Production workspace · Tiruppur'}</p>
              <h1>{detailId ? 'Order detail' : isMerchandiser ? 'Production orders' : 'Production overview'}</h1>
            </div>
            {isMerchandiser && !detailId && (
              <button className="primary-action" type="button" onClick={() => setCreateOpen(true)}>Create order</button>
            )}
          </div>

          {!isMerchandiser && (
            <section className="empty-panel">
              <h2>Restricted order information</h2>
              <p>This account is not assigned to production orders in the current access model.</p>
            </section>
          )}

          {isMerchandiser && !detailId && (
            <>
              <section className="metric-strip" aria-label="Order summary">
                <article className="metric"><p>Total orders</p><strong>{orders.length}</strong></article>
                <article className="metric"><p>In progress</p><strong>{counts.inProgress}</strong></article>
                <article className="metric"><p>At risk</p><strong>0</strong><span>Risk calculations not configured in this phase</span></article>
                <article className="metric"><p>Completed</p><strong>{counts.completed}</strong></article>
              </section>
              <div className="order-filters">
                <label>Buyer
                  <select value={buyerId} onChange={event => setBuyerId(event.target.value)}>
                    <option value="">All buyers</option>
                    {buyers.map(buyer => <option key={buyer.id} value={buyer.id}>{buyer.buyerName}</option>)}
                  </select>
                </label>
                <label>Status
                  <select value={statusFilter} onChange={event => setStatusFilter(event.target.value)}>
                    <option value="all">All statuses</option><option value="PLANNED">Planned</option>
                    <option value="IN_PROGRESS">In progress</option><option value="COMPLETED">Completed</option>
                    <option value="CANCELLED">Cancelled</option>
                  </select>
                </label>
                <label>Ex-factory from<input type="date" value={from} onChange={event => setFrom(event.target.value)} /></label>
                <label>Ex-factory to<input type="date" value={to} onChange={event => setTo(event.target.value)} /></label>
                <button className="state-action" type="button" onClick={loadOrders}>Apply</button>
              </div>
              <OrderTable orders={orders} totalOrders={orders.length} viewState={viewState}
                query={query} statusFilter={statusFilter}
                onQueryChange={setQuery} onStatusFilterChange={setStatusFilter}
                onRetry={loadOrders} onClearFilters={() => { setQuery(''); setBuyerId(''); setStatusFilter('all'); setFrom(''); setTo(''); }}
                errorMessage={error} onOpen={setDetailId} />
            </>
          )}
          {detailId && <OrderDetail id={detailId} onBack={() => setDetailId(null)} />}
        </main>
      </div>
      {createOpen && <CreateOrder buyers={buyers} templates={templates} onClose={() => setCreateOpen(false)}
        onCreated={() => { setCreateOpen(false); void loadOrders(); }} />}
    </div>
  );
}

function CreateOrder({ buyers, templates, onClose, onCreated }: {
  buyers: Buyer[]; templates: Template[]; onClose: () => void; onCreated: () => void;
}) {
  const [buyerId, setBuyerId] = useState('');
  const [poNumber, setPoNumber] = useState('');
  const [styleName, setStyleName] = useState('');
  const [description, setDescription] = useState('');
  const [quantity, setQuantity] = useState('');
  const [exFactory, setExFactory] = useState('');
  const [templateId, setTemplateId] = useState('');
  const [error, setError] = useState('');
  const [variants, setVariants] = useState<Array<{ color: string; size: string; quantity: string }>>([
    { color: '', size: '', quantity: '' },
  ]);
  const selectedTemplate = templates.find(template => String(template.id) === templateId);
  const variantTotal = variants.reduce((sum, variant) => sum + Number(variant.quantity || 0), 0);

  const save = async () => {
    setError('');
    if (!buyerId || !poNumber || !styleName || !quantity || !exFactory || !templateId) {
      setError('Buyer, PO number, style, quantity, ex-factory date and template are required.');
      return;
    }
    if (variantTotal !== Number(quantity)) {
      setError('Variant quantities must add up to the total quantity.');
      return;
    }
    try {
      await ordersApi.create({
        buyerId: Number(buyerId), poNumber, styleName, productDescription: description,
        totalQuantity: Number(quantity), exFactoryDate: exFactory, templateId: Number(templateId),
        variants: variants.map(variant => ({ ...variant, quantity: Number(variant.quantity) })),
      });
      onCreated();
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Order could not be saved.'); }
  };

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="order-modal" role="dialog" aria-modal="true" aria-label="Create production order">
        <div className="modal-title"><div><p>New production order</p><h2>Order details</h2></div>
          <button type="button" aria-label="Close" onClick={onClose}>Close</button></div>
        {error && <div className="form-error" role="alert">{error}</div>}
        <div className="create-grid">
          <label>Buyer<select value={buyerId} onChange={event => setBuyerId(event.target.value)}><option value="">Select buyer</option>{buyers.map(b => <option key={b.id} value={b.id}>{b.buyerName}, {b.country}</option>)}</select></label>
          <label>PO number<input value={poNumber} onChange={event => setPoNumber(event.target.value)} /></label>
          <label>Style name<input value={styleName} onChange={event => setStyleName(event.target.value)} /></label>
          <label>Description<textarea value={description} onChange={event => setDescription(event.target.value)} /></label>
          <label>Total quantity<input type="number" min="1" value={quantity} onChange={event => setQuantity(event.target.value)} /></label>
          <label>Ex-factory date<input type="date" value={exFactory} onChange={event => setExFactory(event.target.value)} /></label>
          <label>TNA template<select value={templateId} onChange={event => setTemplateId(event.target.value)}><option value="">Select template</option>{templates.map(t => <option key={t.id} value={t.id}>{t.templateName}</option>)}</select></label>
        </div>
        <div className="variant-section">
          <div><strong>Color and size variants</strong><span>Current total: {variantTotal}</span></div>
          {variants.map((variant, index) => (
            <div className="variant-row" key={index}>
              <input aria-label="Color" placeholder="Color" value={variant.color} onChange={event => setVariants(items => items.map((item, i) => i === index ? { ...item, color: event.target.value } : item))} />
              <input aria-label="Size" placeholder="Size" value={variant.size} onChange={event => setVariants(items => items.map((item, i) => i === index ? { ...item, size: event.target.value } : item))} />
              <input aria-label="Quantity" type="number" min="1" placeholder="Quantity" value={variant.quantity} onChange={event => setVariants(items => items.map((item, i) => i === index ? { ...item, quantity: event.target.value } : item))} />
              <button type="button" aria-label="Remove variant" disabled={variants.length === 1} onClick={() => setVariants(items => items.filter((_, i) => i !== index))}>Remove</button>
            </div>
          ))}
          <button className="state-action" type="button" onClick={() => setVariants(items => [...items, { color: '', size: '', quantity: '' }])}>Add variant</button>
        </div>
        {selectedTemplate && <section className="milestone-preview"><h3>Milestone preview</h3>{selectedTemplate.milestones.map(m => <p key={m.id}>{m.sequenceNumber}. {m.milestoneName} · {m.defaultDurationDays} days</p>)}</section>}
        <div className="modal-actions"><button className="state-action" type="button" onClick={onClose}>Cancel</button><button className="primary-action" type="button" onClick={() => void save()}>Save order</button></div>
      </section>
    </div>
  );
}

function OrderDetail({ id, onBack }: { id: number; onBack: () => void }) {
  const [order, setOrder] = useState<Order | null>(null);
  const [error, setError] = useState('');
  useEffect(() => {
    ordersApi.get(id).then(setOrder).catch(cause => setError(cause instanceof Error ? cause.message : 'Order could not be loaded.'));
  }, [id]);
  if (error) return <section className="empty-panel"><h2>Order unavailable</h2><p>{error}</p><button className="state-action" onClick={onBack}>Back to orders</button></section>;
  if (!order) return <section className="empty-panel">Loading order...</section>;
  return (
    <section>
      <button className="state-action" type="button" onClick={onBack}>Back to orders</button>
      <div className="detail-header"><div><p>{order.poNumber}</p><h2>{order.styleName}</h2></div><strong>{order.status}</strong></div>
      <dl className="detail-grid"><div><dt>Buyer</dt><dd>{order.buyer.buyerName}, {order.buyer.country}</dd></div><div><dt>Total quantity</dt><dd>{order.totalQuantity.toLocaleString()}</dd></div><div><dt>Ex-factory</dt><dd>{order.exFactoryDate}</dd></div></dl>
      <section className="variant-section"><h3>Color and size breakdown</h3>{order.variants.map(variant => <p key={variant.id ?? `${variant.color}-${variant.size}`}>{variant.color} · {variant.size} · {variant.quantity.toLocaleString()}</p>)}</section>
      <section className="timeline"><h3>TNA timeline</h3>{order.milestones.map(milestone => <article key={milestone.id}><div><strong>{milestone.sequenceNumber}. {milestone.milestoneName}</strong><span>{milestone.status}</span></div><p>{milestone.plannedStartDate} to {milestone.plannedEndDate}</p><p>{milestone.actualStartDate || 'Actual start: —'} · {milestone.actualCompletionDate || 'Actual completion: —'}</p></article>)}</section>
    </section>
  );
}

export default App;
