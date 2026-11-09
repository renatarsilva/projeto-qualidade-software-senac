# Decisão de qualidade

Registra as decisões de qualidade que o time tomou, o que ficou de fora e os riscos que aceitou. Cada decisão aponta a fonte (ADR, spec ou PRD). Onde a decisão depende de um resultado que ainda não existe, o texto diz `A preencher`.

> **Estado em 2026-11-07:** as decisões abaixo vêm das specs da semana 36 (ainda em revisão por pares) e dos ADRs. Os ADRs 0007 e 0009 estão com status "Proposta". Os resultados medidos (cobertura, carga, concorrência) ainda não existem. A task T-57 fecha este documento.

## 1. Decisões de qualidade tomadas

### 1.1 Processo

| Decisão | Por quê | Fonte |
|---|---|---|
| Nenhum código de produção sem spec revisada por outra pessoa, validada e (se for regra crítica) passada pelo grill | Evita implementar regra mal entendida; deixa a revisão por pares como evidência | ADR-0002; `docs/processo/ai-workflow.md` |
| Sem Jira: branches, commits e PRs levam o ID da task do backlog | Uma fonte só para o andamento | ADR-0003 |
| TDD: o teste nasce vermelho e falha por asserção antes do código | O commit vermelho prova que o teste testa algo | Plano TDD (`docs/testes/plano-tdd.md`); T-20, T-21 |
| Cada pessoa escreve as regras e os testes da própria task | Na prova oral cada um explica o que fez | `CLAUDE.md` (Autoria) |

### 1.2 Arquitetura e dados

| Decisão | Por quê | Fonte |
|---|---|---|
| PostgreSQL com Flyway; o banco é a última linha de defesa (restrições `EXCLUDE`, `UNIQUE`, `CHECK`, `ON DELETE RESTRICT`) | A regra vale mesmo que o código da aplicação erre ou seja contornado | ADR-0004, ADR-0005; `docs/arquitetura.md` seção 3.2 |
| Telas Thymeleaf no mesmo projeto da API | Um só deploy e um só pipeline | ADR-0006 |
| `Clock` injetado em toda regra que depende de horário | Testes com tempo fixo, sem depender do relógio real | ADR-0008 |
| Sessão nas telas, token assinado de 30 minutos na API; autorização negada por padrão; senha só em hash BCrypt | Protege contra o limitador "falha de autorização" | ADR-0007 (Proposta); spec T-15 |
| Cada usuário tem exatamente um perfil e nunca é apagado, só desativado | Simplifica a autorização e preserva o histórico | ADR-0009 (Proposta) |

### 1.3 Regras que o time acrescentou à especificação

| Regra | Decisão | Fonte |
|---|---|---|
| RN-13 | O dono cancela até o início (`CANCELADA`). Depois do início, com check-in (`EM_USO`) o cancelamento conclui a reserva; sem check-in vira `NAO_COMPARECEU` | PRD RN-13; spec T-21 |
| RN-14 | Prazo de aprovação e de alteração: 24 h antes do início; 1 h antes se o pedido tem menos de 24 h de antecedência; limites inclusivos | PRD RN-14; specs T-20 e T-21 |
| RN-15 | Pedido com no mínimo 1 h de antecedência (no cadastro e na alteração) | PRD RN-15; spec T-20 |
| RN-16 | Nome da sala no padrão `sala-NN`, minúsculas, 2 ou 3 dígitos | PRD RN-16; spec T-16 |
| RN-17 | Nome do professor `primeironome-ultimonome-matricula`, matrícula de 5 dígitos | PRD RN-17; spec T-17 |
| Intervalo | Períodos `[início, fim)`: uma reserva que termina quando outra começa não é sobreposição | Spec T-20 (S-01) |
| Aprovação | Só o Responsável aprova ou rejeita, em qualquer reserva | Spec T-21 (RN-07) |
| Check-in | O dono faz o check-in, só dentro de `[início, fim)` | Spec T-21 (T-59) |
| Sem tolerância de atraso | A reserva aprovada ocupa o horário inteiro até o check-in ou o cancelamento | Spec T-21 |
| Mudanças por tempo | Um job a cada minuto marca `NAO_COMPARECEU`, conclui e cancela por prazo vencido | Spec T-21 (T-60) |

### 1.4 Segurança e erros

| Decisão | Por quê | Fonte |
|---|---|---|
| O login falha sempre com a mesma mensagem ("E-mail ou senha inválidos") | Não revela quais e-mails existem | Spec T-15 (P-03) |
| O token não é revogável, mas usuário e perfil são conferidos no banco a cada chamada | Desativar ou trocar o perfil vale na hora | Spec T-15 (CA-15) |
| O primeiro Administrador vem de variáveis de ambiente; a migração não grava senha | Nenhum segredo no repositório | Spec T-15 (CA-16, CA-13) |
| Todo erro da API é `ProblemDetail` (RFC 9457) com mensagem em português; erro inesperado vira 500 neutro | RNF-08: nenhum stack trace, SQL, nome de classe ou caminho no corpo | Spec T-22 |
| Violação de `UNIQUE`, `EXCLUDE` ou chave estrangeira vira 409 | Quem perde uma corrida ou exclui um recurso em uso não recebe 500 | Spec T-22 |

## 2. O que ficou de fora e por quê

| Item | Por quê | Fonte |
|---|---|---|
| Janela de funcionamento, último horário e antecedência máxima (RN-10 a RN-12) | Propostas do time sem validação; precisam de spec própria | `docs/testes/plano-tdd.md`; spec T-20 |
| Recuperação de senha, bloqueio por tentativas, dois fatores, SSO | Não estão nos requisitos | Spec T-15 |
| Reenvio de notificação perdida | Fora de escopo (FE-05) | `docs/arquitetura.md` (R-07) |
| Pesquisa com filtros, retirada, relatório, OpenAPI, telas completas | `A preencher` (depende do que o time conseguir entregar até 12/11) | `docs/planejamento/backlog.md` |
| Lista exata das tasks do backlog cortadas | `A preencher` pela T-57 | — |

## 3. Riscos aceitos

| Risco | Por que aceitamos | Fonte |
|---|---|---|
| Notificação perdida não é reenviada | Reenvio está fora de escopo | R-07 em `docs/arquitetura.md` |
| Nomes parecidos (por exemplo, "Projetor Epson" e "Projetor Epsom") valem como materiais diferentes | O sistema compara só o texto limpo; o Administrador corrige pela alteração | Spec T-18 (riscos) |
| O token não pode ser revogado antes de expirar | Compensado pela conferência no banco a cada chamada | Spec T-15 |
| O job periódico não tem janela nem volume medidos | Depende do teste de carga | R-06 e PS-03 em `docs/arquitetura.md` |
| Metas de desempenho (RNF-09, RNF-10, RNF-11) são candidatas | Valores de exemplo ainda não aprovados | `docs/prd.md` |
| Limite de 72 bytes da senha (BCrypt) | O sistema recusa acima disso, em vez de truncar | Spec T-15 (P-02) |

## 4. Resultados que fecham este documento (`A preencher`)

| Item | Valor | Evidência | Task |
|---|---|---|---|
| Cobertura de linhas e de branches | A preencher | [Evidências](evidencias/README.md) | T-27 |
| Vulnerabilidades críticas | A preencher | [Evidências](evidencias/README.md) | T-29 |
| Resultado do teste de concorrência | A preencher | [Evidências](evidencias/README.md) | T-37 |
| p95 do teste de carga | A preencher | [Evidências](evidencias/README.md) | T-38 |
| Requisitos críticos na RTM | A preencher | [RTM](RTM.md) | T-50 |
