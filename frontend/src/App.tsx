import { useEffect, useState } from 'react';
import { Sidebar } from './components/Sidebar';
import { LoginScreen } from './components/LoginScreen';
import { OrderTable, type Order } from './components/OrderTable';
import { OrderAssignmentPanel } from './components/OrderAssignmentPanel';
import { OrderDelayRiskPanel, ProductionDelaySummary } from './components/ProductionDelayView';
import { useAuth } from './auth/AuthProvider';
import { getDefaultWorkspacePath, isPathWorkspaceAllowed } from './auth/auth';
import { ordersApi, type Buyer, type Template } from './api/orders';
import { productionApi, validatePhotos, type AvailableOrder, type ProductionUpdate } from './api/production';

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
            {isMerchandiser && window.location.hash === '#production' && (
              <ProductionHistory />
            )}
            {isMerchandiser && window.location.hash !== '#production' && !detailId && (
              <button className="primary-action" type="button" onClick={() => setCreateOpen(true)}>Create order</button>
            )}
          </div>

          {!isMerchandiser && (
            <SupervisorProductionWorkspace />
          )}

          {isMerchandiser && !detailId && (
            <>
              <ProductionDelaySummary onOpenOrder={setDetailId} />
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
      <OrderDelayRiskPanel orderId={id} />
      <section className="variant-section"><h3>Color and size breakdown</h3>{order.variants.map(variant => <p key={variant.id ?? `${variant.color}-${variant.size}`}>{variant.color} · {variant.size} · {variant.quantity.toLocaleString()}</p>)}</section>
      <OrderAssignmentPanel orderId={id} />
      <section className="timeline"><h3>TNA timeline</h3>{order.milestones.map(milestone => <article key={milestone.id}><div><strong>{milestone.sequenceNumber}. {milestone.milestoneName}</strong><span>{milestone.status}</span></div><p>{milestone.plannedStartDate} to {milestone.plannedEndDate}</p><p>{milestone.actualStartDate || 'Actual start: —'} · {milestone.actualCompletionDate || 'Actual completion: —'}</p></article>)}</section>
    </section>
  );
}

function SupervisorProductionWorkspace() {
  const [orders, setOrders] = useState<AvailableOrder[]>([]);
  const [updates, setUpdates] = useState<ProductionUpdate[]>([]);
  const [orderId, setOrderId] = useState('');
  const [milestoneId, setMilestoneId] = useState('');
  const [productionDate, setProductionDate] = useState(new Date().toISOString().slice(0, 10));
  const [quantity, setQuantity] = useState('');
  const [issueNote, setIssueNote] = useState('');
  const [photos, setPhotos] = useState<File[]>([]);
  const [photoPreviews, setPhotoPreviews] = useState<string[]>([]);
  const [photoError, setPhotoError] = useState('');
  const [formError, setFormError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [idempotencyKey, setIdempotencyKey] = useState(() =>
    globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).slice(2)}`
  );
  const selectedOrder = orders.find(order => String(order.id) === orderId);

  useEffect(() => () => {
    photoPreviews.forEach(url => URL.revokeObjectURL(url));
  }, [photoPreviews]);

  const load = async () => {
    setLoading(true); setFormError('');
    try {
      const [available, history] = await Promise.all([productionApi.availableOrders(), productionApi.myUpdates()]);
      setOrders(available); setUpdates(history);
    } catch (cause) {
      setFormError(cause instanceof Error ? cause.message : 'Production information could not be loaded.');
    } finally { setLoading(false); }
  };
  useEffect(() => { void load(); }, []);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setFormError(''); setSuccess('');
    const quantityNumber = Number(quantity);
    if (!orderId || !milestoneId) { setFormError('Select an order and production stage.'); return; }
    if (!Number.isInteger(quantityNumber) || quantityNumber < 1) { setFormError('Enter a positive whole quantity.'); return; }
    if (photoError) return;
    setSubmitting(true);
    try {
      await productionApi.submit({
        orderId: Number(orderId), milestoneId: Number(milestoneId), productionDate,
        completedQuantity: quantityNumber, issueNote, photos,
        idempotencyKey,
      });
      setSuccess('Production update saved.');
      setQuantity(''); setIssueNote(''); setPhotos([]); setPhotoError('');
      photoPreviews.forEach(url => URL.revokeObjectURL(url));
      setPhotoPreviews([]);
      setIdempotencyKey(globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).slice(2)}`);
      await load();
    } catch (cause) {
      setFormError(cause instanceof Error ? cause.message : 'Connection error. Retry when the network is available.');
    } finally { setSubmitting(false); }
  };

  if (loading) return <section className="empty-panel">Loading production workspace...</section>;
  return (
    <div className="supervisor-workspace">
      {orders.length === 0 ? (
        <section className="empty-panel supervisor-empty" role={formError ? 'alert' : 'status'}>
          <h2>{formError ? 'Production orders unavailable' : 'No production orders assigned'}</h2>
          <p>{formError || 'Orders assigned to your production units or directly to your account will appear here.'}</p>
          <button className="state-action" type="button" onClick={() => void load()}>Retry</button>
        </section>
      ) : <>
        <section className="assigned-orders" aria-labelledby="assigned-orders-title">
          <div className="section-heading"><div><p>Work available to this account</p><h2 id="assigned-orders-title">Assigned orders</h2></div><span>{orders.length} orders</span></div>
          <div className="assigned-order-list">{orders.map(order => (
            <article className={`assigned-order ${orderId === String(order.id) ? 'selected' : ''}`} key={order.id}>
              <div className="assigned-order-heading"><strong>{order.poNumber}</strong><span>{order.assignmentSource}</span></div>
              <p>{order.buyerName} · {order.styleName}</p>
              <dl><div><dt>Production unit</dt><dd>{order.productionUnit?.name || 'No unit assigned'}</dd></div>
                <div><dt>Quantity</dt><dd>{order.totalQuantity.toLocaleString()}</dd></div>
                <div><dt>Ex-factory</dt><dd>{order.exFactoryDate}</dd></div></dl>
              <button className="state-action" type="button" onClick={() => { setOrderId(String(order.id)); setMilestoneId(''); }}>
                {orderId === String(order.id) ? 'Selected for update' : 'Record production'}
              </button>
            </article>
          ))}</div>
        </section>
        <div className="production-layout">
          <section className="production-entry">
            <div className="section-heading"><div><p>Daily floor entry</p><h2>Record production</h2></div></div>
            {formError && <div className="login-alert" role="alert">{formError} <button className="inline-retry" type="button" onClick={load}>Retry</button></div>}
            {success && <div className="success-alert" role="status">{success}</div>}
            <form onSubmit={submit} className="production-form">
              <label>Order<select value={orderId} onChange={event => { setOrderId(event.target.value); setMilestoneId(''); }}><option value="">Select an order</option>{orders.map(order => <option key={order.id} value={order.id}>{order.poNumber} · {order.styleName}</option>)}</select></label>
              <label>Production stage<select value={milestoneId} onChange={event => setMilestoneId(event.target.value)} disabled={!selectedOrder}><option value="">Select stage</option>{selectedOrder?.milestones.map(milestone => <option key={milestone.id} value={milestone.id}>{milestone.sequenceNumber}. {milestone.milestoneName}</option>)}</select></label>
              <div className="production-fields">
                <label>Production date<input type="date" max={productionDate} value={productionDate} onChange={event => setProductionDate(event.target.value)} /></label>
                <label>Completed quantity<input type="number" min="1" step="1" inputMode="numeric" value={quantity} onChange={event => setQuantity(event.target.value)} placeholder="0" /></label>
              </div>
              <label>Issue note<textarea value={issueNote} maxLength={5000} onChange={event => setIssueNote(event.target.value)} placeholder="Tamil or English" /></label>
              <label className="photo-input">Production photos<input type="file" accept="image/jpeg,image/png,image/webp" multiple onChange={event => {
                const selected = Array.from(event.target.files || []);
                const validation = validatePhotos(selected);
                photoPreviews.forEach(url => URL.revokeObjectURL(url));
                setPhotoError(validation);
                setPhotos(validation ? [] : selected);
                setPhotoPreviews(validation ? [] : selected.map(file => URL.createObjectURL(file)));
              }} /><span>JPEG, PNG or WebP · 10 MB per photo</span></label>
              {photoError && <p className="field-error">{photoError}</p>}
              {photoPreviews.length > 0 && <div className="photo-previews">{photos.map((file, index) => <img key={`${file.name}-${index}`} src={URL.createObjectURL(file)} alt={file.name} />)}</div>}
              <button className="primary-action production-submit" type="submit" disabled={submitting}>{submitting ? 'Saving...' : 'Save production update'}</button>
            </form>
          </section>
          <ProductionHistoryContent updates={updates} title="My update history" emptyText="No production updates recorded yet." />
        </div>
      </>}
    </div>
  );
}

