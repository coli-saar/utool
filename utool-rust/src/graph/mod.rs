//! Dominance-graph representation and validation.

use serde::{Deserialize, Serialize};
use std::collections::{HashMap, HashSet, VecDeque};
use thiserror::Error;

/// A compact node identifier, stable for the lifetime of a graph.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, PartialOrd, Ord, Serialize, Deserialize)]
pub struct NodeId(u32);

impl NodeId {
    /// Construct an identifier from an index.
    #[must_use]
    pub(crate) fn from_index(index: usize) -> Self {
        Self(index as u32)
    }

    /// Returns the zero-based node index.
    #[must_use]
    pub const fn index(self) -> usize {
        self.0 as usize
    }
}

/// One node in a dominance graph.
#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct Node {
    name: String,
    label: Option<String>,
    tree_children: Vec<NodeId>,
}

impl Node {
    /// External node name used by codecs and diagnostics.
    #[must_use]
    pub fn name(&self) -> &str {
        &self.name
    }

    /// Semantic label, or `None` for a hole.
    #[must_use]
    pub fn label(&self) -> Option<&str> {
        self.label.as_deref()
    }

    /// Ordered tree children.
    #[must_use]
    pub fn tree_children(&self) -> &[NodeId] {
        &self.tree_children
    }

    /// Whether this node is an unlabeled hole.
    #[must_use]
    pub const fn is_hole(&self) -> bool {
        self.label.is_none()
    }
}

/// A parsed graph which may not yet satisfy solver invariants.
#[derive(Clone, Debug, Default, PartialEq, Eq, Serialize, Deserialize)]
pub struct ParsedGraph {
    nodes: Vec<Node>,
    dominance_edges: Vec<(NodeId, NodeId)>,
}

impl ParsedGraph {
    /// Start constructing a graph.
    #[must_use]
    pub fn builder() -> GraphBuilder {
        GraphBuilder::default()
    }

    /// All nodes in identifier order.
    #[must_use]
    pub fn nodes(&self) -> &[Node] {
        &self.nodes
    }

    /// All dominance edges in input order.
    #[must_use]
    pub fn dominance_edges(&self) -> &[(NodeId, NodeId)] {
        &self.dominance_edges
    }

    /// Look up a node by its external name.
    #[must_use]
    pub fn node_id(&self, name: &str) -> Option<NodeId> {
        self.nodes
            .iter()
            .position(|node| node.name == name)
            .map(|index| NodeId(index as u32))
    }

    /// Access a node by identifier.
    #[must_use]
    pub fn node(&self, id: NodeId) -> &Node {
        &self.nodes[id.index()]
    }

    pub(crate) fn tree_parents(&self) -> Result<Vec<Option<NodeId>>, GraphError> {
        let mut parents = vec![None; self.nodes.len()];
        for (parent_index, parent) in self.nodes.iter().enumerate() {
            let parent_id = NodeId(parent_index as u32);
            for &child in &parent.tree_children {
                if let Some(previous) = parents[child.index()].replace(parent_id) {
                    return Err(GraphError::MultipleTreeParents {
                        node: self.node(child).name.clone(),
                        first: self.node(previous).name.clone(),
                        second: parent.name.clone(),
                    });
                }
            }
        }
        Ok(parents)
    }

    pub(crate) fn normalize_dominance_targets(&mut self) -> Result<(), GraphError> {
        let parents = self.tree_parents()?;
        for (_, target) in &mut self.dominance_edges {
            let mut root = *target;
            let mut seen = HashSet::new();
            while let Some(parent) = parents[root.index()] {
                if !seen.insert(root) {
                    return Err(GraphError::TreeCycle(self.nodes[root.index()].name.clone()));
                }
                root = parent;
            }
            *target = root;
        }
        deduplicate_edges(self);
        Ok(())
    }

