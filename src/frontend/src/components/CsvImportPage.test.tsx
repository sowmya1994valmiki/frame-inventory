import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { importFrames } from '../api/frames'
import { CsvImportPage } from './CsvImportPage'

vi.mock('../api/frames', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/frames')>()
  return { ...actual, importFrames: vi.fn() }
})

const mockedImportFrames = vi.mocked(importFrames)

describe('CsvImportPage', () => {
  beforeEach(() => mockedImportFrames.mockReset())

  it('renders a partial import summary and row-level errors', async () => {
    mockedImportFrames.mockResolvedValue({
      totalRows: 4,
      created: 2,
      duplicates: 1,
      failed: 1,
      errors: [
        { rowNumber: 3, frameId: 'FRAME-3', reason: 'Frame already exists' },
        { rowNumber: 4, frameId: null, reason: 'Frame ID is required' },
      ],
    })
    const user = userEvent.setup()
    render(<CsvImportPage onViewInventory={vi.fn()} />)
    const file = new File(['frame_id\nFRAME-1'], 'frames.csv', { type: 'text/csv' })

    await user.upload(screen.getByLabelText('CSV file'), file)
    await user.click(screen.getByRole('button', { name: 'Import CSV' }))

    expect(await screen.findByRole('heading', { name: 'Import partially successful' })).toBeInTheDocument()
    expect(screen.getByText('2 of 4 rows were imported.')).toBeInTheDocument()
    expect(within(screen.getByLabelText('Import summary')).getByText('2')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Rows not imported' })).toBeInTheDocument()
    expect(screen.getByText('FRAME-3')).toBeInTheDocument()
    expect(screen.getByText('Frame already exists')).toBeInTheDocument()
    expect(mockedImportFrames).toHaveBeenCalledWith(file)
  })
})
