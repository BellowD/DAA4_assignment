# Assignment 4 — Analysis

## Environment and commands

- **Selected Language & Runtime:** Java (OpenJDK 26, target release `--release 17` UTF-8).
- **Environment & Hardware:** Windows 11 (build 10.0.26300), 64-bit OS, 20 logical processors, 32 GB RAM.
- **Commands:**
  - Setup validation: `python tools/run.py --language Java --setup`
  - Public checks (44 cases + state reset): `python tools/run.py --language Java --check`
  - Custom student tests: `python tools/run.py --language Java --input tests/student/custom_cases.jsonl`
  - Empirical search benchmark: `python tools/benchmark_search.py`

The process maintains the UTF-8 JSON Lines interface where each input line triggers a fresh query. All algorithms allocate independent local structures per query, ensuring complete isolation and state reset between successive requests.

---

## Algorithm explanations

### 1. Longest Common Subsequence (`lcs`)

- **State and Invariant:** $dp[i][j]$ represents the length of the longest common subsequence of prefixes $a[0 \dots i-1]$ and $b[0 \dots j-1]$. The invariant guarantees that for every computed $(i, j)$, $dp[i][j]$ is the maximum length of any common subsequence of these prefixes.
- **Transitions and Base Cases:**
  - Base cases: $dp[i][0] = 0$ for all $0 \le i \le |a|$, and $dp[0][j] = 0$ for all $0 \le j \le |b|$.
  - Transition:
    - If $a[i-1] == b[j-1]$: $dp[i][j] = dp[i-1][j-1] + 1$.
    - If $a[i-1] \neq b[j-1]$: $dp[i][j] = \max(dp[i-1][j], dp[i][j-1])$.
- **Processing Order:** Row-by-row (increasing $i$ from 1 to $|a|$, and $j$ from 1 to $|b|$). Dependencies $(i-1, j-1)$, $(i-1, j)$, and $(i, j-1)$ are already evaluated.
- **Correctness:** If characters match, appending them to the optimal LCS of the shorter prefixes strictly maximizes length. If they differ, the optimal common subsequence cannot include both characters simultaneously; thus, it must be the LCS of either $a[0 \dots i-2]$ and $b[0 \dots j-1]$ or $a[0 \dots i-1]$ and $b[0 \dots j-2]$.
- **Reconstruction:** Backtrack from $(|a|, |b|)$. If $a[i-1] == b[j-1]$, collect $a[i-1]$ and step to $(i-1, j-1)$. Otherwise, step to whichever neighbor achieves $\max(dp[i-1][j], dp[i][j-1])$. Reverse the collected sequence. Any optimal sequence among tied alternatives is valid.
- **Complexity:** Time $O((|a|+1)(|b|+1))$, space $O((|a|+1)(|b|+1))$ for the DP table. Empty inputs take $O(1)$ returning `length = 0` and `sequence = ""`.
- **Boundary Test:** Disjoint strings `"ABCDE"` and `"FGHIJ"` produce `length = 0` and `sequence = ""`. Detects false positive character alignment.

---

### 2. Ordered Adjacent-Block Merging (`merge`)

- **State and Invariant:** $dp[l][r]$ denotes the minimum total cost to merge the contiguous subsegment of initial blocks from index $l$ to $r$ ($0 \le l \le r < n$). The invariant is that $dp[l][r]$ equals the minimal cumulative cost of reducing blocks $l \dots r$ to a single block.
- **Transitions and Base Cases:**
  - Base cases: $dp[i][i] = 0$ for all $0 \le i < n$ (single blocks need 0 merges).
  - Transition for interval length $len = r - l + 1 \ge 2$:
    $$dp[l][r] = \min_{l \le k < r} \left( dp[l][k] + dp[k+1][r] + \sum_{t=l}^{r} sizes[t] \right)$$
    The partition point $k$ splitting $[l, r]$ into $[l, k]$ and $[k+1, r]$ is stored in $split[l][r]$.
  - The range sum $\sum_{t=l}^{r} sizes[t]$ is evaluated in $O(1)$ using prefix sums.
