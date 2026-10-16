#!/usr/bin/env python3
"""Gera docs/planejamento/backlog.md e docs/planejamento/backlog-jira.csv a partir de uma única fonte.

Uso: python3 scripts/gerar-backlog.py

Para mudar uma task, edite a lista TASKS abaixo e rode o script de novo.
As tasks seguem as semanas do cronograma da disciplina (32 a 47). Cada semana
cobre o que o professor pede na aula correspondente, aplicado ao projeto.
"""
import csv
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent

PESSOAS = {
    "R": ("Renata", "@renatarsilva"),
    "S": ("Samuel", "@Samwel-Merces"),
    "C": ("Caio", "@CaiosAguiar"),
    "T": ("Todos", "time"),
}

AULAS = {
    32: "Aula 01 · Introdução à Qualidade de Software",
    33: "Aula 02 · ATAM e planejamento de casos de teste",
    34: "Aula 03 · Caixa branca × caixa preta (nasce o projeto)",
    35: "Aula 04 · Arquitetura e ATAM com IA agêntica",
    36: "Aula 05 · Gestão de erros e bugs (TDD fase red)",
    37: "Aula 06 · Testes automatizados em CI (fase green)",
    38: "Aula 07 · JUnit 5, JaCoCo e SonarCloud",
    39: "Aula 08 · WireMock, Testcontainers e chamadas seguras",
    41: "Aula 09 · TDD e BDD no projeto",
    42: "Aula 10 · Carga e concorrência",
    43: "Aula 11 · Auditoria, relatórios e requisitos restantes",
    44: "Aula 12 · Autorização, RTM e interface",
    45: "Aula 13 · Preparação para a prova oral",
    46: "Aula 14 · Entrega do trabalho final",
    47: "Prova oral",
}

# Semana do curso → sprint real (o time está comprimindo 15 semanas em 4)
SPRINT_DA_SEMANA = {32: 1, 33: 1, 34: 1, 35: 1, 36: 2, 37: 2, 38: 2, 39: 2,
                    41: 3, 42: 3, 43: 3, 44: 4, 45: 4, 46: 4, 47: 4}
SPRINTS = {
    1: "Sprint 1 · 15/10 a 21/10 · semanas 32 a 35",
    2: "Sprint 2 · 22/10 a 28/10 · semanas 36 a 39",
    3: "Sprint 3 · 29/10 a 04/11 · semanas 41 a 43",
    4: "Sprint 4 · 05/11 a 11/11 · semanas 44 a 46 (prova oral na semana 47)",
}

AREA_LABEL = {"Backend": "area-backend", "Frontend": "area-frontend", "Infra": "area-infra",
              "Documentação": "area-documentacao", "Testes": "area-testes"}

# fluxo: "sem-spec" = PR direto; "spec" = spec + revisão + validação;
#        "critica" = spec + revisão + validação + grill; "mesma-spec" = usa a spec de outra task
FLUXO_TXT = {"sem-spec": "Sem spec (PR direto)", "spec": "Spec + revisão + validação",
             "critica": "Regra crítica: spec + revisão + validação + grill-with-docs",
             "mesma-spec": "Usa a spec da task indicada"}
FLUXO_CURTO = {"sem-spec": "—", "spec": "spec", "critica": "spec + grill", "mesma-spec": "spec de outra task"}

