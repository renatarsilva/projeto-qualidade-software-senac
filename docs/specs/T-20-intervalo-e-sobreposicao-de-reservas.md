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
| Status | Liberada |
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
- Antecedência mínima de 1 h do início (RN-15), no pedido e na alteração, e os campos `criada_em` e `prazo_aprovacao_em` da reserva.
- A alteração de recursos pelo `PUT` (troca, liberação do recurso anterior e atomicidade).
- Contrato de `POST /api/reservas` e `PUT /api/reservas/{id}` para esses erros, inclusive 401, 403 e 404 do `PUT`.
- A verificação de sobreposição na aplicação e a tradução da recusa do banco (`EXCLUDE`) para 409, que é feita pela T-22 (CA-08).

**Fora (e por quê):**
- Transições de estado, cancelamento e "reserva iniciada não se apaga" (RN-05, RN-06, RN-08): são da T-21 e da T-24.
- Duas solicitações simultâneas (RN-03): o **teste de concorrência** é da T-37 (semana 42). Aqui a verificação da aplicação é sequencial e a garantia do PostgreSQL (`EXCLUDE`) é testada com inserção direta e com a tradução do erro para 409 (CA-04, TC-57 e TC-58).
- Recurso em manutenção (RN-04): semana 41.
- Auditoria (RN-09), autenticação e perfis (RN-07, T-15): semanas 43 e 44.
- Janela de funcionamento, último horário e antecedência máxima (RN-10 a RN-12): são propostas do time e precisam de spec própria. A antecedência **mínima** de 1 h (RN-15) é desta spec, e por ela uma reserva com início no passado também é recusada (CA-15). O cancelamento que libera o recurso (RN-13) é da T-21.
- O cálculo dos prazos de aprovação e de alteração e a transição de estado: são da T-21. Esta spec só grava `prazo_aprovacao_em` no pedido e na alteração.
- O formato das respostas de erro: é da T-22. Esta spec só diz **qual** erro ocorre.

## 3. Regras de negócio

- **RN-01:** o término da reserva deve ser posterior ao início. A comparação é entre data e hora. Término igual ou anterior ao início é recusado, e início ou término ausente também.
- **RN-02:** reservas do mesmo recurso (sala, material ou professor) não podem se sobrepor. O pedido é verificado contra **cada** recurso pedido: basta um ocupado para recusar o pedido inteiro.

- **RN-15 (antecedência mínima, regra do time, registrada no PRD):** o início da reserva precisa estar pelo menos 1 h depois do instante atual, e exatamente 1 h ainda vale. A regra vale no pedido (`POST`) e na alteração (`PUT`), medida entre o instante atual do `Clock` e o **novo** início, e nunca contra o `criada_em` original. Um início no passado também é recusado. A RN-15 **substitui** o caso RN-12-T3 do plano TDD, que aceitava início igual ao instante atual: o plano será atualizado em outro commit (T-19).
- **`criada_em` (G11):** é o instante em que o pedido é aceito, obtido do `Clock` da aplicação (ADR-0008). O cliente não o informa e nenhuma alteração o muda: o `PUT` preserva o valor.
- **`prazo_aprovacao_em` (RN-14, T-21):** coluna da reserva, gravada no pedido e **recalculada na alteração com o novo início**: 24 h antes do início ou, se o pedido (ou a alteração) tem menos de 24 h de antecedência, 1 h antes. O cálculo e o que acontece no prazo são da T-21.
- **Alteração (decisão do grill):** alterar uma reserva `APROVADA` a devolve ao estado `SOLICITADA`, a aguardar nova aprovação, e recalcula o `prazo_aprovacao_em`. O horário antigo é liberado e o novo é ocupado (`SOLICITADA` ocupa). Se a alteração for rejeitada ou o prazo de aprovação vencer (A-05), a reserva vira `REJEITADA` ou `CANCELADA` e o dono perde o horário antigo. A resposta traz o campo `aviso`. O prazo para alterar (RN-14, L11) é da T-21 (CA-13).
- **Ordem das verificações e dos erros (G9):** 401 (sem autenticação), 403 por perfil, 404 (reserva ou recurso inexistente), 403 por dono (quem altera não é o dono), 400 (entrada inválida) e, por último, os 409. Entre os 409 a ordem é: reserva que não pode mais ser alterada (estado ou prazo, T-21), antecedência mínima insuficiente e, por fim, sobreposição. Cada 409 tem uma mensagem própria, e os testes conferem a mensagem.
- **Mensagens do 409 (G2):** quando a verificação da aplicação pega a sobreposição, a mensagem é específica ("A sala já está reservada neste horário"). Quando a verificação passa e o banco recusa (`EXCLUDE`, numa corrida), a T-22 devolve 409 com a mensagem fixa "Esta operação conflita com outro registro". Em nenhum dos dois casos a mensagem revela quem reservou.

