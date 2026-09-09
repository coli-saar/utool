import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import { getCurrentWindow } from "@tauri-apps/api/window";
import { open, save } from "@tauri-apps/plugin-dialog";
import { readTextFile, writeTextFile } from "@tauri-apps/plugin-fs";
import { readText, writeText } from "@tauri-apps/plugin-clipboard-manager";
import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import type { CSSProperties } from "react";
import { GraphCanvas } from "./GraphCanvas";
import type { Zoom } from "./GraphCanvas";
import type { AppInfo, ChartRowPage, ChartRule, ChartState, ChartView, ExampleSummary, GraphView, LoadedDocumentView, ServerDialogInfo, ServerStatus, SolutionView, StartupDocument, StartupFilter } from "./types";

const EXAMPLE = `[label(x f(x1)) label(y g(y1)) label(z a) dom(x1 z) dom(y1 z) dom(y x1)]`;
const WINDOW_LABEL = getCurrentWindow().label;
type ViewName = "graph" | "chart" | "solutions";
type DocumentView = { title: string; documentId: number; graph: GraphView };
type ChartVariant = { key: string; name: string; chart: ChartView };
type ActionStatus = { action: string; elapsedMs: number | null; running: boolean };
type OutputFormat = {
  name: string;
  extension: string;
  label: string;
  graph: boolean;
  solution: boolean;
};
type InputFormat = { name: string; label: string };

const INPUT_FORMATS: InputFormat[] = [
  { name: "chain", label: "Generated Chain" },
  { name: "domcon-oz", label: "Domcon/Oz" },
  { name: "domgraph-gxl", label: "Domgraph GXL" },
  { name: "holesem-comsem", label: "Hole Semantics" },
  { name: "mrs-prolog", label: "MRS Prolog" },
  { name: "mrs-xml", label: "MRS XML" },
];

const OUTPUT_FORMATS: OutputFormat[] = [
  { name: "domcon-oz", extension: "clls", label: "Domcon/Oz", graph: true, solution: true },
  { name: "domgraph-dot", extension: "dg.dot", label: "Graphviz DOT", graph: true, solution: false },
  { name: "domgraph-gxl", extension: "dg.xml", label: "Domgraph GXL", graph: true, solution: true },
  { name: "domgraph-udraw", extension: "dg.udg", label: "uDraw(Graph)", graph: true, solution: false },
  { name: "domgraph-codegen", extension: "java", label: "Java Code", graph: true, solution: true },
  { name: "plugging-oz", extension: "plug.oz", label: "Plugging/Oz", graph: true, solution: true },
  { name: "plugging-lkb", extension: "lkbplug.lisp", label: "LKB Plugging", graph: true, solution: true },
  { name: "plugging-groovy", extension: "plug.groovy", label: "Groovy Plugging", graph: true, solution: true },
  { name: "term-prolog", extension: "t.pl", label: "Prolog Term", graph: false, solution: true },
  { name: "term-oz", extension: "t.oz", label: "Oz Term", graph: false, solution: true },
];

function formatElapsed(elapsedMs: number): string {
  if (elapsedMs < 1) return `${(elapsedMs * 1000).toFixed(elapsedMs < 0.1 ? 1 : 0)} µs`;
  if (elapsedMs < 1000) return `${elapsedMs.toFixed(elapsedMs < 10 ? 2 : 1)} ms`;
  return `${(elapsedMs / 1000).toFixed(3)} s`;
}

function describeError(reason: unknown): string {
  if (reason instanceof Error) return reason.stack ?? reason.message;
  return String(reason);
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

function dismissStartupScreen() {
  const screen = window.document.getElementById("startup-screen");
  const root = window.document.documentElement;
  if (!screen || !root.classList.contains("startup") || screen.dataset.dismissing) return;
  screen.dataset.dismissing = "true";
  const shownAt = Number(root.dataset.startupShownAt);
  const elapsed = Number.isFinite(shownAt) ? performance.now() - shownAt : 650;
  const remaining = Math.max(0, 650 - elapsed);
  window.setTimeout(() => {
    screen.classList.add("startup-screen-exit");
    window.setTimeout(() => {
      screen.remove();
      root.classList.remove("startup");
      delete root.dataset.startupShownAt;
    }, 180);
  }, remaining);
}

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

  if (chart.displayRowCount === 0) return <div className="empty-chart">The chart contains no productive rules.</div>;
  if (loadError) return <div className="empty-chart">Could not load chart rules. See the Event Log for details.</div>;
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
  const pending = "__pending_filter__";
  return <label className="filter-picker" htmlFor="solution-space">
    <span>Filter</span>
    <select id="solution-space" value={filterRunning ? pending : activeKey} onChange={(event) => event.target.value === choose ? onAdd() : onSelect(event.target.value)} disabled={Boolean(filterRunning) || variants.length === 0}>
      {variants.length === 0 && <option value="base">Computing chart…</option>}
      {variants.map((variant) => <option key={variant.key} value={variant.key}>{variant.key === "base" ? "None" : variant.name} · {variant.chart.solutionCount} solutions</option>)}
      {filterRunning && <option value={pending}>{filterRunning} · Computing…</option>}
      {variants.length > 0 && <option value={choose}>Choose filter…</option>}
    </select>
  </label>;
}

