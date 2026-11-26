#!/usr/bin/env python3
"""Fetch the pinned Android toolchain pack into app/src/apk/assets/android/pack.tar.xz.

Pinned version lives in android-pack.version; sha256 in android-pack.sha256.
"""
import hashlib, os, shutil, sys, urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VER_FILE = os.path.join(ROOT, "android-pack.version")
SHA_FILE = os.path.join(ROOT, "android-pack.sha256")
DEST = os.path.join(ROOT, "app", "src", "apk", "assets", "android", "pack.tar.zst")
CACHE = os.path.join(ROOT, ".cache", f"android-{{}}.tar.xz")
URL = "https://github.com/a2heng/androc-market/releases/download/android-{ver}/android-{ver}.tar.xz"

def read(path):
    with open(path, encoding="utf-8") as f: return f.read().strip()

def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for b in iter(lambda: f.read(1 << 20), b""): h.update(b)
    return h.hexdigest()

def download(url, dest):
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    part = dest + ".part"
    print("GET", url)
    req = urllib.request.Request(url, headers={"User-Agent": "androc"})
    proxy = os.environ.get("HTTPS_PROXY") or os.environ.get("https_proxy") or os.environ.get("HTTP_PROXY") or os.environ.get("http_proxy")
    opener = urllib.request.build_opener()
    if proxy:
        opener = urllib.request.build_opener(urllib.request.ProxyHandler({"https": proxy, "http": proxy}))
    with opener.open(req, timeout=300) as src, open(part, "wb") as out:
        shutil.copyfileobj(src, out, 1 << 20)
    os.replace(part, dest)

def main():
    ver = read(VER_FILE)
    want = read(SHA_FILE).lower()
    cache = os.path.join(ROOT, ".cache", f"android-{ver}.tar.xz")
    os.makedirs(os.path.dirname(DEST), exist_ok=True)
    if os.path.isfile(DEST) and sha256(DEST) == want:
        print("ok", DEST)
        return
    for cand in [cache, cache + ".part"]:
        if os.path.isfile(cand) and sha256(cand) == want:
            shutil.copy2(cand, cache)
            shutil.copy2(cache, DEST)
            print("copied", cand, "->", DEST)
            return
    download(URL.format(ver=ver), cache)
    got = sha256(cache)
    if got != want:
        raise SystemExit(f"sha256 mismatch: got {got}, want {want}")
    # 上游是 xz；本地转成 zst 加快设备解压
    if DEST.endswith('.zst') or os.environ.get('ANDROID_PACK_ZST','1')=='1':
        zst_dest = DEST if DEST.endswith('.zst') else DEST.replace('.tar.xz','.tar.zst')
        tmp = zst_dest + '.tmp'
        import subprocess
        p1 = subprocess.Popen(['xz','-dc',cache], stdout=subprocess.PIPE)
        p2 = subprocess.Popen(['zstd','-19','-T0','-o',tmp], stdin=p1.stdout)
        if p1.stdout: p1.stdout.close()
        p2.wait(); p1.wait()
        if p1.returncode!=0 or p2.returncode!=0: raise SystemExit('repack failed')
        os.replace(tmp, zst_dest)
        shutil.copy2(zst_dest, DEST if DEST.endswith('.zst') else zst_dest)
        print('ok', zst_dest)
        return
    shutil.copy2(cache, DEST)
    print('ok', DEST)

if __name__ == "__main__":
    main()
