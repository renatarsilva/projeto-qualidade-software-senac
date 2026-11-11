# Mutação manual nas regras críticas

> Task **T-50**. Na mesa, o professor pede para **quebrar uma regra** e mostrar **qual teste falha**. Aqui estão as mutações feitas à mão, com o resultado. Cada mutação foi desfeita depois: o código da `main` é o original e a suíte volta a passar.

## Resumo

| # | Regra crítica | Mutação (o que foi quebrado) | Testes que ficaram vermelhos | Evidência |
|---|---|---|---|---|
| M1 | RN-02 e S-01: períodos `[início, fim)` | Na comparação de sobreposição, `<` virou `<=` (reservas encostadas passam a conflitar) | `SobreposicaoNoMesmoRecursoTest`: `aceitaEncostadasNaSala`, `aceitaEncostadasNoMaterial` e `aceitaEncostadasNoProfessor` (os 6 casos de fronteira) | Execução de 2026-11-11 |
| M2 | RN-15: antecedência mínima de 1 hora | O limite foi reduzido em 1 minuto (passa a aceitar 59 minutos) | `DonoEAntecedenciaDaReservaTest.recusaComCinquentaENoveMinutos` (TC-46) | Execução de 2026-11-11 |
| M3 | RN-04: recurso em manutenção não se reserva | A verificação de manutenção foi removida da regra de reserva (criação e alteração) | `ManutencaoDeRecursosApiTest`: `reservaQueTocaAManutencao` (3 casos), `materialEmManutencao`, `professorEmManutencao`, `tudoOuNadaComManutencao` e os demais de recusa | Execução de 2026-11-11 |
| M4 | RN-03: dupla reserva | O `EXCLUDE` do banco foi removido (só no banco descartável do teste) | `ProtecaoDoBancoConcorrenciaTest` (20 inserções aceitas, esperado 1) e `DisputaPelaMesmaSalaConcorrenciaTest` (14 de 20 solicitações aceitas) | [T-37-vermelho.md](../evidencias/T-37-vermelho.md) |
| M5 | RF-01 e RN-07: autorização | O `PUT /api/salas` foi aberto ao Solicitante | `MatrizDeAutorizacaoApiTest.celulaDeNegacao` (célula S-04 por Solicitante: esperado 403) | [T-49-matriz-de-autorizacao.md](../evidencias/T-49-matriz-de-autorizacao.md) |

## Como repetir

1. Aplique a mutação do quadro acima no arquivo indicado (por exemplo, `SobreposicaoPelaAgenda`, `RegraDeReserva` ou `SalaAcessoConfiguracao`).
2. Rode só os testes da regra: `./mvnw test -Dtest=<ClasseDeTeste>`.
3. Confirme que o teste falha **por asserção**, desfaça a mutação e rode de novo para ver o verde.

Para M4, rode `./mvnw test -Ddupla-reserva.sem-protecao=true -Dtest=ProtecaoDoBancoConcorrenciaTest`.

## O que a mutação prova

O teste não é decorativo: se a regra quebrar, alguma verificação falha. As mutações foram escolhidas nos pontos de maior risco: a fronteira do intervalo (erro de um minuto), o prazo mínimo, a proteção contra dupla reserva e a autorização, que são os limitadores de nota.
