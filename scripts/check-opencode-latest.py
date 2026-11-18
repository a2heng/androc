#!/usr/bin/env python3
"""Check opencode.version against the newest upstream release.

Release CI runs this with --strict: an outdated pin blocks the release.
Local/PR runs stay advisory and only print a warning.
"""
import argparse
import json
import os
import re
import sys
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VERSION_FILE = os.path.join(ROOT, "opencode.version")
RELEASES_API = "https://api.github.com/repos/anomalyco/opencode/releases?per_page=100"


def read_pinned():
    try:
        with open(VERSION_FILE, encoding="utf-8") as f:
            version = f.read().strip().lstrip("v")
    except FileNotFoundError:
        raise SystemExit("opencode.version is missing")
    if not version:
        raise SystemExit("opencode.version is empty")
    return version


def fetch_latest():
    headers = {"User-Agent": "androc", "Accept": "application/vnd.github+json"}
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(RELEASES_API, headers=headers)
    with urllib.request.urlopen(req, timeout=30) as resp:
        releases = json.load(resp)
    versions = [
        r.get("tag_name", "").lstrip("v")
        for r in releases
        if not r.get("prerelease")
        and re.fullmatch(r"v?\d+\.\d+\.\d+", r.get("tag_name", ""))
    ]
    if not versions:
        raise SystemExit("no stable opencode release found")
    return max(versions, key=parse)


def parse(version):
    core = version.split("-")[0].lstrip("v")
    nums = re.findall(r"\d+", core)
    if not nums:
        raise ValueError(f"cannot parse version: {version}")
    parts = [int(x) for x in nums[:3]]
    while len(parts) < 3:
        parts.append(0)
    return tuple(parts)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--strict", action="store_true",
                    help="exit 1 when the pinned version is outdated")
    ap.add_argument("--quiet", action="store_true")
    args = ap.parse_args()

    pinned = read_pinned()
    try:
        latest = fetch_latest()
    except Exception as exc:  # network/API failure
        print(f"cannot check latest opencode: {exc}", file=sys.stderr)
        return 1 if args.strict else 0

    if parse(latest) > parse(pinned):
        print(f"opencode is outdated: pinned {pinned}, latest {latest}")
        print("run: python3 scripts/fetch-opencode.py --latest")
        return 1 if args.strict else 0
    if parse(pinned) > parse(latest):
        print(f"pinned opencode {pinned} is ahead of the latest release {latest}")
        print("check whether the tag is a pre-release or opencode.version is wrong")
        return 0
    if not args.quiet:
        print(f"opencode {pinned} is the latest release")
    return 0


if __name__ == "__main__":
    sys.exit(main())
