# Backlog do projeto

> Gerado por `scripts/gerar-backlog.py`, a mesma fonte do `backlog-jira.csv`. Para mudar uma task, edite o script e rode-o de novo.

As tasks seguem as **semanas do cronograma da disciplina** (32 a 47) e cobrem tudo o que o professor pede em cada aula, aplicado ao projeto, mais os requisitos que nenhuma aula cobre (interface, retirada e devolução, pesquisa e OpenAPI). A semana 40 é a prova escrita, que não tem entrega no projeto.

Cada task tem um **ID estável** (`T-NN`), que vai no título do Jira. A chave do Jira (`KAN-NN`) é gerada na importação, e é ela que aparece no nome da branch.

**Áreas:** Backend, Frontend, Testes, Infra e Documentação.

**Fluxo** (ver [ai-workflow](../processo/ai-workflow.md)): **—** vai direto para o PR · **spec** passa por spec, revisão e validação · **spec + grill** é regra crítica, com fluxo completo · **spec de outra task** é a fase green de uma spec já liberada.

## Distribuição

| Pessoa | Tasks | Backend | Frontend | Testes | Infra | Documentação | Regras críticas |
|---|---|---|---|---|---|---|---|
| Renata (@renatarsilva) | 18 | 2 | 1 | 6 | 2 | 7 | T-20, T-40 |
| Samuel (@Samwel-Merces) | 18 | 7 | 1 | 2 | 4 | 4 | T-15, T-35, T-37, T-49 |
| Caio (@CaiosAguiar) | 19 | 6 | 1 | 2 | 2 | 8 | T-21, T-34 |
| Todos (time) | 3 | 0 | 0 | 0 | 1 | 2 | — |

## Sprint 1 · 15/10 a 21/10 · semanas 32 a 35

### Semana 32 · Aula 01 · Introdução à Qualidade de Software

