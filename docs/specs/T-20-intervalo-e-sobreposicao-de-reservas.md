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
| Status | Rascunho |
| Regra crítica | Sim |
| Requisitos | RF-04, RF-05 |
| Característica ISO 25010 | Adequação funcional |
| Criada em | 2026-11-04 |

## 1. Contexto e objetivo

Uma reserva é um pedido de uso de recursos (sala, material, professor) em um intervalo de tempo. Duas regras decidem se o pedido é válido: o **término vem depois do início** (RN-01) e **dois pedidos não podem ocupar o mesmo recurso ao mesmo tempo** (RN-02). Esta spec define o comportamento dessas duas regras para a criação e a alteração de reserva, para que os testes da T-20 nasçam vermelhos e a implementação da T-23 os deixe verdes. Os casos vêm do [plano TDD](../testes/plano-tdd.md).

## 2. Escopo

**Dentro:**
- Validar o intervalo da reserva (RN-01), na criação e na alteração.
- Detectar sobreposição no mesmo recurso (sala, material ou professor), inclusive na agenda do professor (RN-02, RF-05).
- Decidir quais estados da reserva ocupam o horário (provisório, ver seção 3).
- Contrato de `POST /api/reservas` e `PUT /api/reservas/{id}` para esses erros.

**Fora (e por quê):**
- Transições de estado, cancelamento e "reserva iniciada não se apaga" (RN-05, RN-06, RN-08): são da T-21 e da T-24.
- Duas solicitações simultâneas (RN-03): o teste de concorrência é da semana 42. Aqui a verificação é sequencial.
- Recurso em manutenção (RN-04): semana 41.
- Auditoria (RN-09), autenticação e perfis (RN-07, T-15): semanas 43 e 44.
- O formato das respostas de erro: é da T-22. Esta spec só diz **qual** erro ocorre.

## 3. Regras de negócio

- **RN-01:** o término da reserva deve ser posterior ao início. Término igual ou anterior ao início é recusado. Início ou término ausente também.
- **RN-02:** reservas do mesmo recurso (sala, material ou professor) não podem se sobrepor. A reserva é verificada contra **cada** recurso pedido: basta um recurso ocupado para recusar o pedido.

**Decisões provisórias** (o PRD é omisso; a decisão final é do professor ou da equipe e está no plano TDD, seção 3):

- **L01:** o intervalo é fechado no início e aberto no término. Uma reserva que termina exatamente quando outra começa **não** conta como sobreposição.
- **L02:** os estados `SOLICITADA`, `APROVADA` e `EM_USO` ocupam o horário. `CANCELADA` e `REJEITADA` o liberam. O PRD não diz nada sobre `CONCLUIDA` e `NAO_COMPARECEU`; esta spec não trata esses dois, porque a reserva deles já passou.

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

`PUT /api/reservas/{id}`: altera uma reserva, com o mesmo corpo. A reserva que está sendo alterada **não** conta como conflito consigo mesma.

**Respostas** (erros no formato da T-22):

| Situação | Status HTTP | Corpo |
|---|---|---|
| Sucesso | 201 (criação) ou 200 (alteração) | A reserva, no estado `SOLICITADA` na criação |
| Término igual ou anterior ao início | 400 | `ProblemDetail`, `erros` com `campo = termino` e `motivo = "deve ser posterior ao início"` |
| Início ou término ausente | 400 | `ProblemDetail`, `erros` com o campo ausente e `motivo = "deve ser informado"` |
| Sobreposição em algum recurso | 409 | `ProblemDetail` em linguagem de negócio, por exemplo "A sala já está reservada neste horário" |
| Sem autenticação e perfil sem permissão | 401 e 403 | Fora desta spec (T-15), mesmo formato |

## 5. Critérios de aceite

### CA-01: Intervalo válido é aceito

- **Dado** que existe um recurso livre
- **Quando** o cliente pede uma reserva com término posterior ao início, mesmo que por um único minuto
- **Então** a reserva é aceita no estado `SOLICITADA`

### CA-02: Término igual ou anterior ao início é recusado

- **Dado** o cadastro de reserva (criação ou alteração)
- **Quando** o cliente informa um término igual ou anterior ao início
- **Então** o pedido é recusado com 400
- **E** o erro aponta o campo `termino` com o motivo "deve ser posterior ao início"

### CA-03: Intervalo incompleto é recusado

- **Dado** o cadastro de reserva
- **Quando** o cliente não informa o início ou o término
- **Então** o pedido é recusado com 400
- **E** o erro aponta o campo ausente

### CA-04: Sobreposição no mesmo recurso é recusada

- **Dado** que já existe uma reserva ativa de um recurso (sala ou material) em um intervalo
- **Quando** o cliente pede o mesmo recurso em um intervalo que se sobrepõe a ela, em qualquer uma destas formas: começa dentro, termina dentro, está contido, contém ou é idêntico
- **Então** o pedido é recusado com 409
- **E** a mensagem diz, em linguagem de negócio, que o recurso já está reservado no horário

