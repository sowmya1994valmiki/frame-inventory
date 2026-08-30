import type { FrameSummary } from '../api/frames'

const dateFormatter = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' })

export function FrameTable({ frames }: { frames: FrameSummary[] }) {
  return (
    <div className="table-scroll" role="region" tabIndex={0} aria-label="Scrollable frame inventory table">
      <table className="inventory-table">
        <caption className="sr-only">Frame inventory search results</caption>
        <thead><tr>
          <th scope="col">Frame ID</th><th scope="col">Site</th><th scope="col">Location</th>
          <th scope="col">Media type</th><th scope="col">Format</th><th scope="col">Environment</th>
          <th scope="col">Status</th><th scope="col">Last updated</th>
        </tr></thead>
        <tbody>{frames.map((frame) => (
          <tr key={frame.frameId}>
            <td><span className="frame-id">{frame.frameId}</span></td>
            <td><StackedValue primary={frame.station || frame.airport} secondary={frame.address} /></td>
            <td><StackedValue primary={frame.town} secondary={frame.region} /></td>
            <td>{displayValue(frame.mediaType)}</td><td>{displayValue(frame.format)}</td>
            <td>{displayValue(frame.environment)}</td><td><StatusChip status={frame.status} /></td>
            <td><time className="date-value" dateTime={frame.modifiedDate}>{formatDate(frame.modifiedDate)}</time></td>
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

function formatDate(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : dateFormatter.format(date)
}

function displayValue(value: string | null) { return value || '—' }
