import { useRef, useState, type FormEvent, type ReactNode } from 'react'
import {
  type FormErrors,
  type FormField,
  type FrameFormValues,
  validateFrameForm,
} from './frameFormModel'
import { COUNTRY_OPTIONS } from './countries'

type Props = {
  mode: 'create' | 'edit'
  initialValues: FrameFormValues
  submitting: boolean
  submitError: string | null
  serverErrors: FormErrors
  onSubmit: (values: FrameFormValues) => void
  onCancel: () => void
  onValueChange: () => void
}

export function FrameForm({
  mode, initialValues, submitting, submitError, serverErrors, onSubmit, onCancel, onValueChange,
}: Props) {
  const [values, setValues] = useState(initialValues)
  const [clientErrors, setClientErrors] = useState<FormErrors>({})
  const errorSummaryRef = useRef<HTMLDivElement>(null)
  const errors = { ...serverErrors, ...clientErrors }

  const changeValue = (field: FormField, value: string) => {
    setValues((current) => ({ ...current, [field]: value }))
    setClientErrors((current) => removeError(current, field))
    onValueChange()
  }

  const validateField = (field: FormField) => {
    const nextError = validateFrameForm(values, mode)[field]
    setClientErrors((current) => nextError
      ? { ...current, [field]: nextError }
      : removeError(current, field))
  }

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (submitting) return
    const nextErrors = validateFrameForm(values, mode)
    setClientErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) {
      window.requestAnimationFrame(() => errorSummaryRef.current?.focus())
      return
    }
    onSubmit(values)
  }

  const fieldProps = (field: FormField) => ({
    field,
    value: values[field],
    error: errors[field],
    onChange: changeValue,
    onBlur: validateField,
  })

  return (
    <form className="frame-form" noValidate onSubmit={submit} aria-busy={submitting}>
      {(submitError || Object.keys(clientErrors).length > 0) && (
        <div className="form-error-summary" role="alert" tabIndex={-1} ref={errorSummaryRef}>
          <strong>{Object.keys(clientErrors).length > 0 ? 'Check the highlighted fields.' : 'Could not save frame.'}</strong>
          {submitError && <span>{submitError}</span>}
        </div>
      )}

      <div className="surface form-surface">
        <FormSection title="Basics and lifecycle" description="Core inventory classification and current operating status."
          disabled={submitting}>
          <div className="form-grid form-grid-three">
            <TextField {...fieldProps('frameId')} label="Frame ID" required={mode === 'create'} readOnly={mode === 'edit'} maxLength={64}
              help={mode === 'edit' ? 'Frame ID cannot be changed.' : undefined} />
            <TextField {...fieldProps('mediaType')} label="Media type" required placeholder="e.g. DIGITAL" />
            <TextField {...fieldProps('format')} label="Format" required placeholder="e.g. D6" />
            <TextField {...fieldProps('environment')} label="Environment" placeholder="e.g. UNDERGROUND" />
            <SelectField {...fieldProps('status')} label="Status" required>
              <option value="LIVE">LIVE</option>
              <option value="INACTIVE">INACTIVE</option>
            </SelectField>
            <TextField {...fieldProps('statusReason')} label="Status reason" wide multiline />
          </div>
        </FormSection>

        <FormSection title="Location" description="Postal address, geographic identifiers and coordinates."
          disabled={submitting}>
          <div className="form-grid form-grid-three">
            <TextField {...fieldProps('postcode')} label="Postcode" required />
            <TextField {...fieldProps('region')} label="Region" required />
            <TextField {...fieldProps('town')} label="Town" required />
            <TextField {...fieldProps('address')} label="Address" wide multiline />
            <SelectField {...fieldProps('countryCode')} label="Country">
              <option value="">Not set</option>
              {COUNTRY_OPTIONS.map((country) => (
                <option key={country.code} value={country.code}>{country.code} - {country.name}</option>
              ))}
            </SelectField>
            <TextField {...fieldProps('postcodeArea')} label="Postcode area" />
            <TextField {...fieldProps('postcodeDistrict')} label="Postcode district" />
            <TextField {...fieldProps('postcodeSector')} label="Postcode sector" />
            <TextField {...fieldProps('postcodeUnit')} label="Postcode unit" />
            <TextField {...fieldProps('longitude')} label="Longitude" inputMode="decimal" />
            <TextField {...fieldProps('latitude')} label="Latitude" inputMode="decimal" />
            <TextField {...fieldProps('distanceToClosestSchool')} label="Distance to closest school" inputMode="numeric"
              help="Metres" />
            <TextField {...fieldProps('rawLocationPoint')} label="Raw location point" />
            <TextField {...fieldProps('locationId')} label="Location ID" />
          </div>
        </FormSection>

        <FormSection title="Site" description="Identifiers and placement within the physical site."
          disabled={submitting}>
          <div className="form-grid form-grid-three">
            <TextField {...fieldProps('siteNumber')} label="Site number" required />
            <TextField {...fieldProps('inventorySiteNumber')} label="Inventory site number" />
            <TextField {...fieldProps('panelNumber')} label="Panel number" />
            <TextField {...fieldProps('station')} label="Station" />
            <TextField {...fieldProps('airport')} label="Airport" />
          </div>
        </FormSection>

        <FormSection title="Technical" description="Display size, illumination and delivery details."
          disabled={submitting}>
          <div className="form-grid form-grid-three">
            <TextField {...fieldProps('illuminationTypeId')} label="Illumination type ID" />
            <TextField {...fieldProps('numberOfSlots')} label="Number of slots" inputMode="numeric" />
            <TextField {...fieldProps('sizeCode')} label="Size code" />
            <TextField {...fieldProps('sizeGroupCode')} label="Size group code" />
            <TextField {...fieldProps('aspectRatioCode')} label="Aspect ratio code" />
            <TextField {...fieldProps('sizeCategory')} label="Size category" />
            <TextField {...fieldProps('pixelWidth')} label="Pixel width" inputMode="numeric" />
            <TextField {...fieldProps('pixelHeight')} label="Pixel height" inputMode="numeric" />
          </div>
        </FormSection>

        <FormSection title="Commercial" description="Pricing references and commercial weighting."
          disabled={submitting}>
          <div className="form-grid form-grid-three">
            <TextField {...fieldProps('impactWeight')} label="Impact weight" inputMode="decimal" />
            <TextField {...fieldProps('pricingGrade')} label="Pricing grade" />
            <TextField {...fieldProps('priceEntityId')} label="Price entity ID" />
            <TextField {...fieldProps('productionRateCard')} label="Production rate card" />
            <TextField {...fieldProps('legacyProductionRateCard')} label="Legacy production rate card" />
            <SelectField {...fieldProps('premium')} label="Premium">
              <option value="">Not set</option>
              <option value="true">Yes</option>
              <option value="false">No</option>
            </SelectField>
          </div>
        </FormSection>

        <FormSection title="Integrations" description="Identifiers used by Broadsign and linked inventory records."
          disabled={submitting} last>
          <div className="form-grid form-grid-two">
            <TextField {...fieldProps('broadsignDisplayUnitId')} label="Broadsign display unit ID" />
            <TextField {...fieldProps('broadsignFrameId')} label="Broadsign frame ID" />
            <TextField {...fieldProps('broadsignDomainId')} label="Broadsign domain ID" />
            <TextField {...fieldProps('linkedFrameIds')} label="Linked frame IDs" multiline />
          </div>
        </FormSection>
      </div>

      <div className="form-actions">
        <span>{mode === 'create' ? 'Create a new inventory frame' : `Editing ${values.frameId}`}</span>
        <div className="form-actions-buttons">
          <button className="button" type="button" disabled={submitting} onClick={onCancel}>Cancel</button>
          <button className="button button-primary" type="submit" disabled={submitting}>
            {submitting && <span className="loading-spinner loading-spinner-small" aria-hidden="true" />}
            {submitting ? 'Saving…' : mode === 'create' ? 'Create frame' : 'Save changes'}
          </button>
        </div>
      </div>
    </form>
  )
}

