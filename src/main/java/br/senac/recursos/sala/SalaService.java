package br.senac.recursos.sala;

import org.springframework.stereotype.Service;

@Service
public class SalaService {

    private final SalaRepository salas;

    public SalaService(SalaRepository salas) {
        this.salas = salas;
    }

    public Sala cadastra(SalaRequest pedido) {
        boolean restrito = Boolean.TRUE.equals(pedido.restrito()); // P-03: omitido no cadastro vale false
        Sala sala = new Sala(pedido.nome(), restrito, pedido.capacidade(), pedido.localizacao(), pedido.tipoSala());
        return salas.save(sala);
    }
}
