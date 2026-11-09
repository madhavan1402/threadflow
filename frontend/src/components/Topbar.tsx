import { Menu, Search, AlertCircle, RefreshCw, MapPin } from 'lucide-react';
import type { UserRole } from '../auth/auth';

interface TopbarProps {
  onMenu: () => void;
  activeHash: string;
  roles: UserRole[];
  email: string;
  pendingIssuesCount?: number;
  onRefresh?: () => void;
  searchQuery?: string;
  onSearchChange?: (val: string) => void;
}

export function Topbar({
  onMenu,
  activeHash,
  roles,
  email,
  pendingIssuesCount = 0,
  onRefresh,
  searchQuery,
  onSearchChange,
}: TopbarProps) {
  const isMerchandiser = roles.includes('MERCHANDISER');

  const getBreadcrumbContext = () => {
    if (!isMerchandiser) return 'Production Floor';
    if (activeHash === '#issues') return 'Quality & Issues';
    if (activeHash === '#production') return 'Floor Updates';
    if (activeHash === '#buyer') return 'Buyer Visibility';
    if (activeHash === '#overview') return 'Analytics & Overview';
    return 'Production Orders';
  };

  const getPageTitle = () => {
    if (!isMerchandiser) return 'Supervisor Operations';
    if (activeHash === '#issues') return 'Floor Issue Review';
    if (activeHash === '#production') return 'Production History';
    if (activeHash === '#buyer') return 'Buyer Portal';
    if (activeHash === '#overview') return 'Merchandiser Overview';
    return 'Order Management';
  };

  // Generate initials for avatar
  const initials = email
    ? email.split('@')[0].slice(0, 2).toUpperCase()
    : 'TF';

  return (
    <header className="app-topbar">
      <div className="topbar-left">
        <button
          className="topbar-menu-btn"
          type="button"
          aria-label="Open navigation menu"
          onClick={onMenu}
        >
          <Menu size={20} />
        </button>

        <div className="topbar-breadcrumbs">
          <span className="crumb-app">ThreadFlow</span>
          <span className="crumb-separator">/</span>
          <span className="crumb-section">{getBreadcrumbContext()}</span>
          <span className="crumb-separator">/</span>
          <strong className="crumb-current">{getPageTitle()}</strong>
        </div>
      </div>

      <div className="topbar-center">
        {onSearchChange !== undefined && activeHash !== '#issues' && (
          <div className="topbar-search-box">
            <Search size={15} className="search-icon" aria-hidden="true" />
            <input
              type="text"
              placeholder="Search orders, PO, style, or buyer..."
              value={searchQuery || ''}
              onChange={(e) => onSearchChange(e.target.value)}
              className="topbar-search-input"
              aria-label="Quick search"
            />
            <kbd className="search-shortcut">⌘K</kbd>
          </div>
        )}
      </div>

      <div className="topbar-right">
        {/* Hub status badge */}
        <div className="topbar-hub-badge" title="Tiruppur Manufacturing Operations Hub">
          <MapPin size={13} className="hub-pin" />
          <span>Tiruppur Hub</span>
          <span className="hub-dot" aria-hidden="true" />
        </div>

        {/* Pending issue alert pill (for merchandisers) */}
        {isMerchandiser && pendingIssuesCount > 0 && (
          <a
            href="#issues"
            className="topbar-alert-pill"
            title={`${pendingIssuesCount} Tamil floor issues waiting for review`}
            data-testid="topbar-issues-alert"
          >
            <AlertCircle size={14} />
            <span>{pendingIssuesCount} Review{pendingIssuesCount > 1 ? 's' : ''}</span>
          </a>
        )}

        {/* Refresh action */}
        {onRefresh && (
          <button
            className="topbar-icon-action"
            type="button"
            aria-label="Refresh workspace data"
            onClick={onRefresh}
            title="Refresh workspace data"
          >
            <RefreshCw size={16} />
          </button>
        )}

        {/* User profile capsule */}
        <div className="topbar-user-capsule">
          <div className="capsule-avatar">{initials}</div>
          <div className="capsule-meta">
            <span className="capsule-name">{email.split('@')[0]}</span>
            <span className="capsule-role">{isMerchandiser ? 'Merchandiser' : 'Supervisor'}</span>
          </div>
        </div>
      </div>
    </header>
  );
};