- **Processing Order:** Increasing interval length $len$ from 2 to $n$, then left endpoint $l$ from 0 to $n - len$. Smaller subintervals are solved before larger ones.
- **Correctness:** Any valid merge plan must finish with a final merge of two adjacent compound blocks $[l, k]$ and $[k+1, r]$. By optimal substructure, both $[l, k]$ and $[k+1, r]$ must themselves have been merged optimally.
- **Reconstruction:** Recursive postorder traversal: recursively emit the merge plan for $[l, k]$, then for $[k+1, r]$, and finally append the parent merge $[l, k, r]$. This ensures adjacent blocks exist in the active block list before they are combined.
- **Complexity:** Time $O(n^3)$ for interval DP, auxiliary space $O(n^2)$ for tables. For $n \le 1$, cost is 0 and plan is empty in $O(1)$.
- **Boundary Test:** Single block `[100]` produces `cost = 0` and `plan = []`. Detects out-of-bounds indexing or superfluous merge operations.

---

### 3. Tree Maximum-Weight Independent Set (`tree`)

- **State and Invariant:** For each rooted subtree at $u$:
  - $dpTake[u]$: maximum weight of an independent set in subtree $u$ that **includes** $u$.
  - $dpSkip[u]$: maximum weight of an independent set in subtree $u$ that **excludes** $u$.
- **Transitions and Base Cases:**
  - For a leaf $u$: $dpTake[u] = weights[u]$, $dpSkip[u] = 0$.
  - For an internal node $u$ with children $v$:
    $$dpTake[u] = weights[u] + \sum_{v \in children(u)} dpSkip[v]$$
    $$dpSkip[u] = \sum_{v \in children(u)} \max(dpTake[v], dpSkip[v])$$
  - Global optimum: $\max(0, \max(dpTake[root], dpSkip[root]))$. Since the empty set is valid, all-negative weights return 0.
- **Processing Order:** An explicit iterative BFS assigns parent pointers and defines a topological BFS order. The DP is computed strictly in reverse BFS order (bottom-up from leaves to root), guaranteeing all child values are computed before the parent.
- **Correctness:** If $u$ is taken, none of its children may be taken, requiring the skip state for all children. If $u$ is skipped, each child can independently be taken or skipped to maximize total weight. Iterative BFS eliminates recursion, preventing stack overflow on 100,000-vertex chains.
- **Reconstruction:** Top-down via forward BFS order:
  - If root is taken: mark `taken[root] = true`.
  - For each node $u$ in BFS order: if `taken[u]` is true, all children $v$ have `taken[v] = false`. If `taken[u]` is false, child $v$ is taken if and only if $dpTake[v] > dpSkip[v]$.
- **Complexity:** Time $O(n + m) = O(n)$ and space $O(n)$ using primitive arrays (`head`, `to`, `next`). Empty tree returns `weight = 0` and `selected = []` in $O(1)$.
- **Boundary Test:** 100,000-vertex deep chain. Solved in 60 ms without `StackOverflowError`. Detects recursive stack limitations.

---

### 4. Exact Search — Naive Baseline (`naive`)

- **Invariant:** At each start index $i \in [0, n - m]$, character comparisons check $text[i+j] == pattern[j]$ for $j \in [0, m-1]$. The window matches if and only if all $m$ characters match.
- **Transitions & Base Cases:**
  - If $m == 0$: empty pattern matches every boundary $0 \dots n$ inclusive, returning $n + 1$ indices.
  - If $m > n$: pattern cannot fit, returns `[]`.
  - Otherwise, loop $i$ from 0 to $n - m$.
- **Processing Order:** Sequential linear iteration of starts $0 \dots n - m$.
- **Correctness:** Tests every valid window start without skipping any candidate, verifying characters directly.
- **Complexity:** Time $O(n \cdot m + n + 1)$, auxiliary space $O(1)$, output space $O(z + 1)$ for $z$ matches.
- **Boundary Test:** Text `"aaaa"`, pattern `"aa"`. Returns `[0, 1, 2]`. Detects failure to report overlapping matches.

---

### 5. Exact Search — Knuth-Morris-Pratt (`kmp`)

- **State and Invariant:**
  - Prefix function $\pi[i]$ is the length of the longest proper prefix of $pattern[0 \dots i]$ that is also a suffix of $pattern[0 \dots i]$.
  - During text search, $j$ is the length of the longest prefix of $pattern$ that matches the suffix of $text$ ending at character $i$.
