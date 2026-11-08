import json
import random
import subprocess
import time
import sys
import pathlib

P = pathlib.Path(__file__).resolve().parents[1]

def main():
    cmd = ['java', '-Xmx384m', '-cp', str(P / 'Java' / 'build'), 'Main']
    proc = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, encoding='utf-8')

    def send_and_measure(req):
        line = json.dumps(req) + "\n"
        start = time.perf_counter()
        proc.stdin.write(line)
        proc.stdin.flush()
        out_line = proc.stdout.readline()
        elapsed = (time.perf_counter() - start) * 1000.0
        return json.loads(out_line.strip()), elapsed

    # Warmup
    for _ in range(50):
        send_and_measure({"task": "kmp", "text": "ab" * 500, "pattern": "ab"})

    seed = 20261108
    random.seed(seed)

    sizes = [
        (500, 50),
        (1500, 150),
        (3000, 300)
    ]

    print("Case,n,m,Seed/Generator,Algorithm,Run 1 (ms),Run 2 (ms),Run 3 (ms),Median (ms),Correct outputs checked")

    for case_type in ["Random", "Repetitive"]:
        for n, m in sizes:
            if case_type == "Random":
                chars = "abcdefghijklmnopqrstuvwxyz"
                text = "".join(random.choices(chars, k=n))
                pattern = "".join(random.choices(chars, k=m))
            else:
                # Highly repetitive case: worst-case for naive
                text = "a" * (n - 1) + "b"
                pattern = "a" * (m - 1) + "b"

            # Check correctness across all three
            res_naive, _ = send_and_measure({"task": "naive", "text": text, "pattern": pattern})
            res_kmp, _ = send_and_measure({"task": "kmp", "text": text, "pattern": pattern})
            res_rk, _ = send_and_measure({"task": "rk", "text": text, "pattern": pattern})

            if not (res_naive["positions"] == res_kmp["positions"] == res_rk["positions"]):
                raise ValueError(f"Mismatch at {case_type} {n} {m}")

            for alg in ["naive", "kmp", "rk"]:
                req = {"task": alg, "text": text, "pattern": pattern}
                # 2 warmups
                send_and_measure(req)
                send_and_measure(req)
                runs = []
                for _ in range(3):
                    _, t = send_and_measure(req)
                    runs.append(t)
                runs_sorted = sorted(runs)
                med = runs_sorted[1]
                print(f"{case_type},{n},{m},{seed},{alg},{runs[0]:.3f},{runs[1]:.3f},{runs[2]:.3f},{med:.3f},MATCH (pos={len(res_naive['positions'])})")

    proc.stdin.close()
    proc.wait()

if __name__ == "__main__":
    main()
