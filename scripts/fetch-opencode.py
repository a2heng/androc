#!/usr/bin/env python3
"""Fetch the official opencode linux-arm64-musl build into jniLibs.

The pinned version lives in opencode.version. Use --latest to resolve the
newest upstream release and rewrite that file.
"""
import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import tarfile
import time
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, ".cache", "opencode")
VERSION_FILE = os.path.join(ROOT, "opencode.version")
DEST = os.path.join(ROOT, "app", "src", "main", "jniLibs", "arm64-v8a",
                    "libopencode2_bin.so")
LATEST_API = "https://api.github.com/repos/anomalyco/opencode/releases?per_page=100"


def read_pinned():
    try:
        with open(VERSION_FILE, encoding="utf-8") as f:
            version = f.read().strip().lstrip("v")
    except FileNotFoundError:
        raise SystemExit("opencode.version is missing")
    if not version:
        raise SystemExit("opencode.version is empty")
    return version


def version_key(tag):
    core = tag.lstrip("v").split("-")[0]
    nums = [int(x) for x in core.split(".") if x.isdigit()]
    while len(nums) < 3:
        nums.append(0)
    return tuple(nums[:3])


def fetch_latest():
    headers = {"User-Agent": "androc", "Accept": "application/vnd.github+json"}
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(LATEST_API, headers=headers)
    with urllib.request.urlopen(req, timeout=60) as resp:
        releases = json.load(resp)
    versions = [
        r.get("tag_name", "").lstrip("v")
        for r in releases
        if not r.get("prerelease")
        and re.fullmatch(r"v?\d+\.\d+\.\d+", r.get("tag_name", ""))
    ]
    if not versions:
        raise SystemExit("no stable opencode release found")
    return max(versions, key=version_key)



def download(url, dest, version):
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    if os.path.isfile(dest) and os.path.getsize(dest) > 0:
        print("cached", dest)
        return
    print("GET", url)
    part = dest + ".part"
    last = None
    for attempt in range(5):
        try:
            req = urllib.request.Request(
                url, headers={"User-Agent": "androc"})
            with urllib.request.urlopen(req, timeout=180) as src, open(part, "wb") as out:
                shutil.copyfileobj(src, out, 1 << 20)
            os.replace(part, dest)
            return
        except Exception as e:
            last = e
            print(f"attempt {attempt + 1} failed: {e}", file=sys.stderr)
            time.sleep(min(2 ** attempt, 10))
    # GitHub release assets occasionally return 504 from the CDN; curl retries
    # the whole redirect chain more aggressively.
    if shutil.which("curl"):
        print("falling back to curl", file=sys.stderr)
        if os.path.exists(part):
            os.remove(part)
        try:
            subprocess.run(["curl", "-fL", "--retry", "10", "--retry-all-errors",
                            "--retry-delay", "2", "-A", "androc",
                            "-o", dest, url], check=True)
            return
        except subprocess.CalledProcessError:
            pass
    if download_with_gh(version, dest):
        return
    raise SystemExit(f"download failed: {last}")


def download_with_gh(version, dest):
    """Authenticated fallback for networks where direct GitHub is blocked."""
    gh = shutil.which("gh")
    if not gh:
        return False
    asset = "opencode-linux-arm64-musl.tar.gz"
    tmp = dest + ".gh"
    os.makedirs(tmp, exist_ok=True)
    print("falling back to gh release download", file=sys.stderr)
    try:
        result = subprocess.run(
            [gh, "release", "download", f"v{version}",
             "-R", "anomalyco/opencode", "-p", asset, "-D", tmp, "--clobber"],
            check=False,
        )
        src = os.path.join(tmp, asset)
        if result.returncode == 0 and os.path.isfile(src):
            os.replace(src, dest)
            return True
        return False
    finally:
        shutil.rmtree(tmp, ignore_errors=True)



def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--version",
                    help="pinned version (default: read opencode.version)")
    ap.add_argument("--latest", action="store_true",
                    help="resolve the latest upstream release and update opencode.version")
    args = ap.parse_args()

    version = args.version
    if args.latest:
        version = fetch_latest()
        print("latest opencode:", version)
    if not version:
        version = read_pinned()

    name = f"opencode-linux-arm64-musl-{version}.tar.gz"
    archive = os.path.join(CACHE, name)
    url = ("https://github.com/anomalyco/opencode/releases/download/"
           f"v{version}/opencode-linux-arm64-musl.tar.gz")
    download(url, archive, version)

    os.makedirs(os.path.dirname(DEST), exist_ok=True)
    with tarfile.open(archive, "r:gz") as tar:
        member = next((m for m in tar.getmembers() if m.name in ("opencode", "./opencode")), None)
        if member is None:
            raise SystemExit("opencode binary not found in archive")
        with tar.extractfile(member) as src, open(DEST, "wb") as out:
            shutil.copyfileobj(src, out, 1 << 20)
    os.chmod(DEST, 0o755)
    print("wrote", DEST, os.path.getsize(DEST), "bytes")
    if args.latest:
        with open(VERSION_FILE, "w", encoding="utf-8") as f:
            f.write(version + "\n")
        print("pinned opencode", version, "in opencode.version")


if __name__ == "__main__":
    main()
