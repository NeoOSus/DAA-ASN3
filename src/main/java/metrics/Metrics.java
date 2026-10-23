package metrics;

public class Metrics {
    public long findCalls, successfulUnions, edgeScans, comparisons;
    public int maxDepth;

    public long dsuOps() { return findCalls + successfulUnions; }
    public void depth(int depth) { maxDepth = Math.max(maxDepth, depth); }
}
