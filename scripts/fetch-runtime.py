#!/usr/bin/env python3
"""下载 bundle 内置的 musl rootfs 到 assets/runtime/rootfs.tar.zst。

Assets are hosted on the androc repository (tag assets-1).
"""
import hashlib, os, shutil, sys, urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEST = os.path.join(ROOT, "app", "src", "oc", "assets", "runtime", "rootfs.tar.zst")
SHA_FILE = os.path.join(ROOT, "runtime-rootfs.sha256")
URL = ("https://github.com/a2heng/androc/releases/download/assets-1/"
       "runtime-rootfs.tar.zst")

def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for b in iter(lambda: f.read(1 << 20), b""):
            h.update(b)
    return h.hexdigest()

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

def main():
    want = open(SHA_FILE, encoding="utf-8").read().strip().lower()
    if os.path.isfile(DEST) and os.path.getsize(DEST) > 20 * 1024 * 1024 and sha256(DEST) == want:
        print("cached", DEST)
        return 0
    download(URL, DEST)
    got = sha256(DEST)
    if got != want:
        raise SystemExit(f"sha256 mismatch: got {got}, want {want}")
    print("wrote", DEST, os.path.getsize(DEST), "bytes")
    return 0

if __name__ == "__main__":
    sys.exit(main())
