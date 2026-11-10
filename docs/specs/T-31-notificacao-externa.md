# Spec T-31: notificação externa isolada e testada com WireMock

<!--
Como usar: copie este arquivo para docs/specs/<ID-DA-TASK>-<descricao-curta>.md
e substitua todo trecho _preencher_. O fluxo completo está em docs/processo/ai-workflow.md.

Status possíveis, nesta ordem:
Rascunho → Em revisão → Aprovada → Validada → Liberada
(tasks não críticas vão de Validada direto para Liberada, sem grill)
-->

| Campo | Valor |
|---|---|
| Task | T-31 |
| Autor(a) | @Samwel-Merces |
| Revisor(a) | @CaiosAguiar |
| Status | Em revisão |
| Regra crítica | Não |
| Requisitos | RF-11, RNF-10 |
| Característica ISO 25010 | Confiabilidade |
| Criada em | 2026-11-09 |

## 1. Contexto e objetivo

O RF-11 pede "notificação simulada ou integração com API externa". O Solicitante quer saber quando o pedido dele foi aprovado, rejeitado ou cancelado (NC-03). Esta task cria a **porta de notificação** e o **adaptador HTTP** para uma API externa, isolados do fluxo da reserva: uma falha da API (HTTP 500, 404, timeout, resposta inválida ou conexão recusada) nunca desfaz a reserva nem vaza detalhe interno (RNF-10, CEN-03, CT-03).

## 2. Escopo

**Dentro:**
- A porta `Notificador` (interface) e o pedido `Notificacao` (reserva, evento, destinatário e instante).
- Os eventos que notificam: `APROVADA`, `REJEITADA` e `CANCELADA` (decisão DP-03, proposta do backlog; ver P-01).
- O adaptador `NotificadorHttp`, que faz `POST {url-base}/notificacoes` com o token no cabeçalho `Authorization: Bearer`.
- URL base, token e tempos limite (conexão 500 ms, leitura 1500 ms) **só por configuração** (DP-08, MC-04, MC-05). O token vem de variável de ambiente.
- O modo **simulado**: sem URL configurada, a aplicação usa `NotificadorEmLog`, que só registra o aviso no log (RF-11 aceita "simulada"). Assim a aplicação sobe sem a API externa.
- O ouvinte `OuvinteDeNotificacao`, que recebe o evento `ReservaNotificavel` **depois do commit** da transação e chama o `Notificador`. A T-24 e a T-34 só precisam publicar esse evento.
- A matriz INT-01 a INT-05 com WireMock, mais a conexão recusada e o vazamento do token.

**Fora (e por quê):**
- Publicar o evento nas transições de estado: depende da T-24 (ciclo de vida) e da T-34 (aprovação), que ainda não existem. Esta spec entrega o contrato (`ReservaNotificavel`) e a T-24/T-34 fazem a chamada de uma linha. O teste ponta a ponta do CT-03 (aprovar a RESERVA-003 com o WireMock falhando) fica para a T-34, usando esta integração.
- Reenvio, fila ou retentativa de notificações que falharam: fora de escopo (FE-05, risco aceito R-07).
- Guardar notificações no banco: não há tabela (arquitetura, seção do modelo de dados: "o aviso é enviado e não guardado").
- Envio real por e-mail, SMS ou push (FE-04) e o provedor real (NT-01 do plano de teste).
- Envio assíncrono (fila ou thread separada): a chamada é síncrona, depois do commit, limitada pelos tempos limite. Ver risco R3.

## 3. Regras de negócio

- **RF-11:** notificação simulada ou integração com API externa.
- **RNF-10:** a falha da notificação (HTTP 500 ou timeout) não desfaz a reserva: 0 reversões. Tempos limite configuráveis, nunca fixos no código: MC-04 (conexão 500 ms) e MC-05 (leitura 1500 ms).
- **DP-03 (proposta desta spec):** notificam `APROVADA`, `REJEITADA` e `CANCELADA`. Outras transições (`EM_USO`, `CONCLUIDA`, `NAO_COMPARECEU`) não notificam.
- **DP-08 (proposta desta spec):** os valores de MC-04 e MC-05 ficam no `application.properties` e podem ser trocados por variável de ambiente.

## 4. Contrato da API

Esta task **não cria endpoint** no sistema. O contrato abaixo é o da chamada **para fora**, para a API de notificações.

`POST {app.notificacao.url-base}/notificacoes`