**Premissas da arquitetura** (S-01 e S-02 são decisões do time; D-02 segue da arquitetura):

- **S-01 (decisão do time):** o período é fechado no início e aberto no término, `[início, fim)`. Uma reserva que termina exatamente quando outra começa **não** é sobreposição.
- **S-02 (decisão do time):** `SOLICITADA`, `APROVADA` e `EM_USO` ocupam o horário. `REJEITADA`, `CANCELADA`, `NAO_COMPARECEU` e `CONCLUIDA` não bloqueiam conflito, mas continuam registradas para relatório. Uma `APROVADA` ocupa o horário inteiro até o dono cancelar ou fazer check-in, sem tolerância de atraso (ver T-21).
- **D-02:** a reserva se liga aos recursos pela tabela `reserva_recurso`, então uma reserva pode ocupar sala, professor e material ao mesmo tempo.

## 4. Contrato da API

`POST /api/reservas`: cria uma reserva (perfil SOLICITANTE, regra de acesso na T-15). O endpoint e os campos são uma proposta desta spec, porque o PRD ainda não os define. Os exemplos usam o `Clock` fixo de teste (segunda, 2026-11-09, 08:00 UTC).

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

O **dono** da reserva é o usuário autenticado que fez o `POST`: o corpo não traz campo de dono, e ninguém reserva em nome de outra pessoa (a identidade vem da T-15). O corpo também não traz `criada_em` nem `prazo_aprovacao_em`: um valor enviado é ignorado.

`PUT /api/reservas/{id}`: altera uma reserva (perfil SOLICITANTE e dono da reserva), com o mesmo corpo. A reserva que está sendo alterada **não** conta como conflito consigo mesma. O `PUT` substitui o intervalo e a lista de recursos por inteiro e é atômico: ou tudo muda, ou nada muda.

**Resposta de sucesso do `PUT`** (a reserva e o aviso):

```json
{
  "id": 15,
  "estado": "SOLICITADA",
  "inicio": "2026-11-10T14:00:00Z",
  "termino": "2026-11-10T15:00:00Z",
  "criadaEm": "2026-11-09T08:00:00Z",
  "prazoAprovacaoEm": "2026-11-09T13:00:00Z",
  "aviso": "A reserva precisa ser aprovada de novo"
}
```

O campo `aviso` só aparece quando a reserva estava `APROVADA` e voltou a `SOLICITADA`; nos outros casos ele não vem no corpo.

**Respostas** (erros no formato da T-22):

