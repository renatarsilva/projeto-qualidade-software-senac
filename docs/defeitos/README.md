# Registro e gestão de defeitos

> Task **T-14** · Aula 05 (Gestão de erros e bugs)

Um defeito é um comportamento do sistema, ou de um documento, diferente do que o requisito pede. Todo defeito vira uma **issue no GitHub** com o rótulo `bug`, e o histórico fica no repositório.

## Ciclo de vida

Novo → Triado → Em correção → Em verificação → Fechado

| Estado | O que significa | Quem avança |
|---|---|---|
| Novo | Relatado, ainda sem análise | Quem encontrou |
| Triado | Reproduzido, com severidade e prioridade definidas | Quem triou |
| Em correção | Alguém está corrigindo (branch `fix/...`) | Quem corrige |
| Em verificação | Corrigido; falta conferir | Quem não corrigiu |
| Fechado | Verificado, com teste de regressão ou evidência | Quem verificou |

Saídas fora do caminho principal: **Duplicado**, **Não reproduzível**, **Não será corrigido** (com motivo) e **Reaberto** (o defeito voltou).
