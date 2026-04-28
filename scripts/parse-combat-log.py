#!/usr/bin/env python3
"""Parse TerminatorPlus combat-debug log lines ([tplus-cbt]) and report
per-bot decision breakdowns.

Usage:
    python scripts/parse-combat-log.py <path-to-server.log> [--bot NAME]

Outputs:
    - total events by type
    - weapon-pick histogram (what did the director choose, and how often?)
    - swing gate rejections (charge too low / target i-framed)
    - melee try -> hit conversion rate
    - mace state-machine phase transitions
    - dir-noop reasons (ticks where NOTHING fired)
    - first/last event timestamps + tick range
    - a "red flags" section that highlights common pathologies (e.g. 100%
      of picks were END_CRYSTAL, zero melee hits landed, etc.)
"""

from __future__ import annotations

import argparse
import re
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from pathlib import Path

LINE_RE = re.compile(
    r"\[(?P<ts>\d{2}:\d{2}:\d{2})\].*?\[tplus-cbt\]\s+"
    r"(?P<bot>\S+)\s+t=(?P<tick>\d+)\s+(?P<event>\S+)"
    r"(?:\s+(?P<kv>.*))?$"
)
KV_RE = re.compile(r"(\w+)=(\S+)")


@dataclass
class BotStats:
    name: str
    events: Counter = field(default_factory=Counter)
    weapons: Counter = field(default_factory=Counter)
    noop_reasons: Counter = field(default_factory=Counter)
    swing_blocks: Counter = field(default_factory=Counter)
    mace_phase_transitions: Counter = field(default_factory=Counter)
    melee_try: int = 0
    melee_hit: int = 0
    melee_oor: int = 0
    mace_smash: int = 0
    mace_smash_iframed: int = 0
    mace_cd_skips: int = 0
    first_ts: str | None = None
    last_ts: str | None = None
    first_tick: int | None = None
    last_tick: int | None = None
    weapon_pick_by_distance_bucket: dict[str, Counter] = field(
        default_factory=lambda: defaultdict(Counter)
    )


def parse_kv(blob: str | None) -> dict[str, str]:
    if not blob:
        return {}
    return dict(KV_RE.findall(blob))


def dist_bucket(d: float) -> str:
    if d < 1.5:
        return "0.0-1.5"
    if d < 3.5:
        return "1.5-3.5"
    if d < 6.0:
        return "3.5-6.0"
    if d < 12.0:
        return "6.0-12.0"
    if d < 28.0:
        return "12.0-28.0"
    return "28.0+"


def parse(path: Path, bot_filter: str | None) -> dict[str, BotStats]:
    stats: dict[str, BotStats] = {}

    with path.open("r", encoding="utf-8", errors="replace") as fh:
        for line in fh:
            if "[tplus-cbt]" not in line:
                continue
            m = LINE_RE.search(line)
            if not m:
                continue
            bot = m["bot"]
            if bot_filter and bot != bot_filter:
                continue
            tick = int(m["tick"])
            event = m["event"]
            ts = m["ts"]
            kv = parse_kv(m["kv"])

            s = stats.setdefault(bot, BotStats(name=bot))
            s.events[event] += 1
            if s.first_ts is None:
                s.first_ts = ts
                s.first_tick = tick
            s.last_ts = ts
            s.last_tick = tick

            if event == "weapon-pick":
                w = kv.get("w", "?")
                s.weapons[w] += 1
                try:
                    d = float(kv.get("dist", "nan"))
                    s.weapon_pick_by_distance_bucket[dist_bucket(d)][w] += 1
                except ValueError:
                    pass
            elif event == "dir-noop":
                s.noop_reasons[kv.get("reason", "?")] += 1
            elif event == "swing-block":
                s.swing_blocks[kv.get("reason", "?")] += 1
            elif event == "melee-try":
                s.melee_try += 1
            elif event == "melee-hit":
                s.melee_hit += 1
            elif event == "melee-oor":
                s.melee_oor += 1
            elif event == "mace-smash":
                s.mace_smash += 1
                if kv.get("iframes") == "true":
                    s.mace_smash_iframed += 1
            elif event == "mace-cd":
                s.mace_cd_skips += 1
            elif event == "mace-phase":
                s.mace_phase_transitions[
                    f'{kv.get("from", "?")}->{kv.get("to", "?")}'
                ] += 1
    return stats


def fmt_counter(c: Counter, total: int | None = None, top: int = 15) -> str:
    if not c:
        return "  (none)"
    if total is None:
        total = sum(c.values())
    lines = []
    for k, v in c.most_common(top):
        pct = 100.0 * v / total if total else 0.0
        lines.append(f"  {v:>8,}  {pct:5.1f}%  {k}")
    if len(c) > top:
        rest = sum(v for _, v in c.most_common()[top:])
        lines.append(f"  {rest:>8,}        (+{len(c) - top} other)")
    return "\n".join(lines)