| Situação | Status HTTP | Corpo |
|---|---|---|
| Sucesso | 201 (criação) ou 200 (alteração) | A reserva, no estado `SOLICITADA` na criação; na alteração de uma `APROVADA`, a reserva `SOLICITADA` com o campo `aviso` |
| Término igual ou anterior ao início | 400 | `ProblemDetail`, `erros` com `campo = termino` e `motivo = "deve ser posterior ao início"` |
| Início ou término ausente | 400 | `ProblemDetail`, `erros` com o campo ausente e `motivo = "deve ser informado"` |
| Sem autenticação | 401 | `ProblemDetail` neutro (formato da T-22; o mecanismo é da T-15) |
| Perfil sem permissão (quem não é SOLICITANTE) | 403 | `ProblemDetail` (T-22, CA-10) |
| `PUT` de reserva que não existe | 404 | `ProblemDetail`: "Recurso não encontrado"; nada muda |
| `PUT` por quem não é o dono | 403 | `ProblemDetail`; a reserva não muda |
| Alterar reserva que não está `SOLICITADA` nem `APROVADA`, ou fora do prazo da T-21 (CA-13) | 409 | `ProblemDetail` em linguagem de negócio, por exemplo "Esta reserva não pode mais ser alterada"; a reserva não muda |
| Recurso que não existe | 404 | `ProblemDetail`: "Recurso não encontrado"; nada é gravado |
| Lista de recursos vazia | 400 | `ProblemDetail`, `erros` com `campo = recursos` e `motivo = "deve ter ao menos um recurso"` |
| Início com menos de 1 h de antecedência (inclusive no passado), no pedido ou na alteração | 409 | `ProblemDetail`: "A reserva precisa ser pedida com pelo menos 1 hora de antecedência"; nada é gravado ou alterado |
| Sobreposição em algum recurso, pega pela aplicação | 409 | `ProblemDetail` em linguagem de negócio, por exemplo "A sala já está reservada neste horário" |
| Sobreposição que só o banco pega (corrida, `EXCLUDE`) | 409 | `ProblemDetail`: "Esta operação conflita com outro registro" (T-22, CA-08) |

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
- **E** nenhuma linha é gravada em `reserva` nem em `reserva_recurso`
- **E** a mensagem não revela quem reservou nem outros dados da reserva existente
- **E** se a verificação da aplicação não pegar o conflito e a recusa vier do banco (`EXCLUDE`, numa corrida), a resposta continua sendo 409, com a mensagem fixa "Esta operação conflita com outro registro" (T-22, CA-08)
- **E** o teste de concorrência (dois pedidos de verdade ao mesmo tempo) é da T-37
- **E** na alteração, a reserva mantém o horário original

### CA-05: Sobreposição na agenda do professor é recusada

- **Dado** que um professor já tem reserva que ocupa um intervalo
- **Quando** o cliente pede uma reserva em que esse professor está alocado, em intervalo sobreposto, ainda que a sala e o material estejam livres
- **Então** o pedido é recusado com 409 e nada é gravado
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
- **E** o corpo da resposta traz `estado = SOLICITADA` e o campo `aviso` com "A reserva precisa ser aprovada de novo"
- **E** o `prazo_aprovacao_em` é recalculado com o novo início

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

### CA-15: Antecedência mínima de 1 hora (RN-15)

- **Dado** o cadastro ou a alteração de reserva, com o `Clock` fixo
- **Quando** o novo início está a menos de 1 h do instante atual do `Clock`, inclusive no passado
- **Então** o pedido é recusado com 409, com a mensagem "A reserva precisa ser pedida com pelo menos 1 hora de antecedência", e nada é gravado ou alterado
- **E** com exatamente 1 h de antecedência, ou mais, o pedido é aceito
- **E** na alteração, a antecedência é medida contra o instante atual do `Clock`, e não contra o `criada_em` original: uma reserva antiga não pode ser alterada para começar poucos minutos depois de agora
- **E** quando uma reserva `APROVADA` volta a `SOLICITADA` pela alteração, vale a mesma conta, e o `prazo_aprovacao_em` é recalculado com o novo início (24 h antes do início ou, com menos de 24 h de antecedência, 1 h antes)
- **E** se faltar antecedência **e** houver sobreposição ao mesmo tempo, a resposta é a de antecedência: ela vem antes na ordem dos 409, e os testes conferem a mensagem

### CA-16: `criada_em` vem do `Clock` e não muda

- **Dado** um pedido aceito
- **Quando** a reserva é gravada
- **Então** o `criada_em` é o instante atual do `Clock` (ADR-0008), em UTC
- **E** um `criada_em` enviado no corpo é ignorado
- **E** o `PUT` preserva o `criada_em` original

### CA-17: Alterar os recursos de uma reserva é atômico

