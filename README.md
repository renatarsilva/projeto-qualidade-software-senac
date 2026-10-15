# Organização de Recursos — Projeto de Qualidade de Software

Projeto semestral da disciplina **Qualidade de Software** (TADS · Senac · 2026.2), com o Prof. Afonso Lelis.

O sistema organiza a reserva de **salas, professores e materiais** de uma instituição de ensino, sem conflitos de horário. O foco da avaliação não é só fazer o sistema funcionar: é apresentar **evidências objetivas** de qualidade, segurança, rastreabilidade e desempenho.

> **Status:** em construção. O projeto evolui semana a semana, seguindo o cronograma da disciplina (ver [Roadmap](#roadmap)).

## O problema

Em uma instituição de ensino, a mesma sala, o mesmo projetor ou o mesmo professor podem ser pedidos para o mesmo horário por pessoas diferentes. Sem controle, isso gera reservas duplicadas, aulas sem sala e equipamentos que somem. O sistema garante que cada recurso tenha uma única reserva válida por horário, com aprovação, histórico e relatórios.

## Perfis de acesso

| Perfil | O que faz |
|---|---|
| **Solicitante** | Professor ou coordenador. Consulta a disponibilidade e cria, altera ou cancela as próprias reservas. |
| **Responsável** | Aprova solicitações de recursos restritos, valida a alocação de docentes e acompanha retiradas e devoluções de material. |
| **Administrador** | Gerencia salas, professores, materiais, usuários, bloqueios e períodos de manutenção. |

## Funcionalidades previstas

- Autenticação e autorização por perfil.
- Cadastro e consulta de salas, professores e materiais.
- Pesquisa por tipo, capacidade, localização, competência e disponibilidade.
- Criação, alteração e cancelamento de reservas.
- Detecção de sobreposição de horários, inclusive na agenda do professor.
- Proteção contra dupla reserva em solicitações simultâneas.
- Aprovação obrigatória para recursos restritos.
- Bloqueio de recursos em manutenção.
- Registro de retirada e devolução de materiais e equipamentos.
- Histórico auditável de mudanças.
- Notificação simulada ou integração com API externa.
- Relatório de utilização por recurso, carga horária alocada e conflitos evitados.
- Interface responsiva e mensagens de erro compreensíveis.
- Documentação da API.

## Regras críticas

**Tempo e concorrência**

- O término de uma reserva deve ser posterior ao início.
- Reservas do mesmo recurso (sala, material ou professor) não podem se sobrepor.
- Duas solicitações simultâneas para o mesmo recurso e horário produzem uma única reserva aceita.
- Recursos em manutenção não podem ser reservados.

**Estados e auditoria**

```
SOLICITADA → APROVADA → EM_USO → CONCLUÍDA
```

- Estados alternativos: `REJEITADA`, `CANCELADA` e `NAO_COMPARECEU`.
- Somente o Responsável aprova recursos restritos.
- Reservas já iniciadas não podem ser apagadas.
- Toda mudança de estado gera um registro de auditoria.

## Estratégia de qualidade

| Frente | Como será verificada |
|---|---|
| Testes de unidade | JUnit 5, incluindo testes parametrizados (classes de equivalência e valor limite) |
| Testes de integração | Banco PostgreSQL real em container, com Testcontainers |
| Integrações externas | WireMock simulando o serviço de notificação |
| API | Testes em caixa-preta dos endpoints |
| Concorrência | Teste automatizado provando que não há dupla reserva |
| TDD / BDD | Ao menos uma funcionalidade desenvolvida com evidência da técnica |
| Integração contínua | GitHub Actions a cada pull request |
| Cobertura e análise estática | JaCoCo e SonarCloud com Quality Gate |
| Desempenho | Teste de carga com JMeter |
| Segurança | Testes de autorização por perfil, validação de entrada e tratamento seguro de erros |

### Metas

| Métrica | Meta |
|---|---|
| Cobertura de linhas | ≥ 80% |
| Cobertura de branches | ≥ 70% |
| Requisitos críticos na RTM | 100% |
| Bugs ou vulnerabilidades críticas | 0 |

Cobertura alta, sozinha, não garante qualidade. Cada teste precisa ter asserções que falhariam se a regra quebrasse.

## Tecnologias

- Java 21
- Spring Boot 3.x (Web, Security, Data JPA, Validation)
- PostgreSQL
- Maven (via `./mvnw`)
- JUnit 5, Testcontainers, WireMock, JaCoCo
- GitHub Actions, SonarCloud
- JMeter
- [AIOX](https://www.npmjs.com/package/aiox-core) para apoio ao desenvolvimento com agentes de IA

## Como executar

> Instruções serão adicionadas quando a aplicação for criada (semana 34 do cronograma).

## Documentação

| Documento | Conteúdo | Situação |
|---|---|---|
| `docs/prd.md` | Visão do produto, personas e escopos | A criar |
| `docs/arquitetura.md` | Diagramas C4, decisões (ADRs) e análise ATAM | A criar |
| `docs/plano-de-teste.md` | Estratégia, níveis de teste e critérios de entrada e saída | A criar |
| `docs/rtm.md` | Matriz de rastreabilidade: requisito → risco → teste → evidência | A criar |
| `docs/defeitos/` | Registro de defeitos encontrados, priorizados e corrigidos | A criar |
| `docs/relatorio-qualidade.md` | Relatório final de qualidade | A criar |

## Roadmap

O projeto segue o cronograma da disciplina. Cada técnica vista em aula é aplicada ao projeto na mesma semana.

| Semana | Tema da aula | Entrega no projeto | Status |
|---|---|---|---|
| 32 | Introdução à Qualidade de Software | Diagnóstico de qualidade de um projeto existente | ⬜ |
| 33 | ATAM e planejamento de casos de teste | Utility tree, cenários e casos de teste | ⬜ |
| 34 | Caixa branca vs caixa preta | Repositório, ambiente, escopo e perfis definidos | ⬜ |
| 35 | Arquitetura e análise ATAM | C4, utility tree, riscos, trade-offs e ADRs | ⬜ |
| 36 | Gestão de erros e bugs | Plano de TDD e testes na fase red | ⬜ |
| 37 | Testes automatizados em CI | Fase green e pipeline no GitHub Actions | ⬜ |
| 38 | JUnit 5, JaCoCo e SonarCloud | Cobertura de ramo nas regras de sobreposição e Quality Gate | ⬜ |
| 39 | WireMock e Testcontainers | Banco em container e integração externa isolada | ⬜ |
| 41 | TDD e BDD | Regras de aprovação e bloqueio guiadas por teste | ⬜ |
| 42 | Testes de carga com JMeter | Reservas simultâneas do mesmo recurso sob carga | ⬜ |
| 43 | Auditoria e relatórios | Histórico auditável e relatório de utilização | ⬜ |
| 44 | Autorização e RTM | Matriz de autorização testada e RTM fechada | ⬜ |
| 45 | Preparação para a prova oral | Relatório de qualidade | ⬜ |
| 46 | Entrega | Release candidate e pacote de evidências | ⬜ |

## Referências

- [Hub de aulas da disciplina](https://afonsolelis.github.io/aulas_senac/pages/home_qualidade_2026_2.html)
- [Especificação do projeto](https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html)
- [CineSenac](https://github.com/afonsolelis/cinesenac): referência de organização do PRD

## Autoria

Projeto individual desenvolvido por [@renatarsilva](https://github.com/renatarsilva).
