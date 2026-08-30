export type FrameSummary = {
  frameId: string
  mediaType: string
  format: string
  environment: string | null
  status: string
  modifiedDate: string
  address: string | null
  town: string | null
  region: string | null
  station: string | null
  airport: string | null
}

export type LocationDetails = {
  postcode: string
  postcodeArea: string | null
  postcodeDistrict: string | null
  postcodeSector: string | null
  postcodeUnit: string | null
  address: string | null
  region: string
  countryCode: string | null
  town: string
  longitude: number | null
  latitude: number | null
  distanceToClosestSchool: number | null
  rawLocationPoint: string | null
  locationId: string | null
}

export type SiteDetails = {
  siteNumber: string
  inventorySiteNumber: string | null
  panelNumber: string | null
  station: string | null
  airport: string | null
}

export type TechnicalDetails = {
  illuminationTypeId: string | null
  numberOfSlots: number | null
  sizeCode: string | null
  sizeGroupCode: string | null
  aspectRatioCode: string | null
  sizeCategory: string | null
  pixelHeight: number | null
  pixelWidth: number | null
}

export type CommercialDetails = {
  impactWeight: number | null
  productionRateCard: string | null
  legacyProductionRateCard: string | null
  pricingGrade: string | null
  priceEntityId: string | null
  premium: boolean | null
}

export type IntegrationDetails = {
  broadsignDisplayUnitId: string | null
  broadsignFrameId: string | null
  broadsignDomainId: string | null
  linkedFrameIds: string | null
}

export type FrameWritableDetails = {
  mediaType: string
  format: string
  environment: string | null
  status: 'LIVE' | 'INACTIVE'
  statusReason: string | null
  location: LocationDetails
  site: SiteDetails
  technical: TechnicalDetails | null
  commercial: CommercialDetails | null
  integrations: IntegrationDetails | null
}

export type CreateFrameRequest = FrameWritableDetails & { frameId: string }
export type UpdateFrameRequest = FrameWritableDetails

export type Frame = FrameWritableDetails & {
  frameId: string
  createdDate: string
  modifiedDate: string
}

export type FrameHistoryEventType = 'CREATED' | 'IMPORTED' | 'UPDATED'
export type FrameHistorySource = 'MANUAL' | 'CSV_UPLOAD'

export type FrameFieldChange = {
  old: string | null
  new: string | null
}

export type FrameHistoryEntry = {
  eventType: FrameHistoryEventType
  occurredAt: string
  source: FrameHistorySource
  changedFields: Record<string, FrameFieldChange>
}

export type FramePage = {
  items: FrameSummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type FrameFilters = {
  status: '' | 'LIVE' | 'INACTIVE'
  mediaType: string
  environment: string
  format: string
  region: string
}

export type GetFramesParams = FrameFilters & { page: number; size: number; q: string }
export type FrameFieldError = { field: string; message: string }
type ProblemDetail = { title?: string; detail?: string; errors?: FrameFieldError[] }

export class FrameApiError extends Error {
  status: number
  fieldErrors: FrameFieldError[]

  constructor(message: string, status: number, fieldErrors: FrameFieldError[] = []) {
    super(message)
    this.name = 'FrameApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

export async function getFrames(
  params: GetFramesParams,
  signal?: AbortSignal,
): Promise<FramePage> {
  const searchParams = new URLSearchParams({ page: String(params.page), size: String(params.size) })
  addIfPresent(searchParams, 'q', params.q)
  addIfPresent(searchParams, 'status', params.status)
  addIfPresent(searchParams, 'mediaType', params.mediaType)
  addIfPresent(searchParams, 'environment', params.environment)
  addIfPresent(searchParams, 'format', params.format)
  addIfPresent(searchParams, 'region', params.region)

  return request<FramePage>(`/api/frames?${searchParams.toString()}`, { signal })
}

export function getFrame(frameId: string, signal?: AbortSignal): Promise<Frame> {
  return request<Frame>(`/api/frames/${encodeURIComponent(frameId)}`, { signal })
}

export function getFrameHistory(frameId: string, signal?: AbortSignal): Promise<FrameHistoryEntry[]> {
  return request<FrameHistoryEntry[]>(`/api/frames/${encodeURIComponent(frameId)}/history`, { signal })
}

export function createFrame(payload: CreateFrameRequest): Promise<Frame> {
  return request<Frame>('/api/frames', jsonRequest('POST', payload))
}

export function updateFrame(frameId: string, payload: UpdateFrameRequest): Promise<Frame> {
  return request<Frame>(`/api/frames/${encodeURIComponent(frameId)}`, jsonRequest('PUT', payload))
}

function addIfPresent(params: URLSearchParams, key: string, value: string) {
  const normalizedValue = value.trim()
  if (normalizedValue) params.set(key, normalizedValue)
}

function jsonRequest(method: 'POST' | 'PUT', body: CreateFrameRequest | UpdateFrameRequest): RequestInit {
  return {
    method,
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }
}

async function request<T>(url: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(url, {
    ...init,
    headers: init.headers ?? { Accept: 'application/json' },
  })
  if (!response.ok) throw await readApiError(response)
  return (await response.json()) as T
}

async function readApiError(response: Response): Promise<FrameApiError> {
  try {
    const problem = (await response.json()) as ProblemDetail
    return new FrameApiError(
      problem.detail || problem.title || `Request failed with status ${response.status}`,
      response.status,
      Array.isArray(problem.errors) ? problem.errors : [],
    )
  } catch {
    return new FrameApiError(`Request failed with status ${response.status}`, response.status)
  }
}
