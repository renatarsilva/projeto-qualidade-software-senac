# Plano TDD: inventário das regras de negócio

> Task **T-19** · Autoria: Renata (@renatarsilva) · Revisão: Samuel (@Samwel-Merces)
>
> Este documento lista cada regra de negócio do [PRD](../prd.md) com os casos de teste e o resultado esperado. **Não contém código nem testes.** Os testes red nascem a partir dele (T-20 e T-21).

## 1. Fontes

Em ordem de prioridade:

1. [`prd.md`](../prd.md): regras RN-01 a RN-09 (seção 4.4), lacunas (4.8) e decisões pendentes (4.9).
2. [`arquitetura.md`](../arquitetura.md): suposições S-01 a S-06, decisões D-01 e D-02, sequências (2.4) e restrições do banco (3.2).
3. [`atam/utility-tree-inicial.md`](../atam/utility-tree-inicial.md): casos CT-01 (sobreposição), CT-02 (simultaneidade) e CT-03 (notificação), que este plano referencia em vez de repetir.
4. Os [ADRs](../adr/) 0004 a 0009.

A especificação oficial vale sobre qualquer outro documento. Nenhuma regra foi inventada: o que não está nas fontes aparece na seção 4, como ambiguidade.

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
| RN-12 (proposta) | Antecedência: a reserva vale para hoje ou para qualquer dia futuro, nunca no passado, desde que o recurso esteja livre | RF-04 | Semana 36/37 |
| RN-13 (proposta) | Cancelamento libera o recurso: o dono cancela antes do início (`CANCELADA`) ou encerra antes do fim (`CONCLUIDA`), e o resto do horário volta a ficar livre | RF-04 | Semana 36/37 |

**RN-10 a RN-13 são propostas do time**: não estão na especificação nem no PRD, e precisam ser registradas no PRD e validadas com o professor.

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
| (conflito) | Conflito | Não se aplica: a regra compara os dois horários da própria reserva, sem outra reserva envolvida | — |
| (estado) | Estado proibido | Não se aplica: a regra não depende do estado da reserva | — |

Premissas de teste: a fonte não exige duração mínima geral, então qualquer duração positiva é válida no meio do dia (RN-01-T3); e toda reserva fica dentro de um único dia, conforme a RN-10.

### RN-02 · Reservas do mesmo recurso não podem se sobrepor

Fonte: PRD, seção 4.4 · RF-05. As formas de sobreposição em sala, material e professor (V01 a V21) estão no [CT-01](../atam/utility-tree-inicial.md) e não se repetem aqui. Premissas: S-01 (reservas encostadas convivem) e S-02 (quais estados ocupam o horário).

Base dos casos: sala 102, terça, 10:00 às 11:00, reserva `APROVADA` do usuário A, salvo indicação.

| Caso | Tipo | Situação | Resultado esperado |
|---|---|---|---|
| RN-02-T1 | Conflito | A estende a sua reserva para 10:00 às 12:00, mas B tem a sala 102 das 11:00 às 12:00 | Recusada, e a reserva de A continua das 10:00 às 11:00 |
| RN-02-T2 | Feliz | A estende a sua reserva para 10:00 às 12:00, com a sala livre depois | Aceita (a reserva não conflita com ela mesma) |
| RN-02-T3 | Limite | A estende de 10:00 às 10:30 para 10:00 às 11:00, e B tem 11:00 às 12:00 | Aceita (encostada, S-01) |
| RN-02-T4 | Conflito | A muda a reserva para 13:00 às 14:00, horário já ocupado por C | Recusada, e a reserva de A fica no horário original |
| RN-02-T5 | Conflito | Pedido de sala 102, projetor e professor X; só o projetor está ocupado | Recusada por inteiro; a sala e o professor X continuam livres |
| RN-02-T6 | Conflito | Pedido em que a sala e o professor conflitam ao mesmo tempo | Recusada, e nada é gravado |
| RN-02-T7 | Estado | B pede 10:30 às 11:30, e a reserva existente está `SOLICITADA` | Recusada (ocupa o horário) |
| RN-02-T8 | Estado | Idem, com a existente `EM_USO` | Recusada (ocupa o horário) |
| RN-02-T9 | Estado | Idem, com a existente `CANCELADA` | Aceita (não ocupa) |
| RN-02-T10 | Estado | Idem, com a existente `REJEITADA` | Aceita (não ocupa) |
| RN-02-T11 | Estado | Idem, com a existente `NAO_COMPARECEU` | Aceita (não ocupa) |
| RN-02-T12 | Feliz | B pede a sala 101, terça das 10:00 às 11:00, com a sala 102 reservada | Aceita (recursos diferentes) |
| RN-02-T13 | Feliz | B pede a sala 102 em outro dia, das 10:00 às 11:00 | Aceita |
| (inválida) | Inválida | Não se aplica: os dados malformados são da RN-01 | — |

