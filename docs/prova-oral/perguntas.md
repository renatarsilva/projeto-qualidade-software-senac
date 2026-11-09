# Banco de perguntas da prova oral

Perguntas prováveis da semana 47, ligadas às decisões que o time já tomou. Na prova, o professor pergunta a **qualquer integrante, inclusive sobre partes que ele não escreveu**, e pede a **fonte de qualquer número**. Task T-53 (depende da T-52).

> **Estado em 2026-11-07:** só as perguntas e a fonte onde a resposta está. **As respostas não estão escritas de propósito**: cada integrante escreve a sua com as próprias palavras, ensaia em voz alta e confere na fonte (task T-54).

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