- **Transitions and Base Cases:**
  - $\pi[0] = 0$. For $i = 1 \dots m - 1$, while $j > 0$ and $pattern[i] \neq pattern[j]$, fallback $j = \pi[j-1]$. If equal, $j++$, and $\pi[i] = j$.
  - Searching text: for each character $text[i]$, fallback $j = \pi[j-1]$ until match or $j = 0$. If $text[i] == pattern[j]$, $j++$.
  - Upon full match ($j == m$): record occurrence at start $i - m + 1$, and set $j = \pi[m-1]$ to continue searching for overlapping occurrences.
- **Processing Order:** Linear scan of pattern to build $\pi$, followed by linear scan of text.
- **Correctness:** The prefix function guarantees that when a mismatch occurs at $pattern[j]$, the longest valid prefix that could continue a match without re-reading text characters is of length $\pi[j-1]$.
- **Complexity:** Time $O(n + m + z + 1)$, auxiliary memory $O(m + 1)$, output space $O(z + 1)$.
- **Boundary Test:** Text `"abababa"`, pattern `"aba"`. Returns `[0, 2, 4]`. Detects missed overlaps if $j$ is unconditionally reset to 0 after a match.

---

### 6. Exact Search — Rabin-Karp (`rk`)

- **State and Invariant:**
  - Rolling hash configuration: base $B = 31$, modulus $M = 101$.
  - Hash value $H(s) = \left(\sum_{k=0}^{m-1} s[k] \cdot B^{m-1-k}\right) \pmod M$.
  - Invariant: at start $i$, $windowHash$ accurately represents the polynomial hash of $text[i \dots i+m-1]$.
- **Transitions and Base Cases:**
  - If $m == 0$: returns $[0, 1, \dots, n]$. If $m > n$: returns `[]`.
  - Window slide:
    $$windowHash_{i+1} = \left( (windowHash_i - text[i] \cdot B^{m-1}) \cdot B + text[i+m] \right) \pmod M$$
    Negative remainders are normalized via `(val % M + M) % M`.
  - Collision verification: whenever $windowHash == patternHash$, actual characters are compared. Only confirmed matches are added to the result.
- **Complexity:**
  - Best / average case: $O(n + m)$ when hash collisions are rare.
  - Worst case: $O(n \cdot m)$ when many non-matching windows produce the same hash mod 101 (candidate-sensitive bound $O(n + m + c \cdot m)$ where $c$ is the number of hash collisions).
  - Auxiliary memory: $O(1)$ for rolling windows.
- **Boundary Test:** Text `"bcek"`, pattern `"bc"`. Both `"bc"` and `"ek"` hash to 6 mod 101. Character verification confirms `"bc"` at index 0 and rejects `"ek"` at index 2, returning `[0]`. Detects false positive emission.

---

### 7. Suffix Array and Kasai LCP (`suffix`)

- **Algorithm Structure:**
  1. **Prefix Doubling for Suffix Array ($sa$):**
     - Suffixes are iteratively sorted by their prefixes of length $2^k$ ($k = 0, 1, 2, \dots$).
     - At each stage, suffix $i$ has a rank pair $(rank[i], rank[i + 2^k])$.
     - A 2-pass counting sort (radix sort) orders suffixes stably by secondary rank, then by primary rank.
     - New ranks are assigned based on whether adjacent pairs in sorted order differ.
     - Terminates early when all ranks are distinct ($r == n - 1$).
     - Time: $O(n \log n)$ using counting sort with bucket size $\max(256, n + 1)$. Auxiliary space: $O(n)$.
  2. **Kasai Algorithm for LCP ($lcp$):**
     - Computes the Longest Common Prefix between lexicographically adjacent suffixes $sa[i-1]$ and $sa[i]$.
     - Invariant: let $h$ be the LCP between suffix $i$ and its lexicographical predecessor. When advancing to suffix $i + 1$, its LCP with its predecessor is at least $h - 1$.
     - Because $h$ decrements by at most 1 per step and never exceeds $n$, the while-loop character increments occur at most $2n$ times.
     - Time: strictly $O(n)$.
  3. **Longest Repeated Substring (`repeated`):**
     - Any substring appearing at least twice in $text$ is a common prefix of two distinct suffixes.
     - The maximum value in $lcp$ ($maxLcp = \max_{1 \le i < n} lcp[i]$) gives the exact length of the longest repeated substring.
     - If $maxLcp == 0$, `repeated = ""`. Otherwise, `repeated = text.substring(sa[idx], sa[idx] + maxLcp)`.
