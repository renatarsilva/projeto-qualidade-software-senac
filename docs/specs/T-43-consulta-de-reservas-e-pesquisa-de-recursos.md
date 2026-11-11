# Spec T-43: consulta de reservas e pesquisa de recursos

| Campo | Valor |
|---|---|
| Task | T-43 |
| Autor(a) | @renatarsilva |
| Revisor(a) | @Samwel-Merces |
| Status | Liberada |
| Regra crítica | Não |
| Requisitos | RF-03, RF-10 (consulta) |
| Característica ISO 25010 | Adequação funcional |
| Criada em | 2026-11-10 |

## 1. Contexto e objetivo

O Solicitante consulta a disponibilidade antes de pedir uma reserva (RF-03) e precisa ver as próprias reservas. Hoje a API não tem nenhuma leitura de reservas (divergência D-01 da matriz de autorização) nem pesquisa de recursos além de listar salas, professores e materiais separadamente. Esta spec define a **pesquisa de recursos** por tipo, capacidade, localização, competência e disponibilidade, e a **consulta de reservas** (lista e por id), com a posse respeitada.

## 2. Escopo

**Dentro:**
- `GET /api/recursos` com filtros combinados (E): tipo, capacidade mínima, localização, competência e disponibilidade num intervalo.
- `GET /api/reservas` (lista, com filtro opcional de estado) e `GET /api/reservas/{id}`.
- Validação dos filtros (400) e o formato das respostas.

**Fora (e por quê):**
- Exclusão de recursos em **manutenção** na disponibilidade: a manutenção (RN-04) é da T-35. Quando ela entrar, a disponibilidade passa a excluí-los (o CA-06 fica registrado como pendente da T-35).
- Paginação e ordenação configurável: o volume é pequeno; a ordem é fixa (por nome, e por início nas reservas).
- Histórico de mudanças de estado: é a auditoria (T-40).

## 3. Regras de negócio

- **RF-03:** a pesquisa combina os filtros por E: o recurso precisa atender todos os informados. Sem filtro, devolve todos os recursos.
- **Filtro de um tipo só devolve aquele tipo:** capacidade mínima e localização valem só para salas; competência vale só para professores. Se o filtro `tipo` for de outro tipo, o resultado é vazio.
- **Localização e competência:** contêm o texto, sem diferenciar maiúsculas.
- **Disponibilidade:** com `disponivelDe` e `disponivelAte`, o recurso só aparece se **não** tem reserva que ocupa o horário (`SOLICITADA`, `APROVADA` ou `EM_USO`, S-02) sobreposta ao intervalo `[início, fim)` (S-01). Os dois parâmetros vêm juntos, e o término é posterior ao início (RN-01).
- **Posse na consulta de reservas (NC-01):** o Solicitante vê só as próprias reservas; o Responsável e o Administrador veem todas. O Solicitante que pede a reserva de outro por id recebe 403.
- **Ordem dos erros:** 401, 403 por perfil, 404, 403 por dono, 400.

## 4. Contrato da API

`GET /api/recursos?tipo=SALA&capacidadeMinima=30&localizacao=bloco&competencia=java&disponivelDe=2026-11-10T10:00:00Z&disponivelAte=2026-11-10T11:00:00Z` (qualquer usuário autenticado)

```json
[{ "id": 1, "tipo": "SALA", "nome": "sala-102", "restrito": false, "capacidade": 40, "localizacao": "Bloco B", "competencias": null }]
```

`GET /api/reservas?estado=APROVADA` (Solicitante: as próprias; Responsável e Administrador: todas) e `GET /api/reservas/{id}`: a reserva no mesmo formato do `POST` (T-20).

| Situação | Status HTTP | Corpo |
|---|---|---|
| Sucesso | 200 | Lista (pode ser vazia) ou a reserva |
| Filtro inválido (tipo, capacidade, estado, intervalo) | 400 | `ProblemDetail`, `erros` com o campo e o motivo |
| Sem autenticação | 401 | `ProblemDetail` neutro (T-22) |
| `GET /api/reservas/{id}` de outro Solicitante | 403 | `ProblemDetail` |
| Reserva que não existe | 404 | "Recurso não encontrado" |

## 5. Critérios de aceite

### CA-01: Pesquisa sem filtro

