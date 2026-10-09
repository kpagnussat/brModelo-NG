package util;

import java.awt.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EscalaTest {
    // Guards fixed column width for text fields even when the content would prefer a wider size.
    @Test void textWidthScalesButDoesNotFollowText() {
        JTextField field = new JTextField("A very long invented field name");
        field.setFont(new Font(Font.DIALOG, Font.PLAIN, 24));
        field.setPreferredSize(new Dimension(999, 999));
        Dimension result = Escala.ajuste(field, 80, 20);
        assertEquals(160, result.width);
        assertEquals(Math.max(20, field.getPreferredSize().height), result.height);
    }

    // Guards natural button widths so larger labels stay readable.
    @Test void buttonsMayGrowNaturally() {
        JButton button = new JButton("An unusually long invented action name");
        button.setFont(new Font(Font.DIALOG, Font.PLAIN, 24));
        Dimension natural = button.getPreferredSize();
        assertEquals(new Dimension(Math.max(160, natural.width), Math.max(20, natural.height)), Escala.ajuste(button, 80, 20));
    }

    // Guards aligned checkbox columns despite long labels and scaled fonts.
    @Test void togglesKeepColumnWidth() {
        JCheckBox toggle = new JCheckBox("An unusually long invented field");
        toggle.setFont(new Font(Font.DIALOG, Font.PLAIN, 18));
        assertEquals(60, Escala.ajuste(toggle, 40, 20).width);
    }

    // Guards the minimum scale and caller-specified height at small or missing font sizes.
    @Test void minimumScaleAndHeight() {
        JTextField field = new JTextField();
        field.setFont(new Font(Font.DIALOG, Font.PLAIN, 8));
        assertEquals(new Dimension(80, 100), Escala.ajuste(field, 80, 100));
        field.setFont(null);
        assertEquals(1f, Escala.fator(field));
    }
}
