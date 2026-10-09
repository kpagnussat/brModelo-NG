/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import javax.swing.JLabel;

/**
 *
 * @author ccandido
 */
public class BrLogger {

    private static volatile JLabel status;
    private static volatile Runnable atualizarIndicador = () -> {};
    private static final ContagemLogs novas = new ContagemLogs();

    public static ContagemLogs.Estado novasMensagens() {
        return novas.estado();
    }

    public static void setAtualizarIndicador(Runnable atualizar) {
        atualizarIndicador = atualizar;
        atualizarInterface(null);
    }

    public static void marcarLidas() {
        novas.marcarLidas();
        atualizarInterface(null);
    }

    private static void atualizarInterface(Excecao entrada) {
        Runnable atualizar = () -> {
            if (entrada != null && ContagemLogs.ehErro(entrada.Tipo)) {
                MensagensStatus.mostrar(status, entrada.Tipo + entrada.Complemento + entrada.Valor,
                        ContagemLogs.ehErro(entrada.Tipo));
            }
            atualizarIndicador.run();
        };
        if (SwingUtilities.isEventDispatchThread()) atualizar.run();
        else SwingUtilities.invokeLater(atualizar);
    }

    private static void registrar(Excecao entrada) {
        synchronized (Logs) {
            Logs.add(entrada);
            novas.receber(entrada.Tipo);
        }
        atualizarInterface(entrada);
    }

    public static JLabel getStatus() {
        return status;
    }

    public static void setStatus(JLabel status) {
        BrLogger.status = status;
    }

    /**
     * Objeto simples para organizar as exceções.
     */
    public static class Excecao {
        public final java.time.LocalDateTime Hora = java.time.LocalDateTime.now();
        public String Tipo = "";
        public String Valor = "";
        public String Complemento = "";
        public Excecao (String tp, String valor) {
            Tipo = tp;
            Valor = valor;
        }
        
        public Excecao (String tp, String valor, String complemento) {
            this(tp, valor);
            Complemento = complemento;
        }

        @Override
        public String toString() {
            return Tipo + ": " + Valor;
        }
    }
            
    /**
     * Lista de pares de erro e mensagens.
     */
    public final static ArrayList<Excecao> Logs = new ArrayList<>();
    
    public static void Logger(String rpt, String exception) {
        Excecao p = new Excecao(rpt , (exception != null ? " (java: " + exception + ")" : ""));
        registrar(p);
    }

    public static void Logger(String rpt, String complemento, String exception) {
        registrar(new Excecao(rpt, (exception != null ? " (java: " + exception + ")" : ""), complemento));
    }
    
    public static void Clean() {
        synchronized (Logs) {
            Logs.clear();
            novas.marcarLidas();
        }
        Runnable limpar = () -> {
            MensagensStatus.parar(status);
            if (status != null) status.setText("");
            atualizarIndicador.run();
        };
        if (SwingUtilities.isEventDispatchThread()) limpar.run();
        else SwingUtilities.invokeLater(limpar);
    }
}
