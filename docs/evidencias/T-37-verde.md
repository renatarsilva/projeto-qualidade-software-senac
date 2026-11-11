# Evidência T-37: o teste de concorrência fica verde com a proteção

> Spec [T-37](../specs/T-37-protecao-contra-dupla-reserva.md), CA-01 a CA-14. Produzida em 2026-11-10. O vermelho sem a proteção está em [`T-37-vermelho.md`](T-37-vermelho.md).

## Comando

```bash
./mvnw test -Dtest='*ConcorrenciaTest,DeadlockNaGravacaoTest'
```

## Resultado com a proteção (verde)

- `Tests run: 23, Failures: 0, Errors: 0`, em 8 execuções seguidas da bateria (e mais 4 antes do último ajuste).
- Suíte completa (`./mvnw verify`): **352 testes, 0 falhas**; o gate do JaCoCo (80% de linhas e 70% de ramos) foi cumprido.
- A contagem vem sempre de **consulta ao banco** (`reserva_recurso` com `ocupa` e período sobreposto), nunca só das respostas.

| Caso | O que prova |
|---|---|
| TC-01, TC-11 | 20 solicitantes, mesma sala e intervalo: 1 × 201, 19 × 409 e 1 reserva ativa, em 5 repetições (`@RepeatedTest(5)`, pool de 25 conexões) |
| TC-02, TC-08 | A menor disputa (2 solicitantes), em 5 repetições |
| TC-03, TC-04, TC-05, TC-06, TC-09 | Sobreposição parcial, encostadas, salas diferentes, duplo clique e recusado pedindo outro horário |
| TC-07, TC-10 | Vários recursos (tudo ou nada) e agenda do professor |
| TC-12, TC-16 | Toda recusa é 409 com mensagem do contrato, sem vazamento, e sem linhas das recusadas no banco |
| TC-13 | Dois `PUT` para o mesmo horário: 1 × 200, 1 × 409 e a recusada mantém o horário |
| TC-14 | 20 inserções diretas no banco: 1 aceita e 19 recusadas por `23P01` (`EXCLUDE`) ou `40P01` (deadlock) |
| TC-17, TC-18 | Recursos em ordem invertida (15 rodadas, sem 500) e o deadlock abortado pelo banco virando 409 genérico |

## O que a correção mudou (DEF-02, ADR-0010)

1. `ReservaService` traduz o deadlock (`40P01`) para o 409 genérico, além da violação do `EXCLUDE`.
2. `AgendaNoBanco` grava os vínculos ordenados por `recurso_id`.
3. Decisão registrada no [ADR-0010](../adr/0010-protecao-contra-dupla-reserva-no-banco.md).

## Ajuste da spec descoberto pelo teste

O TC-14 previa só `SQLState 23P01` nas 19 recusas. Em execuções reais, o PostgreSQL recusou também com `40P01` (deadlock entre inserções conflitantes), e a execução inteira com as 19 recusas em `40P01` é válida. A spec (CA-11 e TC-14) passou a aceitar as duas.
