# Spec T-22: tratamento padronizado de erros e validação de entrada

<!--
Como usar: copie este arquivo para docs/specs/<ID-DA-TASK>-<descricao-curta>.md
e substitua todo trecho _preencher_. O fluxo completo está em docs/processo/ai-workflow.md.

Status possíveis, nesta ordem:
Rascunho → Em revisão → Aprovada → Validada → Liberada
(tasks não críticas vão de Validada direto para Liberada, sem grill)
-->

| Campo | Valor |
|---|---|
| Task | T-22 |
| Autor(a) | @renatarsilva |
| Revisor(a) | @Samwel-Merces |
| Status | Rascunho |
| Regra crítica | Não |
| Requisitos | RF-13, RNF-07, RNF-08 |
| Característica ISO 25010 | Segurança |
| Criada em | 2026-11-02 |

## 1. Contexto e objetivo

Hoje cada endpoint poderia devolver erros em um formato diferente, e uma falha inesperada poderia expor detalhes internos, como stack trace, SQL ou nome de classe. Esta task cria **um jeito único** de responder a erros, na API e nas telas, para os três perfis (Solicitante, Responsável e Administrador). O objetivo é que toda mensagem seja compreensível (RF-13) e que nenhum erro vaze detalhe interno (RNF-08).

## 2. Escopo

**Dentro:**
- Formato único de erro da API (`ProblemDetail`, RFC 9457), com mensagem compreensível em português.
- `400` para entrada inválida, com o campo e o motivo (RNF-07).
- `404` para recurso não encontrado e `409` para conflito de regra.
- Erro inesperado vira `500` neutro, sem stack trace, SQL, nome de classe nem caminho interno (RNF-08). O detalhe técnico fica só no log.
- Página de erro amigável nas telas.

**Fora (e por quê):**
- `401` e `403` (autenticação e autorização): são da T-15.
- As regras de negócio que geram o `409`, como a sobreposição de horários: são da T-23 e da T-24. Esta task só define o **formato** da resposta.
- As validações de cada campo de cada endpoint: cada task de funcionalidade define as suas. Esta task entrega o mecanismo.
- O conteúdo e o visual das telas: são da T-44 e da T-45.

## 3. Regras de negócio

Liste as regras com o ID do PRD. Se a regra ainda não existe no PRD, ela entra primeiro no PRD.

Esta task não cria regra de negócio: ela aplica requisitos do PRD.

- **RF-13:** interface responsiva e **mensagens de erro compreensíveis**.
- **RNF-07 (validação de entrada):** toda entrada inválida catalogada é recusada, e nenhuma gravação inválida acontece. A exigência é oficial; a medida (100 % do catálogo) é candidata, com validação pendente.
- **RNF-08 (tratamento seguro de erros):** nenhuma resposta de erro contém stack trace, SQL, nome de classe nem caminho interno.

## 4. Contrato da API

Esta task não cria um endpoint: ela define **como todos os endpoints respondem a erro**. Toda resposta de erro tem o tipo `application/problem+json` (RFC 9457), com mensagens em português, e vale para os três perfis.

**Formato:**

| Campo | Conteúdo |
|---|---|
| `type` | Identifica o tipo do erro |
| `title` | Título curto e compreensível |
| `status` | O código HTTP |
| `detail` | A explicação para a pessoa, sem detalhe interno |
| `instance` | O caminho da requisição |
| `erros` | Só no `400`: a lista de campos inválidos, cada um com `campo` e `motivo` |

**Exemplo de entrada inválida (`400`):**

```json
{
  "type": "about:blank",
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Um ou mais campos estão inválidos.",
  "instance": "/api/salas",
  "erros": [
    { "campo": "capacidade", "motivo": "deve ser maior que zero" }
  ]
}
```

**Respostas:**

| Situação | Status HTTP | Corpo |
|---|---|---|
| Entrada inválida | 400 | `ProblemDetail` com a lista `erros` (campo e motivo) |
| Recurso não encontrado | 404 | `ProblemDetail`: "Recurso não encontrado" |
| Conflito de regra | 409 | `ProblemDetail` com o motivo do conflito, em linguagem de negócio |
| Erro inesperado | 500 | `ProblemDetail` neutro: "Ocorreu um erro inesperado. Tente novamente mais tarde." Sem stack trace, SQL, nome de classe nem caminho |
| Sem autenticação e perfil sem permissão | 401 e 403 | Fora desta task (T-15), mas devem seguir o mesmo formato |

Nas telas, os mesmos erros aparecem como uma página de erro amigável, sem detalhe técnico.

## 5. Critérios de aceite

### CA-01: _preencher_ nome do comportamento

- **Dado** _preencher_
- **Quando** _preencher_
- **Então** _preencher_

### CA-02: _preencher_

- **Dado** _preencher_
- **Quando** _preencher_
- **Então** _preencher_

## 6. Casos de teste

Todo critério de aceite precisa de pelo menos um caso. Use classes de equivalência e valor limite.

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 | CA-01 | Valor limite | _preencher_ | _preencher_ | Unitário |
| TC-02 | CA-02 | Classe inválida | _preencher_ | _preencher_ | Integração |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| _preencher_ | Alto / Médio / Baixo | _preencher_ |

## 8. Mini-plano de implementação (ciclos TDD)

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-01: _preencher_ | _preencher_ | _preencher_ |
| 2 | TC-02: _preencher_ | _preencher_ | _preencher_ |

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
