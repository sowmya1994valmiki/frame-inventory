import { useEffect, useRef, useState, type FormEvent } from 'react'
import {
  FrameApiError,
  importFrames,
  type FrameCsvImportSummary,
} from '../api/frames'

type Props = {
  onViewInventory: () => void
}

type ImportOutcome = {
  className: string
  title: string
  message: string
}

export function CsvImportPage({ onViewInventory }: Props) {
  const headingRef = useRef<HTMLHeadingElement>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [file, setFile] = useState<File | null>(null)
  const [uploading, setUploading] = useState(false)
  const [result, setResult] = useState<FrameCsvImportSummary | null>(null)
  const [requestError, setRequestError] = useState<string | null>(null)

  useEffect(() => {
    headingRef.current?.focus()
  }, [])

  const selectFile = (selectedFile: File | null) => {
    setFile(selectedFile)
    setResult(null)
    setRequestError(null)
  }

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!file || uploading) return

    setUploading(true)
    setRequestError(null)
    try {
      setResult(await importFrames(file))
    } catch (caught: unknown) {
      setRequestError(errorMessage(caught))
    } finally {
      setUploading(false)
    }
  }

  const chooseAnother = () => {
    setFile(null)
    setResult(null)
    setRequestError(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
      fileInputRef.current.focus()
    }
  }

  return (
    <>
      <header className="page-header editor-page-header">
        <button className="button back-button" type="button" disabled={uploading}
          onClick={onViewInventory} aria-label="Back to frames">
          <BackIcon />
        </button>
        <div>
          <h1 ref={headingRef} tabIndex={-1}>Import frames</h1>
          <p className="page-meta">Add frames to the inventory from a CSV file.</p>
        </div>
      </header>
      <div className="editor-layout import-layout">
        {result ? (
          <ImportResults result={result} onChooseAnother={chooseAnother} onViewInventory={onViewInventory} />
        ) : (
          <form className="surface import-upload" onSubmit={submit}>
            <div className="import-section">
              <h2>Select a CSV file</h2>
              <p>Choose a non-empty UTF-8 CSV containing the required frame columns.</p>
              {requestError && (
                <div className="form-error-summary" role="alert">
                  <strong>Import failed</strong>
                  <span>{requestError}</span>
                </div>
              )}
              <div className="field">
                <label htmlFor="csv-file">CSV file</label>
                <input ref={fileInputRef} className="file-input" id="csv-file" name="file" type="file"
                  accept=".csv,text/csv" disabled={uploading}
                  onChange={(event) => selectFile(event.target.files?.[0] ?? null)} />
                {file && <span className="field-help" role="status">
                  Selected: {file.name} ({formatFileSize(file.size)})
                </span>}
              </div>
            </div>
            <div className="import-actions">
              <button className="button" type="button" disabled={uploading} onClick={onViewInventory}>View inventory</button>
              <button className="button button-primary" type="submit" disabled={!file || uploading}>
                {uploading && <span className="loading-spinner loading-spinner-small" />}
                {uploading ? 'Importing…' : 'Import CSV'}
              </button>
            </div>
          </form>
        )}
      </div>
    </>
  )
}

function ImportResults({ result, onChooseAnother, onViewInventory }: {
  result: FrameCsvImportSummary
  onChooseAnother: () => void
  onViewInventory: () => void
}) {
  const outcome = importOutcome(result)

  return <div className="import-results">
    <section className={`import-result-banner ${outcome.className}`} role="status">
      <span className="import-result-icon" aria-hidden="true"><OutcomeIcon /></span>
      <div><h2>{outcome.title}</h2><p>{outcome.message}</p></div>
    </section>
    <dl className="import-summary" aria-label="Import summary">
      <SummaryCount label="Imported" value={result.created} />
      <SummaryCount label="Duplicates" value={result.duplicates} />
      <SummaryCount label="Failed" value={result.failed} />
    </dl>
    {result.errors.length > 0 && (
      <section className="surface import-errors" aria-labelledby="import-errors-heading">
        <div className="import-errors-header">
          <h2 id="import-errors-heading">Rows not imported</h2>
          <p>{result.errors.length.toLocaleString()} {result.errors.length === 1 ? 'row needs' : 'rows need'} attention.</p>
        </div>
        <div className="table-scroll">
          <table className="import-errors-table">
            <thead><tr><th scope="col">Row</th><th scope="col">Frame ID</th><th scope="col">Reason</th></tr></thead>
            <tbody>{result.errors.map((error, index) => <tr key={`${error.rowNumber}-${index}`}>
              <td className="date-value">{error.rowNumber}</td>
              <td className="frame-id">{error.frameId || '—'}</td>
              <td>{error.reason}</td>
            </tr>)}</tbody>
          </table>
        </div>
      </section>
    )}
    <div className="import-result-actions">
      <button className="button" type="button" onClick={onChooseAnother}>Choose another file</button>
      <button className="button button-primary" type="button" onClick={onViewInventory}>View inventory</button>
    </div>
  </div>
}

function SummaryCount({ label, value }: { label: string; value: number }) {
  return <div className="surface import-summary-item"><dt>{label}</dt><dd>{value.toLocaleString()}</dd></div>
}

function importOutcome(result: FrameCsvImportSummary): ImportOutcome {
  if (result.created === result.totalRows) {
    return {
      className: 'import-result-success',
      title: 'Import successful',
      message: `All ${result.totalRows.toLocaleString()} rows were imported.`,
    }
  }
  if (result.created === 0 && result.failed === 0 && result.duplicates === result.totalRows) {
    return {
      className: 'import-result-no-changes',
      title: 'Import completed with no changes',
      message: `All ${result.totalRows.toLocaleString()} rows were skipped as duplicates.`,
    }
  }
  if (result.created > 0) {
    return {
      className: 'import-result-partial',
      title: 'Import partially successful',
      message: `${result.created.toLocaleString()} of ${result.totalRows.toLocaleString()} rows were imported.`,
    }
  }
  return {
    className: 'import-result-failed',
    title: 'Import failed',
    message: `No rows were imported from the ${result.totalRows.toLocaleString()} rows processed.`,
  }
}

function errorMessage(caught: unknown) {
  return caught instanceof FrameApiError || caught instanceof Error ? caught.message : 'An unexpected error occurred.'
}

function formatFileSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function BackIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
    <path d="m15 18-6-6 6-6" />
  </svg>
}

function OutcomeIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
    <circle cx="12" cy="12" r="9" /><path d="m8 12 2.5 2.5L16 9" />
  </svg>
}
