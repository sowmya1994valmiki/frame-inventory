import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getFrames, type FrameSummary } from '../api/frames'
import { InventoryPage } from './InventoryPage'

vi.mock('../api/frames', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/frames')>()
  return { ...actual, getFrames: vi.fn() }
})

const FRAME: FrameSummary = {
  frameId: 'FRAME-1',
  mediaType: 'DIGITAL',
  format: 'D6',
  environment: 'UNDERGROUND',
  status: 'LIVE',
  modifiedDate: '2026-08-31T10:00:00Z',
  address: 'Platform 1',
  town: 'London',
  region: 'London',
  station: 'Central',
  airport: null,
}

const mockedGetFrames = vi.mocked(getFrames)

describe('InventoryPage', () => {
  beforeEach(() => {
    mockedGetFrames.mockReset()
    mockedGetFrames.mockImplementation(async (params) => ({
      items: [FRAME],
      page: params.page,
      size: params.size,
      totalElements: 40,
      totalPages: 2,
    }))
  })

  it('normalizes typed filters, resets pagination, and debounces trimmed searches', async () => {
    const user = userEvent.setup()
    render(
      <InventoryPage successMessage={null} focusOnMount={false} onNew={vi.fn()} onImport={vi.fn()}
        onView={vi.fn()} onEdit={vi.fn()} />,
    )

    await screen.findByRole('button', { name: 'FRAME-1' })
    expect(mockedGetFrames).toHaveBeenNthCalledWith(1, {
      status: '', mediaType: '', environment: '', format: '', region: '', page: 0, size: 20, q: '',
    }, expect.any(AbortSignal))

    await user.click(screen.getByRole('button', { name: 'Next' }))
    await waitFor(() => expect(mockedGetFrames.mock.calls.at(-1)?.[0].page).toBe(1))

    await user.type(screen.getByLabelText('Media type'), '  DIGITAL  ')
    await waitFor(() => expect(mockedGetFrames.mock.calls.at(-1)?.[0]).toMatchObject({
      mediaType: 'DIGITAL', page: 0,
    }))

    const callsBeforeSearchSettles = mockedGetFrames.mock.calls.length
    await user.type(screen.getByLabelText('Search'), '  London  ')
    expect(mockedGetFrames).toHaveBeenCalledTimes(callsBeforeSearchSettles)
    await waitFor(() => expect(mockedGetFrames.mock.calls.at(-1)?.[0]).toMatchObject({
      mediaType: 'DIGITAL', q: 'London', page: 0,
    }))
  })
})
