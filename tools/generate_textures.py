#!/usr/bin/env python3
"""Generates every PNG the Whippets mod ships.

The mod has no binary art assets that were drawn by hand: the entity sheets, the
item icons and the mod icon are all produced here, so a coat colour is a couple
of hex values rather than a trip through an image editor. Run it from anywhere:

    python3 tools/generate_textures.py

Entity UVs follow Minecraft's cuboid layout. For a box at (u, v) with size
(w, h, d) the six faces land like this:

    top    (u+d,     v,     w, d)      bottom (u+d+w,   v,     w, d)
    west   (u,       v+d,   d, h)      north  (u+d,     v+d,   w, h)
    east   (u+d+w,   v+d,   d, h)      south  (u+d+w+d, v+d,   w, h)

"north" is the face pointing the way the mob looks. The box table below must stay
in step with WhippetEntityModel.getModelData().
"""

from __future__ import annotations

import pathlib
import struct
import zlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "whippets"
ENTITY_DIR = ASSETS / "textures" / "entity" / "whippet"
SQUIRREL_DIR = ASSETS / "textures" / "entity" / "squirrel"
ITEM_DIR = ASSETS / "textures" / "item"

TRANSPARENT = (0, 0, 0, 0)

# name -> (u, v, width, height, depth)
WHIPPET_BOXES = {
    "skull": (0, 0, 3, 3, 5),
    "muzzle": (20, 0, 2, 2, 3),
    "ear": (36, 0, 1, 2, 3),
    "chest": (0, 12, 5, 7, 7),
    "loin": (26, 12, 4, 4, 8),
    "leg": (0, 28, 2, 9, 2),
    "haunch": (10, 28, 3, 4, 3),
    "tail": (26, 28, 1, 9, 1),
    "neck": (32, 28, 3, 3, 7),
}

SQUIRREL_BOXES = {
    "body": (0, 0, 4, 4, 6),
    "head": (20, 0, 4, 4, 4),
    "muzzle": (36, 0, 2, 2, 1),
    "ear": (44, 0, 2, 2, 1),
    "leg": (0, 12, 2, 3, 2),
    "tail": (10, 12, 4, 8, 3),
    "tail_tip": (26, 12, 5, 5, 3),
    "nut": (44, 12, 2, 2, 2),
}

BODY_BOXES = ("chest", "loin", "neck", "haunch", "tail")
FLANKS = ("west", "east")
SIDES = ("west", "north", "east", "south")


def faces(box: str, boxes: dict | None = None) -> dict[str, tuple[int, int, int, int]]:
    u, v, w, h, d = (boxes or WHIPPET_BOXES)[box]
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "west": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),
        "east": (u + d + w, v + d, d, h),
        "south": (u + d + w + d, v + d, w, h),
    }


def blend(a: tuple[int, int, int, int], b: tuple[int, int, int, int], amount: float) -> tuple[int, int, int, int]:
    """Mixes two colours; amount is how much of b to take."""
    return (
        round(a[0] + (b[0] - a[0]) * amount),
        round(a[1] + (b[1] - a[1]) * amount),
        round(a[2] + (b[2] - a[2]) * amount),
        a[3],
    )


def shift(color: tuple[int, int, int, int], amount: float) -> tuple[int, int, int, int]:
    """Lightens (amount > 0) or darkens (amount < 0) a colour."""
    r, g, b, a = color
    if amount >= 0:
        return (
            round(r + (255 - r) * amount),
            round(g + (255 - g) * amount),
            round(b + (255 - b) * amount),
            a,
        )
    return (round(r * (1 + amount)), round(g * (1 + amount)), round(b * (1 + amount)), a)


