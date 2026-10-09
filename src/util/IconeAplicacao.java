package util;

import java.awt.Image;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Application artwork remains full color, independently of the UI theme. */
public final class IconeAplicacao {
    private IconeAplicacao() {}

    public static List<Image> imagens() {
        var images = new ArrayList<Image>();
        for (int size : new int[] {16, 24, 32, 48, 64, 128, 256, 512}) {
            try (var in = IconeAplicacao.class.getResourceAsStream("/imagens/app/brModelo-" + size + ".png")) {
                if (in == null) throw new IllegalStateException("Missing generated app icon: " + size);
                images.add(ImageIO.read(in));
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read application icon", e);
            }
        }
        return List.copyOf(images);
    }
}