def report(bot: BotStats) -> None:
    print("=" * 72)
    print(f"bot: {bot.name}")
    print(f"  window: {bot.first_ts}..{bot.last_ts}  "
          f"ticks {bot.first_tick}..{bot.last_tick}  "
          f"({(bot.last_tick or 0) - (bot.first_tick or 0):,} ticks spanned)")
    print(f"  total events: {sum(bot.events.values()):,}")
    print()

    print("events by type:")
    print(fmt_counter(bot.events, top=20))
    print()

    print("weapon-pick distribution:")
    print(fmt_counter(bot.weapons, top=20))
    print()

    if bot.weapon_pick_by_distance_bucket:
        print("weapon-pick by distance:")
        for bucket in [
            "0.0-1.5", "1.5-3.5", "3.5-6.0", "6.0-12.0", "12.0-28.0", "28.0+"
        ]:
            if bucket not in bot.weapon_pick_by_distance_bucket:
                continue
            c = bot.weapon_pick_by_distance_bucket[bucket]
            top_str = ", ".join(f"{k}={v:,}" for k, v in c.most_common(4))
            print(f"  {bucket:>10}  total={sum(c.values()):,}   {top_str}")
        print()

    if bot.swing_blocks:
        print("swing-block reasons (why canSwing() returned false):")
        print(fmt_counter(bot.swing_blocks))
        print()

    if bot.mace_phase_transitions:
        print("mace phase transitions:")
        print(fmt_counter(bot.mace_phase_transitions))
        print()

    print("melee conversion:")
    print(f"  melee-try: {bot.melee_try:,}")
    print(f"  melee-hit: {bot.melee_hit:,}  "
          f"({100.0 * bot.melee_hit / bot.melee_try if bot.melee_try else 0.0:.1f}% of tries)")
    print(f"  melee-oor: {bot.melee_oor:,}  (distance > ATTACK_RANGE)")
    print()

    print("mace activity:")
    print(f"  mace-smash:          {bot.mace_smash:,}  "
          f"({bot.mace_smash_iframed:,} wasted on i-frames)")
    print(f"  mace-cd swings:      {bot.mace_cd_skips:,}  "
          f"(stay-and-swing while jump cooldown burns)")
    print()

    if bot.noop_reasons:
        print("dir-noop (no branch matched) reasons:")
        print(fmt_counter(bot.noop_reasons))
        print()

    # ---- red flags ----
    flags: list[str] = []
    total_picks = sum(bot.weapons.values())
    if total_picks:
        for w, v in bot.weapons.items():
            if v / total_picks >= 0.95 and v > 50:
                flags.append(
                    f"  !! {w} accounts for {100.0 * v / total_picks:.1f}% of "
                    f"weapon picks ({v:,}/{total_picks:,}). Pipeline is stuck."
                )
        if bot.weapons.get("END_CRYSTAL", 0) > 0:
            crystal_close = sum(
                v for bucket, c in bot.weapon_pick_by_distance_bucket.items()
                for w, v in c.items()
                if w == "END_CRYSTAL" and bucket in ("0.0-1.5",)
            )
            if crystal_close > 50:
                flags.append(
                    f"  !! {crystal_close:,} END_CRYSTAL picks at distance < 1.5 — "
                    f"the bot is point-blank-nuking itself."
                )
    if bot.melee_try == 0 and bot.melee_hit == 0 and total_picks > 100:
        flags.append(
            "  !! zero melee attempts recorded. "
            "Either no sword/axe in loadout, or a higher-priority branch "
            "(crystal / anchor / mace-airborne) preempts every tick."
        )
    if bot.melee_try and bot.melee_hit == 0:
        flags.append(
            f"  !! {bot.melee_try:,} melee-try events but 0 melee-hit — every swing "
            f"was gated by canSwing(). Check swing-block reasons above."
        )
    if bot.mace_smash and bot.mace_smash_iframed / bot.mace_smash > 0.5:
        flags.append(
            f"  !! {100.0 * bot.mace_smash_iframed / bot.mace_smash:.0f}% of mace "
            f"smashes wasted on i-frames ({bot.mace_smash_iframed}/{bot.mace_smash})."
        )

    if flags:
        print("red flags:")
        for f in flags:
            print(f)
    else:
        print("red flags: none detected")
    print()


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("log", type=Path, help="path to server.log")
    ap.add_argument("--bot", help="only report on this bot name")
    args = ap.parse_args()

    if not args.log.exists():
        print(f"log not found: {args.log}", file=sys.stderr)
        return 1

    stats = parse(args.log, args.bot)
    if not stats:
        print("no [tplus-cbt] lines found.", file=sys.stderr)
        return 2

    print(f"source: {args.log}")
    print(f"bots seen: {len(stats)}  names: {', '.join(sorted(stats))}")
    print()
    for name in sorted(stats):
        report(stats[name])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
