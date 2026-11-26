#!/usr/bin/env python3
"""Fetch the pinned Android toolchain pack into app/src/apk/assets/android/pack.tar.zst.

Pinned version lives in android-pack.version; sha256 in android-pack.sha256.
Assets are hosted on the androc repository (tag assets-1), not a market repo.
"""
import hashlib, os, shutil, sys, urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VER_FILE = os.path.join(ROOT, "android-pack.version")
SHA_FILE = os.path.join(ROOT, "android-pack.sha256")
DEST = os.path.join(ROOT, "app", "src", "apk", "assets", "android", "pack.tar.zst")
URL = ("https://github.com/a2heng/androc/releases/download/assets-1/"
       "android-{ver}.tar.zst")

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

def main():
    ver = read(VER_FILE)
    want = read(SHA_FILE).lower()
    if os.path.isfile(DEST) and sha256(DEST) == want:
        print("cached", DEST)
        return 0
    cache = os.path.join(ROOT, ".cache", f"android-{ver}.tar.zst")
    for cand in (cache, cache + ".part"):
        if os.path.isfile(cand) and sha256(cand) == want:
            shutil.copy2(cand, DEST)
            print("copied", cand, "->", DEST)
            return 0
    download(URL.format(ver=ver), cache)
    got = sha256(cache)
    if got != want:
        raise SystemExit(f"sha256 mismatch: got {got}, want {want}")
    shutil.copy2(cache, DEST)
    print("wrote", DEST, os.path.getsize(DEST), "bytes")
    return 0

if __name__ == "__main__":
    sys.exit(main())
