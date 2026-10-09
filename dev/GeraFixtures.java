import controlador.Controler.Comandos;
import controlador.Diagrama;
import diagramas.conceitual.*;
import diagramas.logico.*;
import controlador.Editor;
import desenho.formas.Forma;
import desenho.FormaElementar;
import desenho.preAnyDiagrama.PreCardinalidade.TiposCard;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

/** Invented examples created by the palette/canvas command implementation, then saved by the app. */
public final class GeraFixtures {
    private GeraFixtures() {}

    private static Diagrama diagram(Editor editor, Diagrama.TipoDeDiagrama type) {
        return editor.AddAsAtual(type.name());
    }

    // ExternalRealiseComando is the public entry to the same RealiseComando that
    // mousePressed calls. DoAction starts a fresh palette gesture (clears cliq1/2).
    private static FormaElementar tool(Diagrama d, Comandos command, Point... clicks) {
        d.getEditor().DoAction(new ActionEvent(new JButton(), ActionEvent.ACTION_PERFORMED, command.name()));
        FormaElementar result = null;
        for (Point click : clicks) result = d.ExternalRealiseComando(command, click);
        if (result == null) throw new IllegalStateException("Incomplete palette gesture: " + command);
        return result;
    }

    private static Forma shape(Diagrama d, Comandos command, String text, int x, int y) {
        Forma result = (Forma) tool(d, command, new Point(x, y));
        result.setTexto(text);
        return result;
    }

    private static Point center(Forma f) {
        return new Point(f.getLeft() + f.getWidth() / 2, f.getTop() + f.getHeight() / 2);
    }

    private static void link(Diagrama d, Comandos command, Forma a, Forma b) {
        tool(d, command, a.getMelhorPontoDeLigacao(center(b)), b.getMelhorPontoDeLigacao(center(a)));
    }

    private static Diagrama conceptual(Editor editor) {
        Diagrama d = diagram(editor, Diagrama.TipoDeDiagrama.tpConceitual);
        Entidade aluno = (Entidade) shape(d, Comandos.cmdEntidade, "Aluno", 260, 180);
        Entidade curso = (Entidade) shape(d, Comandos.cmdEntidade, "Curso", 740, 180);
        Relacionamento matricula = (Relacionamento) tool(d, Comandos.cmdLinha,
                new Point(aluno.getLeftWidth() - 2, 209), new Point(curso.getLeft() + 2, 209));
        matricula.setTexto("Matrícula");
        for (var item : d.getListaDeItens()) {
            if (item instanceof Ligacao line && line.getCard() != null) {
                boolean isAluno = line.getFormaPontaA() == aluno || line.getFormaPontaB() == aluno;
                line.getCard().setCard(isAluno ? TiposCard.C0N : TiposCard.C1N);
            }
        }
        Atributo codigo = (Atributo) shape(d, Comandos.cmdAtributo, "Código", 262, 190);
        codigo.setIdentificador(true);
        codigo.DoMove(-80, 0);
        Atributo nome = (Atributo) shape(d, Comandos.cmdAtributo, "Nome", 310, 182);
        nome.DoMove(0, -80);
        Atributo telefones = (Atributo) shape(d, Comandos.cmdAtributo, "Telefones", 262, 225);
        telefones.setMultivalorado(true);
        telefones.DoMove(-80, 55);
        Especializacao esp = (Especializacao) tool(d, Comandos.cmdEspecializacao_Exclusiva, center(aluno));
        esp.setTotal(true);
        esp.DoMove(0, 80);
        Entidade bolsista = d.getListaDeItens().stream().filter(i -> i instanceof Entidade)
                .map(i -> (Entidade) i).filter(i -> i != aluno && i != curso).findFirst().orElseThrow();
        bolsista.setTexto("Bolsista");
        bolsista.DoMove(0, 160);
        EntidadeAssociativa inscricao = (EntidadeAssociativa) shape(d, Comandos.cmdEntidadeAssociativa,
                "Inscrição", 720, 400);
        inscricao.getInterno().setTexto("Em oficina");
        Entidade oficina = (Entidade) shape(d, Comandos.cmdEntidade, "Oficina", 740, 600);
        link(d, Comandos.cmdLinha, curso, inscricao.getInterno());
        link(d, Comandos.cmdLinha, oficina, inscricao.getInterno());
        Forma note = shape(d, Comandos.cmdTexto, "Escola Aurora — exemplo inventado", 200, 700);
        note.SetBounds(200, 700, 500, 45);
        return d;
    }

    private static Campo field(Diagrama d, Tabela table, Comandos command, String text, String type) {
        tool(d, command, new Point(table.getLeft() + 10, table.getTop() + 10));
        Campo result = table.getCampos().get(table.getCampos().size() - 1);
        result.setTexto(text);
        result.setTipo(type);
        paint(d);
        return result;
    }

    private static Point fieldPoint(Tabela table, Campo field) {
        // Hit the same painted field row that the UI uses when dragging a PK to an FK.
        for (int y = table.getTop(); y < table.getTopHeight(); y++) {
            Point p = new Point(table.getLeft() + 25, y);
            if (table.getCampoFromPoint(p) == field) return p;
        }
        throw new IllegalStateException("Field has no canvas row: " + field.getTexto());
    }

