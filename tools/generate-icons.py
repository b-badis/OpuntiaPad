#!/usr/bin/env python3
"""
OpuntiaPad icon generator.

Draws the logo once (a prickly-pear pad surrounded by file-type badges: .txt, JSON,
YAML/XML tree, SQL database, plus the fruit) and writes every derived asset:

  docs/images/icon.svg                                  master artwork (README, store graphics)
  app/src/main/res/drawable/ic_launcher_background.xml  adaptive icon background
  app/src/main/res/drawable/ic_launcher_foreground.xml  adaptive icon foreground (safe-zone scaled)
  app/src/main/res/drawable/ic_launcher_monochrome.xml  themed-icon silhouette (Android 13+)
  app/src/main/res/drawable/ic_logo.xml                 in-app logo (no background)

Labels are converted to outlines, so no font is needed at runtime.

Requirements: python3 -m pip install fonttools shapely   (+ the Inter font, OFL, for the labels)
Usage:        python3 tools/generate-icons.py [path/to/Inter-Bold.otf]
"""
import math
import os
import sys

from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont
from shapely import affinity
from shapely.geometry import Point, Polygon
from shapely.ops import unary_union

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FONT = sys.argv[1] if len(sys.argv) > 1 else "/usr/share/fonts/opentype/inter/Inter-Bold.otf"

# --------------------------------------------------------------------------- primitives
# Shapes: dict(d=path, fill=paint, stroke=(color, width)|None, alpha=1.0)
# Paints: ("solid", "#RRGGBB") | ("lin", x1, y1, x2, y2, c1, c2) | ("rad", cx, cy, r, c1, c2)
# Groups: dict(group=[...], rotate=(deg, px, py))


def ellipse_d(cx, cy, rx, ry):
    return f"M{cx - rx:g},{cy:g} a{rx:g},{ry:g} 0 1,0 {2 * rx:g},0 a{rx:g},{ry:g} 0 1,0 {-2 * rx:g},0 Z"


def rrect_d(x, y, w, h, r):
    return (f"M{x + r:g},{y:g} H{x + w - r:g} a{r:g},{r:g} 0 0 1 {r:g},{r:g} V{y + h - r:g} "
            f"a{r:g},{r:g} 0 0 1 {-r:g},{r:g} H{x + r:g} a{r:g},{r:g} 0 0 1 {-r:g},{-r:g} V{y + r:g} "
            f"a{r:g},{r:g} 0 0 1 {r:g},{-r:g} Z")


def doc_d(x, y, w, h, fold=6, r=2):
    """Document with a folded top-right corner."""
    return (f"M{x + r:g},{y:g} H{x + w - fold:g} L{x + w:g},{y + fold:g} V{y + h - r:g} "
            f"a{r:g},{r:g} 0 0 1 {-r:g},{r:g} H{x + r:g} a{r:g},{r:g} 0 0 1 {-r:g},{-r:g} V{y + r:g} "
            f"a{r:g},{r:g} 0 0 1 {r:g},{-r:g} Z")


def fold_d(x, y, w, fold=6, r=1.5):
    return f"M{x + w - fold:g},{y:g} V{y + fold - r:g} a{r:g},{r:g} 0 0 0 {r:g},{r:g} H{x + w:g} Z"


_font = TTFont(FONT)
_glyphs = _font.getGlyphSet()
_cmap = _font.getBestCmap()
_upm = _font["head"].unitsPerEm


def text_d(text, size, x=None, baseline=0.0, center=None):
    """Outline of `text` (Inter Bold) at `size` px; left at x or centred on `center`."""
    scale = size / _upm
    names = [_cmap[ord(c)] for c in text]
    width = sum(_glyphs[n].width for n in names) * scale
    cursor = (center - width / 2) if center is not None else x
    parts = []
    for n in names:
        pen = SVGPathPen(_glyphs, ntos=lambda v: f"{v:.2f}")
        _glyphs[n].draw(TransformPen(pen, (scale, 0, 0, -scale, cursor, baseline)))
        parts.append(pen.getCommands())
        cursor += _glyphs[n].width * scale
    return " ".join(p for p in parts if p)


