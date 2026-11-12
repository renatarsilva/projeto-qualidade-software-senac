# Organização de Recursos — Projeto de Qualidade de Software

Projeto semestral da disciplina **Qualidade de Software** (TADS · Senac · 2026.2), com o Prof. Afonso Lelis.

O sistema organiza a reserva de **salas, professores e materiais** de uma instituição de ensino, sem conflitos de horário. O foco da avaliação não é só fazer o sistema funcionar: é apresentar **evidências objetivas** de qualidade, segurança, rastreabilidade e desempenho.

> **Status:** versão `v1.0.0-rc3` entregue. API completa com as regras críticas provadas por **1043 testes** (96,0 % de linhas, 85,2 % de branches), CI e SonarCloud verdes. As **telas responsivas dos três perfis** (Thymeleaf e Bootstrap) estão entregues e foram conferidas no Chrome em 375 px; o que ficou de fora está dito em [status dos requisitos](docs/status-requisitos.md). Para ver o sistema funcionando, siga a [demonstração](docs/entrega/demonstracao.md).

## Executar em 5 minutos (para quem avalia)

**Você precisa de:** Java 21, Docker aberto e Git. O Maven não precisa instalar (o projeto traz o `mvnw`).

**macOS ou Linux (um comando):**

```bash
git clone https://github.com/renatarsilva/projeto-qualidade-software-senac.git
cd projeto-qualidade-software-senac
./scripts/executar.sh
```

O script acha o Java 21, confere o Docker, define as variáveis com valores de demonstração e sobe a aplicação. Quando aparecer `Started OrganizacaoRecursosApplication`, abra **http://localhost:8080** e entre com:

| E-mail | Senha |
|---|---|
| `admin@exemplo.com` | `senha-do-admin-123` |

**Windows (PowerShell):**

```powershell
git clone https://github.com/renatarsilva/projeto-qualidade-software-senac.git
cd projeto-qualidade-software-senac
$env:JAVA_HOME = "C:\caminho\do\jdk-21"
$env:APP_JWT_CHAVE = "chave-de-demonstracao-local-com-mais-de-32-caracteres"
$env:APP_ADMIN_EMAIL = "admin@exemplo.com"
$env:APP_ADMIN_SENHA = "senha-do-admin-123"
.\mvnw.cmd spring-boot:run
```

### Roteiro de 3 minutos nas telas

O menu do topo muda conforme o perfil. O administrador cadastra; o solicitante reserva; o responsável aprova.

1. **Administrador:** em **Salas**, cadastre `sala-101`. Em **Usuários**, crie um **Solicitante** e um **Responsável** (senha com 8 a 72 caracteres). Clique em **Sair**.
2. **Solicitante:** em **Nova reserva**, escolha um horário futuro (pelo menos 1 hora de antecedência), marque a sala e salve. A reserva nasce **Solicitada**.
3. **Conflito:** peça a mesma sala em horário que se sobrepõe. A tela recusa com a mensagem da regra.
4. **Responsável:** em **Aprovações**, aprove. Abra a reserva e veja o **Histórico de mudanças**.
5. **Perfil errado:** como Solicitante, abra `http://localhost:8080/admin/salas`: **Acesso negado** (403).

A documentação da API (Swagger) fica em http://localhost:8080/swagger-ui.html. O roteiro completo, com respostas reais, está em [docs/entrega/demonstracao.md](docs/entrega/demonstracao.md).

### Rodar os testes

```bash
./mvnw verify
```

Precisa do Docker ligado (os testes sobem um PostgreSQL de verdade). Roda **1043 testes** em cerca de 4 minutos, incluindo os 17 cenários BDD (Cucumber) e o teste ponta a ponta, e confere o gate de cobertura (80 % de linhas, 70 % de branches). Relatórios: `target/site/jacoco/index.html` (cobertura) e [docs/relatorio-de-testes.md](docs/relatorio-de-testes.md).

### Se der erro

