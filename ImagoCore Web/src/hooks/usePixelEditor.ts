import { useRef, useCallback, useEffect } from "react";
import { useEditorStore } from "@/store/useEditorStore";
import {
  TRANSPARENT_PATTERN_SIZE,
  TRANSPARENT_COLOR_LIGHT,
  TRANSPARENT_COLOR_DARK,
} from "@/lib/constants";

interface UsePixelEditorOptions {
  canvasRef: React.RefObject<HTMLCanvasElement | null>;
  imageData: ImageData | null;
  width: number;
  height: number;
}

export function usePixelEditor({ canvasRef, imageData, width, height }: UsePixelEditorOptions) {
  // Subscribe only to slow-changing fields individually — NOT the whole pixelEditor object
  const tool = useEditorStore((s) => s.pixelEditor.tool);
  const color = useEditorStore((s) => s.pixelEditor.color);
  const showGrid = useEditorStore((s) => s.pixelEditor.showGrid);
  const zoom = useEditorStore((s) => s.pixelEditor.zoom);
  const updatePixelEditor = useEditorStore((s) => s.updatePixelEditor);

  // Pan offset stored in refs during active panning — NOT in zustat on every move.
  // This is the key performance fix: no React re-render per mouse-move pixel.
  const isDrawing = useRef(false);
  const isSpaceDown = useRef(false);
  const lastX = useRef(-1);
  const lastY = useRef(-1);
  const offscreenCanvas = useRef<HTMLCanvasElement | null>(null);
  const imageDataCopy = useRef<ImageData | null>(null);
  const isPanning = useRef(false);
  const panStartX = useRef(0);
  const panStartY = useRef(0);
  const panOffsetStartX = useRef(0);
  const panOffsetStartY = useRef(0);
  const renderRef = useRef<() => void>(() => {});

  // Initialize offscreen canvas with image data
  useEffect(() => {
    if (!imageData || width <= 0 || height <= 0) return;
    try {
      const canvas = document.createElement("canvas");
      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext("2d");
      if (!ctx) return;
      ctx.putImageData(imageData, 0, 0);
      offscreenCanvas.current = canvas;
      imageDataCopy.current = imageData;
      // Force re-draw when imageData changes but width/height stay the same.
      // The render effect (line 147) only fires on [render, zoom, showGrid]
      // changes — and render's useCallback doesn't depend on imageData.
      requestAnimationFrame(() => renderRef.current());
    } catch (e) {
      console.warn("[ImagoCore] putImageData failed:", e);
    }
  }, [imageData, width, height]);

  // Track Space key state for panning (Space + left click = drag canvas)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.code === "Space" && !e.repeat) {
        isSpaceDown.current = true;
        e.preventDefault();
      }
    };
    const handleKeyUp = (e: KeyboardEvent) => {
      if (e.code === "Space") {
        isSpaceDown.current = false;
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("keyup", handleKeyUp);
    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("keyup", handleKeyUp);
    };
  }, []);

  const drawCheckerboard = useCallback(
    (ctx: CanvasRenderingContext2D, dw: number, dh: number, ox: number, oy: number, z: number) => {
      const pattern = document.createElement("canvas");
      pattern.width = TRANSPARENT_PATTERN_SIZE * 2;
      pattern.height = TRANSPARENT_PATTERN_SIZE * 2;
      const pctx = pattern.getContext("2d")!;

      pctx.fillStyle = TRANSPARENT_COLOR_LIGHT;
      pctx.fillRect(0, 0, pattern.width, pattern.height);
      pctx.fillStyle = TRANSPARENT_COLOR_DARK;
      pctx.fillRect(0, 0, TRANSPARENT_PATTERN_SIZE, TRANSPARENT_PATTERN_SIZE);
      pctx.fillRect(
        TRANSPARENT_PATTERN_SIZE,
        TRANSPARENT_PATTERN_SIZE,
        TRANSPARENT_PATTERN_SIZE,
        TRANSPARENT_PATTERN_SIZE
      );

      const fill = ctx.createPattern(pattern, "repeat");
      if (fill) {
        ctx.fillStyle = fill;
        ctx.fillRect(ox * z, oy * z, dw, dh);
      }
    },
    []
  );

  const render = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas || width <= 0 || height <= 0) return;

    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    // Always read latest state directly from store — avoids stale closures
    const { zoom: z, showGrid: g, offsetX: ox, offsetY: oy } =
      useEditorStore.getState().pixelEditor;

    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    const dw = rect.width;
    const dh = rect.height;

    canvas.width = dw * dpr;
    canvas.height = dh * dpr;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

    ctx.clearRect(0, 0, dw, dh);

    // Checkerboard background
    drawCheckerboard(ctx, dw, dh, ox, oy, z);

    // Image layer (pixel-perfect, no smoothing)
    if (offscreenCanvas.current) {
      ctx.imageSmoothingEnabled = false;
      ctx.drawImage(
        offscreenCanvas.current,
        0, 0, width, height,
        ox * z, oy * z, width * z, height * z
      );
    }

    // Pixel grid overlay
    if (g && z >= 4) {
      const px = ox * z;
      const py = oy * z;
      const pixelSize = z;
      const startCol = Math.max(0, Math.floor(-ox));
      const endCol = Math.min(width, Math.ceil(((-ox * z) + dw) / z));
      const startRow = Math.max(0, Math.floor(-oy));
      const endRow = Math.min(height, Math.ceil(((-oy * z) + dh) / z));

      ctx.strokeStyle = "rgba(255,255,255,0.12)";
      ctx.lineWidth = 0.5;
      ctx.beginPath();
      for (let x = startCol; x <= endCol; x++) {
        ctx.moveTo(px + x * pixelSize + 0.5, py);
        ctx.lineTo(px + x * pixelSize + 0.5, py + height * pixelSize);
      }
      for (let y = startRow; y <= endRow; y++) {
        ctx.moveTo(px, py + y * pixelSize + 0.5);
        ctx.lineTo(px + width * pixelSize, py + y * pixelSize + 0.5);
      }
      ctx.stroke();
    }
  }, [canvasRef, width, height, drawCheckerboard]);

  // Keep renderRef current for imperative calls during pan/paint
  renderRef.current = render;

  // Schedule render on zoom / showGrid / image changes
  useEffect(() => {
    const id = requestAnimationFrame(render);
    return () => cancelAnimationFrame(id);
  }, [render, zoom, showGrid]);

  const screenToPixel = useCallback(
    (clientX: number, clientY: number): { x: number; y: number } => {
      const canvas = canvasRef.current;
      if (!canvas) return { x: -1, y: -1 };
      const state = useEditorStore.getState().pixelEditor;
      const rect = canvas.getBoundingClientRect();
      return {
        x: Math.floor(
          (clientX - rect.left - state.offsetX * state.zoom) / state.zoom
        ),
        y: Math.floor(
          (clientY - rect.top - state.offsetY * state.zoom) / state.zoom
        ),
      };
    },
    [canvasRef]
  );

  const getHexColor = useCallback((r: number, g: number, b: number, a: number) => {
    if (a === 0) return "#00000000";
    return (
      "#" +
      [r, g, b]
        .map((x) => x.toString(16).padStart(2, "0"))
        .join("") +
      (a < 255 ? a.toString(16).padStart(2, "0") : "")
    );
  }, []);

  const setPixel = useCallback(
    (x: number, y: number) => {
      if (!offscreenCanvas.current) return;
      if (x < 0 || x >= width || y < 0 || y >= height) return;

      const ctx = offscreenCanvas.current.getContext("2d");
      if (!ctx) return;

      if (tool === "pencil") {
        ctx.fillStyle = color;
        ctx.fillRect(x, y, 1, 1);
      } else if (tool === "eraser") {
        ctx.clearRect(x, y, 1, 1);
      } else if (tool === "eyedropper") {
        const pixel = ctx.getImageData(x, y, 1, 1).data;
        const hex = getHexColor(pixel[0], pixel[1], pixel[2], pixel[3]);
        updatePixelEditor({ color: hex === "#00000000" ? "#FFFFFF" : hex, tool: "pencil" });
        return;
      } else if (tool === "fill") {
        const targetData = ctx.getImageData(x, y, 1, 1).data;
        const [tr, tg, tb, ta] = targetData;
        if (color === `rgba(${tr},${tg},${tb},${ta})`) return;
        floodFill(ctx, x, y, tr, tg, tb, ta, color, width, height);
        return;
      }

      render();
    },
    [tool, color, width, height, getHexColor, updatePixelEditor, render]
  );

  const handleMouseDown = useCallback(
    (e: React.MouseEvent<HTMLCanvasElement>) => {
      // Middle mouse or space+left for panning
      if (e.button === 1 || (e.button === 0 && isSpaceDown.current)) {
        isPanning.current = true;
        panStartX.current = e.clientX;
        panStartY.current = e.clientY;
        const state = useEditorStore.getState().pixelEditor;
        panOffsetStartX.current = state.offsetX;
        panOffsetStartY.current = state.offsetY;
        e.preventDefault();
        return;
      }

      if (e.button !== 0) return;
      if (tool === "eyedropper") {
        const p = screenToPixel(e.clientX, e.clientY);
        setPixel(p.x, p.y);
        return;
      }

      isDrawing.current = true;
      const p = screenToPixel(e.clientX, e.clientY);
      setPixel(p.x, p.y);
      lastX.current = p.x;
      lastY.current = p.y;
    },
    [screenToPixel, setPixel, tool]
  );

  const handleMouseMove = useCallback(
    (e: React.MouseEvent<HTMLCanvasElement>) => {
      if (isPanning.current) {
        const dx = (e.clientX - panStartX.current) / zoom;
        const dy = (e.clientY - panStartY.current) / zoom;
        // Update zustand directly WITHOUT triggering React re-render (getState + setState)
        const store = useEditorStore.getState();
        store.updatePixelEditor({
          offsetX: panOffsetStartX.current + dx,
          offsetY: panOffsetStartY.current + dy,
        });
        // Redraw canvas imperatively — no React re-render
        renderRef.current();
        return;
      }

      if (!isDrawing.current || tool === "eyedropper") return;
      const p = screenToPixel(e.clientX, e.clientY);
      if (p.x === lastX.current && p.y === lastY.current) return;

      // Bresenham line for smooth drawing
      const points = bresenhamLine(lastX.current, lastY.current, p.x, p.y);
      for (const point of points) {
        setPixel(point.x, point.y);
      }
      lastX.current = p.x;
      lastY.current = p.y;
    },
    [screenToPixel, setPixel, zoom, tool]
  );

  const handleMouseUp = useCallback(() => {
    isDrawing.current = false;
    isPanning.current = false;
    lastX.current = -1;
    lastY.current = -1;
  }, []);

  const handleWheel = useCallback(
    (e: React.WheelEvent<HTMLCanvasElement>) => {
      const state = useEditorStore.getState().pixelEditor;
      const newZoom = Math.max(1, Math.min(32, state.zoom - Math.sign(e.deltaY)));
      updatePixelEditor({ zoom: newZoom });
    },
    [updatePixelEditor]
  );

  // Get current drawing buffer as ImageData
  const getImageData = useCallback((): ImageData | null => {
    if (!offscreenCanvas.current) return null;
    const ctx = offscreenCanvas.current.getContext("2d");
    if (!ctx) return null;
    return ctx.getImageData(0, 0, width, height);
  }, [width, height]);

  return {
    handleMouseDown,
    handleMouseMove,
    handleMouseUp,
    handleWheel,
    getImageData,
    offscreenCanvas,
  };
}

