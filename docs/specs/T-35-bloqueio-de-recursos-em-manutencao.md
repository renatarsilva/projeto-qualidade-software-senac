# Spec T-35: bloqueio de recursos em manutenção

| Campo | Valor |
|---|---|
| Task | T-35 |
| Autor(a) | @renatarsilva |
| Revisor(a) | @Samwel-Merces |
| Status | Liberada |
| Regra crítica | Sim |
| Requisitos | RF-08, RN-04 |
| Característica ISO 25010 | Adequação funcional |
| Criada em | 2026-11-10 |

## 1. Contexto e objetivo

Um recurso em manutenção não pode ser reservado (RN-04, regra crítica da especificação). Esta spec define como o **Administrador** registra e encerra períodos de manutenção, como a reserva (criação e alteração) é recusada quando toca um período, o que acontece com reservas que já existiam (S-05) e que a pesquisa de disponibilidade (T-43) não mostra o recurso em manutenção. Na mesa, o professor pede para quebrar a regra e ver qual teste falha.

## 2. Escopo

**Dentro:**
- `POST`, `GET` e `DELETE` de períodos de manutenção por recurso (só o Administrador).
- Recusa de reserva (`POST` e `PUT`) que toca um período de manutenção do recurso pedido (sala, professor ou material), com as fronteiras `[início, fim)`.
- Criar manutenção **não cancela** reservas existentes: a resposta lista as reservas afetadas (S-05).
- A disponibilidade da pesquisa (T-43, CA-06) exclui o recurso em manutenção.
- Manutenções do mesmo recurso não se sobrepõem (restrição do banco, arquitetura 3.2).
- Manutenção e reserva ao mesmo tempo: nenhuma reserva é gravada depois de a manutenção ser confirmada (CEN-04).

**Fora (e por quê):**
- Cancelar em lote as reservas afetadas: decisão S-05 (o Administrador trata caso a caso).
- Auditoria da manutenção: a auditoria (RN-09) é das mudanças de estado da reserva, não da manutenção.
- Telas de manutenção: ficam para a interface (T-46).

## 3. Regras de negócio

- **RN-04 (oficial):** recurso em manutenção não pode ser reservado.
- **S-04 / S-05 (decisões do time):** bloqueio e manutenção são o mesmo mecanismo; criar manutenção não cancela reservas existentes, só impede novas e lista as afetadas.
- **Período:** `[início, fim)`. Uma reserva que termina quando a manutenção começa, ou começa quando ela termina, **não** conflita (S-01).
- **Reserva afetada:** reserva existente em estado que ocupa o horário (`SOLICITADA`, `APROVADA` ou `EM_USO`, S-02) cujo período cruza o da manutenção.
- **Ordem dos 409 da reserva:** reserva que não pode ser alterada, antecedência mínima (RN-15), **manutenção** e, por último, sobreposição. A mensagem diz o recurso: "A sala está em manutenção neste horário", "O professor está em manutenção neste horário" ou "O material está em manutenção neste horário".
- **Validação do período:** término posterior ao início (RN-01), término posterior ao instante atual do `Clock` (não se registra manutenção que já acabou), motivo obrigatório com até 100 caracteres.
- **Quem registra:** só o Administrador (RF-08). Responsável e Solicitante recebem 403.
- **Encerrar a manutenção:** o `DELETE` do período libera o recurso para novas reservas.

## 4. Contrato da API

`POST /api/recursos/{id}/manutencoes` (Administrador):

```json
{ "inicio": "2026-11-10T10:00:00Z", "termino": "2026-11-10T12:00:00Z", "motivo": "Troca do ar-condicionado" }
```

Resposta 201: `{ "id": 4, "recursoId": 1, "inicio": "...", "termino": "...", "motivo": "...", "reservasAfetadas": [15, 16] }`.

`GET /api/recursos/{id}/manutencoes` (lista, por início) e `DELETE /api/recursos/{id}/manutencoes/{manutencaoId}` (204), só Administrador.