class Image:
    def __init__(self, width: int, height: int, fill=TRANSPARENT, boxes: dict | None = None):
        self.width = width
        self.height = height
        self.pixels = [[fill] * width for _ in range(height)]
        # Which animal's cuboid table this sheet is laid out for.
        self.boxes = boxes or WHIPPET_BOXES

    def set(self, x: int, y: int, color) -> None:
        if 0 <= x < self.width and 0 <= y < self.height:
            self.pixels[y][x] = color

    def rect(self, x: int, y: int, w: int, h: int, color) -> None:
        for dy in range(h):
            for dx in range(w):
                self.set(x + dx, y + dy, color)

    def fill_face(self, box: str, face: str, color) -> None:
        x, y, w, h = faces(box, self.boxes)[face]
        self.rect(x, y, w, h, color)

    def fill_box(self, box: str, color) -> None:
        for face in faces(box, self.boxes):
            self.fill_face(box, face, color)

    def face_row(self, box: str, face: str, row: int, color) -> None:
        """Paints one row of a face, counted from its top edge."""
        x, y, w, h = faces(box, self.boxes)[face]
        if 0 <= row < h:
            self.rect(x, y + row, w, 1, color)

    def face_rows_from_bottom(self, box: str, face: str, count: int, color) -> None:
        x, y, w, h = faces(box, self.boxes)[face]
        self.rect(x, y + max(0, h - count), w, min(count, h), color)

    def stripe_face(self, box: str, face: str, color, period: int = 3) -> None:
        """Vertical brindle striping: columns on the flanks, rows on top/bottom."""
        x, y, w, h = faces(box, self.boxes)[face]
        if face in ("top", "bottom"):
            for dy in range(h):
                if dy % period == 0:
                    self.rect(x, y + dy, w, 1, color)
        else:
            for dx in range(w):
                if dx % period == 0:
                    self.rect(x + dx, y, 1, h, color)

    def write(self, path: pathlib.Path) -> None:
        raw = bytearray()
        for row in self.pixels:
            raw.append(0)
            for r, g, b, a in row:
                raw += bytes((r, g, b, a))

        def chunk(tag: bytes, data: bytes) -> bytes:
            return (
                struct.pack(">I", len(data))
                + tag
                + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
            )

        png = b"\x89PNG\r\n\x1a\n"
        png += chunk(b"IHDR", struct.pack(">IIBBBBB", self.width, self.height, 8, 6, 0, 0, 0))
        png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        png += chunk(b"IEND", b"")
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(png)
        print(f"wrote {path.relative_to(ROOT)}")


class Coat:
    def __init__(
        self,
        name: str,
        base: tuple[int, int, int, int],
        belly: tuple[int, int, int, int],
        mask: tuple[int, int, int, int],
        *,
        stripe: tuple[int, int, int, int] | None = None,
        stripe_period: int = 3,
        socks: bool = True,
        blaze: bool = True,
        patches: bool = False,
        grey_face: tuple[int, int, int, int] | None = None,
        white_front: bool = False,
        nose: tuple[int, int, int, int] = (26, 21, 23, 255),
    ):
        self.name = name
        self.base = base
        self.belly = belly
        self.mask = mask
        self.stripe = stripe
        self.stripe_period = stripe_period
        self.socks = socks
        self.blaze = blaze
        self.patches = patches
        # A silvered face, the way a blue brindle greys off around the muzzle.
        self.grey_face = grey_face
        # White that runs from the chin down the throat and over the belly.
        self.white_front = white_front
        self.nose = nose


WHITE = (240, 234, 226, 255)
EYE = (23, 17, 15, 255)
BROW = (0, 0, 0, 0)