function ExampleChooser({ examples, selectedId, opening, onSelect, onOpen, onClose }: {
  examples: ExampleSummary[] | null;
  selectedId: string | null;
  opening: boolean;
  onSelect: (id: string) => void;
  onOpen: (id: string) => void;
  onClose: () => void;
}) {
  const selected = examples?.find((example) => example.id === selectedId) ?? null;
  const selectedButton = useRef<HTMLButtonElement | null>(null);

  useEffect(() => {
    selectedButton.current?.focus();
  }, [selectedId, examples]);

  useEffect(() => {
    const keyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault();
        onClose();
      } else if (event.key === "Enter" && selected && !opening) {
        event.preventDefault();
        onOpen(selected.id);
      } else if ((event.key === "ArrowDown" || event.key === "ArrowUp") && examples?.length) {
        event.preventDefault();
        const current = Math.max(0, examples.findIndex((example) => example.id === selectedId));
        const direction = event.key === "ArrowDown" ? 1 : -1;
        const next = Math.min(examples.length - 1, Math.max(0, current + direction));
        onSelect(examples[next].id);
      }
    };
    window.addEventListener("keydown", keyDown);
    return () => window.removeEventListener("keydown", keyDown);
  }, [examples, onClose, onOpen, onSelect, opening, selected, selectedId]);

  const codecLabel = selected?.codec === "domcon-oz" ? "Domcon/Oz"
    : selected?.codec === "holesem-comsem" ? "Hole Semantics"
    : selected?.codec === "mrs-prolog" ? "MRS Prolog"
    : selected?.codec ?? "";

  return <div className="dialog-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
    <section className="example-dialog" role="dialog" aria-modal="true" aria-labelledby="example-dialog-title">
      <header><h1 id="example-dialog-title">Open Example</h1></header>
      <div className="example-dialog-body">
        <div className="example-list" role="listbox" aria-label="Built-in examples">
          {examples === null && <div className="example-loading"><span className="small-spinner" />Loading examples…</div>}
          {examples?.map((example) => <button
            key={example.id}
            ref={example.id === selectedId ? selectedButton : undefined}
            type="button"
            role="option"
            aria-selected={example.id === selectedId}
            className={example.id === selectedId ? "selected" : ""}
            onClick={() => onSelect(example.id)}
            onDoubleClick={() => { if (!opening) onOpen(example.id); }}
          >{example.filename}</button>)}
        </div>
        <article className="example-description">
          {selected ? <>
            <h2>{selected.filename}</h2>
            <p className="example-codec">Codec: {codecLabel}</p>
            <p>{selected.description}</p>
          </> : examples !== null && <p>Select an example to see its description.</p>}
        </article>
      </div>
      <footer>
        <button type="button" onClick={onClose} disabled={opening}>Cancel</button>
        <button type="button" className="primary" onClick={() => { if (selected) onOpen(selected.id); }} disabled={!selected || opening}>{opening ? "Opening…" : "Open"}</button>
      </footer>
    </section>
  </div>;
}

function AboutDialog({ info, onClose }: { info: AppInfo; onClose: () => void }) {
  const closeButton = useRef<HTMLButtonElement | null>(null);

  useEffect(() => {
    closeButton.current?.focus();
    const keyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault();
        onClose();
      }
    };
    window.addEventListener("keydown", keyDown);
    return () => window.removeEventListener("keydown", keyDown);
  }, [onClose]);

  return <div className="dialog-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
    <section className="about-dialog" role="dialog" aria-modal="true" aria-labelledby="about-dialog-title">
      <header><h1 id="about-dialog-title">About Utool</h1></header>
      <div className="about-dialog-body">
        <strong>Utool</strong>
        <p>The Swiss Army Knife of Underspecification</p>
        <p>Saarland University</p>
        <dl><div><dt>Version</dt><dd>{info.version}</dd></div><div><dt>Build</dt><dd>{info.buildId}</dd></div></dl>
      </div>
      <footer><button ref={closeButton} type="button" className="primary" onClick={onClose}>Close</button></footer>
    </section>
  </div>;
}