| Situação | Status HTTP | Corpo |
|---|---|---|
| Período registrado | 201 | O período e a lista `reservasAfetadas` |
| Término igual ou anterior ao início, término no passado, motivo ausente ou maior que 100 | 400 | `ProblemDetail`, `erros` com o campo |
| Sem autenticação | 401 | `ProblemDetail` neutro (T-22) |
| Responsável ou Solicitante | 403 | `ProblemDetail` |
| Recurso ou período que não existe | 404 | "Recurso não encontrado" |
| Período que se sobrepõe a outra manutenção do mesmo recurso | 409 | "O recurso já tem manutenção neste período" |
| Reserva que toca a manutenção (`POST` e `PUT /api/reservas`) | 409 | "A sala está em manutenção neste horário" (ou professor, ou material) |

## 5. Critérios de aceite

### CA-01: Reserva dentro ou cruzando a manutenção é recusada

- **Dado** a sala-102 em manutenção na terça, das 10:00 às 12:00
- **Quando** o Solicitante pede a sala-102 das 10:30 às 11:30 (dentro) ou das 09:30 às 10:30 (cruza o início)
- **Então** o pedido é recusado com 409 e a mensagem de manutenção, e nada é gravado

### CA-02: As fronteiras encostadas são aceitas

- **Dado** a mesma manutenção, das 10:00 às 12:00
- **Quando** o Solicitante pede a sala das 09:00 às 10:00 ou das 12:00 às 13:00
- **Então** o pedido é aceito (S-01)

### CA-03: Outro recurso e outro dia não são afetados

- **Dado** a sala-102 em manutenção
- **Quando** o Solicitante pede a sala-101 no mesmo horário, ou a sala-102 em outro dia
- **Então** o pedido é aceito

### CA-04: Vale para sala, professor e material

- **Dado** uma manutenção de um professor ou de um material
- **Quando** o pedido inclui esse recurso no horário
- **Então** o pedido é recusado com a mensagem do tipo certo

### CA-05: Pedido com vários recursos é tudo ou nada

- **Dado** um pedido de sala livre e projetor em manutenção
- **Quando** o pedido é feito
- **Então** é recusado por inteiro e a sala não fica gravada

### CA-06: A alteração também respeita a manutenção

- **Dado** uma reserva do dono
- **Quando** ele a altera para um horário ou recurso em manutenção
- **Então** a alteração é recusada com 409 e a reserva mantém o horário original

### CA-07: Criar manutenção não cancela reservas existentes

- **Dado** uma reserva `APROVADA` das 10:30 às 11:30 da sala
- **Quando** o Administrador registra a manutenção das 10:00 às 12:00
- **Então** o período é criado (201), a reserva continua `APROVADA` e aparece em `reservasAfetadas`

### CA-08: Encerrar a manutenção libera o recurso

- **Dado** uma manutenção registrada
- **Quando** o Administrador a encerra (`DELETE`) e depois alguém reserva o recurso no período
- **Então** a reserva é aceita

### CA-09: Validação e conflito entre manutenções

- **Dado** o cadastro de manutenção
- **Quando** o término é igual ou anterior ao início, já passou, o motivo falta ou é grande demais, ou o período cruza outra manutenção do mesmo recurso
- **Então** recebe 400 (com o campo) ou 409 (cruzamento), e nada é gravado

### CA-10: Só o Administrador registra

- **Dado** as três rotas de manutenção
- **Quando** o Responsável, o Solicitante ou um anônimo as chama
- **Então** a resposta é 403, 403 e 401, e nada muda

### CA-11: A pesquisa de disponibilidade exclui o recurso em manutenção

- **Dado** um recurso em manutenção num intervalo
- **Quando** o usuário pesquisa a disponibilidade desse intervalo (T-43)
- **Então** o recurso não aparece

### CA-12: Manutenção e reserva ao mesmo tempo