// Bresenham's line algorithm
function bresenhamLine(
  x0: number,
  y0: number,
  x1: number,
  y1: number
): { x: number; y: number }[] {
  const points: { x: number; y: number }[] = [];
  const dx = Math.abs(x1 - x0);
  const dy = Math.abs(y1 - y0);
  const sx = x0 < x1 ? 1 : -1;
  const sy = y0 < y1 ? 1 : -1;
  let err = dx - dy;

  while (true) {
    points.push({ x: x0, y: y0 });
    if (x0 === x1 && y0 === y1) break;
    const e2 = err * 2;
    if (e2 > -dy) {
      err -= dy;
      x0 += sx;
    }
    if (e2 < dx) {
      err += dx;
      y0 += sy;
    }
  }
  return points;
}

// Flood fill algorithm
function floodFill(
  ctx: CanvasRenderingContext2D,
  startX: number,
  startY: number,
  tr: number,
  tg: number,
  tb: number,
  ta: number,
  fillColor: string,
  w: number,
  h: number
) {
  const imageData = ctx.getImageData(0, 0, w, h);
  const data = imageData.data;
  const visited = new Uint8Array(w * h);

  // Parse fill color
  fillColor = fillColor.replace("#", "");
  let fr = 0, fg = 0, fb = 0, fa = 0;
  if (fillColor.length === 6) {
    fr = parseInt(fillColor.substring(0, 2), 16);
    fg = parseInt(fillColor.substring(2, 4), 16);
    fb = parseInt(fillColor.substring(4, 6), 16);
    fa = 255;
  } else if (fillColor.length === 8) {
    fr = parseInt(fillColor.substring(0, 2), 16);
    fg = parseInt(fillColor.substring(2, 4), 16);
    fb = parseInt(fillColor.substring(4, 6), 16);
    fa = parseInt(fillColor.substring(6, 8), 16);
  }

  const stack: number[] = [startY * w + startX];
  visited[startY * w + startX] = 1;

  while (stack.length > 0) {
    const idx = stack.pop()!;
    const px = idx % w;
    const py = Math.floor(idx / w);

    const i = idx * 4;
    data[i] = fr;
    data[i + 1] = fg;
    data[i + 2] = fb;
    data[i + 3] = fa;

    // Check 4 neighbors
    for (const [nx, ny] of [
      [px - 1, py],
      [px + 1, py],
      [px, py - 1],
      [px, py + 1],
    ]) {
      if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
      const nidx = ny * w + nx;
      if (visited[nidx]) continue;
      const ni = nidx * 4;
      if (
        data[ni] === tr &&
        data[ni + 1] === tg &&
        data[ni + 2] === tb &&
        data[ni + 3] === ta
      ) {
        visited[nidx] = 1;
        stack.push(nidx);
      }
    }
  }

  ctx.putImageData(imageData, 0, 0);
}
