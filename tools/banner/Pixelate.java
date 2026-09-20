import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;

/**
 * Turns a photograph into Minecraft-ish pixel art: box-downsample to a coarse
 * grid, then flatten the palette with k-means so the result reads as blocks of
 * flat colour rather than a blurry photo.
 *
 * Usage: Pixelate in.jpg out.png cropX cropY cropW cropH cellsWide colours
 */
public class Pixelate {
	public static void main(String[] args) throws Exception {
		BufferedImage src = ImageIO.read(new File(args[0]));
		int cx = Integer.parseInt(args[2]);
		int cy = Integer.parseInt(args[3]);
		int cw = Integer.parseInt(args[4]);
		int ch = Integer.parseInt(args[5]);
		int cols = Integer.parseInt(args[6]);
		int palette = Integer.parseInt(args[7]);

		if (cw <= 0) {
			cx = 0; cy = 0; cw = src.getWidth(); ch = src.getHeight();
		}

		int rows = Math.max(1, (int) Math.round(cols * (double) ch / cw));
		int[][] pix = new int[rows][cols];

		// Box average each cell.
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				int x0 = cx + c * cw / cols, x1 = cx + (c + 1) * cw / cols;
				int y0 = cy + r * ch / rows, y1 = cy + (r + 1) * ch / rows;
				long sr = 0, sg = 0, sb = 0, n = 0;

				for (int y = y0; y < Math.max(y0 + 1, y1); y++) {
					for (int x = x0; x < Math.max(x0 + 1, x1); x++) {
						if (x < 0 || y < 0 || x >= src.getWidth() || y >= src.getHeight()) continue;
						int rgb = src.getRGB(x, y);
						sr += (rgb >> 16) & 0xFF; sg += (rgb >> 8) & 0xFF; sb += rgb & 0xFF; n++;
					}
				}

				n = Math.max(n, 1);
				pix[r][c] = (int) ((sr / n) << 16 | (sg / n) << 8 | (sb / n));
			}
		}

		// k-means over the cells to flatten the palette.
		List<int[]> centres = new ArrayList<>();
		Random rng = new Random(7);
		for (int i = 0; i < palette; i++) {
			int v = pix[rng.nextInt(rows)][rng.nextInt(cols)];
			centres.add(new int[] {(v >> 16) & 0xFF, (v >> 8) & 0xFF, v & 0xFF});
		}

		int[][] assign = new int[rows][cols];
		for (int iter = 0; iter < 14; iter++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					int v = pix[r][c];
					int vr = (v >> 16) & 0xFF, vg = (v >> 8) & 0xFF, vb = v & 0xFF;
					int best = 0; long bestD = Long.MAX_VALUE;

					for (int i = 0; i < centres.size(); i++) {
						int[] k = centres.get(i);
						long d = (long) (vr - k[0]) * (vr - k[0]) + (long) (vg - k[1]) * (vg - k[1]) + (long) (vb - k[2]) * (vb - k[2]);
						if (d < bestD) { bestD = d; best = i; }
					}

					assign[r][c] = best;
				}
			}

			long[][] sums = new long[centres.size()][4];
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					int v = pix[r][c]; int i = assign[r][c];
					sums[i][0] += (v >> 16) & 0xFF; sums[i][1] += (v >> 8) & 0xFF; sums[i][2] += v & 0xFF; sums[i][3]++;
				}
			}

			for (int i = 0; i < centres.size(); i++) {
				if (sums[i][3] > 0) {
					centres.set(i, new int[] {
						(int) (sums[i][0] / sums[i][3]), (int) (sums[i][1] / sums[i][3]), (int) (sums[i][2] / sums[i][3])});
				}
			}
		}

		BufferedImage out = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_RGB);
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				int[] k = centres.get(assign[r][c]);
				out.setRGB(c, r, k[0] << 16 | k[1] << 8 | k[2]);
			}
		}

		ImageIO.write(out, "png", new File(args[1]));
		System.out.println("wrote " + args[1] + " " + cols + "x" + rows + " from " + src.getWidth() + "x" + src.getHeight());
	}
}
