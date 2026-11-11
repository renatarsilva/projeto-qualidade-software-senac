# Spec T-42: retirada e devolução de materiais

| Campo | Valor |
|---|---|
| Task | T-42 |
| Autor(a) | @renatarsilva |
| Revisor(a) | @Samwel-Merces |
| Status | Liberada |
| Regra crítica | Não |
| Requisitos | RF-09, DP-04 |
| Característica ISO 25010 | Adequação funcional |
| Criada em | 2026-11-11 |

## 1. Contexto e objetivo

O Responsável acompanha a retirada e a devolução dos materiais (projetor, kit de prática) ligados às reservas aprovadas (RF-09). Esta spec define como o Responsável **registra** a retirada e a devolução, as condições para retirar (só material de uma reserva aprovada) e como a **devolução atrasada** fica sinalizada. A DP-04 (quem registra) é decidida aqui: só o Responsável.

## 2. Escopo

**Dentro:**
- Registro da retirada de um material ligado a uma reserva `APROVADA` ou `EM_USO`.
- Registro da devolução, com o instante do `Clock`.
- Sinalização de devolução atrasada: o prazo é o **término da reserva**.
- Consulta das retiradas de uma reserva (Responsável e Administrador).

**Fora (e por quê):**
- Estado de conservação do material, multa e bloqueio do solicitante por atraso: a especificação não pede.
- Notificação de atraso: fora do escopo da T-31, que notifica só aprovação, rejeição e cancelamento.
- Telas: ficam para a interface.

## 3. Regras de negócio

- **RF-09:** registro de retirada e devolução de materiais e equipamentos.
- **DP-04 (decisão desta spec):** só o **Responsável** registra a retirada e a devolução. O Administrador e o Solicitante recebem 403.
- **Retirada só com reserva aprovada:** a reserva precisa estar `APROVADA` ou `EM_USO`, o material precisa fazer parte dela, e a retirada só vale antes do término da reserva.
- **Uma retirada aberta por material e reserva:** não se retira de novo um material que ainda não foi devolvido.
- **Devolução:** registrada uma vez só, depois da retirada. O instante vem do `Clock` (ADR-0008).
- **Devolução atrasada:** a devolução (ou, sem devolução, o instante atual) depois do **término da reserva** é atraso. A resposta traz `atrasada = true`.
- **Ordem dos erros:** 401, 403 por perfil, 404, 400 e 409.

## 4. Contrato da API

`POST /api/reservas/{id}/retiradas` (Responsável), corpo `{ "materialId": 5 }`: registra a retirada.

`POST /api/reservas/{id}/retiradas/{retiradaId}/devolucao` (Responsável): registra a devolução.

`GET /api/reservas/{id}/retiradas` (Responsável e Administrador): as retiradas da reserva, em ordem.

```json
{ "id": 1, "reservaId": 15, "materialId": 5, "registradaPor": 9, "retiradaEm": "2026-11-10T10:05:00Z",
  "devolvidaEm": null, "atrasada": false }
```

| Situação | Status HTTP | Corpo |
|---|---|---|
| Retirada ou devolução registrada | 201 (retirada) ou 200 (devolução) | A retirada, com `atrasada` |
| `materialId` ausente | 400 | `ProblemDetail`, `erros` com `materialId` |
| Sem autenticação | 401 | `ProblemDetail` neutro (T-22) |
| Solicitante ou Administrador registra | 403 | `ProblemDetail` |
| Reserva, material ou retirada que não existe | 404 | "Recurso não encontrado" |
| Reserva que não está `APROVADA` nem `EM_USO` | 409 | "Só se retira material de uma reserva aprovada" |
| Material que não faz parte da reserva | 409 | "O material não faz parte desta reserva" |
| Retirada depois do término da reserva | 409 | "A reserva já terminou" |
| Material já retirado e não devolvido; devolução já registrada | 409 | "Este material já foi retirado" / "Esta devolução já foi registrada" |

A retirada guarda a reserva e o material (`reserva_id` e `material_id`), e não o vínculo `reserva_recurso`: o vínculo é recriado quando a reserva é alterada, e a retirada não pode depender dele.

## 5. Critérios de aceite

### CA-01: Retirada de material de uma reserva aprovada

- **Dado** uma reserva `APROVADA` (ou `EM_USO`) com um material
- **Quando** o Responsável registra a retirada desse material
- **Então** recebe 201 com a retirada: o Responsável como autor, o instante do `Clock`, sem devolução e `atrasada = false`

### CA-02: Sem reserva aprovada, não há retirada

- **Dado** uma reserva `SOLICITADA`, `CANCELADA`, `REJEITADA`, `CONCLUIDA` ou `NAO_COMPARECEU`
- **Quando** o Responsável tenta registrar a retirada
- **Então** recebe 409 e nada é gravado

### CA-03: O material precisa ser da reserva

- **Dado** uma reserva aprovada e um material que não faz parte dela
- **Quando** o Responsável tenta registrar a retirada
- **Então** recebe 409 ("O material não faz parte desta reserva")

### CA-04: Não se retira de novo sem devolver

- **Dado** um material já retirado e ainda não devolvido
- **Quando** o Responsável tenta registrar nova retirada do mesmo material na mesma reserva
- **Então** recebe 409

### CA-05: Retirada depois do término é recusada

- **Dado** uma reserva `EM_USO` cujo término já passou
- **Quando** o Responsável tenta registrar a retirada
- **Então** recebe 409 ("A reserva já terminou")

### CA-06: Devolução no prazo

- **Dado** uma retirada aberta e o instante antes do término da reserva
- **Quando** o Responsável registra a devolução
- **Então** recebe 200 com `devolvidaEm` do `Clock` e `atrasada = false`

