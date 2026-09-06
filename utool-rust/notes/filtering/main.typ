#import "@preview/pergamon:0.7.1": *
#import "@preview/bananote:0.1.2": *
#import "@preview/ctheorems:1.1.3": *
#import "@preview/eggs:0.8.0" as eggs

#show: note.with(
  title: [Relative-Normal-Form Filtering in `utool-rust`: Algorithm and Performance],
  authors: (
    ([Alexander Koller], [Saarland University]),
  ),
  date: datetime.today(),
  version: [0.7],
)

#show: thmrules.with(qed-symbol: $square$)

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

The principal performance problem in the initial Rust implementation was a Cartesian-product computation inside automaton difference. On `rondane-650`, this computation considered more than 1.56 billion state tuples and made the difference phase take about 12.7 seconds. The current implementation instead uses transition indexes to inspect existing transitions, shares these searches across related residual evaluations, and avoids indexes for intermediate transitions that trimming will discard. The measured median for filtering is now 694 milliseconds. The input chart represents 2,414,835,788,400 solved forms, of which 1,956,116 remain after filtering.
]

= The filtering problem

== Chart languages and their alphabets

A chart is a finite representation of a tree language. Its principal component is a finite bottom-up tree automaton; the chart also carries the dominance graph and the fragment metadata needed to interpret the automaton. If the automaton has a transition

$ F (q_1, dots, q_k) arrow.r q, $

where $F$ is a symbol with a fixed number $k$ of children, the $q_i$ are child states, and $q$ is the result state, then an occurrence of $F$ can combine subtrees reaching $q_1,dots,q_k$ into a tree reaching $q$. A symbol equipped with such a fixed number is called #emph[ranked], and $k$ is its rank. The automaton accepts a tree when its bottom-up run ends in an accepting state. We write $L (A_F)$ for the language accepted by the fragment automaton $A_F$ in the chart. The chart represents this language; it does not contain its trees as explicit data records.

The #emph[fragment alphabet] $Sigma_F$ contains one ranked symbol for each fragment context used by the chart. A fragment context is a fixed tree with numbered holes, each of which occurs once. Its rank is the number of holes, and the children of a fragment-tree node fill those holes. A tree in $L (A_F)$ is therefore a #emph[fragment tree]. This alphabet is useful for solving and displaying dominance charts because one automaton transition records the choice of a top fragment and the subtrees inserted below it.

A dominance graph distinguishes a #emph[graph node] from the #emph[predicate label] written on that node. For example, two different graph nodes $u$ and $v$ may both carry the predicate label `a`. They remain different objects because they can occupy different positions in a solved form. The Rust implementation represents their identities by different `NodeId` values.

The #emph[node alphabet] $Sigma_N$ contains one ranked symbol for each labeled graph node. The symbol for $u$ identifies $u$ itself; its rank is the number of tree children of $u$. Consequently, the symbols for $u$ and $v$ are different even when both nodes carry the label `a` and have the same rank. Predicate labels are used to state general rewrite patterns. Concrete node symbols are used in node trees and in the automata that recognize them.

We use $a,b,f$ for predicate labels and $u,v,w$ for graph nodes and their corresponding symbols in $Sigma_N$. In text diagrams, the notation `u:a` displays both pieces of information: `u` is the graph-node identity, and `a` is its predicate label. The colon notation is explanatory notation for this note; it is not rewrite-system syntax.

Each fragment symbol $F in Sigma_F$ denotes a node-tree context $h (F)$ over $Sigma_N$. Substituting child expansions into its holes extends $h$ from symbols to fragment trees and languages. We call $h (t)$ the #emph[node tree] represented by fragment tree $t$, and write

$ L_F = L (A_F) quad "and" quad L_N = h (L_F). $

Thus $L_F$ is the fragment-tree language accepted by the chart automaton, while $L_N$ is the node-tree language of solved forms represented by the chart.

#query-example[
Suppose that the graph contains a binary node $u$ labeled `a`, a binary node $v$ labeled `every`, and nullary nodes $x,y,z$. Suppose also that the fragment alphabet contains a unary symbol $F$ and a nullary symbol $G$, with

$ h (F) = u (x, square_1) quad "and" quad h (G) = v (y,z). $

Here $u,v,x,y,z$ are symbols of the node alphabet because they identify graph nodes. The words `a` and `every` are labels on $u$ and $v$; they are not the symbols in these node trees. The accepted fragment tree

```text
F
└─ G
```

expands to the node tree

```text
u:a
├─ x
└─ v:every
   ├─ y
   └─ z
```

That is, $h (F (G)) = u (x,v (y,z))$. The labels after the colons in the diagram merely help the reader recognize the predicates. The first tree uses two symbols of $Sigma_F$; the second uses five graph-node symbols from $Sigma_N$. They represent the same solved form at different levels of the implementation.
]

== Relative normal forms

Rewrite patterns are stated over predicate labels because one rule should apply to every graph node with the relevant predicate. Conceptually, each constructor occurrence in a rule also has an #emph[occurrence name]. The rewrite syntax writes such a name after `#` when it must be explicit; the parser supplies internal names for unindexed occurrences when their correspondence is unambiguous. In `a#1(X,Y)`, `a` is a predicate label and `#1` names this occurrence of the constructor within the rule. The name `#1` is not a graph-node identity. Repeating it on the right-hand side says that the output constructor must be the same graph node that matched this occurrence on the left-hand side.

Before constructing the transducer, Utool performs #emph[specialization]. It assigns every occurrence name to a concrete graph node with the required label and rank. Different occurrence names must receive different graph nodes. If $u$ is labeled `a` and $v$ is labeled `every`, one specialization of

```text
a#1(X, every#2(Y, Z)) -> every#2(Y, a#1(X, Z))
```