    private static Diagrama logical(Editor editor) {
        Diagrama d = diagram(editor, Diagrama.TipoDeDiagrama.tpLogico);
        Tabela aluno = (Tabela) shape(d, Comandos.cmdTabela, "Aluno", 120, 100);
        Campo id = field(d, aluno, Comandos.cmdCampo_Key, "id_aluno", "INTEGER");
        field(d, aluno, Comandos.cmdCampo, "email", "VARCHAR(120)").setUnique(true);
        Tabela curso = (Tabela) shape(d, Comandos.cmdTabela, "Curso", 740, 100);
        Campo idCurso = field(d, curso, Comandos.cmdCampo_Key, "id_curso", "INTEGER");
        field(d, curso, Comandos.cmdCampo, "nome", "VARCHAR(80)");
        Tabela matricula = (Tabela) shape(d, Comandos.cmdTabela, "Matricula", 420, 380);
        field(d, matricula, Comandos.cmdCampo_Key, "id_matricula", "INTEGER");
        Campo fkAluno = field(d, matricula, Comandos.cmdCampo, "id_aluno", "INTEGER");
        Campo fkCurso = field(d, matricula, Comandos.cmdCampo, "id_curso", "INTEGER");
        tool(d, Comandos.cmdLogicoLinha, fieldPoint(aluno, id), fieldPoint(matricula, fkAluno));
        tool(d, Comandos.cmdLogicoLinha, fieldPoint(curso, idCurso), fieldPoint(matricula, fkCurso));
        return d;
    }

    private static void paint(Diagrama d) {
        var graphics = new java.awt.image.BufferedImage(1200, 900,
                java.awt.image.BufferedImage.TYPE_INT_ARGB).createGraphics();
        try { d.ProcessPaint(graphics); } finally { graphics.dispose(); }
    }

    private static void save(Diagrama d, Path out, String name) {
        d.ClearSelect(false);
        for (int pass = 0; pass < 3; pass++) paint(d);
        if (!d.Salvar(out.resolve(name + ".brM3").toFile(), false))
            throw new IllegalStateException("Could not save " + name);
        System.out.println("Generated " + name + ": " + d.getListaDeItens().size() + " elements");
    }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length == 0 ? "test-resources/fixtures" : args[0]);
        Files.createDirectories(out);
        SwingUtilities.invokeAndWait(() -> {
            principal.FramePrincipal frame = new principal.FramePrincipal();
            principal.Aplicacao.fmPrincipal = frame;
            try {
                Editor editor = frame.getEditor();
                save(conceptual(editor), out, "conceitual");
                save(logical(editor), out, "logico");
                Diagrama flow = diagram(editor, Diagrama.TipoDeDiagrama.tpFluxo);
                Forma start = shape(flow, Comandos.cmdFluxIniFim, "Início", 320, 80);
                Forma process = shape(flow, Comandos.cmdFluxProcesso, "Consultar catálogo", 320, 260);
                Forma end = shape(flow, Comandos.cmdFluxIniFim, "Fim", 320, 440);
                start.DoMove(center(process).x - center(start).x, 0);
                end.DoMove(center(process).x - center(end).x, 0);
                link(flow, Comandos.cmdFluxLigacao, start, process);
                link(flow, Comandos.cmdFluxLigacao, process, end);
                save(flow, out, "fluxo");
                Diagrama activity = diagram(editor, Diagrama.TipoDeDiagrama.tpAtividade);
                start = shape(activity, Comandos.cmdInicioAtividade, "", 375, 80);
                process = shape(activity, Comandos.cmdEstadoAtividade, "Reservar livro", 320, 260);
                end = shape(activity, Comandos.cmdFimAtividade, "", 375, 440);
                start.DoMove(center(process).x - center(start).x, 0);
                end.DoMove(center(process).x - center(end).x, 0);
                link(activity, Comandos.cmdLigacaoAtividade, start, process);
                link(activity, Comandos.cmdLigacaoAtividade, process, end);
                save(activity, out, "atividade");
                Diagrama eap = diagram(editor, Diagrama.TipoDeDiagrama.tpEap);
                Forma root = shape(eap, Comandos.cmdEapProcesso, "Feira escolar", 320, 80);
                Forma child = shape(eap, Comandos.cmdEapProcesso, "Preparar espaço", 320, 340);
                link(eap, Comandos.cmdEapLigacao, root, child);
                save(eap, out, "eap");
                Diagrama free = diagram(editor, Diagrama.TipoDeDiagrama.tpLivre);
                Forma library = shape(free, Comandos.cmdLivreRetangulo, "Biblioteca Aurora", 120, 180);
                Forma books = shape(free, Comandos.cmdLivreRetanguloArr, "Acervo", 480, 180);
                link(free, Comandos.cmdLivreLigacaoSimples, library, books);
                save(free, out, "livre");
            } finally { frame.dispose(); }
        });
        System.exit(0);
    }
}
