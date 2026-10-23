"""Regenerate the required plots and report from results/results.csv."""
import csv
import math
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import inch
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image, PageBreak

ROOT = Path(__file__).resolve().parents[1]
PLOTS = ROOT / "results" / "plots"
PLOTS.mkdir(parents=True, exist_ok=True)
with (ROOT / "results" / "results.csv").open() as f:
    rows = list(csv.DictReader(f))
assert len(rows) == 50, "Expected a completed 50-row benchmark"
for row in rows:
    for key in row:
        if key not in ("algorithm", "graph"):
            row[key] = float(row[key]) if key == "time_ms" else int(row[key])

ALGORITHMS = ["Kruskal", "Prim", "Kosaraju", "Kahn", "DagShortestPath"]
LABELS = {"DagShortestPath": "DAG paths"}
COLORS = dict(zip(ALGORITHMS, ["#1f77b4", "#d95f02", "#238b45", "#756bb1", "#b22222"]))

def series(algorithm, density):
    return sorted([r for r in rows if r["algorithm"] == algorithm and r["graph"].endswith(density)], key=lambda r: r["n"])

def slope(points, counter):
    x = [math.log(r["n"]) for r in points]
    y = [math.log(r[counter]) for r in points]
    mx, my = sum(x) / len(x), sum(y) / len(y)
    return sum((a - mx) * (b - my) for a, b in zip(x, y)) / sum((a - mx) ** 2 for a in x)

def finish_plot(fig, ax, name):
    ax.set_xlabel("Vertices n")
    ax.grid(True, which="both", alpha=0.22)
    ax.legend(fontsize=8, ncol=2)
    fig.tight_layout()
    fig.savefig(PLOTS / name, dpi=180)
    plt.close(fig)

fig, ax = plt.subplots(figsize=(8, 4.1))
for algorithm in ALGORITHMS:
    for density, style, marker in [("sparse", "-", "o"), ("dense", "--", "s")]:
        points = series(algorithm, density)
        ax.loglog([r["n"] for r in points],
                  [r["dsu_ops"] + r["edge_scans"] + r["comparisons"] for r in points],
                  color=COLORS[algorithm], linestyle=style, marker=marker, markersize=4,
                  label=f"{LABELS.get(algorithm, algorithm)} {density}")
ax.set_ylabel("dsu_ops + edge_scans + comparisons")
ax.set_title("Counted operations (Kahn and DAG paths overlap)")
finish_plot(fig, ax, "operations.png")

fig, ax = plt.subplots(figsize=(8, 4.1))
for algorithm in ALGORITHMS:
    for density, style, marker in [("sparse", "-", "o"), ("dense", "--", "s")]:
        points = series(algorithm, density)
        ax.semilogx([r["n"] for r in points], [r["edge_scans"] / r["m"] for r in points],
                    color=COLORS[algorithm], linestyle=style, marker=marker, markersize=4,
                    label=f"{LABELS.get(algorithm, algorithm)} {density}")
ax.set_ylim(0, 2.15)
ax.set_ylabel("edge_scans / m")
ax.set_title("Scans per edge (several curves overlap at 1 and 2)")
finish_plot(fig, ax, "scans_per_edge.png")

fig, ax = plt.subplots(figsize=(8, 3.8))
for density, style in [("sparse", "o-"), ("dense", "s--")]:
    points = series("Kruskal", density)
    ax.semilogx([r["n"] for r in points], [r["edge_scans"] / r["m"] for r in points], style, label=density)
ax.set_ylim(0, 1.05)
ax.set_ylabel("Kruskal edge_scans / m")
ax.set_title("Share of sorted edges examined by Kruskal")
finish_plot(fig, ax, "kruskal_share.png")

styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name="BodySmall", fontName="Helvetica", fontSize=9.5, leading=13, spaceAfter=7))
styles.add(ParagraphStyle(name="CellSmall", fontName="Helvetica", fontSize=8, leading=10))
styles.add(ParagraphStyle(name="TitleSmall", fontName="Helvetica-Bold", fontSize=17, leading=21, spaceAfter=12))
styles.add(ParagraphStyle(name="CaptionSmall", fontName="Helvetica", fontSize=8, leading=10, alignment=TA_CENTER, spaceAfter=8))
story = []

def p(text):
    story.append(Paragraph(text, styles["BodySmall"]))

def heading(text):
    story.append(Paragraph(text, styles["Heading2"]))

