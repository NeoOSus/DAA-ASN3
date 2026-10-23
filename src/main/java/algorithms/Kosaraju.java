package algorithms;

import graph.Graph;
import metrics.Metrics;
import java.util.ArrayDeque;
import java.util.Arrays;

public class Kosaraju {
    public record Result(int[] component, int count) {}

    public static Result run(Graph g, Metrics metrics) {
        if (!g.directed) throw new IllegalArgumentException("SCC needs a directed graph");
        boolean[] visited = new boolean[g.n];
        int[] nextEdge = new int[g.n], order = new int[g.n];
        int finished = 0;
        ArrayDeque<Integer> stack = new ArrayDeque<>();
        for (int start = 0; start < g.n; start++) {
            if (visited[start]) continue;
            visited[start] = true;
            stack.push(start);
            metrics.depth(stack.size());
            while (!stack.isEmpty()) {
                int v = stack.peek();
                if (nextEdge[v] < g.adj.get(v).size()) {
                    int to = g.adj.get(v).get(nextEdge[v]++).to();
                    metrics.edgeScans++;
                    if (!visited[to]) {
                        visited[to] = true;
                        stack.push(to);
                        metrics.depth(stack.size());
                    }
                } else {
                    stack.pop();
                    order[finished++] = v;
                }
            }
        }
        Graph reversed = g.reverse();
        int[] component = new int[g.n];
        Arrays.fill(component, -1);
        int count = 0;
        for (int i = finished - 1; i >= 0; i--) {
            int start = order[i];
            if (component[start] != -1) continue;
            component[start] = count;
            stack.push(start);
            metrics.depth(stack.size());
            while (!stack.isEmpty()) {
                int v = stack.pop();
                for (Graph.Edge e : reversed.adj.get(v)) {
                    metrics.edgeScans++;
                    if (component[e.to()] == -1) {
                        component[e.to()] = count;
                        stack.push(e.to());
                        metrics.depth(stack.size());
                    }
                }
            }
            count++;
        }
        return new Result(component, count);
    }
}
