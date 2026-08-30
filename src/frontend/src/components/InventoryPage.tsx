import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  FrameApiError,
  getFrames,
  type FrameFilters as FilterValues,
  type FramePage,
  type GetFramesParams,
} from '../api/frames'
import { FrameFilters } from './FrameFilters'
import { FrameTable } from './FrameTable'
import { Pagination } from './Pagination'

const EMPTY_FILTERS: FilterValues = { status: '', mediaType: '', environment: '', format: '', region: '' }
const INITIAL_REQUEST: ActiveRequest = {
  id: 0,
  params: { ...EMPTY_FILTERS, page: 0, size: 20, q: '' },
}

type ActiveRequest = { id: number; params: GetFramesParams }
type RequestUpdate = (current: GetFramesParams) => GetFramesParams

export function InventoryPage() {
  const [queryInput, setQueryInput] = useState('')
  const [filters, setFilters] = useState<FilterValues>(EMPTY_FILTERS)
  const requestSequence = useRef(0)
  const requestRef = useRef<ActiveRequest>(INITIAL_REQUEST)
  const activeRequestId = useRef(INITIAL_REQUEST.id)
  const [request, setRequest] = useState<ActiveRequest>(INITIAL_REQUEST)
  const [settledRequestId, setSettledRequestId] = useState<number | null>(null)
  const [result, setResult] = useState<FramePage | null>(null)
  const [error, setError] = useState<string | null>(null)
  const loading = settledRequestId !== request.id
  const visibleError = settledRequestId === request.id ? error : null
  const filterSuggestions = useMemo(() => {
    const frames = result?.items ?? []
    return {
      mediaType: uniqueValues(frames.map((frame) => frame.mediaType)),
      environment: uniqueValues(frames.map((frame) => frame.environment)),
      format: uniqueValues(frames.map((frame) => frame.format)),
      region: uniqueValues(frames.map((frame) => frame.region)),
    }
  }, [result])

  const scheduleRequest = useCallback((update: RequestUpdate, force = false) => {
    const current = requestRef.current
    const params = update(current.params)
    if (!force && params === current.params) return

    const next = { id: ++requestSequence.current, params }
    requestRef.current = next
    activeRequestId.current = next.id
    setRequest(next)
  }, [])

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      const q = queryInput.trim()

      scheduleRequest((current) => {
        return current.q === q ? current : { ...current, q, page: 0 }
      })
    }, 300)
    return () => window.clearTimeout(timeout)
  }, [queryInput, scheduleRequest])

  useEffect(() => {
    const controller = new AbortController()
    getFrames(request.params, controller.signal)
      .then((data) => {
        if (activeRequestId.current !== request.id) return
        if (request.params.page > 0 && request.params.page >= data.totalPages) {
          const correctedPage = Math.max(0, data.totalPages - 1)
          scheduleRequest((current) => ({ ...current, page: correctedPage }))
          return
        }
        setResult(data)
        setError(null)
        setSettledRequestId(request.id)
      })
      .catch((caught: unknown) => {
        if (caught instanceof DOMException && caught.name === 'AbortError') return
        if (activeRequestId.current !== request.id) return
        setError(caught instanceof FrameApiError || caught instanceof Error ? caught.message : 'An unexpected error occurred.')
        setSettledRequestId(request.id)
      })
    return () => controller.abort()
  }, [request, scheduleRequest])

  const hasActiveFilters = Boolean(
    request.params.q || request.params.status || request.params.mediaType || request.params.environment ||
    request.params.format || request.params.region,
  )
  const changeFilter = <K extends keyof FilterValues,>(name: K, value: FilterValues[K]) => {
    setFilters((current) => ({ ...current, [name]: value }))
    if (name === 'status') {
      const status = value as FilterValues['status']
      scheduleRequest((current) => current.status === status ? current : { ...current, status, page: 0 })
      return
    }

    const suggestionName = name as Exclude<keyof FilterValues, 'status'>
    const normalizedValue = value.trim()
    if (normalizedValue && !filterSuggestions[suggestionName].includes(normalizedValue)) return

    scheduleRequest((current) => current[suggestionName] === normalizedValue
      ? current
      : { ...current, [suggestionName]: normalizedValue, page: 0 })
  }
  const clearFilters = () => {
    setQueryInput('')
    setFilters(EMPTY_FILTERS)
    scheduleRequest((current) => {
      const unchanged = current.q === '' && current.status === '' && current.mediaType === '' &&
        current.environment === '' && current.format === '' && current.region === ''
      return unchanged ? current : { ...current, ...EMPTY_FILTERS, q: '', page: 0 }
    })
  }
  const changeSize = (nextSize: number) => {
    scheduleRequest((current) => current.size === nextSize ? current : { ...current, size: nextSize, page: 0 })
  }
  const changePage = (nextPage: number) => {
    scheduleRequest((current) => current.page === nextPage ? current : { ...current, page: nextPage })
  }
  const retry = () => {
    scheduleRequest((current) => current, true)
  }

  return (
    <>
      <header className="page-header"><div>
        <h1>Frames</h1>
        <p className="page-meta">{result && !visibleError ? `${result.totalElements.toLocaleString()} frames` : 'Search the frame inventory'}</p>
      </div></header>
      <div className="inventory-layout">
        <FrameFilters query={queryInput} filters={filters} suggestions={filterSuggestions} onQueryChange={setQueryInput}
          onFilterChange={changeFilter} onClear={clearFilters} />
        <section className="results" aria-label="Frame results" aria-busy={loading}>
          <div className="surface">
            {loading && (!result || result.items.length === 0) ? <LoadingState /> : visibleError ? (
              <ErrorState message={visibleError} onRetry={retry} />
            ) : result && result.items.length === 0 ? (
              <EmptyState hasActiveFilters={hasActiveFilters} onClear={clearFilters} />
            ) : result ? <>
              {loading && <div className="results-loading" role="status">
                <span className="loading-spinner loading-spinner-small" /> Updating frames
              </div>}
              <FrameTable frames={result.items} />
              <Pagination page={result.page} size={result.size} totalElements={result.totalElements}
                totalPages={result.totalPages} disabled={loading} onPageChange={changePage} onSizeChange={changeSize} />
            </> : null}
          </div>
        </section>
      </div>
    </>
  )
}

