import { useEditorStore } from "@/store/useEditorStore";
import type { SlotSize, GuiDefaults } from "@/types/imago";
import { SLOT_SIZES } from "@/lib/constants";
import { useState } from "react";

export function PropertyPanel() {
  const guiFolders = useEditorStore((s) => s.guiFolders);
  const selectedEntry = useEditorStore((s) => s.selectedEntry);
  const updateGuiEntry = useEditorStore((s) => s.updateGuiEntry);
  const updateCharEntry = useEditorStore((s) => s.updateCharEntry);
  const setGuiDefaults = useEditorStore((s) => s.setGuiDefaults);
  const activeView = useEditorStore((s) => s.activeView);
  const setActiveView = useEditorStore((s) => s.setActiveView);

  const [selectedFolder, setSelectedFolder] = useState<SlotSize>(54);

  return (
    <div className="w-72 bg-zinc-900 border-l border-zinc-700 flex flex-col h-full select-none">
      {/* Header */}
      <div className="p-3 border-b border-zinc-700">
        <span className="text-sm font-medium text-zinc-300">Properties</span>
      </div>

      <div className="flex-1 overflow-y-auto">
        {/* View toggle */}
        <div className="p-3 border-b border-zinc-700">
          <div className="flex gap-1 bg-zinc-800 rounded-md p-0.5">
            <button
              onClick={() => setActiveView("pixel-editor")}
              className={`flex-1 px-3 py-1 rounded text-xs transition-colors ${
                activeView === "pixel-editor"
                  ? "bg-blue-600 text-white"
                  : "text-zinc-400 hover:text-white"
              }`}
            >
              Pixel Editor
            </button>
            <button
              onClick={() => setActiveView("layout-editor")}
              className={`flex-1 px-3 py-1 rounded text-xs transition-colors ${
                activeView === "layout-editor"
                  ? "bg-blue-600 text-white"
                  : "text-zinc-400 hover:text-white"
              }`}
            >
              Layouts
            </button>
          </div>
        </div>

        {/* Selected Entry Properties */}
        {selectedEntry?.type === "gui" && (
          <GUIFolderDefaultsSection
            folders={guiFolders}
            selectedFolder={selectedFolder}
            setSelectedFolder={setSelectedFolder}
            setGuiDefaults={setGuiDefaults}
          />
        )}

        {selectedEntry && (
          <SelectedEntryProperties
            selectedEntry={selectedEntry}
            guiFolders={guiFolders}
            updateGuiEntry={updateGuiEntry}
            updateCharEntry={updateCharEntry}
          />
        )}
        {!selectedEntry && (
          <div className="p-4 text-sm text-zinc-500 text-center">
            Select a resource to view properties
          </div>
        )}
      </div>
    </div>
  );
}

function GUIFolderDefaultsSection({
  folders,
  selectedFolder,
  setSelectedFolder,
  setGuiDefaults,
}: {
  folders: ReturnType<typeof useEditorStore.getState>["guiFolders"];
  selectedFolder: SlotSize;
  setSelectedFolder: (s: SlotSize) => void;
  setGuiDefaults: (slot: SlotSize, d: GuiDefaults) => void;
}) {
  const folder = folders.find((f) => f.slot === selectedFolder);
  if (!folder) return null;

  return (
    <div className="p-3 border-b border-zinc-700">
      <h4 className="text-xs text-zinc-500 font-semibold uppercase mb-2">
        Folder Defaults
      </h4>

      <div className="mb-2">
        <select
          value={selectedFolder}
          onChange={(e) => setSelectedFolder(Number(e.target.value) as SlotSize)}
          className="w-full bg-zinc-700 text-zinc-200 border border-zinc-600 rounded px-2 py-1 text-xs focus:outline-none focus:border-blue-500"
        >
          {SLOT_SIZES.map((s) => (
            <option key={s} value={s}>
              {s} Slots
            </option>
          ))}
        </select>
      </div>

      <div className="grid grid-cols-2 gap-2">
        <div>
          <label className="block text-xs text-zinc-500 mb-0.5">Ascent</label>
          <input
            type="number"
            value={folder.defaults.ascent}
            onChange={(e) =>
              setGuiDefaults(selectedFolder, {
                ...folder.defaults,
                ascent: Number(e.target.value),
              })
            }
            className="w-full bg-zinc-700 text-zinc-200 border border-zinc-600 rounded px-2 py-1 text-xs focus:outline-none focus:border-blue-500"
          />
        </div>
        <div>
          <label className="block text-xs text-zinc-500 mb-0.5">Height</label>
          <input
            type="number"
            value={folder.defaults.height}
            onChange={(e) =>
              setGuiDefaults(selectedFolder, {
                ...folder.defaults,
                height: Number(e.target.value),
              })
            }
            className="w-full bg-zinc-700 text-zinc-200 border border-zinc-600 rounded px-2 py-1 text-xs focus:outline-none focus:border-blue-500"
          />
        </div>
      </div>
    </div>
  );
}

