package algorithms;

import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class MSTTest {
    private void validateTree(Graph g, MST.Result r) {
        DSU d = new DSU(g.n, new Metrics());
        long sum = 0;
        for (Graph.Edge e : r.edges()) {
            assertTrue(g.edges.contains(e) || g.edges.contains(new Graph.Edge(e.to(), e.from(), e.weight())));
            assertTrue(d.union(e.from(), e.to()));
            sum += e.weight();
        }
        assertEquals(r.weight(), sum);
        assertEquals(r.components(), d.components());
        assertEquals(g.n - r.components(), r.edges().size());
    }

    @Test void hundredConnectedGraphs() {
        Random random = new Random(8);
        for (int t = 0; t < 100; t++) {
            int n = 2 + random.nextInt(60);
            Graph g = new Graph(n, false);
            for (int v = 1; v < n; v++) g.addEdge(random.nextInt(v), v, random.nextInt(101) - 50);
            for (int i = 0; i < 4 * n; i++) g.addEdge(random.nextInt(n), random.nextInt(n), random.nextInt(101) - 50);
            MST.Result a = MST.kruskal(g, new Metrics()), b = MST.prim(g, new Metrics());
            assertEquals(a.weight(), b.weight());
            assertEquals(n - 1, a.edges().size());
            assertEquals(n - 1, b.edges().size());
            validateTree(g, a);
            validateTree(g, b);
        }
    }

    @Test void disconnectedInputAndEmptyGraph() throws Exception {
        Graph g = Graph.read(Path.of("data/disconnected.txt"));
        for (MST.Result r : new MST.Result[]{MST.kruskal(g, new Metrics()), MST.prim(g, new Metrics())}) {
            assertEquals(4, r.weight());
            assertEquals(3, r.components());
            validateTree(g, r);
        }
        Graph empty = new Graph(0, false);
        assertEquals(0, MST.kruskal(empty, new Metrics()).weight());
        assertEquals(0, MST.prim(empty, new Metrics()).components());
        Graph isolated = new Graph(4, false);
        assertEquals(4, MST.kruskal(isolated, new Metrics()).components());
        assertEquals(4, MST.prim(isolated, new Metrics()).components());
        assertThrows(IllegalArgumentException.class, () -> MST.prim(new Graph(2, true), new Metrics()));
    }
}
