# Spec T-49: matriz de autorização como teste parametrizado

| Campo | Valor |
|---|---|
| Task | T-49 |
| Autor(a) | @renatarsilva |
| Revisor(a) | @Samwel-Merces |
| Status | Liberada |
| Regra crítica | Sim |
| Requisitos | RF-01, RF-13 |
| Característica ISO 25010 | Segurança (autorização) |
| Criada em | 2026-11-10 |

## 1. Contexto e objetivo

Falha de autorização é **limitador de nota**. A [matriz de autorização](../seguranca/matriz-autorizacao.md) (T-48) diz, para cada operação da API, o que cada perfil recebe. Hoje a autorização é provada por testes soltos, um por recurso. Esta spec define **um único teste parametrizado que percorre cada célula da matriz** com tokens reais, os casos de posse (Solicitante mexendo na reserva de outro), a recusa de entrada inválida sem vazamento de detalhes internos e a garantia de que **toda rota da API está na matriz**. Na mesa, o professor faz uma requisição de Solicitante na reserva de outro: nenhuma célula pode ser apagada para o teste passar.

## 2. Escopo

**Dentro:**
- Todas as operações da seção 2 da matriz e as rotas negadas por padrão (X-01 a X-03), para Anônimo, Solicitante, Responsável e Administrador.
- Regras de posse da reserva (NC-01): alterar, cancelar e fazer check-in da reserva de outro.
- Validação de entrada: término antes do início, capacidade negativa e campos ausentes devolvem 400 com mensagem compreensível.
- Nenhuma resposta de erro traz stack trace, SQL, nome de classe ou caminho interno.
- Teste de completude: toda rota mapeada na aplicação aparece na matriz.
- Cada célula vermelha vira defeito em `docs/defeitos/` antes da correção.

**Fora (e por quê):**
- Telas (formulário, sessão e CSRF): ainda não existem; entram com a T-15 (telas) e a T-44.
- Rotas que ainda não existem (consulta de reservas, manutenção, retirada, relatório): entram na matriz e no teste quando as tasks delas entregarem (divergências D-01 e D-04 da matriz).
- A decisão da DP-05 (o Administrador cria reservas?): fica como está no código (403) e registrada na matriz (D-03).
- O formato das respostas de erro: é da T-22. Aqui só se confere que ele não vaza detalhes.

## 3. Regras de negócio

- **RF-01:** autenticação e autorização por perfil. Cada usuário tem um perfil, conferido no banco a cada chamada (T-15, CA-15).
- **Negado por padrão (T-15, CA-08):** rota sem regra declarada é recusada para todos, inclusive o Administrador.
- **NC-01:** o Solicitante altera, cancela e faz check-in somente das próprias reservas; quem não é o dono recebe 403 e a reserva não muda.
- **RN-07:** só o Responsável aprova e rejeita, em qualquer reserva; o Administrador e o Solicitante recebem 403.
- **Ordem dos erros:** 401, 403 por perfil, 404, 403 por dono, 400 e 409.
- **RF-13:** mensagens de erro compreensíveis, sem stack trace, SQL, nome de classe ou caminho interno.

## 4. Contrato da API

Nenhum endpoint novo. O teste exercita as rotas da matriz (`docs/seguranca/matriz-autorizacao.md`) por `MockMvc`, com um usuário real de cada perfil no banco e o token dele.

| Situação | Status HTTP | Corpo |
|---|---|---|
| Perfil com permissão | O status de sucesso da matriz (200, 201 ou 204) | Conforme a rota |
| Perfil sem permissão | 403 | `ProblemDetail` (T-22), sem detalhes internos |
| Sem autenticação | 401 | `ProblemDetail` neutro (T-22) |
| Rota fora da matriz | 403 (ou 401 sem token) | Negado por padrão |
| Entrada inválida (perfil com permissão) | 400 | `ProblemDetail`, `erros` com campo e motivo |

## 5. Critérios de aceite

### CA-01: Sem autenticação, toda rota exige token

- **Dado** qualquer operação da matriz, exceto o login
- **Quando** a requisição chega sem token, ou com token adulterado
- **Então** a resposta é 401 e nada é executado

### CA-02: Perfil sem permissão recebe 403

- **Dado** cada operação da matriz e cada perfil autenticado que **não** tem permissão nela
- **Quando** o perfil chama a operação
- **Então** a resposta é 403 e nenhum dado muda

