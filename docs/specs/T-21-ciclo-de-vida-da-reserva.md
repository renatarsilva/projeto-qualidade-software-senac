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

Uma reserva passa por estados (`SOLICITADA`, `APROVADA`, `EM_USO`, `CONCLUIDA` e os alternativos `REJEITADA`, `CANCELADA`, `NAO_COMPARECEU`), e só algumas mudanças fazem sentido: não se conclui uma reserva que nunca foi aprovada, e uma reserva cancelada não volta. Esta spec define **quais transições são permitidas e quais são proibidas**, quem pode cancelar, como o cancelamento libera o horário e por que uma reserva nunca é apagada (RN-05, RN-06, RN-08 e a proposta RN-13). Os testes da T-20 nascem vermelhos a partir desta tabela e a T-24 os deixa verdes. Os casos vêm do [plano TDD](../testes/plano-tdd.md).

## 2. Escopo

**Dentro:**
- A tabela de transições de estado (válidas e proibidas), incluindo estado final e estado inexistente.
- O cancelamento pelo dono e seu efeito conforme o instante (`CANCELADA` ou encerramento antecipado `CONCLUIDA`).
- Só o dono cancela ou altera a própria reserva (NC-01).
- O prazo de alteração pelo dono: até 24 h antes do início, ou até 1 h antes se a reserva foi solicitada no mesmo dia (L11, resposta do professor). Intervalo e sobreposição ao alterar continuam na T-20.
- Reserva nunca é apagada.
- Contrato dos endpoints de mudança de estado, cancelamento e exclusão.

**Fora (e por quê):**
- Quem aprova ou rejeita (perfil Responsável, RN-07): semanas 41 e 44. Aqui a transição `SOLICITADA→APROVADA` é testada na regra, com o ator já autorizado.
- O job diário que dispara `NAO_COMPARECEU` e conclui as `EM_USO` vencidas (S-06): esta spec define a transição que ele usa, não o job.
- Auditoria de cada mudança (RN-09, incluindo os casos RN-13-T13 e o motivo de RN-06-T9): semana 43.
- A chave estrangeira que impede `DELETE` direto no banco (RN-08-T9): é do esquema do banco, na arquitetura 3.2.
- Intervalo e sobreposição ao alterar: são da T-20.
- O formato das respostas de erro: é da T-22.

## 3. Regras de negócio

- **RN-05 (oficial):** o fluxo principal é `SOLICITADA → APROVADA → EM_USO → CONCLUIDA`. Não se pula nem se volta.
- **RN-06 (alternativos oficiais; as transições são propostas):** os estados alternativos são `REJEITADA`, `CANCELADA` e `NAO_COMPARECEU`.
- **RN-08 (oficial):** reservas iniciadas não podem ser apagadas. Pela premissa L11 e pela restrição 5 da arquitetura, **nenhuma** reserva é apagada: o caminho é cancelar.
- **RN-13 (validada pelo professor, com ajuste):** o dono cancela até o instante do início (`CANCELADA`, horário inteiro livre). Depois do início e antes do fim: se já houve check-in (`EM_USO`), é um encerramento antecipado (`CONCLUIDA`, resto livre); se não houve check-in (`APROVADA`), é `NAO_COMPARECEU` (resto livre). Depois do fim, não há o que cancelar.
- **L11 (resposta do professor):** o dono altera a reserva até 24 h antes do início; se ela foi solicitada no mesmo dia, até 1 h antes.
- **A-05 (resposta do professor):** a `SOLICITADA` não aprovada até 24 h antes do início é cancelada automaticamente; se foi solicitada no mesmo dia, precisa ser aprovada até 1 h antes, senão é cancelada.
- **NC-01:** o Solicitante altera e cancela somente as próprias reservas.

**Tabela de transições permitidas.** Qualquer outra combinação de origem e destino (42 das 49, inclusive repetir o estado atual) é recusada, e o estado não muda.

