#!/usr/bin/env python3
"""生成启动图标：`>` 提示符 + 右下光标方块。

- thin：光标空心
- bundle：光标实心
- debug：右上角一颗小状态点
SVG 源在 docs/icons/，PNG 写进 app/src/<variant>/res/mipmap-*/。
需要 resvg：RESVG=/path/to/resvg（或加入 PATH）。
"""
import os
import shutil
import subprocess

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RESVG = os.environ.get("RESVG") or shutil.which("resvg")
ICONS = os.path.join(ROOT, "docs", "icons")

DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}

DEFS = """
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#122A3B"/><stop offset="1" stop-color="#08121B"/>
    </linearGradient>
    <linearGradient id="body" x1="0" y1="0" x2="0.4" y2="1">
      <stop offset="0" stop-color="#FFE391"/><stop offset="1" stop-color="#F3B33A"/>
    </linearGradient>
    <linearGradient id="accent" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#7CF7E3"/><stop offset="1" stop-color="#23A392"/>
    </linearGradient>
  </defs>
"""

BODY = """
  <rect width="108" height="108" fill="url(#bg)"/>

  <path d="M34 32L60 54L34 76" fill="none" stroke="#F1F6FF" stroke-width="7"
        stroke-linecap="round" stroke-linejoin="round"/>
"""

CURSOR_HOLLOW = """
  <rect x="70" y="60" width="16" height="14" rx="4.4" fill="none"
        stroke="url(#accent)" stroke-width="3.4"/>
"""

CURSOR_SOLID = """
  <rect x="70" y="60" width="16" height="14" rx="4.4" fill="url(#accent)"/>
"""

DEBUG_DOT = """
  <circle cx="84" cy="40" r="2.4" fill="#7DD3FC"/>
  <circle cx="84" cy="40" r="4" fill="#7DD3FC" opacity="0.2"/>
"""


def svg(bundle: bool, debug: bool) -> str:
    body = ('<svg xmlns="http://www.w3.org/2000/svg" width="108" height="108" '
            'viewBox="0 0 108 108">\n') + DEFS + BODY
    body += CURSOR_SOLID if bundle else CURSOR_HOLLOW
    if debug:
        body += DEBUG_DOT
    body += "</svg>\n"
    return body


def render(svg_path, png_path, px):
    os.makedirs(os.path.dirname(png_path), exist_ok=True)
    subprocess.run([RESVG, svg_path, png_path, "--width", str(px)], check=True)
    print("wrote", png_path)


def main():
    if not RESVG:
        raise SystemExit("resvg not found: set RESVG=/path/to/resvg")
    os.makedirs(ICONS, exist_ok=True)
    variants = {
        "thin": (False, False),
        "bundle": (True, False),
        "thinDebug": (False, True),
        "bundleDebug": (True, True),
    }
    for name, (bundle, debug) in variants.items():
        src = os.path.join(ICONS, f"{name}.svg")
        with open(src, "w", encoding="utf-8", newline="\n") as f:
            f.write(svg(bundle, debug))
        res = os.path.join(ROOT, "app", "src", name, "res")
        for bucket, scale in DENSITIES.items():
            render(src, os.path.join(res, f"mipmap-{bucket}", "ic_launcher_foreground.png"),
                   round(108 * scale))
            render(src, os.path.join(res, f"mipmap-{bucket}", "ic_launcher.png"),
                   round(48 * scale))
            render(src, os.path.join(res, f"mipmap-{bucket}", "ic_launcher_round.png"),
                   round(48 * scale))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
