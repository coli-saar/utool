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
  version: [0.5],
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
Utool represents many solved forms by a compact tree automaton and filters this language without enumerating its members. The filtering construction is due to Koller and Thater and was already implemented in Java Utool. This note first states that construction and then explains its realization in `utool-rust`.

The principal performance problem in the initial Rust implementation was a Cartesian-product computation inside automaton difference. On `rondane-650`, this computation considered more than 1.56 billion state tuples and made the difference phase take about 12.7 seconds. The current implementation uses indexed sparse joins, shares those joins across related chart alternatives, limits work during preimage construction, and delays the construction of automaton indexes until after trimming. Filtering now has a warm median of 694 milliseconds. The input chart represents 2,414,835,788,400 solved forms, of which 1,956,116 remain after filtering.
]

= The filtering problem

== Chart languages and their alphabets

A chart is a finite representation of a tree language. Its principal component is a finite bottom-up tree automaton; the chart also carries the dominance graph and the fragment metadata needed to interpret the automaton. If the automaton has a transition

$ F (q_1, dots, q_k) arrow.r q, $

where $F$ is a ranked symbol, the $q_i$ are child states, and $q$ is the result state, then an occurrence of $F$ can combine subtrees reaching $q_1,dots,q_k$ into a tree reaching $q$. The automaton accepts a tree when its bottom-up run ends in an accepting state. We write $L (A_F)$ for the language accepted by the fragment automaton $A_F$ in the chart. The chart represents this language; it does not contain its trees as explicit data records.

The #emph[fragment alphabet] $Sigma_F$ contains one ranked symbol for each fragment context used by the chart. A fragment context is a fixed linear tree with numbered holes. Its rank is the number of holes, and the children of a fragment-tree node fill those holes. A tree in $L (A_F)$ is therefore a #emph[fragment tree]. This alphabet is useful for solving and displaying dominance charts because one automaton transition records one top-fragment decomposition.

The #emph[node alphabet] $Sigma_N$ contains one ranked symbol for each individual node of the dominance graph. Its rank is the arity of that node. Two graph nodes remain different symbols even when they carry the same label. Rewrite rules are matched over this alphabet because a rewrite site may occur inside a fragment context.

Each fragment symbol $F in Sigma_F$ denotes a node-tree context $h (F)$ over $Sigma_N$. Substituting child expansions into its holes extends $h$ from symbols to fragment trees and languages. We call $h (t)$ the #emph[node tree] represented by fragment tree $t$, and write

$ L_F = L (A_F) quad "and" quad L_N = h (L_F). $

Thus $L_F$ is the fragment-tree language accepted by the chart automaton, while $L_N$ is the node-tree language of solved forms represented by the chart.

#query-example[
Suppose the fragment alphabet contains a unary symbol $F$ and a nullary symbol $G$, with

$ h (F) = a_1 (x, square_1) quad "and" quad h (G) = "every"_2 (y,z). $

The subscripts distinguish the particular graph nodes named $a_1$ and $"every"_2$; $x,y,z$ are nullary graph nodes. The accepted fragment tree

```text
F
└─ G
```

expands to the node tree

```text
a₁
├─ x
└─ every₂
   ├─ y
   └─ z
```

That is, $h (F (G)) = a_1 (x,"every"_2 (y,z))$. The first tree uses two symbols of $Sigma_F$; the second uses five symbols of $Sigma_N$. They represent the same solved form at different levels of the implementation.
]

== Relative normal forms

Let $R$ be the one-step rewrite relation on node trees. Before filtering, Utool instantiates each rewrite pattern with the graph nodes on which it can match. This #emph[specialization] turns a rule over labels, such as `a(X,Y)`, into rules over the distinct node symbols of the current graph. The bad node trees are those members of $L_N$ that can be rewritten to another member of $L_N$:

$ "Bad"_R (L_N) = { s in L_N | exists t in L_N: s arrow.r.long_R t }. $

