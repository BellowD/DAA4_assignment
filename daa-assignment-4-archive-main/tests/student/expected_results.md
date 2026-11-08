# Student Custom Tests

Custom test cases are located in `custom_cases.jsonl` and their verified actual outputs in `actual_results.jsonl`.

| Line | Task | Input summary | Expected Result | What defect this test detects |
|---:|---|---|---|---|
| 1 | `lcs` | Identical strings `"ABCDE"`, `"ABCDE"` | `length: 5`, `sequence: "ABCDE"` | Checks full match; detects off-by-one errors or early termination. |
| 2 | `lcs` | Disjoint strings `"ABCDE"`, `"FGHIJ"` | `length: 0`, `sequence: ""` | Checks zero intersection; detects false positives when no characters match. |
| 3 | `merge` | 5 blocks `[10, 20, 30, 40, 50]` | `cost: 330`, 4 valid merges | Checks multi-way dynamic partitioning; detects greedy choice defects. |
| 4 | `merge` | 2 blocks `[100, 200]` | `cost: 300`, `[[0, 0, 1]]` | Checks base multi-block merge; detects boundary indexing issues. |
| 5 | `tree` | Star graph (center 100, four leaves 10 each) | `weight: 100`, `selected: [0]` | Checks root selection vs leaf sum ($100 > 40$); detects greedy tree decisions. |
| 6 | `tree` | Line graph $N=5$, weights `[10, 1, 10, 1, 10]` | `weight: 30`, `selected: [0, 2, 4]` | Checks alternating optimal independent set; detects neighbor conflict leakage. |
| 7 | `naive` | Full match `"hello"`, `"hello"` | `positions: [0]` | Checks single exact full-string match boundary. |
| 8 | `naive` | Pattern longer than text | `positions: []` | Detects index-out-of-bounds when $m > n$. |
| 9 | `kmp` | Repetitive overlap `"aaaaa"`, `"aa"` | `positions: [0, 1, 2, 3]` | Detects missing overlapping starts when resetting index after full match. |
| 10 | `kmp` | Pattern at end `"the quick brown fox"`, `"fox"` | `positions: [16]` | Detects termination before scanning the final character. |
| 11 | `rk` | Hash collision `"abczdj"`, `"ab"` (hashes equal 75 mod 101) | `positions: [0]` | Detects false positives when hash equality is not verified character-by-character. |
| 12 | `rk` | Hash collision `"bczzek"`, `"bc"` (hashes equal 6 mod 101) | `positions: [0]` | Validates that collision `"ek"` at end is discarded. |
| 13 | `suffix` | Repetitive text `"aaaa"` | `sa: [3, 2, 1, 0]`, `lcp: [0, 1, 2, 3]`, `repeated: "aaa"` | Detects incorrect rank updates when all characters are identical. |
| 14 | `suffix` | Distinct text `"abcdef"` | `sa: [0, 1, 2, 3, 4, 5]`, `lcp: [0, 0, 0, 0, 0, 0]`, `repeated: ""` | Detects false repeated substrings when all characters are unique. |
| 15 | `aho` | Substring patterns `["a", "aa", "aaa"]`, text `"aaaa"` | `counts: [[4, 3, 2], [1, 0, 0]]` | Detects failure to propagate counts to shorter prefix/suffix patterns. |
| 16 | `aho` | Duplicate patterns `["test", "test"]`, text `"testtest"` | `counts: [[2, 2]]` | Detects missing or overwriting counts for duplicate dictionary entries. |
