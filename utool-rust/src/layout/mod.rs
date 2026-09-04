//! Renderer-neutral graph layout.

use crate::graph::{HncGraph, NodeId};
use serde::{Deserialize, Serialize};
use std::collections::{BTreeMap, HashMap, HashSet, VecDeque};
use thiserror::Error;

/// Two-dimensional point.
#[derive(Clone, Copy, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct Point {
    /// Horizontal coordinate.
    pub x: f32,
    /// Vertical coordinate.
    pub y: f32,
}

/// Width and height.
#[derive(Clone, Copy, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct Size {
    /// Width.
    pub width: f32,
    /// Height.
    pub height: f32,
}

/// Positioned node rectangle.
#[derive(Clone, Copy, Debug, PartialEq, Serialize, Deserialize)]
pub struct NodeBox {
    /// Graph node.
    pub node: NodeId,
    /// Top-left corner.
    pub origin: Point,
    /// Measured size.
    pub size: Size,
}

/// Edge appearance class.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub enum EdgeKind {
    /// Ordered fragment-tree edge.
    Tree,
    /// Dominance constraint.
    Dominance,
}

/// Routed edge polyline.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
pub struct LayoutEdge {
    /// Source node.
    pub source: NodeId,
    /// Target node.
    pub target: NodeId,
    /// Edge class.
    pub kind: EdgeKind,
    /// Renderer-neutral polyline.
    pub points: Vec<Point>,
    /// Whether the renderer should de-emphasize the edge.
    pub light: bool,
}

/// Complete graph layout.
#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct Layout {
    /// Positioned nodes.
    pub nodes: Vec<NodeBox>,
    /// Routed edges.
    pub edges: Vec<LayoutEdge>,
    /// Total drawing extent.
    pub size: Size,
}

/// Layout spacing parameters.
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct LayoutOptions {
    /// Horizontal space between sibling nodes.
    pub node_x_gap: f32,
    /// Vertical space between tree levels.
    pub node_y_gap: f32,
    /// Horizontal space between fragments.
    pub fragment_x_gap: f32,
    /// Vertical space reserved between fragment levels.
    pub fragment_y_gap: f32,
}

impl Default for LayoutOptions {
    fn default() -> Self {
        Self {
            node_x_gap: 15.0,
            node_y_gap: 15.0,
            fragment_x_gap: 30.0,
            fragment_y_gap: 75.0,
        }
    }
}

/// Invalid size input or graph/layout mismatch.
#[derive(Clone, Debug, Error, PartialEq)]
pub enum LayoutError {
    /// Missing measured size.
    #[error("no measured size for node {0:?}")]
    MissingNodeSize(NodeId),
}

/// Lay out every fragment as an ordered tree, then arrange the fragment graph
/// in dominance-distance layers.
pub fn layout_graph(
    graph: &HncGraph,
    measured_sizes: &[(NodeId, Size)],
    options: LayoutOptions,
) -> Result<Layout, LayoutError> {
    let sizes: HashMap<_, _> = measured_sizes.iter().copied().collect();
    for index in 0..graph.parsed().nodes().len() {
        let node = NodeId::from_index(index);
        if !sizes.contains_key(&node) {
            return Err(LayoutError::MissingNodeSize(node));
        }
    }

    let mut fragment_of = vec![None; graph.parsed().nodes().len()];
    for &root in graph.roots() {
        mark_fragment(graph, root, root, &mut fragment_of);
    }

    let mut adjacency: HashMap<NodeId, Vec<NodeId>> = HashMap::new();
    let mut incoming: HashMap<NodeId, usize> = HashMap::new();
    for &root in graph.roots() {
        adjacency.entry(root).or_default();
        incoming.entry(root).or_default();
    }
    for &(source, target) in graph.parsed().dominance_edges() {
        let source_fragment =
            fragment_of[source.index()].expect("every node belongs to a fragment");
        let target_fragment =
            fragment_of[target.index()].expect("every node belongs to a fragment");
        if source_fragment != target_fragment
            && !adjacency
                .entry(source_fragment)
                .or_default()
                .contains(&target_fragment)
        {
            adjacency
                .entry(source_fragment)
                .or_default()
                .push(target_fragment);
            adjacency
                .entry(target_fragment)
                .or_default()
                .push(source_fragment);
            *incoming.entry(target_fragment).or_default() += 1;
        }
    }

    let mut starts: Vec<_> = graph
        .roots()
        .iter()
        .copied()
        .filter(|root| incoming.get(root).copied().unwrap_or(0) == 0)
        .collect();
    if starts.is_empty() {
        starts.extend(graph.roots().iter().copied().take(1));
    }
    let mut levels = HashMap::new();
    let mut queue = VecDeque::new();
    for root in starts {
        levels.insert(root, 0_usize);
        queue.push_back(root);
    }
    while let Some(root) = queue.pop_front() {
        let next_level = levels[&root] + 1;
        for &next in adjacency.get(&root).into_iter().flatten() {
            if let std::collections::hash_map::Entry::Vacant(entry) = levels.entry(next) {
                entry.insert(next_level);
                queue.push_back(next);
            }
        }
    }
    let mut fallback_level = levels.values().copied().max().unwrap_or(0);
    for &root in graph.roots() {
        if !levels.contains_key(&root) {
            fallback_level += 1;
            levels.insert(root, fallback_level);
        }
    }

    let mut local_positions = HashMap::new();
    let mut fragment_sizes = HashMap::new();
    for &root in graph.roots() {
        let metrics = layout_fragment(graph, root, &sizes, options, 0.0, 0.0, &mut local_positions);
        fragment_sizes.insert(
            root,
            Size {
                width: metrics.0,
                height: metrics.1,
            },
        );
    }

    let mut by_level: BTreeMap<usize, Vec<NodeId>> = BTreeMap::new();
    for (&root, &level) in &levels {
        by_level.entry(level).or_default().push(root);
    }
    for roots in by_level.values_mut() {
        roots.sort_unstable();
    }

    let mut fragment_offsets = HashMap::new();
    let mut y = 0.0_f32;
    let mut total_width = 0.0_f32;
    for roots in by_level.values() {
        let mut x = 0.0_f32;
        let row_height = roots
            .iter()
            .map(|root| fragment_sizes[root].height)
            .fold(0.0, f32::max);
        for root in roots {
            fragment_offsets.insert(*root, Point { x, y });
            x += fragment_sizes[root].width + options.fragment_x_gap;
        }
        total_width = total_width.max((x - options.fragment_x_gap).max(0.0));
        y += row_height + options.fragment_y_gap;
    }

    let mut positioned = HashMap::new();
    let mut nodes = Vec::with_capacity(graph.parsed().nodes().len());
    for index in 0..graph.parsed().nodes().len() {
        let node = NodeId::from_index(index);
        let fragment = fragment_of[index].expect("every node belongs to a fragment");
        let local = local_positions[&node];
        let offset = fragment_offsets[&fragment];
        let origin = Point {
            x: local.x + offset.x,
            y: local.y + offset.y,
        };
        positioned.insert(node, origin);
        nodes.push(NodeBox {
            node,
            origin,
            size: sizes[&node],
        });
    }

    let mut edges = Vec::new();
    for (parent_index, node) in graph.parsed().nodes().iter().enumerate() {
        let parent = NodeId::from_index(parent_index);
        for &child in node.tree_children() {
            edges.push(route_edge(
                parent,
                child,
                EdgeKind::Tree,
                false,
                &positioned,
                &sizes,
            ));
        }
    }
    for &(source, target) in graph.parsed().dominance_edges() {
        let light = tree_reachable(graph, source, target);
        edges.push(route_edge(
            source,
            target,
            EdgeKind::Dominance,
            light,
            &positioned,
            &sizes,
        ));
    }

    let total_height = nodes
        .iter()
        .map(|node| node.origin.y + node.size.height)
        .fold(0.0, f32::max);
    Ok(Layout {
        nodes,
        edges,
        size: Size {
            width: total_width,
            height: total_height,
        },
    })
}

