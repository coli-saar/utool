#import "@preview/pergamon:0.7.1": *
#import "@preview/bananote:0.1.2": *
#import "@preview/ctheorems:1.1.3": *
#import "@preview/eggs:0.8.0" as eggs

#show: note.with(
  title: [Relative-Normal-Form Filtering in `utool-rust`: From Twelve Seconds to 700 Milliseconds],
  authors: (
    ([Alexander Koller], [Saarland University]),
  ),
  date: datetime.today(),
  version: [0.2],
)

#show: thmrules.with(qed-symbol: $square$)

#let theorem = thmbox(
  "theorem",
  "Theorem",
  base: none,
  fill: rgb("#f7fbff"),
  stroke: rgb("#b7cde2"),
  inset: (x: 1em, y: 0.75em),
)
#let proposition = thmbox(
  "proposition",
  "Proposition",
  base: none,
  fill: rgb("#fbfaf4"),
  stroke: rgb("#d9cfa8"),
  inset: (x: 1em, y: 0.75em),
)
#let query-example(body) = eggs.example(
  auto-subexamples: false,
  auto-glosses: false,
  number: none,
)[#body]

#abstract[
Utool filters a compact chart of solved forms by retaining only those trees that cannot be rewritten to a preferred tree in the same chart language. The Rust implementation performs this operation entirely on tree automata. It expands the persistent fragment chart to a temporary node automaton, computes the inverse image of that language under a context tree transducer, and subtracts the resulting bad language directly from the fragment chart. The subtraction uses residual sets of bad-language states and therefore fuses inverse homomorphism, determinization, complement, and intersection without materializing their large generic intermediates. This note derives the construction, relates it to the implementation, and separates the algorithmic changes from lower-level representation optimizations. On `rondane-650` with the Stefan 2026 rewrite system, residual transition evaluation initially occupied about 12.7 seconds of a 14.8-second run. The current filtering pipeline has a warm median of 694 milliseconds, and the full command takes 0.79--0.80 seconds, while preserving a result of 26,560 chart rules and 1,956,116 solved forms.
]

= The filtering problem

== Charts denote languages, not lists

A solved dominance graph usually has many solved forms. Utool represents this set by a finite bottom-up tree automaton rather than by a list of trees. A transition has the form

$ F(q_1, dots, q_k) arrow.r q, $

where $F$ is a ranked terminal, the $q_i$ are child states, and $q$ is the result state. A tree is accepted when its bottom-up run ends in an accepting state. Sharing states and transitions allows a chart with thousands or millions of solved forms to remain compact.

The persistent Rust chart uses #emph[fragment terminals]. A fragment terminal is a fixed linear tree context containing semantic graph nodes and ordered holes. The transition children fill those holes. This alphabet is well suited to solving and displaying dominance charts because one transition corresponds to one top-fragment decomposition.

Filtering asks a different question. Rewrite rules mention individual semantic constructors, possibly below the root of a fragment. The implementation therefore uses two alphabets:

- the #emph[fragment alphabet] of the persistent chart;
- the #emph[node alphabet] used temporarily while matching rewrite rules.

The distinction between these alphabets drives the whole construction.

== Relative normal forms

Let $L$ be the finite tree language denoted by a chart, and let $R$ be the specialized one-step rewrite relation. The bad trees are those trees in $L$ that can be rewritten to another tree in $L$:

$ "Bad"_R(L) = { s in L | exists t in L: s arrow.r.long_R t }. $

The arrow denotes one application of one directed rewrite rule: $s$ is the dispreferred source and $t$ is its preferred target. It does not denote reflexive-transitive closure. Equivalently, if the transducer relation is written $T_R(s)=t$, then inverse-image construction searches backwards from acceptable targets $t in L$ to the sources $s$ that must be removed.

The relative normal forms are the remaining trees:

$ "RNF"_R(L) = L - "Bad"_R(L). $

The word “relative” matters. A tree is removed only if its preferred rewrite target also belongs to the current chart language. The filter does not compute normal forms in the set of all trees. It computes minimal representatives among the readings licensed by one underspecified representation. This is the automata-theoretic formulation developed for redundancy elimination in underspecified semantics #cite("koller-thater-2010").

Directed rules state the preference explicitly. Equations are oriented by a stable total order on specialized patterns. This selects one representative consistently and makes repeated filtering idempotent on the tested equivalence systems.

