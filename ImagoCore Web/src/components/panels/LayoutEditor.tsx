import { useState, useMemo } from "react";
import { useEditorStore } from "@/store/useEditorStore";
import { GUI_TYPE_OPTIONS, SLOT_LAYOUTS } from "@/lib/constants";
import type { FunctionItem } from "@/types/imago";
import { Plus, Trash2, Edit3, X, Check } from "lucide-react";

const FUNCTION_COLORS = [
  "bg-blue-500/50 border-blue-400",
  "bg-green-500/50 border-green-400",
  "bg-purple-500/50 border-purple-400",
  "bg-orange-500/50 border-orange-400",
  "bg-pink-500/50 border-pink-400",
  "bg-cyan-500/50 border-cyan-400",
  "bg-yellow-500/50 border-yellow-400",
  "bg-red-500/50 border-red-400",
  "bg-teal-500/50 border-teal-400",
  "bg-indigo-500/50 border-indigo-400",
];

export function LayoutEditor() {
  const layouts = useEditorStore((s) => s.layouts);
  const transparentItem = useEditorStore((s) => s.transparentItem);
  const addFunctionItem = useEditorStore((s) => s.addFunctionItem);
  const removeFunctionItem = useEditorStore((s) => s.removeFunctionItem);
  const updateFunctionItem = useEditorStore((s) => s.updateFunctionItem);
  const setTransparentItem = useEditorStore((s) => s.setTransparentItem);

  const [selectedGui, setSelectedGui] = useState<string>(
    GUI_TYPE_OPTIONS[0].value
  );
  const [editingFunction, setEditingFunction] = useState<{
    index: number;
    fn: FunctionItem;
  } | null>(null);
  const [addingNew, setAddingNew] = useState(false);
  const [newFnId, setNewFnId] = useState("");
  const [newMaterial, setNewMaterial] = useState("paper");
  const [newCMD, setNewCMD] = useState(0);

  const { cols, rows } = useMemo(() => {
    const layout = SLOT_LAYOUTS[54]; // Always use 6x9 for visualization
    return { cols: layout?.cols || 9, rows: layout?.rows || 6 };
  }, []);

  const currentLayout = layouts[selectedGui] || [];
  const slots = Array.from({ length: cols * rows }, (_, i) => i);

  // Build slot → function mapping
  const slotFnMap = useMemo(() => {
    const map: Record<number, { fn: FunctionItem; idx: number; color: string }> = {};
    currentLayout.forEach((fn, idx) => {
      fn.slots.forEach((slot) => {
        map[slot] = {
          fn,
          idx,
          color: FUNCTION_COLORS[idx % FUNCTION_COLORS.length],
        };
      });
    });
    return map;
  }, [currentLayout]);

  const toggleSlotForFn = (slot: number, fnIdx: number) => {
    const fn = currentLayout[fnIdx];
    if (!fn) return;
    const newSlots = fn.slots.includes(slot)
      ? fn.slots.filter((s) => s !== slot)
      : [...fn.slots, slot].sort((a, b) => a - b);
    updateFunctionItem(selectedGui, fnIdx, { slots: newSlots });
  };

  const handleAddFn = () => {
    if (!newFnId.trim()) return;
    addFunctionItem(selectedGui, {
      functionId: newFnId.trim(),
      material: newMaterial.trim() || "paper",
      customModelData: newCMD,
      slots: [],
    });
    setNewFnId("");
    setNewMaterial("paper");
    setNewCMD(0);
    setAddingNew(false);
  };

  return (
    <div className="flex-1 flex flex-col bg-zinc-900 overflow-hidden">
      {/* Top bar */}
      <div className="p-3 border-b border-zinc-700 flex items-center gap-3">
        <span className="text-sm font-medium text-zinc-300">Layout Editor</span>

        {/* GUI Selector */}
        <select
          value={selectedGui}
          onChange={(e) => setSelectedGui(e.target.value)}
          className="bg-zinc-700 text-zinc-200 border border-zinc-600 rounded px-2 py-1 text-xs focus:outline-none focus:border-blue-500"
        >
          {GUI_TYPE_OPTIONS.map(({ value, label }) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </div>

      {/* Transparent Item Config */}
      <div className="px-3 py-2 border-b border-zinc-700">
        <div className="flex items-center gap-3 text-xs">
          <span className="text-zinc-400">Transparent Item:</span>
          <input
            type="text"
            value={transparentItem.material}
            onChange={(e) =>
              setTransparentItem({ ...transparentItem, material: e.target.value })
            }
            className="w-24 bg-zinc-700 text-zinc-200 border border-zinc-600 rounded px-2 py-1 text-xs focus:outline-none focus:border-blue-500"
            placeholder="material"
          />
          <span className="text-zinc-500">CMD:</span>
          <input
            type="number"
            value={transparentItem.customModelData}
            onChange={(e) =>
              setTransparentItem({
                ...transparentItem,
                customModelData: Number(e.target.value),
              })
            }
            className="w-20 bg-zinc-700 text-zinc-200 border border-zinc-600 rounded px-2 py-1 text-xs focus:outline-none focus:border-blue-500"
          />
        </div>
      </div>

      <div className="flex-1 flex overflow-hidden">
        {/* Slot Grid */}
        <div className="flex-1 flex items-center justify-center p-4">
          <div
            className="grid gap-1 p-4 bg-zinc-850 rounded-lg border border-zinc-700"
            style={{
              gridTemplateColumns: `repeat(${cols}, 32px)`,
              gridTemplateRows: `repeat(${rows}, 32px)`,
            }}
          >
            {slots.map((slot) => {
              const mapping = slotFnMap[slot];
              return (
                <button
                  key={slot}
                  onClick={() => {
                    // Show slot assignment
                    if (mapping) {
                      toggleSlotForFn(slot, mapping.idx);
                    }
                  }}
                  title={`Slot ${slot}${mapping ? ` → ${mapping.fn.functionId}` : " (empty)"}`}
                  className={`w-8 h-8 rounded border text-[10px] font-mono flex items-center justify-center transition-colors ${
                    mapping
                      ? mapping.color + " text-white"
                      : "border-zinc-600 bg-zinc-800/50 text-zinc-600 hover:border-zinc-500"
                  }`}
                >
                  {slot}
                </button>
              );
            })}
          </div>
        </div>

        {/* Function List */}
        <div className="w-64 border-l border-zinc-700 p-3 overflow-y-auto">
          <div className="flex items-center justify-between mb-2">
            <h4 className="text-xs text-zinc-500 font-semibold uppercase">
              Functions
            </h4>
            <button
              onClick={() => setAddingNew(!addingNew)}
              className="p-0.5 text-zinc-500 hover:text-white transition-colors"
            >
              <Plus size={14} />
            </button>
          </div>

          {/* Add new function form */}
          {addingNew && (
            <div className="mb-3 p-2 bg-zinc-800 rounded space-y-1.5">
              <input
                type="text"
                value={newFnId}
                onChange={(e) => setNewFnId(e.target.value)}
                placeholder="function_id"
                className="w-full bg-zinc-700 text-zinc-200 border border-zinc-600 rounded px-2 py-1 text-xs focus:outline-none focus:border-blue-500"
              />
              <input
                type="text"
                value={newMaterial}
                onChange={(e) => setNewMaterial(e.target.value)}
                placeholder="material"
                className="w-full bg-zinc-700 text-zinc-200 border border-zinc-600 rounded px-2 py-1 text-xs focus:outline-none focus:border-blue-500"
              />
              <input
                type="number"
                value={newCMD}
                onChange={(e) => setNewCMD(Number(e.target.value))}
                placeholder="custom_model_data"
                className="w-full bg-zinc-700 text-zinc-200 border border-zinc-600 rounded px-2 py-1 text-xs focus:outline-none focus:border-blue-500"
              />
              <div className="flex gap-1">
                <button
                  onClick={handleAddFn}
                  className="flex-1 px-2 py-1 text-xs bg-blue-600 text-white rounded hover:bg-blue-700"
                >
                  Add
                </button>
                <button
                  onClick={() => setAddingNew(false)}
                  className="px-2 py-1 text-xs text-zinc-400 hover:text-white"
                >
                  Cancel
                </button>
              </div>
            </div>
          )}

          {/* Function list */}
          <div className="space-y-1.5">
            {currentLayout.map((fn, idx) => (
              <div
                key={fn.functionId}
                className={`p-2 rounded border text-xs ${FUNCTION_COLORS[idx % FUNCTION_COLORS.length]}`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-medium text-white">{fn.functionId}</span>
                  <div className="flex gap-0.5">
                    <button
                      onClick={() => removeFunctionItem(selectedGui, idx)}
                      className="p-0.5 text-zinc-300 hover:text-red-300"
                    >
                      <Trash2 size={11} />
                    </button>
                  </div>
                </div>
                <div className="text-zinc-300/70 mt-0.5">
                  {fn.material}:{fn.customModelData}
                </div>
                <div className="text-zinc-300/50 mt-0.5 text-[10px]">
                  Slots: {fn.slots.length > 0 ? fn.slots.join(", ") : "none"}
                </div>
              </div>
            ))}
            {currentLayout.length === 0 && (
              <div className="text-xs text-zinc-600 py-3 text-center">
                No functions defined
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
