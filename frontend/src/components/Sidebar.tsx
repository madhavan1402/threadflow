import { Boxes, Factory, LayoutDashboard, LogOut } from 'lucide-react';
import type { UserRole } from '../auth/auth';

const navItems = [
  { label: 'Overview', icon: LayoutDashboard, target: '#overview' },
  { label: 'Orders', icon: Boxes, target: '#orders' },
  { label: 'Production', icon: Factory, target: '#production' },
];

export function Sidebar({
  menuOpen,
  onClose,
  roles,
  email,
  onLogout,
  activeHash,
}: {
  menuOpen: boolean;
  onClose: () => void;
  roles: UserRole[];
  email: string;
  onLogout: () => void;
  activeHash: string;
}) {
  const isMerchandiser = roles.includes('MERCHANDISER');
  const roleLabel = isMerchandiser ? 'Merchandising' : 'Production';
  const visibleNavItems = navItems.filter((item) => {
    if (item.label === 'Orders') return roles.includes('MERCHANDISER');
    if (item.label === 'Production') return roles.includes('SUPERVISOR');
    return true;
  });
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
            <strong>{isMerchandiser ? 'Merchandising' : 'Production'}</strong>
            <span>Tiruppur workspace</span>
          </div>
        </div>
        <nav className="nav" aria-label="Primary navigation">
          <p className="nav-group">Workspace</p>
          {visibleNavItems.map((item, index) => {
            const Icon = item.icon;
            return (
              <a
                className={`nav-item ${item.target === activeHash || (activeHash === '' && index === 0) ? 'active' : ''}`}
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
            <strong>{email}</strong>
            <span>{roleLabel}</span>
          </div>
          <button className="profile-logout" type="button" aria-label="Log out" onClick={onLogout}>
            <LogOut size={16} />
          </button>
        </div>
      </aside>
    </>
  );
}