fn mark_fragment(graph: &HncGraph, node: NodeId, root: NodeId, fragment_of: &mut [Option<NodeId>]) {
    fragment_of[node.index()] = Some(root);
    for &child in graph.node(node).tree_children() {
        mark_fragment(graph, child, root, fragment_of);
    }
}

fn layout_fragment(
    graph: &HncGraph,
    node: NodeId,
    sizes: &HashMap<NodeId, Size>,
    options: LayoutOptions,
    left: f32,
    top: f32,
    positions: &mut HashMap<NodeId, Point>,
) -> (f32, f32) {
    let size = sizes[&node];
    let children = graph.node(node).tree_children();
    if children.is_empty() {
        positions.insert(node, Point { x: left, y: top });
        return (size.width, size.height);
    }

    let child_top = top + size.height + options.node_y_gap;
    let mut cursor = left;
    let mut child_metrics = Vec::with_capacity(children.len());
    for &child in children {
        let metrics = layout_fragment(graph, child, sizes, options, cursor, child_top, positions);
        child_metrics.push((child, metrics));
        cursor += metrics.0 + options.node_x_gap;
    }
    let children_width = (cursor - left - options.node_x_gap).max(0.0);
    let width = size.width.max(children_width);
    let shift = (width - children_width) / 2.0;
    if shift > 0.0 {
        shift_subtrees(graph, children, shift, positions);
    }
    positions.insert(
        node,
        Point {
            x: left + (width - size.width) / 2.0,
            y: top,
        },
    );
    let height = size.height
        + options.node_y_gap
        + child_metrics
            .iter()
            .map(|(_, (_, height))| *height)
            .fold(0.0, f32::max);
    (width, height)
}

fn shift_subtrees(
    graph: &HncGraph,
    roots: &[NodeId],
    shift: f32,
    positions: &mut HashMap<NodeId, Point>,
) {
    let mut stack = roots.to_vec();
    while let Some(node) = stack.pop() {
        positions
            .get_mut(&node)
            .expect("child already positioned")
            .x += shift;
        stack.extend(graph.node(node).tree_children());
    }
}

fn route_edge(
    source: NodeId,
    target: NodeId,
    kind: EdgeKind,
    light: bool,
    positions: &HashMap<NodeId, Point>,
    sizes: &HashMap<NodeId, Size>,
) -> LayoutEdge {
    let source_origin = positions[&source];
    let target_origin = positions[&target];
    let start = Point {
        x: source_origin.x + sizes[&source].width / 2.0,
        y: source_origin.y + sizes[&source].height,
    };
    let end = Point {
        x: target_origin.x + sizes[&target].width / 2.0,
        y: target_origin.y,
    };
    let points = if kind == EdgeKind::Tree {
        let middle = (start.y + end.y) / 2.0;
        vec![
            start,
            Point {
                x: start.x,
                y: middle,
            },
            Point {
                x: end.x,
                y: middle,
            },
            end,
        ]
    } else {
        vec![start, end]
    };
    LayoutEdge {
        source,
        target,
        kind,
        points,
        light,
    }
}

fn tree_reachable(graph: &HncGraph, source: NodeId, target: NodeId) -> bool {
    let mut stack = vec![source];
    let mut seen = HashSet::new();
    while let Some(node) = stack.pop() {
        if node == target {
            return true;
        }
        if seen.insert(node) {
            stack.extend(graph.node(node).tree_children());
        }
    }
    false
}
