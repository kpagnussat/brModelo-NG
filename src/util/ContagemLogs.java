package util;

/** Counts log entries received since the last read acknowledgement, without Swing state. */
public final class ContagemLogs {
    private int erros;
    private int avisos;

    public static boolean ehErro(String chave) {
        return chave != null && (chave.startsWith("ERROR") || chave.startsWith("ERRO") || chave.startsWith("Controler.ERRO"));
    }

    public synchronized void receber(String chave) {
        if (ehErro(chave)) erros++;
        else avisos++;
    }

    public synchronized void marcarLidas() {
        erros = 0;
        avisos = 0;
    }

    public synchronized Estado estado() {
        return new Estado(erros, avisos);
    }

    /** An immutable snapshot keeps icon, count and tooltip consistent across threads. */
    public record Estado(int erros, int avisos) {
        public int total() { return erros + avisos; }
        public String icone() {
            return erros > 0 ? "log_erro" : avisos > 0 ? "log_info" : "log_lido";
        }
        public String descricao() {
            if (total() == 0) return "Nenhuma mensagem nova";
            String texto = erros > 0 ? erros + (erros == 1 ? " erro" : " erros") : "";
            if (avisos > 0) {
                if (!texto.isEmpty()) texto += ", ";
                texto += avisos + (avisos == 1 ? " aviso" : " avisos");
            }
            return texto + " — clique para ver o log";
        }
    }
}