    pub(crate) fn remove_node(self, removed: NodeId) -> Self {
        let mut remap = vec![None; self.nodes.len()];
        let mut nodes = Vec::with_capacity(self.nodes.len().saturating_sub(1));
        for (old_index, node) in self.nodes.iter().enumerate() {
            let old = NodeId(old_index as u32);
            if old != removed {
                remap[old_index] = Some(NodeId(nodes.len() as u32));
                nodes.push(Node {
                    name: node.name.clone(),
                    label: node.label.clone(),
                    tree_children: Vec::new(),
                });
            }
        }
        for (old_index, node) in self.nodes.iter().enumerate() {
            let Some(new_parent) = remap[old_index] else {
                continue;
            };
            nodes[new_parent.index()].tree_children = node
                .tree_children
                .iter()
                .filter_map(|child| remap[child.index()])
                .collect();
        }
        let dominance_edges = self
            .dominance_edges
            .into_iter()
            .filter_map(|(source, target)| Some((remap[source.index()]?, remap[target.index()]?)))
            .collect();
        Self {
            nodes,
            dominance_edges,
        }
    }

    /// Weakly connected components over both edge kinds.
    #[must_use]
    pub fn weakly_connected_components(&self) -> Vec<Vec<NodeId>> {
        let mut adjacent = vec![Vec::new(); self.nodes.len()];
        for (parent, node) in self
            .nodes
            .iter()
            .enumerate()
            .flat_map(|(parent, node)| {
                node.tree_children
                    .iter()
                    .copied()
                    .map(move |child| (NodeId(parent as u32), child))
            })
            .chain(self.dominance_edges.iter().copied())
        {
            adjacent[parent.index()].push(node);
            adjacent[node.index()].push(parent);
        }

        let mut seen = vec![false; self.nodes.len()];
        let mut result = Vec::new();
        for start in 0..self.nodes.len() {
            if seen[start] {
                continue;
            }
            seen[start] = true;
            let mut queue = VecDeque::from([NodeId(start as u32)]);
            let mut component = Vec::new();
            while let Some(node) = queue.pop_front() {
                component.push(node);
                for &next in &adjacent[node.index()] {
                    if !seen[next.index()] {
                        seen[next.index()] = true;
                        queue.push_back(next);
                    }
                }
            }
            result.push(component);
        }
        result
    }
}

/// Builder which interns external node names.
#[derive(Clone, Debug, Default)]
pub struct GraphBuilder {
    graph: ParsedGraph,
    names: HashMap<String, NodeId>,
}

impl GraphBuilder {
    /// Return the existing node for `name`, or insert a hole.
    pub fn ensure_node(&mut self, name: impl Into<String>) -> NodeId {
        let name = name.into();
        if let Some(&id) = self.names.get(&name) {
            return id;
        }
        let id = NodeId(self.graph.nodes.len() as u32);
        self.graph.nodes.push(Node {
            name: name.clone(),
            label: None,
            tree_children: Vec::new(),
        });
        self.names.insert(name, id);
        id
    }

    /// Set the label of a node.
    pub fn set_label(&mut self, node: NodeId, label: impl Into<String>) -> Result<(), GraphError> {
        let label = label.into();
        let target = &mut self.graph.nodes[node.index()];
        if let Some(old) = &target.label
            && old != &label
        {
            return Err(GraphError::ConflictingLabel {
                node: target.name.clone(),
                first: old.clone(),
                second: label,
            });
        }
        target.label = Some(label);
        Ok(())
    }

    /// Append an ordered tree edge.
    pub fn add_tree_edge(&mut self, parent: NodeId, child: NodeId) {
        self.graph.nodes[parent.index()].tree_children.push(child);
    }

    /// Append a dominance constraint.
    pub fn add_dominance_edge(&mut self, source: NodeId, target: NodeId) {
        self.graph.dominance_edges.push((source, target));
    }

    /// Finish building without imposing HNC invariants.
    #[must_use]
    pub fn finish(self) -> ParsedGraph {
        self.graph
    }
}

/// A dominance graph validated for the supported solver fragment.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct HncGraph {
    graph: ParsedGraph,
    tree_parent: Vec<Option<NodeId>>,
    roots: Vec<NodeId>,
    holes: Vec<NodeId>,
}

impl HncGraph {
    /// The underlying normalized graph.
    #[must_use]
    pub const fn parsed(&self) -> &ParsedGraph {
        &self.graph
    }

    /// All fragment roots.
    #[must_use]
    pub fn roots(&self) -> &[NodeId] {
        &self.roots
    }

    /// All holes.
    #[must_use]
    pub fn holes(&self) -> &[NodeId] {
        &self.holes
    }

