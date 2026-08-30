import { useEffect, useRef, useState } from 'react'
import { createFrame, FrameApiError, getFrame, updateFrame } from '../api/frames'
import { FrameForm } from './FrameForm'
import {
  apiErrorsToForm,
  emptyFrameForm,
  frameToForm,
  toCreateRequest,
  toUpdateRequest,
  type FormErrors,
  type FrameFormValues,
} from './frameFormModel'

type Props = {
  mode: 'create' | 'edit'
  frameId?: string
  onCancel: () => void
  onSuccess: (frameId: string, mode: 'create' | 'edit') => void
}

export function FrameEditorPage({ mode, frameId, onCancel, onSuccess }: Props) {
  const headingRef = useRef<HTMLHeadingElement>(null)
  const [initialValues, setInitialValues] = useState<FrameFormValues | null>(mode === 'create' ? emptyFrameForm() : null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [loadAttempt, setLoadAttempt] = useState(0)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [serverErrors, setServerErrors] = useState<FormErrors>({})

  useEffect(() => {
    headingRef.current?.focus()
  }, [])

  useEffect(() => {
    if (mode !== 'edit' || !frameId) return
    const controller = new AbortController()
    getFrame(frameId, controller.signal)
      .then((frame) => setInitialValues(frameToForm(frame)))
      .catch((caught: unknown) => {
        if (caught instanceof DOMException && caught.name === 'AbortError') return
        setLoadError(errorMessage(caught))
      })
    return () => controller.abort()
  }, [frameId, loadAttempt, mode])

  const submit = async (values: FrameFormValues) => {
    if (submitting) return
    setSubmitting(true)
    setSubmitError(null)
    setServerErrors({})
    try {
      const saved = mode === 'create'
        ? await createFrame(toCreateRequest(values))
        : await updateFrame(frameId!, toUpdateRequest(values))
      onSuccess(saved.frameId, mode)
    } catch (caught: unknown) {
      setSubmitError(errorMessage(caught))
      setServerErrors(caught instanceof FrameApiError ? apiErrorsToForm(caught.fieldErrors) : {})
      setSubmitting(false)
    }
  }

  const clearSubmitState = () => {
    if (submitError) setSubmitError(null)
    if (Object.keys(serverErrors).length > 0) setServerErrors({})
  }

  return (
    <>
      <header className="page-header editor-page-header">
        <button className="button back-button" type="button" disabled={submitting} onClick={onCancel} aria-label="Back to frames">
          <BackIcon />
        </button>
        <div>
          <h1 ref={headingRef} tabIndex={-1}>{mode === 'create' ? 'New frame' : 'Edit frame'}</h1>
          <p className="page-meta">
            {mode === 'create' ? 'Add a new physical OOH frame to the inventory.' : `Update inventory details for ${frameId}.`}
          </p>
        </div>
      </header>
      <div className="editor-layout">
        {initialValues ? (
          <FrameForm mode={mode} initialValues={initialValues} submitting={submitting}
            submitError={submitError} serverErrors={serverErrors} onSubmit={submit}
            onCancel={onCancel} onValueChange={clearSubmitState} />
        ) : loadError ? (
          <div className="surface"><div className="state-panel" role="alert"><div className="state-panel-content">
            <span className="state-icon"><AlertIcon /></span>
            <h2>Could not load frame</h2><p>{loadError}</p>
            <button className="button button-primary" type="button" onClick={() => {
              setLoadError(null)
              setInitialValues(null)
              setLoadAttempt((value) => value + 1)
            }}>
              Try again
            </button>
          </div></div></div>
        ) : (
          <div className="surface"><div className="state-panel" role="status"><div className="state-panel-content">
            <span className="state-icon"><span className="loading-spinner" /></span>
            <h2>Loading frame</h2><p>Fetching the latest inventory details.</p>
          </div></div></div>
        )}
      </div>
    </>
  )
}

function errorMessage(caught: unknown) {
  return caught instanceof FrameApiError || caught instanceof Error ? caught.message : 'An unexpected error occurred.'
}

function BackIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
    <path d="m15 18-6-6 6-6" />
  </svg>
}

function AlertIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
    <circle cx="12" cy="12" r="9" /><path d="M12 8v5M12 16h.01" />
  </svg>
}
