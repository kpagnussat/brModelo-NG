import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.ViewBox;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import javax.imageio.ImageIO;

/** Rasterizes the single SVG source, without theme substitution or platform tools. */
public final class GenerateIcons {
    public static void main(String[] args) throws Exception {
        var svg = new SVGLoader().load(Path.of(args[0]).toUri().toURL());
        if (svg == null) throw new IllegalArgumentException("Invalid application SVG");
        Path output = Files.createDirectories(Path.of(args[1]));
        int[] sizes = {16, 24, 32, 48, 64, 128, 256, 512};
        var pngs = new ArrayList<byte[]>();
        for (int size : sizes) {
            var image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            var graphics = image.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                svg.render(null, graphics, new ViewBox(0, 0, size, size));
            } finally { graphics.dispose(); }
            var bytes = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bytes);
            pngs.add(bytes.toByteArray());
            Files.write(output.resolve("brModelo-" + size + ".png"), bytes.toByteArray());
        }
        // ICO embeds PNG frames (Vista and later); 0 encodes the 256px frame size.
        int count = sizes.length - 1;
        int offset = 6 + 16 * count;
        var ico = ByteBuffer.allocate(offset + pngs.subList(0, count).stream().mapToInt(p -> p.length).sum())
                .order(ByteOrder.LITTLE_ENDIAN);
        ico.putShort((short) 0).putShort((short) 1).putShort((short) count);
        for (int i = 0; i < count; i++) {
            int size = sizes[i];
            ico.put((byte) size).put((byte) size).put((byte) 0).put((byte) 0)
                    .putShort((short) 1).putShort((short) 32).putInt(pngs.get(i).length).putInt(offset);
            offset += pngs.get(i).length;
        }
        for (int i = 0; i < count; i++) ico.put(pngs.get(i));
        Files.write(output.resolve("brModelo.ico"), ico.array());
        // Modern ICNS chunks contain PNG images, with big-endian chunk lengths.
        String[] types = {"icp4", "icp5", "icp6", "ic07", "ic08", "ic09"};
        int[] indices = {0, 2, 4, 5, 6, 7};
        int length = 8;
        for (int i : indices) length += 8 + pngs.get(i).length;
        var icns = ByteBuffer.allocate(length);
        icns.putInt(0x69636e73).putInt(length);
        for (int i = 0; i < indices.length; i++) {
            icns.put(types[i].getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            icns.putInt(8 + pngs.get(indices[i]).length).put(pngs.get(indices[i]));
        }
        Files.write(output.resolve("brModelo.icns"), icns.array());
    }
}
