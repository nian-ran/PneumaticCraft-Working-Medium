#!/usr/bin/env python3
"""Synthesize a few original, low-key industrial/UI sounds. Not ripped from any game."""
from __future__ import annotations

import math
import struct
import subprocess
import tempfile
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src/main/resources/assets/workingmedium/sounds"
RATE = 44100


def fade(samples: np.ndarray, attack=0.01, release=0.04) -> np.ndarray:
    n = len(samples)
    a = min(n, int(attack * RATE))
    r = min(n, int(release * RATE))
    env = np.ones(n, dtype=np.float64)
    if a:
        env[:a] *= np.linspace(0, 1, a)
    if r:
        env[-r:] *= np.linspace(1, 0, r)
    return samples * env


def write_ogg(name: str, samples: np.ndarray, volume=0.7):
    samples = np.clip(samples * volume, -1, 1)
    pcm = (samples * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wav = Path(tmp.name)
    with wav.open("wb") as f:
        f.write(b"RIFF")
        data = pcm.tobytes()
        f.write(struct.pack("<I", 36 + len(data)))
        f.write(b"WAVEfmt ")
        f.write(struct.pack("<IHHIIHH", 16, 1, 1, RATE, RATE * 2, 2, 16))
        f.write(b"data")
        f.write(struct.pack("<I", len(data)))
        f.write(data)
    dest = OUT / f"{name}.ogg"
    dest.parent.mkdir(parents=True, exist_ok=True)
    subprocess.check_call([
        "ffmpeg", "-y", "-loglevel", "error", "-i", str(wav),
        "-c:a", "libvorbis", "-q:a", "4", str(dest),
    ])
    wav.unlink(missing_ok=True)
    print("wrote", dest, "samples", len(pcm))


def machine_run():
    n = int(RATE * 0.38)
    t = np.arange(n) / RATE
    rumble = 0.35 * np.sin(2 * math.pi * 72 * t) + 0.18 * np.sin(2 * math.pi * 108 * t)
    rng = np.random.default_rng(11)
    brown = np.cumsum(rng.normal(0, 1, n))
    brown = brown / (np.max(np.abs(brown)) + 1e-9)
    # gentle low-pass
    ker = np.ones(48) / 48
    hiss = np.convolve(rng.normal(0, 1, n), ker, mode="same") * 0.12
    return fade(rumble + 0.28 * brown + hiss, 0.04, 0.1)


def machine_done():
    n = int(RATE * 0.16)
    t = np.arange(n) / RATE
    click = np.zeros(n)
    for i, (f, d, a) in enumerate(((620, 0.035, 0.5), (410, 0.05, 0.35))):
        start = int(0.015 * i * RATE)
        length = int(d * RATE)
        sl = slice(start, min(n, start + length))
        tt = np.arange(sl.stop - sl.start) / RATE
        click[sl] += a * np.sin(2 * math.pi * f * tt) * np.exp(-tt * 38)
    rng = np.random.default_rng(22)
    noise = rng.normal(0, 1, n)
    ker = np.ones(8) / 8
    tick = np.convolve(noise, ker, mode="same")
    tick[:int(0.012 * RATE)] *= 0.25
    return fade(click + 0.08 * tick, 0.002, 0.05)


def vent():
    n = int(RATE * 0.42)
    rng = np.random.default_rng(33)
    noise = rng.normal(0, 1, n)
    # band-pass-ish: high-pass then mild low-pass
    hp = np.diff(noise, prepend=noise[0])
    ker = np.ones(12) / 12
    air = np.convolve(hp, ker, mode="same")
    t = np.arange(n) / RATE
    wobble = 1 + 0.08 * np.sin(2 * math.pi * 9 * t)
    return fade(air * wobble, 0.02, 0.14)


def ui_click():
    n = int(RATE * 0.055)
    t = np.arange(n) / RATE
    tone = 0.45 * np.sin(2 * math.pi * 880 * t) * np.exp(-t * 55)
    tone += 0.2 * np.sin(2 * math.pi * 1320 * t) * np.exp(-t * 70)
    rng = np.random.default_rng(44)
    tick = rng.normal(0, 1, n)
    tick *= np.exp(-t * 90)
    return fade(tone + 0.08 * tick, 0.001, 0.02)


def main():
    write_ogg("machine_run", machine_run(), 0.55)
    write_ogg("machine_done", machine_done(), 0.6)
    write_ogg("vent", vent(), 0.45)
    write_ogg("ui_click", ui_click(), 0.5)
    print("Generated original Working Medium sounds.")


if __name__ == "__main__":
    main()
