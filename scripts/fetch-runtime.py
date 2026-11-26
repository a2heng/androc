#!/usr/bin/env python3
"""下载 bundle 内置的 musl rootfs 到 assets/runtime/rootfs.tar.zst。

上游是 androc-market 的 runtime-rootfs-1（tar.xz）；本地统一转成 zstd，
解压更快且 Android 兼容。
"""
import os, shutil, subprocess, sys, tarfile, urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEST = os.path.join(ROOT, "app", "src", "oc", "assets", "runtime", "rootfs.tar.zst")
CACHE_XZ = os.path.join(ROOT, ".cache", "runtime-rootfs.tar.xz")
DEFAULT_URL = ("https://github.com/a2heng/androc-market/releases/download/"
               "runtime-rootfs-1/runtime-rootfs.tar.xz")

def download(url, dest):
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    part = dest + ".part"
    last = None
    for attempt in range(5):
        try:
            print("GET", url)
            req = urllib.request.Request(url, headers={"User-Agent": "androc"})
            with urllib.request.urlopen(req, timeout=900) as src, open(part, "wb") as out:
                shutil.copyfileobj(src, out, 1 << 20)
            os.replace(part, dest)
            return
        except Exception as exc:
            last = exc
            print(f"attempt {attempt + 1} failed: {exc}", file=sys.stderr)
    raise SystemExit(f"download failed: {url}: {last}")

def validate(path):
    if path.endswith(".zst"):
        p1 = subprocess.Popen(["zstd", "-dc", path], stdout=subprocess.PIPE)
        p2 = subprocess.Popen(["tar", "-tf", "-"], stdin=p1.stdout,
                              stdout=subprocess.PIPE, text=True)
        if p1.stdout:
            p1.stdout.close()
        out, _ = p2.communicate(); p1.wait()
        names = set(out.splitlines())
    else:
        with tarfile.open(path, "r:xz") as tar:
            names = set(tar.getnames())
    for probe in ("rootfs/bin/sh", "rootfs/usr/bin/python3", "rootfs/usr/bin/node"):
        if probe not in names:
            raise SystemExit(f"rootfs missing {probe}")

def repack_xz_to_zst(src_xz, dest_zst):
    tmp = dest_zst + ".tmp"
    p1 = subprocess.Popen(["xz", "-dc", src_xz], stdout=subprocess.PIPE)
    p2 = subprocess.Popen(["zstd", "-19", "-T0", "-o", tmp], stdin=p1.stdout)
    if p1.stdout:
        p1.stdout.close()
    p2.wait(); p1.wait()
    if p1.returncode != 0 or p2.returncode != 0:
        raise SystemExit("repack failed")
    os.replace(tmp, dest_zst)

def main():
    os.makedirs(os.path.dirname(DEST), exist_ok=True)
    if os.path.isfile(DEST) and os.path.getsize(DEST) > 20 * 1024 * 1024:
        validate(DEST)
        print("cached", DEST)
        return 0
    url = os.environ.get("RUNTIME_ROOTFS_URL") or DEFAULT_URL
    if not (os.path.isfile(CACHE_XZ) and os.path.getsize(CACHE_XZ) > 20 * 1024 * 1024):
        download(url, CACHE_XZ)
    repack_xz_to_zst(CACHE_XZ, DEST)
    validate(DEST)
    print("wrote", DEST, os.path.getsize(DEST), "bytes")
    return 0

if __name__ == "__main__":
    sys.exit(main())
