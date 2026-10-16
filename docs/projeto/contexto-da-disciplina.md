# Contexto da disciplina

Resumo da especificação oficial do projeto e de como ele é avaliado. **A fonte da verdade é sempre o site do professor**: em caso de divergência, vale o site, e este arquivo deve ser corrigido.

## Links oficiais

| O quê | Link |
|---|---|
| Hub de aulas | https://afonsolelis.github.io/aulas_senac/pages/home_qualidade_2026_2.html |
| Especificação do projeto | https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html |
| Orientações de entrega | https://afonsolelis.github.io/aulas_senac/pages/qualidade2/material/material_entrega-projeto-final.html |
| Escopo da prova escrita | https://afonsolelis.github.io/aulas_senac/pages/qualidade2/escopo-prova.html |
| Materiais das aulas | `https://afonsolelis.github.io/aulas_senac/pages/qualidade2/material/material_<tema>.html` |

Nas aulas, o professor demonstra as técnicas no case **Foot Fanatics**. O projeto avaliado é este, o de **Organização de Recursos**: cada técnica vista no Foot Fanatics deve ser aplicada aqui.

## Objetivo

Aplicação que organiza a alocação de **salas, professores e materiais** sem conflitos de horário. A avaliação foca em **evidências objetivas** de qualidade, segurança, rastreabilidade e desempenho.

**Stack exigida:** Java 21 e Spring Boot 3.x. Equipes de 1 a 4 pessoas.

## Perfis

| Perfil | Responsabilidades |
|---|---|
| **Solicitante** | Professor ou coordenador. Consulta a disponibilidade e cria, altera ou cancela as próprias reservas. |
| **Responsável** | Aprova solicitações especiais, valida a alocação de docentes e acompanha retiradas e devoluções de material. |
| **Administrador** | Gerencia salas, professores, materiais, usuários, bloqueios e manutenção. |

## Requisitos funcionais obrigatórios

| ID | Requisito | Task(s) |
|---|---|---|
| RF-01 | Autenticação e autorização por perfil | T-15, T-48, T-49 |
| RF-02 | Cadastro e consulta de salas, professores e materiais | T-16, T-17, T-18 |
| RF-03 | Pesquisa por tipo, capacidade, localização, competência e disponibilidade | T-43, T-45 |
| RF-04 | Criação, alteração e cancelamento de reservas | T-20, T-21, T-23, T-24 |
| RF-05 | Detecção de sobreposição de horários, inclusive na agenda do professor | T-20, T-23, T-28 |
| RF-06 | Proteção contra dupla reserva em solicitações simultâneas | T-37, T-38 |
| RF-07 | Aprovação obrigatória para recursos restritos | T-33, T-34, T-36 |
| RF-08 | Bloqueio de recursos em manutenção | T-33, T-35, T-36 |
| RF-09 | Registro de retirada e devolução de materiais | T-42 |
| RF-10 | Histórico auditável de mudanças | T-40 |
| RF-11 | Notificação simulada ou integração com API externa | T-31 |
| RF-12 | Relatório de utilização por recurso, carga horária alocada e conflitos evitados | T-41 |
| RF-13 | Interface responsiva e mensagens de erro compreensíveis | T-22, T-44, T-45, T-46, T-47 |
| RF-14 | Documentação da API ou dos fluxos públicos | T-51 |

> A numeração RF-01 a RF-14 segue a ordem da especificação e o formato usado nas aulas. O PRD (task T-06) detalha cada requisito e numera as regras de negócio (RN).

## Regras críticas

- O término deve ser posterior ao início.
- Reservas do mesmo recurso (sala, material ou professor) não podem se sobrepor.
- Duas solicitações simultâneas devem produzir uma única reserva aceita.
- Recursos em manutenção não podem ser reservados.
- Estados: `SOLICITADA → APROVADA → EM_USO → CONCLUIDA`. Estados alternativos: `REJEITADA`, `CANCELADA` e `NAO_COMPARECEU`.
- Somente responsáveis aprovam recursos restritos.
- Reservas iniciadas não podem ser apagadas.
- Toda mudança de estado gera auditoria.

## Estratégia de qualidade obrigatória

- JUnit 5: testes unitários e parametrizados.
- Testes de integração, de API em caixa-preta e end-to-end.
- **Testcontainers** para o banco e **WireMock** para integrações externas. Mocks só em testes unitários isolados.
- Teste automatizado de **concorrência** contra dupla reserva.
- Pelo menos uma funcionalidade desenvolvida com **TDD ou BDD**, com evidência.
- **GitHub Actions** em pull requests, **JaCoCo** e **SonarCloud**.
- **JMeter**, testes de autorização, validação de entrada e tratamento seguro de erros.

## Metas

| Métrica | Meta |
|---|---|
| Cobertura de linhas | ≥ 80% |
| Cobertura de branches | ≥ 70% |
| Requisitos críticos na RTM | 100% |
| Bugs ou vulnerabilidades críticas | 0 |

Testes triviais feitos só para subir o percentual não garantem nota máxima.

## Entregáveis

Repositório com histórico e revisão por pares; aplicação executável com instruções; README e documentação da API; `RTM.md` (requisito → risco → teste → evidência); diagramas de contexto, componentes e sequências críticas; plano, suíte e relatório de testes; pipeline de CI; relatórios do SonarCloud e do JaCoCo; plano e resultado de carga; registro de defeitos; relatório final de qualidade; apresentação e demonstração.

## Avaliação

| Dimensão | Peso |
|---|---|
| Requisitos e regras de negócio | 15% |
| Estratégia e documentação de testes | 15% |
| Qualidade dos testes automatizados | 25% |
| CI, cobertura e análise estática | 15% |
| Performance, segurança e robustez | 10% |
| Rastreabilidade e gestão de defeitos | 10% |
| Organização, apresentação e autoria | 10% |

