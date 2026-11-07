# Spec T-21: ciclo de vida da reserva

| Campo | Valor |
|---|---|
| Task | T-21 (a mesma spec vale para a implementação, T-24) |
| Autor(a) | @renatarsilva |
| Revisor(a) | @Samwel-Merces |
| Status | Em revisão |
| Regra crítica | Sim |
| Requisitos | RF-04, RF-07 |
| Característica ISO 25010 | Adequação funcional |
| Criada em | 2026-11-04 |

## 1. Contexto e objetivo

Uma reserva passa por estados (`SOLICITADA`, `APROVADA`, `EM_USO`, `CONCLUIDA` e os alternativos `REJEITADA`, `CANCELADA`, `NAO_COMPARECEU`), e só algumas mudanças fazem sentido: não se conclui uma reserva que nunca foi aprovada, e uma reserva cancelada não volta. Esta spec define **quais transições são permitidas e quais são proibidas**, quem pode cancelar, como o cancelamento libera o horário e por que uma reserva nunca é apagada (RN-05, RN-06, RN-08, RN-13 e RN-14, as duas últimas registradas no PRD como regras do time). Os testes da T-21 nascem vermelhos a partir desta tabela e a T-24 os deixa verdes. Os casos vêm do [plano TDD](../testes/plano-tdd.md).

## 2. Escopo

**Dentro:**
- A tabela de transições de estado (válidas e proibidas), incluindo estado final e estado inexistente.
- O cancelamento pelo dono e seu efeito conforme o instante e o check-in (`CANCELADA` até o início; depois do início, `CONCLUIDA` se houve check-in ou `NAO_COMPARECEU` se não houve).
- O check-in do dono (`POST /api/reservas/{id}/checkin`), que leva `APROVADA` a `EM_USO`: o contrato e a regra são desta spec; o endpoint e a tela são da T-59.
- Só o dono cancela ou altera a própria reserva (NC-01).
- Os prazos de aprovação e de alteração (24 h antes do início; 1 h antes se o pedido foi feito com menos de 24 h de antecedência), guardados em `prazo_aprovacao_em`. Intervalo e sobreposição ao alterar continuam na T-20.
- Quem pode pedir cada mudança pelo `PATCH`, por perfil e por destino (CA-14), e a ordem dos erros 401, 403, 404, 400 e 409 (CA-17).
- Reserva nunca é apagada.
- Contrato dos endpoints de mudança de estado, cancelamento e exclusão.

**Fora (e por quê):**
- A aprovação de recurso restrito com TDD estrito e a notificação ao Solicitante (RN-07): semanas 41 e 44, T-34. Aqui só se define quem pode pedir `APROVADA` e `REJEITADA` pelo `PATCH`: **somente o Responsável**, para qualquer reserva (CA-14).
- Reservas afetadas por manutenção (S-05) e o Administrador cancelar reservas de outros: semana 41. Nesta spec só o dono cancela ou altera.
- O job periódico que aplica as mudanças por tempo (`NAO_COMPARECEU`, `CONCLUIDA` e o cancelamento por prazo de aprovação, S-06): é da T-60 (a criar no backlog). Esta spec define as transições que ele usa, não o job, e depende da T-60 para elas acontecerem sozinhas.
- Auditoria de cada mudança (RN-09, incluindo os casos RN-13-T13 e o motivo de RN-06-T9): semana 43.
- A chave estrangeira que impede `DELETE` direto no banco (RN-08-T9): é do esquema do banco, na arquitetura 3.2.
- A tela e o endpoint do check-in: são da T-59. O formato das respostas de erro é da T-22.
- Intervalo e sobreposição ao alterar: são da T-20.

## 3. Regras de negócio

