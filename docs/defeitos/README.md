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

## Severidade e prioridade

**Severidade** mede o dano: confidencialidade, integridade, disponibilidade, fluxo bloqueado e quantas pessoas são afetadas. É uma análise técnica.

**Prioridade** é a decisão de quando resolver: urgência, compromisso com a entrega, exposição, existência de um contorno e custo de esperar. É uma decisão do time.

| Severidade | Quando usar |
|---|---|
| Crítica | Dupla reserva possível, falha de autorização, perda de dados ou aplicação que não executa |
| Alta | Um fluxo principal bloqueado, sem contorno |
| Média | Um fluxo prejudicado, mas com contorno |
| Baixa | Efeito pequeno: texto, alinhamento, documentação |

| Prioridade | Significado |
|---|---|
| P1 | Corrigir já; bloqueia o trabalho ou a nota |
| P2 | Corrigir nesta semana |
| P3 | Corrigir quando houver espaço |

A combinação não é automática. Exemplos do projeto:

| | Prioridade baixa | Prioridade alta |
|---|---|---|
| **Severidade baixa** | Desalinhamento em uma tela administrativa rara | Referência errada na documentação, na véspera da entrega |
| **Severidade alta** | Falha grave em uma função que ainda não foi liberada | Possibilidade de dupla reserva |
