package benchmark;

import algorithms.*;
import graph.Graph;
import metrics.Metrics;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Function;

public class Benchmark {
    private static final int[] SIZES = {1000, 3162, 10000, 31623, 100000};
    private static final long[] MST_REFERENCE = {147970,30363,468911,97589,1486139,309550,4715515,968517,15012094,3056681};
    private static final long[] SCC_REFERENCE = {27,1,137,1,373,1,1188,1,3923,1};
    private static final long[] DAG_REFERENCE = {396274,252411,1271911,723157,3941828,2381342,12551891,7376612,40096539,23512087};
    private record Run(long result, Metrics metrics, double time) {}

    public static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static Run measure(Function<Metrics, Long> algorithm) {
        long[] times = new long[5];
        Metrics first = null;
        long value = 0;
        for (int i = 0; i < times.length; i++) {
            Metrics current = new Metrics();
            long start = System.nanoTime();
            long result = algorithm.apply(current);
            times[i] = System.nanoTime() - start;
            if (i == 0) { first = current; value = result; }
            else require(value == result && first.dsuOps() == current.dsuOps() &&
                    first.edgeScans == current.edgeScans && first.comparisons == current.comparisons &&
                    first.maxDepth == current.maxDepth, "Non-reproducible run");
        }
        Arrays.sort(times);
        return new Run(value, first, times[2] / 1_000_000.0);
    }

    private static void row(PrintWriter out, String algorithm, String name, Graph g, Run run) {
        Metrics m = run.metrics();
        out.printf(Locale.ROOT, "%s,%s,%d,%d,%.3f,%d,%d,%d,%d,%d%n", algorithm, name,
                g.n, g.edges.size(), run.time(), m.dsuOps(), m.edgeScans, m.comparisons, m.maxDepth, run.result());
        out.flush();
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("results"));
        // Replace the CSV only after all reference checks pass.
        Path temporary = Path.of("results/results.tmp");
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(temporary))) {
            out.println("algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result");
            int index = 0;
            for (int n : SIZES) for (int density : new int[]{4, 20}) {
                int m = density * n;
                String densityName = density == 4 ? "sparse" : "dense";
                Graph undirected = Generator.generate(n, m, Generator.Kind.UNDIRECTED);
                Run kruskal = measure(metrics -> {
                    MST.Result r = MST.kruskal(undirected, metrics);
                    require(r.components() == 1 && r.edges().size() == n - 1, "Invalid Kruskal tree");
                    return r.weight();
                });
                Run prim = measure(metrics -> {
                    MST.Result r = MST.prim(undirected, metrics);
                    require(r.components() == 1 && r.edges().size() == n - 1, "Invalid Prim tree");
                    return r.weight();
                });
                require(kruskal.result() == prim.result(), "MST algorithms disagree");
                require(kruskal.result() == MST_REFERENCE[index], "MST reference mismatch at " + n + "/" + m + ": " + kruskal.result());
                row(out, "Kruskal", "undirected_" + densityName, undirected, kruskal);
                row(out, "Prim", "undirected_" + densityName, undirected, prim);

                Graph directed = Generator.generate(n, m, Generator.Kind.DIRECTED);
                Run scc = measure(metrics -> (long) Kosaraju.run(directed, metrics).count());
                require(scc.result() == SCC_REFERENCE[index], "SCC reference mismatch at " + n + "/" + m + ": " + scc.result());
                require(scc.metrics().edgeScans == 2L * m, "Kosaraju must scan 2m");
                row(out, "Kosaraju", "directed_" + densityName, directed, scc);

                Graph dag = Generator.generate(n, m, Generator.Kind.DAG);
                require(Kahn.isValid(dag, Kahn.sort(dag, new Metrics())), "Invalid topological order");
                Run kahn = measure(metrics -> (long) Kahn.sort(dag, metrics).length);
                Run paths = measure(metrics -> Arrays.stream(DagShortestPath.run(dag, metrics).distance()).sum());
                require(paths.result() == DAG_REFERENCE[index], "DAG reference mismatch at " + n + "/" + m + ": " + paths.result());
                require(kahn.result() == n && kahn.metrics().edgeScans == m && paths.metrics().edgeScans == m, "Invalid DAG counts");
                row(out, "Kahn", "dag_" + densityName, dag, kahn);
                row(out, "DagShortestPath", "dag_" + densityName, dag, paths);
                System.out.printf("n=%d m=%d MST=%d SCC=%d DAG=%d OK%n", n, m, kruskal.result(), scc.result(), paths.result());
                index++;
            }
        }
        Files.move(temporary, Path.of("results/results.csv"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        System.out.println("All reference values matched. Wrote 50 rows to results/results.csv");
    }
}