def table(data, widths):
    cells = [[Paragraph(str(c), styles["CellSmall"]) for c in row] for row in data]
    t = Table(cells, colWidths=widths, repeatRows=1, hAlign="LEFT")
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#e8edf2")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#b8c0c8")),
        ("LEFTPADDING", (0, 0), (-1, -1), 5), ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5)]))
    story.append(t)
    story.append(Spacer(1, 8))

def picture(filename, height):
    story.append(Image(str(PLOTS / filename), width=474, height=height))

def page():
    story.append(PageBreak())

story.append(Paragraph("Assignment 3 Graphs", styles["TitleSmall"]))
p("Yerkebulan Niyalov | SE2526 | Design and Analysis of Algorithms")
p("The project implements the required graph algorithms in Java. The full benchmark matches all ten rows of the assignment reference table. Kruskal and Prim agree on every generated graph. The main evidence below is operation counts; execution time is only supporting information.")
heading("1. Implementation and complexity")
p("One adjacency-list Graph class stores directed and undirected graphs. Each logical edge is also kept once in an edge list. DSU uses union by size and iterative path compression. Prim uses a PriorityQueue of edges. Kosaraju uses an explicit stack. Disconnected MST inputs return a forest, and DAG algorithms reject cycles. Weights are int, but totals and distances are long.")
table([
    ["Algorithm", "Time", "Extra space", "Reason"],
    ["DSU find / union", "O(alpha(V)) amortized", "O(V) DSU storage; O(1) per call", "Size-based union and path compression flatten trees. Initialization takes O(V)."],
    ["Kruskal", "O(V + E log E)", "O(V + E)", "Copy and sort edges; DSU checks cost O(E alpha(V))."],
    ["Prim (lazy heap)", "O(V + E log(E + 1))", "O(V + E)", "Every adjacency entry is scanned; up to O(E) queued edges are inserted and removed."],
    ["Kosaraju", "O(V + E)", "O(V + E)", "Two DFS passes and a reversed graph; arrays and stack use O(V)."],
    ["Kahn", "O(V + E)", "O(V)", "Compute in-degrees, enqueue each vertex once and process each outgoing edge once."],
    ["DAG shortest paths", "O(V + E)", "O(V)", "Topological sorting plus one relaxation pass; distance and parent arrays."],
], [89, 103, 102, 180])
p("For a simple graph, E is at most V squared, so Prim is also usually written as O((V + E) log V). This implementation allows parallel edges and uses a lazy edge heap, so log E is the more precise bound. alpha(V) is the inverse Ackermann function: it grows so slowly that DSU operations are almost constant in practice, but the bound is amortized over a sequence.")
heading("Counter definitions")
p("dsu_ops counts find calls and successful unions. comparisons counts weight comparisons made by the sort or heap. edge_scans counts the main traversal: Kruskal's considered edges, Prim's adjacency entries, the two SCC DFS passes, Kahn's queue processing and the DAG relaxation pass. max_depth records the largest explicit DFS stack size and is not added to operation totals.")
p("To match the assignment's exact scan counts, reverse-graph construction, initial in-degree calculation, topological preprocessing for paths and validation are excluded from edge_scans. They are still real O(V + E) work and are included in runtime where performed inside the algorithm. Thus a scan count is a defined part of the work, not every instruction executed.")

