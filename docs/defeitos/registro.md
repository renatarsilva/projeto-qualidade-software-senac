# Registro de defeitos

Lista dos defeitos encontrados, priorizados e corrigidos. O processo está em [`README.md`](README.md).

| ID | Título | Severidade | Prioridade | Estado |
|---|---|---|---|---|
| DEF-01 | A arquitetura cita a task de código como evidência de teste | Baixa | P2 | Em verificação |

## DEF-01 · A arquitetura cita a task de código como evidência de teste

- **Encontrado em:** revisão do PR #21 (plano de teste), divergência DV-01
- **Onde:** `docs/arquitetura.md`, seções 5.1 (RF-04 e RF-05) e 6.4
- **Resultado obtido:** o teste de sobreposição aparecia como evidência da T-23 e da T-24, que são as tasks de **código** (fase green)
- **Resultado esperado:** a evidência é a task que **escreve o teste** (fase red): T-20 e T-21
- **Severidade: Baixa.** É um erro de documentação, sem efeito no sistema
- **Prioridade: P2.** A matriz é o começo da rastreabilidade (10% da nota), e a correção é de poucas linhas
- **Correção:** PR #22 (três referências trocadas e a regra escrita no cabeçalho da matriz)
- **Verificação:** buscar `T-23` e `T-24` em `docs/arquitetura.md`; só a frase da regra deve restar (conferido). A revisão do PR #22 confirma

### Causa raiz (5 porquês)

1. Por que apareceu? A matriz e a tabela de táticas citavam a T-23 como task de teste.
2. Por que isso aconteceu? A coluna "evidência" foi preenchida com a task que implementa o comportamento.
3. Por que isso aconteceu? A matriz foi preenchida pelo backlog, sem conferir a fase (red ou green) de cada task.
4. Por que ninguém percebeu antes? A revisão conferiu IDs e links, mas não a fase, e não havia checklist de referências.
5. O que muda no processo? Ver a prevenção abaixo.

### Prevenção

- Regra escrita no cabeçalho da matriz: a evidência é a task da fase red (feito no PR #22).
- Item no checklist de revisão: "a evidência aponta para a task que escreve o teste?".
