import java.util.*;

public final class Algorithms {

    static Map<String, Object> lcs(Map<String, Object> input) {
        String a = (String) input.get("a");
        String b = (String) input.get("b");
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("length", 0L);
            res.put("sequence", "");
            return res;
        }

        int n = a.length();
        int m = b.length();
        int[][] dp = new int[n + 1][m + 1];

        for (int i = 1; i <= n; i++) {
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                if (ca == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        int i = n, j = m;
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) {
                sb.append(a.charAt(i - 1));
                i--;
                j--;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("length", (long) dp[n][m]);
        res.put("sequence", sb.reverse().toString());
        return res;
    }

    static Map<String, Object> merge(Map<String, Object> input) {
        List<?> rawSizes = (List<?>) input.get("sizes");
        int n = rawSizes != null ? rawSizes.size() : 0;
        if (n <= 1) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("cost", 0L);
            res.put("plan", Collections.emptyList());
            return res;
        }

        long[] sizes = new long[n];
        for (int i = 0; i < n; i++) {
            sizes[i] = ((Number) rawSizes.get(i)).longValue();
        }

        long[] pref = new long[n + 1];
        for (int i = 0; i < n; i++) {
            pref[i + 1] = pref[i] + sizes[i];
        }

        long[][] dp = new long[n][n];
        int[][] split = new int[n][n];

        for (int len = 2; len <= n; len++) {
            for (int l = 0; l <= n - len; l++) {
                int r = l + len - 1;
                long minCost = Long.MAX_VALUE;
                int bestK = -1;
                long sum = pref[r + 1] - pref[l];
                for (int k = l; k < r; k++) {
                    long c = dp[l][k] + dp[k + 1][r] + sum;
                    if (c < minCost) {
                        minCost = c;
                        bestK = k;
                    }
                }
                dp[l][r] = minCost;
                split[l][r] = bestK;
            }
        }

        List<Object> plan = new ArrayList<>();
        buildMergePlan(0, n - 1, split, plan);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("cost", dp[0][n - 1]);
        res.put("plan", plan);
        return res;
    }

    private static void buildMergePlan(int l, int r, int[][] split, List<Object> plan) {
        if (l >= r) return;
        int k = split[l][r];
        buildMergePlan(l, k, split, plan);
        buildMergePlan(k + 1, r, split, plan);
        plan.add(List.of((long) l, (long) k, (long) r));
    }

    static Map<String, Object> tree(Map<String, Object> input) {
        int n = ((Number) input.get("n")).intValue();
        if (n <= 0) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("weight", 0L);
            res.put("selected", Collections.emptyList());
            return res;
        }

        List<?> rawWeights = (List<?>) input.get("weights");
        long[] weights = new long[n];
        for (int i = 0; i < n; i++) {
            weights[i] = ((Number) rawWeights.get(i)).longValue();
        }

        List<?> rawEdges = (List<?>) input.get("edges");
        int m = rawEdges != null ? rawEdges.size() : 0;
        int[] head = new int[n];
        Arrays.fill(head, -1);
        int edgeCount = m * 2;
        int[] to = new int[edgeCount];
        int[] next = new int[edgeCount];
        int eIdx = 0;

        if (rawEdges != null) {
            for (Object edgeObj : rawEdges) {
                List<?> edge = (List<?>) edgeObj;
                int u = ((Number) edge.get(0)).intValue();
                int v = ((Number) edge.get(1)).intValue();
                to[eIdx] = v; next[eIdx] = head[u]; head[u] = eIdx++;
                to[eIdx] = u; next[eIdx] = head[v]; head[v] = eIdx++;
            }
        }

        int[] parent = new int[n];
        Arrays.fill(parent, -1);
        int[] bfsOrder = new int[n];
        boolean[] visited = new boolean[n];
        int headQueue = 0, tailQueue = 0;
        visited[0] = true;
        bfsOrder[tailQueue++] = 0;

