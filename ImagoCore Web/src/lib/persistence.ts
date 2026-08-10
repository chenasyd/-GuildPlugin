// ============================================================
// Data Persistence Layer — IndexedDB via idb-keyval
// ============================================================

import { get, set, del } from "idb-keyval";

// ---------- Async storage adapter for Zustand persist ----------

/** Implements Zustand's async storage interface backed by IndexedDB. */
export const indexedDBStorage = {
  getItem: async (name: string): Promise<string | null> => {
    try {
      const value = await get(name);
      return value ?? null;
    } catch {
      // IndexedDB unavailable (private browsing, etc.) — graceful fallback
      return null;
    }
  },
  setItem: async (name: string, value: string): Promise<void> => {
    try {
      await set(name, value);
    } catch {
      // Silently fail — data stays in memory
      console.warn("[ImagoCore] Failed to persist state to IndexedDB");
    }
  },
  removeItem: async (name: string): Promise<void> => {
    try {
      await del(name);
    } catch {
      // Silently fail
    }
  },
};

// ---------- Serialization helpers ----------

/**
 * Recursively strip `textureData` (ImageData) fields from the state object.
 * ImageData is non-JSON-serializable and can be regenerated from `textureSrc`.
 */
function stripImageData(obj: unknown): unknown {
  if (obj === null || obj === undefined) return obj;
  if (Array.isArray(obj)) return obj.map(stripImageData);
  if (typeof obj === "object") {
    const result: Record<string, unknown> = {};
    for (const [key, value] of Object.entries(obj as Record<string, unknown>)) {
      if (key === "textureData") continue; // skip ImageData — regenerated on load
      result[key] = stripImageData(value);
    }
    return result;
  }
  return obj;
}

/**
 * Custom serializer: strips ImageData before JSON.stringify.
 * This prevents serializing massive Uint8ClampedArray pixel buffers.
 */
export function persistSerialize(state: Record<string, unknown>): string {
  const stripped = stripImageData(state);
  return JSON.stringify(stripped);
}

/**
 * Custom deserializer: restores textureData as null on all entries.
 * textureData is regenerated later from textureSrc by regenerateAllTextureData().
 */
export function persistDeserialize(str: string): Record<string, unknown> {
  try {
    return JSON.parse(str);
  } catch {
    console.warn("[ImagoCore] Corrupted persisted data, starting fresh");
    return {};
  }
}

// ---------- Texture regeneration ----------

/**
 * Load an Image from a Base64 data URL and extract ImageData.
 * Used to restore pixel buffers after rehydration from storage.
 */
export function loadImageDataFromSrc(src: string): Promise<ImageData> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => {
      const canvas = document.createElement("canvas");
      canvas.width = img.naturalWidth;
      canvas.height = img.naturalHeight;
      const ctx = canvas.getContext("2d")!;
      ctx.drawImage(img, 0, 0);
      resolve(ctx.getImageData(0, 0, canvas.width, canvas.height));
    };
    img.onerror = () => reject(new Error("Failed to load image from src"));
    img.src = src;
  });
}
