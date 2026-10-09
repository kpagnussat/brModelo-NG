import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

/** Compare every decoded PNG pixel, including alpha, with matching relative paths. */
public final class ComparePngs {
    private ComparePngs() {}

    private static Set<Path> images(Path root) throws Exception {
        try (Stream<Path> paths = Files.walk(root)) {
            Set<Path> names = new TreeSet<>();
            paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".png"))
                    .map(root::relativize).forEach(names::add);
            return names;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: ComparePngs <before-dir> <after-dir>");
        Path before = Path.of(args[0]);
        Path after = Path.of(args[1]);
        Set<Path> names = images(before);
        if (names.isEmpty() || !names.equals(images(after))) {
            throw new IllegalStateException("PNG inventories differ or are empty");
        }
        int failures = 0;
        for (Path name : names) {
            BufferedImage a = ImageIO.read(before.resolve(name).toFile());
            BufferedImage b = ImageIO.read(after.resolve(name).toFile());
            if (a == null || b == null) throw new IllegalStateException("Cannot decode " + name);
            if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
                System.out.println("DIFFERENT SIZE " + name);
                failures++;
                continue;
            }
            long changed = 0;
            for (int y = 0; y < a.getHeight(); y++) {
                for (int x = 0; x < a.getWidth(); x++) {
                    if (a.getRGB(x, y) != b.getRGB(x, y)) changed++;
                }
            }
            System.out.println(name + " changedPixels=" + changed);
            if (changed != 0) failures++;
        }
        System.out.println("RESULT TOTAL=" + names.size() + " IDENTICAL=" + (names.size() - failures)
                + " DIFFERENT=" + failures);
        if (failures != 0) throw new IllegalStateException("PNG pixels differ");
    }
}