- **Dado** uma reserva do dono com um ou mais recursos
- **Quando** ele a altera trocando ou acrescentando um recurso
- **Então** se o recurso novo estiver ocupado no intervalo, a alteração é recusada com 409 e a reserva continua com os recursos e o horário originais
- **E** se estiver livre, a alteração é aceita e o recurso que saiu da reserva fica livre para outro usuário
- **E** se qualquer recurso da nova lista estiver ocupado, **nada** muda: nem os recursos que estavam livres entram na reserva
- **E** os recursos que a reserva já ocupava não contam como conflito consigo mesma

### CA-18: Contrato do `PUT`: acesso, existência e dono

- **Dado** o `PUT /api/reservas/{id}`
- **Quando** o cliente não está autenticado, não tem o perfil SOLICITANTE, pede uma reserva que não existe ou não é o dono da reserva
- **Então** a resposta é 401, 403, 404 e 403, respectivamente, e a reserva não muda
- **E** a ordem de verificação é 401, 403 por perfil, 404, 403 por dono, 400 e 409 (por exemplo, quem não é o dono recebe 403 mesmo que o corpo seja inválido, e uma reserva inexistente recebe 404 mesmo com corpo inválido)

## 6. Casos de teste

Todo critério de aceite precisa de pelo menos um caso. Use classes de equivalência e valor limite.

**Relógio dos testes:** todos os casos usam um `Clock` fixo de teste em **segunda, 2026-11-09, 08:00 UTC**, injetado (ADR-0008), salvo quando o caso indica outro instante. As datas dos testes, inclusive as de integração, são calculadas **a partir desse relógio** (por exemplo, "terça, 10:00" é 2026-11-10T10:00Z), e nunca a partir do relógio real; assim os testes não envelhecem. Os horários "10:00 às 11:00" sem outra indicação são os de terça, 2026-11-10; "no mesmo dia" é a segunda, 2026-11-09, com início depois das 09:00.