def tri_d(px, py, angle, length, base):
    """Small spine: triangle with its base centred on (px,py), pointing along `angle`."""
    ux, uy = math.cos(angle), math.sin(angle)
    nx, ny = -uy, ux
    tip = (px + ux * length, py + uy * length)
    a = (px + nx * base / 2, py + ny * base / 2)
    b = (px - nx * base / 2, py - ny * base / 2)
    return f"M{a[0]:.2f},{a[1]:.2f} L{tip[0]:.2f},{tip[1]:.2f} L{b[0]:.2f},{b[1]:.2f} Z"


# --------------------------------------------------------------------------- the design
PAD_D = "M54,25 C67,25 75,40 75,55 C75,73 66,88 54,88 C42,88 33,73 33,55 C33,40 41,25 54,25 Z"
PAD_CENTER, PAD_RX, PAD_RY = (54, 57), 21, 31.5


def pad_point(t):
    return PAD_CENTER[0] + PAD_RX * math.cos(t), PAD_CENTER[1] + PAD_RY * math.sin(t)


def design():
    s = []
    # Cactus pad.
    s.append(dict(d=PAD_D, fill=("lin", 40, 28, 70, 88, "#8FD47E", "#2E7D45"), stroke=("#1E5631", 1.4)))
    s.append(dict(d=ellipse_d(46, 45, 5, 10), fill=("solid", "#FFFFFF"), alpha=0.18))
    # Spines on the rim (pointing outwards) and on the surface (pointing up).
    for deg in (200, 225, 250, 290, 315, 340, 160, 135, 20, 45, 110, 70):
        t = math.radians(deg)
        x, y = pad_point(t)
        s.append(dict(d=tri_d(x, y, t, 3.2, 1.8), fill=("solid", "#2F6B3A")))
    for x, y in ((47, 38), (60, 36), (44, 55), (56, 50), (64, 60), (50, 68), (60, 76), (45, 78), (54, 60)):
        s.append(dict(d=tri_d(x, y + 1.2, -math.pi / 2, 3.0, 1.6), fill=("solid", "#DDF5C4")))

    # Prickly pear fruit (top right), slightly tilted.
    fruit = [dict(d=ellipse_d(71, 28, 9.5, 7.6), fill=("rad", 67.5, 24.5, 12, "#FFB24C", "#C0392B"),
                  stroke=("#7E2414", 0.9))]
    for x, y in ((66, 27), (70, 31), (74, 26), (68, 23.5), (75, 31), (63.5, 30)):
        fruit.append(dict(d=ellipse_d(x, y, 0.75, 0.75), fill=("solid", "#7E2414")))
    fruit.append(dict(d=ellipse_d(79.2, 27.5, 1.9, 2.4), fill=("solid", "#8E2A1A")))
    for deg in (100, 140, 200, 250, 300, 60):
        t = math.radians(deg)
        x, y = 71 + 9.5 * math.cos(t), 28 + 7.6 * math.sin(t)
        fruit.append(dict(d=tri_d(x, y, t, 2.2, 1.1), fill=("solid", "#7E2414")))
    s.append(dict(group=fruit, rotate=(-25, 71, 28)))

    # .txt document (top left).
    s.append(dict(d=doc_d(20, 21, 21, 25), fill=("lin", 20, 21, 41, 46, "#5BCB78", "#2B8A3E")))
    s.append(dict(d=fold_d(20, 21, 21), fill=("solid", "#A6E9B8")))
    s.append(dict(d=text_d(".txt", 7.2, center=30.5, baseline=40.5), fill=("solid", "#FFFFFF")))

    # {.json} tile (right).
    s.append(dict(d=rrect_d(62, 41, 27, 15.5, 3.5), fill=("lin", 62, 41, 89, 56.5, "#5AA8FF", "#1F5FD1")))
    s.append(dict(d=text_d("{.json}", 5.8, center=75.5, baseline=51.2), fill=("solid", "#FFFFFF")))

    # YAML / XML document with a tree (bottom left).
    s.append(dict(d=doc_d(19, 62, 21, 25), fill=("lin", 19, 62, 40, 87, "#FFDD6E", "#E2A72E")))
    s.append(dict(d=fold_d(19, 62, 21), fill=("solid", "#FFF0B8")))
    ink = "#8A5A0F"
    for x in (22.5, 26.5, 30.5):
        s.append(dict(d=rrect_d(x, 66.5, 2.6, 1.2, 0.6), fill=("solid", ink)))
    s.append(dict(d=rrect_d(27, 70.5, 5, 3.4, 0.7), fill=("solid", ink)))
    s.append(dict(d="M29.5,73.9 V76.2 M23.4,76.2 H35.6 M23.4,76.2 V78.2 M29.5,76.2 V78.2 M35.6,76.2 V78.2",
                  fill=None, stroke=(ink, 0.9)))
    for x in (21.4, 27.5, 33.6):
        s.append(dict(d=rrect_d(x, 78.2, 4, 3, 0.6), fill=("solid", ink)))

    # SQL database (bottom right).
    s.append(dict(d="M65,67.5 V82.5 a11,3.8 0 0 0 22,0 V67.5 Z", fill=("lin", 65, 64, 87, 86, "#B388FF", "#6A3FC6")))
    s.append(dict(d=ellipse_d(76, 67.5, 11, 3.8), fill=("solid", "#D9C6FF")))
    for y in (73.5, 77.5, 81.5):
        s.append(dict(d=f"M67.6,{y} a11,3.8 0 0 0 16.8,0", fill=None, stroke=("#EBDFFF", 1.0)))
    return s


