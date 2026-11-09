# Plano TDD: inventário das regras de negócio

> Task **T-19** · Autoria: Renata (@renatarsilva) · Revisão: Samuel (@Samwel-Merces)
>
> Este documento lista cada regra de negócio do [PRD](../prd.md) com os casos de teste e o resultado esperado. **Não contém código nem testes.** Os testes red nascem a partir dele (T-20 e T-21).

## 1. Fontes

Em ordem de prioridade:

1. [`prd.md`](../prd.md): regras RN-01 a RN-09 (oficiais) e RN-13 a RN-17 (regras do time), seção 4.4, lacunas (4.8) e decisões pendentes (4.9).
2. [`arquitetura.md`](../arquitetura.md): suposições S-01 a S-06, decisões D-01 e D-02, sequências (2.4) e restrições do banco (3.2).
3. [`atam/utility-tree-inicial.md`](../atam/utility-tree-inicial.md): casos CT-01 (sobreposição), CT-02 (simultaneidade) e CT-03 (notificação), que este plano referencia em vez de repetir.
4. Os [ADRs](../adr/) 0004 a 0009.

A especificação oficial vale sobre qualquer outro documento. Nenhuma regra foi inventada: o que não está nas fontes aparece na seção 4, como ambiguidade. As regras RN-13 a RN-17 são decisões do time, registradas no PRD, e valem como regra desde que entraram nele.

## 2. Como ler o plano

Cada regra tem casos de **cinco tipos**, cada um com um ID `RN-xx-Tn`:

| Tipo | Letra | O que verifica |
|---|---|---|
| Caminho feliz | F | O comportamento normal, aceito |
| Valor-limite | L | A fronteira exata da regra |
| Entrada inválida | I | Dados malformados ou fora do domínio |
| Conflito | C | Choque com outra reserva, recurso ou usuário |
| Estado proibido | E | Operação não permitida no estado atual |

Um tipo que não se aplica a uma regra aparece como "não se aplica", com o motivo. O resultado esperado de cada caso é escrito de forma que um teste possa verificá-lo.

## 3. Inventário das regras

| Regra | Texto | RF | Quando ganha teste |
|---|---|---|---|
| RN-01 | O término da reserva deve ser posterior ao início | RF-04 | Semana 36/37 |
| RN-02 | Reservas do mesmo recurso (sala, material ou professor) não podem se sobrepor | RF-05 | Semana 36/37 |
| RN-03 | Duas solicitações simultâneas devem produzir uma única reserva aceita | RF-06 | Semana 42 |
| RN-04 | Recurso em manutenção não pode ser reservado | RF-08 | Semana 41 (BDD) |
| RN-05 | Fluxo principal: `SOLICITADA → APROVADA → EM_USO → CONCLUIDA` | RF-04, RF-07 | Semana 36/37 |
| RN-06 | Estados alternativos: `REJEITADA`, `CANCELADA` e `NAO_COMPARECEU` | RF-04, RF-07 | Semana 36/37 |
| RN-07 | Somente o perfil Responsável aprova recursos restritos | RF-07, RF-01 | Semana 41 (aprovação) e 44 (autorização) |
| RN-08 | Reservas iniciadas não podem ser apagadas | RF-04, RF-10 | Semana 36/37 |
| RN-09 | Toda mudança de estado gera auditoria | RF-10 | Semana 43 |
| RN-10 (proposta) | Janela de funcionamento: segunda a sexta, 07:00 às 22:45; sábado, 08:00 às 14:00; domingo e feriados fechados | RF-04 | Semana 36/37 |
| RN-11 (proposta) | Último horário: o início da reserva não passa de 22:00 (segunda a sexta) nem de 13:00 (sábado) | RF-04 | Semana 36/37 |
| RN-12 (proposta) | Antecedência: a reserva vale para hoje ou para qualquer dia futuro, nunca no passado, desde que o recurso esteja livre. A antecedência mínima de 1 h é a RN-15 | RF-04 | Semana 36/37 |
| RN-13 | Cancelamento libera o recurso: o dono cancela antes do início (`CANCELADA`); depois do início, com check-in encerra (`CONCLUIDA`) e sem check-in vira `NAO_COMPARECEU`; o resto do horário volta a ficar livre | RF-04, RF-07 | Semana 36/37 |
| RN-14 | Prazos de aprovação e de alteração: 24 h antes do início; 1 h antes se o pedido ou a alteração tem menos de 24 h de antecedência; limites inclusivos | RF-04, RF-07 | Semana 36/37 |
| RN-15 | Antecedência mínima de 1 h do início, no cadastro e na alteração | RF-04 | Semana 36/37 |
| RN-16 | O nome da sala segue o padrão `sala-NN` | RF-02 | Semana 36 |
| RN-17 | O nome do professor segue o padrão `primeironome-ultimonome-matricula`, matrícula de 5 dígitos | RF-02 | Semana 36 |

**RN-10 a RN-12 são propostas do time**: não estão na especificação nem no PRD, e precisam ser registradas no PRD. **RN-13 a RN-17 são regras do time já registradas no PRD** (não vêm da especificação). Os casos de cadastro de recurso (sala, professor e material) pertencem ao **RF-02**, que não tem RN própria: eles aparecem nas regras mais próximas (RN-01 para entrada inválida, RN-02 para conflito, RN-08 para exclusão) e nas RN-16 e RN-17.

Os casos de cada regra entram nas seções abaixo, uma regra por vez.

### RN-01 · O término da reserva deve ser posterior ao início

Fonte: PRD, seção 4.4 · RF-04. A comparação é entre **data e hora**, não só entre horários. A regra vale na **criação** e na **alteração** de uma reserva.

| Caso | Tipo | Situação | Resultado esperado |
|---|---|---|---|
| RN-01-T1 | Feliz | Início às 10:00 e término às 11:00, no mesmo dia | Aceita, e a reserva é gravada |
| RN-01-T2 | Feliz | Início às 10:00 e término às 11:00 de um dia futuro (data e hora comparadas, não só o horário) | Aceita |
| RN-01-T3 | Limite | Início às 10:00 e término às 10:01 (a menor duração positiva) | Aceita |
| RN-01-T4 | Limite | Início às 10:00 e término às 10:00 (mesmo instante) | Recusada, e nada é gravado |
| RN-01-T5 | Inválida | Início às 10:00 e término às 09:00 (término antes do início) | Recusada, e nada é gravado |
| RN-01-T6 | Inválida | Início ausente | Recusada com mensagem de erro compreensível, e nada é gravado |
| RN-01-T7 | Inválida | Término ausente | Recusada com mensagem de erro compreensível, e nada é gravado |
| RN-01-T8 | Inválida | Alterar uma reserva existente para um término anterior ao início | Recusada, e a reserva mantém o horário original |
| RN-01-T9 | Inválida | Pedido com um recurso que não existe (sala-999) | Recusada como não encontrada, e nada é gravado |
| RN-01-T10 | Inválida | Pedido com a lista de recursos vazia | Recusada, e o erro aponta o campo `recursos` |
| RN-01-T11 | Inválida | (RF-02, cadastro de recurso) capacidade da sala `30.5` (decimal) | Recusada com `400`, `erros` com `campo = capacidade` e motivo "valor inválido"; o valor não vira `30` |
| RN-01-T12 | Inválida | (RF-02) capacidade da sala `"30"` (texto) | Recusada com `400`, motivo "valor inválido"; o texto não vira número |
| RN-01-T13 | Inválida | (RF-02) `restrito` ausente no `PUT` de uma sala, de um professor ou de um material | Recusada com `400`, `erros` com `campo = restrito`; no `POST`, ausente vale `false` |
| RN-01-T14 | Feliz | (RF-02) tipo da sala ou do material com espaços nas pontas e repetidos no meio, como `"  Laboratório  1 "` | Aceito e gravado limpo (`"Laboratório 1"`) |
| RN-01-T15 | Inválida | (RF-02) `PUT` de um professor sem `competencias` | Recusada com `400`, `erros` com `campo = competencias`; lista vazia explícita é aceita |
| (conflito) | Conflito | Não se aplica: a regra compara os dois horários da própria reserva, sem outra reserva envolvida | — |
| (estado) | Estado proibido | Não se aplica: a regra não depende do estado da reserva | — |

