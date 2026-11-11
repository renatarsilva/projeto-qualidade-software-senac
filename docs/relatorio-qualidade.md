# Relatório de qualidade

Resume o que o time fez para garantir a qualidade do sistema de Organização de Recursos, com a **fonte de cada número**. Na prova oral o professor pede a origem de qualquer número citado, então nenhum valor entra aqui sem arquivo ou run que o comprove. Task T-52.

> **Estado em 2026-11-11 (entrega):** números medidos na `main` pelo `./mvnw verify` (1023 testes) e pelo painel do SonarCloud.

## 1. Resumo

| Item | Conteúdo |
|---|---|
| O sistema | Aplicação Java 21 e Spring Boot 3.5 que reserva salas, professores e materiais sem conflito de horário |
| O que foi entregue | Os 14 requisitos, com as **telas responsivas dos três perfis** (Thymeleaf e Bootstrap, login por sessão com CSRF) e a API: cadastros, pesquisa, reservas com todo o ciclo de vida, dupla reserva impedida no banco, manutenção, retirada e devolução, auditoria imutável, notificação, relatório de utilização e documentação OpenAPI. Detalhe em [status dos requisitos](status-requisitos.md) |
| O que ficou fora | Job periódico das mudanças por tempo (T-60), feature Cucumber (BDD), teste de navegador no CI (o roteiro do Chrome roda à mão), edição de cadastros pelas telas. Ver [decisão de qualidade](decisao-qualidade.md), seção 2 |
| Equipe | Renata (@renatarsilva), Samuel (@Samwel-Merces), Caio (@CaiosAguiar) |

## 2. Problema e riscos

| Item | Onde está |
|---|---|
| Diagnóstico de qualidade (achados pela ISO 25010) | `docs/diagnostico/` |
| Utility tree e cenários | [Utility tree](atam/utility-tree-inicial.md) |
| Riscos da arquitetura (R-01 a R-08) e trade-offs | [Arquitetura](arquitetura.md) |
| Limitadores de nota que o projeto protege | Aplicação executa, CI, autorização e dupla reserva: [Evidências](evidencias/README.md) |

## 3. Arquitetura e decisões

| Item | Onde está |
|---|---|
| Diagramas C4 e sequências críticas | [Arquitetura](arquitetura.md) |
| ADRs 0001 a 0010 (o 0010 é a proteção contra dupla reserva) | `docs/adr/` |
| Decisões de qualidade | [Decisão de qualidade](decisao-qualidade.md) |

## 4. Estratégia de testes e resultados

| Item | Resultado | Fonte | Task |
|---|---|---|---|
| Plano de teste | Existe | `docs/plano-de-teste.md` | T-06 |
| Relatório de testes consolidado | 1023 testes por tipo e por pacote, cobertura por pacote e o run do CI | [relatorio-de-testes.md](relatorio-de-testes.md) | T-52 |
| Demonstração da aplicação | Roteiro de 8 minutos executado de verdade | [entrega/demonstracao.md](entrega/demonstracao.md) | T-54 |
| Plano TDD (casos por regra) | Existe; todos os casos das regras críticas viraram testes | `docs/testes/plano-tdd.md` | T-19 |
| Testes automatizados | **1023 testes, 0 falhas, 0 erros**, entre unitários, parametrizados, de integração e de API caixa-preta | `./mvnw verify` em 2026-11-11; run verde do CI na `main` | T-20 a T-51 |
| Testcontainers e WireMock | Toda a suíte de API roda com PostgreSQL real; a notificação externa é testada com WireMock (INT-01 a INT-05, timeouts, token que não vaza) | `TesteDeApiComBanco`; `NotificadorHttpWireMockTest` | T-30, T-31 |
| Cobertura (JaCoCo) | **95,9 % de linhas** (1356/1398) e **85,1 % de branches** (467/543); o build falha abaixo de 80 % e 70 % | `target/site/jacoco` depois do `verify` | T-27 |
| Análise estática (SonarCloud) | **0 bugs, 0 vulnerabilidades, 0 code smells, 0 hotspots**, duplicação 0,0 %, cobertura 93,9 % (medida do Sonar), **Quality Gate aprovado** | Painel do SonarCloud da `main` | T-29 |
| Teste de concorrência (dupla reserva) | 20 solicitantes simultâneos: 1 aceita e 19 recusadas (409), 1 reserva ativa no banco, estável em 5 repetições e 8 execuções seguidas; sem a proteção, 20 inserções passam (vermelho provado) | [T-37-vermelho.md](evidencias/T-37-vermelho.md), [T-37-verde.md](evidencias/T-37-verde.md) | T-37 |
| Teste de carga (JMeter) | 50 usuários, p95 de **2 a 7 ms** (meta 300 ms), **0 erro real**, 409 separado do erro, banco com 1 reserva ativa por intervalo | [resultado-01.md](performance/resultado-01.md) | T-38 |
| TDD com evidência | 21 pares `test(red)` → `feat(green)` | [evidencia-tdd.md](testes/evidencia-tdd.md) | T-15 a T-51 |
| Mutação manual nas regras críticas | 5 mutações, todas pegas pelos testes | [mutacao-manual.md](testes/mutacao-manual.md) | T-50 |