The arrow denotes one application of one directed rewrite rule: $s$ is the dispreferred source and $t$ is its preferred target. It does not denote reflexive-transitive closure. Equivalently, if the transducer relation is written $T_R (s)=t$, then inverse-image construction searches backwards from acceptable targets $t in L_N$ to the sources $s$ that must be removed.

The relative normal forms are the remaining node trees:

$ "RNF"_R (L_N) = L_N - "Bad"_R (L_N). $

The word “relative” matters. A tree is removed only if its preferred rewrite target also belongs to $L_N$. The filter does not compute normal forms in the set of all trees. It computes minimal representatives among the readings licensed by one underspecified representation. This is the automata-theoretic formulation developed for redundancy elimination in underspecified semantics #cite("koller-thater-2010").

A directed rule states which of its two sides is preferred: its left-hand side is removed when the right-hand side is also available. An equation does not state a direction. Utool gives each specialized equation a deterministic orientation by comparing its two sides under a fixed total order. The particular order is not semantically significant; its purpose is to choose the same representative consistently.

#query-example[
Suppose the node-tree language $L_N$ represented by the chart contains both

```text
a#1(x, every#2(y, z))
every#2(y, a#1(x, z))
```

and the rewrite system contains the directed rule

```text
a(X, every(Y, Z)) -> every(Y, a(X, Z)).
```

Here `a#1`, `every#2`, `x`, `y`, and `z` name graph nodes in the two node trees; `X`, `Y`, and `Z` are variables in the rewrite rule.

The first tree is removed because its target also belongs to $L_N$. If the target did not belong to $L_N$, the source tree would remain: filtering is relative to the language represented by this chart.
]

== Why enumeration is the wrong implementation

A direct algorithm would enumerate every node tree in $L_N$, apply every rewrite rule, test whether each result belongs to $L_N$, and construct a chart for the survivors. Its cost is tied to the number of solved forms before filtering, which is precisely the quantity the input chart compresses. On `rondane-650`, the input chart has 74,977 rules but represents 2,414,835,788,400 solved forms. The fact that only 1,956,116 remain afterward does not make enumeration feasible: the direct algorithm would first have to examine a language of roughly 2.4 trillion trees.

The automata algorithm instead scales with the chart, the graph-specialized rewrite system, and the intermediate automata. It can still be expensive, but it does not contain a step proportional to the explicit solution language.

== Provenance of the method and the Rust contributions

The filtering method is established work. Koller and Thater define relative normal forms and reduce their computation to operations on finite tree automata and a context tree transducer #cite("koller-thater-2010"). Java Utool implements this method in `RelativeNormalFormsComputer`: it converts a chart to a node-level finite tree automaton, compiles the rewrite system into a CTT, computes and trims the preimage, applies `differenceSpecialized`, trims again, and converts the result back to a chart. In particular, automata-based filtering, CTT preimage, and residual-set difference predate the Rust implementation.

This note documents a faster realization of the established method in `utool-rust`. The central algorithmic change adapts the sibling-finder idea of Groschwitz, Koller, and Johnson to the residual sets used during difference. The Rust implementation also shares indexed searches across related chart alternatives, restricts preimage construction to transducer rules that can produce the required output symbol, and constructs full automaton indexes only for states and rules that survive trimming. These changes preserve the filtering relation and the public API.

Separate representation changes make the retained work cheaper. They replace repeated residual sets by integer references, use arrays where automaton states are numbered consecutively, and keep common short sequences inline. Section 4 discusses changes to the amount of algorithmic work; Section 5 discusses these representation choices.

