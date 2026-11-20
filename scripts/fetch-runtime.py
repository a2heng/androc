#!/usr/bin/env python3
"""下载 bundle 内置的 musl rootfs 到 assets/runtime/rootfs.tar.xz。

rootfs 由 androc-market 的 runtime-rootfs 工作流构建并作为构建输入发布，
不进市场索引。内容只有 `rootfs/`（Alpine musl + bash/git/python/node）。
"""
import os
import shutil
import sys
import tarfile
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEST = os.path.join(ROOT, "app", "src", "bundle", "assets", "runtime", "rootfs.tar.xz")
DEFAULT_URL = ("https://github.com/a2heng/androc-market/releases/download/"
               "runtime-rootfs-1/runtime-rootfs.tar.xz")


def download(url, dest):
    part = dest + ".part"
    last = None
    for attempt in range(5):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "androc"})
            with urllib.request.urlopen(req, timeout=900) as src, open(part, "wb") as out:
                shutil.copyfileobj(src, out, 1 << 20)
            os.replace(part, dest)
            return
        except Exception as exc:  # noqa: BLE001
            last = exc
            print(f"attempt {attempt + 1} failed: {exc}", file=sys.stderr)
    raise SystemExit(f"download failed: {url}: {last}")


def main():
    if os.path.isfile(DEST) and os.path.getsize(DEST) > 20 * 1024 * 1024:
        print("cached", DEST)
        return 0

    os.makedirs(os.path.dirname(DEST), exist_ok=True)
    url = os.environ.get("RUNTIME_ROOTFS_URL") or DEFAULT_URL
    print("GET", url)
    download(url, DEST)

    with tarfile.open(DEST, "r:xz") as tar:
        names = set(tar.getnames())
    for probe in ("rootfs/bin/sh", "rootfs/usr/bin/python3", "rootfs/usr/bin/node"):
        if probe not in names:
            raise SystemExit(f"rootfs missing {probe}")
    print("wrote", DEST, os.path.getsize(DEST), "bytes")
    return 0


if __name__ == "__main__":
    sys.exit(main())
