import { useRef, useEffect } from "react";
import { useEditorStore } from "@/store/useEditorStore";
import { usePixelEditor } from "@/hooks/usePixelEditor";
import { imageDataToDataURL } from "@/lib/persistence";

interface PixelCanvasProps {
  editable?: boolean;
}

export function PixelCanvas({ editable = true }: PixelCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const selectedEntry = useEditorStore((s) => s.selectedEntry);
  const zoom = useEditorStore((s) => s.pixelEditor.zoom);
  // Subscribe to guiFolders & charEntries so the component reacts when
  // regenerateAllTextureData() adds textureData back after hydration.
  // Pixel edits modify an offscreen canvas — they do NOT update the store
  // per-pixel, so subscribing to these arrays does not cause performance issues.
  const guiFolders = useEditorStore((s) => s.guiFolders);
  const charEntries = useEditorStore((s) => s.charEntries);

  let currentImageData: ImageData | null = null;
  let currentTextureSrc: string | null = null;
  let imageWidth = 0;
  let imageHeight = 0;
  let entryKey: string | null = null;

  if (selectedEntry?.type === "gui") {
    const folder = guiFolders.find((f) => f.slot === selectedEntry.folderSlot);
    const entry = folder?.entries.find((e) => e.name === selectedEntry.entryName);
    if (entry) {
      currentImageData = entry.textureData;
      currentTextureSrc = entry.textureSrc;
      imageWidth = entry.textureData?.width ?? 0;
      imageHeight = entry.textureData?.height ?? 0;
      entryKey = `gui:${entry.slot}:${entry.name}`;
    }
  } else if (selectedEntry?.type === "char") {
    const entry = charEntries.find((e) => e.name === selectedEntry.entryName);
    if (entry) {
      currentImageData = entry.textureData;
      currentTextureSrc = entry.textureSrc;
      imageWidth = entry.textureData?.width ?? 0;
      imageHeight = entry.textureData?.height ?? 0;
      entryKey = `char:${entry.name}`;
    }
  }

  const pushEntryHistory = useEditorStore((s) => s.pushEntryHistory);
  const updateGuiEntryTextureFromImageData = useEditorStore(
    (s) => s.updateGuiEntryTextureFromImageData
  );
  const updateCharEntryTextureFromImageData = useEditorStore(
    (s) => s.updateCharEntryTextureFromImageData
  );

  useEffect(() => {
    if (!entryKey || !currentTextureSrc) return;
    const existing = useEditorStore.getState().history[entryKey];
    if (existing?.present === currentTextureSrc) return;
    pushEntryHistory(entryKey, currentTextureSrc);
  }, [entryKey, currentTextureSrc, pushEntryHistory]);

  const saveEditorData = (imageData: ImageData) => {
    if (!entryKey) return;
    const textureSrc = imageDataToDataURL(imageData);
    pushEntryHistory(entryKey, textureSrc);
    if (selectedEntry?.type === "gui") {
      const folderSlot = selectedEntry.folderSlot;
      updateGuiEntryTextureFromImageData(folderSlot, selectedEntry.entryName, imageData);
    } else if (selectedEntry?.type === "char") {
      updateCharEntryTextureFromImageData(selectedEntry.entryName, imageData);
    }
  };

  const { handleMouseDown, handleMouseMove, handleMouseUp, handleWheel } =
    usePixelEditor({
      canvasRef,
      imageData: currentImageData,
      width: imageWidth,
      height: imageHeight,
      entryKey,
      onSaveImageData: saveEditorData,
    });

  // Resize observer
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const observer = new ResizeObserver(() => {
      // Force re-render on resize
      const event = new Event("resize");
      canvas.dispatchEvent(event);
    });
    observer.observe(canvas.parentElement!);
    return () => observer.disconnect();
  }, []);

  if (!selectedEntry || !currentImageData) {
    return (
      <div className="flex-1 flex items-center justify-center bg-zinc-900 text-zinc-500 select-none">
        <div className="text-center">
          <div className="text-4xl mb-3">🖼️</div>
          <p className="text-sm">选择一个资源开始编辑</p>
          <p className="text-xs mt-1 text-zinc-600">
            从左侧面板选择 GUI 图片或 Char 图片
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 relative bg-zinc-900 overflow-hidden">
      {/* Zoom indicator */}
      <div className="absolute top-2 right-2 z-10 bg-zinc-800/90 text-zinc-300 px-2 py-1 rounded text-xs select-none border border-zinc-700">
        {Math.round(zoom * 100)}%
      </div>

      {/* Canvas */}
      <canvas
        ref={canvasRef}
        className="absolute inset-0 w-full h-full cursor-crosshair"
        onMouseDown={editable ? handleMouseDown : undefined}
        onMouseMove={editable ? handleMouseMove : undefined}
        onMouseUp={editable ? handleMouseUp : undefined}
        onMouseLeave={editable ? handleMouseUp : undefined}
        onWheel={editable ? handleWheel : undefined}
        onContextMenu={(e) => e.preventDefault()}
      />
    </div>
  );
}
