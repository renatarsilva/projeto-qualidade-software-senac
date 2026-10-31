# ADR-0004: Adotar o PostgreSQL como banco de dados

- **Status:** Proposta
- **Data:** 2026-10-31
- **Autor(a):** @renatarsilva
- **Task:** T-13

## Contexto

- A [especificação do projeto](https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html) exige que "persistência e integrações" sejam testadas em ambiente real (Testcontainers), e que duas solicitações simultâneas produzam uma única reserva aceita (RN-03, RNF-05).
- A regra central é que reservas do mesmo recurso não se sobreponham (RN-02). A arquitetura decide que **o banco garante isso**, com uma restrição de exclusão sobre um intervalo de tempo (seção 3.2 de [`arquitetura.md`](../arquitetura.md), suposições S-01 e S-02, decisão D-02).
- O esqueleto do projeto já usa PostgreSQL 16 (`pom.xml`, `compose.yaml` e `TestcontainersConfiguration`).

## Decisão

O PostgreSQL 16 é o único banco de dados do sistema, e a restrição anti-conflito usa `EXCLUDE USING gist` sobre `tstzrange`.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| H2 em memória | Não tem restrição de exclusão nem tipo de intervalo, e testar com um banco diferente do real esconde defeitos. O `CLAUDE.md` já proíbe H2 nos testes de integração. |
| MySQL | Também não tem restrição de exclusão nem tipo de intervalo. A dupla reserva teria que ser evitada por bloqueio de linha ou isolamento `SERIALIZABLE` no código, alternativa descartada na seção 3.2 porque um esquecimento a reabre. |
| Banco sem esquema relacional (NoSQL) | Sem chaves estrangeiras nem restrições declarativas, as regras RN-02, RN-08 e RN-09 ficariam todas só no código. |

## Consequências

- **Positivas:** a dupla reserva, que é limitador de nota, fica impossível mesmo se o código errar; os testes usam o mesmo motor da produção (`postgres:16-alpine`); a pilha não tem licença paga.
- **Negativas e riscos:** a restrição de exclusão depende da extensão `btree_gist`, que precisa de permissão no banco; a solução fica presa ao PostgreSQL; os testes precisam de Docker. Riscos ligados: R-02 (`ocupa` divergente) e R-04 (sem índice definido para a pesquisa de disponibilidade).
- **Como vamos verificar:** o teste de concorrência (T-37) com Testcontainers mostra 1 reserva ativa em 20 pedidos simultâneos (CEN-02); a migração que cria a extensão roda do zero em um banco limpo.
- **Condição de revisão:** rever se o ambiente de entrega não permitir instalar a extensão `btree_gist`, ou se a meta de desempenho (RNF-09) não for atendida com os índices disponíveis.