COATS = [
    Coat("fawn", (199, 150, 99, 255), (230, 203, 168, 255), (110, 82, 56, 255)),
    Coat(
        "brindle",
        (122, 92, 62, 255),
        (168, 138, 102, 255),
        (46, 34, 24, 255),
        stripe=(58, 42, 28, 255),
    ),
    Coat(
        "blue",
        (115, 122, 128, 255),
        (154, 161, 167, 255),
        (69, 76, 82, 255),
        socks=False,
        blaze=False,
        nose=(42, 46, 51, 255),
    ),
    Coat("black", (42, 42, 44, 255), (58, 58, 61, 255), (26, 26, 28, 255)),
    Coat(
        "white",
        WHITE,
        (255, 255, 255, 255),
        (199, 150, 99, 255),
        patches=True,
        blaze=False,
        nose=(38, 30, 30, 255),
    ),
    # Modelled on Bonnie: a blue brindle with a silver face, a white blaze,
    # white from the chin all the way down the chest, and four white feet.
    Coat(
        "bonnie",
        (164, 146, 122, 255),
        (242, 237, 228, 255),
        (107, 100, 89, 255),
        stripe=(131, 116, 96, 255),
        stripe_period=4,
        grey_face=(146, 141, 132, 255),
        white_front=True,
    ),
    # Bobby Brazil, from the photograph: a lurcher, near enough black with a
    # warm cast to him, a tan muzzle going grey, a white chin and chest and
    # white toes on every foot.
    Coat(
        "lurcher",
        (44, 38, 34, 255),
        (70, 61, 54, 255),
        (26, 23, 21, 255),
        stripe=(33, 29, 26, 255),
        stripe_period=5,
        grey_face=(126, 112, 96, 255),
        white_front=True,
        nose=(26, 22, 20, 255),
    ),
]