Premissas de teste: a fonte não exige duração mínima geral, então qualquer duração positiva é válida no meio do dia (RN-01-T3); e toda reserva fica dentro de um único dia, conforme a RN-10.

### RN-02 · Reservas do mesmo recurso não podem se sobrepor

Fonte: PRD, seção 4.4 · RF-05. As formas de sobreposição em sala, material e professor (V01 a V21) estão no [CT-01](../atam/utility-tree-inicial.md) e não se repetem aqui. Premissas: S-01 (reservas encostadas convivem) e S-02 (quais estados ocupam o horário).

Base dos casos: sala-102, terça, 10:00 às 11:00, reserva `APROVADA` do usuário A, salvo indicação.

| Caso | Tipo | Situação | Resultado esperado |
|---|---|---|---|
| RN-02-T1 | Conflito | A estende a sua reserva para 10:00 às 12:00, mas B tem a sala-102 das 11:00 às 12:00 | Recusada, e a reserva de A continua das 10:00 às 11:00 |
| RN-02-T2 | Feliz | A estende a sua reserva para 10:00 às 12:00, com a sala livre depois | Aceita (a reserva não conflita com ela mesma) |
| RN-02-T3 | Limite | A estende de 10:00 às 10:30 para 10:00 às 11:00, e B tem 11:00 às 12:00 | Aceita (encostada, S-01) |
| RN-02-T4 | Conflito | A muda a reserva para 13:00 às 14:00, horário já ocupado por C | Recusada, e a reserva de A fica no horário original |
| RN-02-T5 | Conflito | Pedido de sala-102, projetor e professor X; só o projetor está ocupado | Recusada por inteiro; a sala e o professor X continuam livres |
| RN-02-T6 | Conflito | Pedido em que a sala e o professor conflitam ao mesmo tempo | Recusada, e nada é gravado |
| RN-02-T7 | Estado | B pede 10:30 às 11:30, e a reserva existente está `SOLICITADA` | Recusada (ocupa o horário) |
| RN-02-T8 | Estado | Idem, com a existente `EM_USO` | Recusada (ocupa o horário) |
| RN-02-T9 | Estado | Idem, com a existente `CANCELADA` | Aceita (não ocupa) |
| RN-02-T10 | Estado | Idem, com a existente `REJEITADA` | Aceita (não ocupa) |
| RN-02-T11 | Estado | Idem, com a existente `NAO_COMPARECEU` | Aceita (não ocupa) |
| RN-02-T14 | Estado | Idem, com a existente `CONCLUIDA` (S-02) | Aceita (não ocupa) |
| RN-02-T15 | Estado | B pede 10:30 às 11:00, e a existente é `APROVADA` sem check-in | Recusada (a `APROVADA` ocupa o horário inteiro, sem tolerância de atraso) |
| RN-02-T16 | Conflito | (RF-02, cadastro de recurso) cadastrar o material `Projetor Epson` e depois `projetor  epson` (minúsculas e dois espaços) | `409` (nome único sem diferenciar maiúsculas nem espaços extras); a mensagem cita o nome |
| RN-02-T17 | Conflito | (RF-02) cadastrar o patrimônio `PAT-1` e depois `pat-1` | `409`; a mensagem cita o patrimônio |
| RN-02-T18 | Conflito | (RF-02) cadastrar a sala `sala-01` duas vezes | `409` |
| RN-02-T19 | Conflito | (RF-02) dois cadastros do mesmo nome ao mesmo tempo | Um `201` e um `409` com a mensagem fixa "Esta operação conflita com outro registro" (a recusa vem do banco) |
| RN-02-T20 | Conflito | (RF-02) professores `renata-silva-12345` e `maria-souza-12345` (nomes diferentes, mesma matrícula) | O segundo recebe `409`; a matrícula é única |
| RN-02-T12 | Feliz | B pede a sala-101, terça das 10:00 às 11:00, com a sala-102 reservada | Aceita (recursos diferentes) |
| RN-02-T13 | Feliz | B pede a sala-102 em outro dia, das 10:00 às 11:00 | Aceita |
| (inválida) | Inválida | Não se aplica: os dados malformados são da RN-01 | — |

### RN-03 · Duas solicitações simultâneas produzem uma única reserva aceita

Fonte: PRD, seção 4.4 · RF-06. Premissas: S-02 (quais estados ocupam o horário) e L03 (a forma da recusa não está definida: o teste só verifica que a reserva recusada **não é gravada**). O caso de 20 solicitações é o [CT-02](../atam/utility-tree-inicial.md); os demais são menores e não se repetem lá.

Base dos casos: sala-102, terça, salvo indicação.

| Caso | Tipo | Situação | Resultado esperado |
|---|---|---|---|
| RN-03-T1 | Conflito | 20 solicitações simultâneas para a mesma sala e o mesmo intervalo (CT-02) | Exatamente 1 aceita, 19 recusadas e 1 reserva ativa |
| RN-03-T2 | Limite | 2 solicitações simultâneas, mesma sala e mesmo intervalo (a menor disputa) | 1 aceita, 1 recusada e 1 reserva ativa |
| RN-03-T3 | Conflito | 2 simultâneas com sobreposição parcial (10:00 às 11:00 e 10:30 às 11:30) | 1 aceita, 1 recusada |
| RN-03-T4 | Feliz | 2 simultâneas na mesma sala, em intervalos encostados (10:00 às 11:00 e 11:00 às 12:00) | As duas aceitas |
| RN-03-T5 | Feliz | 2 simultâneas, em salas diferentes e no mesmo horário | As duas aceitas |
| RN-03-T6 | Conflito | O mesmo usuário envia o mesmo pedido 2 vezes ao mesmo tempo (duplo clique) | 1 reserva criada, e a repetição é recusada |
| RN-03-T7 | Conflito | A pede sala-102 e projetor, e B pede sala-103 e o mesmo projetor, ao mesmo tempo | 1 aceita, e a recusada não deixa a sala-103 gravada |
| RN-03-T8 | Estável | O caso RN-03-T2 repetido 5 vezes seguidas | O mesmo resultado nas 5 (premissa: 5 repetições é métrica candidata, MC-03) |
| RN-03-T9 | Feliz | Depois da disputa, quem foi recusado pede outro horário livre | Aceita |
| (inválida e estado) | — | Não se aplicam: a regra é só sobre simultaneidade | — |

