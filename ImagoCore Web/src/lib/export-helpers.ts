/**
 * Export helpers - generate ImagoCore + GuildPlugin configs as download.
 */
import JSZip from "jszip";
import { dump } from "js-yaml";
import type {
  GuiFolder,
  CharEntry,
  ImagoGuiYml,
  ImagoCharYml,
  ImagoCoreConfig,
  FunctionItem,
  GuiDefaults,
  OverlayConfig,
} from "@/types/imago";
import { SHIFT_CHARS } from "@/types/imago";

type EditorStore = {
  guiFolders: GuiFolder[];
  charEntries: CharEntry[];
  enabled: boolean;
  defaultEntry: string;
  bindings: Record<string, string>;
  overlays: OverlayConfig[];
  transparentItem: { material: string; customModelData: number };
  layouts: Record<string, FunctionItem[]>;
};

function padHex(code: number): string {
  return code.toString(16).padStart(4, "0");
}

export function buildImagoGuiYml(folders: GuiFolder[]): Record<string, ImagoGuiYml> {
  const result: Record<string, ImagoGuiYml> = {};

  for (const folder of folders) {
    const gui: Record<string, Record<string, number>> = {};
    const defaults: GuiDefaults = { ...folder.defaults };

    for (const entry of folder.entries) {
      const def: Record<string, number> = {};
      if (!entry.useDefaults) {
        if (entry.ascent !== folder.defaults.ascent) def.ascent = entry.ascent;
        if (entry.height !== folder.defaults.height) def.height = entry.height;
      }
      if (entry.shiftX !== 0) def.shift_x = entry.shiftX;
      gui[entry.name] = def;
    }

    result[String(folder.slot)] = { defaults, gui };
  }
  return result;
}

export function buildImagoCharYml(chars: CharEntry[]): ImagoCharYml {
  const char: Record<string, Record<string, number>> = {};
  let firstAscent = 7;
  let firstHeight = 8;

  for (const c of chars) {
    if (chars.indexOf(c) === 0) {
      firstAscent = c.ascent;
      firstHeight = c.height;
    }
    const def: Record<string, number> = {};
    if (c.ascent !== firstAscent) def.ascent = c.ascent;
    if (c.height !== firstHeight) def.height = c.height;
    char[c.name] = def;
  }

  return { defaults: { ascent: firstAscent, height: firstHeight }, char };
}

function buildFontJson(
  folders: GuiFolder[],
  chars: CharEntry[]
): Record<string, unknown> {
  const providers: Record<string, unknown>[] = [];

  // Shift space provider
  const shiftChars: string[] = [];
  Object.values(SHIFT_CHARS).forEach((ch) => shiftChars.push(ch));

  providers.push({
    type: "bitmap",
    file: "minecraft:font/shift.png",
    ascent: -8192,
    height: -8258,
    chars: shiftChars,
  });

  // GUI entries
  for (const folder of folders) {
    const ascent = folder.defaults.ascent;
    const height = folder.defaults.height;

    for (const entry of folder.entries) {
      if (!entry.textureSrc) continue;
      const actualAscent = entry.useDefaults ? ascent : entry.ascent;
      const actualHeight = entry.useDefaults ? height : entry.height;
      const fileName = `minecraft:font/gui_${padHex(entry.character.codePointAt(0)!)}.png`;
      providers.push({
        type: "bitmap",
        file: fileName,
        ascent: actualAscent,
        height: actualHeight,
        chars: [entry.character],
      });
    }
  }

  // Char entries
  for (const c of chars) {
    if (!c.textureSrc) continue;
    const fileName = `minecraft:font/char_${padHex(c.character.codePointAt(0)!)}.png`;
    providers.push({
      type: "bitmap",
      file: fileName,
      ascent: c.ascent,
      height: c.height,
      chars: [c.character],
    });
    // Also add the ascent variant char
    const ascentChar = String.fromCodePoint(c.character.codePointAt(0)! + 0x100);
    providers.push({
      type: "bitmap",
      file: fileName,
      ascent: -c.ascent,
      height: c.height,
      chars: [ascentChar],
    });
  }

  return { providers };
}