function ServerDialog({ info, starting, error, onStart, onClose }: {
  info: ServerDialogInfo;
  starting: boolean;
  error: string | null;
  onStart: (port: number, acceptNonLocal: boolean) => void;
  onClose: () => void;
}) {
  const [port, setPort] = useState(String(info.port));
  const [acceptNonLocal, setAcceptNonLocal] = useState(info.acceptNonLocal);
  const portNumber = Number(port);
  const validPort = Number.isInteger(portNumber) && portNumber >= 1 && portNumber <= 65535;
  const host = acceptNonLocal ? info.ethernetAddress : info.localAddress;
  const address = host === "Unavailable" ? host : `${host}${validPort ? `:${portNumber}` : ""}`;
  const portInput = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    portInput.current?.focus();
    const keyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !starting) {
        event.preventDefault();
        onClose();
      } else if (event.key === "Enter" && validPort && !starting) {
        event.preventDefault();
        onStart(portNumber, acceptNonLocal);
      }
    };
    window.addEventListener("keydown", keyDown);
    return () => window.removeEventListener("keydown", keyDown);
  }, [acceptNonLocal, onClose, onStart, portNumber, starting, validPort]);

  return <div className="dialog-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget && !starting) onClose(); }}>
    <section className="server-dialog" role="dialog" aria-modal="true" aria-labelledby="server-dialog-title">
      <header><h1 id="server-dialog-title">Start Server</h1></header>
      <div className="server-dialog-body">
        <label className="server-port"><span>Port</span><input ref={portInput} type="number" min="1" max="65535" value={port} onChange={(event) => setPort(event.target.value)} aria-invalid={!validPort} /></label>
        <label className="server-checkbox"><input type="checkbox" checked={acceptNonLocal} onChange={(event) => setAcceptNonLocal(event.target.checked)} /><span>Accept non-local connections</span></label>
        <div className="server-address"><span>Server address</span><code>{address}</code></div>
        {acceptNonLocal && <p>Other computers that can reach this address can connect to Utool while the server is running.</p>}
        {error && <div className="server-dialog-error" role="alert">{error}</div>}
      </div>
      <footer>
        <button type="button" onClick={onClose} disabled={starting}>Cancel</button>
        <button type="button" className="primary" onClick={() => onStart(portNumber, acceptNonLocal)} disabled={!validPort || starting}>{starting ? "Starting…" : "Start Server"}</button>
      </footer>
    </section>
  </div>;
}

