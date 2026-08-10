import type { ImagoCoreConfig } from "@/types/imago";

export const GUI_TYPE_OPTIONS = [
  { value: "GUILD", label: "Guild Main" },
  { value: "MAINGUILDGUI", label: "Main Guild GUI" },
  { value: "auction-list-gui", label: "Auction List" },
  { value: "guild-bank-gui", label: "Guild Bank" },
  { value: "guild-battle-hall-gui", label: "Guild Battle Hall" },
  { value: "guild-contribution-gui", label: "Guild Contribution" },
  { value: "guild-manage-gui", label: "Guild Manage" },
  { value: "guild-screen-gui", label: "Guild Screen" },
  { value: "guild-security-gui", label: "Guild Security" },
  { value: "guild-shop-gui", label: "Guild Shop" },
  { value: "guild-warehouse-gui", label: "Guild Warehouse" },
  { value: "guild-wiki-gui", label: "Guild Wiki" },
  { value: "member-manage-gui", label: "Member Manage" },
];

export const SLOT_SIZES = [9, 18, 27, 36, 45, 54] as const;

export const SLOT_LAYOUTS: Record<number, { cols: number; rows: number }> = {
  9: { cols: 9, rows: 1 },
  18: { cols: 9, rows: 2 },
  27: { cols: 9, rows: 3 },
  36: { cols: 9, rows: 4 },
  45: { cols: 9, rows: 5 },
  54: { cols: 9, rows: 6 },
};

export const DEFAULT_IMAGO_CORE_CONFIG: ImagoCoreConfig = {
  verbose_logging: false,
  render_interval: 5,
  resource_pack_output: "build.zip",
};

export const DEFAULT_FONT_JSON_TEMPLATE = {
  providers: [
    {
      type: "bitmap",
      file: "minecraft:font/glyph.png",
      ascent: 7,
      height: 8,
      chars: ["\uE820"],
    },
  ],
};

export const COLOR_PRESETS = [
  "#FFFFFF", "#FF5555", "#FFAA00", "#FFFF55", "#55FF55",
  "#55FFFF", "#5555FF", "#FF55FF", "#AAAAAA", "#555555",
  "#000000", // transparent
];

export const TRANSPARENT_PATTERN_SIZE = 16;
export const TRANSPARENT_COLOR_LIGHT = "#cccccc";
export const TRANSPARENT_COLOR_DARK = "#999999";