| Mensagem | O que fazer |
|---|---|
| `JAVA_HOME environment variable is not defined correctly` | Aponte `JAVA_HOME` para um JDK 21. No macOS: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` |
| `Cannot connect to the Docker daemon` ou o script diz que o Docker não está ligado | Abra o Docker Desktop, espere ele iniciar e rode de novo |
| `Port 8080 was already in use` | Feche o que usa a porta 8080 (outra execução do projeto) ou espere a anterior parar |
| Falha ao baixar dependências na primeira vez | Confira a internet e rode de novo: o Maven baixa tudo na primeira execução (1 a 2 minutos) |

Para parar a aplicação, use `Ctrl+C` no terminal.

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

### Evidência do CI (T-25)

O workflow `qualidade` (GitHub Actions) roda `./mvnw verify` a cada pull request e publica os relatórios de teste e de cobertura como artefatos.

- Run vermelho intencional (asserção do `tc03` da `PoliticaDeSenhaTest` invertida): https://github.com/renatarsilva/projeto-qualidade-software-senac/actions/runs/42518673377 — 08/11/2026
- Recuperação após o revert: https://github.com/renatarsilva/projeto-qualidade-software-senac/actions/runs/42542405648 — 08/11/2026

No run vermelho, só o artefato `relatorios-de-teste` é publicado: o relatório do JaCoCo é gerado depois dos testes e não existe quando um teste falha.

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

**Acesso.** A aplicação sobe em http://localhost:8080. **Telas:** abra o endereço e entre com o e-mail e a senha do administrador das variáveis abaixo (o menu muda conforme o perfil: Solicitante, Responsável ou Administrador). **API:** toda rota `/api/**` exige o token `Bearer` e devolve `401` sem ele; o token vem de `POST /api/auth/login` com `{"email":"...","senha":"..."}` e vale 30 minutos. O primeiro administrador é criado na subida a partir de `APP_ADMIN_EMAIL` e `APP_ADMIN_SENHA`. A documentação da API fica pública em http://localhost:8080/swagger-ui.html.

### Variáveis de ambiente

Os segredos nunca ficam no repositório. A aplicação lê estas variáveis (os nomes valem para qualquer sistema; os valores são seus):

| Variável | Para que serve | Obrigatória |
|---|---|---|
| `APP_JWT_CHAVE` | Chave que assina o token da API. Use um texto aleatório e longo. | Sim |
| `APP_ADMIN_EMAIL` | E-mail do primeiro administrador, criado na subida se o banco não tiver nenhum. | Só no primeiro uso |
| `APP_ADMIN_SENHA` | Senha desse administrador (de 8 a 72 bytes). | Só no primeiro uso |

Defina antes de subir a aplicação. Linux/macOS:

```bash
export APP_JWT_CHAVE='<texto aleatório e longo>'
export APP_ADMIN_EMAIL='<seu e-mail>'
export APP_ADMIN_SENHA='<uma senha>'
```

Windows PowerShell: `$env:APP_JWT_CHAVE = '<texto aleatório e longo>'` (e o mesmo para as outras). No IntelliJ, use **Run → Edit Configurations → Environment variables**. Não grave esses valores em arquivos do projeto.

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
| 1 | 15/10 a 21/10 | 32 a 35 | Ambiente, diagnóstico, PRD, plano de teste, arquitetura e ATAM | ✅ |
| 2 | 22/10 a 28/10 | 36 a 39 | Autenticação, cadastros, TDD das reservas, CI, JaCoCo, SonarCloud, Testcontainers e WireMock | ✅ (menos a T-26) |
| 3 | 29/10 a 04/11 | 41 a 43 | Aprovação e manutenção (BDD), dupla reserva, carga, auditoria e relatórios | ✅ |
| 4 | 05/11 a 11/11 | 44 a 46 | Interface, autorização, RTM, relatório de qualidade e release `v1.0.0-rc3` | ✅ |

Planejado e realizado de cada sprint em [sprints](docs/planejamento/sprints.md) e [backlog](docs/planejamento/backlog.md). Entrega na semana 46 e prova oral na semana 47.

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

## Documentação da API

Com a aplicação rodando (`./mvnw spring-boot:run`), a documentação fica em:

- **Swagger UI:** http://localhost:8080/swagger-ui.html (abre sem token e permite testar com o botão *Authorize*).
- **Documento OpenAPI (JSON):** http://localhost:8080/v3/api-docs.

Todos os endpoints estão descritos com o resumo, os **perfis permitidos** (lidos das mesmas regras de acesso da segurança), as respostas de erro (400, 401, 403, 404 e 409) e o esquema do token Bearer. Os erros seguem o formato `ProblemDetail` (RFC 9457).

**Fluxo público:** só `POST /api/auth/login` é público. Ele devolve o token (válido por 30 minutos) que as demais rotas exigem no cabeçalho `Authorization: Bearer <token>`. A [matriz de autorização](docs/seguranca/matriz-autorizacao.md) lista cada operação e o status esperado por perfil.