assigns `#1` to $u$ and `#2` to $v$, producing the node-specific rule

```text
u(X, v(Y, Z)) -> v(Y, u(X, Z))
```

The first rule is written over predicate labels and uses occurrence names to express correspondence between its two sides. The specialized rule is written over symbols of $Sigma_N$. Internally, those symbols are the `NodeId` values for $u$ and $v$. Specialization enumerates every compatible injective assignment, so another `a`-node in the graph gives rise to another node-specific rule.

This use of node identity is semantically relevant. If two graph nodes have the same predicate label, a rewrite must still carry each particular node from the source tree to the corresponding position in the target tree. The chart can license one arrangement of those nodes without licensing the arrangement obtained by exchanging their identities. A tree over predicate labels alone would identify these two cases. Specialization retains the distinction while allowing rewrite rules to be stated once at the level of labels.

Let $R$ be the resulting one-step rewrite relation on node trees. The bad node trees are those members of $L_N$ that can be rewritten to another member of $L_N$:

$ "Bad"_R (L_N) = { s in L_N | exists t in L_N: s arrow.r.long_R t }. $

The arrow denotes one application of one directed rewrite rule: $s$ is the dispreferred source and $t$ is its preferred target. It does not denote reflexive-transitive closure. If $T_R$ denotes the same relation as a transducer, then $(s,t) in T_R$. Inverse-image construction searches backwards from acceptable targets $t in L_N$ to the sources $s$ that must be removed.

The relative normal forms are the remaining node trees:

$ "RNF"_R (L_N) = L_N - "Bad"_R (L_N). $

The word “relative” matters. A tree is removed only if its preferred rewrite target also belongs to $L_N$. The filter does not compute normal forms in the set of all trees. It computes minimal representatives among the readings licensed by one underspecified representation. This is the automata-theoretic formulation developed for redundancy elimination in underspecified semantics #cite("koller-thater-2010").

A directed rule states which of its two sides is preferred: its left-hand side is removed when the right-hand side is also available. An equation does not state a direction. Utool gives each specialized equation a deterministic orientation by comparing its two sides under a fixed total order. The particular order is not semantically significant; its purpose is to choose the same representative consistently.

#query-example[
Using the graph nodes $u$ and $v$ from the preceding example, suppose that the node-tree language $L_N$ contains both

```text
u:a(x, v:every(y, z))
v:every(y, u:a(x, z))
```

and that the rewrite system contains the directed rule

```text
a#1(X, every#2(Y, Z)) -> every#2(Y, a#1(X, Z))
```

In the node trees, `u` and `v` are graph-node identities and the text after each colon shows its predicate label. In the rewrite pattern, `a` and `every` are predicate labels, while `#1` and `#2` connect constructor occurrences across the two sides. The specialization that assigns `#1` to $u$ and `#2` to $v$ relates the two displayed node trees. As usual, `X`, `Y`, and `Z` are subtree variables.

The first tree is removed because its target also belongs to $L_N$. If the target did not belong to $L_N$, the source tree would remain: filtering is relative to the language represented by this chart.
]

== Why enumeration is the wrong implementation

A direct algorithm would enumerate every node tree in $L_N$, apply every rewrite rule, test whether each result belongs to $L_N$, and construct a chart for the survivors. Its cost is tied to the number of solved forms before filtering, which is precisely the quantity the input chart compresses. On `rondane-650`, the input chart automaton has 74,977 transitions but represents 2,414,835,788,400 solved forms. The fact that only 1,956,116 remain afterward does not make enumeration feasible: the direct algorithm would first have to examine a language of roughly 2.4 trillion trees.

The automata algorithm instead scales with the chart, the graph-specialized rewrite system, and the intermediate automata. It can still be expensive, but it does not contain a step proportional to the explicit solution language.

== Provenance of the method and the Rust contributions

The filtering method is established work. Koller and Thater define relative normal forms and reduce their computation to operations on finite tree automata and a context tree transducer (CTT) #cite("koller-thater-2010"). Java Utool implements this method in `RelativeNormalFormsComputer`: it converts a chart to a node-level finite tree automaton, compiles the rewrite system into a CTT, computes and trims the preimage, applies `differenceSpecialized`, trims again, and converts the result back to a chart. In particular, automata-based filtering, transducer preimage, and specialized automaton difference predate the Rust implementation.

This note documents a faster realization of the established method in `utool-rust`. The central algorithmic change adapts the sibling-finder idea of Groschwitz, Koller, and Johnson to the state sets used during difference; Section 2.5 defines these as residual sets. The Rust implementation also shares indexed searches across related chart alternatives. During preimage construction, it considers only transducer rules that can produce the required output symbol. During trimming, it constructs full automaton indexes only for states and transitions that remain in the result. These changes preserve the filtering relation and the public API.

Separate representation changes make the retained work cheaper. They replace repeated residual sets by integer references, use arrays where automaton states are numbered consecutively, and keep common short sequences inline. Section 4 discusses changes to the amount of algorithmic work; Section 5 discusses these representation choices.

