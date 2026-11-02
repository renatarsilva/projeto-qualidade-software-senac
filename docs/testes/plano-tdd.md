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

Os casos de cada regra entram nas seções abaixo, uma regra por vez.

## 4. Ambiguidades, lacunas e perguntas

Pontos que as fontes não resolvem. Nenhum deles é regra oficial: os que viram **premissa de teste** estão marcados e precisam de validação.

| ID | Ambiguidade | Regra afetada | Premissa de teste atual |
|---|---|---|---|
| L01 · S-01 | Uma reserva que termina quando outra começa conta como sobreposição? | RN-02 | Não conta: período `[início, fim)` |
| L02 · S-02 | Quais estados ocupam o horário? | RN-02, RN-03 | `SOLICITADA`, `APROVADA` e `EM_USO` ocupam; os demais não |
| L03 | O que é "simultâneo" e que resposta recebe a solicitação recusada? | RN-03 | Sem premissa: o teste só verifica que uma reserva fica ativa |
| L04 | A reserva de um recurso não restrito passa por `APROVADA`? | RN-05 | Sem premissa: o plano só cobre o fluxo completo |
| L11 | Até quando uma reserva pode ser alterada ou cancelada? Cancelar uma reserva iniciada conta como "apagar"? | RN-06, RN-08 | Sem premissa |

## 5. Matriz regra → caso → teste → status

Atualizada pelas tasks seguintes. O status começa em "sem teste" e muda quando o teste red é escrito (T-20 e T-21).

| Regra | Caso | Teste | Status |
|---|---|---|---|
| (preenchida à medida que os casos são escritos) | | | |
