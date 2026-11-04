import { Boxes, Factory, LayoutDashboard } from 'lucide-react';

const navItems = [
  { label: 'Overview', icon: LayoutDashboard, target: '#overview' },
  { label: 'Orders', icon: Boxes, target: '#orders' },
  { label: 'Production', icon: Factory, target: '#production' },
];

export function Sidebar({ menuOpen, onClose }: { menuOpen: boolean; onClose: () => void }) {
  return (
    <>
      {menuOpen && <button className="menu-backdrop" aria-label="Close menu" onClick={onClose} />}
      <aside className={`sidebar ${menuOpen ? 'open' : ''}`}>
        <div className="brand">
          <div className="brand-mark" aria-hidden="true"><span /></div>
          <span>ThreadFlow</span>
        </div>
        <div className="workspace-switcher">
          <div className="workspace-monogram">TF</div>
          <div>
            <strong>Merchandising</strong>
            <span>Tiruppur workspace</span>
          </div>
        </div>
        <nav className="nav" aria-label="Primary navigation">
          <p className="nav-group">Workspace</p>
          {navItems.map((item, index) => {
            const Icon = item.icon;
            return (
              <a
                className={`nav-item ${index === 0 ? 'active' : ''}`}
                href={item.target}
                key={item.label}
                onClick={onClose}
              >
                <Icon size={17} strokeWidth={1.8} />
                <span>{item.label}</span>
              </a>
            );
          })}
        </nav>
        <div className="sidebar-profile">
          <div className="profile-avatar">MN</div>
          <div>
            <strong>M. Natarajan</strong>
            <span>Merchandising lead</span>
          </div>
        </div>
      </aside>
    </>
  );
}