### CA-03: Perfil com permissão recebe o status de sucesso

- **Dado** cada operação da matriz e cada perfil que tem permissão nela
- **Quando** o perfil chama a operação com um pedido válido
- **Então** a resposta é o status de sucesso da matriz (200, 201 ou 204)

### CA-04: A posse da reserva é respeitada

- **Dado** uma reserva de um Solicitante
- **Quando** outro Solicitante tenta alterá-la, cancelá-la ou fazer o check-in
- **Então** a resposta é 403 e a reserva continua como estava

### CA-05: A ordem dos erros é respeitada

- **Dado** um pedido que poderia gerar mais de um erro
- **Quando** ele é enviado
- **Então** a resposta é a do primeiro erro: 401, 403 por perfil, 404, 403 por dono, 400 e 409 (por exemplo, Solicitante que não é o dono, com corpo inválido, recebe 403)

### CA-06: O que não está na matriz é negado

- **Dado** uma rota `/api/**` que não está na matriz
- **Quando** qualquer perfil autenticado a chama
- **Então** a resposta é 403, e sem token é 401

### CA-07: Entrada inválida devolve 400 sem vazamento

- **Dado** o perfil com permissão para a operação
- **Quando** envia término antes do início, capacidade negativa ou campo ausente
- **Então** a resposta é 400 com o campo e um motivo compreensível
- **E** nenhuma resposta de erro da API traz stack trace, SQL, nome de classe ou caminho interno

### CA-08: Toda rota está na matriz (completude)

- **Dado** a lista de rotas mapeadas na aplicação
- **Quando** o teste a compara com as operações da matriz
- **Então** toda rota mapeada aparece na matriz, e uma rota nova sem linha na matriz faz o teste falhar

### CA-09: O perfil vem do banco a cada chamada

- **Dado** um token válido de um Solicitante
- **Quando** o Administrador rebaixa ou desativa esse usuário
- **Então** a próxima chamada com o mesmo token já recebe a resposta do novo perfil (ou 401, se desativado)

## 6. Casos de teste

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 | CA-01 | Tabela de decisão | Cada operação da matriz sem token | 401 | Integração |
| TC-02 | CA-01 | Classe inválida | Cada operação com token adulterado | 401 | Integração |
| TC-03 | CA-02 | Tabela de decisão | Cada célula 403 da matriz (operação × perfil) | 403 e nada muda | Integração |
| TC-04 | CA-03 | Tabela de decisão | Cada célula de sucesso da matriz (operação × perfil) | 200, 201 ou 204 conforme a matriz | Integração |
| TC-05 | CA-04 | Conflito | Solicitante B altera, cancela e faz check-in da reserva de A | 403 nas três; a reserva não muda | Integração |
| TC-06 | CA-05 | Tabela de decisão | Não dono com corpo inválido; Solicitante com `"ABC"` no PATCH; id inexistente por perfil sem permissão | 403, 403 e 403 | Integração |
| TC-07 | CA-06 | Classe inválida | Rotas X-01, X-02 e X-03 por cada perfil | 403 (401 sem token) | Integração |
| TC-08 | CA-07 | Valor limite | Término antes do início, capacidade negativa e campo ausente nos cadastros | 400 com campo e motivo | Integração |
| TC-09 | CA-07 | Classe inválida | Todas as respostas de erro dos casos acima | Sem stack trace, SQL, nome de classe ou caminho interno | Integração |
| TC-10 | CA-08 | Completude | Rotas mapeadas na aplicação contra as linhas da matriz | Toda rota está na matriz | Integração |
| TC-11 | CA-09 | Conflito | Rebaixar e desativar o usuário e repetir a chamada com o mesmo token | Resposta do novo perfil; 401 se desativado | Integração |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| Apagar ou afrouxar uma célula para o teste passar | Alto | A matriz é a fonte única; o TC-10 falha se uma rota não estiver nela, e o professor confere |
| Uma rota nova entra sem regra e fica aberta | Alto | Negado por padrão (CA-06) e completude (CA-08) |
| Célula vermelha escondida por correção sem registro | Médio | Cada célula vermelha vira defeito em `docs/defeitos/` antes da correção |
| Vazamento de detalhe interno numa mensagem de erro | Alto | TC-09 confere todas as respostas de erro |
| A matriz diverge do código com o tempo | Médio | Divergências ficam listadas (seção 5 da matriz) e entram na revisão de cada task |

