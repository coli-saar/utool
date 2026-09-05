import { PointerEvent, useEffect, useMemo, useRef, useState } from "react";
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
  const drag = useRef<{ pointerId: number; members: number[]; start: Point; originals: Record<number, Point> } | null>(null);
  const pendingMove = useRef<{ members: number[]; originals: Record<number, Point>; dx: number; dy: number } | null>(null);
  const animationFrame = useRef<number | null>(null);
  const nodes = useMemo(() => new Map(graph.nodes.map((node) => [node.id, node])), [graph]);
  const fragmentOf = useMemo(() => fragments(graph), [graph]);
  const fragmentMembers = useMemo(() => {
    const unique = new Map<number, number[]>();
    graph.nodes.forEach((node) => {
      const members = fragmentOf.get(node.id) ?? [node.id];
      unique.set(Math.min(...members), members);
    });
    return Array.from(unique.values());
  }, [fragmentOf, graph.nodes]);

  useEffect(() => () => {
    if (animationFrame.current !== null) cancelAnimationFrame(animationFrame.current);
  }, []);

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
    event.preventDefault();
    const svg = svgRef.current!;
    svg.setPointerCapture(event.pointerId);
    svg.classList.add("dragging");
    const members = fragmentOf.get(id) ?? [id];
    drag.current = {
      pointerId: event.pointerId,
      members,
      start: svgPoint(event),
      originals: Object.fromEntries(members.map((member) => [member, offsets[member] ?? { x: 0, y: 0 }])),
    };
  };

  const commitPendingMove = () => {
    const pending = pendingMove.current;
    pendingMove.current = null;
    if (!pending) return;
    setOffsets((old) => ({
      ...old,
      ...Object.fromEntries(pending.members.map((id) => [id, {
        x: pending.originals[id].x + pending.dx,
        y: pending.originals[id].y + pending.dy,
      }])),
    }));
  };

  const moveDrag = (event: PointerEvent) => {
    if (!drag.current || drag.current.pointerId !== event.pointerId) return;
    event.preventDefault();
    const point = svgPoint(event);
    const current = drag.current;
    const dx = point.x - current.start.x;
    const dy = point.y - current.start.y;
    pendingMove.current = { members: current.members, originals: current.originals, dx, dy };
    if (animationFrame.current !== null) return;
    animationFrame.current = requestAnimationFrame(() => {
      animationFrame.current = null;
      commitPendingMove();
    });
  };

  const endDrag = (event: PointerEvent) => {
    if (!drag.current || drag.current.pointerId !== event.pointerId) return;
    const svg = svgRef.current;
    drag.current = null;
    if (animationFrame.current !== null) {
      cancelAnimationFrame(animationFrame.current);
      animationFrame.current = null;
    }
    commitPendingMove();
    svg?.classList.remove("dragging");
    if (svg?.hasPointerCapture(event.pointerId)) svg.releasePointerCapture(event.pointerId);
  };

  const lostPointerCapture = () => {
    drag.current = null;
    if (animationFrame.current !== null) {
      cancelAnimationFrame(animationFrame.current);
      animationFrame.current = null;
    }
    commitPendingMove();
    svgRef.current?.classList.remove("dragging");
  };

  const route = (sourceId: number, targetId: number) => {
    const source = nodes.get(sourceId)!;
    const target = nodes.get(targetId)!;
    const s = position(sourceId);
    const t = position(targetId);
    const start = { x: s.x + source.width / 2, y: s.y + source.height };
    const end = { x: t.x + target.width / 2, y: t.y };
    return `${start.x},${start.y} ${end.x},${end.y}`;
  };

  const factor = zoom / 100;
  const viewWidth = Math.max(graph.width / factor, 300);
  const viewHeight = Math.max(graph.height / factor, 220);
  const fragmentBoxes = fragmentMembers.map((members) => {
    const placed = members.map((id) => ({ node: nodes.get(id)!, at: position(id) }));
    const x = Math.min(...placed.map(({ at }) => at.x));
    const y = Math.min(...placed.map(({ at }) => at.y));
    const right = Math.max(...placed.map(({ node, at }) => at.x + node.width));
    const bottom = Math.max(...placed.map(({ node, at }) => at.y + node.height));
    return { members, x, y, width: right - x, height: bottom - y };
  }).sort((left, right) => right.width * right.height - left.width * left.height);
  return (
    <svg
      ref={(element) => { svgRef.current = element; onSvgReady?.(element); }}
      className="graph-canvas"
      viewBox={`${-(viewWidth - graph.width) / 2 - 20} ${-(viewHeight - graph.height) / 2 - 20} ${viewWidth} ${viewHeight}`}
      onPointerMove={moveDrag}
      onPointerUp={endDrag}
      onPointerCancel={endDrag}
      onLostPointerCapture={lostPointerCapture}
      onDragStart={(event) => event.preventDefault()}
      role="img"
      aria-label="Dominance graph"
    >
      <defs>
        <marker id="arrow" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto"><path d="M0,0 L8,4 L0,8 Z" /></marker>
      </defs>
      <g className="fragment-hitboxes" aria-hidden="true">
        {fragmentBoxes.map((box) => <rect
          key={box.members[0]}
          x={box.x}
          y={box.y}
          width={box.width}
          height={box.height}
          onPointerDown={(event) => beginDrag(event, box.members[0])}
        />)}
      </g>
      <g className="edges">
        {graph.edges.map((edge, index) => <polyline key={`${edge.source}-${edge.target}-${index}`} points={route(edge.source, edge.target)} className={`${edge.kind} ${edge.light ? "light" : ""}`} markerEnd={edge.kind === "dominance" ? "url(#arrow)" : undefined} />)}
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