| Origem | Destino | Quem ou quando |
|---|---|---|
| `SOLICITADA` | `APROVADA` | Responsável aprova (RN-07) |
| `SOLICITADA` | `REJEITADA` | Responsável rejeita (NC-04) |
| `SOLICITADA` | `CANCELADA` | Dono, até o início; ou o sistema, quando o prazo de aprovação vence (24 h antes do início, ou 1 h antes se solicitada no mesmo dia, A-05) |
| `APROVADA` | `EM_USO` | Só o sistema, pelo check-in da sala (A-04), em qualquer instante do horário reservado (`[início, fim)`); pedido de usuário via `PATCH` é recusado com 403 |
| `APROVADA` | `CANCELADA` | Dono, até o instante do início |
| `APROVADA` | `NAO_COMPARECEU` | Sistema, no instante do fim sem nenhum check-in (S-06); ou o dono, ao cancelar depois do início sem check-in; o horário fica livre |
| `EM_USO` | `CONCLUIDA` | O sistema, a partir do instante do fim (S-01, `[início, fim)`); ou o dono, encerrando antes do fim (RN-13) |

`CONCLUIDA`, `REJEITADA`, `CANCELADA` e `NAO_COMPARECEU` são estados finais: não têm saída.

**Alterar uma reserva aprovada.** A alteração pelo `PUT` (T-20, CA-11) devolve a `APROVADA` ao estado `SOLICITADA`. Isso não é uma transição do `PATCH`: `APROVADA→SOLICITADA` por `PATCH` continua proibida (CA-02), e a tabela segue com 7 transições. O prazo para alterar é o do CA-13.

**Cancelar depois do início.** Com check-in (`EM_USO`), o cancelamento é `EM_USO → CONCLUIDA`. Sem check-in (`APROVADA`), é `APROVADA → NAO_COMPARECEU`. Não existe `APROVADA → EM_USO → CONCLUIDA` encadeado: `APROVADA→CONCLUIDA` continua proibida (RN-05-T6), e o RN-13-T14 vale só para `EM_USO`. Decisão do grill (Pergunta 7).

**Sem tolerância de atraso (decisão do grill, diverge da resposta do professor):** a reserva `APROVADA` ocupa o horário inteiro até o dono cancelar ou fazer check-in. Quem chega às 10:35 numa reserva das 10:00 às 11:00 ainda faz check-in. Só vira `NAO_COMPARECEU` se não houver check-in até o fim, ou se o dono cancelar sem check-in. A resposta do professor falava em check-in em até 15 min (P-01): confirmar.

**Premissas ainda em validação pendente:** S-06 (o job diário). Já confirmadas pelo professor: A-04, A-05, L04 (fluxo completo passa por `APROVADA`), L11, S-01 (`[início, fim)`), S-02 (não ocupam o horário, mas ficam registradas para relatório). P-01 (15 min) foi descartada nesta spec e precisa de confirmação.

## 4. Contrato da API

Os endpoints e os campos são uma proposta desta spec, porque o PRD ainda não os define. Os erros seguem o formato da T-22.

`PATCH /api/reservas/{id}/estado`: muda o estado da reserva (quem pode chamar depende da transição; ver seção 3).

```json
{ "estado": "APROVADA" }
```

`POST /api/reservas/{id}/cancelamento`: o dono cancela a própria reserva. O resultado depende do instante: `CANCELADA` até o início, `CONCLUIDA` depois do início e antes do fim.

`DELETE /api/reservas/{id}`: existe só para recusar. O sistema nunca apaga reservas.

**Respostas:**

| Situação | Status HTTP | Corpo |
|---|---|---|
| Transição ou cancelamento aceitos | 200 | A reserva no novo estado |
| Estado que não existe | 400 | `ProblemDetail`, `erros` com `campo = estado` |
| Reserva não encontrada | 404 | `ProblemDetail`: "Recurso não encontrado" |
| Transição proibida, cancelamento fora de hora ou reserva final | 409 | `ProblemDetail` em linguagem de negócio, por exemplo "Esta reserva não pode mais ser cancelada" |
| `DELETE` de reserva existente | 409 | `ProblemDetail`: "Reservas não podem ser apagadas. Use o cancelamento." |
| Quem não é o dono tenta cancelar ou alterar, ou um usuário pede `EM_USO` | 403 | `ProblemDetail`, mesmo formato (regra de acesso na T-15) |
| Sem autenticação | 401 | Fora desta spec (T-15), mesmo formato |

## 5. Critérios de aceite

### CA-01: O fluxo principal avança um passo por vez

- **Dado** uma reserva no estado `SOLICITADA`, `APROVADA` ou `EM_USO`
- **Quando** ela é movida para o próximo estado do fluxo principal
- **Então** a transição é aceita e a reserva passa a `APROVADA`, `EM_USO` ou `CONCLUIDA`, respectivamente

