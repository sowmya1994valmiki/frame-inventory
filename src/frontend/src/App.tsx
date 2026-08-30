import { useState } from 'react'
import './App.css'
import { AppShell } from './components/AppShell'
import { FrameDetailPage } from './components/FrameDetailPage'
import { FrameEditorPage } from './components/FrameEditorPage'
import { InventoryPage } from './components/InventoryPage'
import { CsvImportPage } from './components/CsvImportPage'

type Screen =
  | { name: 'inventory' }
  | { name: 'create' }
  | { name: 'import' }
  | { name: 'detail'; frameId: string }
  | { name: 'edit'; frameId: string }

function App() {
  const [screen, setScreen] = useState<Screen>({ name: 'inventory' })
  const [successMessage, setSuccessMessage] = useState<string | null>(null)
  const [restoreInventoryFocus, setRestoreInventoryFocus] = useState(false)

  const openScreen = (nextScreen: Screen) => {
    setSuccessMessage(null)
    setRestoreInventoryFocus(true)
    setScreen(nextScreen)
  }

  const saved = (frameId: string, mode: 'create' | 'edit') => {
    setSuccessMessage(`Frame ${frameId} ${mode === 'create' ? 'created' : 'updated'}.`)
    setScreen({ name: 'inventory' })
  }

  const breadcrumbs = screen.name === 'create' ? ['Frames', 'New']
    : screen.name === 'import' ? ['Frames', 'Import CSV']
    : screen.name === 'edit' ? ['Frames', 'Edit']
      : screen.name === 'detail' ? ['Frames', screen.frameId] : ['Frames']

  return (
    <AppShell breadcrumbs={breadcrumbs}>
      {screen.name === 'inventory' ? (
        <InventoryPage successMessage={successMessage} focusOnMount={restoreInventoryFocus}
          onNew={() => openScreen({ name: 'create' })}
          onImport={() => openScreen({ name: 'import' })}
          onView={(frameId) => openScreen({ name: 'detail', frameId })}
          onEdit={(frameId) => openScreen({ name: 'edit', frameId })} />
      ) : screen.name === 'import' ? (
        <CsvImportPage onViewInventory={() => setScreen({ name: 'inventory' })} />
      ) : screen.name === 'detail' ? (
        <FrameDetailPage frameId={screen.frameId} onBack={() => setScreen({ name: 'inventory' })} />
      ) : (
        <FrameEditorPage mode={screen.name} frameId={screen.name === 'edit' ? screen.frameId : undefined}
          onCancel={() => setScreen({ name: 'inventory' })} onSuccess={saved} />
      )}
    </AppShell>
  )
}

export default App
