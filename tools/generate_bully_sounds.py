#!/usr/bin/env python3
"""Synthesises the XL Bully's voice.

The whippet's honk and whine are recordings of a real dog (see `docs/sound.md`).
Nobody is putting a microphone in front of an XL Bully for this mod, so its
voice is built rather than recorded — the same source-and-filter method as
`generate_sounds.py`, moved down into a much bigger chest.

A big dog's bark is a low fundamental — 110-150 Hz against a whippet's 450 — a
hard noisy onset where the whole airway opens at once, formants low and close
together because the throat is short and wide, and a fast drop in pitch as the
bark collapses. The growl is lower still, barely voiced, and its character is
almost entirely a heavy amplitude ripple: the rattle of a very large animal
telling you it has noticed you.

    pip install soundfile numpy
    python3 tools/generate_bully_sounds.py

It writes straight into the mod's assets, because unlike the whippet's voice
this one has no original to overwrite.
"""

from __future__ import annotations

import pathlib

import numpy as np
import soundfile as sf

ROOT = pathlib.Path(__file__).resolve().parent.parent
SOUNDS = ROOT / "src" / "main" / "resources" / "assets" / "whippets" / "sounds" / "bully"

RATE = 44100
# A short, wide throat: low formants, and not much space between them.
FORMANTS = ((320.0, 130.0, 1.00), (780.0, 220.0, 0.80), (1550.0, 380.0, 0.40), (2600.0, 700.0, 0.12))
HARMONICS = 60


def contour(t: np.ndarray, points: list[tuple[float, float]]) -> np.ndarray:
    fractions = np.array([p[0] for p in points])
    values = np.array([p[1] for p in points])
    return np.interp(t / t[-1], fractions, values)


def formant_gain(frequency: np.ndarray) -> np.ndarray:
    gain = np.zeros_like(frequency)

    for centre, bandwidth, level in FORMANTS:
        gain += level * bandwidth**2 / ((frequency - centre) ** 2 + bandwidth**2)

    return gain


def voiced(t: np.ndarray, f0: np.ndarray, rng: np.random.Generator, brightness: np.ndarray | float) -> np.ndarray:
    """A buzzy glottal source pushed through the formants above."""
    phase = 2.0 * np.pi * np.cumsum(f0) / RATE
    wave = np.zeros_like(t)

    for n in range(1, HARMONICS + 1):
        amplitude = formant_gain(f0 * n) / n**1.15

        if n > 4:
            amplitude = amplitude * brightness

        wave += amplitude * np.sin(n * phase + rng.uniform(0.0, 2.0 * np.pi))

    return wave


def bark(duration: float, pitch: float, seed: int, snarl: float = 1.0) -> np.ndarray:
    """One bark: a bang, then a short collapsing shout."""
    rng = np.random.default_rng(seed)
    t = np.linspace(0.0, duration, int(RATE * duration), endpoint=False)

    # Barks start high and fall through themselves. That fall is most of what
    # makes a bark a bark rather than a shout.
    f0 = pitch * contour(t, [(0.0, 1.35), (0.05, 1.12), (0.25, 1.0), (0.6, 0.9), (1.0, 0.74)])
    f0 = f0 * (1.0 + 0.02 * np.sin(2.0 * np.pi * 17.0 * t) + 0.015 * np.sin(2.0 * np.pi * 4.0 * t))

    brightness = contour(t, [(0.0, 1.3), (0.1, 1.0), (0.5, 0.7), (1.0, 0.35)]) * snarl
    wave = voiced(t, f0, rng, brightness)

    # The onset: the whole airway letting go at once, which is noise, not tone.
    noise = rng.normal(0.0, 1.0, t.size)
    spectrum = np.fft.rfft(noise)
    spectrum *= formant_gain(np.fft.rfftfreq(t.size, 1.0 / RATE))
    breath = np.fft.irfft(spectrum, t.size)
    breath /= np.max(np.abs(breath)) + 1e-9
    wave += breath * 0.35 * contour(t, [(0.0, 1.0), (0.06, 0.4), (0.3, 0.12), (1.0, 0.05)])

    attack = 1.0 - np.exp(-t / 0.004)
    release = np.clip((duration - t) / 0.09, 0.0, 1.0) ** 1.6
    body = contour(t, [(0.0, 1.0), (0.2, 0.95), (0.6, 0.7), (1.0, 0.45)])
    wave = wave * attack * release * body

    wave /= np.max(np.abs(wave)) + 1e-9
    return wave * 0.95


def growl(duration: float, pitch: float, seed: int) -> np.ndarray:
    """A long low rattle, held flat, with the ripple doing the talking."""
    rng = np.random.default_rng(seed)
    t = np.linspace(0.0, duration, int(RATE * duration), endpoint=False)

    f0 = pitch * contour(t, [(0.0, 0.92), (0.2, 1.0), (0.75, 0.98), (1.0, 0.88)])
    f0 = f0 * (1.0 + 0.03 * np.sin(2.0 * np.pi * 3.0 * t + 0.4))
    wave = voiced(t, f0, rng, 0.55)

    # The rattle: a slow, deep amplitude ripple with its own wobble, which is
    # what a growl is once you take the pitch away.
    ripple = 0.55 + 0.45 * np.sin(2.0 * np.pi * (27.0 + 3.0 * np.sin(2.0 * np.pi * 1.7 * t)) * t)
    wave = wave * ripple

    noise = rng.normal(0.0, 1.0, t.size)
    spectrum = np.fft.rfft(noise)
    spectrum *= formant_gain(np.fft.rfftfreq(t.size, 1.0 / RATE))
    breath = np.fft.irfft(spectrum, t.size)
    breath /= np.max(np.abs(breath)) + 1e-9
    wave += breath * 0.12

    attack = np.clip(t / 0.12, 0.0, 1.0)
    release = np.clip((duration - t) / 0.2, 0.0, 1.0) ** 1.2
    wave = wave * attack * release

    wave /= np.max(np.abs(wave)) + 1e-9
    return wave * 0.8


def main() -> None:
    SOUNDS.mkdir(parents=True, exist_ok=True)
    calls = {
        "bark1": bark(0.34, 132.0, 3),
        "bark2": bark(0.29, 148.0, 17, snarl=1.2),
        "bark3": bark(0.40, 118.0, 29, snarl=0.85),
        "growl1": growl(1.30, 82.0, 41),
        "growl2": growl(1.60, 74.0, 59),
    }

    for name, wave in calls.items():
        path = SOUNDS / f"{name}.ogg"
        sf.write(path, wave.astype(np.float32), RATE, format="OGG", subtype="VORBIS")
        print(f"wrote {path.relative_to(ROOT)}  {wave.size / RATE:.2f}s peak {np.max(np.abs(wave)):.2f}")


if __name__ == "__main__":
    main()
