package br.senac.recursos.manutencao;

import br.senac.recursos.reserva.RecursoPedido;
import br.senac.recursos.reserva.VerificadorDeManutencao;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Manutenção no PostgreSQL. Esqueleto da T-35: nunca vê manutenção até a implementação. */
@Component
public class ManutencaoNoBanco implements VerificadorDeManutencao {

    @Override
    public boolean emManutencao(RecursoPedido recurso, Instant inicio, Instant termino) {
        return false;
    }
}