function uniqueValues(values: Array<string | null>) {
  return [...new Set(values.flatMap((value) => value?.trim() ? [value.trim()] : []))]
    .sort((left, right) => left.localeCompare(right))
}

function LoadingState() {
  return <div className="state-panel" role="status"><div className="state-panel-content">
    <span className="state-icon"><span className="loading-spinner" /></span><h2>Loading frames</h2>
    <p>Fetching the latest inventory.</p>
  </div></div>
}

function ErrorState({ message, onRetry }: { message: string; onRetry: () => void }) {
  return <div className="state-panel" role="alert"><div className="state-panel-content">
    <span className="state-icon"><AlertIcon /></span><h2>Could not load frames</h2><p>{message}</p>
    <button className="button button-primary" type="button" onClick={onRetry}>Try again</button>
  </div></div>
}

function EmptyState({ hasActiveFilters, onClear }: { hasActiveFilters: boolean; onClear: () => void }) {
  return <div className="state-panel"><div className="state-panel-content">
    <span className="state-icon"><SearchIcon /></span>
    <h2>{hasActiveFilters ? 'No matching frames' : 'No frames found'}</h2>
    <p>{hasActiveFilters ? 'Try changing your search or clearing the filters.' : 'The inventory does not contain any frames yet.'}</p>
    {hasActiveFilters && <button className="button" type="button" onClick={onClear}>Clear filters</button>}
  </div></div>
}

function SearchIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
    <circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" />
  </svg>
}

function AlertIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
    <circle cx="12" cy="12" r="9" /><path d="M12 8v5M12 16h.01" />
  </svg>
}
