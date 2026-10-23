# Assignment 3 Graphs

Yerkebulan Niyalov, SE2526

Java implementations of DSU, Kruskal, Prim, Kosaraju, Kahn and shortest paths in a DAG. This project covers the required tasks, without the bonus algorithms.

## Requirements and commands

Use JDK 17 or newer and Maven 3.9 or newer. Run commands from the project folder. The first Maven build needs internet access to download dependencies.

```sh
mvn clean package
java -Xmx3g -jar target/graphs-1.0.jar
```

The first command builds the project and runs all JUnit 5 tests. The second runs the full benchmark, checks the reference values and writes `results/results.csv`. Each algorithm is measured five times; the CSV contains the median time and the counters from one run. A mismatch stops the benchmark with an error. The final CSV is replaced only after all checks pass.

To run just the tests:

```sh
mvn test
```

To regenerate the three PNG plots and the five-page report after the benchmark, install Python 3.10 or newer with the packages below and run the script:

```sh
python -m pip install -r requirements.txt
python scripts/report.py
```

The checked-in CSV, plots and report were generated from an actual complete benchmark run on Java 21.0.7. Times will change between runs.

## Files

- `src/main/java/graph`: adjacency-list graph and file input.
- `src/main/java/algorithms`: the six required algorithms.
- `src/main/java/metrics`: operation counters.
- `src/main/java/benchmark`: seeded generator and benchmark.
- `src/test/java/algorithms`: JUnit 5 tests, including reference values and stack safety.
- `data`: disconnected, cyclic and DAG examples, all used in tests.
- `results/results.csv`: 50 rows, five graph algorithms for each of ten size/density combinations.
- `results/plots`: three required PNG charts.
- `REPORT.pdf`: complexity, correctness arguments and experimental analysis.

## Input and results

Input starts with `n m directed` or `n m undirected`, followed by `m` lines of `u v w`. Vertices are numbered from 0. Weights are `int`; total weights and distances use `long` to avoid integer overflow.

`MST.kruskal` and `MST.prim` return the weight, chosen edges and number of components. A disconnected graph returns a forest. `Kosaraju.run` returns a component ID for every vertex and the component count. `Kahn.sort` returns an order, or throws an exception saying that the graph contains a cycle. DAG shortest paths also reject cyclic graphs.

All vertices with in-degree zero are sources for DAG shortest paths. `DagShortestPath.Result.pathTo(v)` restores the path to a vertex. For example, after building the project, start `jshell --class-path target/classes` and enter:

```java
var g = graph.Graph.read(java.nio.file.Path.of("data/dag.txt"));
var r = algorithms.DagShortestPath.run(g, new metrics.Metrics());
System.out.println(r.pathTo(4));
System.out.println(r.distance()[4]);
```

This prints `[1, 2, 3, 4]` and `-4`.

## Counters and assignment details

Counters are incremented inside the algorithms, not calculated from complexity formulas. `dsu_ops` is the number of `find` calls plus successful unions. `comparisons` counts weight comparisons in sorting and the priority queue. `max_depth` is the largest explicit DFS stack size.

To follow the reference counts, `edge_scans` counts algorithm traversal work: Kosaraju's two DFS passes, Kahn's queue traversal and the DAG relaxation pass. Building the reverse graph, initial in-degree calculation, topological preprocessing for paths and external validation are excluded from this counter. These steps still take time and are included in the stated time complexity. Prim scans each adjacency entry, so an undirected edge is scanned twice. Kruskal stops when one component remains; on disconnected graphs it examines the full sorted list.

The generator creates a new `Random(42)` for each graph. If random endpoints coincide, it redraws only `b` until `b != a`. The resulting graphs match all ten reference rows. The detailed benchmark section specifies ten size/density combinations and 50 CSV rows; this is used instead of the inconsistent phrase "four generated graphs" in the deliverables list.

The report shows the fitted slopes of edge scans and also of Kruskal's sorting comparisons. Sorting gives Kruskal its logarithmic factor; its edge-scan counter alone does not measure sorting.

## Git and submission

The local repository contains `main`, `feature/dsu`, `feature/mst`, `feature/scc`, `feature/paths`, `feature/metrics` and release tag `v1.0`. The ZIP includes the local `.git` directory to preserve this history. No remote GitHub repository has been created. The assignment also asks for a GitHub link; add that separately when submitting to Moodle.

## AI use acknowledgement

OpenAI Codex was used to generate the initial implementations, tests, benchmark, plotting script and report, and to run the checks and fix issues. This assistance is disclosed rather than presented as unaided work. The submitted code and explanations should be reviewed and understood before the defense.
