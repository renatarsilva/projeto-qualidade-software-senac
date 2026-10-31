# ADR-0008: Injetar o `Clock` em toda regra que depende da hora atual

- **Status:** Proposta
- **Data:** 2026-10-31
- **Autor(a):** @renatarsilva
- **Task:** T-13

## Contexto

- Regras do sistema dependem de "agora": o job diário marca `NAO_COMPARECEU` nas reservas `APROVADA` cujo início já passou e conclui as `EM_USO` vencidas (S-06, seção 2.4.4 de [`arquitetura.md`](../arquitetura.md)), e a auditoria e a criação de reservas gravam o instante em que ocorreram.
- Testar essas regras exige controlar o tempo. Sem isso, o teste do job precisaria esperar o fim do dia, ou seria frágil e dependeria da hora em que roda.
- A especificação exige testes automatizados de qualidade, e o `CLAUDE.md` já determina `Clock` injetado nas regras que dependem do horário atual.
- Os horários ficam em UTC no banco (F-04).

## Decisão

O `java.time.Clock` é um bean da aplicação (em UTC), e **todo código que precisa da hora atual o recebe por injeção**. Nenhum código de produção chama `Instant.now()`, `LocalDateTime.now()` ou equivalente diretamente.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Chamar `Instant.now()` onde for preciso | O teste não controla o tempo: depende do relógio real, e o cenário "reserva vencida ontem" não é reproduzível. |
| Simular o método estático `now()` nos testes | É frágil, atrelado à ferramenta de simulação, e esconde a dependência do tempo no código. |
| Usar a hora do banco (`now()` do PostgreSQL) | Mistura duas fontes de tempo, e o teste da regra passa a depender do banco para algo que é regra da aplicação. |

## Consequências

- **Positivas:** os testes fixam ou avançam o tempo à vontade, e o job diário é testado sem esperar; o instante de cada auditoria e reserva vem de uma única fonte.
- **Negativas e riscos:** depende de disciplina, porque um `Instant.now()` esquecido reabre o problema sem erro de compilação (liga ao tema TR-01); toda classe com regra de tempo ganha um parâmetro a mais.
- **Como vamos verificar:** o teste do job diário usa um `Clock` fixo e o avança para provar `NAO_COMPARECEU` e a conclusão; a revisão do PR e uma busca por `.now()` em `src/main` não encontram chamadas diretas.
- **Condição de revisão:** rever se a hora precisar vir de uma fonte externa confiável (por exemplo, vários servidores com relógios diferentes).
