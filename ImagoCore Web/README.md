# ImagoCore Web Editor

An online visual editor for Minecraft resource packs, purpose-built for the ImagoCore guild plugin system. Features pixel-level drawing, GUI slot layout editing, and one-click export of complete resource packs.

## Features

- **Pixel Brush Drawing** — Pencil, eraser, eyedropper, and fill tools with transparent checkerboard background and free zoom/pan
- **Resource Management** — Manage GUI background images by slot size (9/18/27/36/45/54), with per-entry ascent/height/shift_x configuration
- **Character Image Management** — Manage character images under the char directory with per-entry ascent/height properties
- **Slot Layout Editor** — Visual 6×9 slot grid for assigning function items by click, with transparent carrier (BARRIER + custom_model_data) configuration
- **GUI Binding Configuration** — 28+ GUI type to ImagoCore entry bindings, with overlay decoration management
- **Import/Export** — Import `build.zip` to resume editing; export the full ImagoCore directory + `imago-gui.yml` + `gui-image-layout.yml`

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | React 19 + TypeScript |
| Build | Vite |
| Styling | Tailwind CSS + shadcn/ui |
| State Management | Zustand (with IndexedDB persistence) |
| Graphics Editing | HTML Canvas 2D API |
| File Handling | JSZip (pack/unpack), js-yaml (config generation) |
| Storage | IndexedDB (idb-keyval) |

## Development

```bash
# Install dependencies
npm install

# Start dev server
npm run dev

# Production build
npm run build

# TypeScript type check
npx tsc --noEmit
```

## Project Structure

```
src/
├── types/imago.ts              # TypeScript type definitions
├── store/useEditorStore.ts     # Zustand global state (with persistence)
├── hooks/usePixelEditor.ts     # Canvas pixel editing core hook
├── lib/
│   ├── constants.ts            # Constant configuration
│   ├── utils.ts                # Utility functions
│   ├── export-helpers.ts       # ZIP export logic
│   ├── import-helpers.ts       # build.zip import logic
│   └── persistence.ts          # IndexedDB persistence adapter
├── components/
│   ├── editor/
│   │   ├── PixelCanvas.tsx     # Pixel canvas component
│   │   └── PixelToolbar.tsx    # Toolbar component
│   ├── panels/
│   │   ├── ResourcePanel.tsx   # Left sidebar resource tree
│   │   ├── PropertyPanel.tsx   # Right sidebar property panel
│   │   ├── ConfigPanel.tsx     # imago-gui.yml config panel
│   │   └── LayoutEditor.tsx    # gui-image-layout.yml layout editor
│   └── dialogs/
│       ├── AddGuiImageDialog.tsx   # Add GUI image dialog
│       ├── AddCharImageDialog.tsx  # Add character image dialog
│       └── ImportExportDialog.tsx  # Import/export dialog
├── App.tsx                     # Root layout
├── main.tsx                    # Entry point
└── index.css                   # Global styles
```

## Related Projects

This tool serves the ImagoCore image system of [Guild Plugin](https://github.com/chenasyd/-GuildPlugin). Generated files must be used with the plugin.

---

Built on the [Vite React TypeScript template](https://github.com/vitejs/vite/tree/main/packages/create-vite/template-react-ts).
