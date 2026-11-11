# Spec T-41: relatório de utilização

| Campo | Valor |
|---|---|
| Task | T-41 |
| Autor(a) | @renatarsilva |
| Revisor(a) | @Samwel-Merces |
| Status | Liberada |
| Regra crítica | Não |
| Requisitos | RF-12, NC-08 |
| Característica ISO 25010 | Adequação funcional |
| Criada em | 2026-11-11 |

## 1. Contexto e objetivo

O relatório de utilização mostra, para um período, as **horas reservadas por recurso**, a **carga horária alocada por professor** e os **conflitos evitados** (tentativas de reserva recusadas por sobreposição). Ele ajuda a justificar decisões sobre o uso dos recursos (RF-12). Esta spec define o cálculo, as bordas do período e quem consulta. Os valores esperados dos testes são **calculados à mão**, com a conta no comentário do teste, e não copiados da saída da aplicação.

## 2. Escopo

**Dentro:**
- `GET /api/relatorios/utilizacao?inicio=&fim=`: horas por recurso, carga horária por professor e total de conflitos evitados no período.
- O registro dos conflitos evitados: toda recusa por sobreposição (da aplicação ou do banco) deixa uma linha própria, gravada mesmo com a transação da reserva desfeita.
- Validação do período e acesso por perfil.

**Fora (e por quê):**
- Gráficos, exportação em arquivo e filtros por recurso: a especificação pede só os três números.
- Recusas por manutenção, antecedência ou intervalo inválido: não são conflito de horário e não contam.
- Telas: ficam para a interface.

## 3. Regras de negócio

- **RF-12:** relatório de utilização por recurso, carga horária alocada e conflitos evitados.
- **Reservas que contam:** as que não foram canceladas, rejeitadas nem marcadas como não comparecimento: `SOLICITADA`, `APROVADA`, `EM_USO` e `CONCLUIDA`. Reservas `CANCELADA`, `REJEITADA` e `NAO_COMPARECEU` **não contam**.
- **Horas no período:** só a parte da reserva que cai dentro de `[início, fim)` do relatório. Uma reserva que cruza a borda conta só pelo trecho de dentro. A unidade é hora, com até duas casas decimais.
- **Carga horária do professor:** a soma das horas das reservas que têm o professor como recurso (mesma regra de contagem e de borda).
- **Conflito evitado:** cada recusa por sobreposição (409 da regra ou do banco) conta 1, no instante em que a tentativa aconteceu (`Clock`). Entra no relatório se esse instante está em `[início, fim)`.
- **Perfil (NC-08, decisão desta spec):** o Responsável e o Administrador consultam. O Solicitante recebe 403.
- **Período:** `inicio` e `fim` são obrigatórios e `fim` vem depois de `inicio` (RN-01). Período sem reservas devolve as listas vazias e zero conflitos.
- **Ordem dos erros:** 401, 403, 400.

## 4. Contrato da API

`GET /api/relatorios/utilizacao?inicio=2026-11-09T00:00:00Z&fim=2026-11-16T00:00:00Z` (Responsável e Administrador):

```json
{ "inicio": "2026-11-09T00:00:00Z", "fim": "2026-11-16T00:00:00Z",
  "recursos": [{ "recursoId": 1, "tipo": "SALA", "nome": "sala-102", "horasReservadas": 3.5 }],
  "professores": [{ "professorId": 7, "nome": "renata-silva-12345", "horasAlocadas": 2.0 }],
  "conflitosEvitados": 4 }
```

Os recursos saem por nome; só entram os que têm horas no período.

| Situação | Status HTTP | Corpo |
|---|---|---|
| Sucesso | 200 | O relatório (listas vazias e zero se não há nada) |
| `inicio` ou `fim` ausente, inválido, ou `fim` não posterior a `inicio` | 400 | `ProblemDetail`, `erros` com o campo |
| Sem autenticação | 401 | `ProblemDetail` neutro (T-22) |
| Solicitante | 403 | `ProblemDetail` |

## 5. Critérios de aceite

### CA-01: Horas reservadas por recurso

- **Dado** reservas `APROVADA` e `CONCLUIDA` de duas salas dentro do período
- **Quando** o Responsável consulta o relatório
- **Então** cada sala traz a soma das horas das suas reservas, calculada à mão

### CA-02: Carga horária do professor

- **Dado** reservas que têm um professor como recurso
- **Quando** consulta o relatório
- **Então** o professor traz a soma das horas dessas reservas

### CA-03: Reservas que não contam

- **Dado** reservas `CANCELADA`, `REJEITADA` e `NAO_COMPARECEU` no período
- **Quando** consulta o relatório
- **Então** elas não entram nas horas

### CA-04: Reserva que cruza a borda do período

- **Dado** uma reserva que começa antes do `início` do relatório e termina dentro dele (e outra que começa dentro e termina depois do `fim`)
- **Quando** consulta o relatório
- **Então** só o trecho de dentro do período conta

### CA-05: Período vazio

- **Dado** um período sem reservas nem conflitos
- **Quando** consulta o relatório
- **Então** as listas vêm vazias e `conflitosEvitados` é 0

### CA-06: Conflitos evitados

- **Dado** tentativas de reserva recusadas por sobreposição no período
- **Quando** consulta o relatório
- **Então** `conflitosEvitados` é o número dessas recusas
- **E** uma recusa por manutenção ou antecedência não conta, e uma recusa fora do período também não

### CA-07: A recusa deixa registro mesmo sem reserva gravada