#query-example[
Suppose the chart contains both

```text
a(X, every(Y, Z))
every(Y, a(X, Z))
```

and the rewrite system contains the directed rule

```text
a(X, every(Y, Z)) -> every(Y, a(X, Z)).
```

The first tree is removed because its target is present in the same chart. If the target were absent, the source tree would remain: filtering is relative to the chart language.
]

== Why enumeration is the wrong implementation

A direct algorithm would enumerate every tree in $L$, apply every rewrite rule, test whether each result belongs to $L$, and rebuild a chart from the survivors. Its cost is tied to the number of solved forms, which is precisely the quantity the chart compresses. `rondane-650`, for example, has 1,956,116 solved forms after filtering. Materializing those trees would discard the central benefit of chart representation.

The automata algorithm instead scales with the chart, the graph-specialized rewrite system, and the intermediate automata. It can still be expensive, but it does not contain a step proportional to the explicit solution language.

= The automata construction

== Expanding fragments into semantic nodes

Let $A_F$ be the fragment automaton of the source chart. Each fragment symbol $F$ denotes a fixed linear context $h(F)$ over node symbols. Expanding every fragment transition yields the node automaton

$ A_N = h(A_F). $

Boundary states retain the state identifiers of $A_F$; states inside fragment contexts are created as needed. The expansion is both bottom-up deterministic and top-down deterministic for a fixed result state and symbol. These properties support two later operations: deterministic matching of transducer output patterns and deterministic routing through completed input contexts.

For example, if a fragment symbol denotes the context $h(F)=f(g(square_1),square_2)$, then a chart rule $F(q_1,q_2) arrow.r q$ expands to node rules $g(q_1) arrow.r u$ and $f(u,q_2) arrow.r q$. The hole states $q_1,q_2$ and the root state $q$ remain chart boundary states; only the interior state $u$ is temporary. This is why node expansion can inspect rewrite sites inside a fragment without changing the fragment chart returned to the caller.

Node symbols identify graph nodes, not merely predicate labels. Before constructing the transducer, a rewrite pattern such as `a(X,Y)` is specialized to every compatible `a`-node in the graph. Different graph nodes remain distinct even if their labels and arities agree. Explicit occurrence names in a rule constrain which left- and right-hand-side constructors refer to the same node.

== A context tree transducer for exactly one rewrite

The rewrite relation is compiled into a context tree transducer (CTT). Its control states have two roles:

- `Annotation(a)` carries annotation $a$ along the unique path from the tree root to the rewrite site;
- `Neutral` copies a subtree outside that path unchanged.

At an annotated constructor, a copy rule chooses exactly one child through which the annotation continues. All siblings enter `Neutral` control. A non-identity rewrite rule terminates the annotated path. Thus an accepting CTT run contains exactly one rewrite, surrounded by arbitrary unchanged context.

Annotation propagation restricts the permitted path. A declaration for a parent annotation and constructor specifies the annotations passed to its children; where no declaration applies, the neutral annotation is used. A one-node context wildcard is compiled by expanding it over compatible graph-node shapes before specialization.

Equations and directed rules share the same transducer machinery after orientation. Linear, nondeleting variables are represented by dense variable slots. Globally unique identifiers distinguish constructor positions inside compiled left-hand sides, preventing unrelated partial matches from being merged in the inverse image.

== Computing the inverse image

Let $T_R$ be the CTT. The next automaton recognizes all node trees that $T_R$ can rewrite into a tree accepted by $A_N$:

$ P_N = T_R^(-1)(L(A_N)). $

These are the bad trees on the node alphabet. The implementation constructs $P_N$ backwards:

1. It seeds an agenda with pairs of CTT final states and accepting states of $A_N$.
2. For each agenda item, it considers CTT rules compatible with the root symbol available at the target state.
3. It matches the rule's right-hand side top-down through the deterministic target automaton.
4. On a successful match, it decomposes the left-hand side into transitions of the inverse-image automaton.
5. Newly discovered variable-boundary configurations are added to the agenda.

A preimage state combines a target state with a CTT configuration. Interior states carry an additional match-local marker. The latter is necessary because two internal left-hand-side positions can share a target component without denoting interchangeable computations.

The CTT rules are indexed by control state and right-hand-side root symbol. Variable-rooted outputs occupy a separate index bucket because they match every target state. This index is an algorithmic restriction of the search space: preimage construction no longer scans rules that cannot produce the current target transition.