### RN-04 · Recurso em manutenção não pode ser reservado

Fonte: PRD, seção 4.4 · RF-08. Premissas: S-01 (períodos encostados convivem), S-04 (bloqueio e manutenção são o mesmo mecanismo) e S-05 (criar manutenção não cancela reservas existentes). O caso concorrente é o CEN-04 da [arquitetura](../arquitetura.md).

Base dos casos: sala-102 em manutenção na terça, das 10:00 às 12:00, salvo indicação.

| Caso | Tipo | Situação | Resultado esperado |
|---|---|---|---|
| RN-04-T1 | Conflito | Reserva da sala-102 das 10:30 às 11:30, dentro da manutenção | Recusada, e nada é gravado |
| RN-04-T2 | Conflito | Reserva das 09:30 às 10:30, que invade o início da manutenção | Recusada, e nada é gravado |
| RN-04-T3 | Limite | Reserva das 09:00 às 10:00 (termina quando a manutenção começa) | Aceita (encostada, S-01) |
| RN-04-T4 | Limite | Reserva das 12:00 às 13:00 (começa quando a manutenção termina) | Aceita |
| RN-04-T5 | Feliz | Reserva da sala-101, sem manutenção, no mesmo horário | Aceita |
| RN-04-T6 | Feliz | Reserva da sala-102 em outro dia, sem manutenção | Aceita |
| RN-04-T7 | Conflito | Reserva de um material em manutenção (por exemplo, o projetor) no período | Recusada, e nada é gravado |
| RN-04-T8 | Conflito | Pedido de sala-102 livre e projetor em manutenção | Recusada por inteiro, e a sala não fica gravada |
| RN-04-T9 | Estado | A sala-102 já tem reserva `APROVADA` das 10:30 às 11:30, e a manutenção é criada depois | A manutenção é criada; a reserva não é cancelada, continua `APROVADA` e aparece na lista de reservas afetadas (S-05) |
| RN-04-T10 | Concorrência | Manutenção e reserva da sala-102 criadas ao mesmo tempo (CEN-04) | Nenhuma reserva é gravada depois de a manutenção ser confirmada |
| RN-04-T11 | Feliz | A manutenção é encerrada, e depois alguém reserva a sala-102 no mesmo período | Aceita (a sala voltou a ficar livre) |
| (inválida) | Inválida | Não se aplica: os dados malformados são da RN-01 | — |

Quem pode criar manutenção (só o Administrador) é autorização e fica para a matriz da T-48.

### RN-05 e RN-06 · Estados da reserva

Fonte: PRD, seção 4.4 · RF-04, RF-07. O fluxo principal é oficial; os caminhos alternativos, o check-in e os prazos são decisões do time (RN-13 e RN-14, spec do ciclo de vida na T-21). A ordem dos erros nos endpoints é 401 → 403 por perfil → 404 → 403 por dono → 400 → 409. Cada transição permitida gera auditoria (RN-09), tratada em outra seção.

**Fluxo principal (RN-05)**

| Caso | Tipo | Situação | Resultado esperado |
|---|---|---|---|
| RN-05-T1 | Feliz | `SOLICITADA` aprovada pelo Responsável | Estado `APROVADA` |
| RN-05-T2 | Feliz | `APROVADA` recebe o check-in na sala, em qualquer instante do horário | Estado `EM_USO` |
| RN-05-T3 | Feliz | `EM_USO` é encerrada (pelo job no instante do fim, ou pelo dono antes do fim) | Estado `CONCLUIDA` |
| RN-05-T4 | Estado proibido | `SOLICITADA` direto para `EM_USO` (pula a aprovação) | Recusada, e continua `SOLICITADA` |
| RN-05-T5 | Estado proibido | `SOLICITADA` direto para `CONCLUIDA` | Recusada, e o estado não muda |
| RN-05-T6 | Estado proibido | `APROVADA` direto para `CONCLUIDA` (pula o uso) | Recusada, e o estado não muda |
| RN-05-T7 | Estado proibido | Retrocesso: `APROVADA→SOLICITADA`, `EM_USO→APROVADA`, `CONCLUIDA→EM_USO` | Todas recusadas |
| RN-05-T8 | Estado proibido | `PATCH` com destino `EM_USO`, pedido por qualquer perfil (inclusive Responsável e Administrador) | Recusado com `403`, e o estado não muda; o check-in é o endpoint próprio |
| RN-05-T9 | Feliz | O dono faz o check-in (`POST /api/reservas/{id}/checkin`) às 10:30, numa reserva `APROVADA` das 10:00 às 11:00 | Estado `EM_USO` |
| RN-05-T10 | Limite | Check-in às 09:59, antes do início | Recusado com `409`, e continua `APROVADA` |
| RN-05-T11 | Limite | Check-in às 10:00 em ponto | Aceito, estado `EM_USO` |
| RN-05-T12 | Limite | Check-in às 10:59 | Aceito, estado `EM_USO` |
| RN-05-T13 | Limite | Check-in às 11:00 em ponto, no instante do fim (`[início, fim)`) | Recusado com `409` |
| RN-05-T14 | Conflito | Check-in por quem não é o dono | `403`, e a reserva não muda |
| RN-05-T15 | Inválida | Check-in de uma reserva que não existe | `404` |
| RN-05-T16 | Estado proibido | Check-in de uma reserva `SOLICITADA` (ainda não aprovada) | `409`, e o estado não muda |

**Caminhos alternativos (RN-06)**

