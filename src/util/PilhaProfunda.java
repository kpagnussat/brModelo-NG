package util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Runs Java serialization of a whole diagram on a thread with a deep stack.
 *
 * ObjectOutputStream/ObjectInputStream walk the shape graph recursively (shape → line → point →
 * shape ...), so the stack depth grows with how interconnected the diagram is. A conceptual
 * diagram of ~2,000 items already needs more than the 1 MB a default thread (and the EDT) gets,
 * and then opening, saving and every undo snapshot fail with StackOverflowError — the original
 * brModelo has the same limit. The stack size is only reserved address space: pages are
 * committed as the recursion actually reaches them.
 */
public final class PilhaProfunda {
    private PilhaProfunda() {}

    static final long PILHA = 256L << 20;
    private static final String NOME = "brModelo-serializacao";

    /** {@code out.writeObject(object)} on a deep-stack thread. */
    public static void escrever(ObjectOutput out, Object object) throws IOException {
        Object erro = executar(() -> { out.writeObject(object); return null; });
        if (erro instanceof IOException e) throw e;
    }

    /** {@code in.readObject()} on a deep-stack thread. */
    public static Object ler(ObjectInput in) throws IOException, ClassNotFoundException {
        Object[] lido = new Object[1];
        Object erro = executar(() -> { lido[0] = in.readObject(); return null; });
        if (erro instanceof IOException e) throw e;
        if (erro instanceof ClassNotFoundException e) throw e;
        return lido[0];
    }

    private interface Tarefa {
        Object executar() throws Exception;
    }

    // Returns the task's checked exception, if any; unchecked ones and errors are rethrown here.
    private static Exception executar(Tarefa tarefa) {
        Throwable[] erro = new Throwable[1];
        Runnable corpo = () -> {
            try {
                tarefa.executar();
            } catch (Throwable t) {
                erro[0] = t;
            }
        };
        if (NOME.equals(Thread.currentThread().getName())) {
            corpo.run(); // already deep, e.g. a nested envelope read inside a read
        } else {
            Thread thread = new Thread(null, corpo, NOME, PILHA);
            thread.setDaemon(true);
            thread.start();
            boolean interrompida = false;
            while (true) {
                try {
                    thread.join();
                    break;
                } catch (InterruptedException e) {
                    interrompida = true; // the caller still needs the outcome; restore the flag after
                }
            }
            if (interrompida) Thread.currentThread().interrupt();
        }
        if (erro[0] instanceof RuntimeException e) throw e;
        if (erro[0] instanceof Error e) throw e;
        return (Exception) erro[0];
    }
}