#table(
  columns: (2.2fr, 1.5fr, 1.4fr),
  inset: 5pt,
  stroke: 0.4pt + rgb("#c8c8c8"),
  [*Component*], [*Origin*], [*Status here*],
  [Relative-normal-form filtering by FTA and CTT operations], [Koller and Thater #cite("koller-thater-2010")], [Established method],
  [Chart conversion, CTT preimage, trimming, and `differenceSpecialized`], [Java Utool], [Established implementation],
  [Sibling-finder indexed child queries], [Groschwitz et al. #cite("groschwitz-etal-2016-efficient")], [Established algorithm],
  [Residual-set sibling evaluation with a positional-degree trigger], [`utool-rust` work], [New here],
  [Batched sibling joins across derived chart alternatives], [`utool-rust` work], [New here],
  [Symbol-directed preimage dispatch and construction-aware trimming], [`utool-rust` work], [New here],
  [Residual interning and compact hot-path representations], [`utool-rust` work], [New here],
)

The rest of the note follows this division. Section 2 recalls the inherited construction and the optimizations already present in Java Utool. Section 3 explains the Rust-specific realization. Sections 4 and 5 present the subsequent algorithmic and representational improvements, and Section 6 reports their measured effect.

= Established construction and Java optimizations

== Overall construction

Koller and Thater reduce relative-normal-form filtering to regular tree-language operations. Java Utool realizes the reduction in the following order:

```text
chart
    -> convertToFta
node-level chart automaton
    -> rewrite-system CTT + computePreImage + reduceFull
trimmed candidate-source automaton
    -> differenceSpecialized + reduceFull
filtered node automaton
    -> convertFtaToChart
filtered chart
```

This pipeline already avoids enumerating solved forms. It also contains two optimizations that matter for the later Rust comparison. First, it trims the preimage before computing the difference and trims the result before reconstructing the chart. Second, `differenceSpecialized` computes the difference through residual sets instead of materializing a generic determinize--complete--complement--intersection pipeline.

== Expanding fragment trees into node trees

Let $A_F$ be the fragment automaton of the source chart. Expanding the node context of every fragment transition yields a node automaton

$ A_N = h (A_F). $

Its accepted language is $L (A_N)=h (L (A_F))=L_N$. The notation $h (A_F)$ means that the homomorphism is applied to the transitions of the automaton; it does not identify the automaton with its language.

For example, suppose that a fragment symbol denotes the context $h (F)=f (g (square_1),square_2)$. A chart transition $F (q_1,q_2) arrow.r q$ then expands into the node transitions

$ g (q_1) arrow.r u quad "and" quad f (u,q_2) arrow.r q. $

The expansion reuses $q_1$ and $q_2$ where the two child trees are inserted and reuses $q$ at the root of the context. It introduces the fresh state $u$ for the internal result of $g$. In this way, the node automaton makes positions inside a fragment visible to the rewrite machinery.

Node symbols identify graph nodes, not merely predicate labels. Before constructing the transducer, a rewrite pattern such as `a(X,Y)` is specialized to every compatible `a`-node in the graph. Different graph nodes remain distinct even if their labels and arities agree. Explicit occurrence names in a rule constrain which left- and right-hand-side constructors refer to the same node.

== A context tree transducer for exactly one rewrite

The rewrite relation is compiled into a context tree transducer (CTT). A CTT rule matches an input-tree pattern and produces an output-tree pattern. Its variables identify the subtrees that are carried from input to output. The CTT used here recognizes runs with exactly one genuine rewrite: everything above, beside, and below the rewrite site is copied unchanged.

Rewrite annotations determine where a rule is allowed to apply. An annotation is a finite tag attached to a possible rewrite position. The rewrite-system file specifies the annotation at the root and may specify how a tag is transformed when a path passes through a constructor. If a node with constructor $f$ is reached under annotation $a$, a propagation declaration supplies one annotation for each child of $f$. A rule marked with annotation $b$ may be used only when the path reaches its left-hand side under $b$. Annotations can therefore express restrictions such as polarity: the permissibility of a rewrite may depend on the constructors between the root and the rewrite site.

The CTT records this information in #emph[control states]. A control state is simply the finite mode carried at one position of a transducer run. This construction uses the following modes:

- `Annotation(a)` means that the unique path to the rewrite site currently has annotation $a$;
- `Neutral` means that this subtree lies outside that path and must be copied unchanged.

For every graph-node symbol, the CTT contains #emph[copy rules]: rules whose input and output constructors are the same and whose variables occur unchanged. Under `Neutral`, such a rule copies all children in `Neutral`. Under `Annotation(a)`, it chooses one child as the continuation of the path. That child receives the annotation prescribed for its position; the other children enter `Neutral` and are copied without a rewrite.

The path ends with a #emph[rewrite rule], whose left- and right-hand sides differ. This is the one step that changes the tree. Since copy rules do not change the tree and only one child can continue the annotated path, every accepting CTT run contains exactly one rewrite rule. The surrounding context is copied unchanged. If the rewrite-system file contains no propagation declaration for a constructor, the designated neutral annotation is passed to its children.

A context wildcard in a rewrite pattern denotes one unspecified surrounding node. During specialization, Utool replaces it by each compatible graph-node shape. Equations and directed rules use the same CTT machinery after equations have been oriented.

== Computing the inverse image

Let $T_R$ be the CTT. Define the candidate bad-source language

$ B_N = T_R^(-1) (L (A_N)). $

The preimage automaton $P_N$ is constructed so that $L (P_N)=B_N$. Thus $P_N$ recognizes every node tree that $T_R$ can rewrite into a target accepted by $A_N$. Some of these source trees may lie outside $L_N$; membership in the source language is imposed later by subtracting from $L_F$.

A backward preimage construction starts from the accepting states of $A_N$, because these are the possible results of a rewrite. It then works backwards through CTT rules. For a CTT rule, the construction first matches the rule's output pattern against transitions of $A_N$. This match determines which target-automaton state is associated with each variable. The construction then uses the rule's input pattern, together with those variable states, to add transitions to $P_N$.

The implementation maintains a worklist of pairs $(c,q)$. Here $c$ is a CTT control state and $q$ is a state of $A_N$; the pair asks which input trees the transducer can process in mode $c$ to produce a tree that reaches $q$. Initially, $c$ is the accepting CTT mode and $q$ ranges over the accepting states of $A_N$. Matching a CTT rule may discover further pairs at its variables, which are added to the worklist. The process ends when no new pair is found.

== Specialized difference in Java Utool

After preimage construction, Java Utool computes the node-language difference

$ D_N = L (A_N) - L (P_N). $

A generic implementation could determinize and complete $P_N$, complement it, and intersect the result with $A_N$. Java Utool instead calls `differenceSpecialized`. Its derived states pair a state $q$ of $A_N$ with a residual set $Q$ of states of $P_N$. The residual records all states that $P_N$ can reach on the same partial node tree.

The implementation already avoids the naive inner Cartesian product of residual states. A state of the Java preimage automaton is itself a pair whose first component is a target-chart state. Before evaluating the difference, `differenceSpecialized` indexes preimage rules by their node symbol and by the tuple of first components of their child states. For a chart rule $f (q_1,dots,q_k) arrow.r q$, it retrieves only preimage rules with the same symbol and first-component tuple $(q_1,dots,q_k)$, then tests whether each remaining component belongs to the corresponding child residual. Thus Java Utool iterates over structurally compatible preimage rules rather than over every tuple in $Q_1 times dots times Q_k$.

The derived state $(q,Q)$ is accepting precisely when $q$ is accepting in $A_N$ and $Q$ contains no accepting state of $P_N$. In particular, the empty residual is accepting whenever $q$ is: it represents a node tree on which the preimage automaton has no run. Java Utool trims the resulting automaton and converts it back to a chart.

= The implementation in `utool-rust`

== Rust pipeline

The public function `filter_chart` exposes the complete pipeline:

```text
fragment chart
    -> expand_chart
node chart
    -> build_ctt + compute_preimage + trim
candidate bad-source node automaton
    -> difference_on_fragments
filtered fragment chart
```

Java Utool computes the difference on the expanded node automaton and then reconstructs a chart. The Rust implementation instead keeps the fragment automaton $A_F$ as the left operand of the difference, uses $A_N$ only as the target of preimage construction, and computes the result directly on the fragment alphabet. Denotationally, the unwanted fragment trees are

$ "Bad"_F = h^(-1) (B_N) = h^(-1) (L (P_N)), $

so the output language is $L (A_F)-"Bad"_F$. This is a Rust-specific realization of the inherited language difference, not a different filtering criterion.

`ExpansionBuilder` preserves the state identifiers of $A_F$ at fragment boundaries and creates fresh states only inside fragment contexts. It enforces two determinism properties: a node symbol and child tuple determine at most one result state, and a result state and node symbol determine at most one child tuple. The backward preimage matcher relies on the second property; fragment-context evaluation relies on the first.

== Fragment-level residual difference

The Rust construction retains the residual-state idea used by Java's `differenceSpecialized`, but evaluates an entire fragment context at each source transition. A derived state has the form $(q,Q)$, where $q$ is a state of $A_F$ and $Q$ is a set of states of $P_N$. The residual $Q$ records all states that $P_N$ can reach after expanding the represented fragment derivation to a node tree.

For a fragment transition

$ F (q_1, dots, q_k) arrow.r q $

and derived child states $(q_i,Q_i)$, the algorithm evaluates the fixed node context $h (F)$ in $P_N$, substituting $Q_i$ at hole $i$. The resulting state set $Q$ determines the parent state $(q,Q)$.

#proposition("Residual-difference invariant")[
For every constructed state $(q,Q)$ and every fragment derivation $d$ reaching it:

1. $d$ reaches $q$ in the source fragment automaton $A_F$; and
2. $Q$ is exactly the set of states reachable in $P_N$ on the node expansion $h (d)$.

Consequently, $(q,Q)$ is accepting exactly when $q$ is accepting in $A_F$ and $Q$ contains no accepting state of $P_N$.
]

