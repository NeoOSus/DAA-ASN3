package algorithms;

import graph.Graph;
import graph.Graph.Edge;
import metrics.Metrics;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class MST {
    public record Result(long weight, List<Edge> edges, int components) {}

    public static Result kruskal(Graph g, Metrics metrics) {
        if (g.directed) throw new IllegalArgumentException("MST needs an undirected graph");
        List<Edge> sorted = new ArrayList<>(g.edges);
        sorted.sort((a, b) -> {
            metrics.comparisons++;
            return Integer.compare(a.weight(), b.weight());
        });
        DSU dsu = new DSU(g.n, metrics);
        List<Edge> tree = new ArrayList<>();
        long weight = 0;
        for (Edge e : sorted) {
            if (dsu.components() <= 1) break;
            metrics.edgeScans++;
            if (dsu.find(e.from()) != dsu.find(e.to())) {
                dsu.union(e.from(), e.to());
                tree.add(e);
                weight += e.weight();
            }
        }
        return new Result(weight, tree, dsu.components());
    }

    public static Result prim(Graph g, Metrics metrics) {
        if (g.directed) throw new IllegalArgumentException("MST needs an undirected graph");
        boolean[] visited = new boolean[g.n];
        PriorityQueue<Edge> queue = new PriorityQueue<>((a, b) -> {
            metrics.comparisons++;
            int result = Integer.compare(a.weight(), b.weight());
            if (result == 0) result = Integer.compare(a.from(), b.from());
            if (result == 0) result = Integer.compare(a.to(), b.to());
            return result;
        });
        List<Edge> tree = new ArrayList<>();
        long weight = 0;
        int components = 0;
        // Start at 0, then restart at each unvisited component.
        for (int start = 0; start < g.n; start++) {
            if (visited[start]) continue;
            components++;
            queue.add(new Edge(-1, start, 0));
            while (!queue.isEmpty()) {
                Edge chosen = queue.remove();
                int v = chosen.to();
                if (visited[v]) continue;
                visited[v] = true;
                if (chosen.from() != -1) { tree.add(chosen); weight += chosen.weight(); }
                for (Edge e : g.adj.get(v)) {
                    metrics.edgeScans++;
                    if (!visited[e.to()]) queue.add(e);
                }
            }
        }
        return new Result(weight, tree, components);
    }
}
