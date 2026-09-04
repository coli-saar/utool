# Command-line interface

Build or run the CLI with `cargo run --bin utool -- …`.

The command-line surface follows Java Utool:

```text
utool solve -O term-prolog example.clls
utool solve -I domcon-oz -O term-oz -f rules.rew --limit 10 -
utool solvable --nochart example.clls
utool convert -I domcon-oz -O domgraph-dot -o example.dg.dot example.clls
utool convert -I chain 3 -O domcon-oz
utool classify example.clls
utool help solve
utool --display-codecs
```

Options may occur before or after the command. Input and output codecs are
inferred from filenames where Java Utool would infer them. `-` denotes standard
input when an explicit input codec is supplied. The intentionally unusual Java
exit convention is preserved: `solve` and `solvable` return 1 for a solvable
graph and 0 for an unsolvable graph; errors use the legacy 128–255 ranges.
