# Checklist de entrega (semana 46)

Cada item tem a task dona e uma caixa de marcar. Só marque com a evidência em mãos (arquivo, run do CI ou link). Fonte: [contexto da disciplina](../projeto/contexto-da-disciplina.md).

> **Data da entrega:** 12/11/2026 (confirmada pela equipe). **Prova oral:** semana 47.

## 1. Limitadores de nota (bloqueiam a nota se falharem)

- [x] **A aplicação executa** a partir de um clone limpo, seguindo só o README (T-55): a aplicação subiu com `./mvnw spring-boot:run` e o PostgreSQL em contêiner na carga de 2026-11-11 ([resultado-01.md](../performance/resultado-01.md)); **clone limpo conferido em 2026-11-11**: `git clone` do repositório público, `./mvnw verify` com 940 testes e 0 falhas e o gate de cobertura atendido, e `spring-boot:run` com as 3 variáveis do README (`401` sem token, login do administrador devolvendo token, Swagger UI com `200`)
- [x] **CI presente e verde** (T-25): runs verdes na `main` e em todos os PRs, e o run vermelho intencional registrado. A tag `v1.0.0-rc1` ainda precisa ser criada (T-56)
- [x] **Sem falha de autorização**: matriz de autorização e teste parametrizado passando, 176 casos (T-48, T-49)
- [x] **Sem possibilidade de dupla reserva**: teste de concorrência passando e `EXCLUDE` do banco ativo (T-37)

## 2. Itens da semana 46 (o professor confere)

| Item | Dono | Pronto |
|---|---|---|
| Clone limpo e checklist de entrega (este arquivo) | T-55, Caio | - [x] |
| Tag `v1.0.0-rc1` criada, com CI verde e o run da tag aberto | T-56, Samuel | - [ ] |
| `CHANGELOG.md` com a versão `v1.0.0-rc1` | T-56, Samuel | - [x] |
| `docs/evidencias/README.md` com todas as evidências preenchidas | T-56, Samuel | - [x] |
| `docs/decisao-qualidade.md` completo | T-57, Renata | - [x] |
| Envio no formulário do Hub: um integrante envia pelo grupo, com a turma, o nome completo dos 3 integrantes e o link do repositório | T-57, Renata | - [ ] |
| Repositório **público** (o formulário confere antes de enviar) | T-57, Renata | - [ ] |
| Comprovante do envio guardado (reenviar com o mesmo link se precisar corrigir) | T-57, Renata | - [ ] |

Formulário: https://afonsolelis.github.io/aulas_senac/pages/qualidade2/entrega/entrega.html

## 3. Entregáveis das semanas anteriores (devem estar no repositório)

| Semana | Entregável | Task | Pronto |
|---|---|---|---|
| 32 | Diagnóstico com 5 achados ou mais, classificados pela ISO 25010 | T-03 | - [x] |
| 33 | Personas, utility tree (3 atributos, 6 folhas), 3 cenários com medida numérica, 3 casos de teste | T-04, T-05 | - [x] |
| 34 | Projeto Spring Boot 3, `./mvnw test` verde, `docs/prd.md`, `docs/arquitetura.md`, `docs/plano-de-teste.md` | T-06, T-07, T-09 | - [x] |
| 35 | Arquitetura com C4, sequências, ATAM, ADRs e matriz requisito → decisão → componente → evidência | T-10 a T-13 | - [x] |
| 36 | Registro de defeitos, `docs/testes/plano-tdd.md`, testes na fase red | T-14, T-19 a T-21 | - [x] |
| 37 | `.github/workflows/ci.yml`, run vermelho intencional registrado, `docs/avaliacao-arquitetural/ciclo-01.md` | T-25, T-26 | - [ ] (falta `docs/avaliacao-arquitetural/ciclo-01.md`) |
| 38 | Gate de cobertura 80/70 no `verify`, cobertura de ramo na sobreposição, Quality Gate do SonarCloud no PR | T-27 a T-29 | - [x] |
| 39 | Integração externa isolada com WireMock (INT-01 a INT-05), banco em container, RTM | T-30, T-31, T-32 | - [x] |
| 41 | `docs/testes/bdd-aprovacao-bloqueio.md`, commits `test(red)`, `feat(green)` e `refactor`, feature Cucumber, `docs/testes/evidencia-tdd.md` | T-33 a T-36 | - [ ] (TDD com evidência em `docs/testes/evidencia-tdd.md`; BDD e feature Cucumber não foram feitos) |
| 42 | Teste de 20 solicitações simultâneas, ADR da proteção, `perf/reservas.jmx`, `docs/performance/rnf.md` e `resultado-01.md` | T-37, T-38 | - [x] |
| 43 | `docs/status-requisitos.md`, auditoria com rollback testado, relatório de utilização com oráculo à mão | T-39 a T-41 | - [x] |
| 44 | `docs/seguranca/matriz-autorizacao.md`, teste parametrizado, `docs/testes/mutacao-manual.md`, RTM a 100 % | T-48 a T-50 | - [x] |
| 45 | `docs/relatorio-qualidade.md`, `docs/prova-oral/perguntas.md` e `ensaio.md` | T-52 a T-54 | - [ ] (relatório e perguntas prontos; falta o `ensaio.md`, que depende da equipe) |

## 4. Metas mensuráveis

| Meta | Valor | Evidência | Atingida |
|---|---|---|---|
| Cobertura de linhas | ≥ 80 % | JaCoCo: **97,0 %** | - [x] |
| Cobertura de branches | ≥ 70 % | JaCoCo: **86,0 %** | - [x] |
| Requisitos críticos na RTM | 100 % | [RTM](../RTM.md): RN-01 a RN-09 com evidência | - [x] |
| Bugs ou vulnerabilidades críticas | 0 | SonarCloud da `main`: 0 bugs, 0 vulnerabilidades, Quality Gate aprovado | - [x] |

## 4.1 Demonstração e relatório de testes

- [x] [Relatório de testes](../relatorio-de-testes.md): 940 testes, cobertura por pacote e o run do CI
- [x] [Demonstração da aplicação](demonstracao.md): roteiro de 8 minutos executado de verdade, com as respostas reais
- [ ] Capturas de tela da apresentação (Swagger UI, SonarCloud, CI, JaCoCo) em `docs/entrega/capturas/`

## 5. Autoria e prova oral

- [ ] Cada integrante sabe explicar o próprio código e os próprios testes (T-54, T-58)
- [ ] Roteiro de 8 minutos ensaiado: problema e risco, arquitetura, demonstração, testes e CI, RTM e relatório, limitações (T-54)
