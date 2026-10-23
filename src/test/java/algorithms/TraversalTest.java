package algorithms;

import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class TraversalTest {
    private boolean[] reachable(Graph g, int source) {
        boolean[] seen = new boolean[g.n];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(source);
        seen[source] = true;
        while (!queue.isEmpty()) {
            int v = queue.remove();
            for (Graph.Edge e : g.adj.get(v)) if (!seen[e.to()]) {
                seen[e.to()] = true;
                queue.add(e.to());
            }
        }
        return seen;
    }

    @Test void sccAgainstReachability() {
        Random random = new Random(9);
        for (int t = 0; t < 40; t++) {
            int n = 1 + random.nextInt(200);
            Graph g = new Graph(n, true);
            int m = random.nextInt(5 * n);
            for (int i = 0; i < m; i++) g.addEdge(random.nextInt(n), random.nextInt(n), 1);
            Metrics metrics = new Metrics();
            Kosaraju.Result r = Kosaraju.run(g, metrics);
            Graph reversed = g.reverse();
            for (int u = 0; u < n; u++) {
                boolean[] forward = reachable(g, u), backward = reachable(reversed, u);
                for (int v = 0; v < n; v++)
                    assertEquals(forward[v] && backward[v], r.component()[u] == r.component()[v]);
            }
            assertEquals(r.count(), java.util.Arrays.stream(r.component()).distinct().count());
            assertEquals(2L * m, metrics.edgeScans);
        }
    }

    @Test void inputGraphsAndCycleDetection() throws Exception {
        Graph cyclic = Graph.read(Path.of("data/cyclic.txt"));
        assertEquals(3, Kosaraju.run(cyclic, new Metrics()).count());
        assertThrows(IllegalArgumentException.class, () -> Kahn.sort(cyclic, new Metrics()));
        Graph dag = Graph.read(Path.of("data/dag.txt"));
        Metrics m = new Metrics();
        int[] order = Kahn.sort(dag, m);
        assertTrue(Kahn.isValid(dag, order));
        assertEquals(dag.edges.size(), m.edgeScans);
        assertFalse(Kahn.isValid(dag, new int[dag.n]));
        Graph selfLoop = new Graph(1, true);
        selfLoop.addEdge(0, 0, 1);
        assertThrows(IllegalArgumentException.class, () -> Kahn.sort(selfLoop, new Metrics()));
    }

    @Test void hundredThousandVertexPathIsStackSafe() {
        Graph path = new Graph(100_000, true);
        for (int v = 1; v < path.n; v++) path.addEdge(v - 1, v, 1);
        Metrics m = new Metrics();
        assertEquals(path.n, Kosaraju.run(path, m).count());
        assertEquals(path.n, m.maxDepth);
        assertEquals(2L * (path.n - 1), m.edgeScans);
    }

    @Test void emptyAndIsolatedGraphs() {
        Graph g = new Graph(0, true);
        assertEquals(0, Kosaraju.run(g, new Metrics()).count());
        assertEquals(0, Kahn.sort(g, new Metrics()).length);
        g = new Graph(5, true);
        assertEquals(5, Kosaraju.run(g, new Metrics()).count());
        assertTrue(Kahn.isValid(g, Kahn.sort(g, new Metrics())));
    }
}
