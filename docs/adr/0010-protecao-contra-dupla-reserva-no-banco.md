# ADR-0010: A proteção contra dupla reserva é o `EXCLUDE` do banco, com ordem fixa de gravação e tradução do deadlock

- **Status:** Proposta
- **Data:** 2026-11-10
- **Autor(a):** @renatarsilva
- **Task:** T-37

## Contexto

- A dupla reserva (duas reservas ativas para o mesmo recurso e intervalo) é limitador de nota (RF-06, RN-03, RNF-05). A verificação de sobreposição da aplicação é sequencial: sob solicitações simultâneas, duas requisições passam por ela antes de qualquer uma gravar.
- A [arquitetura 3.2](../arquitetura.md) já escolheu o `EXCLUDE USING gist (recurso_id WITH =, periodo WITH &&) WHERE (ocupa)` em `reserva_recurso` e descartou o bloqueio de linha e o nível `SERIALIZABLE`, porque dependem de cada trecho do código lembrar de usá-los. A migração V9 (T-23) o criou. Este ADR não rediscute essa escolha.
- O teste de concorrência da T-37 (20 threads, `CountDownLatch`, PostgreSQL real) mostrou o que a escolha ainda não resolvia: com duas inserções conflitantes e ainda não confirmadas, o PostgreSQL pode abortar uma por **deadlock** (`40P01`) em vez de recusá-la com a violação do `EXCLUDE` (`23P01`), e a aplicação respondia 500 (DEF-02).

## Decisão

1. **A garantia é o `EXCLUDE` do banco.** A verificação da aplicação só dá a mensagem específica quando pega o conflito antes.
2. **Toda recusa do banco é 409.** `23P01` (exclusão) e `40P01` (deadlock) viram o 409 genérico "Esta operação conflita com outro registro", e a transação é desfeita sem gravação parcial.
3. **Os vínculos de uma reserva são gravados ordenados por `recurso_id`.** Dois pedidos com os mesmos recursos em ordem invertida disputam na mesma ordem e não travam um ao outro.
4. **A prova é o teste de concorrência** (`*ConcorrenciaTest`), com a contagem vinda do banco, e a evidência do vermelho sem o `EXCLUDE` e do verde com ele em `docs/evidencias/T-37-vermelho.md` e `T-37-verde.md`.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Lock pessimista (`SELECT ... FOR UPDATE`) | Já descartada na arquitetura 3.2: só protege o código que lembrar de usá-lo, e não protege inserções diretas |
| Nível de isolamento `SERIALIZABLE` | Idem; além disso, exigiria repetir transações que falham por serialização (`40001`) |
| Deixar o deadlock virar 500 | Quebra o contrato de recusa (409) justamente na disputa que o RN-03 testa |
| Só ordenar os recursos, sem traduzir o `40P01` | A ordem resolve o cruzamento entre pedidos de vários recursos, mas não o deadlock entre duas inserções conflitantes do mesmo recurso, que continua possível |

## Consequências

- **Positivas:** uma só garantia, no banco, que vale também para inserções fora da aplicação; o cliente sempre recebe 409 numa disputa; o comportamento é provado com threads de verdade.
- **Negativas e riscos:** o `EXCLUDE` pode demorar um pouco para decidir uma disputa (o PostgreSQL espera `deadlock_timeout`, 1 s por padrão, antes de abortar uma das transações); a ordem de gravação precisa ser mantida por quem mexer em `AgendaNoBanco`.
