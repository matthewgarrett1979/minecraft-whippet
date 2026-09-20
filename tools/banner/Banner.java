import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Draws the Whippets banner.
 *
 * The dog is the actual mob, cut out of an in-game screenshot and scaled with
 * nearest-neighbour so the pixels stay square — a drawing of a whippet would be
 * a promise the mod has to keep, and this way the banner cannot lie.
 */
public class Banner {
	static final Color SKY_TOP = new Color(0x8CBFEE);
	static final Color SKY_LOW = new Color(0xDCEAF6);
	static final Color GRASS = new Color(0x7DA450);
	static final Color GRASS_DARK = new Color(0x5F8740);
	static final Color INK = new Color(0x2C2119);

	public static void main(String[] args) throws Exception {
		String dogPath = args[0];
		String outPath = args[1];
		int w = Integer.parseInt(args[2]);
		int h = Integer.parseInt(args[3]);
		int scale = Integer.parseInt(args[4]);

		BufferedImage dog = ImageIO.read(new File(dogPath));
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		// Sky over a band of grass, with the horizon under the dog's feet.
		int horizon = (int) (h * 0.72);
		g.setPaint(new GradientPaint(0, 0, SKY_TOP, 0, horizon, SKY_LOW));
		g.fillRect(0, 0, w, horizon);
		g.setPaint(new GradientPaint(0, horizon, GRASS, 0, h, GRASS_DARK));
		g.fillRect(0, horizon, w, h - horizon);

		int dw = dog.getWidth() * scale;
		int dh = dog.getHeight() * scale;
		int dx = w - dw - (int) (w * 0.08);
		int dy = horizon - dh + scale * 3;

		// A soft shadow so it is standing on the grass rather than hovering.
		g.setColor(new Color(0x3A, 0x54, 0x28, 70));
		g.fill(new Ellipse2D.Double(dx + dw * 0.16, horizon - scale * 2, dw * 0.66, scale * 6));

		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage(dog, dx, dy, dw, dh, null);

		// Title block, vertically centred against the dog.
		int baseline = (int) (h * 0.42);
		g.setColor(INK);
		g.setFont(new Font("DejaVu Sans", Font.BOLD, (int) (h * 0.19)));
		g.drawString("Whippets", (int) (w * 0.07), baseline);
		g.setFont(new Font("DejaVu Sans", Font.PLAIN, (int) (h * 0.060)));
		g.setColor(new Color(0x3C3128));
		g.drawString("Tameable sighthounds for Minecraft", (int) (w * 0.073), baseline + (int) (h * 0.095));
		g.setFont(new Font("DejaVu Sans", Font.PLAIN, (int) (h * 0.046)));
		g.setColor(new Color(0x5A4C3E));
		g.drawString("Fabric · 1.21.11 · racing, zoomies and a duvet habit",
			(int) (w * 0.073), baseline + (int) (h * 0.165));

		g.dispose();
		ImageIO.write(img, "png", new File(outPath));
		System.out.println("wrote " + outPath + " " + w + "x" + h);
	}
}