- **Complexity:** Total time $O(n \log n)$ for suffix array + $O(n)$ for Kasai = $O(n \log n)$. Memory $O(n)$ auxiliary and output. Empty text returns `sa = []`, `lcp = []`, `repeated = ""` in $O(1)$.
- **Boundary Test:** Text `"aaaa"`. Yields $sa = [3, 2, 1, 0]$, $lcp = [0, 1, 2, 3]$, and `repeated = "aaa"`. Detects incorrect rank assignment when all characters are identical.

---

### 8. Dictionary Matching — Aho–Corasick (`aho`)

- **State and Invariant:**
  - Trie nodes $0 \dots S-1$ (root 0). Each node represents a distinct prefix of dictionary patterns.
  - Failure link $fail[u]$ points to the state representing the longest proper suffix of the string for node $u$ that exists in the trie.
  - Automaton transitions $next[u][c]$ allow $O(1)$ state advance for all 95 printable ASCII characters (codes 32..126).
- **Construction & BFS:**
  - Insert all patterns into the trie. Terminal nodes record the original dictionary indices of matching patterns. Duplicate patterns attach multiple indices to the same terminal node.
  - BFS computes failure links and completes automaton transitions:
    - For root children: $fail[v] = 0$.
    - For node $u$ with character $c$: if $next[u][c] \neq 0$, $fail[next[u][c]] = next[fail[u]][c]$; otherwise $next[u][c] = next[fail[u]][c]$.
- **Text Processing & Reverse-BFS Propagation:**
  - For each text, maintain `visitCount[u]` initialized to 0.
  - Traverse the text character by character: $curr = next[curr][c]$, and increment `visitCount[curr]++`.
  - **Reverse-BFS Propagation:** Iterate nodes in reverse BFS order (from leaves to root) and add `visitCount[u]` to `visitCount[fail[u]]`. This guarantees every suffix match is propagated to shorter patterns in $O(S)$ time without traversing output links per character.
  - Assign counts to each pattern index attached to terminal states.
- **Complexity:**
  - Trie & BFS: $O(L + 95 \cdot S)$ where $L$ is total pattern length and $S$ is state count.
  - Per text: $O(|text| + S + p)$ where $p$ is dictionary size.
  - Total auxiliary memory: $O(95 \cdot S + p)$.
- **Boundary Test:** Patterns `["he", "she", "he"]`, texts `["sheshe", "sheshe", ""]`. Yields counts `[[2, 2, 2], [2, 2, 2], [0, 0, 0]]`. Confirms duplicate dictionary handling, suffix-pattern detection (`he` in `she`), and complete state reset between independent texts.

---

## Own tests

