#!/usr/bin/env python3
"""Generate PneumaticCraft-like 16x16 textures, models and blockstates for Working Medium.

Casings use an in-mod connected-cube model and a 64x64 compact CTM sheet.
Mixed grades do not connect; only identical casing blocks do.

Textures are original pixel art in a PNC-adjacent pressure-chamber metal language:
low-contrast mottled plates, shallow recesses, cool-grey frames and circular
valve ports. Each grade and each ingot/alloy has its own readable metal color;
they are not flattened to one iron grey. Official PNC assets are not copied.
"""
from __future__ import annotations

import json
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets/workingmedium"
BLOCK_TEX = ASSETS / "textures/block"
ITEM_TEX = ASSETS / "textures/item"
GUI_TEX = ASSETS / "textures/gui"
WIDGET_TEX = ASSETS / "textures/progwidgets"
BLOCKSTATES = ASSETS / "blockstates"
BLOCK_MODELS = ASSETS / "models/block"
ITEM_MODELS = ASSETS / "models/item"
MOD = "workingmedium"

def _metal_pal(name, *, accent, base, mid, hi, lo, fill=None, rust=None, sheen=None,
               edge=None, rivet=None, rivet_hi=None):
    """Per-grade metal finish. Construction language is shared; hue and value are not."""
    return dict(
        name=name,
        base=base, mid=mid, hi=hi, lo=lo,
        rivet=rivet or (89, 89, 89), rivet_hi=rivet_hi or (104, 104, 104),
        edge=edge or lo, accent=accent, fill=fill or mid,
        rust=rust or lo, sheen=sheen or hi,
    )


GRADES = {
    # G0 stays closest to the approved pressure-chamber iron wall.
    "g0": _metal_pal("iron", accent=(86, 70, 58),
                     base=(71, 66, 65), mid=(77, 72, 70), hi=(84, 79, 75), lo=(54, 49, 48),
                     fill=(66, 61, 59), rust=(58, 53, 52), sheen=(102, 98, 94), edge=(43, 39, 39)),
    # G1 bronze / copper banding — clearly warmer than iron, still muted metal.
    "g1": _metal_pal("bronze", accent=(138, 88, 52),
                     base=(92, 64, 46), mid=(110, 78, 54), hi=(128, 94, 64), lo=(64, 42, 32),
                     fill=(96, 66, 48), rust=(84, 50, 34), sheen=(156, 118, 80),
                     edge=(52, 34, 26), rivet=(102, 74, 50), rivet_hi=(130, 96, 66)),
    # G2 blued steel — cool, readable against iron and bronze.
    "g2": _metal_pal("steel", accent=(88, 106, 116),
                     base=(60, 68, 76), mid=(74, 84, 92), hi=(94, 104, 112), lo=(42, 48, 56),
                     fill=(58, 66, 74), rust=(50, 54, 60), sheen=(132, 140, 148),
                     edge=(30, 36, 42), rivet=(80, 88, 96), rivet_hi=(112, 122, 130)),
    # G3 ceramic-coated / tan alloy — sandy, distinct from bronze (less red, more clay).
    "g3": _metal_pal("ceramic", accent=(140, 114, 78),
                     base=(94, 80, 60), mid=(112, 94, 70), hi=(130, 110, 84), lo=(64, 52, 40),
                     fill=(98, 84, 64), rust=(80, 62, 44), sheen=(154, 136, 104),
                     edge=(50, 40, 30), rivet=(104, 88, 66), rivet_hi=(132, 114, 86)),
    # G4 cryo — frost blue-grey, cooler than steel, not saturated teal.
    "g4": _metal_pal("cryo", accent=(96, 128, 134),
                     base=(52, 68, 74), mid=(66, 86, 92), hi=(90, 112, 118), lo=(36, 50, 56),
                     fill=(54, 70, 76), rust=(44, 58, 62), sheen=(148, 170, 174),
                     edge=(26, 38, 44), rivet=(76, 96, 102), rivet_hi=(124, 144, 150)),
    # G5 critical — pale polished silver, the lightest finish.
    "g5": _metal_pal("critical", accent=(154, 152, 150),
                     base=(100, 98, 96), mid=(118, 116, 114), hi=(142, 140, 138), lo=(72, 70, 68),
                     fill=(104, 102, 100), rust=(80, 78, 76), sheen=(180, 180, 178),
                     edge=(54, 52, 52), rivet=(124, 122, 120), rivet_hi=(158, 158, 156)),
}

CONTROLLERS = {
    "impact_crusher": ("g0", "chamber"),
    "gas_bank": ("g0", "storage"),
    "residue_kiln": ("g1", "kettle"),
    "lube_mixer": ("g1", "kettle"),
    "flare_stack": ("g1", "flare"),
    "washer_tower": ("g2", "tower"),
    "thermolysis_kettle": ("g2", "kettle"),
    "compressor_column": ("g2", "column"),
    "regulator_skid": ("g2", "regulator"),
    "cyclone": ("g2", "chamber"),
    "item_gallery": ("g2", "manifold"),
    "fraction_tower": ("g3", "tower"),
    "cracker": ("g3", "gallery"),
    "electrolyzer": ("g3", "kettle"),
    "polymerizer": ("g3", "kettle"),
    "leach_line": ("g3", "gallery"),
    "alloy_kettle": ("g3", "kettle"),
    "sinter_kiln": ("g3", "kettle"),
    "quench_well": ("g3", "quench"),
    "heat_gallery": ("g3", "heat"),
    "scrubber_tower": ("g4", "tower"),
    "expander_column": ("g4", "column"),
    "cryo_tower": ("g4", "tower"),
    "lamination_gallery": ("g4", "gallery"),
    "clean_room": ("g4", "room"),
    "cycle_island": ("g4", "cycle"),
    "charging_station": ("g4", "charge"),
    "drone_nest": ("g4", "charge"),
    "critical_kettle": ("g5", "kettle"),
    "calibration_bench": ("g5", "calibration"),
    "fluid_loop": ("g5", "loop"),
}

HATCHES = {
    "item_in": (72, 78, 70),
    "item_out": (86, 70, 58),
    "fluid": (70, 74, 80),
    "gas": (78, 84, 86),
    "heat": (92, 48, 36),
    "vent": (90, 88, 86),
    "drain": (70, 58, 52),
    "gauge": (104, 104, 104),
    "manifold": (90, 88, 86),
}

PARTS = ["tray", "coil", "stage", "intercooler", "bay", "body", "cap", "airlock"]

# Names kept for leftover item helpers; values are iron-adjacent, not saturated teal/cream.
TEAL = (70, 74, 76)
TEAL_DK = (54, 49, 48)
TEAL_HI = (104, 104, 104)
OUT = (14, 12, 10, 255)
WHITE = (236, 236, 230, 255)
CREAM = (82, 75, 73)
NEEDLE = (64, 32, 26)
RUBBER = (42, 36, 34)


def rgba(c, a=255):
    if c is None:
        return (0, 0, 0, 0)
    if len(c) == 4:
        return (int(c[0]), int(c[1]), int(c[2]), int(c[3]))
    return (int(c[0]), int(c[1]), int(c[2]), a)


def clamp(v):
    return 0 if v < 0 else 255 if v > 255 else int(v)


def add(c, d):
    return (clamp(c[0] + d), clamp(c[1] + d), clamp(c[2] + d))


def mix(a, b, t):
    t = 0 if t < 0 else 1 if t > 1 else t
    return (clamp(a[0] + (b[0] - a[0]) * t), clamp(a[1] + (b[1] - a[1]) * t), clamp(a[2] + (b[2] - a[2]) * t))


def new(size=16, color=(0, 0, 0, 0)):
    return Image.new("RGBA", (size, size) if isinstance(size, int) else size, color)


def pset(im, x, y, c, a=255):
    if c is None:
        return
    if 0 <= x < im.width and 0 <= y < im.height:
        im.putpixel((x, y), rgba(c, a))


def fill(im, x0, y0, x1, y1, c, a=255):
    if x0 > x1:
        x0, x1 = x1, x0
    if y0 > y1:
        y0, y1 = y1, y0
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            pset(im, x, y, c, a)


def rect(im, x0, y0, x1, y1, c, a=255):
    if x0 > x1:
        x0, x1 = x1, x0
    if y0 > y1:
        y0, y1 = y1, y0
    for x in range(x0, x1 + 1):
        pset(im, x, y0, c, a)
        pset(im, x, y1, c, a)
    for y in range(y0, y1 + 1):
        pset(im, x0, y, c, a)
        pset(im, x1, y, c, a)


def hash2(x, y, seed=0):
    n = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return (n ^ (n >> 16)) & 0xFF


def metal_at(pal, x, y, seed=0, light=0):
    h = hash2(x, y, seed)
    v = light
    # faint brushed grain; contrast stays inside the compressed-iron range
    v += ((hash2(x, 0, seed + 1) % 5) - 2)
    if (y + seed) % 8 == 3 and h > 200:
        v += 2
    if h < 10:
        v -= 6
    elif h < 22:
        v -= 3
    elif h > 250:
        v += 6
    elif h > 230:
        v += 3
    c = add(pal["base"], v)
    rust = pal.get("rust")
    if rust is not None and 36 <= h <= 44 and ((x * 3 + y + seed) % 7 == 0):
        c = mix(c, rust, 0.15)
    return c


def fill_metal(im, x0, y0, x1, y1, pal, seed=0, bevel=True, recessed=False, light0=0):
    if x0 > x1:
        x0, x1 = x1, x0
    if y0 > y1:
        y0, y1 = y1, y0
    w = max(1, x1 - x0)
    h = max(1, y1 - y0)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            light = light0
            if bevel or recessed:
                nx = (x - x0) / w
                ny = (y - y0) / h
                if recessed:
                    light += int(-3 + 4 * nx + 3 * ny)
                    if y == y0 or x == x0:
                        light -= 6
                    if y == y1 or x == x1:
                        light += 4
                    if y == y0 + 1 and x0 < x < x1:
                        light -= 3
                    if x == x0 + 1 and y0 < y < y1:
                        light -= 2
                else:
                    light += int(4 * (1 - nx) + 3 * (1 - ny) - 3)
                    if y == y0 or x == x0:
                        light += 6
                    if y == y1 or x == x1:
                        light -= 6
                    if y == y0 + 1 and x0 < x < x1:
                        light += 3
                    if x == x0 + 1 and y0 < y < y1:
                        light += 2
                    if y == y1 - 1 and x0 < x < x1:
                        light -= 3
                    if x == x1 - 1 and y0 < y < y1:
                        light -= 2
            pset(im, x, y, metal_at(pal, x, y, seed, light))


def rivet(im, x, y, pal):
    # quiet cool-grey stud; pal kept for call-site compatibility
    pset(im, x, y, (104, 104, 104))
    pset(im, x + 1, y, (89, 89, 89))
    pset(im, x, y + 1, (89, 89, 89))
    pset(im, x + 1, y + 1, (66, 61, 59))


def stud(im, x, y, pal):
    pset(im, x, y, (104, 104, 104))
    pset(im, x + 1, y, (89, 89, 89))


def disc(im, cx, cy, r, color, r_min=0):
    r2 = r * r
    rmin2 = r_min * r_min
    for y in range(int(cy - r) - 1, int(cy + r) + 2):
        for x in range(int(cx - r) - 1, int(cx + r) + 2):
            d2 = (x - cx) ** 2 + (y - cy) ** 2
            if rmin2 <= d2 <= r2:
                pset(im, x, y, color)


def disc_shaded(im, cx, cy, r, base, hi, lo, r_min=0):
    r2 = r * r + 0.35
    rmin2 = r_min * r_min
    for y in range(int(cy - r) - 1, int(cy + r) + 2):
        for x in range(int(cx - r) - 1, int(cx + r) + 2):
            dx, dy = x - cx, y - cy
            d2 = dx * dx + dy * dy
            if d2 <= r2 and d2 >= rmin2:
                nd = (d2 ** 0.5) / max(r, 0.01)
                lx = (dx + dy) / (r * 1.6 + 0.01)
                t = 0.5 - lx * 0.5 - nd * 0.15
                pset(im, x, y, mix(lo, hi, t) if t < 0.55 else mix(base, hi, (t - 0.35)))


def glass_rect(im, x0, y0, x1, y1, color=TEAL, frame=None):
    dk = mix(color, (8, 16, 18), 0.55)
    hi = mix(color, (230, 255, 245), 0.45)
    mid = color
    fill(im, x0, y0, x1, y1, dk)
    fill(im, x0 + 1, y0 + 1, x1 - 1, y1 - 1, mid)
    if x1 - x0 >= 3 and y1 - y0 >= 3:
        pset(im, x0 + 1, y0 + 1, hi)
        if x1 - x0 >= 4:
            pset(im, x0 + 2, y0 + 1, hi)
        if y1 - y0 >= 4:
            pset(im, x0 + 1, y0 + 2, mix(mid, hi, 0.5))
        pset(im, x1 - 1, y1 - 1, mix(dk, (0, 0, 0), 0.2))
    if frame is not None:
        rect(im, x0, y0, x1, y1, frame)


def gauge(im, cx, cy, r, pal, needle="ne", face=CREAM):
    disc(im, cx, cy, r, pal["edge"])
    disc(im, cx, cy, r - 0.6, pal["lo"])
    disc_shaded(im, cx, cy, r - 1.1, face, add(face, 20), mix(face, (80, 80, 70), 0.25))
    if r >= 3:
        for dx, dy in ((0, -1), (1, 0), (0, 1), (-1, 0)):
            pset(im, round(cx + dx * (r - 2)), round(cy + dy * (r - 2)), (40, 38, 32))
    pset(im, round(cx), round(cy), (30, 28, 24))
    dirs = {
        "ne": [(1, -1), (2, -2)],
        "n": [(0, -1), (0, -2)],
        "e": [(1, 0), (2, 0)],
        "se": [(1, 1), (2, 2)],
        "nw": [(-1, -1), (-2, -2)],
    }
    steps = dirs.get(needle, dirs["ne"])
    for i, (dx, dy) in enumerate(steps):
        pset(im, round(cx + dx), round(cy + dy), NEEDLE if i == len(steps) - 1 else (30, 28, 24))
    pset(im, round(cx - r + 2), round(cy - r + 2), (255, 255, 255, 180))


def flange_port(im, cx, cy, pal, hole, bolts=True, r=3):
    disc(im, cx, cy, r + 1, pal["edge"])
    disc(im, cx, cy, r, pal["mid"])
    disc(im, cx, cy, r - 1.2, pal["lo"])
    disc(im, cx, cy, max(1, r - 2), hole)
    if r >= 3:
        pset(im, cx - 1, cy - 1, mix(hole, WHITE, 0.35))
    if bolts and r >= 3:
        for dx, dy in ((-r, 0), (r, 0), (0, -r), (0, r)):
            pset(im, cx + dx, cy + dy, pal["rivet_hi"])


def vents(im, x0, y0, x1, y1, pal, horizontal=True):
    fill_metal(im, x0, y0, x1, y1, pal, recessed=True, seed=3)
    if horizontal:
        for y in range(y0 + 1, y1, 2):
            fill(im, x0 + 1, y, x1 - 1, y, pal["edge"])
            if y + 1 <= y1:
                for x in range(x0 + 1, x1):
                    pset(im, x, y + 1, pal["hi"] if x == x0 + 1 else pal["mid"])
    else:
        for x in range(x0 + 1, x1, 2):
            fill(im, x, y0 + 1, x, y1 - 1, pal["edge"])


def pipe_h(im, x0, y, x1, pal, core=None, thick=3):
    cols = [pal["hi"], pal["mid"], pal["lo"]]
    if thick == 4:
        cols = [pal["hi"], pal["mid"], pal.get("accent", pal["mid"]) if core is None else core, pal["lo"]]
    elif thick == 2:
        cols = [pal["hi"], pal["lo"]]
    for i, c in enumerate(cols):
        fill(im, x0, y + i, x1, y + i, c)
    for x in range(x0 + 1, x1, 4):
        fill(im, x, y, x, y + thick - 1, pal["edge"])
    if core is not None and thick == 3:
        fill(im, x0, y + 1, x1, y + 1, core)


def pipe_v(im, x, y0, y1, pal, core=None, thick=3):
    cols = [pal["hi"], pal["mid"], pal["lo"]]
    if thick == 4:
        cols = [pal["hi"], pal["mid"], pal.get("accent", pal["mid"]) if core is None else core, pal["lo"]]
    elif thick == 2:
        cols = [pal["hi"], pal["lo"]]
    for i, c in enumerate(cols):
        fill(im, x + i, y0, x + i, y1, c)
    for y in range(y0 + 1, y1, 4):
        fill(im, x, y, x + thick - 1, y, pal["edge"])
    if core is not None and thick == 3:
        fill(im, x + 1, y0, x + 1, y1, core)


def chevron(im, cx, cy, color, direction="in"):
    if direction in ("in", "down"):
        pts = [(0, 2), (-1, 1), (1, 1), (-2, 0), (2, 0), (0, 1)]
    elif direction == "up":
        pts = [(0, 0), (-1, 1), (1, 1), (-2, 2), (2, 2), (0, 1)]
    else:
        pts = [(-2, 0), (2, 0), (-1, 1), (1, 1), (0, 2)]
    for dx, dy in pts:
        pset(im, cx + dx, cy + dy, color)


def save(im, path: Path):
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)


def _grade_seed(pal):
    return {"iron": 11, "bronze": 23, "steel": 37, "ceramic": 47, "cryo": 59, "critical": 67}.get(pal.get("name", ""), 11)


# --- Pressure-chamber iron language (original pixels; not official PNC copies) ---
D0, D1, D2, D3 = (31, 27, 27), (43, 39, 39), (54, 49, 48), (58, 53, 52)
M0, M1, M2, M3 = (66, 61, 59), (71, 66, 65), (77, 72, 70), (82, 75, 73)
G0, G1, G2, G3 = (89, 89, 89), (104, 104, 104), (116, 116, 116), (127, 127, 127)
H0, H1, H2 = (84, 79, 75), (102, 98, 94), (136, 136, 136)
WARM, COOL, CER, FROST, PALE = (138, 88, 52), (88, 106, 116), (140, 114, 78), (96, 128, 134), (154, 152, 150)
EMBER, EMBER2 = (92, 48, 36), (64, 32, 26)
RAMP = [D0, D1, D2, D3, M0, M1, M2, M3]


def nrand(x, y, seed=0):
    n = (x * 19 + y * 37 + seed * 91 + 11) & 0xFFFFFFFF
    n = (n * 1103515245 + 12345) & 0x7FFFFFFF
    return n


def grain_at(x, y, seed, lo=D2, hi=M2):
    try:
        i0 = RAMP.index(lo)
        i1 = RAMP.index(hi)
    except ValueError:
        return lo if (nrand(x, y, seed) & 1) else hi
    if i1 < i0:
        i0, i1 = i1, i0
    span = i1 - i0 + 1
    k = nrand(x, y, seed) % (span + 1)
    if k >= span:
        k = span // 2
    return RAMP[i0 + k]


def _tone(c, pal):
    """Keep the iron plate's light/dark grain, shift the whole field onto the grade metal."""
    if not pal:
        return c
    lum = (c[0] + c[1] + c[2]) / 3.0
    lo = pal.get("lo", D2)
    hi = pal.get("hi", M3)
    sheen = pal.get("sheen", hi)
    if lum <= 85:
        t = (lum - 27) / 58.0
        t = 0 if t < 0 else 1 if t > 1 else t
        body = mix(lo, hi, t)
    else:
        t = (lum - 85) / 51.0
        t = 0 if t < 0 else 1 if t > 1 else t
        body = mix(hi, sheen, t)
    accent = pal.get("accent")
    if accent:
        body = mix(body, accent, 0.12)
    return body