export default function App() {
  const [document, setDocument] = useState<DocumentView | null>(null);
  const [graphReady, setGraphReady] = useState(false);
  const [activeView, setActiveView] = useState<ViewName>("graph");
  const [variants, setVariants] = useState<ChartVariant[]>([]);
  const [activeVariantKey, setActiveVariantKey] = useState("base");
  const [chartRunning, setChartRunning] = useState(false);
  const [filterRunning, setFilterRunning] = useState<string | null>(null);
  const [startupFilter, setStartupFilter] = useState<StartupFilter | null | undefined>(undefined);
  const [solutionRunning, setSolutionRunning] = useState(false);
  const [solution, setSolution] = useState<SolutionView | null>(null);
  const [solutionIndex, setSolutionIndex] = useState(0);
  const [graphZoom, setGraphZoom] = useState<Zoom>(100);
  const [solutionZoom, setSolutionZoom] = useState<Zoom>(100);
  const [graphOffsets, setGraphOffsets] = useState<Record<number, { x: number; y: number }>>({});
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState<ActionStatus>({ action: "Ready", elapsedMs: null, running: false });
  const [aboutInfo, setAboutInfo] = useState<AppInfo | null>(null);
  const [serverDialog, setServerDialog] = useState<ServerDialogInfo | null>(null);
  const [serverStarting, setServerStarting] = useState(false);
  const [serverStartError, setServerStartError] = useState<string | null>(null);
  const [serverStatus, setServerStatus] = useState<ServerStatus>({ state: "stopped", address: null, tooltip: "Server stopped", notice: null });
  const [exampleChooserOpen, setExampleChooserOpen] = useState(false);
  const [examples, setExamples] = useState<ExampleSummary[] | null>(null);
  const [selectedExampleId, setSelectedExampleId] = useState<string | null>(null);
  const [exampleOpening, setExampleOpening] = useState(false);
  const svg = useRef<SVGSVGElement | null>(null);
  const operation = useRef(0);
  const loadOperation = useRef(0);
  const solutionOperation = useRef(0);
  const activeJob = useRef<string | null>(null);
  const autoFilterDocument = useRef<number | null>(null);
  const activeVariant = variants.find((variant) => variant.key === activeVariantKey) ?? variants[0];

  const flashError = useCallback((message: string) => {
    const summary = message.trim().replace(/[.!?]+$/, "");
    const banner = `${summary} (check the Event Log for details).`;
    setError(banner);
    window.setTimeout(() => {
      setError((current) => current === banner ? null : current);
    }, 4000);
  }, []);

  const recordClientAction = useCallback((action: string, arguments_: unknown, startedAt: number, reason?: unknown) => {
    void invoke("report_client_action", {
      action,
      arguments: arguments_,
      elapsedMs: performance.now() - startedAt,
      error: reason === undefined ? null : describeError(reason),
    });
  }, []);

  const reportFailure = useCallback((summary: string, action: string, arguments_: unknown, startedAt: number, reason: unknown) => {
    flashError(summary);
    recordClientAction(action, arguments_, startedAt, reason);
  }, [flashError, recordClientAction]);

  const applyServerStatus = useCallback((nextStatus: ServerStatus) => {
    setServerStatus(nextStatus);
    if (nextStatus.notice) {
      setServerDialog(null);
      setServerStarting(false);
      flashError(nextStatus.notice);
      recordClientAction("Server failure", {}, performance.now(), nextStatus.notice);
      void invoke("clear_server_notice");
    } else if (nextStatus.state === "error") {
      reportFailure("The server stopped with an error.", "Server failure", {}, performance.now(), nextStatus.tooltip);
    }
  }, [flashError, recordClientAction, reportFailure]);

  const showServerDialog = useCallback(() => {
    setServerStartError(null);
    const startedAt = performance.now();
    void invoke<ServerDialogInfo>("server_dialog_info")
      .then(setServerDialog)
      .catch((reason) => reportFailure("Could not open the server settings.", "Open server settings", {}, startedAt, reason));
  }, [reportFailure]);

  const startServer = useCallback((port: number, acceptNonLocal: boolean) => {
    setServerStartError(null);
    setServerStarting(true);
    const startedAt = performance.now();
    void invoke<ServerStatus>("start_desktop_server", { port, acceptNonLocal })
      .then((nextStatus) => { applyServerStatus(nextStatus); setServerDialog(null); })
      .catch((reason) => {
        const message = describeError(reason);
        recordClientAction("Start server", { port, acceptNonLocal }, startedAt, reason);
        if (/Port \d+ is already in use/.test(message)) {
          setServerDialog(null);
          setServerStatus({ state: "stopped", address: null, tooltip: "Server stopped", notice: null });
          flashError(`Port ${port} is already in use.`);
          void invoke("clear_server_notice");
        } else {
          setServerStartError("Could not start the server. See the Event Log for details.");
        }
      })
      .finally(() => setServerStarting(false));
  }, [applyServerStatus, flashError, recordClientAction]);

  const makeJobId = () => crypto.randomUUID();

  useEffect(() => {
    const reportWindowError = (event: ErrorEvent) => {
      flashError("An unexpected interface error occurred.");
      recordClientAction("Unhandled UI error", { filename: event.filename, line: event.lineno, column: event.colno }, performance.now(), event.error ?? event.message);
    };
    const reportRejection = (event: PromiseRejectionEvent) => {
      flashError("An unexpected interface error occurred.");
      recordClientAction("Unhandled UI promise rejection", {}, performance.now(), event.reason);
    };
    window.addEventListener("error", reportWindowError);
    window.addEventListener("unhandledrejection", reportRejection);
    return () => {
      window.removeEventListener("error", reportWindowError);
      window.removeEventListener("unhandledrejection", reportRejection);
    };
  }, [flashError, recordClientAction]);
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
      reportFailure("Could not compute that solution.", "Compute solution", { chartId: chart.chartId, solution: index + 1 }, startedAt, reason);
      setStatus({ action: "Computing solution failed", elapsedMs: performance.now() - startedAt, running: false });
    } finally {
      if (solutionOperation.current === token) setSolutionRunning(false);
    }
  }, [reportFailure]);

  const computeBaseChart = useCallback(async (loaded: LoadedDocumentView, token: number) => {
    const startedAt = performance.now();
    const jobId = makeJobId();
    activeJob.current = jobId;
    setChartRunning(true);
    setStatus({ action: "Computing chart", elapsedMs: null, running: true });
    try {
      const chart = await invoke<ChartView>("build_chart", { documentId: loaded.documentId, jobId });
      if (operation.current !== token) return;
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
      setGraphReady(true);
      reportFailure("Could not compute the solution chart.", "Compute solution chart", { documentId: loaded.documentId }, startedAt, reason);
      setStatus({ action: "Computing chart failed", elapsedMs: performance.now() - startedAt, running: false });
    } finally {
      if (operation.current === token) {
        activeJob.current = null;
        setChartRunning(false);
      }
    }
  }, [loadSolution, reportFailure]);

  const installGraph = useCallback((loaded: LoadedDocumentView) => {
      const token = ++operation.current;
      if (activeJob.current) void invoke("cancel_chart", { jobId: activeJob.current });
      activeJob.current = null;
      solutionOperation.current++;
      setVariants([]);
      setSolution(null);
      setChartRunning(false);
      setFilterRunning(null);
      autoFilterDocument.current = null;
      setActiveView("graph");
      setGraphOffsets({});
      setGraphZoom(100);
      setSolutionZoom(100);
      setGraphReady(false);
      setDocument({ title: loaded.title, documentId: loaded.documentId, graph: loaded.graph });
      setStatus({ action: `Opened ${loaded.title}`, elapsedMs: loaded.elapsedMs, running: false });
      void computeBaseChart(loaded, token);
  }, [computeBaseChart]);

  const addGraph = useCallback(async (input: string, codec: string, title: string, startedAt = performance.now()) => {
    const loadToken = ++loadOperation.current;
    setStatus({ action: `Opening ${title}`, elapsedMs: null, running: true });
    setError(null);
    try {
      const loaded = await invoke<LoadedDocumentView>("load_document", { input, codec, title });
      if (loadOperation.current !== loadToken) return;
      installGraph(loaded);
    } catch (reason) {
      if (loadOperation.current !== loadToken) return;
      reportFailure(`Could not open ${title}.`, "Load graph document", { title, codec }, startedAt, reason);
      setStatus({ action: `Opening ${title} failed`, elapsedMs: performance.now() - startedAt, running: false });
    }
  }, [installGraph, reportFailure]);

  const openDocument = useCallback(async () => {
    // Native pickers filter by the final filesystem extension. Compound codec
    // suffixes are resolved below after the user has selected the file.
    const selected = await open({ multiple: false, filters: [{ name: "Dominance graphs", extensions: ["clls", "pl", "xml"] }] });
    if (!selected) return;
    const startedAt = performance.now();
    const title = selected.split(/[\\/]/).pop() ?? "Graph";
    try {
      const lower = selected.toLowerCase();
      const codec = lower.endsWith(".mrs.pl") ? "mrs-prolog"
        : lower.endsWith(".hs.pl") ? "holesem-comsem"
        : lower.endsWith(".mrs.xml") ? "mrs-xml"
        : lower.endsWith(".dg.xml") ? "domgraph-gxl"
        : lower.endsWith(".clls") ? "domcon-oz"
        : null;
      if (!codec) throw new Error(`Unsupported graph filename: ${title}`);
      const input = await readTextFile(selected);
      await invoke("open_graph_window", { request: { input, codec, title, filename: selected } });
    } catch (reason) {
      reportFailure("Could not open the selected graph.", "Open graph file", { filename: selected }, startedAt, reason);
    }
  }, [reportFailure]);

  const pasteDocument = useCallback(async (format: InputFormat) => {
    const startedAt = performance.now();
    const title = `Clipboard — ${format.label}`;
    setError(null);
    try {
      const input = await readText();
      await invoke("open_graph_window", {
        request: { input, codec: format.name, title, filename: "Clipboard" },
      });
    } catch (reason) {
      reportFailure("Could not paste the graph.", "Paste graph from clipboard", { format: format.name }, startedAt, reason);
    }
  }, [reportFailure]);

  const showExampleChooser = useCallback(() => {
    setExampleChooserOpen(true);
    if (examples !== null) {
      setSelectedExampleId((current) => current ?? examples[0]?.id ?? null);
      return;
    }
    void invoke<ExampleSummary[]>("list_examples")
      .then((items) => {
        setExamples(items);
        setSelectedExampleId((current) => current ?? items[0]?.id ?? null);
      })
      .catch((reason) => {
        setExampleChooserOpen(false);
        reportFailure("Could not load the built-in examples.", "List built-in examples", {}, performance.now(), reason);
      });
  }, [examples, reportFailure]);

  const openExample = useCallback(async (id: string) => {
    const example = examples?.find((item) => item.id === id);
    if (!example || exampleOpening) return;
    const startedAt = performance.now();
    setExampleOpening(true);
    try {
      await invoke("open_example_window", { id });
      setExampleChooserOpen(false);
    } catch (reason) {
      reportFailure("Could not open the selected example.", "Open built-in example", { id }, startedAt, reason);
    } finally {
      setExampleOpening(false);
    }
  }, [exampleOpening, examples, reportFailure]);

  const applyFilterFile = useCallback(async (selected: string, rewriteSystem?: string) => {
    const base = variants.find((variant) => variant.key === "base");
    if (!base || filterRunning) return;
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
      const chart = await invoke<ChartView>("filter_chart_command", { chartId: base.chart.chartId, rewriteSystem: rewriteSystem ?? await readTextFile(selected), filename: selected, jobId });
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
      reportFailure(`Could not apply ${filterName}.`, "Apply filter file", { filename: selected }, startedAt, reason);
      setStatus({ action: "Filtering chart failed", elapsedMs: performance.now() - startedAt, running: false });
    } finally {
      if (operation.current === token) {
        activeJob.current = null;
        setFilterRunning(null);
      }
    }
  }, [filterRunning, loadSolution, reportFailure, variants]);

  const chooseFilter = useCallback(async () => {
    const selected = await open({ multiple: false });
    if (selected) await applyFilterFile(selected);
  }, [applyFilterFile]);

  useEffect(() => {
    const base = variants.find((variant) => variant.key === "base");
    if (!document || !base || startupFilter === undefined || autoFilterDocument.current === document.documentId) return;
    autoFilterDocument.current = document.documentId;
    if (!startupFilter) return;
    void applyFilterFile(startupFilter.filename, startupFilter.rewriteSystem);
  }, [applyFilterFile, document, startupFilter, variants]);

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
    if (!selected) return;
    const startedAt = performance.now();
    try {
      await writeTextFile(selected, `<?xml version="1.0" encoding="UTF-8"?>\n${svg.current.outerHTML}`);
      recordClientAction("Export SVG", { filename: selected }, startedAt);
    } catch (reason) {
      reportFailure("Could not export the SVG image.", "Export SVG", { filename: selected }, startedAt, reason);
    }
  }, [activeView, document?.title, recordClientAction, reportFailure]);

  const encodeCurrent = useCallback(async (format: OutputFormat, filename: string) => {
    if (activeView === "graph" && document && format.graph) {
      return invoke<string>("export_document", { documentId: document.documentId, format: format.name, filename });
    }
    if (activeView === "solutions" && activeVariant && solution && format.solution) {
      return invoke<string>("export_solution", { chartId: activeVariant.chart.chartId, index: solutionIndex, format: format.name, filename });
    }
    throw new Error(`${format.label} is not applicable to the current view.`);
  }, [activeVariant, activeView, document, solution, solutionIndex]);

  const exportCurrent = useCallback(async (format: OutputFormat) => {
    if (!document) return;
    const basename = activeView === "solutions" ? `${document.title}-solution-${solutionIndex + 1}` : document.title;
    const selected = await save({ defaultPath: `${basename}.${format.extension}`, filters: [{ name: format.label, extensions: [format.extension.split(".").pop()!] }] });
    if (!selected) return;
    const startedAt = performance.now();
    setStatus({ action: `Exporting ${format.label}`, elapsedMs: null, running: true });
    try {
      const text = await encodeCurrent(format, selected);
      await writeTextFile(selected, text);
      recordClientAction("Write export", { filename: selected, format: format.name, view: activeView }, startedAt);
      setStatus({ action: `Exported ${format.label}`, elapsedMs: performance.now() - startedAt, running: false });
    } catch (reason) {
      reportFailure(`Could not export as ${format.label}.`, "Write export", { filename: selected, format: format.name, view: activeView }, startedAt, reason);
      setStatus({ action: `Exporting ${format.label} failed`, elapsedMs: performance.now() - startedAt, running: false });
    }
  }, [activeView, document, encodeCurrent, recordClientAction, reportFailure, solutionIndex]);

  const copyCurrent = useCallback(async (format: OutputFormat) => {
    const startedAt = performance.now();
    try {
      await writeText(await encodeCurrent(format, "Clipboard"));
      recordClientAction("Copy encoded output", { format: format.name, view: activeView }, startedAt);
      setStatus({ action: `Copied as ${format.label}`, elapsedMs: performance.now() - startedAt, running: false });
    } catch (reason) {
      reportFailure(`Could not copy as ${format.label}.`, "Copy encoded output", { format: format.name, view: activeView }, startedAt, reason);
    }
  }, [activeView, encodeCurrent, recordClientAction, reportFailure]);

  const copySvg = useCallback(async () => {
    if (!svg.current || activeView === "chart") return;
    const startedAt = performance.now();
    try {
      await writeText(`<?xml version="1.0" encoding="UTF-8"?>\n${svg.current.outerHTML}`);
      recordClientAction("Copy SVG", { view: activeView }, startedAt);
      setStatus({ action: "Copied as SVG", elapsedMs: performance.now() - startedAt, running: false });
    } catch (reason) {
      reportFailure("Could not copy the SVG image.", "Copy SVG", { view: activeView }, startedAt, reason);
    }
  }, [activeView, recordClientAction, reportFailure]);

  useEffect(() => {
    const syncMenu = () => invoke("set_output_context", { view: activeView, hasDocument: Boolean(document), hasSolution: Boolean(solution && activeVariant) });
    void syncMenu();
    let disposed = false;
    let unlisten: (() => void) | undefined;
    void getCurrentWindow().onFocusChanged(({ payload }) => {
      if (payload) void syncMenu();
    }).then((dispose) => { if (disposed) dispose(); else unlisten = dispose; });
    return () => { disposed = true; unlisten?.(); };
  }, [activeVariant, activeView, document, solution]);

  useEffect(() => {
    void invoke<ServerStatus>("server_status")
      .then(applyServerStatus)
      .catch((reason) => reportFailure("Could not read the server status.", "Read server status", {}, performance.now(), reason));
    let disposed = false;
    let unlisten: (() => void) | undefined;
    void listen<ServerStatus>("server-status-changed", ({ payload }) => applyServerStatus(payload))
      .then((dispose) => { if (disposed) dispose(); else unlisten = dispose; });
    return () => { disposed = true; unlisten?.(); };
  }, [applyServerStatus, reportFailure]);

  useEffect(() => {
    if (WINDOW_LABEL === "main") {
      void Promise.all([
        invoke<StartupDocument[]>("take_startup_documents"),
        invoke<StartupFilter | null>("startup_filter")
          .then((selected) => { setStartupFilter(selected); })
          .catch((reason) => {
            setStartupFilter(null);
            reportFailure("Could not load the startup filter.", "Load startup filter", {}, performance.now(), reason);
          }),
      ]).then(async ([documents]) => {
        try {
          if (documents.length === 0) {
            await addGraph(EXAMPLE, "domcon-oz", "Example");
            return;
          }
          const [first, ...rest] = documents;
          await addGraph(first.input, first.codec, first.title);
          for (const request of rest) {
            await invoke("open_graph_window", { request });
          }
        } finally {
          dismissStartupScreen();
        }
      }).catch((reason) => {
        dismissStartupScreen();
        setStartupFilter(null);
        reportFailure("Could not restore the startup documents.", "Restore startup documents", {}, performance.now(), reason);
      });
    } else {
      void invoke<StartupFilter | null>("startup_filter")
        .then(setStartupFilter)
        .catch((reason) => {
          setStartupFilter(null);
          reportFailure("Could not load the startup filter.", "Load startup filter", {}, performance.now(), reason);
        });
      void invoke<LoadedDocumentView | null>("current_document").then((loaded) => {
        if (loaded) installGraph(loaded);
        else reportFailure("This graph window no longer has an open document.", "Restore graph window", {}, performance.now(), "No document was associated with this window.");
      }).catch((reason) => reportFailure("Could not restore this graph window.", "Restore graph window", {}, performance.now(), reason));
    }
  }, [addGraph, installGraph, reportFailure]);
  useEffect(() => {
    let disposed = false;
    const listenMenu = (event: string, handler: () => void) =>
      listen(event, handler, { target: WINDOW_LABEL });
    const pending = Promise.all([
      listenMenu("menu-open", openDocument), listenMenu("menu-open-example", showExampleChooser), listenMenu("menu-start-server", showServerDialog), listenMenu("menu-export-svg", exportSvg), listenMenu("menu-copy-svg", copySvg),
      ...OUTPUT_FORMATS.flatMap((format) => [
        listenMenu(`menu-export-${format.name}`, () => exportCurrent(format)),
        listenMenu(`menu-copy-${format.name}`, () => copyCurrent(format)),
      ]),
      ...INPUT_FORMATS.map((format) =>
        listenMenu(`menu-paste-${format.name}`, () => pasteDocument(format))
      ),
      listenMenu("menu-view-graph", () => setActiveView("graph")),
      listenMenu("menu-view-chart", () => setActiveView("chart")),
      listenMenu("menu-view-solutions", () => setActiveView("solutions")),
      listenMenu("menu-zoom-in", () => changeZoom(1)),
      listenMenu("menu-zoom-out", () => changeZoom(-1)),
      listenMenu("menu-actual-size", () => setZoom(100)),
      listenMenu("menu-fit-window", () => setZoom("fit")),
      listenMenu("menu-about", () => {
        void invoke<AppInfo>("app_info")
          .then(setAboutInfo)
          .catch((reason) => reportFailure("Could not open About Utool.", "Open About Utool", {}, performance.now(), reason));
      }),
    ]);
    return () => { disposed = true; void pending.then((items) => { if (disposed) items.forEach((unlisten) => unlisten()); }); };
  }, [changeZoom, copyCurrent, copySvg, exportCurrent, exportSvg, openDocument, pasteDocument, reportFailure, setZoom, showExampleChooser, showServerDialog]);

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
    {aboutInfo && <AboutDialog info={aboutInfo} onClose={() => setAboutInfo(null)} />}
    {serverDialog && <ServerDialog info={serverDialog} starting={serverStarting} error={serverStartError} onStart={startServer} onClose={() => setServerDialog(null)} />}
    {exampleChooserOpen && <ExampleChooser
      examples={examples}
      selectedId={selectedExampleId}
      opening={exampleOpening}
      onSelect={setSelectedExampleId}
      onOpen={(id) => void openExample(id)}
      onClose={() => { if (!exampleOpening) setExampleChooserOpen(false); }}
    />}
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
      {document && activeVariant && <div className={`chart-view${activeView === "chart" ? "" : " chart-view-preload"}`} aria-hidden={activeView !== "chart"}>
        {filterRunning && <div className="pending-banner"><span className="small-spinner" />Computing {filterRunning}. Currently showing {activeVariant.name}.</div>}
        <ChartRules key={activeVariant.chart.chartId} chart={activeVariant.chart} />
        <div className="chart-bar"><span className="chart-stats"><b>{activeVariant.chart.stateCount}</b> states{activeVariant.chart.stateCount !== activeVariant.chart.subgraphCount && <> · <b>{activeVariant.chart.subgraphCount}</b> subgraphs</>} · <b>{activeVariant.chart.ruleCount}</b> rules · <strong>{activeVariant.chart.solutionCount} solutions</strong></span></div>
      </div>}
      {document && activeView === "solutions" && activeVariant && <div className="solutions-view">
        {filterRunning && <div className="pending-banner"><span className="small-spinner" />Computing {filterRunning}. Currently showing {activeVariant.name}.</div>}
        {solutionTotal === "0" ? <div className="zero-solutions"><h2>No solutions</h2><p>No solutions satisfy {activeVariant.name === "Unfiltered" ? "this graph" : activeVariant.name}.</p>{activeVariant.key !== "base" && <button onClick={() => selectVariant("base")}>Show unfiltered</button>}</div>
          : solution ? <><GraphCanvas key={`${activeVariant.key}-${solutionIndex}`} graph={solutionGraph(solution)} zoom={solutionZoom} draggable={false} onZoomChange={setSolutionZoom} onSvgReady={(element) => { svg.current = element; }} /><div className="solution-bar"><div className="solution-nav"><button title="Previous solution (Left Arrow)" aria-label="Previous solution" disabled={solutionIndex === 0 || solutionRunning} onClick={() => void loadSolution(activeVariant.chart, solutionIndex - 1)}>←</button><label><span>Solution</span><input value={solutionIndex + 1} onChange={(event) => { const value = Number(event.target.value); if (Number.isSafeInteger(value) && value > 0 && BigInt(value) <= BigInt(solutionTotal)) void loadSolution(activeVariant.chart, value - 1); }} /></label><span>of <b>{solutionTotal}</b></span><button title="Next solution (Right Arrow)" aria-label="Next solution" disabled={BigInt(solutionIndex + 1) >= BigInt(solutionTotal) || solutionRunning} onClick={() => void loadSolution(activeVariant.chart, solutionIndex + 1)}>→</button></div></div></>
          : <div className="computing"><span className="large-spinner" /><h2>Computing first solution</h2></div>}
      </div>}
    </section>
    <footer className="status-bar">
      <span className={`server-status ${serverStatus.state}`} title={serverStatus.tooltip} aria-label={serverStatus.tooltip}><i aria-hidden="true" /></span>
      <span className={`status-operation${status.running ? " busy" : ""}`}>{status.action}</span>
      <time>{status.elapsedMs === null ? (status.running ? "Running…" : "") : formatElapsed(status.elapsedMs)}</time>
    </footer>
  </main>;
}
