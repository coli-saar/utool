import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import { getCurrentWindow } from "@tauri-apps/api/window";
import { open, save } from "@tauri-apps/plugin-dialog";
import { readTextFile, writeTextFile } from "@tauri-apps/plugin-fs";
import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import type { CSSProperties } from "react";
import { GraphCanvas } from "./GraphCanvas";
import type { Zoom } from "./GraphCanvas";
import type { ChartRowPage, ChartRule, ChartState, ChartView, GraphView, LoadedDocumentView, SolutionView } from "./types";

const EXAMPLE = `[label(x f(x1)) label(y g(y1)) label(z a) dom(x1 z) dom(y1 z) dom(y x1)]`;
type ViewName = "graph" | "chart" | "solutions";
type DocumentView = { title: string; documentId: number; graph: GraphView };
type ChartVariant = { key: string; name: string; chart: ChartView };
type ActionStatus = { action: string; elapsedMs: number | null; running: boolean };

function formatElapsed(elapsedMs: number): string {
  if (elapsedMs < 1) return `${(elapsedMs * 1000).toFixed(elapsedMs < 0.1 ? 1 : 0)} µs`;
  if (elapsedMs < 1000) return `${elapsedMs.toFixed(elapsedMs < 10 ? 2 : 1)} ms`;
  return `${(elapsedMs / 1000).toFixed(3)} s`;
}

function solutionGraph(solution: SolutionView): GraphView {
  const SIBLING_GAP = 18;
  const ROOT_GAP = 64;
  const LEVEL_GAP = 85;
  const children = new Map<number, number[]>();
  const incoming = new Set<number>();
  solution.edges.forEach(([from, to]) => { children.set(from, [...(children.get(from) ?? []), to]); incoming.add(to); });
  const widths = new Map(solution.nodes.map((node) => [node.id, Math.max(54, node.label.length * 8 + 28)]));
  const positions = new Map<number, { x: number; y: number }>();

  type TreePoint = { x: number; level: number };
  type Subtree = {
    width: number;
    rootCenter: number;
    leftContour: number[];
    rightContour: number[];
    positions: Map<number, TreePoint>;
  };
  const layoutTree = (id: number): Subtree => {
    const nodeWidth = widths.get(id)!;
    const descendants = children.get(id) ?? [];
    if (!descendants.length) {
      return {
        width: nodeWidth,
        rootCenter: nodeWidth / 2,
        leftContour: [0],
        rightContour: [nodeWidth],
        positions: new Map([[id, { x: 0, level: 0 }]]),
      };
    }

    const childTrees = descendants.map((child) => layoutTree(child));
    const childOffsets: number[] = [];
    const childrenLeft: number[] = [];
    const childrenRight: number[] = [];
    childTrees.forEach((tree, index) => {
      const offset = index === 0 ? 0 : Array.from(
        { length: Math.min(childrenRight.length, tree.leftContour.length) },
        (_, level) => childrenRight[level] + SIBLING_GAP - tree.leftContour[level],
      ).reduce((required, value) => Math.max(required, value), 0);
      childOffsets.push(offset);
      tree.leftContour.forEach((value, level) => {
        childrenLeft[level] = Math.min(childrenLeft[level] ?? Infinity, value + offset);
        childrenRight[level] = Math.max(childrenRight[level] ?? -Infinity, tree.rightContour[level] + offset);
      });
    });
    const childrenCenter = descendants.reduce((sum, child, index) => {
      return sum + childOffsets[index] + childTrees[index].rootCenter;
    }, 0) / descendants.length;
    const parentX = childrenCenter - nodeWidth / 2;
    const left = Math.min(parentX, ...childrenLeft);
    const right = Math.max(parentX + nodeWidth, ...childrenRight);
    const shift = -left;
    const subtreePositions = new Map<number, TreePoint>();
    childTrees.forEach((tree, index) => {
      tree.positions.forEach((point, nodeId) => {
        subtreePositions.set(nodeId, {
          x: point.x + childOffsets[index] + shift,
          level: point.level + 1,
        });
      });
    });
    subtreePositions.set(id, { x: parentX + shift, level: 0 });
    return {
      width: right - left,
      rootCenter: childrenCenter + shift,
      leftContour: [parentX + shift, ...childrenLeft.map((value) => value + shift)],
      rightContour: [parentX + nodeWidth + shift, ...childrenRight.map((value) => value + shift)],
      positions: subtreePositions,
    };
  };

  let rootCursor = 0;
  solution.nodes.filter((node) => !incoming.has(node.id)).forEach((root) => {
    const tree = layoutTree(root.id);
    tree.positions.forEach((point, id) => positions.set(id, {
      x: point.x + rootCursor,
      y: point.level * LEVEL_GAP,
    }));
    rootCursor += tree.width + ROOT_GAP;
  });
  const contentWidth = Math.max(0, rootCursor - ROOT_GAP);
  const nodes = solution.nodes.map((node) => ({ ...node, hole: false, x: positions.get(node.id)?.x ?? 0, y: positions.get(node.id)?.y ?? 0, width: widths.get(node.id)!, height: 34 }));
  return {
    nodes,
    edges: solution.edges.map(([source, target]) => ({ source, target, kind: "tree", points: [], light: false })),
    width: Math.max(220, contentWidth),
    height: Math.max(160, ...nodes.map((node) => node.y + 70)),
  };
}