- **Dado** recursos cadastrados dos três tipos
- **Quando** o usuário autenticado pesquisa sem filtro
- **Então** recebe todos os recursos, ordenados por nome, com 200

### CA-02: Filtro por tipo

- **Dado** salas, professores e materiais cadastrados
- **Quando** pesquisa com `tipo=SALA`, `PROFESSOR` ou `MATERIAL`
- **Então** recebe só os recursos daquele tipo

### CA-03: Capacidade e localização (salas)

- **Dado** salas com capacidades e localizações diferentes
- **Quando** pesquisa com `capacidadeMinima` e `localizacao`
- **Então** recebe as salas com capacidade maior ou igual ao valor (limite inclusivo) e localização que contém o texto, sem diferenciar maiúsculas
- **E** professores e materiais não aparecem

### CA-04: Competência (professores)

- **Dado** professores com competências diferentes
- **Quando** pesquisa com `competencia`
- **Então** recebe os professores que têm uma competência que contém o texto, sem diferenciar maiúsculas
- **E** salas e materiais não aparecem

### CA-05: Disponibilidade no intervalo

- **Dado** uma reserva que ocupa uma sala das 10:00 às 11:00
- **Quando** pesquisa a disponibilidade em 10:30 às 11:30, ou em 11:00 às 12:00
- **Então** no primeiro intervalo a sala não aparece (sobreposta) e no segundo ela aparece (reservas encostadas não conflitam)
- **E** reservas `CANCELADA`, `REJEITADA`, `NAO_COMPARECEU` e `CONCLUIDA` não tiram o recurso da lista

### CA-06: Disponibilidade respeita manutenção

- **Dado** um recurso em manutenção no intervalo
- **Quando** pesquisa a disponibilidade
- **Então** o recurso não aparece
- **E** este critério fica pendente até a T-35 (hoje não existe manutenção)

### CA-07: Filtros combinados e inválidos

- **Dado** qualquer combinação de filtros
- **Quando** o usuário os informa juntos
- **Então** o resultado atende a todos (E); `tipo` inexistente, capacidade que não é inteiro maior que zero, só um dos dois limites do intervalo, ou término que não é posterior ao início recebem 400 com o campo e o motivo

### CA-08: Consulta das próprias reservas

- **Dado** reservas de dois Solicitantes
- **Quando** um Solicitante lista as reservas
- **Então** recebe só as dele, ordenadas pelo início, e o filtro `estado` restringe a lista (estado que não existe é 400)

### CA-09: Responsável e Administrador veem todas

- **Dado** reservas de vários Solicitantes
- **Quando** o Responsável ou o Administrador lista as reservas
- **Então** recebem todas

### CA-10: Reserva por id e posse

- **Dado** uma reserva de um Solicitante
- **Quando** o dono, o Responsável ou o Administrador a consulta por id
- **Então** recebe 200 com a reserva
- **E** outro Solicitante recebe 403 e um id que não existe recebe 404

