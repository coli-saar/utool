//! Core library for the Rust reimplementation of Utool.

#[allow(clippy::all, dead_code, missing_docs)]
mod domcon_oz_grammar_trait {
    include!(concat!(env!("OUT_DIR"), "/domcon_oz_grammar_trait.rs"));
}
mod domcon_oz_grammar;
#[allow(clippy::all, dead_code, missing_docs)]
mod domcon_oz_parser {
    include!(concat!(env!("OUT_DIR"), "/domcon_oz_parser.rs"));
}

#[allow(clippy::all, dead_code, missing_docs)]
mod holesem_grammar_trait {
    include!(concat!(env!("OUT_DIR"), "/holesem_grammar_trait.rs"));
}
mod holesem_grammar;
#[allow(clippy::all, dead_code, missing_docs)]
mod holesem_parser {
    include!(concat!(env!("OUT_DIR"), "/holesem_parser.rs"));
}

pub mod automata_ext;
pub mod codec;
pub mod filter;
pub mod graph;
pub mod layout;
pub mod solver;

pub use codec::{
    CodecError, InputCodec, encode_domcon_oz, encode_dot, parse_chain, parse_domcon_oz,
    parse_holesem,
};
pub use filter::{FilterError, Pattern, RewriteRule, RewriteSystem, filter_chart};
pub use graph::{GraphBuilder, GraphError, HncGraph, Node, NodeId, ParsedGraph};
pub use layout::{
    EdgeKind, Layout, LayoutEdge, LayoutError, LayoutOptions, NodeBox, Point, Size, layout_graph,
};
pub use solver::{
    Chart, ChartRule, Solution, SolutionNode, Solutions, SolveError, solve, solve_with_cancellation,
};
