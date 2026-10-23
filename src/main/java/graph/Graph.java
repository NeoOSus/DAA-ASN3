package graph;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Graph {
    public record Edge(int from, int to, int weight) {}
    public final int n;
    public final boolean directed;
    public final List<List<Edge>> adj;
    public final List<Edge> edges = new ArrayList<>();

    public Graph(int n, boolean directed) {
        if (n < 0) throw new IllegalArgumentException("Negative vertex count");
        this.n = n;
        this.directed = directed;
        adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    }

    public void addEdge(int u, int v, int w) {
        if (u < 0 || u >= n || v < 0 || v >= n)
            throw new IllegalArgumentException("Invalid vertex");
        Edge e = new Edge(u, v, w);
        edges.add(e);
        adj.get(u).add(e);
        if (!directed) adj.get(v).add(new Edge(v, u, w));
    }

    public Graph reverse() {
        Graph reversed = new Graph(n, directed);
        for (Edge e : edges) reversed.addEdge(e.to(), e.from(), e.weight());
        return reversed;
    }

    public static Graph read(Path path) throws IOException {
        try (Scanner in = new Scanner(path)) {
            int n = in.nextInt(), m = in.nextInt();
            String type = in.next();
            if (!type.equals("directed") && !type.equals("undirected"))
                throw new IllegalArgumentException("Expected directed or undirected");
            if (m < 0) throw new IllegalArgumentException("Negative edge count");
            Graph g = new Graph(n, type.equals("directed"));
            for (int i = 0; i < m; i++) g.addEdge(in.nextInt(), in.nextInt(), in.nextInt());
            if (in.hasNext()) throw new IllegalArgumentException("Extra input after edges");
            return g;
        }
    }
}