## 6. Casos de teste

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 | CA-01 | Classe válida | Pesquisa sem filtro com 1 sala, 1 professor e 1 material | 200 com os 3, ordenados por nome | Integração |
| TC-02 | CA-02 | Classe equivalente | `tipo=SALA`, `PROFESSOR` e `MATERIAL` | Só o tipo pedido em cada um | Integração |
| TC-03 | CA-03 | Valor limite | Salas de 29, 30 e 31 lugares; `capacidadeMinima=30` | As de 30 e 31 | Integração |
| TC-04 | CA-03 | Classe válida | `localizacao=BLOCO` com salas no "Bloco B" e no "Anexo" | Só a do Bloco B | Integração |
| TC-05 | CA-03 | Classe inválida | `capacidadeMinima=30` sem tipo, com professores e materiais cadastrados | Só salas | Integração |
| TC-06 | CA-04 | Classe válida | `competencia=jav` com professores de Java e de Banco de Dados | Só o de Java | Integração |
| TC-07 | CA-04 | Classe inválida | `tipo=SALA` e `competencia=java` | Lista vazia | Integração |
| TC-08 | CA-05 | Valor limite | Sala reservada 10:00-11:00; disponibilidade 10:30-11:30 | A sala não aparece | Integração |
| TC-09 | CA-05 | Valor limite | Mesma sala; disponibilidade 11:00-12:00 (e 09:00-10:00) | A sala aparece nos dois | Integração |
| TC-10 | CA-05 | Classe equivalente | Reserva `CANCELADA` no horário; disponibilidade igual | A sala aparece | Integração |
| TC-11 | CA-05 | Classe válida | Professor e material reservados no horário | Eles também somem da pesquisa por disponibilidade | Integração |
| TC-12 | CA-07 | Tabela de decisão | `tipo=XYZ`; `capacidadeMinima=0`, `-1` e `abc`; só `disponivelDe`; término antes do início | 400 com o campo de cada erro | Integração |
| TC-13 | CA-07 | Classe válida | Tipo, capacidade, localização e disponibilidade juntos | Só o que atende a todos | Integração |
| TC-14 | CA-08 | Conflito | Solicitantes A e B com reservas; A lista | Só as de A, ordenadas pelo início | Integração |
| TC-15 | CA-08 | Classe equivalente | A lista com `estado=APROVADA`; e com `estado=ABC` | Só as aprovadas; 400 no `ABC` | Integração |
| TC-16 | CA-09 | Classe válida | Responsável e Administrador listam | Veem as de A e de B | Integração |
| TC-17 | CA-10 | Classe válida | Dono, Responsável e Administrador consultam a reserva por id | 200 nos três | Integração |
| TC-18 | CA-10 | Conflito | O Solicitante B consulta a reserva de A | 403 | Integração |
| TC-19 | CA-10 | Classe inválida | `GET /api/reservas/999999` | 404 (e 403 para quem não tem perfil, se houver) | Integração |
| TC-20 | CA-06 | Pendente | Recurso em manutenção no intervalo | Não aparece (entra com a T-35) | Integração |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| A disponibilidade devolve recurso ocupado (falso disponível) | Alto | TC-08 a TC-11 testam as fronteiras e todos os tipos |
| O Solicitante vê a reserva de outro | Alto | CA-08 e CA-10; a matriz de autorização exige as linhas novas |
| Filtro numérico quebra com texto | Médio | TC-12 |
| Manutenção ainda não existe | Médio | CA-06 pendente da T-35, registrado |

## 8. Mini-plano de implementação (ciclos TDD)

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-01 e TC-02: sem filtro e por tipo | `GET /api/recursos` com consulta aos três tipos | Extrair a consulta |
| 2 | TC-03 a TC-07: capacidade, localização e competência | Filtros por tipo de recurso | Montar a consulta a partir dos filtros |
| 3 | TC-08 a TC-11: disponibilidade | Excluir recursos com reserva ativa sobreposta | Reaproveitar a regra de ocupação |
| 4 | TC-12 e TC-13: filtros inválidos e combinados | Validação e combinação por E | Erros no formato da T-22 |
| 5 | TC-14 a TC-16: lista de reservas | `GET /api/reservas` com posse por perfil | Isolar a visão por perfil |
| 6 | TC-17 a TC-19: reserva por id | `GET /api/reservas/{id}` com 403 e 404 | Reaproveitar a regra de dono |
| 7 | Matriz de autorização: linhas novas | Regras de acesso das rotas novas | Atualizar a matriz e o teste |

## 9. Revisão por pares

> Preenchida pelo revisor, com commit próprio. Nunca pelo autor.

- [x] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [x] As regras citam RNs que existem no PRD.
- [x] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [x] Os casos de teste incluem valores limite e entradas inválidas.
- [x] O contrato da API cobre os erros (400, 401, 403, 409).
- [x] Não há conflito com outras specs nem com os ADRs.

**Comentários:** Revisão feita pela autora (autoaprovação liberada pelo professor em 2026-11-10; o revisor formal pelo rodízio segue sendo @Samwel-Merces). Conferi que cada critério tem caso de teste, que a manutenção fica pendente da T-35 de forma explícita e que a posse na consulta de reservas segue a NC-01.

**Parecer:** Aprovada · @renatarsilva · 2026-11-10

## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** 2026-11-10
- **Resultado:** APROVADA na spec validation

## 11. Grill-with-docs

> Obrigatório só para regra crítica. Nas demais, escreva "Não se aplica".

Não se aplica.

**Resultado:** Liberada · 2026-11-10
