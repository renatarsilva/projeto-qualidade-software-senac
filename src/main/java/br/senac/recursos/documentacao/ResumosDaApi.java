package br.senac.recursos.documentacao;

import java.util.Map;

/** O resumo de cada endpoint da API, em português, para a documentação OpenAPI (RF-14). A chave é "MÉTODO caminho". */
final class ResumosDaApi {

    static final Map<String, String> RESUMOS = Map.ofEntries(
            Map.entry("POST /api/auth/login", "Entra no sistema e devolve o token de acesso (30 minutos)"),
            Map.entry("POST /api/usuarios", "Cria um usuário com perfil"),
            Map.entry("GET /api/usuarios", "Lista os usuários"),
            Map.entry("PATCH /api/usuarios/{id}/perfil", "Troca o perfil de um usuário"),
            Map.entry("PATCH /api/usuarios/{id}/ativo", "Ativa ou desativa um usuário"),
            Map.entry("GET /api/salas", "Lista as salas"),
            Map.entry("GET /api/salas/{id}", "Consulta uma sala"),
            Map.entry("POST /api/salas", "Cadastra uma sala (nome no padrão sala-NN)"),
            Map.entry("PUT /api/salas/{id}", "Altera uma sala"),
            Map.entry("GET /api/professores", "Lista os professores"),
            Map.entry("GET /api/professores/{id}", "Consulta um professor"),
            Map.entry("POST /api/professores", "Cadastra um professor (nome primeironome-ultimonome-matrícula)"),
            Map.entry("PUT /api/professores/{id}", "Altera um professor e as suas competências"),
            Map.entry("DELETE /api/professores/{id}", "Exclui um professor sem vínculos"),
            Map.entry("GET /api/materiais", "Lista os materiais"),
            Map.entry("GET /api/materiais/{id}", "Consulta um material"),
            Map.entry("POST /api/materiais", "Cadastra um material com patrimônio único"),
            Map.entry("PUT /api/materiais/{id}", "Altera um material"),
            Map.entry("DELETE /api/materiais/{id}", "Exclui um material sem vínculos"),
            Map.entry("GET /api/recursos", "Pesquisa recursos por tipo, capacidade, localização, competência e disponibilidade"),
            Map.entry("POST /api/recursos/{recursoId}/manutencoes", "Registra um período de manutenção do recurso"),
            Map.entry("GET /api/recursos/{recursoId}/manutencoes", "Lista os períodos de manutenção do recurso"),
            Map.entry("DELETE /api/recursos/{recursoId}/manutencoes/{manutencaoId}", "Encerra um período de manutenção"),
            Map.entry("POST /api/reservas", "Cria uma reserva (intervalo, recursos e sem sobreposição)"),
            Map.entry("PUT /api/reservas/{id}", "Altera uma reserva do próprio dono"),
            Map.entry("GET /api/reservas", "Lista as reservas (o Solicitante vê só as próprias)"),
            Map.entry("GET /api/reservas/{id}", "Consulta uma reserva (o Solicitante só a própria)"),
            Map.entry("POST /api/reservas/{id}/cancelamento", "Cancela a reserva do próprio dono"),
            Map.entry("POST /api/reservas/{id}/checkin", "Registra o check-in do dono, dentro do horário reservado"),
            Map.entry("PATCH /api/reservas/{id}/estado", "O Responsável aprova ou rejeita uma reserva"),
            Map.entry("DELETE /api/reservas/{id}", "Recusa apagar: reservas nunca são apagadas, só canceladas"),
            Map.entry("GET /api/reservas/{id}/auditoria", "Consulta o histórico de mudanças de estado da reserva"),
            Map.entry("POST /api/reservas/{reservaId}/retiradas", "Registra a retirada de um material da reserva"),
            Map.entry("POST /api/reservas/{reservaId}/retiradas/{retiradaId}/devolucao", "Registra a devolução do material"),
            Map.entry("GET /api/reservas/{reservaId}/retiradas", "Lista as retiradas e devoluções da reserva"),
            Map.entry("GET /api/relatorios/utilizacao", "Relatório de utilização: horas por recurso, carga do professor e conflitos evitados"));

    private ResumosDaApi() {
    }
}