### CA-07: Devolução atrasada é sinalizada

- **Dado** uma retirada aberta e o instante depois do término da reserva
- **Quando** o Responsável registra a devolução
- **Então** recebe 200 com `atrasada = true`
- **E** a devolução exatamente no término não é atraso

### CA-08: A devolução vale uma vez

- **Dado** uma retirada já devolvida
- **Quando** o Responsável tenta devolver de novo
- **Então** recebe 409 e a primeira devolução não muda

### CA-09: Só o Responsável registra

- **Dado** as rotas de retirada e de devolução
- **Quando** o Solicitante, o Administrador ou um anônimo as chama
- **Então** recebe 403, 403 e 401, e nada é gravado

### CA-10: Consulta e inexistentes

- **Dado** retiradas de uma reserva
- **Quando** o Responsável ou o Administrador consulta a lista
- **Então** recebe 200 com as retiradas em ordem, e o Solicitante recebe 403
- **E** reserva, material ou retirada que não existe recebe 404

### CA-11: Retirada aberta depois do prazo aparece como atrasada

- **Dado** uma retirada sem devolução e o instante depois do término da reserva
- **Quando** o Responsável consulta a lista
- **Então** a retirada vem com `atrasada = true`

## 6. Casos de teste

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 | CA-01 | Classe válida | Retirada do projetor de uma reserva `APROVADA` | 201; autor, instante, sem devolução, `atrasada = false` | Integração |
| TC-02 | CA-01 | Classe equivalente | A reserva está `EM_USO` | 201 | Integração |
| TC-03 | CA-02 | Estado proibido | Reserva `SOLICITADA`, `CANCELADA`, `REJEITADA`, `CONCLUIDA` e `NAO_COMPARECEU` (parametrizado) | 409 nas cinco; nada gravado | Integração |
| TC-04 | CA-03 | Conflito | Material que não está na reserva | 409; nada gravado | Integração |
| TC-05 | CA-04 | Conflito | Segunda retirada do mesmo material sem devolver | 409 | Integração |
| TC-06 | CA-05 | Valor limite | Retirada no instante do término da reserva | 409 | Integração |
| TC-07 | CA-06 | Classe válida | Devolução antes do término | 200; `devolvidaEm` do `Clock`; `atrasada = false` | Integração |
| TC-08 | CA-07 | Valor limite | Devolução 1 s depois do término | 200; `atrasada = true` | Integração |
| TC-09 | CA-07 | Valor limite | Devolução exatamente no término | 200; `atrasada = false` | Integração |
| TC-10 | CA-08 | Conflito | Segunda devolução | 409; a primeira não muda | Integração |
| TC-11 | CA-09 | Tabela de decisão | Solicitante e Administrador registram retirada e devolução; anônimo | 403, 403 e 401; nada gravado | Integração |
| TC-12 | CA-10 | Classe válida | Responsável e Administrador listam as retiradas | 200 em ordem | Integração |
| TC-13 | CA-10 | Classe inválida | Solicitante lista; reserva inexistente; material inexistente; retirada inexistente | 403; 404; 404; 404 | Integração |
| TC-14 | CA-11 | Valor limite | Retirada aberta e a lista consultada 1 s depois do término | `atrasada = true` | Integração |
| TC-15 | CA-01 | Classe inválida | Corpo sem `materialId` | 400 com o campo `materialId` | Integração |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| Retirar material sem reserva aprovada | Alto | TC-03 percorre os cinco estados proibidos |
| Atraso medido no limite errado | Médio | TC-08 e TC-09 testam os dois lados do término |
| Qualquer perfil registra | Alto | CA-09 e TC-11; a matriz de autorização ganha as linhas |
| A decisão DP-04 muda | Baixo | Está nesta spec e na matriz; a mudança é de uma regra de acesso |

## 8. Mini-plano de implementação (ciclos TDD)

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-01, TC-02 e TC-15: retirada | Migração da tabela `retirada` e `POST` da retirada | Nomear as restrições |
| 2 | TC-03 a TC-06: condições da retirada | Estado da reserva, material da reserva, uma retirada aberta e antes do término | Isolar as verificações |
| 3 | TC-07 a TC-10: devolução e atraso | `POST` da devolução e o cálculo de `atrasada` | Isolar o prazo de devolução |
| 4 | TC-12 a TC-14: consulta | `GET` das retiradas | Reaproveitar a resposta |
| 5 | TC-11: autorização | Regras de acesso (só Responsável registra) e linhas na matriz | Atualizar a matriz |

## 9. Revisão por pares

> Preenchida pelo revisor, com commit próprio. Nunca pelo autor.

- [x] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [x] As regras citam RNs que existem no PRD.
- [x] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [x] Os casos de teste incluem valores limite e entradas inválidas.
- [x] O contrato da API cobre os erros (400, 401, 403, 409).
- [x] Não há conflito com outras specs nem com os ADRs.

**Comentários:** Revisão feita pela autora (autoaprovação liberada pelo professor em 2026-11-10; revisor formal pelo rodízio: @Samwel-Merces). Conferi que cada critério tem caso de teste, que o prazo de devolução (término da reserva) tem os dois lados da fronteira testados e que a DP-04 fica decidida nesta spec.

**Parecer:** Aprovada · @renatarsilva · 2026-11-11

## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** 2026-11-11
- **Resultado:** APROVADA na spec validation

## 11. Grill-with-docs

> Obrigatório só para regra crítica. Nas demais, escreva "Não se aplica".

Não se aplica.

**Resultado:** Liberada · 2026-11-11