- **Dado** um pedido recusado por sobreposição
- **Quando** a resposta 409 é devolvida
- **Então** existe 1 registro do conflito evitado, e nenhuma reserva foi gravada

### CA-08: Acesso e validação

- **Dado** a rota do relatório
- **Quando** o Solicitante a chama, ou sem token, ou com período inválido
- **Então** recebe 403, 401 e 400 (com o campo), respectivamente
- **E** o Responsável e o Administrador recebem 200

## 6. Casos de teste

A massa é pequena e semeada no banco, com o `Clock` fixo (segunda, 2026-11-09, 08:00 UTC) e o período do relatório de segunda 09/11 às 00:00 a segunda 16/11 às 00:00, salvo indicação. A conta de cada valor esperado está no comentário do teste.

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 | CA-01 | Classe válida | Sala-102: terça 10:00-11:30 (`APROVADA`) e quarta 14:00-16:00 (`CONCLUIDA`); sala-101: terça 09:00-10:00 (`APROVADA`) | sala-102: 1,5 + 2 = 3,5 h; sala-101: 1 h | API caixa-preta |
| TC-02 | CA-02 | Classe válida | Professor X em duas reservas: 10:00-11:00 e 14:00-15:30 | 1 + 1,5 = 2,5 h | API caixa-preta |
| TC-03 | CA-03 | Classe inválida | Reservas `CANCELADA`, `REJEITADA` e `NAO_COMPARECEU` de 2 h cada, mais uma `SOLICITADA` de 1 h | Só a de 1 h conta | API caixa-preta |
| TC-04 | CA-04 | Valor limite | Reserva de domingo 08/11 23:00 a segunda 09/11 02:00 | 2 h (só o trecho de segunda, 00:00-02:00) | API caixa-preta |
| TC-05 | CA-04 | Valor limite | Reserva de domingo 15/11 23:00 a segunda 16/11 01:00 | 1 h (só o trecho até 00:00) | API caixa-preta |
| TC-06 | CA-05 | Classe válida | Período sem reservas nem conflitos | 200; listas vazias; `conflitosEvitados` = 0 | API caixa-preta |
| TC-07 | CA-06 | Classe válida | 3 pedidos recusados por sobreposição pela API | `conflitosEvitados` = 3 | API caixa-preta |
| TC-08 | CA-06 | Classe inválida | 1 recusa por manutenção e 1 por antecedência, mais 1 por sobreposição | `conflitosEvitados` = 1 | API caixa-preta |
| TC-09 | CA-06 | Valor limite | Recusa registrada antes do `início` do período | Não conta | API caixa-preta |
| TC-10 | CA-07 | Classe válida | Um pedido recusado por sobreposição | 1 registro em `conflito_evitado` e nenhuma reserva nova | API caixa-preta |
| TC-11 | CA-08 | Tabela de decisão | Solicitante; sem token; Responsável; Administrador | 403; 401; 200; 200 | API caixa-preta |
| TC-12 | CA-08 | Classe inválida | `inicio` ausente; `fim` ausente; `fim` igual ao `inicio`; data inválida | 400 com o campo | API caixa-preta |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| Os valores esperados são copiados da saída da aplicação | Alto | A conta fica no comentário de cada teste, feita antes à mão |
| Reserva cruzando a borda conta inteira | Médio | TC-04 e TC-05 |
| Reserva cancelada entra nas horas | Alto | TC-03 |
| A recusa some com o rollback da reserva | Alto | O registro do conflito é gravado em transação própria (CA-07, TC-10) |
| Qualquer perfil lê o relatório | Alto | CA-08, TC-11 e a matriz de autorização |

## 8. Mini-plano de implementação (ciclos TDD)

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-06 e TC-12: período vazio e validação | `GET /api/relatorios/utilizacao` com o formato e o 400 | Nomear a resposta |
| 2 | TC-01 e TC-03: horas por recurso | Soma das horas por recurso, só das reservas que contam | Extrair a consulta |
| 3 | TC-04 e TC-05: bordas | Recorte da reserva ao período | Nomear a regra da borda |
| 4 | TC-02: professor | Carga horária por professor | Reaproveitar a consulta de horas |
| 5 | TC-07 a TC-10: conflitos evitados | Tabela `conflito_evitado` e o registro em transação própria | Isolar o registro |
| 6 | TC-11: autorização | Regra de acesso (Responsável e Administrador) e linha na matriz | Atualizar a matriz |

## 9. Revisão por pares

> Preenchida pelo revisor, com commit próprio. Nunca pelo autor.

- [x] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [x] As regras citam RNs que existem no PRD.
- [x] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [x] Os casos de teste incluem valores limite e entradas inválidas.
- [x] O contrato da API cobre os erros (400, 401, 403, 409).
- [x] Não há conflito com outras specs nem com os ADRs.

**Comentários:** Revisão feita pela autora (autoaprovação liberada pelo professor em 2026-11-10; revisor formal pelo rodízio: @Samwel-Merces). Conferi que cada caso tem a conta do oráculo, que as bordas do período têm os dois lados e que o registro do conflito não some com o rollback.

**Parecer:** Aprovada · @renatarsilva · 2026-11-11

## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** 2026-11-11
- **Resultado:** APROVADA na spec validation

## 11. Grill-with-docs

> Obrigatório só para regra crítica. Nas demais, escreva "Não se aplica".

Não se aplica.

**Resultado:** Liberada · 2026-11-11
