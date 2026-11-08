# Plano de teste: Organização de Recursos

> Task **T-06** · Semana 33 (Aula 02 · ATAM e planejamento de casos de teste) · Modelo: ISO/IEC/IEEE 29119-3
>
> O esqueleto deste arquivo nasceu na T-07 (semana 34) e foi reestruturado na T-06.

## 1. Autoria

| Item | Registro |
|---|---|
| Autoria do conteúdo | Samuel (@Samwel-Merces) |
| Revisão cruzada | Caio (@CaiosAguiar) |
| Situação | Em elaboração |

**Classificação usada neste plano** (a mesma do [PRD](prd.md#41-como-ler-esta-seção)):

| Marca | Significado |
|---|---|
| **Oficial** | Texto ou número da especificação oficial |
| **Derivada** | Medida que o PRD derivou de uma regra oficial |
| **Candidata** | Métrica candidata do PRD (MC-xx), sem origem oficial |
| **Proposta, validação pendente** | Decisão deste plano, sem origem oficial |

## 2. Fontes

Em ordem de prioridade:

1. [Especificação oficial do projeto](https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html)
2. [Material da Aula 02](https://afonsolelis.github.io/aulas_senac/pages/qualidade2/material/material_planejamento-casos-teste.html) (seção 8, plano de teste)
3. [`prd.md`](prd.md): RF, RN, RNF, métricas candidatas (MC), lacunas (L) e fora de escopo (FE)
4. [`arquitetura.md`](arquitetura.md) e [`adr/`](adr/): decisões, riscos (R) e folhas da utility tree (F)
5. [`atam/utility-tree-inicial.md`](atam/utility-tree-inicial.md): cenários (CEN) e casos de teste (CT)
6. [`planejamento/backlog.md`](planejamento/backlog.md): tasks de teste, donos e semanas

## 3. Estratégia

**Proposta, validação pendente.**

A estratégia de testes é organizada em vários níveis: unitário, integração, API caixa-preta, concorrência, end-to-end e carga.

A prioridade é estabelecer o pipeline de CI e, em seguida, validar os riscos de maior impacto para o projeto, especialmente a possibilidade de dupla reserva, as falhas de autorização e a integridade da persistência.

Os testes de integração e de persistência usam PostgreSQL real por meio do Testcontainers, sem H2. O WireMock simula as falhas de serviços externos. Mocks ficam restritos a testes unitários isolados.

Nas regras críticas, adota-se o TDD, e os commits `test(red)` e `feat(green)` são a evidência do ciclo de desenvolvimento.

Os resultados são registrados nos relatórios Surefire, Failsafe e JaCoCo, publicados como artifacts do CI, nas análises do SonarCloud, na rastreabilidade da RTM e no relatório de carga previsto na T-38.

A execução segue a disponibilidade real das ferramentas e das funcionalidades. Testes planejados e dependências ainda não implementadas não contam como evidência de teste concluído.

## 4. Escopo

### 4.1 Itens a testar

Item: texto do [PRD](prd.md). Task: [arquitetura, seção 5.1](arquitetura.md#51-matriz-requisito--decisão--componente--evidência) e [backlog](planejamento/backlog.md). Nível: seção 5 deste plano ou texto do backlog; sem nenhum dos dois, "sem definição".

**Requisitos funcionais**

| Item | Origem | Nível(is) | Task que escreve o teste |
|---|---|---|---|
| Autenticação e autorização por perfil | RF-01 | Integração com MockMvc (T-49) | T-15, T-49 |
| Cadastro e consulta de salas, professores e materiais | RF-02 | sem definição | T-16, T-17, T-18 |
| Pesquisa por tipo, capacidade, localização, competência e disponibilidade | RF-03 | sem definição | T-43 |
| Criação, alteração e cancelamento de reservas | RF-04 | Unitário (T-20, T-21) | T-20, T-21, T-23, T-24 |
| Detecção de sobreposição de horários, inclusive na agenda do professor | RF-05 | Unitário (T-20) | T-20, T-23, T-28 |
| Proteção contra dupla reserva em solicitações simultâneas | RF-06 | Concorrência | T-37 |
| Aprovação obrigatória para recursos restritos | RF-07 | Unitário (T-34); integração com MockMvc (T-36) | T-34, T-36 |
| Bloqueio de recursos em manutenção | RF-08 | Unitário e concorrência (T-35); integração com MockMvc (T-36) | T-35, T-36 |
| Registro de retirada e devolução de materiais e equipamentos | RF-09 | sem definição | T-42 |
| Histórico auditável de mudanças | RF-10 | Integração (backlog, T-40: "Teste de integração com Testcontainers") | T-40 |
| Notificação simulada ou integração com API externa | RF-11 | Integração (WireMock) | T-31 |
| Relatório de utilização por recurso, carga horária alocada e conflitos evitados | RF-12 | API caixa-preta (backlog, T-41: "Testes de API caixa-preta") | T-41 |
| Interface responsiva e mensagens de erro compreensíveis | RF-13 | Unitário (T-22); end-to-end (T-47) | T-22, T-44, T-45, T-46, T-47 |
| Documentação de API ou dos fluxos públicos do sistema | RF-14 | sem definição | T-51 |

**Regras de negócio**

| Item | Origem | Nível(is) | Task que escreve o teste |
|---|---|---|---|
| O término da reserva deve ser posterior ao início | RN-01 | Unitário | T-20 |
| Reservas do mesmo recurso não podem se sobrepor | RN-02 | Unitário (T-20); integração com a restrição no banco (T-30) | T-20, T-30 |
| Duas solicitações simultâneas devem produzir uma única reserva aceita | RN-03 | Concorrência | T-37 |
| Recurso em manutenção não pode ser reservado | RN-04 | Unitário e concorrência | T-35 |
| Fluxo principal de estados | RN-05 | Unitário | T-21 |
| Estados alternativos | RN-06 | Unitário | T-21 |
| Somente o perfil Responsável aprova recursos restritos | RN-07 | Unitário | T-34 |
| Reservas iniciadas não podem ser apagadas | RN-08 | Unitário | T-21 |
| Toda mudança de estado gera auditoria | RN-09 | Integração (backlog, T-40) | T-40 |

**Requisitos não funcionais**

| Item | Origem | Nível(is) | Task que escreve o teste |
|---|---|---|---|
| Cobertura, RTM e defeitos críticos | RNF-01 a RNF-04 | Seção 7 | T-27, T-29, T-32, T-50 |
| Proteção contra dupla reserva | RNF-05 | Concorrência | T-37 |
| Autorização por perfil | RNF-06 | Integração com MockMvc | T-49 |
| Validação de entrada | RNF-07 | API caixa-preta; integração com MockMvc (T-49) | T-22, T-49 |
| Tratamento seguro de erros | RNF-08 | API caixa-preta; integração com MockMvc (T-49) | T-22, T-49 |
| Desempenho da consulta de disponibilidade e da criação de reserva | RNF-09 | Carga | T-38 |
| Resiliência da notificação | RNF-10 | Integração (WireMock) | T-31 |
| Responsividade | RNF-11 | End-to-end | T-44, T-47 |

### 4.2 Itens a não testar

Todas as omissões abaixo são **proposta, validação pendente**.

| ID | Item | Motivo | Risco aceito | Quem assina |
|---|---|---|---|---|
| NT-01 | Integração com o provedor real de notificações e disponibilidade real do provedor externo | A integração é simulada com WireMock. As falhas da API de notificação continuam sendo testadas com WireMock (T-31) | O comportamento do provedor real (contrato, latência e disponibilidade) não é verificado | Samuel |
| NT-02 | Carga de produção com milhares de usuários | O teste de carga fica limitado ao cenário e ao volume da T-38 | O comportamento acima do volume da T-38 não é conhecido | Samuel |
| NT-03 | Compatibilidade com navegadores além do navegador-alvo, Chromium via Playwright (**proposta, validação pendente**) | Foco no navegador-alvo | As telas podem falhar em navegadores diferentes do Chromium | Samuel |
| NT-04 | Infraestrutura de produção | Não há ambiente de produção definido | Falhas próprias do ambiente de produção não são detectadas | Samuel |

**Fora de escopo do produto, não omissão de teste:** o reenvio de notificações pendentes ([FE-05](prd.md#5-fora-de-escopo)) não é uma funcionalidade do produto, então não há o que testar. O risco correspondente é o R-07 da [arquitetura](arquitetura.md#riscos), registrado lá como risco aceito.

## 5. Níveis de teste

| Nível | O que verifica | Ferramenta | Tipo de evidência | Task |
|---|---|---|---|---|
| Unitário | Regras de negócio isoladas: intervalo, estados e validação | JUnit 5 (**Oficial**) | Relatório Surefire e relatório JaCoCo, como artifacts do CI (T-25, T-27) | T-20, T-21, T-22, T-34, T-35 |
| Integração (Testcontainers) | Persistência e restrição anti-conflito em PostgreSQL real (T-30). Transições de estado e indicador `ocupa` (T-21, R-02). Auditoria (T-40). Com WireMock: resiliência da notificação (RNF-10, T-31). Com MockMvc: matriz de autorização (T-49) e cenários Cucumber (T-36) | Testcontainers com PostgreSQL (**Oficial**); WireMock (**Oficial**; dependência a adicionar); MockMvc com contexto Spring e Testcontainers (backlog, T-36 e T-49) | Relatório Surefire, como artifact do CI (T-25). Relatório Failsafe: prevista; a T-25 configura o plugin failsafe (**proposta, validação pendente**) | T-21, T-30, T-31, T-36, T-40, T-49 |
| API caixa-preta | Por HTTP, contra a aplicação iniciada em porta real, sem MockMvc: status, corpo, autenticação, validação de entrada (RNF-07) e erros sem stack trace (RNF-08) | RestAssured: ferramenta prevista, dependência a adicionar na task que escrever o primeiro teste (**proposta, validação pendente**) | Relatório Surefire, como artifact do CI (T-25). Relatório Failsafe: prevista; a T-25 configura o plugin failsafe (**proposta, validação pendente**) | T-41; demais conforme a funcionalidade e a task, sem uma pessoa única |
| End-to-end | Fluxos pelas telas Thymeleaf + Bootstrap. Responsividade (RNF-11) em 375 px (MC-06, **Candidata**) | Playwright, no Chromium: ferramenta prevista, dependência a adicionar na task que escrever o primeiro teste (**proposta, validação pendente**) | Relatório do Playwright (**proposta, validação pendente**) | T-47, depois das telas das T-45 e T-46 |
| Concorrência | T-37: só uma reserva é aceita e persistida; a contagem de reservas vem do banco; o teste falha quando a proteção contra dupla reserva é removida (mutação da T-50). T-35: reserva e manutenção ao mesmo tempo (folha F9, R-01) | JUnit 5 e Testcontainers. Na T-37: 20 threads Java sincronizadas por `CountDownLatch` ou `CyclicBarrier`, cada uma autenticada como um Solicitante distinto, de SOLICITANTE-001 a SOLICITANTE-020, como no CT-02 (20 threads: **Candidata**, MC-02) | Relatório do teste da T-37 e 5 execuções consecutivas verdes no CI (5 execuções: **Candidata**, MC-03; no CI: **proposta, validação pendente**) | T-35, T-37 |
| Carga | Carga e desempenho | JMeter (**Oficial**) | `docs/performance/resultado-01.md` com o relatório do JMeter | T-38 |

## 6. Critérios

### 6.1 Entrada (quando começar)

Todos são **proposta, validação pendente**.

**Regra pelo fluxo da task** (coluna "Fluxo" do [backlog](planejamento/backlog.md)):

- Task com fluxo "spec" ou "spec + grill": a spec está com status `Liberada` antes do primeiro teste.
- Task "sem spec": segue a Definition of Done do [AI Workflow](processo/ai-workflow.md#passo-6-abrir-o-pr-só-com-a-task-terminada).

**Todos os níveis:**

1. O código compila com Java 21.

**Integração, API caixa-preta, end-to-end, concorrência e carga:**

1. O PostgreSQL 16 está disponível, via Testcontainers.
2. As migrations estão aplicadas.
3. Os requisitos e os resultados esperados estão documentados.
4. Os dados de teste estão preparados.

**Integração, API caixa-preta, end-to-end e carga:**

1. A funcionalidade está implementada.

**Concorrência (T-37):**

1. A spec, com grill, está `Liberada`.
2. O PostgreSQL está disponível via Testcontainers.
3. O teste falha sem a proteção contra dupla reserva.

### 6.2 Saída (quando parar)

| # | Critério | Classificação |
|---|---|---|
| 1 | Cobertura de linhas ≥ 80 % | **Oficial** (RNF-01) |
| 2 | Cobertura de branches ≥ 70 % | **Oficial** (RNF-02) |
| 3 | 100 % dos requisitos críticos na RTM | **Oficial** (RNF-03) |
| 4 | Zero bugs ou vulnerabilidades críticas abertos | **Oficial** (RNF-04) |
| 5 | Teste de dupla reserva passando com a proteção e falhando sem ela | **Derivada** (RNF-05) |
| 6 | Teste de concorrência estável em 5 execuções consecutivas | **Candidata** (MC-03) |
| 7 | Matriz de autorização testada (T-49) | **Derivada** (RNF-06) |
| 8 | Pipeline verde | **Proposta, validação pendente** |
| 9 | Resultados registrados | **Proposta, validação pendente** |
| 10 | Resultado de carga documentado | **Proposta, validação pendente** |

### 6.3 Suspensão (quando abortar e devolver) e retomada

Todos são **proposta, validação pendente**. Em toda suspensão, o motivo é registrado.

| Critério de suspensão | Critério de retomada | Quem confirma a retomada |
|---|---|---|
| Ambiente indisponível ou instável | Após a correção | Ambiente ou CI: Samuel |
| Banco inconsistente | Após a correção | Dono da task do nível (seção 10.1) |
| Dados contaminados | Após a correção | Dono da task do nível (seção 10.1) |
| Defeito crítico que impeça a execução confiável | Após a correção | Dono da task e revisor do rodízio ([CONTRIBUTING](../CONTRIBUTING.md#time)) |

## 7. Metas

Valores oficiais, copiados de [`prd.md`](prd.md#45-requisitos-não-funcionais) (seção 4.5). Em caso de divergência, vale o PRD.

| RNF | Métrica | Meta | Ferramenta | Evidência | Quando é medida |
|---|---|---|---|---|---|
| RNF-01 | Cobertura de linhas | ≥ 80 % | JaCoCo (T-27) | Relatório do CI | A cada PR |
| RNF-02 | Cobertura de branches | ≥ 70 % | JaCoCo (T-27) | Relatório do CI | A cada PR |
| RNF-03 | Requisitos críticos na RTM | 100 % | RTM | `docs/rtm.md` (T-32, T-50) | A cada PR e no fechamento (T-50) |
| RNF-04 | Bugs ou vulnerabilidades críticas | 0 | SonarCloud (T-29) | Quality Gate do SonarCloud | A cada PR |

O JaCoCo mede a suíte inteira (RNF-01 e RNF-02). A configuração que soma os testes unitários e os de integração fica com a T-27 (a confirmar).

**Metas derivadas e candidatas usadas neste plano** (valores no PRD, só o ID aqui):

| RNF | Onde o plano usa | Classificação |
|---|---|---|
| RNF-05 (MC-02, MC-03) | Nível de concorrência (seção 5) e critérios de saída 5 e 6 | **Derivada**; N e repetições **Candidatas** |
| RNF-06 | Nível de integração, com MockMvc (seção 5), e critério de saída 7 | **Derivada** |
| RNF-07 | Nível de API (seção 5) | Exigência **oficial**; medida **Candidata** e **Derivada** |
| RNF-08 | Nível de API (seção 5) | **Derivada** |
| RNF-09 (MC-01) | Nível de carga (seção 5) e critério de saída 10 | **Candidata** (decisão na T-38, DP-07) |
| RNF-10 (MC-04, MC-05) | Nível de integração (seção 5) | **Candidata** |
| RNF-11 (MC-06) | Nível end-to-end (seção 5) | **Candidata** |

## 8. Ambiente de teste

| Item | Configuração | Fonte |
|---|---|---|
| Linguagem | Java 21 | Critério de entrada · arquitetura F-01 |
| Banco | PostgreSQL 16, com as migrations aplicadas | Critérios de entrada · arquitetura F-01 |
| Containers | Docker em execução, exigido pelo Testcontainers | [README](../README.md#pré-requisitos) |
| Interface | Thymeleaf + Bootstrap | Nível end-to-end · ADR-0006 |
| Navegador | Chromium, via Playwright (**proposta, validação pendente**) | NT-03 |

## 9. Riscos

### 9.1 Riscos de produto

Riscos arquiteturais escolhidos da [arquitetura, seção 6.5](arquitetura.md#riscos). O texto é o de lá. A resposta vem da coluna "Teste planejado" da [arquitetura, seção 6.4](arquitetura.md#64-táticas-e-métricas) e do backlog.

| ID | Risco | Origem | Resposta no plano |
|---|---|---|---|
| R-01 | A verificação de manutenção e a gravação da reserva não são atômicas: uma reserva pode ser gravada depois de a manutenção ser confirmada | Arquitetura 6.5 · F9 | T-35, nível de concorrência (folha F9) |
| R-02 | `ocupa` diverge do estado da reserva se o Ciclo de vida esquecer de atualizá-lo, e a restrição anti-conflito deixa de proteger | Arquitetura 6.5 · D-02 · F10 | T-21, nível de integração, parametrizado por transição (folha F10) |
| R-04 | Nenhuma tática de desempenho definida, e a meta ainda não foi aprovada | Arquitetura 6.5 · DP-07, RNF-09 · F11 | T-38 (carga, folha F11) |
| R-05 | A auditoria só de inserção depende da aplicação, sem reforço no banco | Arquitetura 6.5 · F6 | T-40 (auditoria, folha F6) |
| R-08 | O mesmo intervalo é guardado duas vezes (`reserva` e `reserva_recurso`); se um mudar sem o outro, a restrição anti-conflito protege um horário que não é o da reserva | Arquitetura 6.5 · D-02 · F1, F2, F10 | T-20 (teste de sobreposição) e T-30 (integração com a restrição no banco). Ver DV-01 |

### 9.2 Riscos de projeto

| ID | Risco | Origem | Resposta no plano |
|---|---|---|---|
| RP-01 | CI ausente: não há `.github/workflows` na `main`. O risco fica ativo até a T-25 ser mergeada | Repositório · arquitetura F-08 | T-25 |
| RP-02 | Proteção da `main` pendente | [`configuracao-github-e-jira.md`](processo/configuracao-github-e-jira.md), Parte A | T-02 |
| RP-03 | Cronograma comprimido | [`sprints.md`](planejamento/sprints.md): "as 15 semanas de conteúdo foram comprimidas em 4 sprints de uma semana" | sem resposta definida |

## 10. Recursos

### 10.1 Responsáveis e prazos

Tasks que escrevem ou habilitam testes. Semanas conforme o [backlog](planejamento/backlog.md).

| Pessoa | Tasks que escrevem ou habilitam testes | Semanas | Ligação com riscos e ferramentas |
|---|---|---|---|
| Renata | T-20, T-28, T-36, T-38, T-40, T-50 | 36, 38, 41, 42, 43, 44 | T-40 → R-05 |
| Caio | T-21, T-22, T-30, T-33, T-34, T-41 | 36, 36, 39, 41, 41, 43 | — |
| Samuel | T-25, T-31, T-35, T-37, T-47, T-49 | 37, 39, 41, 42, 44, 44 | T-35 → R-01 · T-31 → WireMock |

Os testes de API caixa-preta se distribuem conforme a funcionalidade e a task: não ficam com uma pessoa só.

### 10.2 Ferramentas

| Ferramenta | Uso | Nível | Situação |
|---|---|---|---|
| JUnit 5 | Testes automatizados | Unitário, integração, concorrência | Adotada (`pom.xml`) |
| Testcontainers | PostgreSQL real nos testes | Integração, concorrência | Adotada (`pom.xml`) |
| MockMvc | Requisições ao contexto Spring, sem porta real (T-36, T-49) | Integração | Adotada (`spring-boot-starter-test` no `pom.xml`) |
| Surefire | Relatório dos testes | Todos os níveis executados pelo Maven | Plugin padrão do Maven, sem declaração no `pom.xml` |
| Failsafe | Relatório dos testes de integração e de API | Integração, API caixa-preta | Prevista; a T-25 configura o plugin failsafe (**proposta, validação pendente**) |
| WireMock | Simulação da API de notificação, inclusive as falhas | Integração | Prevista; dependência a adicionar (T-31) |
| RestAssured | Testes de API contra a aplicação em porta real | API caixa-preta | Prevista; dependência a adicionar na task que escrever o primeiro teste |
| Playwright | Testes pelas telas, no Chromium | End-to-end | Prevista; dependência a adicionar na task que escrever o primeiro teste (T-47) |
| `CountDownLatch` ou `CyclicBarrier` | Sincronizar as 20 threads | Concorrência | Do próprio Java; sem dependência |
| JaCoCo | Cobertura de linhas e branches da suíte inteira | Unitário e integração | Prevista (T-27; soma dos níveis a confirmar) |
| SonarCloud | Análise estática e Quality Gate | — | Prevista (T-29) |
| JMeter | Carga e desempenho | Carga | Prevista (T-38) |

### 10.3 Dados de teste

- Os dados de teste são preparados antes da execução (critério de entrada) e, se contaminados, suspendem a execução (seção 6.3).
- Massa já definida: os Solicitantes SOLICITANTE-001 a SOLICITANTE-020, do [CT-02](atam/utility-tree-inicial.md).
- Método de carga e restauração dos dados comuns: sem definição (LP-01).

## 11. Premissas e lacunas

### 11.1 Premissas

| ID | Premissa | Fonte |
|---|---|---|
| PP-01 | Os critérios de entrada, saída e suspensão não são oficiais: a especificação não os define | Especificação |
| PP-02 | Os ADRs 0004 a 0009 citados neste plano estão com status "Proposta" | [`adr/`](adr/) |

### 11.2 Lacunas

As lacunas do PRD mantêm o ID de lá. As lacunas próprias deste plano usam o prefixo `LP`, para não colidir com L01 a L14.

| ID | Lacuna | Efeito no plano | Onde será resolvida |
|---|---|---|---|
| L03 | O que é "simultâneo" e que resposta recebe a solicitação recusada | Resultado esperado do teste de concorrência | T-37 |
| L06 | Não existe matriz oficial perfil × operação | Denominador do critério de saída 7 (RNF-06) | T-48, T-49 |
| DP-07 | Valores finais de desempenho | Meta do nível de carga (RNF-09) | T-38 |
| LP-01 | Método de carga e restauração dos dados de teste comuns não definido | Seção 10.3 | sem definição |
| LP-02 | Formato do "relatório de testes", entregável da especificação, não definido | Seção 3 | sem definição |
| LP-03 | Onde a aplicação roda na entrega não está definido ([arquitetura, 5.2](arquitetura.md#52-lacunas-suposições-perguntas-e-riscos-aceitos)) | Seção 8 e NT-04 | sem definição |

O RP-03 (seção 9.2) também fica como lacuna declarada: sem resposta definida.

### 11.3 Divergências com outros documentos

| ID | Divergência | Documento a corrigir |
|---|---|---|
| DV-01 | A [arquitetura, seção 6.4](arquitetura.md#64-táticas-e-métricas), cita a T-23 como teste de sobreposição (folhas F1 e F2). A T-23 é a fase green (código); o teste é escrito na T-20 | `docs/arquitetura.md` |

## 12. Revisão e aprovação

| Data | Quem | Parecer |
|---|---|---|
| | | |
