import { useEditorStore } from "@/store/useEditorStore";
import { COLOR_PRESETS } from "@/lib/constants";
import type { EditorTool } from "@/types/imago";
import {
  Pencil,
  Eraser,
  Pipette,
  Grid3X3,
  PaintBucket,
  ZoomIn,
  ZoomOut,
} from "lucide-react";

const TOOLS: { id: EditorTool; icon: React.ReactNode; label: string }[] = [
  { id: "pencil", icon: <Pencil size={16} />, label: "Pencil" },
  { id: "eraser", icon: <Eraser size={16} />, label: "Eraser" },
  { id: "eyedropper", icon: <Pipette size={16} />, label: "Color Picker" },
  { id: "fill", icon: <PaintBucket size={16} />, label: "Fill" },
];

export function PixelToolbar() {
  // Subscribe to individual primitives — avoids re-render on offsetX/Y changes during pan
  const currentTool = useEditorStore((s) => s.pixelEditor.tool);
  const currentColor = useEditorStore((s) => s.pixelEditor.color);
  const currentShowGrid = useEditorStore((s) => s.pixelEditor.showGrid);
  const currentZoom = useEditorStore((s) => s.pixelEditor.zoom);
  const updatePixelEditor = useEditorStore((s) => s.updatePixelEditor);
  const selected = useEditorStore((s) => s.selectedEntry);

  if (!selected) return null;

  return (
    <div className="h-12 bg-zinc-850 border-b border-zinc-700 flex items-center px-3 gap-2 select-none">
      {/* Tools */}
      <div className="flex items-center gap-0.5 bg-zinc-800 rounded-md p-0.5 mr-3">
        {TOOLS.map((tool) => (
          <button
            key={tool.id}
            title={tool.label}
            onClick={() => updatePixelEditor({ tool: tool.id })}
            className={`p-1.5 rounded transition-colors ${
              currentTool === tool.id
                ? "bg-zinc-600 text-white"
                : "text-zinc-400 hover:text-white"
            }`}
          >
            {tool.icon}
          </button>
        ))}
      </div>

      <div className="w-px h-6 bg-zinc-700" />

      {/* Color */}
      <div className="flex items-center gap-1.5 ml-3">
        {COLOR_PRESETS.map((presetColor) => (
          <button
            key={presetColor}
            onClick={() => updatePixelEditor({ color: presetColor })}
            className={`w-5 h-5 rounded border-2 transition-transform hover:scale-110 ${
              currentColor === presetColor
                ? "border-white scale-110"
                : "border-zinc-600"
            }`}
            style={{
              background:
                presetColor === "#000000"
                  ? "repeating-conic-gradient(#cccccc 0% 25%, #999999 0% 50%) 50% / 8px 8px"
                  : presetColor,
            }}
            title={presetColor}
          />
        ))}
        {/* Custom color input */}
        <input
          type="color"
          value={currentColor.startsWith("#") && currentColor.length === 7 ? currentColor : "#ffffff"}
          onChange={(e) => updatePixelEditor({ color: e.target.value })}
          className="w-5 h-5 rounded cursor-pointer border-2 border-zinc-600 bg-transparent p-0"
          title="Custom color"
        />
      </div>

      <div className="flex-1" />

      {/* Grid toggle */}
      <button
        title="Toggle grid"
        onClick={() => updatePixelEditor({ showGrid: !currentShowGrid })}
        className={`p-1.5 rounded transition-colors ${
          currentShowGrid
            ? "bg-zinc-600 text-white"
            : "text-zinc-400 hover:text-white"
        }`}
      >
        <Grid3X3 size={16} />
      </button>

      {/* Zoom controls */}
      <button
        title="Zoom out"
        onClick={() =>
          updatePixelEditor({ zoom: Math.max(1, currentZoom - 1) })
        }
        className="p-1.5 rounded text-zinc-400 hover:text-white transition-colors"
      >
        <ZoomOut size={16} />
      </button>
      <span className="text-xs text-zinc-400 w-10 text-center">
        {Math.round(currentZoom * 100)}%
      </span>
      <button
        title="Zoom in"
        onClick={() =>
          updatePixelEditor({ zoom: Math.min(32, currentZoom + 1) })
        }
        className="p-1.5 rounded text-zinc-400 hover:text-white transition-colors"
      >
        <ZoomIn size={16} />
      </button>
    </div>
  );
}
