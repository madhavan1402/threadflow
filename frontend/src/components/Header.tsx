import { Menu, RefreshCw } from 'lucide-react';

export function Header({
  onMenu,
  onRefresh,
}: {
  onMenu: () => void;
  onRefresh: () => void;
}) {
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
        <button className="icon-button" type="button" aria-label="Refresh production data" onClick={onRefresh}>
          <RefreshCw size={17} />
        </button>
      </div>
    </header>
  );
}
