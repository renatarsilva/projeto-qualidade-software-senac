# Spec KAN-00: _preencher_ título curto

<!--
Como usar: copie este arquivo para docs/specs/<CHAVE-JIRA>-<descricao-curta>.md
e substitua todo trecho _preencher_. O fluxo completo está em docs/processo/ai-workflow.md.

Status possíveis, nesta ordem:
Rascunho → Em revisão → Aprovada → Validada → Liberada
(tasks não críticas vão de Validada direto para Liberada, sem grill)
-->

| Campo | Valor |
|---|---|
| Task (Jira) | KAN-00 · T-00 |
| Autor(a) | @_preencher_ |
| Revisor(a) | @_preencher_ |
| Status | Rascunho |
| Regra crítica | Sim / Não |
| Requisitos | RF00, RN00 |
| Característica ISO 25010 | _preencher_ (ex.: Adequação Funcional, Confiabilidade, Segurança) |
| Criada em | AAAA-MM-DD |

## 1. Contexto e objetivo

_preencher_: qual problema esta task resolve e para qual perfil (Solicitante, Responsável ou Administrador). Duas ou três frases.

## 2. Escopo

**Dentro:**
- _preencher_

**Fora (e por quê):**
- _preencher_

## 3. Regras de negócio

Liste as regras com o ID do PRD. Se a regra ainda não existe no PRD, ela entra primeiro no PRD.

- **RN00:** _preencher_

## 4. Contrato da API

`MÉTODO /caminho`: _preencher_ (quem pode chamar)

**Requisição:**

```json
{ "_preencher_": "" }
```

**Respostas:**

| Situação | Status HTTP | Corpo |
|---|---|---|
| Sucesso | 201 | _preencher_ |
| Entrada inválida | 400 | ProblemDetail com mensagem compreensível |
| Sem autenticação | 401 | _preencher_ |
| Perfil sem permissão | 403 | _preencher_ |
| Conflito de regra | 409 | _preencher_ |

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