| Caso | Tipo | Situação | Resultado esperado |
|---|---|---|---|
| RN-06-T1 | Feliz | `SOLICITADA` rejeitada pelo Responsável (NC-04) | Estado `REJEITADA` |
| RN-06-T2 | Feliz | `SOLICITADA` cancelada pelo dono | Estado `CANCELADA` |
| RN-06-T3 | Feliz | `APROVADA` cancelada pelo dono, antes do início | Estado `CANCELADA` |
| RN-06-T4 | Feliz | `APROVADA` que chega ao fim do horário sem nenhum check-in, pelo job a cada minuto (S-06, T-60) | Estado `NAO_COMPARECEU`; o horário fica livre |
| RN-06-T5 | Estado proibido | `EM_USO` para `CANCELADA` por transição direta (o fim antecipado é o cancelamento do dono, que resulta em `CONCLUIDA`, RN-13) | Recusada, e continua `EM_USO` |
| RN-06-T6 | Estado proibido | `EM_USO` para `REJEITADA` ou `NAO_COMPARECEU` | Recusadas |
| RN-06-T7 | Estado proibido | Qualquer saída de `CONCLUIDA`, `REJEITADA`, `CANCELADA` ou `NAO_COMPARECEU` (estados finais) | Recusada, e o estado não muda |
| RN-06-T8 | Inválida | Mudar para um estado que não existe (por exemplo, `"ABC"`) | Recusada, e o estado não muda |
| RN-06-T9 | Feliz | `SOLICITADA` pedida com menos de 24 h de antecedência, sem aprovação até 1 h antes do início (RN-14) | Estado `CANCELADA` pelo job, com o motivo "não aprovada a tempo" na auditoria |
| RN-06-T10 | Feliz | Depois do T9, outro usuário pede o mesmo recurso e horário, com início igual ou depois de agora | Aceita (`CANCELADA` não ocupa o horário) |
| RN-06-T11 | Estado proibido | `APROVADA` sem check-in marcada `NAO_COMPARECEU` às 10:35, antes do fim (reserva 10:00-11:00) | Recusada, e o estado não muda |
| RN-06-T12 | Feliz | `SOLICITADA` pedida com 24 h ou mais de antecedência, sem aprovação depois de 24 h antes do início (RN-14) | Estado `CANCELADA` pelo job |
| RN-06-T13 | Feliz | Dono altera uma reserva `APROVADA` para um horário livre | Estado `SOLICITADA`, a aguardar nova aprovação; horário antigo livre e novo ocupado |
| RN-06-T14 | Conflito | Um Solicitante envia `PATCH` de estado com `"ABC"` | `403` (o perfil vem antes da validação) |
| RN-06-T15 | Inválida | O Responsável envia `PATCH` de estado com `"ABC"` | `400`, `erros` com `campo = estado`; o estado não muda |
| RN-06-T16 | Inválida | O Responsável envia `PATCH` de uma reserva que não existe | `404` |
| RN-06-T17 | Conflito | Um Solicitante envia `PATCH` de uma reserva que não existe | `403` (o perfil vem antes da existência) |

O cancelamento por falta de aprovação (RN-06-T9 e T12) é feito pelo job (T-60), e não pelo dono. O prazo (24 h antes, ou 1 h antes se o pedido tem menos de 24 h de antecedência) vem da RN-14.

### RN-07 · Somente o Responsável aprova recursos restritos

Fonte: PRD, seção 4.4 · RF-07, RF-01. Premissas: S-03 (o recurso tem o indicador `restrito`), DP-06 (se "solicitação especial" e "recurso restrito" são a mesma coisa) e L04 (respondida pelo time: toda reserva passa por `SOLICITADA → APROVADA`). **Decisão do time: só o Responsável aprova e rejeita, em qualquer reserva, restrita ou não**; o Administrador e o Solicitante recebem `403`. A matriz completa de perfil por operação é da T-48 e da T-49.

Base dos casos: a sala-201 (recurso restrito) tem uma reserva `SOLICITADA` do usuário A.

| Caso | Tipo | Situação | Resultado esperado |
|---|---|---|---|
| RN-07-T1 | Feliz | O Responsável aprova a reserva | Estado `APROVADA` |
| RN-07-T2 | Conflito | O dono (Solicitante) tenta aprovar a própria reserva | Recusada (acesso negado), e continua `SOLICITADA` |
| RN-07-T3 | Conflito | O Administrador tenta aprovar (decisão do time: só o Responsável aprova) | Recusada com `403`, e continua `SOLICITADA` |
| RN-07-T4 | Conflito | Alguém sem login tenta aprovar | Recusada (não autenticado), e o estado não muda |
| RN-07-T5 | Conflito | O Solicitante tenta rejeitar a reserva de outro usuário | Recusada, e continua `SOLICITADA` |
| RN-07-T6 | Estado | O Responsável tenta aprovar uma reserva já `APROVADA`, `CANCELADA` ou `REJEITADA` | Recusada, e o estado não muda |
| RN-07-T7 | Feliz | Reserva de um recurso não restrito | Também fica `SOLICITADA` e só o Responsável a aprova (L04, decisão do time) |
| RN-07-T8 | Conflito | O Responsável clica em aprovar 2 vezes ao mesmo tempo | 1 aprovação e 1 registro de auditoria |
| RN-07-T9 | Concorrência | O Responsável aprova enquanto o dono cancela, ao mesmo tempo | Um só resultado final (`APROVADA` ou `CANCELADA`), nunca os dois, com 1 registro de auditoria |
| RN-07-T10 | Inválida | Aprovar uma reserva que não existe | Recusada, sem alterar nada |
| RN-07-T11 | Feliz | Depois da aprovação, a auditoria registra o Responsável que aprovou | O registro traz o usuário, o estado anterior e o novo (RN-09) |
| RN-07-T12 | Conflito | O Administrador tenta aprovar uma reserva de recurso não restrito | `403`, e continua `SOLICITADA` |
| RN-07-T13 | Conflito | O Administrador tenta rejeitar uma reserva | `403`, e continua `SOLICITADA` |
| RN-07-T14 | Conflito | O Solicitante envia `PATCH` com `APROVADA` | `403` antes de qualquer outra verificação |
| RN-07-T15 | Conflito | O Responsável pede, por `PATCH`, um destino que é do sistema (`EM_USO`, `CONCLUIDA`, `CANCELADA`, `NAO_COMPARECEU` ou `SOLICITADA`) | `403`, e o estado não muda |

### RN-08 · Reservas iniciadas não podem ser apagadas

Fonte: PRD, seção 4.4 · RF-04, RF-10. Premissas: L11 (cancelar não é apagar) e a restrição 5 da [arquitetura](../arquitetura.md) (nenhuma reserva é apagada, nem antes do início). Base: reserva do usuário A, sala-102, terça, das 10:00 às 11:00.

| Caso | Tipo | Situação | Resultado esperado |
|---|---|---|---|
| RN-08-T1 | Estado proibido | Apagar uma reserva `EM_USO` | Recusada, e a reserva continua no sistema |
| RN-08-T2 | Estado proibido | Apagar uma reserva `CONCLUIDA` | Recusada, e a reserva continua no sistema |
| RN-08-T3 | Limite | Apagar às 10:00 em ponto, no instante do início | Recusada (o sistema não apaga reservas, nem antes, nem depois do início) |
| RN-08-T4 | Limite | Apagar às 09:59, antes do início (premissa: o sistema não apaga reservas, só cancela) | Recusada; o caminho é cancelar (RN-13) |
| RN-08-T5 | Estado proibido | Apagar uma reserva `CANCELADA`, `REJEITADA` ou `NAO_COMPARECEU` | Recusada, e a reserva continua no sistema |
| RN-08-T6 | Feliz | A cancela a reserva antes do início | A reserva continua consultável como `CANCELADA`, com o histórico |
| RN-08-T7 | Conflito | Alguém sem permissão tenta apagar | Recusada, e a reserva continua |
| RN-08-T8 | Inválida | Apagar uma reserva que não existe | Recusada (não encontrada), sem alterar nada |
| RN-08-T9 | Integração | Um `DELETE` direto no banco de uma reserva que tem auditoria | Recusado pela chave estrangeira, e a auditoria e a reserva continuam (arquitetura 3.2) |
| RN-08-T10 | Integração | (RF-02) excluir uma sala, um professor ou um material com reserva, retirada ou período de manutenção | `409` "Este registro está em uso e não pode ser removido" (SQLState 23503 traduzido pela T-22); nada é apagado; testes na T-61 |
| RN-08-T11 | Feliz | (RF-02) excluir um recurso sem nenhum vínculo e cadastrar de novo o mesmo nome | `204`; o recurso e as linhas filhas somem, e o mesmo nome é aceito no novo cadastro |