**Cabeçalhos:** `Content-Type: application/json`, `Accept: application/json`, `Authorization: Bearer {app.notificacao.token}`

**Requisição:**

```json
{
  "reservaId": 3,
  "evento": "APROVADA",
  "destinatario": "marina@senac.br",
  "ocorridaEm": "2026-11-12T12:00:00Z"
}
```

**Respostas da API externa e o que o sistema faz:**

| Situação | Resposta da API externa | Resultado no sistema | Log |
|---|---|---|---|
| Sucesso | `2xx` com JSON `{"id": "<texto não vazio>"}` | `ENVIADA`, com o `id` | `INFO` com reserva e evento |
| Recurso não encontrado | `404` | `FALHOU`, causa `HTTP_4XX` | 1 `WARN` com reserva e causa |
| Erro do provedor | `5xx` | `FALHOU`, causa `INDISPONIVEL` | 1 `WARN` com reserva e causa |
| Demora | sem resposta em 1500 ms | `FALHOU`, causa `TIMEOUT` | 1 `WARN` com reserva e causa |
| Resposta inválida | `2xx` com JSON malformado, sem `id` ou com `id` vazio | `FALHOU`, causa `RESPOSTA_INVALIDA` | 1 `WARN` com reserva e causa |
| Sem conexão | conexão recusada ou acima de 500 ms | `FALHOU`, causa `CONEXAO` | 1 `WARN` com reserva e causa |

O `Notificador` **nunca lança exceção** para quem chama: toda falha vira um resultado `FALHOU` com a causa. O log de falha nunca traz o token, o corpo de erro do provedor, a URL com credenciais nem o stack trace da biblioteca HTTP.

**Configuração (`application.properties`):**

```properties
app.notificacao.url-base=${APP_NOTIFICACAO_URL:}
app.notificacao.token=${APP_NOTIFICACAO_TOKEN:}
app.notificacao.timeout-conexao=${APP_NOTIFICACAO_TIMEOUT_CONEXAO:500ms}
app.notificacao.timeout-leitura=${APP_NOTIFICACAO_TIMEOUT_LEITURA:1500ms}
```

Com `url-base` vazia, o sistema usa o `NotificadorEmLog` (modo simulado).

## 5. Critérios de aceite

### CA-01: Notificação enviada com sucesso

- **Dado** a API externa responde `200` com `{"id": "n-1"}`
- **Quando** o sistema notifica a aprovação da reserva 3
- **Então** o resultado é `ENVIADA` com o id `n-1`
- **E** a API recebeu exatamente 1 `POST /notificacoes` com o JSON do contrato e o cabeçalho `Authorization: Bearer <token configurado>`

### CA-02: Erro do cliente (404) vira falha controlada

- **Dado** a API externa responde `404`
- **Quando** o sistema notifica
- **Então** o resultado é `FALHOU` com causa `HTTP_4XX`, sem exceção para quem chamou

### CA-03: Erro do provedor (5xx) vira indisponibilidade controlada

- **Dado** a API externa responde `500`
- **Quando** o sistema notifica
- **Então** o resultado é `FALHOU` com causa `INDISPONIVEL` (o "503 controlado" do backlog: o sistema trata como serviço indisponível e não repassa o 500; ver P-02)
- **E** nenhuma exceção chega a quem chamou

### CA-04: Timeout de leitura respeita o limite configurado

- **Dado** a API externa demora 2000 ms para responder e o tempo limite de leitura é 1500 ms
- **Quando** o sistema notifica
- **Então** o resultado é `FALHOU` com causa `TIMEOUT`
- **E** a chamada termina antes de 2000 ms (o sistema não espera a resposta atrasada)

### CA-05: Resposta inválida é rejeitada

- **Dado** a API externa responde `200` com corpo que não é JSON válido, ou JSON sem `id`, ou `id` vazio
- **Quando** o sistema notifica
- **Então** o resultado é `FALHOU` com causa `RESPOSTA_INVALIDA`, e não `ENVIADA`

### CA-06: Conexão recusada vira falha controlada

- **Dado** não há nada escutando na URL configurada
- **Quando** o sistema notifica
- **Então** o resultado é `FALHOU` com causa `CONEXAO`

### CA-07: O token e o erro do provedor não vazam

- **Dado** a API externa responde `500` com um corpo que contém o próprio token e um stack trace
- **Quando** o sistema notifica
- **Então** nem o resultado nem o log contêm o token, o corpo de erro do provedor ou o nome de classe da biblioteca HTTP

### CA-08: Exatamente um registro observável por falha

