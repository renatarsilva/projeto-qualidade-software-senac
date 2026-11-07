# Spec T-20: intervalo e sobreposição de horários da reserva

<!--
Como usar: copie este arquivo para docs/specs/<ID-DA-TASK>-<descricao-curta>.md
e substitua todo trecho _preencher_. O fluxo completo está em docs/processo/ai-workflow.md.

Status possíveis, nesta ordem:
Rascunho → Em revisão → Aprovada → Validada → Liberada
(tasks não críticas vão de Validada direto para Liberada, sem grill)
-->

| Campo | Valor |
|---|---|
| Task | T-20 (a mesma spec vale para a implementação, T-23) |
| Autor(a) | @renatarsilva |
| Revisor(a) | @CaiosAguiar |
| Status | Em revisão |
| Regra crítica | Sim |
| Requisitos | RF-04, RF-05 |
| Característica ISO 25010 | Adequação funcional |
| Criada em | 2026-11-04 |

## 1. Contexto e objetivo

Uma reserva é um pedido de uso de recursos (sala, material, professor) em um intervalo de tempo. Duas regras decidem se o pedido é válido: o **término vem depois do início** (RN-01) e **dois pedidos não podem ocupar o mesmo recurso ao mesmo tempo** (RN-02). Esta spec define o comportamento dessas duas regras para a criação e a alteração de reserva, para que os testes da T-20 nasçam vermelhos e a implementação da T-23 os deixe verdes. Os casos vêm do [plano TDD](../testes/plano-tdd.md) (T-19) e do caso de teste CT-01 da [utility tree](../atam/utility-tree-inicial.md), que lista as 21 verificações de sobreposição (V01 a V21).

## 2. Escopo

**Dentro:**
- Validar o intervalo da reserva (RN-01), na criação e na alteração.
- Detectar sobreposição no mesmo recurso (sala, material ou professor), inclusive na agenda do professor (RN-02, RF-05).
- Recusar o pedido por inteiro quando qualquer recurso pedido estiver ocupado.
- Decidir quais estados da reserva ocupam o horário (S-02).
- Contrato de `POST /api/reservas` e `PUT /api/reservas/{id}` para esses erros.

**Fora (e por quê):**
- Transições de estado, cancelamento e "reserva iniciada não se apaga" (RN-05, RN-06, RN-08): são da T-21 e da T-24.
- Duas solicitações simultâneas (RN-03): o teste de concorrência é da semana 42. Aqui a verificação é sequencial.
- Recurso em manutenção (RN-04): semana 41.
- Auditoria (RN-09), autenticação e perfis (RN-07, T-15): semanas 43 e 44.
- Janela de funcionamento, último horário e antecedência máxima (RN-10 a RN-12): são propostas do time e precisam de spec própria. O cancelamento que libera o recurso (RN-13) é da T-21.
- O formato das respostas de erro: é da T-22. Esta spec só diz **qual** erro ocorre.

## 3. Regras de negócio

- **RN-01:** o término da reserva deve ser posterior ao início. A comparação é entre data e hora. Término igual ou anterior ao início é recusado, e início ou término ausente também.
- **RN-02:** reservas do mesmo recurso (sala, material ou professor) não podem se sobrepor. O pedido é verificado contra **cada** recurso pedido: basta um ocupado para recusar o pedido inteiro.

- **Antecedência mínima (decisão do time, vem da T-21):** o pedido precisa ser feito com pelo menos 1 h de antecedência do início; exatamente 1 h ainda vale. A reserva guarda o instante do pedido (`solicitada_em`), porque os prazos de aprovação e de alteração da T-21 dependem dele.
- **Alteração (decisão do grill):** alterar uma reserva `APROVADA` a devolve ao estado `SOLICITADA`, a aguardar nova aprovação. O horário antigo é liberado e o novo é ocupado (`SOLICITADA` ocupa). Se a alteração for rejeitada ou o prazo de aprovação vencer (A-05), a reserva vira `REJEITADA` ou `CANCELADA` e o dono perde o horário antigo. A resposta avisa isso. O prazo para alterar (L11) é da T-21 (CA-13).

**Premissas da arquitetura** (S-01 e S-02 são decisões do time; D-02 segue da arquitetura):