== Returning to fragments by residual difference

The node automaton $P_N$ is temporary. The result must again be a fragment chart. Denotationally, the unwanted fragment trees are

$ "Bad"_F = h^(-1)(L(P_N)), $

and the desired output language is

$ L(A_F) - "Bad"_F. $

A generic implementation could construct the inverse homomorphism, determinize and complete it, complement it, and intersect it with $A_F$. The Rust implementation fuses these operations in a left-driven construction.

A derived state has the form $(q,Q)$, where $q$ is a source-chart state and $Q$ is a set of states of $P_N$. The set $Q$ is the #emph[residual]: it contains exactly the bad-language states reachable after expanding the fragment derivation represented by the derived state.

For a fragment transition

$ F(q_1, dots, q_k) arrow.r q $

and derived child states $(q_i,Q_i)$, the algorithm evaluates the fixed node context $h(F)$ in $P_N$, substituting $Q_i$ at hole $i$. The resulting state set $Q$ determines the parent state $(q,Q)$.

#proposition("Residual-difference invariant")[
For every constructed state $(q,Q)$ and every fragment derivation $d$ reaching it:

1. $d$ reaches $q$ in the source fragment automaton $A_F$; and
2. $Q$ is exactly the set of states reachable in $P_N$ on the node expansion $h(d)$.

Consequently, $(q,Q)$ is accepting exactly when $q$ is accepting in $A_F$ and $Q$ contains no accepting state of $P_N$.
]

The invariant follows by induction over the bottom-up construction. At a hole, evaluation returns the residual of the corresponding derived child. At a semantic node, it applies all matching transitions of $P_N$ to the child residuals. The parent residual is therefore exact. The acceptance condition keeps precisely those source derivations that are not recognized by the bad-language automaton.

The empty residual is useful: it means that no bad-language run exists for the current derivation. It therefore behaves like the rejecting sink that a generic completion-and-complement pipeline would materialize, but no explicit sink or global complement is needed.

= The implementation in `utool-rust`

== Pipeline and principal types

The public function `filter_chart` exposes the complete pipeline:

```text
fragment chart
    -> expand_chart
node chart
    -> build_ctt + compute_preimage + trim
bad node language
    -> difference_on_fragments
filtered fragment chart
```

#table(
  columns: (1.15fr, 1.35fr, 2.5fr),
  inset: 5pt,
  stroke: 0.4pt + rgb("#c8c8c8"),
  [*Concept*], [*Rust representation*], [*Role*],
  [$A_F$], [`Chart` / `FragmentAutomaton`], [Persistent automaton over top-fragment contexts.],
  [$h$ and $A_N$], [`ExpansionBuilder` / `NodeExpansion`], [Ephemeral expansion from fragments to graph-node symbols.],
  [$T_R$], [`Ctt`, `CttRule`, `CttState`], [Exactly-one-rewrite transducer with annotation propagation.],
  [$P_N$], [`PreBuilder`, `PreState`, `LhsDecomposer`], [Backward construction of the bad node language.],
  [$Q$], [`ResidualId`, `ResidualInterner`], [Canonical set of reachable bad-language states.],
  [$(q,Q)$], [`DifferenceBuilder`, `DerivedState`], [Reachable fragment-level language difference.],
  [trim], [`GeneratedBuilder`], [Remove useless staged states before constructing full indexes.],
)

The source chart is immutable. The filtered chart reuses its graph and fragment metadata, while each retained derived state records the source-chart state from which its subgraph provenance is obtained. Distinct residuals remain distinct states even when they share the same source subgraph; merging them would reintroduce filtered derivations.

== Construction order and trimming

The fragment difference is elaborated bottom-up. Before processing a source state, all derived alternatives for every child state are complete. Every emitted derived state is therefore productive: it is created only as the result of a rule whose children already have derivations.

Productivity does not imply usefulness. Some productive states cannot occur below an accepting state and must be removed by a top-down coaccessibility pass. This is the expected duality: bottom-up construction establishes productivity, while top-down trimming establishes participation in an accepting run. The backward preimage construction has the converse shape and receives a general trim that checks both properties.

`GeneratedBuilder` stores generated rules once without constructing `Explicit` transition indexes. It first computes the useful state set, compacts state identifiers, and only then builds the indexed automaton. This avoids building bottom-up and top-down indexes for rules that trimming immediately discards.

