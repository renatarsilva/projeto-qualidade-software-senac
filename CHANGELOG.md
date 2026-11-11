# Changelog

Todas as mudanças relevantes deste projeto ficam neste arquivo.
O formato segue o [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o projeto usa [versionamento semântico](https://semver.org/lang/pt-BR/).

## [Não lançado]

Nada além da versão abaixo.

## [1.0.0-rc1] - 2026-11-12

Release candidate da entrega do trabalho final de Qualidade de Software (semana 46).

### Adicionado

- **Autenticação e autorização (T-15):** login com token de 30 minutos, senha em BCrypt, três perfis, perfil conferido no banco a cada chamada, rotas negadas por padrão e gestão de usuários pelo Administrador.
- **Cadastros (T-16, T-17, T-18):** salas (`sala-NN`), professores (com matrícula e competências) e materiais (com patrimônio único), com restrições no banco.
- **Erros unificados (T-22):** todo erro da API é `ProblemDetail` em português, sem detalhe interno.
- **Reservas (T-20 a T-24):** intervalo `[início, fim)`, sobreposição em sala, material e professor (tudo ou nada), antecedência mínima de 1 hora, alteração com prazo, cancelamento em três formas, check-in e a tabela das 7 transições de estado.
- **Dupla reserva (T-37):** `EXCLUDE` do banco, gravação em ordem de id, deadlock traduzido para 409 e teste com 20 solicitantes simultâneos (ADR-0010).
- **Pesquisa e consulta (T-43):** pesquisa de recursos por tipo, capacidade, localização, competência e disponibilidade; consulta de reservas com posse.
- **Manutenção (T-35):** registro e encerramento de períodos, reserva recusada nas fronteiras, reservas afetadas listadas.
- **Retirada e devolução (T-42):** só o Responsável registra, só de reserva aprovada, devolução atrasada sinalizada.
- **Auditoria (T-40):** registro de toda mudança de estado, imutável por gatilho do banco, com consulta para Responsável e Administrador.
- **Notificação externa (T-31):** adaptador HTTP com timeouts por configuração, modo simulado e envio depois do commit; testado com WireMock.
- **Relatório de utilização (T-41):** horas por recurso, carga do professor e conflitos evitados, com oráculo calculado à mão.
- **Telas (T-44, T-45, T-46):** Thymeleaf e Bootstrap responsivos (conferido no Chrome em 375 px), login por formulário com sessão e CSRF, menu por perfil e autorização das rotas negada por padrão. Solicitante: pesquisa, reserva, alteração, cancelamento e check-in. Responsável: fila de aprovação, retirada, devolução e auditoria. Administrador: cadastros, usuários, manutenção e relatório. Mensagens de erro em linguagem clara.
- **Documentação da API (T-51):** OpenAPI e Swagger UI com perfis permitidos e erros de cada endpoint.
- **Qualidade:** CI com JaCoCo (gate de 80 % de linhas e 70 % de branches) e SonarCloud, matriz de autorização testada célula a célula (T-48, T-49), teste de carga com JMeter (T-38), mutação manual, RTM, status dos requisitos e relatório de qualidade.

### Alterado

- O `Notificador` passa a ser escolhido por um único bean conforme a configuração (DEF-03).

### Corrigido

- **DEF-02:** o deadlock do PostgreSQL sob concorrência virava erro 500; agora é 409.
- **DEF-03:** as duas pendências críticas do SonarCloud (S9352 e S4502); o Quality Gate passou.
- **DEF-04:** nas telas, o acesso negado (403) aparecia como erro 500; agora mostra a página amigável com 403.

### Limitações conhecidas

- O teste de navegador (`e2e/`) roda à mão, e não no CI; nas telas, sala, material e professor só têm listagem e cadastro (a edição existe na API).
- O **job periódico** das mudanças por tempo (T-60) e o **BDD com Cucumber** ficaram de fora.
- O teste de carga foi local. Ver [decisão de qualidade](docs/decisao-qualidade.md).