# Prioridade Jira: Highest = protege contra limitador de nota; High = regra crítica ou entregável obrigatório
TASKS = [
    # ───────────── Semana 32 · Aula 01 ─────────────
    dict(id="T-01", sem=32, area="Infra", dono="T", fluxo="sem-spec", prio="Highest",
         titulo="Preparar o ambiente de cada integrante",
         oque="Cada integrante instala Java 21, Docker e IntelliJ; clona o repositório; configura git config user.name e user.email com o e-mail da conta do GitHub; abre o projeto no IntelliJ e roda ./mvnw verify até ficar verde; faz um commit de teste numa branch própria; entra no Discord da turma.",
         porque="Na Aula 01 o professor exige que ninguém saia da sala sem um push funcionando. Sem o e-mail certo no git, os commits não aparecem no seu nome, e a autoria vale 10% da nota.",
         aceite=["As três pessoas rodaram ./mvnw verify verde na própria máquina",
                 "Um commit de cada pessoa aparece no GitHub com o avatar correto"],
         arquivos="—", dep="T-07", req="—"),
    dict(id="T-02", sem=32, area="Infra", dono="R", fluxo="sem-spec", prio="High",
         titulo="Configurar Jira, board e proteção da branch main",
         oque="Seguir docs/processo/configuracao-github-e-jira.md: merge commit como única forma de merge, ruleset protegendo a main (PR obrigatório, 1 aprovação, sem bypass), colunas do board, importação deste backlog e app GitHub for Jira.",
         porque="A especificação pede 'repositório com histórico e revisão por pares'. A proteção da main garante que todo código passa por PR revisado por outra pessoa. A Aula 06 também pede branch protection.",
         aceite=["Push direto na main é recusado", "As 58 tasks estão no Jira, nas sprints certas",
                 "Branch com a chave KAN aparece dentro da task no Jira"],
         arquivos="docs/processo/configuracao-github-e-jira.md", dep="—", req="—"),
    dict(id="T-03", sem=32, area="Documentação", dono="R", fluxo="sem-spec", prio="Medium",
         titulo="Diagnóstico de qualidade do projeto da disciplina (Atividade #1)",
         oque="Clonar github.com/afonsolelis/qualidade_software, seguir o README, rodar a suíte e registrar o que passou, falhou, foi pulado e o que não tem teste. Ler o código procurando problemas que a suíte não pega. Escrever um documento de 1 a 2 páginas com a saída dos testes, no mínimo 5 achados (onde, o quê, por que é problema), a característica ISO/IEC 25010 de cada um e a priorização com critério explícito. Não escrever testes.",
         porque="É a primeira etapa da evolução do projeto na especificação ('diagnóstico inicial'). Treina o vocabulário da ISO 25010, que cai na prova escrita e na oral. Cinco achados bem argumentados valem mais do que vinte listados.",
         aceite=["Pelo menos 5 achados com onde, o quê e por quê",
                 "Cada achado ligado a uma das 9 características da ISO 25010:2023",
                 "Critério de priorização explícito", "Nenhuma credencial do repositório analisado copiada para o documento"],
         arquivos="docs/diagnostico/diagnostico-inicial.md", dep="—", req="—"),
    # ───────────── Semana 33 · Aula 02 ─────────────
    dict(id="T-04", sem=33, area="Documentação", dono="C", fluxo="sem-spec", prio="High",
         titulo="Personas dos três perfis",
         oque="Escrever 3 personas (Solicitante, Responsável, Administrador), cada uma com objetivo, contexto e frustração, na seção 3 do docs/prd.md.",
         porque="A Aula 02 pede 3 personas como base para elicitar requisitos. Na Aula 03 elas viram a origem dos RF e RNF; na Aula 04, os stakeholders da arquitetura.",
         aceite=["3 personas com objetivo, contexto e frustração", "Cada persona ligada a um perfil de acesso da especificação"],
         arquivos="docs/prd.md (seção 3)", dep="T-07", req="—"),
    dict(id="T-05", sem=33, area="Documentação", dono="C", fluxo="sem-spec", prio="High",
         titulo="Utility tree, cenários (A, A) e casos de teste (Atividade #2)",
         oque="Montar uma utility tree com pelo menos 3 atributos de qualidade e 6 cenários folha, cada um com (Importância, Dificuldade) justificado em uma linha. Escrever os 3 cenários (A, A) completos nas 6 partes (fonte, estímulo, ambiente, artefato, resposta, medida). Derivar pelo menos 1 caso de teste completo por cenário (A, A): ID, título, pré-condições, dados concretos, passos, resultado esperado e rastreabilidade. Identificar 1 ponto de sensibilidade, 1 ponto de trade-off e 1 não-risco (com a premissa), um parágrafo cada.",
         porque="Liga atributo de qualidade a teste verificável. Regra da Aula 02: cenário sem número na medida da resposta não pontua. Os cenários (A, A) voltam na semana 42, no JMeter.",
         aceite=["≥ 3 atributos e ≥ 6 folhas com (I, D) justificado", "3 cenários (A, A) com medida numérica",
                 "≥ 3 casos de teste com todos os campos", "Sensibilidade, trade-off e não-risco justificados"],
         arquivos="docs/atam/utility-tree-inicial.md", dep="T-04", req="—"),
    dict(id="T-06", sem=33, area="Documentação", dono="S", fluxo="sem-spec", prio="High",
         titulo="Plano de teste (ISO/IEC/IEEE 29119-3)",
         oque="Preencher docs/plano-de-teste.md: estratégia, níveis (unitário, integração com Testcontainers, API caixa-preta, end-to-end, concorrência, carga), itens a testar e a NÃO testar, critérios de entrada, saída e suspensão, metas de cobertura (80% linhas, 70% branches), ambiente de teste, riscos e recursos.",
         porque="A especificação lista 'plano, suíte e relatório de testes' como entregável, e o peso 'estratégia e documentação de testes' vale 15%. A Aula 02 apresenta o modelo da 29119-3.",
         aceite=["Todas as seções preenchidas", "Cada nível aponta ferramenta e tipo de evidência",
                 "Lista explícita do que não será testado, com motivo"],
         arquivos="docs/plano-de-teste.md", dep="T-07", req="—"),
    # ───────────── Semana 34 · Aula 03 ─────────────
    dict(id="T-07", sem=34, area="Infra", dono="R", fluxo="sem-spec", prio="Highest",
         titulo="Criar o projeto Spring Boot 3 e o esqueleto de documentação",
         oque="Projeto Maven com Java 21, Spring Boot 3.5 (última linha 3.x estável), group br.senac e as dependências da Aula 03 (Web, Security, Data JPA, Validation, PostgreSQL, Testcontainers), mais Thymeleaf e Flyway. compose.yaml com PostgreSQL local. Esqueletos de docs/prd.md, docs/arquitetura.md e docs/plano-de-teste.md com as seções da aula. README com 'como subir' e 'como rodar os testes'. (Gerado pelo Claude em 15/10; falta o commit.)",
         porque="É o 'marco zero': a suíte roda verde sem nenhum teste escrito por vocês. A aplicação que não executa é limitador de nota.",
         aceite=["./mvnw verify verde com Java 21", "./mvnw spring-boot:run sobe com o PostgreSQL do compose",
                 "Projeto abre no IntelliJ sem erros", "docs/prd.md, docs/arquitetura.md e docs/plano-de-teste.md existem"],
         arquivos="pom.xml, compose.yaml, src/, docs/", dep="—", req="—"),
    dict(id="T-08", sem=34, area="Infra", dono="S", fluxo="sem-spec", prio="Medium",
         titulo="Instalar o AIOX no repositório",
         oque="Rodar npx aiox-core@latest install (squad development) na raiz do repositório e versionar a pasta .aiox-core/. Ler como o fluxo de histórias do AIOX se encaixa no nosso ai-workflow e registrar a conclusão no PR.",
         porque="A Aula 03 lista .aiox-core/ no checklist 'precisa existir'. É a ferramenta de agentes que o professor usa nas práticas.",
         aceite=[".aiox-core/ versionado", "Nota no PR explicando como o AIOX convive com o ai-workflow"],
         arquivos=".aiox-core/", dep="T-07", req="—"),
    dict(id="T-09", sem=34, area="Documentação", dono="R", fluxo="sem-spec", prio="High",
         titulo="PRD: visão, escopos, RF e RNF numerados e regras de negócio",
         oque="Preencher docs/prd.md: 1 Visão, 2 Problema, 3 Personas (T-04), 4 Escopos, 5 Fora de escopo, 6 Métricas de sucesso. Elicitar com IA, a partir das personas, os requisitos funcionais (RF-01 a RF-14 da especificação) e não funcionais (RNF-01...), todos os RNF medíveis (ex.: p95 < 2 s), cada um ligado ao escopo de origem. Numerar as regras de negócio (RN-01...) a partir das regras críticas. Registrar necessidades implícitas e o que fica fora da v1 (ex.: reserva recorrente).",
         porque="Requisitos e regras de negócio valem 15%. As specs, os testes e a RTM citam esses IDs: sem PRD numerado não há rastreabilidade.",
         aceite=["RF e RNF numerados e ligados a um escopo", "Todo RNF tem número na medida",
                 "Regras críticas da especificação numeradas como RN", "Seção 'fora de escopo' com justificativa"],
         arquivos="docs/prd.md", dep="T-04", req="RF-01 a RF-14"),
    # ───────────── Semana 35 · Aula 04 ─────────────
    dict(id="T-10", sem=35, area="Documentação", dono="S", fluxo="sem-spec", prio="High",
         titulo="Arquitetura: descoberta, C4 e diagramas de sequência",
         oque="Em docs/arquitetura.md: (1) descoberta: contexto, stakeholders, fonte de verdade, requisitos significativos, drivers priorizados, glossário, fatos, lacunas, suposições (S-01...) e decisões (D-01...), preservando os IDs do PRD. (2) Diagramas Mermaid C4 de contexto, contêineres e componentes. (3) Sequências críticas: criar reserva, aprovar recurso restrito, duas solicitações simultâneas e o fluxo batch diário (adaptação proposta: job no fim do dia que marca NAO_COMPARECEU e conclui reservas vencidas). (4) Comparar pelo menos 2 alternativas quando faltar dado.",
         porque="A especificação pede 'diagramas de contexto, componentes e sequências críticas'. O DoD da Aula 04: outra equipe entende, rastreia e contesta a arquitetura sem explicação oral.",
         aceite=["C4 em 3 níveis renderizando no GitHub", "Sequências online e batch",
                 "Suposições e decisões com ID", "Nenhuma decisão sem requisito de origem sem estar marcada"],
         arquivos="docs/arquitetura.md", dep="T-09", req="—"),
    dict(id="T-11", sem=35, area="Documentação", dono="C", fluxo="sem-spec", prio="High",
         titulo="Modelo de dados",
         oque="Diagrama ER em Mermaid na seção 3 de docs/arquitetura.md: usuário (com perfil), sala, professor e competências, material, reserva (com intervalo e estado), período de manutenção, retirada/devolução e auditoria. Descrever as restrições do banco que impedem conflito (FKs, unicidade, e a opção de EXCLUDE USING gist sobre tstzrange).",
         porque="O modelo é a base das migrações Flyway e das regras de sobreposição. Decidir as restrições cedo evita retrabalho na semana 42 (dupla reserva).",
         aceite=["Todas as entidades dos 14 RFs presentes", "Restrições anti-conflito descritas"],
         arquivos="docs/arquitetura.md (seção 3)", dep="T-09", req="—"),
    dict(id="T-12", sem=35, area="Documentação", dono="C", fluxo="sem-spec", prio="High",
         titulo="ATAM: utility tree, cenários, táticas e riscos",
         oque="Em docs/arquitetura.md: utility tree completa com (I, D) justificados; cenários em 6 partes (faixa marcada 'validação pendente' quando faltar meta); situações de falha aplicáveis (falha parcial, reexecução, jobs sobrepostos, dados duplicados, impacto do batch no online etc.); tática de Len Bass e métrica para cada abordagem; sensibilidades, trade-offs, riscos, não-riscos e temas de risco. Baixar o catálogo de táticas do site e guardá-lo em docs/.",
         porque="O ATAM é método de identificação de riscos. 'Arquitetura sem trade-off é sinal de análise superficial'. Cai na prova e alimenta o ciclo-01 da semana 37.",
         aceite=["Cada cenário com medida numérica ou marcado 'validação pendente'",
                 "Pelo menos 1 trade-off real e 1 tema de risco", "Catálogo de táticas versionado"],
         arquivos="docs/arquitetura.md, docs/taticas-arquiteturais-len-bass.md", dep="T-05, T-10", req="—"),
    dict(id="T-13", sem=35, area="Documentação", dono="S", fluxo="sem-spec", prio="High",
         titulo="ADRs e matriz requisito → decisão → componente → evidência",
         oque="Revisão cruzada com 6 papéis (Analista, Arquiteto, Dados, Segurança/LGPD, Ops, Custos) registrando objeções. ADRs em docs/adr/ (status, contexto, alternativas, decisão, consequências, riscos, evidência necessária, condição de revisão) para: PostgreSQL, Flyway, Thymeleaf no mesmo projeto, autenticação (sessão nas telas e token na API), Clock injetado, entidade Usuario com perfil. Matriz requisito → decisão → componente → evidência e listas de requisitos sem cobertura, decisões sem requisito, suposições, perguntas e riscos aceitos.",
         porque="ADR é a memória das decisões e assunto certo da prova oral ('por que PostgreSQL?'). A matriz é o começo da rastreabilidade.",
         aceite=["Um ADR por decisão, no template", "Matriz cobrindo os 14 RFs", "Listas de lacunas e riscos preenchidas"],
         arquivos="docs/adr/, docs/arquitetura.md", dep="T-10", req="—"),
    # ───────────── Semana 36 · Aula 05 ─────────────
    dict(id="T-14", sem=36, area="Documentação", dono="R", fluxo="sem-spec", prio="High",
         titulo="Processo e registro de defeitos",
         oque="Criar docs/defeitos/README.md com o ciclo de vida (Novo → Triado → Em correção → Em verificação → Fechado; Duplicado, Não reproduzível, Não será corrigido, Reaberto), severidade × prioridade, 5 porquês e métricas (sem contar bugs por pessoa). Criar .github/ISSUE_TEMPLATE/bug.md com as seções da Aula 05 e labels bug e needs-triage. Registrar e triar os primeiros defeitos encontrados (ex.: divergências achadas na arquitetura).",
         porque="Registro de defeitos é entregável, e rastreabilidade e gestão de defeitos valem 10%. Nas semanas 42 e 44 o professor manda registrar defeito antes de corrigir.",
         aceite=["Template de relato reproduzível com dados fictícios (domínio .invalid)",
                 "Pelo menos 1 defeito registrado e triado com severidade e prioridade justificadas"],
         arquivos="docs/defeitos/, .github/ISSUE_TEMPLATE/bug.md", dep="—", req="—"),
    dict(id="T-15", sem=36, area="Backend", dono="S", fluxo="critica", prio="Highest",
         titulo="Autenticação e autorização por perfil",
         oque="Spring Security com os perfis SOLICITANTE, RESPONSAVEL e ADMINISTRADOR; usuários em banco (migração Flyway) com senha em hash; login por formulário (sessão) para as telas e endpoint de login que devolve token para a API (MockMvc, Cucumber e JMeter usam token); gestão de usuários pelo Administrador; 401 sem autenticação e 403 para perfil sem permissão, sem vazar detalhes.",
         porque="RF01. Falha de autorização é limitador de nota. As Aulas 09, 10 e 12 dependem de tokens de cada perfil.",
         aceite=["Spec liberada", "Testes de 401 e 403 por perfil passando", "Nenhuma senha em texto puro no banco ou em log"],
         arquivos="src/main/java/.../seguranca/, db/migration/", dep="T-07, T-13", req="RF-01"),
    dict(id="T-16", sem=36, area="Backend", dono="C", fluxo="spec", prio="Medium",
         titulo="Cadastro e consulta de salas",
         oque="Entidade, migração, repositório, serviço e API REST de salas (tipo, capacidade, localização, restrita ou não). Cadastro e alteração só pelo Administrador; consulta por usuários autenticados. Validação de entrada (capacidade > 0, campos obrigatórios).",
         porque="RF02. Sala é o recurso principal das reservas.",
         aceite=["Spec validada", "Testes de validação com valor limite (capacidade 0, 1)", "403 para não administrador"],
         arquivos="src/main/java/.../sala/", dep="T-15", req="RF-02"),
    dict(id="T-17", sem=36, area="Backend", dono="S", fluxo="spec", prio="Medium",
         titulo="Cadastro e consulta de professores e competências",
         oque="CRUD de professores com suas competências, restrito ao Administrador para escrita.",
         porque="RF02. O professor também é recurso reservável: a agenda dele não pode ter sobreposição.",
         aceite=["Spec validada", "Testes de validação e de permissão"],
         arquivos="src/main/java/.../professor/", dep="T-15", req="RF-02"),
    dict(id="T-18", sem=36, area="Backend", dono="S", fluxo="spec", prio="Medium",
         titulo="Cadastro e consulta de materiais e equipamentos",
         oque="CRUD de materiais (tipo, patrimônio, restrito ou não, situação), restrito ao Administrador para escrita.",
         porque="RF02. Material é o terceiro tipo de recurso e a base da retirada e devolução (RF09).",
         aceite=["Spec validada", "Testes de validação e de permissão"],
         arquivos="src/main/java/.../material/", dep="T-15", req="RF-02"),
    dict(id="T-19", sem=36, area="Testes", dono="R", fluxo="sem-spec", prio="High",
         titulo="Plano TDD: inventário de todas as regras de negócio",
         oque="Criar docs/testes/plano-tdd.md com TODAS as regras de negócio do PRD, cada uma com o ID de origem e os casos com resultado esperado: caminho feliz, valores-limite, entradas inválidas, conflitos e estados proibidos. Ambiguidades e perguntas em seção separada. Matriz regra → caso → teste → status (atualizada pelas tasks seguintes). Indicar em que semana cada regra ganha teste: intervalo, sobreposição e estados na 36/37; aprovação e manutenção na 41 (BDD); concorrência na 42; auditoria na 43; autorização na 44.",
         porque="Prompt 1 da Aula 05. É o mapa que garante que nenhuma regra fica sem teste ('só encerre quando todas as regras estiverem cobertas').",
         aceite=["Toda RN do PRD aparece com casos", "Matriz regra → caso → teste → status criada", "Não contém código nem testes"],
         arquivos="docs/testes/plano-tdd.md", dep="T-09", req="Todas as RN"),
    dict(id="T-20", sem=36, area="Testes", dono="R", fluxo="critica", prio="Highest",
         titulo="Fase red: testes de intervalo e sobreposição de horários",
         oque="Escrever a spec da reserva (intervalo e sobreposição em sala, material e professor). Depois, testes JUnit 5 para cada caso do plano TDD dessas regras, com nome citando o ID da regra e estrutura Arrange-Act-Assert. Criar só as assinaturas mínimas para compilar. Rodar e confirmar que cada teste falha por asserção, não por compilação. Commits test(red).",
         porque="Prompt 2 da Aula 05. Teste que nasce vermelho prova que testa alguma coisa. O commit vermelho é a evidência de TDD que o professor procura no histórico.",
         aceite=["Spec liberada", "≥ 1 teste por caso do plano para essas regras",
                 "Todos falham por asserção", "Matriz do plano TDD atualizada"],
         arquivos="docs/specs/, src/test/java/.../reserva/", dep="T-16, T-17, T-18, T-19", req="RF-04, RF-05"),
    dict(id="T-21", sem=36, area="Testes", dono="C", fluxo="critica", prio="High",
         titulo="Fase red: testes do ciclo de vida da reserva",
         oque="Escrever a spec do ciclo de vida: transições válidas (SOLICITADA → APROVADA → EM_USO → CONCLUIDA) e alternativas (REJEITADA, CANCELADA, NAO_COMPARECEU), transições proibidas, alteração e cancelamento pelo dono, e reserva iniciada não pode ser apagada. Depois, testes JUnit 5 parametrizados (tabela de transições) falhando por asserção. Commits test(red).",
         porque="Regras críticas da especificação. A tabela de transições é ótima para @ParameterizedTest e será usada pela aprovação (semana 41) e pela auditoria (43).",
         aceite=["Spec liberada", "Toda transição válida e inválida tem caso", "Todos falham por asserção"],
         arquivos="docs/specs/, src/test/java/.../reserva/", dep="T-19", req="RF-04"),
    dict(id="T-22", sem=36, area="Backend", dono="C", fluxo="spec", prio="High",
         titulo="Tratamento padronizado de erros e validação de entrada",
         oque="@RestControllerAdvice devolvendo ProblemDetail (RFC 9457) com mensagem compreensível em português; 400 para validação (com o campo e o motivo), 404, 409 para conflito de regra; erro inesperado vira 500 neutro, sem stack trace, SQL ou nome de classe. Nas telas, página de erro amigável.",
         porque="RF13 (mensagens compreensíveis) e 'tratamento seguro de erros'. Na Aula 12 o professor provoca um erro e lê o corpo da resposta. Fazer agora evita que cada endpoint trate erro de um jeito.",
         aceite=["Spec validada", "Teste prova que nenhum erro expõe stack trace ou SQL", "Formato único para todos os endpoints"],
         arquivos="src/main/java/.../erro/", dep="T-07", req="RF-13"),
    # ───────────── Semana 37 · Aula 06 ─────────────
    dict(id="T-23", sem=37, area="Backend", dono="R", fluxo="mesma-spec", spec_de="T-20", prio="Highest",
         titulo="Fase green: criar e alterar reserva com intervalo e sobreposição",
         oque="Implementar a criação e a alteração de reserva até todos os testes da T-20 passarem: término posterior ao início, sem sobreposição no mesmo recurso (sala, material ou professor), Clock injetado. Um caso por vez; refatorar só com tudo verde. Sem @Disabled, sem enfraquecer asserção, sem mudar o resultado esperado. Commits feat(green) e refactor.",
         porque="Prompt 1 da Aula 06. É o núcleo do sistema e a base de quase todas as outras tasks.",
         aceite=["Todos os testes da T-20 verdes", "./mvnw -B verify verde", "Matriz do plano TDD atualizada"],
         arquivos="src/main/java/.../reserva/", dep="T-20", req="RF-04, RF-05"),
    dict(id="T-24", sem=37, area="Backend", dono="C", fluxo="mesma-spec", spec_de="T-21", prio="High",
         titulo="Fase green: ciclo de vida, alteração e cancelamento",
         oque="Implementar a máquina de estados, o cancelamento e a regra de que reserva iniciada não pode ser apagada até todos os testes da T-21 passarem. Commits feat(green) e refactor.",
         porque="Regras críticas de estado. Base para aprovação (41) e auditoria (43).",
         aceite=["Todos os testes da T-21 verdes", "./mvnw -B verify verde"],
         arquivos="src/main/java/.../reserva/", dep="T-21, T-23", req="RF-04"),
    dict(id="T-25", sem=37, area="Infra", dono="S", fluxo="sem-spec", prio="Highest",
         titulo="Pipeline de CI no GitHub Actions",
         oque="Criar .github/workflows/ci.yml (name: qualidade) disparando em pull_request e push na main; permissions contents: read; concurrency com cancel-in-progress; timeout de 10 minutos; Java 21 temurin com cache Maven; ./mvnw -B verify; upload dos relatórios surefire e failsafe como artifact com if: always(). Quebrar uma asserção de propósito, registrar o run vermelho, reverter e confirmar o verde; anotar os IDs dos runs no README. Adicionar o check do CI como obrigatório na proteção da main.",
         porque="Pipeline ausente é limitador de nota. CI, cobertura e análise estática valem 15%. O professor confere o run do CI ligado à tag na entrega.",
         aceite=["CI verde em um PR", "Run vermelho intencional e recuperação registrados no README",
                 "Merge bloqueado com check falhando", "Relatórios baixáveis em sucesso e falha"],
         arquivos=".github/workflows/ci.yml, README.md", dep="T-07", req="—"),
    dict(id="T-26", sem=37, area="Documentação", dono="C", fluxo="sem-spec", prio="Medium",
         titulo="Avaliação arquitetural: ciclo 01",
         oque="Criar docs/avaliacao-arquitetural/ciclo-01.md: direcionadores de negócio, cenários em 6 partes, utility tree A/M/B, táticas encontradas no código com arquivo e linha como evidência, sensibilidades, trade-offs, riscos, não-riscos, temas de risco, divergências entre docs/arquitetura.md e o código, e ações priorizadas para o próximo ciclo.",
         porque="Prompt 3 da Aula 06: confronta o que foi planejado com o que foi construído. Divergência encontrada vira defeito ou ADR.",
         aceite=["Toda tática citada tem arquivo e linha", "Divergências listadas com ação"],
         arquivos="docs/avaliacao-arquitetural/ciclo-01.md", dep="T-12, T-23", req="—"),
    # ───────────── Semana 38 · Aula 07 ─────────────
    dict(id="T-27", sem=38, area="Infra", dono="C", fluxo="sem-spec", prio="High",
         titulo="JaCoCo com gate de 80% de linhas e 70% de branches",
         oque="Adicionar o JaCoCo 0.8.15 ao pom.xml (prepare-agent, report e check na fase verify com LINE 0.80 e BRANCH 0.70, relatórios HTML e XML). Confirmar que o build falha abaixo da meta e volta a passar. Publicar o relatório como artifact no ci.yml.",
         porque="A meta 80/70 é piso da especificação. Sem o gate no verify, a cobertura pode cair sem ninguém perceber.",
         aceite=["verify falha abaixo de 80/70 (com evidência)", "Relatório JaCoCo como artifact do CI"],
         arquivos="pom.xml, .github/workflows/ci.yml", dep="T-25", req="—"),
    dict(id="T-28", sem=38, area="Testes", dono="R", fluxo="sem-spec", prio="High",
         titulo="Cobertura de ramo nas regras de sobreposição",
         oque="Listar as linhas e branches não cobertos nas regras de reserva, classificando cada um como lacuna de risco ou cosmética. Testar só as lacunas de risco com @ParameterizedTest na trinca -1 / 0 / +1 (ex.: reserva que termina 1 s antes, exatamente no início e 1 s depois de outra) e Clock fixo. Atualizar a matriz do plano TDD.",
         porque="Entregável da especificação para a semana 38: 'cobertura de ramo nas regras de sobreposição'. Testar getters para subir a cobertura não pontua.",
         aceite=["100% de branches na regra de sobreposição", "Nenhum teste sem asserção", "Lista de lacunas com classificação"],
         arquivos="src/test/java/.../reserva/", dep="T-23, T-27", req="RF-05"),
    dict(id="T-29", sem=38, area="Infra", dono="S", fluxo="sem-spec", prio="High",
         titulo="SonarCloud com Quality Gate no PR",
         oque="Importar o repositório no SonarCloud; a Renata (admin) cria o secret SONAR_TOKEN; configurar sonar.organization e sonar.projectKey no pom.xml; desligar a análise automática; step no ci.yml com fetch-depth: 0 e ./mvnw -B verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar; definir 'novo código'. Tratar cada issue (corrigir, aceitar com justificativa ou falso positivo com evidência). Atualizar o ciclo-01 com as medidas do JaCoCo e do SonarCloud.",
         porque="A especificação exige SonarCloud e a meta de 0 bugs ou vulnerabilidades críticas. Marcar falso positivo para 'ficar verde' não vale.",
         aceite=["Quality Gate aparece e passa no PR", "Nenhum token versionado", "ciclo-01 atualizado com as medidas"],
         arquivos="pom.xml, .github/workflows/ci.yml, docs/avaliacao-arquitetural/ciclo-01.md", dep="T-25, T-26", req="—"),
    # ───────────── Semana 39 · Aula 08 ─────────────
    dict(id="T-30", sem=39, area="Testes", dono="C", fluxo="sem-spec", prio="High",
         titulo="Testes de integração com PostgreSQL real (Testcontainers)",
         oque="Criar a base de testes de integração com Testcontainers (PostgreSQL real, sem H2) e cobrir os repositórios e as restrições do banco: migrações Flyway aplicadas, FKs, unicidade e consultas de disponibilidade.",
         porque="A especificação exige que a persistência seja verificada em ambiente real. A Aula 10 depende dessa base para o teste de concorrência.",
         aceite=["Testes de integração rodando no CI", "Nenhum uso de H2 ou mock de repositório nesses testes"],
         arquivos="src/test/java/.../integracao/", dep="T-23", req="—"),
    dict(id="T-31", sem=39, area="Backend", dono="S", fluxo="spec", prio="High",
         titulo="Notificação externa com WireMock",
         oque="Porta Notificador e adapter HTTP para um serviço externo de notificação (avisar aprovação, rejeição e cancelamento). URL base, token e timeouts por configuração (connect 500 ms, read 1500 ms), nunca fixos. Testes com WireMock: INT-01 200 válido, INT-02 404, INT-03 500 → 503 controlado, INT-04 timeout não persiste, INT-05 JSON inválido rejeitado. Teste negativo: o provedor devolve o próprio token no corpo de erro e ele não vaza pela API. Falha na notificação não quebra a reserva.",
         porque="RF11 e Aula 08. Mostra isolamento de dependência externa, que é exigido pela estratégia de qualidade.",
         aceite=["Spec validada", "Matriz INT-01 a INT-05 testada", "Suíte roda offline e no CI",
                 "Nenhuma credencial real em código, fixture ou log"],
         arquivos="src/main/java/.../notificacao/", dep="T-24", req="RF-11"),
    dict(id="T-32", sem=39, area="Documentação", dono="R", fluxo="sem-spec", prio="High",
         titulo="Iniciar a RTM",
         oque="Criar docs/rtm.md com uma linha por requisito: requisito → risco → teste (caminho e nome) → nível → evidência (run do CI) → situação. Preencher o que já existe e identificar os requisitos de aprovação e manutenção (pré-requisito da Aula 09). A partir daqui, todo PR de feature atualiza a RTM.",
         porque="RTM é entregável e a meta é 100% dos requisitos críticos rastreados. 'Não escreva testado sem apontar para uma execução.'",
         aceite=["14 RFs e todas as regras críticas na tabela", "Toda linha marcada como testada aponta um run"],
         arquivos="docs/rtm.md", dep="T-25", req="Todos"),
    # ───────────── Semana 41 · Aula 09 ─────────────
    dict(id="T-33", sem=41, area="Documentação", dono="C", fluxo="sem-spec", prio="High",
         titulo="Cenários BDD de aprovação e bloqueio",
         oque="Escrever docs/testes/bdd-aprovacao-bloqueio.md em Dado/Quando/Então para 4 regras: recurso restrito exige aprovação do Responsável; Solicitante não aprova a própria reserva; recurso em manutenção não pode ser reservado; transições de estado só pelos caminhos permitidos. Tabela de fronteiras da manutenção: reserva que termina exatamente no início, que começa exatamente no fim e que cruza um segundo. Fronteira indefinida vira pergunta. Indicar quais cenários viram teste de unidade e quais viram teste de API.",
         porque="Prompt 1 da Aula 09. BDD transforma regra em exemplo verificável antes do código. Cada cenário tem um único Quando e um Então observável.",
         aceite=["4 regras com cenários", "Tabela de fronteiras", "Cada cenário com um único Quando"],
         arquivos="docs/testes/bdd-aprovacao-bloqueio.md", dep="T-24, T-32", req="RF-07, RF-08"),
    dict(id="T-34", sem=41, area="Backend", dono="C", fluxo="critica", prio="High",
         titulo="Aprovação de recurso restrito com TDD estrito",
         oque="Um cenário por vez: 1 teste JUnit 5 → ./mvnw -B test falhando por asserção → commit test(red): <cenário> → mínimo para ficar verde → commit feat(green): <cenário> → refatoração → commit refactor: <motivo>. Regras: reserva de recurso restrito fica SOLICITADA até o Responsável aprovar ou rejeitar; só o Responsável aprova; o Solicitante não aprova a própria reserva.",
         porque="RF07 e o item 'TDD ou BDD com evidência' da especificação. O professor abre o histórico e procura o commit vermelho.",
         aceite=["Spec liberada", "Commits red, green e refactor por cenário no histórico", "403 para quem não é Responsável"],
         arquivos="src/main/java/.../reserva/aprovacao/", dep="T-15, T-33", req="RF-07"),
    dict(id="T-35", sem=41, area="Backend", dono="S", fluxo="critica", prio="High",
         titulo="Bloqueio de recursos em manutenção com TDD estrito",
         oque="Mesmo ciclo red → green → refactor da T-34, com commits separados. O Administrador registra períodos de manutenção; reservas que tocam o período são recusadas, seguindo a tabela de fronteiras do BDD. Clock injetado nas regras de manutenção.",
         porque="RF08. Na mesa, o professor pede para quebrar a regra de manutenção e mostrar qual teste falha.",
         aceite=["Spec liberada", "Commits red, green e refactor por cenário", "Fronteiras da tabela testadas"],
         arquivos="src/main/java/.../manutencao/", dep="T-33", req="RF-08"),
    dict(id="T-36", sem=41, area="Testes", dono="R", fluxo="sem-spec", prio="High",
         titulo="Feature Cucumber executável e evidência de TDD",
         oque="Adicionar cucumber-java e cucumber-junit-platform-engine; escrever src/test/resources/features/aprovacao-bloqueio.feature a partir do BDD; steps chamando a API via MockMvc com tokens de cada perfil; o verify roda as features no CI e o gate do JaCoCo continua passando. Montar docs/testes/evidencia-tdd.md (cenário → commit red → commit green → commit refactor → trecho da saída) com os commits das T-34 e T-35. Atualizar a RTM com requisito → cenário → feature → commit.",
         porque="Fecha o entregável da Aula 09: PR com commits red/green/refactor, feature executável, evidência de TDD e RTM atualizada.",
         aceite=["Features rodando no CI", "evidencia-tdd.md com links para os commits", "RTM atualizada"],
         arquivos="src/test/resources/features/, docs/testes/evidencia-tdd.md, docs/rtm.md", dep="T-34, T-35", req="RF-07, RF-08"),
    # ───────────── Semana 42 · Aula 10 ─────────────
    dict(id="T-37", sem=42, area="Backend", dono="S", fluxo="critica", prio="Highest",
         titulo="Proteção contra dupla reserva com teste de concorrência",
         oque="Teste de integração JUnit 5 + Testcontainers: 20 solicitações simultâneas (ExecutorService + CountDownLatch) para a mesma sala e horário; esperado exatamente 1 aceita e 19 recusadas com 409, e 1 única linha no banco, verificada por consulta. Repetir para a agenda do professor. Rodar 5 vezes seguidas. Se falhar, registrar o defeito em docs/defeitos/ e proteger no banco com EXCLUDE USING gist sobre tstzrange ou lock pessimista, com ADR curto.",
         porque="RF06. Possibilidade de dupla reserva é limitador de nota. Na mesa, o professor pede para remover a proteção e rodar o teste: ele precisa ficar vermelho.",
         aceite=["Spec liberada", "Vermelho sem a proteção e verde com ela", "Contagem vem do banco",
                 "ADR da proteção", "Estável em 5 execuções seguidas"],
         arquivos="src/test/java/.../concorrencia/, db/migration/, docs/adr/", dep="T-30", req="RF-06"),
    dict(id="T-38", sem=42, area="Testes", dono="R", fluxo="sem-spec", prio="High",
         titulo="Teste de carga com JMeter: RNF, plano, execução e resultado",
         oque="Escrever o RNF antes da ferramenta em docs/performance/rnf.md (ex.: p95 até 300 ms com 50 usuários e erro < 1% para consulta de disponibilidade e criação de reserva) e o modelo de carga. Plano perf/reservas.jmx com login e JSON Extractor do token, CSV Data Set (perf/usuarios.csv, usuários fictícios), consulta e criação disputando o mesmo recurso; asserção: criação responde 201 ou 409, nunca 500. Executar em CLI: jmeter -n -t perf/reservas.jmx -l perf/resultados/rodada-01.jtl -e -o perf/relatorio-01. Escrever docs/performance/resultado-01.md com ambiente, carga, p50/p95/p99, vazão e erro (separando 409 esperado de erro real), consulta ao banco confirmando no máximo 1 reserva por intervalo, decisão e hipótese de gargalo. Ligar RNF → plano → resultado na RTM.",
         porque="Plano e resultado de carga são entregáveis; performance, segurança e robustez valem 10%. O professor pergunta de onde saiu o p95 e confere se o 409 foi contado como erro.",
         aceite=["RNF escrito antes da execução", "Relatório HTML fora do Git (.gitignore)",
                 "409 separado de erro real", "Decisão: atende, não atende ou inconclusivo"],
         arquivos="docs/performance/, perf/reservas.jmx, perf/usuarios.csv", dep="T-15, T-37", req="RNF de desempenho"),
    # ───────────── Semana 43 · Aula 11 ─────────────
    dict(id="T-39", sem=43, area="Documentação", dono="R", fluxo="sem-spec", prio="High",
         titulo="Status dos requisitos",
         oque="Escrever docs/status-requisitos.md com os 14 RFs e as regras críticas: situação (feito, parcial, ausente) → evidência (arquivo e nome do teste) → risco se ficar de fora. Só marcar 'feito' quando houver teste que falharia se a regra quebrasse. Ordenar começando pelos limitadores de nota e propor a divisão das pendências até a semana 46.",
         porque="Prompt 1 da Aula 11. Mostra com honestidade o que falta. Na mesa, o professor escolhe um item marcado como feito e pede o teste.",
         aceite=["14 RFs e todas as regras críticas", "Todo 'feito' aponta um teste", "Pendências com responsável"],
         arquivos="docs/status-requisitos.md", dep="T-32", req="Todos"),
    dict(id="T-40", sem=43, area="Backend", dono="R", fluxo="critica", prio="High",
         titulo="Histórico auditável de mudanças de estado",
         oque="Por TDD (commits separados de teste e código): toda mudança de estado grava um registro com reserva, usuário, instante (Clock), estado anterior, estado novo e motivo; o registro é imutável pela API; reserva iniciada não pode ser apagada. Teste de integração com Testcontainers provando a gravação na mesma transação e que rollback não deixa auditoria órfã. Consulta do histórico só para Responsável e Administrador (403 para Solicitante).",
         porque="RF10 e regra crítica 'toda mudança de estado gera auditoria'. Na mesa, o professor pede o registro de auditoria de uma reserva cancelada.",
         aceite=["Spec liberada", "Teste de rollback sem registro órfão", "Nenhum endpoint altera ou apaga auditoria", "403 para Solicitante"],
         arquivos="src/main/java/.../auditoria/", dep="T-24, T-30", req="RF-10"),
    dict(id="T-41", sem=43, area="Backend", dono="C", fluxo="spec", prio="High",
         titulo="Relatório de utilização com oráculo calculado à mão",
         oque="GET /relatorios/utilizacao?inicio=&fim= com horas reservadas por recurso, carga horária por professor e conflitos evitados (tentativas recusadas por sobreposição). Testes de API caixa-preta com massa pequena semeada e valores esperados calculados à mão, com a conta num comentário do teste. Cobrir período vazio, reserva cruzando a borda do período e reservas canceladas (não contam). 401 sem token e 403 para Solicitante.",
         porque="RF12. 'Os valores esperados não foram copiados da saída da própria aplicação.' Na mesa, o professor refaz uma linha à mão.",
         aceite=["Spec validada", "Conta do oráculo em comentário", "Bordas cobertas", "401 e 403 testados"],
         arquivos="src/main/java/.../relatorio/", dep="T-24, T-37", req="RF-12"),
    dict(id="T-42", sem=43, area="Backend", dono="S", fluxo="spec", prio="Medium",
         titulo="Registro de retirada e devolução de materiais",
         oque="O Responsável registra a retirada e a devolução de um material ligado a uma reserva aprovada; devolução atrasada fica sinalizada; não é possível retirar material sem reserva aprovada.",
         porque="RF09. Nenhuma aula cobre este requisito, mas ele é obrigatório.",
         aceite=["Spec validada", "Testes de retirada sem reserva, devolução atrasada e permissão"],
         arquivos="src/main/java/.../material/", dep="T-24", req="RF-09"),
    dict(id="T-43", sem=43, area="Backend", dono="C", fluxo="spec", prio="Medium",
         titulo="Pesquisa de recursos por tipo, capacidade, localização, competência e disponibilidade",
         oque="Endpoint de busca combinando filtros, incluindo disponibilidade num intervalo de tempo (exclui recursos com reserva sobreposta ou em manutenção).",
         porque="RF03. Nenhuma aula cobre este requisito, mas ele é obrigatório e é a tela mais usada pelo Solicitante.",
         aceite=["Spec validada", "Testes com classes de equivalência dos filtros", "Disponibilidade respeita manutenção"],
         arquivos="src/main/java/.../pesquisa/", dep="T-23, T-35", req="RF-03"),
    dict(id="T-44", sem=43, area="Frontend", dono="S", fluxo="spec", prio="High",
         titulo="Layout base responsivo, login e navegação por perfil",
         oque="Layout Thymeleaf com Bootstrap (responsivo em celular e desktop), tela de login, menu que muda conforme o perfil (thymeleaf-extras-springsecurity6), exibição padronizada de mensagens de sucesso e de erro compreensíveis, página de erro amigável.",
         porque="RF13. É a base de todas as telas; fazer uma vez evita que cada tela tenha um visual diferente.",
         aceite=["Spec validada", "Telas utilizáveis em 375 px de largura", "Menu mostra só o que o perfil pode fazer"],
         arquivos="src/main/resources/templates/, src/main/java/.../web/", dep="T-15, T-22", req="RF-13"),
    # ───────────── Semana 44 · Aula 12 ─────────────
    dict(id="T-45", sem=44, area="Frontend", dono="R", fluxo="spec", prio="High",
         titulo="Telas do Solicitante",
         oque="Pesquisa e consulta de disponibilidade, criação, alteração e cancelamento de reserva, lista 'minhas reservas' com estado. Mensagens de erro compreensíveis (ex.: 'A sala 12 já está reservada das 14h às 16h').",
         porque="RF13 e RF04 pela interface. É o fluxo principal da demonstração na prova oral.",
         aceite=["Spec validada", "Fluxo completo de reserva pela tela", "Erros de regra exibidos em linguagem clara"],
         arquivos="src/main/resources/templates/solicitante/", dep="T-43, T-44", req="RF-03, RF-04, RF-13"),
    dict(id="T-46", sem=44, area="Frontend", dono="C", fluxo="spec", prio="High",
         titulo="Telas do Responsável e do Administrador",
         oque="Responsável: fila de aprovação (aprovar ou rejeitar), retirada e devolução, histórico de auditoria. Administrador: cadastros de salas, professores, materiais e usuários, períodos de manutenção e relatório de utilização.",
         porque="RF13 aplicado aos outros dois perfis.",
         aceite=["Spec validada", "Cada operação dos perfis acessível pela tela", "Ações proibidas não aparecem no menu e retornam 403 se forçadas"],
         arquivos="src/main/resources/templates/responsavel/, .../administrador/", dep="T-41, T-42, T-44", req="RF-02, RF-07, RF-08, RF-09, RF-10, RF-12, RF-13"),
    dict(id="T-47", sem=44, area="Testes", dono="S", fluxo="sem-spec", prio="High",
         titulo="Testes end-to-end dos fluxos principais",
         oque="Testes E2E com navegador real (Playwright for Java ou Selenium) sobre a aplicação subindo com Testcontainers: login, pesquisa, criação de reserva, aprovação pelo Responsável e cancelamento. Rodar no CI.",
         porque="A especificação exige testes end-to-end e nenhuma aula mostra como; fica a critério do time.",
         aceite=["Pelo menos 3 fluxos E2E", "Rodando no CI sem intervenção manual"],
         arquivos="src/test/java/.../e2e/", dep="T-45, T-46", req="RF-13"),
    dict(id="T-48", sem=44, area="Documentação", dono="S", fluxo="sem-spec", prio="Highest",
         titulo="Matriz de autorização",
         oque="Escrever docs/seguranca/matriz-autorizacao.md: uma linha por operação da API e colunas anônimo, Solicitante, Responsável e Administrador, com o status esperado (200, 201, 204, 401, 403). Regras de posse: o Solicitante só altera e cancela as próprias reservas; só o Responsável aprova recurso restrito. Marcar divergências com o código sem corrigir e listar rotas fora de qualquer regra de segurança.",
         porque="Prompt 1 da Aula 12. Falha de autorização é limitador de nota. Toda rota precisa estar na matriz.",
         aceite=["Toda rota da API está na matriz", "Regras de posse explícitas", "Divergências listadas"],
         arquivos="docs/seguranca/matriz-autorizacao.md", dep="T-41, T-42, T-43", req="RF-01"),
    dict(id="T-49", sem=44, area="Testes", dono="S", fluxo="critica", prio="Highest",
         titulo="Matriz de autorização como teste parametrizado",
         oque="@ParameterizedTest + @MethodSource percorrendo cada célula da matriz (operação, perfil, status) via MockMvc com tokens reais. Casos de posse (Solicitante altera ou cancela a reserva de outro pelo id). Validação de entrada (término antes do início, capacidade negativa, campos ausentes) → 400 com mensagem compreensível. Nenhuma resposta de erro com stack trace, SQL ou nome de classe. Cada célula vermelha vira defeito em docs/defeitos/ antes da correção, que vai em commit próprio referenciando o defeito.",
         porque="Prompt 2 da Aula 12. Na mesa, o professor faz uma requisição de Solicitante na reserva de outro. Nenhuma célula pode ser apagada para passar.",
         aceite=["Spec liberada", "Todas as células da matriz testadas", "Defeitos registrados antes das correções"],
         arquivos="src/test/java/.../seguranca/, docs/defeitos/", dep="T-48", req="RF-01, RF-13"),
    dict(id="T-50", sem=44, area="Testes", dono="R", fluxo="sem-spec", prio="Highest",
         titulo="Mutação manual nas regras críticas e fechamento da RTM",
         oque="Para cada regra crítica (sobreposição, dupla reserva, aprovação de restrito, manutenção, autorização, auditoria): quebrar a regra no código, rodar o teste ligado, registrar e desfazer com git restore. Registrar em docs/testes/mutacao-manual.md a regra, a quebra, o teste e o resultado. Teste que continuou verde é lacuna: escrever o teste que falta. Percorrer docs/rtm.md e garantir que cada requisito crítico tem risco, teste e evidência do CI no commit atual. Informar a % de requisitos críticos com evidência.",
         porque="Prompt 3 da Aula 12. Meta da especificação: 100% dos requisitos críticos na RTM. Na mesa, o professor pede para repetir uma mutação.",
         aceite=["Uma mutação por regra crítica", "Nenhuma mutação sobreviveu sem ação", "RTM com 100% dos críticos com evidência"],
         arquivos="docs/testes/mutacao-manual.md, docs/rtm.md", dep="T-37, T-40, T-49", req="Regras críticas"),
    dict(id="T-51", sem=44, area="Documentação", dono="C", fluxo="sem-spec", prio="Medium",
         titulo="Documentação da API com OpenAPI",
         oque="Adicionar springdoc-openapi, descrever todos os endpoints (parâmetros, respostas, erros e perfis permitidos) e linkar o Swagger UI no README, junto com os fluxos públicos.",
         porque="RF14 e entregável 'README e documentação da API'. O relatório de utilização (Aula 11) também pede documentação no README ou OpenAPI.",
         aceite=["Swagger UI acessível com a aplicação rodando", "Todos os endpoints descritos", "Link no README"],
         arquivos="pom.xml, README.md", dep="T-41", req="RF-14"),
    # ───────────── Semana 45 · Aula 13 ─────────────
    dict(id="T-52", sem=45, area="Documentação", dono="R", fluxo="sem-spec", prio="High",
         titulo="Relatório de qualidade",
         oque="Escrever docs/relatorio-qualidade.md: identificação (repositório, commit, data, integrantes, ambiente), escopo e fora de escopo, estratégia por nível, resultados comparados às metas (80% linhas, 70% branches, 100% críticos na RTM, 0 bugs ou vulnerabilidades críticas), defeitos por severidade e situação, riscos residuais e recomendação. Para cada afirmação: evidência, interpretação e limite; o que não foi medido fica 'não medido' com o motivo.",
         porque="Entregável 'relatório final de qualidade'. Na mesa, o professor escolhe um número do relatório e pede a fonte.",
         aceite=["Toda métrica aponta a fonte", "Comparação explícita com as 4 metas", "Riscos residuais listados"],
         arquivos="docs/relatorio-qualidade.md", dep="T-50", req="—"),
    dict(id="T-53", sem=45, area="Documentação", dono="C", fluxo="sem-spec", prio="High",
         titulo="Banco de perguntas da prova oral",
         oque="Escrever docs/prova-oral/perguntas.md com 15 perguntas específicas do projeto, cada uma com o arquivo onde está a resposta e a descrição de uma resposta fraca. Distribuir para que cada integrante responda sobre partes que NÃO escreveu. Sem as respostas.",
         porque="Prompt 2 da Aula 13. Na prova oral qualquer integrante pode ser perguntado sobre qualquer parte.",
         aceite=["15 perguntas com arquivo de referência", "Distribuição cruzada entre os 3"],
         arquivos="docs/prova-oral/perguntas.md", dep="T-52", req="—"),
    dict(id="T-54", sem=45, area="Documentação", dono="T", fluxo="sem-spec", prio="High",
         titulo="Ensaio da prova oral e apresentação",
         oque="Ensaio com o agente: uma pergunta por vez, notas de 0 a 2 em correção técnica, evidência citada e domínio de autoria, trocando de integrante a cada 5 perguntas. Gerar docs/prova-oral/ensaio.md com a pontuação por pessoa, as 3 lacunas mais frequentes (cada uma vira task com responsável) e um plano de estudo até a semana 47. Montar a apresentação e simular o roteiro de 8 minutos: problema e risco, arquitetura, demonstração, testes e CI, RTM e relatório, limitações.",
         porque="Prompt 3 da Aula 13 e entregável 'apresentação e demonstração'. O professor confere se as lacunas do ensaio têm responsável.",
         aceite=["ensaio.md com pontuação por pessoa", "Lacunas viraram tasks com responsável", "Roteiro de 8 minutos cronometrado"],
         arquivos="docs/prova-oral/ensaio.md, docs/prova-oral/apresentacao/", dep="T-53", req="—"),
    # ───────────── Semana 46 · Aula 14 ─────────────
    dict(id="T-55", sem=46, area="Infra", dono="C", fluxo="sem-spec", prio="Highest",
         titulo="Clone limpo e checklist de entrega",
         oque="Clonar o repositório num diretório temporário e seguir APENAS o README até ./mvnw -B verify passar e a aplicação subir. Conferir os 12 entregáveis da especificação e escrever docs/entrega/checklist.md com item → existe → atualizado → caminho, e os bloqueadores em ordem. Não corrigir nesta etapa: cada bloqueador vira task.",
         porque="Prompt 1 da Aula 14. O professor clona o repositório e segue o README; aplicação que não executa é limitador de nota.",
         aceite=["Clone limpo executado só com o README", "12 entregáveis conferidos", "Bloqueadores com responsável"],
         arquivos="docs/entrega/checklist.md", dep="T-52", req="—"),
    dict(id="T-56", sem=46, area="Infra", dono="S", fluxo="sem-spec", prio="Highest",
         titulo="Release candidate v1.0.0-rc1 e pacote de evidências",
         oque="Com o CI verde na main, criar a tag anotada v1.0.0-rc1 e enviá-la. Escrever CHANGELOG.md. Montar docs/evidencias/README.md como índice: run do CI no commit da tag, artifact do JaCoCo, Quality Gate do SonarCloud, docs/performance/resultado-01.md, docs/rtm.md, docs/defeitos/ e docs/relatorio-qualidade.md. Toda evidência aponta para o commit da tag; correção depois vira v1.0.0-rc2 (nunca mover tag).",
         porque="Prompt 2 da Aula 14. O professor abre o run do CI ligado à tag.",
         aceite=["Tag anotada com CI verde", "CHANGELOG.md", "Índice de evidências apontando para o commit da tag"],
         arquivos="CHANGELOG.md, docs/evidencias/README.md", dep="T-55", req="—"),
    dict(id="T-57", sem=46, area="Documentação", dono="R", fluxo="sem-spec", prio="Highest",
         titulo="Decisão de qualidade e envio do formulário",
         oque="Escrever docs/decisao-qualidade.md: decisão (liberar, liberar com ressalvas ou bloquear), critérios com a evidência de cada um, riscos aceitos com impacto, justificativa e responsável, e o nome de cada integrante declarando o que fez. Não escrever 'pronto para produção' a partir de testes locais. Enviar o formulário de entrega do Hub (turma, nomes completos dos 3, link do repositório público) e guardar o comprovante.",
         porque="Prompt 3 da Aula 14 e a entrega oficial. Sem o formulário, não há entrega.",
         aceite=["decisao-qualidade.md com a declaração dos 3", "Formulário enviado e comprovante guardado"],
         arquivos="docs/decisao-qualidade.md", dep="T-56", req="—"),
    # ───────────── Semana 47 · Prova oral ─────────────
    dict(id="T-58", sem=47, area="Documentação", dono="T", fluxo="sem-spec", prio="High",
         titulo="Prova oral",
         oque="Apresentar o roteiro de 8 minutos com demonstração ao vivo e responder às perguntas do professor. Cada integrante defende as próprias decisões e responde sobre as partes dos colegas.",
         porque="Arguição do trabalho final: 'você defende as decisões de qualidade que tomou'.",
         aceite=["Os 3 presentes", "Demonstração funcionando a partir da tag"],
         arquivos="—", dep="T-54, T-57", req="—"),
]