const CHART_PAGE_SIZE = 96;
const CHART_CACHE_PAGES = 8;
const CHART_ROW_ESTIMATE = 58;
const CHART_OVERSCAN = 10;
const FALLBACK_LAYOUT_DELAY_MS = 200;

class RowHeightIndex {
  private readonly corrections: Float32Array;
  private readonly prefixes: Float64Array;

  constructor(size: number) {
    this.corrections = new Float32Array(size);
    this.prefixes = new Float64Array(size + 1);
  }

  set(index: number, height: number): boolean {
    const correction = height - CHART_ROW_ESTIMATE;
    const delta = correction - this.corrections[index];
    if (Math.abs(delta) < 1) return false;
    this.corrections[index] = correction;
    for (let cursor = index + 1; cursor < this.prefixes.length; cursor += cursor & -cursor) {
      this.prefixes[cursor] += delta;
    }
    return true;
  }

  top(index: number): number {
    let correction = 0;
    for (let cursor = index; cursor > 0; cursor -= cursor & -cursor) {
      correction += this.prefixes[cursor];
    }
    return index * CHART_ROW_ESTIMATE + correction;
  }
}

function ChartRuleRow({ row, state, index, top, onHeight }: { row: ChartRule; state: ChartState; index: number; top: number; onHeight: (index: number, height: number) => void }) {
  const element = useRef<HTMLDivElement | null>(null);
  useEffect(() => {
    if (!element.current) return;
    const observer = new ResizeObserver(([entry]) => onHeight(index, entry.borderBoxSize[0]?.blockSize ?? entry.contentRect.height));
    observer.observe(element.current);
    return () => observer.disconnect();
  }, [index, onHeight]);
  return <div ref={element} className={`chart-row${row.ordinal === 1 ? " chart-row-first" : ""}`} style={{ top }}>
    {row.ordinal === 1 && <div className="chart-state-label">
      <span>[{state.subgraph.join(", ")}]</span>
      {state.variant !== null && <small>variant {state.variant}</small>}
    </div>}
    <div className="chart-rule-grid" aria-label={`Rule ${row.ordinal} of ${state.ruleCount}`}>
      <span className="chart-rule-number">{row.ordinal}</span>
      <span className="chart-fragment" aria-label="Top fragment">{row.fragment}</span>
      <span className="chart-assignments" aria-label="Hole assignments">
        {row.assignments.map(([hole, subgraph]) => <span key={hole}><b>{hole}</b><span aria-hidden="true"> → </span><i>[{subgraph.join(", ")}]</i></span>)}
      </span>
    </div>
  </div>;
}