- **Dado** uma reserva e uma manutenção do mesmo recurso e horário, pedidas ao mesmo tempo
- **Quando** as duas chegam em paralelo
- **Então** nunca existem as duas: ou a manutenção foi registrada e a reserva é recusada, ou a reserva foi gravada e a manutenção lista a reserva como afetada

## 6. Casos de teste

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 (RN-04-T1) | CA-01 | Conflito | Manutenção 10:00-12:00; reserva 10:30-11:30 | 409 de manutenção; nada gravado | Integração |
| TC-02 (RN-04-T2) | CA-01 | Conflito | Reserva 09:30-10:30 (invade o início) | 409; nada gravado | Integração |
| TC-03 (RN-04-T3) | CA-02 | Valor limite | Reserva 09:00-10:00 | 201 (encostada) | Integração |
| TC-04 (RN-04-T4) | CA-02 | Valor limite | Reserva 12:00-13:00 | 201 | Integração |
| TC-05 (RN-04-T5) | CA-03 | Classe válida | A sala-101, sem manutenção, no mesmo horário | 201 | Integração |
| TC-06 (RN-04-T6) | CA-03 | Classe válida | A sala-102 em outro dia | 201 | Integração |
| TC-07 (RN-04-T7) | CA-04 | Conflito | Material em manutenção no período | 409 com a mensagem do material | Integração |
| TC-08 (sem caso no plano) | CA-04 | Conflito | Professor em manutenção no período | 409 com a mensagem do professor | Integração |
| TC-09 (RN-04-T8) | CA-05 | Conflito | Sala livre e projetor em manutenção | 409 por inteiro; a sala não fica gravada | Integração |
| TC-10 (sem caso no plano) | CA-06 | Conflito | O dono altera a reserva para o horário em manutenção | 409; mantém o horário original | Integração |
| TC-11 (RN-04-T9) | CA-07 | Estado | Reserva `APROVADA` 10:30-11:30; manutenção 10:00-12:00 | 201; a reserva continua `APROVADA` e está em `reservasAfetadas` | Integração |
| TC-12 (RN-04-T11) | CA-08 | Classe válida | `DELETE` da manutenção; depois, reserva no período | 204; reserva 201 | Integração |
| TC-13 (sem caso no plano) | CA-09 | Tabela de decisão | Término igual ao início; término antes de agora; sem motivo; motivo com 101 caracteres | 400 com o campo de cada erro | Integração |
| TC-14 (sem caso no plano) | CA-09 | Conflito | Manutenção que cruza outra do mesmo recurso | 409; nada gravado | Integração |
| TC-15 (sem caso no plano) | CA-10 | Tabela de decisão | `POST`, `GET` e `DELETE` por Solicitante, Responsável e anônimo | 403, 403 e 401 | Integração |
| TC-16 (sem caso no plano) | CA-09 | Classe inválida | Manutenção de um recurso que não existe | 404 | Integração |
| TC-17 (sem caso no plano) | CA-11 | Classe válida | Recurso em manutenção; pesquisa de disponibilidade do intervalo | O recurso não aparece | Integração |
| TC-18 (RN-04-T10, CEN-04) | CA-12 | Concorrência | Reserva e manutenção do mesmo recurso e horário, em paralelo, em 15 rodadas | Nunca existem as duas sem a reserva constar como afetada | Integração |
| TC-19 (sem caso no plano) | CA-01 | Valor limite | Reserva que contém toda a manutenção (09:00-13:00) | 409 | Integração |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| Erro de um minuto na fronteira | Alto | TC-03 e TC-04 testam os dois lados |
| A manutenção é ignorada na alteração da reserva | Alto | CA-06 e TC-10 |
| Manutenção e reserva passam juntas (corrida) | Alto | A criação da reserva e da manutenção serializam pelo recurso (bloqueio da linha do recurso); TC-18 |
| Reservas existentes viram "furos" sem aviso | Médio | CA-07: a resposta lista as afetadas |
| O professor quebra a regra e nenhum teste falha | Alto | A mutação manual (retirar a verificação) fica na T-50; os testes TC-01 a TC-10 ficam vermelhos |

