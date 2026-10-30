# Organização de Recursos — Projeto de Qualidade de Software

Projeto semestral da disciplina **Qualidade de Software** (TADS · Senac · 2026.2), com o Prof. Afonso Lelis.

O sistema organiza a reserva de **salas, professores e materiais** de uma instituição de ensino, sem conflitos de horário. O foco da avaliação não é só fazer o sistema funcionar: é apresentar **evidências objetivas** de qualidade, segurança, rastreabilidade e desempenho.

> **Status:** em construção, em quatro sprints até a entrega (ver [Roadmap](#roadmap)).

## Como trabalhamos

Toda task nasce no [backlog](docs/planejamento/backlog.md), com um ID `T-NN`, e segue o fluxo **Spec-Driven Development**: spec → revisão por pares → spec validation → grill-with-docs (regras críticas) → TDD → pull request. Leia antes de começar:

- [AI Workflow](docs/processo/ai-workflow.md): o passo a passo de cada task;
- [CONTRIBUTING](CONTRIBUTING.md): branches, commits e pull requests;
- [Índice da documentação](docs/README.md).

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

- Java 21 e Spring Boot 3.5 (Web, Security, Data JPA, Validation)
- PostgreSQL 16, com migrações Flyway
- Interface Thymeleaf + Bootstrap (responsiva)
- Maven, via wrapper (`./mvnw`)
- JUnit 5, Testcontainers, WireMock, Cucumber e JaCoCo
- GitHub Actions e SonarCloud
- JMeter
- [AIOX](https://www.npmjs.com/package/aiox-core) para apoio ao desenvolvimento com agentes de IA

## Como executar

### Pré-requisitos

- **Java 21.** Confira com `java -version`. Outra versão não serve.
- **`JAVA_HOME` apontando para um JDK 21.** O wrapper usa o Java do `JAVA_HOME`, que pode ser diferente do `java` do terminal.
- **Docker** rodando. Ele é usado pelo banco local e pelos testes.
- Não precisa instalar o Maven: o projeto usa o Maven Wrapper (`mvnw` e `mvnw.cmd`), que baixa a versão certa na primeira execução.

### Rodar os testes

Linux/macOS:

```bash
./mvnw verify
```

Windows PowerShell:

```powershell
.\mvnw.cmd verify
```

Windows CMD:

```bat
mvnw.cmd verify
```

Os testes sobem um PostgreSQL real em container (Testcontainers). Não há H2 nem banco em memória.

### Subir a aplicação

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Windows CMD:

```bat
mvnw.cmd spring-boot:run
```

**Banco local.** O Spring Boot usa o `compose.yaml` e sobe o serviço PostgreSQL automaticamente, se ele ainda não estiver rodando. A porta 5432 do container é publicada em uma porta livre do host, e o Spring Boot descobre essa porta sozinho para configurar o DataSource. Para inspecionar o serviço e ver a porta real (útil para conectar um cliente de banco):

```bash
docker compose ps
```

**Segurança no bootstrap.** A aplicação inicia em http://localhost:8080 com a segurança padrão do Spring Boot ativa. Por isso, acessar sem autenticação redireciona para `/login` (no navegador) ou devolve `401` (em clientes como o `curl`). Esse comportamento é esperado nesta fase: a autenticação e a autorização definitivas pertencem à task T-15.

### Abrir no IntelliJ

1. **File → Open** e escolha a pasta do repositório (o IntelliJ reconhece o `pom.xml`).
2. Em **File → Project Structure → Project → SDK**, selecione um **JDK 21**.
3. Para rodar: abra `OrganizacaoRecursosApplication` e clique em ▶ ao lado do `main`.
   Para rodar sem o `compose.yaml`, use `TestOrganizacaoRecursosApplication` (em `src/test`), que sobe o banco com Testcontainers.

> **Validação do ambiente:** em 29/10/2026, Caio validou no Windows, com Java 21: `verify` verde, PostgreSQL 16 subindo pelo Compose e aplicação iniciada na porta 8080.

## Documentação

O índice completo, com a situação de cada documento, está em [`docs/README.md`](docs/README.md).

## Roadmap

| Sprint | Período | Semanas do curso | Foco | Status |
|---|---|---|---|---|
| 1 | 15/10 a 21/10 | 32 a 35 | Ambiente, diagnóstico, PRD, plano de teste, arquitetura e ATAM | ⬜ |
| 2 | 22/10 a 28/10 | 36 a 39 | Autenticação, cadastros, TDD das reservas, CI, JaCoCo, SonarCloud, Testcontainers e WireMock | ⬜ |
| 3 | 29/10 a 04/11 | 41 a 43 | Aprovação e manutenção (BDD), dupla reserva, carga, auditoria e relatórios | ⬜ |
| 4 | 05/11 a 11/11 | 44 a 46 | Interface, autorização, RTM, relatório de qualidade e release `v1.0.0-rc1` | ⬜ |

Detalhes em [sprints](docs/planejamento/sprints.md) e [backlog](docs/planejamento/backlog.md). Entrega na semana 46 e prova oral na semana 47.

## Referências

- [Hub de aulas da disciplina](https://afonsolelis.github.io/aulas_senac/pages/home_qualidade_2026_2.html)
- [Especificação do projeto](https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html)
- [CineSenac](https://github.com/afonsolelis/cinesenac): referência de organização do PRD

## Equipe

| Nome | GitHub |
|---|---|
| Renata | [@renatarsilva](https://github.com/renatarsilva) |
| Samuel | [@Samwel-Merces](https://github.com/Samwel-Merces) |
| Caio | [@CaiosAguiar](https://github.com/CaiosAguiar) |
