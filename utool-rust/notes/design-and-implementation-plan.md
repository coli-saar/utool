# Rust Reimplementation: Design and Implementation Plan

Status: implemented through the Phase 8 usable slice  
Date: 2026-09-04

## 1. Scope and guiding decisions

The Rust implementation should be a focused reimplementation of the parts of Utool that support its main use case. It should not begin as a line-by-line port of the Java program.

The initial implementation will make the following choices.

- It will support hypernormally connected (HNC) dominance graphs. Inputs outside this tractable fragment will receive a precise validation error rather than being passed to a more general solver.
- A `Chart` will denote a compact, finite tree language represented by a tree automaton. A `Solution` will be one fully resolved tree in that language.
- There will be no public distinction between fully fleshed-out trees and solved forms. If compatibility code needs a plugging or solved-form view, that view will be derived from a `Solution` rather than becoming a second central abstraction.
- [`rusty-alto`](https://crates.io/crates/rusty-alto) will provide the basic tree-automata representation and algorithms. Utool will add the operations needed for efficient filtering behind a deliberately generic boundary.
- Constraint tree transducers (CTTs), dominance-graph specialization, relative-normal-form policy, and rewrite-rule syntax will remain Utool concepts. They should not be moved into `rusty-alto` merely because they are implemented using automata.
- The graph layout algorithm will be ported from Utool. The desktop frontend will render and manipulate the resulting layout but will not replace the domain-specific layout algorithm with a generic graph-layout package.
- The initial source tree should remain small. Begin with one algorithmic library package, then add a thin Tauri desktop package. CLI and server entry points will be added to the library package toward the end rather than split into further crates.

The first version is not required to preserve every legacy input format, server feature, or incidental iteration order. Compatibility should be driven by Rust-owned tests and concrete use cases.

## 2. End-to-end architecture

The principal data flow is:

```text
input text
   |
   v
format parser -> format-specific AST -> semantic lowering
                                            |
                                            v
                                   parsed dominance graph
                                            |
                                  validate and normalize
                                            |
                                            v
                                         HncGraph
                                            |
                                            v
                                          solver
                                            |
                                            v
                     Chart (finite tree automaton over solution trees)
                              |                         |
                              |                         v
                              |                 count / enumerate
                              v                         |
                         filtering                     v
                              |                     Solution
                              v                         |
                            Chart                       v
                                               layout / export / UI
```

This pipeline makes the boundary between syntax, graph semantics, solution construction, automata operations, and presentation explicit. In particular, parsers do not mutate a dominance graph while recognizing tokens, and the desktop application does not own algorithmic state.

## 3. Core data model

### 3.1 Dominance graphs

The graph layer should use typed identifiers rather than stringly typed references:

```rust
pub struct NodeId(/* compact integer key */);

pub struct ParsedGraph { /* nodes, labels, tree edges, dominance edges */ }

pub struct HncGraph { /* validated and normalized representation */ }
```

Tree-child order is semantically relevant and must be stored explicitly. Dominance edges are a separate edge class. External node names and labels belong to node metadata and must not be confused with internal identity. Two nodes with the same label remain different nodes throughout solving and enumeration.

`ParsedGraph` may temporarily represent malformed or unsupported input so that validation can report useful errors. Construction of `HncGraph` is the proof boundary: after it succeeds, solver code may rely on the documented HNC invariants. The validator should report structural witnesses where possible, such as the relevant nodes or components, instead of returning only a Boolean result.

### 3.2 Charts and solutions

Conceptually, the public types are:

```rust
pub struct Chart {
    automaton: /* rusty_alto explicit automaton */,
    signature: /* ranked solution-tree symbols */,
    metadata: ChartMetadata,
}

pub struct Solution {
    arena: TreeArena<SolutionSymbol>,
    root: Tree,
}
```

A `Chart` is not a collection of partially materialized `Solution` values. It is an automaton recognizing exactly the solution trees of an input graph, possibly after filtering. `Solution` is the materialized tree returned by enumeration, accepted as a membership query, sent to layout, or exported.

For now, each `Solution` owns its own tree arena. Enumeration will use `rusty-alto`'s `clone_tree` operation to copy the generated tree out of the iterator's internal arena. Different solutions will therefore not share subtrees. This is deliberately simpler than coupling solution lifetimes to an enumerator-owned shared arena; shared storage should be reconsidered only if profiling shows that retained solutions consume significant memory.

The automaton alphabet must preserve node identity. Display labels alone cannot be used as ranked symbols because equal labels at distinct graph nodes would otherwise collapse. User-facing labels can be recovered through chart metadata.

Compatibility views such as a plugging, a dominance graph with resolved dominance edges, or the old solved-form representation should be functions of `Solution`:

```rust
impl Solution {
    pub fn to_plugging(&self) -> Plugging;
    pub fn to_resolved_graph(&self) -> ResolvedGraph;
}
```

These need not all be implemented initially, but this direction prevents two competing representations of the same semantic object.

### 3.3 Suggested public workflow

The precise signatures will evolve, but the library should expose a workflow of this shape:

```rust
let parsed = codecs.parse(format, input, options)?;
let graph = HncGraph::try_from(parsed)?;
let chart = solve(&graph)?;
let filtered = filter(&chart, &filter_spec)?;

let count = filtered.count_solutions()?;
for solution in filtered.solutions() {
    // inspect, lay out, or export the solution
}
```

Parsing, input validation, solving, filtering, and enumeration should have distinct error types. Errors crossing the Tauri boundary can then be serialized into stable error codes plus human-readable context.

## 4. Constructing a chart

The current Java chart is already structurally close to a regular tree grammar. The Rust solver should compile a generalized split directly into bottom-up tree-automaton rules rather than first recreating all legacy chart classes.

A split chooses a root fragment and assigns subgraphs to its holes. Compilation can introduce internal states for fragments and holes and an accepting state for the completed top-level tree. The resulting automaton should recognize actual `Solution` trees, not a second intermediate tree language that later has to be interpreted.

This design needs an early executable specification. For a small set of Rust-owned test cases, each graph should have:

- the expected set of solutions, stored in a canonical textual form;
- the expected exact solution count;
- rejection expectations for non-HNC or malformed graphs; and
- expected results ported from the corresponding Java tests where those tests exist.

Exact counting over an acyclic finite chart should use arbitrary-precision integers, most likely `num_bigint::BigUint`. This can remain a Utool-level facility initially. It is a candidate for `rusty-alto` only if a suitably generic automaton counting API emerges.

Enumeration order should be treated as unspecified unless an actual UI or compatibility requirement makes it part of the contract. Deterministic output for tests can be obtained through canonical serialization and sorting at the test boundary.

## 5. Filtering and the `rusty-alto` boundary

The filtering pipeline is algorithmically important, so its module boundary should be fixed early even though its implementation follows the first usable desktop application. In the current formulation, if `C` is the solution chart and a CTT characterizes a transformation whose preimage identifies undesirable trees, filtering has the form

```text
P = preimage_CTT(C)
filtered = C \ P
```

The CTT and its interpretation are Utool-specific. The automata operations used to realize the final difference are not.

### 5.1 Utool-specific filtering code

The Utool `filter` module should own:

- CTT data structures and validation;
- parsing and semantic checking of rewrite systems;
- construction of relative-normal-form filters;
- annotation and specialization using dominance-graph information;
- CTT preimage construction; and
- the policy that determines which solutions are removed.

These APIs may freely mention `HncGraph`, `Chart`, rewrite rules, and Utool symbols.

### 5.2 Generic automata extensions

An `automata_ext` module should contain only generic tree-automata code and tests over small artificial alphabets. It must not import Utool graph, chart, codec, or CTT types. This is the extraction seam for upstreaming improvements into `rusty-alto`.

Likely generic additions are:

1. **Trimming.** Remove states and rules that are not both reachable/productive and co-reachable to a final state, with terminology matched to bottom-up tree automata.
2. **On-the-fly difference.** Construct only product states reachable from the left-hand automaton, avoiding construction of an unnecessarily large global complement.
3. **Complete determinization and complement.** A deterministic automaton that simply omits transitions for the empty subset is not by itself suitable for complement. Complement needs a complete transition relation or an explicit sink-state policy.
4. **Provenance-aware construction.** Provide a generic way to retain the source state or state-pair behind generated states and rules. Filtering diagnostics and extraction tests benefit from this, but the provenance mechanism should not know what a Utool chart is.

The preferred development order is to implement these operations in `automata_ext`, exercise them with generic property tests, use them in Utool, and then propose narrowly scoped patches to `rusty-alto`. Until a patch is released upstream, Utool can depend on its local implementation without forking the entire crate.

Direct difference deserves particular attention. A naive implementation determinizes and completes the right automaton, complements it, and then computes intersection. A left-driven construction can instead explore pairs only as demanded by rules of the usually much smaller solution chart. Benchmarks should compare these approaches on real filter workloads before an API is proposed upstream.

## 6. Parsers and codecs

Parser migration is a substantial workstream, not a peripheral compatibility task. The Java repository contains seven JavaCC grammars, covering Domcon/Oz, Glue, Hole Semantics, MRS Prolog, RMRS/Domcon, RTG, and rewrite systems, in addition to handwritten and XML-based codecs.

### 6.1 Parser architecture

Every codec should have two separate stages:

1. A parser produces a format-specific syntax tree with source spans.
2. A lowering step validates format semantics and constructs `ParsedGraph`, a rewrite-system value, or another domain object.

This is intentionally different from JavaCC grammars with embedded semantic actions that mutate graph or codec state. Separating the stages gives us better diagnostics, makes parsers easier to test, and permits a parser technology to be replaced without rewriting graph construction.

A small, static `CodecRegistry` can describe available codecs, filename extensions, supported options, and parser/serializer functions. Dynamic plugin loading is unnecessary for the initial program.

### 6.2 Parser generator choice: Parol

Use [`parol`](https://github.com/jsinger67/parol) as the parser generator. Parol is implemented in Rust and generates deterministic LL(k) parsers. This is the closest match to JavaCC's grammar model and therefore minimizes conceptual conversion work. It also avoids ANTLR's unofficial third-party Rust target and the more substantial grammar transformations that an LR(1) generator such as LALRPOP could require. Pest is attractive for small grammars, but changing to ordered-choice PEG semantics would make subtle compatibility differences more likely.

The existing grammars are a good fit for this choice. Most declare lookahead one or two; the exceptional productions explicitly request at most five tokens. Parol supports configurable multi-token lookahead, EBNF repetition and optionals, regular-expression terminals, scanner states, generated syntax types, source spans, and error recovery. Its generated parsers are deterministic and do not rely on general backtracking.

The grammar conversion should nevertheless be mechanical only at the syntax layer. Embedded Java semantic actions must not be transliterated into parser callbacks that construct or mutate a `DominanceGraph`. For each codec:

1. convert `TOKEN` and `SKIP` declarations into Parol terminals and scanner configuration;
2. translate JavaCC EBNF productions into a `.par` grammar;
3. model explicit `LOOKAHEAD(n)` sites using Parol's LL(k) analysis, left factoring only where the generated grammar requires it;
4. shape the generated parse representation into a small format-specific AST with source spans; and
5. move the old semantic actions into ordinary Rust lowering code from that AST.

Parol and `parol_runtime` versions must be pinned together. The preferred initial setup is generation from `build.rs` into Cargo's `OUT_DIR`, as documented by [Parol's build API](https://docs.rs/parol/latest/parol/build/). This keeps generated files out of source control and ensures that the grammar is the source of truth. If clean-build cost becomes material, we can switch to explicit CLI generation and commit the generated parser; this is a build-policy change, not a grammar rewrite.

The first conversion should still be treated as a validation checkpoint rather than an invitation to port all seven grammars at once. Convert Domcon/Oz and then Hole Semantics, because together they exercise comments, lexical classes, repeated structures, semantic lowering, and explicit lookahead up to five. Confirm diagnostics, Unicode and quoting behavior, clean-build time, generated-code warnings, and the independently ported codec tests. If Parol fails this concrete checkpoint, reconsider LALRPOP plus a dedicated lexer; ANTLR4 Rust is the fallback only if it offers a demonstrated advantage on these grammars.

### 6.3 Porting order and independent tests

A sensible codec order is:

1. Domcon/Oz input and a simple canonical output format;
2. MRS Prolog input;
3. rewrite-system parsing, in time for filtering;
4. Hole Semantics, Glue, and RMRS inputs;
5. RTG parsing only if chart interchange remains useful; and
6. XML/GXL codecs and less-used legacy formats according to real demand.

Relevant Java codec tests should be translated into Rust tests as each codec is ported. Their input examples and semantically meaningful expected results may be copied once into the Rust source tree, then maintained there as ordinary Rust-owned fixtures. The Rust tests must not read fixtures from the Java directories, invoke the Java program, or derive their expected values dynamically from it. The two implementations remain independent codebases.

The port is an opportunity to express expectations at better abstraction boundaries. Parser tests should check format ASTs and source locations; lowering tests should check normalized graph or rewrite-system values; malformed-input tests should check Rust error categories and spans. The Java tests supply cases and intended behavior, but their class structure and incidental serialization need not be reproduced.

## 7. Layout and desktop UI

### 7.1 Layout ownership

The existing Utool layout algorithm contains domain knowledge that ordinary graph-layout packages are unlikely to reproduce: fragment trees, tower placement, root-candidate costs, one-hole cases, and the visual treatment of dominance edges. It should be ported into a Rust `layout` module and tested independently of any GUI toolkit.

The frontend should measure rendered node labels and send node dimensions and layout options to Rust. Rust should return a platform-neutral result containing node rectangles, routed edges, fragment or grouping metadata, and flags such as whether a dominance edge is visually de-emphasized. This makes the layout algorithm usable from the CLI, tests, and future frontends.

Manual node positions are presentation state. Dragging a node should not mutate `HncGraph` or `Solution`; it should update a view model containing overrides or pinned coordinates. Relayout can either preserve pinned nodes or explicitly clear the overrides.

### 7.2 Tauri behavior

A Tauri application looks and behaves like a normal desktop application. Its main content is rendered by the operating system's embedded webview rather than by launching a browser tab or showing browser chrome. Tauri can supply native application/window menus and native file/message dialogs. Buttons, toolbars, tabs, inspectors, status bars, and the graph canvas inside the window are implemented by the web frontend.

The proposed frontend is a small TypeScript application, likely using Svelte for controls and state. This is not an architectural commitment to a large web stack; it is a practical way to build menus' corresponding commands, toolbars, keyboard interactions, panes, and accessible controls.

There are two credible rendering approaches:

- Use Cytoscape.js only as an interaction and rendering surface in `preset` positions supplied by Rust. It would provide pan, zoom, selection, edge rendering, and dragging without being allowed to lay out the graph.
- Use a custom SVG canvas. This gives exact control over Utool's visual conventions and may make printing/export simpler, at the cost of implementing more interaction machinery.

A short UI spike should render the same nontrivial layout both ways and test dragging, selection, zoom, light dominance edges, large labels, and export. The Rust layout API remains the same whichever renderer wins.

Long-running solve, filter, and count commands must execute away from the UI thread and support progress reporting or cancellation where the underlying algorithms permit it. Rust should own documents, graphs, and charts behind opaque handles; the frontend should receive summaries and requested solutions rather than serialized automata with every command.

## 8. Initial source organization

The source tree should begin as one substantive Rust package. The two binaries shown below are late work packages, not part of the initial scaffold:

```text
utool-rust/
  Cargo.toml
  src/
    lib.rs
    graph/
    codec/
    solver/
    solution.rs
    automata_ext/
    filter/
    layout/
    bin/
      utool.rs              # added in the CLI work package
      utool-server.rs       # added in the server work package
  grammars/                 # Parol .par grammar sources
  tests/
    fixtures/
    codec/
    solver/
    filter/
    layout/
  notes/
    design-and-implementation-plan.md
```

When GUI work starts, add one thin desktop package and a frontend directory. Whether Tauri's conventional `src-tauri` structure lives at the repository root or in `desktop/` should be decided from the generated Tauri setup at that time. It should depend on the core library rather than cause the library to split into graph, solver, codec, automata, and layout crates prematurely. The eventual CLI and server binaries should also call the same library APIs; neither requires another workspace crate unless packaging evidence later justifies one.

Modules can be extracted into crates later when there is evidence for a separate release cycle, dependency boundary, or downstream consumer. `automata_ext` does not need to be a crate to be upstreamable: independence is enforced first through its generic API and tests.

The initial external dependency set should also remain conservative. Likely essentials are `rusty-alto`, `parol_runtime`, an error-derivation crate, a serialization crate for application boundaries, and `num-bigint` for exact counts; `parol` itself is a build dependency. XML, CLI, logging, server, and frontend dependencies should be added only with the work package that uses them. In particular, an async runtime belongs to the server adapter, not to the algorithmic library.

## 9. Implementation phases

### Implementation status (September 2026)

Phases 1–4 now have an executable first implementation in this directory. The
Rust tests are independent ports: they neither execute the Java implementation
nor share a gold corpus with it.

Most importantly, Phase 3 follows Utool's algorithmic structure. Generalized
splitting is itself the conversion from an HNC dominance graph to a chart/tree
automaton: node-induced subgraphs become automaton states and free-root splits
become transitions. Solutions are obtained by lazily enumerating derivation
trees from that automaton and cloning each result into an independent arena.
There is no intermediate exhaustive enumeration of pluggings.

The Phase 4 implementation establishes the renderer-neutral API, ordered-tree
layout within fragments, dominance-layer placement between fragments, edge
routing, and geometry invariants. It is the first testable layout stage; exact
compatibility with all of the Java `DomGraphLayout` tower heuristics remains a
follow-up within Phase 4 before visual parity can be claimed.

Phase 5 now has a runnable Tauri desktop application in `desktop/`. Its custom
SVG renderer consumes the Rust layout rather than delegating placement to a
generic graph package. It supports native menus and file dialogs, Domcon/Oz and
Hole Semantics input, solving and exact chart statistics, lazy Solution
navigation, manual dragging, and SVG export. Chart construction runs away from
the UI thread and uses cooperative cancellation checkpoints between split
expansion steps.

The desktop workflow follows Utool's tab model: decoded dominance graphs,
charts, and individual solved forms occupy separate document tabs. Decoding
does not leave an editable source-text view in the interface, chart
construction never enumerates a Solution, and opening a solved-form tab is a
separate explicit action. Manual movement operates on whole solid-edge
fragments, and each graphical tab has its own zoom setting.

File opening is exposed through the native menu, with codec selection inferred
from the filename extension. A persistent status bar records the runtime of the
most recent action; the same action-timing mechanism is intended for filtering.

Phases 6–8 now provide an initial end-to-end implementation. Filtering parses
the legacy rewrite surface syntax, propagates annotations, and constructs a new
finite `Chart` containing precisely the retained derivations. Because HNC
charts are finite, this implementation currently computes relative normal
forms by exact language enumeration and then recompacts accepted derivations;
it does not yet implement the CTT/preimage optimization or context wildcards.
The generic `automata_ext` seam contains language-preserving trim with state
provenance, ready for extraction into `rusty-alto`; generic on-the-fly
difference remains future optimization work.

The desktop can filter a chart from the Solver menu or toolbar and opens the
result as a separate chart tab. Chart handles ensure that solutions of original
and filtered charts remain independently browsable. Domcon/Oz and Graphviz DOT
exports are shared library operations exposed by both the desktop and CLI.
The CLI binary mirrors Java Utool's option-based interface: `solve`, `solvable`,
`convert`, `classify`, and `help`, plus its global codec, filtering, statistics,
output, chart-dump, and limit options. It preserves codec names and the legacy
exit convention so existing scripts can invoke the Rust binary in place of the
Java launcher for the formats implemented here.

“Implemented through Phase 8” here means the usable slices of those phases,
not that every historical Utool codec or the performance-oriented CTT backend
has been ported. Those deliberately remain tracked by the uncompleted bullets
and open decisions below rather than being represented as finished work.

### Phase 0: Select and translate the test specification

- Inventory the Java graph, codec, solver, filtering, and layout tests relevant to the supported Rust scope.
- Select small and medium HNC examples covering the important corner cases.
- Translate expected graph properties, solution sets, counts, errors, and layout invariants into a Rust test plan.
- Copy any selected textual examples into `utool-rust/tests/fixtures`; these copies become independent Rust test data.
- Record the desktop workflows required for the first useful release and the later CLI and server contracts.

Exit criterion: each implementation phase has an explicit list of Java test cases to port, and no planned Rust test depends on running or reading from the Java codebase.

### Phase 1: Scaffold the core and implement graph semantics

- Create the single-package algorithmic library skeleton.
- Define error conventions, typed node identifiers, source spans, and fixture organization.
- Implement `ParsedGraph`, its programmatic builder, normalization, and `HncGraph`.
- Port the relevant Java graph tests into Rust unit tests using the programmatic builder.
- Implement the graph invariants and HNC checks required by the solver, including structural diagnostics.

Exit criterion: tests can construct graphs without any codec, valid examples normalize into documented `HncGraph` invariants, and invalid or unsupported examples produce the expected Rust errors.

### Phase 2: Validate Parol and deliver the first codec

- Convert Domcon/Oz to Parol and establish the format-AST/lowering pattern.
- Lower the Domcon/Oz AST into the already implemented `ParsedGraph`.
- Port the corresponding Java codec tests and input examples into independent Rust tests and fixtures.
- Convert enough of Hole Semantics to validate LL(5), lexical behavior, diagnostics, and generated-code ergonomics.
- Record the pinned Parol versions and generated-source policy.
- Add a simple canonical Rust output format for inspecting parsed and normalized graphs.

Exit criterion: real Domcon/Oz files parse and lower into `HncGraph`, the ported codec tests pass without consulting the Java tree, and the Hole Semantics checkpoint confirms that Parol remains suitable for the harder grammars.

### Phase 3: Implement solver, `Chart`, and `Solution`

- Port generalized splitting for HNC graphs.
- Compile splits directly into the `rusty-alto` automaton underlying `Chart`.
- Implement exact counting and lazy solution enumeration.
- Clone each enumerated tree into an independently owned `Solution` arena.
- Implement canonical solution serialization and initial compatibility views.
- Translate the relevant Java solver and chart tests into Rust tests.

Exit criterion: the ported solver tests have the expected solution languages and counts, including cases with repeated labels, and solutions remain valid independently of the enumerator.

### Phase 4: Port layout

- Define the renderer-independent layout request and result types.
- Port fragment and tower layout in small, testable stages.
- Port relevant Java layout cases as Rust coordinate invariants and Rust-owned visual snapshots.

Exit criterion: the ported layout tests capture the intended structural and visual behavior without a GUI dependency.

### Phase 5: Build the desktop application

- Add the thin Tauri package and TypeScript frontend.
- Implement native application menus and dialogs plus in-window controls.
- Evaluate Cytoscape.js preset rendering against custom SVG.
- Add document state, background commands, cancellation, solution navigation, and manual node dragging.

Exit criterion: a user can open a graph, solve it, browse solutions, adjust the drawing, and export a result in a normal desktop window. Filtering is not required for this first usable desktop application.

### Phase 6: Implement filtering

- Implement and test generic trim, provenance, and difference operations in `automata_ext`.
- Port rewrite parsing, CTT representation, specialization, and preimage construction into `filter`.
- Compare direct/on-the-fly difference with the conventional complement-and-intersection construction.
- Prepare independent upstream patches for generic improvements that prove useful.
- Translate the relevant Java filtering tests and rewrite-system inputs into independent Rust tests.
- Add filtering commands, progress, cancellation, and results to the existing desktop application.

Exit criterion: the ported filtering tests recognize the expected languages, benchmarks identify the viable construction strategy, and users can filter and browse the resulting chart in the desktop application.

### Phase 7: Expand codecs and exporters

- Port remaining codecs in priority order.
- Port the required output codecs and export operations.
- Extend the desktop application's open, save, import, and export workflows as formats become available.

Exit criterion: the core library and desktop application cover the agreed production input and output formats.

### Phase 8: Add the CLI mode

- Add a thin `utool` binary over the stable library API.
- Replicate Java Utool's `solve`, `solvable`, `convert`, `classify`, and help interface rather than introducing new subcommands.
- Preserve its short and long option names, codec inference, files and standard streams, output framing, statistics, and legacy exit-code conventions.
- Port relevant Java command-line tests as independent Rust integration tests.

Exit criterion: scripted workflows can use the same functionality as the desktop application without duplicating graph, solver, filtering, codec, or layout logic.

### Phase 9: Add the server mode

- Add a thin `utool-server` binary, or a `server` subcommand if packaging favors a single executable.
- Define a versioned transport API around library request and response types rather than exposing internal automata representations.
- Isolate the asynchronous runtime and networking dependencies in this adapter.
- Add request-size, time, concurrency, and resource limits plus cooperative cancellation for long-running jobs.
- Test transport behavior, structured errors, cancellation, and concurrent requests independently of the desktop and CLI frontends.

Exit criterion: remote clients can invoke the agreed stateless and job-oriented operations safely, while the algorithmic library remains independent of the server framework.

### Phase 10: Harden performance and distribution

- Add benchmarks for large charts, counting, enumeration latency, filtering, and layout.
- Reduce peak memory and add cancellation checkpoints.
- Package and test the desktop, CLI, and server deliverables on their supported operating systems.

Exit criterion: all three modes have measured resource behavior on difficult cases and reproducible release packaging.

## 10. Verification strategy

The test plan should use several complementary levels.

- **Unit tests** cover graph invariants, split construction, automata operations, AST lowering, and layout calculations.
- **Property tests** check automaton language preservation for trim, agreement of alternative difference algorithms on small generated automata, and round-trip properties of codecs that support output.
- **Ported regression tests** translate relevant Java test cases into idiomatic, independently maintained Rust tests.
- **Rust fixture tests** preserve important accepted and rejected input files, canonical solutions, and diagnostics entirely within `utool-rust`.
- **Visual tests** compare layout geometry or rendered snapshots while keeping tolerances explicit.
- **Benchmarks** track solve time, first-solution latency, total enumeration, exact counting, filter time, peak automaton size, and layout time.

Filtering tests must compare languages, not merely counts: two wrong languages can have the same cardinality. Likewise, solver tests should include distinct nodes with identical display labels.

## 11. Important open decisions

The following decisions should be resolved by spikes or usage evidence rather than by further abstract design discussion.

1. Does the Parol build dependency add enough clean-build cost to justify committing generated parser sources later?
2. Does Cytoscape.js in preset-layout mode preserve Utool's edge geometry and interaction requirements, or should the frontend use custom SVG?
3. Which codecs and exporters are required for the first desktop release, and which are historical compatibility only?
4. Which operations and job semantics belong in the first server API?
5. Is legacy solution enumeration order observable enough to preserve?
6. Which generic automata additions have APIs mature enough to propose to `rusty-alto`, and which should remain experimental in Utool?

## 12. First implementation milestone

The first meaningful milestone should be deliberately narrow: construct and validate an `HncGraph` programmatically, parse a Domcon/Oz example with Parol into the same graph representation, solve it into a `Chart`, assert its exact count, and enumerate independently owned canonical `Solution` trees in a library integration test. It should include the relevant tests translated from Java into the Rust tree and a pinned, reproducible parser-generation setup.

This slice exercises the central abstractions without prematurely introducing a command-line interface. Once it works, layout and the desktop frontend can be developed against stable graph, chart, and solution interfaces; filtering follows as a feature of that usable application, and CLI and server adapters follow after the library API has settled.