function ProductionHistory() {
  const [updates, setUpdates] = useState<ProductionUpdate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const load = async () => {
    setLoading(true); setError('');
    try { setUpdates(await productionApi.updates()); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Production history could not be loaded.'); }
    finally { setLoading(false); }
  };
  useEffect(() => { void load(); }, []);
  if (loading) return <section className="empty-panel">Loading production history...</section>;
  if (error) return <section className="empty-panel"><h2>Production history unavailable</h2><p>{error}</p><button className="state-action" onClick={load}>Retry</button></section>;
  return <ProductionHistoryContent updates={updates} title="Production updates" emptyText="No production updates have been submitted." />;
}

function ProductionHistoryContent({ updates, title, emptyText }: { updates: ProductionUpdate[]; title: string; emptyText: string }) {
  const [query, setQuery] = useState('');
  const filtered = updates.filter(update =>
    `${update.poNumber} ${update.milestoneName} ${update.supervisorEmail} ${update.issueNote || ''}`
      .toLowerCase().includes(query.toLowerCase()));
  return (
    <section className="production-history">
      <div className="section-heading"><div><p>Submitted records</p><h2>{title}</h2></div><label className="history-search"><span className="sr-only">Filter production updates</span><input value={query} onChange={event => setQuery(event.target.value)} placeholder="Filter updates" /></label></div>
      {filtered.length === 0 ? <div className="empty-panel">{emptyText}</div> : <div className="update-list">{filtered.map(update => (
        <article className="update-record" key={update.id}>
          <div className="update-record-head"><strong>{update.poNumber}</strong><time>{update.productionDate}</time></div>
          <p className="update-stage">{update.milestoneName}</p>
          <div className="update-quantity"><strong>{update.completedQuantity.toLocaleString()}</strong><span>completed</span></div>
          {update.issueNote && <p className="issue-note">{update.issueNote}</p>}
          <small>{update.supervisorEmail}</small>
          {update.photos.length > 0 && <div className="production-photo-links">{update.photos.map(photo => <a key={photo.id} href={productionApi.photoUrl(photo.id)} target="_blank" rel="noreferrer">{photo.originalFilename}</a>)}</div>}
        </article>
      ))}</div>}
    </section>
  );
}

export default App;
