#!/usr/bin/env python3
"""Fetch the runtime-core pack into the bundle's assets.

The `bundle` flavor ships the musl runtime inside the APK, so the local
OpenCode works out of the box. The `thin` flavor does not include this asset.
"""
import os
import shutil
import sys
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEST = os.path.join(ROOT, "app", "src", "bundle", "assets", "runtime", "rootfs.tar.xz")
DEFAULT_URL = ("https://github.com/a2heng/androc-market/releases/download/"
               "runtime-core-1-1/runtime-core-1-1.tar.xz")


def main():
    url = os.environ.get("RUNTIME_CORE_URL") or (sys.argv[1] if len(sys.argv) > 1 else DEFAULT_URL)
    os.makedirs(os.path.dirname(DEST), exist_ok=True)
    if os.path.isfile(DEST) and os.path.getsize(DEST) > 0:
        print("cached", DEST)
        return 0
    print("GET", url)
    part = DEST + ".part"
    last = None
    for attempt in range(5):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "androc"})
            with urllib.request.urlopen(req, timeout=300) as src, open(part, "wb") as out:
                shutil.copyfileobj(src, out, 1 << 20)
            os.replace(part, DEST)
            print("wrote", DEST, os.path.getsize(DEST), "bytes")
            return 0
        except Exception as exc:  # noqa: BLE001
            last = exc
            print(f"attempt {attempt + 1} failed: {exc}", file=sys.stderr)
    raise SystemExit(f"download failed: {url}: {last}")


if __name__ == "__main__":
    sys.exit(main())