BACKGROUND = ("lin", 0, 0, 108, 108, "#E9F8F5", "#A9D8E0")

# --------------------------------------------------------------------------- SVG output


def svg_paint(paint, defs, prefix):
    if paint is None:
        return 'fill="none"'
    if paint[0] == "solid":
        return f'fill="{paint[1]}"'
    gid = f"{prefix}{len(defs)}"
    if paint[0] == "lin":
        _, x1, y1, x2, y2, c1, c2 = paint
        defs.append(f'<linearGradient id="{gid}" gradientUnits="userSpaceOnUse" x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}">'
                    f'<stop offset="0" stop-color="{c1}"/><stop offset="1" stop-color="{c2}"/></linearGradient>')
    else:
        _, cx, cy, r, c1, c2 = paint
        defs.append(f'<radialGradient id="{gid}" gradientUnits="userSpaceOnUse" cx="{cx}" cy="{cy}" r="{r}">'
                    f'<stop offset="0" stop-color="{c1}"/><stop offset="1" stop-color="{c2}"/></radialGradient>')
    return f'fill="url(#{gid})"'


def svg_items(items, defs, prefix="g"):
    out = []
    for it in items:
        if "group" in it:
            deg, px, py = it["rotate"]
            out.append(f'<g transform="rotate({deg} {px} {py})">{svg_items(it["group"], defs, prefix)}</g>')
            continue
        attrs = [f'd="{it["d"]}"', svg_paint(it.get("fill"), defs, prefix)]
        if it.get("alpha", 1) != 1:
            attrs.append(f'fill-opacity="{it["alpha"]}"')
        if it.get("stroke"):
            c, w = it["stroke"]
            attrs.append(f'stroke="{c}" stroke-width="{w}" stroke-linecap="round" stroke-linejoin="round"')
        out.append(f'<path {" ".join(attrs)}/>')
    return "".join(out)


def write_svg(path):
    defs = []
    bg = svg_paint(BACKGROUND, defs, "g")
    body = svg_items(design(), defs)
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="108" height="108">\n'
           f'  <!-- OpuntiaPad logo — generated by tools/generate-icons.py, do not edit by hand. -->\n'
           f'  <defs>{"".join(defs)}</defs>\n  <rect width="108" height="108" rx="24" {bg}/>\n  {body}\n</svg>\n')
    open(path, "w").write(svg)


# --------------------------------------------------------------------------- VectorDrawable output


def argb(color, alpha=1.0):
    return f"#{round(alpha * 255):02X}{color.lstrip('#').upper()}"


