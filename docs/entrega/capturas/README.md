# Capturas de tela da entrega

Índice das capturas guardadas nesta pasta. Cada uma é **evidência** de um item da [RTM](../../RTM.md) e do [relatório de qualidade](../../relatorio-qualidade.md).

## 1. Qualidade: CI, SonarCloud e cobertura (`qualidade/`)

| Captura | O que mostra | Evidência de |
|---|---|---|
| [ci-run-verde-rc3.png](qualidade/ci-run-verde-rc3.png) | O run do GitHub Actions da tag **`v1.0.0-rc3`** (commit `cdec55f`, o merge do PR #64): status **Success**, job `build` verde em 4 min 7 s. Os avisos ao pé da página são da própria plataforma (versão do Node e do `setup-java`), não do projeto | CI presente e verde (T-25, T-56) |
| [sonarcloud-quality-gate.png](qualidade/sonarcloud-quality-gate.png) | O painel do SonarCloud em 2026-11-12: **Quality Gate Passed**; segurança, confiabilidade e manutenibilidade nota A, com 0 problemas; 100% dos hotspots revisados; cobertura **93,0%**; duplicação **0,0%** | 0 bugs e 0 vulnerabilidades críticas (T-29) |
| [jacoco-relatorio.png](qualidade/jacoco-relatorio.png) | O relatório do JaCoCo do `./mvnw verify` (1043 testes): **96%** das instruções e das linhas, **85%** dos branches (103 de 698 perdidos), por pacote, incluindo o pacote `web` das telas | Metas de 80% de linhas e 70% de branches (T-27) |

Nota sobre o painel do SonarCloud: o indicador **"Dependency risks: C, 90"** é um recurso novo da ferramenta, que olha bibliotecas de terceiros com vulnerabilidade conhecida. Ele é separado do Quality Gate (que está aprovado) e **não foi tratado** neste trabalho. O Sonar mede 93,0% de cobertura e o JaCoCo mede 96%: são ferramentas diferentes, com critérios diferentes (o Sonar exclui a classe principal da aplicação, por exemplo).

## 2. Telas, em celular (375 px) e desktop

Geradas pelo roteiro do Chrome de verdade, em [`e2e/`](../../../e2e/README.md). O resultado das 31 verificações está em [resultado.json](resultado.json).

| Captura | O que mostra |
|---|---|
| [01-login-375px.png](01-login-375px.png) | Tela de login |
| [02-menu-do-administrador-375px.png](02-menu-do-administrador-375px.png) | Menu recolhido e aberto do Administrador |
| [03-salas-375px.png](03-salas-375px.png), [04-usuarios-375px.png](04-usuarios-375px.png), [05-manutencao-375px.png](05-manutencao-375px.png) | Cadastros e manutenção do Administrador |
| [06-pesquisa-375px.png](06-pesquisa-375px.png) | Pesquisa de recursos livres |
| [07-nova-reserva-375px.png](07-nova-reserva-375px.png), [08-reserva-criada-375px.png](08-reserva-criada-375px.png) | Nova reserva e reserva criada como **Solicitada** |
| [09-conflito-375px.png](09-conflito-375px.png) | Mensagem de conflito de horário |
| [10-minhas-reservas-375px.png](10-minhas-reservas-375px.png) | Lista do Solicitante |
| [11-acesso-negado-375px.png](11-acesso-negado-375px.png) | Página de acesso negado (403) |
| [12-fila-de-aprovacao-375px.png](12-fila-de-aprovacao-375px.png), [13-historico-375px.png](13-historico-375px.png) | Fila de aprovação e histórico de mudanças (auditoria) |
| [14-relatorio-375px.png](14-relatorio-375px.png) | Relatório de utilização |
| [15-reserva-desktop.png](15-reserva-desktop.png), [16-fila-desktop.png](16-fila-desktop.png) | A mesma interface em tela larga |
