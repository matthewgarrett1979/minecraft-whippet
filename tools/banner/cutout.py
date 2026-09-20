import struct, zlib, pathlib, sys
def read_png(p):
    d = pathlib.Path(p).read_bytes(); pos = 8; idat = b''
    while pos < len(d):
        ln = struct.unpack(">I", d[pos:pos+4])[0]; tag = d[pos+4:pos+8]
        data = d[pos+8:pos+8+ln]; pos += 12 + ln
        if tag == b'IHDR': W, H, bd, ct, _, _, _ = struct.unpack(">IIBBBBB", data)
        elif tag == b'IDAT': idat += data
    raw = zlib.decompress(idat); bpp = 3 if ct == 2 else 4
    rows = []; prev = bytearray(W*bpp); i = 0
    for y in range(H):
        f = raw[i]; i += 1; line = bytearray(raw[i:i+W*bpp]); i += W*bpp
        for x in range(len(line)):
            a = line[x-bpp] if x >= bpp else 0; b = prev[x]; c = prev[x-bpp] if x >= bpp else 0
            if f == 1: line[x] = (line[x]+a) & 255
            elif f == 2: line[x] = (line[x]+b) & 255
            elif f == 3: line[x] = (line[x]+(a+b)//2) & 255
            elif f == 4:
                pp = a+b-c; pa = abs(pp-a); pb = abs(pp-b); pc = abs(pp-c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[x] = (line[x]+pr) & 255
        rows.append([tuple(line[x*bpp:(x+1)*bpp][:3]) for x in range(W)]); prev = line
    return W, H, rows
def write_rgba(path, rows):
    H = len(rows); W = len(rows[0])
    raw = bytearray()
    for r in rows:
        raw.append(0)
        for px in r: raw += bytes(px)
    def chunk(t, d): return struct.pack(">I", len(d))+t+d+struct.pack(">I", zlib.crc32(t+d) & 0xffffffff)
    pathlib.Path(path).write_bytes(b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR", struct.pack(">IIBBBBB", W, H, 8, 6, 0, 0, 0))+chunk(b"IDAT", zlib.compress(bytes(raw), 9))+chunk(b"IEND", b""))
src, dst, x0, y0, w, h = sys.argv[1], sys.argv[2], *map(int, sys.argv[3:7])
W, H, rows = read_png(src)
out = []
for y in range(y0, min(y0+h, H)):
    row = []
    for x in range(x0, min(x0+w, W)):
        r, g, b = rows[y][x]
        grass = g > r + 6 and g > b + 6
        # Grass in the mob's own shadow goes dark olive rather than green.
        shadowed_grass = max(r, g, b) < 95 and g >= r and g >= b
        sky = b > r + 14 and b > g + 6
        grass = grass or shadowed_grass
        row.append((0, 0, 0, 0) if (grass or sky) else (r, g, b, 255))
    out.append(row)
# trim to what is left
def solid(row): return any(px[3] for px in row)
top = next((i for i, r in enumerate(out) if solid(r)), 0)
bot = len(out) - next((i for i, r in enumerate(reversed(out)) if solid(r)), 0)
cols = [any(out[y][x][3] for y in range(len(out))) for x in range(len(out[0]))]
left = next((i for i, c in enumerate(cols) if c), 0)
right = len(cols) - next((i for i, c in enumerate(reversed(cols)) if c), 0)
out = [r[left:right] for r in out[top:bot]]
write_rgba(dst, out)
kept = sum(1 for r in out for px in r if px[3])
print(f"{dst} {len(out[0])}x{len(out)} solid px {kept}")
