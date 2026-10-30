# PRD: Organização de Recursos

> Esqueleto criado na semana 34 (Aula 03). Preenchido nas tasks T-04 (personas) e T-09 (PRD completo).

## 1. Visão

TODO

## 2. Problema

TODO

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

TODO (T-09): requisitos funcionais (RF-01...) e não funcionais (RNF-01...), numerados, medíveis e ligados ao escopo de origem. Regras de negócio numeradas (RN-01...).

## 5. Fora de escopo

TODO

## 6. Métricas de sucesso

TODO
