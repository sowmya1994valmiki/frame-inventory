import { describe, expect, it } from 'vitest'
import { emptyFrameForm, toCreateRequest, toUpdateRequest } from './frameFormModel'

describe('frame request mapping', () => {
  it('normalizes form values and omits the immutable frame ID from updates', () => {
    const values = {
      ...emptyFrameForm(),
      frameId: '  FRAME-1  ',
      mediaType: ' DIGITAL ',
      format: ' D6 ',
      status: 'INACTIVE',
      postcode: ' W1J 9DZ ',
      region: ' London ',
      town: ' Westminster ',
      siteNumber: ' SITE-1 ',
      longitude: ' -0.1417 ',
      numberOfSlots: '6',
      pixelWidth: '1920',
      impactWeight: '0.75',
      premium: 'false' as const,
    }

    const createRequest = toCreateRequest(values)
    expect(createRequest).toMatchObject({
      frameId: 'FRAME-1',
      mediaType: 'DIGITAL',
      format: 'D6',
      environment: null,
      status: 'INACTIVE',
      location: { postcode: 'W1J 9DZ', region: 'London', town: 'Westminster', longitude: -0.1417 },
      site: { siteNumber: 'SITE-1' },
      technical: { numberOfSlots: 6, pixelWidth: 1920 },
      commercial: { impactWeight: 0.75, premium: false },
      integrations: null,
    })

    const updateRequest = toUpdateRequest(values)
    expect(updateRequest).not.toHaveProperty('frameId')
    expect(updateRequest.location.latitude).toBeNull()
  })
})