def sprint(t):
    return SPRINT_DA_SEMANA[t["sem"]]


def fluxo_txt(t):
    if t["fluxo"] == "mesma-spec":
        return f"Usa a spec da {t['spec_de']}"
    return FLUXO_TXT[t["fluxo"]]


def md():
    linhas = []
    a = linhas.append
    a("# Backlog do projeto")
    a("")
    a("> Gerado por `scripts/gerar-backlog.py`, a mesma fonte do `backlog-jira.csv`. Para mudar uma task, edite o script e rode-o de novo.")
    a("")
    a("As tasks seguem as **semanas do cronograma da disciplina** (32 a 47) e cobrem tudo o que o professor pede em cada aula, aplicado ao projeto, mais os requisitos que nenhuma aula cobre (interface, retirada e devolução, pesquisa e OpenAPI). A semana 40 é a prova escrita, que não tem entrega no projeto.")
    a("")
    a("Cada task tem um **ID estável** (`T-NN`), que vai no título do Jira. A chave do Jira (`KAN-NN`) é gerada na importação, e é ela que aparece no nome da branch.")
    a("")
    a("**Áreas:** Backend, Frontend, Testes, Infra e Documentação.")
    a("")
    a("**Fluxo** (ver [ai-workflow](../processo/ai-workflow.md)): **—** vai direto para o PR · **spec** passa por spec, revisão e validação · **spec + grill** é regra crítica, com fluxo completo · **spec de outra task** é a fase green de uma spec já liberada.")
    a("")
    a("## Distribuição")
    a("")
    a("| Pessoa | Tasks | Backend | Frontend | Testes | Infra | Documentação | Regras críticas |")
    a("|---|---|---|---|---|---|---|---|")
    for k in ("R", "S", "C", "T"):
        nome, gh = PESSOAS[k]
        ts = [t for t in TASKS if t["dono"] == k]
        cont = {ar: sum(1 for t in ts if t["area"] == ar) for ar in AREA_LABEL}
        crit = ", ".join(t["id"] for t in ts if t["fluxo"] == "critica") or "—"
        a(f"| {nome} ({gh}) | {len(ts)} | {cont['Backend']} | {cont['Frontend']} | {cont['Testes']} | {cont['Infra']} | {cont['Documentação']} | {crit} |")
    a("")
    for s, nome_s in SPRINTS.items():
        a(f"## {nome_s}")
        a("")
        for sem in sorted({t["sem"] for t in TASKS if sprint(t) == s}):
            a(f"### Semana {sem} · {AULAS[sem]}")
            a("")
            a("| ID | Área | Task | Dono | Fluxo | Prioridade | Depende de |")
            a("|---|---|---|---|---|---|---|")
            for t in TASKS:
                if t["sem"] == sem:
                    fl = f"spec da {t['spec_de']}" if t["fluxo"] == "mesma-spec" else FLUXO_CURTO[t["fluxo"]]
                    a(f"| [{t['id']}](#{t['id'].lower()}) | {t['area']} | {t['titulo']} | {PESSOAS[t['dono']][0]} | {fl} | {t['prio']} | {t['dep']} |")
            a("")
    a("## Detalhe das tasks")
    a("")
    for t in TASKS:
        a(f"### {t['id']}")
        a("")
        a(f"**{t['titulo']}**")
        a("")
        a(f"- **Semana:** {t['sem']} ({AULAS[t['sem']]}) · **Sprint:** {sprint(t)} · **Área:** {t['area']} · **Dono:** {PESSOAS[t['dono']][0]}")
        a(f"- **Fluxo:** {fluxo_txt(t)} · **Prioridade:** {t['prio']} · **Requisitos:** {t['req']} · **Depende de:** {t['dep']}")
        a(f"- **O que fazer:** {t['oque']}")
        a(f"- **Por quê:** {t['porque']}")
        a("- **Critérios de aceite:**")
        for c in t["aceite"]:
            a(f"  - {c}")
        a(f"- **Arquivos:** {t['arquivos']}")
        a("")
    return "\n".join(linhas)


