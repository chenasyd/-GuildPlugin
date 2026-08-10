import { useEditorStore } from "@/store/useEditorStore";
import { ImportExportBar } from "@/components/dialogs/ImportExportDialog";
import { ResourcePanel } from "@/components/panels/ResourcePanel";
import { PixelCanvas } from "@/components/editor/PixelCanvas";
import { PixelToolbar } from "@/components/editor/PixelToolbar";
import { PropertyPanel } from "@/components/panels/PropertyPanel";
import { ConfigPanel } from "@/components/panels/ConfigPanel";
import { LayoutEditor } from "@/components/panels/LayoutEditor";
import { useEffect, useState } from "react";

function App() {
  const [ready, setReady] = useState(false);
  const _hasHydrated = useEditorStore((s) => s._hasHydrated);
  const activeView = useEditorStore((s) => s.activeView);

  useEffect(() => {
    if (!_hasHydrated) return;

    const bootstrap = async () => {
      // Read from getState() instead of closure — avoids race where React
      // fires this effect with stale guiFolders before the rehydrated
      // guiFolders array reaches the subscribed component.
      const s = useEditorStore.getState();
      if (s.guiFolders.length === 0) {
        s.initGuiFolders();
      } else {
        s.restoreCharCounters();
        await s.regenerateAllTextureData();
      }
      setReady(true);
    };

    bootstrap();
  }, [_hasHydrated]);

  return (
    <div className="h-screen flex flex-col bg-zinc-950 text-zinc-200">
      {/* Top bar */}
      <ImportExportBar />

      {/* Main content */}
      {!ready ? (
        <div className="flex-1 flex items-center justify-center">
          <div className="flex flex-col items-center gap-3 text-zinc-500">
            <div className="w-8 h-8 border-2 border-zinc-700 border-t-zinc-400 rounded-full animate-spin" />
            <span className="text-sm">Loading editor state...</span>
          </div>
        </div>
      ) : activeView === "layout-editor" ? (
        <div className="flex-1 flex overflow-hidden">
          <LayoutEditor />
          <ConfigPanel />
        </div>
      ) : (
        <div className="flex-1 flex overflow-hidden">
          {/* Left panel: Resource tree */}
          <ResourcePanel />

          {/* Center: Editor area */}
          <div className="flex-1 flex flex-col min-w-0">
            <PixelToolbar />
            <PixelCanvas />
          </div>

          {/* Right panel: Properties + Config */}
          <div className="flex">
            <PropertyPanel />
            <ConfigPanel />
          </div>
        </div>
      )}
    </div>
  );
}

export default App;