| ID | Área | Task | Dono | Fluxo | Prioridade | Depende de |
|---|---|---|---|---|---|---|
| [T-01](#t-01) | Infra | Preparar o ambiente de cada integrante | Todos | — | Highest | T-07 |
| [T-02](#t-02) | Infra | Configurar Jira, board e proteção da branch main | Renata | — | High | — |
| [T-03](#t-03) | Documentação | Diagnóstico de qualidade do projeto da disciplina (Atividade #1) | Renata | — | Medium | — |

### Semana 33 · Aula 02 · ATAM e planejamento de casos de teste

| ID | Área | Task | Dono | Fluxo | Prioridade | Depende de |
|---|---|---|---|---|---|---|
| [T-04](#t-04) | Documentação | Personas dos três perfis | Caio | — | High | T-07 |
| [T-05](#t-05) | Documentação | Utility tree, cenários (A, A) e casos de teste (Atividade #2) | Caio | — | High | T-04 |
| [T-06](#t-06) | Documentação | Plano de teste (ISO/IEC/IEEE 29119-3) | Samuel | — | High | T-07 |

### Semana 34 · Aula 03 · Caixa branca × caixa preta (nasce o projeto)

| ID | Área | Task | Dono | Fluxo | Prioridade | Depende de |
|---|---|---|---|---|---|---|
| [T-07](#t-07) | Infra | Criar o projeto Spring Boot 3 e o esqueleto de documentação | Renata | — | Highest | — |
| [T-08](#t-08) | Infra | Instalar o AIOX no repositório | Samuel | — | Medium | T-07 |
| [T-09](#t-09) | Documentação | PRD: visão, escopos, RF e RNF numerados e regras de negócio | Renata | — | High | T-04 |

### Semana 35 · Aula 04 · Arquitetura e ATAM com IA agêntica

| ID | Área | Task | Dono | Fluxo | Prioridade | Depende de |
|---|---|---|---|---|---|---|
| [T-10](#t-10) | Documentação | Arquitetura: descoberta, C4 e diagramas de sequência | Samuel | — | High | T-09 |
| [T-11](#t-11) | Documentação | Modelo de dados | Caio | — | High | T-09 |
| [T-12](#t-12) | Documentação | ATAM: utility tree, cenários, táticas e riscos | Caio | — | High | T-05, T-10 |
| [T-13](#t-13) | Documentação | ADRs e matriz requisito → decisão → componente → evidência | Samuel | — | High | T-10 |

## Sprint 2 · 22/10 a 28/10 · semanas 36 a 39

### Semana 36 · Aula 05 · Gestão de erros e bugs (TDD fase red)

| ID | Área | Task | Dono | Fluxo | Prioridade | Depende de |
|---|---|---|---|---|---|---|
| [T-14](#t-14) | Documentação | Processo e registro de defeitos | Renata | — | High | — |
| [T-15](#t-15) | Backend | Autenticação e autorização por perfil | Samuel | spec + grill | Highest | T-07, T-13 |
| [T-16](#t-16) | Backend | Cadastro e consulta de salas | Caio | spec | Medium | T-15 |
| [T-17](#t-17) | Backend | Cadastro e consulta de professores e competências | Samuel | spec | Medium | T-15 |
| [T-18](#t-18) | Backend | Cadastro e consulta de materiais e equipamentos | Samuel | spec | Medium | T-15 |
| [T-19](#t-19) | Testes | Plano TDD: inventário de todas as regras de negócio | Renata | — | High | T-09 |
| [T-20](#t-20) | Testes | Fase red: testes de intervalo e sobreposição de horários | Renata | spec + grill | Highest | T-16, T-17, T-18, T-19 |
| [T-21](#t-21) | Testes | Fase red: testes do ciclo de vida da reserva | Caio | spec + grill | High | T-19 |
| [T-22](#t-22) | Backend | Tratamento padronizado de erros e validação de entrada | Caio | spec | High | T-07 |

### Semana 37 · Aula 06 · Testes automatizados em CI (fase green)

| ID | Área | Task | Dono | Fluxo | Prioridade | Depende de |
|---|---|---|---|---|---|---|
| [T-23](#t-23) | Backend | Fase green: criar e alterar reserva com intervalo e sobreposição | Renata | spec da T-20 | Highest | T-20 |
| [T-24](#t-24) | Backend | Fase green: ciclo de vida, alteração e cancelamento | Caio | spec da T-21 | High | T-21, T-23 |
| [T-25](#t-25) | Infra | Pipeline de CI no GitHub Actions | Samuel | — | Highest | T-07 |
| [T-26](#t-26) | Documentação | Avaliação arquitetural: ciclo 01 | Caio | — | Medium | T-12, T-23 |

### Semana 38 · Aula 07 · JUnit 5, JaCoCo e SonarCloud

| ID | Área | Task | Dono | Fluxo | Prioridade | Depende de |
|---|---|---|---|---|---|---|
| [T-27](#t-27) | Infra | JaCoCo com gate de 80% de linhas e 70% de branches | Caio | — | High | T-25 |
| [T-28](#t-28) | Testes | Cobertura de ramo nas regras de sobreposição | Renata | — | High | T-23, T-27 |
| [T-29](#t-29) | Infra | SonarCloud com Quality Gate no PR | Samuel | — | High | T-25, T-26 |

### Semana 39 · Aula 08 · WireMock, Testcontainers e chamadas seguras

| ID | Área | Task | Dono | Fluxo | Prioridade | Depende de |
|---|---|---|---|---|---|---|
| [T-30](#t-30) | Testes | Testes de integração com PostgreSQL real (Testcontainers) | Caio | — | High | T-23 |
| [T-31](#t-31) | Backend | Notificação externa com WireMock | Samuel | spec | High | T-24 |
| [T-32](#t-32) | Documentação | Iniciar a RTM | Renata | — | High | T-25 |

## Sprint 3 · 29/10 a 04/11 · semanas 41 a 43

### Semana 41 · Aula 09 · TDD e BDD no projeto

| ID | Área | Task | Dono | Fluxo | Prioridade | Depende de |
|---|---|---|---|---|---|---|
| [T-33](#t-33) | Documentação | Cenários BDD de aprovação e bloqueio | Caio | — | High | T-24, T-32 |
| [T-34](#t-34) | Backend | Aprovação de recurso restrito com TDD estrito | Caio | spec + grill | High | T-15, T-33 |
| [T-35](#t-35) | Backend | Bloqueio de recursos em manutenção com TDD estrito | Samuel | spec + grill | High | T-33 |
| [T-36](#t-36) | Testes | Feature Cucumber executável e evidência de TDD | Renata | — | High | T-34, T-35 |

### Semana 42 · Aula 10 · Carga e concorrência

| ID | Área | Task | Dono | Fluxo | Prioridade | Depende de |
|---|---|---|---|---|---|---|
| [T-37](#t-37) | Backend | Proteção contra dupla reserva com teste de concorrência | Samuel | spec + grill | Highest | T-30 |
| [T-38](#t-38) | Testes | Teste de carga com JMeter: RNF, plano, execução e resultado | Renata | — | High | T-15, T-37 |

### Semana 43 · Aula 11 · Auditoria, relatórios e requisitos restantes

| ID | Área | Task | Dono | Fluxo | Prioridade | Depende de |
|---|---|---|---|---|---|---|
| [T-39](#t-39) | Documentação | Status dos requisitos | Renata | — | High | T-32 |
| [T-40](#t-40) | Backend | Histórico auditável de mudanças de estado | Renata | spec + grill | High | T-24, T-30 |
| [T-41](#t-41) | Backend | Relatório de utilização com oráculo calculado à mão | Caio | spec | High | T-24, T-37 |
| [T-42](#t-42) | Backend | Registro de retirada e devolução de materiais | Samuel | spec | Medium | T-24 |
| [T-43](#t-43) | Backend | Pesquisa de recursos por tipo, capacidade, localização, competência e disponibilidade | Caio | spec | Medium | T-23, T-35 |
| [T-44](#t-44) | Frontend | Layout base responsivo, login e navegação por perfil | Samuel | spec | High | T-15, T-22 |

## Sprint 4 · 05/11 a 11/11 · semanas 44 a 46 (prova oral na semana 47)

### Semana 44 · Aula 12 · Autorização, RTM e interface

| ID | Área | Task | Dono | Fluxo | Prioridade | Depende de |
|---|---|---|---|---|---|---|
| [T-45](#t-45) | Frontend | Telas do Solicitante | Renata | spec | High | T-43, T-44 |
| [T-46](#t-46) | Frontend | Telas do Responsável e do Administrador | Caio | spec | High | T-41, T-42, T-44 |
| [T-47](#t-47) | Testes | Testes end-to-end dos fluxos principais | Samuel | — | High | T-45, T-46 |
| [T-48](#t-48) | Documentação | Matriz de autorização | Samuel | — | Highest | T-41, T-42, T-43 |
| [T-49](#t-49) | Testes | Matriz de autorização como teste parametrizado | Samuel | spec + grill | Highest | T-48 |
| [T-50](#t-50) | Testes | Mutação manual nas regras críticas e fechamento da RTM | Renata | — | Highest | T-37, T-40, T-49 |
| [T-51](#t-51) | Documentação | Documentação da API com OpenAPI | Caio | — | Medium | T-41 |

### Semana 45 · Aula 13 · Preparação para a prova oral

| ID | Área | Task | Dono | Fluxo | Prioridade | Depende de |
|---|---|---|---|---|---|---|
| [T-52](#t-52) | Documentação | Relatório de qualidade | Renata | — | High | T-50 |
| [T-53](#t-53) | Documentação | Banco de perguntas da prova oral | Caio | — | High | T-52 |
| [T-54](#t-54) | Documentação | Ensaio da prova oral e apresentação | Todos | — | High | T-53 |

### Semana 46 · Aula 14 · Entrega do trabalho final

| ID | Área | Task | Dono | Fluxo | Prioridade | Depende de |
|---|---|---|---|---|---|---|
| [T-55](#t-55) | Infra | Clone limpo e checklist de entrega | Caio | — | Highest | T-52 |
| [T-56](#t-56) | Infra | Release candidate v1.0.0-rc1 e pacote de evidências | Samuel | — | Highest | T-55 |
| [T-57](#t-57) | Documentação | Decisão de qualidade e envio do formulário | Renata | — | Highest | T-56 |

### Semana 47 · Prova oral

| ID | Área | Task | Dono | Fluxo | Prioridade | Depende de |
|---|---|---|---|---|---|---|
| [T-58](#t-58) | Documentação | Prova oral | Todos | — | High | T-54, T-57 |

## Detalhe das tasks

### T-01

**Preparar o ambiente de cada integrante**

- **Semana:** 32 (Aula 01 · Introdução à Qualidade de Software) · **Sprint:** 1 · **Área:** Infra · **Dono:** Todos
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** Highest · **Requisitos:** — · **Depende de:** T-07
- **O que fazer:** Cada integrante instala Java 21, Docker e IntelliJ; clona o repositório; configura git config user.name e user.email com o e-mail da conta do GitHub; abre o projeto no IntelliJ e roda ./mvnw verify até ficar verde; faz um commit de teste numa branch própria; entra no Discord da turma.
- **Por quê:** Na Aula 01 o professor exige que ninguém saia da sala sem um push funcionando. Sem o e-mail certo no git, os commits não aparecem no seu nome, e a autoria vale 10% da nota.
- **Critérios de aceite:**
  - As três pessoas rodaram ./mvnw verify verde na própria máquina
  - Um commit de cada pessoa aparece no GitHub com o avatar correto
- **Arquivos:** —

### T-02

**Configurar Jira, board e proteção da branch main**

- **Semana:** 32 (Aula 01 · Introdução à Qualidade de Software) · **Sprint:** 1 · **Área:** Infra · **Dono:** Renata
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** — · **Depende de:** —
- **O que fazer:** Seguir docs/processo/configuracao-github-e-jira.md: merge commit como única forma de merge, ruleset protegendo a main (PR obrigatório, 1 aprovação, sem bypass), colunas do board, importação deste backlog e app GitHub for Jira.
- **Por quê:** A especificação pede 'repositório com histórico e revisão por pares'. A proteção da main garante que todo código passa por PR revisado por outra pessoa. A Aula 06 também pede branch protection.
- **Critérios de aceite:**
  - Push direto na main é recusado
  - As 58 tasks estão no Jira, nas sprints certas
  - Branch com a chave KAN aparece dentro da task no Jira
- **Arquivos:** docs/processo/configuracao-github-e-jira.md

### T-03

**Diagnóstico de qualidade do projeto da disciplina (Atividade #1)**

- **Semana:** 32 (Aula 01 · Introdução à Qualidade de Software) · **Sprint:** 1 · **Área:** Documentação · **Dono:** Renata
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** Medium · **Requisitos:** — · **Depende de:** —
- **O que fazer:** Clonar github.com/afonsolelis/qualidade_software, seguir o README, rodar a suíte e registrar o que passou, falhou, foi pulado e o que não tem teste. Ler o código procurando problemas que a suíte não pega. Escrever um documento de 1 a 2 páginas com a saída dos testes, no mínimo 5 achados (onde, o quê, por que é problema), a característica ISO/IEC 25010 de cada um e a priorização com critério explícito. Não escrever testes.
- **Por quê:** É a primeira etapa da evolução do projeto na especificação ('diagnóstico inicial'). Treina o vocabulário da ISO 25010, que cai na prova escrita e na oral. Cinco achados bem argumentados valem mais do que vinte listados.
- **Critérios de aceite:**
  - Pelo menos 5 achados com onde, o quê e por quê
  - Cada achado ligado a uma das 9 características da ISO 25010:2023
  - Critério de priorização explícito
  - Nenhuma credencial do repositório analisado copiada para o documento
- **Arquivos:** docs/diagnostico/diagnostico-inicial.md

### T-04

**Personas dos três perfis**

- **Semana:** 33 (Aula 02 · ATAM e planejamento de casos de teste) · **Sprint:** 1 · **Área:** Documentação · **Dono:** Caio
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** — · **Depende de:** T-07
- **O que fazer:** Escrever 3 personas (Solicitante, Responsável, Administrador), cada uma com objetivo, contexto e frustração, na seção 3 do docs/prd.md.
- **Por quê:** A Aula 02 pede 3 personas como base para elicitar requisitos. Na Aula 03 elas viram a origem dos RF e RNF; na Aula 04, os stakeholders da arquitetura.
- **Critérios de aceite:**
  - 3 personas com objetivo, contexto e frustração
  - Cada persona ligada a um perfil de acesso da especificação
- **Arquivos:** docs/prd.md (seção 3)

### T-05

**Utility tree, cenários (A, A) e casos de teste (Atividade #2)**

- **Semana:** 33 (Aula 02 · ATAM e planejamento de casos de teste) · **Sprint:** 1 · **Área:** Documentação · **Dono:** Caio
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** — · **Depende de:** T-04
- **O que fazer:** Montar uma utility tree com pelo menos 3 atributos de qualidade e 6 cenários folha, cada um com (Importância, Dificuldade) justificado em uma linha. Escrever os 3 cenários (A, A) completos nas 6 partes (fonte, estímulo, ambiente, artefato, resposta, medida). Derivar pelo menos 1 caso de teste completo por cenário (A, A): ID, título, pré-condições, dados concretos, passos, resultado esperado e rastreabilidade. Identificar 1 ponto de sensibilidade, 1 ponto de trade-off e 1 não-risco (com a premissa), um parágrafo cada.
- **Por quê:** Liga atributo de qualidade a teste verificável. Regra da Aula 02: cenário sem número na medida da resposta não pontua. Os cenários (A, A) voltam na semana 42, no JMeter.
- **Critérios de aceite:**
  - ≥ 3 atributos e ≥ 6 folhas com (I, D) justificado
  - 3 cenários (A, A) com medida numérica
  - ≥ 3 casos de teste com todos os campos
  - Sensibilidade, trade-off e não-risco justificados
- **Arquivos:** docs/atam/utility-tree-inicial.md

### T-06

**Plano de teste (ISO/IEC/IEEE 29119-3)**

- **Semana:** 33 (Aula 02 · ATAM e planejamento de casos de teste) · **Sprint:** 1 · **Área:** Documentação · **Dono:** Samuel
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** — · **Depende de:** T-07
- **O que fazer:** Preencher docs/plano-de-teste.md: estratégia, níveis (unitário, integração com Testcontainers, API caixa-preta, end-to-end, concorrência, carga), itens a testar e a NÃO testar, critérios de entrada, saída e suspensão, metas de cobertura (80% linhas, 70% branches), ambiente de teste, riscos e recursos.
- **Por quê:** A especificação lista 'plano, suíte e relatório de testes' como entregável, e o peso 'estratégia e documentação de testes' vale 15%. A Aula 02 apresenta o modelo da 29119-3.
- **Critérios de aceite:**
  - Todas as seções preenchidas
  - Cada nível aponta ferramenta e tipo de evidência
  - Lista explícita do que não será testado, com motivo
- **Arquivos:** docs/plano-de-teste.md

### T-07

**Criar o projeto Spring Boot 3 e o esqueleto de documentação**

- **Semana:** 34 (Aula 03 · Caixa branca × caixa preta (nasce o projeto)) · **Sprint:** 1 · **Área:** Infra · **Dono:** Renata
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** Highest · **Requisitos:** — · **Depende de:** —
- **O que fazer:** Projeto Maven com Java 21, Spring Boot 3.5 (última linha 3.x estável), group br.senac e as dependências da Aula 03 (Web, Security, Data JPA, Validation, PostgreSQL, Testcontainers), mais Thymeleaf e Flyway. compose.yaml com PostgreSQL local. Esqueletos de docs/prd.md, docs/arquitetura.md e docs/plano-de-teste.md com as seções da aula. README com 'como subir' e 'como rodar os testes'. (Gerado pelo Claude em 15/10; falta o commit.)
- **Por quê:** É o 'marco zero': a suíte roda verde sem nenhum teste escrito por vocês. A aplicação que não executa é limitador de nota.
- **Critérios de aceite:**
  - ./mvnw verify verde com Java 21
  - ./mvnw spring-boot:run sobe com o PostgreSQL do compose
  - Projeto abre no IntelliJ sem erros
  - docs/prd.md, docs/arquitetura.md e docs/plano-de-teste.md existem
- **Arquivos:** pom.xml, compose.yaml, src/, docs/

### T-08

**Instalar o AIOX no repositório**

- **Semana:** 34 (Aula 03 · Caixa branca × caixa preta (nasce o projeto)) · **Sprint:** 1 · **Área:** Infra · **Dono:** Samuel
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** Medium · **Requisitos:** — · **Depende de:** T-07
- **O que fazer:** Rodar npx aiox-core@latest install (squad development) na raiz do repositório e versionar a pasta .aiox-core/. Ler como o fluxo de histórias do AIOX se encaixa no nosso ai-workflow e registrar a conclusão no PR.
- **Por quê:** A Aula 03 lista .aiox-core/ no checklist 'precisa existir'. É a ferramenta de agentes que o professor usa nas práticas.
- **Critérios de aceite:**
  - .aiox-core/ versionado
  - Nota no PR explicando como o AIOX convive com o ai-workflow
- **Arquivos:** .aiox-core/

### T-09

**PRD: visão, escopos, RF e RNF numerados e regras de negócio**

- **Semana:** 34 (Aula 03 · Caixa branca × caixa preta (nasce o projeto)) · **Sprint:** 1 · **Área:** Documentação · **Dono:** Renata
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** RF-01 a RF-14 · **Depende de:** T-04
- **O que fazer:** Preencher docs/prd.md: 1 Visão, 2 Problema, 3 Personas (T-04), 4 Escopos, 5 Fora de escopo, 6 Métricas de sucesso. Elicitar com IA, a partir das personas, os requisitos funcionais (RF-01 a RF-14 da especificação) e não funcionais (RNF-01...), todos os RNF medíveis (ex.: p95 < 2 s), cada um ligado ao escopo de origem. Numerar as regras de negócio (RN-01...) a partir das regras críticas. Registrar necessidades implícitas e o que fica fora da v1 (ex.: reserva recorrente).
- **Por quê:** Requisitos e regras de negócio valem 15%. As specs, os testes e a RTM citam esses IDs: sem PRD numerado não há rastreabilidade.
- **Critérios de aceite:**
  - RF e RNF numerados e ligados a um escopo
  - Todo RNF tem número na medida
  - Regras críticas da especificação numeradas como RN
  - Seção 'fora de escopo' com justificativa
- **Arquivos:** docs/prd.md

### T-10

**Arquitetura: descoberta, C4 e diagramas de sequência**

- **Semana:** 35 (Aula 04 · Arquitetura e ATAM com IA agêntica) · **Sprint:** 1 · **Área:** Documentação · **Dono:** Samuel
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** — · **Depende de:** T-09
- **O que fazer:** Em docs/arquitetura.md: (1) descoberta: contexto, stakeholders, fonte de verdade, requisitos significativos, drivers priorizados, glossário, fatos, lacunas, suposições (S-01...) e decisões (D-01...), preservando os IDs do PRD. (2) Diagramas Mermaid C4 de contexto, contêineres e componentes. (3) Sequências críticas: criar reserva, aprovar recurso restrito, duas solicitações simultâneas e o fluxo batch diário (adaptação proposta: job no fim do dia que marca NAO_COMPARECEU e conclui reservas vencidas). (4) Comparar pelo menos 2 alternativas quando faltar dado.
- **Por quê:** A especificação pede 'diagramas de contexto, componentes e sequências críticas'. O DoD da Aula 04: outra equipe entende, rastreia e contesta a arquitetura sem explicação oral.
- **Critérios de aceite:**
  - C4 em 3 níveis renderizando no GitHub
  - Sequências online e batch
  - Suposições e decisões com ID
  - Nenhuma decisão sem requisito de origem sem estar marcada
- **Arquivos:** docs/arquitetura.md

### T-11

**Modelo de dados**

- **Semana:** 35 (Aula 04 · Arquitetura e ATAM com IA agêntica) · **Sprint:** 1 · **Área:** Documentação · **Dono:** Caio
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** — · **Depende de:** T-09
- **O que fazer:** Diagrama ER em Mermaid na seção 3 de docs/arquitetura.md: usuário (com perfil), sala, professor e competências, material, reserva (com intervalo e estado), período de manutenção, retirada/devolução e auditoria. Descrever as restrições do banco que impedem conflito (FKs, unicidade, e a opção de EXCLUDE USING gist sobre tstzrange).
- **Por quê:** O modelo é a base das migrações Flyway e das regras de sobreposição. Decidir as restrições cedo evita retrabalho na semana 42 (dupla reserva).
- **Critérios de aceite:**
  - Todas as entidades dos 14 RFs presentes
  - Restrições anti-conflito descritas
- **Arquivos:** docs/arquitetura.md (seção 3)

### T-12

**ATAM: utility tree, cenários, táticas e riscos**

- **Semana:** 35 (Aula 04 · Arquitetura e ATAM com IA agêntica) · **Sprint:** 1 · **Área:** Documentação · **Dono:** Caio
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** — · **Depende de:** T-05, T-10
- **O que fazer:** Em docs/arquitetura.md: utility tree completa com (I, D) justificados; cenários em 6 partes (faixa marcada 'validação pendente' quando faltar meta); situações de falha aplicáveis (falha parcial, reexecução, jobs sobrepostos, dados duplicados, impacto do batch no online etc.); tática de Len Bass e métrica para cada abordagem; sensibilidades, trade-offs, riscos, não-riscos e temas de risco. Baixar o catálogo de táticas do site e guardá-lo em docs/.
- **Por quê:** O ATAM é método de identificação de riscos. 'Arquitetura sem trade-off é sinal de análise superficial'. Cai na prova e alimenta o ciclo-01 da semana 37.
- **Critérios de aceite:**
  - Cada cenário com medida numérica ou marcado 'validação pendente'
  - Pelo menos 1 trade-off real e 1 tema de risco
  - Catálogo de táticas versionado
- **Arquivos:** docs/arquitetura.md, docs/taticas-arquiteturais-len-bass.md

### T-13

**ADRs e matriz requisito → decisão → componente → evidência**

- **Semana:** 35 (Aula 04 · Arquitetura e ATAM com IA agêntica) · **Sprint:** 1 · **Área:** Documentação · **Dono:** Samuel
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** — · **Depende de:** T-10
- **O que fazer:** Revisão cruzada com 6 papéis (Analista, Arquiteto, Dados, Segurança/LGPD, Ops, Custos) registrando objeções. ADRs em docs/adr/ (status, contexto, alternativas, decisão, consequências, riscos, evidência necessária, condição de revisão) para: PostgreSQL, Flyway, Thymeleaf no mesmo projeto, autenticação (sessão nas telas e token na API), Clock injetado, entidade Usuario com perfil. Matriz requisito → decisão → componente → evidência e listas de requisitos sem cobertura, decisões sem requisito, suposições, perguntas e riscos aceitos.
- **Por quê:** ADR é a memória das decisões e assunto certo da prova oral ('por que PostgreSQL?'). A matriz é o começo da rastreabilidade.
- **Critérios de aceite:**
  - Um ADR por decisão, no template
  - Matriz cobrindo os 14 RFs
  - Listas de lacunas e riscos preenchidas
- **Arquivos:** docs/adr/, docs/arquitetura.md

### T-14

**Processo e registro de defeitos**

- **Semana:** 36 (Aula 05 · Gestão de erros e bugs (TDD fase red)) · **Sprint:** 2 · **Área:** Documentação · **Dono:** Renata
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** — · **Depende de:** —
- **O que fazer:** Criar docs/defeitos/README.md com o ciclo de vida (Novo → Triado → Em correção → Em verificação → Fechado; Duplicado, Não reproduzível, Não será corrigido, Reaberto), severidade × prioridade, 5 porquês e métricas (sem contar bugs por pessoa). Criar .github/ISSUE_TEMPLATE/bug.md com as seções da Aula 05 e labels bug e needs-triage. Registrar e triar os primeiros defeitos encontrados (ex.: divergências achadas na arquitetura).
- **Por quê:** Registro de defeitos é entregável, e rastreabilidade e gestão de defeitos valem 10%. Nas semanas 42 e 44 o professor manda registrar defeito antes de corrigir.
- **Critérios de aceite:**
  - Template de relato reproduzível com dados fictícios (domínio .invalid)
  - Pelo menos 1 defeito registrado e triado com severidade e prioridade justificadas
- **Arquivos:** docs/defeitos/, .github/ISSUE_TEMPLATE/bug.md

### T-15

**Autenticação e autorização por perfil**

- **Semana:** 36 (Aula 05 · Gestão de erros e bugs (TDD fase red)) · **Sprint:** 2 · **Área:** Backend · **Dono:** Samuel
- **Fluxo:** Regra crítica: spec + revisão + validação + grill-with-docs · **Prioridade:** Highest · **Requisitos:** RF-01 · **Depende de:** T-07, T-13
- **O que fazer:** Spring Security com os perfis SOLICITANTE, RESPONSAVEL e ADMINISTRADOR; usuários em banco (migração Flyway) com senha em hash; login por formulário (sessão) para as telas e endpoint de login que devolve token para a API (MockMvc, Cucumber e JMeter usam token); gestão de usuários pelo Administrador; 401 sem autenticação e 403 para perfil sem permissão, sem vazar detalhes.
- **Por quê:** RF01. Falha de autorização é limitador de nota. As Aulas 09, 10 e 12 dependem de tokens de cada perfil.
- **Critérios de aceite:**
  - Spec liberada
  - Testes de 401 e 403 por perfil passando
  - Nenhuma senha em texto puro no banco ou em log
- **Arquivos:** src/main/java/.../seguranca/, db/migration/

### T-16

**Cadastro e consulta de salas**

- **Semana:** 36 (Aula 05 · Gestão de erros e bugs (TDD fase red)) · **Sprint:** 2 · **Área:** Backend · **Dono:** Caio
- **Fluxo:** Spec + revisão + validação · **Prioridade:** Medium · **Requisitos:** RF-02 · **Depende de:** T-15
- **O que fazer:** Entidade, migração, repositório, serviço e API REST de salas (tipo, capacidade, localização, restrita ou não). Cadastro e alteração só pelo Administrador; consulta por usuários autenticados. Validação de entrada (capacidade > 0, campos obrigatórios).
- **Por quê:** RF02. Sala é o recurso principal das reservas.
- **Critérios de aceite:**
  - Spec validada
  - Testes de validação com valor limite (capacidade 0, 1)
  - 403 para não administrador
- **Arquivos:** src/main/java/.../sala/

### T-17

**Cadastro e consulta de professores e competências**

- **Semana:** 36 (Aula 05 · Gestão de erros e bugs (TDD fase red)) · **Sprint:** 2 · **Área:** Backend · **Dono:** Samuel
- **Fluxo:** Spec + revisão + validação · **Prioridade:** Medium · **Requisitos:** RF-02 · **Depende de:** T-15
- **O que fazer:** CRUD de professores com suas competências, restrito ao Administrador para escrita.
- **Por quê:** RF02. O professor também é recurso reservável: a agenda dele não pode ter sobreposição.
- **Critérios de aceite:**
  - Spec validada
  - Testes de validação e de permissão
- **Arquivos:** src/main/java/.../professor/

### T-18

**Cadastro e consulta de materiais e equipamentos**

- **Semana:** 36 (Aula 05 · Gestão de erros e bugs (TDD fase red)) · **Sprint:** 2 · **Área:** Backend · **Dono:** Samuel
- **Fluxo:** Spec + revisão + validação · **Prioridade:** Medium · **Requisitos:** RF-02 · **Depende de:** T-15
- **O que fazer:** CRUD de materiais (tipo, patrimônio, restrito ou não, situação), restrito ao Administrador para escrita.
- **Por quê:** RF02. Material é o terceiro tipo de recurso e a base da retirada e devolução (RF09).
- **Critérios de aceite:**
  - Spec validada
  - Testes de validação e de permissão
- **Arquivos:** src/main/java/.../material/

### T-19

**Plano TDD: inventário de todas as regras de negócio**

- **Semana:** 36 (Aula 05 · Gestão de erros e bugs (TDD fase red)) · **Sprint:** 2 · **Área:** Testes · **Dono:** Renata
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** Todas as RN · **Depende de:** T-09
- **O que fazer:** Criar docs/testes/plano-tdd.md com TODAS as regras de negócio do PRD, cada uma com o ID de origem e os casos com resultado esperado: caminho feliz, valores-limite, entradas inválidas, conflitos e estados proibidos. Ambiguidades e perguntas em seção separada. Matriz regra → caso → teste → status (atualizada pelas tasks seguintes). Indicar em que semana cada regra ganha teste: intervalo, sobreposição e estados na 36/37; aprovação e manutenção na 41 (BDD); concorrência na 42; auditoria na 43; autorização na 44.
- **Por quê:** Prompt 1 da Aula 05. É o mapa que garante que nenhuma regra fica sem teste ('só encerre quando todas as regras estiverem cobertas').
- **Critérios de aceite:**
  - Toda RN do PRD aparece com casos
  - Matriz regra → caso → teste → status criada
  - Não contém código nem testes
- **Arquivos:** docs/testes/plano-tdd.md

### T-20

**Fase red: testes de intervalo e sobreposição de horários**

- **Semana:** 36 (Aula 05 · Gestão de erros e bugs (TDD fase red)) · **Sprint:** 2 · **Área:** Testes · **Dono:** Renata
- **Fluxo:** Regra crítica: spec + revisão + validação + grill-with-docs · **Prioridade:** Highest · **Requisitos:** RF-04, RF-05 · **Depende de:** T-16, T-17, T-18, T-19
- **O que fazer:** Escrever a spec da reserva (intervalo e sobreposição em sala, material e professor). Depois, testes JUnit 5 para cada caso do plano TDD dessas regras, com nome citando o ID da regra e estrutura Arrange-Act-Assert. Criar só as assinaturas mínimas para compilar. Rodar e confirmar que cada teste falha por asserção, não por compilação. Commits test(red).
- **Por quê:** Prompt 2 da Aula 05. Teste que nasce vermelho prova que testa alguma coisa. O commit vermelho é a evidência de TDD que o professor procura no histórico.
- **Critérios de aceite:**
  - Spec liberada
  - ≥ 1 teste por caso do plano para essas regras
  - Todos falham por asserção
  - Matriz do plano TDD atualizada
- **Arquivos:** docs/specs/, src/test/java/.../reserva/

### T-21

**Fase red: testes do ciclo de vida da reserva**

- **Semana:** 36 (Aula 05 · Gestão de erros e bugs (TDD fase red)) · **Sprint:** 2 · **Área:** Testes · **Dono:** Caio
- **Fluxo:** Regra crítica: spec + revisão + validação + grill-with-docs · **Prioridade:** High · **Requisitos:** RF-04 · **Depende de:** T-19
- **O que fazer:** Escrever a spec do ciclo de vida: transições válidas (SOLICITADA → APROVADA → EM_USO → CONCLUIDA) e alternativas (REJEITADA, CANCELADA, NAO_COMPARECEU), transições proibidas, alteração e cancelamento pelo dono, e reserva iniciada não pode ser apagada. Depois, testes JUnit 5 parametrizados (tabela de transições) falhando por asserção. Commits test(red).
- **Por quê:** Regras críticas da especificação. A tabela de transições é ótima para @ParameterizedTest e será usada pela aprovação (semana 41) e pela auditoria (43).
- **Critérios de aceite:**
  - Spec liberada
  - Toda transição válida e inválida tem caso
  - Todos falham por asserção
- **Arquivos:** docs/specs/, src/test/java/.../reserva/

### T-22

**Tratamento padronizado de erros e validação de entrada**

- **Semana:** 36 (Aula 05 · Gestão de erros e bugs (TDD fase red)) · **Sprint:** 2 · **Área:** Backend · **Dono:** Caio
- **Fluxo:** Spec + revisão + validação · **Prioridade:** High · **Requisitos:** RF-13 · **Depende de:** T-07
- **O que fazer:** @RestControllerAdvice devolvendo ProblemDetail (RFC 9457) com mensagem compreensível em português; 400 para validação (com o campo e o motivo), 404, 409 para conflito de regra; erro inesperado vira 500 neutro, sem stack trace, SQL ou nome de classe. Nas telas, página de erro amigável.
- **Por quê:** RF13 (mensagens compreensíveis) e 'tratamento seguro de erros'. Na Aula 12 o professor provoca um erro e lê o corpo da resposta. Fazer agora evita que cada endpoint trate erro de um jeito.
- **Critérios de aceite:**
  - Spec validada
  - Teste prova que nenhum erro expõe stack trace ou SQL
  - Formato único para todos os endpoints
- **Arquivos:** src/main/java/.../erro/

### T-23

**Fase green: criar e alterar reserva com intervalo e sobreposição**

- **Semana:** 37 (Aula 06 · Testes automatizados em CI (fase green)) · **Sprint:** 2 · **Área:** Backend · **Dono:** Renata
- **Fluxo:** Usa a spec da T-20 · **Prioridade:** Highest · **Requisitos:** RF-04, RF-05 · **Depende de:** T-20
- **O que fazer:** Implementar a criação e a alteração de reserva até todos os testes da T-20 passarem: término posterior ao início, sem sobreposição no mesmo recurso (sala, material ou professor), Clock injetado. Um caso por vez; refatorar só com tudo verde. Sem @Disabled, sem enfraquecer asserção, sem mudar o resultado esperado. Commits feat(green) e refactor.
- **Por quê:** Prompt 1 da Aula 06. É o núcleo do sistema e a base de quase todas as outras tasks.
- **Critérios de aceite:**
  - Todos os testes da T-20 verdes
  - ./mvnw -B verify verde
  - Matriz do plano TDD atualizada
- **Arquivos:** src/main/java/.../reserva/

### T-24

**Fase green: ciclo de vida, alteração e cancelamento**

- **Semana:** 37 (Aula 06 · Testes automatizados em CI (fase green)) · **Sprint:** 2 · **Área:** Backend · **Dono:** Caio
- **Fluxo:** Usa a spec da T-21 · **Prioridade:** High · **Requisitos:** RF-04 · **Depende de:** T-21, T-23
- **O que fazer:** Implementar a máquina de estados, o cancelamento e a regra de que reserva iniciada não pode ser apagada até todos os testes da T-21 passarem. Commits feat(green) e refactor.
- **Por quê:** Regras críticas de estado. Base para aprovação (41) e auditoria (43).
- **Critérios de aceite:**
  - Todos os testes da T-21 verdes
  - ./mvnw -B verify verde
- **Arquivos:** src/main/java/.../reserva/

### T-25

**Pipeline de CI no GitHub Actions**

- **Semana:** 37 (Aula 06 · Testes automatizados em CI (fase green)) · **Sprint:** 2 · **Área:** Infra · **Dono:** Samuel
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** Highest · **Requisitos:** — · **Depende de:** T-07
- **O que fazer:** Criar .github/workflows/ci.yml (name: qualidade) disparando em pull_request e push na main; permissions contents: read; concurrency com cancel-in-progress; timeout de 10 minutos; Java 21 temurin com cache Maven; ./mvnw -B verify; upload dos relatórios surefire e failsafe como artifact com if: always(). Quebrar uma asserção de propósito, registrar o run vermelho, reverter e confirmar o verde; anotar os IDs dos runs no README. Adicionar o check do CI como obrigatório na proteção da main.
- **Por quê:** Pipeline ausente é limitador de nota. CI, cobertura e análise estática valem 15%. O professor confere o run do CI ligado à tag na entrega.
- **Critérios de aceite:**
  - CI verde em um PR
  - Run vermelho intencional e recuperação registrados no README
  - Merge bloqueado com check falhando
  - Relatórios baixáveis em sucesso e falha
- **Arquivos:** .github/workflows/ci.yml, README.md

### T-26

**Avaliação arquitetural: ciclo 01**

- **Semana:** 37 (Aula 06 · Testes automatizados em CI (fase green)) · **Sprint:** 2 · **Área:** Documentação · **Dono:** Caio
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** Medium · **Requisitos:** — · **Depende de:** T-12, T-23
- **O que fazer:** Criar docs/avaliacao-arquitetural/ciclo-01.md: direcionadores de negócio, cenários em 6 partes, utility tree A/M/B, táticas encontradas no código com arquivo e linha como evidência, sensibilidades, trade-offs, riscos, não-riscos, temas de risco, divergências entre docs/arquitetura.md e o código, e ações priorizadas para o próximo ciclo.
- **Por quê:** Prompt 3 da Aula 06: confronta o que foi planejado com o que foi construído. Divergência encontrada vira defeito ou ADR.
- **Critérios de aceite:**
  - Toda tática citada tem arquivo e linha
  - Divergências listadas com ação
- **Arquivos:** docs/avaliacao-arquitetural/ciclo-01.md

### T-27

**JaCoCo com gate de 80% de linhas e 70% de branches**

- **Semana:** 38 (Aula 07 · JUnit 5, JaCoCo e SonarCloud) · **Sprint:** 2 · **Área:** Infra · **Dono:** Caio
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** — · **Depende de:** T-25
- **O que fazer:** Adicionar o JaCoCo 0.8.15 ao pom.xml (prepare-agent, report e check na fase verify com LINE 0.80 e BRANCH 0.70, relatórios HTML e XML). Confirmar que o build falha abaixo da meta e volta a passar. Publicar o relatório como artifact no ci.yml.
- **Por quê:** A meta 80/70 é piso da especificação. Sem o gate no verify, a cobertura pode cair sem ninguém perceber.
- **Critérios de aceite:**
  - verify falha abaixo de 80/70 (com evidência)
  - Relatório JaCoCo como artifact do CI
- **Arquivos:** pom.xml, .github/workflows/ci.yml

### T-28

**Cobertura de ramo nas regras de sobreposição**

- **Semana:** 38 (Aula 07 · JUnit 5, JaCoCo e SonarCloud) · **Sprint:** 2 · **Área:** Testes · **Dono:** Renata
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** RF-05 · **Depende de:** T-23, T-27
- **O que fazer:** Listar as linhas e branches não cobertos nas regras de reserva, classificando cada um como lacuna de risco ou cosmética. Testar só as lacunas de risco com @ParameterizedTest na trinca -1 / 0 / +1 (ex.: reserva que termina 1 s antes, exatamente no início e 1 s depois de outra) e Clock fixo. Atualizar a matriz do plano TDD.
- **Por quê:** Entregável da especificação para a semana 38: 'cobertura de ramo nas regras de sobreposição'. Testar getters para subir a cobertura não pontua.
- **Critérios de aceite:**
  - 100% de branches na regra de sobreposição
  - Nenhum teste sem asserção
  - Lista de lacunas com classificação
- **Arquivos:** src/test/java/.../reserva/

### T-29

**SonarCloud com Quality Gate no PR**

- **Semana:** 38 (Aula 07 · JUnit 5, JaCoCo e SonarCloud) · **Sprint:** 2 · **Área:** Infra · **Dono:** Samuel
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** — · **Depende de:** T-25, T-26
- **O que fazer:** Importar o repositório no SonarCloud; a Renata (admin) cria o secret SONAR_TOKEN; configurar sonar.organization e sonar.projectKey no pom.xml; desligar a análise automática; step no ci.yml com fetch-depth: 0 e ./mvnw -B verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar; definir 'novo código'. Tratar cada issue (corrigir, aceitar com justificativa ou falso positivo com evidência). Atualizar o ciclo-01 com as medidas do JaCoCo e do SonarCloud.
- **Por quê:** A especificação exige SonarCloud e a meta de 0 bugs ou vulnerabilidades críticas. Marcar falso positivo para 'ficar verde' não vale.
- **Critérios de aceite:**
  - Quality Gate aparece e passa no PR
  - Nenhum token versionado
  - ciclo-01 atualizado com as medidas
- **Arquivos:** pom.xml, .github/workflows/ci.yml, docs/avaliacao-arquitetural/ciclo-01.md

### T-30

**Testes de integração com PostgreSQL real (Testcontainers)**

- **Semana:** 39 (Aula 08 · WireMock, Testcontainers e chamadas seguras) · **Sprint:** 2 · **Área:** Testes · **Dono:** Caio
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** — · **Depende de:** T-23
- **O que fazer:** Criar a base de testes de integração com Testcontainers (PostgreSQL real, sem H2) e cobrir os repositórios e as restrições do banco: migrações Flyway aplicadas, FKs, unicidade e consultas de disponibilidade.
- **Por quê:** A especificação exige que a persistência seja verificada em ambiente real. A Aula 10 depende dessa base para o teste de concorrência.
- **Critérios de aceite:**
  - Testes de integração rodando no CI
  - Nenhum uso de H2 ou mock de repositório nesses testes
- **Arquivos:** src/test/java/.../integracao/

### T-31

**Notificação externa com WireMock**

- **Semana:** 39 (Aula 08 · WireMock, Testcontainers e chamadas seguras) · **Sprint:** 2 · **Área:** Backend · **Dono:** Samuel
- **Fluxo:** Spec + revisão + validação · **Prioridade:** High · **Requisitos:** RF-11 · **Depende de:** T-24
- **O que fazer:** Porta Notificador e adapter HTTP para um serviço externo de notificação (avisar aprovação, rejeição e cancelamento). URL base, token e timeouts por configuração (connect 500 ms, read 1500 ms), nunca fixos. Testes com WireMock: INT-01 200 válido, INT-02 404, INT-03 500 → 503 controlado, INT-04 timeout não persiste, INT-05 JSON inválido rejeitado. Teste negativo: o provedor devolve o próprio token no corpo de erro e ele não vaza pela API. Falha na notificação não quebra a reserva.
- **Por quê:** RF11 e Aula 08. Mostra isolamento de dependência externa, que é exigido pela estratégia de qualidade.
- **Critérios de aceite:**
  - Spec validada
  - Matriz INT-01 a INT-05 testada
  - Suíte roda offline e no CI
  - Nenhuma credencial real em código, fixture ou log
- **Arquivos:** src/main/java/.../notificacao/

### T-32

**Iniciar a RTM**

- **Semana:** 39 (Aula 08 · WireMock, Testcontainers e chamadas seguras) · **Sprint:** 2 · **Área:** Documentação · **Dono:** Renata
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** Todos · **Depende de:** T-25
- **O que fazer:** Criar docs/rtm.md com uma linha por requisito: requisito → risco → teste (caminho e nome) → nível → evidência (run do CI) → situação. Preencher o que já existe e identificar os requisitos de aprovação e manutenção (pré-requisito da Aula 09). A partir daqui, todo PR de feature atualiza a RTM.
- **Por quê:** RTM é entregável e a meta é 100% dos requisitos críticos rastreados. 'Não escreva testado sem apontar para uma execução.'
- **Critérios de aceite:**
  - 14 RFs e todas as regras críticas na tabela
  - Toda linha marcada como testada aponta um run
- **Arquivos:** docs/rtm.md

### T-33

**Cenários BDD de aprovação e bloqueio**

- **Semana:** 41 (Aula 09 · TDD e BDD no projeto) · **Sprint:** 3 · **Área:** Documentação · **Dono:** Caio
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** RF-07, RF-08 · **Depende de:** T-24, T-32
- **O que fazer:** Escrever docs/testes/bdd-aprovacao-bloqueio.md em Dado/Quando/Então para 4 regras: recurso restrito exige aprovação do Responsável; Solicitante não aprova a própria reserva; recurso em manutenção não pode ser reservado; transições de estado só pelos caminhos permitidos. Tabela de fronteiras da manutenção: reserva que termina exatamente no início, que começa exatamente no fim e que cruza um segundo. Fronteira indefinida vira pergunta. Indicar quais cenários viram teste de unidade e quais viram teste de API.
- **Por quê:** Prompt 1 da Aula 09. BDD transforma regra em exemplo verificável antes do código. Cada cenário tem um único Quando e um Então observável.
- **Critérios de aceite:**
  - 4 regras com cenários
  - Tabela de fronteiras
  - Cada cenário com um único Quando
- **Arquivos:** docs/testes/bdd-aprovacao-bloqueio.md

### T-34

**Aprovação de recurso restrito com TDD estrito**

- **Semana:** 41 (Aula 09 · TDD e BDD no projeto) · **Sprint:** 3 · **Área:** Backend · **Dono:** Caio
- **Fluxo:** Regra crítica: spec + revisão + validação + grill-with-docs · **Prioridade:** High · **Requisitos:** RF-07 · **Depende de:** T-15, T-33
- **O que fazer:** Um cenário por vez: 1 teste JUnit 5 → ./mvnw -B test falhando por asserção → commit test(red): <cenário> → mínimo para ficar verde → commit feat(green): <cenário> → refatoração → commit refactor: <motivo>. Regras: reserva de recurso restrito fica SOLICITADA até o Responsável aprovar ou rejeitar; só o Responsável aprova; o Solicitante não aprova a própria reserva.
- **Por quê:** RF07 e o item 'TDD ou BDD com evidência' da especificação. O professor abre o histórico e procura o commit vermelho.
- **Critérios de aceite:**
  - Spec liberada
  - Commits red, green e refactor por cenário no histórico
  - 403 para quem não é Responsável
- **Arquivos:** src/main/java/.../reserva/aprovacao/

### T-35

**Bloqueio de recursos em manutenção com TDD estrito**

- **Semana:** 41 (Aula 09 · TDD e BDD no projeto) · **Sprint:** 3 · **Área:** Backend · **Dono:** Samuel
- **Fluxo:** Regra crítica: spec + revisão + validação + grill-with-docs · **Prioridade:** High · **Requisitos:** RF-08 · **Depende de:** T-33
- **O que fazer:** Mesmo ciclo red → green → refactor da T-34, com commits separados. O Administrador registra períodos de manutenção; reservas que tocam o período são recusadas, seguindo a tabela de fronteiras do BDD. Clock injetado nas regras de manutenção.
- **Por quê:** RF08. Na mesa, o professor pede para quebrar a regra de manutenção e mostrar qual teste falha.
- **Critérios de aceite:**
  - Spec liberada
  - Commits red, green e refactor por cenário
  - Fronteiras da tabela testadas
- **Arquivos:** src/main/java/.../manutencao/

### T-36

**Feature Cucumber executável e evidência de TDD**

- **Semana:** 41 (Aula 09 · TDD e BDD no projeto) · **Sprint:** 3 · **Área:** Testes · **Dono:** Renata
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** RF-07, RF-08 · **Depende de:** T-34, T-35
- **O que fazer:** Adicionar cucumber-java e cucumber-junit-platform-engine; escrever src/test/resources/features/aprovacao-bloqueio.feature a partir do BDD; steps chamando a API via MockMvc com tokens de cada perfil; o verify roda as features no CI e o gate do JaCoCo continua passando. Montar docs/testes/evidencia-tdd.md (cenário → commit red → commit green → commit refactor → trecho da saída) com os commits das T-34 e T-35. Atualizar a RTM com requisito → cenário → feature → commit.
- **Por quê:** Fecha o entregável da Aula 09: PR com commits red/green/refactor, feature executável, evidência de TDD e RTM atualizada.
- **Critérios de aceite:**
  - Features rodando no CI
  - evidencia-tdd.md com links para os commits
  - RTM atualizada
- **Arquivos:** src/test/resources/features/, docs/testes/evidencia-tdd.md, docs/rtm.md

### T-37

**Proteção contra dupla reserva com teste de concorrência**

- **Semana:** 42 (Aula 10 · Carga e concorrência) · **Sprint:** 3 · **Área:** Backend · **Dono:** Samuel
- **Fluxo:** Regra crítica: spec + revisão + validação + grill-with-docs · **Prioridade:** Highest · **Requisitos:** RF-06 · **Depende de:** T-30
- **O que fazer:** Teste de integração JUnit 5 + Testcontainers: 20 solicitações simultâneas (ExecutorService + CountDownLatch) para a mesma sala e horário; esperado exatamente 1 aceita e 19 recusadas com 409, e 1 única linha no banco, verificada por consulta. Repetir para a agenda do professor. Rodar 5 vezes seguidas. Se falhar, registrar o defeito em docs/defeitos/ e proteger no banco com EXCLUDE USING gist sobre tstzrange ou lock pessimista, com ADR curto.
- **Por quê:** RF06. Possibilidade de dupla reserva é limitador de nota. Na mesa, o professor pede para remover a proteção e rodar o teste: ele precisa ficar vermelho.
- **Critérios de aceite:**
  - Spec liberada
  - Vermelho sem a proteção e verde com ela
  - Contagem vem do banco
  - ADR da proteção
  - Estável em 5 execuções seguidas
- **Arquivos:** src/test/java/.../concorrencia/, db/migration/, docs/adr/

### T-38

**Teste de carga com JMeter: RNF, plano, execução e resultado**

- **Semana:** 42 (Aula 10 · Carga e concorrência) · **Sprint:** 3 · **Área:** Testes · **Dono:** Renata
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** RNF de desempenho · **Depende de:** T-15, T-37
- **O que fazer:** Escrever o RNF antes da ferramenta em docs/performance/rnf.md (ex.: p95 até 300 ms com 50 usuários e erro < 1% para consulta de disponibilidade e criação de reserva) e o modelo de carga. Plano perf/reservas.jmx com login e JSON Extractor do token, CSV Data Set (perf/usuarios.csv, usuários fictícios), consulta e criação disputando o mesmo recurso; asserção: criação responde 201 ou 409, nunca 500. Executar em CLI: jmeter -n -t perf/reservas.jmx -l perf/resultados/rodada-01.jtl -e -o perf/relatorio-01. Escrever docs/performance/resultado-01.md com ambiente, carga, p50/p95/p99, vazão e erro (separando 409 esperado de erro real), consulta ao banco confirmando no máximo 1 reserva por intervalo, decisão e hipótese de gargalo. Ligar RNF → plano → resultado na RTM.
- **Por quê:** Plano e resultado de carga são entregáveis; performance, segurança e robustez valem 10%. O professor pergunta de onde saiu o p95 e confere se o 409 foi contado como erro.
- **Critérios de aceite:**
  - RNF escrito antes da execução
  - Relatório HTML fora do Git (.gitignore)
  - 409 separado de erro real
  - Decisão: atende, não atende ou inconclusivo
- **Arquivos:** docs/performance/, perf/reservas.jmx, perf/usuarios.csv

### T-39

**Status dos requisitos**

- **Semana:** 43 (Aula 11 · Auditoria, relatórios e requisitos restantes) · **Sprint:** 3 · **Área:** Documentação · **Dono:** Renata
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** Todos · **Depende de:** T-32
- **O que fazer:** Escrever docs/status-requisitos.md com os 14 RFs e as regras críticas: situação (feito, parcial, ausente) → evidência (arquivo e nome do teste) → risco se ficar de fora. Só marcar 'feito' quando houver teste que falharia se a regra quebrasse. Ordenar começando pelos limitadores de nota e propor a divisão das pendências até a semana 46.
- **Por quê:** Prompt 1 da Aula 11. Mostra com honestidade o que falta. Na mesa, o professor escolhe um item marcado como feito e pede o teste.
- **Critérios de aceite:**
  - 14 RFs e todas as regras críticas
  - Todo 'feito' aponta um teste
  - Pendências com responsável
- **Arquivos:** docs/status-requisitos.md

### T-40

**Histórico auditável de mudanças de estado**

- **Semana:** 43 (Aula 11 · Auditoria, relatórios e requisitos restantes) · **Sprint:** 3 · **Área:** Backend · **Dono:** Renata
- **Fluxo:** Regra crítica: spec + revisão + validação + grill-with-docs · **Prioridade:** High · **Requisitos:** RF-10 · **Depende de:** T-24, T-30
- **O que fazer:** Por TDD (commits separados de teste e código): toda mudança de estado grava um registro com reserva, usuário, instante (Clock), estado anterior, estado novo e motivo; o registro é imutável pela API; reserva iniciada não pode ser apagada. Teste de integração com Testcontainers provando a gravação na mesma transação e que rollback não deixa auditoria órfã. Consulta do histórico só para Responsável e Administrador (403 para Solicitante).
- **Por quê:** RF10 e regra crítica 'toda mudança de estado gera auditoria'. Na mesa, o professor pede o registro de auditoria de uma reserva cancelada.
- **Critérios de aceite:**
  - Spec liberada
  - Teste de rollback sem registro órfão
  - Nenhum endpoint altera ou apaga auditoria
  - 403 para Solicitante
- **Arquivos:** src/main/java/.../auditoria/

### T-41

**Relatório de utilização com oráculo calculado à mão**

- **Semana:** 43 (Aula 11 · Auditoria, relatórios e requisitos restantes) · **Sprint:** 3 · **Área:** Backend · **Dono:** Caio
- **Fluxo:** Spec + revisão + validação · **Prioridade:** High · **Requisitos:** RF-12 · **Depende de:** T-24, T-37
- **O que fazer:** GET /relatorios/utilizacao?inicio=&fim= com horas reservadas por recurso, carga horária por professor e conflitos evitados (tentativas recusadas por sobreposição). Testes de API caixa-preta com massa pequena semeada e valores esperados calculados à mão, com a conta num comentário do teste. Cobrir período vazio, reserva cruzando a borda do período e reservas canceladas (não contam). 401 sem token e 403 para Solicitante.
- **Por quê:** RF12. 'Os valores esperados não foram copiados da saída da própria aplicação.' Na mesa, o professor refaz uma linha à mão.
- **Critérios de aceite:**
  - Spec validada
  - Conta do oráculo em comentário
  - Bordas cobertas
  - 401 e 403 testados
- **Arquivos:** src/main/java/.../relatorio/

### T-42

**Registro de retirada e devolução de materiais**

- **Semana:** 43 (Aula 11 · Auditoria, relatórios e requisitos restantes) · **Sprint:** 3 · **Área:** Backend · **Dono:** Samuel
- **Fluxo:** Spec + revisão + validação · **Prioridade:** Medium · **Requisitos:** RF-09 · **Depende de:** T-24
- **O que fazer:** O Responsável registra a retirada e a devolução de um material ligado a uma reserva aprovada; devolução atrasada fica sinalizada; não é possível retirar material sem reserva aprovada.
- **Por quê:** RF09. Nenhuma aula cobre este requisito, mas ele é obrigatório.
- **Critérios de aceite:**
  - Spec validada
  - Testes de retirada sem reserva, devolução atrasada e permissão
- **Arquivos:** src/main/java/.../material/

### T-43

**Pesquisa de recursos por tipo, capacidade, localização, competência e disponibilidade**

- **Semana:** 43 (Aula 11 · Auditoria, relatórios e requisitos restantes) · **Sprint:** 3 · **Área:** Backend · **Dono:** Caio
- **Fluxo:** Spec + revisão + validação · **Prioridade:** Medium · **Requisitos:** RF-03 · **Depende de:** T-23, T-35
- **O que fazer:** Endpoint de busca combinando filtros, incluindo disponibilidade num intervalo de tempo (exclui recursos com reserva sobreposta ou em manutenção).
- **Por quê:** RF03. Nenhuma aula cobre este requisito, mas ele é obrigatório e é a tela mais usada pelo Solicitante.
- **Critérios de aceite:**
  - Spec validada
  - Testes com classes de equivalência dos filtros
  - Disponibilidade respeita manutenção
- **Arquivos:** src/main/java/.../pesquisa/

### T-44

**Layout base responsivo, login e navegação por perfil**

- **Semana:** 43 (Aula 11 · Auditoria, relatórios e requisitos restantes) · **Sprint:** 3 · **Área:** Frontend · **Dono:** Samuel
- **Fluxo:** Spec + revisão + validação · **Prioridade:** High · **Requisitos:** RF-13 · **Depende de:** T-15, T-22
- **O que fazer:** Layout Thymeleaf com Bootstrap (responsivo em celular e desktop), tela de login, menu que muda conforme o perfil (thymeleaf-extras-springsecurity6), exibição padronizada de mensagens de sucesso e de erro compreensíveis, página de erro amigável.
- **Por quê:** RF13. É a base de todas as telas; fazer uma vez evita que cada tela tenha um visual diferente.
- **Critérios de aceite:**
  - Spec validada
  - Telas utilizáveis em 375 px de largura
  - Menu mostra só o que o perfil pode fazer
- **Arquivos:** src/main/resources/templates/, src/main/java/.../web/

### T-45

**Telas do Solicitante**

- **Semana:** 44 (Aula 12 · Autorização, RTM e interface) · **Sprint:** 4 · **Área:** Frontend · **Dono:** Renata
- **Fluxo:** Spec + revisão + validação · **Prioridade:** High · **Requisitos:** RF-03, RF-04, RF-13 · **Depende de:** T-43, T-44
- **O que fazer:** Pesquisa e consulta de disponibilidade, criação, alteração e cancelamento de reserva, lista 'minhas reservas' com estado. Mensagens de erro compreensíveis (ex.: 'A sala 12 já está reservada das 14h às 16h').
- **Por quê:** RF13 e RF04 pela interface. É o fluxo principal da demonstração na prova oral.
- **Critérios de aceite:**
  - Spec validada
  - Fluxo completo de reserva pela tela
  - Erros de regra exibidos em linguagem clara
- **Arquivos:** src/main/resources/templates/solicitante/

### T-46

**Telas do Responsável e do Administrador**

- **Semana:** 44 (Aula 12 · Autorização, RTM e interface) · **Sprint:** 4 · **Área:** Frontend · **Dono:** Caio
- **Fluxo:** Spec + revisão + validação · **Prioridade:** High · **Requisitos:** RF-02, RF-07, RF-08, RF-09, RF-10, RF-12, RF-13 · **Depende de:** T-41, T-42, T-44
- **O que fazer:** Responsável: fila de aprovação (aprovar ou rejeitar), retirada e devolução, histórico de auditoria. Administrador: cadastros de salas, professores, materiais e usuários, períodos de manutenção e relatório de utilização.
- **Por quê:** RF13 aplicado aos outros dois perfis.
- **Critérios de aceite:**
  - Spec validada
  - Cada operação dos perfis acessível pela tela
  - Ações proibidas não aparecem no menu e retornam 403 se forçadas
- **Arquivos:** src/main/resources/templates/responsavel/, .../administrador/

### T-47

**Testes end-to-end dos fluxos principais**

- **Semana:** 44 (Aula 12 · Autorização, RTM e interface) · **Sprint:** 4 · **Área:** Testes · **Dono:** Samuel
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** RF-13 · **Depende de:** T-45, T-46
- **O que fazer:** Testes E2E com navegador real (Playwright for Java ou Selenium) sobre a aplicação subindo com Testcontainers: login, pesquisa, criação de reserva, aprovação pelo Responsável e cancelamento. Rodar no CI.
- **Por quê:** A especificação exige testes end-to-end e nenhuma aula mostra como; fica a critério do time.
- **Critérios de aceite:**
  - Pelo menos 3 fluxos E2E
  - Rodando no CI sem intervenção manual
- **Arquivos:** src/test/java/.../e2e/

### T-48

**Matriz de autorização**

- **Semana:** 44 (Aula 12 · Autorização, RTM e interface) · **Sprint:** 4 · **Área:** Documentação · **Dono:** Samuel
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** Highest · **Requisitos:** RF-01 · **Depende de:** T-41, T-42, T-43
- **O que fazer:** Escrever docs/seguranca/matriz-autorizacao.md: uma linha por operação da API e colunas anônimo, Solicitante, Responsável e Administrador, com o status esperado (200, 201, 204, 401, 403). Regras de posse: o Solicitante só altera e cancela as próprias reservas; só o Responsável aprova recurso restrito. Marcar divergências com o código sem corrigir e listar rotas fora de qualquer regra de segurança.
- **Por quê:** Prompt 1 da Aula 12. Falha de autorização é limitador de nota. Toda rota precisa estar na matriz.
- **Critérios de aceite:**
  - Toda rota da API está na matriz
  - Regras de posse explícitas
  - Divergências listadas
- **Arquivos:** docs/seguranca/matriz-autorizacao.md

### T-49

**Matriz de autorização como teste parametrizado**

- **Semana:** 44 (Aula 12 · Autorização, RTM e interface) · **Sprint:** 4 · **Área:** Testes · **Dono:** Samuel
- **Fluxo:** Regra crítica: spec + revisão + validação + grill-with-docs · **Prioridade:** Highest · **Requisitos:** RF-01, RF-13 · **Depende de:** T-48
- **O que fazer:** @ParameterizedTest + @MethodSource percorrendo cada célula da matriz (operação, perfil, status) via MockMvc com tokens reais. Casos de posse (Solicitante altera ou cancela a reserva de outro pelo id). Validação de entrada (término antes do início, capacidade negativa, campos ausentes) → 400 com mensagem compreensível. Nenhuma resposta de erro com stack trace, SQL ou nome de classe. Cada célula vermelha vira defeito em docs/defeitos/ antes da correção, que vai em commit próprio referenciando o defeito.
- **Por quê:** Prompt 2 da Aula 12. Na mesa, o professor faz uma requisição de Solicitante na reserva de outro. Nenhuma célula pode ser apagada para passar.
- **Critérios de aceite:**
  - Spec liberada
  - Todas as células da matriz testadas
  - Defeitos registrados antes das correções
- **Arquivos:** src/test/java/.../seguranca/, docs/defeitos/

### T-50

**Mutação manual nas regras críticas e fechamento da RTM**

- **Semana:** 44 (Aula 12 · Autorização, RTM e interface) · **Sprint:** 4 · **Área:** Testes · **Dono:** Renata
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** Highest · **Requisitos:** Regras críticas · **Depende de:** T-37, T-40, T-49
- **O que fazer:** Para cada regra crítica (sobreposição, dupla reserva, aprovação de restrito, manutenção, autorização, auditoria): quebrar a regra no código, rodar o teste ligado, registrar e desfazer com git restore. Registrar em docs/testes/mutacao-manual.md a regra, a quebra, o teste e o resultado. Teste que continuou verde é lacuna: escrever o teste que falta. Percorrer docs/rtm.md e garantir que cada requisito crítico tem risco, teste e evidência do CI no commit atual. Informar a % de requisitos críticos com evidência.
- **Por quê:** Prompt 3 da Aula 12. Meta da especificação: 100% dos requisitos críticos na RTM. Na mesa, o professor pede para repetir uma mutação.
- **Critérios de aceite:**
  - Uma mutação por regra crítica
  - Nenhuma mutação sobreviveu sem ação
  - RTM com 100% dos críticos com evidência
- **Arquivos:** docs/testes/mutacao-manual.md, docs/rtm.md

### T-51

**Documentação da API com OpenAPI**

- **Semana:** 44 (Aula 12 · Autorização, RTM e interface) · **Sprint:** 4 · **Área:** Documentação · **Dono:** Caio
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** Medium · **Requisitos:** RF-14 · **Depende de:** T-41
- **O que fazer:** Adicionar springdoc-openapi, descrever todos os endpoints (parâmetros, respostas, erros e perfis permitidos) e linkar o Swagger UI no README, junto com os fluxos públicos.
- **Por quê:** RF14 e entregável 'README e documentação da API'. O relatório de utilização (Aula 11) também pede documentação no README ou OpenAPI.
- **Critérios de aceite:**
  - Swagger UI acessível com a aplicação rodando
  - Todos os endpoints descritos
  - Link no README
- **Arquivos:** pom.xml, README.md

### T-52

**Relatório de qualidade**

- **Semana:** 45 (Aula 13 · Preparação para a prova oral) · **Sprint:** 4 · **Área:** Documentação · **Dono:** Renata
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** — · **Depende de:** T-50
- **O que fazer:** Escrever docs/relatorio-qualidade.md: identificação (repositório, commit, data, integrantes, ambiente), escopo e fora de escopo, estratégia por nível, resultados comparados às metas (80% linhas, 70% branches, 100% críticos na RTM, 0 bugs ou vulnerabilidades críticas), defeitos por severidade e situação, riscos residuais e recomendação. Para cada afirmação: evidência, interpretação e limite; o que não foi medido fica 'não medido' com o motivo.
- **Por quê:** Entregável 'relatório final de qualidade'. Na mesa, o professor escolhe um número do relatório e pede a fonte.
- **Critérios de aceite:**
  - Toda métrica aponta a fonte
  - Comparação explícita com as 4 metas
  - Riscos residuais listados
- **Arquivos:** docs/relatorio-qualidade.md

### T-53

**Banco de perguntas da prova oral**

- **Semana:** 45 (Aula 13 · Preparação para a prova oral) · **Sprint:** 4 · **Área:** Documentação · **Dono:** Caio
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** — · **Depende de:** T-52
- **O que fazer:** Escrever docs/prova-oral/perguntas.md com 15 perguntas específicas do projeto, cada uma com o arquivo onde está a resposta e a descrição de uma resposta fraca. Distribuir para que cada integrante responda sobre partes que NÃO escreveu. Sem as respostas.
- **Por quê:** Prompt 2 da Aula 13. Na prova oral qualquer integrante pode ser perguntado sobre qualquer parte.
- **Critérios de aceite:**
  - 15 perguntas com arquivo de referência
  - Distribuição cruzada entre os 3
- **Arquivos:** docs/prova-oral/perguntas.md

### T-54

**Ensaio da prova oral e apresentação**

- **Semana:** 45 (Aula 13 · Preparação para a prova oral) · **Sprint:** 4 · **Área:** Documentação · **Dono:** Todos
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** — · **Depende de:** T-53
- **O que fazer:** Ensaio com o agente: uma pergunta por vez, notas de 0 a 2 em correção técnica, evidência citada e domínio de autoria, trocando de integrante a cada 5 perguntas. Gerar docs/prova-oral/ensaio.md com a pontuação por pessoa, as 3 lacunas mais frequentes (cada uma vira task com responsável) e um plano de estudo até a semana 47. Montar a apresentação e simular o roteiro de 8 minutos: problema e risco, arquitetura, demonstração, testes e CI, RTM e relatório, limitações.
- **Por quê:** Prompt 3 da Aula 13 e entregável 'apresentação e demonstração'. O professor confere se as lacunas do ensaio têm responsável.
- **Critérios de aceite:**
  - ensaio.md com pontuação por pessoa
  - Lacunas viraram tasks com responsável
  - Roteiro de 8 minutos cronometrado
- **Arquivos:** docs/prova-oral/ensaio.md, docs/prova-oral/apresentacao/

### T-55

**Clone limpo e checklist de entrega**

- **Semana:** 46 (Aula 14 · Entrega do trabalho final) · **Sprint:** 4 · **Área:** Infra · **Dono:** Caio
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** Highest · **Requisitos:** — · **Depende de:** T-52
- **O que fazer:** Clonar o repositório num diretório temporário e seguir APENAS o README até ./mvnw -B verify passar e a aplicação subir. Conferir os 12 entregáveis da especificação e escrever docs/entrega/checklist.md com item → existe → atualizado → caminho, e os bloqueadores em ordem. Não corrigir nesta etapa: cada bloqueador vira task.
- **Por quê:** Prompt 1 da Aula 14. O professor clona o repositório e segue o README; aplicação que não executa é limitador de nota.
- **Critérios de aceite:**
  - Clone limpo executado só com o README
  - 12 entregáveis conferidos
  - Bloqueadores com responsável
- **Arquivos:** docs/entrega/checklist.md

### T-56

**Release candidate v1.0.0-rc1 e pacote de evidências**

- **Semana:** 46 (Aula 14 · Entrega do trabalho final) · **Sprint:** 4 · **Área:** Infra · **Dono:** Samuel
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** Highest · **Requisitos:** — · **Depende de:** T-55
- **O que fazer:** Com o CI verde na main, criar a tag anotada v1.0.0-rc1 e enviá-la. Escrever CHANGELOG.md. Montar docs/evidencias/README.md como índice: run do CI no commit da tag, artifact do JaCoCo, Quality Gate do SonarCloud, docs/performance/resultado-01.md, docs/rtm.md, docs/defeitos/ e docs/relatorio-qualidade.md. Toda evidência aponta para o commit da tag; correção depois vira v1.0.0-rc2 (nunca mover tag).
- **Por quê:** Prompt 2 da Aula 14. O professor abre o run do CI ligado à tag.
- **Critérios de aceite:**
  - Tag anotada com CI verde
  - CHANGELOG.md
  - Índice de evidências apontando para o commit da tag
- **Arquivos:** CHANGELOG.md, docs/evidencias/README.md

### T-57

**Decisão de qualidade e envio do formulário**

- **Semana:** 46 (Aula 14 · Entrega do trabalho final) · **Sprint:** 4 · **Área:** Documentação · **Dono:** Renata
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** Highest · **Requisitos:** — · **Depende de:** T-56
- **O que fazer:** Escrever docs/decisao-qualidade.md: decisão (liberar, liberar com ressalvas ou bloquear), critérios com a evidência de cada um, riscos aceitos com impacto, justificativa e responsável, e o nome de cada integrante declarando o que fez. Não escrever 'pronto para produção' a partir de testes locais. Enviar o formulário de entrega do Hub (turma, nomes completos dos 3, link do repositório público) e guardar o comprovante.
- **Por quê:** Prompt 3 da Aula 14 e a entrega oficial. Sem o formulário, não há entrega.
- **Critérios de aceite:**
  - decisao-qualidade.md com a declaração dos 3
  - Formulário enviado e comprovante guardado
- **Arquivos:** docs/decisao-qualidade.md

### T-58

**Prova oral**

- **Semana:** 47 (Prova oral) · **Sprint:** 4 · **Área:** Documentação · **Dono:** Todos
- **Fluxo:** Sem spec (PR direto) · **Prioridade:** High · **Requisitos:** — · **Depende de:** T-54, T-57
- **O que fazer:** Apresentar o roteiro de 8 minutos com demonstração ao vivo e responder às perguntas do professor. Cada integrante defende as próprias decisões e responde sobre as partes dos colegas.
- **Por quê:** Arguição do trabalho final: 'você defende as decisões de qualidade que tomou'.
- **Critérios de aceite:**
  - Os 3 presentes
  - Demonstração funcionando a partir da tag
- **Arquivos:** —