### RN-09 · Toda mudança de estado gera auditoria

Fonte: PRD, seção 4.4 · RF-10. Premissas: L07 (o conteúdo do registro não está definido: aqui, estado anterior, estado novo, usuário e instante) e a criação da reserva conta como mudança de estado (estado anterior vazio). Alterar o horário não é mudança de estado e fica fora desta regra. Quem consulta a auditoria é a DP-02, da T-40.

| Caso | Tipo | Situação | Resultado esperado |
|---|---|---|---|
| RN-09-T1 | Feliz | Criar uma reserva | 1 registro: estado anterior vazio, novo `SOLICITADA`, usuário e instante |
| RN-09-T2 | Feliz | O Responsável aprova | 1 registro: `SOLICITADA→APROVADA`, com o Responsável como usuário |
| RN-09-T3 | Feliz | Cada transição do fluxo principal (`APROVADA→EM_USO` e `EM_USO→CONCLUIDA`) | 1 registro por transição (teste parametrizado) |
| RN-09-T4 | Feliz | Cada caminho alternativo (`REJEITADA`, `CANCELADA` e `NAO_COMPARECEU`) | 1 registro por transição; no `NAO_COMPARECEU`, o usuário é o sistema (job a cada minuto) |
| RN-09-T5 | Feliz | Cancelamento por falta de aprovação (RN-06-T9) | 1 registro com o motivo "não aprovada a tempo", feito pelo sistema |
| RN-09-T6 | Estado proibido | Uma transição proibida (por exemplo, `SOLICITADA→EM_USO`) | Recusada, e nenhum registro é gravado |
| RN-09-T7 | Conflito | Um pedido recusado por conflito de horário | Nenhuma reserva e nenhum registro (nada foi gravado) |
| RN-09-T8 | Integração | A gravação da auditoria falha | A mudança de estado é desfeita, e a reserva mantém o estado anterior (mesma transação) |
| RN-09-T9 | Feliz | O instante do registro, com o `Clock` fixo | Igual ao instante do `Clock` (ADR-0008) |
| RN-09-T10 | Estado proibido | Alterar ou apagar um registro de auditoria | Recusado (a auditoria só recebe inserções; risco R-05) |
| RN-09-T11 | Concorrência | Aprovar e cancelar a mesma reserva ao mesmo tempo (RN-07-T9) | Uma transição vence e há exatamente 1 registro |
| RN-09-T12 | Integração | A notificação falha depois da mudança de estado | O registro de auditoria permanece (RNF-10) |

### RN-10 · Janela de funcionamento (proposta)

Segunda a sexta, de 07:00 às 22:45. Sábado, de 08:00 às 14:00. Domingo e feriados, fechado. A reserva começa não antes da abertura, termina não depois do fechamento e fica dentro de **um único dia**. Os horários são os de Brasília, e o `Clock` do teste é fixo.

| Caso | Tipo | Situação | Resultado esperado |
|---|---|---|---|
| RN-10-T1 | Feliz | Terça, 10:00 às 11:00 | Aceita |
| RN-10-T2 | Limite | Segunda, 07:00 às 08:00 (início na abertura) | Aceita |
| RN-10-T3 | Limite | Segunda, 06:59 às 08:00 (início antes da abertura) | Recusada, e nada é gravado |
| RN-10-T4 | Limite | Segunda, 22:00 às 22:45 (término no fechamento) | Aceita |
| RN-10-T5 | Limite | Segunda, 22:00 às 22:46 (término depois do fechamento) | Recusada, e nada é gravado |
| RN-10-T6 | Feliz | Sábado, 08:00 às 14:00 | Aceita |
| RN-10-T7 | Limite | Sábado, 07:59 às 09:00 | Recusada, e nada é gravado |
| RN-10-T8 | Limite | Sábado, 10:00 às 14:01 | Recusada, e nada é gravado |
| RN-10-T9 | Estado proibido | Domingo, 10:00 às 11:00 (dia fechado) | Recusada, e nada é gravado |
| RN-10-T10 | Inválida | Início às 22:00 de sexta e término às 08:00 de sábado (atravessa dias) | Recusada, e nada é gravado |
| RN-10-T11 | Estado proibido | Feriado em dia útil, 10:00 às 11:00 (validação pendente, A-03) | Recusada, e nada é gravado |
| (conflito) | Conflito | Não se aplica: a regra não compara com outra reserva | — |

### RN-11 · Último horário (proposta)

O início da reserva não passa do **último início do dia**: 22:00 de segunda a sexta (o último horário, das 22:00 às 22:45, tem 45 minutos) e 13:00 no sábado (o último horário, das 13:00 às 14:00, tem 60 minutos). No meio do dia não há duração mínima.

| Caso | Tipo | Situação | Resultado esperado |
|---|---|---|---|
| RN-11-T1 | Feliz | Segunda, 22:00 às 22:45 (último início) | Aceita |
| RN-11-T2 | Limite | Segunda, 22:01 às 22:45 (um minuto depois do último início) | Recusada, e nada é gravado |
| RN-11-T3 | Limite | Segunda, 22:30 às 22:45 (dentro da janela, mas depois do último início) | Recusada, e nada é gravado |
| RN-11-T4 | Feliz | Sábado, 13:00 às 14:00 (último início do sábado) | Aceita |
| RN-11-T5 | Limite | Sábado, 13:01 às 14:00 | Recusada, e nada é gravado |
| RN-11-T6 | Limite | Segunda, 10:00 às 10:15 (curta, no meio do dia) | Aceita (premissa: a regra só vale para o último início do dia) |
| (inválida, conflito, estado) | — | Não se aplicam: a regra só compara o início com o último início do dia | — |

### RN-12 · Antecedência (proposta; a antecedência mínima é a RN-15)

A reserva vale para hoje ou para qualquer dia futuro, **nunca no passado**, desde que o recurso esteja livre. Os casos usam o `Clock` fixo em segunda, às 10:00.