The invariant follows by induction over the bottom-up construction. At a hole, evaluation returns the residual of the corresponding derived child. At a node symbol, it applies all matching transitions of $P_N$ to the child residuals. The empty residual means that $P_N$ has no run on the expanded derivation and is therefore safe at an accepting source state.

== Principal Rust types

#table(
  columns: (1.15fr, 1.35fr, 2.5fr),
  inset: 5pt,
  stroke: 0.4pt + rgb("#c8c8c8"),
  [*Concept*], [*Rust representation*], [*Role*],
  [$A_F$], [`Chart` / `FragmentAutomaton`], [Persistent automaton over top-fragment contexts.],
  [$h$ and $A_N$], [`ExpansionBuilder` / `NodeExpansion`], [Ephemeral expansion from fragment symbols to node symbols.],
  [$T_R$], [`Ctt`, `CttRule`, `CttState`], [Exactly-one-rewrite transducer with annotation propagation.],
  [$P_N$], [`PreBuilder`, `PreState`, `LhsDecomposer`], [Backward construction of the candidate bad-source language.],
  [$Q$], [`ResidualId`, `ResidualInterner`], [Canonical set of reachable states of $P_N$.],
  [$(q,Q)$], [`DifferenceBuilder`, `DerivedState`], [Reachable fragment-level language difference.],
  [trim], [`GeneratedBuilder`], [Remove useless staged states before constructing full indexes.],
)