def vd_items(items, indent="        "):
    out = []
    for it in items:
        if "group" in it:
            deg, px, py = it["rotate"]
            out.append(f'{indent}<group android:rotation="{deg}" android:pivotX="{px}" android:pivotY="{py}">\n'
                       f'{vd_items(it["group"], indent + "    ")}\n{indent}</group>')
            continue
        attrs = [f'android:pathData="{it["d"]}"']
        inner = ""
        paint = it.get("fill")
        if paint and paint[0] == "solid":
            attrs.append(f'android:fillColor="{argb(paint[1], it.get("alpha", 1))}"')
        elif paint and paint[0] == "lin":
            _, x1, y1, x2, y2, c1, c2 = paint
            inner = (f'<aapt:attr name="android:fillColor"><gradient android:type="linear" android:startX="{x1}" '
                     f'android:startY="{y1}" android:endX="{x2}" android:endY="{y2}" android:startColor="{argb(c1)}" '
                     f'android:endColor="{argb(c2)}"/></aapt:attr>')
        elif paint and paint[0] == "rad":
            _, cx, cy, r, c1, c2 = paint
            inner = (f'<aapt:attr name="android:fillColor"><gradient android:type="radial" android:centerX="{cx}" '
                     f'android:centerY="{cy}" android:gradientRadius="{r}" android:startColor="{argb(c1)}" '
                     f'android:endColor="{argb(c2)}"/></aapt:attr>')
        if it.get("stroke"):
            c, w = it["stroke"]
            attrs += [f'android:strokeColor="{argb(c)}"', f'android:strokeWidth="{w}"',
                      'android:strokeLineCap="round"', 'android:strokeLineJoin="round"']
        a = " ".join(attrs)
        out.append(f"{indent}<path {a}>{inner}</path>" if inner else f"{indent}<path {a}/>")
    return "\n".join(out)


def vector(body, comment, size=108, viewport=108):
    return (f'<?xml version="1.0" encoding="utf-8"?>\n<!-- {comment} Generated by tools/generate-icons.py. -->\n'
            f'<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    xmlns:aapt="http://schemas.android.com/aapt"\n'
            f'    android:width="{size}dp" android:height="{size}dp"\n'
            f'    android:viewportWidth="{viewport}" android:viewportHeight="{viewport}">\n{body}\n</vector>\n')


# The launcher only shows a circle of diameter 66 inside the 108 canvas: shrink the artwork into it.
SAFE_SCALE = 0.70


def safe_group(body):
    return (f'    <group android:scaleX="{SAFE_SCALE}" android:scaleY="{SAFE_SCALE}" android:pivotX="54" android:pivotY="54.5">\n'
            f'{body}\n    </group>')


def monochrome_d():
    """Single-colour silhouette for themed icons: the pad and its fruit (badges would turn into noise)."""
    def ell(cx, cy, rx, ry, rot=0):
        e = affinity.scale(Point(cx, cy).buffer(1, resolution=64), rx, ry)
        return affinity.rotate(e, rot, origin=(cx, cy)) if rot else e

    # Centred version of the pad + fruit, a bit larger since it stands alone.
    pad = ell(50, 60, 22, 30)
    fruit = ell(70, 28, 10.5, 8.4, -25)
    spines = unary_union([
        Polygon([(x - 1.1, y), (x + 1.1, y), (x, y - 3.4)]) for x, y in ((43, 45), (57, 42), (40, 62), (52, 55), (61, 66), (47, 74), (57, 80))
    ])
    shape = unary_union([pad.difference(fruit.buffer(1.8)).difference(spines), fruit])

    def ring(c):
        pts = list(c.coords)[:-1]
        return "M" + " L".join(f"{x:.2f},{y:.2f}" for x, y in pts) + " Z"

    polys = getattr(shape, "geoms", [shape])
    return " ".join(ring(p.exterior) + "".join(" " + ring(i) for i in p.interiors) for p in polys)


def main():
    res = os.path.join(ROOT, "app/src/main/res/drawable")
    write_svg(os.path.join(ROOT, "docs/images/icon.svg"))
    items = design()
    bg = vd_items([dict(d="M0,0h108v108h-108z", fill=BACKGROUND)], "    ")
    open(os.path.join(res, "ic_launcher_background.xml"), "w").write(vector(bg, "Adaptive icon background."))
    open(os.path.join(res, "ic_launcher_foreground.xml"), "w").write(
        vector(safe_group(vd_items(items)), "OpuntiaPad logo: prickly-pear pad with .txt / JSON / YAML / SQL badges."))
    mono = f'        <path android:fillColor="#FFFFFFFF" android:fillType="evenOdd" android:pathData="{monochrome_d()}"/>'
    open(os.path.join(res, "ic_launcher_monochrome.xml"), "w").write(
        vector(safe_group(mono), "Themed-icon silhouette (Android 13+)."))
    logo = f'    <group android:translateX="-16" android:translateY="-17">\n{vd_items(items)}\n    </group>'
    open(os.path.join(res, "ic_logo.xml"), "w").write(vector(logo, "In-app logo (no background).", 72, 76))
    print("icons generated")


if __name__ == "__main__":
    main()