It does not avoid all work on discarded rules. A bottom-up construction can establish that a state has some derivation, but it cannot yet know whether that state will occur below an accepting state. Residual evaluation must therefore create productive states and rules that the later top-down pass may find non-coaccessible. The staging builder saves the cost of indexing and rebuilding those rules; avoiding their semantic construction would require information flowing in the opposite direction.

== Complex and simple fragments

`FragmentEvaluator` is the general path. It recursively evaluates every node of an arbitrary fragment context. Holes return existing child residuals; semantic nodes call `transition_over_state_sets`.

Most work on `rondane-650` has a simpler form: the fragment is one semantic node whose children are exactly its holes. `BatchedEvaluator` handles these fragments as one join across all derived child alternatives. Complex fragments retain the compositional fallback, so the optimization does not restrict the accepted chart representation.

= High-level algorithm improvements

High-level improvements reduce the number of semantic candidates or avoid entire intermediate constructions. They matter independently of Rust container choices.

== The twelve-second bottleneck

The first profile made the structure of the problem clear. The source chart had 9,077 states and 74,977 rules. The trimmed preimage had about 400,000 states and 919,850 rules. Preimage construction took about 0.8 seconds and preimage trimming another 0.2--0.3 seconds. The fragment difference took about 12.7 seconds and therefore dominated the 14.8-second full run.

The difference was not slow because it emitted too many rules. Suppressing all rule emission changed 12.72 seconds to only 12.57 seconds. The cost lay in residual transition evaluation and state discovery. The construction evaluated 734,666 derived child products and produced 3,621,511 residual results; later instrumentation found more than 1.56 billion tuples in the nontrivial inner Cartesian products. Trimming reduced 734,666 staged difference rules to 26,560 useful rules, but avoiding those writes could not remove the transition work that had already been done.

This profile set the optimization order. The first task was to replace inner Cartesian enumeration by a sparse join. Only after that change was it worthwhile to optimize preimage dispatch, residual representation, and construction overhead.

== The first collapse: sibling-finder evaluation

Evaluating a semantic node with child residuals $Q_1, dots, Q_k$ requires the set

$ { q | exists q_1 in Q_1, dots, q_k in Q_k: f(q_1,dots,q_k) arrow.r q }. $

The initial method enumerated the Cartesian product $Q_1 times dots times Q_k$ and performed an exact transition lookup for every tuple. Most tuples had no corresponding rule in the sparse preimage automaton.

The replacement is an adaptation of the sibling-finder technique introduced by Groschwitz, Koller, and Johnson for efficient tree-automata intersection #cite("groschwitz-etal-2016-efficient"). Their central idea is to expose indexed queries that retrieve rules once a child state at one position is known, rather than repeatedly scanning or constructing unrelated rule combinations. In filtering, a residual supplies a set of possible child states instead of one newly discovered product state, but the same sparse-join principle applies.

Here #emph[degree] is positional. For a bad-automaton state $p$ and child position $i$, define

$ "deg"_i(p) = |{ r in Delta | "the child of" r "at position" i "is" p }|. $

Thus $"deg"_i(p)$ is the length of the rule-index posting list for the key $(i,p)$; it is not the graph-theoretic degree of state $p$. For a residual set $Q_i$, the estimated posting volume at position $i$ is

$ "cost"_i(Q_i) = sum_(p in Q_i) "deg"_i(p). $

The sibling finder chooses a trigger position minimizing this cost, enumerates the indexed rules reached from states in that child residual, and tests the remaining children by binary search in their canonical residual sets. The estimate therefore accounts for both the size of $Q_i$ and the number of rules attached to each of its states: a larger residual can be the cheaper trigger if its states have lower degrees. The unit of iteration changes from possible tuples to existing rules. This degree-based choice is specific to the filter implementation; it extends the sibling-finder idea to unequal residual sets and arbitrary rule arities.

This was the decisive step. Across 462,063 nontrivial joins, Cartesian evaluation represented 1,560,608,589 tuples. The degree-guided sibling finder visited 8,791,940 rule candidates and retained 2,566,492 matches, reducing candidate work by about a factor of 177 at those joins. Single-thread wall time fell from 14.56 seconds to 2.24--2.59 seconds. The evaluator still uses exact Cartesian lookup for products of at most four tuples because its lower fixed cost wins there.

