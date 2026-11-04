import { Bell, ChevronDown, Menu, RefreshCw } from 'lucide-react';
import { useState } from 'react';
import type { ViewState } from '../App';

export function Header({
  onMenu,
  onRefresh,
  viewState,
  onViewStateChange,
}: {
  onMenu: () => void;
  onRefresh: () => void;
  viewState: ViewState;
  onViewStateChange: (state: ViewState) => void;
}) {
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  return (
    <header className="topbar">
      <button className="menu-button" type="button" aria-label="Open navigation" onClick={onMenu}>
        <Menu size={20} />
      </button>
      <div className="topbar-context">
        <span>Merchandising</span>
        <b>/</b>
        <strong>Overview</strong>
      </div>
      <div className="topbar-actions">
        <label className="demo-state">
          <span>Preview state</span>
          <select value={viewState} onChange={(event) => onViewStateChange(event.target.value as ViewState)}>
            <option value="ready">Success</option>
            <option value="loading">Loading</option>
            <option value="empty">Empty</option>
            <option value="error">Error</option>
          </select>
          <ChevronDown size={14} aria-hidden="true" />
        </label>
        <button className="icon-button" type="button" aria-label="Refresh production data" onClick={onRefresh}>
          <RefreshCw size={17} />
        </button>
        <button
          className="icon-button notification-button"
          type="button"
          aria-label="Notifications"
          aria-expanded={notificationsOpen}
          onClick={() => setNotificationsOpen((open) => !open)}
        >
          <Bell size={17} />
          <span aria-label="2 notifications">2</span>
        </button>
        {notificationsOpen && (
          <div className="notification-panel" role="dialog" aria-label="Internal notifications">
            <strong>2 items need review</strong>
            <p>TF-88240 · Finishing risk</p>
            <p>TF-88417 · Cutting review</p>
          </div>
        )}
      </div>
    </header>
  );
}