| Command | Input or fixture path | Expected result | Actual result / log | What it checks |
|---|---|---|---|---|
| `lcs` | `{"a":"ABCDE","b":"ABCDE"}` | `length: 5`, `sequence: "ABCDE"` | `{"length":5,"sequence":"ABCDE"}` | Full identical match; verifies boundary indices. |
| `lcs` | `{"a":"ABCDE","b":"FGHIJ"}` | `length: 0`, `sequence: ""` | `{"length":0,"sequence":""}` | Completely disjoint sequences; verifies zero-length handling. |
| `merge` | `{"sizes":[10,20,30,40,50]}` | `cost: 330`, 4 merges | `{"cost":330,"plan":[[0,0,1],[0,1,2],[3,3,4],[0,2,4]]}` | Multi-step partition; verifies postorder chronological output. |
| `merge` | `{"sizes":[100,200]}` | `cost: 300`, `[[0,0,1]]` | `{"cost":300,"plan":[[0,0,1]]}` | Single merge base case. |
| `tree` | Star $N=5$, weights `[100,10,10,10,10]` | `weight: 100`, `selected: [0]` | `{"weight":100,"selected":[0]}` | Center vs sum of leaves ($100 > 40$); verifies optimal selection. |
| `tree` | Line $N=5$, weights `[10,1,10,1,10]` | `weight: 30`, `selected: [0,2,4]` | `{"weight":30,"selected":[0,2,4]}` | Alternating optimal choice; verifies no neighbor conflicts. |
| `naive` | `{"text":"hello","pattern":"hello"}` | `positions: [0]` | `{"positions":[0]}` | Full text-pattern equality. |
| `naive` | `{"text":"abcdef","pattern":"abcdefgh"}` | `positions: []` | `{"positions":[]}` | Pattern longer than text ($m > n$). |
| `kmp` | `{"text":"aaaaa","pattern":"aa"}` | `positions: [0,1,2,3]` | `{"positions":[0,1,2,3]}` | Periodic overlapping matches. |
| `kmp` | `{"text":"the quick brown fox","pattern":"fox"}` | `positions: [16]` | `{"positions":[16]}` | Occurrence at final string boundary. |
| `rk` | `{"text":"abczdj","pattern":"ab"}` | `positions: [0]` | `{"positions":[0]}` | Collision test: `"ab"` and `"dj"` both hash to 75 mod 101. Verifies character check rejects `"dj"`. |
| `rk` | `{"text":"bczzek","pattern":"bc"}` | `positions: [0]` | `{"positions":[0]}` | Collision test: `"bc"` and `"ek"` both hash to 6 mod 101. Verifies rejection. |
| `suffix` | `{"text":"aaaa"}` | `sa:[3,2,1,0]`, `lcp:[0,1,2,3]`, `rep:"aaa"` | `{"sa":[3,2,1,0],"lcp":[0,1,2,3],"repeated":"aaa"}` | Identical characters; verifies stable rank sorting. |
| `suffix` | `{"text":"abcdef"}` | `sa:[0..5]`, `lcp:[0..0]`, `rep:""` | `{"sa":[0,1,2,3,4,5],"lcp":[0,0,0,0,0,0],"repeated":""}` | All unique characters; verifies empty repeat string. |
| `aho` | Patterns `["a","aa","aaa"]`, text `"aaaa"` | `counts: [[4,3,2],[1,0,0]]` | `{"counts":[[4,3,2],[1,0,0]]}` | Suffix pattern propagation on dense overlapping text. |
| `aho` | Patterns `["test","test"]`, text `"testtest"` | `counts: [[2,2]]` | `{"counts":[[2,2]]}` | Duplicate dictionary patterns; verifies identical counts per column. |

All test cases are stored in `tests/student/custom_cases.jsonl` and verified via `tests/student/actual_results.jsonl`.

---

## Search comparison

### Experimental Setup
- **Benchmark Script:** `tools/benchmark_search.py`.
- **Environment:** Windows 11 (10.0.26300), OpenJDK 26 (`--release 17`), Intel 20-thread CPU.
- **Timing Scope:** Algorithm execution inside a persistent JVM process over stdin/stdout. Compilation time and process startup overhead are completely excluded.
- **Warm-up Policy:** 50 preliminary queries executed before measurement to trigger JIT compilation and steady-state optimization.
- **Correctness Verification:** For every size and case, the exact returned `positions` lists of all three algorithms were compared and asserted equal before timing.
- **Seed & Generator:** Python `random.seed(20261108)`. Random case uses lowercase English alphabet; Repetitive case uses worst-case string $text = a^{n-1}b$ and $pattern = a^{m-1}b$.

### Measurement Results

