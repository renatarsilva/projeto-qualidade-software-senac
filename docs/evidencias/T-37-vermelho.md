# Evidência T-37: o teste de concorrência fica vermelho sem a proteção

> Spec [T-37](../specs/T-37-protecao-contra-dupla-reserva.md), CA-13 e TC-15. Produzida em 2026-11-10. A T-50 pode reaproveitá-la como mutação manual.

## O que foi feito

O `EXCLUDE` (`reserva_recurso_sem_sobreposicao`, migração V9) foi removido **só do banco descartável do teste** (PostgreSQL de Testcontainers, que some ao fim da execução), por `ALTER TABLE ... DROP CONSTRAINT`, ativado pela propriedade `-Ddupla-reserva.sem-protecao=true` em `BaseDeConcorrencia`. **Nenhuma migration foi alterada** e o banco de verdade nunca é tocado.

```bash
./mvnw test -Ddupla-reserva.sem-protecao=true \
  -Dtest='ProtecaoDoBancoConcorrenciaTest,DisputaPelaMesmaSalaConcorrenciaTest'
```

## Resultado sem a proteção (vermelho)

`Tests run: 18, Failures: 16, Errors: 0`. Todas as falhas são **de asserção**, e mostram a dupla reserva acontecendo:

| Teste | Esperado | Obtido sem o `EXCLUDE` |
|---|---|---|
| TC-14: 20 inserções diretas simultâneas no mesmo recurso e período | 1 inserção aceita | **20 aceitas** |
| TC-01 e TC-11: 20 solicitantes pedem a mesma sala e o mesmo intervalo | 1 × 201 | **14 × 201** na primeira repetição (e falha nas 5 repetições) |

Sem a proteção, a verificação da aplicação (sequencial) deixa passar várias solicitações simultâneas, e o sistema grava várias reservas ativas para o mesmo recurso e intervalo. É a dupla reserva, limitador de nota.

## Outro defeito, com a proteção presente (DEF-02)

Com o `EXCLUDE` ativo, a primeira execução da bateria vermelha mostrou 9 falhas de asserção. A causa não era a falta da proteção: o PostgreSQL abortava uma das transações por **deadlock** (`40P01`) e a aplicação respondia **500**. O defeito está em [`docs/defeitos/registro.md`](../defeitos/registro.md) (DEF-02) e foi corrigido no green da T-37.

Para ver o verde, [`T-37-verde.md`](T-37-verde.md).