### CA-02: Pular ou voltar no fluxo principal é recusado

- **Dado** uma reserva em um estado do fluxo principal
- **Quando** o cliente pede para pular um estado (`SOLICITADA→EM_USO`, `SOLICITADA→CONCLUIDA`, `APROVADA→CONCLUIDA`) ou voltar (`APROVADA→SOLICITADA`, `EM_USO→APROVADA`, `CONCLUIDA→EM_USO`)
- **Então** o pedido é recusado com 409 e o estado não muda

### CA-03: Os caminhos alternativos válidos são aceitos

- **Dado** uma reserva `SOLICITADA` ou `APROVADA`
- **Quando** ocorre uma das saídas previstas: rejeição pelo Responsável, cancelamento pelo dono antes do início, prazo de aprovação vencido (A-05) ou fim do horário sem nenhum check-in (S-06)
- **Então** a reserva passa a `REJEITADA`, `CANCELADA`, `CANCELADA` ou `NAO_COMPARECEU`, respectivamente
- **E** `NAO_COMPARECEU` só é aceito no instante do fim sem check-in (ou no cancelamento do dono sem check-in) e libera o horário. Antes disso a reserva `APROVADA` ocupa o horário

### CA-04: Estados finais não têm saída e `EM_USO` não vira alternativo

- **Dado** uma reserva `CONCLUIDA`, `REJEITADA`, `CANCELADA` ou `NAO_COMPARECEU`, ou uma reserva `EM_USO`
- **Quando** o cliente pede qualquer mudança que a tabela da seção 3 não prevê (nos estados finais, qualquer destino; em `EM_USO`, `CANCELADA`, `REJEITADA` ou `NAO_COMPARECEU`)
- **Então** o pedido é recusado com 409 e o estado não muda

### CA-05: Estado inexistente é recusado

- **Dado** o endpoint de mudança de estado
- **Quando** o cliente envia um estado que não existe, por exemplo `"ABC"`
- **Então** o pedido é recusado com 400 e o estado não muda
- **E** o erro aponta o campo `estado`

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

- **Dado** uma reserva já terminada (depois do fim, inclusive no instante do fim), ou já `CANCELADA`, `REJEITADA` ou `NAO_COMPARECEU`
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
- **E** o cancelamento preserva a reserva, consultável como `CANCELADA`, com o histórico

### CA-12: Reserva inexistente é tratada como não encontrada

- **Dado** um identificador que não corresponde a nenhuma reserva
- **Quando** o cliente tenta cancelar ou apagar
- **Então** o pedido é recusado com 404 e nada muda

### CA-13: O prazo de alteração pelo dono

- **Dado** uma reserva do dono
- **Quando** ele tenta alterá-la; o limite é 24 h antes do início, ou 1 h antes se a reserva foi solicitada no mesmo dia (L11)
- **Então** a alteração é aceita até o limite (inclusive) e recusada com 409 depois dele, e a reserva não muda

## 6. Casos de teste

Todo critério de aceite precisa de pelo menos um caso. Use classes de equivalência e valor limite.