def draw_coat(coat: Coat) -> Image:
    image = Image(64, 64)
    shade = shift(coat.base, -0.18)
    highlight = shift(coat.base, 0.10)

    for box in WHIPPET_BOXES:
        image.fill_box(box, coat.base)
        image.fill_face(box, "top", highlight)

    # Deep chest and tucked belly: the underside is always paler.
    flank_belly = blend(coat.base, coat.belly, 0.55)

    for box in ("chest", "loin", "neck"):
        image.fill_face(box, "bottom", coat.belly)
        for face in ("north", "south"):
            image.face_rows_from_bottom(box, face, 2, coat.belly)
        for face in FLANKS:
            image.face_rows_from_bottom(box, face, 2, flank_belly)
            image.face_rows_from_bottom(box, face, 1, shift(flank_belly, -0.08))

    for box in ("chest", "loin", "haunch"):
        for face in FLANKS:
            image.face_row(box, face, 0, shade)

    if coat.stripe:
        for box in BODY_BOXES + ("leg", "skull"):
            for face in FLANKS + ("top",):
                image.stripe_face(box, face, coat.stripe, coat.stripe_period)

    if coat.patches:
        # An Irish-marked white dog: colour over the ears, skull and one hip.
        image.fill_box("ear", coat.mask)
        image.fill_face("skull", "top", coat.mask)
        image.face_row("skull", "west", 0, coat.mask)
        image.face_row("skull", "east", 0, coat.mask)
        image.fill_face("haunch", "east", coat.mask)
        image.fill_face("haunch", "top", coat.mask)
    else:
        image.fill_box("ear", shift(coat.base, -0.26))

    # A rose ear is a folded flap: the leather darkens towards the tip, which is
    # the back end of the flap, and the fold catches the light along the top.
    tip = shift(coat.base, -0.42)
    ex, ey, ew, eh = faces("ear")["west"]
    image.rect(ex, ey, 1, eh, tip)
    ex, ey, ew, eh = faces("ear")["east"]
    image.rect(ex + ew - 1, ey, 1, eh, tip)
    image.fill_face("ear", "south", tip)
    image.face_row("ear", "top", 0, shift(coat.base, -0.14))

    if coat.grey_face:
        # A silvered skull, the way a blue brindle greys off around the face.
        for face in FLANKS + ("top", "north"):
            image.fill_face("skull", face, coat.grey_face)

    # Dark mask over the muzzle, black nose on the front of it.
    for face in ("top", "west", "east", "south"):
        image.fill_face("muzzle", face, coat.mask)
    image.fill_face("muzzle", "north", coat.mask)
    image.fill_face("muzzle", "bottom", shift(coat.mask, 0.22))
    mx, my, mw, _ = faces("muzzle")["north"]
    image.rect(mx, my, mw, 1, coat.nose)

    # Eyes, one pixel each, set well forward on a narrow skull.
    skull = faces("skull")
    wx, wy, ww, _ = skull["west"]
    ex, ey, _, _ = skull["east"]
    image.set(wx + ww - 2, wy + 1, EYE)
    image.set(ex + 1, ey + 1, EYE)

    if coat.blaze:
        # The blaze: a white stripe up the bridge of the muzzle and on over the
        # forehead, with a white chin and lip under it.
        white = coat.belly if coat.white_front else WHITE
        mtx, mty, mtw, mth = faces("muzzle")["top"]
        image.rect(mtx + mtw // 2, mty, 1, mth, white)
        # The blaze dies out between the eyes rather than running back over the
        # whole skull, so only the front of the skull top takes it.
        stx, sty, stw, sth = faces("skull")["top"]
        image.rect(stx + stw // 2, sty + sth - 2, 1, 2, white)
        image.fill_face("muzzle", "bottom", white)
        for face in FLANKS:
            image.face_rows_from_bottom("muzzle", face, 1, white)
        cx, cy, cw, ch = faces("chest")["north"]
        image.rect(cx + 1, cy + ch - 4, cw - 2, 3, white)

    if coat.white_front:
        # Chin, throat, chest and belly are one unbroken run of white.
        for box in ("neck", "chest", "loin"):
            image.fill_face(box, "bottom", coat.belly)
            image.face_rows_from_bottom(box, "north", 2, coat.belly)
        image.fill_face("neck", "north", coat.belly)
        cx, cy, cw, ch = faces("chest")["north"]
        image.rect(cx + 1, cy + 1, cw - 2, ch - 1, coat.belly)

    if coat.socks:
        for face in SIDES:
            image.face_rows_from_bottom("leg", face, 2, WHITE)
            image.face_rows_from_bottom("leg", face, 1, shift(WHITE, -0.10))
        image.fill_face("leg", "bottom", shift(WHITE, -0.16))
        # White tip on the whip tail.
        for face in SIDES:
            image.face_rows_from_bottom("tail", face, 2, WHITE)
        image.fill_face("tail", "bottom", WHITE)

    return image


class Squirrel:
    """One squirrel's colouring. Cuteness is mostly the pale bits."""

    def __init__(
        self,
        name: str,
        base: tuple[int, int, int, int],
        belly: tuple[int, int, int, int],
        fringe: tuple[int, int, int, int],
        ear: tuple[int, int, int, int],
        nose: tuple[int, int, int, int],
        ring: tuple[int, int, int, int],
        saddle: tuple[int, int, int, int] | None = None,
    ):
        self.name = name
        self.base = base
        self.belly = belly
        # The pale edge that makes the tail read as a plume rather than a plank.
        self.fringe = fringe
        self.ear = ear
        self.nose = nose
        # The pale ring that makes the eye look twice the size it is.
        self.ring = ring
        self.saddle = saddle


SQUIRRELS = [
    Squirrel(
        "red",
        (168, 92, 46, 255),
        (243, 234, 218, 255),
        (216, 160, 110, 255),
        (231, 190, 150, 255),
        (48, 36, 32, 255),
        (245, 238, 226, 255),
    ),
    Squirrel(
        "grey",
        (146, 146, 142, 255),
        (243, 241, 237, 255),
        (212, 212, 208, 255),
        (196, 190, 186, 255),
        (52, 50, 50, 255),
        (245, 243, 239, 255),
        saddle=(150, 128, 100, 255),
    ),
    Squirrel(
        "black",
        (52, 48, 46, 255),
        (96, 90, 85, 255),
        (80, 74, 70, 255),
        (86, 74, 70, 255),
        (26, 25, 25, 255),
        (158, 148, 138, 255),
    ),
]

ACORN_CAP = (92, 62, 38, 255)
ACORN_NUT = (188, 142, 88, 255)


def draw_squirrel(squirrel: Squirrel) -> Image:
    image = Image(64, 32, boxes=SQUIRREL_BOXES)
    highlight = shift(squirrel.base, 0.10)
    shade = shift(squirrel.base, -0.16)

    for box in SQUIRREL_BOXES:
        image.fill_box(box, squirrel.base)
        image.fill_face(box, "top", highlight)

    # A warm saddle over the back, the way a grey squirrel browns off along
    # the spine in summer.
    if squirrel.saddle:
        for box in ("body", "head"):
            image.fill_face(box, "top", blend(highlight, squirrel.saddle, 0.45))

    # Pale from the chin all the way down the belly: the bit you only see when
    # it sits up, which is when you are looking.
    for box in ("body", "head"):
        image.fill_face(box, "bottom", squirrel.belly)

    image.fill_face("muzzle", "bottom", blend(squirrel.base, squirrel.belly, 0.7))

    for face in FLANKS:
        image.face_rows_from_bottom("body", face, 1, blend(squirrel.base, squirrel.belly, 0.6))

    image.fill_face("body", "north", blend(squirrel.base, squirrel.belly, 0.75))
    image.face_row("body", "west", 0, shade)
    image.face_row("body", "east", 0, shade)

    # The tail: base down the middle, pale fringe up both edges and over the
    # top, which is what makes it look like hair rather than a board.
    for box in ("tail", "tail_tip"):
        for face in FLANKS:
            x, y, w, h = faces(box, SQUIRREL_BOXES)[face]
            image.rect(x, y, 1, h, squirrel.fringe)
            image.rect(x + w - 1, y, 1, h, squirrel.fringe)
        image.fill_face(box, "north", squirrel.fringe)
        image.fill_face(box, "south", squirrel.fringe)
        image.face_row(box, "top", 0, squirrel.fringe)
    image.fill_face("tail_tip", "bottom", squirrel.fringe)

    # Ears: pale inside, dark tufted tip.
    image.fill_face("ear", "north", squirrel.ear)
    for face in ("west", "east"):
        image.face_row("ear", face, 0, shift(squirrel.base, -0.3))
    image.face_row("ear", "top", 0, shift(squirrel.base, -0.3))

    # Face: dark nose on the front of the muzzle, pale cheeks under it.
    image.fill_face("muzzle", "north", shift(squirrel.base, -0.12))
    mx, my, mw, _ = faces("muzzle", SQUIRREL_BOXES)["north"]
    image.rect(mx, my, mw, 1, squirrel.nose)
    for face in FLANKS:
        image.face_rows_from_bottom("muzzle", face, 1, squirrel.belly)

    # Eyes, big and dark in a pale ring. A squirrel is 60% eye.
    head = faces("head", SQUIRREL_BOXES)
    wx, wy, ww, _ = head["west"]
    ex, ey, _, _ = head["east"]
    image.rect(wx + ww - 3, wy + 1, 2, 2, squirrel.ring)
    image.rect(ex + 1, ey + 1, 2, 2, squirrel.ring)
    image.set(wx + ww - 2, wy + 1, EYE)
    image.set(ex + 1, ey + 1, EYE)

    # Little pale hands and feet.
    for face in SIDES:
        image.face_rows_from_bottom("leg", face, 1, blend(squirrel.base, squirrel.belly, 0.7))
    image.fill_face("leg", "bottom", blend(squirrel.base, squirrel.belly, 0.7))

    # Whatever it is carrying is always drawn as an acorn, because an acorn is
    # funnier than whatever it actually stole.
    image.fill_box("nut", ACORN_NUT)
    image.fill_face("nut", "top", ACORN_CAP)
    for face in SIDES:
        image.face_row("nut", face, 0, ACORN_CAP)

    return image


def draw_squirrel_spawn_egg() -> Image:
    """Spawn-egg silhouette: a rusty shell with pale flecks."""
    return from_map(
        [
            "................",
            ".....######.....",
            "....#RRRRRR#....",
            "...#RRPRRRRR#...",
            "...#RRRRRPRR#...",
            "..#RRRPRRRRRR#..",
            "..#RPRRRRRPRR#..",
            "..#RRRRRPRRRR#..",
            "..#RRPRRRRRRR#..",
            "..#RRRRRRPRRR#..",
            "..#RPRRRRRRPR#..",
            "...#RRRPRRRR#...",
            "...#RRRRRRPR#...",
            "....#RRPRRR#....",
            ".....######.....",
            "................",
        ],
        {
            "#": (86, 44, 24, 255),
            "R": (168, 92, 46, 255),
            "P": (238, 226, 208, 255),
        },
    )


def draw_collar() -> Image:
    """A band near the front of the neck, tinted in-game by the dye colour.

    Only the front of the neck box clears the chest, so the band sits a slice or
    two back from the head end — which is where a collar sits on a real dog too.
    Within a face, the depth axis runs back-to-front on the west face, front-to-
    back on the east face, and back-to-front down the rows of the top and bottom.
    """
    image = Image(64, 64)
    leather = (255, 255, 255, 255)
    buckle = (215, 215, 215, 255)
    offset = 1
    band = 2

    x, y, fw, fh = faces("neck")["west"]
    image.rect(x + fw - offset - band, y, band, fh, leather)
    x, y, fw, fh = faces("neck")["east"]
    image.rect(x + offset, y, band, fh, leather)
    for face in ("top", "bottom"):
        x, y, fw, fh = faces("neck")[face]
        image.rect(x, y + fh - offset - band, fw, band, leather)

    # A tag hanging under the band.
    x, y, fw, fh = faces("neck")["bottom"]
    image.set(x + fw // 2, y + fh - offset - band, buckle)
    return image


def from_map(rows: list[str], palette: dict[str, tuple[int, int, int, int]]) -> Image:
    """Builds a small icon from an ASCII map; '.' is left transparent."""
    image = Image(len(rows[0]), len(rows))
    for y, row in enumerate(rows):
        for x, key in enumerate(row):
            if key != ".":
                image.set(x, y, palette[key])
    return image


def draw_spawn_egg() -> Image:
    """Spawn-egg silhouette: fawn shell with dark brindle speckles."""
    return from_map(
        [
            "................",
            ".....######.....",
            "....#BBBBBB#....",
            "...#BBSBBBBB#...",
            "...#BBBBBSBB#...",
            "..#BBBSBBBBBB#..",
            "..#BSBBBBBSBB#..",
            "..#BBBBBSBBBB#..",
            "..#BBSBBBBBBB#..",
            "..#BBBBBBSBBB#..",
            "..#BSBBBBBBSB#..",
            "...#BBBSBBBB#...",
            "...#BBBBBBSB#...",
            "....#BBSBBB#....",
            ".....######.....",
            "................",
        ],
        {
            "#": (62, 44, 30, 255),
            "B": (199, 150, 99, 255),
            "S": (86, 62, 42, 255),
        },
    )


def draw_whistle() -> Image:
    """A dog whistle: tapered mouthpiece, steel chamber, ring for the lead."""
    return from_map(
        [
            "................",
            "................",
            "................",
            ".........###....",
            ".........#.#....",
            ".........#S#....",
            "......########..",
            "....###SSOOSS#..",
            "..###SSSSSSSS#..",
            "..#SHSSSSSSSS#..",
            "..###DDDDDDDD#..",
            "....##########..",
            "................",
            "................",
            "................",
            "................",
        ],
        {
            "#": (44, 48, 54, 255),
            "S": (200, 205, 212, 255),
            "H": (238, 242, 247, 255),
            "D": (96, 103, 112, 255),
            "O": (30, 33, 38, 255),
        },
    )


def draw_lure() -> Image:
    """The racing lure: a rag on a stick, with a red band so the dogs see it."""
    return from_map(
        [
            "................",
            "...........###..",
            "..........#WWW#.",
            ".........#WWRW#.",
            ".........#WRRW#.",
            ".........#WWW#..",
            "........#SS#....",
            ".......#SS#.....",
            "......#SS#......",
            ".....#SS#.......",
            "....#SS#........",
            "...#SS#.........",
            "..#SS#..........",
            "..#S#...........",
            "................",
            "................",
        ],
        {
            "#": (60, 45, 32, 255),
            "S": (140, 101, 62, 255),
            "W": (238, 233, 224, 255),
            "R": (176, 64, 52, 255),
        },
    )


def draw_trophy() -> Image:
    """The trophy: a cup on a plinth, which is what beating Bobby is worth."""
    return from_map(
        [
            "................",
            "..############..",
            "..#GGGGGGGGGG#..",
            "...#GGGGGGGG#...",
            "..#.#GGGGGG#.#..",
            "..#..#GGGG#..#..",
            "..#...#GG#...#..",
            "...#..####..#...",
            "........##......",
            "........##......",
            ".....######.....",
            "....########....",
            "....#WWWWWW#....",
            "....########....",
            "................",
            "................",
        ],
        {
            "#": (140, 106, 32, 255),
            "G": (232, 196, 88, 255),
            "W": (70, 54, 34, 255),
        },
    )


def draw_guvnor() -> Image:
    """
    The Guv'nor's skin, in the standard sixty-four square layout: a man in a
    flat cap, a brown jacket and boots, with the face of somebody who has been
    stood by that line in the rain since before you were born.
    """
    skin = (214, 168, 132, 255)
    shadow = (186, 141, 108, 255)
    cap = (58, 52, 46, 255)
    cap_light = (74, 67, 60, 255)
    jacket = (94, 66, 42, 255)
    jacket_dark = (74, 51, 32, 255)
    shirt = (206, 202, 190, 255)
    trousers = (48, 52, 62, 255)
    boot = (38, 32, 28, 255)
    eye = (38, 30, 24, 255)

    image = Image(64, 64, (0, 0, 0, 0))

    def block(x0, y0, w, h, colour):
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                image.set(x, y, colour)

    # Head: top, bottom, then right, front, left, back.
    block(8, 0, 16, 8, skin)
    block(0, 8, 8, 8, shadow)
    block(8, 8, 8, 8, skin)
    block(16, 8, 8, 8, shadow)
    block(24, 8, 8, 8, shadow)
    image.set(10, 12, eye)
    image.set(13, 12, eye)
    block(10, 14, 4, 1, shadow)

    # The cap, on the hat layer, with a peak over the front.
    block(40, 0, 16, 8, cap)
    block(32, 8, 8, 3, cap)
    block(40, 8, 8, 3, cap_light)
    block(48, 8, 8, 3, cap)
    block(56, 8, 8, 3, cap)
    # The peak, over the eyes and no further: he has a face under there.
    block(40, 11, 8, 1, cap_light)

    # Jacket over a shirt.
    block(20, 16, 16, 4, jacket)
    block(16, 20, 8, 12, jacket)
    block(24, 20, 8, 12, jacket_dark)
    block(32, 20, 8, 12, jacket)
    block(40, 20, 8, 12, jacket_dark)
    block(26, 20, 4, 6, shirt)

    # Arms.
    for x0 in (40, 32):
        y0 = 16 if x0 == 40 else 48
        block(x0 + 4, y0, 8, 4, jacket)
        block(x0, y0 + 4, 16, 12, jacket)
        block(x0 + 4, y0 + 13, 4, 3, skin)

    # Legs and boots.
    for x0 in (0, 16):
        y0 = 16 if x0 == 0 else 48
        block(x0 + 4, y0, 8, 4, trousers)
        block(x0, y0 + 4, 16, 12, trousers)
        block(x0, y0 + 13, 16, 3, boot)

    return image


def draw_carried_ball() -> Image:
    """The ball as the dog wears it: a 3-cube's worth of chewed tennis ball."""
    image = Image(16, 16, (0, 0, 0, 0))
    base = (186, 205, 74, 255)
    shade = (152, 170, 58, 255)
    seam = (233, 240, 206, 255)

    for y in range(6):
        for x in range(12):
            image.set(x, y, base if (x + y) % 5 else shade)

    # A seam curling over the top and down one side, as they are painted on.
    for x in range(3, 9):
        image.set(x, 0 if x % 2 else 1, seam)

    for y in range(3, 6):
        image.set(3 + (y % 2), y, seam)

    return image


def draw_ball() -> Image:
    """The ball: a chewed tennis ball, seam and all, and slightly the worse for wear."""
    return from_map(
        [
            "................",
            "................",
            "....########....",
            "...##LLLLLL##...",
            "..#LLLLLLLLLL#..",
            ".#LLLSSLLLLLLL#.",
            ".#LLSSLLLLLLLL#.",
            "#LLLSLLLLLLSSLL#",
            "#LLLSLLLLLSSLLL#",
            ".#LLSLLLLSSLLL#.",
            ".#LLLSSSSSLLLL#.",
            "..#LLLLLLLLLL#..",
            "...##LLLLLL##...",
            "....########....",
            "................",
            "................",
        ],
        {
            "#": (108, 122, 38, 255),
            "L": (186, 205, 74, 255),
            "S": (233, 240, 206, 255),
        },
    )


def draw_icon() -> Image:
    """Mod icon: a whippet in profile, standing square, on a warm background."""
    size = 128
    image = Image(size, size, (246, 238, 226, 255))
    edge = (214, 197, 175, 255)
    dog = (92, 66, 45, 255)
    dog_light = (124, 92, 64, 255)

    image.rect(0, 0, size, 3, edge)
    image.rect(0, size - 3, size, 3, edge)
    image.rect(0, 0, 3, size, edge)
    image.rect(size - 3, 0, 3, size, edge)

    def ellipse(cx: float, cy: float, rx: float, ry: float, color) -> None:
        for y in range(size):
            for x in range(size):
                if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0:
                    image.set(x, y, color)

    def line(x0: float, y0: float, x1: float, y1: float, thickness: float, color) -> None:
        steps = int(max(abs(x1 - x0), abs(y1 - y0)) * 3) + 1
        for i in range(steps + 1):
            t = i / steps
            ellipse(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, thickness, thickness, color)

    # Legs first so the body covers their tops.
    line(48, 70, 42, 104, 3.0, dog)
    line(52, 70, 56, 104, 3.0, dog)
    line(84, 70, 80, 104, 3.0, dog)
    line(88, 70, 96, 104, 3.0, dog)
    # Deep chest, tucked waist, powerful hindquarters.
    ellipse(56, 64, 15, 13, dog)
    ellipse(72, 62, 12, 8, dog)
    ellipse(88, 62, 14, 12, dog)
    # Arched neck and narrow head.
    line(50, 58, 40, 40, 5.0, dog)
    ellipse(37, 35, 8, 6, dog)
    ellipse(27, 37, 6, 4, dog)
    image.rect(24, 35, 3, 2, (32, 26, 24, 255))
    # Folded ear and eye.
    ellipse(41, 30, 4, 3, dog_light)
    image.set(33, 34, (246, 238, 226, 255))
    # Whip tail: down off the croup, then a flick up at the tip.
    line(99, 62, 108, 78, 2.5, dog)
    line(108, 78, 116, 72, 2.0, dog)
    line(116, 72, 118, 62, 1.5, dog)
    return image


def main() -> None:
    for coat in COATS:
        draw_coat(coat).write(ENTITY_DIR / f"whippet_{coat.name}.png")
    draw_collar().write(ENTITY_DIR / "whippet_collar.png")
    draw_carried_ball().write(ENTITY_DIR / "ball.png")

    for squirrel in SQUIRRELS:
        draw_squirrel(squirrel).write(SQUIRREL_DIR / f"squirrel_{squirrel.name}.png")

    draw_squirrel_spawn_egg().write(ITEM_DIR / "squirrel_spawn_egg.png")
    draw_spawn_egg().write(ITEM_DIR / "whippet_spawn_egg.png")
    draw_whistle().write(ITEM_DIR / "whippet_whistle.png")
    draw_lure().write(ITEM_DIR / "whippet_lure.png")
    draw_ball().write(ITEM_DIR / "whippet_ball.png")
    draw_trophy().write(ITEM_DIR / "racing_trophy.png")
    draw_guvnor().write(ENTITY_DIR.parent / "guvnor.png")
    draw_icon().write(ASSETS / "icon.png")


if __name__ == "__main__":
    main()
