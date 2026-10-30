# PRD: Organização de Recursos

> Esqueleto criado na semana 34 (Aula 03). Seção 3 preenchida na task **T-04** (personas). Seções 1, 2, 4, 5 e 6 preenchidas na task **T-09** · Autoria do conteúdo: Caio (@CaiosAguiar) · Revisão cruzada: Samuel (@Samwel-Merces).
>
> Fonte da verdade: a [especificação oficial](https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html). Em caso de divergência com outro documento do repositório, vale a especificação.

## 1. Visão

Um sistema confiável para alocar salas, professores e materiais sem conflitos.

O produto organiza a alocação de **salas, professores e materiais** de uma instituição de ensino **sem conflitos de horário**. Atende três perfis de acesso definidos pela especificação: o **Solicitante** (professor ou coordenador), que consulta a disponibilidade e cria, altera ou cancela suas reservas; o **Responsável**, que aprova solicitações especiais, valida a alocação de docentes e acompanha retiradas e devoluções de material; e o **Administrador**, que gerencia salas, professores, materiais, usuários, bloqueios e períodos de manutenção.

Como projeto da disciplina, o foco não é só fazer o sistema funcionar, mas apresentar **evidências objetivas de qualidade, segurança, rastreabilidade e desempenho**. Stack exigida: Java 21 e Spring Boot 3.x.

## 2. Problema

Em uma instituição de ensino, a mesma sala, o mesmo material ou o mesmo professor podem ser pedidos para o mesmo horário por pessoas diferentes. Sem controle, isso gera reservas duplicadas, aulas sem sala e equipamentos cuja retirada e devolução ninguém acompanha.

A especificação transforma esse problema em regras críticas (seção 4.4): o término posterior ao início, a proibição de sobreposição no mesmo recurso, uma única reserva aceita entre solicitações simultâneas, o bloqueio de recursos em manutenção, a aprovação de recursos restritos só pelo Responsável e a auditoria de toda mudança de estado. Duas falhas ligadas ao problema são **limitadores de nota**: a falha de autorização e a possibilidade de dupla reserva.

As frustrações das personas da seção 3 ilustram o problema do ponto de vista de cada perfil (descobrir o conflito em cima da hora, não saber quem está com um equipamento, ver um recurso em manutenção ser reservado). Elas são fictícias e não definem regra de negócio.

## 3. Personas

> Task **T-04** · Autoria do conteúdo: Caio (@CaiosAguiar) · Revisão cruzada: Samuel (@Samwel-Merces)

