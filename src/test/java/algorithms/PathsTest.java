package algorithms;

import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class PathsTest {
    private long[] bellmanFord(Graph g) {
        long[] distance = new long[g.n];
        Arrays.fill(distance, DagShortestPath.INF);
        int[] degree = new int[g.n];
        for (Graph.Edge e : g.edges) degree[e.to()]++;
        for (int v = 0; v < g.n; v++) if (degree[v] == 0) distance[v] = 0;
        for (int pass = 0; pass < g.n - 1; pass++) {
            boolean changed = false;
            for (Graph.Edge e : g.edges) {
                if (distance[e.from()] != DagShortestPath.INF && distance[e.from()] + e.weight() < distance[e.to()]) {
                    distance[e.to()] = distance[e.from()] + e.weight();
                    changed = true;
                }
            }
            if (!changed) break;
        }
        return distance;
    }

    @Test void randomDagsWithNegativeWeights() {
        Random random = new Random(10);
        for (int t = 0; t < 100; t++) {
            int n = 2 + random.nextInt(50);
            Graph g = new Graph(n, true);
            int[] rank = new int[n];
            for (int i = 0; i < n; i++) rank[i] = i;
            for (int i = n - 1; i > 0; i--) {
                int j = random.nextInt(i + 1), temp = rank[i];
                rank[i] = rank[j]; rank[j] = temp;
            }
            for (int i = 0; i < 4 * n; i++) {
                int a = random.nextInt(n), b = random.nextInt(n);
                if (a == b) continue;
                if (rank[a] > rank[b]) { int temp = a; a = b; b = temp; }
                g.addEdge(a, b, random.nextInt(101) - 50);
            }
            Metrics metrics = new Metrics();
            DagShortestPath.Result result = DagShortestPath.run(g, metrics);
            assertArrayEquals(bellmanFord(g), result.distance());
            assertEquals(g.edges.size(), metrics.edgeScans);
            assertTrue(Kahn.isValid(g, Kahn.sort(g, new Metrics())));
            for (int v = 0; v < n; v++) {
                List<Integer> path = result.pathTo(v);
                assertEquals(v, path.get(path.size() - 1).intValue());
                int source = path.get(0);
                assertTrue(g.edges.stream().noneMatch(e -> e.to() == source));
                for (int i = 1; i < path.size(); i++) {
                    int from = path.get(i - 1), to = path.get(i);
                    assertTrue(g.adj.get(from).stream().anyMatch(e -> e.to() == to &&
                            result.distance()[from] + e.weight() == result.distance()[to]));
                }
            }
        }
    }

    @Test void multipleSourcesPathAndCycle() throws Exception {
        Graph g = Graph.read(Path.of("data/dag.txt"));
        DagShortestPath.Result r = DagShortestPath.run(g, new Metrics());
        assertArrayEquals(new long[]{0, 0, -2, 1, -4, 0}, r.distance());
        assertEquals(List.of(1, 2, 3, 4), r.pathTo(4));
        assertEquals(List.of(5), r.pathTo(5));
        Graph cyclic = Graph.read(Path.of("data/cyclic.txt"));
        assertThrows(IllegalArgumentException.class, () -> DagShortestPath.run(cyclic, new Metrics()));
        assertEquals(0, DagShortestPath.run(new Graph(0, true), new Metrics()).distance().length);
    }

    @Test void distanceDoesNotOverflowInt() {
        Graph g = new Graph(3, true);
        g.addEdge(0, 1, Integer.MAX_VALUE);
        g.addEdge(1, 2, Integer.MAX_VALUE);
        assertEquals(2L * Integer.MAX_VALUE, DagShortestPath.run(g, new Metrics()).distance()[2]);
    }
}