function ChartRules({ chart }: { chart: ChartView }) {
  const viewport = useRef<HTMLDivElement | null>(null);
  const fragmentRulers = useRef<HTMLDivElement | null>(null);
  const alive = useRef(true);
  const heights = useMemo(() => new RowHeightIndex(chart.displayRowCount), [chart.chartId, chart.displayRowCount]);
  const pending = useRef(new Set<number>());
  const [rows, setRows] = useState(new Map<number, ChartRule>());
  const [states, setStates] = useState(new Map<number, ChartState>());
  const [scrollTop, setScrollTop] = useState(0);
  const [viewportHeight, setViewportHeight] = useState(400);
  const [fragmentColumnWidth, setFragmentColumnWidth] = useState(0);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [, setMeasurementVersion] = useState(0);

  useEffect(() => () => { alive.current = false; }, []);

  useEffect(() => {
    setRows(new Map()); setStates(new Map()); pending.current.clear(); setScrollTop(0); setLoadError(null); setFragmentColumnWidth(0);
    viewport.current?.scrollTo({ top: 0 });
  }, [chart.chartId]);

  useEffect(() => {
    if (!viewport.current) return;
    const observer = new ResizeObserver(([entry]) => setViewportHeight(entry.contentRect.height));
    observer.observe(viewport.current);
    return () => observer.disconnect();
  }, []);

  const rowTop = useCallback((index: number) => {
    return heights.top(index);
  }, [heights]);
  const rowAt = useCallback((offset: number) => {
    let low = 0; let high = chart.displayRowCount;
    while (low < high) { const middle = Math.floor((low + high) / 2); if (rowTop(middle + 1) <= offset) low = middle + 1; else high = middle; }
    return low;
  }, [chart.displayRowCount, rowTop]);
  const first = Math.max(0, rowAt(scrollTop) - CHART_OVERSCAN);
  const last = Math.min(chart.displayRowCount, rowAt(scrollTop + viewportHeight) + CHART_OVERSCAN + 1);

  useEffect(() => {
    const firstPage = Math.floor(first / CHART_PAGE_SIZE) * CHART_PAGE_SIZE;
    for (let start = firstPage; start < last; start += CHART_PAGE_SIZE) {
      if (rows.has(start) || pending.current.has(start)) continue;
      pending.current.add(start);
      void invoke<ChartRowPage>("chart_rows", { chartId: chart.chartId, start, count: CHART_PAGE_SIZE })
        .then((page) => {
          if (!alive.current) return;
          setLoadError(null);
          setStates((current) => {
            const next = new Map(current);
            page.states.forEach((definition) => next.set(definition.state, definition));
            return next;
          });
          setRows((current) => {
            const next = new Map(current);
            page.rows.forEach((row, offset) => next.set(page.start + offset, row));
            const centerPage = Math.floor(((first + last) / 2) / CHART_PAGE_SIZE);
            const firstCachedPage = Math.max(0, centerPage - Math.floor(CHART_CACHE_PAGES / 2));
            const lastCachedPage = firstCachedPage + CHART_CACHE_PAGES;
            next.forEach((_row, index) => {
              const pageIndex = Math.floor(index / CHART_PAGE_SIZE);
              if (pageIndex < firstCachedPage || pageIndex >= lastCachedPage) next.delete(index);
            });
            return next;
          });
        })
        .catch((reason) => { if (alive.current) setLoadError(String(reason)); })
        .finally(() => pending.current.delete(start));
    }
  }, [chart.chartId, first, last, rows]);

  useEffect(() => {
    const retained = new Set(Array.from(rows.values(), (row) => row.state));
    setStates((current) => new Map(Array.from(current).filter(([state]) => retained.has(state))));
  }, [rows]);

  useLayoutEffect(() => {
    const rulers = fragmentRulers.current;
    if (!rulers) return;
    const measured = Array.from(rulers.children)
      .map((element) => {
        const style = getComputedStyle(element);
        return element.getBoundingClientRect().width + Number.parseFloat(style.paddingLeft || "0");
      })
      .reduce((widest, width) => Math.max(widest, width), 0);
    if (measured > 0) setFragmentColumnWidth(Math.ceil(measured));
  }, [chart.topFragments]);

  const recordHeight = useCallback((index: number, height: number) => {
    if (!heights.set(index, height)) return;
    setMeasurementVersion((version) => version + 1);
  }, [heights]);

  if (chart.displayRowCount === 0) return <div className="empty-chart">The chart contains no productive split rules.</div>;
  if (loadError) return <div className="empty-chart">Could not load chart rules: {loadError}</div>;
  const rendered = [];
  for (let index = first; index < last; index++) {
    const row = rows.get(index);
    const state = row && states.get(row.state);
    rendered.push(row && state
      ? <ChartRuleRow key={`${chart.chartId}-${index}`} row={row} state={state} index={index} top={rowTop(index)} onHeight={recordHeight} />
      : <div key={`${chart.chartId}-${index}`} className="chart-row chart-row-loading" style={{ top: rowTop(index), height: CHART_ROW_ESTIMATE }}>Loading rule</div>);
  }
  const columnStyle = fragmentColumnWidth > 0
    ? { "--chart-fragment-width": `${fragmentColumnWidth}px` } as CSSProperties
    : undefined;
  return <div ref={viewport} className="chart-list" style={columnStyle} onScroll={(event) => setScrollTop(event.currentTarget.scrollTop)}>
    <div className="chart-list-space" style={{ height: rowTop(chart.displayRowCount) }}>
      <div ref={fragmentRulers} className="chart-fragment-rulers" aria-hidden="true">
        {chart.topFragments.map((fragment) => <span key={fragment} className="chart-fragment">{fragment}</span>)}
      </div>
      {rendered}
    </div>
  </div>;
}