In the Rust preimage, a `PreState` combines a target-automaton state with a CTT control state. States introduced inside a compiled left-hand side also carry a match-local interior identifier, because two pattern positions that reach the same target state are not necessarily interchangeable. Dense variable slots record the target states matched by the rule's variables.

The source chart is immutable. The filtered chart reuses its graph and fragment metadata, while each retained derived state records the source-chart state from which its subgraph provenance is obtained. Distinct residuals remain distinct states even when they share the same source subgraph; merging them would reintroduce filtered derivations.

== Construction order and trimming

The fragment difference is elaborated bottom-up. Before processing a source state, all derived alternatives for every child state are complete. Every emitted derived state is therefore productive: it is created only as the result of a rule whose children already have derivations.

Productivity does not imply usefulness. Some productive states cannot occur below an accepting state and must be removed by a top-down coaccessibility pass. This is the expected duality: bottom-up construction establishes productivity, while top-down trimming establishes participation in an accepting run. The backward preimage construction has the converse shape and receives a general trim that checks both properties.

The optimized builder exploits this construction order as described in Section 4.

== Complex and simple fragments

`FragmentEvaluator` is the general path. It recursively evaluates every node of an arbitrary fragment context. Holes return existing child residuals; node symbols call `transition_over_state_sets`.