def descricao_jira(t):
    """Descrição em wiki markup do Jira (h3., listas com *)."""
    partes = [
        f"*Área:* {t['area']}  |  *Semana:* {t['sem']} ({AULAS[t['sem']]})  |  *Sprint:* {sprint(t)}",
        f"*Dono:* {PESSOAS[t['dono']][0]}  |  *Fluxo:* {fluxo_txt(t)}  |  *Depende de:* {t['dep']}",
        "",
        "h3. O que fazer",
        t["oque"],
        "",
        "h3. Por que (explicação)",
        t["porque"],
        "",
        "h3. Critérios de aceite",
        *[f"* {c}" for c in t["aceite"]],
        "",
        "h3. Rastreabilidade",
        f"* Requisitos: {t['req']}",
        f"* Arquivos: {t['arquivos']}",
        f"* Processo: docs/processo/ai-workflow.md",
    ]
    return "\n".join(partes)


def csv_jira(caminho):
    fluxo_label = {"sem-spec": "sem-spec", "spec": "spec", "critica": "regra-critica", "mesma-spec": "spec"}
    with open(caminho, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["Summary", "Issue Type", "Priority", "Assignee", "Description",
                    "Labels", "Labels", "Labels", "Labels", "Labels"])
        for t in TASKS:
            w.writerow([
                f"[{t['id']}] S{t['sem']} · {t['area']} · {t['titulo']}",
                "Task", t["prio"], "" if t["dono"] == "T" else PESSOAS[t["dono"]][0],
                descricao_jira(t),
                f"semana-{t['sem']}", f"sprint-{sprint(t)}", AREA_LABEL[t["area"]],
                fluxo_label[t["fluxo"]], "qualidade-senac",
            ])


(REPO / "docs/planejamento/backlog.md").write_text(md(), encoding="utf-8")
csv_jira(REPO / "docs/planejamento/backlog-jira.csv")
print(f"{len(TASKS)} tasks geradas")
for k in ("R", "S", "C", "T"):
    ts = [t for t in TASKS if t["dono"] == k]
    print(f"{PESSOAS[k][0]:7} {len(ts):2}  " + " ".join(t["id"] for t in ts))
