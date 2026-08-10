/**
 * Import helpers - parse an existing build.zip resource pack.
 *
 * The ImagoCore resource pack puts bitmap textures under:
 *   assets/minecraft/textures/textures/gui/{slot}/{name}.png    (GUI backgrounds)
 *   assets/minecraft/textures/textures/char/{name}.png          (character images)
 *
 * font/default.json maps each PNG to a Unicode PUA character via bitmap providers:
 *   { "type": "bitmap", "file": "minecraft:textures/gui/54/default.png", ... }
 */
import JSZip from "jszip";

export interface ImportedData {
  guiEntries: Array<{
    slot: number;
    name: string;
    ascent: number;
    height: number;
    character?: string;
    textureSrc: string;
  }>;
  charEntries: Array<{
    name: string;
    ascent: number;
    height: number;
    character?: string;
    textureSrc: string;
  }>;
}

interface BitmapProvider {
  type: string;
  file: string;
  ascent?: number;
  height?: number;
  chars?: string[];
}

/**
 * Given a resource path from font/default.json (e.g. "textures/gui/18/default.png"),
 * try to locate the actual PNG inside the zip. Some build packs have an extra
 * "textures/" segment (e.g. assets/minecraft/textures/textures/gui/18/default.png).
 */
function findZipPng(zip: JSZip, resourcePath: string): ReturnType<typeof zip.file> {
  // Canonical: assets/minecraft/textures/gui/18/default.png
  const standardPath = `assets/minecraft/${resourcePath}`;
  const found = zip.file(standardPath);
  if (found) return found;

  // Alternate: assets/minecraft/textures/textures/gui/18/default.png
  const altPath = `assets/minecraft/textures/${resourcePath}`;
  const foundAlt = zip.file(altPath);
  if (foundAlt) return foundAlt;

  // Last resort: scan all PNGs, match by filename suffix
  const filename = resourcePath.split("/").pop();
  if (filename) {
    const allFiles = Object.keys(zip.files);
    const match = allFiles.find((p) => p.endsWith(`/${filename}`));
    if (match) return zip.file(match);
  }

  return null;
}

export async function parseBuildZip(file: File): Promise<ImportedData> {
  const zip = await JSZip.loadAsync(file);
  const result: ImportedData = { guiEntries: [], charEntries: [] };

  // 1. Parse font/default.json to get bitmap provider definitions
  const fontJsonFile = zip.file("assets/minecraft/font/default.json");
  if (!fontJsonFile) throw new Error("font/default.json not found in zip");

  const fontJsonStr = await fontJsonFile.async("string");
  const fontJson = JSON.parse(fontJsonStr);
  const providers: BitmapProvider[] = fontJson.providers || [];

  // 2. Process each BITMAP provider (skip "space" type used for shift chars)
  for (const p of providers) {
    if (p.type !== "bitmap") continue;
    if (!p.file) continue;

    // Strip "minecraft:" namespace prefix from resource identifier
    // "minecraft:textures/gui/54/mainguildgui.png" → "textures/gui/54/mainguildgui.png"
    const resourcePath = p.file.startsWith("minecraft:")
      ? p.file.slice("minecraft:".length)
      : p.file;

    const zipEntry = findZipPng(zip, resourcePath);
    if (!zipEntry) {
      console.warn(`[ImagoCore Import] PNG not found in zip for: ${resourcePath}`);
      continue;
    }

    const blob = await zipEntry.async("blob");
    const dataURL = await blobToDataURL(blob);
    const ascent = p.ascent ?? 7;
    const height = p.height ?? 8;
    const charset = p.chars ?? [];

    // 3. Classify entry by texture sub-path (strip any leading "textures/" segments)
    const classifyPath = resourcePath.replace(/^textures\/+/, "");

    if (classifyPath.startsWith("gui/")) {
      // "gui/{slot}/{name}.png"  → slot + name
      const rel = classifyPath.slice("gui/".length); // "54/default.png"
      const slashIdx = rel.indexOf("/");
      if (slashIdx < 0) continue; // malformed — no slot folder

      const slotStr = rel.slice(0, slashIdx);
      const name = rel.slice(slashIdx + 1).replace(/\.png$/i, "") || "unknown";
      const slot = parseInt(slotStr, 10) || 54;

      result.guiEntries.push({
        slot,
        name,
        ascent,
        height,
        character: charset[0],
        textureSrc: dataURL,
      });
    } else if (classifyPath.startsWith("char/")) {
      // "char/{name}.png"  → name
      const name = classifyPath
        .slice("char/".length)
        .replace(/\.png$/i, "") || "unknown";

      result.charEntries.push({
        name,
        ascent,
        height,
        character: charset[0],
        textureSrc: dataURL,
      });
    }
  }

  console.log(`[ImagoCore Import] Found ${result.guiEntries.length} GUI + ${result.charEntries.length} Char entries`);

  return result;
}

function blobToDataURL(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = reject;
    reader.readAsDataURL(blob);
  });
}