## 8. Mini-plano de implementação (ciclos TDD)

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-13 a TC-16: cadastro e validação do período | Migração do período com `EXCLUDE`, `POST` e `DELETE` | Nomear as restrições |
| 2 | TC-01 a TC-06 e TC-19: reserva e fronteiras | Verificação de manutenção na criação da reserva | Extrair a verificação |
| 3 | TC-07 a TC-09: sala, professor, material e tudo ou nada | A verificação cobre os três tipos | Tratar os tipos do mesmo jeito |
| 4 | TC-10: alteração | A verificação também na alteração | Reaproveitar a verificação |
| 5 | TC-11 e TC-12: reservas afetadas e encerramento | Lista de afetadas e `DELETE` | Isolar a consulta de afetadas |
| 6 | TC-15 e a matriz de autorização | Regras de acesso (só Administrador) e linhas na matriz | Atualizar a matriz |
| 7 | TC-17: disponibilidade (T-43, TC-20) | A pesquisa exclui o recurso em manutenção | Reaproveitar a consulta |
| 8 | TC-18: concorrência | Bloqueio da linha do recurso na reserva e na manutenção | Documentar no ADR-0010 |

## 9. Revisão por pares

> Preenchida pelo revisor, com commit próprio. Nunca pelo autor.

- [x] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [x] As regras citam RNs que existem no PRD.
- [x] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [x] Os casos de teste incluem valores limite e entradas inválidas.
- [x] O contrato da API cobre os erros (400, 401, 403, 409).
- [x] Não há conflito com outras specs nem com os ADRs.

**Comentários:** Revisão feita pela autora (autoaprovação liberada pelo professor em 2026-11-10; revisor formal pelo rodízio: @Samwel-Merces). Conferi as fronteiras, a ordem dos 409, a regra S-05 (não cancela reservas existentes) e a corrida entre manutenção e reserva.

**Parecer:** Aprovada · @renatarsilva · 2026-11-10

## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** 2026-11-10
- **Resultado:** APROVADA na spec validation

## 11. Grill-with-docs

> Obrigatório só para regra crítica. Nas demais, escreva "Não se aplica".

| # | Pergunta | Resposta / decisão | Documento afetado |
|---|---|---|---|
| 1 | Quem registra, lista e encerra a manutenção? | Só o Administrador; Responsável e Solicitante recebem 403 | CA-10, matriz |
| 2 | O que acontece com reservas que já existiam no período? | Nada: continuam como estão e aparecem em `reservasAfetadas` (S-05) | CA-07 |
| 3 | Qual a ordem dos 409 da reserva? | Não alterável, antecedência, manutenção, sobreposição; a mensagem diz o recurso | Seção 3 |
| 4 | Que validação tem o período? | Término depois do início e depois de agora (`Clock`); motivo obrigatório com até 100 caracteres | CA-09 |
| 5 | Como evitar duas manutenções cruzadas no mesmo recurso? | `EXCLUDE` do banco (arquitetura 3.2, restrição 4) | CA-09 |
| 6 | Como resolver a corrida entre manutenção e reserva? | Bloqueio da linha do recurso nas duas operações (reserva compartilha, manutenção exclusivo); o TC-18 prova | CA-12 |
| 7 | A pesquisa mostra recurso em manutenção? | Não: a disponibilidade da T-43 o exclui (TC-20 da T-43 reativado) | CA-11 |
| 8 | Qual linha da RTM isto preenche? | RN-04 e RF-08: de `sem teste` para `red` e `green` | `docs/RTM.md` |

Decisões confirmadas em bloco por @renatarsilva (dona da task) em 2026-11-10.

**Resultado:** Liberada · 2026-11-10