- **S-01 (decisão do time):** o período é fechado no início e aberto no término, `[início, fim)`. Uma reserva que termina exatamente quando outra começa **não** é sobreposição.
- **S-02 (decisão do time):** `SOLICITADA`, `APROVADA` e `EM_USO` ocupam o horário. `REJEITADA`, `CANCELADA`, `NAO_COMPARECEU` e `CONCLUIDA` não bloqueiam conflito, mas continuam registradas para relatório. Uma `APROVADA` ocupa o horário inteiro até o dono cancelar ou fazer check-in, sem tolerância de atraso (ver T-21).
- **D-02:** a reserva se liga aos recursos pela tabela `reserva_recurso`, então uma reserva pode ocupar sala, professor e material ao mesmo tempo.

## 4. Contrato da API

`POST /api/reservas`: cria uma reserva (perfil Solicitante, regra de acesso na T-15). O endpoint e os campos são uma proposta desta spec, porque o PRD ainda não os define.

**Requisição:**

```json
{
  "inicio": "2026-11-10T10:00:00Z",
  "termino": "2026-11-10T11:00:00Z",
  "recursos": [
    { "tipo": "SALA", "id": 1 },
    { "tipo": "PROFESSOR", "id": 7 }
  ]
}
```

O **dono** da reserva é o usuário autenticado que fez o `POST`: o corpo não traz campo de dono, e ninguém reserva em nome de outra pessoa (a identidade vem da T-15).

`PUT /api/reservas/{id}`: altera uma reserva, com o mesmo corpo. A reserva que está sendo alterada **não** conta como conflito consigo mesma.

**Respostas** (erros no formato da T-22):

| Situação | Status HTTP | Corpo |
|---|---|---|
| Sucesso | 201 (criação) ou 200 (alteração) | A reserva, no estado `SOLICITADA` na criação |
| Término igual ou anterior ao início | 400 | `ProblemDetail`, `erros` com `campo = termino` e `motivo = "deve ser posterior ao início"` |
| Início ou término ausente | 400 | `ProblemDetail`, `erros` com o campo ausente e `motivo = "deve ser informado"` |
| Alterar reserva que não está `SOLICITADA` nem `APROVADA`, ou fora do prazo da T-21 (CA-13) | 409 | `ProblemDetail` em linguagem de negócio, por exemplo "Esta reserva não pode mais ser alterada"; a reserva não muda |
| Recurso que não existe | 404 | `ProblemDetail`: "Recurso não encontrado"; nada é gravado |
| Lista de recursos vazia | 400 | `ProblemDetail`, `erros` com `campo = recursos` e `motivo = "deve ter ao menos um recurso"` |
| Pedido com menos de 1 h de antecedência | 409 | `ProblemDetail` em linguagem de negócio, por exemplo "A reserva precisa ser pedida com pelo menos 1 hora de antecedência"; nada é gravado |
| Sobreposição em algum recurso | 409 | `ProblemDetail` em linguagem de negócio, por exemplo "A sala já está reservada neste horário" |
| Sem autenticação e perfil sem permissão | 401 e 403 | Fora desta spec (T-15), mesmo formato |

## 5. Critérios de aceite

### CA-01: Intervalo válido é aceito

- **Dado** que existe um recurso livre
- **Quando** o cliente pede uma reserva com término posterior ao início, mesmo que por um único minuto, no mesmo dia ou em dia futuro
- **Então** a reserva é aceita e gravada no estado `SOLICITADA`

### CA-02: Término igual ou anterior ao início é recusado

- **Dado** o cadastro de reserva (criação ou alteração)
- **Quando** o cliente informa um término igual ou anterior ao início
- **Então** o pedido é recusado com 400 e nada é gravado
- **E** o erro aponta o campo `termino` com o motivo "deve ser posterior ao início"
- **E** na alteração, a reserva mantém o horário original

### CA-03: Intervalo incompleto é recusado

- **Dado** o cadastro de reserva
- **Quando** o cliente não informa o início ou o término
- **Então** o pedido é recusado com 400 e nada é gravado
- **E** o erro aponta o campo ausente

### CA-04: Sobreposição no mesmo recurso é recusada