    /// Tree parent, if present.
    #[must_use]
    pub fn tree_parent(&self, node: NodeId) -> Option<NodeId> {
        self.tree_parent[node.index()]
    }

    /// Access a node.
    #[must_use]
    pub fn node(&self, id: NodeId) -> &Node {
        self.graph.node(id)
    }

    /// Find a node by external name.
    #[must_use]
    pub fn node_id(&self, name: &str) -> Option<NodeId> {
        self.graph.node_id(name)
    }
}

impl TryFrom<ParsedGraph> for HncGraph {
    type Error = GraphError;

    fn try_from(mut graph: ParsedGraph) -> Result<Self, Self::Error> {
        deduplicate_edges(&mut graph);
        let mut parent = vec![None; graph.nodes.len()];
        for (source_index, source) in graph.nodes.iter().enumerate() {
            let source_id = NodeId(source_index as u32);
            let mut local = HashSet::new();
            for &target in &source.tree_children {
                if !local.insert(target) {
                    return Err(GraphError::DuplicateTreeChild {
                        parent: source.name.clone(),
                        child: graph.node(target).name.clone(),
                    });
                }
                if let Some(previous) = parent[target.index()].replace(source_id) {
                    return Err(GraphError::MultipleTreeParents {
                        node: graph.node(target).name.clone(),
                        first: graph.node(previous).name.clone(),
                        second: source.name.clone(),
                    });
                }
            }
        }

        ensure_tree_acyclic(&graph)?;

        let roots: Vec<_> = parent
            .iter()
            .enumerate()
            .filter_map(|(index, parent)| parent.is_none().then_some(NodeId(index as u32)))
            .collect();
        let holes: Vec<_> = graph
            .nodes
            .iter()
            .enumerate()
            .filter_map(|(index, node)| node.is_hole().then_some(NodeId(index as u32)))
            .collect();

        for &hole in &holes {
            let node = graph.node(hole);
            if !node.tree_children.is_empty() {
                return Err(GraphError::HoleIsNotLeaf(node.name.clone()));
            }
            if parent[hole.index()].is_none()
                && graph
                    .dominance_edges
                    .iter()
                    .any(|(source, _)| *source == hole)
            {
                return Err(GraphError::EmptyFragment(node.name.clone()));
            }
        }

        for &(source, target) in &graph.dominance_edges {
            if !graph.node(source).is_hole() && parent[source.index()].is_some() {
                return Err(GraphError::DominanceShape {
                    source_node: graph.node(source).name.clone(),
                    target_node: graph.node(target).name.clone(),
                });
            }
        }

        if !is_hypernormally_connected(&graph) {
            return Err(GraphError::NotHypernormallyConnected);
        }

        Ok(Self {
            graph,
            tree_parent: parent,
            roots,
            holes,
        })
    }
}

fn deduplicate_edges(graph: &mut ParsedGraph) {
    let mut dominance = HashSet::new();
    graph.dominance_edges.retain(|edge| dominance.insert(*edge));
}

fn ensure_tree_acyclic(graph: &ParsedGraph) -> Result<(), GraphError> {
    fn visit(graph: &ParsedGraph, node: NodeId, colors: &mut [u8]) -> Result<(), GraphError> {
        match colors[node.index()] {
            1 => return Err(GraphError::TreeCycle(graph.node(node).name.clone())),
            2 => return Ok(()),
            _ => {}
        }
        colors[node.index()] = 1;
        for &child in graph.node(node).tree_children() {
            visit(graph, child, colors)?;
        }
        colors[node.index()] = 2;
        Ok(())
    }

    let mut colors = vec![0; graph.nodes.len()];
    for index in 0..graph.nodes.len() {
        visit(graph, NodeId(index as u32), &mut colors)?;
    }
    Ok(())
}

#[derive(Clone, Copy)]
struct AdjacentEdge {
    id: usize,
    neighbor: NodeId,
    dominance: bool,
    outgoing: bool,
}