Most work on `rondane-650` has a simpler form: the fragment context consists of one node symbol whose children are exactly its holes. `BatchedEvaluator` handles these fragments as one join across all derived child alternatives. Complex fragment contexts retain the compositional fallback, so the optimization does not restrict the accepted chart representation.

= High-level algorithm improvements in `utool-rust`

The changes in this section are new relative to the initial Rust implementation, not to the Koller--Thater filtering method as a whole. Some, especially sparse difference evaluation, recover and generalize work avoidance that Java's `differenceSpecialized` already achieved with a more specialized index. The changes reduce the number of transition or rule candidates, share work across several required outputs, or avoid entire intermediate constructions. They matter independently of Rust container choices.

== The twelve-second bottleneck

The first profile of the Rust implementation made the structure of the problem clear. The source chart had 9,077 states and 74,977 rules. The trimmed preimage had about 400,000 states and 919,850 rules. Preimage construction took about 0.8 seconds and preimage trimming another 0.2--0.3 seconds. The fragment difference took about 12.7 seconds and therefore dominated the 14.8-second full run.

This Cartesian bottleneck belonged to the initial Rust realization, not to the established Java algorithm. Java's `differenceSpecialized` already used the target-state components inside preimage states to retrieve structurally compatible rules. The initial Rust evaluator instead treated $P_N$ through a general transition interface and enumerated the inner product of its child residuals. The first optimization task was therefore to recover sparse rule-driven evaluation in a form compatible with fragment contexts and the Rust automaton representation.

The difference was not slow because it emitted too many rules. Suppressing all rule emission changed 12.72 seconds to only 12.57 seconds. The cost lay in residual transition evaluation and state discovery. The construction evaluated 734,666 derived child products and produced 3,621,511 residual results; later instrumentation found more than 1.56 billion tuples in the nontrivial inner Cartesian products. Trimming reduced 734,666 staged difference rules to 26,560 useful rules, but avoiding those writes could not remove the transition work that had already been done.

This profile set the optimization order. The first task was to replace inner Cartesian enumeration by a sparse join. Only after that change was it worthwhile to optimize preimage dispatch, residual representation, and construction overhead.

== Sparse residual evaluation with a sibling finder

Evaluating a node symbol with child residuals $Q_1, dots, Q_k$ requires the set

$ { q | exists q_1 in Q_1, dots, q_k in Q_k: f (q_1,dots,q_k) arrow.r q }. $

The initial method enumerated the Cartesian product $Q_1 times dots times Q_k$ and performed an exact transition lookup for every tuple. Most tuples had no corresponding rule in the sparse preimage automaton.

The replacement is an adaptation of the sibling-finder technique introduced by Groschwitz, Koller, and Johnson for efficient tree-automata intersection #cite("groschwitz-etal-2016-efficient"). Their central idea is to expose indexed queries that retrieve rules once a child state at one position is known, rather than repeatedly scanning or constructing unrelated rule combinations. Java's specialized difference and the sibling finder are both sparse joins, but they use different keys: Java fixes the tuple of target-chart components, whereas the Rust sibling finder can start from any one preimage state at any child position. In filtering, a residual supplies a set of possible trigger states rather than one newly discovered product state.

Here #emph[degree] is positional. For a state $p$ of the preimage automaton $P_N$ and child position $i$, define

$ "deg"_i (p) = |{ r in Delta | "the child of" r "at position" i "is" p }|. $

Thus $"deg"_i (p)$ is the length of the rule-index posting list for the key $(i,p)$; it is not the graph-theoretic degree of state $p$. For a residual set $Q_i$, the estimated posting volume at position $i$ is

$ "cost"_i (Q_i) = sum_(p in Q_i) "deg"_i (p). $

The sibling finder chooses a trigger position minimizing this cost, enumerates the indexed rules reached from states in that child residual, and tests the remaining children by binary search in their canonical residual sets. The estimate therefore accounts for both the size of $Q_i$ and the number of rules attached to each of its states: a larger residual can be the cheaper trigger if its states have lower degrees. The unit of iteration changes from possible tuples to existing rules. This degree-based choice is specific to the filter implementation; it extends the sibling-finder idea to unequal residual sets and arbitrary rule arities.