        while (headQueue < tailQueue) {
            int u = bfsOrder[headQueue++];
            for (int e = head[u]; e != -1; e = next[e]) {
                int v = to[e];
                if (!visited[v]) {
                    visited[v] = true;
                    parent[v] = u;
                    bfsOrder[tailQueue++] = v;
                }
            }
        }

        long[] dpTake = new long[n];
        long[] dpSkip = new long[n];

        for (int i = n - 1; i >= 0; i--) {
            int u = bfsOrder[i];
            dpTake[u] = weights[u];
            dpSkip[u] = 0L;
            for (int e = head[u]; e != -1; e = next[e]) {
                int v = to[e];
                if (parent[v] == u) {
                    dpTake[u] += dpSkip[v];
                    dpSkip[u] += Math.max(dpTake[v], dpSkip[v]);
                }
            }
        }

        long maxWeight = Math.max(0L, Math.max(dpTake[0], dpSkip[0]));
        List<Long> selected = new ArrayList<>();

        if (maxWeight > 0) {
            boolean[] isTaken = new boolean[n];
            if (dpTake[0] > dpSkip[0]) {
                isTaken[0] = true;
            }
            for (int i = 0; i < n; i++) {
                int u = bfsOrder[i];
                if (isTaken[u]) {
                    selected.add((long) u);
                    for (int e = head[u]; e != -1; e = next[e]) {
                        int v = to[e];
                        if (parent[v] == u) {
                            isTaken[v] = false;
                        }
                    }
                } else {
                    for (int e = head[u]; e != -1; e = next[e]) {
                        int v = to[e];
                        if (parent[v] == u) {
                            if (dpTake[v] > dpSkip[v]) {
                                isTaken[v] = true;
                            }
                        }
                    }
                }
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("weight", maxWeight);
        res.put("selected", selected);
        return res;
    }

    static Map<String, Object> naive(Map<String, Object> input) {
        String text = (String) input.get("text");
        String pattern = (String) input.get("pattern");
        if (text == null) text = "";
        if (pattern == null) pattern = "";
        int n = text.length();
        int m = pattern.length();
        List<Long> positions = new ArrayList<>();

        if (m == 0) {
            for (int i = 0; i <= n; i++) {
                positions.add((long) i);
            }
        } else if (m <= n) {
            for (int i = 0; i <= n - m; i++) {
                boolean match = true;
                for (int j = 0; j < m; j++) {
                    if (text.charAt(i + j) != pattern.charAt(j)) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    positions.add((long) i);
                }
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("positions", positions);
        return res;
    }

    static Map<String, Object> kmp(Map<String, Object> input) {
        String text = (String) input.get("text");
        String pattern = (String) input.get("pattern");
        if (text == null) text = "";
        if (pattern == null) pattern = "";
        int n = text.length();
        int m = pattern.length();
        List<Long> positions = new ArrayList<>();

        if (m == 0) {
            for (int i = 0; i <= n; i++) {
                positions.add((long) i);
            }
        } else if (m <= n) {
            int[] pi = new int[m];
            int j = 0;
            for (int i = 1; i < m; i++) {
                while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) {
                    j = pi[j - 1];
                }
                if (pattern.charAt(i) == pattern.charAt(j)) {
                    j++;
                }
                pi[i] = j;
            }

            j = 0;
            for (int i = 0; i < n; i++) {
                while (j > 0 && text.charAt(i) != pattern.charAt(j)) {
                    j = pi[j - 1];
                }
                if (text.charAt(i) == pattern.charAt(j)) {
                    j++;
                }
                if (j == m) {
                    positions.add((long) (i - m + 1));
                    j = pi[j - 1];
                }
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("positions", positions);
        return res;
    }

    static Map<String, Object> rk(Map<String, Object> input) {
        String text = (String) input.get("text");
        String pattern = (String) input.get("pattern");
        if (text == null) text = "";
        if (pattern == null) pattern = "";
        int n = text.length();
        int m = pattern.length();
        List<Long> positions = new ArrayList<>();

        if (m == 0) {
            for (int i = 0; i <= n; i++) {
                positions.add((long) i);
            }
        } else if (m <= n) {
            final long BASE = 31;
            final long MOD = 101;
            long patternHash = 0;
            long windowHash = 0;
            long power = 1;

            for (int i = 0; i < m; i++) {
                patternHash = (patternHash * BASE + (int) pattern.charAt(i)) % MOD;
                windowHash = (windowHash * BASE + (int) text.charAt(i)) % MOD;
                if (i > 0) {
                    power = (power * BASE) % MOD;
                }
            }

            for (int i = 0; i <= n - m; i++) {
                if (windowHash == patternHash) {
                    boolean match = true;
                    for (int j = 0; j < m; j++) {
                        if (text.charAt(i + j) != pattern.charAt(j)) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        positions.add((long) i);
                    }
                }
                if (i < n - m) {
                    long leftVal = ((int) text.charAt(i) * power) % MOD;
                    windowHash = (windowHash - leftVal) % MOD;
                    if (windowHash < 0) windowHash += MOD;
                    windowHash = (windowHash * BASE + (int) text.charAt(i + m)) % MOD;
                }
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("positions", positions);
        return res;
    }

    static Map<String, Object> suffix(Map<String, Object> input) {
        String text = (String) input.get("text");
        if (text == null) text = "";
        int n = text.length();

        if (n == 0) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("sa", Collections.emptyList());
            res.put("lcp", Collections.emptyList());
            res.put("repeated", "");
            return res;
        }

        if (n == 1) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("sa", List.of(0L));
            res.put("lcp", List.of(0L));
            res.put("repeated", "");
            return res;
        }

        int[] sa = new int[n];
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) {
            sa[i] = i;
            rank[i] = (int) text.charAt(i);
        }

        for (int k = 1; k < n; k <<= 1) {
            int maxRank = 0;
            for (int r : rank) if (r > maxRank) maxRank = r;
            int maxKey = Math.max(256, maxRank + 1);

            int[] secondKey = new int[n];
            for (int i = 0; i < n; i++) {
                secondKey[i] = (i + k < n ? rank[i + k] + 1 : 0);
            }
            countingSort(sa, secondKey, maxKey, n);

            int[] firstKey = new int[n];
            for (int i = 0; i < n; i++) {
                firstKey[i] = rank[i] + 1;
            }
            countingSort(sa, firstKey, maxKey, n);

            int[] newRank = new int[n];
            newRank[sa[0]] = 0;
            int r = 0;
            for (int i = 1; i < n; i++) {
                int prev = sa[i - 1], curr = sa[i];
                int prevK = (prev + k < n ? rank[prev + k] : -1);
                int currK = (curr + k < n ? rank[curr + k] : -1);
                if (rank[prev] != rank[curr] || prevK != currK) {
                    r++;
                }
                newRank[curr] = r;
            }
            rank = newRank;
            if (r == n - 1) break;
        }

        int[] rankArr = new int[n];
        for (int i = 0; i < n; i++) rankArr[sa[i]] = i;
        int[] lcp = new int[n];
        int h = 0;
        int maxLcp = 0;
        int maxLcpIdx = -1;

        for (int i = 0; i < n; i++) {
            if (rankArr[i] > 0) {
                int prev = sa[rankArr[i] - 1];
                while (i + h < n && prev + h < n && text.charAt(i + h) == text.charAt(prev + h)) {
                    h++;
                }
                lcp[rankArr[i]] = h;
                if (h > maxLcp) {
                    maxLcp = h;
                    maxLcpIdx = rankArr[i];
                }
                if (h > 0) h--;
            } else {
                lcp[0] = 0;
            }
        }

        String repeated = (maxLcp > 0 && maxLcpIdx >= 0) ? text.substring(sa[maxLcpIdx], sa[maxLcpIdx] + maxLcp) : "";

        List<Long> saList = new ArrayList<>(n);
        List<Long> lcpList = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            saList.add((long) sa[i]);
            lcpList.add((long) lcp[i]);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("sa", saList);
        res.put("lcp", lcpList);
        res.put("repeated", repeated);
        return res;
    }

    private static void countingSort(int[] sa, int[] key, int maxKey, int n) {
        int[] count = new int[maxKey + 1];
        for (int i = 0; i < n; i++) {
            count[key[i]]++;
        }
        for (int i = 1; i <= maxKey; i++) {
            count[i] += count[i - 1];
        }
        int[] temp = new int[n];
        for (int i = n - 1; i >= 0; i--) {
            temp[--count[key[sa[i]]]] = sa[i];
        }
        System.arraycopy(temp, 0, sa, 0, n);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> aho(Map<String, Object> input) {
        List<?> rawPatterns = (List<?>) input.get("patterns");
        List<?> rawTexts = (List<?>) input.get("texts");
        int p = rawPatterns != null ? rawPatterns.size() : 0;
        int q = rawTexts != null ? rawTexts.size() : 0;

        if (q == 0) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("counts", Collections.emptyList());
            return res;
        }

        if (p == 0) {
            List<Object> counts = new ArrayList<>(q);
            for (int i = 0; i < q; i++) {
                counts.add(Collections.emptyList());
            }
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("counts", counts);
            return res;
        }

        int totalLen = 0;
        for (Object obj : rawPatterns) {
            totalLen += ((String) obj).length();
        }
        int maxNodes = totalLen + 1;
        final int ALPHABET = 95;
        int[][] next = new int[maxNodes][ALPHABET];
        int[] fail = new int[maxNodes];
        List<Integer>[] termPatterns = new List[maxNodes];
        int nodeCount = 1;

        for (int patIdx = 0; patIdx < p; patIdx++) {
            String pat = (String) rawPatterns.get(patIdx);
            int curr = 0;
            for (int i = 0; i < pat.length(); i++) {
                int c = pat.charAt(i) - 32;
                if (c < 0 || c >= ALPHABET) c = 0;
                if (next[curr][c] == 0) {
                    next[curr][c] = nodeCount++;
                }
                curr = next[curr][c];
            }
            if (termPatterns[curr] == null) {
                termPatterns[curr] = new ArrayList<>();
            }
            termPatterns[curr].add(patIdx);
        }

        int[] queue = new int[nodeCount];
        int headQueue = 0, tailQueue = 0;
        int[] bfsOrder = new int[nodeCount];
        int bfsSize = 0;

        for (int c = 0; c < ALPHABET; c++) {
            int v = next[0][c];
            if (v != 0) {
                fail[v] = 0;
                queue[tailQueue++] = v;
            }
        }

        while (headQueue < tailQueue) {
            int u = queue[headQueue++];
            bfsOrder[bfsSize++] = u;
            int f = fail[u];
            for (int c = 0; c < ALPHABET; c++) {
                int v = next[u][c];
                if (v != 0) {
                    fail[v] = next[f][c];
                    queue[tailQueue++] = v;
                } else {
                    next[u][c] = next[f][c];
                }
            }
        }

        List<Object> counts = new ArrayList<>(q);
        long[] visitCount = new long[nodeCount];

        for (int t = 0; t < q; t++) {
            String text = (String) rawTexts.get(t);
            Arrays.fill(visitCount, 0L);
            if (text != null && !text.isEmpty()) {
                int curr = 0;
                for (int i = 0; i < text.length(); i++) {
                    int c = text.charAt(i) - 32;
                    if (c >= 0 && c < ALPHABET) {
                        curr = next[curr][c];
                        visitCount[curr]++;
                    } else {
                        curr = 0;
                    }
                }

                for (int i = bfsSize - 1; i >= 0; i--) {
                    int u = bfsOrder[i];
                    visitCount[fail[u]] += visitCount[u];
                }
            }

            List<Long> row = new ArrayList<>(p);
            long[] patCounts = new long[p];
            for (int u = 0; u < nodeCount; u++) {
                if (termPatterns[u] != null && visitCount[u] > 0) {
                    long cnt = visitCount[u];
                    for (int patIdx : termPatterns[u]) {
                        patCounts[patIdx] = cnt;
                    }
                }
            }
            for (int i = 0; i < p; i++) {
                row.add(patCounts[i]);
            }
            counts.add(row);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("counts", counts);
        return res;
    }
}