| Caso | Tipo | Situação | Resultado esperado |
|---|---|---|---|
| RN-12-T1 | Feliz | Hoje, das 11:00 às 12:00 (início depois de agora) | Aceita |
| RN-12-T2 | Feliz | Uma quinta futura, das 14:00 às 15:00, com a sala livre | Aceita |
| RN-12-T3 | Substituído | Hoje, das 10:00 às 11:00 (início exatamente agora). **Substituído pela RN-15**: o caso deixa de valer, porque agora o início precisa de pelo menos 1 h de antecedência | Recusada pela RN-15-T3 |
| RN-12-T4 | Inválida | Hoje, das 08:00 às 09:00 (início já passou) | Recusada, e nada é gravado |
| RN-12-T5 | Inválida | Ontem, das 10:00 às 11:00 | Recusada, e nada é gravado |
| RN-12-T6 | Conflito | Uma quinta futura, em horário já ocupado no mesmo recurso | Recusada pela RN-02, e nada é gravado |
| (estado) | Estado proibido | Não se aplica: a regra compara datas, não estados | — |

### RN-13 · Cancelamento libera o recurso (regra do time, no PRD)

O **dono** da reserva pode cancelá-la. Até o instante do início (inclusive), a reserva vira `CANCELADA` e o horário inteiro volta a ficar livre. Depois do início e antes do fim, o resultado depende do **check-in**: com check-in (`EM_USO`), é um **encerramento antecipado** e a reserva vira `CONCLUIDA`; sem check-in (`APROVADA`), vira `NAO_COMPARECEU`. Nos dois casos o resto do horário volta a ficar livre. O dono altera a reserva até 24 h antes do início (1 h se o pedido tem menos de 24 h de antecedência, RN-14); alterar uma `APROVADA` a devolve a `SOLICITADA`. Depois do fim, não há o que cancelar. A reserva nunca é apagada (RN-08) e toda mudança gera auditoria (RN-09).

Os casos usam o `Clock` fixo em segunda, às 08:00, e uma reserva `APROVADA` do usuário A, na sala-102, das 10:00 às 11:00, salvo indicação.

| Caso | Tipo | Situação | Resultado esperado |
|---|---|---|---|
| RN-13-T1 | Feliz | A cancela às 08:00, antes do início | Estado `CANCELADA`; o horário das 10:00 às 11:00 fica livre; a auditoria registra a mudança |
| RN-13-T2 | Feliz | Depois do cancelamento de T1, o usuário B reserva a sala-102 das 10:00 às 11:00 | Aceita (o conflito deixou de existir) |
| RN-13-T3 | Feliz | A cancela uma reserva ainda `SOLICITADA` (sem aprovação), antes do início | Estado `CANCELADA`; o horário fica livre |
| RN-13-T4 | Limite | A cancela às 09:59, um minuto antes do início | Estado `CANCELADA` |
| RN-13-T5 | Limite | A cancela às 10:00 em ponto, no instante do início (o uso ainda não começou) | Estado `CANCELADA`, e o horário inteiro das 10:00 às 11:00 volta a ficar livre |
| RN-13-T6 | Feliz | Reserva das 10:00 às 12:00, `EM_USO` (com check-in); A encerra às 10:40 | Estado `CONCLUIDA`; a reserva continua no histórico |
| RN-13-T7 | Feliz | Depois de T6, B reserva a sala-102 das 11:00 às 12:00 | Aceita (o resto do horário foi liberado) |
| RN-13-T8 | Estado proibido | A tenta cancelar às 11:30, depois do fim | Recusada, e o estado não muda |
| RN-13-T9 | Estado proibido | A cancela uma reserva que já está `CANCELADA` | Recusada, e o estado não muda |
| RN-13-T10 | Estado proibido | A cancela uma reserva `REJEITADA` ou `NAO_COMPARECEU` | Recusada, e o estado não muda |
| RN-13-T11 | Conflito | B, que não é o dono, tenta cancelar a reserva de A | Recusada, e a reserva fica intacta (premissa: só o dono cancela; o caso do Administrador entra na matriz de autorização, T-48) |
| RN-13-T12 | Inválida | Cancelar uma reserva que não existe | Recusada, sem alterar nada |
| RN-13-T13 | Feliz | Em T1, T4 e T6, o registro de auditoria traz o estado anterior, o novo, o usuário e o instante | Um registro por mudança (RN-09) |
| RN-13-T14 | Limite | A cancela uma reserva `EM_USO` às 10:01, um minuto depois do início (vale qualquer instante após 10:00:00) | Encerramento antecipado: estado `CONCLUIDA`, e o resto do horário fica livre |
| RN-13-T15 | Feliz | A cancela às 10:05 uma reserva `APROVADA` sem check-in | Estado `NAO_COMPARECEU`; o resto do horário fica livre |
| RN-13-T16 | Limite | A tenta cancelar às 11:00 em ponto, no instante do fim | Recusada (`[início, fim)`, S-01) |
| RN-13-T17 | Conflito | B, que não é o dono, tenta alterar a reserva de A | Recusada, e a reserva fica intacta |
| RN-13-T18 | Limite | Pedido feito com mais de 24 h de antecedência; A altera exatamente 24 h antes do início | Aceita |
| RN-13-T19 | Limite | Mesma reserva; A altera 23 h 59 min antes | Recusada, e a reserva não muda |
| RN-13-T20 | Limite | Pedido feito com menos de 24 h de antecedência; A altera exatamente 1 h antes | Aceita |
| RN-13-T21 | Limite | Pedido feito com menos de 24 h de antecedência; A altera 59 min antes | Recusada, e a reserva não muda |
| RN-13-T22 | Estado proibido | A altera uma reserva `EM_USO`, `CONCLUIDA`, `CANCELADA`, `REJEITADA` ou `NAO_COMPARECEU` | Recusada, e a reserva não muda |

### RN-14 · Prazos de aprovação e de alteração (regra do time, no PRD)

O prazo de aprovação e o de alteração são **24 h antes do início**. Se o pedido ou a alteração tem menos de 24 h de antecedência, o prazo é **1 h antes do início**. Os limites são inclusivos. A reserva guarda `prazo_aprovacao_em`, calculado na criação e recalculado na alteração com o **novo** início. O job (T-60) cancela a `SOLICITADA` só depois do prazo. Os casos usam o `Clock` fixo em segunda, 2026-11-09, às 08:00 UTC, e uma reserva de terça, 10:00 às 11:00, salvo indicação.