### RN-03 · Duas solicitações simultâneas produzem uma única reserva aceita

Fonte: PRD, seção 4.4 · RF-06. Premissas: S-02 (quais estados ocupam o horário) e L03 (a forma da recusa não está definida: o teste só verifica que a reserva recusada **não é gravada**). O caso de 20 solicitações é o [CT-02](../atam/utility-tree-inicial.md); os demais são menores e não se repetem lá.

Base dos casos: sala 102, terça, salvo indicação.

| Caso | Tipo | Situação | Resultado esperado |
|---|---|---|---|
| RN-03-T1 | Conflito | 20 solicitações simultâneas para a mesma sala e o mesmo intervalo (CT-02) | Exatamente 1 aceita, 19 recusadas e 1 reserva ativa |
| RN-03-T2 | Limite | 2 solicitações simultâneas, mesma sala e mesmo intervalo (a menor disputa) | 1 aceita, 1 recusada e 1 reserva ativa |
| RN-03-T3 | Conflito | 2 simultâneas com sobreposição parcial (10:00 às 11:00 e 10:30 às 11:30) | 1 aceita, 1 recusada |
| RN-03-T4 | Feliz | 2 simultâneas na mesma sala, em intervalos encostados (10:00 às 11:00 e 11:00 às 12:00) | As duas aceitas |
| RN-03-T5 | Feliz | 2 simultâneas, em salas diferentes e no mesmo horário | As duas aceitas |
| RN-03-T6 | Conflito | O mesmo usuário envia o mesmo pedido 2 vezes ao mesmo tempo (duplo clique) | 1 reserva criada, e a repetição é recusada |
| RN-03-T7 | Conflito | A pede sala 102 e projetor, e B pede sala 103 e o mesmo projetor, ao mesmo tempo | 1 aceita, e a recusada não deixa a sala 103 gravada |
| RN-03-T8 | Estável | O caso RN-03-T2 repetido 5 vezes seguidas | O mesmo resultado nas 5 (premissa: 5 repetições é métrica candidata, MC-03) |
| RN-03-T9 | Feliz | Depois da disputa, quem foi recusado pede outro horário livre | Aceita |
| (inválida e estado) | — | Não se aplicam: a regra é só sobre simultaneidade | — |

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

### RN-12 · Antecedência (proposta)

A reserva vale para hoje ou para qualquer dia futuro, **nunca no passado**, desde que o recurso esteja livre. Os casos usam o `Clock` fixo em segunda, às 10:00.

| Caso | Tipo | Situação | Resultado esperado |
|---|---|---|---|
| RN-12-T1 | Feliz | Hoje, das 11:00 às 12:00 (início depois de agora) | Aceita |
| RN-12-T2 | Feliz | Uma quinta futura, das 14:00 às 15:00, com a sala livre | Aceita |
| RN-12-T3 | Limite | Hoje, das 10:00 às 11:00 (início exatamente agora) | Aceita (premissa: início igual a agora não é passado) |
| RN-12-T4 | Inválida | Hoje, das 08:00 às 09:00 (início já passou) | Recusada, e nada é gravado |
| RN-12-T5 | Inválida | Ontem, das 10:00 às 11:00 | Recusada, e nada é gravado |
| RN-12-T6 | Conflito | Uma quinta futura, em horário já ocupado no mesmo recurso | Recusada pela RN-02, e nada é gravado |
| (estado) | Estado proibido | Não se aplica: a regra compara datas, não estados | — |

### RN-13 · Cancelamento libera o recurso (proposta)

O **dono** da reserva pode cancelá-la. Antes do início, a reserva vira `CANCELADA` e o horário inteiro volta a ficar livre. Depois do início e antes do fim, é um **encerramento antecipado**: a reserva vira `CONCLUIDA` (o uso aconteceu) e o resto do horário volta a ficar livre. Depois do fim, não há o que cancelar. A reserva nunca é apagada (RN-08) e toda mudança gera auditoria (RN-09).

Os casos usam o `Clock` fixo em segunda, às 08:00, e uma reserva `APROVADA` do usuário A, na sala 102, das 10:00 às 11:00, salvo indicação.