def _tint(c, pal, amount=None):
    if not pal:
        return c
    body = _tone(c, pal)
    if amount is None:
        return body
    return mix(body, pal.get("accent", body), amount)


def wall_plate(seed=3, recess=True, pal=None):
    """Shallow pressed-iron plate, same language as a pressure-chamber wall."""
    im = new(16, (*D1, 255))
    for y in range(16):
        for x in range(16):
            edge = min(x, y, 15 - x, 15 - y)
            if edge == 0:
                c = D1 if (nrand(x, y, seed) % 5) else D2
            elif edge == 1:
                c = grain_at(x, y, seed, D3, M1)
            else:
                c = grain_at(x, y, seed, M0, M2)
            pset(im, x, y, _tint(c, pal))
    if not recess:
        for x, y in ((4, 5), (11, 4), (7, 10), (9, 12), (5, 13)):
            pset(im, x, y, _tint(D2, pal))
        return im
    for y in range(3, 13):
        for x in range(3, 13):
            on_lip = x in (3, 12) or y in (3, 12)
            if on_lip or (x, y) in ((4, 3), (11, 3), (3, 4), (12, 4), (3, 11), (12, 11), (4, 12), (11, 12)):
                if (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
                    pset(im, x, y, _tint(grain_at(x, y, seed + 1, D3, M0), pal))
                else:
                    pset(im, x, y, _tint(grain_at(x, y, seed + 2, M1, M3), pal))
            else:
                pset(im, x, y, _tint(grain_at(x, y, seed + 5, D2, M0), pal))
    for x, y in ((6, 6), (7, 6), (9, 9), (5, 10), (10, 7)):
        if nrand(x, y, seed) % 2 == 0:
            pset(im, x, y, _tint(D1, pal))
    return im


def quiet_rivet(im, x, y):
    """Tiny cool-grey stud, 2x2, very quiet."""
    pset(im, x, y, G1)
    pset(im, x + 1, y, G0)
    pset(im, x, y + 1, G0)
    pset(im, x + 1, y + 1, M0)


def grey_window(im, x, y, w, h, glass=D0):
    """Grey metal frame, dark glass. No bright teal."""
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            edge = xx in (x, x + w - 1) or yy in (y, y + h - 1)
            if edge:
                if xx == x or yy == y:
                    pset(im, xx, yy, G2 if (xx == x and yy == y) else G1)
                else:
                    pset(im, xx, yy, G0)
            else:
                pset(im, xx, yy, glass)
                if xx == x + 1 and yy == y + 1 and w > 3 and h > 3:
                    pset(im, xx, yy, D3)


def port(im, cx, cy, r=3):
    """Recessed circular port like a chamber valve, not a cartoon flange."""
    for dy in range(-r - 1, r + 2):
        for dx in range(-r - 1, r + 2):
            d2 = dx * dx + dy * dy
            if d2 <= (r + 1) * (r + 1) + 1:
                if d2 >= r * r:
                    pset(im, cx + dx, cy + dy, G0 if d2 <= r * r + 2 else M3)
                elif (d2 <= (r - 2) * (r - 2)) if r >= 3 else d2 <= 1:
                    pset(im, cx + dx, cy + dy, D0)
                else:
                    pset(im, cx + dx, cy + dy, D1)


def _paint_ctm_edge(im, side, seed, pal):
    """Shallow recessed frame along one unconnected edge; no cartoon outline."""
    if side == "up":
        coords = [((x, 0), (x, 1), (x, 2)) for x in range(16)]
    elif side == "down":
        coords = [((x, 15), (x, 14), (x, 13)) for x in range(16)]
    elif side == "left":
        coords = [((0, y), (1, y), (2, y)) for y in range(16)]
    else:
        coords = [((15, y), (14, y), (13, y)) for y in range(16)]
    for i, (a, b, c) in enumerate(coords):
        pset(im, a[0], a[1], _tint(D1 if (nrand(a[0], a[1], seed) % 5) else D2, pal))
        pset(im, b[0], b[1], _tint(grain_at(b[0], b[1], seed, D3, M1), pal))
        # inner lip, skip the very corners so adjacent edges meet softly
        if 1 < i < 14:
            pset(im, c[0], c[1], _tint(grain_at(c[0], c[1], seed + 2, M1, M3), pal))


def casing_tile(pal, mask: int):
    """Connected-cube sheet tile. Interiors are a flat mottled iron field;
    only unconnected edges get a shallow recessed frame. Isolated (mask 0)
    uses the approved pressed-plate + quiet grade mark."""
    seed = _grade_seed(pal)
    im = new(16, (*D1, 255))
    for y in range(16):
        for x in range(16):
            pset(im, x, y, _tint(grain_at(x, y, seed, M0, M2), pal, 0.08))
    up, right, down, left = mask & 1, mask & 2, mask & 4, mask & 8
    if not up:
        _paint_ctm_edge(im, "up", seed, pal)
    if not down:
        _paint_ctm_edge(im, "down", seed + 3, pal)
    if not left:
        _paint_ctm_edge(im, "left", seed + 5, pal)
    if not right:
        _paint_ctm_edge(im, "right", seed + 7, pal)
    if mask == 0:
        im = wall_plate(seed=seed, recess=True, pal=pal)
        _casing_emblem(im, pal)
    return im


def _casing_emblem(im, pal):
    name = pal.get("name", "iron")
    if name == "iron":
        for x in range(5, 11):
            pset(im, x, 7, D1)
            pset(im, x, 8, M2 if x % 2 else M1)
    elif name == "bronze":
        for x in range(4, 12):
            pset(im, x, 6, WARM)
            pset(im, x, 7, D2)
            pset(im, x, 9, WARM)
            pset(im, x, 10, D2)
    elif name == "steel":
        for y in range(5, 11):
            pset(im, 5, y, COOL)
            pset(im, 10, y, COOL)
        quiet_rivet(im, 7, 7)
    elif name == "ceramic":
        for (xx, yy) in ((4, 4), (8, 4), (4, 8), (8, 8)):
            for y in range(yy, yy + 4):
                for x in range(xx, xx + 4):
                    if x == xx or y == yy:
                        pset(im, x, y, CER if (x + y) % 3 == 0 else pal.get("mid", M1))
                    elif x == xx + 3 or y == yy + 3:
                        pset(im, x, y, D2)
                    else:
                        pset(im, x, y, _tone(grain_at(x, y, 41, M0, M2), pal))
    elif name == "cryo":
        grey_window(im, 5, 5, 6, 6, glass=D0)
        pset(im, 5, 5, G3)
        pset(im, 10, 5, G2)
        pset(im, 5, 10, FROST)
    else:
        for y, xs in ((5, (7, 8)), (6, (6, 9)), (7, (5, 10)), (8, (5, 10)), (9, (6, 9)), (10, (7, 8))):
            for x in range(xs[0], xs[1] + 1):
                pset(im, x, y, PALE)
        for y, xs in ((7, (7, 8)), (8, (7, 8))):
            for x in range(xs[0], xs[1] + 1):
                pset(im, x, y, D0)
        pset(im, 7, 7, D3)


def machine_side(pal):
    im = wall_plate(seed=_grade_seed(pal) + 71, recess=True, pal=pal)
    for y in (5, 8, 11):
        for x in range(4, 12):
            pset(im, x, y, D0)
            pset(im, x, y + 1, D3)
    return im


def machine_bottom(pal):
    return wall_plate(seed=_grade_seed(pal) + 20, recess=False, pal=pal)


def machine_face(pal, family, name=None):
    key = name or family
    fn = FACE_DRAWERS.get(key) or FACE_DRAWERS.get(family) or FACE_DRAWERS["chamber"]
    return fn(pal)


def _face_generic(pal, family="chamber"):
    im = wall_plate(seed=_grade_seed(pal), recess=True, pal=pal)
    grey_window(im, 4, 4, 8, 6, glass=D0)
    port(im, 8, 12, 2)
    return im


def _face_impact_crusher(pal):
    im = wall_plate(seed=101, pal=pal)
    for y in range(8, 13):
        for x in range(3, 13):
            pset(im, x, y, D0 if y in (8, 12) or x in (3, 12) else D1)
    for x in (5, 7, 9, 11):
        pset(im, x, 9, D3)
        pset(im, x, 10, M0)
        pset(im, x, 11, D1)
    quiet_rivet(im, 4, 4)
    quiet_rivet(im, 11, 4)
    return im


def _face_gas_bank(pal):
    im = wall_plate(seed=111, pal=pal)
    for y in range(6, 12):
        for x in range(3, 13):
            if y in (6, 11) or x in (3, 12):
                pset(im, x, y, D1)
            elif y == 8:
                pset(im, x, y, M2)
            else:
                pset(im, x, y, M0)
    for x in (5, 8, 11):
        for y in range(6, 12):
            pset(im, x, y, D2)
    port(im, 10, 8, 2)
    return im


def _face_residue_kiln(pal):
    im = wall_plate(seed=121, pal=pal)
    for y in range(5, 13):
        for x in range(4, 12):
            if y == 5 and x in (4, 11):
                continue
            border = x in (4, 11) or y in (5, 12)
            pset(im, x, y, D2 if border else D1)
    for x in range(5, 11):
        pset(im, x, 4, D2)
    pset(im, 7, 10, EMBER)
    pset(im, 8, 10, EMBER2)
    quiet_rivet(im, 11, 4)
    return im


def _face_lube_mixer(pal):
    im = wall_plate(seed=131, pal=pal)
    port(im, 8, 8, 4)
    for x in range(5, 11):
        if x not in (7, 8):
            pset(im, x, 8, D1)
    for y in range(5, 11):
        if y not in (7, 8):
            pset(im, 8, y, D1)
    quiet_rivet(im, 3, 3)
    return im


def _face_flare_stack(pal):
    im = wall_plate(seed=141, pal=pal)
    for y in range(2, 14):
        for x in range(6, 10):
            pset(im, x, y, D2 if x in (6, 9) or y in (2, 13) else D0)
    pset(im, 7, 2, EMBER2)
    pset(im, 8, 2, EMBER)
    for y in (5, 8, 11):
        pset(im, 3, y, WARM)
        pset(im, 4, y, WARM)
    port(im, 12, 11, 2)
    return im


def _face_washer_tower(pal):
    im = wall_plate(seed=151, pal=pal)
    grey_window(im, 4, 3, 8, 3, glass=D0)
    grey_window(im, 4, 7, 8, 3, glass=D0)
    grey_window(im, 4, 11, 8, 2, glass=D1)
    return im


def _face_thermolysis_kettle(pal):
    im = wall_plate(seed=161, pal=pal)
    port(im, 8, 8, 4)
    grey_window(im, 7, 7, 3, 3, glass=D0)
    for y in (6, 8, 10):
        pset(im, 3, y, M3)
        pset(im, 12, y, M3)
    quiet_rivet(im, 12, 3)
    return im


def _face_compressor_column(pal):
    im = wall_plate(seed=171, pal=pal)
    port(im, 8, 8, 4)
    port(im, 4, 8, 1)
    port(im, 12, 8, 1)
    quiet_rivet(im, 3, 3)
    quiet_rivet(im, 12, 3)
    return im


def _face_cyclone(pal):
    im = wall_plate(seed=181, pal=pal)
    bands = [(4, 5, 10), (5, 4, 11), (6, 4, 11), (7, 4, 11), (8, 4, 11), (9, 5, 10), (10, 5, 10), (11, 6, 9), (12, 7, 8)]
    for y, x0, x1 in bands:
        for x in range(x0, x1 + 1):
            pset(im, x, y, D1 if x in (x0, x1) else D0)
    for x in range(11, 14):
        pset(im, x, 6, D2)
        pset(im, x, 7, D1)
    pset(im, 13, 6, D0)
    quiet_rivet(im, 3, 3)
    return im


def _face_alloy_kettle(pal):
    im = wall_plate(seed=191, pal=pal)
    for y in range(6, 13):
        for x in range(4, 12):
            pset(im, x, y, D2 if x in (4, 11) or y in (6, 12) else D1)
    for x in range(4, 12):
        pset(im, x, 6, M2)
    pset(im, 7, 10, WARM)
    pset(im, 8, 10, D2)
    grey_window(im, 7, 3, 3, 2, glass=D0)
    return im


def _face_charging_station(pal):
    im = wall_plate(seed=201, pal=pal)
    for y in range(7, 13):
        for x in range(4, 12):
            pset(im, x, y, D1 if x in (4, 11) or y in (7, 12) else D0)
    pset(im, 6, 9, G1)
    pset(im, 7, 9, G2)
    pset(im, 8, 9, G1)
    pset(im, 9, 9, G0)
    for y in range(3, 8):
        pset(im, 8, y, D2)
    for x in range(8, 14):
        pset(im, x, 3, D2)
    quiet_rivet(im, 3, 3)
    return im


def _face_critical_kettle(pal):
    im = wall_plate(seed=211, pal=pal)
    grey_window(im, 4, 4, 8, 8, glass=D0)
    for y in range(6, 10):
        for x in range(6, 10):
            pset(im, x, y, D1)
    pset(im, 7, 7, D3)
    pset(im, 8, 7, D2)
    quiet_rivet(im, 13, 2)
    quiet_rivet(im, 2, 13)
    return im


def _face_clean_room(pal):
    im = wall_plate(seed=221, pal=pal)
    for y in range(3, 13):
        for x in range(4, 12):
            pset(im, x, y, D1)
    for y in range(3, 13):
        pset(im, 7, y, D0)
        pset(im, 8, y, D2)
    grey_window(im, 9, 5, 2, 4, glass=D0)
    pset(im, 5, 8, G0)
    quiet_rivet(im, 3, 3)
    return im


def _face_electrolyzer(pal):
    im = wall_plate(seed=241, pal=pal)
    grey_window(im, 3, 4, 10, 8, glass=D0)
    for y in range(5, 12):
        pset(im, 6, y, G0)
        pset(im, 9, y, G0)
    pset(im, 6, 5, G2)
    pset(im, 9, 5, G2)
    quiet_rivet(im, 2, 2)
    return im


def _face_polymerizer(pal):
    im = wall_plate(seed=251, pal=pal)
    port(im, 8, 9, 4)
    for y in range(2, 6):
        x0, x1 = 6 - (y - 2) // 2, 9 + (y - 2) // 2
        for x in range(x0, x1 + 1):
            pset(im, x, y, D1 if x in (x0, x1) or y == 2 else D2)
    quiet_rivet(im, 3, 12)
    return im


def _face_sinter_kiln(pal):
    im = wall_plate(seed=261, pal=pal)
    for y in range(5, 13):
        for x in range(4, 12):
            border = x in (4, 11) or y in (5, 12)
            pset(im, x, y, D2 if border else D1)
    for x in range(5, 11):
        pset(im, x, 4, M2)
    pset(im, 7, 11, EMBER2)
    pset(im, 8, 11, EMBER)
    quiet_rivet(im, 2, 2)
    return im


def _face_tower(pal, seed=151, rows=3):
    im = wall_plate(seed=seed, pal=pal)
    ys = (3, 7, 11) if rows == 3 else (3, 6, 9, 12)
    h = 3 if rows == 3 else 2
    for i, y in enumerate(ys):
        hh = h if y + h <= 15 else 15 - y
        if hh >= 2:
            grey_window(im, 4, y, 8, hh, glass=D0 if i < rows - 1 else D1)
    return im


def _face_fraction_tower(pal):
    return _face_tower(pal, seed=271, rows=4)


def _face_scrubber_tower(pal):
    im = _face_tower(pal, seed=281, rows=3)
    port(im, 13, 8, 2)
    return im


def _face_cryo_tower(pal):
    im = wall_plate(seed=291, pal=pal)
    grey_window(im, 4, 3, 8, 10, glass=D0)
    pset(im, 4, 3, G3)
    pset(im, 11, 3, G2)
    pset(im, 4, 12, FROST)
    pset(im, 11, 12, FROST)
    return im


def _face_expander_column(pal):
    im = wall_plate(seed=301, pal=pal)
    port(im, 8, 8, 5)
    pset(im, 8, 8, FROST)
    port(im, 3, 11, 2)
    quiet_rivet(im, 12, 3)
    return im


def _face_regulator_skid(pal):
    im = wall_plate(seed=311, pal=pal)
    port(im, 8, 8, 5)
    # quiet ticks, no cream face / red needle
    for dx, dy in ((0, -3), (3, 0), (0, 3), (-3, 0)):
        pset(im, 8 + dx, 8 + dy, G1)
    pset(im, 9, 6, G2)
    pset(im, 10, 5, G0)
    quiet_rivet(im, 2, 2)
    return im


def _face_item_gallery(pal):
    im = wall_plate(seed=321, pal=pal)
    for x in (3, 7, 11):
        for y in range(5, 13):
            for xx in range(x, x + 2):
                pset(im, xx, y, D0 if y in (5, 12) or xx in (x, x + 1) and y in (5, 12) else D1)
        pset(im, x, 5, D2)
        pset(im, x + 1, 5, D2)
    quiet_rivet(im, 2, 2)
    return im


def _face_cracker(pal):
    im = wall_plate(seed=331, pal=pal)
    for y in (4, 7, 10):
        for x in range(2, 14):
            pset(im, x, y, D0)
            pset(im, x, y + 1, D2)
            pset(im, x, y + 2, D1)
        pset(im, 2, y + 1, D0)
        pset(im, 13, y + 1, D0)
    pset(im, 12, 11, EMBER2)
    return im


def _face_leach_line(pal):
    im = wall_plate(seed=341, pal=pal)
    for y in range(6, 13):
        for x in range(2, 14):
            pset(im, x, y, D2 if x in (2, 13) or y in (6, 12) else D1)
    for x in range(3, 13):
        pset(im, x, 9, D0)
        pset(im, x, 10, D0)
    quiet_rivet(im, 2, 2)
    return im


def _face_lamination_gallery(pal):
    im = wall_plate(seed=351, pal=pal)
    for y0 in (4, 9):
        for y in range(y0, y0 + 3):
            for x in range(2, 14):
                pset(im, x, y, D0 if y in (y0, y0 + 2) else M1)
        for x in (4, 8, 12):
            pset(im, x, y0 + 1, D2)
    pset(im, 3, 7, M2)
    pset(im, 12, 7, M2)
    return im


def _face_quench_well(pal):
    im = wall_plate(seed=361, pal=pal)
    for y in range(6, 14):
        for x in range(3, 13):
            pset(im, x, y, D2 if x in (3, 12) or y in (6, 13) else D0)
    grey_window(im, 6, 3, 4, 3, glass=D0)
    return im


def _face_heat_gallery(pal):
    im = wall_plate(seed=371, pal=pal)
    for x, y in ((4, 5), (8, 5), (12, 5), (4, 9), (8, 9), (12, 9)):
        port(im, x, y, 2)
    pset(im, 8, 9, EMBER2)
    return im


def _face_drone_nest(pal):
    im = wall_plate(seed=381, pal=pal)
    port(im, 8, 8, 5)
    for x, y in ((4, 4), (11, 4), (4, 11), (11, 11)):
        quiet_rivet(im, x, y)
    pset(im, 8, 8, G0)
    return im


def _face_cycle_island(pal):
    im = wall_plate(seed=391, pal=pal)
    for y in range(3, 13):
        for x in range(3, 13):
            edge = x in (3, 12) or y in (3, 12)
            inner = 6 <= x <= 9 and 6 <= y <= 9
            if edge:
                pset(im, x, y, D2)
            elif not inner:
                pset(im, x, y, M0)
            else:
                pset(im, x, y, grain_at(x, y, 391, M0, M2))
    port(im, 8, 8, 2)
    return im


def _face_fluid_loop(pal):
    im = wall_plate(seed=401, pal=pal)
    for x in range(3, 13):
        pset(im, x, 4, D1)
        pset(im, x, 5, D0)
        pset(im, x, 10, D1)
        pset(im, x, 11, D0)
    for y in range(4, 12):
        pset(im, 3, y, D1)
        pset(im, 4, y, D0)
        pset(im, 11, y, D1)
        pset(im, 12, y, D0)
    port(im, 8, 8, 2)
    return im


def _face_calibration_bench(pal):
    im = wall_plate(seed=411, pal=pal)
    port(im, 5, 7, 3)
    port(im, 11, 7, 3)
    pset(im, 6, 6, G1)
    pset(im, 12, 6, G1)
    for x in range(3, 13):
        pset(im, x, 12, D2)
        pset(im, x, 13, D1)
    quiet_rivet(im, 2, 2)
    return im


def _face_reducer(pal):
    im = wall_plate(seed=421, pal=pal)
    # wide-to-narrow recess, front is the narrow outlet (left in UV)
    for x, (y0, y1) in ((3, (5, 11)), (6, (6, 10)), (9, (7, 9)), (12, (7, 9))):
        for y in range(y0, y1 + 1):
            for xx in range(x, min(x + 3, 14)):
                pset(im, xx, y, D0 if y in (y0, y1) or xx == x else D1)
    port(im, 3, 8, 2)
    port(im, 13, 8, 1)
    return im


def _face_booster(pal):
    im = wall_plate(seed=431, pal=pal)
    port(im, 9, 8, 5)
    for y in range(7, 10):
        for x in range(2, 6):
            pset(im, x, y, D1 if y in (7, 9) else D0)
    quiet_rivet(im, 2, 2)
    return im


def _face_relief_valve(pal):
    im = wall_plate(seed=441, pal=pal)
    for y in range(7, 14):
        for x in range(5, 11):
            pset(im, x, y, D2 if x in (5, 10) or y in (7, 13) else D1)
    for y in range(3, 8):
        for x in range(6, 10):
            pset(im, x, y, D1 if x in (6, 9) else M1)
    pset(im, 7, 3, EMBER2)
    pset(im, 8, 3, EMBER)
    port(im, 3, 10, 2)
    port(im, 13, 10, 2)
    return im


FACE_DRAWERS = {
    "chamber": _face_generic,
    "impact_crusher": _face_impact_crusher,
    "cyclone": _face_cyclone,
    "storage": _face_gas_bank,
    "gas_bank": _face_gas_bank,
    "residue_kiln": _face_residue_kiln,
    "lube_mixer": _face_lube_mixer,
    "thermolysis_kettle": _face_thermolysis_kettle,
    "electrolyzer": _face_electrolyzer,
    "polymerizer": _face_polymerizer,
    "alloy_kettle": _face_alloy_kettle,
    "sinter_kiln": _face_sinter_kiln,
    "critical_kettle": _face_critical_kettle,
    "kettle": _face_thermolysis_kettle,
    "tower": lambda pal: _face_tower(pal),
    "washer_tower": _face_washer_tower,
    "fraction_tower": _face_fraction_tower,
    "scrubber_tower": _face_scrubber_tower,
    "cryo_tower": _face_cryo_tower,
    "column": _face_compressor_column,
    "compressor_column": _face_compressor_column,
    "expander_column": _face_expander_column,
    "gallery": _face_cracker,
    "cracker": _face_cracker,
    "leach_line": _face_leach_line,
    "lamination_gallery": _face_lamination_gallery,
    "heat": _face_heat_gallery,
    "heat_gallery": _face_heat_gallery,
    "manifold": _face_item_gallery,
    "item_gallery": _face_item_gallery,
    "flare": _face_flare_stack,
    "flare_stack": _face_flare_stack,
    "quench": _face_quench_well,
    "quench_well": _face_quench_well,
    "room": _face_clean_room,
    "clean_room": _face_clean_room,
    "charge": _face_charging_station,
    "charging_station": _face_charging_station,
    "drone_nest": _face_drone_nest,
    "cycle": _face_cycle_island,
    "cycle_island": _face_cycle_island,
    "loop": _face_fluid_loop,
    "fluid_loop": _face_fluid_loop,
    "regulator": _face_regulator_skid,
    "regulator_skid": _face_regulator_skid,
    "calibration": _face_calibration_bench,
    "calibration_bench": _face_calibration_bench,
    "reducer": _face_reducer,
    "booster": _face_booster,
    "relief_valve": _face_relief_valve,
}


def window_tex():
    im = wall_plate(seed=2, recess=False)
    grey_window(im, 2, 2, 12, 12, glass=D0)
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        pset(im, x, y, G1)
    pset(im, 3, 3, D3)
    return im


def dial_tex():
    im = wall_plate(seed=5, recess=False)
    port(im, 8, 8, 5)
    for dx, dy in ((0, -4), (4, 0), (0, 4), (-4, 0), (3, -3), (3, 3), (-3, 3), (-3, -3)):
        pset(im, 8 + dx, 8 + dy, G1)
    pset(im, 9, 6, G2)
    pset(im, 10, 5, G0)
    pset(im, 8, 8, D0)
    return im


def hatch_face(kind, color):
    im = wall_plate(seed=15 + len(kind), recess=True)
    if kind in ("item_in", "item_out"):
        port(im, 8, 8, 4)
        # chevron grooves, identity only as a hint of the muted color
        dy = 1 if kind == "item_in" else -1
        for i, x in enumerate(range(6, 10)):
            pset(im, x, 7 + dy, mix(color, M2, 0.55))
            pset(im, x, 8, mix(color, D1, 0.7))
    elif kind in ("fluid", "gas"):
        port(im, 8, 8, 4)
        pset(im, 8, 8, mix(color, D0, 0.65))
    elif kind == "heat":
        for y in range(5, 12):
            for x in range(4, 12):
                pset(im, x, y, D0 if y % 2 == 0 else D2)
        pset(im, 7, 8, EMBER)
        pset(im, 8, 8, EMBER2)
    elif kind == "vent":
        for y in (5, 8, 11):
            for x in range(4, 12):
                pset(im, x, y, D0)
                pset(im, x, y + 1, D3)
    elif kind == "drain":
        for i in range(5, 12, 2):
            for t in range(5, 12):
                pset(im, i, t, D0)
                pset(im, t, i, D0)
        pset(im, 8, 10, mix(color, D1, 0.7))
    elif kind == "gauge":
        port(im, 8, 8, 4)
        pset(im, 9, 6, G1)
    elif kind == "manifold":
        for cx, cy in ((5, 5), (10, 5), (5, 10), (10, 10)):
            port(im, cx, cy, 2)
    else:
        port(im, 8, 8, 4)
    return im


def part_tex(kind: str):
    seed = 17 + len(kind)
    pal = GRADES["g2"]
    if kind == "coil":
        pal = GRADES["g1"]
    elif kind == "intercooler":
        pal = GRADES["g4"]
    elif kind == "cap":
        pal = GRADES["g3"]
    im = wall_plate(seed=seed, recess=True, pal=pal)
    if kind == "tray":
        for y in range(5, 12, 2):
            for x in range(4, 12):
                pset(im, x, y, D0)
                pset(im, x, y + 1, D3)
    elif kind == "coil":
        for y in range(4, 13, 2):
            for x in range(3, 13):
                pset(im, x, y, D1)
                pset(im, x, y + 1, WARM if x % 3 == 0 else D2)
    elif kind == "stage":
        port(im, 8, 8, 3)
        for y in (4, 12):
            for x in range(3, 13):
                pset(im, x, y, D2)
                pset(im, x, y + (1 if y == 4 else -1), M2)
    elif kind == "intercooler":
        for y in range(4, 13, 2):
            for x in range(2, 14):
                pset(im, x, y, D0)
                pset(im, x, y + 1, FROST if x % 2 == 0 else D2)
        for y in range(3, 14):
            pset(im, 7, y, M2)
            pset(im, 8, y, M1)
    elif kind == "bay":
        for y in range(5, 13):
            for x in range(3, 13):
                pset(im, x, y, D0 if x in (3, 12) or y in (5, 12) else D1)
        grey_window(im, 6, 7, 4, 4, glass=D0)
    elif kind == "body":
        port(im, 8, 8, 3)
        port(im, 4, 8, 1)
        port(im, 12, 8, 1)
        for x in range(3, 13):
            pset(im, x, 4, M2)
            pset(im, x, 12, D2)
    elif kind == "cap":
        port(im, 8, 7, 5)
        for x in range(2, 14):
            pset(im, x, 13, D1)
            pset(im, x, 14, D2)
    elif kind == "airlock":
        grey_window(im, 3, 3, 5, 9, glass=D0)
        grey_window(im, 8, 3, 5, 9, glass=D0)
        for y in range(3, 13):
            pset(im, 7, y, D2)
        for x in range(3, 13):
            pset(im, x, 12, D1 if x % 2 else D0)
    return im


def tube_tex(cryo=False):
    pal = GRADES["g4"] if cryo else GRADES["g2"]
    im = new(16, (0, 0, 0, 0))
    # hoop-ringed pipe; middle band is a shaded cylinder (inventory UV)
    for y in range(5, 11):
        for x in range(16):
            if y == 5:
                c = M2
            elif y == 10:
                c = D1
            elif y == 7:
                c = M1
            else:
                c = M0
            if cryo:
                c = mix(c, FROST, 0.18)
            if x in (3, 7, 8, 12):
                c = D2 if not cryo else mix(D2, FROST, 0.2)
            pset(im, x, y, c)
    for y in range(4, 12):
        pset(im, 0, y, D1)
        pset(im, 15, y, D1)
    for y in range(6, 10):
        pset(im, 0, y, D0)
        pset(im, 15, y, D0)
    return im


def asphalt_tex():
    im = new(16, (*D0, 255))
    for y in range(16):
        for x in range(16):
            h = nrand(x, y, 90)
            c = RAMP[h % 4]  # D0-D3
            if h % 17 == 0:
                c = M0
            elif h % 23 == 0:
                c = D0
            pset(im, x, y, c)
    for x, y in ((0, 4), (1, 4), (2, 5), (3, 5), (4, 6), (5, 6), (6, 7),
                 (9, 2), (9, 3), (10, 4), (10, 5), (11, 6),
                 (2, 11), (3, 12), (4, 12), (5, 13), (6, 13), (7, 14)):
        pset(im, x, y, D0)
    return im


def catalog_tex():
    im = wall_plate(seed=33, recess=True)
    # two recessed "pages" in iron, not cream paper
    for x0 in (3, 8):
        for y in range(4, 13):
            for x in range(x0, x0 + 5):
                pset(im, x, y, D1 if x in (x0, x0 + 4) or y in (4, 12) else M1)
        for y in (6, 8, 10):
            for x in range(x0 + 1, x0 + 4):
                pset(im, x, y, D2)
    for y in range(4, 13):
        pset(im, 7, y, D0)
        pset(im, 8, y, D2)
    return im

def fluid_still():
    im = new(16)
    for y in range(16):
        for x in range(16):
            wave = int(6 * ((x + y * 2) % 8 < 3)) + int(4 * ((x * 2 + y) % 7 == 0))
            v = 170 + wave + (hash2(x, y, 4) % 10)
            if y % 8 == 2:
                v += 18
            if y % 8 == 3:
                v += 8
            pset(im, x, y, (v, v, v))
    return im


def fluid_flow():
    im = new(16)
    for y in range(16):
        for x in range(16):
            wave = ((x + y) % 6) * 8
            v = 150 + wave + (hash2(x, y, 5) % 12)
            if (x + y) % 6 == 0:
                v += 24
            pset(im, x, y, (v, v, v))
    return im


def outline(im, color=OUT):
    out = im.copy()
    w, h = im.size
    opaque = [[im.getpixel((x, y))[3] > 0 for x in range(w)] for y in range(h)]
    for y in range(h):
        for x in range(w):
            if opaque[y][x]:
                continue
            for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and opaque[ny][nx]:
                    pset(out, x, y, color)
                    break
    return out


def item_base():
    return new(16, (0, 0, 0, 0))


def make_item_plate(pal):
    im = item_base()
    for y, x0, x1 in (
        (3, 4, 11), (4, 3, 12), (5, 3, 12), (6, 3, 12), (7, 3, 12),
        (8, 3, 12), (9, 4, 12), (10, 5, 12),
    ):
        for x in range(x0, x1 + 1):
            light = 14 - (x - 3) - (y - 3)
            if y == 3 or x == x0:
                light += 18
            if x == x1:
                light -= 14
            if y == 10:
                light -= 10
            pset(im, x, y, metal_at(pal, x, y, _grade_seed(pal), light))
    for x in range(5, 13):
        pset(im, x, 11, pal["lo"])
    for y in range(4, 11):
        pset(im, 13, y, pal["lo"])
    pset(im, 13, 11, pal["rivet"])
    pset(im, 12, 11, pal["lo"])
    for x, y in ((4, 4), (11, 4), (5, 9), (11, 9)):
        pset(im, x, y, pal["edge"])
        pset(im, x + 1, y, pal["rivet"])
    fill(im, 7, 6, 9, 7, pal["accent"])
    pset(im, 7, 6, pal["sheen"])
    return outline(im)


def make_item_tube(pal):
    im = item_base()
    for x in range(3, 13):
        pset(im, x, 5, pal["hi"])
        pset(im, x, 6, pal["mid"])
        pset(im, x, 7, pal["accent"] if pal.get("name") in ("cryo", "critical") else TEAL)
        pset(im, x, 8, pal["mid"])
        pset(im, x, 9, pal["lo"])
        if x % 3 == 0:
            for y in range(5, 10):
                pset(im, x, y, pal["edge"])
            pset(im, x, 5, pal["mid"])
    for x in (1, 2, 13, 14):
        fill(im, x, 4, x, 10, pal["mid"])
        pset(im, x, 4, pal["hi"])
        pset(im, x, 10, pal["lo"])
    fill(im, 14, 6, 14, 8, pal["edge"])
    pset(im, 14, 7, mix(pal.get("accent", TEAL), (0, 0, 0), 0.4))
    pset(im, 1, 7, pal["edge"])
    return outline(im)


def make_item_seal(pal):
    im = item_base()
    cx, cy, ro, ri = 7.5, 7.5, 5.6, 2.4
    for y in range(16):
        for x in range(16):
            d2 = (x - cx) ** 2 + (y - cy) ** 2
            if ri * ri <= d2 <= ro * ro:
                lx = (x - cx + y - cy) / 10
                base = mix(pal["mid"], RUBBER, 0.35) if pal["name"] in ("iron", "steel") else pal["mid"]
                if pal["name"] == "cryo":
                    base = mix(pal["mid"], pal["accent"], 0.3)
                if pal["name"] == "critical":
                    base = pal["hi"]
                c = add(base, int(-lx * 18) + (8 if d2 > (ro - 0.8) ** 2 else 0))
                if d2 < (ri + 0.9) ** 2:
                    c = add(c, 10)
                pset(im, x, y, c)
    pset(im, 5, 4, pal["sheen"])
    pset(im, 4, 5, mix(pal["hi"], WHITE, 0.3))
    return outline(im)


def make_item_nozzle(pal):
    im = item_base()
    fill_metal(im, 1, 4, 5, 11, pal, bevel=True, seed=3)
    rect(im, 1, 4, 5, 11, pal["edge"])
    stud(im, 2, 5, pal)
    stud(im, 2, 9, pal)
    for x, y0, y1 in ((6, 5, 10), (7, 5, 10), (8, 6, 9), (9, 6, 9), (10, 6, 9), (11, 7, 8), (12, 7, 8)):
        fill(im, x, y0, x, y1, pal["mid"])
        pset(im, x, y0, pal["hi"])
        pset(im, x, y1, pal["lo"])
    fill(im, 13, 6, 14, 9, pal["lo"])
    pset(im, 14, 7, pal["edge"])
    pset(im, 14, 8, (10, 10, 12))
    pset(im, 13, 7, pal["accent"])
    return outline(im)


def make_item_diaphragm(pal):
    im = item_base()
    disc(im, 7.5, 7.5, 6.2, pal["edge"])
    disc_shaded(im, 7.5, 7.5, 5.4, pal["mid"], pal["hi"], pal["lo"])
    for x, y in ((3, 4), (7, 2), (11, 4), (12, 8), (11, 11), (7, 13), (3, 11), (2, 8)):
        pset(im, x, y, pal["rivet_hi"])
    disc(im, 7.5, 7.5, 3.6, pal["accent"])
    disc(im, 7.5, 7.5, 2.4, pal["hi"])
    disc(im, 7.5, 7.5, 1.2, pal["sheen"])
    pset(im, 6, 6, WHITE)
    return outline(im)


def make_item_cylinder(pal):
    im = item_base()
    fill_metal(im, 1, 4, 3, 11, pal, bevel=True, seed=4)
    rect(im, 1, 4, 3, 11, pal["edge"])
    for x in range(4, 11):
        pset(im, x, 5, pal["hi"])
        pset(im, x, 6, pal["mid"])
        pset(im, x, 7, pal["mid"])
        pset(im, x, 8, pal["lo"])
        pset(im, x, 9, pal["lo"])
        pset(im, x, 10, pal["edge"])
        if x in (6, 9):
            fill(im, x, 5, x, 10, pal["edge"])
            pset(im, x, 5, pal["mid"])
    fill_metal(im, 10, 4, 12, 11, pal, bevel=True, seed=4)
    rect(im, 10, 4, 12, 11, pal["edge"])
    pset(im, 5, 4, pal["accent"])
    pset(im, 9, 4, pal["accent"])
    pset(im, 5, 3, pal["lo"])
    pset(im, 9, 3, pal["lo"])
    fill(im, 13, 6, 15, 8, pal["hi"])
    pset(im, 13, 6, pal["sheen"])
    pset(im, 15, 8, pal["lo"])
    return outline(im)


def make_item_pump(pal):
    im = item_base()
    disc(im, 8, 8, 5.6, pal["edge"])
    disc_shaded(im, 8, 8, 4.8, pal["mid"], pal["hi"], pal["lo"])
    pset(im, 8, 8, pal["edge"])
    pset(im, 9, 7, pal["accent"])
    pset(im, 10, 6, pal["accent"])
    pset(im, 7, 9, pal["lo"])
    pset(im, 6, 10, pal["lo"])
    fill(im, 0, 6, 3, 9, pal["mid"])
    pset(im, 0, 6, pal["hi"])
    pset(im, 0, 9, pal["lo"])
    pset(im, 0, 7, pal["edge"])
    fill(im, 13, 6, 15, 9, pal["mid"])
    pset(im, 15, 6, pal["hi"])
    pset(im, 15, 9, pal["lo"])
    pset(im, 15, 7, pal["edge"])
    fill(im, 4, 13, 12, 14, pal["lo"])
    return outline(im)


def make_item_gripper(pal):
    im = item_base()
    fill_metal(im, 5, 8, 10, 14, pal, bevel=True, seed=6)
    rect(im, 5, 8, 10, 14, pal["edge"])
    flange_port(im, 7, 12, pal, TEAL_DK, False, 1.4)
    fill(im, 6, 6, 9, 8, pal["mid"])
    fill(im, 6, 6, 9, 6, pal["hi"])
    fill(im, 1, 2, 5, 6, pal["mid"])
    fill(im, 1, 2, 5, 2, pal["hi"])
    fill(im, 1, 2, 1, 6, pal["hi"])
    fill(im, 5, 3, 6, 7, pal["lo"])
    pset(im, 1, 2, pal["sheen"])
    fill(im, 10, 2, 14, 6, pal["mid"])
    fill(im, 10, 2, 14, 2, pal["hi"])
    fill(im, 14, 2, 14, 6, pal["lo"])
    fill(im, 9, 3, 10, 7, pal["lo"])
    fill(im, 5, 3, 5, 5, pal["accent"])
    fill(im, 10, 3, 10, 5, pal["accent"])
    return outline(im)


def make_item_slide(pal):
    im = item_base()
    fill(im, 1, 6, 14, 7, pal["hi"])
    fill(im, 1, 8, 14, 9, pal["lo"])
    fill(im, 1, 7, 14, 7, pal["mid"])
    fill(im, 1, 9, 14, 9, pal["edge"])
    fill(im, 1, 5, 2, 10, pal["mid"])
    fill(im, 13, 5, 14, 10, pal["mid"])
    pset(im, 1, 5, pal["hi"])
    pset(im, 13, 5, pal["hi"])
    fill_metal(im, 5, 3, 10, 12, pal, bevel=True, seed=7)
    rect(im, 5, 3, 10, 12, pal["edge"])
    fill(im, 6, 5, 9, 6, pal["accent"])
    pset(im, 7, 4, TEAL)
    pset(im, 8, 4, TEAL_HI)
    return outline(im)


def make_item_ingot(pal, stamp=True, glow=None):
    """Raised-rim ingot matching PneumaticCraft compressed iron: same silhouette for every bar and alloy."""
    im = item_base()
    well = pal["lo"]
    if glow:
        well = mix(pal["lo"], glow, 0.4)
    colors = {
        "O": pal["edge"],
        "B": add(pal["edge"], -12),
        "D": well,
        "S": mix(pal["lo"], pal["base"], 0.45),
        "r": pal["base"],
        "R": pal["mid"],
        "1": mix(pal["mid"], pal["hi"], 0.45),
        "2": pal["hi"],
        "H": pal["sheen"],
    }
    # 16x16 map traced from the official compressed-iron ingot silhouette (original art, recolored).
    template = [
        "................",
        "................",
        "................",
        "..........OO....",
        ".......OOORRO...",
        "....OOOR1H21RO..",
        ".OOORH212211RRO.",
        "O211H212211R22RO",
        "ORH221221H22RDrB",
        "OrRH12HHHRSDDSrB",
        "OrrRHHRSDDDDrrrB",
        "OSrrRSDDDDrrSBB.",
        ".OSrRSDDSSBBB...",
        "..OSrSOBBB......",
        "...OOBB.........",
        "................",
    ]
    for y, row in enumerate(template):
        for x, ch in enumerate(row):
            if ch != ".":
                pset(im, x, y, colors[ch])
    return im



def make_item_crushed(color, flecks=None):
    """Mounded crushed-ore pile matching the PneumaticCraft crushed-metal silhouette."""
    im = item_base()
    lo = mix(color, (24, 16, 12), 0.7)
    hi = mix(color, (255, 248, 220), 0.7)
    pts = [
        (7, 2, 0.511), (8, 2, 0.511),
        (6, 3, 0.511), (7, 3, 0.921), (8, 3, 0.796), (9, 3, 0.511),
        (5, 4, 0.511), (6, 4, 0.921), (7, 4, 1.0), (8, 4, 0.375), (9, 4, 0.846), (10, 4, 0.336),
        (4, 5, 0.382), (5, 5, 0.866), (6, 5, 1.0), (7, 5, 0.767), (8, 5, 0.866), (9, 5, 0.767), (10, 5, 0.697), (11, 5, 0.253),
        (3, 6, 0.361), (4, 6, 0.866), (5, 6, 0.921), (6, 6, 0.767), (7, 6, 0.878), (8, 6, 0.697), (9, 6, 0.796), (10, 6, 0.509), (11, 6, 0.336), (12, 6, 0.168),
        (2, 7, 0.253), (3, 7, 0.509), (4, 7, 0.727), (5, 7, 0.807), (6, 7, 0.309), (7, 7, 0.807), (8, 7, 0.878), (9, 7, 0.498), (10, 7, 0.253), (11, 7, 0.309), (12, 7, 0.268), (13, 7, 0.0),
        (2, 8, 0.253), (3, 8, 0.666), (4, 8, 0.274), (5, 8, 0.666), (6, 8, 0.727), (7, 8, 0.523), (8, 8, 0.523), (9, 8, 0.498), (10, 8, 0.454), (11, 8, 0.287), (12, 8, 0.168), (13, 8, 0.0),
        (2, 9, 0.168), (3, 9, 0.361), (4, 9, 0.574), (5, 9, 0.767), (6, 9, 0.509), (7, 9, 0.523), (8, 9, 0.767), (9, 9, 0.369), (10, 9, 0.253), (11, 9, 0.382), (12, 9, 0.168), (13, 9, 0.0),
        (3, 10, 0.168), (4, 10, 0.361), (5, 10, 0.361), (6, 10, 0.511), (7, 10, 0.268), (8, 10, 0.382), (9, 10, 0.253), (10, 10, 0.168), (11, 10, 0.168), (12, 10, 0.0),
        (4, 11, 0.0), (5, 11, 0.02), (6, 11, 0.268), (7, 11, 0.268), (8, 11, 0.168), (9, 11, 0.168), (10, 11, 0.0), (11, 11, 0.0),
        (6, 12, 0.0), (7, 12, 0.0), (8, 12, 0.0), (9, 12, 0.0),
    ]
    for x, y, t in pts:
        if t < 0.5:
            c = mix(lo, color, t / 0.5)
        else:
            c = mix(color, hi, (t - 0.5) / 0.5)
        if flecks is not None and t >= 0.9:
            c = mix(c, flecks, 0.4)
        pset(im, x, y, c)
    return im


def make_item_dust(color, clean=False, flecks=None, angular=False):
    im = item_base()
    rows = {5: (6, 9), 6: (5, 10), 7: (4, 11), 8: (3, 12), 9: (3, 12), 10: (4, 12), 11: (5, 11)}
    if angular:
        rows = {5: (7, 9), 6: (5, 10), 7: (4, 11), 8: (3, 11), 9: (4, 12), 10: (5, 12), 11: (6, 10)}
    hi = add(color, 40 if not clean else 55)
    lo = add(color, -45)
    mid = color
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            h = hash2(x, y, 8)
            t = 0.6 - 0.08 * (x - 6) - 0.08 * (y - 6)
            c = mix(lo, hi, t)
            if h < 40:
                c = lo
            elif h > 220:
                c = hi
            pset(im, x, y, c)
    for x, y in ((2, 10), (3, 12), (13, 10), (12, 12), (4, 13), (10, 13), (14, 9)):
        pset(im, x, y, lo if (x + y) % 2 == 0 else mid)
        if hash2(x, y, 1) > 120:
            pset(im, x, y - 1, mid)
    if flecks:
        for x, y in ((6, 7), (8, 6), (9, 8), (5, 9), (10, 9), (7, 10)):
            pset(im, x, y, flecks)
    if clean:
        pset(im, 6, 6, mix(hi, WHITE, 0.5))
        pset(im, 8, 7, mix(hi, WHITE, 0.35))
        pset(im, 7, 8, mix(color, WHITE, 0.25))
    return outline(im)


def make_item_chip(board, die, pins=True, broken=False, traces=True, channels=False):
    im = item_base()
    fill(im, 3, 3, 12, 12, board)
    fill(im, 3, 3, 12, 3, add(board, 24))
    fill(im, 3, 3, 3, 12, add(board, 16))
    fill(im, 3, 12, 12, 12, add(board, -28))
    fill(im, 12, 3, 12, 12, add(board, -20))
    if pins:
        for y in (4, 6, 8, 10):
            pset(im, 2, y, (200, 170, 60))
            pset(im, 13, y, (200, 170, 60))
            pset(im, 3, y, (160, 130, 40))
            pset(im, 12, y, (160, 130, 40))
    if traces:
        for x, y in ((5, 5), (6, 5), (7, 5), (7, 6), (7, 7), (8, 7), (9, 7), (9, 8), (9, 9), (5, 9), (6, 9)):
            pset(im, x, y, (40, 120, 70) if not channels else TEAL)
    if channels:
        fill(im, 5, 5, 10, 5, TEAL)
        fill(im, 5, 5, 5, 10, TEAL)
        fill(im, 5, 10, 10, 10, TEAL)
        fill(im, 10, 7, 10, 10, TEAL)
        pset(im, 5, 5, TEAL_HI)
        pset(im, 8, 7, die)
    else:
        fill(im, 6, 6, 9, 9, die)
        pset(im, 6, 6, mix(die, WHITE, 0.4))
        pset(im, 9, 9, add(die, -40))
    if broken:
        for x, y in ((10, 3), (11, 3), (12, 3), (12, 4), (11, 4), (12, 5)):
            pset(im, x, y, (0, 0, 0, 0))
        for x, y in ((8, 5), (9, 6), (10, 7), (9, 8), (10, 9), (11, 10)):
            pset(im, x, y, add(board, -40))
    return outline(im)


def make_bucket_body():
    pal = GRADES["g0"]
    im = item_base()
    fill(im, 4, 1, 11, 1, pal["hi"])
    pset(im, 4, 2, pal["mid"])
    pset(im, 11, 2, pal["mid"])
    pset(im, 4, 3, pal["lo"])
    pset(im, 11, 3, pal["lo"])
    fill(im, 3, 4, 12, 5, pal["mid"])
    fill(im, 3, 4, 12, 4, pal["hi"])
    fill(im, 3, 5, 12, 5, pal["lo"])
    pset(im, 3, 4, pal["sheen"])
    for y, x0, x1 in ((6, 3, 12), (7, 3, 12), (8, 3, 12), (9, 4, 11), (10, 4, 11), (11, 4, 11), (12, 5, 10), (13, 5, 10), (14, 6, 9)):
        for x in range(x0, x1 + 1):
            light = 8
            if x == x0 or y == 6:
                light += 16
            if x == x1 or y >= 13:
                light -= 18
            pset(im, x, y, metal_at(pal, x, y, 11, light))
    rivet(im, 4, 8, pal)
    rivet(im, 10, 8, pal)
    return outline(im)


def make_bucket_fluid():
    im = item_base()
    for y, x0, x1 in ((6, 5, 10), (7, 5, 10), (8, 5, 10), (9, 5, 10), (10, 5, 10), (11, 5, 10), (12, 6, 9), (13, 6, 9)):
        for x in range(x0, x1 + 1):
            pset(im, x, y, (255, 255, 255, 255 if y > 6 else 230))
    fill(im, 5, 6, 10, 6, (255, 255, 255, 255))
    return im


def make_item_electrode(pal):
    im = item_base()
    fill(im, 6, 1, 9, 3, pal["mid"])
    fill(im, 6, 1, 9, 1, pal["hi"])
    cer = GRADES["g3"]
    fill(im, 5, 3, 10, 7, cer["mid"])
    fill(im, 5, 3, 10, 3, cer["hi"])
    fill(im, 5, 7, 10, 7, cer["lo"])
    fill(im, 6, 4, 9, 4, cer["sheen"])
    fill(im, 5, 5, 10, 5, cer["lo"])
    fill(im, 7, 7, 8, 14, pal["hi"])
    pset(im, 7, 14, pal["sheen"])
    pset(im, 8, 14, pal["lo"])
    pset(im, 7, 13, pal["mid"])
    return outline(im)


def make_item_wax():
    im = item_base()
    body = (186, 160, 70)
    hi = (226, 204, 120)
    lo = (128, 100, 40)
    rows = {3: (6, 9), 4: (5, 11), 5: (4, 12), 6: (4, 12), 7: (3, 12), 8: (4, 13), 9: (4, 12), 10: (5, 12), 11: (6, 11), 12: (7, 10)}
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            t = 0.6 - 0.07 * (x - 6) - 0.06 * (y - 5)
            c = mix(lo, hi, t)
            if hash2(x, y, 2) < 30:
                c = lo
            pset(im, x, y, c)
    pset(im, 8, 13, lo)
    pset(im, 8, 14, mix(lo, body, 0.5))
    pset(im, 5, 12, lo)
    pset(im, 6, 4, hi)
    return outline(im)


def make_item_plasticizer():
    im = item_base()
    cap = (70, 80, 70)
    fill(im, 6, 1, 9, 2, cap)
    fill(im, 6, 1, 9, 1, add(cap, 30))
    fill(im, 7, 3, 8, 4, (200, 210, 205))
    glass = (190, 210, 205)
    for y, x0, x1 in ((5, 5, 10), (6, 4, 11), (7, 4, 11), (8, 4, 11), (9, 4, 11),
                      (10, 4, 11), (11, 4, 11), (12, 5, 10), (13, 5, 10)):
        for x in range(x0, x1 + 1):
            pset(im, x, y, glass if x == x0 or y == 5 else add(glass, -20))
            if x == x0:
                pset(im, x, y, add(glass, 20))
            if x == x1:
                pset(im, x, y, add(glass, -30))
    liq = (60, 150, 110)
    for y in range(8, 13):
        fill(im, 5, y, 10, y, liq if y > 8 else mix(liq, WHITE, 0.25))
    pset(im, 5, 8, mix(liq, WHITE, 0.5))
    pset(im, 5, 6, WHITE)
    pset(im, 5, 7, WHITE)
    return outline(im)


def make_item_filter_cake():
    im = item_base()
    pal = dict(base=(168, 156, 132), mid=(184, 172, 146), hi=(204, 192, 164), lo=(120, 108, 88),
               rivet=(80, 72, 58), rivet_hi=(200, 188, 160), edge=(80, 72, 58), accent=(150, 132, 100),
               fill=(160, 148, 124), sheen=(214, 204, 178), name="cake")
    disc_shaded(im, 7.5, 8, 6, pal["mid"], pal["hi"], pal["lo"])
    fill(im, 3, 10, 12, 11, pal["lo"])
    fill(im, 4, 11, 11, 12, add(pal["lo"], -16))
    for x, y in ((7, 6), (8, 7), (9, 8), (6, 8)):
        pset(im, x, y, add(pal["lo"], -10))
    pset(im, 5, 5, pal["sheen"])
    pset(im, 3, 13, (80, 80, 78))
    pset(im, 12, 13, (80, 80, 78))
    return outline(im)


def make_item_catalyst():
    pal = GRADES["g3"]
    im = item_base()
    fill_metal(im, 3, 5, 11, 12, pal, bevel=True, seed=12)
    for y, x0, x1 in ((3, 5, 12), (4, 4, 12), (5, 3, 11)):
        for x in range(x0, x1 + 1):
            pset(im, x, y, pal["hi"] if y == 3 or x == x0 else pal["mid"])
    fill(im, 11, 5, 12, 11, pal["lo"])
    rect(im, 3, 5, 11, 12, pal["edge"])
    for x, y in ((5, 7), (8, 7), (6, 9), (9, 9), (5, 11), (8, 11), (7, 4), (10, 4)):
        pset(im, x, y, pal["edge"])
        pset(im, x + 1, y, pal["lo"])
    return outline(im)


def make_item_plastic():
    im = item_base()
    hi = (126, 168, 154)
    lo = (32, 64, 56)
    mid = (70, 118, 102)
    for y, x0, x1 in ((3, 4, 11), (4, 3, 12), (5, 3, 12), (6, 3, 12), (7, 3, 12),
                      (8, 3, 12), (9, 3, 12), (10, 4, 12), (11, 5, 12)):
        for x in range(x0, x1 + 1):
            light = 10 - (x - 3) - (y - 4)
            if y == 3 or x == x0:
                light += 20
            if x == x1 or y == 11:
                light -= 16
            t = 0.5 + light / 50
            pset(im, x, y, mix(lo, hi, t))
    for x in range(5, 13):
        pset(im, x, 12, lo)
    fill(im, 13, 5, 13, 11, lo)
    fill(im, 3, 7, 12, 7, mix(mid, lo, 0.4))
    pset(im, 4, 4, WHITE)
    pset(im, 5, 4, hi)
    return outline(im)


def make_item_substrate():
    im = item_base()
    mid = (150, 120, 70)
    hi = (186, 154, 96)
    lo = (100, 78, 46)
    for y, x0, x1 in ((3, 3, 12), (4, 3, 12), (5, 3, 12), (6, 3, 12), (7, 3, 12),
                      (8, 3, 12), (9, 3, 12), (10, 3, 12), (11, 4, 12)):
        for x in range(x0, x1 + 1):
            c = hi if y == 3 or x == x0 else (lo if x == x1 or y == 11 else mid)
            pset(im, x, y, c)
    fill(im, 4, 12, 12, 12, lo)
    fill(im, 13, 5, 13, 11, lo)
    for x, y in ((5, 5), (8, 5), (11, 5), (5, 8), (8, 8), (11, 8)):
        pset(im, x, y, (180, 140, 60))
        pset(im, x, y + 1, (120, 90, 40))
    return outline(im)


def make_item_paper(kind="sheet"):
    im = item_base()
    page = (176, 168, 152)
    hi = (198, 190, 174)
    lo = (128, 120, 104)
    ink = (54, 50, 48)
    if kind == "manual":
        cover = (52, 56, 54)
        fill(im, 3, 2, 13, 14, cover)
        fill(im, 3, 2, 13, 2, add(cover, 30))
        fill(im, 3, 2, 3, 14, add(cover, 16))
        fill(im, 13, 2, 13, 14, add(cover, -24))
        fill(im, 12, 3, 13, 13, (176, 168, 152))
        fill(im, 13, 4, 13, 12, (140, 132, 116))
        fill(im, 5, 4, 10, 6, (140, 128, 96))
        pset(im, 5, 4, (168, 156, 120))
        fill(im, 6, 8, 10, 8, add(cover, 40))
        fill(im, 6, 10, 9, 10, add(cover, 40))
        fill(im, 10, 2, 11, 5, (92, 48, 36))
        return outline(im)
    fill(im, 3, 2, 12, 14, page)
    fill(im, 3, 2, 12, 2, hi)
    fill(im, 3, 2, 3, 14, hi)
    fill(im, 12, 2, 12, 14, lo)
    fill(im, 3, 14, 12, 14, lo)
    pset(im, 12, 2, (0, 0, 0, 0))
    pset(im, 11, 2, lo)
    pset(im, 12, 3, lo)
    for y in (5, 7, 9, 11):
        fill(im, 5, y, 10, y, ink)
    if kind == "certificate":
        rect(im, 4, 3, 11, 13, (128, 108, 70))
        fill(im, 6, 11, 9, 13, (92, 48, 36))
        pset(im, 7, 12, (168, 140, 88))
        pset(im, 8, 12, (184, 156, 100))
    if kind == "board":
        fill(im, 4, 2, 6, 3, (80, 80, 78))
    return outline(im)


def make_item_probe(pal):
    im = item_base()
    fill(im, 1, 6, 4, 9, (54, 50, 46))
    fill(im, 1, 6, 4, 6, (80, 76, 70))
    fill(im, 1, 9, 4, 9, (30, 28, 26))
    fill_metal(im, 4, 5, 8, 10, pal, bevel=True, seed=9)
    rect(im, 4, 5, 8, 10, pal["edge"])
    fill(im, 8, 6, 10, 9, pal["mid"])
    fill(im, 8, 6, 10, 6, pal["hi"])
    fill(im, 8, 9, 10, 9, pal["lo"])
    fill(im, 6, 3, 7, 5, pal["lo"])
    pset(im, 6, 3, TEAL)
    pset(im, 7, 3, TEAL_HI)
    fill(im, 10, 6, 13, 9, pal["mid"])
    fill(im, 10, 6, 13, 6, pal["hi"])
    fill(im, 14, 7, 15, 8, TEAL)
    pset(im, 15, 7, TEAL_HI)
    return outline(im)


def make_item_drill(pal):
    im = item_base()
    fill(im, 3, 9, 6, 14, (50, 46, 42))
    fill(im, 3, 9, 3, 14, (70, 66, 60))
    fill(im, 6, 9, 6, 14, (30, 28, 26))
    pset(im, 4, 10, (30, 28, 26))
    fill_metal(im, 2, 5, 10, 9, pal, bevel=True, seed=10)
    rect(im, 2, 5, 10, 9, pal["edge"])
    fill(im, 10, 5, 12, 8, pal["lo"])
    fill(im, 10, 5, 12, 5, pal["hi"])
    for i, (x, y) in enumerate(((12, 4), (13, 3), (13, 2), (14, 2), (14, 1), (15, 1), (15, 0))):
        pset(im, x, y, pal["sheen"] if i % 2 == 0 else pal["mid"])
    pset(im, 2, 6, TEAL)
    pset(im, 1, 6, pal["lo"])
    pset(im, 1, 7, pal["lo"])
    pset(im, 3, 5, pal["sheen"])
    return outline(im)


def make_item_curtain(pal):
    im = item_base()
    fill_metal(im, 2, 2, 13, 4, pal, bevel=True, seed=11)
    rect(im, 2, 2, 13, 4, pal["edge"])
    stud(im, 3, 2, pal)
    stud(im, 11, 2, pal)
    for i, x in enumerate(range(3, 13, 2)):
        mid = pal["mid"]
        hi = pal["accent"] if i % 2 == 0 else pal["hi"]
        lo = pal["lo"]
        fill(im, x, 5, x + 1, 14, mid)
        pset(im, x, 5, hi)
        fill(im, x, 5, x, 14, hi)
        fill(im, x + 1, 5, x + 1, 14, lo)
        if i % 2:
            pset(im, x, 14, (0, 0, 0, 0))
        else:
            pset(im, x + 1, 13, lo)
    return outline(im)


def make_item_valve(pal):
    im = item_base()
    fill_metal(im, 5, 8, 10, 14, pal, bevel=True, seed=13)
    rect(im, 5, 8, 10, 14, pal["edge"])
    pipe_h(im, 1, 10, 5, pal, thick=3)
    pipe_h(im, 10, 10, 14, pal, thick=3)
    fill(im, 7, 5, 8, 8, pal["hi"])
    disc(im, 7.5, 4, 3.3, pal["edge"])
    disc(im, 7.5, 4, 2.5, pal["mid"])
    pset(im, 7, 2, pal["hi"])
    pset(im, 8, 2, pal["hi"])
    pset(im, 7, 4, pal["lo"])
    pset(im, 8, 4, pal["sheen"])
    pset(im, 5, 4, pal["hi"])
    pset(im, 10, 4, pal["lo"])
    return outline(im)


def make_item_core():
    pal = GRADES["g5"]
    im = item_base()
    fill_metal(im, 4, 4, 11, 12, pal, bevel=True, seed=14)
    rect(im, 4, 4, 11, 12, pal["edge"])
    rivet(im, 4, 4, pal)
    rivet(im, 10, 4, pal)
    rivet(im, 4, 11, pal)
    rivet(im, 10, 11, pal)
    disc_shaded(im, 7.5, 8, 3.4, pal["accent"], pal["sheen"], TEAL_DK)
    pset(im, 7, 7, WHITE)
    pset(im, 8, 7, pal["sheen"])
    pset(im, 7, 8, pal["accent"])
    return outline(im)


def make_progwidget(kind: str):
    im = Image.new("RGBA", (80, 64), (0, 0, 0, 0))
    body = (48, 52, 58) if kind == "read_gauge" else (42, 58, 56)
    hi = add(body, 36)
    lo = add(body, -24)
    edge = (16, 16, 18)
    fill(im, 4, 4, 75, 59, body)
    rect(im, 4, 4, 75, 59, edge)
    fill(im, 5, 5, 74, 5, hi)
    fill(im, 5, 5, 5, 58, hi)
    fill(im, 5, 58, 74, 58, lo)
    fill(im, 74, 5, 74, 58, lo)
    fill(im, 10, 10, 69, 53, add(body, -10))
    rect(im, 10, 10, 69, 53, edge)
    fill(im, 11, 11, 68, 11, add(body, 16))
    fill(im, 0, 24, 4, 40, body)
    fill(im, 0, 24, 3, 24, hi)
    fill(im, 0, 40, 3, 40, lo)
    fill(im, 75, 24, 79, 40, body)
    fill(im, 76, 24, 79, 24, hi)
    fill(im, 76, 40, 79, 40, lo)
    if kind == "read_gauge":
        cx, cy = 39, 32
        disc(im, cx, cy, 16, edge)
        disc(im, cx, cy, 14, (220, 216, 200))
        disc(im, cx, cy, 13, (232, 228, 210))
        for a in range(0, 360, 30):
            x = round(cx + 11 * math.cos(math.radians(a)))
            y = round(cy + 11 * math.sin(math.radians(a)))
            pset(im, x, y, (40, 36, 30))
        for i in range(12):
            pset(im, cx + i, cy - i, (30, 28, 24) if i < 8 else NEEDLE)
        pset(im, cx, cy, (20, 18, 16))
        pset(im, cx - 6, cy - 6, WHITE)
    else:
        pal = GRADES["g2"]
        fill(im, 24, 16, 55, 47, pal["edge"])
        fill(im, 26, 18, 53, 45, TEAL_DK)
        fill(im, 28, 20, 51, 43, TEAL)
        pset(im, 28, 20, TEAL_HI)
        pset(im, 29, 20, TEAL_HI)
        pset(im, 28, 21, TEAL_HI)
        fill(im, 39, 18, 40, 45, pal["edge"])
        fill(im, 26, 31, 53, 32, pal["edge"])
        fill(im, 22, 30, 57, 33, (196, 164, 90))
        fill(im, 22, 30, 57, 30, (230, 200, 120))
    return im


def generate_textures():
    BLOCK_TEX.mkdir(parents=True, exist_ok=True)
    ITEM_TEX.mkdir(parents=True, exist_ok=True)
    WIDGET_TEX.mkdir(parents=True, exist_ok=True)

    for grade, pal in GRADES.items():
        isolated = casing_tile(pal, 0)
        save(isolated, BLOCK_TEX / f"casing_{grade}.png")
        sheet = new(64)
        for mask in range(16):
            tile = casing_tile(pal, mask)
            col, row = mask & 3, mask >> 2
            sheet.paste(tile, (col * 16, row * 16))
        save(sheet, BLOCK_TEX / f"casing_{grade}_ctm.png")
        save(machine_side(pal), BLOCK_TEX / f"machine_side_{grade}.png")
        save(machine_face(pal, "chamber"), BLOCK_TEX / f"machine_face_{grade}.png")

    for name, (grade, family) in CONTROLLERS.items():
        pal = GRADES[grade]
        save(machine_face(pal, family, name), BLOCK_TEX / f"machine_face_{name}.png")
        save(machine_top(pal, name), BLOCK_TEX / f"machine_top_{name}.png")
        save(machine_side_named(pal, name), BLOCK_TEX / f"machine_side_{name}.png")

    for grade, pal in GRADES.items():
        save(machine_bottom(pal), BLOCK_TEX / f"machine_bottom_{grade}.png")

    save(window_tex(), BLOCK_TEX / "window.png")
    save(dial_tex(), BLOCK_TEX / "dial.png")
    save(machine_side(GRADES["g2"]), BLOCK_TEX / "machine_side.png")
    save(machine_face(GRADES["g2"], "chamber"), BLOCK_TEX / "machine_face.png")
    save(casing_tile(GRADES["g2"], 0), BLOCK_TEX / "casing.png")
    save(hatch_face("gas", HATCHES["gas"]), BLOCK_TEX / "hatch.png")

    for name, color in HATCHES.items():
        save(hatch_face(name, color), BLOCK_TEX / f"hatch_{name}.png")
    for part in PARTS:
        save(part_tex(part), BLOCK_TEX / f"part_{part}.png")

    save(tube_tex(False), BLOCK_TEX / "tube.png")
    save(tube_tex(True), BLOCK_TEX / "tube_cryo.png")
    save(asphalt_tex(), BLOCK_TEX / "asphalt.png")
    save(catalog_tex(), BLOCK_TEX / "catalog.png")
    save(fluid_still(), BLOCK_TEX / "fluid_still.png")
    save(fluid_flow(), BLOCK_TEX / "fluid_flow.png")

    save(_face_reducer(GRADES["g2"]), BLOCK_TEX / "machine_face_reducer.png")
    save(_face_booster(GRADES["g2"]), BLOCK_TEX / "machine_face_booster.png")
    save(_face_relief_valve(GRADES["g2"]), BLOCK_TEX / "machine_face_relief_valve.png")

    item_makers = {
        "plate": make_item_plate,
        "tube": make_item_tube,
        "seal": make_item_seal,
        "nozzle": make_item_nozzle,
        "diaphragm": make_item_diaphragm,
        "cylinder": make_item_cylinder,
        "pump": make_item_pump,
        "gripper": make_item_gripper,
        "slide": make_item_slide,
    }
    for grade in ("g2", "g3", "g4", "g5"):
        pal = GRADES[grade]
        for shape, fn in item_makers.items():
            save(fn(pal), ITEM_TEX / f"{grade}_{shape}.png")

    save(make_item_ingot(GRADES["g2"]), ITEM_TEX / "compressed_steel.png")
    save(make_item_ingot(GRADES["g3"]), ITEM_TEX / "ceramic_alloy.png")
    save(make_item_ingot(GRADES["g4"]), ITEM_TEX / "cryo_alloy.png")
    save(make_item_ingot(GRADES["g5"], stamp=False, glow=(168, 176, 184)), ITEM_TEX / "critical_core.png")
    save(make_item_core(), ITEM_TEX / "core.png")

    save(make_item_dust((130, 128, 122)), ITEM_TEX / "stone_dust.png")
    save(make_item_dust((62, 54, 46), angular=True, flecks=(90, 80, 60)), ITEM_TEX / "slag.png")
    save(make_item_dust((86, 78, 70)), ITEM_TEX / "metal_ash.png")
    save(make_item_crushed((168, 104, 70)), ITEM_TEX / "crushed_iron.png")
    save(make_item_crushed((196, 112, 58), flecks=(240, 180, 90)), ITEM_TEX / "crushed_copper.png")
    save(make_item_crushed((212, 170, 58), flecks=(255, 230, 120)), ITEM_TEX / "crushed_gold.png")
    save(make_item_crushed((72, 58, 54), flecks=(78, 84, 86)), ITEM_TEX / "crushed_ancient.png")
    save(make_item_dust((190, 90, 70), clean=True, flecks=(230, 160, 120)), ITEM_TEX / "purified_iron.png")
    save(make_item_dust((220, 140, 70), clean=True, flecks=(255, 190, 100)), ITEM_TEX / "purified_copper.png")
    save(make_item_dust((236, 206, 90), clean=True, flecks=(255, 240, 160)), ITEM_TEX / "purified_gold.png")

    save(make_item_wax(), ITEM_TEX / "heavy_wax.png")
    save(make_item_wax(), ITEM_TEX / "wax.png")
    save(make_item_plasticizer(), ITEM_TEX / "plasticizer.png")
    save(make_item_catalyst(), ITEM_TEX / "catalyst.png")
    save(make_item_filter_cake(), ITEM_TEX / "filter_cake.png")
    save(make_item_filter_cake(), ITEM_TEX / "cake.png")
    save(make_item_chip((70, 74, 68), (90, 80, 70), broken=True), ITEM_TEX / "waste_chip.png")
    save(make_item_plastic(), ITEM_TEX / "engineering_plastic.png")
    save(make_item_plastic(), ITEM_TEX / "plastic.png")
    save(make_item_electrode(GRADES["g2"]), ITEM_TEX / "electrode.png")
    save(make_item_substrate(), ITEM_TEX / "substrate.png")
    save(make_item_chip((36, 48, 52), (78, 84, 86), channels=True, pins=True), ITEM_TEX / "microfluidic_chip.png")

    save(make_item_paper("sheet"), ITEM_TEX / "program_sheet.png")
    save(make_item_paper("sheet"), ITEM_TEX / "paper.png")
    save(make_item_paper("certificate"), ITEM_TEX / "certificate.png")
    save(make_item_paper("manual"), ITEM_TEX / "manual.png")
    save(make_item_paper("board"), ITEM_TEX / "board.png")

    save(make_item_probe(GRADES["g2"]), ITEM_TEX / "venturi_probe.png")
    save(make_item_probe(GRADES["g2"]), ITEM_TEX / "probe.png")
    save(make_item_drill(GRADES["g2"]), ITEM_TEX / "grade_drill.png")
    save(make_item_drill(GRADES["g2"]), ITEM_TEX / "drill.png")
    save(make_item_valve(GRADES["g2"]), ITEM_TEX / "valve.png")

    for grade, pal in (("g2", GRADES["g2"]), ("g3", GRADES["g3"]), ("g4", GRADES["g4"]), ("g5", GRADES["g5"])):
        save(make_item_curtain(pal), ITEM_TEX / f"curtain_{grade}.png")
    save(make_item_curtain(GRADES["g2"]), ITEM_TEX / "curtain.png")

    save(make_bucket_body(), ITEM_TEX / "bucket.png")
    save(make_bucket_fluid(), ITEM_TEX / "bucket_fluid.png")

    save(make_item_plate(GRADES["g2"]), ITEM_TEX / "plate.png")
    save(make_item_tube(GRADES["g2"]), ITEM_TEX / "tube.png")
    save(make_item_seal(GRADES["g2"]), ITEM_TEX / "seal.png")
    save(make_item_nozzle(GRADES["g2"]), ITEM_TEX / "nozzle.png")
    save(make_item_cylinder(GRADES["g2"]), ITEM_TEX / "cylinder.png")
    save(make_item_pump(GRADES["g2"]), ITEM_TEX / "pump.png")
    save(make_item_gripper(GRADES["g2"]), ITEM_TEX / "gripper.png")
    save(make_item_slide(GRADES["g2"]), ITEM_TEX / "slide.png")
    save(make_item_ingot(GRADES["g0"]), ITEM_TEX / "ingot.png")
    save(make_item_dust((130, 128, 122)), ITEM_TEX / "dust.png")
    save(make_item_dust((190, 90, 70), clean=True), ITEM_TEX / "dust_clean.png")
    save(make_item_chip((54, 58, 56), (70, 74, 76)), ITEM_TEX / "chip.png")

    save(make_progwidget("read_gauge"), WIDGET_TEX / "read_gauge.png")
    save(make_progwidget("hold_window"), WIDGET_TEX / "hold_window.png")

    GUI_TEX.mkdir(parents=True, exist_ok=True)
    # 176x188 apparatus, 176x166 catalog, 256x200 manual page — all in the 256 sheet
    save(make_gui_panel(176, 188, bar=(86, 62, 82, 6)), GUI_TEX / "apparatus.png")
    save(make_gui_panel(176, 166), GUI_TEX / "catalog.png")
    save(make_gui_panel(256, 200), GUI_TEX / "manual.png")



def make_gui_panel(width, height, well_slots=None, bar=None):
    """Code-adjacent GUI background in the iron chamber-wall language."""
    im = new((256, 256), (0, 0, 0, 0))
    for y in range(height):
        for x in range(width):
            edge = min(x, y, width - 1 - x, height - 1 - y)
            if edge == 0:
                c = D0
            elif edge == 1:
                c = D1
            elif edge == 2:
                c = mix(D2, G0, 0.25) if (x + y) % 9 == 0 else D2
            else:
                c = grain_at(x, y, 9, M0, M2)
                if y < 16:
                    c = mix(c, D1, 0.25)
            pset(im, x, y, c)
    for y in range(3, 16):
        for x in range(3, width - 3):
            pset(im, x, y, grain_at(x, y, 11, D2, M0))
    for x in range(3, width - 3):
        pset(im, x, 16, G0)
        pset(im, x, height - 4, D1)
    for y in range(3, height - 3):
        pset(im, 3, y, G0 if y % 5 else D3)
        pset(im, width - 4, y, D0)
    if well_slots:
        for sx, sy, sw, sh in well_slots:
            fill(im, sx - 1, sy - 1, sx + sw, sy + sh, D0)
            fill(im, sx, sy, sx + sw - 1, sy + sh - 1, (20, 18, 16))
    if bar:
        bx, by, bw, bh = bar
        fill(im, bx - 1, by - 1, bx + bw, by + bh, D0)
        fill(im, bx, by, bx + bw - 1, by + bh - 1, (20, 18, 16))
    return im


def write_json(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def cube(tex, particle=None):
    t = f"{MOD}:block/{tex}"
    return {"parent": "minecraft:block/cube_all", "textures": {"all": t, "particle": f"{MOD}:block/{particle or tex}"}}


def connected_casing(grade):
    return {
        "loader": f"{MOD}:connected_cube",
        "textures": {
            "all": f"{MOD}:block/casing_{grade}",
            "ctm": f"{MOD}:block/casing_{grade}_ctm",
            "particle": f"{MOD}:block/casing_{grade}",
        },
    }


def facing_state(model):
    return {
        "variants": {
            "facing=north": {"model": f"{MOD}:block/{model}", "y": 0},
            "facing=east": {"model": f"{MOD}:block/{model}", "y": 90},
            "facing=south": {"model": f"{MOD}:block/{model}", "y": 180},
            "facing=west": {"model": f"{MOD}:block/{model}", "y": 270},
        }
    }


def facing_all_state(model):
    return {
        "variants": {
            "facing=north": {"model": f"{MOD}:block/{model}"},
            "facing=south": {"model": f"{MOD}:block/{model}", "y": 180},
            "facing=west": {"model": f"{MOD}:block/{model}", "y": 270},
            "facing=east": {"model": f"{MOD}:block/{model}", "y": 90},
            "facing=up": {"model": f"{MOD}:block/{model}", "x": 270},
            "facing=down": {"model": f"{MOD}:block/{model}", "x": 90},
        }
    }


def simple_state(model):
    return {"variants": {"": {"model": f"{MOD}:block/{model}"}}}


SHAPE_CONTROLLER = {
    "parent": "minecraft:block/block",
    "textures": {
        "metal": f"{MOD}:block/machine_side_g2",
        "face": f"{MOD}:block/machine_face_g2",
        "glass": f"{MOD}:block/window",
        "particle": f"{MOD}:block/machine_side_g2",
    },
    "elements": [
        {
            "from": [1, 0, 1],
            "to": [15, 11, 15],
            "faces": {
                "down": {"uv": [1, 1, 15, 15], "texture": "#metal", "cullface": "down"},
                "up": {"uv": [1, 1, 15, 15], "texture": "#metal"},
                "north": {"uv": [1, 5, 15, 16], "texture": "#face"},
                "south": {"uv": [1, 5, 15, 16], "texture": "#metal"},
                "west": {"uv": [1, 5, 15, 16], "texture": "#metal"},
                "east": {"uv": [1, 5, 15, 16], "texture": "#metal"},
            },
        },
        {
            "from": [3, 11, 3],
            "to": [13, 14, 8],
            "faces": {
                "up": {"uv": [3, 3, 13, 8], "texture": "#glass"},
                "north": {"uv": [3, 2, 13, 5], "texture": "#face"},
                "south": {"uv": [3, 2, 13, 5], "texture": "#metal"},
                "west": {"uv": [3, 2, 8, 5], "texture": "#metal"},
                "east": {"uv": [8, 2, 13, 5], "texture": "#metal"},
            },
        },
        {
            "from": [6, 11, 10],
            "to": [10, 16, 14],
            "faces": {
                "up": {"uv": [6, 10, 10, 14], "texture": "#metal", "cullface": "up"},
                "north": {"uv": [6, 0, 10, 5], "texture": "#metal"},
                "south": {"uv": [6, 0, 10, 5], "texture": "#metal"},
                "west": {"uv": [10, 0, 14, 5], "texture": "#metal"},
                "east": {"uv": [2, 0, 6, 5], "texture": "#metal"},
            },
        },
    ],
}

SHAPE_HATCH = {
    "parent": "minecraft:block/block",
    "textures": {
        "metal": f"{MOD}:block/machine_side_g2",
        "face": f"{MOD}:block/hatch_gas",
        "particle": f"{MOD}:block/machine_side_g2",
    },
    "elements": [
        {
            "from": [0, 0, 0],
            "to": [16, 16, 16],
            "faces": {
                "down": {"uv": [0, 0, 16, 16], "texture": "#metal", "cullface": "down"},
                "up": {"uv": [0, 0, 16, 16], "texture": "#metal", "cullface": "up"},
                "north": {"uv": [0, 0, 16, 16], "texture": "#face", "cullface": "north"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#metal", "cullface": "south"},
                "west": {"uv": [0, 0, 16, 16], "texture": "#metal", "cullface": "west"},
                "east": {"uv": [0, 0, 16, 16], "texture": "#metal", "cullface": "east"},
            },
        },
        {
            "from": [4, 4, -1],
            "to": [12, 12, 2],
            "faces": {
                "down": {"uv": [4, 0, 12, 3], "texture": "#metal"},
                "up": {"uv": [4, 0, 12, 3], "texture": "#metal"},
                "north": {"uv": [4, 4, 12, 12], "texture": "#face"},
                "west": {"uv": [0, 4, 3, 12], "texture": "#metal"},
                "east": {"uv": [13, 4, 16, 12], "texture": "#metal"},
            },
        },
    ],
}

SHAPE_GAUGE = {
    "parent": "minecraft:block/block",
    "textures": {
        "metal": f"{MOD}:block/machine_side_g2",
        "face": f"{MOD}:block/dial",
        "particle": f"{MOD}:block/machine_side_g2",
    },
    "elements": [
        {
            "from": [4, 0, 4],
            "to": [12, 2, 12],
            "faces": {
                "down": {"uv": [4, 4, 12, 12], "texture": "#metal", "cullface": "down"},
                "up": {"uv": [4, 4, 12, 12], "texture": "#metal"},
                "north": {"uv": [4, 14, 12, 16], "texture": "#metal"},
                "south": {"uv": [4, 14, 12, 16], "texture": "#metal"},
                "west": {"uv": [4, 14, 12, 16], "texture": "#metal"},
                "east": {"uv": [4, 14, 12, 16], "texture": "#metal"},
            },
        },
        {
            "from": [6, 2, 6],
            "to": [10, 8, 10],
            "faces": {
                "north": {"uv": [6, 6, 10, 12], "texture": "#metal"},
                "south": {"uv": [6, 6, 10, 12], "texture": "#metal"},
                "west": {"uv": [6, 6, 10, 12], "texture": "#metal"},
                "east": {"uv": [6, 6, 10, 12], "texture": "#metal"},
            },
        },
        {
            "from": [3, 6, 2],
            "to": [13, 14, 5],
            "faces": {
                "down": {"uv": [3, 2, 13, 5], "texture": "#metal"},
                "up": {"uv": [3, 2, 13, 5], "texture": "#metal"},
                "north": {"uv": [2, 2, 14, 14], "texture": "#face", "tintindex": 0},
                "south": {"uv": [3, 2, 13, 14], "texture": "#metal"},
                "west": {"uv": [2, 2, 5, 14], "texture": "#metal"},
                "east": {"uv": [11, 2, 14, 14], "texture": "#metal"},
            },
        },
    ],
}


def tube_center(tex):
    return {
        "parent": "minecraft:block/block",
        "textures": {"metal": f"{MOD}:block/{tex}", "particle": f"{MOD}:block/{tex}"},
        "elements": [
            {
                "from": [6, 6, 6],
                "to": [10, 10, 10],
                "faces": {d: {"uv": [6, 6, 10, 10], "texture": "#metal"} for d in ("down", "up", "north", "south", "west", "east")},
            }
        ],
    }


def tube_arm(tex, end="window"):
    return {
        "parent": "minecraft:block/block",
        "textures": {"metal": f"{MOD}:block/{tex}", "end": f"{MOD}:block/{end}", "particle": f"{MOD}:block/{tex}"},
        "elements": [
            {
                "from": [6, 6, 10],
                "to": [10, 10, 16],
                "faces": {
                    "down": {"uv": [6, 10, 10, 16], "texture": "#metal"},
                    "up": {"uv": [6, 0, 10, 6], "texture": "#metal"},
                    "north": {"uv": [6, 6, 10, 10], "texture": "#metal"},
                    "south": {"uv": [6, 6, 10, 10], "texture": "#end", "cullface": "south"},
                    "west": {"uv": [10, 6, 16, 10], "texture": "#metal"},
                    "east": {"uv": [0, 6, 6, 10], "texture": "#metal"},
                },
            }
        ],
    }


def tube_inventory(tex, end="window"):
    return {
        "parent": "minecraft:block/block",
        "textures": {"metal": f"{MOD}:block/{tex}", "end": f"{MOD}:block/{end}", "particle": f"{MOD}:block/{tex}"},
        "elements": [
            {
                "from": [0, 6, 6],
                "to": [16, 10, 10],
                "faces": {
                    "down": {"uv": [0, 6, 16, 10], "texture": "#metal"},
                    "up": {"uv": [0, 6, 16, 10], "texture": "#metal"},
                    "north": {"uv": [0, 6, 16, 10], "texture": "#metal"},
                    "south": {"uv": [0, 6, 16, 10], "texture": "#metal"},
                    "west": {"uv": [6, 6, 10, 10], "texture": "#end", "cullface": "west"},
                    "east": {"uv": [6, 6, 10, 10], "texture": "#end", "cullface": "east"},
                },
            }
        ],
    }


def tube_multipart(center, arm):
    return {
        "multipart": [
            {"apply": {"model": f"{MOD}:block/{center}"}},
            {"when": {"south": True}, "apply": {"model": f"{MOD}:block/{arm}"}},
            {"when": {"north": True}, "apply": {"model": f"{MOD}:block/{arm}", "y": 180}},
            {"when": {"west": True}, "apply": {"model": f"{MOD}:block/{arm}", "y": 90}},
            {"when": {"east": True}, "apply": {"model": f"{MOD}:block/{arm}", "y": 270}},
            {"when": {"up": True}, "apply": {"model": f"{MOD}:block/{arm}", "x": 270}},
            {"when": {"down": True}, "apply": {"model": f"{MOD}:block/{arm}", "x": 90}},
        ]
    }


# --- Distinct 3D controller models (original, PNC-adjacent; not official PNC copies) ---

DIRS = ("down", "up", "north", "south", "west", "east")


def E(frm, to, tex="#metal", skip=(), cull=(), north=None, up=None, down=None,
      south=None, west=None, east=None, shade=True, rot=None):
    """Axis-aligned model element. Per-face overrides may be a texture name or a dict."""
    overrides = {"north": north, "up": up, "down": down, "south": south, "west": west, "east": east}
    faces = {}
    for d in DIRS:
        if d in skip:
            continue
        o = overrides[d]
        if o is None:
            f = {"texture": tex}
        elif isinstance(o, str):
            f = {"texture": o}
        else:
            f = dict(o)
            f.setdefault("texture", tex)
        if d in cull:
            f["cullface"] = d
        faces[d] = f
    el = {
        "from": [round(float(c), 3) for c in frm],
        "to": [round(float(c), 3) for c in to],
        "faces": faces,
    }
    if not shade:
        el["shade"] = False
    if rot:
        el["rotation"] = rot
    return el


def _R(angle, axis, origin):
    return {"angle": angle, "axis": axis, "origin": [float(c) for c in origin]}


def machine_model(name, grade, elements, extra=None):
    textures = {
        "metal": f"{MOD}:block/machine_side_{grade}",
        "face": f"{MOD}:block/machine_face_{name}",
        "glass": f"{MOD}:block/window",
        "dial": f"{MOD}:block/dial",
        "heat": f"{MOD}:block/hatch_heat",
        "fluid": f"{MOD}:block/hatch_fluid",
        "gas": f"{MOD}:block/hatch_gas",
        "warn": f"{MOD}:block/hatch_item_out",
        "particle": f"{MOD}:block/machine_side_{grade}",
    }
    if extra:
        textures.update(extra)
    return {
        "parent": "minecraft:block/block",
        "textures": textures,
        "elements": elements,
    }


DIAL = {"texture": "#dial", "uv": [0, 0, 16, 16]}
GLASS = {"texture": "#glass", "uv": [0, 0, 16, 16]}


def plinth(x0=1, z0=1, x1=15, z1=15, h=2, tex="#metal"):
    return [E((x0, 0, z0), (x1, h, z1), tex, cull=("down",))]


def feet(inset=1, size=3, h=2, tex="#metal"):
    x1, z1 = 16 - inset, 16 - inset
    return [
        E((inset, 0, inset), (inset + size, h, inset + size), tex, cull=("down",)),
        E((x1 - size, 0, inset), (x1, h, inset + size), tex, cull=("down",)),
        E((inset, 0, z1 - size), (inset + size, h, z1), tex, cull=("down",)),
        E((x1 - size, 0, z1 - size), (x1, h, z1), tex, cull=("down",)),
    ]


def cyl_y(cx, cz, r, y0, y1, tex="#metal", north=None, inset=1):
    """Two overlapping boxes that read as a vertical cylinder."""
    ins = min(inset, max(0, r - 1))
    return [
        E((cx - r, y0, cz - r + ins), (cx + r, y1, cz + r - ins), tex, north=north),
        E((cx - r + ins, y0, cz - r), (cx + r - ins, y1, cz + r), tex),
    ]


def cyl_z(cx, cy, r, z0, z1, tex="#metal", north=None, south=None, inset=1):
    """Cylinder along Z (front-back)."""
    ins = min(inset, max(0, r - 1))
    return [
        E((cx - r, cy - r + ins, z0), (cx + r, cy + r - ins, z1), tex, north=north, south=south),
        E((cx - r + ins, cy - r, z0), (cx + r - ins, cy + r, z1), tex, north=north, south=south),
    ]


def cyl_x(cz, cy, r, x0, x1, tex="#metal", north=None, inset=1):
    """Cylinder along X (west-east)."""
    ins = min(inset, max(0, r - 1))
    return [
        E((x0, cy - r + ins, cz - r), (x1, cy + r - ins, cz + r), tex, north=north),
        E((x0, cy - r, cz - r + ins), (x1, cy + r, cz + r - ins), tex, north=north),
    ]


def flange_z(cx, cy, z, r=3, t=1, tex="#metal"):
    z0, z1 = (z, z + t) if t > 0 else (z + t, z)
    return [E((cx - r, cy - r, z0), (cx + r, cy + r, z1), tex)]


def flange_x(cz, cy, x, r=3, t=1, tex="#metal"):
    x0, x1 = (x, x + t) if t > 0 else (x + t, x)
    return [E((x0, cy - r, cz - r), (x1, cy + r, cz + r), tex)]


def flange_y(cx, cz, y, r=3, t=1, tex="#metal"):
    y0, y1 = (y, y + t) if t > 0 else (y + t, y)
    return [E((cx - r, y0, cz - r), (cx + r, y1, cz + r), tex)]


def pipe_z(cx, cy, z0, z1, r=2, tex="#metal", flanged=False, fr=3):
    els = cyl_z(cx, cy, r, z0, z1, tex)
    if flanged:
        els += flange_z(cx, cy, min(z0, z1), r=fr, t=1, tex=tex)
        els += flange_z(cx, cy, max(z0, z1) - 1, r=fr, t=1, tex=tex)
    return els


def pipe_x(cz, cy, x0, x1, r=2, tex="#metal", flanged=False, fr=3):
    els = cyl_x(cz, cy, r, x0, x1, tex)
    if flanged:
        els += flange_x(cz, cy, min(x0, x1), r=fr, t=1, tex=tex)
        els += flange_x(cz, cy, max(x0, x1) - 1, r=fr, t=1, tex=tex)
    return els


def pipe_y(cx, cz, y0, y1, r=2, tex="#metal", flanged=False, fr=3):
    els = cyl_y(cx, cz, r, y0, y1, tex)
    if flanged:
        els += flange_y(cx, cz, min(y0, y1), r=fr, t=1, tex=tex)
        els += flange_y(cx, cz, max(y0, y1) - 1, r=fr, t=1, tex=tex)
    return els


# ----- individual machines -----

def mdl_impact_crusher():
    els = plinth(0, 0, 16, 16, 2)
    # heavy body
    els.append(E((1, 2, 3), (15, 10, 15), north="#face"))
    # front jaw frame and recessed maw
    els.append(E((1, 2, 1), (15, 10, 3), north="#face"))
    els.append(E((3, 3, 0), (13, 9, 2), north={"texture": "#face", "uv": [3, 7, 13, 13]}))
    # upper teeth
    for x in (3, 6, 9, 12):
        els.append(E((x, 8, 0), (x + 2, 10, 2), "#metal", north="#face"))
    # hopper flare
    els.append(E((3, 10, 4), (13, 13, 14), "#metal"))
    els.append(E((2, 13, 3), (14, 16, 15), "#metal", cull=("up",), up="#face"))
    # side flywheel
    els += cyl_x(9, 7, 3, 15, 16, tex="#metal")
    return els


def mdl_gas_bank():
    els = plinth(0, 2, 16, 14, 2)
    # three separated vertical bottles
    for i, cx in enumerate((3, 8, 13)):
        els += cyl_y(cx, 9, 2, 2, 12, north="#face" if i == 1 else "#gas", inset=1)
        els += flange_y(cx, 9, 11, r=2, t=1)
        els.append(E((cx - 1, 12, 8), (cx + 1, 14, 10), "#metal"))
    # manifold along the back, risers from each valve
    els += pipe_y(3, 12, 12, 15, r=1)
    els += pipe_y(8, 12, 12, 15, r=1)
    els += pipe_y(13, 12, 12, 15, r=1)
    els += pipe_x(12, 15, 2, 14, r=1)
    # small front gauge on the middle bottle
    els.append(E((6, 7, 6), (10, 11, 7), north=DIAL))
    return els

def mdl_residue_kiln():
    els = plinth(1, 1, 15, 15, 2)
    # firebox
    els.append(E((2, 2, 2), (14, 6, 14), north="#heat"))
    els.append(E((5, 3, 1), (11, 5, 3), north="#heat"))  # fire door
    # kettle vat
    els += cyl_y(8, 8, 5, 6, 13, north="#face", inset=1)
    els += flange_y(8, 8, 12, r=6, t=1)  # bolted rim
    # closed lid
    els.append(E((5, 13, 5), (11, 14, 11), "#metal"))
    # rear chimney
    els += cyl_y(8, 12, 2, 13, 16, tex="#metal")
    return els


def mdl_lube_mixer():
    els = feet()
    els += plinth(2, 2, 14, 14, 2)
    # vat
    els += cyl_y(8, 8, 5, 2, 11, north="#face", inset=1)
    els += flange_y(8, 8, 10, r=6, t=1)
    # sight glass
    els.append(E((6, 5, 2), (10, 9, 3), north="#glass"))
    # motor gearbox on top
    els.append(E((5, 11, 5), (11, 14, 11), "#metal", north="#face"))
    # stirrer shaft + bar
    els.append(E((7, 14, 7), (9, 16, 9), "#metal", cull=("up",)))
    els.append(E((4, 15, 7), (12, 16, 9), "#metal"))
    return els


def mdl_flare_stack():
    els = plinth(3, 3, 13, 13, 2)
    els.append(E((4, 2, 4), (12, 5, 12), north="#face"))
    els += pipe_x(8, 3, 0, 4, r=2, flanged=True)
    els += cyl_y(8, 8, 3, 5, 12, north="#face", inset=1)
    els += flange_y(8, 8, 8, r=3, t=1)
    els += flange_y(8, 8, 12, r=4, t=1)
    els += cyl_y(8, 8, 2, 13, 14)
    # wide unshaded flame so it reads in the creative tab
    els.append(E((7, 13, 4), (9, 16, 12), "#heat", north="#heat", south="#heat", west="#warn", east="#warn", shade=False))
    els.append(E((4, 13, 7), (12, 16, 9), "#heat", north="#warn", south="#warn", west="#heat", east="#heat", shade=False))
    return els

def mdl_washer_tower():
    els = plinth(3, 3, 13, 13, 2)
    els += cyl_y(8, 8, 4, 2, 15, north="#face", inset=1)
    for y in (5, 9, 13):
        els += flange_y(8, 8, y, r=5, t=1)
    els.append(E((6, 6, 3), (10, 8, 4), north=GLASS))
    els.append(E((6, 10, 3), (10, 12, 4), north=GLASS))
    els += pipe_x(8, 7, 0, 4, r=2, flanged=True)
    els += pipe_x(8, 11, 12, 16, r=2, flanged=True)
    els += flange_y(8, 8, 15, r=3, t=1)
    return els

def mdl_thermolysis_kettle():
    els = plinth(1, 1, 15, 15, 2)
    els += cyl_y(8, 8, 5, 2, 12, north="#face", inset=1)
    for y in (4, 7, 10):
        els += flange_y(8, 8, y, r=6, t=1, tex="#heat")
    els += flange_y(8, 8, 11, r=6, t=1)
    els.append(E((5, 12, 5), (11, 14, 11), "#metal"))
    els += pipe_y(8, 8, 14, 16, r=1)
    els.append(E((11, 8, 2), (14, 11, 3), north=DIAL))
    return els

def mdl_compressor_column():
    els = plinth(2, 2, 14, 14, 2)
    # motor housing at the base
    els.append(E((3, 2, 3), (13, 6, 13), north="#face"))
    # column with piston rings
    els += cyl_y(8, 8, 4, 6, 13, north="#face", inset=1)
    for y in (7, 10, 12):
        els += flange_y(8, 8, y, r=5, t=1)
    # piston rod and cap
    els.append(E((7, 13, 7), (9, 15, 9), "#metal"))
    els += flange_y(8, 8, 15, r=2, t=1)
    # side ports
    els += pipe_x(8, 8, 0, 4, r=2, flanged=True)
    els += pipe_x(8, 11, 12, 16, r=2, flanged=True)
    return els


def mdl_regulator_skid():
    els = plinth(0, 2, 16, 14, 3)
    els += cyl_x(8, 8, 3, 3, 13, north="#face")
    els += flange_x(8, 8, 3, r=4, t=1)
    els += flange_x(8, 8, 12, r=4, t=1)
    els += pipe_x(8, 8, 0, 3, r=2, flanged=True)
    els += pipe_x(8, 8, 13, 16, r=2, flanged=True)
    # large front gauge, full dial texture
    els.append(E((4, 4, 1), (12, 12, 3), north=DIAL))
    els.append(E((6, 6, 0), (10, 10, 1), north=DIAL))
    # chunky handwheel on the east
    els.append(E((13, 6, 7), (16, 10, 9), "#metal"))
    els.append(E((14, 5, 5), (16, 11, 11), "#metal"))
    els.append(E((15, 7, 4), (16, 9, 12), "#metal"))
    return els

def mdl_cyclone():
    els = plinth(4, 4, 12, 12, 1)
    # dipleg
    els += cyl_y(8, 8, 2, 1, 4)
    # stepped cone
    els += cyl_y(8, 8, 3, 4, 7, inset=1)
    els += cyl_y(8, 8, 4, 7, 10, north="#face", inset=1)
    # upper barrel
    els += cyl_y(8, 8, 5, 10, 14, north="#face", inset=1)
    els += flange_y(8, 8, 14, r=5, t=1)
    # vortex finder
    els += cyl_y(8, 8, 2, 14, 16)
    # tangential inlet on the west
    els += pipe_x(8, 12, 0, 4, r=2, flanged=True)
    els.append(E((3, 10, 6), (5, 14, 10), "#metal"))  # scroll housing
    return els


def mdl_item_gallery():
    els = plinth(0, 2, 16, 14, 2)
    els.append(E((1, 2, 3), (15, 13, 14), north="#face"))
    for x in (2, 6, 10):
        els.append(E((x, 4, 1), (x + 4, 11, 3), north="#face"))
        els.append(E((x + 1, 5, 0), (x + 3, 10, 2), north={"texture": "#face", "uv": [x, 6, x + 2, 12]}))
    els.append(E((1, 13, 3), (15, 15, 7), "#metal"))
    els += pipe_x(12, 3, 0, 16, r=1)
    return els

def mdl_fraction_tower():
    els = plinth(3, 3, 13, 13, 2)
    els += cyl_y(8, 8, 3, 2, 11, north="#face", inset=1)
    for y in (4, 7, 10):
        els += flange_y(8, 8, y, r=4, t=1)
    for y in (5, 8):
        els.append(E((6, y, 4), (10, y + 2, 5), north=GLASS))
    # big condenser bulb on top — the silhouette difference vs other towers
    els += cyl_y(8, 8, 5, 11, 15, inset=1)
    els += flange_y(8, 8, 15, r=2, t=1)
    els += pipe_x(8, 13, 12, 16, r=1, flanged=True)
    els += pipe_x(8, 6, 0, 5, r=2, flanged=True)
    return els

def mdl_cracker():
    # horizontal tube furnace
    els = plinth(0, 1, 16, 15, 2)
    # firebox
    els.append(E((1, 2, 2), (15, 6, 14), north="#heat"))
    els.append(E((5, 3, 1), (11, 5, 3), north="#heat"))
    # process tubes running front-to-back, raised
    for x in (3, 7, 11):
        els += cyl_z(x + 1, 9, 2, 2, 14, north="#face")
    # tube support frame
    els.append(E((2, 6, 3), (14, 7, 5), "#metal"))
    els.append(E((2, 6, 11), (14, 7, 13), "#metal"))
    # rear stack
    els += cyl_y(8, 13, 2, 10, 16)
    return els


def mdl_electrolyzer():
    els = plinth(1, 1, 15, 15, 2)
    els.append(E((2, 2, 3), (14, 10, 14), north="#face"))
    els.append(E((4, 4, 2), (12, 9, 3), north=GLASS))
    els.append(E((5, 10, 6), (7, 16, 8), "#metal", cull=("up",), up="#face"))
    els.append(E((9, 10, 6), (11, 16, 8), "#metal", cull=("up",), up="#face"))
    els.append(E((5, 14, 6), (11, 16, 8), "#metal"))
    els += pipe_x(8, 6, 0, 2, r=1, flanged=True)
    return els

def mdl_polymerizer():
    els = plinth(1, 1, 15, 15, 2)
    els += cyl_y(8, 8, 5, 2, 11, north="#face", inset=1)
    els += flange_y(8, 8, 10, r=6, t=1)
    # jacket sight
    els.append(E((6, 5, 2), (10, 8, 3), north="#glass"))
    # hopper / funnel on top
    els.append(E((5, 11, 5), (11, 13, 11), "#metal"))
    els.append(E((6, 13, 6), (10, 15, 10), "#metal"))
    els.append(E((7, 15, 7), (9, 16, 9), "#metal", cull=("up",)))
    # side discharge
    els += pipe_x(8, 4, 13, 16, r=2, flanged=True)
    return els


def mdl_leach_line():
    els = plinth(0, 2, 16, 14, 2)
    els.append(E((0, 2, 3), (16, 8, 13), north="#face", skip=("up",)))
    els.append(E((1, 3, 4), (15, 6, 12), "#fluid", up="#fluid", north=GLASS))
    els.append(E((0, 8, 3), (16, 9, 5), "#metal"))
    els.append(E((0, 8, 11), (16, 9, 13), "#metal"))
    els.append(E((0, 8, 5), (2, 9, 11), "#metal"))
    els.append(E((14, 8, 5), (16, 9, 11), "#metal"))
    els.append(E((12, 9, 4), (16, 12, 12), "#metal", north="#face"))
    els += pipe_x(8, 5, 0, 1, r=2)
    els += flange_x(8, 5, 0, r=3, t=1)
    return els

def mdl_alloy_kettle():
    els = feet(inset=2, size=3, h=2)
    els += flange_y(8, 8, 2, r=6, t=1)
    els += cyl_y(8, 8, 5, 3, 12, north="#face", inset=1)
    els += flange_y(8, 8, 11, r=6, t=1)
    # open glowing melt, visible from the top and through the pouring lip
    els.append(E((5, 6, 5), (11, 11, 11), "#heat", up="#heat", shade=False))
    els.append(E((6, 7, 2), (10, 11, 5), "#heat", north="#heat", up="#heat", shade=False))
    els.append(E((6, 10, 1), (10, 12, 4), "#metal", north="#face"))
    return els

def mdl_sinter_kiln():
    els = plinth(1, 1, 15, 15, 2)
    # box kiln
    els.append(E((2, 2, 3), (14, 13, 15), north="#face"))
    # arched door recess
    els.append(E((4, 3, 1), (12, 11, 3), north="#face"))
    els.append(E((5, 4, 0), (11, 10, 2), north="#heat"))
    # door arch cap
    els.append(E((4, 11, 1), (12, 13, 3), "#metal"))
    # short chimney at the back
    els += cyl_y(8, 13, 2, 13, 16)
    return els


def mdl_quench_well():
    els = plinth(1, 1, 15, 15, 2)
    els.append(E((2, 2, 2), (14, 8, 14), north="#face", skip=("up",)))
    els.append(E((3, 3, 3), (13, 6, 13), "#fluid", up=GLASS, north=GLASS))
    els.append(E((2, 8, 2), (14, 9, 4), "#metal"))
    els.append(E((2, 8, 12), (14, 9, 14), "#metal"))
    els.append(E((2, 8, 4), (4, 9, 12), "#metal"))
    els.append(E((12, 8, 4), (14, 9, 12), "#metal"))
    els.append(E((7, 12, 4), (9, 14, 14), "#metal"))
    els.append(E((6, 14, 6), (10, 16, 10), "#metal", cull=("up",)))
    els += pipe_y(8, 8, 9, 12, r=1)
    els.append(E((7, 8, 7), (9, 9, 9), "#metal"))
    return els

def mdl_heat_gallery():
    els = plinth(0, 3, 16, 13, 2)
    # horizontal shell
    els += cyl_x(8, 8, 5, 1, 15, north="#face", inset=1)
    # channel heads / headers
    els += flange_x(8, 8, 1, r=6, t=2)
    els += flange_x(8, 8, 13, r=6, t=2)
    # tube-end plate on the front (north)
    els.append(E((3, 4, 2), (13, 12, 3), north="#face"))
    for x, y in ((5, 6), (8, 6), (11, 6), (5, 9), (8, 9), (11, 9)):
        els.append(E((x - 1, y - 1, 1), (x + 1, y + 1, 3), "#heat"))
    # end pipes
    els += pipe_x(8, 8, 0, 1, r=2)
    els += pipe_x(8, 8, 15, 16, r=2)
    return els


def mdl_scrubber_tower():
    els = plinth(2, 2, 14, 14, 2)
    els += cyl_y(8, 8, 4, 2, 6, north="#face", inset=1)
    els += cyl_y(8, 8, 6, 6, 12, north="#face", inset=1)
    els += flange_y(8, 8, 6, r=6, t=1)
    els += flange_y(8, 8, 11, r=6, t=1)
    els.append(E((6, 7, 2), (10, 10, 3), north=GLASS))
    els += cyl_y(8, 8, 3, 12, 14)
    els += flange_y(8, 8, 14, r=5, t=1)
    els.append(E((6, 15, 6), (10, 16, 10), "#metal", cull=("up",)))
    els += pipe_x(8, 4, 0, 4, r=2, flanged=True)
    return els

def mdl_expander_column():
    els = plinth(2, 2, 14, 14, 2)
    els += cyl_y(8, 8, 4, 2, 7, north="#face", inset=1)
    els += cyl_y(8, 8, 6, 7, 11, north="#face", inset=1)
    els += flange_y(8, 8, 7, r=6, t=1)
    els += flange_y(8, 8, 10, r=6, t=1)
    els += cyl_y(8, 8, 3, 11, 15, inset=1)
    els += flange_y(8, 8, 15, r=2, t=1)
    els.append(E((6, 8, 1), (10, 10, 3), north=GLASS))
    els += pipe_x(8, 9, 12, 16, r=2, flanged=True, tex="#gas")
    els += pipe_x(8, 4, 0, 4, r=2, flanged=True)
    return els

def mdl_cryo_tower():
    els = plinth(2, 2, 14, 14, 2)
    els += cyl_y(8, 8, 5, 2, 14, north="#face", inset=1)
    for y in (4, 8, 12):
        els += flange_y(8, 8, y, r=6, t=1)
    els.append(E((5, 6, 2), (11, 11, 4), north=GLASS))
    els += cyl_y(8, 8, 3, 14, 16, inset=1)
    els.append(E((6, 15, 6), (10, 16, 10), "#gas", cull=("up",)))
    els += pipe_x(8, 5, 0, 3, r=2, flanged=True)
    return els

def mdl_lamination_gallery():
    els = plinth(0, 1, 16, 15, 2)
    # open side frames so the rollers read from the north
    els.append(E((0, 2, 2), (2, 14, 14), "#metal", north="#face"))
    els.append(E((14, 2, 2), (16, 14, 14), "#metal", north="#face"))
    els.append(E((0, 13, 2), (16, 15, 14), "#metal"))
    # two large rollers; the lower one comes forward
    els += cyl_x(5, 6, 4, 2, 14, north="#face", inset=1)
    els += cyl_x(10, 11, 4, 2, 14, north="#face", inset=1)
    # sheet running through the mill, protruding at the front
    els.append(E((2, 8, 0), (14, 10, 16), "#face", north="#face"))
    return els

def mdl_clean_room():
    # boxy chamber, but with a door, window, hazard plinth and rooftop filter
    els = plinth(1, 1, 15, 15, 2)
    # hazard stripe base uses the warn texture
    els.append(E((1, 2, 1), (15, 4, 15), "#warn", north="#warn"))
    # chamber
    els.append(E((2, 4, 3), (14, 13, 15), north="#face"))
    # door recess + window
    els.append(E((4, 4, 1), (12, 13, 3), north="#face"))
    els.append(E((6, 7, 0), (10, 11, 2), north="#glass"))
    # rooftop HEPA / filter box
    els.append(E((4, 13, 5), (12, 16, 13), "#metal", cull=("up",), north="#face"))
    els.append(E((6, 15, 7), (10, 16, 11), "#gas", cull=("up",)))
    return els


def mdl_cycle_island():
    els = plinth(2, 2, 14, 14, 3)
    # pump body in the middle
    els.append(E((5, 3, 5), (11, 10, 11), north="#face"))
    # a single, readable rectangular pipe loop standing in front of the pump
    els.append(E((2, 4, 3), (14, 6, 5), "#gas"))
    els.append(E((2, 12, 3), (14, 14, 5), "#gas"))
    els.append(E((2, 4, 3), (4, 14, 5), "#gas"))
    els.append(E((12, 4, 3), (14, 14, 5), "#gas"))
    # short connectors from the pump to the loop
    els.append(E((7, 6, 3), (9, 8, 6), "#metal"))
    return els

def mdl_charging_station():
    # PNC-adjacent charging bench: low deck, backboard, two contact posts
    els = plinth(1, 2, 15, 15, 2)
    # deck / pad
    els.append(E((2, 2, 3), (14, 6, 14), north="#face", up="#face"))
    # backboard
    els.append(E((2, 6, 12), (14, 15, 15), north="#face", cull=("south",)))
    # two charging posts
    els.append(E((4, 6, 5), (6, 13, 7), "#metal", up="#gas"))
    els.append(E((10, 6, 5), (12, 13, 7), "#metal", up="#gas"))
    # contact caps
    els.append(E((3, 13, 4), (7, 15, 8), "#gas"))
    els.append(E((9, 13, 4), (13, 15, 8), "#gas"))
    # front lip
    els.append(E((2, 2, 2), (14, 4, 3), north="#face"))
    return els


def mdl_drone_nest():
    els = plinth(1, 1, 15, 15, 2)
    # wide octagonal landing pad
    els.append(E((2, 2, 4), (14, 4, 12), "#metal", north="#face"))
    els.append(E((4, 2, 2), (12, 4, 14), "#metal"))
    # recessed nest
    els.append(E((5, 3, 5), (11, 5, 11), north="#face", up="#gas"))
    # short corner guides
    for x, z in ((2, 2), (12, 2), (2, 12), (12, 12)):
        els.append(E((x, 4, z), (x + 2, 7, z + 2), "#metal"))
    # modest mast
    els.append(E((7, 5, 7), (9, 12, 9), "#metal"))
    els.append(E((6, 12, 6), (10, 14, 10), "#gas"))
    return els

def mdl_critical_kettle():
    els = plinth(1, 1, 15, 15, 2)
    els += cyl_y(8, 8, 5, 2, 12, north="#face", inset=1)
    # containment hoops
    for y in (4, 8, 11):
        els += flange_y(8, 8, y, r=6, t=1)
    # glowing core viewport
    els.append(E((5, 5, 2), (11, 10, 4), north="#glass", shade=False))
    els.append(E((6, 6, 3), (10, 9, 5), "#gas", shade=False))
    # top cap / lid
    els += flange_y(8, 8, 12, r=5, t=1)
    els.append(E((6, 13, 6), (10, 15, 10), "#metal"))
    els.append(E((7, 15, 7), (9, 16, 9), "#gas", cull=("up",)))
    return els


def mdl_calibration_bench():
    # solid end-panel legs, not skinny stilts
    els = [
        E((1, 0, 2), (3, 7, 14), "#metal", cull=("down",)),
        E((13, 0, 2), (15, 7, 14), "#metal", cull=("down",)),
        E((1, 7, 1), (15, 9, 15), north="#face", up="#face"),
    ]
    # back instrument rack
    els.append(E((2, 9, 11), (14, 16, 15), north="#face", cull=("south",)))
    # two flush dials on the backboard
    els.append(E((3, 10, 10), (7, 15, 12), north=DIAL))
    els.append(E((9, 10, 10), (13, 15, 12), north=DIAL))
    # workpiece on the bench
    els.append(E((6, 9, 4), (10, 11, 8), "#metal", north="#face"))
    return els

def mdl_fluid_loop():
    els = plinth(1, 1, 15, 15, 2)
    # horizontal square-section loop on the deck
    els.append(E((2, 2, 2), (14, 5, 4), "#fluid"))
    els.append(E((2, 2, 12), (14, 5, 14), "#fluid"))
    els.append(E((2, 2, 2), (4, 5, 14), "#fluid"))
    els.append(E((12, 2, 2), (14, 5, 14), "#fluid"))
    # vertical loop standing at the back
    els.append(E((3, 5, 10), (13, 7, 13), "#gas"))
    els.append(E((3, 13, 10), (13, 16, 13), "#gas", cull=("up",)))
    els.append(E((3, 5, 10), (5, 16, 13), "#gas"))
    els.append(E((11, 5, 10), (13, 16, 13), "#gas"))
    # pump body
    els.append(E((6, 5, 5), (10, 12, 10), north="#face"))
    return els

def mdl_reducer():
    # front (north) is the narrow outlet; back is the wide inlet
    els = plinth(2, 3, 14, 13, 3)
    els += flange_z(8, 8, 14, r=6, t=2)
    els += cyl_z(8, 8, 5, 10, 14, south="#face", inset=1)
    els += cyl_z(8, 8, 3, 5, 10, inset=1)
    els += cyl_z(8, 8, 2, 1, 5, north="#face")
    els += flange_z(8, 8, 0, r=3, t=1)
    els.append(E((6, 13, 11), (10, 16, 15), north=DIAL, cull=("up",)))
    return els

def mdl_booster():
    els = plinth(2, 3, 14, 13, 3)
    # motor housing at the back
    els += cyl_z(8, 8, 4, 10, 16, inset=1)
    els += flange_z(8, 8, 15, r=5, t=1)
    els += cyl_z(8, 8, 3, 8, 10)
    # wider volute toward the front outlet
    els += cyl_z(8, 8, 6, 3, 8, north="#face", inset=1)
    els += flange_z(8, 8, 3, r=6, t=1)
    els += cyl_z(8, 8, 3, 0, 3, north="#face")
    els += flange_z(8, 8, 0, r=4, t=1)
    # side flywheel, attached to the motor
    els += cyl_x(8, 8, 3, 0, 2)
    els.append(E((11, 11, 4), (15, 15, 8), north=DIAL))
    return els

def mdl_relief_valve():
    # compact vertical valve, not a cabinet
    els = plinth(4, 4, 12, 12, 2)
    # horizontal run-through pipe (front-back)
    els += pipe_z(8, 5, 0, 16, r=2, flanged=True, fr=3)
    # valve body
    els += cyl_y(8, 8, 3, 5, 11, north="#face", inset=1)
    els += flange_y(8, 8, 10, r=4, t=1)
    # spring housing (banded)
    els += cyl_y(8, 8, 2, 11, 13)
    els.append(E((6, 11, 6), (10, 12, 10), "#metal"))
    els.append(E((6, 13, 6), (10, 14, 10), "#metal"))
    # red cap
    els.append(E((6, 14, 6), (10, 16, 10), "#warn", cull=("up",), up="#warn", north="#warn"))
    return els


CONTROLLER_MODEL_BUILDERS = {
    "impact_crusher": mdl_impact_crusher,
    "gas_bank": mdl_gas_bank,
    "residue_kiln": mdl_residue_kiln,
    "lube_mixer": mdl_lube_mixer,
    "flare_stack": mdl_flare_stack,
    "washer_tower": mdl_washer_tower,
    "thermolysis_kettle": mdl_thermolysis_kettle,
    "compressor_column": mdl_compressor_column,
    "regulator_skid": mdl_regulator_skid,
    "cyclone": mdl_cyclone,
    "item_gallery": mdl_item_gallery,
    "fraction_tower": mdl_fraction_tower,
    "cracker": mdl_cracker,
    "electrolyzer": mdl_electrolyzer,
    "polymerizer": mdl_polymerizer,
    "leach_line": mdl_leach_line,
    "alloy_kettle": mdl_alloy_kettle,
    "sinter_kiln": mdl_sinter_kiln,
    "quench_well": mdl_quench_well,
    "heat_gallery": mdl_heat_gallery,
    "scrubber_tower": mdl_scrubber_tower,
    "expander_column": mdl_expander_column,
    "cryo_tower": mdl_cryo_tower,
    "lamination_gallery": mdl_lamination_gallery,
    "clean_room": mdl_clean_room,
    "cycle_island": mdl_cycle_island,
    "charging_station": mdl_charging_station,
    "drone_nest": mdl_drone_nest,
    "critical_kettle": mdl_critical_kettle,
    "calibration_bench": mdl_calibration_bench,
    "fluid_loop": mdl_fluid_loop,
    "reducer": mdl_reducer,
    "booster": mdl_booster,
    "relief_valve": mdl_relief_valve,
}


def machine_bottom(pal):
    return wall_plate(seed=_grade_seed(pal) + 20, recess=False, pal=pal)


def machine_top(pal, name):
    fn = TOP_DRAWERS.get(name)
    return fn(pal) if fn else _top_default(pal)


def _top_default(pal, seed=200):
    im = wall_plate(seed=_grade_seed(pal) + seed, recess=True, pal=pal)
    port(im, 8, 8, 3)
    return im


def _top_impact_crusher(pal):
    im = wall_plate(seed=501, recess=True, pal=pal)
    for y in range(6, 11):
        for x in range(4, 12):
            pset(im, x, y, D0 if y in (6, 10) or x in (4, 11) else D1)
    return im


def _top_gas_bank(pal):
    im = wall_plate(seed=511, recess=True, pal=pal)
    for x in (4, 8, 12):
        port(im, x, 8, 2)
    return im


def _top_residue_kiln(pal):
    im = wall_plate(seed=521, recess=True, pal=pal)
    for y in (5, 8, 11):
        for x in range(4, 12):
            pset(im, x, y, D0)
            pset(im, x, y + 1, D2)
    pset(im, 8, 10, EMBER2)
    return im


def _top_lube_mixer(pal):
    im = wall_plate(seed=531, recess=True, pal=pal)
    port(im, 8, 8, 4)
    pset(im, 8, 8, D1)
    return im


def _top_thermolysis_kettle(pal):
    return _top_default(pal, 541)


def _top_electrolyzer(pal):
    im = wall_plate(seed=551, recess=True, pal=pal)
    port(im, 5, 8, 2)
    port(im, 11, 8, 2)
    return im


def _top_polymerizer(pal):
    im = wall_plate(seed=561, recess=True, pal=pal)
    port(im, 8, 9, 3)
    for y in range(3, 6):
        for x in range(6, 10):
            pset(im, x, y, D1 if x in (6, 9) else D2)
    return im


def _top_alloy_kettle(pal):
    im = _top_default(pal, 571)
    pset(im, 7, 8, WARM)
    pset(im, 8, 8, D2)
    return im


def _top_sinter_kiln(pal):
    im = wall_plate(seed=581, recess=True, pal=pal)
    port(im, 8, 8, 3)
    pset(im, 7, 8, EMBER2)
    pset(im, 8, 8, EMBER)
    return im


def _top_critical_kettle(pal):
    im = wall_plate(seed=591, recess=True, pal=pal)
    grey_window(im, 5, 5, 6, 6, glass=D0)
    pset(im, 7, 7, PALE)
    pset(im, 8, 7, D3)
    return im


def _top_washer_tower(pal):
    return _top_default(pal, 601)


def _top_fraction_tower(pal):
    im = wall_plate(seed=611, recess=True, pal=pal)
    port(im, 8, 8, 3)
    port(im, 4, 8, 1)
    port(im, 12, 8, 1)
    return im


def _top_scrubber_tower(pal):
    return _top_default(pal, 621)


def _top_cryo_tower(pal):
    im = wall_plate(seed=631, recess=True, pal=pal)
    grey_window(im, 5, 5, 6, 6, glass=D0)
    pset(im, 5, 5, FROST)
    pset(im, 10, 10, FROST)
    return im


def _top_compressor_column(pal):
    return _top_default(pal, 641)


def _top_expander_column(pal):
    im = _top_default(pal, 651)
    pset(im, 8, 8, FROST)
    return im


def _top_cyclone(pal):
    im = wall_plate(seed=661, recess=True, pal=pal)
    port(im, 8, 8, 5)
    return im


def _top_item_gallery(pal):
    im = wall_plate(seed=671, recess=True, pal=pal)
    for x in (4, 8, 12):
        for y in range(5, 12):
            pset(im, x, y, D0)
            pset(im, x + 1 if x != 12 else x - 1, y, D1)
    return im


def _top_cracker(pal):
    im = wall_plate(seed=681, recess=True, pal=pal)
    for y in (5, 8, 11):
        for x in range(2, 14):
            pset(im, x, y, D0)
            pset(im, x, y + 1, D2)
    return im


def _top_leach_line(pal):
    im = wall_plate(seed=691, recess=True, pal=pal)
    for y in range(6, 11):
        for x in range(2, 14):
            pset(im, x, y, D0 if y in (6, 10) or x in (2, 13) else D1)
    return im


def _top_lamination_gallery(pal):
    im = wall_plate(seed=701, recess=True, pal=pal)
    for y0 in (4, 9):
        for y in range(y0, y0 + 3):
            for x in range(2, 14):
                pset(im, x, y, D0 if y in (y0, y0 + 2) else M1)
    return im


def _top_heat_gallery(pal):
    im = wall_plate(seed=711, recess=True, pal=pal)
    for x, y in ((4, 5), (8, 5), (12, 5), (4, 11), (8, 11), (12, 11)):
        port(im, x, y, 2)
    pset(im, 8, 11, EMBER2)
    return im


def _top_flare_stack(pal):
    im = wall_plate(seed=721, recess=True, pal=pal)
    port(im, 8, 8, 4)
    pset(im, 7, 7, EMBER2)
    pset(im, 8, 7, EMBER)
    return im


def _top_quench_well(pal):
    im = wall_plate(seed=731, recess=True, pal=pal)
    for y in range(4, 12):
        for x in range(4, 12):
            pset(im, x, y, D0 if min(x - 4, y - 4, 11 - x, 11 - y) == 0 else D1)
    return im


def _top_clean_room(pal):
    im = wall_plate(seed=741, recess=False, pal=pal)
    grey_window(im, 4, 4, 8, 8, glass=D0)
    return im


def _top_charging_station(pal):
    im = wall_plate(seed=751, recess=True, pal=pal)
    for y in range(5, 12):
        for x in range(4, 12):
            pset(im, x, y, D1 if x in (4, 11) or y in (5, 11) else D0)
    pset(im, 7, 8, G1)
    pset(im, 8, 8, G0)
    return im


def _top_drone_nest(pal):
    im = wall_plate(seed=761, recess=True, pal=pal)
    port(im, 8, 8, 5)
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        quiet_rivet(im, x, y)
    return im


def _top_cycle_island(pal):
    im = wall_plate(seed=771, recess=True, pal=pal)
    for y in range(3, 13):
        for x in range(3, 13):
            if x in (3, 12) or y in (3, 12):
                pset(im, x, y, D2)
            elif not (6 <= x <= 9 and 6 <= y <= 9):
                pset(im, x, y, M0)
    port(im, 8, 8, 2)
    return im


def _top_fluid_loop(pal):
    im = wall_plate(seed=781, recess=True, pal=pal)
    for x in range(3, 13):
        pset(im, x, 4, D1)
        pset(im, x, 5, D0)
        pset(im, x, 10, D1)
        pset(im, x, 11, D0)
    for y in range(4, 12):
        pset(im, 3, y, D1)
        pset(im, 12, y, D1)
    port(im, 8, 8, 2)
    return im


def _top_regulator_skid(pal):
    im = wall_plate(seed=791, recess=True, pal=pal)
    port(im, 8, 8, 5)
    for dx, dy in ((0, -3), (3, 0), (0, 3), (-3, 0)):
        pset(im, 8 + dx, 8 + dy, G1)
    return im


def _top_calibration_bench(pal):
    im = wall_plate(seed=801, recess=True, pal=pal)
    port(im, 5, 8, 3)
    port(im, 11, 8, 3)
    return im


TOP_DRAWERS = {
    "impact_crusher": _top_impact_crusher,
    "gas_bank": _top_gas_bank,
    "residue_kiln": _top_residue_kiln,
    "lube_mixer": _top_lube_mixer,
    "thermolysis_kettle": _top_thermolysis_kettle,
    "electrolyzer": _top_electrolyzer,
    "polymerizer": _top_polymerizer,
    "alloy_kettle": _top_alloy_kettle,
    "sinter_kiln": _top_sinter_kiln,
    "critical_kettle": _top_critical_kettle,
    "washer_tower": _top_washer_tower,
    "fraction_tower": _top_fraction_tower,
    "scrubber_tower": _top_scrubber_tower,
    "cryo_tower": _top_cryo_tower,
    "compressor_column": _top_compressor_column,
    "expander_column": _top_expander_column,
    "cyclone": _top_cyclone,
    "item_gallery": _top_item_gallery,
    "cracker": _top_cracker,
    "leach_line": _top_leach_line,
    "lamination_gallery": _top_lamination_gallery,
    "heat_gallery": _top_heat_gallery,
    "flare_stack": _top_flare_stack,
    "quench_well": _top_quench_well,
    "clean_room": _top_clean_room,
    "charging_station": _top_charging_station,
    "drone_nest": _top_drone_nest,
    "cycle_island": _top_cycle_island,
    "fluid_loop": _top_fluid_loop,
    "regulator_skid": _top_regulator_skid,
    "calibration_bench": _top_calibration_bench,
}


def machine_side_named(pal, name):
    fn = SIDE_DRAWERS.get(name)
    if fn:
        return fn(pal)
    return machine_side(pal)


def _side_vents(pal, seed=71):
    im = wall_plate(seed=_grade_seed(pal) + seed, recess=True, pal=pal)
    for y in (5, 8, 11):
        for x in range(4, 12):
            pset(im, x, y, D0)
            pset(im, x, y + 1, D3)
    return im


def _side_port(pal, seed=81, r=3, cx=8, cy=8):
    im = wall_plate(seed=_grade_seed(pal) + seed, recess=True, pal=pal)
    port(im, cx, cy, r)
    return im


def _side_window(pal, seed=91):
    im = wall_plate(seed=_grade_seed(pal) + seed, recess=True, pal=pal)
    grey_window(im, 5, 5, 6, 6, glass=D0)
    return im


def _side_pipes(pal, seed=101):
    im = wall_plate(seed=_grade_seed(pal) + seed, recess=True, pal=pal)
    for y in (5, 10):
        for x in range(1, 15):
            pset(im, x, y, D0)
            pset(im, x, y + 1, D2)
            pset(im, x, y + 2, D1)
    return im


def _side_cryo(pal):
    im = wall_plate(seed=_grade_seed(pal) + 111, recess=True, pal=pal)
    grey_window(im, 5, 5, 6, 6, glass=D0)
    pset(im, 5, 5, FROST)
    pset(im, 10, 10, FROST)
    return im


def _side_kiln(pal):
    im = _side_vents(pal, 121)
    pset(im, 8, 12, EMBER2)
    return im


def _side_flare(pal):
    im = _side_port(pal, 131, r=2, cx=8, cy=11)
    for y in (5, 8):
        pset(im, 3, y, WARM)
        pset(im, 4, y, WARM)
    return im


def _side_heat(pal):
    im = _side_pipes(pal, 141)
    pset(im, 8, 11, EMBER2)
    return im


def _side_regulator(pal):
    im = wall_plate(seed=_grade_seed(pal) + 151, recess=True, pal=pal)
    port(im, 8, 8, 4)
    pset(im, 9, 6, G1)
    return im


def _side_calibration(pal):
    im = wall_plate(seed=_grade_seed(pal) + 161, recess=True, pal=pal)
    port(im, 5, 8, 2)
    port(im, 11, 8, 2)
    return im


SIDE_DRAWERS = {
    "impact_crusher": lambda pal: _side_port(pal, 201, r=3),
    "gas_bank": lambda pal: _side_port(pal, 202, r=3),
    "residue_kiln": _side_kiln,
    "lube_mixer": lambda pal: _side_window(pal, 204),
    "thermolysis_kettle": lambda pal: _side_window(pal, 205),
    "electrolyzer": lambda pal: _side_window(pal, 206),
    "polymerizer": lambda pal: _side_window(pal, 207),
    "alloy_kettle": lambda pal: _side_window(pal, 208),
    "sinter_kiln": _side_kiln,
    "critical_kettle": lambda pal: _side_window(pal, 210),
    "washer_tower": lambda pal: _side_pipes(pal, 211),
    "fraction_tower": lambda pal: _side_pipes(pal, 212),
    "scrubber_tower": lambda pal: _side_pipes(pal, 213),
    "cryo_tower": _side_cryo,
    "compressor_column": lambda pal: _side_port(pal, 215, r=4),
    "expander_column": lambda pal: _side_port(pal, 216, r=4),
    "cyclone": lambda pal: _side_port(pal, 217, r=3),
    "item_gallery": lambda pal: _side_port(pal, 218, r=2),
    "cracker": lambda pal: _side_pipes(pal, 219),
    "leach_line": lambda pal: _side_window(pal, 220),
    "lamination_gallery": lambda pal: _side_pipes(pal, 221),
    "heat_gallery": _side_heat,
    "flare_stack": _side_flare,
    "quench_well": lambda pal: _side_window(pal, 224),
    "clean_room": lambda pal: _side_window(pal, 225),
    "charging_station": lambda pal: _side_port(pal, 226, r=3),
    "drone_nest": lambda pal: _side_port(pal, 227, r=4),
    "cycle_island": lambda pal: _side_pipes(pal, 228),
    "fluid_loop": lambda pal: _side_pipes(pal, 229),
    "regulator_skid": _side_regulator,
    "calibration_bench": _side_calibration,
}

def cube_controller(name, grade):
    faces = {}
    for d in ("down", "up", "north", "south", "west", "east"):
        faces[d] = {"uv": [0, 0, 16, 16], "texture": f"#{d}", "cullface": d}
    return {
        "parent": "minecraft:block/block",
        "textures": {
            "particle": f"{MOD}:block/machine_face_{name}",
            "north": f"{MOD}:block/machine_face_{name}",
            "south": f"{MOD}:block/machine_side_{name}",
            "east": f"{MOD}:block/machine_side_{name}",
            "west": f"{MOD}:block/machine_side_{name}",
            "up": f"{MOD}:block/machine_top_{name}",
            "down": f"{MOD}:block/machine_bottom_{grade}",
        },
        "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}],
    }

def controller_model(name, grade, _family=None):
    # Apparati are full 16x16x16 cubes with unique face/top/side textures.
    # Reducer, booster and relief_valve are not multiblock controllers and keep specialized models.
    if name in ("reducer", "booster", "relief_valve"):
        builder = CONTROLLER_MODEL_BUILDERS[name]
        return machine_model(name, grade, builder())
    return cube_controller(name, grade)

def generate_models():
    write_json(BLOCK_MODELS / "shape_controller.json", SHAPE_CONTROLLER)
    write_json(BLOCK_MODELS / "shape_hatch.json", SHAPE_HATCH)
    write_json(BLOCK_MODELS / "shape_gauge.json", SHAPE_GAUGE)
    write_json(BLOCK_MODELS / "tube_center.json", tube_center("tube"))
    write_json(BLOCK_MODELS / "tube_arm.json", tube_arm("tube"))
    write_json(BLOCK_MODELS / "tube_inventory.json", tube_inventory("tube"))
    write_json(BLOCK_MODELS / "cryo_tube_center.json", tube_center("tube_cryo"))
    write_json(BLOCK_MODELS / "cryo_tube_arm.json", tube_arm("tube_cryo"))
    write_json(BLOCK_MODELS / "cryo_tube_inventory.json", tube_inventory("tube_cryo"))

    for grade in GRADES:
        write_json(BLOCK_MODELS / f"casing_{grade}.json", connected_casing(grade))
        write_json(BLOCKSTATES / f"casing_{grade}.json", simple_state(f"casing_{grade}"))
        write_json(ITEM_MODELS / f"casing_{grade}.json", {"parent": f"{MOD}:block/casing_{grade}"})

    for name, (grade, family) in CONTROLLERS.items():
        write_json(BLOCK_MODELS / f"{name}.json", controller_model(name, grade, family))
        write_json(BLOCKSTATES / f"{name}.json", facing_state(name))
        write_json(ITEM_MODELS / f"{name}.json", {"parent": f"{MOD}:block/{name}"})

    for hatch in HATCHES:
        write_json(BLOCK_MODELS / f"hatch_{hatch}.json", {
            "parent": f"{MOD}:block/shape_hatch",
            "textures": {
                "metal": f"{MOD}:block/machine_side_g2",
                "face": f"{MOD}:block/hatch_{hatch}",
                "particle": f"{MOD}:block/hatch_{hatch}",
            },
        })
        write_json(BLOCKSTATES / f"hatch_{hatch}.json", facing_all_state(f"hatch_{hatch}"))
        write_json(ITEM_MODELS / f"hatch_{hatch}.json", {"parent": f"{MOD}:block/hatch_{hatch}"})

    for part in PARTS:
        write_json(BLOCK_MODELS / f"part_{part}.json", cube(f"part_{part}"))
        write_json(BLOCKSTATES / f"part_{part}.json", simple_state(f"part_{part}"))
        write_json(ITEM_MODELS / f"part_{part}.json", {"parent": f"{MOD}:block/part_{part}"})

    write_json(BLOCK_MODELS / "industrial_tube.json", tube_center("tube"))  # unused, multipart uses pieces
    write_json(BLOCKSTATES / "industrial_tube.json", tube_multipart("tube_center", "tube_arm"))
    write_json(ITEM_MODELS / "industrial_tube.json", {"parent": f"{MOD}:block/tube_inventory"})
    write_json(BLOCKSTATES / "cryo_tube.json", tube_multipart("cryo_tube_center", "cryo_tube_arm"))
    write_json(ITEM_MODELS / "cryo_tube.json", {"parent": f"{MOD}:block/cryo_tube_inventory"})

    write_json(BLOCK_MODELS / "asphalt.json", cube("asphalt"))
    write_json(BLOCKSTATES / "asphalt.json", simple_state("asphalt"))
    write_json(ITEM_MODELS / "asphalt.json", {"parent": f"{MOD}:block/asphalt"})
    write_json(BLOCK_MODELS / "catalog.json", {
        "parent": "minecraft:block/cube",
        "textures": {
            "north": f"{MOD}:block/catalog",
            "south": f"{MOD}:block/machine_side_g2",
            "east": f"{MOD}:block/machine_side_g2",
            "west": f"{MOD}:block/machine_side_g2",
            "up": f"{MOD}:block/machine_side_g2",
            "down": f"{MOD}:block/machine_side_g2",
            "particle": f"{MOD}:block/catalog",
        },
    })
    write_json(BLOCKSTATES / "catalog.json", simple_state("catalog"))
    write_json(ITEM_MODELS / "catalog.json", {"parent": f"{MOD}:block/catalog"})

    write_json(BLOCK_MODELS / "gauge.json", {"parent": f"{MOD}:block/shape_gauge"})
    gauge_variants = {}
    for band in ("off", "green", "yellow", "red"):
        for facing, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            gauge_variants[f"band={band},facing={facing}"] = {"model": f"{MOD}:block/gauge", "y": rot}
    write_json(BLOCKSTATES / "gauge.json", {"variants": gauge_variants})
    write_json(ITEM_MODELS / "gauge.json", {"parent": f"{MOD}:block/gauge"})

    for name in ("reducer", "booster", "relief_valve"):
        write_json(BLOCK_MODELS / f"{name}.json", controller_model(name, "g2", name))
        write_json(BLOCKSTATES / f"{name}.json", facing_state(name))
        write_json(ITEM_MODELS / f"{name}.json", {"parent": f"{MOD}:block/{name}"})

    fluids = ["brine", "chlorine", "hydrogen", "caustic", "monomer", "hydrochloric", "acid_mist",
              "metal_solution", "byproduct_solution", "flue_gas", "nitrogen", "pure_nitrogen",
              "argon", "oxygen", "inerts", "liquid_co2", "dusty_steam", "acid_feed"]
    for fluid in fluids:
        write_json(BLOCK_MODELS / f"{fluid}.json", {
            "textures": {"particle": f"{MOD}:block/fluid_still"},
        })
        write_json(BLOCKSTATES / f"{fluid}.json", simple_state(fluid))
        write_json(ITEM_MODELS / f"{fluid}_bucket.json", {
            "parent": "minecraft:item/generated",
            "textures": {
                "layer0": f"{MOD}:item/bucket",
                "layer1": f"{MOD}:item/bucket_fluid",
            },
        })

    # standalone items
    items = [
        "stone_dust", "slag", "metal_ash", "heavy_wax", "plasticizer", "compressed_steel",
        "ceramic_alloy", "cryo_alloy", "critical_core", "catalyst", "filter_cake", "waste_chip",
        "engineering_plastic", "electrode", "substrate", "microfluidic_chip",
        "crushed_iron", "crushed_copper", "crushed_gold", "crushed_ancient",
        "purified_iron", "purified_copper", "purified_gold",
        "program_sheet", "certificate", "manual", "venturi_probe", "grade_drill",
        "curtain_g2", "curtain_g3", "curtain_g4", "curtain_g5",
    ]
    for grade in ("g2", "g3", "g4", "g5"):
        for shape in ("plate", "tube", "seal", "nozzle", "diaphragm", "cylinder", "pump", "gripper", "slide"):
            items.append(f"{grade}_{shape}")
    for item in items:
        write_json(ITEM_MODELS / f"{item}.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"{MOD}:item/{item}"},
        })


def main():
    generate_textures()
    generate_models()
    print("Generated Working Medium textures and models.")


if __name__ == "__main__":
    main()
