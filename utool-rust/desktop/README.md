# Utool desktop

The desktop opens Domcon/Oz (`.clls`, `.domcon`, `.oz`, `.txt`) and Hole
Semantics (`.pl`, `.holesem`) files with codec selection inferred from the
extension. Each window shows one graph in three permanent views: Graph, Chart,
and Solutions. Chart construction begins in the background as soon as a graph
opens, and its exact solution count appears on the Solutions tab when ready.

A chart can be filtered with a Utool rewrite-system file from the shared
solution-space control in the Chart and Solutions views. The unfiltered chart
and completed filtered variants are cached, so
switching between them is immediate and keeps Chart and Solutions synchronized.

Graphs can be exported as Domcon/Oz or Graphviz DOT, and graphical graph and
solution tabs can be exported as SVG.

The desktop application is a Tauri 2 window with a React/TypeScript interface
and the Rust `utool` library as its backend.

## Run it

From this directory:

```sh
npm install
npm run tauri dev
```

Pass graph filenames after `--` to open them at startup. Each graph gets its
own window. Use `-f` (or `--filter`) to preselect a rewrite-system file for all
graphs opened during that desktop session:

```sh
npm run tauri dev -- -- -f ../../src/test/resources/server/filter-rules.txt graph.clls another.mrs.pl
```

The desktop first computes the original chart, uses it to lay out the graph,
and then applies the selected filter while keeping the Graph view visible. The
same filter is applied automatically to graphs opened later with **File →
Open…**.

This keeps the Tauri application shell in Cargo's debuggable development
profile, but compiles the `utool` engine and all other Rust dependencies with
`opt-level = 3`. To compile and run the entire desktop application with Cargo's
release profile instead, use:

```sh
npm run dev:release
```

To build the standalone `utool-display` executable with the frontend embedded,
use:

```sh
npm run build:standalone
```

The executable is written to `src-tauri/target/release/utool-display`. Do not
use plain `cargo build --release` for this artifact: Tauri's build command
enables its production asset protocol.

The app-icon master is `src-tauri/icons/icon.svg`. Regenerate the macOS,
Windows, Linux, and store-size assets after changing it with:

```sh
npm run tauri -- icon src-tauri/icons/icon.svg
```

The release command takes longer to compile and does not enable Rust debug
assertions, but gives the most representative runtime performance. `tauri
build` also uses the release profile when producing an application bundle.

The initial graph is a small Domcon/Oz example with two solutions. Its chart is
computed immediately. Use the fixed tabs to inspect the graph, chart, and
solutions; controls along the bottom of Solutions navigate to any exact solution
number. **File → Open Example…** shows the built-in example catalogue with codec
and description details. The example catalogue and sources live under
`src-tauri/resources/examples`; the build reads `examples.xml` and embeds every
listed source file into the executable. The Chart view lists the actual split
rules, grouped and numbered by their left-hand-side subgraph.

Graph and solution tabs start at 100%. Zoom commands are in the **View** menu;
Cmd/Ctrl-wheel zooms, while ordinary wheel and trackpad gestures pan scrollable
drawings. Dragging a node in the Graph tab moves its entire solid-edge fragment
and the adjustment survives tab changes. **File → Open…** accepts Domcon/Oz
text or Hole Semantics; `.pl` files select the latter codec automatically. The input
text is intentionally not displayed: after decoding, the graph is the
document. **Edit → Paste as** decodes clipboard text with a selected input
codec and opens the resulting graph in a new window. **File → Close All** closes
all graph windows while leaving auxiliary windows alone. **File → Export SVG…**
exports the active graph or solution drawing,
including manual fragment adjustments. The bottom status bar reports the
runtime of loading/codec conversion, chart construction, and each Solution
enumeration.

For build-only verification:

```sh
npm run build
cd src-tauri && cargo check
```
