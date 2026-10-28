## Descrição

<!-- O que este PR resolve ou implementa. Uma ou duas frases. -->

- **Task:** T- <!-- ID do backlog, ou "issue #N" se não houver task. Se resolver uma issue: Closes #N -->
- **Revisor:** @... <!-- pessoa do rodízio; se for substituição, diga quem substitui quem e por quê -->
- **Spec:** `docs/specs/...` <!-- ou "Não se aplica (task sem spec)" -->

## Tipo de alteração

- [ ] Nova funcionalidade (feature)
- [ ] Correção de defeito (bugfix)
- [ ] Refatoração (sem alteração de comportamento)
- [ ] Testes
- [ ] Documentação
- [ ] Configuração, build ou CI

## Especificação técnica (specs)

<!-- Em tasks sem spec, marque apenas "Não se aplica". -->

- [ ] Spec criada e aprovada pelo revisor do rodízio (@...)
- [ ] `spec validation` executado com sucesso (`./scripts/validar-spec.sh`)
- [ ] `grill-with-docs` aprovado (obrigatório para regra crítica)
- [ ] Não se aplica (task sem spec)

## Testes realizados

- [ ] TDD aplicado: commits red e green no histórico, com cenários de sucesso e de erro
- [ ] Todo critério de aceite da spec tem um teste correspondente
- [ ] `./mvnw verify` passando localmente

| Critério | Teste |
|---|---|
| CA-01 | `NomeDaClasseTest#nomeDoMetodo` |

## Checklist

- [ ] O código segue os padrões do projeto (SOLID, Clean Code e as convenções do CONTRIBUTING)
- [ ] A documentação foi atualizada (README, arquitetura, ADR ou RTM, se aplicável)
- [ ] A branch está atualizada com a `main`

## Evidências

<!-- Prints, saída de testes, links do relatório do CI. Opcional. -->