- **Dado** que já existe uma reserva que ocupa o horário de um recurso (sala ou material)
- **Quando** o cliente pede o mesmo recurso em um intervalo que se sobrepõe a ela, em qualquer uma destas formas: começa dentro, termina dentro, está contido, contém ou é idêntico; ou quando altera uma reserva para um horário já ocupado
- **Então** o pedido é recusado com 409
- **E** a mensagem diz, em linguagem de negócio, que o recurso já está reservado no horário
- **E** se dois pedidos disputarem o mesmo horário ao mesmo tempo e a recusa vier do banco (`EXCLUDE`), a resposta continua sendo 409 (T-22, CA-08)
- **E** a mensagem não revela quem reservou nem outros dados da reserva existente
- **E** na alteração, a reserva mantém o horário original

### CA-05: Sobreposição na agenda do professor é recusada

- **Dado** que um professor já tem reserva que ocupa um intervalo
- **Quando** o cliente pede uma reserva em que esse professor está alocado, em intervalo sobreposto, ainda que a sala e o material estejam livres
- **Então** o pedido é recusado com 409
- **E** a mensagem indica que o professor já está ocupado no horário

### CA-06: Reservas encostadas não conflitam

- **Dado** que existe uma reserva que ocupa o horário de um recurso
- **Quando** o cliente pede o mesmo recurso começando exatamente quando ela termina, ou terminando exatamente quando ela começa
- **Então** o pedido é aceito (premissa S-01)

### CA-07: Recursos ou intervalos distintos não conflitam

- **Dado** que existe uma reserva que ocupa o horário de um recurso
- **Quando** o cliente pede o mesmo intervalo em **outro** recurso, ou o mesmo recurso em outro dia
- **Então** o pedido é aceito

### CA-08: Só os estados ativos ocupam o horário

- **Dado** uma reserva existente em um intervalo
- **Quando** o cliente pede o mesmo recurso em um intervalo sobreposto
- **Então** o pedido é recusado se a reserva existente está `SOLICITADA`, `APROVADA` ou `EM_USO`
- **E** é aceito se está `CANCELADA`, `REJEITADA`, `NAO_COMPARECEU` ou `CONCLUIDA` (premissa S-02)

### CA-09: A reserva alterada não conflita consigo mesma

- **Dado** uma reserva existente de um recurso
- **Quando** o cliente a altera para um intervalo que só se sobrepõe ao intervalo antigo dela
- **Então** a alteração é aceita

### CA-10: Pedido com vários recursos é tudo ou nada

- **Dado** um pedido que reúne mais de um recurso (por exemplo, sala, projetor e professor)
- **Quando** pelo menos um dos recursos está ocupado no intervalo
- **Então** o pedido é recusado por inteiro e nada é gravado
- **E** os recursos livres do pedido continuam livres

### CA-11: Alterar uma reserva aprovada exige nova aprovação

- **Dado** uma reserva `APROVADA`
- **Quando** o dono a altera para um horário ou recursos livres
- **Então** a alteração é aceita e a reserva volta a `SOLICITADA`
- **E** o horário antigo fica livre e o novo fica ocupado
- **E** a resposta avisa que a reserva precisa ser aprovada de novo

### CA-12: Recurso inexistente ou lista vazia é recusado

- **Dado** o cadastro ou a alteração de reserva
- **Quando** o pedido traz um recurso que não existe, ou nenhum recurso
- **Então** o recurso inexistente é recusado com 404 e a lista vazia com 400, e nada é gravado
- **E** na lista vazia o erro aponta o campo `recursos`

### CA-13: Só se altera reserva solicitada ou aprovada, dentro do prazo

- **Dado** uma reserva do dono
- **Quando** ele tenta alterá-la e ela está `EM_USO`, `CONCLUIDA`, `CANCELADA`, `REJEITADA` ou `NAO_COMPARECEU`
- **Então** o pedido é recusado com 409 e a reserva não muda
- **E** para `SOLICITADA` e `APROVADA`, vale o prazo de alteração da T-21 (CA-13)

### CA-14: O dono é o usuário autenticado

- **Dado** um usuário autenticado
- **Quando** ele cria uma reserva
- **Então** o dono da reserva é esse usuário
- **E** um campo de dono enviado no corpo é ignorado

