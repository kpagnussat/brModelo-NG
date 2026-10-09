package controlador.editores;

import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("gui")
class MostradorDeCodigoTest {
    @Test void keywordAtLineStartKeepsItsLastLetter() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var code = new MostradorDeCodigo(null, false);
            try {
                // Formate pads every line with spaces, so a leading keyword is found at 0.
                assertEquals("<b>CREATE</b> TABLE ", code.injete(" CREATE TABLE ", "CREATE", "<b>", "</b>", 0));
                assertEquals(" A <b>NULL</b>,", code.injete(" A NULL,", "NULL", "<b>", "</b>", 2));
                code.setTexto("CREATE TABLE Aluno (\n  id INTEGER PRIMARY KEY\n);\n");
                String html = code.lblHtml.getText().replaceAll("<[^>]*>", "");
                assertTrue(html.contains("CREATE TABLE Aluno"), html);
            } finally {
                code.dispose();
            }
        });
    }

    @Test void codeStartsAtTheTopOfTheViewer() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var code = new MostradorDeCodigo(null, false);
            try {
                assertEquals(SwingConstants.TOP, code.lblHtml.getVerticalAlignment());
            } finally {
                code.dispose();
            }
        });
    }
}