Os casos da regra são testes **unitários** da regra de ciclo de vida, com o `Clock` fixo em segunda, às 08:00, e uma reserva das 10:00 às 11:00, salvo indicação (ADR 0008). Cada um cita o caso do [plano TDD](../testes/plano-tdd.md). Os casos que o plano **ainda não tem** estão marcados "sem caso no plano" e devem entrar na próxima revisão da T-19. TC-07, TC-16 e TC-18 são testes parametrizados (`@ParameterizedTest`) a partir da tabela de transições. Os quatro últimos são de integração e só conferem o contrato HTTP.

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 (RN-05-T1) | CA-01 | Classe válida | `SOLICITADA` aprovada pelo Responsável | Estado `APROVADA` | Unitário |
| TC-02 (RN-05-T2) | CA-01 | Classe válida | `APROVADA` passa a ser usada | Estado `EM_USO` | Unitário |
| TC-03 (RN-05-T3) | CA-01 | Classe válida | `EM_USO` é encerrada | Estado `CONCLUIDA` | Unitário |
| TC-04 (RN-05-T4) | CA-02 | Classe inválida | `SOLICITADA` direto para `EM_USO` | Recusa; continua `SOLICITADA` | Unitário |
| TC-05 (RN-05-T5) | CA-02 | Classe inválida | `SOLICITADA` direto para `CONCLUIDA` | Recusa; o estado não muda | Unitário |
| TC-06 (RN-05-T6) | CA-02 | Classe inválida | `APROVADA` direto para `CONCLUIDA` | Recusa; o estado não muda | Unitário |
| TC-07 (RN-05-T7) | CA-02 | Classe inválida | Retrocessos: `APROVADA→SOLICITADA`, `EM_USO→APROVADA`, `CONCLUIDA→EM_USO` | As três recusadas | Unitário |
| TC-08 (RN-06-T1) | CA-03 | Classe válida | `SOLICITADA` rejeitada pelo Responsável | Estado `REJEITADA` | Unitário |
| TC-09 (RN-06-T2) | CA-03 | Classe válida | `SOLICITADA` cancelada pelo dono | Estado `CANCELADA` | Unitário |
| TC-10 (RN-06-T3) | CA-03 | Classe válida | `APROVADA` cancelada pelo dono antes do início | Estado `CANCELADA` | Unitário |
| TC-11 (RN-06-T4) | CA-03 | Classe válida | `APROVADA` sem check-in às 11:00, no fim (reserva 10:00-11:00; job, S-06) | Estado `NAO_COMPARECEU`; o horário fica livre | Unitário |
| TC-12 (sem caso no plano) | CA-03 | Valor limite | `APROVADA` marcada `NAO_COMPARECEU` às 10:35, sem check-in e antes do fim | Recusa; o estado não muda | Unitário |
| TC-13 (RN-06-T9) | CA-03 | Classe válida | `SOLICITADA` no mesmo dia, sem aprovação às 09:00 (1 h antes das 10:00) | Estado `CANCELADA` pelo sistema | Unitário |
| TC-14 (RN-06-T5) | CA-04 | Classe inválida | `EM_USO` para `CANCELADA` por transição direta | Recusa; continua `EM_USO` | Unitário |
| TC-15 (RN-06-T6) | CA-04 | Classe inválida | `EM_USO` para `REJEITADA` ou `NAO_COMPARECEU` | As duas recusadas | Unitário |
| TC-16 (RN-06-T7) | CA-04 | Classe inválida | Qualquer saída de `CONCLUIDA`, `REJEITADA`, `CANCELADA` ou `NAO_COMPARECEU` | Todas recusadas; o estado não muda | Unitário |
| TC-17 (RN-06-T8) | CA-05 | Classe inválida | Mudar para um estado que não existe (`"ABC"`) | Recusa; o estado não muda | Unitário |
| TC-18 (matriz de RN-05 e RN-06) | CA-06 | Tabela de decisão | As 49 combinações de estado de origem e de destino | Exatamente as 7 combinações da seção 3 são aceitas | Unitário |
| TC-19 (RN-13-T1) | CA-07 | Classe válida | Dono cancela às 08:00, antes do início (reserva 10:00-11:00) | `CANCELADA`; o horário 10:00-11:00 fica livre | Unitário |
| TC-20 (RN-13-T2 e RN-06-T10) | CA-07 | Classe válida | Depois do T1, outro usuário reserva a mesma sala, das 10:00 às 11:00 | Aceita (`CANCELADA` não ocupa) | Unitário |
| TC-21 (RN-13-T3) | CA-07 | Classe válida | Dono cancela reserva ainda `SOLICITADA`, antes do início | `CANCELADA`; o horário fica livre | Unitário |
| TC-22 (RN-13-T4) | CA-07 | Valor limite | Dono cancela às 09:59 | `CANCELADA` | Unitário |
| TC-23 (RN-13-T5) | CA-07 | Valor limite | Dono cancela às 10:00 em ponto | `CANCELADA`; o horário inteiro fica livre | Unitário |
| TC-24 (RN-13-T6) | CA-08 | Classe válida | Reserva 10:00-12:00 `EM_USO` (com check-in); dono encerra às 10:40 | `CONCLUIDA`; continua no histórico | Unitário |
| TC-25 (RN-13-T7) | CA-08 | Classe válida | Depois do T6, outro usuário reserva a sala das 11:00 às 12:00 | Aceita (o resto do horário foi liberado) | Unitário |
| TC-26 (RN-13-T14) | CA-08 | Valor limite | Dono cancela uma reserva `EM_USO` às 10:01, um minuto depois do início (exemplo: vale qualquer instante após 10:00:00, como 10:00:01) | `CONCLUIDA`; o resto do horário fica livre | Unitário |
| TC-27 (RN-13-T8) | CA-09 | Classe inválida | Dono tenta cancelar às 11:30, depois do fim | Recusa; o estado não muda | Unitário |
| TC-28 (sem caso no plano) | CA-09 | Valor limite | Dono tenta cancelar às 11:00 em ponto, no instante do fim | Recusa (premissa S-01: `[início, fim)`) | Unitário |
| TC-29 (RN-13-T9) | CA-09 | Classe inválida | Cancelar reserva já `CANCELADA` | Recusa; o estado não muda | Unitário |
| TC-30 (RN-13-T10) | CA-09 | Classe inválida | Cancelar reserva `REJEITADA` ou `NAO_COMPARECEU` | Recusa; o estado não muda | Unitário |
| TC-31 (RN-13-T11) | CA-10 | Conflito | Outro usuário, que não é o dono, tenta cancelar | Recusa; a reserva fica intacta | Unitário |
| TC-32 (sem caso no plano) | CA-10 | Conflito | Usuário que não é o dono tenta alterar a reserva | Recusa; a reserva fica intacta | Unitário |
| TC-33 (RN-08-T1) | CA-11 | Estado proibido | Apagar reserva `EM_USO` | Recusa; a reserva continua | Unitário |
| TC-34 (RN-08-T2) | CA-11 | Estado proibido | Apagar reserva `CONCLUIDA` | Recusa; a reserva continua | Unitário |
| TC-35 (RN-08-T3) | CA-11 | Valor limite | Apagar às 10:00 em ponto | Recusa | Unitário |
| TC-36 (RN-08-T4) | CA-11 | Valor limite | Apagar às 09:59, antes do início | Recusa; o caminho é cancelar | Unitário |
| TC-37 (RN-08-T5) | CA-11 | Estado proibido | Apagar reserva `CANCELADA`, `REJEITADA` ou `NAO_COMPARECEU` | Recusa; a reserva continua | Unitário |
| TC-38 (RN-08-T6) | CA-11 | Classe válida | Dono cancela antes do início | Continua consultável como `CANCELADA`, com o histórico | Unitário |
| TC-39 (RN-13-T12 e RN-08-T8) | CA-12 | Classe inválida | Cancelar ou apagar uma reserva que não existe | Recusa como não encontrada; nada muda | Unitário |
| TC-40 (contrato) | CA-01 | Classe válida | `PATCH /api/reservas/{id}/estado` com uma transição válida | 200 com a reserva no novo estado | Integração |
| TC-41 (contrato) | CA-02 | Classe inválida | `PATCH /api/reservas/{id}/estado` com transição proibida | 409 `ProblemDetail` em linguagem de negócio | Integração |
| TC-42 (contrato) | CA-05 | Classe inválida | `PATCH /api/reservas/{id}/estado` com `"ABC"` | 400 `ProblemDetail`, `erros` com `campo = estado` | Integração |
| TC-43 (contrato) | CA-11 | Estado proibido | `DELETE /api/reservas/{id}` de uma reserva existente | 409 `ProblemDetail`: reservas não são apagadas, use o cancelamento | Integração |
| TC-44 (sem caso no plano) | CA-08 | Classe válida | Dono cancela às 10:05 uma reserva `APROVADA` sem check-in | `NAO_COMPARECEU`; o resto do horário fica livre | Unitário |
| TC-45 (sem caso no plano) | CA-13 | Valor limite | Reserva solicitada com antecedência; dono altera 24 h antes do início, exatas | Aceita | Unitário |
| TC-46 (sem caso no plano) | CA-13 | Valor limite | Mesma reserva; dono altera 23 h 59 min antes | Recusa; a reserva não muda | Unitário |
| TC-47 (sem caso no plano) | CA-13 | Valor limite | Reserva solicitada no mesmo dia; dono altera 1 h antes, exata | Aceita | Unitário |
| TC-48 (sem caso no plano) | CA-13 | Valor limite | Reserva solicitada no mesmo dia; dono altera 59 min antes | Recusa; a reserva não muda | Unitário |
| TC-49 (sem caso no plano) | CA-03 | Valor limite | `SOLICITADA` com antecedência, sem aprovação 24 h antes do início | `CANCELADA` pelo sistema | Unitário |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| O plano proíbe `APROVADA→CONCLUIDA` (RN-05-T6) mas permite o cancelamento que conclui (RN-13-T14) | Alto | Resolvido no grill: RN-13-T14 vale só para `EM_USO`; `APROVADA` cancelada depois do início vira `NAO_COMPARECEU` (TC-44) |
| O sistema de check-in não existe ainda (A-04) | Médio | Os testes aplicam `APROVADA→EM_USO` diretamente; o check-in real fica para a implementação |
| Sem tolerância de 15 min: diverge da resposta do professor (P-01); `NAO_COMPARECEU` pelo cancelamento sem check-in (TC-44) é interpretação desta spec | Médio | Confirmar com o professor |
| Erro de um minuto na fronteira do instante de início ou de fim | Alto | TC-22, TC-23 e TC-26, TC-28, TC-35 e TC-36 testam 09:59, 10:00, 10:01 e 11:00 |
| Uma combinação de estados esquecida na tabela | Alto | TC-18 percorre as 49 combinações e exige exatamente 7 aceitas |
| Reserva apagada por um caminho não previsto | Alto | TC-33 e TC-37 e TC-43; o `DELETE` direto no banco é protegido pela chave estrangeira (arquitetura 3.2) |
| Quem aprova e rejeita (RN-07) fica sem teste de permissão aqui | Médio | Fora desta spec: semanas 41 e 44; a T-21 assume o ator já autorizado |
| O professor não disse se o dono pode cancelar a reserva solicitada 1 h antes até 15 min antes do início (exceção da A-05) | Médio | Fora desta spec até esclarecer; hoje o dono cancela até o início |
| Auditoria de cada mudança fica de fora | Médio | Semana 43; a mudança de estado fica concentrada em um único ponto para a auditoria se plugar |