export async function generateExportZip(store: EditorStore): Promise<Blob> {
  const zip = new JSZip();

  // --- ImagoCore/ folder ---
  const imagoZip = zip.folder("ImagoCore")!;
  const guiFolder = imagoZip.folder("gui")!;
  const charFolder = imagoZip.folder("char")!;

  // config.yml
  const imagoCoreConfig: ImagoCoreConfig = {
    verbose_logging: false,
    render_interval: 5,
    resource_pack_output: "build.zip",
  };
  imagoZip.file("config.yml", dump(imagoCoreConfig, { lineWidth: -1 }));

  // Per-slot gui.yml
  const guiYmls = buildImagoGuiYml(store.guiFolders);
  for (const [slot, yml] of Object.entries(guiYmls)) {
    const slotFolder = guiFolder.folder(slot)!;
    slotFolder.file("gui.yml", dump(yml, { lineWidth: -1, noRefs: true }));
  }

  // Root gui/gui.yml
  const rootGuiYml = buildRootGuiYml(store.guiFolders);
  guiFolder.file("gui.yml", dump(rootGuiYml, { lineWidth: -1 }));

  // char.yml
  const charYml = buildImagoCharYml(store.charEntries);
  charFolder.file("char.yml", dump(charYml, { lineWidth: -1, noRefs: true }));

  // Copy PNG images into correct folders
  for (const folder of store.guiFolders) {
    for (const entry of folder.entries) {
      if (entry.textureSrc) {
        const blob = dataURLtoBlob(entry.textureSrc);
        const slotFolder = guiFolder.folder(String(folder.slot))!;
        slotFolder.file(`${entry.name}.png`, blob);
      }
    }
  }

  for (const c of store.charEntries) {
    if (c.textureSrc) {
      const blob = dataURLtoBlob(c.textureSrc);
      charFolder.file(`${c.name}.png`, blob);
    }
  }

  // --- GuildPlugin files ---
  const guildFolder = zip.folder("GuildPlugin")!;

  const imagoGuiYml: Record<string, unknown> = {
    enabled: store.enabled,
  };

  if (store.defaultEntry) {
    imagoGuiYml.defaults = store.defaultEntry;
  }

  const bindingsSection: Record<string, string> = {};
  for (const [guiType, entryId] of Object.entries(store.bindings) as [string, string][]) {
    bindingsSection[guiType] = entryId;
  }
  imagoGuiYml.bindings = bindingsSection;

  if (store.overlays.length > 0) {
    imagoGuiYml.overlays = store.overlays.map((o: OverlayConfig) => {
      const item: Record<string, unknown> = {
        gui_type: o.guiType,
        char: o.charName,
        x: o.x,
      };
      if (o.ascent !== undefined) item.ascent = o.ascent;
      return item;
    });
  }

  guildFolder.file("imago-gui.yml", dump(imagoGuiYml, { lineWidth: -1 }));

  const layoutYml: Record<string, unknown> = {
    transparent_item: store.transparentItem,
  };

  for (const [guiType, functions] of Object.entries(store.layouts) as [string, FunctionItem[]][]) {
    if (functions.length === 0) continue;
    layoutYml[guiType] = functions.map((f: FunctionItem) => ({
      function_id: f.functionId,
      material: f.material,
      custom_model_data: f.customModelData,
      slots: f.slots,
    }));
  }

  guildFolder.file("gui-image-layout.yml", dump(layoutYml, { lineWidth: -1 }));

  // --- Font JSON ---
  const fontJson = buildFontJson(store.guiFolders, store.charEntries);
  const assetsFolder = zip.folder("assets")!;
  const fontFolder = assetsFolder.folder("minecraft")!.folder("font")!;
  fontFolder.file("default.json", JSON.stringify(fontJson, null, 2));

  // Copy font PNGs
  for (const folder of store.guiFolders) {
    for (const entry of folder.entries) {
      if (entry.textureSrc) {
        const blob = dataURLtoBlob(entry.textureSrc);
        const hexName = `gui_${padHex(entry.character.codePointAt(0)!)}.png`;
        fontFolder.file(hexName, blob);
      }
    }
  }

  for (const c of store.charEntries) {
    if (c.textureSrc) {
      const blob = dataURLtoBlob(c.textureSrc);
      const hexName = `char_${padHex(c.character.codePointAt(0)!)}.png`;
      fontFolder.file(hexName, blob);
    }
  }

  return await zip.generateAsync({ type: "blob" });
}

function buildRootGuiYml(folders: GuiFolder[]): Record<string, unknown> {
  const result: Record<string, unknown> = {
    defaults: { ascent: 7, height: 8 },
    gui: {},
  };

  const gui: Record<string, string> = {};
  for (const folder of folders) {
    for (const entry of folder.entries) {
      // Key format: {slot}/{entryName}
      gui[`${folder.slot}/${entry.name}`] = `\\\\u${padHex(entry.character.codePointAt(0)!)}`;
    }
  }
  result.gui = gui;
  return result;
}

function dataURLtoBlob(dataURL: string): Blob {
  const [header, data] = dataURL.split(",");
  const mime = header.match(/:(.*?);/)?.[1] || "image/png";
  const binary = atob(data);
  const array = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    array[i] = binary.charCodeAt(i);
  }
  return new Blob([array], { type: mime });
}