function FormSection({ title, description, disabled, last = false, children }: {
  title: string; description: string; disabled: boolean; last?: boolean; children: ReactNode
}) {
  return (
    <fieldset className={`form-section${last ? ' form-section-last' : ''}`} disabled={disabled}>
      <legend>{title}</legend>
      <p>{description}</p>
      {children}
    </fieldset>
  )
}

type FieldProps = {
  field: FormField
  label: string
  value: string
  error?: string
  required?: boolean
  wide?: boolean
  help?: string
  onChange: (field: FormField, value: string) => void
  onBlur: (field: FormField) => void
}

function TextField({
  field, label, value, error, required, wide, help, onChange, onBlur, multiline = false,
  readOnly = false, maxLength, placeholder, inputMode,
}: FieldProps & {
  multiline?: boolean; readOnly?: boolean; maxLength?: number; placeholder?: string
  inputMode?: 'decimal' | 'numeric'
}) {
  const errorId = `${field}-error`
  const helpId = `${field}-help`
  const describedBy = [error ? errorId : '', help ? helpId : ''].filter(Boolean).join(' ') || undefined
  const controlProps = {
    id: field,
    name: field,
    className: `input${error ? ' input-error' : ''}`,
    value,
    placeholder,
    readOnly,
    maxLength,
    inputMode,
    'aria-invalid': Boolean(error),
    'aria-required': required || undefined,
    'aria-describedby': describedBy,
    onChange: (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => onChange(field, event.target.value),
    onBlur: () => onBlur(field),
  }

  return (
    <div className={`field form-field${wide ? ' form-field-wide' : ''}`}>
      <label htmlFor={field}>{label}{required && <RequiredMark />}</label>
      {multiline ? <textarea {...controlProps} /> : <input type="text" {...controlProps} />}
      {help && <span className="field-help" id={helpId}>{help}</span>}
      {error && <span className="field-error" id={errorId}>{error}</span>}
    </div>
  )
}

function SelectField({ field, label, value, error, required, wide, onChange, onBlur, children }: FieldProps & {
  children: ReactNode
}) {
  const errorId = `${field}-error`
  return (
    <div className={`field form-field${wide ? ' form-field-wide' : ''}`}>
      <label htmlFor={field}>{label}{required && <RequiredMark />}</label>
      <select id={field} name={field} className={`select${error ? ' input-error' : ''}`} value={value}
        aria-invalid={Boolean(error)} aria-required={required || undefined}
        aria-describedby={error ? errorId : undefined}
        onChange={(event) => onChange(field, event.target.value)} onBlur={() => onBlur(field)}>
        {children}
      </select>
      {error && <span className="field-error" id={errorId}>{error}</span>}
    </div>
  )
}

function RequiredMark() {
  return <span className="required-mark" aria-hidden="true"> *</span>
}

function removeError(errors: FormErrors, field: FormField): FormErrors {
  if (!errors[field]) return errors
  const next = { ...errors }
  delete next[field]
  return next
}