## 8. Mini-plano de implementação (ciclos TDD)

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-01 e TC-02: sem token | Nenhuma esperada; se uma rota vazar, registrar o defeito e corrigir a regra | Extrair a lista de operações da matriz |
| 2 | TC-03 e TC-04: célula a célula | Idem | Ler a matriz de uma fonte única |
| 3 | TC-05 e TC-06: posse e ordem dos erros | Idem | Isolar a criação das reservas de cada dono |
| 4 | TC-07: rotas negadas por padrão | Idem | Reaproveitar o cenário |
| 5 | TC-08 e TC-09: validação sem vazamento | Corrigir o vazamento, se houver | Reaproveitar a lista de termos proibidos da T-22 |
| 6 | TC-10: completude | Nenhuma; o teste fica vermelho se faltar linha | Nomear a regra |
| 7 | TC-11: perfil vindo do banco | Idem | Reaproveitar os auxiliares da T-15 |

Os ciclos 1 a 4, 6 e 7 verificam comportamento que a T-15 e as outras tasks já entregaram, então podem nascer verdes; o que importa é que **cada célula vermelha vire defeito antes da correção**.

## 9. Revisão por pares

> Preenchida pelo revisor, com commit próprio. Nunca pelo autor.

- [x] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [x] As regras citam RNs que existem no PRD.
- [x] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [x] Os casos de teste incluem valores limite e entradas inválidas.
- [x] O contrato da API cobre os erros (400, 401, 403, 409).
- [x] Não há conflito com outras specs nem com os ADRs.

**Comentários:** Revisão feita pela autora, por decisão do time de 2026-11-10 (autoaprovação por falta de tempo até a entrega; o revisor formal pelo rodízio segue sendo @Samwel-Merces). Conferi que cada critério tem caso de teste, que a completude da matriz (CA-08) impede apagar células para o teste passar e que o escopo deixa claro o que fica de fora (telas, rotas futuras e a DP-05).

**Parecer:** Aprovada · @renatarsilva · 2026-11-10

## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** 2026-11-10
- **Resultado:** APROVADA na spec validation

## 11. Grill-with-docs

> Obrigatório só para regra crítica. Nas demais, escreva "Não se aplica".

| # | Pergunta | Resposta / decisão | Documento afetado |
|---|---|---|---|
| 1 | O teste lê a matriz do `.md` ou de uma lista no código? | Uma lista no código (`MatrizDeAutorizacao`), espelho fiel do `.md`; o TC-10 confere a lista contra as rotas mapeadas na aplicação, e a revisão confere a lista e o `.md` | Seção 8, matriz |
| 2 | Se a célula do código diverge da matriz, quem está certo? | A matriz descreve o PRD. Célula vermelha vira defeito em `docs/defeitos/` antes da correção; só vira "divergência aceita" por decisão registrada | Seção 7, `docs/defeitos/` |
| 3 | Como não reaproveitar estado entre células destrutivas (PUT, DELETE, cancelamento)? | O teste prepara dados novos a cada caso (usuários, recursos e reservas reais) e limpa o banco depois | Seção 8 |
| 4 | O `DELETE` de reserva (R-07) conta como célula de sucesso? | Sim: o status esperado do Solicitante é 409 (recusa de negócio) e o dos outros perfis é 403; o teste confere os dois lados | Matriz R-07 |
| 5 | Como provar que a mensagem de erro não vaza nada? | Reaproveitar a lista de termos proibidos da T-22 (`TermosProibidos`) em todas as respostas de erro do teste | CA-07, T-22 |
| 6 | A decisão da DP-05 (Administrador cria reservas) muda a matriz? | Hoje 403, como está no código; se a DP-05 mudar, as células R-01 e R-02 do Administrador mudam na matriz e no teste na mesma task | Matriz D-03 |
| 7 | Qual linha da RTM isto preenche? | RF-01 e a linha do limitador "Sem falha de autorização": de `sem teste` para `red` e `green`, com a matriz e o teste parametrizado como evidência | `docs/RTM.md`, `docs/evidencias/README.md` |

Decisões confirmadas em bloco por @renatarsilva (dona da task) em 2026-11-10, a partir das respostas propostas na sessão do grill.

**Resultado:** Liberada · 2026-11-10
