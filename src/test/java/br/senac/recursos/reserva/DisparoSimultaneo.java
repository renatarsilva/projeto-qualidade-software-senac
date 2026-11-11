package br.senac.recursos.reserva;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Dispara várias tarefas no mesmo instante: todas as threads ficam prontas e esperam o mesmo sinal de largada
 * ({@code CountDownLatch}). O limite de 30 s só impede o teste de ficar preso (spec T-37, seção 3).
 */
final class DisparoSimultaneo {

    static final int LIMITE_EM_SEGUNDOS = 30;

    private DisparoSimultaneo() {
    }

    /** O que uma solicitação devolveu: quem pediu (posição na lista), o status e o corpo. */
    record Resposta(int indice, int status, String corpo) {
    }

    /** Roda as tarefas ao mesmo tempo e devolve as respostas na ordem das tarefas. */
    static List<Resposta> dispara(List<Callable<Resposta>> tarefas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tarefas.size());
        try {
            CountDownLatch prontas = new CountDownLatch(tarefas.size());
            CountDownLatch largada = new CountDownLatch(1);
            List<Future<Resposta>> futuros = new ArrayList<>();
            for (Callable<Resposta> tarefa : tarefas) {
                futuros.add(pool.submit(() -> {
                    prontas.countDown();
                    largada.await();
                    return tarefa.call();
                }));
            }
            assertThat(prontas.await(LIMITE_EM_SEGUNDOS, TimeUnit.SECONDS)).as("todas as threads prontas").isTrue();
            largada.countDown();
            List<Resposta> respostas = new ArrayList<>();
            for (Future<Resposta> futuro : futuros) {
                respostas.add(espera(futuro));
            }
            return respostas;
        } finally {
            pool.shutdownNow();
        }
    }

    /** Falha de uma solicitação (por exemplo, um 500 que escapa da API) vira falha de asserção, e não erro. */
    private static Resposta espera(Future<Resposta> futuro) throws Exception {
        try {
            return futuro.get(LIMITE_EM_SEGUNDOS, TimeUnit.SECONDS);
        } catch (ExecutionException erro) {
            if (erro.getCause() instanceof AssertionError assercao) {
                throw assercao;
            }
            throw new AssertionError("Uma solicitação falhou com exceção: " + erro.getCause(), erro.getCause());
        } catch (TimeoutException erro) {
            throw new AssertionError("Uma solicitação passou do limite de " + LIMITE_EM_SEGUNDOS + " s", erro);
        }
    }

    static long quantosCom(List<Resposta> respostas, int status) {
        return respostas.stream().filter(r -> r.status() == status).count();
    }
}
