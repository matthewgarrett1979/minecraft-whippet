import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * The banner: Bonnie, pixelated into the game's idiom, beside the title.
 *
 * Usage: Banner2 bonnie.png out.png width height scale
 */
public class Banner2 {
	static final Color PAPER = new Color(0xEDE6D8);
	static final Color INK = new Color(0x2E2419);
	static final Color MUTED = new Color(0x5A4C3C);
	static final Color RULE = new Color(0x3A2F24);

	public static void main(String[] args) throws Exception {
		BufferedImage bonnie = ImageIO.read(new File(args[0]));
		int w = Integer.parseInt(args[2]);
		int h = Integer.parseInt(args[3]);
		int scale = Integer.parseInt(args[4]);

		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g.setColor(PAPER);
		g.fillRect(0, 0, w, h);

		// Bonnie, flush to the right edge, pixels kept square.
		int bw = bonnie.getWidth() * scale;
		int bh = bonnie.getHeight() * scale;
		int bx = w - bw;
		int by = (h - bh) / 2;
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage(bonnie, bx, by, bw, bh, null);

		g.setColor(RULE);
		g.fillRect(bx - 4, 0, 4, h);

		// Title.
		g.setColor(INK);
		g.setFont(new Font("DejaVu Sans", Font.BOLD, 104));
		g.drawString("Whippets", 86, 214);
		g.setColor(MUTED);
		g.setFont(new Font("DejaVu Sans", Font.PLAIN, 31));
		g.drawString("Tameable sighthounds for Minecraft", 92, 266);
		g.setFont(new Font("DejaVu Sans", Font.PLAIN, 24));
		g.drawString("Fabric · 1.21.11 · racing, zoomies and a duvet habit", 92, 310);
		g.setFont(new Font("DejaVu Sans", Font.ITALIC, 21));
		g.setColor(new Color(0x7A6B58));
		g.drawString("modelled on Bonnie, pictured", 92, 358);

		g.dispose();
		ImageIO.write(img, "png", new File(args[1]));
		System.out.println("wrote " + args[1] + " " + w + "x" + h);
	}
}