| Caso | Tipo | Situação | Resultado esperado |
|---|---|---|---|
| RN-13-T1 | Feliz | A cancela às 08:00, antes do início | Estado `CANCELADA`; o horário das 10:00 às 11:00 fica livre; a auditoria registra a mudança |
| RN-13-T2 | Feliz | Depois do cancelamento de T1, o usuário B reserva a sala 102 das 10:00 às 11:00 | Aceita (o conflito deixou de existir) |
| RN-13-T3 | Feliz | A cancela uma reserva ainda `SOLICITADA` (sem aprovação), antes do início | Estado `CANCELADA`; o horário fica livre |
| RN-13-T4 | Limite | A cancela às 09:59, um minuto antes do início | Estado `CANCELADA` |
| RN-13-T5 | Limite | A cancela às 10:00 em ponto (premissa: início igual a agora já conta como iniciada) | Encerramento antecipado: estado `CONCLUIDA` |
| RN-13-T6 | Feliz | Reserva das 10:00 às 12:00; A encerra às 10:40 | Estado `CONCLUIDA`; a reserva continua no histórico |
| RN-13-T7 | Feliz | Depois de T6, B reserva a sala 102 das 11:00 às 12:00 | Aceita (o resto do horário foi liberado) |
| RN-13-T8 | Estado proibido | A tenta cancelar às 11:30, depois do fim | Recusada, e o estado não muda |
| RN-13-T9 | Estado proibido | A cancela uma reserva que já está `CANCELADA` | Recusada, e o estado não muda |
| RN-13-T10 | Estado proibido | A cancela uma reserva `REJEITADA` ou `NAO_COMPARECEU` | Recusada, e o estado não muda |
| RN-13-T11 | Conflito | B, que não é o dono, tenta cancelar a reserva de A | Recusada, e a reserva fica intacta (premissa: só o dono cancela; o caso do Administrador entra na matriz de autorização, T-48) |
| RN-13-T12 | Inválida | Cancelar uma reserva que não existe | Recusada, sem alterar nada |
| RN-13-T13 | Feliz | Em T1, T4 e T6, o registro de auditoria traz o estado anterior, o novo, o usuário e o instante | Um registro por mudança (RN-09) |

## 4. Ambiguidades, lacunas e perguntas

Pontos que as fontes não resolvem. Nenhum deles é regra oficial: os que viram **premissa de teste** estão marcados e precisam de validação.

| ID | Ambiguidade | Regra afetada | Premissa de teste atual |
|---|---|---|---|
| L01 · S-01 | Uma reserva que termina quando outra começa conta como sobreposição? | RN-02 | Não conta: período `[início, fim)` |
| L02 · S-02 | Quais estados ocupam o horário? | RN-02, RN-03 | `SOLICITADA`, `APROVADA` e `EM_USO` ocupam; os demais não |
| L03 | O que é "simultâneo" e que resposta recebe a solicitação recusada? | RN-03 | Sem premissa: o teste só verifica que uma reserva fica ativa |
| L04 | A reserva de um recurso não restrito passa por `APROVADA`? | RN-05 | Sem premissa: o plano só cobre o fluxo completo |
| L11 | Até quando uma reserva pode ser alterada ou cancelada? Cancelar uma reserva iniciada conta como "apagar"? | RN-06, RN-08 | **Respondida por proposta (RN-13, validação pendente)**: cancelar não apaga; a reserva fica no histórico e o resto do horário é liberado |
| A-01 | Horário de funcionamento e duração das reservas: nada nas fontes define. **Resolvida por proposta**: RN-10 e RN-11 (validação pendente, sugerir registrar no PRD) | RN-10, RN-11 | Valem as regras propostas |
| A-02 | Até quantos dias à frente se pode reservar? Sem número nas fontes | RN-12 | Sem limite máximo de antecedência |
| A-03 | Feriados: quais são e quem os cadastra (tabela do Administrador ou bloqueios)? | RN-10 | Os casos de feriado ficam marcados como validação pendente |
| A-04 | O que faz uma reserva passar a `EM_USO`: uma ação do usuário ("iniciar uso") ou a chegada da hora de início? A fonte não define. Por isso a RN-13 decide pelo **horário** (agora antes ou depois do início) | RN-05, RN-13 | A decisão é pelo horário, não pelo estado |

## 5. Matriz regra → caso → teste → status

Atualizada pelas tasks seguintes. O status começa em "sem teste" e muda quando o teste red é escrito (T-20 e T-21).

| Regra | Caso | Teste | Status |
|---|---|---|---|
| (preenchida à medida que os casos são escritos) | | | |