- **Dado** qualquer uma das falhas dos CA-02 a CA-06
- **Quando** o sistema notifica
- **Então** o log tem exatamente 1 linha de falha da notificação, com o id da reserva e a causa (CEN-03, CT-03 passo 5)

### CA-09: Tudo vem da configuração

- **Dado** a URL, o token e os tempos limite vêm de `app.notificacao.*`
- **Quando** a aplicação sobe
- **Então** o adaptador usa esses valores, e não há URL, token ou tempo limite fixo no código
- **E** sem `url-base` configurada, o `Notificador` ativo é o `NotificadorEmLog`, que devolve `ENVIADA` e registra o aviso no log sem chamada externa

### CA-10: Notificação só depois do commit, e a falha não desfaz nada

- **Dado** uma transação que grava um registro e publica `ReservaNotificavel`
- **Quando** a transação é confirmada e a notificação falha
- **Então** o registro continua gravado (0 reversões, RNF-10)
- **E** quando a transação é desfeita, nenhuma notificação é enviada

### CA-11: Só os eventos da DP-03 notificam

- **Dado** o tipo `EventoDeNotificacao`
- **Quando** alguém tenta criar uma notificação com outro estado (por exemplo `EM_USO`)
- **Então** ele aceita só `APROVADA`, `REJEITADA` e `CANCELADA`

## 6. Casos de teste

A suíte usa o WireMock em processo, numa porta livre: roda offline e no CI, sem Docker, com exceção do TC-16 (banco real, Testcontainers). Os tokens dos testes são valores falsos criados no próprio teste.

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 (INT-01) | CA-01 | Classe válida | `200` com `{"id":"n-1"}` | `ENVIADA`, id `n-1`; WireMock verifica 1 `POST /notificacoes` com os 4 campos do JSON e o `Authorization: Bearer` | Integração (WireMock) |
| TC-02 | CA-01 | Valor limite | `201` e `202` com `{"id":"n-1"}` | `ENVIADA` (qualquer `2xx` válido) | Integração (WireMock) |
| TC-03 (INT-02) | CA-02 | Classe inválida | `404` | `FALHOU`/`HTTP_4XX`; nenhuma exceção | Integração (WireMock) |
| TC-04 (INT-03) | CA-03 | Classe inválida | `500` | `FALHOU`/`INDISPONIVEL`; nenhuma exceção | Integração (WireMock) |
| TC-05 | CA-03 | Valor limite | `503` e `599` | `FALHOU`/`INDISPONIVEL` | Integração (WireMock) |
| TC-06 (INT-04) | CA-04 | Valor limite | atraso de 2000 ms, leitura 1500 ms | `FALHOU`/`TIMEOUT`; duração da chamada < 2000 ms | Integração (WireMock) |
| TC-07 | CA-04 | Valor limite | atraso de 1000 ms, leitura 1500 ms | `ENVIADA` (dentro do limite) | Integração (WireMock) |
| TC-08 (INT-05) | CA-05 | Classe inválida | `200` com `{id: ` (malformado) | `FALHOU`/`RESPOSTA_INVALIDA` | Integração (WireMock) |
| TC-09 | CA-05 | Classe inválida | `200` com `{}` e com `{"id":""}` | `FALHOU`/`RESPOSTA_INVALIDA` | Integração (WireMock) |
| TC-10 | CA-06 | Classe inválida | URL numa porta fechada | `FALHOU`/`CONEXAO`; nenhuma exceção | Integração |
| TC-11 | CA-07 | Classe inválida | `500` com corpo `{"erro":"token tk-falso-123 inválido","trace":"java.lang.IllegalStateException at ..."}` | resultado e log sem `tk-falso-123`, sem `IllegalStateException`, sem o corpo do provedor | Integração (WireMock) |
| TC-12 | CA-09 | Classe válida | `url-base`, `token` e tempos limite de teste nas propriedades | o WireMock recebe na porta e com o token configurados; o TC-06 falha com outro tempo limite de leitura (prova que vem da configuração) | Integração (WireMock) |
| TC-13 | CA-09 | Classe válida | contexto sem `app.notificacao.url-base` | o bean `Notificador` é o `NotificadorEmLog`; `ENVIADA`; 1 linha de aviso no log | Integração (contexto Spring) |
| TC-14 | CA-11 | Classe inválida | `EventoDeNotificacao.valueOf("EM_USO")` | `IllegalArgumentException`; os valores são exatamente os três da DP-03 | Unitário |
| TC-15 | CA-08 | Classe inválida | Teste parametrizado com cada falha dos TC-03, TC-04, TC-06, TC-08 e TC-10, com o log capturado | Exatamente 1 linha de falha da notificação por execução, com o id da reserva e a causa certa | Integração (WireMock) |
| TC-16 | CA-10 | Conflito | transação que grava em uma tabela de teste e publica `ReservaNotificavel`, com o `Notificador` falhando; depois, outra transação desfeita | 1ª: o registro continua gravado e o notificador foi chamado 1 vez; 2ª: o notificador não foi chamado | Integração (Testcontainers) |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| R1: a falha da notificação desfazer a reserva | Alto | A chamada acontece depois do commit (`@TransactionalEventListener(AFTER_COMMIT)`) e o `Notificador` nunca lança exceção (CA-10, TC-16) |
| R2: o token do provedor vazar pelo log ou pela resposta | Alto | O log só registra reserva, evento e causa; o corpo de erro é descartado (CA-07, TC-11) |
| R3: a chamada síncrona segurar a resposta ao usuário | Médio | O pior caso é limitado a 500 ms + 1500 ms. Envio assíncrono fica como melhoria futura, registrada no relatório |
| R4: testes com tempo (timeout) instáveis no CI | Médio | Margem de 500 ms entre o limite (1500 ms) e o atraso (2000 ms), e o caso dentro do limite usa 1000 ms (TC-07) |
| R5: a T-24/T-34 não publicarem o evento | Médio | O contrato `ReservaNotificavel` fica pronto e documentado aqui; a ligação vira item do checklist da T-24 e da T-34 |
| R6: notificação perdida não é reenviada | Baixo | Risco aceito R-07 (FE-05) |

