import { useEditorStore } from "@/store/useEditorStore";
import type { SlotSize, GuiEntry, CharEntry } from "@/types/imago";
import { SLOT_SIZES } from "@/lib/constants";
import { Plus, Trash2, Folder, Image, FileImage, GripVertical } from "lucide-react";
import { useState } from "react";
import { AddGuiImageDialog } from "@/components/dialogs/AddGuiImageDialog";
import { AddCharImageDialog } from "@/components/dialogs/AddCharImageDialog";

export function ResourcePanel() {
  const guiFolders = useEditorStore((s) => s.guiFolders);
  const charEntries = useEditorStore((s) => s.charEntries);
  const selectedEntry = useEditorStore((s) => s.selectedEntry);
  const selectEntry = useEditorStore((s) => s.selectEntry);
  const removeGuiEntry = useEditorStore((s) => s.removeGuiEntry);
  const removeCharEntry = useEditorStore((s) => s.removeCharEntry);

  const [showAddGui, setShowAddGui] = useState(false);
  const [showAddChar, setShowAddChar] = useState(false);
  const [expandedFolders, setExpandedFolders] = useState<Set<SlotSize>>(
    new Set(SLOT_SIZES)
  );

  const toggleFolder = (s: SlotSize) => {
    setExpandedFolders((prev) => {
      const next = new Set(prev);
      if (next.has(s)) next.delete(s);
      else next.add(s);
      return next;
    });
  };

  if (guiFolders.length === 0) {
    return (
      <div className="w-64 bg-zinc-900 border-r border-zinc-700 p-4 flex flex-col">
        <button
          onClick={() => useEditorStore.getState().initGuiFolders()}
          className="px-3 py-2 bg-blue-600 text-white rounded text-sm hover:bg-blue-700 transition-colors"
        >
          Initialize Editor
        </button>
      </div>
    );
  }

  return (
    <>
      <div className="w-64 bg-zinc-900 border-r border-zinc-700 flex flex-col h-full select-none">
        {/* Header */}
        <div className="p-3 border-b border-zinc-700 flex items-center justify-between">
          <span className="text-sm font-medium text-zinc-300">Resources</span>
          <div className="flex gap-1">
            <button
              onClick={() => setShowAddGui(true)}
              className="p-1 rounded text-zinc-400 hover:text-white hover:bg-zinc-700 transition-colors"
              title="Add GUI Image"
            >
              <Plus size={14} />
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto">
          {/* GUI Images section */}
          <div className="p-2">
            <div className="text-xs text-zinc-500 uppercase font-semibold px-2 py-1">
              GUI Images
            </div>
            {guiFolders.map((folder) => (
              <div key={folder.slot}>
                <button
                  onClick={() => toggleFolder(folder.slot)}
                  className="w-full flex items-center gap-1.5 px-2 py-1.5 text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 rounded transition-colors"
                >
                  <Folder size={14} />
                  <span className="flex-1 text-left">
                    {folder.slot} Slots
                  </span>
                  <span className="text-xs text-zinc-500">
                    {folder.entries.length}
                  </span>
                </button>

                {expandedFolders.has(folder.slot) &&
                  folder.entries.map((entry) => (
                    <div
                      key={entry.name}
                      role="button"
                      tabIndex={0}
                      onClick={() =>
                        selectEntry({
                          type: "gui",
                          folderSlot: folder.slot,
                          entryName: entry.name,
                        })
                      }
                      onKeyDown={(e) => {
                        if (e.key === "Enter" || e.key === " ") {
                          e.preventDefault();
                          selectEntry({
                            type: "gui",
                            folderSlot: folder.slot,
                            entryName: entry.name,
                          });
                        }
                      }}
                      className={`w-full flex items-center gap-1.5 pl-7 pr-2 py-1.5 text-sm rounded transition-colors group cursor-pointer ${
                        selectedEntry?.type === "gui" &&
                        selectedEntry.folderSlot === folder.slot &&
                        selectedEntry.entryName === entry.name
                          ? "bg-blue-600/30 text-blue-300"
                          : "text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800"
                      }`}
                    >
                      <Image size={14} className="shrink-0" />
                      <span className="flex-1 text-left truncate">
                        {entry.name}
                      </span>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          removeGuiEntry(folder.slot, entry.name);
                          if (
                            selectedEntry?.type === "gui" &&
                            selectedEntry.folderSlot === folder.slot &&
                            selectedEntry.entryName === entry.name
                          ) {
                            selectEntry(null);
                          }
                        }}
                        className="opacity-0 group-hover:opacity-100 p-0.5 text-zinc-500 hover:text-red-400 transition-all shrink-0"
                      >
                        <Trash2 size={12} />
                      </button>
                    </div>
                  ))}

                {folder.entries.length === 0 && (
                  <div className="pl-7 pr-2 py-1 text-xs text-zinc-600">
                    No entries
                  </div>
                )}
              </div>
            ))}
          </div>

          <div className="border-t border-zinc-700 mx-3" />

          {/* Char Images section */}
          <div className="p-2">
            <div className="flex items-center justify-between px-2 py-1">
              <span className="text-xs text-zinc-500 uppercase font-semibold">
                Char Images
              </span>
              <button
                onClick={() => setShowAddChar(true)}
                className="p-0.5 rounded text-zinc-500 hover:text-white hover:bg-zinc-700 transition-colors"
                title="Add Char Image"
              >
                <Plus size={12} />
              </button>
            </div>

            {charEntries.map((entry) => (
              <div
                key={entry.name}
                role="button"
                tabIndex={0}
                onClick={() =>
                  selectEntry({
                    type: "char",
                    entryName: entry.name,
                  })
                }
                onKeyDown={(e) => {
                  if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    selectEntry({
                      type: "char",
                      entryName: entry.name,
                    });
                  }
                }}
                className={`w-full flex items-center gap-1.5 px-2 py-1.5 text-sm rounded transition-colors group cursor-pointer ${
                  selectedEntry?.type === "char" &&
                  selectedEntry.entryName === entry.name
                    ? "bg-blue-600/30 text-blue-300"
                    : "text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800"
                }`}
              >
                <FileImage size={14} className="shrink-0" />
                <span className="flex-1 text-left truncate">{entry.name}</span>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    removeCharEntry(entry.name);
                    if (
                      selectedEntry?.type === "char" &&
                      selectedEntry.entryName === entry.name
                    ) {
                      selectEntry(null);
                    }
                  }}
                  className="opacity-0 group-hover:opacity-100 p-0.5 text-zinc-500 hover:text-red-400 transition-all shrink-0"
                >
                  <Trash2 size={12} />
                </button>
              </div>
            ))}

            {charEntries.length === 0 && (
              <div className="px-2 py-1 text-xs text-zinc-600">
                No char images
              </div>
            )}
          </div>
        </div>
      </div>

      <AddGuiImageDialog open={showAddGui} onClose={() => setShowAddGui(false)} />
      <AddCharImageDialog open={showAddChar} onClose={() => setShowAddChar(false)} />
    </>
  );
}