== Symbol-directed preimage construction

A later improvement was to retain the expanded chart's deterministic top-down table and index CTT rules by control state and output-root symbol. At a target state, the preimage agenda now visits only variable-rooted rules and rules whose output root is available there. Dense variable slots and preassigned interior identifiers support this traversal, but the main gain comes from changing which rules are considered.

Direction-aware trimming accompanied this change. Dense state identifiers allow productivity and coaccessibility to be computed as graph reachability over arrays. For the bottom-up difference, productivity is known by construction, so only the complementary top-down pass is necessary.

== Batched sibling joins

Three products occur in this part of the implementation and should be kept distinct:

- The #emph[inner state product] $Q_1 times dots times Q_k$ combines bad-automaton states within one residual transition. The sibling finder replaces its exhaustive enumeration by an indexed sparse join.
- The #emph[outer alternative product] combines the derived states available for the children of one source-chart rule. These alternatives represent genuinely different output-chart derivations and cannot in general be omitted.
- The #emph[incidence product] combines the indexes of outer alternatives whose residuals contain the children of one matching bad-automaton rule. It tells the batched evaluator which output cells receive that rule's result.

Ordinary sibling finding still repeated the same indexed scan for every tuple in the outer alternative product. The batched algorithm shares that scan across the entire source-chart rule. It does not eliminate the outer product: it fills all required output cells more economically.

After completing a source state, `StateIncidence` records, for every bad-language state, the indexes of derived alternatives whose residual contains it. For a simple $k$-ary fragment, `BatchedEvaluator` allocates a dense row-major table with one cell per derived child tuple. It scans bad-language transitions from the cheapest trigger position once. The incidence maps translate each transition child into compatible derived-alternative indexes, and the Cartesian product of those indexes identifies the cells to which the transition result contributes.

For this batched choice, the cost formula uses the keys of each incidence map—that is, the union of the alternatives' residual states at that child position. Each bad-language state is counted once even if it occurs in several alternatives, because its posting list will be scanned only once for the whole batch.

#query-example[
Suppose the left child has alternatives $d_0,d_1$ with residuals ${a,b}$ and ${c}$, while the right child has alternatives $e_0,e_1$ with residuals ${x}$ and ${y,z}$. The outer alternative product has four cells: $(d_0,e_0)$, $(d_0,e_1)$, $(d_1,e_0)$, and $(d_1,e_1)$.

For a bad-automaton rule $f(b,y) arrow.r p$, the incidence maps return $b mapsto {0}$ on the left and $y mapsto {1}$ on the right. Their incidence product therefore contains only $(0,1)$, so $p$ is added only to the residual for cell $(d_0,e_1)$. Scanning the rule once replaces rediscovering it independently while evaluating all four outer cells.
]

The feasibility measurement explains why this matters. Single-node fragment groups covered 728,219 of 734,666 outer products, or 99.1%. The old evaluator performed 8,772,166 indexed posting visits plus 794,435 small exact-tuple probes. The batched join required an estimated 1,462,649 posting visits and produced the same 4,042,978 successful contributions. Reusable incidence maps required 503,668 residual memberships; rebuilding them per rule would have required over 40 million and was therefore not used.

== Construction-aware single-build trimming

The preimage and difference automata used to be built as fully indexed `Explicit` automata and then trimmed into new `Explicit` automata. The staging builder removes this duplicate construction. The change is algorithmic because it avoids constructing indexes for the discarded subautomaton, not merely because the staged rule record is smaller.

== Deduplicating rewrite work

Exact duplicate rewrite rules are removed after parsing. Generated preimage rules are also deduplicated before staging because the backward agenda can reach the same rule through several paths. Neither operation changes the accepted language, but both prevent repeated semantic candidates from entering later constructions. They are algorithmic preprocessing under the distinction used here, although their implementation uses hash sets.

= Low-level implementation optimizations

Low-level optimizations preserve the candidate structure of the algorithm. They reduce allocation, hashing, indirection, or representation cost within the same logical operations.

== Dense state-indexed data

Automaton state identifiers are consecutive integers. Productivity flags, DFS marks, source-state provenance, child-partner lists, and top-down transitions therefore use `Vec` indexed by `StateId` rather than hash maps or hash sets. This both states the invariant in the type of access and improves locality.

== Residual interning

