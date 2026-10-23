package algorithms;

import benchmark.Generator;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class GeneratorTest {
    @Test void smallestReferenceValues() {
        int[] densities = {4, 20};
        long[] weights = {147970, 30363}, sums = {396274, 252411};
        int[] counts = {27, 1};
        for (int i = 0; i < densities.length; i++) {
            int m = densities[i] * 1000;
            Graph g = Generator.generate(1000, m, Generator.Kind.UNDIRECTED);
            assertEquals(weights[i], MST.kruskal(g, new Metrics()).weight());
            assertEquals(weights[i], MST.prim(g, new Metrics()).weight());
            g = Generator.generate(1000, m, Generator.Kind.DIRECTED);
            assertEquals(counts[i], Kosaraju.run(g, new Metrics()).count());
            g = Generator.generate(1000, m, Generator.Kind.DAG);
            assertTrue(Kahn.isValid(g, Kahn.sort(g, new Metrics())));
            assertEquals(sums[i], Arrays.stream(DagShortestPath.run(g, new Metrics()).distance()).sum());
        }
    }
}
