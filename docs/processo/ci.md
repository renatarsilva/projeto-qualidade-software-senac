# Integração contínua (CI)

Este documento explica o pipeline do projeto (tasks T-25, T-27 e T-29). O arquivo do pipeline é [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml).

## O que o pipeline faz

Em todo pull request e em todo push na `main`, o job `build`:

1. baixa o código (histórico completo, que o SonarCloud pede);
2. instala o Java 21 (Temurin) com cache do Maven;
3. roda `./mvnw -B verify`: compila, executa os testes (surefire e failsafe), gera o relatório do JaCoCo e **falha o build** se a cobertura ficar abaixo de **80 % das linhas** ou de **70 % dos branches**;
4. roda a análise do SonarCloud, **somente se** o segredo `SONAR_TOKEN` existir;
5. publica os relatórios como artefatos, também quando o build falha: `relatorios-de-teste` (surefire e failsafe) e `relatorio-jacoco`.

O run é cancelado se chegar um push novo no mesmo PR, e tem limite de 10 minutos. O pipeline só tem permissão de leitura no repositório.

## Rodar localmente

```bash
java21                 # alias do projeto para o Java 21
./mvnw -B verify       # o mesmo comando do CI
```

Os testes de integração usam Testcontainers, então o **Docker precisa estar rodando**. Sem o Docker, o teste de contexto falha com "Could not find a valid Docker environment".

Relatório de cobertura: `target/site/jacoco/index.html`.

### Ligar e desligar o JaCoCo

A meta de cobertura está no `pom.xml` e **não deve ser afrouxada**. Enquanto o projeto ainda tem pouco código, dá para desligar o JaCoCo só na linha de comando:

```bash
./mvnw -B verify -Djacoco.skip=true   # sem agente, relatório nem gate
./mvnw -B verify                      # com tudo (padrão e o que o CI roda)
```

Fica fora do cálculo apenas a classe `OrganizacaoRecursosApplication`, que só chama `SpringApplication.run` e não tem lógica.

## Exigir o check na main (T-25)

No GitHub, a Renata (administradora) faz uma vez:

1. **Settings → Branches → Add branch ruleset** (ou *Branch protection rule*) para `main`;
2. marque **Require a pull request before merging** e **Require status checks to pass**;
3. adicione o check **`build`** (ele só aparece na lista depois do primeiro run do pipeline);
4. marque **Require branches to be up to date before merging**.

Com isso, um PR com o `build` vermelho não pode ser mergeado.

## SonarCloud (T-29)

1. Importe o repositório no SonarCloud e **desligue a análise automática** (Administration → Analysis Method).
2. No `pom.xml`, troque os dois `TODO-preencher` em `sonar.organization` e `sonar.projectKey` pelos valores do projeto.
3. Em **Settings → Secrets and variables → Actions** do GitHub, crie o segredo `SONAR_TOKEN` com o token gerado no SonarCloud. **Nunca** coloque o token em arquivo do repositório.
4. Defina a janela de "novo código" no SonarCloud e confira que o Quality Gate aparece no PR.

Em PR vindo de fork o segredo fica vazio, e o passo do Sonar é pulado de propósito.

## Registrar o run vermelho intencional (Aula 06)

A aula pede a evidência de que o pipeline **detecta** uma falha e **se recupera**. Faça numa branch de teste, sem forjar nada:

1. Crie uma branch, por exemplo `chore/T-25-run-vermelho`.
2. Quebre uma asserção de propósito em um teste que já existe (por exemplo, troque o valor esperado) e faça o commit.
3. Abra um PR e espere o `build` ficar **vermelho**. Anote o link do run e confira que os relatórios foram publicados nos artefatos.
4. Reverta o commit, dê o push e espere o `build` ficar **verde**. Anote o link desse run.
5. Registre no README, na seção de CI, os dois links e a data. Não apague o PR nem os runs: eles são a evidência.

## Resolução de problemas

- **`Could not find a valid Docker environment`:** inicie o Docker Desktop (ou o daemon) e rode de novo.
- **`Rule violated for bundle ...: lines covered ratio is ...`:** a cobertura ficou abaixo da meta. Escreva testes das regras que faltam. Não mude o limite no `pom.xml`.
- **Aviso 401 de `maven.pkg.github.com/flyway/...`:** vem do repositório de pacotes da Flyway, é inofensivo e não quebra o build.
