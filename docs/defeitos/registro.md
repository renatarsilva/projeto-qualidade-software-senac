# Registro de defeitos

Lista dos defeitos encontrados, priorizados e corrigidos. O processo está em [`README.md`](README.md).

| ID | Título | Severidade | Prioridade | Estado |
|---|---|---|---|---|
| DEF-01 | A arquitetura cita a task de código como evidência de teste | Baixa | P2 | Fechado |
| DEF-02 | Deadlock do PostgreSQL sob concorrência vira 500 em vez de 409 | Crítica | P1 | Fechado |
| DEF-03 | SonarCloud reprova o Quality Gate: 1 bug (S9352) e 1 vulnerabilidade (S4502) críticos | Crítica | P1 | Em verificação |

## DEF-01 · A arquitetura cita a task de código como evidência de teste

- **Encontrado em:** revisão do PR #21 (plano de teste), divergência DV-01
- **Onde:** `docs/arquitetura.md`, seções 5.1 (RF-04 e RF-05) e 6.4
- **Resultado obtido:** o teste de sobreposição aparecia como evidência da T-23 e da T-24, que são as tasks de **código** (fase green)
- **Resultado esperado:** a evidência é a task que **escreve o teste** (fase red): T-20 e T-21
- **Severidade: Baixa.** É um erro de documentação, sem efeito no sistema
- **Prioridade: P2.** A matriz é o começo da rastreabilidade (10% da nota), e a correção é de poucas linhas
- **Correção:** PR #22 (três referências trocadas e a regra escrita no cabeçalho da matriz)
- **Verificação:** buscar `T-23` e `T-24` em `docs/arquitetura.md`; só a frase da regra deve restar (conferido). Confirmado na revisão (duas aprovações) e no merge do PR #22

### Causa raiz (5 porquês)

1. Por que apareceu? A matriz e a tabela de táticas citavam a T-23 como task de teste.
2. Por que isso aconteceu? A coluna "evidência" foi preenchida com a task que implementa o comportamento.
3. Por que isso aconteceu? A matriz foi preenchida pelo backlog, sem conferir a fase (red ou green) de cada task.
4. Por que ninguém percebeu antes? A revisão conferiu IDs e links, mas não a fase, e não havia checklist de referências.
5. O que muda no processo? Ver a prevenção abaixo.

### Prevenção