page()
heading("2. Correctness arguments")
heading("Cut property and the two MST algorithms")
p("Consider a cut that does not split any component of the forest already selected. Let e be a lightest edge crossing that cut. If a minimum spanning tree does not contain e, adding e creates a cycle. The cycle contains another edge f crossing the same cut, and the weight of f is at least the weight of e. Replacing f with e preserves connectivity and cannot increase the total weight, so some minimum spanning tree contains e. Kruskal uses this property when it selects the next lightest edge joining two DSU components. Prim uses it when it selects the lightest edge leaving its current tree; restarting on each unvisited component gives a minimum spanning forest.")
heading("Induction on topological order")
p("In a topological order, every predecessor of a vertex appears before that vertex. Initialize every zero-in-degree vertex to distance zero, which is equivalent to adding a virtual source with zero-weight edges to all such vertices. For the induction hypothesis, assume all vertices before v already have their final shortest distances. Every path to v ends with an edge from one of these earlier vertices, so all candidates for its distance have been relaxed before v is processed. Therefore the minimum candidate stored at v is its final shortest distance, and relaxing its outgoing edges preserves the induction. Negative edges do not invalidate this reasoning because a DAG has no cycle that can send a better path back to an already processed vertex. A general graph needs another method: negative edges can break Dijkstra's greedy choice, and a reachable negative cycle can make a finite shortest distance undefined; Bellman-Ford handles negative edges when no such cycle affects the destination.")
heading("Tests")
table([
    ["Test group", "Validation performed"],
    ["DSU", "Random unions against simple set labels; every pair's connectivity and component count; counters and reinitialization."],
    ["MST", "100 random connected graphs, including negative weights, loops and parallel edges. Equal weights, n - 1 edges, no cycle, valid selected edges. Disconnected input returns three components."],
    ["SCC", "40 random graphs with up to 200 vertices compared with BFS reachability in both directions. A chain of 100,000 vertices completes with max_depth = 100,000."],
    ["Topological order", "Every edge respects positions in the order. Cyclic input and a self-loop are rejected; empty and isolated graphs work."],
    ["DAG paths", "100 random DAGs with negative weights compared with Bellman-Ford. Restored paths, multiple sources, an isolated vertex and long distance sums are checked."],
    ["Generator", "Both densities at n = 1,000 reproduce the MST, SCC and DAG reference values. The full benchmark checks every larger size too."],
], [92, 382])
p("The Maven test suite contains 12 test methods and passed with no failures. All three small input files are used by tests. The full benchmark additionally checks equal MST weights, tree sizes, valid topological order and reference results before accepting the CSV.")

page()
heading("3. Benchmark results and normalized counters")
p("Each graph starts with a fresh Random(42). The generator shuffles using Fisher-Yates, builds the required undirected spanning tree, and draws weights from 1 to 1,000. When endpoints match, only b is redrawn. Five sizes and two densities give ten cases; three graph kinds are generated per case. Five algorithm results per case produce 50 CSV rows. All reference values below matched exactly.")
reference = [["n", "m", "MST weight", "SCC count", "DAG distance sum"]]
for n in [1000, 3162, 10000, 31623, 100000]:
    for density in ["sparse", "dense"]:
        selected = {a: next(r for r in series(a, density) if r["n"] == n) for a in ALGORITHMS}
        reference.append([f"{n:,}", f"{selected['Kruskal']['m']:,}", f"{selected['Kruskal']['result']:,}",
                          f"{selected['Kosaraju']['result']:,}", f"{selected['DagShortestPath']['result']:,}"])
table(reference, [65, 85, 100, 85, 139])
p("The table below shows the minimum and maximum ratio over the five sizes. E log E uses log base 2. A bounded, roughly stable ratio supports the predicted growth; a finite benchmark is not a proof of an asymptotic bound.")
ratios = [["Algorithm / ratio", "Sparse min - max", "Dense min - max"]]
formulas = [
    ("DSU in Kruskal: dsu_ops / E", "Kruskal", lambda r: r["dsu_ops"] / r["m"]),
    ("Kruskal: comparisons / (E log E)", "Kruskal", lambda r: r["comparisons"] / (r["m"] * math.log2(r["m"]))),
    ("Prim: comparisons / (E log E)", "Prim", lambda r: r["comparisons"] / (r["m"] * math.log2(r["m"]))),
    ("Kosaraju: scans / (V + E)", "Kosaraju", lambda r: r["edge_scans"] / (r["n"] + r["m"])),
    ("Kahn: scans / (V + E)", "Kahn", lambda r: r["edge_scans"] / (r["n"] + r["m"])),
    ("DAG paths: scans / (V + E)", "DagShortestPath", lambda r: r["edge_scans"] / (r["n"] + r["m"])),
]
for label, algorithm, formula in formulas:
    values = []
    for density in ["sparse", "dense"]:
        ratios_for_case = [formula(r) for r in series(algorithm, density)]
        values.append(f"{min(ratios_for_case):.3f} - {max(ratios_for_case):.3f}")
    ratios.append([label] + values)
table(ratios, [224, 125, 125])
p("Traversal ratios are constant because E/V is fixed within each density. Sorting and heap ratios vary with input structure and repeated weights but stay within a small range. DSU call counts measure how often DSU is used; they do not count parent-pointer steps and cannot by themselves verify the alpha(V) bound. Kruskal's early stopping also changes the fraction of edges that need DSU checks.")

