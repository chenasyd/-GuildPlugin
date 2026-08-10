import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import {
  type SlotSize,
  type GuiFolder,
  type GuiEntry,
  type CharEntry,
  type OverlayConfig,
  type FunctionItem,
  type SelectedEntry,
  type PixelEditorState,
  type TransparentItem,
  type GuiDefaults,
  GUI_CHAR_START,
  CHAR_IMAGE_START,
} from "@/types/imago";
import { SLOT_SIZES } from "@/lib/constants";
import {
  indexedDBStorage,
  loadImageDataFromSrc,
} from "@/lib/persistence";

function imageDataToDataURL(imageData: ImageData): string {
  const canvas = document.createElement("canvas");
  canvas.width = imageData.width;
  canvas.height = imageData.height;
  const ctx = canvas.getContext("2d");
  if (!ctx) {
    throw new Error("Failed to create canvas context for image export");
  }
  ctx.putImageData(imageData, 0, 0);
  return canvas.toDataURL("image/png");
}

interface EditorStore {
  // --- GUI Images ---
  guiFolders: GuiFolder[];
  // --- Char Images ---
  charEntries: CharEntry[];
  // --- Bindings (imago-gui.yml) ---
  enabled: boolean;
  defaultEntry: string;
  bindings: Record<string, string>;
  overlays: OverlayConfig[];
  // --- Layouts (gui-image-layout.yml) ---
  transparentItem: TransparentItem;
  layouts: Record<string, FunctionItem[]>;
  // --- UI State ---
  selectedEntry: SelectedEntry;
  activeView: "pixel-editor" | "layout-editor";
  pixelEditor: PixelEditorState;

  // --- Persistence ---
  _hasHydrated: boolean;
  setHasHydrated: (v: boolean) => void;

  // --- Undo / Redo ---
  history: Record<string, {
    past: string[];
    present: string | null;
    future: string[];
  }>;
  pushEntryHistory: (entryKey: string, textureSrc: string) => void;
  undoHistory: (entryKey: string) => string | null;
  redoHistory: (entryKey: string) => string | null;
  canUndo: (entryKey: string) => boolean;
  canRedo: (entryKey: string) => boolean;

  // --- Actions: GUI ---
  initGuiFolders: () => void;
  setGuiDefaults: (slot: SlotSize, defaults: GuiDefaults) => void;
  addGuiEntry: (entry: GuiEntry) => void;
  removeGuiEntry: (folderSlot: SlotSize, entryName: string) => void;
  updateGuiEntry: (folderSlot: SlotSize, entryName: string, updates: Partial<GuiEntry>) => void;
  updateGuiEntryTexture: (folderSlot: SlotSize, entryName: string, img: HTMLImageElement) => void;
  updateGuiEntryTextureFromImageData: (
    folderSlot: SlotSize,
    entryName: string,
    imageData: ImageData
  ) => void;

  // --- Actions: Char ---
  addCharEntry: (entry: CharEntry) => void;
  removeCharEntry: (entryName: string) => void;
  updateCharEntry: (entryName: string, updates: Partial<CharEntry>) => void;
  updateCharEntryTexture: (entryName: string, img: HTMLImageElement) => void;
  updateCharEntryTextureFromImageData: (
    entryName: string,
    imageData: ImageData
  ) => void;

  // --- Actions: Bindings ---
  setEnabled: (v: boolean) => void;
  setDefaultEntry: (v: string) => void;
  setBinding: (guiType: string, entryId: string) => void;
  removeBinding: (guiType: string) => void;
  addOverlay: (overlay: OverlayConfig) => void;
  removeOverlay: (index: number) => void;
  updateOverlay: (index: number, updates: Partial<OverlayConfig>) => void;

  // --- Actions: Layouts ---
  setTransparentItem: (item: TransparentItem) => void;
  addFunctionItem: (guiType: string, item: FunctionItem) => void;
  removeFunctionItem: (guiType: string, index: number) => void;
  updateFunctionItem: (guiType: string, index: number, updates: Partial<FunctionItem>) => void;

