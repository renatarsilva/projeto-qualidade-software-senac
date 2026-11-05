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

### CA-01: Entrada inválida (400)

- **Dado** que existe o endpoint de cadastro de sala (`POST /api/salas`)
- **Quando** o cliente envia uma requisição informando uma capacidade menor ou igual a 0 (ex.: 0 ou -1)
- **Então** o sistema retorna o status HTTP 400 Bad Request
- **E** o corpo traz, na lista `erros`, o campo inválido e a mensagem de negócio correspondente (ex.: "deve ser maior que zero")

### CA-02: Recurso inexistente (404)

- **Dado** que o endpoint de consulta de salas por ID está disponível
- **Quando** o cliente executa `GET /api/salas/{id}` com um identificador inexistente
- **Então** o sistema retorna o status HTTP 404 Not Found
- **E** o corpo traz a mensagem padrão em português: "Recurso não encontrado"

### CA-03: Conflito de regra de negócio (409)

- **Dado** que existe o mecanismo de tratamento de conflitos de negócio (como uma reserva com horário sobreposto ou uma exceção de conflito de teste)
- **Quando** o cliente executa a operação que gera o conflito de domínio
- **Então** o sistema retorna o status HTTP 409 Conflict
- **E** o corpo traz a explicação em linguagem de negócio, sem expor rastros técnicos do banco de dados

### CA-04: Erro inesperado (500 neutro)

- **Dado** que ocorre uma falha ou exceção não tratada no servidor durante o processamento da requisição
- **Quando** o sistema intercepta o erro inesperado
- **Então** o sistema retorna o status HTTP 500 Internal Server Error
- **E** o corpo traz obrigatoriamente o detalhe fixo: "Ocorreu um erro inesperado. Tente novamente mais tarde."
- **E** o corpo não contém nenhum dos termos proibidos: `Exception`, `SELECT`, `at com.` ou caminhos de diretório (`/src/`), que vão exclusivamente para o log do servidor

### CA-05: Tela de erro amigável (Thymeleaf)

- **Dado** que a pessoa usuária acessa uma rota da interface web renderizada pelo servidor
- **Quando** ocorre uma falha ou erro não tratado durante a requisição ou a renderização da página
- **Então** o servidor responde com uma página HTML de erro amigável, com orientações gerais para a pessoa
- **E** a página não exibe os termos proibidos do CA-04 (como `Exception`, rastros de pilha ou caminhos de diretório)

### CA-06: Formato de erro padronizado (RFC 9457, Problem Details)

- **Dado** que qualquer resposta de erro da API (400, 404, 409 ou 500) é disparada
- **Quando** o cliente recebe o corpo de erro do servidor
- **Então** o cabeçalho `Content-Type` é `application/problem+json`
- **E** o corpo JSON contém os campos do contrato: `type`, `title`, `status`, `detail` e `instance`

## 6. Casos de teste

Todo critério de aceite precisa de pelo menos um caso. Use classes de equivalência e valor limite.

Os testes de integração usam um controller de teste (só em `src/test`) que imita `/api/salas` e lança as exceções de cada situação. Assim a T-22 não depende da T-23 nem da entidade Sala. Lista de termos proibidos: `Exception`, `SELECT`, `at com.`, `/src/`.

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 | CA-01 | Valor limite | `POST` com `capacidade = 0` | 400; `erros` com `campo = capacidade` e `motivo = "deve ser maior que zero"` | Integração |
| TC-02 | CA-01 | Valor limite | `POST` com `capacidade = 1` | Sem erro: requisição aceita (limite válido) | Integração |
| TC-03 | CA-01 | Classe inválida | `POST` com `capacidade = -1` | 400; mesmo `motivo` do TC-01 | Integração |
| TC-04 | CA-01 | Classe inválida | `POST` sem o campo `capacidade` | 400; `erros` aponta `capacidade` | Integração |
| TC-05 | CA-02 | Classe inválida | `GET` com id inexistente | 404; `detail = "Recurso não encontrado"` | Integração |
| TC-06 | CA-03 | Classe inválida | Operação que lança a exceção de conflito de teste | 409; `detail` em linguagem de negócio, sem termo proibido | Integração |
| TC-07 | CA-04 | Classe inválida | Falha com `RuntimeException` cuja mensagem contém `SELECT * FROM salas` | 500; `detail` fixo; corpo sem termo proibido | Integração |
| TC-08 | CA-04 | Classe inválida | Falha de acesso a dados simulada, com SQL na mensagem | 500; `detail` fixo; corpo sem SQL nem nome de classe | Integração |
| TC-09 | CA-04 | Classe inválida | Mesma falha do TC-07, com o log capturado | O log registra o detalhe técnico; a resposta não | Integração |
| TC-10 | CA-05 | Classe inválida | Rota web de teste que falha, com `Accept: text/html` | Página HTML de erro amigável, sem termo proibido | Integração |
| TC-11 | CA-06 | Classe válida | Um erro de cada tipo: 400, 404, 409 e 500 (teste parametrizado) | `Content-Type = application/problem+json` e campos `type`, `title`, `status`, `detail`, `instance` | Integração |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| Detalhe interno vaza porque a mensagem da exceção é copiada para o `detail` | Alto | O `detail` do 500 é texto fixo; TC-07 e TC-08 verificam os termos proibidos |
| A lista de termos proibidos fica incompleta | Médio | Lista única compartilhada pelos testes, ampliada a cada novo vazamento; a medida do RNF-08 segue candidata |
| A página de erro padrão do Spring (Whitelabel) aparece nas telas | Médio | TC-10; configurar o Spring para nunca incluir stack trace nem mensagem de exceção nas respostas |
| A T-23 (regra de sobreposição) ainda não existe | Médio | TC-06 usa exceção de conflito de teste; a regra real é testada na T-20 |
| Mensagens de validação inconsistentes entre endpoints | Médio | Esta task entrega o mecanismo; cada task de funcionalidade define as suas mensagens |
| Respostas 401 e 403 fora do formato | Baixo | Fora do escopo (T-15), que deve seguir o formato do CA-06 |

## 8. Mini-plano de implementação (ciclos TDD)

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-01: `capacidade = 0` devolve 400 com campo e motivo | `@RestControllerAdvice` com o tratamento da validação, devolvendo `ProblemDetail` | Extrair a criação do `ProblemDetail` para um método único |
| 2 | TC-03 e TC-04: `-1` e campo ausente devolvem 400 | Ajustar a validação do DTO de teste (`@Min(1)`, `@NotNull`) | Nenhuma (TC-02 já passa e protege o limite válido) |
| 3 | TC-05: id inexistente devolve 404 | Exceção de recurso não encontrado e seu tratamento | Mover os textos fixos para constantes |
| 4 | TC-06: conflito devolve 409 | Exceção de conflito de regra e seu tratamento | Unificar os tratamentos de exceção de negócio |
| 5 | TC-07, TC-08 e TC-09: erro inesperado devolve 500 neutro e vai para o log | Tratamento genérico de `Exception` com texto fixo e log do detalhe | Garantir que nenhum tratamento use a mensagem da exceção no corpo |
| 6 | TC-10: rota web que falha devolve página amigável | Página de erro Thymeleaf e configuração do Spring contra vazamento | Reutilizar os textos fixos da API |
| 7 | TC-11: contrato dos quatro erros | Ajustes que faltarem no `type` e no `instance` | Consolidar o teste parametrizado |

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
