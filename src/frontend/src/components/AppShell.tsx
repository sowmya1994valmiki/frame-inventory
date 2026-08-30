import type { ReactNode } from 'react'

export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className="app-shell">
      <div className="brand">
        <span className="brand-mark" aria-hidden="true"><FrameIcon /></span>
        <span className="brand-name">Global Inventory<small>Frames · OOH</small></span>
      </div>
      <header className="topbar">
        <span>Inventory</span><span className="breadcrumb-separator" aria-hidden="true">/</span>
        <span className="breadcrumb-current">Frames</span>
      </header>
      <nav className="rail" aria-label="Primary navigation">
        <div className="rail-label">Inventory</div>
        <div className="nav-item" aria-current="page"><FrameIcon /> Frames</div>
      </nav>
      <main className="content">{children}</main>
    </div>
  )
}

function FrameIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"
      strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <rect x="3" y="5" width="18" height="14" rx="1" /><path d="M3 9h18" />
    </svg>
  )
}