Os casos da regra são testes **unitários** da regra de reserva (regra pura, sem HTTP nem banco). Os casos que conferem status HTTP, corpo da resposta ou linhas gravadas são **de integração** (G13). Cada caso cita entre parênteses o caso do [plano TDD](../testes/plano-tdd.md) ou a verificação do CT-01; os casos `V01` a `V21` são agrupados e rodam como teste parametrizado. O caso do estado `CONCLUIDA` vem da premissa S-02 e **ainda não tem caso no plano** (ver seção 7). A RN-15 (antecedência mínima) substitui a RN-12-T3 do plano TDD. Nos casos de recusa, "nada é gravado" quer dizer que não surge nenhuma linha nova em `reserva` nem em `reserva_recurso`, e isso é conferido no teste.

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
| TC-28 (RN-02-T13) | CA-07 | Classe válida | B pede a sala-102 em outro dia, das 10:00 às 11:00 | Aceita | Unitário |
| TC-29 (contrato) | CA-02 | Classe inválida | `POST /api/reservas` com término igual ao início | 400 `ProblemDetail`, `erros` com `campo = termino` | Integração |
| TC-30 (contrato) | CA-04 | Conflito | `POST /api/reservas` sobreposta a uma reserva existente | 409 `ProblemDetail` com `detail = "A sala já está reservada neste horário"`; nenhuma linha nova em `reserva` nem em `reserva_recurso` | Integração |
| TC-31 (contrato) | CA-01 | Classe válida | `POST /api/reservas` com intervalo livre | 201 com a reserva no estado `SOLICITADA` | Integração |
| TC-32 (sem caso no plano) | CA-11 | Classe válida | Dono altera uma reserva `APROVADA` para um horário livre | Estado `SOLICITADA`; horário antigo livre, novo ocupado | Unitário |
| TC-33 (sem caso no plano) | CA-11 | Classe válida | Dono altera uma reserva `SOLICITADA` para um horário livre | Continua `SOLICITADA` | Unitário |
| TC-34 (sem caso no plano) | CA-11 | Conflito | Dono altera uma reserva `APROVADA` para um horário ocupado | Recusa 409; continua `APROVADA` no horário original | Unitário |
| TC-35 (contrato) | CA-11 | Classe válida | `PUT /api/reservas/{id}` de uma reserva `APROVADA` | 200; o corpo traz `estado = SOLICITADA` e `aviso = "A reserva precisa ser aprovada de novo"`, e `prazoAprovacaoEm` recalculado | Integração |
| TC-36 (sem caso no plano) | CA-12 | Classe inválida | Pedido com a sala 999, que não existe | Recusa como não encontrada; nada gravado | Unitário |
| TC-37 (sem caso no plano) | CA-12 | Classe inválida | Pedido com a lista de recursos vazia | Recusa; o erro aponta `recursos` | Unitário |
| TC-38 (contrato) | CA-12 | Classe inválida | `POST /api/reservas` com um recurso inexistente | 404 `ProblemDetail`: "Recurso não encontrado" | Integração |
| TC-39 (sem caso no plano) | CA-13 | Estado proibido | Dono altera uma reserva `EM_USO` | Recusa; a reserva não muda | Unitário |
| TC-40 (sem caso no plano) | CA-13 | Estado proibido | Dono altera uma reserva `CONCLUIDA`, `CANCELADA`, `REJEITADA` ou `NAO_COMPARECEU` | As quatro recusadas | Unitário |
| TC-41 (contrato) | CA-13 | Estado proibido | `PUT /api/reservas/{id}` de uma reserva `CONCLUIDA` | 409 `ProblemDetail`: "Esta reserva não pode mais ser alterada" | Integração |
| TC-42 (sem caso no plano) | CA-14 | Classe válida | Usuário autenticado cria uma reserva | O dono é esse usuário | Unitário |
| TC-43 (contrato) | CA-14 | Classe inválida | `POST /api/reservas` com um `solicitanteId` de outro usuário no corpo | 201; o dono é o usuário autenticado e o campo é ignorado | Integração |
| TC-44 (contrato) | CA-04 | Conflito | `POST /api/reservas` sobreposta a uma reserva de outro usuário | 409; a mensagem cita o recurso e não cita o outro usuário | Integração |
| TC-45 (sem caso no plano) | CA-15 | Valor limite | `Clock` em 09:00 e início às 10:00 do mesmo dia, exatamente 1 h antes | Aceita | Unitário |
| TC-46 (sem caso no plano) | CA-15 | Valor limite | `Clock` em 09:01 e início às 10:00 do mesmo dia, 59 min antes | Recusa; nada é gravado | Unitário |
| TC-47 (contrato) | CA-15 | Classe inválida | `POST /api/reservas` com `Clock` em 08:00 e início às 08:30 | 409 `ProblemDetail` com `detail = "A reserva precisa ser pedida com pelo menos 1 hora de antecedência"`; nenhuma linha nova em `reserva` | Integração |
| TC-48 (sem caso no plano) | CA-16 | Classe válida | `POST /api/reservas` válido com o `Clock` em 08:00 | A reserva persistida tem `criada_em = 2026-11-09T08:00:00Z` | Integração |
| TC-49 (sem caso no plano) | CA-16 | Classe inválida | `POST /api/reservas` com `criadaEm` e `prazoAprovacaoEm` no corpo | 201; os dois campos enviados são ignorados e `criada_em` vem do `Clock` | Integração |
| TC-50 (sem caso no plano) | CA-16 | Classe válida | `PUT` feito com o `Clock` avançado para 08:30 | `criada_em` continua `08:00`; a resposta e o banco trazem o mesmo valor | Integração |
| TC-51 (sem caso no plano) | CA-15 | Valor limite | Reserva criada há dias; dono a altera com o `Clock` em 08:00 para começar às 08:30 | Recusa (30 min); o `criada_em` antigo não vale como instante atual; a reserva não muda | Unitário |
| TC-52 (sem caso no plano) | CA-15 | Valor limite | Mesma reserva; dono a altera com o `Clock` em 08:00 para começar às 09:00, exatamente 1 h depois | Aceita | Unitário |
| TC-53 (sem caso no plano) | CA-15 | Classe válida | `APROVADA` alterada com 2 dias de antecedência / com 3 h de antecedência | `SOLICITADA`; `prazo_aprovacao_em` = novo início menos 24 h no primeiro caso e menos 1 h no segundo | Unitário |
| TC-54 (sem caso no plano) | CA-15 | Classe inválida | `Clock` em 08:00 e início às 07:00, no passado | Recusa; nada é gravado | Unitário |
| TC-55 (contrato) | CA-15 | Conflito | `POST` com `Clock` em 08:00, início às 08:30 e a sala ocupada nesse horário | 409 com `detail` de **antecedência** (precedência); a mensagem de sobreposição não aparece; nenhuma linha nova | Integração |
| TC-56 (contrato) | CA-04 | Conflito | `POST` sobreposta a uma reserva existente, com antecedência suficiente | 409 com `detail = "A sala já está reservada neste horário"`, diferente da mensagem de antecedência | Integração |
| TC-57 (contrato) | CA-04 | Conflito | Duas reservas ativas sobrepostas, no mesmo recurso, inseridas direto no banco | O banco recusa a segunda (`EXCLUDE`) | Integração |
| TC-58 (contrato) | CA-04 | Conflito | `POST` em que a verificação da aplicação é contornada no teste e o banco recusa por `EXCLUDE` | 409 com `detail = "Esta operação conflita com outro registro"`; nenhuma linha nova | Integração |
| TC-59 (sem caso no plano) | CA-17 | Conflito | Dono troca a sala 101 pela 102, ocupada no horário | Recusa 409; a reserva continua na sala 101 e no horário original | Unitário |
| TC-60 (sem caso no plano) | CA-17 | Classe válida | Dono troca a sala 101 pela 102, livre; depois outro usuário pede a 101 no mesmo horário | A troca é aceita; o pedido do outro usuário na 101 também (o recurso anterior foi liberado) | Integração |
| TC-61 (sem caso no plano) | CA-17 | Conflito | Reserva com sala e projetor; dono pede sala nova livre e projetor novo ocupado | Recusa por inteiro; a reserva continua com a sala e o projetor originais (atomicidade) | Integração |
| TC-62 (sem caso no plano) | CA-17 | Classe válida | Reserva com sala e professor; dono muda só o término, mantendo os dois recursos | Aceita (os recursos da própria reserva não contam como conflito) | Unitário |
| TC-63 (contrato) | CA-18 | Classe inválida | `PUT /api/reservas/999`, que não existe | 404 `ProblemDetail`: "Recurso não encontrado"; nada muda | Integração |
| TC-64 (contrato) | CA-18 | Tabela de decisão | `PUT` por RESPONSAVEL e por ADMINISTRADOR | 403 `ProblemDetail`; a reserva não muda | Integração |
| TC-65 (contrato) | CA-18 | Conflito | `PUT` de um SOLICITANTE que não é o dono da reserva | 403 `ProblemDetail`; a reserva não muda | Integração |
| TC-66 (contrato) | CA-18 | Classe inválida | `PUT` sem autenticação | 401 `ProblemDetail` neutro; a reserva não muda | Integração |
| TC-67 (contrato) | CA-18 | Tabela de decisão | Quem não é o dono envia `PUT` com término anterior ao início; e, em outro caso, `PUT` de reserva inexistente com corpo inválido | 403 no primeiro (dono antes de 400) e 404 no segundo (404 antes de 400) | Integração |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| A regra de ocupação depende do ciclo de vida da T-21 (sem tolerância de atraso) | Médio | TC-21 a TC-26 seguem a T-21; revistos se a T-21 mudar |
| Erro de um minuto na fronteira (`<` no lugar de `<=`) | Alto | TC-03, TC-04, TC-10, TC-12, TC-14 e TC-17 testam as fronteiras dos dois lados |
| A verificação de sobreposição é esquecida na alteração | Médio | TC-08, TC-15, TC-16 e TC-18 cobrem a alteração; o CA-09 existe só para isso |
| Pedido com vários recursos grava parte dele antes de falhar | Alto | TC-19 e TC-20, junto com o CA-10, exigem recusa por inteiro e nada gravado |
| O estado `CONCLUIDA` não tem caso no plano TDD | Baixo | TC-26 cobre a lacuna; incluir o caso no plano em uma nova revisão da T-19 |
| Duas solicitações simultâneas passam pela verificação ao mesmo tempo | Alto | A garantia é do banco (`EXCLUDE`): TC-57 e TC-58 provam a recusa e a tradução para 409 com a mensagem genérica; o teste de concorrência de verdade (RN-03) é da T-37 |
| Janela de funcionamento, último horário e antecedência máxima (RN-10 a RN-12) | Médio | Fora desta spec: são propostas do time e precisam de spec própria. A reserva no passado é tratada pela RN-15 (TC-54) |
| A RN-15 (1 h) contradiz o caso RN-12-T3 do plano TDD, que aceitava início igual ao instante atual | Médio | A RN-15 substitui a RN-12-T3; o plano TDD é atualizado em outro commit (T-19) e a regra fica no PRD |
| Alterar uma reserva antiga burla a antecedência se for medida contra o `criada_em` | Alto | A antecedência é medida contra o `Clock` (CA-15, TC-51 e TC-52) |
| `criada_em` alterado pelo cliente ou pelo `PUT` mudaria os prazos | Alto | CA-16, TC-48 a TC-50: vem do `Clock` e é imutável |
| Duas causas de 409 ao mesmo tempo escondem uma à outra | Médio | Ordem fixa e mensagens distintas (CA-15, TC-55 e TC-56) |
| A alteração de recursos grava parte antes de falhar | Alto | CA-17 e TC-59 a TC-61: atômica, conferida pelas linhas gravadas |
| Os testes envelhecem com o relógio real | Médio | `Clock` fixo de teste (segunda, 2026-11-09, 08:00 UTC) e datas relativas a ele, inclusive nos testes de integração |
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
| 9 | TC-15, TC-16, TC-18 e TC-62: alteração | Ignorar a própria reserva na verificação | Reaproveitar a verificação na criação e na alteração |
| 9b | TC-32 a TC-35: alterar reserva aprovada | Voltar a `SOLICITADA` ao alterar e devolver o `aviso` | Isolar a regra de reinício da aprovação |
| 9c | TC-36 a TC-38: recurso inexistente e lista vazia | Validar os recursos do pedido | Reaproveitar a tradução de erros da T-22 |
| 9d | TC-39 a TC-41: estados que não podem ser alterados | Recusar a alteração fora de `SOLICITADA` e `APROVADA` | Compartilhar a regra de prazo com a T-21 |
| 9e | TC-42 e TC-43: dono da reserva | Gravar o usuário autenticado como dono | Isolar a leitura da identidade (T-15) |
| 9f | TC-45, TC-46, TC-47 e TC-54: antecedência mínima no pedido | Recusar o pedido com menos de 1 h de antecedência, inclusive no passado | Compartilhar a regra de prazos com a T-21 |
| 9g | TC-48 a TC-50: `criada_em` do `Clock` | Gravar o `criada_em` do `Clock`, ignorar o do corpo e preservá-lo no `PUT` | Isolar a leitura do relógio |
| 9h | TC-51 a TC-53: antecedência na alteração e `prazo_aprovacao_em` | Medir a antecedência contra o `Clock` na alteração e recalcular o prazo com o novo início | Reaproveitar a mesma conta no pedido e na alteração |
| 9i | TC-55 e TC-56: ordem dos 409 | Verificar a antecedência antes da sobreposição e devolver mensagens distintas | Nomear a ordem das verificações |
| 9j | TC-59 a TC-61: alterar recursos | Troca de recurso ocupado recusada, recurso anterior liberado, alteração atômica | Isolar a gravação em uma única operação |
| 10 | TC-19 e TC-20: vários recursos | Verificar todos os recursos antes de gravar | Isolar a gravação em uma única operação |
| 11 | TC-29 a TC-31: contrato HTTP | Mapear os erros para 400 e 409 no formato da T-22 | Consolidar a tradução dos erros |
| 12 | TC-30, TC-44, TC-57 e TC-58: banco e mensagens do 409 | Conferir a mensagem de sobreposição, não gravar linhas na recusa e traduzir o `EXCLUDE` para 409 (T-22) | Consolidar a tradução dos erros |
| 13 | TC-63 a TC-67: contrato do `PUT` | 401, 403 por perfil, 404, 403 por dono e a ordem das verificações | Declarar a ordem das verificações em um só lugar |

