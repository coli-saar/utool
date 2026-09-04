import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import { open, save } from "@tauri-apps/plugin-dialog";
import { readTextFile, writeTextFile } from "@tauri-apps/plugin-fs";
import { useCallback, useEffect, useRef, useState } from "react";
import { GraphCanvas } from "./GraphCanvas";
import type { ChartRule, ChartView, GraphView, LoadedDocumentView, SolutionView } from "./types";

const EXAMPLE = `[label(x f(x1)) label(y g(y1)) label(z a) dom(x1 z) dom(y1 z) dom(y x1)]`;
const ZOOMS = [25, 33, 50, 67, 75, 100, 125, 150];

type GraphTab = { key: string; kind: "graph"; title: string; documentId: number; graph: GraphView; zoom: number };
type ChartTab = { key: string; kind: "chart"; title: string; sourceTitle: string; documentId: number; chart: ChartView };
type SolutionTab = { key: string; kind: "solution"; title: string; sourceTitle: string; documentId: number; chartId: number; solution: SolutionView; index: number; total: string; zoom: number };
type Tab = GraphTab | ChartTab | SolutionTab;
type ActionStatus = { action: string; elapsedMs: number | null; running: boolean };

function solutionGraph(solution: SolutionView): GraphView {
  const children = new Map<number, number[]>();
  const incoming = new Set<number>();
  solution.edges.forEach(([from, to]) => { children.set(from, [...(children.get(from) ?? []), to]); incoming.add(to); });
  const positions = new Map<number, { x: number; y: number }>();
  let leaf = 0;
  const place = (id: number, depth: number): number => {
    const descendants = children.get(id) ?? [];
    if (!descendants.length) { const x = leaf++ * 100; positions.set(id, { x, y: depth * 85 }); return x; }
    const xs = descendants.map((child) => place(child, depth + 1));
    const x = (xs[0] + xs[xs.length - 1]) / 2;
    positions.set(id, { x, y: depth * 85 });
    return x;
  };
  solution.nodes.filter((node) => !incoming.has(node.id)).forEach((root) => place(root.id, 0));
  const nodes = solution.nodes.map((node) => ({ ...node, hole: false, x: positions.get(node.id)?.x ?? 0, y: positions.get(node.id)?.y ?? 0, width: Math.max(54, node.label.length * 8 + 28), height: 34 }));
  return {
    nodes,
    edges: solution.edges.map(([source, target]) => ({ source, target, kind: "tree", points: [], light: false })),
    width: Math.max(220, leaf * 100),
    height: Math.max(160, ...nodes.map((node) => node.y + 70)),
  };
}

function SplitRuleView({ rule }: { rule: ChartRule }) {
  return <span className="split-rule">
    <b>⟨{rule.root}</b>
    {rule.attachments.map(([dominator, subgraph], index) => <span key={`${dominator}-${index}`}> {dominator} ↦ <i>[{subgraph.join(", ")}]</i></span>)}
    {rule.substitutions.map(([hole, root], index) => <span key={`subst-${hole}-${index}`}> {hole} := {root}</span>)}
    <b>⟩</b>
  </span>;
}

function ChartRules({ chart }: { chart: ChartView }) {
  const groups = new Map<string, ChartRule[]>();
  chart.rules.forEach((rule) => {
    const key = rule.subgraph.join("\u001f");
    groups.set(key, [...(groups.get(key) ?? []), rule]);
  });
  return <div className="chart-table-wrap"><table className="chart-table">
    <thead><tr><th>Subgraph</th><th>#</th><th>Split rule</th></tr></thead>
    <tbody>{[...groups.values()].flatMap((rules) => rules.map((rule, index) => <tr key={`${rule.subgraph.join("-")}-${index}`}>
      {index === 0 && <td rowSpan={rules.length}>[{rule.subgraph.join(", ")}]</td>}
      <td>{index + 1}</td><td><SplitRuleView rule={rule} /></td>
    </tr>))}</tbody>
  </table>{chart.rules.length === 0 && <div className="empty-chart">The chart contains no productive split rules.</div>}</div>;
}