| Caso | Tipo | Situação | Resultado esperado |
|---|---|---|---|
| RN-14-T1 | Feliz | Pedido de segunda às 08:00 para terça às 10:00 (26 h de antecedência) | `prazo_aprovacao_em` = segunda às 10:00 (24 h antes do início) |
| RN-14-T2 | Feliz | Pedido de segunda às 16:00 para terça às 10:00 (18 h de antecedência) | `prazo_aprovacao_em` = terça às 09:00 (1 h antes do início) |
| RN-14-T3 | Limite | Pedido com exatamente 24 h de antecedência | `prazo_aprovacao_em` = início − 24 h (o próprio instante do pedido) |
| RN-14-T4 | Limite | Pedido com 23 h 59 min de antecedência | `prazo_aprovacao_em` = início − 1 h |
| RN-14-T5 | Limite | O Responsável aprova exatamente no instante do prazo | Aceita (limite inclusivo), estado `APROVADA` |
| RN-14-T6 | Limite | O Responsável aprova um instante depois do prazo, antes de o job rodar | Recusada com `409` "O prazo de aprovação desta reserva terminou"; a reserva só vira `CANCELADA` quando o job rodar |
| RN-14-T7 | Limite | O job roda exatamente no instante do prazo | Não cancela (só cancela depois do prazo) |
| RN-14-T8 | Feliz | O dono altera uma `APROVADA` com 30 h de antecedência | Volta a `SOLICITADA`, com `prazo_aprovacao_em` recalculado pelo novo início (novo início − 24 h) |
| RN-14-T9 | Limite | O dono altera uma `APROVADA` exatamente 24 h antes do início | Volta a `SOLICITADA`; o prazo recalculado é o próprio instante da alteração e a reserva não é cancelada nesse instante |
| RN-14-T10 | Feliz | O job roda duas vezes seguidas depois do prazo | O mesmo resultado nas duas (idempotente): 1 cancelamento e 1 registro de auditoria |
| RN-14-T11 | Integração | Consultar o banco depois da criação e depois da alteração | `prazo_aprovacao_em` guardado e recalculado com o novo início |
| (inválida) | Inválida | Não se aplica: os dados malformados são da RN-01 | — |

### RN-15 · Antecedência mínima de 1 hora (regra do time, no PRD)

O pedido precisa ter **pelo menos 1 h de antecedência** do início, no cadastro e na alteração, medida entre o instante atual do `Clock` e o **novo** início. Exatamente 1 h vale. Início no passado é recusado pela mesma regra. A RN-15 **substitui a RN-12-T3**. O `criada_em` da reserva é o instante em que o pedido é aceito (vem do `Clock`) e é imutável. Os casos usam o `Clock` fixo em segunda, 2026-11-09, às 09:00 UTC.

| Caso | Tipo | Situação | Resultado esperado |
|---|---|---|---|
| RN-15-T1 | Limite | Pedido às 09:00 para as 10:00 (exatamente 1 h) | Aceita; `criada_em` = 09:00 |
| RN-15-T2 | Limite | Pedido às 09:01 para as 10:00 (59 min) | Recusada com `409` "A reserva precisa ser pedida com pelo menos 1 hora de antecedência"; nada é gravado |
| RN-15-T3 | Inválida | Início exatamente agora (era o RN-12-T3) | Recusada com `409`; nada é gravado |
| RN-15-T4 | Inválida | Início no passado | Recusada com `409`; nada é gravado |
| RN-15-T5 | Feliz | O dono altera uma `APROVADA` para um novo início com 2 h de antecedência | Aceita; volta a `SOLICITADA` |
| RN-15-T6 | Inválida | O dono altera uma reserva antiga para começar 30 min depois do instante da alteração | Recusada com `409` (a conta é contra o `Clock` e o novo início, não contra o `criada_em` original) |
| RN-15-T7 | Conflito | Pedido com antecedência insuficiente **e** sobreposição | `409` com a mensagem da antecedência (vem antes da sobreposição) |
| RN-15-T8 | Feliz | O cliente envia um `criada_em` no corpo | Ignorado; vale o instante do `Clock` |
| RN-15-T9 | Feliz | O dono altera a reserva | O `criada_em` continua o mesmo |
| RN-15-T10 | Integração | Pedido recusado por antecedência | Nenhuma linha em `reserva` nem em `reserva_recurso` |

### RN-16 · Nome da sala no padrão sala-NN (regra do time, no PRD)

O nome da sala é exatamente `sala-` seguido de 2 ou 3 dígitos, em minúsculas, sem espaços e sem outros caracteres. Qualquer nome fora do padrão é recusado com `400`, e nada é corrigido sozinho. O padrão vale para toda sala ("Laboratório" é só o tipo da sala). O `CHECK` do banco só vale para o tipo `SALA`.

| Caso | Tipo | Situação | Resultado esperado |
|---|---|---|---|
| RN-16-T1 | Feliz | Nomes `sala-01`, `sala-99`, `sala-102` e `sala-999` | Todos aceitos |
| RN-16-T2 | Inválida | Nomes ` sala-01` e `sala-01 ` (espaço na frente e atrás) | Recusados com `400`, `erros` com `campo = nome` e motivo "deve seguir o padrão sala-NN" |
| RN-16-T3 | Inválida | Nomes `Sala-01` e `SALA-01` (maiúsculas) | Recusados com `400` |
| RN-16-T4 | Inválida | Nomes `sala 01`, `sala_01` e `sala-01x` | Recusados com `400` |
| RN-16-T5 | Inválida | Nomes `saal-01` e `lab-01` (palavra fora do padrão) | Recusados com `400` |
| RN-16-T6 | Limite | `sala-1` (1 dígito) / `sala-1000` (4 dígitos) | Os dois recusados com `400` |
| RN-16-T7 | Feliz | `sala-01` e `sala-001` | Os dois aceitos (são nomes diferentes) |
| RN-16-T8 | Integração | `INSERT` direto no banco de uma sala com o nome `Sala-01` | Recusado pelo `CHECK` do padrão |
| RN-16-T9 | Integração | `INSERT` direto de um material `Projetor Epson` e de um professor `ana-souza-12345` | Aceitos (o `CHECK` da sala só vale para o tipo `SALA`) |
| (conflito, estado) | — | Nome repetido é o RN-02-T18; estado não se aplica | — |

### RN-17 · Nome do professor no padrão primeironome-ultimonome-matricula (regra do time, no PRD)

O nome do professor é `primeironome-ultimonome-matricula`: duas partes de letras minúsculas sem acento, hífen e matrícula de **exatamente 5 dígitos**, sem espaços e sem nome composto, com até 100 caracteres. A matrícula é única entre os professores (coluna própria, `UNIQUE`). Fora do padrão, `400`. O `CHECK` do banco só vale para o tipo `PROFESSOR`.