Em cada ciclo o teste nasce vermelho porque a assinatura mínima devolve o resultado neutro e o código do ciclo anterior ainda não trata o caso novo (por exemplo, o ciclo 9g falha porque o `criada_em` ainda não vem do `Clock`).

## 9. Revisão por pares

- [X] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [X] As regras citam RNs que existem no PRD.
- [X] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [X] Os casos de teste incluem valores limite e entradas inválidas.
- [X] O contrato da API cobre os erros (400, 401, 403, 409).
- [X] Não há conflito com outras specs nem com os ADRs.

**Comentários:**

Realizei uma nova leitura da versão atual da T-20, considerando os ajustes solicitados nas revisões anteriores e a relação da especificação com as regras críticas de intervalo e sobreposição.

Confirmei que `criada_em` representa o instante em que o pedido é aceito, utiliza o `Clock` da aplicação e permanece imutável durante alterações posteriores. Também verifiquei que valores enviados pelo cliente não substituem esse campo e que os casos de teste utilizam um relógio fixo, evitando resultados dependentes da data real de execução.

A regra de antecedência mínima está rastreada no PRD e, durante o `PUT`, utiliza o novo início solicitado como referência. A especificação também define a precedência entre a validação de antecedência e a verificação de sobreposição, permitindo identificar corretamente o motivo de cada resposta `409 Conflict`.