| Case | $n$ | $m$ | Seed/generator | Algorithm | Run 1 (ms) | Run 2 (ms) | Run 3 (ms) | Median (ms) | Correct outputs checked |
|---|---:|---:|---|---|---:|---:|---:|---:|---|
| Random | 500 | 50 | 20261108 | naive | 0.449 | 0.339 | 0.247 | **0.339** | MATCH (pos=0) |
| Random | 500 | 50 | 20261108 | kmp | 0.331 | 0.252 | 0.393 | **0.331** | MATCH (pos=0) |
| Random | 500 | 50 | 20261108 | rk | 0.451 | 0.406 | 0.497 | **0.451** | MATCH (pos=0) |
| Random | 1500 | 150 | 20261108 | naive | 0.489 | 0.398 | 0.463 | **0.463** | MATCH (pos=0) |
| Random | 1500 | 150 | 20261108 | kmp | 0.422 | 0.450 | 0.825 | **0.450** | MATCH (pos=0) |
| Random | 1500 | 150 | 20261108 | rk | 0.495 | 0.628 | 0.660 | **0.628** | MATCH (pos=0) |
| Random | 3000 | 300 | 20261108 | naive | 0.708 | 0.672 | 0.696 | **0.696** | MATCH (pos=0) |
| Random | 3000 | 300 | 20261108 | kmp | 0.528 | 0.599 | 0.592 | **0.592** | MATCH (pos=0) |
| Random | 3000 | 300 | 20261108 | rk | 1.144 | 1.115 | 0.894 | **1.115** | MATCH (pos=0) |
| Repetitive | 500 | 50 | 20261108 | naive | 1.283 | 0.811 | 0.698 | **0.811** | MATCH (pos=1) |
| Repetitive | 500 | 50 | 20261108 | kmp | 0.147 | 0.133 | 0.185 | **0.147** | MATCH (pos=1) |
| Repetitive | 500 | 50 | 20261108 | rk | 0.177 | 0.214 | 0.279 | **0.214** | MATCH (pos=1) |
| Repetitive | 1500 | 150 | 20261108 | naive | 2.164 | 1.158 | 1.296 | **1.296** | MATCH (pos=1) |
| Repetitive | 1500 | 150 | 20261108 | kmp | 0.371 | 0.480 | 0.264 | **0.371** | MATCH (pos=1) |
| Repetitive | 1500 | 150 | 20261108 | rk | 0.389 | 0.546 | 0.356 | **0.389** | MATCH (pos=1) |
| Repetitive | 3000 | 300 | 20261108 | naive | 2.257 | 1.761 | 1.650 | **1.761** | MATCH (pos=1) |
| Repetitive | 3000 | 300 | 20261108 | kmp | 0.431 | 0.327 | 0.614 | **0.431** | MATCH (pos=1) |
| Repetitive | 3000 | 300 | 20261108 | rk | 0.890 | 0.573 | 0.711 | **0.711** | MATCH (pos=1) |

### Observations and Theoretical Discussion
1. **Random Text Behavior:** On random alphabet inputs, mismatches happen early (average comparison length $\approx 1 / (1 - 1/26) \approx 1.04$). Consequently, the naive search performs almost linearly and exhibits minimal overhead. KMP achieves similar linear time ($0.592$ ms at $n=3000$). Rabin-Karp is slightly slower ($1.115$ ms) due to rolling arithmetic updates modulo 101 per character.
2. **Repetitive Text Behavior (Worst-Case Naive):** On repetitive inputs ($a^{n-1}b$ vs $a^{m-1}b$), the naive search compares $m-1$ characters at almost every position before failing on character $b$, resulting in true $\Theta(n \cdot m)$ operations. This is directly reflected in the runtime: at $n=3000, m=300$, naive takes $1.761$ ms, whereas KMP executes in only $0.431$ ms (over $4\times$ faster).
3. **Rabin-Karp and Modulus 101:** The small modulus $M = 101$ causes hash collisions with probability $\approx 1/101$ on arbitrary windows. Each collision triggers an explicit character-by-character verification of length $m$. In our synthetic repetitive test, characters $a$ produce identical hashes, but once the rolling hash rejects non-candidates, verification occurs only when hashes match. In production, a large prime modulus (e.g. $10^9 + 7$ or $2^{61}-1$) drastically reduces collision probability to near zero.
4. **Conclusion:** Empirical runtimes illustrate constant-factor differences and algorithmic behaviors on specific distributions, but do not replace asymptotic proofs ($O(n+m)$ for KMP vs $O(n \cdot m)$ for naive).

---

## Sources and AI assistance

All algorithm implementations, custom test fixtures, and performance benchmarks were completed and verified independently based on course lecture materials (Weeks 8–10) and *Introduction to Algorithms* (CLRS, 4th edition). No external code or AI tools were used.