Residual sets are sorted and deduplicated at transition boundaries, then interned. Derived states store a compact `ResidualId` rather than an owned vector. The state-interning key is consequently the fixed-size pair `(StateId, ResidualId)`. Fragment holes pass residual identifiers through without cloning or sorting their underlying sets.

A memoization pilot showed why interning outputs was preferable to caching evaluations. There were 740,302 semantic-node transition calls but 734,667 distinct inputs, a potential hit rate of only 0.76%. The outputs collapsed to 86,541 distinct residual sets. Output interning exploits that convergence without maintaining a large cache of nearly unique inputs.

== Hashing and short vectors

Trusted internal keys use `FxHashMap` and `FxHashSet`. Small child tuples, residual results, variable slots, and agenda leaves use `SmallVec`, keeping their common short representation inline. These choices do not change the number of transitions considered, but they remove general-purpose hashing and heap-allocation overhead from hot loops.

Canonical residual ordering permits binary-search membership tests in sibling joins and makes residual identity independent of discovery order.

= Performance on `rondane-650`

== Measurement protocol

The benchmark used the release binary directly on an Apple M4 Pro:

```sh
./target/release/utool solvable -s \
  -f ../stefan-2026/equivalences.rewrite \
  ../src/main/resources/examples/rondane-650.mrs.pl
```

Every retained version produced 26,560 filtered chart rules and exactly 1,956,116 solved forms. These counts guard the semantic result while performance changes. The early profile reports both the filtering phase and the full process; the later focused series used repeated direct release-binary runs, excluded the first cold process, and reported the warm median.

The following table is a development trajectory, not a controlled ablation study. Its rows come from successive experiments, but some report the filter phase, some the full process, and the final focused series was measured in later sessions with the already-built release binary. The rows establish the scale and order of the improvements; they do not isolate every row's causal contribution under identical conditions.

#table(
  columns: (1.75fr, 1fr, 1.25fr, 2.4fr),
  inset: 5pt,
  stroke: 0.4pt + rgb("#c8c8c8"),
  [*Checkpoint*], [*Class*], [*Observed time*], [*Effect*],
  [Cartesian residual evaluation], [Reference], [12.7 s difference; 14.8 s full], [Transition evaluation dominates.],
  [Cartesian, eight threads], [Parallel], [6.17--6.23 s full], [Schedule the same candidate work.],
  [Sibling finder], [Algorithmic], [2.24--2.59 s full], [Replace 1.56 billion tuples by 8.79 million indexed candidates.],
  [Rayon removed], [Structural], [1.74 s filter; 1.79--1.83 s full], [Remove work transport and synchronization.],
  [Directed trim and preimage index], [Algorithmic], [1.53 s direct], [Skip known productivity work and irrelevant CTT rules.],
  [Residual IDs], [Low-level], [1.48 s direct], [Share output sets and use fixed-size state keys.],
  [`FxHash` and `SmallVec`], [Low-level], [1.20 s], [Reduce hot-path hashing and allocation.],
  [Staged trim], [Algorithmic], [1.07 s], [Index only surviving generated rules.],
  [Batched sibling join], [Algorithmic], [0.92 s], [Share scans across derived child tuples.],
  [Cleanup and readability confirmation], [Structural], [0.77--0.79 s], [Later-session no-regression check; no isolated speedup attributed.],
)

The full trajectory has two scales. Sibling finding removed the twelve-second bottleneck and brought a serial run into the two-second range. The remaining changes more than halved that cost again. A fresh phase-level profile measured the entire current filter at 694 milliseconds; the original Cartesian difference phase alone took 12.7 seconds. The complete command fell from 14.8 seconds to 0.79--0.80 seconds, an approximately eighteen-fold reduction. From the consistent 1.53-second direct-binary baseline used for the final focused series, the full-command time fell by about 48%.

== Where the final filter spends its time

The final breakdown comes from five warm runs of a temporarily instrumented release binary. The table reports the median of each phase; independently rounded medians need not sum exactly to the displayed total.

