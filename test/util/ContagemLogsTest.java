package util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContagemLogsTest {
    @Test void unreadInfoThenErrorsThenReadThenNewInfo() {
        ContagemLogs logs = new ContagemLogs();
        assertEquals("log_lido", logs.estado().icone());
        assertEquals("Nenhuma mensagem nova", logs.estado().descricao());
        assertEquals(0, logs.estado().total());

        logs.receber("MSG_LOAD");
        assertEquals("log_info", logs.estado().icone());
        assertEquals("1 aviso — clique para ver o log", logs.estado().descricao());
        logs.receber("ERROR_LOAD");
        logs.receber("ERRO_SAME_FILE");
        assertEquals("log_erro", logs.estado().icone());
        assertEquals(3, logs.estado().total());
        assertEquals("2 erros, 1 aviso — clique para ver o log", logs.estado().descricao());
        logs.receber("MSG_SAVE");
        assertEquals("log_erro", logs.estado().icone(), "Info never hides unread errors");
        assertEquals("2 erros, 2 avisos — clique para ver o log", logs.estado().descricao());

        logs.marcarLidas();
        logs.marcarLidas();
        assertEquals(0, logs.estado().total());
        assertEquals("log_lido", logs.estado().icone());
        logs.receber("MSG_SAVE");
        assertEquals("log_info", logs.estado().icone());
        assertEquals(1, logs.estado().total());
    }

    @Test void keyClassificationAndErrorOnlyTooltip() {
        ContagemLogs logs = new ContagemLogs();
        for (String chave : new String[]{"MSG_TEST", "warning", "PREFIX_ERROR", "error", null}) {
            logs.receber(chave);
        }
        assertTrue(ContagemLogs.ehErro("Controler.ERRO_SAME_FILE"));
        assertEquals(5, logs.estado().avisos());
        assertEquals(0, logs.estado().erros());
        logs.marcarLidas();
        logs.receber("ERROR");
        assertEquals("1 erro — clique para ver o log", logs.estado().descricao());
        logs.receber("ERRO");
        assertEquals("2 erros — clique para ver o log", logs.estado().descricao());
    }

    @Test void immutableSnapshotsAndConcurrentWriters() throws Exception {
        ContagemLogs logs = new ContagemLogs();
        var before = logs.estado();
        Thread info = new Thread(() -> {
            for (int i = 0; i < 1000; i++) logs.receber("MSG_TEST");
        });
        Thread errors = new Thread(() -> {
            for (int i = 0; i < 1000; i++) logs.receber("ERROR_TEST");
        });
        info.start();
        errors.start();
        info.join();
        errors.join();
        assertEquals(2000, logs.estado().total());
        assertEquals(1000, logs.estado().erros());
        assertEquals(1000, logs.estado().avisos());
        assertEquals(0, before.total());
    }
}