This was the decisive step. Across 462,063 nontrivial joins, Cartesian evaluation represented 1,560,608,589 tuples. The degree-guided sibling finder visited 8,791,940 rule candidates and retained 2,566,492 matches, reducing candidate work by about a factor of 177 at those joins. Single-thread wall time fell from 14.56 seconds to 2.24--2.59 seconds. The evaluator still uses exact Cartesian lookup for products of at most four tuples because its lower fixed cost wins there.

== Symbol-directed preimage construction

A later improvement was to retain the expanded chart's deterministic top-down table and index CTT rules by control state and output-root symbol. At a target state, the preimage agenda now visits only variable-rooted rules and rules whose output root is available there. Dense variable slots and preassigned interior identifiers support this traversal, but the main gain comes from changing which rules are considered.

Direction-aware trimming accompanied this change. Dense state identifiers allow productivity and coaccessibility to be computed as graph reachability over arrays. For the bottom-up difference, productivity is known by construction, so only the complementary top-down pass is necessary.

== Batched sibling joins

Three products occur in this part of the implementation and should be kept distinct:

- The #emph[inner state product] $Q_1 times dots times Q_k$ combines states of $P_N$ within one residual transition. The sibling finder replaces its exhaustive enumeration by an indexed sparse join.
- The #emph[outer alternative product] combines the derived states available for the children of one source-chart rule. These alternatives represent genuinely different output-chart derivations and cannot in general be omitted.
- The #emph[incidence product] combines the indexes of outer alternatives whose residuals contain the children of one matching bad-automaton rule. It tells the batched evaluator which output cells receive that rule's result.

Ordinary sibling finding still repeated the same indexed scan for every tuple in the outer alternative product. The batched algorithm shares that scan across the entire source-chart rule. It does not eliminate the outer product: it fills all required output cells more economically.

After completing a source state, `StateIncidence` records, for every state of $P_N$, the indexes of derived alternatives whose residual contains it. For a simple $k$-ary fragment, `BatchedEvaluator` allocates a dense row-major table with one cell per derived child tuple. It scans transitions of $P_N$ from the cheapest trigger position once. The incidence maps translate each transition child into compatible derived-alternative indexes, and the Cartesian product of those indexes identifies the cells to which the transition result contributes.

For this batched choice, the cost formula uses the keys of each incidence map—that is, the union of the alternatives' residual states at that child position. Each state of $P_N$ is counted once even if it occurs in several alternatives, because its posting list will be scanned only once for the whole batch.

#query-example[
Suppose the left child has alternatives $d_0,d_1$ with residuals ${a,b}$ and ${c}$, while the right child has alternatives $e_0,e_1$ with residuals ${x}$ and ${y,z}$. The outer alternative product has four cells: $(d_0,e_0)$, $(d_0,e_1)$, $(d_1,e_0)$, and $(d_1,e_1)$.

For a rule $f (b,y) arrow.r p$ of $P_N$, the incidence maps return $b mapsto {0}$ on the left and $y mapsto {1}$ on the right. Their incidence product therefore contains only $(0,1)$, so $p$ is added only to the residual for cell $(d_0,e_1)$. Scanning the rule once replaces rediscovering it independently while evaluating all four outer cells.
]

The feasibility measurement explains why this matters. Single-node fragment groups covered 728,219 of 734,666 outer products, or 99.1%. The old evaluator performed 8,772,166 indexed posting visits plus 794,435 small exact-tuple probes. The batched join required an estimated 1,462,649 posting visits and produced the same 4,042,978 successful contributions. Reusable incidence maps required 503,668 residual memberships; rebuilding them per rule would have required over 40 million and was therefore not used.

== Construction-aware single-build trimming

