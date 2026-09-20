#!/usr/bin/env python3
"""Generates the mod's sound effects.

Like the textures, nothing here was recorded: the honk is synthesised from a
pitch contour, a harmonic stack and a set of formants, so changing it is a
matter of moving a number rather than finding a microphone and a goose.

    pip install soundfile numpy
    python3 tools/generate_sounds.py

A goose honk is a harsh, nasal, buzzy thing: a fundamental around 400-500 Hz
carrying a lot of harmonics, two strong nasal formants near 1 kHz and 2 kHz, a
pitch that jumps up at the onset and sags away at the end, and enough jitter in
it to sound like an animal rather than a synthesiser. A whippet asking for
something makes very much the same noise, which is why it is in this mod.
"""

from __future__ import annotations

import pathlib

import numpy as np
import soundfile as sf

ROOT = pathlib.Path(__file__).resolve().parent.parent
SOUNDS = ROOT / "src" / "main" / "resources" / "assets" / "whippets" / "sounds" / "whippet"

RATE = 44100
# Nasal formants. Each is (centre Hz, bandwidth Hz, gain).
FORMANTS = ((620.0, 180.0, 1.00), (1080.0, 240.0, 0.85), (2150.0, 420.0, 0.45), (3100.0, 600.0, 0.18))
HARMONICS = 30


def contour(t: np.ndarray, points: list[tuple[float, float]]) -> np.ndarray:
    """A pitch or level curve through (time fraction, value) control points."""
    fractions = np.array([p[0] for p in points])
    values = np.array([p[1] for p in points])
    return np.interp(t / t[-1], fractions, values)


def formant_gain(frequency: np.ndarray) -> np.ndarray:
    """How much of each harmonic survives the throat it came out of."""
    gain = np.zeros_like(frequency)

    for centre, bandwidth, level in FORMANTS:
        gain += level * bandwidth**2 / ((frequency - centre) ** 2 + bandwidth**2)

    return gain


def honk(duration: float, pitch: float, seed: int, rasp: float = 1.0) -> np.ndarray:
    """One honk: up at the front, sagging at the back, and rude throughout."""
    rng = np.random.default_rng(seed)
    t = np.linspace(0.0, duration, int(RATE * duration), endpoint=False)

    # The shape of the call. It snaps up to pitch, holds, and sags away.
    f0 = pitch * contour(t, [(0.0, 0.62), (0.06, 1.05), (0.18, 1.0), (0.62, 0.95), (1.0, 0.78)])
    # Jitter: a fast waver plus a slow drift, which is what stops it sounding
    # like a car horn.
    waver = 1.0 + 0.018 * np.sin(2.0 * np.pi * 24.0 * t) + 0.012 * np.sin(2.0 * np.pi * 5.5 * t + 1.1)
    f0 = f0 * waver

    phase = 2.0 * np.pi * np.cumsum(f0) / RATE
    voice = np.zeros_like(t)

    for n in range(1, HARMONICS + 1):
        # A sawtooth's harmonic roll-off, bent through the formants.
        amplitude = formant_gain(f0 * n) / n**1.05
        # Higher harmonics come in as the call opens up: that is the rasp.
        if n > 6:
            amplitude = amplitude * contour(t, [(0.0, 0.25), (0.12, 1.0), (0.7, 0.9), (1.0, 0.4)]) * rasp

        voice += amplitude * np.sin(n * phase + rng.uniform(0.0, 2.0 * np.pi))

    # A breath of noise through the same formants, for the hiss at the start.
    noise = rng.normal(0.0, 1.0, t.size)
    spectrum = np.fft.rfft(noise)
    spectrum *= formant_gain(np.fft.rfftfreq(t.size, 1.0 / RATE))
    breath = np.fft.irfft(spectrum, t.size)
    breath /= np.max(np.abs(breath)) + 1e-9
    voice += breath * 0.16 * contour(t, [(0.0, 1.0), (0.15, 0.35), (1.0, 0.1)])

    # Envelope: hard attack, a live-sounding wobble, a quick fall at the end.
    attack = 1.0 - np.exp(-t / 0.010)
    release = np.clip((duration - t) / 0.11, 0.0, 1.0) ** 1.4
    body = contour(t, [(0.0, 0.9), (0.15, 1.0), (0.55, 0.88), (0.8, 0.95), (1.0, 0.7)])
    wave = voice * attack * release * body

    wave /= np.max(np.abs(wave)) + 1e-9
    return wave * 0.89


def double_honk(duration: float, pitch: float, seed: int) -> np.ndarray:
    """Honk-honk: the second one shorter and flatter, as a goose does it."""
    first = honk(duration, pitch, seed)
    gap = np.zeros(int(RATE * 0.07))
    second = honk(duration * 0.72, pitch * 0.94, seed + 1, rasp=0.85) * 0.85
    return np.concatenate([first, gap, second])


def main() -> None:
    SOUNDS.mkdir(parents=True, exist_ok=True)
    calls = {
        "honk1": honk(0.44, 455.0, 11),
        "honk2": honk(0.38, 505.0, 23, rasp=1.15),
        "honk3": honk(0.52, 420.0, 37, rasp=0.9),
        "honk4": double_honk(0.36, 470.0, 51),
    }

    for name, wave in calls.items():
        path = SOUNDS / f"{name}.ogg"
        sf.write(path, wave.astype(np.float32), RATE, format="OGG", subtype="VORBIS")
        print(f"wrote {path.relative_to(ROOT)}  {wave.size / RATE:.2f}s peak {np.max(np.abs(wave)):.2f}")


if __name__ == "__main__":
    main()
