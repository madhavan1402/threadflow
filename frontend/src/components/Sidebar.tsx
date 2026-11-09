import { AlertCircle, Boxes, Factory, Globe, LayoutDashboard, LogOut, Layers } from 'lucide-react';
import type { UserRole } from '../auth/auth';

export interface SidebarProps {
  menuOpen: boolean;
  onClose: () => void;
  roles: UserRole[];
  email: string;
  onLogout: () => void;
  activeHash: string;
  pendingIssuesCount?: number;
  ordersCount?: number;
}

export function Sidebar({
  menuOpen,
  onClose,
  roles,
  email,
  onLogout,
  activeHash,
  pendingIssuesCount = 0,
  ordersCount,
}: SidebarProps) {
  const isMerchandiser = roles.includes('MERCHANDISER');
  const roleLabel = isMerchandiser ? 'Merchandiser' : 'Supervisor';
  const roleDepartment = isMerchandiser ? 'Export Merchandising' : 'Shop Floor Operations';

  const initials = email
    ? email.split('@')[0].slice(0, 2).toUpperCase()
    : 'TF';

  // Navigation definition
  const workspaceItems = [
    ...(isMerchandiser
      ? [
          { label: 'Overview', icon: LayoutDashboard, target: '#overview', badge: null },
          { label: 'Orders', icon: Boxes, target: '#orders', badge: ordersCount ? `${ordersCount}` : null },
          {
            label: 'Issues',
            icon: AlertCircle,
            target: '#issues',
            badge: pendingIssuesCount > 0 ? `${pendingIssuesCount}` : null,
            badgeVariant: 'warning',
          },
        ]
      : []),
    {
      label: 'Production',
      icon: Factory,
      target: '#production',
      badge: null,
    },
  ];

  const portalItems = [
    { label: 'Buyer Portal', icon: Globe, target: '#buyer', badge: 'External' },
  ];

  const isCurrentActive = (target: string, index: number) => {
    if (activeHash === target) return true;
    if ((activeHash === '' || activeHash === '#') && index === 0 && target === '#overview') return true;
    return false;
  };

  return (
    <>
      {menuOpen && (
        <button
          className="menu-backdrop"
          aria-label="Close navigation menu"
          onClick={onClose}
        />
      )}
      <aside className={`sidebar ${menuOpen ? 'open' : ''}`}>
        {/* Brand Header */}
        <div className="brand">
          <div className="brand-emblem" aria-hidden="true">
            <Layers size={18} className="brand-icon" />
          </div>
          <div className="brand-text">
            <span className="brand-name">ThreadFlow</span>
            <span className="brand-tag">Tiruppur Export SaaS</span>
          </div>
        </div>

        {/* Workspace Context Tag */}
        <div className="workspace-switcher">
          <div className="workspace-badge-monogram">TF</div>
          <div className="workspace-badge-meta">
            <strong>{roleDepartment}</strong>
            <span>Tamil Nadu Operations Hub</span>
          </div>
        </div>

        {/* Navigation Sections */}
        <nav className="nav" aria-label="Primary navigation">
          <p className="nav-group">OPERATIONS WORKSPACE</p>
          {workspaceItems.map((item, index) => {
            const Icon = item.icon;
            const isActive = isCurrentActive(item.target, index);
            return (
              <a
                className={`nav-item ${isActive ? 'active' : ''}`}
                href={item.target}
                key={item.label}
                onClick={onClose}
              >
                <Icon size={18} strokeWidth={isActive ? 2.2 : 1.8} className="nav-icon" />
                <span className="nav-label">{item.label}</span>
                {item.badge && (
                  <span className={`nav-badge ${item.badgeVariant ? `badge-${item.badgeVariant}` : ''}`}>
                    {item.badge}
                  </span>
                )}
              </a>
            );
          })}

          <p className="nav-group" style={{ marginTop: '18px' }}>VISIBILITY &amp; AUDIT</p>
          {portalItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeHash === item.target;
            return (
              <a
                className={`nav-item ${isActive ? 'active' : ''}`}
                href={item.target}
                key={item.label}
                onClick={onClose}
              >
                <Icon size={18} strokeWidth={isActive ? 2.2 : 1.8} className="nav-icon" />
                <span className="nav-label">{item.label}</span>
                {item.badge && <span className="nav-badge badge-neutral">{item.badge}</span>}
              </a>
            );
          })}
        </nav>

        {/* User Profile Footer */}
        <div className="sidebar-profile">
          <div className="profile-avatar">{initials}</div>
          <div className="profile-meta">
            <strong className="profile-email" title={email}>{email}</strong>
            <span className="profile-role-badge">{roleLabel}</span>
          </div>
          <button
            className="profile-logout"
            type="button"
            aria-label="Log out of ThreadFlow"
            title="Log out"
            onClick={onLogout}
          >
            <LogOut size={16} />
          </button>
        </div>
      </aside>
    </>
  );
}