#table(
  columns: (2.2fr, 1.5fr, 1.4fr),
  inset: 5pt,
  stroke: 0.4pt + rgb("#c8c8c8"),
  [*Component*], [*Origin*], [*Status here*],
  [Relative-normal-form filtering by finite tree automata and a CTT], [Koller and Thater #cite("koller-thater-2010")], [Established method],
  [Chart conversion, CTT preimage, trimming, and `differenceSpecialized`], [Java Utool], [Established implementation],
  [Sibling-finder indexed child queries], [Groschwitz et al. #cite("groschwitz-etal-2016-efficient")], [Established algorithm],
  [Sparse evaluation of residual sets using a sibling finder], [`utool-rust` work], [Rust adaptation],
  [Sharing sibling-finder searches across chart alternatives], [`utool-rust` work], [Rust-specific],
  [Restricting CTT rules by output symbol and delaying indexes until after trimming], [`utool-rust` work], [Rust-specific],
  [Sharing residual sets and reducing storage cost], [`utool-rust` work], [Rust-specific],
)

The rest of the note follows this division. Section 2 recalls the inherited construction and the optimizations already present in Java Utool. Section 3 explains the Rust-specific realization. Section 4 describes changes to the amount of work performed, Section 5 describes changes to data representation, and Section 6 reports the resulting performance.

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

This pipeline already avoids enumerating solved forms. It also contains two optimizations that matter for the later Rust comparison. First, it trims the preimage before computing the difference and trims the result before reconstructing the chart. Second, it does not implement difference by successively determinizing and completing the preimage, complementing it, and intersecting it with the chart automaton. `differenceSpecialized` computes the same language directly through residual sets.

== Expanding fragment trees into node trees

Let $A_F$ be the fragment automaton of the source chart. Expanding the node context of every fragment transition yields a node automaton

$ A_N = h (A_F). $

Its accepted language is $L (A_N)=h (L (A_F))=L_N$. The expression $h (A_F)$ means that each fragment transition is replaced by the node context assigned to its fragment symbol. By contrast, $h (L (A_F))$ applies the same expansion to every fragment tree in the accepted language.

For example, suppose that a fragment symbol denotes the context $h (F)=u (v (square_1),square_2)$, where $u$ and $v$ are graph-node symbols. A chart transition $F (q_1,q_2) arrow.r q$ then expands into the node transitions

$ v (q_1) arrow.r r quad "and" quad u (r,q_2) arrow.r q. $

The expansion reuses $q_1$ and $q_2$ where the two child trees are inserted and reuses $q$ at the root of the context. It introduces the fresh automaton state $r$ for the internal result of node $v$. In this way, the node automaton makes positions inside a fragment visible to the rewrite machinery.

The expansion uses graph-node symbols rather than predicate labels. Before constructing the transducer, specialization maps the occurrence names in a rewrite pattern to compatible graph nodes. The resulting rule therefore preserves node identity across the rewrite even when several graph nodes have the same predicate label.

== A context tree transducer for exactly one rewrite

The rewrite relation is compiled into a context tree transducer (CTT). A CTT rule matches an input-tree pattern and produces an output-tree pattern. Its variables identify the subtrees that are carried from input to output. The CTT used here recognizes runs in which exactly one rule changes the tree. Everything above, beside, and below that rewrite site is copied unchanged.

Rewrite annotations determine where a rule is allowed to apply. An annotation is a finite tag carried along a path from the root of the tree to a possible rewrite site. A rule marked with annotation $b$ may be used only when its left-hand side is reached under $b$. Annotations can, for example, distinguish positive from negative positions when a rewrite is permitted in one polarity but not the other.

The rewrite-system file specifies the annotation at the root and how annotations propagate through predicate labels. If a graph node labeled $f$ is reached under annotation $a$, a propagation declaration supplies one annotation for each child of that node. When the CTT is built, this label-based declaration is instantiated for every graph-node symbol carrying $f$. The annotation of the chosen child then records the relevant context between the root and the possible rewrite site. If no declaration applies, the propagation table uses a designated #emph[neutral annotation] as its default.

The CTT records this information in #emph[control states]. A control state is simply the finite mode carried at one position of a transducer run. This construction uses the following modes:

- `Annotation(a)` means that the unique path to the rewrite site currently has annotation $a$;
- `Neutral` means that this subtree lies outside that path and must be copied unchanged.

The neutral annotation is an ordinary annotation tag. It should not be confused with the `Neutral` CTT control state, which says that the current subtree does not contain the rewrite site.

For every graph-node symbol, the CTT contains #emph[copy rules]: rules whose input and output have the same root symbol and whose variables occur unchanged. Under `Neutral`, such a rule copies all children in `Neutral`. Under `Annotation(a)`, it chooses one child as the continuation of the path. That child receives the annotation prescribed for its position; the other children enter `Neutral` and are copied without a rewrite.

The path ends with a #emph[rewrite rule], whose left- and right-hand sides differ. This is the one step that changes the tree. Since copy rules do not change the tree and only one child can continue the annotated path, every accepting CTT run contains exactly one rewrite rule. The surrounding context is copied unchanged.

A context wildcard in a rewrite pattern denotes one unspecified surrounding constructor. Utool first replaces it by each label-and-rank combination present in the graph. Specialization then assigns the resulting occurrence, like the other named occurrences, to compatible concrete graph nodes. Equations and directed rules use the same CTT machinery after equations have been oriented.

== Computing the inverse image

Let $T_R$ be the CTT. Define the candidate bad-source language

$ B_N = T_R^(-1) (L (A_N)). $

The preimage automaton $P_N$ is constructed so that $L (P_N)=B_N$. Thus $P_N$ recognizes every node tree that $T_R$ can rewrite into a target accepted by $A_N$. Some of these source trees may lie outside $L_N$; membership in the source language is imposed later by subtracting from $L_F$.

A backward preimage construction starts from the accepting states of $A_N$, because these are the possible results of a rewrite. It then works backwards through CTT rules. For a CTT rule, the construction first matches the rule's output pattern against transitions of $A_N$. This match determines which target-automaton state is associated with each variable. The construction then uses the rule's input pattern, together with those variable states, to add transitions to $P_N$.

The implementation maintains a worklist of pairs $(c,q)$. Here $c$ is a CTT control state and $q$ is a state of $A_N$; the pair asks which input trees the transducer can process in mode $c$ to produce a tree that reaches $q$. Initially, $c$ is the accepting CTT mode and $q$ ranges over the accepting states of $A_N$. Matching a CTT rule may discover further pairs at its variables, which are added to the worklist. The process ends when no new pair is found.

== Residual sets and specialized difference in Java Utool

After preimage construction, Java Utool computes the node-language difference

$ D_N = L (A_N) - L (P_N). $

The preimage automaton $P_N$ need not have a unique run on a node tree. For an automaton $A$ and a tree $t$, define

$ "Reach"_A (t) = { p | "A has a run on" t "that ends in" p }. $

We call $"Reach"_(P_N) (t)$ the #emph[residual], or #emph[residual set], of $t$ with respect to $P_N$. Thus a residual is a set of states of $P_N$, not a state of the source chart and not a set of trees. Write $"Final"(A)$ for the accepting states of an automaton $A$. The tree $t$ belongs to the bad-source language exactly when its residual contains an accepting state of $P_N$:

$ t in L (P_N) "iff" "Reach"_(P_N) (t) inter "Final" (P_N) != emptyset. $

The complete residual is required to evaluate a parent tree. Knowing only whether a child tree is accepted by $P_N$ is insufficient: a transition at the parent refers to particular child states, including states that are not themselves accepting. For a graph-node symbol $u$ of rank $k$, define the lifted transition operation

$ "Step"_P (u,Q_1,dots,Q_k) = { p | exists p_1 in Q_1,dots,p_k in Q_k: u (p_1,dots,p_k) arrow.r p in Delta_P }, $

where $Delta_P$ is the transition set of $P_N$. If the children of a node tree have residuals $Q_1,dots,Q_k$, then the residual of the complete tree is $"Step"_P (u,Q_1,dots,Q_k)$.

#query-example[
Suppose two child trees have residuals $Q_1={p_1,p_2}$ and $Q_2={r}$, and $P_N$ contains the transitions

$ u (p_1,r) arrow.r s_1 quad "and" quad u (p_2,r) arrow.r s_2. $

The parent residual is ${s_1,s_2}$. If $s_2$ is accepting, the parent tree belongs to the bad-source language. This conclusion depends on retaining $p_2$ in the first child residual; a Boolean that records only whether the child itself is accepted would lose the information needed at the parent.
]