## 8. Mini-plano de implementação (ciclos TDD)

Na T-21 as assinaturas mínimas devolvem um resultado neutro (que não é aceite nem recusa), para que cada teste falhe por asserção e não por compilação. Cada ciclo é um teste vermelho (T-21, commit `test(red)`), depois o código mínimo (T-24, commit `feat(green)`), depois a refatoração com tudo verde.

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-01 a TC-03: o fluxo principal | Os três passos do fluxo | Representar os estados como um tipo próprio |
| 2 | TC-04 a TC-07: pular e voltar | Recusar o que não é o próximo passo | Extrair o "próximo estado" de cada origem |
| 3 | TC-08 a TC-13: caminhos alternativos | Transições alternativas e a condição de tempo do `NAO_COMPARECEU` | Agrupar as transições por origem |
| 4 | TC-14 a TC-17: proibidas e estado inexistente | Estados finais sem saída; recusar estado que não existe | Unificar a recusa de transição |
| 5 | TC-18: as 49 combinações | Consolidar a tabela de transições em um único lugar | Substituir as regras soltas pela tabela |
| 6 | TC-19 a TC-23: cancelar até o início | Cancelamento pelo dono, que libera o horário | Reaproveitar a verificação de sobreposição da T-20 |
| 7 | TC-24 a TC-26: encerramento antecipado | Cancelar depois do início encerra como `CONCLUIDA` | Nomear a regra da fronteira do instante de início |
| 7b | TC-44 a TC-49: sem check-in, alteração e prazo de aprovação | `NAO_COMPARECEU` ao cancelar sem check-in; prazo de alteração; cancelamento automático | Isolar a regra de prazos |
| 8 | TC-27, TC-28, TC-29, TC-30, TC-31, TC-32 e TC-39: recusas do cancelamento e do dono | Recusa depois do fim, em estado final e para quem não é o dono; reserva inexistente | Isolar a verificação de dono |
| 9 | TC-33 a TC-38: nunca apagar | Recusar a exclusão em qualquer estado; cancelar preserva o histórico | Remover qualquer caminho de exclusão |
| 10 | TC-40 a TC-43: contrato HTTP | Mapear os erros para 400, 403, 404 e 409 no formato da T-22 | Consolidar a tradução dos erros |

