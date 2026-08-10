import { useState } from "react";
import { useEditorStore } from "@/store/useEditorStore";
import type { CharEntry } from "@/types/imago";

interface Props {
  open: boolean;
  onClose: () => void;
}

export function AddCharImageDialog({ open, onClose }: Props) {
  const addCharEntry = useEditorStore((s) => s.addCharEntry);
  const updateCharEntryTexture = useEditorStore((s) => s.updateCharEntryTexture);
  const nextCharImageChar = useEditorStore((s) => s.nextCharImageChar);

  const [name, setName] = useState("");
  const [ascent, setAscent] = useState(7);
  const [height, setHeight] = useState(8);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  if (!open) return null;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setImageFile(file);
    const url = URL.createObjectURL(file);
    setPreviewUrl(url);
    if (!name && file.name) {
      setName(file.name.replace(/\.[^.]+$/, ""));
    }
  };

  const handleSubmit = () => {
    if (!name.trim()) return;
    if (!imageFile) return;

    const character = nextCharImageChar();
    const entry: CharEntry = {
      id: name.trim(),
      name: name.trim(),
      ascent,
      height,
      character,
      textureData: null,
      textureSrc: null,
    };

    const reader = new FileReader();
    reader.onload = () => {
      const img = new Image();
      img.onload = () => {
        addCharEntry(entry);
        updateCharEntryTexture(name.trim(), img);
        onClose();
      };
      img.src = reader.result as string;
    };
    reader.readAsDataURL(imageFile);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={onClose}>
      <div
        className="bg-zinc-850 border border-zinc-700 rounded-lg w-96 p-5 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <h3 className="text-base font-medium text-zinc-200 mb-4">Add Char Image</h3>

        {/* Entry Name */}
        <div className="mb-3">
          <label className="block text-xs text-zinc-400 mb-1">Char Name</label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g. guild_level_bg"
            className="w-full bg-zinc-700 text-zinc-200 border border-zinc-600 rounded px-3 py-1.5 text-sm focus:outline-none focus:border-blue-500"
          />
        </div>

        {/* Image File */}
        <div className="mb-3">
          <label className="block text-xs text-zinc-400 mb-1">Image File (PNG)</label>
          <input
            type="file"
            accept="image/png"
            onChange={handleFileChange}
            className="w-full text-sm text-zinc-400 file:mr-3 file:py-1 file:px-3 file:rounded file:border-0 file:text-sm file:bg-zinc-700 file:text-zinc-200 hover:file:bg-zinc-600"
          />
          {previewUrl && (
            <div className="mt-2 p-2 bg-zinc-800 rounded">
              <img
                src={previewUrl}
                alt="Preview"
                className="max-h-32 object-contain mx-auto"
                style={{ imageRendering: "pixelated" }}
              />
            </div>
          )}
        </div>

        {/* Ascent, Height */}
        <div className="grid grid-cols-2 gap-2 mb-3">
          <div>
            <label className="block text-xs text-zinc-400 mb-0.5">Ascent</label>
            <input
              type="number"
              value={ascent}
              onChange={(e) => setAscent(Number(e.target.value))}
              className="w-full bg-zinc-700 text-zinc-200 border border-zinc-600 rounded px-2 py-1 text-sm focus:outline-none focus:border-blue-500"
            />
          </div>
          <div>
            <label className="block text-xs text-zinc-400 mb-0.5">Height</label>
            <input
              type="number"
              value={height}
              onChange={(e) => setHeight(Number(e.target.value))}
              className="w-full bg-zinc-700 text-zinc-200 border border-zinc-600 rounded px-2 py-1 text-sm focus:outline-none focus:border-blue-500"
            />
          </div>
        </div>

        {/* Actions */}
        <div className="flex justify-end gap-2 mt-4">
          <button
            onClick={onClose}
            className="px-3 py-1.5 text-sm text-zinc-400 hover:text-white transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleSubmit}
            disabled={!name.trim() || !imageFile}
            className="px-3 py-1.5 text-sm bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            Add Entry
          </button>
        </div>
      </div>
    </div>
  );
}
