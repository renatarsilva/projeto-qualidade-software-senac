# ADR-0002: Adotar Spec-Driven Development com revisão por pares

- **Status:** Aceita
- **Data:** 2026-10-15
- **Autor(a):** @renatarsilva
- **Task:** T-02

## Contexto

- A avaliação valoriza rastreabilidade (requisito → risco → teste → evidência), revisão por pares, TDD/BDD com evidência e autoria.
- O time usa IA (Claude Code) no desenvolvimento. Sem um processo, a IA tende a gerar código antes de a regra estar clara, e quem não escreveu o código não consegue defendê-lo na prova oral.
- O prazo é curto (quatro sprints de uma semana), então o processo precisa ser proporcional ao risco de cada task.

## Decisão

Toda task que muda o comportamento do sistema começa por uma **spec** (`docs/specs/`), que passa por **revisão por pares** (revisor em rodízio, nunca o autor) e pela **spec validation** automatizada (`scripts/validar-spec.sh`). Tasks de **regra crítica** passam também pelo **grill-with-docs**. Só depois disso começa o código, guiado por TDD. O fluxo completo está em [`docs/processo/ai-workflow.md`](../processo/ai-workflow.md).

Tasks de documentação e infraestrutura não têm spec e vão direto para o PR.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Fluxo completo para todas as tasks | Custo alto demais para tasks simples dentro do prazo. |
| Código primeiro, documentação depois | Gera documentação que não corresponde ao código e testes escritos para "passar". |
| Revisão apenas no PR | O defeito de requisito só aparece depois do código pronto, quando corrigir custa mais (shift-left). |

## Consequências

- **Positivas:** os critérios de aceite viram testes e linhas da RTM; a revisão por pares fica registrada no histórico; o grill funciona como ensaio da prova oral; defeitos de requisito são encontrados antes do código.
- **Negativas e riscos:** a revisão pode virar gargalo. Mitigação: prazo de 24 horas para o revisor, com a terceira pessoa assumindo depois disso.
- **Como vamos verificar:** todo PR de feature aponta uma spec `Liberada`; o `validar-spec.sh` poderá rodar no CI sobre `docs/specs/`.
