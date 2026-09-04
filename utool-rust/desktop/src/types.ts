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
  splitCount: number;
  rules: ChartRule[];
};

export type ChartRule = {
  subgraph: string[];
  root: string;
  attachments: Array<[string, string[]]>;
  substitutions: Array<[string, string]>;
};