## 9. Revisão por pares

> Preenchida pelo revisor, com commit próprio. Nunca pelo autor.

- [x] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [ ] As regras citam RNs que existem no PRD.
- [ ] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [ ] Os casos de teste incluem valores limite e entradas inválidas.
- [ ] O contrato da API cobre os erros (400, 401, 403, 409).
- [ ] Não há conflito com outras specs nem com os ADRs.

**Comentários:**

O objetivo e o escopo estão claros, e a tabela com as 7 transições permitidas ajuda bastante. A autorização do `PATCH`, porém, precisa ser definida nesta spec.

Autorização: a spec diz que quem chama o `PATCH /api/reservas/{id}/estado` depende da transição, mas não define isso nem testa. Como a T-24 vem antes da T-34, um Solicitante poderia aprovar a própria reserva ou mudar a reserva de outra pessoa. É preciso listar quem pode pedir cada destino ou recusar essas mudanças até a T-34, com casos de 403.

Prazos: uma reserva pedida na segunda às 16:00 para terça às 10:00 já nasce com o prazo de 24 h vencido, e o mesmo ocorre no mesmo dia, com menos de 1 h de antecedência. Também falta definir "mesmo dia". Uma `APROVADA` alterada exatamente 24 h antes volta a `SOLICITADA` e cai no cancelamento automático, porque os dois prazos são iguais. O texto fala em "instante do fim" e em prazos exatos, mas o job previsto é diário, e não está definido o que acontece até ele rodar, nem se o Responsável pode aprovar depois do prazo.