## 5. Segurança e robustez

| Item | Resultado | Fonte | Task |
|---|---|---|---|
| Matriz de autorização (perfil × operação) | 36 endpoints; `MatrizDeAutorizacaoApiTest` percorre cada célula com tokens reais (176 casos), mais posse, ordem dos erros, rotas fora da matriz e completude; 0 falhas | [matriz](seguranca/matriz-autorizacao.md); [T-49](evidencias/T-49-matriz-de-autorizacao.md) | T-48, T-49 |
| Validação de entrada | 400 com campo e motivo; restrições `CHECK`, `UNIQUE` e `EXCLUDE` no banco como última defesa | `EntradaInvalidaSemVazamentoApiTest`; testes das specs T-16 a T-18, T-20 e T-22 | T-22 |
| Tratamento seguro de erros | Nenhuma resposta traz stack trace, SQL, nome de classe ou caminho (lista única `TermosProibidos`) | `ErrosDaApiTest`, `ErrosDasTelasTest`, `MatrizDeAutorizacaoApiTest` | T-22 |
| Senhas em hash e segredos fora do repositório | BCrypt com sal; chave de assinatura e primeiro administrador só por variável de ambiente; nenhuma senha em log | `CodificadorDeSenhaTest`, `AdministradorInicialTest`, `LoginApiTest` | T-15 |
| Robustez sob concorrência | Deadlock do PostgreSQL traduzido para 409 (DEF-02) | [registro de defeitos](defeitos/registro.md) | T-37 |

## 6. Rastreabilidade

| Item | Onde está |
|---|---|
| Matriz requisito → risco → teste → evidência | [RTM](RTM.md) |
| Status dos requisitos | [status-requisitos.md](status-requisitos.md) |
| Índice de evidências | [Evidências](evidencias/README.md) |

## 7. Gestão de defeitos

| Item | Onde está |
|---|---|
| Ciclo de vida do defeito, severidade e prioridade | [Defeitos](defeitos/README.md) |
| Defeitos encontrados | [Registro](defeitos/registro.md): **DEF-01** (documentação), **DEF-02** (deadlock virava 500, achado pelo teste de concorrência) e **DEF-03** (2 pendências críticas do SonarCloud), todos com causa raiz e correção |

## 8. Limitações e riscos aceitos

Ver [decisão de qualidade](decisao-qualidade.md), seções 2 e 3. Em resumo: o **job periódico** (T-60) e o **BDD com Cucumber** ficaram de fora, e o teste de carga foi **local** (aplicação, banco e JMeter na mesma máquina), então prova a folga em relação à meta e o comportamento sob disputa, e não a capacidade de produção.

## 9. Autoria

Cada integrante explica o próprio código e os próprios testes na prova oral. A tabela diz quem fez o quê **conforme o histórico do Git**, que qualquer pessoa pode conferir: `git shortlog -sn --no-merges` (Renata 206 commits, Samuel 27, Caio 20; **81 % dos commits e todo o código-fonte em `src/` são da Renata**) e `gh pr list --state all` (39 dos 54 PRs). A distribuição planejada e o realizado de cada sprint estão em [sprints.md](planejamento/sprints.md).

| Integrante | O que fez | O que explica na prova oral |
|---|---|---|
| Renata | Código, testes e specs das tasks T-15 a T-24, T-31, T-35, T-37, T-40 a T-43, T-48, T-49 e T-51; o pipeline de CI, o gate do JaCoCo e o SonarCloud (T-25, T-27, T-29); o teste de carga (T-38); a arquitetura, o ATAM e os ADRs (T-10 a T-13); os defeitos DEF-02 e DEF-03; a RTM, as evidências e os documentos finais | Regras de reserva, ciclo de vida, dupla reserva e o `EXCLUDE`, autorização, auditoria, manutenção, relatório, carga, CI e qualidade |
| Samuel | Plano de teste (T-06), spec da notificação externa (T-31), pareceres das specs T-16, T-18, T-21, T-22 e T-37, run vermelho intencional do CI (T-25) | O plano de teste, a spec de notificação externa e as revisões |
| Caio | Diagnóstico (T-03), personas (T-04), utility tree (T-05), projeto Spring Boot (T-07), AIOX (T-08), PRD (T-09) e pareceres das specs T-15, T-17, T-20 e T-31 | O PRD, as personas, o diagnóstico e as revisões |
