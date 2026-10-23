package algorithms;

import graph.Graph;
import metrics.Metrics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DagShortestPath {
    public static final long INF = Long.MAX_VALUE / 4;
    public record Result(long[] distance, int[] parent) {
        public List<Integer> pathTo(int vertex) {
            List<Integer> path = new ArrayList<>();
            if (distance[vertex] == INF) return path;
            for (int v = vertex; v != -1; v = parent[v]) path.add(v);
            Collections.reverse(path);
            return path;
        }
    }

    public static Result run(Graph g, Metrics metrics) {
        // The relaxation counter excludes topological preprocessing.
        int[] order = Kahn.sort(g, new Metrics());
        int[] degree = new int[g.n], parent = new int[g.n];
        long[] distance = new long[g.n];
        Arrays.fill(parent, -1);
        Arrays.fill(distance, INF);
        for (Graph.Edge e : g.edges) degree[e.to()]++;
        for (int v = 0; v < g.n; v++) if (degree[v] == 0) distance[v] = 0;
        for (int v : order) for (Graph.Edge e : g.adj.get(v)) {
            metrics.edgeScans++;
            if (distance[v] != INF && distance[v] + e.weight() < distance[e.to()]) {
                distance[e.to()] = distance[v] + e.weight();
                parent[e.to()] = v;
            }
        }
        return new Result(distance, parent);
    }
}
