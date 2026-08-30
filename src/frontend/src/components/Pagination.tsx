type Props = {
  page: number; size: number; totalElements: number; totalPages: number; disabled: boolean
  onPageChange: (page: number) => void; onSizeChange: (size: number) => void
}

export function Pagination({ page, size, totalElements, totalPages, disabled, onPageChange, onSizeChange }: Props) {
  const start = totalElements === 0 ? 0 : page * size + 1
  const end = Math.min((page + 1) * size, totalElements)
  const displayedPage = totalPages === 0 ? 0 : page + 1

  return (
    <nav className="pagination" aria-label="Inventory pagination">
      <span>Showing {start}–{end} of {totalElements}</span><span className="pagination-spacer" />
      <label className="page-size">Rows per page
        <select className="select" value={size} disabled={disabled}
          onChange={(event) => onSizeChange(Number(event.target.value))}>
          <option value={20}>20</option><option value={50}>50</option><option value={100}>100</option>
        </select>
      </label>
      <div className="page-controls">
        <button className="button" type="button" disabled={disabled || page === 0}
          onClick={() => onPageChange(page - 1)}>Previous</button>
        <span className="page-indicator">Page {displayedPage} of {totalPages}</span>
        <button className="button" type="button"
          disabled={disabled || totalPages === 0 || page >= totalPages - 1}
          onClick={() => onPageChange(page + 1)}>Next</button>
      </div>
    </nav>
  )
}
