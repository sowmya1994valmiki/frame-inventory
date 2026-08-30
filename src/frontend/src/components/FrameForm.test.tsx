import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { FrameForm } from './FrameForm'
import { emptyFrameForm } from './frameFormModel'

describe('FrameForm', () => {
  it('blocks submission and shows useful errors for invalid values', async () => {
    const onSubmit = vi.fn()
    const user = userEvent.setup()
    render(
      <FrameForm mode="create" initialValues={{
        ...emptyFrameForm(),
        mediaType: 'DIGITAL',
        format: 'D6',
        postcode: 'W1J 9DZ',
        region: 'London',
        town: 'London',
        siteNumber: 'SITE-1',
        longitude: 'west',
      }} submitting={false} submitError={null} serverErrors={{}} onSubmit={onSubmit}
      onCancel={vi.fn()} onValueChange={vi.fn()} />,
    )

    await user.click(screen.getByRole('button', { name: 'Create frame' }))

    expect(onSubmit).not.toHaveBeenCalled()
    expect(screen.getByRole('alert')).toHaveTextContent('Check the highlighted fields.')
    expect(screen.getByText('Frame ID is required.')).toBeInTheDocument()
    expect(screen.getByText('Longitude must be a number.')).toBeInTheDocument()
  })
})
