import { Fragment, useEffect, useRef, useState } from 'react'
import {
  FrameApiError,
  getFrame,
  getFrameHistory,
  type Frame,
  type FrameHistoryEntry,
} from '../api/frames'

const dateTimeFormatter = new Intl.DateTimeFormat(undefined, {
  dateStyle: 'medium',
  timeStyle: 'short',
})

type Props = {
  frameId: string
  onBack: () => void
}

export function FrameDetailPage({ frameId, onBack }: Props) {
  const headingRef = useRef<HTMLHeadingElement>(null)
  const [frame, setFrame] = useState<Frame | null>(null)
  const [frameError, setFrameError] = useState<string | null>(null)
  const [frameAttempt, setFrameAttempt] = useState(0)
  const [history, setHistory] = useState<FrameHistoryEntry[] | null>(null)
  const [historyError, setHistoryError] = useState<string | null>(null)
  const [historyAttempt, setHistoryAttempt] = useState(0)
  const [expandedEntries, setExpandedEntries] = useState<Set<number>>(() => new Set())

  useEffect(() => {
    headingRef.current?.focus()
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    getFrame(frameId, controller.signal)
      .then((loadedFrame) => {
        setFrame(loadedFrame)
        setFrameError(null)
      })
      .catch((caught: unknown) => {
        if (caught instanceof DOMException && caught.name === 'AbortError') return
        setFrameError(errorMessage(caught))
      })
    return () => controller.abort()
  }, [frameAttempt, frameId])

  useEffect(() => {
    const controller = new AbortController()
    getFrameHistory(frameId, controller.signal)
      .then((entries) => {
        setHistory(entries)
        setHistoryError(null)
      })
      .catch((caught: unknown) => {
        if (caught instanceof DOMException && caught.name === 'AbortError') return
        setHistoryError(errorMessage(caught))
      })
    return () => controller.abort()
  }, [frameId, historyAttempt])

  const retryFrame = () => {
    setFrame(null)
    setFrameError(null)
    setFrameAttempt((attempt) => attempt + 1)
  }

  const retryHistory = () => {
    setHistory(null)
    setHistoryError(null)
    setExpandedEntries(new Set())
    setHistoryAttempt((attempt) => attempt + 1)
  }

  const toggleEntry = (index: number) => {
    setExpandedEntries((current) => {
      const next = new Set(current)
      if (next.has(index)) next.delete(index)
      else next.add(index)
      return next
    })
  }

  return (
    <>
      <header className="page-header detail-page-header">
        <button className="button back-button" type="button" onClick={onBack} aria-label="Back to frames">
          <BackIcon />
        </button>
        <div>
          <div className="detail-heading-line">
            <h1 ref={headingRef} tabIndex={-1}>{frameId}</h1>
            {frame && <StatusChip status={frame.status} />}
          </div>
          <p className="page-meta">Frame details and change history</p>
        </div>
      </header>

      <div className="editor-layout detail-layout">
        {frameError ? (
          <StateSurface role="alert" title="Could not load frame" message={frameError} onRetry={retryFrame} />
        ) : !frame ? (
          <LoadingSurface title="Loading frame" message="Fetching the latest inventory details." />
        ) : (
          <>
            <section className="surface detail-summary" aria-label="Frame summary">
              <SummaryItem label="Status"><StatusChip status={frame.status} /></SummaryItem>
              <SummaryItem label="Media type" value={frame.mediaType} />
              <SummaryItem label="Format" value={frame.format} />
              <SummaryItem label="Environment" value={frame.environment} />
              <SummaryItem label="Last updated">
                <time dateTime={frame.modifiedDate}>{formatDateTime(frame.modifiedDate)}</time>
              </SummaryItem>
            </section>

            <section className="surface history-surface" aria-labelledby="history-heading" aria-busy={!history && !historyError}>
              <div className="history-section-header">
                <div>
                  <h2 id="history-heading">History</h2>
                  <p>{history ? `${history.length} ${history.length === 1 ? 'event' : 'events'}` : 'Changes recorded for this frame'}</p>
                </div>
              </div>
              {historyError ? (
                <StatePanel role="alert" title="Could not load history" message={historyError} onRetry={retryHistory} />
              ) : !history ? (
                <LoadingPanel title="Loading history" message="Fetching recorded frame changes." />
              ) : history.length === 0 ? (
                <StatePanel title="No history recorded" message="There are no history events for this frame." />
              ) : (
                <HistoryTable entries={history} expandedEntries={expandedEntries} onToggle={toggleEntry} />
              )}
            </section>
          </>
        )}
      </div>
    </>
  )
}