### CA-05: Sobreposição na agenda do professor é recusada

- **Dado** que um professor já tem reserva ativa em um intervalo
- **Quando** o cliente pede uma reserva em que esse professor está alocado, em intervalo sobreposto, ainda que a sala esteja livre
- **Então** o pedido é recusado com 409
- **E** a mensagem indica que o professor já está ocupado no horário

### CA-06: Reservas encostadas não conflitam

- **Dado** que existe uma reserva ativa de um recurso
- **Quando** o cliente pede o mesmo recurso começando exatamente quando ela termina, ou terminando exatamente quando ela começa
- **Então** o pedido é aceito (decisão provisória L01)

### CA-07: Recursos ou intervalos distintos não conflitam

- **Dado** que existe uma reserva ativa
- **Quando** o cliente pede o mesmo intervalo em **outro** recurso, ou o mesmo recurso em um intervalo separado por folga
- **Então** o pedido é aceito

### CA-08: Só reservas ativas ocupam o horário

- **Dado** uma reserva existente em um intervalo
- **Quando** o cliente pede o mesmo recurso em um intervalo sobreposto
- **Então** o pedido é recusado se a reserva existente está `SOLICITADA`, `APROVADA` ou `EM_USO`, e aceito se está `CANCELADA` ou `REJEITADA` (decisão provisória L02)

### CA-09: A reserva alterada não conflita consigo mesma

- **Dado** uma reserva existente de um recurso
- **Quando** o cliente a altera para um intervalo que só se sobrepõe ao intervalo antigo dela
- **Então** a alteração é aceita

## 6. Casos de teste

Todo critério de aceite precisa de pelo menos um caso. Use classes de equivalência e valor limite.

Os casos da regra (TC-01 a TC-22) são testes **unitários** da regra de reserva, com o `Clock` injetado (ADR 0008). Cada um cita entre parênteses o caso do [plano TDD](../testes/plano-tdd.md). Os três últimos são de integração e só conferem o contrato HTTP.

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 (RN-01-C01) | CA-01 | Classe válida | Início 10:00 e término 11:00 | Reserva aceita | Unitário |
| TC-02 (RN-01-C02) | CA-01 | Valor limite | Término um minuto depois do início | Reserva aceita | Unitário |
| TC-03 (RN-01-C03) | CA-02 | Valor limite | Término igual ao início | Recusa por intervalo inválido | Unitário |
| TC-04 (RN-01-C04) | CA-02 | Classe inválida | Término antes do início | Recusa por intervalo inválido | Unitário |
| TC-05 (RN-01-C05) | CA-03 | Classe inválida | Início ausente | Recusa por dado ausente | Unitário |
| TC-06 (RN-01-C06) | CA-03 | Classe inválida | Término ausente | Recusa por dado ausente | Unitário |
| TC-07 (RN-02-C01) | CA-07 | Classe válida | Sala livre no intervalo pedido | Reserva aceita | Unitário |
| TC-08 (RN-02-C02) | CA-04 | Classe inválida | Nova reserva começa dentro de uma existente e termina depois (sala) | Recusa por sobreposição | Unitário |
| TC-09 (RN-02-C03) | CA-04 | Classe inválida | Nova reserva começa antes e termina dentro de uma existente (sala) | Recusa por sobreposição | Unitário |
| TC-10 (RN-02-C04) | CA-04 | Classe inválida | Nova reserva contida em uma existente (sala) | Recusa por sobreposição | Unitário |
| TC-11 (RN-02-C05) | CA-04 | Classe inválida | Nova reserva contém uma existente (sala) | Recusa por sobreposição | Unitário |
| TC-12 (RN-02-C06) | CA-04 | Classe inválida | Nova reserva idêntica a uma existente (sala) | Recusa por sobreposição | Unitário |
| TC-13 (RN-02-C07) | CA-04 | Classe inválida | Sobreposição parcial no mesmo material | Recusa por sobreposição | Unitário |
| TC-14 (RN-02-C08) | CA-05 | Classe inválida | Sobreposição parcial na agenda do mesmo professor | Recusa por sobreposição | Unitário |
| TC-15 (RN-02-C09) | CA-05 | Classe inválida | Sala livre, mas o professor já está ocupado no intervalo | Recusa por sobreposição | Unitário |
| TC-16 (RN-02-C10) | CA-06 | Valor limite | Nova reserva termina exatamente quando outra começa (mesmo recurso) | Reserva aceita | Unitário |
| TC-17 (RN-02-C11) | CA-06 | Valor limite | Nova reserva começa exatamente quando outra termina (mesmo recurso) | Reserva aceita | Unitário |
| TC-18 (RN-02-C12) | CA-07 | Classe válida | Mesmo intervalo em duas salas diferentes | Reserva aceita | Unitário |
| TC-19 (RN-02-C13) | CA-07 | Classe válida | Intervalos separados na mesma sala, com folga entre eles | Reserva aceita | Unitário |
| TC-20 (RN-02-C14) | CA-09 | Classe válida | Alterar uma reserva para um intervalo que só se sobrepõe a ela mesma | Alteração aceita | Unitário |
| TC-21 (RN-02-C15) | CA-08 | Classe equivalente | Intervalo ocupado por reserva CANCELADA ou REJEITADA | Reserva aceita | Unitário |
| TC-22 (RN-02-C16) | CA-08 | Classe equivalente | Intervalo ocupado por reserva SOLICITADA, APROVADA ou EM_USO | Recusa por sobreposição | Unitário |
| TC-23 | CA-02 | Classe inválida | `POST /api/reservas` com término igual ao início | 400 `ProblemDetail`, `erros` com `campo = termino` | Integração |
| TC-24 | CA-04 | Classe inválida | `POST /api/reservas` sobreposta a uma reserva existente | 409 `ProblemDetail` em linguagem de negócio | Integração |
| TC-25 | CA-01 | Classe válida | `POST /api/reservas` com intervalo livre | 201 com a reserva no estado `SOLICITADA` | Integração |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| O professor decide L01 ou L02 de outro jeito | Alto | Casos marcados como provisórios no plano; TC-16, TC-17, TC-21 e TC-22 são revistos antes da T-23 |
| Erro de um minuto na fronteira (`<` no lugar de `<=`) | Alto | TC-02, TC-03, TC-16 e TC-17 testam as fronteiras dos dois lados |
| A verificação de sobreposição é esquecida na alteração | Médio | TC-20 cobre a alteração; o CA-09 existe só para isso |
| Duas solicitações simultâneas passam pela verificação ao mesmo tempo | Alto | Fora desta spec: o teste de concorrência (RN-03) é da semana 42; a verificação desta spec é sequencial |
| Reserva no passado (Q-01 do plano) | Médio | Não tratada aqui; se o professor exigir recusa, vira um critério novo antes da T-23 |
| Fuso horário diferente entre pedido e `Clock` | Baixo | Todos os instantes em UTC (ADR 0008) |

