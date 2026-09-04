import { PointerEvent, useMemo, useRef, useState } from "react";
import type { GraphView, Point } from "./types";

type Props = {
  graph: GraphView;
  zoom: number;
  onSvgReady?: (svg: SVGSVGElement | null) => void;
};

function fragments(graph: GraphView): Map<number, number[]> {
  const parent = new Map(graph.nodes.map((node) => [node.id, node.id]));
  const find = (id: number): number => {
    const up = parent.get(id)!;
    if (up === id) return id;
    const root = find(up);
    parent.set(id, root);
    return root;
  };
  const union = (left: number, right: number) => {
    const a = find(left);
    const b = find(right);
    if (a !== b) parent.set(b, a);
  };
  graph.edges.filter((edge) => edge.kind === "tree").forEach((edge) => union(edge.source, edge.target));
  const groups = new Map<number, number[]>();
  graph.nodes.forEach((node) => {
    const root = find(node.id);
    groups.set(root, [...(groups.get(root) ?? []), node.id]);
  });
  const byNode = new Map<number, number[]>();
  groups.forEach((members) => members.forEach((id) => byNode.set(id, members)));
  return byNode;
}

export function GraphCanvas({ graph, zoom, onSvgReady }: Props) {
  const svgRef = useRef<SVGSVGElement | null>(null);
  const [offsets, setOffsets] = useState<Record<number, Point>>({});
  const drag = useRef<{ members: number[]; start: Point; originals: Record<number, Point> } | null>(null);
  const nodes = useMemo(() => new Map(graph.nodes.map((node) => [node.id, node])), [graph]);
  const fragmentOf = useMemo(() => fragments(graph), [graph]);

  const position = (id: number) => {
    const node = nodes.get(id)!;
    const offset = offsets[id] ?? { x: 0, y: 0 };
    return { x: node.x + offset.x, y: node.y + offset.y };
  };

  const svgPoint = (event: PointerEvent) => {
    const svg = svgRef.current!;
    const point = svg.createSVGPoint();
    point.x = event.clientX;
    point.y = event.clientY;
    return point.matrixTransform(svg.getScreenCTM()!.inverse());
  };

  const beginDrag = (event: PointerEvent, id: number) => {
    event.currentTarget.setPointerCapture(event.pointerId);
    const members = fragmentOf.get(id) ?? [id];
    drag.current = {
      members,
      start: svgPoint(event),
      originals: Object.fromEntries(members.map((member) => [member, offsets[member] ?? { x: 0, y: 0 }])),
    };
  };

  const moveDrag = (event: PointerEvent) => {
    if (!drag.current) return;
    const point = svgPoint(event);
    const current = drag.current;
    const dx = point.x - current.start.x;
    const dy = point.y - current.start.y;
    setOffsets((old) => ({
      ...old,
      ...Object.fromEntries(current.members.map((id) => [id, {
        x: current.originals[id].x + dx,
        y: current.originals[id].y + dy,
      }])),
    }));
  };

  const route = (sourceId: number, targetId: number, kind: string) => {
    const source = nodes.get(sourceId)!;
    const target = nodes.get(targetId)!;
    const s = position(sourceId);
    const t = position(targetId);
    const start = { x: s.x + source.width / 2, y: s.y + source.height };
    const end = { x: t.x + target.width / 2, y: t.y };
    if (kind === "tree") {
      const middle = (start.y + end.y) / 2;
      return `${start.x},${start.y} ${start.x},${middle} ${end.x},${middle} ${end.x},${end.y}`;
    }
    return `${start.x},${start.y} ${end.x},${end.y}`;
  };

  const factor = zoom / 100;
  const viewWidth = Math.max(graph.width / factor, 300);
  const viewHeight = Math.max(graph.height / factor, 220);
  return (
    <svg
      ref={(element) => { svgRef.current = element; onSvgReady?.(element); }}
      className="graph-canvas"
      viewBox={`${-(viewWidth - graph.width) / 2 - 20} ${-(viewHeight - graph.height) / 2 - 20} ${viewWidth} ${viewHeight}`}
      onPointerMove={moveDrag}
      onPointerUp={() => { drag.current = null; }}
      onPointerCancel={() => { drag.current = null; }}
      role="img"
      aria-label="Dominance graph"
    >
      <defs>
        <marker id="arrow" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto"><path d="M0,0 L8,4 L0,8 Z" /></marker>
      </defs>
      <g className="edges">
        {graph.edges.map((edge, index) => <polyline key={`${edge.source}-${edge.target}-${index}`} points={route(edge.source, edge.target, edge.kind)} className={`${edge.kind} ${edge.light ? "light" : ""}`} markerEnd={edge.kind === "dominance" ? "url(#arrow)" : undefined} />)}
      </g>
      <g className="nodes">
        {graph.nodes.map((node) => {
          const at = position(node.id);
          return <g key={node.id} transform={`translate(${at.x} ${at.y})`} className={node.hole ? "node hole" : "node"} onPointerDown={(event) => beginDrag(event, node.id)}>
            <rect width={node.width} height={node.height} rx={node.hole ? 17 : 5} />
            <text x={node.width / 2} y={node.height / 2 + 5}>{node.label ?? node.name}</text>
            <title>{node.name} — drag fragment</title>
          </g>;
        })}
      </g>
    </svg>
  );
}