  // --- Actions: UI ---
  selectEntry: (entry: SelectedEntry) => void;
  setActiveView: (view: "pixel-editor" | "layout-editor") => void;
  updatePixelEditor: (updates: Partial<PixelEditorState>) => void;

  // --- Actions: Persistence ---
  regenerateAllTextureData: () => Promise<void>;
  restoreCharCounters: () => void;

  // --- Computed Helpers ---
  nextGuiChar: () => string;
  nextCharImageChar: () => string;
  allGuiEntries: () => GuiEntry[];
}

let guiCharCounter = 0;
let charImageCounter = 0;

export const useEditorStore = create<EditorStore>()(
  persist(
    (set, get) => ({
      guiFolders: [],
      charEntries: [],
      enabled: true,
      defaultEntry: "",
      bindings: {},
      overlays: [],
      transparentItem: { material: "barrier", customModelData: 0 },
      layouts: {},
      selectedEntry: null,
      activeView: "pixel-editor",
      pixelEditor: {
        zoom: 2,
        offsetX: 0,
        offsetY: 0,
        tool: "pencil",
        color: "#FFFFFF",
        brushSize: 1,
        showGrid: true,
      },

      _hasHydrated: false,
      setHasHydrated: (v: boolean) => set({ _hasHydrated: v }),

      initGuiFolders: () => {
        set({
          guiFolders: SLOT_SIZES.map((s) => ({
            slot: s,
            defaults: { ascent: 7, height: 8 },
            entries: [],
          })),
        });
      },

      setGuiDefaults: (slot, defaults) => {
        set((s) => ({
          guiFolders: s.guiFolders.map((f) => (f.slot === slot ? { ...f, defaults } : f)),
        }));
      },

      addGuiEntry: (entry) => {
        set((s) => ({
          guiFolders: s.guiFolders.map((f) =>
            f.slot === entry.slot ? { ...f, entries: [...f.entries, entry] } : f
          ),
        }));
      },

      removeGuiEntry: (folderSlot, entryName) => {
        set((s) => ({
          guiFolders: s.guiFolders.map((f) =>
            f.slot === folderSlot
              ? { ...f, entries: f.entries.filter((e) => e.name !== entryName) }
              : f
          ),
        }));
      },

      addCharEntry: (entry) => {
        set((s) => ({ charEntries: [...s.charEntries, entry] }));
      },

      removeCharEntry: (entryName) => {
        set((s) => ({ charEntries: s.charEntries.filter((e) => e.name !== entryName) }));
      },

      updateGuiEntry: (folderSlot, entryName, updates) => {
        set((s) => ({
          guiFolders: s.guiFolders.map((f) =>
            f.slot === folderSlot
              ? {
                  ...f,
                  entries: f.entries.map((e) =>
                    e.name === entryName ? { ...e, ...updates } : e
                  ),
                }
              : f
          ),
        }));
      },

      updateGuiEntryTexture: (folderSlot, entryName, img) => {
        const canvas = document.createElement("canvas");
        canvas.width = img.naturalWidth;
        canvas.height = img.naturalHeight;
        const ctx = canvas.getContext("2d")!;
        ctx.drawImage(img, 0, 0);
        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);

        set((s) => ({
          guiFolders: s.guiFolders.map((f) =>
            f.slot === folderSlot
              ? {
                  ...f,
                  entries: f.entries.map((e) =>
                    e.name === entryName
                      ? { ...e, textureData: imageData, textureSrc: canvas.toDataURL() }
                      : e
                  ),
                }
              : f
          ),
        }));
      },

      updateGuiEntryTextureFromImageData: (folderSlot, entryName, imageData) => {
        const textureSrc = imageDataToDataURL(imageData);
        set((s) => ({
          guiFolders: s.guiFolders.map((f) =>
            f.slot === folderSlot
              ? {
                  ...f,
                  entries: f.entries.map((e) =>
                    e.name === entryName
                      ? { ...e, textureData: imageData, textureSrc }
                      : e
                  ),
                }
              : f
          ),
        }));
      },

      updateCharEntry: (entryName, updates) => {
        set((s) => ({
          charEntries: s.charEntries.map((e) =>
            e.name === entryName ? { ...e, ...updates } : e
          ),
        }));
      },

      updateCharEntryTexture: (entryName, img) => {
        const canvas = document.createElement("canvas");
        canvas.width = img.naturalWidth;
        canvas.height = img.naturalHeight;
        const ctx = canvas.getContext("2d")!;
        ctx.drawImage(img, 0, 0);
        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);

        set((s) => ({
          charEntries: s.charEntries.map((e) =>
            e.name === entryName
              ? { ...e, textureData: imageData, textureSrc: canvas.toDataURL() }
              : e
          ),
        }));
      },

      updateCharEntryTextureFromImageData: (entryName, imageData) => {
        const textureSrc = imageDataToDataURL(imageData);
        set((s) => ({
          charEntries: s.charEntries.map((e) =>
            e.name === entryName
              ? { ...e, textureData: imageData, textureSrc }
              : e
          ),
        }));
      },

      setEnabled: (v) => set({ enabled: v }),
      setDefaultEntry: (v) => set({ defaultEntry: v }),

  history: {},
  pushEntryHistory: (entryKey, textureSrc) => {
    set((s) => {
      const existing = s.history[entryKey];
      const current = existing?.present;
      const past = current ? [...(existing?.past ?? []), current].slice(-20) : existing?.past ?? [];
      return {
        history: {
          ...s.history,
          [entryKey]: {
            past,
            present: textureSrc,
            future: [],
          },
        },
      };
    });
  },
  undoHistory: (entryKey) => {
    let result: string | null = null;
    set((s) => {
      const existing = s.history[entryKey];
      if (!existing || existing.past.length === 0) return s;
      const previous = existing.past[existing.past.length - 1];
      const newPast = existing.past.slice(0, -1);
      const future = existing.present ? [existing.present, ...existing.future] : existing.future;
      result = previous;
      return {
        history: {
          ...s.history,
          [entryKey]: {
            past: newPast,
            present: previous,
            future,
          },
        },
      };
    });
    return result;
  },
  redoHistory: (entryKey) => {
    let result: string | null = null;
    set((s) => {
      const existing = s.history[entryKey];
      if (!existing || existing.future.length === 0) return s;
      const next = existing.future[0];
      const future = existing.future.slice(1);
      const past = existing.present ? [...existing.past, existing.present].slice(-20) : existing.past;
      result = next;
      return {
        history: {
          ...s.history,
          [entryKey]: {
            past,
            present: next,
            future,
          },
        },
      };
    });
    return result;
  },
  canUndo: (entryKey) => {
    const entry = get().history[entryKey];
    return Boolean(entry && entry.past.length > 0);
  },
  canRedo: (entryKey) => {
    const entry = get().history[entryKey];
    return Boolean(entry && entry.future.length > 0);
  },

  setBinding: (guiType, entryId) => {
    set((s) => ({ bindings: { ...s.bindings, [guiType]: entryId } }));
  },

  removeBinding: (guiType) => {
    set((s) => {
      const bs = { ...s.bindings };
      delete bs[guiType];
      return { bindings: bs };
    });
  },

  addOverlay: (overlay) => {
    set((s) => ({ overlays: [...s.overlays, overlay] }));
  },
  removeOverlay: (index) => {
    set((s) => ({ overlays: s.overlays.filter((_, i) => i !== index) }));
  },
  updateOverlay: (index, updates) => {
    set((s) => ({
      overlays: s.overlays.map((o, i) => (i === index ? { ...o, ...updates } : o)),
    }));
  },

  setTransparentItem: (item) => set({ transparentItem: item }),

  addFunctionItem: (guiType, item) => {
    set((s) => ({
      layouts: {
        ...s.layouts,
        [guiType]: [...(s.layouts[guiType] || []), item],
      },
    }));
  },

  removeFunctionItem: (guiType, index) => {
    set((s) => ({
      layouts: {
        ...s.layouts,
        [guiType]: (s.layouts[guiType] || []).filter((_, i) => i !== index),
      },
    }));
  },

  updateFunctionItem: (guiType, index, updates) => {
    set((s) => {
      const list = [...(s.layouts[guiType] || [])];
      list[index] = { ...list[index], ...updates };
      return { layouts: { ...s.layouts, [guiType]: list } };
    });
  },

  selectEntry: (entry) => set({ selectedEntry: entry }),
  setActiveView: (view) => set({ activeView: view }),
  updatePixelEditor: (updates) => {
    set((s) => ({ pixelEditor: { ...s.pixelEditor, ...updates } }));
  },

  nextGuiChar: () => {
    guiCharCounter++;
    return String.fromCodePoint(GUI_CHAR_START + guiCharCounter);
  },
  nextCharImageChar: () => {
    charImageCounter++;
    return String.fromCodePoint(CHAR_IMAGE_START + charImageCounter);
  },

  allGuiEntries: () => {
    const state = get();
    return state.guiFolders.flatMap((f) => f.entries);
  },

  // ---- Persistence: regenerate ImageData from stored Base64 ----
  regenerateAllTextureData: async () => {
    const state = get();

    // Restore GUI entry textureData
    let restoredCount = 0;
    const newFolders = await Promise.all(
      state.guiFolders.map(async (f) => ({
        ...f,
        entries: await Promise.all(
          f.entries.map(async (e) => {
            if (!e.textureData && e.textureSrc) {
              try {
                const td = await loadImageDataFromSrc(e.textureSrc);
                restoredCount++;
                return { ...e, textureData: td };
              } catch {
                return e;
              }
            }
            return e;
          })
        ),
      }))
    );

    // Restore Char entry textureData
    const newChars = await Promise.all(
      state.charEntries.map(async (e) => {
        if (!e.textureData && e.textureSrc) {
          try {
            const td = await loadImageDataFromSrc(e.textureSrc);
            restoredCount++;
            return { ...e, textureData: td };
          } catch {
            return e;
          }
        }
        return e;
      })
    );

    if (restoredCount > 0) {
      console.log(`[ImagoCore] Regenerated textureData for ${restoredCount} entries`);
    }
    set({ guiFolders: newFolders, charEntries: newChars });
  },

  // ---- Persistence: restore Unicode character counters ----
  restoreCharCounters: () => {
    const state = get();

    // Scan all GUI entries for the highest character code
    for (const f of state.guiFolders) {
      for (const e of f.entries) {
        if (e.character) {
          const cp = e.character.codePointAt(0);
          if (cp && cp >= GUI_CHAR_START) {
            guiCharCounter = Math.max(guiCharCounter, cp - GUI_CHAR_START);
          }
        }
      }
    }

    // Scan all Char entries for the highest character code
    for (const e of state.charEntries) {
      if (e.character) {
        const cp = e.character.codePointAt(0);
        if (cp && cp >= CHAR_IMAGE_START) {
          charImageCounter = Math.max(charImageCounter, cp - CHAR_IMAGE_START);
        }
      }
    }
  },
}),
    {
      name: "imago-core-editor",
      storage: createJSONStorage(() => indexedDBStorage, {
        replacer: (key, value) => (key === "textureData" ? undefined : value),
      }),
      // Exclude functions (actions) and runtime-only fields from persistence
      partialize: (state) => {
        const {
          _hasHydrated,
          setHasHydrated,
          initGuiFolders,
          setGuiDefaults,
          addGuiEntry,
          removeGuiEntry,
          updateGuiEntry,
          updateGuiEntryTexture,
          addCharEntry,
          removeCharEntry,
          updateCharEntry,
          updateCharEntryTexture,
          setEnabled,
          setDefaultEntry,
          setBinding,
          removeBinding,
          addOverlay,
          removeOverlay,
          updateOverlay,
          setTransparentItem,
          addFunctionItem,
          removeFunctionItem,
          updateFunctionItem,
          selectEntry,
          setActiveView,
          updatePixelEditor,
          regenerateAllTextureData,
          restoreCharCounters,
          nextGuiChar,
          nextCharImageChar,
          allGuiEntries,
          ...persistedState
        } = state as EditorStore & Record<string, unknown>;
        return persistedState;
      },
      onRehydrateStorage: () => {
        return (state, error) => {
          if (error) {
            console.warn("[ImagoCore] Rehydration error, starting fresh:", error);
          }
          if (state) {
            state.setHasHydrated(true);
          }
        };
      },
    }
  )
);