function SelectedEntryProperties({
  selectedEntry,
  guiFolders,
  updateGuiEntry,
  updateCharEntry,
}: {
  selectedEntry: NonNullable<ReturnType<typeof useEditorStore.getState>["selectedEntry"]>;
  guiFolders: ReturnType<typeof useEditorStore.getState>["guiFolders"];
  updateGuiEntry: ReturnType<typeof useEditorStore.getState>["updateGuiEntry"];
  updateCharEntry: ReturnType<typeof useEditorStore.getState>["updateCharEntry"];
}) {
  if (selectedEntry.type === "gui") {
    const folder = guiFolders.find((f) => f.slot === selectedEntry.folderSlot);
    const entry = folder?.entries.find((e) => e.name === selectedEntry.entryName);
    if (!entry) return null;

    return (
      <div className="p-3">
        <h4 className="text-xs text-zinc-500 font-semibold uppercase mb-2">
          Entry: {entry.name}
        </h4>

        <div className="space-y-2">
          {/* Character */}
          <div>
            <label className="block text-xs text-zinc-500 mb-0.5">Character</label>
            <input
              type="text"
              value={entry.character}
              readOnly
              className="w-full bg-zinc-800 text-zinc-400 border border-zinc-700 rounded px-2 py-1 text-xs"
            />
          </div>

          {/* Image dimensions */}
          {entry.textureData && (
            <div>
              <label className="block text-xs text-zinc-500 mb-0.5">Size</label>
              <span className="text-xs text-zinc-400">
                {entry.textureData.width} x {entry.textureData.height}
              </span>
            </div>
          )}

          {/* Use defaults */}
          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={entry.useDefaults}
              onChange={(e) =>
                updateGuiEntry(selectedEntry.folderSlot, entry.name, {
                  useDefaults: e.target.checked,
                })
              }
              className="rounded bg-zinc-700 border-zinc-600"
            />
            <span className="text-xs text-zinc-400">Use folder defaults</span>
          </div>

          {!entry.useDefaults && (
            <>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-xs text-zinc-500 mb-0.5">Ascent</label>
                  <input
                    type="number"
                    value={entry.ascent}
                    onChange={(e) =>
                      updateGuiEntry(selectedEntry.folderSlot, entry.name, {
                        ascent: Number(e.target.value),
                      })
                    }
                    className="w-full bg-zinc-700 text-zinc-200 border border-zinc-600 rounded px-2 py-1 text-xs focus:outline-none focus:border-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-xs text-zinc-500 mb-0.5">Height</label>
                  <input
                    type="number"
                    value={entry.height}
                    onChange={(e) =>
                      updateGuiEntry(selectedEntry.folderSlot, entry.name, {
                        height: Number(e.target.value),
                      })
                    }
                    className="w-full bg-zinc-700 text-zinc-200 border border-zinc-600 rounded px-2 py-1 text-xs focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>
            </>
          )}

          <div>
            <label className="block text-xs text-zinc-500 mb-0.5">Shift X</label>
            <input
              type="number"
              value={entry.shiftX}
              onChange={(e) =>
                updateGuiEntry(selectedEntry.folderSlot, entry.name, {
                  shiftX: Number(e.target.value),
                })
              }
              className="w-full bg-zinc-700 text-zinc-200 border border-zinc-600 rounded px-2 py-1 text-xs focus:outline-none focus:border-blue-500"
            />
          </div>
        </div>
      </div>
    );
  }

  // Char entry properties
  const entry = useEditorStore.getState().charEntries.find(
    (e) => e.name === selectedEntry.entryName
  );
  if (!entry) return null;

  return (
    <div className="p-3">
      <h4 className="text-xs text-zinc-500 font-semibold uppercase mb-2">
        Char: {entry.name}
      </h4>

      <div className="space-y-2">
        <div>
          <label className="block text-xs text-zinc-500 mb-0.5">Character</label>
          <input
            type="text"
            value={entry.character}
            readOnly
            className="w-full bg-zinc-800 text-zinc-400 border border-zinc-700 rounded px-2 py-1 text-xs"
          />
        </div>

        {entry.textureData && (
          <div>
            <label className="block text-xs text-zinc-500 mb-0.5">Size</label>
            <span className="text-xs text-zinc-400">
              {entry.textureData.width} x {entry.textureData.height}
            </span>
          </div>
        )}

        <div className="grid grid-cols-2 gap-2">
          <div>
            <label className="block text-xs text-zinc-500 mb-0.5">Ascent</label>
            <input
              type="number"
              value={entry.ascent}
              onChange={(e) =>
                updateCharEntry(entry.name, { ascent: Number(e.target.value) })
              }
              className="w-full bg-zinc-700 text-zinc-200 border border-zinc-600 rounded px-2 py-1 text-xs focus:outline-none focus:border-blue-500"
            />
          </div>
          <div>
            <label className="block text-xs text-zinc-500 mb-0.5">Height</label>
            <input
              type="number"
              value={entry.height}
              onChange={(e) =>
                updateCharEntry(entry.name, { height: Number(e.target.value) })
              }
              className="w-full bg-zinc-700 text-zinc-200 border border-zinc-600 rounded px-2 py-1 text-xs focus:outline-none focus:border-blue-500"
            />
          </div>
        </div>
      </div>
    </div>
  );
}
