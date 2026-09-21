# The voice

The whippet's honk and its whine are both a real whippet: Bonnie, recorded by
her owner on a phone in October 2025, two clips a few seconds each.

## Licence

Her owner has asked for her voice to go out under the same MIT licence as the
rest of the mod. So it does: the recordings in `assets/whippets/sounds/` are
covered by `LICENSE` like everything else here, and anybody may use them, change
them, and ship them in their own work, keeping the notice with them. She is not
carved out and there is nothing extra to agree to.

## The honk

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

## The whine

`whine1.ogg` is one long drawn-out call, 1.03 seconds, from the second clip —
the same dog on the same afternoon, but a call rather than a shout, about 480 Hz
and far less abrupt than the honks. It is what a whippet actually spends most of
its waking day doing, so it is the noise you hear most: whining at food in your
hand, at being left behind, at being cold, and it stands in for the sigh as
well, dropped to seven tenths of its pitch and quietened, which is what a dog
settling down against you sounds like.

One take rather than four, because there was only one call of that kind in the
recordings. The game pitches it per dog — higher for puppies, and a touch either
way on the dog's own pace — so two whippets never whine on quite the same note.

The hurt and death sounds are still a wolf's. Nobody has recorded Bonnie being
hurt and nobody is going to.

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

The whine had the same treatment, cut from the same cleaned audio.

No pitch-shifting and no time-stretching: the game does its own pitching, a
little per dog, so each whippet honks on its own note.

## The one that came before

`tools/generate_sounds.py` synthesises a honk from a pitch contour, thirty
harmonics and four nasal formants. That is what shipped in 1.3.0, before there
was a recording. It now writes to `tools/synthesised-honks/` and nothing uses
its output, but it is kept: it documents what a honk is made of, and it will
make a fresh one if the recordings ever have to come out.
