import type { FrameSummary } from '../api/frames'

const dateFormatter = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' })

export function FrameTable({ frames, onEdit }: { frames: FrameSummary[]; onEdit: (frameId: string) => void }) {
  return (
    <div className="table-scroll" role="region" tabIndex={0} aria-label="Scrollable frame inventory table">
      <table className="inventory-table">
        <caption className="sr-only">Frame inventory search results</caption>
        <thead><tr>
          <th scope="col">Frame ID</th><th scope="col">Site</th><th scope="col">Location</th>
          <th scope="col">Media type</th><th scope="col">Format</th><th scope="col">Environment</th>
          <th scope="col">Status</th><th scope="col">Last updated</th><th scope="col"><span className="sr-only">Actions</span></th>
        </tr></thead>
        <tbody>{frames.map((frame) => (
          <tr key={frame.frameId}>
            <td><span className="frame-id">{frame.frameId}</span></td>
            <td><StackedValue primary={frame.station || frame.airport} secondary={frame.address} /></td>
            <td><StackedValue primary={frame.town} secondary={frame.region} /></td>
            <td>{displayValue(frame.mediaType)}</td><td>{displayValue(frame.format)}</td>
            <td>{displayValue(frame.environment)}</td><td><StatusChip status={frame.status} /></td>
            <td><time className="date-value" dateTime={frame.modifiedDate}>{formatDate(frame.modifiedDate)}</time></td>
            <td><button className="button table-edit-button" type="button" onClick={() => onEdit(frame.frameId)}
              aria-label={`Edit frame ${frame.frameId}`}><EditIcon /> Edit</button></td>
          </tr>
        ))}</tbody>
      </table>
    </div>
  )
}

function StackedValue({ primary, secondary }: { primary: string | null; secondary: string | null }) {
  return (
    <span className="stacked-value">
      <span title={primary || undefined}>{displayValue(primary)}</span>
      {secondary && <span className="secondary" title={secondary}>{secondary}</span>}
    </span>
  )
}

function StatusChip({ status }: { status: string }) {
  const normalized = status.toUpperCase()
  const statusClass = normalized === 'LIVE' ? 'status-live' : normalized === 'INACTIVE' ? 'status-inactive' : ''
  return <span className={`status-chip ${statusClass}`.trim()}>{status}</span>
}

function EditIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
    <path d="M12 20h9" /><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z" />
  </svg>
}

function formatDate(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : dateFormatter.format(date)
}

function displayValue(value: string | null) { return value || '—' }