## 8. Mini-plano de implementação (ciclos TDD)

Na T-20 as assinaturas mínimas devolvem um resultado neutro (que não é aceite nem recusa), para que cada teste falhe por asserção e não por compilação. Cada ciclo é um teste vermelho (T-20, commit `test(red)`), depois o código mínimo (T-23, commit `feat(green)`), depois a refatoração com tudo verde.

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-01 e TC-02: intervalo válido é aceito | Regra de reserva que aceita o intervalo | Extrair o intervalo para um objeto próprio |
| 2 | TC-03 e TC-04: término igual ou anterior ao início é recusado | Validação `termino > inicio` | Mover a validação para dentro do intervalo |
| 3 | TC-05 e TC-06: início ou término ausente | Recusa de dado ausente | Unificar as recusas de intervalo |
| 4 | TC-08 a TC-13: sobreposição no mesmo recurso (sala e material) | Comparação dos intervalos no mesmo recurso | Extrair o método de sobreposição |
| 5 | TC-14 e TC-15: sobreposição na agenda do professor | A verificação passa a cobrir também o professor | Tratar os três tipos de recurso do mesmo jeito |
| 6 | TC-16 e TC-17: reservas encostadas | Fronteira fechada no início e aberta no término | Nomear a regra da fronteira |
| 7 | TC-07, TC-18 e TC-19: recursos e intervalos distintos | Comparar só o mesmo recurso e só intervalos que se sobrepõem | Remover duplicação entre as verificações |
| 8 | TC-21 e TC-22: estados que ocupam o horário | Filtrar as reservas por estado | Extrair o conjunto de estados ativos |
| 9 | TC-20: alteração ignora a própria reserva | Excluir o próprio id da verificação | Reaproveitar a verificação na criação e na alteração |
| 10 | TC-23 a TC-25: contrato HTTP | Mapear os erros para 400 e 409 no formato da T-22 | Consolidar a tradução dos erros |

## 9. Revisão por pares

> Preenchida pelo revisor, com commit próprio. Nunca pelo autor.

- [ ] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [ ] As regras citam RNs que existem no PRD.
- [ ] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [ ] Os casos de teste incluem valores limite e entradas inválidas.
- [ ] O contrato da API cobre os erros (400, 401, 403, 409).
- [ ] Não há conflito com outras specs nem com os ADRs.

**Comentários:** _preencher_

**Parecer:** _preencher_ (Aprovada / Ajustes necessários) · @_preencher_ · AAAA-MM-DD

## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** _preencher_
- **Resultado:** _preencher_

## 11. Grill-with-docs

> Obrigatório só para regra crítica. Nas demais, escreva "Não se aplica".

| # | Pergunta | Resposta / decisão | Documento afetado |
|---|---|---|---|
| 1 | _preencher_ | _preencher_ | _preencher_ |

**Resultado:** _preencher_ (Liberada / Pendências) · AAAA-MM-DD