As regras de intervalo e sobreposição continuam coerentes com a RN-01 e a RN-02. Foram preservados os cenários de início igual ao término, término anterior ao início, horários adjacentes, sobreposição parcial, sobreposição total, reserva contida em outra e reserva que contém outra. Os conflitos de sala, professor e material também permanecem cobertos.

Revisei os cenários de alteração de horário e troca de recursos. A operação está definida como atômica, preservando os dados anteriores quando ocorrer uma recusa e liberando os recursos anteriores somente após uma alteração bem-sucedida. O contrato do `PUT` contempla autenticação, autorização, recurso inexistente e conflitos de negócio, com respostas `401`, `403`, `404` e `409`.

Também conferi a separação entre a validação realizada pela aplicação, a garantia de integridade fornecida pelo PostgreSQL e a futura validação de concorrência real. Essa separação está coerente com a arquitetura e com a utility tree.

Os critérios de aceite estão verificáveis, os casos de teste cobrem os limites e entradas inválidas relevantes e não identifiquei inconsistências impeditivas com o PRD, a arquitetura, a utility tree, o ADR-0008 ou as specs relacionadas.

Considero atendidos os pontos necessários para a liberação da especificação.

**Parecer:** Aprovada · @CaiosAguiar · 2026-11-07

## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** 2026-11-07
- **Resultado:** APROVADA na spec validation

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
| 8 | Antecedência mínima do pedido | 1 h, inclusive; 409 se menos (RN-15) | CA-15, TC-45 a TC-47 |
| 9 | Origem e imutabilidade do `criada_em` | Vem do `Clock`, o cliente não informa e o `PUT` preserva | CA-16, TC-48 a TC-50 |
| 10 | Antecedência na alteração | Medida contra o `Clock` e o novo início, nunca contra o `criada_em`; recalcula o `prazo_aprovacao_em` | CA-15, TC-51 a TC-53 |
| 11 | RN-15 contra a RN-12-T3 do plano TDD | A RN-15 substitui a RN-12-T3 e fica no PRD; início no passado é recusado | Seção 3, PRD, plano TDD |
| 12 | Ordem dos erros e das mensagens do 409 | 401, 403 por perfil, 404, 403 por dono, 400 e 409; antecedência antes de sobreposição, com mensagens distintas | Seção 3, CA-15, CA-18, TC-55 a TC-67 |
| 13 | Escopo da concorrência | A T-20 cobre a verificação da aplicação e o `EXCLUDE` com 409 genérico; o teste de concorrência é da T-37 | CA-04, TC-57 e TC-58 |
| 14 | Alteração de recursos e contrato do `PUT` | Atômica; 401, 403 por perfil, 404 e 403 por dono; campo `aviso` no corpo | CA-17, CA-18, TC-59 a TC-67 |

**Resultado:** Liberada · 2026-11-07
