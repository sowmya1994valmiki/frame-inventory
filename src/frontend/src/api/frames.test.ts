import { afterEach, describe, expect, it, vi } from 'vitest'
import { getFrame, getFrames } from './frames'

describe('frame API', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('builds a normalized inventory query and forwards the abort signal', async () => {
    const page = { items: [], page: 2, size: 50, totalElements: 0, totalPages: 0 }
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(page), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)
    const controller = new AbortController()

    await getFrames({
      page: 2,
      size: 50,
      q: ' station ',
      status: 'LIVE',
      mediaType: ' DIGITAL ',
      environment: ' ',
      format: '',
      region: ' London ',
    }, controller.signal)

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/frames?page=2&size=50&q=station&status=LIVE&mediaType=DIGITAL&region=London',
      { signal: controller.signal, headers: { Accept: 'application/json' } },
    )
  })

  it('maps problem details and malformed failures to safe API errors', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({
        detail: 'One or more fields are invalid',
        errors: [{ field: 'frameId', message: 'Frame ID is required.' }],
      }), { status: 400 }))
      .mockResolvedValueOnce(new Response('not-json', { status: 503 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(getFrame('unsafe/id')).rejects.toMatchObject({
      name: 'FrameApiError',
      status: 400,
      message: 'One or more fields are invalid',
      fieldErrors: [{ field: 'frameId', message: 'Frame ID is required.' }],
    })
    expect(fetchMock.mock.calls[0]?.[0]).toBe('/api/frames/unsafe%2Fid')

    await expect(getFrame('FRAME-2')).rejects.toMatchObject({
      name: 'FrameApiError', status: 503, message: 'Request failed with status 503', fieldErrors: [],
    })
  })
})
