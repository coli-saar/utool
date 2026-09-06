import { PointerEvent, WheelEvent, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import type { GraphView, Point } from "./types";

export type Zoom = number | "fit";

type Props = {
  graph: GraphView;
  zoom: Zoom;
  offsets?: Record<number, Point>;
  draggable?: boolean;
  onOffsetsChange?: (offsets: Record<number, Point>) => void;
  onZoomChange?: (zoom: Zoom) => void;
  onSvgReady?: (svg: SVGSVGElement | null) => void;
};

const MIN_ZOOM = 25;
const MAX_ZOOM = 400;
const PADDING = 30;

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

export function GraphCanvas({ graph, zoom, offsets = {}, draggable = true, onOffsetsChange, onZoomChange, onSvgReady }: Props) {
  const viewportRef = useRef<HTMLDivElement | null>(null);
  const svgRef = useRef<SVGSVGElement | null>(null);
  const [viewportSize, setViewportSize] = useState({ width: 0, height: 0 });
  const drag = useRef<{ pointerId: number; members: number[]; start: Point; originals: Record<number, Point> } | null>(null);
  const pendingMove = useRef<{ members: number[]; originals: Record<number, Point>; dx: number; dy: number } | null>(null);
  const animationFrame = useRef<number | null>(null);
  const previousFrame = useRef<{ x: number; y: number; factor: number; viewportWidth: number; viewportHeight: number } | null>(null);
  const previousGraph = useRef(graph);
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

  useLayoutEffect(() => {
    const viewport = viewportRef.current;
    if (!viewport) return;
    const update = () => setViewportSize({ width: viewport.clientWidth, height: viewport.clientHeight });
    const observer = new ResizeObserver(update);
    observer.observe(viewport);
    update();
    return () => observer.disconnect();
  }, []);

  useEffect(() => () => {
    if (animationFrame.current !== null) cancelAnimationFrame(animationFrame.current);
  }, []);

  const position = (id: number) => {
    const node = nodes.get(id)!;
    const offset = offsets[id] ?? { x: 0, y: 0 };
    return { x: node.x + offset.x, y: node.y + offset.y };
  };

  const bounds = useMemo(() => {
    const placed = graph.nodes.map((node) => {
      const offset = offsets[node.id] ?? { x: 0, y: 0 };
      return { node, x: node.x + offset.x, y: node.y + offset.y };
    });
    if (!placed.length) return { x: 0, y: 0, width: Math.max(graph.width, 1), height: Math.max(graph.height, 1) };
    const left = Math.min(...placed.map(({ x }) => x));
    const top = Math.min(...placed.map(({ y }) => y));
    const right = Math.max(...placed.map(({ node, x }) => x + node.width));
    const bottom = Math.max(...placed.map(({ node, y }) => y + node.height));
    return { x: left - PADDING, y: top - PADDING, width: right - left + PADDING * 2, height: bottom - top + PADDING * 2 };
  }, [graph, offsets]);

  const fitFactor = Math.min(1,
    viewportSize.width > 0 ? viewportSize.width / bounds.width : 1,
    viewportSize.height > 0 ? viewportSize.height / bounds.height : 1,
  );
  const factor = zoom === "fit" ? fitFactor : zoom / 100;
  const viewportWorldWidth = viewportSize.width / factor;
  const viewportWorldHeight = viewportSize.height / factor;
  const viewWidth = Math.max(bounds.width, viewportWorldWidth);
  const viewHeight = Math.max(bounds.height, viewportWorldHeight);
  const viewX = bounds.x - Math.max(0, viewportWorldWidth - bounds.width) / 2;
  const viewY = bounds.y - Math.max(0, viewportWorldHeight - bounds.height) / 2;
  const canvasWidth = viewWidth * factor;
  const canvasHeight = viewHeight * factor;

  useLayoutEffect(() => {
    const viewport = viewportRef.current;
    const previous = previousFrame.current;
    const graphChanged = previousGraph.current !== graph;
    const viewportUnchanged = previous?.viewportWidth === viewportSize.width && previous.viewportHeight === viewportSize.height;
    if (viewport && graphChanged) {
      if (canvasWidth > viewportSize.width) viewport.scrollLeft = 0;
    } else if (viewport && previous && viewportUnchanged && Math.abs(previous.factor - factor) < 0.0001) {
      viewport.scrollLeft += (previous.x - viewX) * factor;
      viewport.scrollTop += (previous.y - viewY) * factor;
    }
    previousGraph.current = graph;
    previousFrame.current = { x: viewX, y: viewY, factor, viewportWidth: viewportSize.width, viewportHeight: viewportSize.height };
  }, [canvasWidth, factor, graph, viewX, viewY, viewportSize.height, viewportSize.width]);

  const svgPoint = (event: PointerEvent) => {
    const svg = svgRef.current!;
    const point = svg.createSVGPoint();
    point.x = event.clientX;
    point.y = event.clientY;
    return point.matrixTransform(svg.getScreenCTM()!.inverse());
  };

  const beginDrag = (event: PointerEvent, id: number) => {
    if (!draggable || event.button !== 0) return;
    event.preventDefault();
    if (zoom === "fit") onZoomChange?.(factor * 100);
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
    onOffsetsChange?.({
      ...offsets,
      ...Object.fromEntries(pending.members.map((id) => [id, {
        x: pending.originals[id].x + pending.dx,
        y: pending.originals[id].y + pending.dy,
      }])),
    });
  };

  const moveDrag = (event: PointerEvent) => {
    if (!drag.current || drag.current.pointerId !== event.pointerId) return;
    event.preventDefault();
    const point = svgPoint(event);
    const current = drag.current;
    pendingMove.current = { members: current.members, originals: current.originals, dx: point.x - current.start.x, dy: point.y - current.start.y };
    if (animationFrame.current !== null) return;
    animationFrame.current = requestAnimationFrame(() => {
      animationFrame.current = null;
      commitPendingMove();
    });
  };

  const finishDrag = (event: PointerEvent) => {
    if (!drag.current || drag.current.pointerId !== event.pointerId) return;
    drag.current = null;
    if (animationFrame.current !== null) {
      cancelAnimationFrame(animationFrame.current);
      animationFrame.current = null;
    }
    commitPendingMove();
    svgRef.current?.classList.remove("dragging");
    if (svgRef.current?.hasPointerCapture(event.pointerId)) svgRef.current.releasePointerCapture(event.pointerId);
  };

  const onWheel = (event: WheelEvent<HTMLDivElement>) => {
    if (!(event.ctrlKey || event.metaKey) || !onZoomChange) return;
    event.preventDefault();
    const current = factor * 100;
    const next = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, current * Math.exp(-event.deltaY * 0.002)));
    onZoomChange(Math.round(next));
  };

  const route = (sourceId: number, targetId: number) => {
    const source = nodes.get(sourceId)!;
    const target = nodes.get(targetId)!;
    const s = position(sourceId);
    const t = position(targetId);
    return `${s.x + source.width / 2},${s.y + source.height} ${t.x + target.width / 2},${t.y}`;
  };

  const fragmentBoxes = fragmentMembers.map((members) => {
    const placed = members.map((id) => ({ node: nodes.get(id)!, at: position(id) }));
    const x = Math.min(...placed.map(({ at }) => at.x));
    const y = Math.min(...placed.map(({ at }) => at.y));
    const right = Math.max(...placed.map(({ node, at }) => at.x + node.width));
    const bottom = Math.max(...placed.map(({ node, at }) => at.y + node.height));
    return { members, x, y, width: right - x, height: bottom - y };
  }).sort((left, right) => right.width * right.height - left.width * left.height);

  const layoutReady = viewportSize.width > 0 && viewportSize.height > 0;

  return <div ref={viewportRef} className={`graph-viewport${layoutReady ? " layout-ready" : ""}`} onWheel={onWheel}>
    <svg
      ref={(element) => { svgRef.current = element; onSvgReady?.(element); }}
      xmlns="http://www.w3.org/2000/svg"
      className={`graph-canvas${draggable ? " draggable" : ""}`}
      style={{ width: canvasWidth, height: canvasHeight }}
      viewBox={`${viewX} ${viewY} ${viewWidth} ${viewHeight}`}
      onPointerMove={moveDrag}
      onPointerUp={finishDrag}
      onPointerCancel={finishDrag}
      onLostPointerCapture={() => { drag.current = null; commitPendingMove(); svgRef.current?.classList.remove("dragging"); }}
      onDragStart={(event) => event.preventDefault()}
      role="img"
      aria-label="Dominance graph"
    >
      <defs>
        <style>{`
          .graph-canvas { background: #fff; }
          .fragment-hitboxes rect { fill: transparent; }
          .edges polyline { fill: none; stroke: #343b45; stroke-width: 1.35; vector-effect: non-scaling-stroke; }
          .edges .dominance { stroke: #df303b; stroke-dasharray: 3 3; }
          .edges .light { opacity: .3; }
          marker path { fill: #df303b; }
          .node rect { fill: #fff; stroke: #384557; stroke-width: 1.35; vector-effect: non-scaling-stroke; }
          .node.hole rect { fill: #fff; stroke: #b8c0ca; stroke-width: .9; }
          .node text { text-anchor: middle; font: 12px sans-serif; }
        `}</style>
        <marker id="arrow" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto"><path d="M0,0 L8,4 L0,8 Z" /></marker>
      </defs>
      {draggable && <g className="fragment-hitboxes" aria-hidden="true">
        {fragmentBoxes.map((box) => <rect key={box.members[0]} x={box.x} y={box.y} width={box.width} height={box.height} onPointerDown={(event) => beginDrag(event, box.members[0])} />)}
      </g>}
      <g className="edges">
        {graph.edges.map((edge, index) => <polyline key={`${edge.source}-${edge.target}-${index}`} points={route(edge.source, edge.target)} className={`${edge.kind} ${edge.light ? "light" : ""}`} markerEnd={edge.kind === "dominance" ? "url(#arrow)" : undefined} />)}
      </g>
      <g className="nodes">
        {graph.nodes.map((node) => {
          const at = position(node.id);
          return <g key={node.id} transform={`translate(${at.x} ${at.y})`} className={node.hole ? "node hole" : "node"} onPointerDown={(event) => beginDrag(event, node.id)}>
            <rect width={node.width} height={node.height} rx={node.hole ? 17 : 5} />
            <text x={node.width / 2} y={node.height / 2 + 5}>{node.label ?? node.name}</text>
            <title>{draggable ? `${node.name} — drag fragment` : node.name}</title>
          </g>;
        })}
      </g>
    </svg>
  </div>;
}
