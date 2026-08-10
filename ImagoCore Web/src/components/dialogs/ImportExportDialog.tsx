import { useState, useRef } from "react";
import { useEditorStore } from "@/store/useEditorStore";
import { parseBuildZip } from "@/lib/import-helpers";
import { generateExportZip } from "@/lib/export-helpers";
import { Upload, Download, Loader2, Edit3, Layout } from "lucide-react";

export function ImportExportBar() {
  const store = useEditorStore();
  const activeView = useEditorStore((s) => s.activeView);
  const setActiveView = useEditorStore((s) => s.setActiveView);
  const [importing, setImporting] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [importMessage, setImportMessage] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleImport = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setImporting(true);
    setImportMessage(null);

    try {
      const data = await parseBuildZip(file);
      store.initGuiFolders();

      // TODO: For more accurate import, we'd need to also parse ImagoCore config
      // from the full directory structure. For now, basic import from build.zip.
      for (const guiEntry of data.guiEntries) {
        const character = store.nextGuiChar();
        const entry = {
          id: `${guiEntry.slot}:${guiEntry.name}`,
          name: guiEntry.name || guiEntry.character || "imported",
          slot: guiEntry.slot as 9 | 18 | 27 | 36 | 45 | 54,
          ascent: guiEntry.ascent,
          height: guiEntry.height,
          shiftX: guiEntry.shiftX || 0,
          character,
          textureData: null as ImageData | null,
          textureSrc: guiEntry.textureSrc,
          useDefaults: false,
        };
        store.addGuiEntry(entry);

        // Load texture
        const img = new Image();
        await new Promise<void>((resolve, reject) => {
          img.onload = () => {
            store.updateGuiEntryTexture(
              entry.slot,
              entry.name,
              img
            );
            resolve();
          };
          img.onerror = reject;
          img.src = guiEntry.textureSrc;
        });
      }

      for (const charEntry of data.charEntries) {
        const character = store.nextCharImageChar();
        const entry = {
          id: charEntry.name || charEntry.character || "imported",
          name: charEntry.name || charEntry.character || "imported",
          ascent: charEntry.ascent,
          height: charEntry.height,
          character,
          textureData: null as ImageData | null,
          textureSrc: charEntry.textureSrc,
        };
        store.addCharEntry(entry);

        const img = new Image();
        await new Promise<void>((resolve, reject) => {
          img.onload = () => {
            store.updateCharEntryTexture(entry.name, img);
            resolve();
          };
          img.onerror = reject;
          img.src = charEntry.textureSrc;
        });
      }

      setImportMessage(`Imported ${data.guiEntries.length} GUI + ${data.charEntries.length} Char entries`);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Import failed";
      setImportMessage(`Error: ${msg}`);
    } finally {
      setImporting(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const handleExport = async () => {
    setExporting(true);
    try {
      const blob = await generateExportZip(store);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "ImagoCore-package.zip";
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="h-10 bg-zinc-800 border-b border-zinc-700 flex items-center px-3 gap-2 select-none">
      <span className="text-sm font-semibold text-zinc-200 mr-3">
        ImagoCore Editor
      </span>

      {/* View toggle — always visible so users can switch from Layout back to Pixel Editor */}
      <div className="flex items-center bg-zinc-800 rounded border border-zinc-700 overflow-hidden">
        <button
          onClick={() => setActiveView("pixel-editor")}
          className={`flex items-center gap-1 px-2.5 py-1 text-xs transition-colors ${
            activeView === "pixel-editor"
              ? "bg-blue-600 text-white"
              : "text-zinc-400 hover:text-zinc-200 hover:bg-zinc-700"
          }`}
          title="Pixel Editor"
        >
          <Edit3 size={12} /> Pixel Edit
        </button>
        <button
          onClick={() => setActiveView("layout-editor")}
          className={`flex items-center gap-1 px-2.5 py-1 text-xs transition-colors ${
            activeView === "layout-editor"
              ? "bg-blue-600 text-white"
              : "text-zinc-400 hover:text-zinc-200 hover:bg-zinc-700"
          }`}
          title="Layouts"
        >
          <Layout size={12} /> Layouts
        </button>
      </div>

      <div className="flex-1" />

      {/* Import */}
      <label className="flex items-center gap-1 px-3 py-1 text-xs bg-zinc-700 text-zinc-300 rounded hover:bg-zinc-600 cursor-pointer transition-colors">
        {importing ? (
          <Loader2 size={14} className="animate-spin" />
        ) : (
          <Upload size={14} />
        )}
        {importing ? "Importing..." : "Import build.zip"}
        <input
          ref={fileInputRef}
          type="file"
          accept=".zip"
          onChange={handleImport}
          className="hidden"
        />
      </label>

      {/* Export */}
      <button
        onClick={handleExport}
        disabled={exporting}
        className="flex items-center gap-1 px-3 py-1 text-xs bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 transition-colors"
      >
        {exporting ? (
          <Loader2 size={14} className="animate-spin" />
        ) : (
          <Download size={14} />
        )}
        {exporting ? "Exporting..." : "Export All"}
      </button>

      {/* Message */}
      {importMessage && (
        <div className="text-xs text-zinc-400 ml-2">{importMessage}</div>
      )}
    </div>
  );
}