#table(
  columns: (2fr, 1fr, 0.8fr, 2.3fr),
  inset: 5pt,
  stroke: 0.4pt + rgb("#c8c8c8"),
  [*Phase*], [*Median*], [*Share*], [*Interpretation*],
  [Expand fragment chart], [12.7 ms], [1.8%], [Node-level view is inexpensive.],
  [Build CTT], [0.4 ms], [0.1%], [Rewrite compilation is negligible.],
  [Construct preimage], [156.4 ms], [22.5%], [Indexed backward matching.],
  [Trim preimage], [129.7 ms], [18.7%], [Both productivity and coaccessibility remain necessary.],
  [Fragment difference], [395.6 ms], [57.0%], [Residual evaluation and output construction.],
  [*Filtering total*], [*694.4 ms*], [*100%*], [Median warm filter pipeline.],
)

Within the 395.6-millisecond fragment difference, state ordering and sibling-index setup took about 4.2 milliseconds, residual evaluation took 369.5 milliseconds, and trimming plus construction of the final indexed chart took 15.1 milliseconds; destruction of temporary difference data accounts for most of the remaining roughly 6.8 milliseconds. Thus residual evaluation is still the largest single operation, but it now occupies about 53% of the complete filter rather than 12.7 seconds by itself.

The earlier 1.74-second post-sibling profile provides a useful intermediate comparison. Preimage construction plus trimming fell from about 945 milliseconds to 286 milliseconds, largely through symbol-directed dispatch and cheaper trimming. The collection of difference phases fell from roughly 778 milliseconds to 396 milliseconds through batched joins, compact residuals, cheaper containers, and single-build trimming. These comparisons combine several retained changes; they are phase-level explanations, not isolated ablations.

== What the measurements say about the algorithm

The largest gain came from replacing Cartesian residual evaluation with the sibling finder. Batched sibling joins then shared the remaining indexed scans across outer products, and construction-aware trimming removed an indexed intermediate automaton. The low-level hashing and inline-vector pass also produced a large measured step because it affected several high-frequency operations at once, but it did not change the candidate set.

Parallel evaluation was useful as a diagnostic bridge, not as the final design. Before sibling finding, eight Rayon threads reduced the full Cartesian run from 14.82 seconds to about 6.2 seconds. Only 81.4% of outer products belonged to groups large enough for parallel execution, and system time rose sharply under allocation, hashing, and memory traffic. After sibling finding, the one-thread result was already 2.24--2.59 seconds and the eight-thread result remained around 2.2 seconds. Removing Rayon and its work-transport layers produced a simpler serial implementation at 1.79--1.83 seconds for the full command. The retained design therefore reduces sequential work rather than scheduling the Cartesian algorithm more aggressively.

Two other tempting optimizations were rejected by measurement. A transition memo table saw almost no repeated inputs, and replacing explicit state-interning lookups with entry APIs changed neither runtime nor the underlying work. These results directed effort toward shared joins and avoided construction rather than toward more caching layers.

= Reading and maintaining the code

The implementation is organized in the same dependency order as this note: rewrite syntax and validation, CTT construction, node-chart expansion, inverse image, and fragment-level difference. A code audit should begin at `filter_chart` near the end of `src/filter/mod.rs`, then follow its four calls in order.

Several invariants carry most of the correctness argument:

- graph-node symbols preserve node identity across equal semantic labels;
- an accepting CTT run contains exactly one non-identity rewrite;
- expanded chart transitions are deterministic in both directions used by preimage matching;
- residual sets are sorted, duplicate-free, and interned;
- derived states are created in source-chart bottom-up order;
- two derived states with the same source state but different residuals must not be merged;
- the fast difference trim is valid only because all generated difference states are productive.

The optimization-specific structures are local. `SiblingDegrees` estimates the cheapest trigger position. `StateIncidence` supports batch distribution. `BatchedEvaluator` handles one-node fragments, while `FragmentEvaluator` remains the semantic reference path for general fragment contexts. `GeneratedBuilder` is kept in `automata_ext` because construction-aware trimming is independent of Utool graph concepts.

= Conclusion

Relative-normal-form filtering is a regular-language subtraction problem whose right-hand language is obtained by a transducer preimage. The practical implementation depends on preserving the right representation at each stage: fragment automata for persistent charts, node automata for rewrite matching, and residual product states for returning to fragments without a generic complement pipeline.

The sibling finder removed the 12.7-second Cartesian bottleneck. Later algorithmic changes avoided irrelevant CTT rules, repeated sibling scans, and indexed intermediate automata; representation changes made the remaining operations cheaper. Together they brought the full `rondane-650` command to the 0.7-second range without enumerating its 1.96 million solved forms.

#add-bib-resource(read("references.bib"))
#print-bananote-bibliography()