### CA-15: Antecedência mínima de 1 hora

- **Dado** o cadastro de reserva
- **Quando** o início está a menos de 1 h do instante do pedido
- **Então** o pedido é recusado com 409 e nada é gravado
- **E** com exatamente 1 h de antecedência, ou mais, o pedido é aceito
- **E** a reserva guarda o instante do pedido

## 6. Casos de teste

Todo critério de aceite precisa de pelo menos um caso. Use classes de equivalência e valor limite.

Os casos da regra são testes **unitários** da regra de reserva, com o `Clock` injetado (ADR 0008). Cada um cita entre parênteses o caso do [plano TDD](../testes/plano-tdd.md) ou a verificação do CT-01; os casos `V01` a `V21` são agrupados e rodam como teste parametrizado. O caso do estado `CONCLUIDA` vem da premissa S-02 e **ainda não tem caso no plano** (ver seção 7). Os três últimos são de integração e só conferem o contrato HTTP.

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 (RN-01-T1) | CA-01 | Classe válida | Início 10:00 e término 11:00, no mesmo dia | Aceita e grava | Unitário |
| TC-02 (RN-01-T2) | CA-01 | Classe válida | Início 10:00 e término 11:00 de um dia futuro (data e hora comparadas) | Aceita | Unitário |
| TC-03 (RN-01-T3) | CA-01 | Valor limite | Início 10:00 e término 10:01 | Aceita | Unitário |
| TC-04 (RN-01-T4) | CA-02 | Valor limite | Início 10:00 e término 10:00 | Recusa; nada é gravado | Unitário |
| TC-05 (RN-01-T5) | CA-02 | Classe inválida | Início 10:00 e término 09:00 | Recusa; nada é gravado | Unitário |
| TC-06 (RN-01-T6) | CA-03 | Classe inválida | Início ausente | Recusa com mensagem compreensível; nada é gravado | Unitário |
| TC-07 (RN-01-T7) | CA-03 | Classe inválida | Término ausente | Recusa com mensagem compreensível; nada é gravado | Unitário |
| TC-08 (RN-01-T8) | CA-02 | Classe inválida | Alterar reserva existente para término anterior ao início | Recusa; a reserva mantém o horário original | Unitário |
| TC-09 (CT-01 V01 a V05) | CA-04 | Classe inválida | Sala: sobreposição no início, no fim, contida, contendo e idêntica | Recusa nas cinco formas | Unitário |
| TC-10 (CT-01 V06 e V07) | CA-06 | Valor limite | Sala: começa quando a existente termina; termina quando a existente começa | Aceita nos dois casos (S-01) | Unitário |
| TC-11 (CT-01 V08 a V12) | CA-04 | Classe inválida | Material: as mesmas cinco formas de sobreposição | Recusa nas cinco formas | Unitário |
| TC-12 (CT-01 V13 e V14) | CA-06 | Valor limite | Material: as duas fronteiras encostadas | Aceita nos dois casos (S-01) | Unitário |
| TC-13 (CT-01 V15 a V19) | CA-05 | Classe inválida | Professor, com sala e material livres: as mesmas cinco formas | Recusa nas cinco formas | Unitário |
| TC-14 (CT-01 V20 e V21) | CA-06 | Valor limite | Professor: as duas fronteiras encostadas | Aceita nos dois casos (S-01) | Unitário |
| TC-15 (RN-02-T1) | CA-04 | Conflito | A estende a reserva (10:00-11:00) para 10:00-12:00 e B tem a sala das 11:00 às 12:00 | Recusa; a reserva de A continua das 10:00 às 11:00 | Unitário |
| TC-16 (RN-02-T2) | CA-09 | Classe válida | A estende a reserva para 10:00-12:00, com a sala livre depois | Aceita (não conflita consigo mesma) | Unitário |
| TC-17 (RN-02-T3) | CA-06 | Valor limite | A estende de 10:00-10:30 para 10:00-11:00 e B tem 11:00-12:00 | Aceita (encostada, S-01) | Unitário |
| TC-18 (RN-02-T4) | CA-04 | Conflito | A muda a reserva para 13:00-14:00, horário já ocupado por C | Recusa; a reserva de A fica no horário original | Unitário |
| TC-19 (RN-02-T5) | CA-10 | Conflito | Pedido de sala, projetor e professor; só o projetor está ocupado | Recusa por inteiro; sala e professor continuam livres | Unitário |
| TC-20 (RN-02-T6) | CA-10 | Conflito | Pedido em que sala e professor conflitam ao mesmo tempo | Recusa; nada é gravado | Unitário |
| TC-21 (RN-02-T7) | CA-08 | Classe equivalente | Existente SOLICITADA; B pede 10:30-11:30 | Recusa (ocupa o horário) | Unitário |
| TC-22 (RN-02-T8) | CA-08 | Classe equivalente | Existente EM_USO; B pede 10:30-11:30 | Recusa (ocupa o horário) | Unitário |
| TC-23 (RN-02-T9) | CA-08 | Classe equivalente | Existente CANCELADA; B pede 10:30-11:30 | Aceita (não ocupa) | Unitário |
| TC-24 (RN-02-T10) | CA-08 | Classe equivalente | Existente REJEITADA; B pede 10:30-11:30 | Aceita (não ocupa) | Unitário |
| TC-25 (RN-02-T11) | CA-08 | Classe equivalente | Existente NAO_COMPARECEU; B pede 10:30-11:30 | Aceita (não ocupa) | Unitário |
| TC-26 (S-02 (sem caso no plano)) | CA-08 | Classe equivalente | Existente CONCLUIDA; B pede 10:30-11:30 | Aceita (não ocupa) | Unitário |
| TC-27 (RN-02-T12) | CA-07 | Classe válida | B pede a sala 101 no mesmo horário em que a 102 está reservada | Aceita (recursos diferentes) | Unitário |
| TC-28 (RN-02-T13) | CA-07 | Classe válida | B pede a sala 102 em outro dia, das 10:00 às 11:00 | Aceita | Unitário |
| TC-29 (contrato) | CA-02 | Classe inválida | `POST /api/reservas` com término igual ao início | 400 `ProblemDetail`, `erros` com `campo = termino` | Integração |
| TC-30 (contrato) | CA-04 | Conflito | `POST /api/reservas` sobreposta a uma reserva existente | 409 `ProblemDetail` em linguagem de negócio | Integração |
| TC-31 (contrato) | CA-01 | Classe válida | `POST /api/reservas` com intervalo livre | 201 com a reserva no estado `SOLICITADA` | Integração |
| TC-32 (sem caso no plano) | CA-11 | Classe válida | Dono altera uma reserva `APROVADA` para um horário livre | Estado `SOLICITADA`; horário antigo livre, novo ocupado | Unitário |
| TC-33 (sem caso no plano) | CA-11 | Classe válida | Dono altera uma reserva `SOLICITADA` para um horário livre | Continua `SOLICITADA` | Unitário |
| TC-34 (sem caso no plano) | CA-11 | Conflito | Dono altera uma reserva `APROVADA` para um horário ocupado | Recusa 409; continua `APROVADA` no horário original | Unitário |
| TC-35 (contrato) | CA-11 | Classe válida | `PUT /api/reservas/{id}` de uma reserva `APROVADA` | 200 com a reserva `SOLICITADA` e o aviso de nova aprovação | Integração |
| TC-36 (sem caso no plano) | CA-12 | Classe inválida | Pedido com a sala 999, que não existe | Recusa como não encontrada; nada gravado | Unitário |
| TC-37 (sem caso no plano) | CA-12 | Classe inválida | Pedido com a lista de recursos vazia | Recusa; o erro aponta `recursos` | Unitário |
| TC-38 (contrato) | CA-12 | Classe inválida | `POST /api/reservas` com um recurso inexistente | 404 `ProblemDetail`: "Recurso não encontrado" | Integração |
| TC-39 (sem caso no plano) | CA-13 | Estado proibido | Dono altera uma reserva `EM_USO` | Recusa; a reserva não muda | Unitário |
| TC-40 (sem caso no plano) | CA-13 | Estado proibido | Dono altera uma reserva `CONCLUIDA`, `CANCELADA`, `REJEITADA` ou `NAO_COMPARECEU` | As quatro recusadas | Unitário |
| TC-41 (contrato) | CA-13 | Estado proibido | `PUT /api/reservas/{id}` de uma reserva `CONCLUIDA` | 409 `ProblemDetail`: "Esta reserva não pode mais ser alterada" | Integração |
| TC-42 (sem caso no plano) | CA-14 | Classe válida | Usuário autenticado cria uma reserva | O dono é esse usuário | Unitário |
| TC-43 (contrato) | CA-14 | Classe inválida | `POST /api/reservas` com um `solicitanteId` de outro usuário no corpo | 201; o dono é o usuário autenticado e o campo é ignorado | Integração |
| TC-44 (contrato) | CA-04 | Conflito | `POST /api/reservas` sobreposta a uma reserva de outro usuário | 409; a mensagem cita o recurso e não cita o outro usuário | Integração |
| TC-45 (sem caso no plano) | CA-15 | Valor limite | Pedido às 09:00 para as 10:00, exatamente 1 h antes | Aceita; guarda `solicitada_em = 09:00` | Unitário |
| TC-46 (sem caso no plano) | CA-15 | Valor limite | Pedido às 09:01 para as 10:00, 59 min antes | Recusa 409; nada é gravado | Unitário |
| TC-47 (contrato) | CA-15 | Classe inválida | `POST /api/reservas` com início a 30 min do pedido | 409 `ProblemDetail`; nada é gravado | Integração |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| A regra de ocupação depende do ciclo de vida da T-21 (sem tolerância de atraso) | Médio | TC-21 a TC-26 seguem a T-21; revistos se a T-21 mudar |
| Erro de um minuto na fronteira (`<` no lugar de `<=`) | Alto | TC-03, TC-04, TC-10, TC-12, TC-14 e TC-17 testam as fronteiras dos dois lados |
| A verificação de sobreposição é esquecida na alteração | Médio | TC-08, TC-15, TC-16 e TC-18 cobrem a alteração; o CA-09 existe só para isso |
| Pedido com vários recursos grava parte dele antes de falhar | Alto | TC-19 e TC-20, junto com o CA-10, exigem recusa por inteiro e nada gravado |
| O estado `CONCLUIDA` não tem caso no plano TDD | Baixo | TC-26 cobre a lacuna; incluir o caso no plano em uma nova revisão da T-19 |
| Duas solicitações simultâneas passam pela verificação ao mesmo tempo | Alto | Fora desta spec: o teste de concorrência (RN-03) é da semana 42; a verificação desta spec é sequencial |
| Reserva no passado e demais restrições de horário (RN-10 a RN-12) | Médio | Fora desta spec: são propostas do time e precisam de spec própria |
| Alterar uma reserva aprovada faz o dono perder o horário antigo se a alteração for rejeitada | Médio | Aceito (grill): a resposta e a tela avisam; guardar a versão anterior ficou fora |
| Fuso horário diferente entre pedido e `Clock` | Baixo | Todos os instantes em UTC (ADR 0008) |

