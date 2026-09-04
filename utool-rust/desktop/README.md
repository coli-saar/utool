# Utool desktop

The desktop opens Domcon/Oz (`.clls`, `.domcon`, `.oz`, `.txt`) and Hole
Semantics (`.pl`, `.holesem`) files with codec selection inferred from the
extension. Use the explicit Graph → Chart → Solution workflow. A chart can be
filtered with a Utool rewrite-system file from the Solver menu; the result is a
new chart tab.

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

The initial tab is a small Domcon/Oz graph with two Solutions. **Build Chart**
opens its chart in a new tab without enumerating a Solution. From that tab,
**Show First Solution** opens a solved-form tab; use the controls along its
bottom edge to navigate lazily through the Solutions. The chart tab lists the
actual split rules, grouped and numbered by their left-hand-side subgraph.

Graph and solved-form tabs have a zoom selector and start at 50%. Dragging a
node moves its entire solid-edge fragment. **File → Open…** accepts Domcon/Oz
text or Hole Semantics; `.pl` files select the latter codec automatically. The input
text is intentionally not displayed: after decoding, the graph is the
document. **Export SVG…** exports the active graph or solved-form drawing,
including manual fragment adjustments. The bottom status bar reports the
runtime of loading/codec conversion, chart construction, and each Solution
enumeration.

For build-only verification:

```sh
npm run build
cd src-tauri && cargo check
```
