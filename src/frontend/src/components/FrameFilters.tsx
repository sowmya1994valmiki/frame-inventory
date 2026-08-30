import type { FrameFilters as FrameFilterValues } from '../api/frames'

type FilterSuggestions = {
  [K in Exclude<keyof FrameFilterValues, 'status'>]: string[]
}

type Props = {
  query: string
  filters: FrameFilterValues
  suggestions: FilterSuggestions
  onQueryChange: (value: string) => void
  onFilterChange: <K extends keyof FrameFilterValues>(name: K, value: FrameFilterValues[K]) => void
  onClear: () => void
}

export function FrameFilters({ query, filters, suggestions, onQueryChange, onFilterChange, onClear }: Props) {
  const hasFilters = Boolean(
    query.trim() || filters.status || filters.mediaType.trim() ||
    filters.environment.trim() || filters.format.trim() || filters.region.trim(),
  )

  return (
    <aside className="filters" aria-label="Frame filters">
      <div className="filters-header">
        <h2>Filter frames</h2>
      </div>
      <div className="filter-search">
        <div className="field">
          <label htmlFor="frame-search">Search</label>
          <input id="frame-search" className="input search-input" type="search" value={query}
            placeholder="ID, site or location" onChange={(event) => onQueryChange(event.target.value)} />
        </div>
      </div>
      <div className="filter-controls">
        <div className="field">
          <label htmlFor="status-filter">Status</label>
          <select id="status-filter" className="select" value={filters.status}
            onChange={(event) => onFilterChange('status', event.target.value as FrameFilterValues['status'])}>
            <option value="">All</option><option value="LIVE">LIVE</option><option value="INACTIVE">INACTIVE</option>
          </select>
        </div>
        <StringFilter id="media-type-filter" label="Media type" value={filters.mediaType}
          suggestions={suggestions.mediaType}
          onChange={(value) => onFilterChange('mediaType', value)} />
        <StringFilter id="environment-filter" label="Environment" value={filters.environment}
          suggestions={suggestions.environment}
          onChange={(value) => onFilterChange('environment', value)} />
        <StringFilter id="format-filter" label="Format" value={filters.format}
          suggestions={suggestions.format}
          onChange={(value) => onFilterChange('format', value)} />
        <StringFilter id="region-filter" label="Region" value={filters.region}
          suggestions={suggestions.region}
          onChange={(value) => onFilterChange('region', value)} />
        {hasFilters && <button className="button button-link clear-filters" type="button" onClick={onClear}>Clear all</button>}
      </div>
    </aside>
  )
}

function StringFilter({ id, label, value, suggestions, onChange }: {
  id: string; label: string; value: string; suggestions: string[]; onChange: (value: string) => void
}) {
  const suggestionListId = `${id}-suggestions`

  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input id={id} className="input" type="text" list={suggestionListId} value={value} placeholder="All"
        onChange={(event) => onChange(event.target.value)} />
      <datalist id={suggestionListId}>
        {suggestions.map((suggestion) => <option key={suggestion} value={suggestion} />)}
      </datalist>
    </div>
  )
}
