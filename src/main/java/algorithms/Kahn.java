package algorithms;

import graph.Graph;
import metrics.Metrics;
import java.util.ArrayDeque;

public class Kahn {
    public static int[] sort(Graph g, Metrics metrics) {
        if (!g.directed) throw new IllegalArgumentException("Topological order needs a directed graph");
        int[] degree = new int[g.n];
        // Setup is separate from traversal edgeScans, as in the assignment's m reference.
        for (Graph.Edge e : g.edges) degree[e.to()]++;
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int v = 0; v < g.n; v++) if (degree[v] == 0) queue.add(v);
        int[] order = new int[g.n];
        int size = 0;
        while (!queue.isEmpty()) {
            int v = queue.remove();
            order[size++] = v;
            for (Graph.Edge e : g.adj.get(v)) {
                metrics.edgeScans++;
                if (--degree[e.to()] == 0) queue.add(e.to());
            }
        }
        if (size != g.n) throw new IllegalArgumentException("Graph contains a cycle");
        return order;
    }

    public static boolean isValid(Graph g, int[] order) {
        if (order.length != g.n) return false;
        int[] position = new int[g.n];
        boolean[] seen = new boolean[g.n];
        for (int i = 0; i < order.length; i++) {
            int v = order[i];
            if (v < 0 || v >= g.n || seen[v]) return false;
            seen[v] = true;
            position[v] = i;
        }
        for (Graph.Edge e : g.edges) if (position[e.from()] >= position[e.to()]) return false;
        return true;
    }
}
