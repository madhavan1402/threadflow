import { useEffect, useMemo, useState } from 'react';
import { Sidebar } from './components/Sidebar';
import { Header } from './components/Header';
import { MetricStrip } from './components/MetricStrip';
import { OrderTable, type Order } from './components/OrderTable';
import { LoginScreen } from './components/LoginScreen';
import { useAuth } from './auth/AuthProvider';

const sampleOrders: Order[] = [
  { po: 'TF-88213', buyer: 'North & Loom · UK', style: "Men's crew neck tee", unit: 'Unit 01 · Tiruppur', stage: 'Stitching', completed: 6800, total: 12000, due: '2026-11-20', status: 'amber', note: 'முன்னேற்பாடு செய்து இருங்கள்' },
  { po: 'TF-88240', buyer: 'Nordic Basics · Sweden', style: "Men's polo shirt", unit: 'Unit 02 · Tiruppur', stage: 'Finishing', completed: 5200, total: 8000, due: '2026-11-12', status: 'red', note: 'நகலம் confirm பண்ணிடுங்க' },
  { po: 'TF-04881', buyer: 'Werkhaus · Germany', style: 'Kids printed tee', unit: 'Print partner · Tiruppur', stage: 'Printing', completed: 3100, total: 10000, due: '2026-11-28', status: 'green', note: 'Print approval வந்தாச்சு' },
  { po: 'TF-88404', buyer: 'Knitfolk · Netherlands', style: "Women's rib cardigan", unit: 'Unit 04 · Tiruppur', stage: 'Knit', completed: 4200, total: 6000, due: '2026-12-02', status: 'green', note: 'Gauge sample approved' },
  { po: 'TF-88417', buyer: 'North & Loom · UK', style: 'Organic polo', unit: 'Unit 06 · Tiruppur', stage: 'Cutting', completed: 2800, total: 7000, due: '2026-11-18', status: 'amber', note: 'Fabric width issue கட்டுப்பாட்டில்' },
  { po: 'TF-88502', buyer: 'Werkhaus · Germany', style: 'Ladies henley', unit: 'Unit 03 · Tiruppur', stage: 'QC & packing', completed: 5900, total: 6000, due: '2026-11-14', status: 'green', note: 'Carton packing இன்று தொடங்கும்' },
  { po: 'TF-88519', buyer: 'Nordic Basics · Sweden', style: 'Boys striped tee', unit: 'Unit 05 · Tiruppur', stage: 'Stitching', completed: 2400, total: 9000, due: '2026-11-25', status: 'green', note: 'Stripe yarn allocation done' },
  { po: 'TF-88573', buyer: 'Knitfolk · Netherlands', style: 'Heavyweight sweatshirt', unit: 'Unit 01 · Tiruppur', stage: 'Dyeing', completed: 1800, total: 5000, due: '2026-12-08', status: 'amber', note: 'Shade review pending' },
];

export type ViewState = 'ready' | 'loading' | 'empty' | 'error';

function App() {
  const { user, loading, logout } = useAuth();
  const [viewState, setViewState] = useState<ViewState>('ready');
  const [query, setQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<'all' | Order['status']>('all');
  const [menuOpen, setMenuOpen] = useState(false);
  const [toast, setToast] = useState('');

  useEffect(() => {
    if (!toast) return;
    const timeout = window.setTimeout(() => setToast(''), 2600);
    return () => window.clearTimeout(timeout);
  }, [toast]);

  useEffect(() => {
    if (!user) return;
    const hash = window.location.hash;
    if (hash === '#orders' && !user.roles.includes('MERCHANDISER')) {
      window.location.hash = '#production';
    }
    if (hash === '#production' && !user.roles.includes('SUPERVISOR')) {
      window.location.hash = '#overview';
    }
  }, [user]);

  const filteredOrders = useMemo(() => sampleOrders.filter((order) => {
    const matchesQuery = `${order.po} ${order.buyer} ${order.style} ${order.note}`
      .toLowerCase()
      .includes(query.toLowerCase());
    const matchesStatus = statusFilter === 'all' || order.status === statusFilter;
    return matchesQuery && matchesStatus;
  }), [query, statusFilter]);

  const statusCounts = useMemo(() => ({
    green: sampleOrders.filter((order) => order.status === 'green').length,
    amber: sampleOrders.filter((order) => order.status === 'amber').length,
    red: sampleOrders.filter((order) => order.status === 'red').length,
  }), []);

  const refresh = () => {
    setViewState('loading');
    window.setTimeout(() => {
      setViewState('ready');
      setToast('Production snapshot refreshed');
    }, 650);
  };

  const retry = () => {
    setViewState('loading');
    window.setTimeout(() => setViewState('ready'), 650);
  };

  if (loading) {
    return <main className="auth-loading" role="status">Opening ThreadFlow workspace...</main>;
  }

  if (!user) {
    return <LoginScreen />;
  }


  return (
    <div className="app-shell">
      <Sidebar
        menuOpen={menuOpen}
        onClose={() => setMenuOpen(false)}
        roles={user.roles}
        email={user.email}
        onLogout={() => { void logout(); }}
      />
      <div className="workspace">
        <Header
          onMenu={() => setMenuOpen(true)}
          onRefresh={refresh}
          viewState={viewState}
          onViewStateChange={setViewState}
        />
        <main className="workspace-main" id="overview">
          <div className="title-row">
            <div>
              <p className="title-context">Production workspace · Tiruppur</p>
              <h1>Production overview</h1>
              <p className="title-finding">
                Two orders need attention before the next planning review.
              </p>
            </div>
            <button
              className="primary-action"
              type="button"
              onClick={() => {
                setToast('Order creation is available after backend setup');
              }}
            >
              Create order
            </button>
          </div>

          <MetricStrip total={sampleOrders.length} counts={statusCounts} />

          <OrderTable
            orders={filteredOrders}
            totalOrders={sampleOrders.length}
            viewState={viewState}
            query={query}
            statusFilter={statusFilter}
            onQueryChange={setQuery}
            onStatusFilterChange={setStatusFilter}
            onRetry={retry}
            onClearFilters={() => {
              setQuery('');
              setStatusFilter('all');
            }}
          />
        </main>
      </div>
      {toast && <div className="toast" role="status">{toast}</div>}
    </div>
  );
}

export default App;