## 8. Mini-plano de implementação (ciclos TDD)

Na T-20 as assinaturas mínimas devolvem um resultado neutro (que não é aceite nem recusa), para que cada teste falhe por asserção e não por compilação. Cada ciclo é um teste vermelho (T-20, commit `test(red)`), depois o código mínimo (T-23, commit `feat(green)`), depois a refatoração com tudo verde.

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-01 a TC-03: intervalo válido é aceito | Regra de reserva que aceita o intervalo | Extrair o intervalo para um objeto próprio |
| 2 | TC-04, TC-05 e TC-08: término igual ou anterior ao início | Validação `termino > inicio`, na criação e na alteração | Mover a validação para dentro do intervalo |
| 3 | TC-06 e TC-07: início ou término ausente | Recusa de dado ausente | Unificar as recusas de intervalo |
| 4 | TC-09 e TC-11: sobreposição em sala e material | Comparação dos intervalos no mesmo recurso | Extrair o método de sobreposição |
| 5 | TC-13: sobreposição na agenda do professor | A verificação passa a cobrir também o professor | Tratar os três tipos de recurso do mesmo jeito |
| 6 | TC-10, TC-12, TC-14 e TC-17: reservas encostadas | Intervalo fechado no início e aberto no término | Nomear a regra da fronteira |
| 7 | TC-27 e TC-28: recursos e intervalos distintos | Comparar só o mesmo recurso e só intervalos que se sobrepõem | Remover duplicação entre as verificações |
| 8 | TC-21 a TC-26: estados que ocupam o horário | Filtrar as reservas existentes pelo estado | Extrair o conjunto de estados que ocupam |
| 9f | TC-45 a TC-47: antecedência mínima | Recusar o pedido com menos de 1 h de antecedência e guardar o instante do pedido | Compartilhar a regra de prazos com a T-21 |
| 9e | TC-42 e TC-43: dono da reserva | Gravar o usuário autenticado como dono | Isolar a leitura da identidade (T-15) |
| 9d | TC-39 a TC-41: estados que não podem ser alterados | Recusar a alteração fora de `SOLICITADA` e `APROVADA` | Compartilhar a regra de prazo com a T-21 |
| 9c | TC-36 a TC-38: recurso inexistente e lista vazia | Validar os recursos do pedido | Reaproveitar a tradução de erros da T-22 |
| 9b | TC-32 a TC-35: alterar reserva aprovada | Voltar a `SOLICITADA` ao alterar | Isolar a regra de reinício da aprovação |
| 9 | TC-15, TC-16 e TC-18: alteração | Ignorar a própria reserva na verificação | Reaproveitar a verificação na criação e na alteração |
| 10 | TC-19 e TC-20: vários recursos | Verificar todos os recursos antes de gravar | Isolar a gravação em uma única operação |
| 11 | TC-29 a TC-31: contrato HTTP | Mapear os erros para 400 e 409 no formato da T-22 | Consolidar a tradução dos erros |