- Regra escrita no cabeçalho da matriz: a evidência é a task da fase red (feito no PR #22).
- Item no checklist de revisão: "a evidência aponta para a task que escreve o teste?".

## DEF-02 · Deadlock do PostgreSQL sob concorrência vira 500 em vez de 409

- **Encontrado em:** bateria vermelha da T-37 (teste de concorrência), 2026-11-10
- **Onde:** `ReservaService` (`gravando`) e `AgendaNoBanco`, na gravação da reserva
- **Resultado obtido:** com duas ou mais solicitações simultâneas para o mesmo recurso e intervalo, o PostgreSQL pode abortar uma transação com `deadlock detected` (SQLState `40P01`) em vez de recusá-la com a violação do `EXCLUDE` (`23P01`). A aplicação só traduz `DataIntegrityViolationException`, então o deadlock chega ao cliente como **500 "Erro inesperado"**. Ele apareceu no teste das 2 solicitações (4 das 4 repetições), na sobreposição parcial, no "recusado pede outro horário" e no teste de mensagens das 20 solicitações; o resultado varia entre execuções (o das 20 solicitações passou nas 5 repetições dessa execução)
- **Resultado esperado:** toda solicitação perdedora recebe 409 (spec T-37, CA-03 e CA-14), com a mensagem genérica "Esta operação conflita com outro registro" quando só o banco decide
- **Severidade: Crítica.** A reserva continua única (o `EXCLUDE` protege), mas o contrato de recusa quebra sob concorrência, justamente no cenário do RN-03 e do limitador de nota
- **Prioridade: P1.** Bloqueia a T-37 e o critério "1 aceita e N-1 recusadas com 409"
- **Correção:** traduzir `PessimisticLockingFailureException` (`40P01`) para o 409 genérico e gravar os vínculos ordenados por `recurso_id` (green da T-37, ADR-0010)
- **Verificação:** os testes `*ConcorrenciaTest` e `DeadlockNaGravacaoTest` passam; evidência em `docs/evidencias/T-37-vermelho.md` e `T-37-verde.md`

### Causa raiz (5 porquês)

1. Por que apareceu? Uma transação foi abortada pelo banco por deadlock e a aplicação respondeu 500.
2. Por que o banco abortou? Com um `EXCLUDE`, duas inserções conflitantes e ainda não confirmadas esperam uma pela outra; o PostgreSQL detecta o ciclo e aborta uma delas com `40P01` (não com `23P01`).
3. Por que a aplicação não tratou? O tratamento da T-23 previu só a violação de integridade (`23P01`), não o deadlock.
4. Por que ninguém percebeu antes? Os testes da T-20 e da T-23 são sequenciais; só a disputa real de threads exercita esse caminho.
5. O que muda no processo? Todo erro transitório de transação do banco (deadlock e falha de serialização) tem tradução e teste, e a ordem de gravação dos recursos é fixa.

### Prevenção

- Teste de concorrência com threads de verdade em toda proteção contra dupla reserva (T-37).
- Gravação dos vínculos sempre ordenada por `recurso_id`.

## DEF-03 · SonarCloud reprova o Quality Gate com 1 bug e 1 vulnerabilidade críticos

- **Encontrado em:** consulta ao SonarCloud na preparação da entrega, 2026-11-11 (Quality Gate em `ERROR`; cobertura 93,9 %, 0 code smells)
- **Resultado obtido:** (1) bug crítico **S9352** em `OuvinteDeNotificacao`: "múltiplos beans casam com a dependência (`notificadorEmLog`, `notificadorHttp`)"; (2) vulnerabilidade crítica **S4502** em `SegurancaConfiguracao`: "CSRF desabilitado"
- **Resultado esperado:** 0 bugs e 0 vulnerabilidades críticas (meta da entrega) e Quality Gate aprovado
- **Severidade: Crítica** (pela classificação do Sonar e pela meta de 0 vulnerabilidades críticas). **Prioridade: P1** (a entrega é 12/11)
- **Análise:** (1) em execução só um dos dois adaptadores existe, escolhido por configuração, mas a análise estática vê dois `@Component` do mesmo tipo e a injeção parece ambígua; é uma fragilidade real de estrutura. (2) O CSRF é desligado só na cadeia da API, que é sem estado, usa token Bearer e não usa cookie; é decisão do ADR-0007, e não vulnerabilidade, mas precisa estar **revisada e justificada** no código
- **Correção:** (1) um único `@Bean Notificador` em `NotificacaoConfiguracao` escolhe o modo simulado ou o HTTP, e os dois `@Component` condicionais saem; teste novo do bean escolhido. (2) A linha ganha o comentário `NOSONAR java:S4502` com a justificativa e a referência ao ADR-0007
- **Verificação:** `./mvnw verify` verde e o Quality Gate do SonarCloud da `main` depois do merge

### Causa raiz (5 porquês)

1. Por que apareceu? O SonarCloud analisou a `main` e marcou os dois pontos como críticos.
2. Por que passou despercebido? O check do PR ficou verde, mas ninguém abriu o painel do SonarCloud para ler as issues, e o Quality Gate não era requisito do ruleset.
3. Por que o bug existia? Dois `@Component` condicionais do mesmo tipo funcionam em execução, mas a análise estática não avalia as condições.
4. Por que o CSRF não estava justificado? A decisão estava no ADR-0007, mas não no código, onde a ferramenta (e o revisor) olha.
5. O que muda no processo? Ver o painel do SonarCloud antes de cada entrega e registrar no código a justificativa de toda regra de segurança desligada de propósito.

### Prevenção

- Escolher implementações alternativas em um único `@Bean`, e não com `@Component` condicionais.
- Toda exceção a uma regra do Sonar leva comentário com o motivo e a referência da decisão.
