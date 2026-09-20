# The honk

The whippet's honk is a real whippet honking: Bonnie, recorded by her owner on
a phone in October 2025, two clips a few seconds each.

Four takes ship with the mod, in `assets/whippets/sounds/whippet/`:

| file | source | length | note |
|---|---|---|---|
| `honk1.ogg` | second clip, first call | 0.36s | the sharp one |
| `honk2.ogg` | second clip, second call | 0.48s | rounder |
| `honk3.ogg` | first clip, third call | 0.34s | |
| `honk4.ogg` | first clip, third and fourth calls | 0.80s | a double, with her own gap between them |

What she actually sounds like, measured off the recordings: a fundamental
around 340–610 Hz with most of the energy between 500 Hz and 2.5 kHz, and calls
of 0.14 to 0.39 seconds. Short, bright and nasal — closer to a sharp bark than
to the long low goose honk the synthesised version guessed at.

## What was done to them

The phone recordings came in quiet — the first peaked at −27 dBFS with about
32 dB over the noise floor, the second about 7 dB hotter — so they needed
cleaning before they could be brought up to game level:

1. A noise profile taken from the silent parts of each clip and subtracted
   across the spectrum, which dropped the hiss by around 26 dB.
2. A gentle high-pass at 110 Hz for handling rumble.
3. Four-millisecond fades in, longer fades out, so nothing clicks.
4. Peak-normalised to 0.89, levelled so the takes sit together without
   flattening the difference between them.
5. Written as mono Ogg Vorbis at 44.1 kHz, which is what Minecraft wants for a
   positional sound.

No pitch-shifting and no time-stretching: the game does its own pitching, a
little per dog, so each whippet honks on its own note.

## The one that came before

`tools/generate_sounds.py` synthesises a honk from a pitch contour, thirty
harmonics and four nasal formants. That is what shipped in 1.3.0, before there
was a recording. It now writes to `tools/synthesised-honks/` and nothing uses
its output, but it is kept: it documents what a honk is made of, and it will
make a fresh one if the recordings ever have to come out.
