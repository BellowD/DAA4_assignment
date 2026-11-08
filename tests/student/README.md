# Student tests

- `custom_cases.jsonl`: Independent request fixtures covering edge cases, large scales, overlapping patterns, and hash collisions across all 8 assignment commands.
- `actual_results.jsonl`: Verified actual outputs produced by running `python tools/run.py --language Java --input tests/student/custom_cases.jsonl`.
- `expected_results.md`: Detailed documentation and explanation of expected outputs and what defect each test detects.