### Limitadores de nota

Qualquer um destes limita a nota, não importa o resto:

1. a aplicação não executa;
2. o pipeline de CI está ausente;
3. existe falha de autorização;
4. existe possibilidade de dupla reserva.

As tasks que protegem contra esses limitadores têm prioridade **Highest** no backlog.

## Entrega

- **Semana 46:** tag `v1.0.0-rc1` com CI verde, `docs/entrega/checklist.md`, `docs/evidencias/README.md`, `docs/decisao-qualidade.md` e envio no formulário do Hub (repositório público, até 4 nomes).
- **Semana 47:** prova oral. Cada pessoa explica o próprio código e os próprios testes.

> O site não traz datas exatas nem horário-limite, só o número das semanas ("meio de novembro" para a prova oral). O planejamento assume entrega na semana de 09/11/2026: confirmem em sala.

### Formulário de entrega

Página: https://afonsolelis.github.io/aulas_senac/pages/qualidade2/entrega/entrega.html

- Um integrante envia pelo grupo inteiro.
- Campos: turma (STADSCAS4NA · segunda, STADSCAS4NB · quinta ou STADSCAS4ND · sexta), **nome completo** de cada integrante e o link do repositório (`https://github.com/usuario/repositorio`).
- O repositório precisa ser **público**: o formulário confere isso antes de enviar.
- Para corrigir, reenvie com o mesmo link; vale o envio mais recente. Guardem o comprovante.

### Prova oral (semana 47)

Formato indicado na Aula 13: **roteiro de 8 minutos** (problema e risco, arquitetura, demonstração, testes e CI, RTM e relatório, limitações) e perguntas a qualquer integrante, inclusive sobre partes que ele não escreveu.

## Cronograma das aulas e o que cada uma pede no projeto

Nas aulas 01 a 08 o professor demonstra no case Foot Fanatics, e o time aplica a mesma técnica no projeto na mesma semana. A partir da Aula 09 os materiais já são escritos para o projeto de Organização de Recursos. O detalhe de cada entrega está nas tasks do [backlog](../planejamento/backlog.md).

| Semana | Aula | Entrega no projeto | O professor confere |
|---|---|---|---|
| 32 | 01 · Introdução | Diagnóstico com ≥ 5 achados classificados pela ISO 25010 | Precisão, vocabulário, justificativa e priorização |
| 33 | 02 · ATAM e casos de teste | 3 personas; utility tree (≥ 3 atributos, 6 folhas); 3 cenários (A, A) com medida numérica; ≥ 3 casos de teste | Cenário sem número na medida não pontua |
| 34 | 03 · Caixa branca × preta | Projeto Spring Boot 3 + AIOX, `./mvnw test` verde, `docs/prd.md`, `docs/arquitetura.md`, `docs/plano-de-teste.md`, RF e RNF numerados e medíveis | Checklist "precisa existir" |
| 35 | 04 · Arquitetura e ATAM | `docs/arquitetura.md` com C4, sequências, ATAM, ADRs e matriz requisito → decisão → componente → evidência | Outra equipe entende e contesta sem explicação oral |
| 36 | 05 · Erros e bugs | Registro de defeitos; `docs/testes/plano-tdd.md`; testes na fase red | Cada teste falha por asserção |
| 37 | 06 · CI | Fase green; `.github/workflows/ci.yml`; run vermelho intencional registrado; `docs/avaliacao-arquitetural/ciclo-01.md` | Merge bloqueado com check falhando |
| 38 | 07 · JUnit, JaCoCo e SonarCloud | Gate 80/70 no `verify`; cobertura de ramo na sobreposição; Quality Gate no PR | Build falha abaixo da meta |
| 39 | 08 · WireMock | Integração externa isolada (INT-01 a INT-05); banco em container; RTM | Nenhum segredo vaza |
| 40 | Prova escrita | — (Aulas 01 a 08) | — |
| 41 | 09 · TDD e BDD | `docs/testes/bdd-aprovacao-bloqueio.md`, commits `test(red)`/`feat(green)`/`refactor`, feature Cucumber, `docs/testes/evidencia-tdd.md` | Procura o commit vermelho; quebra a regra de manutenção |
| 42 | 10 · Carga e concorrência | Teste de 20 solicitações simultâneas; ADR da proteção; `perf/reservas.jmx`; `docs/performance/rnf.md` e `resultado-01.md` | Remove a proteção e roda o teste; pergunta a origem do p95 |
| 43 | 11 · Auditoria e relatórios | `docs/status-requisitos.md`; auditoria com rollback testado; `GET /relatorios/utilizacao` com oráculo à mão | Pede um teste de item "feito"; refaz uma linha do relatório |
| 44 | 12 · Autorização e RTM | `docs/seguranca/matriz-autorizacao.md`; teste parametrizado; `docs/testes/mutacao-manual.md`; RTM 100% | Solicitante na reserva de outro; repete uma mutação |
| 45 | 13 · Prova oral | `docs/relatorio-qualidade.md`; `docs/prova-oral/perguntas.md` e `ensaio.md` | Pede a fonte de um número; pergunta a um integrante sorteado |
| 46 | 14 · Entrega | `docs/entrega/checklist.md`; tag `v1.0.0-rc1`; `CHANGELOG.md`; `docs/evidencias/README.md`; `docs/decisao-qualidade.md`; formulário | Clona e segue o README; abre o run da tag |
| 47 | Prova oral | Roteiro de 8 minutos e perguntas | Defesa das decisões de qualidade |
