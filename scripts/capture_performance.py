#!/usr/bin/env python3
"""Capture a bounded foreground session without collecting accounts, URLs or logcat."""
import argparse
import json
from pathlib import Path
import subprocess
import sys

PACKAGE = "in.synthora.musicbox"


def run(adb, args, timeout=15):
    result = subprocess.run([*adb, *args], stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                            timeout=timeout, check=True)
    return result.stdout


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", default="adb", help="Path to adb, including adb.exe on Windows")
    parser.add_argument("--serial", help="Required when more than one device is connected")
    parser.add_argument("--seconds", type=int, default=60, choices=range(10, 301), metavar="10..300")
    parser.add_argument("--output", type=Path, required=True, help="New output directory; existing captures are never overwritten")
    args = parser.parse_args()
    adb = [args.adb, *(["-s", args.serial] if args.serial else [])]
    try:
        if run(adb, ["get-state"]).strip() != b"device":
            raise RuntimeError("ADB device is not connected and authorized")
        args.output.mkdir(parents=True, exist_ok=False)
        run(adb, ["shell", "dumpsys", "gfxinfo", PACKAGE, "reset"])
        print("Keep Music Box visible and repeat the same journey. Avoid Android Settings.", flush=True)
        for name, command in (
            ("thermal-before.txt", ["shell", "dumpsys", "thermalservice"]),
            ("cpu.txt", ["shell", "top", "-b", "-d", "2", "-n", str(args.seconds // 2)]),
            ("rendering.txt", ["shell", "dumpsys", "gfxinfo", PACKAGE]),
            ("thermal-after.txt", ["shell", "dumpsys", "thermalservice"]),
        ):
            args.output.joinpath(name).write_bytes(run(adb, command, timeout=args.seconds + 30))
        args.output.joinpath("capture.json").write_text(json.dumps({
            "package": PACKAGE, "requestedSeconds": args.seconds,
            "note": "Record journey, build variant, output, refresh rate, brightness, power and charging state when comparing."
        }, indent=2), encoding="utf-8")
        print(f"Saved capture to {args.output}")
    except (OSError, RuntimeError, subprocess.SubprocessError) as error:
        print(f"Capture failed: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