## 9. Revisão por pares

- [x] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [x] As regras citam RNs que existem no PRD.
- [x] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [x] Os casos de teste incluem valores limite e entradas inválidas.
- [x] O contrato da API cobre os erros (400, 401, 403, 409).
- [x] Não há conflito com outras specs nem com os ADRs.

**Comentários:** Conferi regras de intervalo, sobreposição, ocupação por estado, alteração de reserva, contratos da API e casos de teste.

Os critérios permitem validar aprovação e falha de forma objetiva. Os cenários cobrem valores limite, recursos distintos, conflitos de professor, sala e material, além das fronteiras de tempo definidas pela premissa S-01. Não encontrei inconsistências com o PRD, a utility tree, a arquitetura ou os ADRs referenciados.

**Parecer:** Aprovada · @CaiosAguiar · 2026-11-05

## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** _preencher_
- **Resultado:** _preencher_

## 11. Grill-with-docs

> Obrigatório só para regra crítica. Nas demais, escreva "Não se aplica".

| # | Pergunta | Resposta / decisão | Documento afetado |
|---|---|---|---|
| 1 | Alterar uma reserva `APROVADA` | Volta a `SOLICITADA`, a aguardar nova aprovação | CA-11, TC-32 a TC-35; T-21 |
| 2 | Alteração rejeitada ou vencida | O dono perde o horário antigo; a resposta avisa | Seção 7 |
| 3 | Conflito do professor | Só entre reservas que o alocam como recurso `PROFESSOR` (CA-05); ser solicitante não ocupa a agenda | CA-05 |
| 4 | Recurso inexistente ou lista vazia | 404 para inexistente; 400 com `campo = recursos` para lista vazia | CA-12, TC-36 a TC-38 |
| 5 | Quais estados podem ser alterados | Só `SOLICITADA` e `APROVADA`, dentro do prazo da T-21 (CA-13); os outros devolvem 409 | CA-13, TC-39 a TC-41 |
| 6 | Dono da reserva | O usuário autenticado; o corpo não traz o dono | CA-14, TC-42 e TC-43 |
| 7 | Mensagem do 409 | Só o recurso e o fato; não revela quem reservou | CA-04, TC-44 |
| 8 | Antecedência mínima do pedido | 1 h, inclusive; a reserva guarda `solicitada_em`; 409 se menos | CA-15, TC-45 a TC-47 |

**Resultado:** Pendências (o CA-15 é novo e precisa de nova revisão do Caio) · 2026-11-06
