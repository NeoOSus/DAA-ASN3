package benchmark;

import graph.Graph;
import java.util.Random;

public class Generator {
    public enum Kind { UNDIRECTED, DIRECTED, DAG }

    public static Graph generate(int n, int m, Kind kind) {
        if (n < 2 || m < 0 || (kind == Kind.UNDIRECTED && m < n - 1))
            throw new IllegalArgumentException("Invalid benchmark size");
        Random random = new Random(42);
        Graph graph = new Graph(n, kind != Kind.UNDIRECTED);
        int[] perm = new int[n];
        for (int i = 0; i < n; i++) perm[i] = i;
        if (kind != Kind.DIRECTED) {
            for (int i = n - 1; i > 0; i--) {
                int j = random.nextInt(i + 1), temp = perm[i];
                perm[i] = perm[j]; perm[j] = temp;
            }
        }
        if (kind == Kind.UNDIRECTED) {
            for (int i = 1; i < n; i++)
                graph.addEdge(perm[random.nextInt(i)], perm[i], 1 + random.nextInt(1000));
        }
        while (graph.edges.size() < m) {
            int a = random.nextInt(n), b = random.nextInt(n);
            while (a == b) b = random.nextInt(n);
            if (kind == Kind.DAG && perm[a] > perm[b]) { int temp = a; a = b; b = temp; }
            graph.addEdge(a, b, 1 + random.nextInt(1000));
        }
        return graph;
    }
}