A generic difference construction could determinize and complete $P_N$, complement the resulting automaton, and intersect it with $A_N$. Java Utool instead calls `differenceSpecialized`. Its result states have the form $(q,Q)$. Here $q$ is a state of $A_N$, and $Q$ is the residual of the same node tree with respect to $P_N$. Pairing the two components records both facts needed by the difference: the tree belongs to the source language through $q$, and its possible runs in the bad-source automaton end in the states in $Q$.

Java's preimage states retain a state of $A_N$ together with additional information introduced by the transducer construction. `differenceSpecialized` indexes each preimage transition by its graph-node symbol and by the tuple of retained $A_N$ states at its children. When processing $u (q_1,dots,q_k) arrow.r q$ from $A_N$, it retrieves only preimage transitions with symbol $u$ and retained child tuple $(q_1,dots,q_k)$. It then checks whether each transition's additional child component belongs to the corresponding residual set. Java Utool therefore examines structurally compatible preimage transitions rather than every tuple in $Q_1 times dots times Q_k$.

The result state $(q,Q)$ is accepting precisely when $q$ is accepting in $A_N$ and $Q inter "Final" (P_N)=emptyset$. In particular, an empty residual means that $P_N$ has no run on the tree, so $(q,emptyset)$ is accepting whenever $q$ is accepting. Java Utool trims the resulting automaton and converts it back to a chart.

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

Java Utool computes the difference on the expanded node automaton and then reconstructs a chart. The Rust implementation instead keeps the fragment automaton $A_F$ as the left operand of the difference, uses $A_N$ only as the target of preimage construction, and computes the result directly on the fragment alphabet. The set of unwanted fragment trees is

$ "Bad"_F = h^(-1) (B_N) = h^(-1) (L (P_N)), $

so the output language is $L (A_F)-"Bad"_F$. Here $h^(-1) (B_N)$ denotes all fragment trees whose node expansion belongs to $B_N$. The Rust code thus realizes the inherited language difference directly at the fragment level; it does not change the filtering criterion.

`ExpansionBuilder` implements the expansion described in Section 2.2. It reuses each state of $A_F$ where a child tree enters a fragment context and where the completed context returns to the fragment automaton. It creates new states only for nodes strictly inside the context.

The resulting node automaton is deterministic in two directions. Bottom-up, a node symbol together with the states reached by its children determines at most one parent state. Top-down, a node symbol together with a parent state determines at most one tuple of child states. The preimage construction uses the top-down property when it matches the output side of a CTT rule. Fragment evaluation uses the bottom-up property when it computes the state reached by an expanded fragment tree.

== Fragment-level residual difference

The Rust construction applies the same residual idea directly to fragment trees. For a fragment tree $t$, define

$ "Res" (t) = "Reach"_(P_N) (h (t)). $

Thus $"Res" (t)$ is the set of states that $P_N$ can reach on the expanded node tree $h(t)$. A state in the difference automaton has the form $(q,Q)$, where $t$ reaches $q$ in the source fragment automaton $A_F$ and $Q="Res"(t)$. We use $q$ for a source-chart state and $p$ for a state of $P_N$; the members of $Q$ are therefore $p$-states.

We reserve #emph[residual] for the set $Q$ and #emph[difference state] for the pair $(q,Q)$. The implementation stores a residual once and refers to it by a `ResidualId`; that integer is a storage handle, not an automaton state. Keeping these three objects distinct avoids confusing a state of $P_N$, a set of such states, and a state of the constructed difference automaton.

