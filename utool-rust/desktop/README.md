# Utool desktop

The desktop opens Domcon/Oz (`.clls`, `.domcon`, `.oz`, `.txt`) and Hole
Semantics (`.pl`, `.holesem`) files with codec selection inferred from the
extension. Each window shows one graph in three permanent views: Graph, Chart,
and Solutions. Chart construction begins in the background as soon as a graph
opens, and its exact solution count appears on the Solutions tab when ready.

A chart can be filtered with a Utool rewrite-system file from the shared
solution-space control in the Chart and Solutions views, or from the Solver
menu. The unfiltered chart and completed filtered variants are cached, so
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

The initial graph is a small Domcon/Oz example with two solutions. Its chart is
computed immediately. Use the fixed tabs to inspect the graph, chart, and solved
forms; controls along the bottom of Solutions navigate to any exact solution
number. The Chart view lists the actual split rules, grouped and numbered by
their left-hand-side subgraph.

Graph and solved-form tabs have a zoom selector and start at 50%. Dragging a
node moves its entire solid-edge fragment. **File → Open…** accepts Domcon/Oz
text or Hole Semantics; `.pl` files select the latter codec automatically. The input
text is intentionally not displayed: after decoding, the graph is the
document. **File → Export SVG…** exports the active graph or solved-form drawing,
including manual fragment adjustments. The bottom status bar reports the
runtime of loading/codec conversion, chart construction, and each Solution
enumeration.

For build-only verification:

```sh
npm run build
cd src-tauri && cargo check
```