export default function App() {
  const [tabs, setTabs] = useState<Tab[]>([]);
  const [activeKey, setActiveKey] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState<ActionStatus>({ action: "Ready", elapsedMs: null, running: false });
  const svg = useRef<SVGSVGElement | null>(null);
  const serial = useRef(0);
  const operation = useRef(0);
  const actionStarted = useRef(0);
  const active = tabs.find((tab) => tab.key === activeKey);

  const addGraph = useCallback(async (input: string, codec: string, title: string, startedAt = performance.now()) => {
    setStatus({ action: `Opening ${title}`, elapsedMs: null, running: true });
    setError(null);
    try {
      const loaded = await invoke<LoadedDocumentView>("load_document", { input, codec });
      const tab: GraphTab = { key: `graph-${++serial.current}`, kind: "graph", title, documentId: loaded.documentId, graph: loaded.graph, zoom: 50 };
      setTabs((old) => [...old, tab]);
      setActiveKey(tab.key);
      setStatus({ action: `Opened ${title}`, elapsedMs: performance.now() - startedAt, running: false });
    } catch (reason) {
      setError(String(reason));
      setStatus({ action: `Opening ${title} failed`, elapsedMs: performance.now() - startedAt, running: false });
    }
  }, []);

  const openDocument = useCallback(async () => {
    const selected = await open({ multiple: false, filters: [{ name: "Dominance graphs", extensions: ["clls", "oz", "pl", "txt"] }] });
    if (!selected) return;
    const startedAt = performance.now();
    actionStarted.current = startedAt;
    const title = selected.split(/[\\/]/).pop() ?? "Graph";
    setStatus({ action: `Opening ${title}`, elapsedMs: null, running: true });
    try {
      await addGraph(await readTextFile(selected), selected.toLowerCase().endsWith(".pl") ? "holesem" : "domcon-oz", title, startedAt);
    } catch (reason) {
      setError(String(reason));
      setStatus({ action: `Opening ${title} failed`, elapsedMs: performance.now() - startedAt, running: false });
    }
  }, [addGraph]);

  const buildChart = useCallback(async () => {
    const graphTab = tabs.find((tab) => tab.key === activeKey);
    if (!graphTab || graphTab.kind !== "graph") return;
    const token = ++operation.current;
    const startedAt = performance.now();
    actionStarted.current = startedAt;
    setBusy(true); setError(null);
    setStatus({ action: "Computing chart", elapsedMs: null, running: true });
    try {
      const chart = await invoke<ChartView>("build_chart", { documentId: graphTab.documentId });
      if (operation.current !== token) return;
      const tab: ChartTab = { key: `chart-${++serial.current}`, kind: "chart", title: `${graphTab.title} Chart`, sourceTitle: graphTab.title, documentId: graphTab.documentId, chart };
      setTabs((old) => [...old, tab]); setActiveKey(tab.key);
      setStatus({ action: "Computed chart", elapsedMs: performance.now() - startedAt, running: false });
    } catch (reason) { if (operation.current === token) { setError(String(reason)); setStatus({ action: "Computing chart failed", elapsedMs: performance.now() - startedAt, running: false }); } }
    finally { if (operation.current === token) setBusy(false); }
  }, [activeKey, tabs]);

  const showFirstSolution = useCallback(async () => {
    const chartTab = tabs.find((tab) => tab.key === activeKey);
    if (!chartTab || chartTab.kind !== "chart" || chartTab.chart.solutionCount === "0") return;
    const startedAt = performance.now();
    actionStarted.current = startedAt;
    setStatus({ action: "Enumerating Solution 1", elapsedMs: null, running: true });
    setError(null);
    try {
      const solution = await invoke<SolutionView | null>("solution_at", { chartId: chartTab.chart.chartId, index: 0 });
      if (!solution) return;
      const tab: SolutionTab = { key: `solution-${++serial.current}`, kind: "solution", title: `${chartTab.sourceTitle} SF #1`, sourceTitle: chartTab.sourceTitle, documentId: chartTab.documentId, chartId: chartTab.chart.chartId, solution, index: 0, total: chartTab.chart.solutionCount, zoom: 50 };
      setTabs((old) => [...old, tab]); setActiveKey(tab.key);
      setStatus({ action: "Enumerated Solution 1", elapsedMs: performance.now() - startedAt, running: false });
    } catch (reason) { setError(String(reason)); setStatus({ action: "Enumerating Solution failed", elapsedMs: performance.now() - startedAt, running: false }); }
  }, [activeKey, tabs]);

  const showSolution = async (nextIndex: number) => {
    if (!active || active.kind !== "solution") return;
    const startedAt = performance.now();
    actionStarted.current = startedAt;
    setStatus({ action: `Enumerating Solution ${nextIndex + 1}`, elapsedMs: null, running: true });
    try {
      const solution = await invoke<SolutionView | null>("solution_at", { chartId: active.chartId, index: nextIndex });
      if (!solution) return;
      setTabs((old) => old.map((tab) => tab.key === active.key ? { ...active, solution, index: nextIndex, title: `${active.sourceTitle} SF #${nextIndex + 1}` } : tab));
      setStatus({ action: `Enumerated Solution ${nextIndex + 1}`, elapsedMs: performance.now() - startedAt, running: false });
    } catch (reason) { setError(String(reason)); setStatus({ action: "Enumerating Solution failed", elapsedMs: performance.now() - startedAt, running: false }); }
  };

  const filterActiveChart = useCallback(async () => {
    const chartTab = tabs.find((tab) => tab.key === activeKey);
    if (!chartTab || chartTab.kind !== "chart") return;
    const selected = await open({ multiple: false, filters: [{ name: "Utool rewrite systems", extensions: ["rew", "rules", "txt"] }] });
    if (!selected) return;
    const token = ++operation.current;
    const startedAt = performance.now();
    actionStarted.current = startedAt;
    setBusy(true); setError(null); setStatus({ action: "Filtering chart", elapsedMs: null, running: true });
    try {
      const chart = await invoke<ChartView>("filter_chart_command", { chartId: chartTab.chart.chartId, rewriteSystem: await readTextFile(selected) });
      if (operation.current !== token) return;
      const tab: ChartTab = { key: `chart-${++serial.current}`, kind: "chart", title: `${chartTab.sourceTitle} Filtered Chart`, sourceTitle: chartTab.sourceTitle, documentId: chartTab.documentId, chart };
      setTabs((old) => [...old, tab]); setActiveKey(tab.key);
      setStatus({ action: "Filtered chart", elapsedMs: performance.now() - startedAt, running: false });
    } catch (reason) { if (operation.current === token) { setError(String(reason)); setStatus({ action: "Filtering chart failed", elapsedMs: performance.now() - startedAt, running: false }); } }
    finally { if (operation.current === token) setBusy(false); }
  }, [activeKey, tabs]);

  const setZoom = (zoom: number) => {
    if (!active || active.kind === "chart") return;
    setTabs((old) => old.map((tab) => tab.key === active.key ? { ...tab, zoom } : tab));
  };

  const closeTab = (key: string) => {
    setTabs((old) => {
      const index = old.findIndex((tab) => tab.key === key);
      const next = old.filter((tab) => tab.key !== key);
      if (key === activeKey) setActiveKey(next[Math.min(index, next.length - 1)]?.key ?? "");
      return next;
    });
  };

  const exportSvg = useCallback(async () => {
    if (!svg.current) return;
    const selected = await save({ defaultPath: `${active?.title ?? "utool-graph"}.svg`, filters: [{ name: "SVG image", extensions: ["svg"] }] });
    if (selected) await writeTextFile(selected, `<?xml version="1.0" encoding="UTF-8"?>\n${svg.current.outerHTML}`);
  }, [active?.title]);

  const exportGraph = useCallback(async (format: "domcon" | "dot") => {
    if (!active) return;
    const extension = format === "dot" ? "dot" : "clls";
    const selected = await save({ defaultPath: `${active.title}.${extension}`, filters: [{ name: format === "dot" ? "Graphviz DOT" : "Domcon/Oz", extensions: [extension] }] });
    if (!selected) return;
    const startedAt = performance.now();
    setStatus({ action: `Exporting ${format}`, elapsedMs: null, running: true });
    try {
      const text = await invoke<string>("export_document", { documentId: active.documentId, format });
      await writeTextFile(selected, text);
      setStatus({ action: `Exported ${format}`, elapsedMs: performance.now() - startedAt, running: false });
    } catch (reason) { setError(String(reason)); setStatus({ action: `Exporting ${format} failed`, elapsedMs: performance.now() - startedAt, running: false }); }
  }, [active]);

  useEffect(() => { void addGraph(EXAMPLE, "domcon-oz", "Example"); }, [addGraph]);
  useEffect(() => {
    let disposed = false;
    const pending = Promise.all([
      listen("menu-open", openDocument), listen("menu-export-svg", exportSvg),
      listen("menu-export-domcon", () => exportGraph("domcon")), listen("menu-export-dot", () => exportGraph("dot")),
      listen("menu-build-chart", buildChart), listen("menu-show-solution", showFirstSolution),
      listen("menu-filter-chart", filterActiveChart),
      listen("menu-about", () => setError("Utool Rust — HNC dominance graph solving with rusty-alto.")),
    ]);
    return () => { disposed = true; void pending.then((items) => { if (disposed) items.forEach((unlisten) => unlisten()); }); };
  }, [buildChart, exportGraph, exportSvg, filterActiveChart, openDocument, showFirstSolution]);

  return <main>
    <header>
      <h1>Utool</h1>
      <div className="toolbar">
        {active?.kind === "graph" && (!busy ? <button className="primary" onClick={buildChart}>Build Chart</button> : <button onClick={() => { operation.current++; setBusy(false); setStatus({ action: "Chart construction cancelled", elapsedMs: performance.now() - actionStarted.current, running: false }); void invoke("cancel_chart"); }}>Cancel</button>)}
        {active?.kind === "chart" && <><button disabled={busy} onClick={filterActiveChart}>Filter Chart…</button><button className="primary" disabled={active.chart.solutionCount === "0" || busy} onClick={showFirstSolution}>Show First Solution</button></>}
        {active && active.kind !== "chart" && <label className="zoom">Zoom <select value={active.zoom} onChange={(event) => setZoom(Number(event.target.value))}>{ZOOMS.map((value) => <option key={value} value={value}>{value}%</option>)}</select></label>}
        <button onClick={exportSvg} disabled={!active || active.kind === "chart"}>Export SVG…</button>
      </div>
    </header>
    <nav className="tabs">{tabs.map((tab) => <button key={tab.key} className={tab.key === activeKey ? "active" : ""} onClick={() => setActiveKey(tab.key)}><span>{tab.title}</span><i onClick={(event) => { event.stopPropagation(); closeTab(tab.key); }}>×</i></button>)}</nav>
    {error && <div className="error-banner" onClick={() => setError(null)}>{error}</div>}
    <section className="document">
      {!active && <div className="welcome"><h2>No graph open</h2><p>Choose File → Open… to open a dominance graph.</p></div>}
      {active?.kind === "graph" && <GraphCanvas key={active.key} graph={active.graph} zoom={active.zoom} onSvgReady={(element) => { svg.current = element; }} />}
      {active?.kind === "chart" && <div className="chart-view"><div className="chart-header"><span><b>{active.chart.stateCount}</b> subgraphs · <b>{active.chart.splitCount}</b> split rules · <b>{active.chart.solutionCount}</b> Solutions</span><button className="primary" disabled={active.chart.solutionCount === "0"} onClick={showFirstSolution}>Show First Solution</button></div><ChartRules chart={active.chart} /></div>}
      {active?.kind === "solution" && <><GraphCanvas key={active.key} graph={solutionGraph(active.solution)} zoom={active.zoom} onSvgReady={(element) => { svg.current = element; }} /><div className="solution-bar"><b>Solved form</b><button disabled={active.index === 0} onClick={() => showSolution(active.index - 1)}>←</button><input value={active.index + 1} onChange={(event) => { const value = Number(event.target.value); if (value > 0 && BigInt(value) <= BigInt(active.total)) void showSolution(value - 1); }} /><button disabled={BigInt(active.index + 1) >= BigInt(active.total)} onClick={() => showSolution(active.index + 1)}>→</button><span>of {active.total} (Graph: {active.sourceTitle})</span><code>{active.solution.term}</code></div></>}
    </section>
    <footer className="status-bar"><span className={status.running ? "busy" : ""}>{status.action}</span><time>{status.elapsedMs === null ? (status.running ? "Running…" : "") : status.elapsedMs < 1000 ? `${status.elapsedMs.toFixed(1)} ms` : `${(status.elapsedMs / 1000).toFixed(3)} s`}</time></footer>
  </main>;
}
