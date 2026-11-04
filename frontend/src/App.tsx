import { useEffect, useState } from 'react';
import { Activity, AlertTriangle, ArrowUpRight, Bell, Boxes, CalendarDays, CheckCircle2, ChevronDown, Factory, Menu, Plus, Search, Shirt, Users } from 'lucide-react';

type Health = { status: string; service: string; timestamp: string };

const orders = [
  { po: 'B-PO-88213', buyer: 'Company B · UK', style: "Men's crew neck tee", unit: 'Unit 01 · Tiruppur', stage: 'Stitching', done: 6800, total: 12000, due: '20 Nov 2026', status: 'amber' },
  { po: 'B-PO-88240', buyer: 'Company B · UK', style: "Men's polo", unit: 'Unit 02 · Tiruppur', stage: 'Finishing', done: 5200, total: 8000, due: '12 Nov 2026', status: 'red' },
  { po: 'C-PO-0488', buyer: 'Company C · Germany', style: 'Kids printed tee', unit: 'Partner · Printing', stage: 'Printing', done: 3100, total: 10000, due: '28 Nov 2026', status: 'green' },
];

function App() {
  const [apiStatus, setApiStatus] = useState<'checking' | 'connected' | 'offline'>('checking');
  const [query, setQuery] = useState('');

  useEffect(() => {
    fetch('/api/health')
      .then((response) => {
        if (!response.ok) throw new Error('API unavailable');
        return response.json() as Promise<Health>;
      })
      .then(() => setApiStatus('connected'))
      .catch(() => setApiStatus('offline'));
  }, []);

  const filteredOrders = orders.filter((order) =>
    `${order.po} ${order.buyer} ${order.style}`.toLowerCase().includes(query.toLowerCase()),
  );

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand"><div className="brand-mark"><Shirt size={20} /></div><span>threadflow</span></div>
        <div className="workspace-label">WORKSPACE</div>
        <nav>
          <a className="nav-item active"><Activity size={18} /> Overview</a>
          <a className="nav-item"><Boxes size={18} /> Orders <span className="nav-count">24</span></a>
          <a className="nav-item"><Factory size={18} /> Production</a>
          <a className="nav-item"><CalendarDays size={18} /> TNA calendar</a>
          <a className="nav-item"><Users size={18} /> Buyers</a>
        </nav>
        <div className="sidebar-bottom">
          <div className="profile-avatar">MN</div>
          <div><strong>Merchandising team</strong><small>Demo workspace</small></div>
          <ChevronDown size={15} />
        </div>
      </aside>

      <main className="main">
        <header className="topbar">
          <button className="mobile-menu" aria-label="Open menu"><Menu size={20}/></button>
          <div className="breadcrumb">Workspace <span>/</span> Overview</div>
          <div className="top-actions"><span className={`api-pill ${apiStatus}`}><i /> API {apiStatus === 'connected' ? 'connected' : apiStatus === 'offline' ? 'offline' : 'checking'}</span><button className="icon-button" aria-label="Notifications"><Bell size={18}/></button><div className="profile-avatar small">MN</div></div>
        </header>

        <section className="page-heading">
          <div><div className="eyebrow">MONDAY, 03 NOVEMBER 2026</div><h1>Good morning, team <span>✳</span></h1><p>Here's what's happening across your production floor today.</p></div>
          <button className="primary-button"><Plus size={17}/> Create order</button>
        </section>

        <section className="metric-grid">
          <Metric label="Active orders" value="24" change="+3 this week" icon={<Boxes size={18}/>} />
          <Metric label="On track" value="18" change="75% of active orders" icon={<CheckCircle2 size={18}/>} tone="green"/>
          <Metric label="At risk" value="4" change="Needs attention" icon={<AlertTriangle size={18}/>} tone="amber"/>
          <Metric label="Delayed" value="2" change="Requires action today" icon={<Activity size={18}/>} tone="red"/>
        </section>

        <section className="content-card">
          <div className="section-head"><div><h2>Production overview</h2><p>Track order progress and identify delays early.</p></div><button className="secondary-button">View all orders <ArrowUpRight size={15}/></button></div>
          <div className="table-toolbar"><div className="search-box"><Search size={16}/><input aria-label="Search orders" placeholder="Search PO, buyer or style..." value={query} onChange={(e) => setQuery(e.target.value)}/></div><button className="filter-button">All statuses <ChevronDown size={15}/></button></div>
          <div className="table-wrap"><table><thead><tr><th>ORDER / STYLE</th><th>BUYER</th><th>STAGE</th><th>PRODUCTION</th><th>EX-FACTORY</th><th>STATUS</th></tr></thead><tbody>
            {filteredOrders.map((order) => <tr key={order.po}><td><strong>{order.po}</strong><small>{order.style}</small></td><td>{order.buyer}<small>{order.unit}</small></td><td>{order.stage}</td><td><div className="progress-label"><span>{order.done.toLocaleString()} / {order.total.toLocaleString()}</span><b>{Math.round(order.done / order.total * 100)}%</b></div><div className="progress-track"><div className={`progress-fill ${order.status}`} style={{width: `${order.done / order.total * 100}%`}}/></div></td><td>{order.due}</td><td><span className={`status ${order.status}`}><i/>{order.status === 'green' ? 'On track' : order.status === 'amber' ? 'At risk' : 'Delayed'}</span></td></tr>)}
            {filteredOrders.length === 0 && <tr><td colSpan={6} className="empty-state">No orders match your search.</td></tr>}
          </tbody></table></div>
          <div className="table-footer">Showing {filteredOrders.length} of 3 seeded orders <span>Updated just now</span></div>
        </section>
        <footer>ThreadFlow <span>·</span> Production intelligence for every stitch.</footer>
      </main>
    </div>
  );
}

function Metric({ label, value, change, icon, tone = 'neutral' }: { label: string; value: string; change: string; icon: React.ReactNode; tone?: string }) {
  return <article className="metric-card"><div className={`metric-icon ${tone}`}>{icon}</div><div className="metric-label">{label}</div><div className="metric-value">{value}</div><div className="metric-change">{change}</div></article>;
}

export default App;