| Caso | Tipo | Situação | Resultado esperado |
|---|---|---|---|
| RN-17-T1 | Feliz | `renata-silva-12345` | Aceito |
| RN-17-T2 | Feliz | `ana-souza-11111` e `ana-souza-22222` (homônimos) | Os dois aceitos (matrículas diferentes) |
| RN-17-T3 | Inválida | ` renata-silva-12345` e `renata-silva-12345 ` | Recusados com `400`, `erros` com `campo = nome` e motivo "deve seguir o padrão primeironome-ultimonome-matricula" |
| RN-17-T4 | Inválida | `Renata-Silva-12345` (maiúsculas) e `renata-silvá-12345` (acento) | Recusados com `400` |
| RN-17-T5 | Inválida | `renata silva 12345`, `renata_silva-12345`, `renata--silva-12345` e `renata.silva-12345` (separadores) | Recusados com `400` |
| RN-17-T6 | Inválida | `renata-silva`, `renata-silva-abc` e `renata-maria-silva-12345` (sem matrícula, matrícula com letra, três nomes) | Recusados com `400` |
| RN-17-T7 | Limite | Matrícula com 5 dígitos / com 4 (`renata-silva-1234`) / com 6 (`renata-silva-123456`) | 5 aceita; 4 e 6 recusadas com `400` |
| RN-17-T8 | Limite | Nome no padrão com 100 caracteres / com 101 | 100 aceito; 101 recusado com `400` |
| RN-17-T9 | Conflito | `renata-silva-12345` e `maria-souza-12345` (mesma matrícula) | O segundo recebe `409` (RN-02-T20) |
| RN-17-T10 | Integração | `INSERT` direto no banco de um professor com o nome `Ana Souza` | Recusado pelo `CHECK` do padrão |
| RN-17-T11 | Integração | `INSERT` direto de dois professores com a mesma matrícula | Recusado pelo `UNIQUE (matricula)` |

## 4. Ambiguidades, lacunas e perguntas

Pontos que as fontes não resolvem. Nenhum deles é regra oficial: os que viram **premissa de teste** estão marcados e precisam de validação.

| ID | Ambiguidade | Regra afetada | Premissa de teste atual |
|---|---|---|---|
| L01 · S-01 | Uma reserva que termina quando outra começa conta como sobreposição? | RN-02 | **Decisão do time**: período `[início, fim)` |
| L02 · S-02 | Quais estados ocupam o horário? | RN-02, RN-03 | **Decisão do time**: `SOLICITADA`, `APROVADA` e `EM_USO` ocupam; os demais não bloqueiam, mas ficam registrados para relatório |
| L03 | O que é "simultâneo" e que resposta recebe a solicitação recusada? | RN-03 | Sem premissa: o teste só verifica que uma reserva fica ativa |
| L04 | A reserva de um recurso não restrito passa por `APROVADA`? | RN-05 | **Decisão do time**: toda reserva passa por `SOLICITADA → APROVADA`, e só o Responsável aprova |
| L11 | Até quando uma reserva pode ser alterada ou cancelada? Cancelar uma reserva iniciada conta como "apagar"? | RN-06, RN-08 | **Decisão do time (RN-13 e RN-14)**: o dono altera até 24 h antes do início (1 h se o pedido tem menos de 24 h de antecedência); cancelar não apaga, e o resto do horário é liberado |
| A-01 | Horário de funcionamento e duração das reservas: nada nas fontes define. **Resolvida por proposta**: RN-10 e RN-11 (validação pendente, sugerir registrar no PRD) | RN-10, RN-11 | Valem as regras propostas |
| A-02 | Até quantos dias à frente se pode reservar? Sem número nas fontes | RN-12 | Sem limite máximo de antecedência |
| A-03 | Feriados: quais são e quem os cadastra (tabela do Administrador ou bloqueios)? | RN-10 | Os casos de feriado ficam marcados como validação pendente |
| A-04 | O que faz uma reserva passar a `EM_USO`: uma ação do usuário ("iniciar uso") ou a chegada da hora de início? A fonte não define. Por isso a RN-13 decide pelo **horário** (agora antes ou depois do início) | RN-05, RN-13 | **Decisão do time**: o check-in do dono, pelo endpoint `POST /api/reservas/{id}/checkin` (T-59), leva a `EM_USO`; a RN-13 decide pelo estado (com ou sem check-in) |
| A-05 | Uma reserva `SOLICITADA` que nunca foi aprovada, quando chega a hora de começar, vira `REJEITADA`, `CANCELADA` ou continua `SOLICITADA`? A fonte não diz. **Decisão do time (RN-14)**: cancelada pelo job se não for aprovada até o prazo (24 h antes do início, ou 1 h antes se o pedido tem menos de 24 h de antecedência) | RN-05, RN-06 | Valem RN-06-T9 e T12 |
| P-01 | Existe tolerância de atraso para o check-in? | RN-06, RN-13 | **Decisão do time: não há tolerância.** A `APROVADA` ocupa o horário inteiro, e só vira `NAO_COMPARECEU` no fim sem check-in ou no cancelamento do dono sem check-in (RN-06-T4, RN-06-T11 e RN-13-T15) |
| A-06 | Quantas horas de antecedência mínima? | RN-15 | **Decisão do time**: 1 h, inclusive (RN-15) |
| A-07 | Quem aprova? | RN-07 | **Decisão do time**: só o Responsável, em qualquer reserva (RN-07-T3, T12 e T13) |

## 5. Matriz regra → caso → teste → status

Atualizada pelas tasks seguintes. O status começa em `sem teste`, passa a `red` quando o teste é escrito e falha (T-20 e T-21) e a `green` quando o teste passa.

| Regra | Casos | Teste (task) | Status |
|---|---|---|---|
| RN-01 | RN-01-T1 a T15 | T-20; T-16, T-17 e T-18 (cadastro de recurso, RF-02) | sem teste |
| RN-02 | RN-02-T1 a T20, mais CT-01 (V01 a V21) | T-20; T-16, T-17 e T-18 (conflitos de cadastro); T-30 (restrição no banco) | sem teste |
| RN-03 | RN-03-T1 a T9, mais CT-02 | T-37 | sem teste |
| RN-04 | RN-04-T1 a T11, mais CEN-04 | T-35 | sem teste |
| RN-05 | RN-05-T1 a T16 | T-21; T-59 (check-in) | sem teste |
| RN-06 | RN-06-T1 a T17 | T-21; T-60 (job) | sem teste |
| RN-07 | RN-07-T1 a T15 | T-21 (autorização do `PATCH`); T-34; T-49 (matriz de autorização) | sem teste |
| RN-08 | RN-08-T1 a T11 | T-21; T-30 (o T9, no banco); T-61 (o T10, exclusão com vínculos) | sem teste |
| RN-09 | RN-09-T1 a T12 | T-40 | sem teste |
| RN-10 (proposta) | RN-10-T1 a T11 | sem task no backlog | sem teste, proposta |
| RN-11 (proposta) | RN-11-T1 a T6 | sem task no backlog | sem teste, proposta |
| RN-12 (proposta) | RN-12-T1 a T6 (o T3 foi substituído pela RN-15) | sem task no backlog | sem teste, proposta |
| RN-13 | RN-13-T1 a T22 | T-21 (T-20 para a alteração) | sem teste |
| RN-14 | RN-14-T1 a T11 | T-21; T-60 (job) | sem teste |
| RN-15 | RN-15-T1 a T10 | T-20 | sem teste |
| RN-16 | RN-16-T1 a T9 | T-16 | sem teste |
| RN-17 | RN-17-T1 a T11 | T-17 | sem teste |

**Regras sem teste: todas, por enquanto.** As propostas (RN-10 a RN-12) ainda não têm task no backlog. As tasks T-59 (check-in), T-60 (job a cada minuto) e T-61 (testes de exclusão com vínculos) vieram das specs corrigidas da semana 36.
