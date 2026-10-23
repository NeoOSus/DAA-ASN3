package algorithms;

import metrics.Metrics;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class DSUTest {
    @Test void randomUnionsAgreeWithLabels() {
        int n = 50;
        int[] labels = new int[n];
        for (int i = 0; i < n; i++) labels[i] = i;
        DSU dsu = new DSU(n, new Metrics());
        Random random = new Random(7);
        for (int step = 0; step < 300; step++) {
            int a = random.nextInt(n), b = random.nextInt(n);
            int old = labels[b], replacement = labels[a];
            assertEquals(old != replacement, dsu.union(a, b));
            for (int i = 0; i < n; i++) if (labels[i] == old) labels[i] = replacement;
            for (int i = 0; i < n; i++) for (int j = 0; j < n; j++)
                assertEquals(labels[i] == labels[j], dsu.find(i) == dsu.find(j));
            assertEquals(Arrays.stream(labels).distinct().count(), dsu.components());
        }
        dsu.makeSet(3);
        assertEquals(3, dsu.components());
    }

    @Test void countersAndEmptySet() {
        Metrics m = new Metrics();
        DSU dsu = new DSU(2, m);
        assertTrue(dsu.union(0, 1));
        assertFalse(dsu.union(1, 0));
        assertEquals(4, m.findCalls);
        assertEquals(1, m.successfulUnions);
        assertEquals(5, m.dsuOps());
        assertEquals(0, new DSU(0, new Metrics()).components());
    }
}
