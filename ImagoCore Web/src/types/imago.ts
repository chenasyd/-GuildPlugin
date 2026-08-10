// ============================================================
// ImagoCore GUI Entry Types
// ============================================================

export type SlotSize = 9 | 18 | 27 | 36 | 45 | 54;

export interface GuiDefaults {
  ascent: number;
  height: number;
}

export interface GuiEntry {
  id: string;
  name: string;
  slot: SlotSize;
  ascent: number;
  height: number;
  shiftX: number;
  character: string;
  textureData: ImageData | null;
  textureSrc: string | null;
  useDefaults: boolean;
}

export interface GuiFolder {
  slot: SlotSize;
  defaults: GuiDefaults;
  entries: GuiEntry[];
}

// ============================================================
// Character Entry Types
// ============================================================

export interface CharEntry {
  id: string;
  name: string;
  ascent: number;
  height: number;
  character: string;
  textureData: ImageData | null;
  textureSrc: string | null;
}

// ============================================================
// imago-gui.yml Config Types
// ============================================================

export type GuiTypeId =
  | "false"
  | "GUILD"
  | "MAINGUILDGUI"
  | "auction-list-gui"
  | "guild-bank-gui"
  | "guild-battle-hall-gui"
  | "guild-contribution-gui"
  | "guild-manage-gui"
  | "guild-screen-gui"
  | "guild-security-gui"
  | "guild-shop-gui"
  | "guild-warehouse-gui"
  | "guild-wiki-gui"
  | "member-manage-gui";

export interface OverlayConfig {
  guiType: GuiTypeId;
  charName: string;
  x: number;
  ascent?: number;
}

export interface ImagoGuiConfig {
  enabled: boolean;
  defaultEntry: string;
  bindings: Record<string, string>;
  overlays: OverlayConfig[];
}

// ============================================================
// gui-image-layout.yml Config Types
// ============================================================

export interface TransparentItem {
  material: string;
  customModelData: number;
}

export interface FunctionItem {
  functionId: string;
  material: string;
  customModelData: number;
  slots: number[];
}

export interface GuiImageLayoutConfig {
  transparent_item: TransparentItem;
  layouts: Record<string, FunctionItem[]>;
}

// ============================================================
// ImagoCore internal Config Types
// ============================================================

export interface ImagoGuiYml {
  defaults: {
    ascent: number;
    height: number;
  };
  gui: Record<string, ImagoGuiEntryDef>;
}

export interface ImagoGuiEntryDef {
  ascent?: number;
  height?: number;
  shift_x?: number;
}

export interface ImagoCharYml {
  defaults: {
    ascent: number;
    height: number;
  };
  char: Record<string, ImagoCharEntryDef>;
}

export interface ImagoCharEntryDef {
  ascent?: number;
  height?: number;
}

export interface ImagoCoreConfig {
  verbose_logging: boolean;
  render_interval: number;
  resource_pack_output: string;
}

// ============================================================
// Editor State
// ============================================================

export type SelectedEntry =
  | { type: "gui"; folderSlot: SlotSize; entryName: string }
  | { type: "char"; entryName: string }
  | null;

export type EditorTool = "pencil" | "eraser" | "eyedropper" | "fill";
export type EditorView = "pixel-editor" | "layout-editor";

export interface PixelEditorState {
  zoom: number;
  offsetX: number;
  offsetY: number;
  tool: EditorTool;
  color: string;
  brushSize: number;
  showGrid: boolean;
}

// ============================================================
// Font Character Mapping Constants
// ============================================================

export const SHIFT_CHARS: Record<number, string> = {
  "-128": "\uE801",
  "-64": "\uE802",
  "-32": "\uE803",
  "-16": "\uE804",
  "-8": "\uE805",
  "-4": "\uE806",
  "-2": "\uE807",
  "-1": "\uE808",
  1: "\uE809",
  2: "\uE80A",
  4: "\uE80B",
  8: "\uE80C",
  16: "\uE80D",
  32: "\uE80E",
  64: "\uE80F",
  128: "\uE810",
};

export const GUI_CHAR_START = 0xe820;
export const CHAR_IMAGE_START = 0xe900;
export const CHAR_ASCENT_START = 0xea00;