fn is_hypernormally_connected(graph: &ParsedGraph) -> bool {
    if graph.nodes.is_empty() {
        return true;
    }
    let mut adjacency = vec![Vec::new(); graph.nodes.len()];
    let mut next_edge_id = 0;
    for (parent_index, node) in graph.nodes.iter().enumerate() {
        let parent = NodeId(parent_index as u32);
        for &child in &node.tree_children {
            adjacency[parent.index()].push(AdjacentEdge {
                id: next_edge_id,
                neighbor: child,
                dominance: false,
                outgoing: true,
            });
            adjacency[child.index()].push(AdjacentEdge {
                id: next_edge_id,
                neighbor: parent,
                dominance: false,
                outgoing: false,
            });
            next_edge_id += 1;
        }
    }
    for &(source, target) in &graph.dominance_edges {
        adjacency[source.index()].push(AdjacentEdge {
            id: next_edge_id,
            neighbor: target,
            dominance: true,
            outgoing: true,
        });
        adjacency[target.index()].push(AdjacentEdge {
            id: next_edge_id,
            neighbor: source,
            dominance: true,
            outgoing: false,
        });
        next_edge_id += 1;
    }

    let node_count = graph.nodes.len();
    let mut reachable = vec![vec![false; node_count]; node_count];
    for start in 0..node_count {
        let mut table = vec![vec![HashSet::<usize>::new(); node_count]; node_count];
        let mut history = HashSet::new();
        hnc_visit(
            NodeId(start as u32),
            &mut history,
            None,
            &adjacency,
            &mut table,
        );
        for source in 0..node_count {
            for target in 0..node_count {
                reachable[source][target] |= !table[source][target].is_empty();
            }
        }
    }
    (0..node_count)
        .all(|source| (0..node_count).all(|target| source == target || reachable[source][target]))
}

fn hnc_visit(
    node: NodeId,
    history: &mut HashSet<NodeId>,
    last_edge: Option<usize>,
    adjacency: &[Vec<AdjacentEdge>],
    table: &mut [Vec<HashSet<usize>>],
) {
    if history.contains(&node) {
        return;
    }
    let mut seen_it = true;
    if let Some(last_edge) = last_edge {
        for &previous in history.iter() {
            if table[previous.index()][node.index()].insert(last_edge) {
                seen_it = false;
            }
        }
    }
    if !seen_it || last_edge.is_none() {
        let last_was_outgoing_dominance = last_edge.is_some_and(|last| {
            adjacency[node.index()]
                .iter()
                .any(|edge| edge.id == last && edge.dominance && edge.outgoing)
        });
        for edge in &adjacency[node.index()] {
            if Some(edge.id) == last_edge
                || (edge.dominance && edge.outgoing && last_was_outgoing_dominance)
            {
                continue;
            }
            history.insert(node);
            hnc_visit(edge.neighbor, history, Some(edge.id), adjacency, table);
            history.remove(&node);
        }
    }
}

/// Structural or fragment-membership error.
#[derive(Clone, Debug, Error, PartialEq, Eq)]
pub enum GraphError {
    /// A node was assigned two distinct labels.
    #[error("node {node:?} has conflicting labels {first:?} and {second:?}")]
    ConflictingLabel {
        /// Node name.
        node: String,
        /// First label.
        first: String,
        /// Second label.
        second: String,
    },
    /// One node occurs twice in an ordered child list.
    #[error("node {child:?} occurs twice below {parent:?}")]
    DuplicateTreeChild {
        /// Parent name.
        parent: String,
        /// Child name.
        child: String,
    },
    /// Tree edges do not form a forest.
    #[error("node {node:?} has tree parents {first:?} and {second:?}")]
    MultipleTreeParents {
        /// Node name.
        node: String,
        /// First parent.
        first: String,
        /// Second parent.
        second: String,
    },
    /// Tree edges contain a cycle.
    #[error("tree cycle through node {0:?}")]
    TreeCycle(String),
    /// A hole has a tree child.
    #[error("hole {0:?} is not a leaf")]
    HoleIsNotLeaf(String),
    /// An unsupported empty fragment was found.
    #[error("empty fragment at {0:?}")]
    EmptyFragment(String),
    /// A weak-normality edge condition failed.
    #[error("dominance edge {source_node:?} -> {target_node:?} starts at an internal labeled node")]
    DominanceShape {
        /// Source node.
        source_node: String,
        /// Target node.
        target_node: String,
    },
    /// The graph is outside the supported HNC fragment.
    #[error("graph is not hypernormally connected")]
    NotHypernormallyConnected,
}
