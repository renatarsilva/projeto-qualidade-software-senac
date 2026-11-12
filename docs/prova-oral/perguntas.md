# Banco de perguntas da prova oral

Perguntas prováveis da semana 47, ligadas às decisões que o time já tomou. Na prova, o professor pergunta a **qualquer integrante, inclusive sobre partes que ele não escreveu**, e pede a **fonte de qualquer número**. Task T-53 (depende da T-52).

> **Estado em 2026-11-11:** as perguntas 16 a 26 foram acrescentadas com as tasks entregues depois. Só as perguntas e a fonte onde a resposta está. **As respostas não estão escritas de propósito**: cada integrante escreve a sua com as próprias palavras, ensaia em voz alta e confere na fonte (task T-54).

**Roteiro de 8 minutos:** problema e risco, arquitetura, demonstração, testes e CI, RTM e relatório, limitações.

## Perguntas ligadas às decisões já tomadas

| # | Pergunta | Onde está a resposta (fonte) | Quem responde | Resposta ensaiada |
|---|---|---|---|---|
| 1 | Por que a sala tem o nome no padrão `sala-NN` e o professor no padrão `primeironome-ultimonome-matricula`? O que acontece com um nome fora do padrão? | PRD RN-16 e RN-17; specs T-16 e T-17; `docs/arquitetura.md` 3.2 (restrições 10 e 11) | A definir | A escrever |
| 2 | Por que o intervalo da reserva é `[início, fim)`? Uma reserva das 10h às 11h e outra das 11h às 12h conflitam? | Spec T-20 (S-01); PRD RN-02 | A definir | A escrever |
| 3 | Como o sistema garante que duas solicitações simultâneas produzam uma única reserva? O que acontece se a proteção for removida? | PRD RN-03 e RNF-05; `docs/arquitetura.md` 3.2 (restrição 1, `EXCLUDE`); teste de concorrência (T-37) | A definir | A escrever |
| 4 | Por que só o Responsável aprova reservas? O que o Administrador e o Solicitante recebem se tentarem? | PRD RN-07; spec T-21 (tabela perfil × destino) | A definir | A escrever |
| 5 | O que acontece quando o dono cancela uma reserva antes do início, depois do início com check-in e depois do início sem check-in? | PRD RN-13; spec T-21 | A definir | A escrever |
| 6 | Por que o token vale 30 minutos, e como desativar um usuário vale na hora mesmo com um token ainda válido? | Spec T-15 (P-01 e CA-15); ADR-0007 | A definir | A escrever |
| 7 | Por que o login devolve a mesma mensagem para senha errada, e-mail que não existe e usuário desativado? | Spec T-15 (P-03 e CA-02) | A definir | A escrever |
| 8 | Por que o `Clock` é injetado, e como isso aparece nos testes de prazo e de antecedência? | ADR-0008; specs T-20 e T-21 | A definir | A escrever |
| 9 | Como vocês provam que nenhuma resposta de erro expõe stack trace, SQL ou nome de classe? De onde vem a lista de termos proibidos? | PRD RNF-08; spec T-22 (lista única de termos proibidos) | A definir | A escrever |
| 10 | Mostre o commit vermelho de uma regra: onde está a evidência de TDD? Como o teste falha por asserção e não por erro de compilação? | `docs/testes/evidencia-tdd.md` (T-36); plano TDD; histórico de commits `test(red)` | A definir | A escrever |
| 16 | O que acontece com o banco quando duas solicitações disputam a mesma sala? Por que o PostgreSQL pode dar *deadlock* e como o sistema responde? | [T-37-verde.md](../evidencias/T-37-verde.md); ADR-0010; defeito DEF-02 | A definir | A escrever |
| 17 | Remova a proteção contra dupla reserva e rode o teste: qual teste falha e com que número? | [T-37-vermelho.md](../evidencias/T-37-vermelho.md); [mutacao-manual.md](../testes/mutacao-manual.md) (M4) | A definir | A escrever |
| 18 | Uma manutenção é criada em cima de uma reserva já aprovada. O que acontece com a reserva? | Spec T-35 (S-05); `ManutencaoDeRecursosApiTest` (TC-11) | A definir | A escrever |
| 19 | Por que a auditoria não pode ser alterada nem pela aplicação? Onde está a garantia? | Spec T-40; migração V11 (gatilho); `ImutabilidadeDaAuditoriaTest` | A definir | A escrever |
| 20 | Por que o registro do conflito evitado é gravado em transação própria? | Spec T-41 (CA-07); `ConflitosEvitadosNoBanco` | A definir | A escrever |
| 21 | A notificação falha (timeout ou 500). A aprovação é desfeita? Como o teste prova? | Spec T-31; `NotificacaoDasMudancasApiTest` (CT-03); `NotificadorHttpWireMockTest` | A definir | A escrever |
| 22 | Como a matriz de autorização garante que nenhuma rota fique aberta sem querer? | [matriz](../seguranca/matriz-autorizacao.md); `CompletudeDaMatrizTest`; [T-49](../evidencias/T-49-matriz-de-autorizacao.md) | A definir | A escrever |
| 23 | De onde vem o p95 do teste de carga e por que o 409 não conta como erro? | [rnf.md](../performance/rnf.md); [resultado-01.md](../performance/resultado-01.md); `perf/analisar.py` | A definir | A escrever |
| 24 | O SonarCloud reprovava o Quality Gate. Quais eram as duas pendências e o que foi feito? | Defeito DEF-03 em `docs/defeitos/registro.md`; ADR-0007 | A definir | A escrever |
| 25 | A devolução de um material é registrada exatamente no término da reserva. É atraso? E 1 segundo depois? | Spec T-42; `RetiradaDeMateriaisApiTest` (TC-08 e TC-09) | A definir | A escrever |
| 26 | O que ficou de fora da entrega e por quê? | [decisão de qualidade](../decisao-qualidade.md), seção 2; [status-requisitos.md](../status-requisitos.md) | Todos | A escrever |
| 27 | As telas têm regra própria de reserva? Como uma reserva feita pela tela respeita o conflito e a antecedência? | Spec T-44, seção 3; `ReservasTelas` chama `ReservaService` | A definir |
| 28 | Como o sistema impede o Solicitante de abrir `/admin/salas` pela barra de endereço? E uma rota que ninguém declarou? | [TelasSegurancaConfiguracao](../../src/main/java/br/senac/recursos/web/TelasSegurancaConfiguracao.java); `AutorizacaoDasTelasTest` (TC-08 e TC-10) | A definir |
| 29 | Por que as telas usam sessão e CSRF e a API usa token? E se o usuário for desativado com a sessão aberta? | ADR-0007; `ContaDaSessaoFilter`; `LoginEMenuDasTelasTest` (TC-32) | A definir |
| 30 | O 403 virava 500 nas telas. Por que o MockMvc não pegou e como vocês acharam? | DEF-04 em `docs/defeitos/registro.md`; `PaginaDeAcessoNegadoTest` | A definir |
| 31 | Como o BDD roda? Os cenários são executados ou só documentados? O que a mutação da fronteira da manutenção derruba? | `bdd-aprovacao-bloqueio.md`; `FeaturesDoCucumberTest` | A definir |
| 32 | O que o teste E2E no CI vê que o MockMvc não vê? | `FluxoPorHttpRealTest`; DEF-04 | A definir |

## Perguntas que o professor costuma fazer (sem fonte ainda)

| # | Pergunta | Onde procurar |
|---|---|---|
| 11 | De onde vem o número de cobertura citado no relatório? | Relatório do JaCoCo e run do CI (T-27) |
| 12 | De onde vem o p95 do teste de carga? | `docs/performance/resultado-01.md` (T-38) |
| 13 | Escolha uma linha do relatório de utilização e refaça o cálculo à mão | Oráculo calculado à mão (T-41) |
| 14 | Mostre um teste de um item marcado como "feito" na RTM | [RTM](../RTM.md) e [evidências](../evidencias/README.md) |
| 15 | O que o Solicitante não pode fazer na reserva de outra pessoa? | `docs/seguranca/matriz-autorizacao.md` (T-48) |

## Como usar

1. Cada integrante escolhe as perguntas das tasks que escreveu e responde por escrito.
2. Cada um treina também as perguntas das tasks dos colegas.
3. O ensaio registra o resultado em `docs/prova-oral/ensaio.md` (T-54).
