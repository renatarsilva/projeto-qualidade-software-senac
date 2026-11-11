package br.senac.recursos.reserva;

import br.senac.recursos.seguranca.Perfil;

/** Quem chama: a coluna da matriz de autorização (T-48, T-49). */
enum Quem {
    ANONIMO(null),
    SOLICITANTE(Perfil.SOLICITANTE),
    RESPONSAVEL(Perfil.RESPONSAVEL),
    ADMINISTRADOR(Perfil.ADMINISTRADOR);

    final Perfil perfil;

    Quem(Perfil perfil) {
        this.perfil = perfil;
    }
}
