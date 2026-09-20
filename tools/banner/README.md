# The banner

`docs/banner.png` is Bonnie — the whippet the mod is modelled on — put through
the game's idiom rather than drawn.

```sh
java Pixelate.java photo.jpg bonnie.png 0 415 1850 1850 160 24
java Banner.java bonnie.png ../../docs/banner.png 1280 480 3
```

`Pixelate` box-downsamples a photograph to a coarse grid and then flattens the
palette with k-means, so the result is blocks of flat colour instead of a
blurred photo. `Banner` sets that beside the title, scaled nearest-neighbour so
the pixels stay square.

The arguments after the filenames are the crop — x, y, width, height — then
how many cells wide to reduce to, then how many colours to keep. `bonnie.png`
is the 160×160 result and is what the banner uses; the original photograph is
not in the repository.

I tried drawing the whippet in vector first, six times. Every one of them came
out looking like an anteater. A photograph of the actual dog, coarsened until
it reads as pixel art, needs no draughtsmanship and cannot be anatomically
wrong.