Both components are necessary. The component $q$ preserves the run and structure of the source chart. The residual $Q$ determines how the same tree behaves in the bad-source automaton. Two fragment trees may reach the same source state $q$ but have different residuals, and these residuals can behave differently when the trees are inserted into a larger fragment context. The construction must therefore keep $(q,Q)$ and $(q,Q')$ as different states when $Q != Q'$.

For a fragment transition

$ F (q_1, dots, q_k) arrow.r q $

choose one already constructed difference state $(q_i,Q_i)$ for each child. The algorithm evaluates the fixed node context $h(F)$ in $P_N$, substituting $Q_i$ at hole $i$. Formally, define context evaluation by

$ "Eval"_P (square_i; Q_1,dots,Q_k) = Q_i $

and

$ "Eval"_P (u (c_1,dots,c_m); Q_1,dots,Q_k) = "Step"_P (u, "Eval"_P (c_1; Q_1,dots,Q_k), dots, "Eval"_P (c_m; Q_1,dots,Q_k)). $

The first clause inserts a child residual at a fragment hole. The second evaluates an internal graph node using the lifted transition operation defined in Section 2.5. The parent residual is

$ Q = "Eval"_P (h (F); Q_1,dots,Q_k), $

and the construction adds the difference transition

$ F ((q_1,Q_1),dots,(q_k,Q_k)) arrow.r (q,Q). $

Since a source state $q_i$ may have several difference states $(q_i,Q_i)$, one source-chart transition may give rise to several difference transitions. This is not automaton nondeterminism introduced accidentally: the alternatives represent different fragment subtrees that the source chart had already merged into the same state but that $P_N$ distinguishes.

#proposition("Residual-difference invariant")[
For every constructed difference state $(q,Q)$ and every fragment tree $t$ whose run reaches that state:

1. $t$ reaches $q$ in the source fragment automaton $A_F$; and
2. $Q="Res"(t)$, that is, $Q$ is exactly the set of states reachable in $P_N$ on the node expansion $h(t)$.

Consequently, $(q,Q)$ is accepting exactly when $q$ is accepting in $A_F$ and $Q inter "Final"(P_N)=emptyset$.
]

The invariant follows by induction over the fragment tree. The induction step is exactly the context evaluation above: holes use the induction hypotheses for the child trees, and graph nodes apply the transition relation of $P_N$ to the resulting child residuals. The acceptance condition then subtracts the bad-source language. It retains $t$ precisely when $A_F$ accepts $t$ and $P_N$ does not accept $h(t)$.

== Principal Rust types

#table(
  columns: (1.15fr, 1.35fr, 2.5fr),
  inset: 5pt,
  stroke: 0.4pt + rgb("#c8c8c8"),
  [*Concept*], [*Rust representation*], [*Role*],
  [$A_F$], [`Chart` / `FragmentAutomaton`], [The source automaton over fragment symbols.],
  [$h$ and $A_N$], [`ExpansionBuilder` / `NodeExpansion`], [The expansion map and the resulting automaton over graph-node symbols.],
  [$T_R$], [`Ctt`, `CttRule`, `CttState`], [The transducer that performs exactly one rewrite.],
  [$P_N$], [`PreBuilder`, `PreState`, `LhsDecomposer`], [The automaton for node trees that can rewrite into $L_N$.],
  [$Q="Res"(t)$], [`ResidualId`, `ResidualInterner`], [The set of all $P_N$ states reached by the expanded fragment tree, stored once and referred to by an integer.],
  [$(q,Q)$], [`DifferenceBuilder`, `DerivedState`], [A difference state pairing one source-chart state with one residual.],
  [trimming], [`GeneratedBuilder`], [Removal of states and transitions that cannot occur in an accepting run.],
)

The internal names in this table correspond closely to the mathematical objects. A `PreState` stores one worklist pair $(c,q)$ from Section 2.4. When the left-hand side of a CTT rule contains several internal node positions, `LhsDecomposer` must keep those positions distinct even if they happen to reach the same state of $A_N$. It therefore assigns each position a distinct integer within the compiled rule. Variables are also numbered within the rule, so that the target state matched for a variable can be stored in an array at that variable's number.

Filtering does not modify the source chart. The result reuses the same dominance graph and fragment descriptions. Each difference state $(q,Q)$ retains the source state $q$, so the result can associate it with the same part of the dominance graph. For each source state, `DifferenceBuilder` records all difference states whose first component is that source state. These are the alternatives from which parent transitions choose their children during the bottom-up construction.

== Construction order and trimming

The fragment difference is constructed bottom-up. A state is called #emph[productive] if at least one tree reaches it. Before the algorithm processes a state $q$ of the source chart, it has completed all result states obtained from the possible child states of $q$. It creates a new result state only from a transition whose children are already productive. Every state created by this construction is therefore productive.

Productivity alone does not imply that a state contributes to the accepted language. A state is #emph[coaccessible] if it can occur inside some tree whose run ends in an accepting state. After the bottom-up construction, a top-down pass starts at the accepting states and marks the states that can occur below them. Trimming keeps exactly the states that are both productive and coaccessible. For the fragment difference, productivity is guaranteed by construction, so only the coaccessibility pass is needed. The preimage is constructed backwards from accepting target states and does not have the same guarantee; it therefore receives the general trim that checks both properties.

The optimized builder exploits this construction order as described in Section 4.

== Evaluating fragment contexts

`FragmentEvaluator` handles an arbitrary fragment context by following its tree structure. At a hole, it uses the residual set of the fragment-tree child inserted there. At a graph-node symbol, it computes the states that $P_N$ can reach from the residual sets obtained for that node's children.