- **RN-05 (oficial):** o fluxo principal é `SOLICITADA → APROVADA → EM_USO → CONCLUIDA`. Não se pula nem se volta pelo `PATCH`. A única volta é a alteração pelo `PUT` (T-20, CA-11), que reinicia a reserva aprovada em `SOLICITADA`.
- **RN-06 (alternativos oficiais; as transições são propostas):** os estados alternativos são `REJEITADA`, `CANCELADA` e `NAO_COMPARECEU`.
- **RN-08 (oficial):** reservas iniciadas não podem ser apagadas. Pela premissa L11 e pela restrição 5 da arquitetura, **nenhuma** reserva é apagada: o caminho é cancelar.
- **RN-13 (regra do time, registrada no PRD):** o dono cancela até o instante do início (`CANCELADA`, horário inteiro livre). Depois do início e antes do fim: se já houve check-in (`EM_USO`), é um encerramento antecipado (`CONCLUIDA`, resto livre); se não houve check-in (`APROVADA`), é `NAO_COMPARECEU` (resto livre). Depois do fim, não há o que cancelar.
- **Fonte que vale:** o PRD (`docs/prd.md`): RN-13 (cancelamento), RN-14 (prazos de aprovação e alteração) e RN-15 (antecedência mínima), regras do time. O plano TDD (PR #33) é atualizado em outro commit para refletir essas decisões; em caso de divergência vale o PRD.
- **Prazos (RN-14, decisão do time):** cada reserva guarda `prazo_aprovacao_em`, calculado na criação e **recalculado na alteração, com o novo início**. O prazo é **24 h antes do início** quando o instante do pedido (ou da alteração, pelo `Clock`) tem pelo menos 24 h de antecedência; senão, é **1 h antes do início**. Um pedido com menos de 1 h de antecedência é recusado na criação (RN-15, T-20). O mesmo instante vale para os dois prazos: a aprovação é aceita e o dono pode alterar a reserva **até `prazo_aprovacao_em`, inclusive**, usando o início atual da reserva. Depois dele, a aprovação e a alteração são recusadas com 409, mesmo que o job ainda não tenha rodado, e o job (T-60) cancela a `SOLICITADA` quando o instante passa do prazo (instante > prazo). A `criada_em` é o instante em que o pedido é aceito, vem do `Clock` e nunca muda (T-20); esta spec usa `prazo_aprovacao_em`, não `criada_em`.
- **Alterar reserva aprovada:** a alteração de uma `APROVADA` a devolve a `SOLICITADA` e recalcula `prazo_aprovacao_em` com o novo início. Como o novo início tem pelo menos 1 h de antecedência (RN-15), o novo prazo nunca está no passado, e a reserva não é cancelada no mesmo instante por causa da alteração.
- **Mudanças por tempo (P-02, proposta do time):** `NAO_COMPARECEU`, `CONCLUIDA` pelo fim e o cancelamento por prazo de aprovação são aplicados por um job periódico (a cada minuto), que é a T-60. A regra aceita a transição **a partir** do instante; o atraso até o job rodar é de até um minuto.
- **NC-01:** o Solicitante altera e cancela somente as próprias reservas.
- **Quem aprova (RN-07 e PRD):** somente o **Responsável** aprova ou rejeita (`SOLICITADA→APROVADA` e `SOLICITADA→REJEITADA`), em qualquer reserva, restrita ou não. O Administrador **não** aprova, e o Solicitante também não. A aprovação de recurso restrito com TDD estrito é da T-34.
- **Check-in (A-04):** o dono faz o check-in pelo `POST /api/reservas/{id}/checkin`, em qualquer instante de `[início, fim)`; a task é a T-59. O check-in leva `APROVADA` a `EM_USO`. Ninguém muda a reserva para `EM_USO` pelo `PATCH`.
- **Ordem dos erros (CA-17):** 401 (sem autenticação), 403 por perfil, 404 (reserva inexistente), 403 por dono, 400 (entrada inválida) e 409 (regra). O primeiro que se aplica é o que a resposta mostra.

**Tabela de transições permitidas.** Qualquer outra combinação de origem e destino (42 das 49, inclusive repetir o estado atual) é recusada, e o estado não muda.

| Origem | Destino | Quem ou quando |
|---|---|---|
| `SOLICITADA` | `APROVADA` | Responsável aprova, até `prazo_aprovacao_em`, inclusive (RN-07) |
| `SOLICITADA` | `REJEITADA` | Responsável rejeita (NC-04) |
| `SOLICITADA` | `CANCELADA` | Dono, até o início; ou o sistema (T-60), quando o instante passa de `prazo_aprovacao_em` (A-05, RN-14) |
| `APROVADA` | `EM_USO` | O dono, pelo check-in (`POST /api/reservas/{id}/checkin`, A-04, T-59), em qualquer instante de `[início, fim)`; o `PATCH` com `EM_USO` é recusado com 403 para todos os perfis (CA-14) |
| `APROVADA` | `CANCELADA` | Dono, até o instante do início |
| `APROVADA` | `NAO_COMPARECEU` | Sistema (T-60), a partir do instante do fim sem nenhum check-in (S-06, job periódico); ou o dono, ao cancelar depois do início sem check-in; o horário fica livre |
| `EM_USO` | `CONCLUIDA` | O sistema (T-60), a partir do instante do fim (S-01, `[início, fim)`, job periódico); ou o dono, encerrando antes do fim (RN-13) |

`CONCLUIDA`, `REJEITADA`, `CANCELADA` e `NAO_COMPARECEU` são estados finais: não têm saída.

**Alterar uma reserva aprovada.** A alteração pelo `PUT` (T-20, CA-11) devolve a `APROVADA` ao estado `SOLICITADA` e recalcula `prazo_aprovacao_em` com o novo início. Isso não é uma transição do `PATCH`: `APROVADA→SOLICITADA` por `PATCH` continua proibida (CA-02, CA-14), e a tabela segue com 7 transições. O prazo para alterar é o do CA-13.

**Cancelar depois do início.** Com check-in (`EM_USO`), o cancelamento é `EM_USO → CONCLUIDA`. Sem check-in (`APROVADA`), é `APROVADA → NAO_COMPARECEU`. Não existe `APROVADA → EM_USO → CONCLUIDA` encadeado: `APROVADA→CONCLUIDA` continua proibida (RN-05-T6), e o RN-13-T14 vale só para `EM_USO`. Decisão do grill (Pergunta 7).

**Sem tolerância de atraso (P-01, decisão do time):** a reserva `APROVADA` ocupa o horário inteiro até o dono cancelar ou fazer check-in. Quem chega às 10:35 numa reserva das 10:00 às 11:00 ainda faz check-in. Só vira `NAO_COMPARECEU` se não houver check-in até o fim, ou se o dono cancelar sem check-in.

**Perfil × destino no `PATCH /api/reservas/{id}/estado`.** Resposta quando o perfil de quem chama pede cada destino (o 403 por perfil vem antes de 404 e de 400):

| Destino pedido | SOLICITANTE | RESPONSAVEL | ADMINISTRADOR |
|---|---|---|---|
| `APROVADA` | 403 | 200 se a reserva é `SOLICITADA` e está no prazo; 409 senão | 403 |
| `REJEITADA` | 403 | 200 se a reserva é `SOLICITADA`; 409 senão | 403 |
| `EM_USO` | 403 | 403 | 403 |
| `CONCLUIDA` | 403 | 403 | 403 |
| `CANCELADA` | 403 | 403 | 403 |
| `NAO_COMPARECEU` | 403 | 403 | 403 |
| `SOLICITADA` | 403 | 403 | 403 |
| Estado que não existe (`"ABC"`) | 403 | 400 | 403 |

O `403` do Administrador vale para reserva de recurso restrito e de recurso não restrito. O cancelamento do dono e o check-in não passam pelo `PATCH`: têm endpoints próprios (seção 4).

**Decisões do time que valem como regra desta spec:** A-04 (o check-in do dono leva a `EM_USO`, T-59), A-05 e L11 (prazos acima), L04 (todo pedido passa por `APROVADA`), S-01 (`[início, fim)`), S-02 (`CANCELADA`, `REJEITADA`, `NAO_COMPARECEU` e `CONCLUIDA` não bloqueiam conflito, mas ficam registradas para relatório), P-01 (sem tolerância) e P-02 (job a cada minuto).

## 4. Contrato da API

Os endpoints e os campos são uma proposta desta spec, porque o PRD ainda não os define. Os erros seguem o formato da T-22.

`PATCH /api/reservas/{id}/estado`: muda o estado da reserva. Só o **Responsável** consegue pedir `APROVADA` e `REJEITADA`; qualquer outro perfil, ou qualquer outro destino, recebe 403 (tabela perfil × destino da seção 3 e CA-14). As demais transições são do dono (cancelamento e check-in) ou do sistema (T-60).

```json
{ "estado": "APROVADA" }
```

`POST /api/reservas/{id}/cancelamento`: o dono cancela a própria reserva. O resultado depende do instante e do check-in: `CANCELADA` até o início; depois do início e antes do fim, `CONCLUIDA` se já houve check-in (`EM_USO`) e `NAO_COMPARECEU` se não houve.

`POST /api/reservas/{id}/checkin`: o dono registra a chegada à sala. Sem corpo. Aceito só para o dono, com a reserva `APROVADA` e o instante em `[início, fim)` (CA-16). Resposta 200 com a reserva `EM_USO`. O endpoint e a tela são da T-59.

`DELETE /api/reservas/{id}`: existe só para recusar. O sistema nunca apaga reservas.

**Respostas:**

| Situação | Status HTTP | Corpo |
|---|---|---|
| Transição ou cancelamento aceitos | 200 | A reserva no novo estado |
| Estado que não existe | 400 | `ProblemDetail`, `erros` com `campo = estado` |
| Reserva não encontrada (para quem tem o perfil exigido) | 404 | `ProblemDetail`: "Recurso não encontrado" |
| Transição proibida, cancelamento fora de hora ou reserva final | 409 | `ProblemDetail` em linguagem de negócio, por exemplo "Esta reserva não pode mais ser cancelada" |
| `DELETE` de reserva existente | 409 | `ProblemDetail`: "Reservas não podem ser apagadas. Use o cancelamento." |
| Quem não é o dono tenta cancelar, alterar ou fazer check-in; Solicitante ou Administrador usam o `PATCH`; ou qualquer perfil pede `EM_USO`, `CONCLUIDA`, `CANCELADA`, `NAO_COMPARECEU` ou `SOLICITADA` pelo `PATCH` | 403 | `ProblemDetail`, mesmo formato (regra de acesso na T-15). O 403 vem antes do 404, do 400 e do 409 |
| Check-in fora de `[início, fim)` ou com a reserva em outro estado | 409 | `ProblemDetail`: "O check-in só pode ser feito durante o horário reservado" |
| Aprovação depois do prazo de aprovação | 409 | `ProblemDetail`: "O prazo de aprovação desta reserva terminou" |
| Sem autenticação | 401 | Fora desta spec (T-15), mesmo formato |

**Ordem em que os erros valem:** 401, depois 403 por perfil, 404, 403 por dono, 400 e, por último, 409 (CA-17). Por exemplo, um Solicitante que envia `"ABC"` no `PATCH` recebe 403, não 400; e só quem tem o perfil recebe 404 para um id inexistente.

**Respostas de erro vindas do banco** (violação de chave estrangeira, `UNIQUE` ou `EXCLUDE`) seguem a T-22: 409 com mensagem de negócio. Esta spec não depende delas, porque a reserva nunca é apagada.

## 5. Critérios de aceite

### CA-01: O fluxo principal avança um passo por vez

- **Dado** uma reserva no estado `SOLICITADA`, `APROVADA` ou `EM_USO`
- **Quando** ela é movida para o próximo estado do fluxo principal pelo caminho previsto: aprovação pelo Responsável (`PATCH`), check-in do dono (`POST .../checkin`) ou encerramento pelo sistema (T-60)
- **Então** a transição é aceita e a reserva passa a `APROVADA`, `EM_USO` ou `CONCLUIDA`, respectivamente
- **E** o fluxo todo pode ser percorrido pela aplicação, sem chamar a regra diretamente (aprovar pelo `PATCH`, fazer o check-in pelo endpoint da T-59)

### CA-02: Pular ou voltar no fluxo principal é recusado

- **Dado** uma reserva em um estado do fluxo principal
- **Quando** o cliente pede para pular um estado (`SOLICITADA→EM_USO`, `SOLICITADA→CONCLUIDA`, `APROVADA→CONCLUIDA`) ou voltar (`APROVADA→SOLICITADA`, `EM_USO→APROVADA`, `CONCLUIDA→EM_USO`)
- **Então** a **regra** recusa com 409 e o estado não muda
- **E** pelo `PATCH`, os pedidos de `EM_USO` e `SOLICITADA` nem chegam à regra: recebem 403 (CA-14); o 409 aparece quando o Responsável pede `APROVADA` ou `REJEITADA` numa reserva que não está `SOLICITADA`, e na regra pura (tabela de transições)

### CA-03: Os caminhos alternativos válidos são aceitos

- **Dado** uma reserva `SOLICITADA` ou `APROVADA`
- **Quando** ocorre uma das saídas previstas: rejeição pelo Responsável, cancelamento pelo dono antes do início, prazo de aprovação vencido, isto é, instante maior que `prazo_aprovacao_em` (A-05, T-60), ou fim do horário sem nenhum check-in (S-06, T-60)
- **Então** a reserva passa a `REJEITADA`, `CANCELADA`, `CANCELADA` ou `NAO_COMPARECEU`, respectivamente
- **E** `NAO_COMPARECEU` só é aceito a partir do instante do fim sem check-in (ou no cancelamento do dono sem check-in) e libera o horário. Antes disso a reserva `APROVADA` ocupa o horário

### CA-04: Estados finais não têm saída e `EM_USO` não vira alternativo

- **Dado** uma reserva `CONCLUIDA`, `REJEITADA`, `CANCELADA` ou `NAO_COMPARECEU`, ou uma reserva `EM_USO`
- **Quando** o cliente pede qualquer mudança que a tabela da seção 3 não prevê (nos estados finais, qualquer destino; em `EM_USO`, `CANCELADA`, `REJEITADA` ou `NAO_COMPARECEU`)
- **Então** a **regra** recusa com 409 e o estado não muda
- **E** pelo `PATCH`, `EM_USO→CANCELADA`, `EM_USO→REJEITADA` e `EM_USO→NAO_COMPARECEU` são pedidos de destinos do sistema e recebem 403 (CA-14); o 409 é o da regra e do `POST .../cancelamento`

### CA-05: Estado inexistente é recusado

- **Dado** o endpoint de mudança de estado
- **Quando** o Responsável envia um estado que não existe, por exemplo `"ABC"`
- **Então** o pedido é recusado com 400 e o estado não muda
- **E** o erro aponta o campo `estado`
- **E** se quem envia é Solicitante ou Administrador, a resposta é 403, porque o perfil vem antes da validação (CA-17)

### CA-06: Só as sete transições da tabela passam

- **Dado** as 49 combinações de estado de origem e de destino
- **Quando** cada uma é tentada
- **Então** exatamente as 7 combinações da tabela da seção 3 são aceitas e as outras 42 são recusadas

### CA-07: O cancelamento até o início libera o horário

- **Dado** uma reserva `SOLICITADA` ou `APROVADA` e o instante atual igual ou anterior ao início
- **Quando** o dono a cancela
- **Então** a reserva passa a `CANCELADA` e o horário inteiro volta a ficar livre para outro usuário
- **E** o instante do início (10:00 em ponto) ainda conta como antes do uso

### CA-08: O cancelamento durante o uso é um encerramento antecipado

- **Dado** uma reserva já iniciada e ainda não terminada
- **Quando** o dono a cancela em qualquer instante depois do início (o tempo é contínuo, sem arredondar para o minuto) e antes do fim
- **Então** se já houve check-in (`EM_USO`), a reserva passa a `CONCLUIDA` (o uso aconteceu); se não houve (`APROVADA`), passa a `NAO_COMPARECEU`
- **E** continua no histórico e o resto do horário volta a ficar livre

### CA-09: O cancelamento fora de hora ou fora de estado é recusado

- **Dado** uma reserva já terminada (depois do fim, inclusive no instante do fim), ou já `CONCLUIDA`, `CANCELADA`, `REJEITADA` ou `NAO_COMPARECEU` (inclusive uma `CONCLUIDA` por encerramento antecipado, antes do fim)
- **Quando** o dono tenta cancelá-la
- **Então** o pedido é recusado com 409 e o estado não muda

### CA-10: Só o dono cancela ou altera

- **Dado** uma reserva de um usuário
- **Quando** outro usuário, que não é o dono, tenta cancelá-la ou alterá-la
- **Então** o pedido é recusado com 403 e a reserva fica intacta

### CA-11: Nenhuma reserva é apagada

- **Dado** uma reserva em qualquer estado, antes ou depois do início
- **Quando** o cliente tenta apagá-la
- **Então** o pedido é recusado com 409 e a reserva continua no sistema
- **E** o cancelamento preserva a reserva, consultável no estado em que ficou, com as datas dela (o registro de auditoria de cada mudança é da RN-09, semana 43)

### CA-12: Reserva inexistente é tratada como não encontrada

- **Dado** um identificador que não corresponde a nenhuma reserva
- **Quando** o cliente tenta cancelar ou apagar
- **Então** o pedido é recusado com 404 e nada muda

### CA-13: O prazo de alteração pelo dono

- **Dado** uma reserva do dono
- **Quando** ele tenta alterá-la (`PUT`, T-20); o limite é `prazo_aprovacao_em` da reserva, que é 24 h antes do início atual, ou 1 h antes se o pedido foi feito com menos de 24 h de antecedência
- **Então** a alteração é aceita até o limite (inclusive) e recusada com 409 depois dele, e a reserva não muda
- **E** a alteração aceita recalcula `prazo_aprovacao_em` com o novo início

### CA-14: Só o Responsável muda o estado pelo `PATCH`, e só `APROVADA` e `REJEITADA`

- **Dado** o endpoint `PATCH /api/reservas/{id}/estado`
- **Quando** o pedido vem de um Solicitante ou de um Administrador, com qualquer destino; ou de qualquer perfil, com destino `EM_USO`, `CONCLUIDA`, `CANCELADA`, `NAO_COMPARECEU` ou `SOLICITADA`
- **Então** o pedido é recusado com 403 e o estado não muda
- **E** o Administrador recebe 403 para `APROVADA` e `REJEITADA`, tanto em reserva de recurso restrito quanto de recurso não restrito (RN-07 e PRD)
- **E** o Responsável consegue pedir `APROVADA` e `REJEITADA`; a resposta é 200 para uma `SOLICITADA` no prazo e 409 nos outros casos
- **E** a tabela perfil × destino da seção 3 diz a resposta de cada combinação
- **E** o 403 vem antes do 404, do 400 e do 409

### CA-15: Prazo de aprovação

- **Dado** uma reserva `SOLICITADA` com `prazo_aprovacao_em` calculado: início menos 24 h, ou início menos 1 h se o pedido foi feito com menos de 24 h de antecedência
- **Quando** o Responsável tenta aprová-la, ou o sistema (T-60) verifica o prazo
- **Então** até `prazo_aprovacao_em`, inclusive, a aprovação é aceita; depois dele, a aprovação é recusada com 409 ("O prazo de aprovação desta reserva terminou"), mesmo que o job ainda não tenha rodado
- **E** o sistema (T-60) cancela a reserva quando o instante passa do prazo, nunca no instante exato dele
- **E** uma `APROVADA` alterada volta a `SOLICITADA` com `prazo_aprovacao_em` recalculado com o novo início, que nunca está no passado (RN-15), então a alteração não cancela a reserva no mesmo instante

### CA-16: O check-in do dono leva a `EM_USO`

- **Dado** uma reserva `APROVADA` e o instante dentro de `[início, fim)`
- **Quando** o dono faz `POST /api/reservas/{id}/checkin`
- **Então** a resposta é 200 e a reserva passa a `EM_USO`
- **E** quem não é o dono recebe 403, uma reserva inexistente recebe 404 (para quem tem o perfil), e o check-in antes do início, no instante do fim ou depois, ou com a reserva em outro estado, recebe 409 e o estado não muda
- **E** o `PATCH` com `EM_USO` continua recusado com 403 para todos os perfis (CA-14)

### CA-17: Ordem em que os erros valem

- **Dado** um pedido que poderia gerar mais de um erro
- **Quando** o pedido chega a `PATCH`, ao cancelamento ou ao check-in
- **Então** a resposta é a do primeiro erro nesta ordem: 401 sem autenticação, 403 por perfil, 404 para reserva inexistente, 403 por dono, 400 por entrada inválida e 409 por regra
- **E** por exemplo, um Solicitante que envia `"ABC"` ao `PATCH` recebe 403, e só quem tem o perfil recebe 404 para um id inexistente

## 6. Casos de teste

Os casos da regra são testes **unitários** da regra de ciclo de vida, com o `Clock` fixo em segunda, às 08:00, e uma reserva das 10:00 às 11:00, salvo indicação (ADR 0008). Cada um cita o caso do [plano TDD](../testes/plano-tdd.md). Os casos que o plano **ainda não tem** estão marcados "sem caso no plano" e devem entrar na próxima revisão da T-19. TC-07, TC-16 e TC-18 são testes parametrizados (`@ParameterizedTest`) a partir da tabela de transições. Os casos de nível Integração são os que conferem status ou corpo HTTP (TC-40 a TC-43, TC-54 a TC-57, TC-61 a TC-67, TC-68 a TC-79 e TC-83) e os que dependem do banco para liberar o horário (TC-20, TC-25 e TC-44). Unitário é só a regra pura (tabela de transições, prazos e janela do check-in). Os casos de prazo usam o `Clock` fixo e `prazo_aprovacao_em`; a criação do prazo na criação da reserva é da T-20.

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 (RN-05-T1) | CA-01 | Classe válida | `SOLICITADA` aprovada pelo Responsável | Estado `APROVADA` | Unitário |
| TC-02 (RN-05-T2) | CA-01 | Classe válida | `APROVADA` recebe o check-in do dono às 10:30 (dentro de `[início, fim)`) | Estado `EM_USO` | Unitário |
| TC-03 (RN-05-T3) | CA-01 | Classe válida | `EM_USO` é encerrada pelo sistema às 11:00, no fim | Estado `CONCLUIDA` | Unitário |
| TC-04 (RN-05-T4) | CA-02 | Classe inválida | `SOLICITADA` direto para `EM_USO` | Recusa; continua `SOLICITADA` | Unitário |
| TC-05 (RN-05-T5) | CA-02 | Classe inválida | `SOLICITADA` direto para `CONCLUIDA` | Recusa; o estado não muda | Unitário |
| TC-06 (RN-05-T6) | CA-02 | Classe inválida | `APROVADA` direto para `CONCLUIDA` | Recusa; o estado não muda | Unitário |
| TC-07 (RN-05-T7) | CA-02 | Classe inválida | Retrocessos: `APROVADA→SOLICITADA`, `EM_USO→APROVADA`, `CONCLUIDA→EM_USO` | As três recusadas | Unitário |
| TC-08 (RN-06-T1) | CA-03 | Classe válida | `SOLICITADA` rejeitada pelo Responsável | Estado `REJEITADA` | Unitário |
| TC-09 (RN-06-T2) | CA-03 | Classe válida | `SOLICITADA` cancelada pelo dono | Estado `CANCELADA` | Unitário |
| TC-10 (RN-06-T3) | CA-03 | Classe válida | `APROVADA` cancelada pelo dono antes do início | Estado `CANCELADA` | Unitário |
| TC-11 (RN-06-T4) | CA-03 | Classe válida | `APROVADA` sem check-in às 11:00, no fim (reserva 10:00-11:00; job, S-06) | Estado `NAO_COMPARECEU`; o horário fica livre | Unitário |
| TC-12 (sem caso no plano) | CA-03 | Valor limite | `APROVADA` marcada `NAO_COMPARECEU` às 10:35, sem check-in e antes do fim | Recusa; o estado não muda | Unitário |
| TC-13 (RN-06-T9) | CA-15 | Classe válida | `SOLICITADA` pedida com menos de 24 h de antecedência (`prazo_aprovacao_em` às 09:00), sem aprovação às 09:01 (job, T-60) | Estado `CANCELADA` pelo sistema | Unitário |
| TC-14 (RN-06-T5) | CA-04 | Classe inválida | `EM_USO` para `CANCELADA` por transição direta | Recusa; continua `EM_USO` | Unitário |
| TC-15 (RN-06-T6) | CA-04 | Classe inválida | `EM_USO` para `REJEITADA` ou `NAO_COMPARECEU` | As duas recusadas | Unitário |
| TC-16 (RN-06-T7) | CA-04 | Classe inválida | Qualquer saída de `CONCLUIDA`, `REJEITADA`, `CANCELADA` ou `NAO_COMPARECEU` | Todas recusadas; o estado não muda | Unitário |
| TC-17 (RN-06-T8) | CA-05 | Classe inválida | Mudar para um estado que não existe (`"ABC"`) | Recusa; o estado não muda | Unitário |
| TC-18 (matriz de RN-05 e RN-06) | CA-06 | Tabela de decisão | As 49 combinações de estado de origem e de destino | Exatamente as 7 combinações da seção 3 são aceitas | Unitário |
| TC-19 (RN-13-T1) | CA-07 | Classe válida | Dono cancela às 08:00, antes do início (reserva 10:00-11:00) | `CANCELADA`; o horário 10:00-11:00 fica livre | Unitário |
| TC-20 (RN-13-T2 e RN-06-T10) | CA-07 | Classe válida | Depois do T1, outro usuário reserva a mesma sala, das 10:00 às 11:00 | Aceita (`CANCELADA` não ocupa) | Integração |
| TC-21 (RN-13-T3) | CA-07 | Classe válida | Dono cancela reserva ainda `SOLICITADA`, antes do início | `CANCELADA`; o horário fica livre | Unitário |
| TC-22 (RN-13-T4) | CA-07 | Valor limite | Dono cancela às 09:59 | `CANCELADA` | Unitário |
| TC-23 (RN-13-T5) | CA-07 | Valor limite | Dono cancela às 10:00 em ponto | `CANCELADA`; o horário inteiro fica livre | Unitário |
| TC-24 (RN-13-T6) | CA-08 | Classe válida | Reserva 10:00-12:00 `EM_USO` (com check-in); dono encerra às 10:40 | `CONCLUIDA`; continua consultável | Unitário |
| TC-25 (RN-13-T7) | CA-08 | Classe válida | Depois do T6, outro usuário reserva a sala das 11:00 às 12:00 | Aceita (o resto do horário foi liberado) | Integração |
| TC-26 (RN-13-T14) | CA-08 | Valor limite | Dono cancela uma reserva `EM_USO` às 10:01, um minuto depois do início (exemplo: vale qualquer instante após 10:00:00, como 10:00:01) | `CONCLUIDA`; o resto do horário fica livre | Unitário |
| TC-27 (RN-13-T8) | CA-09 | Classe inválida | Dono tenta cancelar às 11:30, depois do fim | Recusa; o estado não muda | Unitário |
| TC-28 (sem caso no plano) | CA-09 | Valor limite | Dono tenta cancelar às 11:00 em ponto, no instante do fim | Recusa (premissa S-01: `[início, fim)`) | Unitário |
| TC-29 (RN-13-T9) | CA-09 | Classe inválida | Cancelar reserva já `CANCELADA`, ou já `CONCLUIDA` por encerramento antecipado | Recusa; o estado não muda | Unitário |
| TC-30 (RN-13-T10) | CA-09 | Classe inválida | Cancelar reserva `REJEITADA` ou `NAO_COMPARECEU` | Recusa; o estado não muda | Unitário |
| TC-31 (RN-13-T11) | CA-10 | Conflito | Outro usuário, que não é o dono, tenta cancelar | Recusa; a reserva fica intacta | Unitário |
| TC-32 (sem caso no plano) | CA-10 | Conflito | Usuário que não é o dono tenta alterar a reserva | Recusa; a reserva fica intacta | Unitário |
| TC-33 (RN-08-T1) | CA-11 | Estado proibido | Apagar reserva `EM_USO` | Recusa; a reserva continua | Unitário |
| TC-34 (RN-08-T2) | CA-11 | Estado proibido | Apagar reserva `CONCLUIDA` | Recusa; a reserva continua | Unitário |
| TC-35 (RN-08-T3) | CA-11 | Valor limite | Apagar às 10:00 em ponto | Recusa | Unitário |
| TC-36 (RN-08-T4) | CA-11 | Valor limite | Apagar às 09:59, antes do início | Recusa; o caminho é cancelar | Unitário |
| TC-37 (RN-08-T5) | CA-11 | Estado proibido | Apagar reserva `CANCELADA`, `REJEITADA` ou `NAO_COMPARECEU` | Recusa; a reserva continua | Unitário |
| TC-38 (RN-08-T6) | CA-11 | Classe válida | Dono cancela antes do início | Continua consultável como `CANCELADA`, com as datas da reserva | Unitário |
| TC-39 (RN-13-T12 e RN-08-T8) | CA-12 | Classe inválida | Cancelar ou apagar uma reserva que não existe | Recusa como não encontrada; nada muda | Unitário |
| TC-40 (contrato) | CA-01 | Classe válida | Responsável chama `PATCH /api/reservas/{id}/estado` com `APROVADA` numa `SOLICITADA` no prazo | 200 com a reserva `APROVADA` | Integração |
| TC-41 (contrato) | CA-02 | Classe inválida | Responsável chama `PATCH` com `APROVADA` numa reserva já `CANCELADA` | 409 `ProblemDetail` em linguagem de negócio | Integração |
| TC-42 (contrato) | CA-05 | Classe inválida | Responsável chama `PATCH /api/reservas/{id}/estado` com `"ABC"` | 400 `ProblemDetail`, `erros` com `campo = estado` | Integração |
| TC-43 (contrato) | CA-11 | Estado proibido | O dono chama `DELETE /api/reservas/{id}` de uma reserva existente | 409 `ProblemDetail`: reservas não são apagadas, use o cancelamento | Integração |
| TC-44 (sem caso no plano) | CA-08 | Classe válida | Dono cancela às 10:05 uma reserva `APROVADA` sem check-in | `NAO_COMPARECEU`; o resto do horário fica livre | Integração |
| TC-45 (sem caso no plano) | CA-13 | Valor limite | Pedido feito com mais de 24 h de antecedência; dono altera 24 h antes do início, exatas | Aceita | Unitário |
| TC-46 (sem caso no plano) | CA-13 | Valor limite | Mesma reserva; dono altera 23 h 59 min antes | Recusa; a reserva não muda | Unitário |
| TC-47 (sem caso no plano) | CA-13 | Valor limite | Pedido feito com menos de 24 h de antecedência; dono altera 1 h antes, exata | Aceita | Unitário |
| TC-48 (sem caso no plano) | CA-13 | Valor limite | Pedido feito com menos de 24 h de antecedência; dono altera 59 min antes | Recusa; a reserva não muda | Unitário |
| TC-49 (sem caso no plano) | CA-15 | Valor limite | `SOLICITADA` pedida com mais de 24 h de antecedência, sem aprovação depois de 24 h antes do início | `CANCELADA` pelo sistema | Unitário |
| TC-50 (sem caso no plano) | CA-01 | Valor limite | Check-in às 09:59, antes do início (reserva 10:00-11:00, `APROVADA`) | Recusa; continua `APROVADA` | Unitário |
| TC-51 (sem caso no plano) | CA-01 | Valor limite | Check-in às 10:00 em ponto | `EM_USO` | Unitário |
| TC-52 (sem caso no plano) | CA-01 | Valor limite | Check-in às 10:59 | `EM_USO` | Unitário |
| TC-53 (sem caso no plano) | CA-01 | Valor limite | Check-in às 11:00, no instante do fim | Recusa (`[início, fim)`); a reserva segue o caminho de `NAO_COMPARECEU` | Unitário |
| TC-54 (contrato) | CA-14 | Estado proibido | Solicitante chama `PATCH /api/reservas/{id}/estado` com `APROVADA` | 403 `ProblemDetail`; o estado não muda | Integração |
| TC-55 (contrato) | CA-14 | Estado proibido | Responsável chama `PATCH` pedindo `EM_USO` | 403 `ProblemDetail`; o estado não muda; o check-in é pelo endpoint próprio | Integração |
| TC-56 (contrato) | CA-10 | Conflito | `POST /api/reservas/{id}/cancelamento` por quem não é o dono | 403 `ProblemDetail`; a reserva fica intacta | Integração |
| TC-57 (contrato) | CA-12 | Classe inválida | `POST /api/reservas/{id}/cancelamento` de uma reserva que não existe | 404 `ProblemDetail`: "Recurso não encontrado" | Integração |
| TC-58 (sem caso no plano) | CA-15 | Valor limite | Responsável aprova exatamente 24 h antes do início (pedido com mais de 24 h) | Aceita; `APROVADA` | Unitário |
| TC-59 (sem caso no plano) | CA-15 | Estado proibido | Responsável aprova depois do prazo, antes de o job rodar | Recusa 409; a reserva continua `SOLICITADA`, e o job (T-60) a cancela depois | Unitário |
| TC-60 (sem caso no plano) | CA-15 | Valor limite | Dono altera uma `APROVADA` exatamente 24 h antes do início, para um início novo 48 h adiante | `SOLICITADA`; `prazo_aprovacao_em` recalculado com o novo início (início novo menos 24 h); não é cancelada no mesmo instante | Unitário |
| TC-61 (contrato) | CA-08 | Classe válida | `POST /api/reservas/{id}/cancelamento` de uma `APROVADA` sem check-in, depois do início | 200 com a reserva `NAO_COMPARECEU` | Integração |
| TC-62 (sem caso no plano) | CA-16 | Classe válida | O dono chama `POST /api/reservas/{id}/checkin` às 10:30 numa `APROVADA` das 10:00 às 11:00 | 200 com a reserva `EM_USO` | Integração |
| TC-63 (sem caso no plano) | CA-16 | Conflito | Quem não é o dono chama o check-in | 403; a reserva continua `APROVADA` | Integração |
| TC-64 (sem caso no plano) | CA-16 | Valor limite | O dono faz check-in às 09:59 / às 11:00 | Os dois recusados com 409 "O check-in só pode ser feito durante o horário reservado" | Integração |
| TC-65 (sem caso no plano) | CA-16 | Estado proibido | O dono faz check-in numa reserva `SOLICITADA` | 409 | Integração |
| TC-66 (sem caso no plano) | CA-16 | Classe inválida | Check-in numa reserva que não existe | 404 `ProblemDetail`: "Recurso não encontrado" | Integração |
| TC-67 (sem caso no plano) | CA-16 | Estado proibido | O Solicitante dono pede `EM_USO` pelo `PATCH` | 403; o caminho é o check-in | Integração |
| TC-68 (sem caso no plano) | CA-14 | Classe válida | Responsável chama `PATCH` com `REJEITADA` numa `SOLICITADA` | 200 com a reserva `REJEITADA` | Integração |
| TC-69 (sem caso no plano) | CA-14 | Estado proibido | Administrador chama `PATCH` com `APROVADA` numa reserva de recurso restrito | 403; o estado não muda | Integração |
| TC-70 (sem caso no plano) | CA-14 | Estado proibido | Administrador chama `PATCH` com `APROVADA` numa reserva de recurso não restrito | 403; o estado não muda | Integração |
| TC-71 (sem caso no plano) | CA-14 | Estado proibido | Administrador chama `PATCH` com `REJEITADA` | 403; o estado não muda | Integração |
| TC-72 (sem caso no plano) | CA-14 | Estado proibido | Solicitante chama `PATCH` com `REJEITADA` | 403; o estado não muda | Integração |
| TC-73 (sem caso no plano) | CA-14 | Tabela de decisão | Responsável pede `CANCELADA`, `CONCLUIDA`, `NAO_COMPARECEU` e `SOLICITADA` (teste parametrizado) | Os quatro recusados com 403; o estado não muda | Integração |
| TC-74 (sem caso no plano) | CA-14 | Tabela de decisão | Solicitante e Administrador pedem `EM_USO` | Os dois recusados com 403 | Integração |
| TC-75 (sem caso no plano) | CA-17 | Conflito | Solicitante chama `PATCH` com `"ABC"` | 403, não 400 | Integração |
| TC-76 (sem caso no plano) | CA-17 | Conflito | Solicitante chama `PATCH` num id que não existe | 403, não 404 | Integração |
| TC-77 (sem caso no plano) | CA-17 | Conflito | Responsável chama `PATCH` num id que não existe | 404 `ProblemDetail`: "Recurso não encontrado" | Integração |
| TC-78 (sem caso no plano) | CA-17 | Conflito | Responsável chama `PATCH` com `"ABC"` numa reserva que existe | 400, `erros` com `campo = estado` | Integração |
| TC-79 (sem caso no plano) | CA-15 | Estado proibido | Responsável chama `PATCH` com `APROVADA` depois de `prazo_aprovacao_em` | 409 `ProblemDetail`: "O prazo de aprovação desta reserva terminou" | Integração |
| TC-80 (sem caso no plano) | CA-15 | Classe válida | Pedido às 08:00 de segunda para terça das 10:00 (26 h) / pedido às 16:00 de segunda para terça das 10:00 (18 h) | `prazo_aprovacao_em` de segunda às 10:00 / de terça às 09:00 | Unitário |
| TC-81 (sem caso no plano) | CA-15 | Valor limite | O job verifica uma `SOLICITADA` exatamente em `prazo_aprovacao_em` / um segundo depois | Continua `SOLICITADA` / `CANCELADA` pelo sistema | Unitário |
| TC-82 (sem caso no plano) | CA-15 | Valor limite | Responsável aprova exatamente em `prazo_aprovacao_em` (pedido com menos de 24 h de antecedência, às 09:00 para início às 10:00) | Aceita; `APROVADA` | Unitário |
| TC-83 (sem caso no plano) | CA-13 | Valor limite | O dono chama `PUT` exatamente em `prazo_aprovacao_em` / um segundo depois | 200 / 409 `ProblemDetail`: "Esta reserva não pode mais ser alterada" | Integração |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| O plano proíbe `APROVADA→CONCLUIDA` (RN-05-T6) mas permite o cancelamento que conclui (RN-13-T14) | Alto | Resolvido no grill: RN-13-T14 vale só para `EM_USO`; `APROVADA` cancelada depois do início vira `NAO_COMPARECEU` (TC-44) |
| O check-in não tem endpoint ainda (A-04) | Médio | O contrato e a regra estão nesta spec (CA-16, TC-62 a TC-67); o endpoint é a T-59. Até lá, os testes da regra aplicam `APROVADA→EM_USO` diretamente, e o fluxo pela aplicação só fecha com a T-59 |
| Sem tolerância de atraso (P-01) e `NAO_COMPARECEU` pelo cancelamento sem check-in (TC-44) são decisões do time | Médio | Registradas no grill; revistas se mudarem |
| A RN-13 e os prazos (24 h e 1 h) eram só desta spec | Baixo | Registradas no PRD (RN-13 a RN-15), que é a fonte que vale; os testes TC-45 a TC-49, TC-58 a TC-60 e TC-80 a TC-83 cobrem os limites |
| Nenhuma task faz o job que cancela, conclui e marca `NAO_COMPARECEU` | Alto | T-60 no backlog; sem ela as transições por tempo nunca acontecem. Os testes desta spec usam o `Clock`, não o job |
| O Administrador é impedido de aprovar, o que muda o que a spec dizia antes | Médio | RN-07 e PRD: só o Responsável aprova (CA-14, TC-69 a TC-71, tabela perfil × destino) |
| A arquitetura ainda diz job diário e não tem `prazo_aprovacao_em` | Médio | Ajuste em outro commit, na branch da arquitetura (job a cada minuto, coluna `prazo_aprovacao_em`) |
| O job periódico (P-02) atrasa até um minuto as mudanças por tempo | Baixo | A regra aceita a transição a partir do instante; o teste usa o `Clock`, não o job |
| Um Solicitante ou Administrador muda o estado pelo `PATCH` antes da T-34 | Alto | CA-14 e TC-54, TC-55, TC-69 a TC-74: o `PATCH` só aceita o Responsável, e só `APROVADA` e `REJEITADA` |
| Erro de um minuto na fronteira do instante de início ou de fim | Alto | TC-22, TC-23 e TC-26, TC-28, TC-35 e TC-36 testam 09:59, 10:00, 10:01 e 11:00 |
| Uma combinação de estados esquecida na tabela | Alto | TC-18 percorre as 49 combinações e exige exatamente 7 aceitas |
| Reserva apagada por um caminho não previsto | Alto | TC-33 e TC-37 e TC-43; o `DELETE` direto no banco é protegido pela chave estrangeira (arquitetura 3.2) |
| A aprovação de recurso restrito com TDD estrito e a notificação ficam para depois | Médio | Semanas 41 e 44 (T-34); aqui o CA-14 define quem pode pedir `APROVADA` e `REJEITADA` |
| Auditoria de cada mudança fica de fora | Médio | Semana 43; a mudança de estado fica concentrada em um único ponto para a auditoria se plugar |

## 8. Mini-plano de implementação (ciclos TDD)

Na T-21 as assinaturas mínimas devolvem um resultado neutro (que não é aceite nem recusa), para que cada teste falhe por asserção e não por compilação. Cada ciclo é um teste vermelho (T-21, commit `test(red)`), depois o código mínimo (T-24, commit `feat(green)`), depois a refatoração com tudo verde.

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-01 e TC-03: aprovar e concluir | Aprovação pelo Responsável (`SOLICITADA→APROVADA`) e encerramento (`EM_USO→CONCLUIDA`) | Representar os estados como um tipo próprio |
| 2 | TC-04 a TC-07: pular e voltar | Recusar o que não é o próximo passo | Extrair o "próximo estado" de cada origem |
| 3 | TC-08 a TC-12: caminhos alternativos | Transições alternativas e a condição de tempo do `NAO_COMPARECEU` | Agrupar as transições por origem |
| 4 | TC-14 a TC-17: proibidas e estado inexistente | Estados finais sem saída; recusar estado que não existe | Unificar a recusa de transição |
| 5 | TC-18: as 49 combinações | Consolidar a tabela de transições em um único lugar | Substituir as regras soltas pela tabela |
| 6 | TC-19 a TC-23: cancelar até o início | Cancelamento pelo dono, que libera o horário | Reaproveitar a verificação de sobreposição da T-20 |
| 7 | TC-24 a TC-26: encerramento antecipado (`EM_USO`) | Cancelar depois do início com check-in encerra como `CONCLUIDA` | Nomear a regra da fronteira do instante de início |
| 8 | TC-44 e TC-61: cancelar sem check-in | `NAO_COMPARECEU` ao cancelar sem check-in | Isolar a regra de cancelamento por estado |
| 9 | TC-27 a TC-32 e TC-39: recusas do cancelamento e do dono | Recusa depois do fim, em estado final e para quem não é o dono; reserva inexistente | Isolar a verificação de dono |
| 10 | TC-33 a TC-38: nunca apagar | Recusar a exclusão em qualquer estado; cancelar preserva a reserva | Remover qualquer caminho de exclusão |
| 11 | TC-02, TC-50 a TC-53 e TC-62 a TC-67: check-in | `APROVADA→EM_USO` só pelo check-in do dono, em `[início, fim)` (o check-in não existe antes deste ciclo, então todos os testes têm red real) | Nomear a janela de uso |
| 12 | TC-13, TC-45 a TC-49, TC-58 a TC-60 e TC-80 a TC-83: prazos | `prazo_aprovacao_em` na criação e na alteração; aprovar e alterar até o prazo; cancelar só depois dele | Reaproveitar a regra de prazos entre aprovação e alteração |
| 13 | TC-54, TC-55, TC-68 a TC-79: autorização do `PATCH` | Tabela perfil × destino; ordem 401, 403, 404, 400 e 409 | Isolar a regra de quem pode pedir cada destino |
| 14 | TC-40 a TC-43, TC-56 e TC-57: contrato HTTP | Mapear os erros para 400, 403, 404 e 409 no formato da T-22 | Consolidar a tradução dos erros |

## 9. Revisão por pares

> Preenchida pelo revisor, com commit próprio. Nunca pelo autor.

- [x] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [ ] As regras citam RNs que existem no PRD.
- [ ] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [x] Os casos de teste incluem valores limite e entradas inválidas.
- [ ] O contrato da API cobre os erros (400, 401, 403, 409).
- [ ] Não há conflito com outras specs nem com os ADRs.

**Comentários:**

Reli a versão atual. A spec avançou bastante: a antecedência agora usa `criada_em`, a aprovação depois do prazo tem resposta, a janela do check-in tem valores limite, e os casos de liberação do horário passaram para integração. O objetivo e o escopo continuam claros, mas ainda há pontos que impedem a aprovação.

Regras no PRD: a RN-13 e os prazos de 24 h e 1 h continuam fora do PRD. A nova versão troca "resposta do professor" por "decisão do time" e retira a divergência dos 15 min, enquanto o PR #33 ainda registra as respostas do professor. É preciso deixar claro qual fonte vale e registrar as regras no PRD.

Autorização: a spec deixa o Administrador aprovar, mas a RN-07 diz que somente o Responsável aprova recursos restritos, e o PRD diz que o Administrador fica fora dessa aprovação. É preciso separar recurso restrito de não restrito e definir quem aprova cada um, com um caso de 403 para o Administrador no restrito. TC-54 e TC-55 cobrem só duas combinações; recomendo uma tabela de perfil por destino, com os casos aceitos.

Critério verificável: o `PATCH` recusa `EM_USO` para todos, e o check-in do sistema não tem endpoint, ator nem task. Assim, nenhuma reserva chega a `EM_USO` pela aplicação, e o CA-01 e o CA-08 só passam chamando a regra diretamente. O CA-15 depende de um novo prazo de 1 h, mas `criada_em` não mostra que a reserva voltou de `APROVADA`: uma reserva pedida com 10 dias de antecedência poderia ser cancelada logo depois da alteração. Falta guardar esse prazo e definir se os prazos usam o início antigo ou o novo.

Contrato: CA-02 e CA-04 esperam 409 em pedidos que o CA-14 manda recusar com 403, como `SOLICITADA→EM_USO` e `EM_USO→CANCELADA`. O CA-14 também não cita `SOLICITADA`, embora o contrato dê 403 para outros destinos. Falta definir a ordem entre 400, 403 e 404 quando um Solicitante envia `"ABC"` ou um id inexistente. A mensagem de aprovação fora do prazo também não tem caso de integração.

Prazos e job: a linha de prazos aceita aprovar exatamente no limite, mas o job pode cancelar a partir do mesmo instante. No TC-59, não fica claro quem cancela quando a aprovação chega depois do prazo e antes de o job rodar. O job está fora do escopo e não tem task no backlog, então as mudanças automáticas podem nunca acontecer.

Conflitos: além da RN-07, a arquitetura ainda descreve um job diário no fim do dia, e não um job a cada minuto. Ela também não tem campo para o prazo reiniciado.

Ajustes de texto: o escopo ainda omite `NAO_COMPARECEU` no cancelamento, e o TC-38 ainda pede "com o histórico". No ciclo 11, TC-51 e TC-52 não têm red real, porque o check-in já é aceito desde o ciclo 1.

**Parecer:** Ajustes necessários · @Samwel-Merces · 2026-11-07

## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** _preencher_
- **Resultado:** _preencher_

## 11. Grill-with-docs

> Obrigatório só para regra crítica. Nas demais, escreva "Não se aplica".

| # | Pergunta | Resposta / decisão | Documento afetado |
|---|---|---|---|
| 1 | CA-02 proíbe `APROVADA→CONCLUIDA`, mas cancelar às 10:01 vira `CONCLUIDA`? | Interpretação encadeada aprovada; depois substituída pela pergunta 7 | Seção 3 |
| 2 | O que acontece entre 10:00 e 10:01? | Tempo contínuo: até 10:00:00 é `CANCELADA`, depois é encerramento | CA-08, TC-26 |
| 3 | Quem move `APROVADA→EM_USO`? | Inicialmente só o sistema; substituída pela linha 14: o dono, pelo check-in (T-59); o `PATCH` com `EM_USO` é 403 | Seção 3, seção 4 |
| 4 | `NAO_COMPARECEU` e o horário | Sem tolerância: `APROVADA` ocupa o horário todo; `NAO_COMPARECEU` só no fim sem check-in, ou no cancelamento sem check-in | Seção 3, CA-03, TC-11, TC-12 |
| 5 | Quem conclui uma `EM_USO` esquecida? | O sistema, a partir do instante do fim | Seção 3 |
| 6 | L04: recurso não restrito passa por `APROVADA`? | Sim, decisão do time | Seção 3 |
| 7 | Cancelar depois do início sem check-in | `NAO_COMPARECEU`; `CONCLUIDA` só para `EM_USO` | Seção 3, CA-08, TC-44 |
| 8 | L11: prazo de alteração | 24 h antes do início; 1 h se o pedido foi feito com menos de 24 h de antecedência | CA-13, TC-45 a TC-48 |
| 9 | A-05: prazo de aprovação | 24 h antes, ou 1 h com pedido de menos de 24 h de antecedência | CA-15, TC-13, TC-49 |
| 10 | Prazos: pedido com menos de 24 h de antecedência e alteração no limite | 24 h antes; com menos de 24 h de antecedência, 1 h antes; limites inclusivos; alterar `APROVADA` reinicia o prazo em 1 h | Seção 3, CA-13, CA-15, TC-58 a TC-60 |
| 11 | Quem muda o estado pelo `PATCH` | Inicialmente Responsável ou Administrador; substituída pela linha 13: só o Responsável, e só `APROVADA` e `REJEITADA`; o resto é 403 | CA-14, TC-54 e TC-55 |
| 12 | Job diário contra o instante exato | Job periódico a cada minuto (P-02); a regra aceita a transição a partir do instante | Seção 3, CA-03 |
| 13 | O Administrador aprova? | Não: só o Responsável aprova e rejeita, em qualquer reserva (RN-07 e PRD); tabela perfil × destino | Seção 3, CA-14, TC-69 a TC-74 |
| 14 | Quem faz o check-in e como | O dono, por `POST /api/reservas/{id}/checkin`, em `[início, fim)`; task T-59; `PATCH` com `EM_USO` é 403 para todos | Seção 4, CA-16, TC-62 a TC-67 |
| 15 | Quem executa as mudanças por tempo | Job a cada minuto, task T-60 (a criar no backlog) | Seção 3, CA-15, TC-81 |
| 16 | Onde fica o prazo reiniciado na alteração | Coluna `prazo_aprovacao_em`, recalculada com o novo início; limite inclusivo; aprovação fora do prazo é 409 mesmo antes do job | Seção 3, CA-13, CA-15, TC-60, TC-79 a TC-83 |
| 17 | Ordem dos erros | 401, 403 por perfil, 404, 403 por dono, 400, 409 | CA-17, TC-75 a TC-78 |
| 18 | Qual fonte vale | O PRD (RN-13 a RN-15); o plano TDD é atualizado em outro commit | Seção 3 |

**Resultado:** Pendências (a spec mudou depois da última revisão e precisa de nova revisão do Samuel) · 2026-11-07
