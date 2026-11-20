#!/usr/bin/env python3
"""Fetch the musl runtime from the OpenCode pack into the bundle's assets.

`kernel-opencode` is the only component that carries the runtime (opencode
binary + rootfs). The `bundle` flavor ships the rootfs inside the APK so local
OpenCode works out of the box; `thin` does not include this asset.

The pack is a tar.xz containing `opencode` and `rootfs/`; only `rootfs/` is
copied into `app/src/bundle/assets/runtime/rootfs.tar.xz` (re-compressed).

Extraction uses the system `tar` on purpose: the rootfs is full of symlinks
and hardlinks (busybox applets) that Python's tarfile resolves poorly.
"""
import json
import os
import subprocess
import sys
import tempfile
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEST = os.path.join(ROOT, "app", "src", "bundle", "assets", "runtime", "rootfs.tar.xz")
CACHE = os.path.join(ROOT, ".cache", "opencode-pack")
INDEX_URL = "https://raw.githubusercontent.com/a2heng/androc-market/main/index.json"
PACK_ID = "kernel-opencode"


def resolve_from_index():
    req = urllib.request.Request(INDEX_URL, headers={"User-Agent": "androc"})
    with urllib.request.urlopen(req, timeout=120) as resp:
        index = json.load(resp)
    for pack in index.get("packs", []):
        if pack.get("id") == PACK_ID:
            return pack["archive"]["url"]
    raise SystemExit(f"{PACK_ID} not in index")


def download(url, dest):
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    part = dest + ".part"
    last = None
    for attempt in range(5):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "androc"})
            with urllib.request.urlopen(req, timeout=600) as src, open(part, "wb") as out:
                while True:
                    chunk = src.read(1 << 20)
                    if not chunk:
                        break
                    out.write(chunk)
            os.replace(part, dest)
            return dest
        except Exception as exc:  # noqa: BLE001
            last = exc
            print(f"attempt {attempt + 1} failed: {exc}", file=sys.stderr)
    raise SystemExit(f"download failed: {url}: {last}")


def main():
    if os.path.isfile(DEST) and os.path.getsize(DEST) > 20 * 1024 * 1024:
        print("cached", DEST)
        return 0

    pack = os.environ.get("OPENCODE_PACK_FILE")
    if pack:
        print("USE", pack)
    else:
        url = os.environ.get("OPENCODE_PACK_URL") or resolve_from_index()
        print("GET", url)
        pack = os.path.join(CACHE, os.path.basename(url))
        if not os.path.isfile(pack):
            download(url, pack)

    os.makedirs(os.path.dirname(DEST), exist_ok=True)
    with tempfile.TemporaryDirectory(dir=CACHE) as tmp:
        subprocess.run(["tar", "-xJf", pack, "-C", tmp, "rootfs"], check=True)
        rootfs = os.path.join(tmp, "rootfs")
        for probe in ("bin/sh", "usr/bin/python3", "usr/bin/node"):
            if not os.path.exists(os.path.join(rootfs, probe)):
                raise SystemExit(f"rootfs missing {probe}")
        part = DEST + ".part"
        subprocess.run(["tar", "-cJf", part, "-C", tmp, "rootfs"], check=True)
        os.replace(part, DEST)
    print("wrote", DEST, os.path.getsize(DEST), "bytes")
    return 0


if __name__ == "__main__":
    sys.exit(main())
