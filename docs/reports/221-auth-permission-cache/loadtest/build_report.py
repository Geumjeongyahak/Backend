#!/usr/bin/env python3
"""raw/<label>/ 의 측정 원본을 모아 ../data.json 을 쓰고, ../index.html 에 박아 넣는다.

    python3 build_report.py            # raw/ 아래 before · after 를 읽는다

index.html 은 data.json 을 <script id="data"> 안에 그대로 담는다 (파일 하나로 열리게).
"""
import json
import re
import statistics
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
RAW = ROOT / "raw"
# 2부: 닫힌 모델 (동시 사용자). 3부: rate-<before|after>-<회차> — 같은 VM 에서 번갈아 돈 고정 도착률
LABELS = ["before", "before-run1", "before-db-standard"]
METRICS = ["p50", "p95", "p99", "rps", "queries_per_req", "tx_per_req", "app_cpu", "db_cpu",
           "hikari_active_avg", "fail_rate"]


def vmstat_cpu(path: Path):
    """vmstat 1초 표본 → (평균 CPU 사용률 %, 평균 steal %). 첫 표본은 부팅 이후 평균이라 버린다."""
    rows = []
    for line in path.read_text().splitlines():
        parts = line.split()
        if len(parts) >= 17 and parts[0].isdigit():
            rows.append(parts)
    rows = rows[1:]
    if not rows:
        return None, None
    idle = [int(r[14]) for r in rows]
    steal = [int(r[16]) for r in rows]
    return round(100 - statistics.mean(idle), 1), round(statistics.mean(steal), 1)


def hikari_stats(path: Path):
    """1초 표본 '활성 대기' → (평균 활성, 평균 대기, 최대 대기). 없으면 None."""
    if not path.exists():
        return None, None, None
    rows = [tuple(map(float, l.split()[:2])) for l in path.read_text().splitlines() if len(l.split()) >= 2]
    if not rows:
        return None, None, None
    return (round(statistics.mean(r[0] for r in rows), 1), round(statistics.mean(r[1] for r in rows), 1),
            int(max(r[1] for r in rows)))


def pgss(path: Path):
    """pg_stat_statements 덤프 → (SELECT 실행 수, BEGIN 수, DB 실행 시간 합 ms, 상위 쿼리 목록).

    BEGIN 은 트랜잭션 수로 따로 센다. 측정 대상과 무관한 문장(스케줄러의 만료 처리 UPDATE,
    통계 초기화)은 뺀다.
    """
    total, begins, exec_ms, top = 0, 0, 0.0, []
    for line in path.read_text().splitlines():
        cols = line.split("\t")
        if len(cols) != 4:
            continue
        calls, tot, mean, query = int(cols[0]), float(cols[1]), float(cols[2]), cols[3]
        if "pg_stat_statements" in query or not query.lower().startswith(("select", "begin")):
            continue
        if query.lower().startswith("begin"):
            begins += calls
            continue
        total += calls
        exec_ms += tot
        top.append({"calls": calls, "mean_ms": mean, "query": shorten(query)})
    return total, begins, round(exec_ms, 1), top[:8]


def shorten(sql: str) -> str:
    """select 열 목록을 줄여 읽을 수 있게 한다."""
    sql = re.sub(r"select .*? from ", "select … from ", sql, count=1, flags=re.I)
    return sql[:160]


def load(label: str):
    d = RAW / label
    if not d.is_dir():
        return None
    runs = []
    for k6 in sorted(d.glob("*.k6.json")):
        name = k6.name.removesuffix(".k6.json")
        scenario, level = name.rsplit("-", 1)
        mode, c = ("rate" if level[0] == "r" else "vus"), level[1:]
        m = json.loads(k6.read_text())["metrics"]
        dur = m["http_req_duration"]
        reqs = int(m["http_reqs"]["count"])
        q_total, begins, db_ms, top = pgss(d / f"{name}.pgss.tsv")
        app_cpu, app_st = vmstat_cpu(d / f"{name}.app-vmstat.txt")
        db_cpu, db_st = vmstat_cpu(d / f"{name}.db-vmstat.txt")
        hikari = hikari_stats(d / f"{name}.hikari.txt")
        runs.append({
            "scenario": scenario, "vus": int(c), "mode": mode,
            "requests": reqs, "rps": round(m["http_reqs"]["rate"], 1),
            "avg": round(dur["avg"], 2), "p50": round(dur["med"], 2), "p90": round(dur["p(90)"], 2),
            "p95": round(dur["p(95)"], 2), "p99": round(dur["p(99)"], 2), "max": round(dur["max"], 2),
            "fail_rate": m["http_req_failed"]["value"],
            "queries_per_req": round(q_total / reqs, 2) if reqs else None,
            "tx_per_req": round(begins / reqs, 2) if reqs else None,
            "db_ms_per_req": round(db_ms / reqs, 3) if reqs else None,
            "app_cpu": app_cpu, "app_steal": app_st, "db_cpu": db_cpu, "db_steal": db_st,
            "hikari_active_avg": hikari[0], "hikari_pending_avg": hikari[1], "hikari_pending_max": hikari[2],
            "top_queries": top,
        })
    env = (d / "env.txt").read_text() if (d / "env.txt").exists() else ""
    return {"runs": runs, "env": env}


def compare():
    """rate-before-N · rate-after-N 을 시나리오 × 도착률별로 묶어 회차 평균과 회차별 값을 낸다."""
    rounds = {}
    for d in sorted(RAW.glob("rate-*-*")):
        _, side, n = d.name.split("-")
        rounds.setdefault(side, {})[n] = load(d.name)
    if "before" not in rounds or "after" not in rounds:
        return None
    rows = []
    keys = sorted({(r["scenario"], r["vus"]) for side in rounds.values() for x in side.values() for r in x["runs"]})
    for scenario, rate in keys:
        row = {"scenario": scenario, "rate": rate}
        for side in ("before", "after"):
            runs = [r for x in rounds[side].values() for r in x["runs"] if r["scenario"] == scenario and r["vus"] == rate]
            row[side] = {m: round(statistics.mean(r[m] for r in runs), 3) for m in METRICS if all(r[m] is not None for r in runs)}
            row[side]["rounds"] = [{m: r[m] for m in ("p50", "p95", "queries_per_req", "app_cpu")} for r in runs]
        rows.append(row)
    env = {side: next(iter(v.values()))["env"] for side, v in rounds.items()}
    return {"rows": rows, "env": env, "rounds": {k: sorted(v) for k, v in rounds.items()}}


def main():
    data = {label: load(label) for label in LABELS}
    data = {k: v for k, v in data.items() if v}
    cmp = compare()
    if cmp:
        data["compare"] = cmp
    (ROOT / "data.json").write_text(json.dumps(data, ensure_ascii=False, indent=1))
    html = (HERE / "report.template.html").read_text()
    html = html.replace("/*__DATA__*/{}", json.dumps(data, ensure_ascii=False).replace("</", "<\\/"))
    (ROOT / "index.html").write_text(html)
    print("wrote", ROOT / "index.html", {k: len(v.get("runs", v.get("rows", []))) for k, v in data.items()})


if __name__ == "__main__":
    main()