page()
heading("4. Operation plots and slope check")
picture("operations.png", 243)
p("Figure 1. Total counted operations against n, on log-log axes. Kahn and DAG paths overlap because both count m scans. Sparse means m = 4n; dense means m = 20n.")
picture("scans_per_edge.png", 243)
p("Figure 2. Prim and Kosaraju have scans/m = 2; Kahn and DAG relaxation have scans/m = 1. Equal curves overlap. Kruskal can stop before reaching the end of the sorted edge list.")
slopes = [["Fitted log-log slope", "Sparse", "Dense"]]
for label, algorithm, counter in [("Kruskal edge scans", "Kruskal", "edge_scans"),
                                  ("Kruskal weight comparisons", "Kruskal", "comparisons"),
                                  ("Prim / Kosaraju / Kahn / DAG scans", "Kosaraju", "edge_scans")]:
    slopes.append([label] + [f"{slope(series(algorithm, d), counter):.3f}" for d in ["sparse", "dense"]])
table(slopes, [284, 95, 95])
p("Slopes are least-squares fits of log(counter) against log(n). All four complete traversals have slope 1.000. Kruskal's log factor belongs to sorting comparisons, not edge scans; scans alone cannot demonstrate E log E. Repeated weights and early termination affect the observed slopes.")

page()
heading("5. Algorithm choice and time")
picture("kruskal_share.png", 225)
p("Figure 3. Kruskal examines a smaller share of the dense edge list, but it still sorts every edge. Prim avoids the global sort but scans all adjacency entries and pays for heap operations. Kruskal uses fewer total counted operations than this lazy Prim on all ten inputs. These experiments do not establish an input range where Prim wins.")
table([
    ["Situation", "Choice and reason"],
    ["Sparse undirected graph in an edge list", "Kruskal: sorting the existing edge list and checking DSU is straightforward."],
    ["Undirected graph already in adjacency lists", "Prim: grow directly from adjacency lists. For this lazy heap version, compare measured work with Kruskal before claiming an advantage."],
    ["Very dense undirected graph", "A matrix-based O(V squared) Prim is a common choice, but it is not implemented here. Both required implementations use sorting or a heap."],
    ["Directed graph where cycles are possible", "Kosaraju for mutually reachable groups; Kahn if the task is to obtain a schedule or detect a cycle."],
    ["DAG with weighted dependencies", "Topological relaxation: O(V + E), with negative weights allowed and parent-based path restoration."],
    ["General graph shortest paths", "Use Dijkstra for nonnegative edges or Bellman-Ford when negative edges are possible. DAG relaxation requires acyclicity."],
], [169, 305])
small = [r["time_ms"] for r in rows if r["n"] == 1000]
largest = {a: series(a, "dense")[-1]["time_ms"] for a in ALGORITHMS}
timing = "; ".join(f"{LABELS.get(a, a)} {largest[a]:.1f} ms" for a in ALGORITHMS)
p(f"Each CSV time is the median of five runs using System.nanoTime on Java 21.0.7. At n = 1,000 the medians ranged from {min(small):.3f} to {max(small):.3f} ms. On the largest dense inputs: {timing}. Graph generation and CSV writing are outside the timer; algorithm allocations and preprocessing are inside it. There is no separate JVM warm-up stage, so JIT compilation, garbage collection, hardware and background load can affect these numbers, especially on the smallest cases. This is why the conclusions use counters rather than a single timing comparison.")
p("Reproduction: run mvn clean package, then java -Xmx3g -jar target/graphs-1.0.jar, then python scripts/report.py with the Python dependencies installed. The seed reproduces graph results and counters for the same implementation; wall-clock times are expected to change. Source of task requirements and reference values: DAA Assignment 3 Graphs, Taubakabyl Nurlybek. AI assistance is acknowledged in README.md.")

def footer(canvas, doc):
    canvas.setFont("Helvetica", 8)
    canvas.drawString(48, 29, "Yerkebulan Niyalov | SE2526")
    canvas.drawRightString(547, 29, str(doc.page))

doc = SimpleDocTemplate(str(ROOT / "REPORT.pdf"), pagesize=(595.28, 841.89),
                        leftMargin=54, rightMargin=54, topMargin=36, bottomMargin=43,
                        title="Assignment 3 Graphs", author="Yerkebulan Niyalov")
doc.build(story, onFirstPage=footer, onLaterPages=footer)
print("Created three PNG plots and REPORT.pdf")