**Perfil × persona.** Solicitante, Responsável e Administrador são os **perfis de acesso** definidos na [especificação oficial](https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html) (resumo em [`contexto-da-disciplina.md`](projeto/contexto-da-disciplina.md#perfis)). As pessoas abaixo são **personas fictícias**: representações de usuários típicos de cada perfil, criadas para orientar a elicitação. Nomes, instituição e histórias são inventados.

**Como ler esta seção:**

- Os campos "Objetivo", "Contexto" e "Frustração" descrevem a persona. O contexto é ilustrativo e não define regra de negócio.
- "Necessidades principais" só repete comportamentos que a especificação já atribui ao perfil. O que a especificação não confirma está marcado como **necessidade candidata, validação pendente**.
- Nenhuma necessidade desta seção é requisito oficial. A elicitação e a numeração de RF, RNF e RN ficam na T-09 (seção 4).
- Os `RF-xx` citados seguem a numeração provisória de [`contexto-da-disciplina.md`](projeto/contexto-da-disciplina.md#requisitos-funcionais-obrigatórios) e serão reconciliados na T-09. As folhas (F), cenários (CEN) e lacunas (L) vêm de [`utility-tree-inicial.md`](atam/utility-tree-inicial.md).

### Marina Couto

- **Perfil relacionado:** Solicitante.
- **Objetivo:** reservar, com antecedência e sem conflito, a sala e os materiais das próprias aulas, e ajustar ou cancelar essas reservas quando o calendário muda.
- **Contexto:** professora fictícia de um curso de tecnologia, que também acumula a coordenação de um curso. Dá aulas em dias e turnos diferentes e às vezes precisa de laboratório e de equipamentos, como projetor ou kit de prática. Consulta a disponibilidade antes de pedir e costuma alterar reservas quando há reposição de aula ou mudança no calendário.
- **Frustração:** descobrir em cima da hora que a sala ou o equipamento já estava ocupado por outra pessoa no mesmo horário, ou que ela mesma tinha sido alocada em duas turmas ao mesmo tempo. Também não saber se um pedido de recurso restrito já foi aprovado ou ainda está pendente.
- **Necessidades principais:**
  - consultar a disponibilidade de salas, professores e materiais (perfil Solicitante; RF-02, RF-03);
  - criar, alterar e cancelar as **próprias** reservas (perfil Solicitante; RF-04);
  - ter o pedido recusado quando houver sobreposição com outra reserva do mesmo recurso, inclusive na agenda do professor (RF-05);
  - receber mensagens de erro compreensíveis quando o pedido for recusado (RF-13);
  - acompanhar se a reserva está SOLICITADA, APROVADA, REJEITADA ou CANCELADA: **necessidade candidata, validação pendente** (a especificação define os estados, mas não diz o que o Solicitante consegue ver);
  - ser avisada quando um pedido de recurso restrito for aprovado ou rejeitado: **necessidade candidata, validação pendente** (o RF-11 exige notificação, mas não define os eventos que a disparam).
- **Requisitos, regras ou cenários relacionados:** RF-01, RF-02, RF-03, RF-04, RF-05, RF-06, RF-07, RF-08, RF-11, RF-13 · regras críticas "O término deve ser posterior ao início", "Reservas do mesmo recurso (sala, material ou professor) não podem se sobrepor", "Recursos em manutenção não podem ser reservados" e "Reservas iniciadas não podem ser apagadas" · F1/CEN-01/CT-01 e F2/CEN-02/CT-02 (o Solicitante é a fonte do estímulo) · F5 (Solicitante não executa operações do Administrador).
- **Premissas ou lacunas:**
  - a especificação reúne professor e coordenador no perfil Solicitante, mas não informa se o coordenador possui permissões adicionais ou pode reservar em nome de outra pessoa;
  - não está definido até quando uma reserva pode ser alterada ou cancelada, além de que reservas iniciadas não podem ser apagadas;
  - lacunas já registradas: L1 (adjacência conta como sobreposição?), L2 (quais estados ocupam o horário) e L4 (reserva de recurso não restrito passa por APROVADA?).

### Rogério Lima

- **Perfil relacionado:** Responsável.
- **Objetivo:** decidir rapidamente sobre os pedidos que exigem aprovação, garantir que os docentes estejam alocados sem conflito e saber, a qualquer momento, quais materiais saíram e quais voltaram.
- **Contexto:** responsável fictício pelos laboratórios e pelo empréstimo de equipamentos de uma unidade de ensino. Recebe os pedidos de recursos restritos, confere a alocação de professores nas reservas e acompanha a retirada e a devolução dos materiais.
- **Frustração:** aprovar um pedido sem ter certeza de que o recurso ou o professor estavam livres; perceber depois que um equipamento não foi devolvido e não conseguir saber quem estava com ele; ver alguém sem autoridade aprovar um recurso restrito.
- **Necessidades principais:**
  - aprovar solicitações de recursos restritos, com a garantia de que só o perfil Responsável faz isso (perfil Responsável; RF-07; regra crítica "Somente responsáveis aprovam recursos restritos.");
  - rejeitar solicitações de recursos restritos: **necessidade candidata, validação pendente**. A especificação prevê o estado REJEITADA, mas não atribui essa transição a um perfil;
  - validar a alocação de docentes, apoiado na detecção de sobreposição na agenda do professor (perfil Responsável; RF-05);
  - acompanhar a retirada e a devolução de materiais (perfil Responsável; RF-09);
  - consultar o histórico de mudanças de estado de uma reserva: **necessidade candidata, validação pendente**. O RF-10 exige histórico auditável, mas não define quais perfis podem consultá-lo;
  - ver uma lista dos pedidos que aguardam a sua aprovação: **necessidade candidata, validação pendente**;
  - registrar que o solicitante não compareceu (estado NAO_COMPARECEU): **necessidade candidata, validação pendente** (a especificação define o estado, mas não diz qual perfil o registra).
- **Requisitos, regras ou cenários relacionados:** RF-01, RF-05, RF-07, RF-09, RF-10, RF-11 · regras críticas de estados (`SOLICITADA → APROVADA → EM_USO → CONCLUIDA` e alternativos), "Somente responsáveis aprovam recursos restritos." e "Toda mudança de estado gera auditoria." · F4 (quem não é Responsável não aprova recurso restrito) · F6 (auditoria) · F7/CEN-03/CT-03 (a aprovação feita pelo Responsável dispara a notificação, como premissa de escopo do cenário).
- **Premissas ou lacunas:**
  - a especificação diz que o Responsável aprova "solicitações especiais", e o RF-07 fala em "recursos restritos". A relação entre solicitação especial e recurso restrito será esclarecida na T-09. Os termos não são tratados como sinônimos nesta seção;
  - não está definido o que significa, na prática, "validar a alocação de docentes" (se é uma aprovação explícita ou só uma conferência);
  - não está definido se o Responsável **registra** a retirada e a devolução ou apenas as acompanha, nem quem faz o registro (RF-09);
  - lacunas já registradas: L4 (fluxo de recurso não restrito), L5 (o que torna um recurso restrito) e L7 (conteúdo mínimo da auditoria).

### Patrícia Nogueira

- **Perfil relacionado:** Administrador.
- **Objetivo:** manter corretos e atualizados os cadastros de salas, professores, materiais e usuários, e bloquear os recursos que estão em manutenção para que ninguém os reserve.
- **Contexto:** analista fictícia da área administrativa de uma unidade de ensino. Cadastra novos espaços e equipamentos, mantém os dados dos professores (incluindo as competências usadas na pesquisa), gerencia os usuários do sistema e registra os bloqueios e os períodos de manutenção.
- **Frustração:** um recurso em manutenção aparecer como disponível e acabar reservado; um usuário receber um perfil errado e conseguir fazer operações que não deveria; não ter dados confiáveis de uso dos recursos para justificar decisões.
- **Necessidades principais:**
  - gerenciar o cadastro de salas, professores e materiais (perfil Administrador; RF-02);
  - gerenciar usuários (perfil Administrador);
  - atribuir ou alterar perfis de acesso dos usuários: **necessidade candidata, validação pendente**. A especificação atribui ao Administrador o gerenciamento de usuários, mas não detalha se isso inclui administrar seus perfis;
  - registrar bloqueios e períodos de manutenção, impedindo novas reservas do recurso (perfil Administrador; RF-08; regra crítica "Recursos em manutenção não podem ser reservados.");
  - impedir que usuários sem autorização executem operações administrativas: **necessidade candidata, validação pendente**. A matriz completa de perfil por operação ainda não foi definida;
  - consultar o relatório de utilização por recurso, carga horária alocada e conflitos evitados: **necessidade candidata, validação pendente** (o RF-12 existe, mas a especificação não diz qual perfil o consulta);
  - saber quais reservas já existentes são afetadas quando um recurso entra em manutenção: **necessidade candidata, validação pendente**.
- **Requisitos, regras ou cenários relacionados:** RF-01, RF-02, RF-03, RF-08, RF-10, RF-12 · regra crítica "Recursos em manutenção não podem ser reservados." · F5 (Solicitante não executa operações do Administrador) · F6 (auditoria).
- **Premissas ou lacunas:**
  - a especificação diz que o Administrador gerencia "bloqueios" e "manutenção", mas não diz se o bloqueio é algo diferente da manutenção;
  - não está definido o que acontece com reservas já feitas para um recurso que entra em manutenção;
  - não está definido se o Administrador também pode criar reservas. O Administrador fica fora da aprovação de recursos restritos, porque a regra crítica atual atribui essa ação ao perfil Responsável;
  - lacuna já registrada: L6 (não existe matriz oficial perfil × operação). A matriz oficial de perfil por operação será documentada na T-48 e posteriormente utilizada pelos testes da T-49.

## 4. Escopos

> Task **T-09** · Autoria do conteúdo: Caio (@CaiosAguiar) · Revisão cruzada: Samuel (@Samwel-Merces)

### 4.1 Como ler esta seção

**Classificação.** Todo requisito, regra ou métrica desta seção tem uma classificação:

| Classificação | Significado |
|---|---|
| **Oficial** | Texto ou número que está na especificação oficial. |
| **Derivada** | Medida numérica que traduz uma regra oficial binária ("uma única", "não podem", limitador de nota) em algo verificável. A regra é oficial; a forma da medida é proposta da equipe. |
| **Candidata** | Proposta da equipe ou exemplo de material de aula, sem origem oficial. Validação pendente: **não é meta aprovada**. |

Necessidades candidatas, lacunas e decisões pendentes **não são requisitos oficiais**. Elas registram o que a especificação não confirma.

**Convenção de IDs.**

| Prefixo | Uso | Observação |
|---|---|---|
| `E0`…`E8` | Escopo | E0 não é escopo funcional (seção 4.2) |
| `RF-01`…`RF-14` | Requisito funcional | Ordem oficial: a especificação lista os requisitos numerados de 1 a 14. O prefixo `RF-` é convenção do time |
| `RN-01`… | Regra de negócio | Numeradas a partir das regras críticas da especificação |
| `RNF-01`… | Requisito não funcional | Todo RNF tem número na medida |
| `NC-01`… | Necessidade candidata | |
| `MC-01`… | Métrica candidata | |
| `L01`… | Lacuna | `L01` a `L09` são as lacunas L1 a L9 da [utility tree](atam/utility-tree-inicial.md#lacunas-das-folhas), com o mesmo conteúdo. Novas lacunas começam em `L10` |
| `DP-01`… | Decisão pendente | |
| `FE-01`… | Fora de escopo | Seção 5 |

**Convenção dos estados da reserva.** No texto natural, os estados seguem a especificação (por exemplo, CONCLUÍDA e NÃO COMPARECEU). Nos identificadores técnicos (RNs, código, banco e testes), os nomes ficam sem acento, em maiúsculas e com sublinhado:

| Texto natural (especificação) | Identificador técnico |
|---|---|
| SOLICITADA | `SOLICITADA` |
| APROVADA | `APROVADA` |
| EM_USO | `EM_USO` |
| CONCLUÍDA | `CONCLUIDA` |
| rejeitada | `REJEITADA` |
| cancelada | `CANCELADA` |
| não compareceu (NÃO COMPARECEU) | `NAO_COMPARECEU` |

### 4.2 Escopos do produto

**E0 não é escopo funcional de usuário.** Nenhum perfil executa uma operação de E0: ele agrupa os requisitos de qualidade e de entrega que valem para o produto inteiro.

| Escopo | Nome | Do que trata | RFs | RNFs |
|---|---|---|---|---|
| E0 | Qualidade transversal | Cobertura de testes, rastreabilidade na RTM e análise estática do produto inteiro | — | RNF-01, RNF-02, RNF-03, RNF-04 |
| E1 | Identidade e acesso | Autenticação, perfis de acesso e autorização por operação | RF-01 | RNF-06 |
| E2 | Recursos | Cadastro, consulta e pesquisa de salas, professores e materiais | RF-02, RF-03 | RNF-09 |
| E3 | Reservas e conflitos | Criação, alteração e cancelamento de reservas; sobreposição de horários, inclusive na agenda do professor; solicitações simultâneas | RF-04, RF-05, RF-06 | RNF-05, RNF-07, RNF-09 |
| E4 | Aprovação e manutenção | Aprovação de recursos restritos e bloqueio de recursos em manutenção | RF-07, RF-08 | — |
| E5 | Retirada e devolução | Retirada e devolução de materiais e equipamentos | RF-09 | — |
| E6 | Auditoria e notificação | Histórico auditável de mudanças e notificação simulada ou por API externa | RF-10, RF-11 | RNF-10 |
| E7 | Relatórios | Utilização por recurso, carga horária alocada e conflitos evitados | RF-12 | — |
| E8 | Interface e documentação | Interface responsiva, mensagens de erro compreensíveis e documentação da API ou dos fluxos públicos | RF-13, RF-14 | RNF-07, RNF-08, RNF-11 |

### 4.3 Requisitos funcionais

Texto literal da especificação, na ordem oficial. As colunas "Utility tree" apontam as folhas (F), cenários (CEN) e casos de teste (CT) da [utility tree](atam/utility-tree-inicial.md) que já citam o requisito.

| ID | Requisito (texto da especificação) | Escopo | Utility tree | Situação |
|---|---|---|---|---|
| RF-01 | Autenticação e autorização por perfil. | E1 | F5 | Oficial |
| RF-02 | Cadastro e consulta de salas, professores e materiais. | E2 | — | Oficial |
| RF-03 | Pesquisa por tipo, capacidade, localização, competência e disponibilidade. | E2 | — | Oficial |
| RF-04 | Criação, alteração e cancelamento de reservas. | E3 | — | Oficial |
| RF-05 | Detecção de sobreposição de horários, inclusive na agenda do professor. | E3 | F1 · CEN-01 · CT-01 | Oficial |
| RF-06 | Proteção contra dupla reserva em solicitações simultâneas. | E3 | F2 · CEN-02 · CT-02 | Oficial |
| RF-07 | Aprovação obrigatória para recursos restritos. | E4 | F4 · evento de disparo do CEN-03 e do CT-03 | Oficial |
| RF-08 | Bloqueio de recursos em manutenção. | E4 | — | Oficial |
| RF-09 | Registro de retirada e devolução de materiais e equipamentos. | E5 | — | Oficial |
| RF-10 | Histórico auditável de mudanças. | E6 | F6 | Oficial |
| RF-11 | Notificação simulada ou integração com API externa. | E6 | F7 · F8 · CEN-03 · CT-03 | Oficial |
| RF-12 | Relatório de utilização por recurso, carga horária alocada e conflitos evitados. | E7 | — | Oficial |
| RF-13 | Interface responsiva e mensagens de erro compreensíveis. | E8 | — | Oficial |
| RF-14 | Documentação de API ou dos fluxos públicos do sistema. | E8 | — | Oficial |

A folha F3 (transições de estado) não tem RF específico: ela se liga às regras RN-05 e RN-06.

### 4.4 Regras de negócio

As 8 regras críticas da especificação viram 9 RNs: o bloco de estados foi separado em fluxo principal (RN-05) e estados alternativos (RN-06). A coluna "Texto da especificação" guarda a redação original.

| ID | Regra | Texto da especificação | RF relacionado | Utility tree e lacunas | Situação |
|---|---|---|---|---|---|
| RN-01 | O término da reserva deve ser posterior ao início. | "O término deve ser posterior ao início." | RF-04 | — | Oficial |
| RN-02 | Reservas do mesmo recurso (sala, material ou professor) não podem se sobrepor. | "Reservas do mesmo recurso não podem se sobrepor — sala, material ou professor." | RF-05 | F1 · L01, L02 | Oficial |
| RN-03 | Duas solicitações simultâneas devem produzir uma única reserva aceita. | "Duas solicitações simultâneas devem produzir uma única reserva aceita." | RF-06 | F2 · L02, L03 | Oficial |
| RN-04 | Recurso em manutenção não pode ser reservado. | "Recursos em manutenção não podem ser reservados." | RF-08 | L13, L14 | Oficial |
| RN-05 | Fluxo principal de estados: `SOLICITADA → APROVADA → EM_USO → CONCLUIDA`. | "SOLICITADA → APROVADA → EM_USO → CONCLUÍDA" | RF-04, RF-07 | F3 · L04 | Oficial |
| RN-06 | Estados alternativos: `REJEITADA`, `CANCELADA` e `NAO_COMPARECEU`. | "Estados alternativos: rejeitada, cancelada e não compareceu." | RF-04, RF-07 | F3, F6 · L02 · DP-01 | Oficial |
| RN-07 | Somente o perfil Responsável aprova recursos restritos. | "Somente responsáveis aprovam recursos restritos." | RF-07, RF-01 | F4 · L05 · DP-06 | Oficial |
| RN-08 | Reservas iniciadas não podem ser apagadas. | "Reservas iniciadas não podem ser apagadas." | RF-04, RF-10 | L11 | Oficial |
| RN-09 | Toda mudança de estado gera auditoria. | "Toda mudança de estado deve gerar auditoria." | RF-10 | F6 · L07 | Oficial |

- RN-05 e RN-06 definem os estados. A especificação não lista todas as transições permitidas entre eles (por exemplo, de quais estados se chega a `CANCELADA`): elas serão detalhadas na spec do ciclo de vida (T-21), sem contradizer estas RNs. Ver L04.
- Que o Solicitante altera e cancela **somente** as próprias reservas **não** é regra crítica da especificação: está registrado como necessidade candidata NC-01.

### 4.5 Requisitos não funcionais

A especificação não tem uma lista de RNF. Ela exige, na estratégia de qualidade, os itens não funcionais "JMeter, autorização, validação de entrada e tratamento seguro de erros", e fixa quatro metas mensuráveis. Os RNFs abaixo transformam isso em medida numérica.

| ID | Requisito | Medida | Escopo | Classificação | Origem e verificação prevista |
|---|---|---|---|---|---|
| RNF-01 | Cobertura de linhas | ≥ 80 % | E0 | **Oficial** | Metas mensuráveis da especificação · gate do JaCoCo (T-27) |
| RNF-02 | Cobertura de branches | ≥ 70 % | E0 | **Oficial** | Metas mensuráveis da especificação · gate do JaCoCo (T-27) |
| RNF-03 | Rastreabilidade dos requisitos críticos | 100 % dos requisitos críticos na RTM | E0 | **Oficial** | Meta "100% dos requisitos críticos na RTM" · comprovação: cada linha aponta evidência (entregável "RTM.md ligando requisito, risco, teste e evidência") · T-32, T-50 |
| RNF-04 | Defeitos críticos | 0 bugs ou vulnerabilidades críticas | E0 | **Oficial** | Metas mensuráveis da especificação · SonarCloud (T-29) |
| RNF-05 | Proteção contra dupla reserva | Sob N solicitações simultâneas ao mesmo recurso e intervalo: exatamente 1 reserva aceita e 0 duplicidades ativas | E3 | **Derivada** (1 aceita: RN-03; 0 duplicidades ativas: RF-06, proteção contra dupla reserva) · **Candidata** (N = 20: MC-02; 5 repetições: MC-03) | RF-06, RN-03 · teste de concorrência (T-37) · L03 |
| RNF-06 | Autorização por perfil | 0 operações executadas por perfil não autorizado; 100 % das células da matriz perfil × operação com o resultado esperado | E1 | **Derivada** (0 operações não autorizadas: limitador "falha de autorização"; RF-01) · **Candidata** (100 % das células: critério futuro da matriz, a criar na T-48 e testar na T-49) | Matriz na T-48 e teste parametrizado na T-49 · L06 |
| RNF-07 | Validação de entrada | 100 % das entradas inválidas catalogadas recusadas; 0 gravações inválidas | E3, E8 | Exigência **oficial** ("validação de entrada") · **Candidata** (100 % do catálogo, ainda a definir nas specs) · **Derivada** (0 gravações inválidas) | T-22, T-49 |
| RNF-08 | Tratamento seguro de erros | 0 respostas de erro contendo stack trace, SQL, nome de classe ou caminho interno | E8 | **Derivada** (item oficial "tratamento seguro de erros") | T-22, T-49 · CEN-03 |
| RNF-09 | Desempenho da consulta de disponibilidade e da criação de reserva | MC-01: p95 ≤ 300 ms com 50 usuários simultâneos e erro real < 1 % | E2, E3 | **Candidata** | Valor de exemplo da Aula 10, que pede a proposta "para o grupo validar". Validação e decisão final na T-38 (DP-07). As respostas 409 esperadas (conflito de reserva) **não** são falhas reais |
| RNF-10 | Resiliência da notificação | Falha da notificação (HTTP 500 ou timeout) não desfaz a reserva: 0 reversões. Timeouts configuráveis, nunca fixos no código: MC-04 (conexão 500 ms) e MC-05 (leitura 1500 ms) | E6 | **Candidata** | RF-11 e item oficial "tratamento seguro de erros" · preservação da reserva: decisão proposta na utility tree (L08) · timeouts: DP-08 · T-31, CEN-03 |
| RNF-11 | Responsividade | Telas utilizáveis a partir de 375 px de largura (MC-06) | E8 | **Candidata** | RF-13 · validação posterior na T-44 |

Nenhum número candidato é meta aprovada. Pela Aula 10, o RNF de desempenho é escrito antes da execução do teste de carga e o resultado é registrado como "atende", "não atende" ou "inconclusivo" (T-38).

### 4.6 Necessidades candidatas

Nenhuma necessidade candidata é requisito oficial. Elas vêm das personas (seção 3) e ficam pendentes de validação até a spec da task relacionada.

| ID | Necessidade | Perfil (persona) | O que a especificação diz | Relação | Task futura |
|---|---|---|---|---|---|
| NC-01 | O Solicitante altera e cancela somente as próprias reservas | Solicitante (Marina) | O perfil "cria, altera ou cancela suas reservas", mas isso não está entre as regras críticas | RF-04, RF-01 | T-21, T-48, T-49 |
| NC-02 | O Solicitante visualiza o estado das próprias reservas | Solicitante (Marina) | Define os estados, mas não diz o que o Solicitante vê | RF-04, RF-13, RN-05, RN-06 | T-45 |
| NC-03 | Notificação de aprovação ou rejeição | Solicitante (Marina) | O RF-11 exige notificação, mas não define os eventos | RF-11 · DP-03 | T-31 |
| NC-04 | O Responsável rejeita a solicitação de recurso restrito | Responsável (Rogério) | Define o estado rejeitada, mas não atribui essa transição a um perfil | RF-07, RN-06 | T-34, T-46 |
| NC-05 | O Responsável consulta o histórico | Responsável (Rogério) | O RF-10 não define quem consulta o histórico | RF-10 · DP-02 | T-40, T-46 |
| NC-06 | O Responsável visualiza os pedidos pendentes | Responsável (Rogério) | Não menciona | RF-07, RF-13 | T-46 |
| NC-07 | O Administrador atribui ou altera perfis | Administrador (Patrícia) | O perfil "gerencia [...] usuários", sem detalhar os perfis | RF-01 | T-46, T-48 |
| NC-08 | O Administrador consulta o relatório de utilização | Administrador (Patrícia) | O RF-12 não diz qual perfil consulta o relatório | RF-12 | T-41, T-46 |

**Necessidades candidatas da seção 3 → seção 4.** A seção 3 não foi alterada; esta tabela mostra para onde cada necessidade candidata das personas foi levada.

| Necessidade candidata na seção 3 | Persona | Registro na seção 4 |
|---|---|---|
| Acompanhar o estado da reserva | Marina | NC-02 |
| Ser avisada da aprovação ou da rejeição | Marina | NC-03 |
| Rejeitar solicitações de recursos restritos | Rogério | NC-04 |
| Consultar o histórico de mudanças de estado | Rogério | NC-05 |
| Ver os pedidos que aguardam aprovação | Rogério | NC-06 |
| Registrar que o solicitante não compareceu | Rogério | DP-01 |
| Atribuir ou alterar perfis de acesso | Patrícia | NC-07 |
| Impedir operações administrativas sem autorização | Patrícia | Coberta pelo RF-01 e pelo RNF-06; a matriz detalhada fica na T-48 (L06) |
| Consultar o relatório de utilização | Patrícia | NC-08 |
| Saber quais reservas são afetadas por uma manutenção | Patrícia | L14 e FE-03 |

### 4.7 Métricas candidatas

Valores propostos, sem origem oficial. Só viram meta depois da validação indicada.

| ID | Valor proposto | Usado em | Origem do número | Quem valida | Onde se decide |
|---|---|---|---|---|---|
| MC-01 | p95 ≤ 300 ms com 50 usuários simultâneos e erro real < 1 % | RNF-09 | Exemplo do material da Aula 10 e da T-38 | Equipe (a Aula 10 pede a proposta "para o grupo validar") | T-38 (DP-07) |
| MC-02 | N = 20 solicitações simultâneas | RNF-05 | T-37 · CEN-02 e CT-02 da utility tree | Professor e equipe (utility tree) | T-37 |
| MC-03 | 5 repetições independentes do teste de concorrência | RNF-05 | T-37 · CT-02 da utility tree | Professor e equipe (utility tree) | T-37 |
| MC-04 | Timeout de conexão de 500 ms | RNF-10 | T-31 · CEN-03 da utility tree | Professor e equipe (utility tree) | T-31 (DP-08) |
| MC-05 | Timeout de leitura de 1500 ms | RNF-10 | T-31 · CEN-03 da utility tree | Professor e equipe (utility tree) | T-31 (DP-08) |
| MC-06 | Telas utilizáveis a partir de 375 px de largura | RNF-11 | Critério de aceite da T-44 | Equipe | T-44 |

- O backlog da T-09 cita "p95 < 2 s" apenas como exemplo de RNF medível. Esse valor **não foi adotado**: conflita com o exemplo da Aula 10 (MC-01), e nenhum dos dois é oficial.
- As métricas candidatas de desenho de teste da utility tree (as 21 verificações do CEN-01 e o atraso simulado de 2000 ms do CEN-03) continuam registradas lá.

### 4.8 Lacunas

`L01` a `L09` apenas referenciam as lacunas L1 a L9 da [utility tree](atam/utility-tree-inicial.md#lacunas-das-folhas), onde está o texto completo. As lacunas `L10` a `L14` vêm das premissas e lacunas das personas (seção 3).

| ID | Lacuna | Origem | Relação |
|---|---|---|---|
| L01 | Uma reserva que termina exatamente quando outra começa conta como sobreposição? | Utility tree, L1 | RN-02 |
| L02 | Quais estados ocupam o horário? | Utility tree, L2 | RN-02, RN-03, RN-05, RN-06 |
| L03 | O que é "simultâneo" e que resposta recebe a solicitação recusada? | Utility tree, L3 | RN-03, RNF-05 |
| L04 | A reserva de um recurso não restrito passa por `APROVADA`? | Utility tree, L4 | RN-05, RF-07 |
| L05 | O que torna um recurso "restrito" e como ele é marcado? | Utility tree, L5 | RN-07, RF-07 |
| L06 | Não existe matriz oficial perfil × operação | Utility tree, L6 | RF-01, RNF-06 · T-48 |
| L07 | Qual o conteúdo mínimo do registro de auditoria? | Utility tree, L7 | RN-09 · T-40 |
| L08 | O que acontece com a reserva quando a notificação falha? | Utility tree, L8 | RNF-10 · T-31 |
| L09 | Notificações pendentes precisam ser reenviadas? | Utility tree, L9 | FE-05 |
| L10 | O coordenador tem permissões além das do professor no perfil Solicitante? | Seção 3 (Marina) | Perfil Solicitante · FE-02 |
| L11 | Até quando uma reserva pode ser alterada ou cancelada? Cancelar uma reserva iniciada conta como "apagar"? | Seção 3 (Marina) | RN-08, RF-04 · T-21 |
| L12 | O que significa, na prática, "validar a alocação de docentes" (aprovação explícita ou conferência)? | Seção 3 (Rogério) | Perfil Responsável · RF-05 |
| L13 | Bloqueio é diferente de período de manutenção? | Seção 3 (Patrícia) | RF-08, RN-04 |
| L14 | O que acontece com as reservas existentes quando o recurso entra em manutenção? | Seção 3 (Patrícia) | RN-04 · FE-03 · T-35 |

### 4.9 Decisões pendentes

| ID | Decisão | Por que está pendente | Quem decide | Task relacionada |
|---|---|---|---|---|
| DP-01 | Quem registra `NAO_COMPARECEU` | A especificação define o estado, mas não o perfil nem o mecanismo. A T-10 traz um job diário como adaptação proposta | Equipe | T-10, T-21 |
| DP-02 | Quais perfis consultam a auditoria | O RF-10 não define. A T-40 propõe Responsável e Administrador | Equipe | T-40, T-48 |
| DP-03 | Quais eventos disparam notificações | O RF-11 não define. A T-31 propõe aprovação, rejeição e cancelamento | Equipe | T-31 |
| DP-04 | Quem registra a retirada e a devolução | O Responsável "acompanha" retiradas e devoluções. A T-42 propõe que ele também registre | Equipe | T-42 |
| DP-05 | Se o Administrador cria reservas | Os perfis da especificação não dizem | Equipe | T-48 |
| DP-06 | Relação entre "solicitação especial" e "recurso restrito" | O Responsável "aprova solicitações especiais"; o RF-07 e a RN-07 falam em recursos restritos. Os termos não são tratados como sinônimos | Professor | T-33, T-34 |
| DP-07 | Valores finais de desempenho | MC-01 é exemplo de aula, não meta | Equipe | T-38 |
| DP-08 | Timeouts da integração externa | MC-04 e MC-05 vêm do backlog, não da especificação | Professor e equipe | T-31 |

## 5. Fora de escopo

Itens que ficam fora da primeira versão (entrega da semana 46). Nenhum deles é pedido pela especificação.

| ID | Item | Justificativa |
|---|---|---|
| FE-01 | Reservas recorrentes | A especificação trata da criação, alteração e cancelamento de reservas (RF-04) sem citar recorrência. O backlog da T-09 cita a reserva recorrente como exemplo de item fora da v1 |
| FE-02 | Reservas em nome de terceiros | O Solicitante cria, altera ou cancela "suas reservas", e nenhum perfil da especificação reserva por outra pessoa. Ver L10 |
| FE-03 | Remanejamento automático por manutenção | A especificação só proíbe reservar recurso em manutenção (RN-04). Não há regra de realocação; o que acontece com reservas já existentes é a lacuna L14 |
| FE-04 | Envio real por e-mail, SMS ou push | O RF-11 aceita "notificação simulada ou integração com API externa". A integração é isolada e testada com WireMock (T-31) |
| FE-05 | Reenvio automático de notificações pendentes | A especificação não exige reenvio. A lacuna L09 registra que pendências e reprocessamento são suposição de projeto |
| FE-06 | Aplicativo móvel nativo | O RF-13 pede interface responsiva, atendida pela interface web (Thymeleaf e Bootstrap) e pelo RNF-11 |
| FE-07 | SSO ou integração com sistema acadêmico | O RF-01 pede autenticação e autorização por perfil, sem provedor externo. A especificação não cita sistemas acadêmicos |
| FE-08 | Pagamento ou cobrança | A especificação não trata de pagamento nem de cobrança pelo uso dos recursos |

Diferente do case Foot Fanatics das aulas, o **painel administrativo não está fora de escopo**: o perfil Administrador é oficial e suas operações fazem parte do produto (RF-02, RF-08 e as telas da T-46).

## 6. Métricas de sucesso

**Metas oficiais** (metas mensuráveis da especificação):

| Métrica | Meta | RNF | Evidência prevista |
|---|---|---|---|
| Cobertura de linhas | ≥ 80 % | RNF-01 | Relatório do JaCoCo no CI (T-27) |
| Cobertura de branches | ≥ 70 % | RNF-02 | Relatório do JaCoCo no CI (T-27) |
| Requisitos críticos na RTM | 100 % | RNF-03 | `docs/rtm.md` (T-32, T-50) |
| Bugs ou vulnerabilidades críticas | 0 | RNF-04 | Quality Gate do SonarCloud (T-29) |

A especificação alerta que cobertura alta, sozinha, não garante qualidade: testes triviais feitos só para elevar o percentual não asseguram a pontuação máxima. Os limitadores de nota (aplicação que não executa, pipeline ausente, falha de autorização e possibilidade de dupla reserva) são condições que precisam ser evitadas, não metas: o RNF-05 transforma em medida o limitador de dupla reserva, e o RNF-06, o de falha de autorização.

**Métricas candidatas** (validação pendente, não são metas aprovadas):

| ID | Métrica | Valor proposto | RNF | Situação |
|---|---|---|---|---|
| MC-01 | Desempenho da consulta de disponibilidade e da criação de reserva | p95 ≤ 300 ms com 50 usuários simultâneos e erro real < 1 % (409 esperado não conta como erro) | RNF-09 | Candidata · decisão na T-38 (DP-07) |
| MC-02 | Solicitações simultâneas no teste de concorrência | 20 | RNF-05 | Candidata · T-37 |
| MC-03 | Repetições do teste de concorrência | 5 | RNF-05 | Candidata · T-37 |
| MC-04 | Timeout de conexão da notificação | 500 ms | RNF-10 | Candidata · T-31 (DP-08) |
| MC-05 | Timeout de leitura da notificação | 1500 ms | RNF-10 | Candidata · T-31 (DP-08) |
| MC-06 | Largura mínima utilizável das telas | 375 px | RNF-11 | Candidata · T-44 |
