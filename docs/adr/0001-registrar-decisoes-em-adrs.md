# ADR-0001: Registrar decisões de arquitetura em ADRs

- **Status:** Aceita
- **Data:** 2026-10-15
- **Autor(a):** @renatarsilva
- **Task:** T-02

## Contexto

O time tem três pessoas, trabalha em paralelo e precisa explicar as próprias decisões na prova oral. A especificação da disciplina pede documentação de arquitetura e rastreabilidade. Decisões tomadas só em conversa se perdem e geram retrabalho.

## Decisão

Toda decisão de arquitetura ou de processo que afete mais de uma task é registrada como um ADR em `docs/adr/`, numerado em sequência (`NNNN-titulo-curto.md`), a partir do [template](_TEMPLATE.md).

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Registrar as decisões só no Jira | Fica fora do repositório e não é versionada junto com o código. |
| Uma seção única de decisões em `docs/arquitetura.md` | Cresce sem estrutura e dificulta saber quando e por que algo mudou. |

## Consequências

- **Positivas:** histórico de decisões versionado; specs e o grill-with-docs podem citar o ADR; material direto para a prova oral.
- **Negativas e riscos:** custo de escrever. Mitigação: ADR curto, de no máximo uma página.
- **Como vamos verificar:** o grill-with-docs confere se as specs respeitam os ADRs aceitos.
- ADRs não são editados depois de aceitos: uma mudança de decisão gera um novo ADR que **substitui** o anterior.
