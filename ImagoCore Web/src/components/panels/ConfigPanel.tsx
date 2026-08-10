import { useEditorStore } from "@/store/useEditorStore";
import { GUI_TYPE_OPTIONS } from "@/lib/constants";
import type { GuiTypeId } from "@/types/imago";
import { Plus, X } from "lucide-react";
import { useState } from "react";

export function ConfigPanel() {
  const bindings = useEditorStore((s) => s.bindings);
  const overlays = useEditorStore((s) => s.overlays);
  const enabled = useEditorStore((s) => s.enabled);
  const defaultEntry = useEditorStore((s) => s.defaultEntry);
  const setEnabled = useEditorStore((s) => s.setEnabled);
  const setDefaultEntry = useEditorStore((s) => s.setDefaultEntry);
  const setBinding = useEditorStore((s) => s.setBinding);
  const removeBinding = useEditorStore((s) => s.removeBinding);
  const addOverlay = useEditorStore((s) => s.addOverlay);
  const removeOverlay = useEditorStore((s) => s.removeOverlay);
  const guiFolders = useEditorStore((s) => s.guiFolders);
  const charEntries = useEditorStore((s) => s.charEntries);

  const [newOverlayGui, setNewOverlayGui] = useState<GuiTypeId>("GUILD");
  const [newOverlayChar, setNewOverlayChar] = useState("");
  const [newOverlayX, setNewOverlayX] = useState(0);
  const [showOverlayForm, setShowOverlayForm] = useState(false);

  // Collect all available entry references
  const allGuiIds = guiFolders.flatMap((f) =>
    f.entries.map((e) => `${f.slot}/${e.name}`)
  );

  return (
    <div className="w-80 bg-zinc-900 border-l border-zinc-700 flex flex-col h-full select-none overflow-y-auto">
      <div className="p-3 border-b border-zinc-700">
        <span className="text-sm font-medium text-zinc-300">imago-gui.yml</span>
      </div>

      {/* Enabled */}
      <div className="p-3 border-b border-zinc-700">
        <div className="flex items-center gap-2">
          <input
            type="checkbox"
            checked={enabled}
            onChange={(e) => setEnabled(e.target.checked)}
            className="rounded bg-zinc-700 border-zinc-600"
          />
          <span className="text-sm text-zinc-300">Enabled</span>
        </div>
      </div>

      {/* Default Entry */}
      <div className="p-3 border-b border-zinc-700">
        <label className="block text-xs text-zinc-500 font-semibold uppercase mb-1.5">Default Entry</label>
        <select
          value={defaultEntry}
          onChange={(e) => setDefaultEntry(e.target.value)}
          className="w-full bg-zinc-700 text-zinc-200 border border-zinc-600 rounded px-2 py-1.5 text-xs focus:outline-none focus:border-blue-500"
        >
          <option value="">— None —</option>
          {allGuiIds.map((id) => (
            <option key={id} value={id}>
              {id}
            </option>
          ))}
        </select>
      </div>

      {/* GUI Bindings */}
      <div className="p-3 border-b border-zinc-700">
        <h4 className="text-xs text-zinc-500 font-semibold uppercase mb-2">GUI Type Bindings</h4>
        <div className="space-y-2">
          {GUI_TYPE_OPTIONS.map(({ value, label }) => (
            <div key={value} className="flex items-center gap-2">
              <span className="text-xs text-zinc-400 w-36 truncate" title={label}>
                {label}
              </span>
              <select
                value={bindings[value] || ""}
                onChange={(e) => {
                  if (e.target.value) {
                    setBinding(value, e.target.value);
                  } else {
                    removeBinding(value);
                  }
                }}
                className="flex-1 bg-zinc-700 text-zinc-200 border border-zinc-600 rounded px-2 py-1 text-xs focus:outline-none focus:border-blue-500"
              >
                <option value="">— None —</option>
                {allGuiIds.map((id) => (
                  <option key={id} value={id}>
                    {id}
                  </option>
                ))}
              </select>
            </div>
          ))}
        </div>
      </div>

      {/* Overlays */}
      <div className="p-3">
        <div className="flex items-center justify-between mb-2">
          <h4 className="text-xs text-zinc-500 font-semibold uppercase">Overlay Decorations</h4>
          <button
            onClick={() => setShowOverlayForm(!showOverlayForm)}
            className="p-0.5 text-zinc-500 hover:text-white transition-colors"
          >
            <Plus size={14} />
          </button>
        </div>

        {showOverlayForm && (
          <div className="mb-3 p-2 bg-zinc-800 rounded space-y-2">
            <div>
              <label className="block text-xs text-zinc-500 mb-0.5">GUI Type</label>
              <select
                value={newOverlayGui}
                onChange={(e) => setNewOverlayGui(e.target.value as GuiTypeId)}
                className="w-full bg-zinc-700 text-zinc-200 border border-zinc-600 rounded px-2 py-1 text-xs focus:outline-none focus:border-blue-500"
              >
                {GUI_TYPE_OPTIONS.map(({ value, label }) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs text-zinc-500 mb-0.5">Char Name</label>
              <select
                value={newOverlayChar}
                onChange={(e) => setNewOverlayChar(e.target.value)}
                className="w-full bg-zinc-700 text-zinc-200 border border-zinc-600 rounded px-2 py-1 text-xs focus:outline-none focus:border-blue-500"
              >
                <option value="">— Select —</option>
                {charEntries.map((c) => (
                  <option key={c.name} value={c.name}>{c.name}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs text-zinc-500 mb-0.5">X Offset</label>
              <input
                type="number"
                value={newOverlayX}
                onChange={(e) => setNewOverlayX(Number(e.target.value))}
                className="w-full bg-zinc-700 text-zinc-200 border border-zinc-600 rounded px-2 py-1 text-xs focus:outline-none focus:border-blue-500"
              />
            </div>
            <button
              onClick={() => {
                if (!newOverlayChar) return;
                addOverlay({
                  guiType: newOverlayGui,
                  charName: newOverlayChar,
                  x: newOverlayX,
                });
                setNewOverlayChar("");
                setNewOverlayX(0);
                setShowOverlayForm(false);
              }}
              className="w-full px-2 py-1 text-xs bg-blue-600 text-white rounded hover:bg-blue-700 transition-colors"
            >
              Add Overlay
            </button>
          </div>
        )}

        <div className="space-y-1.5">
          {overlays.map((o, i) => (
            <div key={i} className="flex items-center gap-1 p-1.5 bg-zinc-800 rounded text-xs">
              <span className="text-zinc-300 flex-1 truncate" title={`${o.guiType} → ${o.charName} (x=${o.x})`}>
                {o.guiType} → {o.charName} x={o.x}
              </span>
              <button
                onClick={() => removeOverlay(i)}
                className="p-0.5 text-zinc-500 hover:text-red-400"
              >
                <X size={12} />
              </button>
            </div>
          ))}
          {overlays.length === 0 && (
            <div className="text-xs text-zinc-600 py-2 text-center">No overlays</div>
          )}
        </div>
      </div>
    </div>
  );
}
