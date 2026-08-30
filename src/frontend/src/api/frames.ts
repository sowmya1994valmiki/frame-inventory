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
type ProblemDetail = { title?: string; detail?: string }

export class FrameApiError extends Error {
  status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'FrameApiError'
    this.status = status
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

  const response = await fetch(`/api/frames?${searchParams.toString()}`, {
    headers: { Accept: 'application/json' },
    signal,
  })
  if (!response.ok) throw new FrameApiError(await readErrorMessage(response), response.status)
  return (await response.json()) as FramePage
}

function addIfPresent(params: URLSearchParams, key: string, value: string) {
  const normalizedValue = value.trim()
  if (normalizedValue) params.set(key, normalizedValue)
}

async function readErrorMessage(response: Response): Promise<string> {
  try {
    const problem = (await response.json()) as ProblemDetail
    return problem.detail || problem.title || `Request failed with status ${response.status}`
  } catch {
    return `Request failed with status ${response.status}`
  }
}
