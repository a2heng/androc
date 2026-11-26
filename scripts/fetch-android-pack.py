#!/usr/bin/env python3
"""Fetch the pinned Android toolchain pack into app/src/apk/assets/android/pack.tar.zst.

Pinned version lives in android-pack.version; sha256 (of the upstream tar.xz)
in android-pack.sha256. The xz is repacked to zstd for faster on-device
extraction. The asset host is the androc-market repository (asset-only).
"""
import hashlib, os, shutil, subprocess, sys, urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VER_FILE = os.path.join(ROOT, "android-pack.version")
SHA_FILE = os.path.join(ROOT, "android-pack.sha256")
DEST = os.path.join(ROOT, "app", "src", "apk", "assets", "android", "pack.tar.zst")
URL = ("https://github.com/a2heng/androc-market/releases/download/android-{ver}/"
       "android-{ver}.tar.xz")

def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read().strip()

def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for b in iter(lambda: f.read(1 << 20), b""):
            h.update(b)
    return h.hexdigest()

def download(url, dest):
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    part = dest + ".part"
    print("GET", url)
    req = urllib.request.Request(url, headers={"User-Agent": "androc"})
    proxy = (os.environ.get("HTTPS_PROXY") or os.environ.get("https_proxy")
             or os.environ.get("HTTP_PROXY") or os.environ.get("http_proxy"))
    opener = urllib.request.build_opener()
    if proxy:
        opener = urllib.request.build_opener(
            urllib.request.ProxyHandler({"https": proxy, "http": proxy}))
    with opener.open(req, timeout=600) as src, open(part, "wb") as out:
        shutil.copyfileobj(src, out, 1 << 20)
    os.replace(part, dest)

def repack(src_xz, dest_zst):
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
    ver = read(VER_FILE)
    want = read(SHA_FILE).lower()
    cache = os.path.join(ROOT, ".cache", f"android-{ver}.tar.xz")
    if os.path.isfile(DEST) and os.path.getsize(DEST) > 100 * 1024 * 1024:
        # The zst is a local derivative; accept it when the cached xz matches.
        if os.path.isfile(cache) and sha256(cache) == want:
            print("cached", DEST)
            return 0
    for cand in (cache, cache + ".part"):
        if os.path.isfile(cand) and sha256(cand) == want:
            repack(cand, DEST)
            print("wrote", DEST, os.path.getsize(DEST), "bytes")
            return 0
    download(URL.format(ver=ver), cache)
    got = sha256(cache)
    if got != want:
        raise SystemExit(f"sha256 mismatch: got {got}, want {want}")
    repack(cache, DEST)
    print("wrote", DEST, os.path.getsize(DEST), "bytes")
    return 0

if __name__ == "__main__":
    sys.exit(main())