On `rondane-650`, almost all evaluations concern a context that consists of one graph-node symbol whose children are holes. `BatchedEvaluator` handles all child alternatives of such a context together. Contexts containing additional internal graph nodes continue to use `FragmentEvaluator`. This distinction changes only the evaluation strategy; both paths implement the residual operation defined above.

= High-level algorithm improvements in `utool-rust`

The changes in this section are new relative to the initial Rust implementation, not to the Koller--Thater filtering method. Sparse difference evaluation recovers, and generalizes to fragment contexts, work avoidance that Java's `differenceSpecialized` already achieved with a different index.

These changes affect the structure of the computation. They reduce the number of transitions considered, share one search across several required results, or avoid indexes for intermediate transitions that trimming discards. Section 5 separately discusses storage choices that make an unchanged computation cheaper.

== Diagnosis of the twelve-second bottleneck

In the initial Rust implementation, the source chart had 9,077 states and 74,977 transitions. The trimmed preimage automaton had about 400,000 states and 919,850 transitions. Constructing and trimming this preimage took about one second. The fragment difference took a further 12.7 seconds and dominated the 14.8-second command.

The expensive operation was the evaluation of a node symbol over several child residual sets. The initial evaluator formed every tuple in the Cartesian product of those sets and asked whether $P_N$ contained a transition for that tuple. Since $P_N$ is sparse, most tuples did not correspond to a transition. For the evaluations later assigned to the sibling finder, these products contained more than 1.56 billion tuples.

Writing the result transitions was not the cause of the delay. A diagnostic run that suppressed those writes changed the difference time only from 12.72 to 12.57 seconds. Although trimming eventually retained only 26,560 of 734,666 generated transitions, the evaluator had already paid for the Cartesian searches needed to discover their result states. The required optimization was therefore to change how residual transitions were found, not merely how the resulting automaton was stored.

This bottleneck was specific to the initial Rust implementation. Java's `differenceSpecialized` already restricted its search to structurally compatible preimage transitions. The Rust implementation required a corresponding sparse method that could evaluate complete fragment contexts.

== Sparse residual evaluation with a sibling finder

Evaluating a graph-node symbol $u$ with child residuals $Q_1, dots, Q_k$ requires the set

$ { q | exists q_1 in Q_1, dots, q_k in Q_k: u (q_1,dots,q_k) arrow.r q }. $

The initial method enumerated the Cartesian product $Q_1 times dots times Q_k$ and performed an exact transition lookup for every tuple. Most tuples had no corresponding transition in the sparse preimage automaton.

The replacement adapts the sibling-finder technique introduced by Groschwitz, Koller, and Johnson for efficient tree-automata intersection #cite("groschwitz-etal-2016-efficient"). A sibling finder indexes automaton transitions by a child position and the state occurring at that position. Given one child state, it returns the existing transitions in which that state occurs, together with the states required at the other child positions. It therefore enumerates transitions that actually exist instead of tuples that might exist.

Java's specialized difference and the sibling finder are both sparse joins, but their indexes answer different questions. The Java index fixes the tuple of target-chart components stored inside preimage states. The Rust index can start with any state of $P_N$ at any child position. This is necessary because a Rust residual contains several possible states, and any one of them can provide the first indexed lookup.

Let $Delta_N$ be the set of transitions of $P_N$. The #emph[positional degree] of a state $p$ at child position $i$ is

$ "deg"_i (p) = |{ r in Delta_N | "the child of" r "at position" i "is" p }|. $

Thus $"deg"_i (p)$ is the number of transitions in which $p$ occurs as child $i$; it is not the graph-theoretic degree of $p$. For a residual set $Q_i$, the number of index entries that a search from position $i$ would inspect is estimated by

$ "cost"_i (Q_i) = sum_(p in Q_i) "deg"_i (p). $

The algorithm starts its search at the child position with the smallest estimated cost. It retrieves the indexed transitions associated with the states in that residual, and then checks whether each retrieved transition uses a member of every other child residual. Residual sets are stored in sorted order, so these membership tests use binary search. The cost estimate accounts for both the size of a residual and the number of transitions associated with its members. A larger residual may therefore be the better starting point when its states occur in few transitions.

The resulting operation is a sparse join: it iterates over existing transitions of $P_N$ and tests their children. The former operation iterated over all possible child-state tuples and tested whether a transition existed. The degree-based choice of the starting position extends the sibling-finder idea to residual sets of different sizes and to symbols of arbitrary rank.

Across the 462,063 evaluations handled by the sibling finder, the corresponding Cartesian products contained 1,560,608,589 tuples. The degree-guided search inspected 8,791,940 existing transitions and found 2,566,492 that matched all child residuals. It reduced the number of candidates considered at these evaluations by a factor of about 177, and the complete command fell to 2.24--2.59 seconds. Products containing at most four tuples still use direct lookup because a sibling-finder query has a larger fixed cost at that scale.

== Restricting preimage construction by output symbol

When the preimage construction processes a pair $(c,q)$, it needs only CTT rules that could produce a tree reaching $q$. The node automaton $A_N$ records which graph-node symbols can occur at the root of such a tree. The CTT is therefore indexed by its control state and by the root symbol of a rule's output pattern. For $(c,q)$, the construction considers rules in control state $c$ whose output begins with an available root symbol. It also considers rules whose output consists of a variable, since a variable can match a tree with any root.

This index removes CTT rules that cannot match before the more expensive pattern traversal begins. The target states associated with rule variables are stored in arrays indexed by the variables' local numbers, as described in Section 3.3.

The trimming implementation also uses the construction invariant from Section 3.4. Since every state generated by the bottom-up difference is productive, its trim performs only the top-down coaccessibility pass. The preimage trim continues to test both productivity and coaccessibility.

== Batched sibling joins

