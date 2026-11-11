# Decisão de qualidade

Registra as decisões de qualidade que o time tomou, o que ficou de fora e os riscos que aceitou. Cada decisão aponta a fonte (ADR, spec ou PRD). 

> **Estado em 2026-11-11 (entrega):** fechado pela T-57. As decisões vêm das specs e dos ADRs; os resultados medidos estão na seção 4. Os ADRs 0007, 0009 e 0010 seguem com status "Proposta" (a aceitação formal é do time).

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
| A proteção contra dupla reserva é o `EXCLUDE` do banco, com os vínculos gravados em ordem de id e o deadlock (`40P01`) traduzido para 409 | A garantia vale mesmo que o código erre; o teste de concorrência achou o deadlock (DEF-02) | ADR-0010; [T-37](evidencias/T-37-verde.md) |
| Manutenção e reserva do mesmo recurso se serializam pelo bloqueio da linha do recurso | Nenhuma reserva é gravada depois da manutenção confirmada | Spec T-35 |
| A auditoria só recebe inserções, garantido por gatilho do banco, e é gravada na mesma transação da mudança | Imutável mesmo para quem acessa o banco; sem registro órfão | Spec T-40 |
| O conflito evitado é registrado em transação própria | O registro da recusa não some com o rollback da reserva | Spec T-41 |
| A notificação é enviada depois do commit e a falha dela não desfaz a mudança | RNF-10: 0 reservas desfeitas | Spec T-31 |

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
| **Telas responsivas** (Thymeleaf e Bootstrap): RF-13 parcial e RNF-11 não atendido; teste ponta a ponta (T-44 a T-47) | Priorizamos a API, as regras críticas e a prova delas (concorrência, autorização, auditoria, carga) dentro do prazo; as telas dependiam de tudo isso pronto. A parte de erros compreensíveis do RF-13 está entregue | [status-requisitos.md](status-requisitos.md) |
| Telas de login com sessão e CSRF (T-15, parte web) | Mesmo motivo; a autenticação da API está completa | Spec T-15 |
| Job periódico das mudanças por tempo (T-60): `NAO_COMPARECEU`, conclusão pelo fim e cancelamento por prazo de aprovação | A regra aceita essas transições (testada com o `Clock`), mas nada as dispara sozinho | Spec T-21 |
| Feature Cucumber e BDD (T-33, T-36) | A especificação aceita TDD **ou** BDD; o TDD tem evidência em todas as funcionalidades | [evidencia-tdd.md](testes/evidencia-tdd.md) |
| Avaliação arquitetural em ciclo (T-26) | Não escrita; a arquitetura tem ATAM e ADRs | `docs/atam/` |
| Tasks cortadas | T-26, T-33, T-36, T-44, T-45, T-46, T-47 e T-60 do backlog | `docs/planejamento/backlog.md` |

## 3. Riscos aceitos

| Risco | Por que aceitamos | Fonte |
|---|---|---|
| Notificação perdida não é reenviada | Reenvio está fora de escopo | R-07 em `docs/arquitetura.md` |
| Nomes parecidos (por exemplo, "Projetor Epson" e "Projetor Epsom") valem como materiais diferentes | O sistema compara só o texto limpo; o Administrador corrige pela alteração | Spec T-18 (riscos) |
| O token não pode ser revogado antes de expirar | Compensado pela conferência no banco a cada chamada | Spec T-15 |
| O job periódico não existe | Ficou fora; as transições por tempo só acontecem por chamada | Seção 2 |
| O teste de carga foi local, com aplicação, banco e JMeter na mesma máquina | Os tempos são pessimistas, mas não provam capacidade de produção | [resultado-01.md](performance/resultado-01.md) |
| A meta de 300 ms (RNF-09) veio do exemplo da Aula 10 | O grupo a validou na T-38 (DP-07) | [rnf.md](performance/rnf.md) |
| A recusa do banco na disputa conta só o primeiro recurso do pedido como conflito evitado | O relatório conta tentativas, e não recursos | Spec T-41 |
| Limite de 72 bytes da senha (BCrypt) | O sistema recusa acima disso, em vez de truncar | Spec T-15 (P-02) |

## 4. Resultados que fecham este documento

| Item | Valor | Evidência | Task |
|---|---|---|---|
| Cobertura de linhas e de branches | **97,0 %** e **86,0 %** (metas 80 e 70) | JaCoCo no `./mvnw verify` de 2026-11-11; [Evidências](evidencias/README.md) | T-27 |
| Vulnerabilidades críticas | **0** (e 0 bugs); Quality Gate aprovado | SonarCloud da `main`; DEF-03 corrigido | T-29 |
| Resultado do teste de concorrência | 1 aceita e 19 recusadas (409); 0 duplicidades no banco | [T-37-verde.md](evidencias/T-37-verde.md) | T-37 |
| p95 do teste de carga | **2 a 7 ms** (meta 300 ms), 0 erro real | [resultado-01.md](performance/resultado-01.md) | T-38 |
| Requisitos críticos na RTM | **100 %** (RN-01 a RN-09 com evidência) | [RTM](RTM.md) | T-50 |