## 8. Mini-plano de implementação (ciclos TDD)

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 0 | — | Dependência de teste `org.wiremock:wiremock-standalone` no `pom.xml` e as propriedades `app.notificacao.*` | — |
| 1 | TC-14: só os três eventos | `EventoDeNotificacao`, `Notificacao` e `ResultadoDaNotificacao` | — |
| 2 | TC-01, TC-02: envio com sucesso | `Notificador`, `NotificadorHttp` com `RestClient` e `PropriedadesDaNotificacao` | — |
| 3 | TC-03, TC-04, TC-05: 4xx e 5xx | Tratamento do status em `FALHOU` com a causa | Um método só para registrar a falha |
| 4 | TC-06, TC-07, TC-10: timeout e conexão | Fábrica de requisição com os tempos limite da configuração e tradução das exceções de E/S | — |
| 5 | TC-08, TC-09: resposta inválida | Leitura do `id` com validação | — |
| 6 | TC-11 e TC-15: sem vazamento e 1 linha por falha | Log sem corpo de erro, token nem stack trace | Revisar as mensagens de log |
| 7 | TC-12, TC-13: configuração e modo simulado | `NotificadorEmLog` e a escolha do bean pela `url-base` | — |
| 8 | TC-16: depois do commit | `ReservaNotificavel` e `OuvinteDeNotificacao` | — |

## 9. Revisão por pares

> Preenchida pelo revisor, com commit próprio. Nunca pelo autor.

- [ ] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [ ] As regras citam RNs que existem no PRD.
- [ ] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [ ] Os casos de teste incluem valores limite e entradas inválidas.
- [ ] O contrato da API cobre os erros (400, 401, 403, 409).
- [ ] Não há conflito com outras specs nem com os ADRs.

**Pontos para o revisor decidir:**

- **P-01 (DP-03):** os eventos que notificam são `APROVADA`, `REJEITADA` e `CANCELADA`, como propõe o backlog. Confirmar.
- **P-02 ("503 controlado"):** o backlog diz "INT-03 500 → 503 controlado". Como a falha da notificação não pode quebrar a reserva (RNF-10), esta spec traduz o 503 como a causa `INDISPONIVEL`, e **não** como uma resposta 503 da API do sistema: quem aprovou a reserva recebe a resposta normal da aprovação. Confirmar essa leitura.
- **P-03 (contrato 400/401/403/409):** não se aplica, porque esta task não cria endpoint no sistema.

**Comentários:** _preencher_

**Parecer:** _preencher_ (Aprovada / Ajustes necessários) · @CaiosAguiar · AAAA-MM-DD

## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** _preencher_
- **Resultado:** _preencher_

## 11. Grill-with-docs

Não se aplica (a T-31 não é regra crítica).