function SolutionSpaceControl({ variants, activeKey, filterRunning, onSelect, onAdd }: {
  variants: ChartVariant[];
  activeKey: string;
  filterRunning: string | null;
  onSelect: (key: string) => void;
  onAdd: () => void;
}) {
  const choose = "__choose_filter__";
  return <label className="filter-picker" htmlFor="solution-space">
    <span>Filter</span>
    <select id="solution-space" value={activeKey} onChange={(event) => event.target.value === choose ? onAdd() : onSelect(event.target.value)} disabled={Boolean(filterRunning) || variants.length === 0}>
      {variants.length === 0 && <option value="base">Computing chart…</option>}
      {variants.map((variant) => <option key={variant.key} value={variant.key}>{variant.key === "base" ? "None" : variant.name} · {variant.chart.solutionCount} solutions</option>)}
      {variants.length > 0 && <option value={choose}>Choose filter…</option>}
    </select>
  </label>;
}

export default function App() {
  const [document, setDocument] = useState<DocumentView | null>(null);
  const [graphReady, setGraphReady] = useState(false);
  const [activeView, setActiveView] = useState<ViewName>("graph");
  const [variants, setVariants] = useState<ChartVariant[]>([]);
  const [activeVariantKey, setActiveVariantKey] = useState("base");
  const [chartRunning, setChartRunning] = useState(false);
  const [filterRunning, setFilterRunning] = useState<string | null>(null);
  const [solutionRunning, setSolutionRunning] = useState(false);
  const [solution, setSolution] = useState<SolutionView | null>(null);
  const [solutionIndex, setSolutionIndex] = useState(0);
  const [graphZoom, setGraphZoom] = useState<Zoom>(100);
  const [solutionZoom, setSolutionZoom] = useState<Zoom>(100);
  const [graphOffsets, setGraphOffsets] = useState<Record<number, { x: number; y: number }>>({});
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState<ActionStatus>({ action: "Ready", elapsedMs: null, running: false });
  const svg = useRef<SVGSVGElement | null>(null);
  const operation = useRef(0);
  const loadOperation = useRef(0);
  const solutionOperation = useRef(0);
  const activeJob = useRef<string | null>(null);
  const fallbackTimer = useRef<number | null>(null);
  const activeVariant = variants.find((variant) => variant.key === activeVariantKey) ?? variants[0];

  const makeJobId = () => crypto.randomUUID();

  const cancelFallbackTimer = () => {
    if (fallbackTimer.current !== null) {
      window.clearTimeout(fallbackTimer.current);
      fallbackTimer.current = null;
    }
  };

  useEffect(() => () => cancelFallbackTimer(), []);
  useEffect(() => {
    void getCurrentWindow().setTitle(document ? `${document.title} — Utool` : "Utool");
  }, [document?.title]);

  const loadSolution = useCallback(async (chart: ChartView, index: number, announce = true) => {
    const token = ++solutionOperation.current;
    const startedAt = performance.now();
    setSolutionRunning(true);
    if (announce) setStatus({ action: `Computing solution ${index + 1}`, elapsedMs: null, running: true });
    try {
      const next = await invoke<SolutionView | null>("solution_at", { chartId: chart.chartId, index });
      if (solutionOperation.current !== token) return;
      setSolution(next);
      setSolutionIndex(index);
      if (announce) setStatus({ action: `Computed solution ${index + 1}`, elapsedMs: next?.elapsedMs ?? 0, running: false });
    } catch (reason) {
      if (solutionOperation.current !== token) return;
      setError(String(reason));
      setStatus({ action: "Computing solution failed", elapsedMs: performance.now() - startedAt, running: false });
    } finally {
      if (solutionOperation.current === token) setSolutionRunning(false);
    }
  }, []);

  const computeBaseChart = useCallback(async (loaded: LoadedDocumentView, token: number) => {
    const startedAt = performance.now();
    const jobId = makeJobId();
    activeJob.current = jobId;
    setChartRunning(true);
    setStatus({ action: "Computing chart", elapsedMs: null, running: true });
    try {
      const chart = await invoke<ChartView>("build_chart", { documentId: loaded.documentId, jobId });
      if (operation.current !== token) return;
      cancelFallbackTimer();
      const base = { key: "base", name: "Unfiltered", chart };
      setVariants([base]);
      setActiveVariantKey("base");
      if (chart.graph) {
        setDocument((current) => current?.documentId === loaded.documentId
          ? { ...current, graph: chart.graph! }
          : current);
        setGraphOffsets({});
      }
      setGraphReady(true);
      setStatus({ action: "Computed chart", elapsedMs: chart.elapsedMs, running: false });
      if (chart.solutionCount !== "0") void loadSolution(chart, 0, false);
    } catch (reason) {
      if (operation.current !== token || String(reason).toLowerCase().includes("cancel")) return;
      cancelFallbackTimer();
      setGraphReady(true);
      setError(String(reason));
      setStatus({ action: "Computing chart failed", elapsedMs: performance.now() - startedAt, running: false });
    } finally {
      if (operation.current === token) {
        activeJob.current = null;
        setChartRunning(false);
      }
    }
  }, [loadSolution]);

  const addGraph = useCallback(async (input: string, codec: string, title: string, startedAt = performance.now()) => {
    const loadToken = ++loadOperation.current;
    setStatus({ action: `Opening ${title}`, elapsedMs: null, running: true });
    setError(null);
    try {
      const loaded = await invoke<LoadedDocumentView>("load_document", { input, codec });
      if (loadOperation.current !== loadToken) return;
      const token = ++operation.current;
      if (activeJob.current) void invoke("cancel_chart", { jobId: activeJob.current });
      activeJob.current = null;
      solutionOperation.current++;
      setVariants([]);
      setSolution(null);
      setChartRunning(false);
      setFilterRunning(null);
      setActiveView("graph");
      setGraphOffsets({});
      setGraphZoom(100);
      setSolutionZoom(100);
      setGraphReady(false);
      setDocument({ title, documentId: loaded.documentId, graph: loaded.graph });
      setStatus({ action: `Opened ${title}`, elapsedMs: loaded.elapsedMs, running: false });
      cancelFallbackTimer();
      fallbackTimer.current = window.setTimeout(() => {
        if (operation.current === token) setGraphReady(true);
        fallbackTimer.current = null;
      }, FALLBACK_LAYOUT_DELAY_MS);
      void computeBaseChart(loaded, token);
    } catch (reason) {
      if (loadOperation.current !== loadToken) return;
      setError(String(reason));
      setStatus({ action: `Opening ${title} failed`, elapsedMs: performance.now() - startedAt, running: false });
    }
  }, [computeBaseChart]);

  const openDocument = useCallback(async () => {
    // Native pickers filter by the final filesystem extension. Compound codec
    // suffixes are resolved below after the user has selected the file.
    const selected = await open({ multiple: false, filters: [{ name: "Dominance graphs", extensions: ["clls", "pl", "xml"] }] });
    if (!selected) return;
    const startedAt = performance.now();
    const title = selected.split(/[\\/]/).pop() ?? "Graph";
    setStatus({ action: `Opening ${title}`, elapsedMs: null, running: true });
    try {
      const lower = selected.toLowerCase();
      const codec = lower.endsWith(".mrs.pl") ? "mrs-prolog"
        : lower.endsWith(".hs.pl") ? "holesem-comsem"
        : lower.endsWith(".mrs.xml") ? "mrs-xml"
        : lower.endsWith(".dg.xml") ? "domgraph-gxl"
        : lower.endsWith(".clls") ? "domcon-oz"
        : null;
      if (!codec) throw new Error(`Unsupported graph filename: ${title}`);
      await addGraph(await readTextFile(selected), codec, title, startedAt);
    } catch (reason) {
      setError(String(reason));
      setStatus({ action: `Opening ${title} failed`, elapsedMs: performance.now() - startedAt, running: false });
    }
  }, [addGraph]);

  const chooseFilter = useCallback(async () => {
    const base = variants.find((variant) => variant.key === "base");
    if (!base || filterRunning) return;
    const selected = await open({ multiple: false });
    if (!selected) return;
    const filterName = selected.split(/[\\/]/).pop() ?? "Filter";
    const key = `filter:${selected}`;
    const cached = variants.find((variant) => variant.key === key);
    if (cached) {
      setActiveVariantKey(key);
      setSolution(null);
      if (cached.chart.solutionCount !== "0") void loadSolution(cached.chart, 0, false);
      return;
    }
    const token = operation.current;
    const startedAt = performance.now();
    const jobId = makeJobId();
    activeJob.current = jobId;
    setFilterRunning(filterName);
    setError(null);
    setStatus({ action: `Applying ${filterName}`, elapsedMs: null, running: true });
    try {
      const chart = await invoke<ChartView>("filter_chart_command", { chartId: base.chart.chartId, rewriteSystem: await readTextFile(selected), jobId });
      if (operation.current !== token) return;
      const variant = { key, name: filterName, chart };
      setVariants((current) => [...current, variant]);
      setActiveVariantKey(key);
      setSolution(null);
      setSolutionIndex(0);
      setStatus({ action: `${filterName} applied`, elapsedMs: chart.elapsedMs, running: false });
      if (chart.solutionCount !== "0") void loadSolution(chart, 0, false);
    } catch (reason) {
      if (operation.current !== token || String(reason).toLowerCase().includes("cancel")) return;
      setError(String(reason));
      setStatus({ action: "Filtering chart failed", elapsedMs: performance.now() - startedAt, running: false });
    } finally {
      if (operation.current === token) {
        activeJob.current = null;
        setFilterRunning(null);
      }
    }
  }, [filterRunning, loadSolution, variants]);

  const selectVariant = useCallback((key: string) => {
    const variant = variants.find((item) => item.key === key);
    if (!variant) return;
    setActiveVariantKey(key);
    setSolution(null);
    setSolutionIndex(0);
    if (variant.chart.solutionCount !== "0") void loadSolution(variant.chart, 0, false);
  }, [loadSolution, variants]);

  const setZoom = useCallback((zoom: Zoom) => {
    if (activeView === "graph") setGraphZoom(zoom);
    if (activeView === "solutions") setSolutionZoom(zoom);
  }, [activeView]);

  const changeZoom = useCallback((direction: 1 | -1) => {
    const change = (current: Zoom): Zoom => {
      const numeric = current === "fit" ? 100 : current;
      return Math.min(400, Math.max(25, Math.round(numeric * (direction > 0 ? 1.2 : 1 / 1.2))));
    };
    if (activeView === "graph") setGraphZoom(change);
    if (activeView === "solutions") setSolutionZoom(change);
  }, [activeView]);

  const exportSvg = useCallback(async () => {
    if (!svg.current || activeView === "chart") return;
    const selected = await save({ defaultPath: `${document?.title ?? "utool-graph"}.svg`, filters: [{ name: "SVG image", extensions: ["svg"] }] });
    if (selected) await writeTextFile(selected, `<?xml version="1.0" encoding="UTF-8"?>\n${svg.current.outerHTML}`);
  }, [activeView, document?.title]);

  const exportGraph = useCallback(async (format: "domcon" | "dot") => {
    if (!document) return;
    const extension = format === "dot" ? "dot" : "clls";
    const selected = await save({ defaultPath: `${document.title}.${extension}`, filters: [{ name: format === "dot" ? "Graphviz DOT" : "Domcon/Oz", extensions: [extension] }] });
    if (!selected) return;
    const startedAt = performance.now();
    setStatus({ action: `Exporting ${format}`, elapsedMs: null, running: true });
    try {
      const text = await invoke<string>("export_document", { documentId: document.documentId, format });
      await writeTextFile(selected, text);
      setStatus({ action: `Exported ${format}`, elapsedMs: performance.now() - startedAt, running: false });
    } catch (reason) { setError(String(reason)); setStatus({ action: `Exporting ${format} failed`, elapsedMs: performance.now() - startedAt, running: false }); }
  }, [document]);

  useEffect(() => { void addGraph(EXAMPLE, "domcon-oz", "Example"); }, [addGraph]);
  useEffect(() => {
    let disposed = false;
    const pending = Promise.all([
      listen("menu-open", openDocument), listen("menu-export-svg", exportSvg),
      listen("menu-export-domcon", () => exportGraph("domcon")), listen("menu-export-dot", () => exportGraph("dot")),
      listen("menu-zoom-in", () => changeZoom(1)),
      listen("menu-zoom-out", () => changeZoom(-1)),
      listen("menu-actual-size", () => setZoom(100)),
      listen("menu-fit-window", () => setZoom("fit")),
      listen("menu-about", () => setError("Utool Rust — HNC dominance graph solving with rusty-alto.")),
    ]);
    return () => { disposed = true; void pending.then((items) => { if (disposed) items.forEach((unlisten) => unlisten()); }); };
  }, [changeZoom, exportGraph, exportSvg, openDocument, setZoom]);

  const solutionTotal = activeVariant?.chart.solutionCount ?? "0";
  const derivedLoading = chartRunning && !activeVariant;

  useEffect(() => {
    if (activeView !== "solutions" || !activeVariant) return;
    const navigate = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null;
      if (target?.matches("input, textarea, select, [contenteditable='true']") || event.altKey || event.ctrlKey || event.metaKey) return;
      if (event.key === "ArrowLeft" && solutionIndex > 0 && !solutionRunning) {
        event.preventDefault();
        void loadSolution(activeVariant.chart, solutionIndex - 1);
      } else if (event.key === "ArrowRight" && BigInt(solutionIndex + 1) < BigInt(solutionTotal) && !solutionRunning) {
        event.preventDefault();
        void loadSolution(activeVariant.chart, solutionIndex + 1);
      }
    };
    window.addEventListener("keydown", navigate);
    return () => window.removeEventListener("keydown", navigate);
  }, [activeVariant, activeView, loadSolution, solutionIndex, solutionRunning, solutionTotal]);

  return <main>
    <nav className="tabs" aria-label="Document views">
      {(["graph", "chart", "solutions"] as ViewName[]).map((view) => <button key={view} className={view === activeView ? "active" : ""} onClick={() => setActiveView(view)} disabled={!document}>
        <span>{view === "graph" ? "Graph" : view === "chart" ? "Chart" : `Solutions${activeVariant ? ` (${activeVariant.chart.solutionCount})` : ""}`}</span>
        {view !== "graph" && derivedLoading && <i className="tab-spinner" aria-label="Computing" />}
      </button>)}
      {document && <SolutionSpaceControl variants={variants} activeKey={activeVariantKey} filterRunning={filterRunning} onSelect={selectVariant} onAdd={chooseFilter} />}
    </nav>
    {error && <div className="error-banner" onClick={() => setError(null)}>{error}</div>}
    <section className="document">
      {!document && <div className="welcome"><h2>No graph open</h2><p>Choose File → Open… to open a dominance graph.</p></div>}
      {document && activeView === "graph" && !graphReady && <div className="computing"><span className="large-spinner" /><h2>Computing chart</h2><p>Preparing the graph layout.</p></div>}
      {document && activeView === "graph" && graphReady && <GraphCanvas key={document.documentId} graph={document.graph} zoom={graphZoom} offsets={graphOffsets} onOffsetsChange={setGraphOffsets} onZoomChange={setGraphZoom} onSvgReady={(element) => { svg.current = element; }} />}
      {document && activeView !== "graph" && derivedLoading && <div className="computing"><span className="large-spinner" /><h2>Computing chart</h2><p>You can continue inspecting the graph while the solution space is prepared.</p></div>}
      {document && activeView === "chart" && activeVariant && <div className="chart-view">
        {filterRunning && <div className="pending-banner"><span className="small-spinner" />Computing {filterRunning}. Currently showing {activeVariant.name}.</div>}
        <ChartRules key={activeVariant.chart.chartId} chart={activeVariant.chart} />
        <div className="chart-bar"><span className="chart-stats"><b>{activeVariant.chart.stateCount}</b> states{activeVariant.chart.stateCount !== activeVariant.chart.subgraphCount && <> · <b>{activeVariant.chart.subgraphCount}</b> subgraphs</>} · <b>{activeVariant.chart.splitCount}</b> split rules · <strong>{activeVariant.chart.solutionCount} solutions</strong></span></div>
      </div>}
      {document && activeView === "solutions" && activeVariant && <div className="solutions-view">
        {filterRunning && <div className="pending-banner"><span className="small-spinner" />Computing {filterRunning}. Currently showing {activeVariant.name}.</div>}
        {solutionTotal === "0" ? <div className="zero-solutions"><h2>No solutions</h2><p>No solutions satisfy {activeVariant.name === "Unfiltered" ? "this graph" : activeVariant.name}.</p>{activeVariant.key !== "base" && <button onClick={() => selectVariant("base")}>Show unfiltered</button>}</div>
          : solution ? <><GraphCanvas key={`${activeVariant.key}-${solutionIndex}`} graph={solutionGraph(solution)} zoom={solutionZoom} draggable={false} onZoomChange={setSolutionZoom} onSvgReady={(element) => { svg.current = element; }} /><div className="solution-bar"><div className="solution-nav"><button title="Previous solution (Left Arrow)" aria-label="Previous solution" disabled={solutionIndex === 0 || solutionRunning} onClick={() => void loadSolution(activeVariant.chart, solutionIndex - 1)}>←</button><label><span>Solution</span><input value={solutionIndex + 1} onChange={(event) => { const value = Number(event.target.value); if (Number.isSafeInteger(value) && value > 0 && BigInt(value) <= BigInt(solutionTotal)) void loadSolution(activeVariant.chart, value - 1); }} /></label><span>of <b>{solutionTotal}</b></span><button title="Next solution (Right Arrow)" aria-label="Next solution" disabled={BigInt(solutionIndex + 1) >= BigInt(solutionTotal) || solutionRunning} onClick={() => void loadSolution(activeVariant.chart, solutionIndex + 1)}>→</button></div></div></>
          : <div className="computing"><span className="large-spinner" /><h2>Computing first solution</h2></div>}
      </div>}
    </section>
    <footer className="status-bar"><span className={`status-operation${status.running ? " busy" : ""}`}>{status.action}</span><time>{status.elapsedMs === null ? (status.running ? "Running…" : "") : formatElapsed(status.elapsedMs)}</time></footer>
  </main>;
}