The sibling finder makes one residual evaluation sparse, but a chart transition generally requires many residual evaluations. Consider $F (q_1,dots,q_k) arrow.r q$. Each child state $q_i$ may occur in several result states $(q_i,Q_i^1), (q_i,Q_i^2), dots$. The difference construction must choose one such result state for each child and evaluate every combination, because different choices can represent different fragment trees. Running the sibling finder separately for every combination repeats many of the same index searches.

For a one-node fragment context, the current implementation evaluates these combinations together. At each child position, `StateIncidence` maps a state $p$ of $P_N$ to the child alternatives whose residual sets contain $p$. `BatchedEvaluator` keeps one result cell for each required combination of child alternatives. It then scans a compatible transition of $P_N$ once. The incidence maps identify exactly which result cells contain all of that transition's child states, and the transition's parent state is added to those cells.

Batching does not omit any combination. It shares transition lookup across combinations. A state of $P_N$ that occurs in several residual alternatives causes one index scan for the complete source transition, rather than one scan for every combination containing that state.

#query-example[
Suppose the left child has alternatives $d_0,d_1$ with residuals ${p_0,p_1}$ and ${p_2}$, while the right child has alternatives $e_0,e_1$ with residuals ${r_0}$ and ${r_1,r_2}$. There are four required result cells: $(d_0,e_0)$, $(d_0,e_1)$, $(d_1,e_0)$, and $(d_1,e_1)$.

For a transition $u (p_1,r_1) arrow.r s$ of $P_N$, the left incidence map reports that $p_1$ occurs in $d_0$, and the right map reports that $r_1$ occurs in $e_1$. The transition therefore contributes $s$ only to the residual for $(d_0,e_1)$. One scan determines its contribution to all four cells.
]

Single-node fragment contexts account for 728,219 of the 734,666 combinations on `rondane-650`, or 99.1%. Without batching, their evaluation inspected 8,772,166 entries in the sibling-finder index, in addition to 794,435 direct probes for very small products. Batching reduced the estimated index entries inspected to 1,462,649 while producing the same 4,042,978 contributions to result cells.

== Building indexes after trimming

The preimage and difference constructions generate many transitions that trimming later removes. A conventional builder immediately creates the indexes needed to query every generated transition, and trimming then builds a second indexed automaton containing only the useful part. This performs indexing work twice and indexes transitions that are never queried after trimming.

`GeneratedBuilder` first stores generated transitions as plain records. It computes which states and transitions can participate in an accepting run, assigns consecutive integer numbers to the retained states, and then constructs the query indexes once for the retained automaton. The accepted language is unchanged; only the discarded part avoids full indexing.

This organization cannot avoid generating every discarded transition. During a bottom-up construction, the algorithm knows that a newly generated state is productive, but it does not yet know whether the state is coaccessible. That information becomes available only in the subsequent top-down pass. `GeneratedBuilder` therefore avoids indexing and rebuilding discarded transitions, but it cannot avoid the residual evaluations that generated them.

== Deduplicating rewrite work

Exact duplicate rewrite rules are removed after parsing. The backward preimage construction can also reach the same generated transition along several paths, so it removes duplicate preimage transitions before passing them to the staging builder. In both cases, deduplication removes identical work items and does not change the accepted language.

= Low-level representation optimizations in `utool-rust`

The preceding changes reduce the number of candidates considered or the number of structures built. The changes in this section leave that work unchanged. They reduce the cost of representing states, residual sets, and short sequences.

== Dense state-indexed data

Internally, an automaton state is represented by a nonnegative integer called `StateId`, and the identifiers in each completed automaton are consecutive. Data with exactly one entry per state can therefore be stored in a `Vec` and accessed directly by this integer. The implementation uses such arrays for productivity and coaccessibility marks, links back to source-chart states, sibling-index data, and top-down transitions. This avoids hashing a state identifier at every access.

== Residual interning

A residual set may recur at many result states. The implementation first puts each residual into a #emph[canonical form] by sorting its state numbers and removing duplicates. It then #emph[interns] the residual: one stored copy receives an integer `ResidualId`, and every occurrence of the same canonical set receives the same identifier. A result state consequently stores the fixed-size pair `(StateId, ResidualId)` instead of owning a separate vector of preimage states. Passing a residual through a fragment hole copies only its identifier.

On the benchmark, 740,302 node-transition evaluations produced only 86,541 distinct residual sets. Interning shares this repeated output data. By contrast, 734,667 evaluations had distinct inputs, so caching complete input--output evaluations would offer little reuse.

== Hashing and short vectors

The hash tables in the filtering implementation contain integer-based internal keys rather than untrusted input strings. They use the faster non-cryptographic `FxHash` algorithm. Short sequences such as transition children, variable bindings, and temporary residual results use `SmallVec`, which stores a small number of elements inside the surrounding value and allocates separate heap memory only when that capacity is exceeded. Neither choice changes which transitions are considered.

Because canonical residuals are sorted, sibling joins can test membership by binary search. Canonicalization also ensures that two equal residual sets receive the same `ResidualId` regardless of the order in which their elements were discovered.

= Performance on `rondane-650`

== Measurement protocol

The benchmark used the release binary directly on an Apple M4 Pro:

```sh
./target/release/utool solvable -s \
  -f ../stefan-2026/equivalences.rewrite \
  ../src/main/resources/examples/rondane-650.mrs.pl
```

All configurations reported below produced a filtered chart with 26,560 transitions representing exactly 1,956,116 solved forms. These counts provide a semantic check across the measurements. The early measurements report the complete command because separate phase timings were not yet available. The final measurements use repeated runs of the release binary, omit the first run so that startup effects do not affect the median, and report the median of the remaining runs.