The preimage and difference automata used to be built as fully indexed `Explicit` automata and then trimmed into new `Explicit` automata. The staging builder removes this duplicate construction. The change is algorithmic because it avoids constructing indexes for the discarded subautomaton, not merely because the staged rule record is smaller.

`GeneratedBuilder` stores generated rules once without constructing `Explicit` transition indexes. It first computes the useful state set, compacts state identifiers, and only then builds the indexed automaton. This avoids building bottom-up and top-down indexes for rules that trimming immediately discards.

It does not avoid all work on discarded rules. A bottom-up construction can establish that a state has some derivation, but it cannot yet know whether that state will occur below an accepting state. Residual evaluation must therefore create productive states and rules that the later top-down pass may find non-coaccessible. The staging builder saves the cost of indexing and rebuilding those rules; avoiding their construction would require information flowing in the opposite direction.

== Deduplicating rewrite work

Exact duplicate rewrite rules are removed after parsing. Generated preimage rules are also deduplicated before staging because the backward agenda can reach the same rule through several paths. Neither operation changes the accepted language, but both prevent repeated rule candidates from entering later constructions. They are algorithmic preprocessing under the distinction used here, although their implementation uses hash sets.

= Low-level representation optimizations in `utool-rust`

These changes preserve both the inherited filtering construction and the candidate structure of the optimized Rust algorithm. They reduce allocation, hashing, indirection, or representation cost within the same logical operations.

== Dense state-indexed data

Automaton state identifiers are consecutive integers. Productivity flags, DFS marks, source-state provenance, child-partner lists, and top-down transitions therefore use `Vec` indexed by `StateId` rather than hash maps or hash sets. This both states the invariant in the type of access and improves locality.

== Residual interning

Residual sets are sorted and deduplicated at transition boundaries, then interned. Derived states store a compact `ResidualId` rather than an owned vector. The state-interning key is consequently the fixed-size pair `(StateId, ResidualId)`. Fragment holes pass residual identifiers through without cloning or sorting their underlying sets.

A memoization pilot showed why interning outputs was preferable to caching evaluations. There were 740,302 node-transition calls but 734,667 distinct inputs, a potential hit rate of only 0.76%. The outputs collapsed to 86,541 distinct residual sets. Output interning exploits that convergence without maintaining a large cache of nearly unique inputs.

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

Every retained version produced 26,560 filtered chart rules and represented exactly 1,956,116 solved forms. These counts guard the result while performance changes. The early profile reports both the filtering phase and the full process; the later focused series used repeated direct release-binary runs, excluded the first cold process, and reported the warm median.

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

- node symbols preserve graph-node identity when two nodes carry the same label;
- an accepting CTT run contains exactly one non-identity rewrite;
- expanded chart transitions are deterministic in both directions used by preimage matching;
- residual sets are sorted, duplicate-free, and interned;
- derived states are created in source-chart bottom-up order;
- two derived states with the same source state but different residuals must not be merged;
- the fast difference trim is valid only because all generated difference states are productive.

The optimization-specific structures are local. `SiblingDegrees` estimates the cheapest trigger position. `StateIncidence` supports batch distribution. `BatchedEvaluator` handles one-node fragment contexts, while `FragmentEvaluator` remains the general reference path. `GeneratedBuilder` is kept in `automata_ext` because construction-aware trimming is independent of Utool graph concepts.

= Conclusion

Relative-normal-form filtering by tree-automata construction is due to Koller and Thater and was already realized in Java Utool. The Rust implementation preserves its essential stages: a fragment chart is expanded to a node automaton, a CTT preimage identifies candidate rewrite sources, and a specialized residual-set difference returns to a filtered fragment chart.

The new result is the performance of this construction in `utool-rust`. Adapting the previously published sibling-finder idea to residual-set evaluation removed the 12.7-second Cartesian bottleneck. Batched sibling joins, selective CTT dispatch, and construction-aware trimming reduced further algorithmic work; representation changes made the remaining operations cheaper. Together they brought filtering on `rondane-650` to about 0.7 seconds without enumerating the 2.4 trillion solved forms represented by the input chart.

#add-bib-resource(read("references.bib"))
#print-bananote-bibliography()