function HistoryTable({
  entries,
  expandedEntries,
  onToggle,
}: {
  entries: FrameHistoryEntry[]
  expandedEntries: Set<number>
  onToggle: (index: number) => void
}) {
  return (
    <div className="table-scroll" role="region" tabIndex={0} aria-label="Scrollable frame history table">
      <table className="history-table">
        <caption className="sr-only">History for this frame, newest event first</caption>
        <thead><tr>
          <th scope="col">Timestamp</th>
          <th scope="col">Event type</th>
          <th scope="col">Source</th>
          <th scope="col">Changed fields</th>
          <th scope="col"><span className="sr-only">Details</span></th>
        </tr></thead>
        <tbody>{entries.map((entry, index) => {
          const changes = Object.entries(entry.changedFields ?? {})
          const expanded = expandedEntries.has(index)
          const detailsId = `history-changes-${index}`
          const showMessage = changes.length === 0
          return (
            <Fragment key={`${entry.occurredAt}-${entry.eventType}-${index}`}>
              <tr>
                <td><time className="date-value" dateTime={entry.occurredAt}>{formatDateTime(entry.occurredAt)}</time></td>
                <td><span className={`history-chip history-${entry.eventType.toLowerCase()}`}>{entry.eventType}</span></td>
                <td><span className="history-source">{entry.source}</span></td>
                <td>{changes.length}</td>
                <td className="history-action-cell">
                  {entry.eventType === 'UPDATED' && changes.length > 0 ? (
                    <button className="button history-expand-button" type="button" aria-expanded={expanded}
                      aria-controls={detailsId} onClick={() => onToggle(index)}>
                      {expanded ? 'Hide' : 'View'} <ChevronIcon expanded={expanded} />
                    </button>
                  ) : <span aria-hidden="true">—</span>}
                </td>
              </tr>
              {showMessage && (
                <tr className="history-detail-row">
                  <td colSpan={5}><p className="history-event-message">{eventMessage(entry)}</p></td>
                </tr>
              )}
              {expanded && (
                <tr className="history-detail-row">
                  <td colSpan={5}>
                    <div id={detailsId} className="history-changes">
                      <table className="history-changes-table">
                        <caption className="sr-only">Fields changed in this update</caption>
                        <thead><tr><th scope="col">Field</th><th scope="col">Old value</th><th scope="col">New value</th></tr></thead>
                        <tbody>{changes.map(([field, change]) => (
                          <tr key={field}>
                            <th scope="row">{formatFieldName(field)}</th>
                            <td>{displayHistoryValue(change.old)}</td>
                            <td>{displayHistoryValue(change.new)}</td>
                          </tr>
                        ))}</tbody>
                      </table>
                    </div>
                  </td>
                </tr>
              )}
            </Fragment>
          )
        })}</tbody>
      </table>
    </div>
  )
}

function SummaryItem({ label, value, children }: { label: string; value?: string | null; children?: React.ReactNode }) {
  return <div className="detail-summary-item"><span>{label}</span><strong>{children ?? displayHistoryValue(value)}</strong></div>
}

function StateSurface({ role, title, message, onRetry }: StateProps) {
  return <div className="surface"><StatePanel role={role} title={title} message={message} onRetry={onRetry} /></div>
}

function LoadingSurface({ title, message }: { title: string; message: string }) {
  return <div className="surface"><LoadingPanel title={title} message={message} /></div>
}

type StateProps = {
  role?: 'alert'
  title: string
  message: string
  onRetry?: () => void
}

function StatePanel({ role, title, message, onRetry }: StateProps) {
  return <div className="state-panel history-state-panel" role={role}><div className="state-panel-content">
    <span className="state-icon"><AlertIcon /></span><h2>{title}</h2><p>{message}</p>
    {onRetry && <button className="button button-primary" type="button" onClick={onRetry}>Try again</button>}
  </div></div>
}

function LoadingPanel({ title, message }: { title: string; message: string }) {
  return <div className="state-panel history-state-panel" role="status"><div className="state-panel-content">
    <span className="state-icon"><span className="loading-spinner" /></span><h2>{title}</h2><p>{message}</p>
  </div></div>
}

function eventMessage(entry: FrameHistoryEntry) {
  if (entry.eventType === 'CREATED') return 'Frame created manually.'
  if (entry.eventType === 'IMPORTED') return 'Frame imported from a CSV upload.'
  return 'No field changes were recorded for this update.'
}

function formatFieldName(field: string) {
  const leafName = field.split('.').at(-1) ?? field
  const words = leafName
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[_-]+/g, ' ')
    .toLowerCase()
    .replace(/\bids\b/g, 'IDs')
    .replace(/\bid\b/g, 'ID')
  return words.charAt(0).toUpperCase() + words.slice(1)
}

function displayHistoryValue(value: string | null | undefined) {
  return value == null || value.trim() === '' ? '—' : value
}

function formatDateTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : dateTimeFormatter.format(date)
}

function errorMessage(caught: unknown) {
  return caught instanceof FrameApiError || caught instanceof Error ? caught.message : 'An unexpected error occurred.'
}

function StatusChip({ status }: { status: string }) {
  const normalized = status.toUpperCase()
  const statusClass = normalized === 'LIVE' ? 'status-live' : normalized === 'INACTIVE' ? 'status-inactive' : ''
  return <span className={`status-chip ${statusClass}`.trim()}>{status}</span>
}

function BackIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
    <path d="m15 18-6-6 6-6" />
  </svg>
}

function ChevronIcon({ expanded }: { expanded: boolean }) {
  return <svg className={expanded ? 'chevron-expanded' : undefined} viewBox="0 0 24 24" fill="none"
    stroke="currentColor" strokeWidth="1.8" aria-hidden="true"><path d="m9 6 6 6-6 6" /></svg>
}

function AlertIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
    <circle cx="12" cy="12" r="9" /><path d="M12 8v5M12 16h.01" />
  </svg>
}