The following table gives cumulative measurements for selected configurations. The measurement scope differs between the early and final rows, and each configuration may include several retained changes. The values therefore establish the scale of the two main outcomes—the removal of Cartesian work and the later reduction in overhead—but not an isolated speedup for every individual change.

#table(
  columns: (1.7fr, 1fr, 3fr),
  inset: 5pt,
  stroke: 0.4pt + rgb("#c8c8c8"),
  table.header([*Configuration*], [*Observed time*], [*Principal property*]),
  [Initial Rust implementation], [12.7 s difference; 14.8 s full], [Residual evaluation tests more than 1.56 billion possible child tuples.],
  [Sparse residual evaluation], [2.24--2.59 s full], [The sibling finder inspects 8.79 million existing transitions instead.],
  [Restricted preimage and interned residuals], [1.48 s full], [Output-symbol lookup reduces preimage work, and equal residual sets share one stored copy.],
  [Cheaper internal storage], [1.20 s full], [`FxHash` and `SmallVec` reduce hashing and allocation cost.],
  [Indexes built after trimming], [1.07 s full], [Only retained transitions receive full automaton indexes.],
  [Batched residual evaluation], [0.92 s full], [Sibling-finder searches are shared across alternatives of one chart transition.],
  [Current implementation], [694 ms filter; 0.79--0.80 s full], [The complete command is about eighteen times faster than the initial version.],
)

The measurements show two distinct effects. Sparse residual evaluation removed the dominant combinatorial cost and brought the command into the two-second range. The remaining algorithmic and representation changes reduced that time by more than half. In the final measurements, the complete filtering phase took 694 milliseconds, while the full command took 0.79--0.80 seconds. Compared with the initial 14.8-second command, the end-to-end reduction is approximately eighteen-fold.

== Where the final filter spends its time

The final breakdown comes from five warm, instrumented runs of the release binary. The table reports the median of each phase; independently rounded medians need not sum exactly to the displayed total.

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

Within the 395.6-millisecond fragment difference, ordering the source states for bottom-up construction and preparing the sibling-finder index took about 4.2 milliseconds. Residual evaluation took 369.5 milliseconds, and trimming plus construction of the final indexed chart took 15.1 milliseconds. Releasing temporary difference data accounts for most of the remaining roughly 6.8 milliseconds. Residual evaluation is therefore still the largest single operation, but it now occupies about 53% of the complete filter rather than taking 12.7 seconds by itself.

After sparse residual evaluation had removed the main bottleneck, preimage construction and trimming still took about 945 milliseconds. In the final profile they take 286 milliseconds together. Restricting CTT rules by their output symbols and avoiding an unnecessary productivity pass account for the change in the work performed; arrays and compact variable bindings reduce its representation cost.

Over the same interval, the measured difference work fell from roughly 778 milliseconds to 396 milliseconds. Batched joins reduce repeated transition searches, building indexes after trimming avoids indexes for discarded transitions, and residual interning and smaller containers reduce the cost of the data that remains. These phase comparisons combine several retained changes and do not provide an isolated speedup for each one.

== What the measurements say about the algorithm

The final algorithm spends its time on transitions that are plausible under the automata's sparse structure. The sibling finder changes the residual operation from enumeration of possible state tuples to inspection of existing transitions. Batching shares those inspections across result alternatives. Output-directed preimage construction excludes incompatible CTT rules before matching, and delayed index construction avoids full indexes for transitions that cannot contribute to an accepted result.

The representation changes serve this algorithm rather than replacing it. Residual interning is effective because many evaluations converge on the same output set, whereas almost every complete evaluation input is distinct. Consecutive state numbers permit direct array access, and short inline vectors reduce allocation in operations that remain frequent after the candidate count has been reduced.

= Reading and maintaining the code

The implementation is organized in the same dependency order as this note: rewrite syntax and validation, CTT construction, node-chart expansion, inverse image, and fragment-level difference. A code audit should begin at `filter_chart` near the end of `src/filter/mod.rs`, then follow its four calls in order.

Several invariants carry most of the correctness argument:

- node symbols preserve graph-node identity when two nodes carry the same label;
- an accepting CTT run uses exactly one rule whose input and output patterns differ;
- expanded chart transitions are deterministic in both directions used by preimage matching;
- residual sets are sorted, duplicate-free, and interned;
- result states for a parent are created only after the result states for its possible children are complete;
- two result states with the same source state but different residuals must not be merged;
- the difference trim may check only coaccessibility because construction guarantees productivity.

The optimization-specific structures are local. `SiblingDegrees` estimates the cheapest child position from which to start a sibling-finder query. `StateIncidence` maps preimage states to the result alternatives that contain them. `BatchedEvaluator` handles one-node fragment contexts, while `FragmentEvaluator` handles arbitrary fragment contexts. `GeneratedBuilder` is kept in `automata_ext` because building an automaton only after trimming does not depend on Utool graph concepts.

= Conclusion

Relative-normal-form filtering by tree-automata construction is due to Koller and Thater and was already realized in Java Utool. The Rust implementation preserves its essential stages: a fragment chart is expanded to a node automaton, a CTT preimage identifies candidate rewrite sources, and a specialized residual-set difference returns to a filtered fragment chart.

The new result is the performance of this construction in `utool-rust`. Adapting the previously published sibling-finder idea to residual-set evaluation removed the 12.7-second Cartesian bottleneck. Batching shares the remaining sibling-finder searches across chart alternatives. Output-symbol indexes restrict the CTT rules considered during preimage construction, and delayed index construction avoids indexes for discarded automaton transitions. Representation changes make the remaining operations cheaper. Together these changes brought filtering on `rondane-650` to about 0.7 seconds without enumerating the 2.4 trillion solved forms represented by the input chart.

#add-bib-resource(read("references.bib"))
#print-bananote-bibliography()
