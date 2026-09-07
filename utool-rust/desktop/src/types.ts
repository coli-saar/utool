export type Point = { x: number; y: number };

export type GraphNode = {
  id: number;
  name: string;
  label: string | null;
  hole: boolean;
  x: number;
  y: number;
  width: number;
  height: number;
};

export type GraphEdge = {
  source: number;
  target: number;
  kind: "tree" | "dominance";
  points: Point[];
  light: boolean;
};

export type GraphView = {
  nodes: GraphNode[];
  edges: GraphEdge[];
  width: number;
  height: number;
};

export type SolutionView = {
  elapsedMs: number;
  nodes: Array<{ id: number; name: string; label: string }>;
  edges: Array<[number, number]>;
};

export type LoadedDocumentView = {
  documentId: number;
  title: string;
  graph: GraphView;
  elapsedMs: number;
};

export type StartupDocument = {
  input: string;
  codec: string;
  title: string;
  filename: string;
};

export type StartupFilter = {
  rewriteSystem: string;
  filename: string;
};

export type AppInfo = {
  version: string;
  buildId: string;
};

export type ServerStatus = {
  state: "stopped" | "starting" | "running" | "stopping" | "error";
  address: string | null;
  tooltip: string;
  notice: string | null;
};

export type ServerDialogInfo = {
  port: number;
  acceptNonLocal: boolean;
  localAddress: string;
  ethernetAddress: string;
};

export type ExampleSummary = {
  id: string;
  filename: string;
  codec: string;
  description: string;
};

export type EventEntry = {
  id: number;
  timestampMs: number;
  windowTitle: string;
  action: string;
  arguments: unknown;
  elapsedMs: number;
  status: "success" | "error";
  error: string | null;
};

export type ChartView = {
  chartId: number;
  elapsedMs: number;
  solutionCount: string;
  stateCount: number;
  subgraphCount: number;
  splitCount: number;
  displayRowCount: number;
  topFragments: string[];
  graph: GraphView | null;
};

export type ChartRule = {
  state: number;
  ordinal: number;
  fragment: string;
  assignments: Array<[string, string[]]>;
};

export type ChartState = {
  state: number;
  ruleCount: number;
  subgraph: string[];
  variant: number | null;
};

export type ChartRowPage = {
  start: number;
  total: number;
  states: ChartState[];
  rows: ChartRule[];
};