Regras no PRD: a RN-13 não existe no PRD, e as respostas do professor sobre L11, A-04 e A-05 ainda não estão na `main`. A P-01 aparece como descartada e pendente ao mesmo tempo, então é preciso confirmar os 15 min com o professor.

Critérios: o CA-03 aceita `NAO_COMPARECEU` só "no instante do fim", o que não combina com um job que roda depois. O CA-11 pede o histórico, mas a auditoria está fora do escopo. O CA-09 não cobre cancelar de novo uma reserva já `CONCLUIDA` antes do fim.

Testes: o `Clock` padrão é 08:00, mas o check-in só vale em `[início, fim)`, então TC-02, TC-03 e TC-40 precisam informar o instante. Faltam limites da janela de check-in (09:59, 10:00, 10:59 e 11:00). TC-20, TC-25 e TC-44 precisam de integração, porque a liberação do horário depende do banco. Faltam também testes de contrato para 403 e 404, e a frase "os quatro últimos são de integração" ficou desatualizada.

Contrato: o cancelamento não cita `NAO_COMPARECEU`, previsto no CA-08. `SOLICITADA→EM_USO` aparece com 403 na tabela de respostas e 409 no CA-02, e o 403 de `EM_USO` não tem caso de teste.

Conflitos: a T-20 diz que a RN-13 ainda não foi validada e precisa de spec própria. A regra "só o dono cancela ou altera" impede o Administrador de tratar as reservas afetadas por manutenção (S-05 da arquitetura). A volta de `APROVADA` para `SOLICITADA` pelo `PUT` fica fora da tabela de transições e contradiz "não se volta" da seção 3. Além disso, a seção 1 cita os testes da T-20, mas deveria citar a T-21.

**Parecer:** Ajustes necessários · @Samwel-Merces · 2026-11-05

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
| 3 | Quem move `APROVADA→EM_USO`? | Só o sistema (check-in da sala); usuário recebe 403 | Seção 3, seção 4 |
| 4 | `NAO_COMPARECEU` e o horário | Sem tolerância: `APROVADA` ocupa o horário todo; `NAO_COMPARECEU` só no fim sem check-in, ou no cancelamento sem check-in (diverge do professor) | Seção 3, CA-03, TC-11, TC-12 |
| 5 | Quem conclui uma `EM_USO` esquecida? | O sistema, a partir do instante do fim | Seção 3 |
| 6 | L04: recurso não restrito passa por `APROVADA`? | Sim, resposta do professor | Seção 3 |
| 7 | Cancelar depois do início sem check-in | `NAO_COMPARECEU`; `CONCLUIDA` só para `EM_USO` | Seção 3, CA-08, TC-44 |
| 8 | L11: prazo de alteração | 24 h antes do início; 1 h se solicitada no mesmo dia (professor) | CA-13, TC-45 a TC-48 |
| 9 | A-05: prazo de aprovação | 24 h antes, ou 1 h no mesmo dia (professor) | CA-03, TC-13, TC-49 |

**Resultado:** _preencher_ (Liberada / Pendências) · AAAA-MM-DD
