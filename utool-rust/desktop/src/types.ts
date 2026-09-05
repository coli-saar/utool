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
  term: string;
  nodes: Array<{ id: number; name: string; label: string }>;
  edges: Array<[number, number]>;
};

export type LoadedDocumentView = {
  documentId: number;
  graph: GraphView;
};

export type ChartView = {
  chartId: number;
  solutionCount: string;
  stateCount: number;
  subgraphCount: number;
  splitCount: number;
  displayRowCount: number;
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
