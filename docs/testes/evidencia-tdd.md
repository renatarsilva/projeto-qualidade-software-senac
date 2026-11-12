# Evidência de TDD

> A especificação pede "TDD ou BDD em ao menos uma funcionalidade, com evidência". O time usou **TDD estrito em todas as funcionalidades**: cada uma tem um commit `test(red)`, em que os testes **falham por asserção** (nunca por erro de compilação ou exceção), seguido de um commit `feat(green)`, em que passam. O histórico do Git é a prova: `git log --oneline --grep "test(red)"`.

## Como o vermelho é provado

1. O esqueleto de produção devolve um resultado **neutro** (por exemplo `Veredito.PENDENTE` ou HTTP 204), que não é aceite nem recusa. Assim o teste compila e falha pela **asserção**, e não por exceção.
2. Antes do commit vermelho, a suíte é executada e o número de falhas por asserção é conferido (zero erros).
3. O commit verde só implementa o que o vermelho exige.

## Pares red → green (do histórico real)

| Task | Funcionalidade | Commit red | Commit green |
|---|---|---|---|
| T-15 | Política de senha (mínimo de 8 caracteres) | `cca966b` | `c11e486` |
| T-15 | Política de senha (limite de 72 bytes) | `8c7b3f6` | `7cd65ee` |
| T-15 | Hash BCrypt com sal | `74daa7f` | `9f91e5b` |
| T-15 | Login emite token assinado | `a1a7a83` | `55f84ba` |
| T-15 | Verificador do token com `Clock` | `670211b` | `6d0e6ac` |
| T-15 | 401 sem token e 403 por perfil, negado por padrão | `8e9c4f7`, `6f8a2b2` | `49276b5`, `edbff66` |
| T-15 | Gestão de usuários e perfil conferido no banco | `b5cbd0e`, `e9fe008` | `b6a6718`, `fe7d962` |
| T-16 | Cadastro de sala, capacidade e restrições do banco | `fb4f6f2`, `4b05e10`, `70bd3cb` | `06bad35`, `906624f`, `74002f0` |
| T-17 | Cadastro de professor | `60ddae3` | `2a8e9cf` |
| T-18 | Cadastro de material | `da033af` | `13100c4` |
| T-22 | Formato único de erro | `d53e190` | `3014c1c` |
| T-20 e T-23 | Intervalo, sobreposição e antecedência da reserva | `c5d6cfb` | `bef401a` |
| T-21 e T-24 | Ciclo de vida da reserva, cancelamento, check-in e prazos | `628129c` | `e182bbc` |
| T-43 | Pesquisa de recursos e consulta de reservas | `252f25d` | `39dd9d1` |
| T-40 | Auditoria de mudança de estado | `573278f` | `a5af558` |
| T-37 | Concorrência contra dupla reserva e deadlock | `0066265` | `f72550f` |
| T-35 | Bloqueio por manutenção | `25949d3` | `4dd5f30` |
| T-31 | Notificação externa com WireMock | `4c6e659` | `abf8bd8` |
| T-42 | Retirada e devolução de materiais | `1b12138` | `057f85e` |
| T-41 | Relatório de utilização (oráculo à mão) | `8e1476e` | `ba6717e` |
| T-51 | Documentação da API (OpenAPI) | `32a66f9` | `ae84ff3` |

## Exemplo para a prova oral: a regra de manutenção (T-35)

- **Vermelho (`25949d3`):** 26 testes falham por asserção, porque o registro de manutenção ainda devolve 204.
- **Verde (`4dd5f30`):** a migração V13, o registro, a verificação nas reservas e o bloqueio da linha do recurso; a suíte inteira passa (812 testes).
- **Quebrar a regra:** a mutação M3 de [mutacao-manual.md](mutacao-manual.md) remove a verificação e os testes `ManutencaoDeRecursosApiTest` ficam vermelhos.

## O que não houve

- **BDD com Cucumber** (T-33 e T-36): feito **depois** do código, como especificação executável e rede de proteção, e não como guia do código (as regras nasceram por TDD estrito). São 3 features em português e 17 cenários rodando no `verify`; a mutação da fronteira da manutenção derruba o cenário certo. Ver [bdd-aprovacao-bloqueio.md](bdd-aprovacao-bloqueio.md).
- Houve duas exceções de processo, registradas com honestidade: na T-37 o código foi escrito antes do commit vermelho (o vermelho foi provado removendo o `EXCLUDE` do banco descartável, ver [T-37-vermelho.md](../evidencias/T-37-vermelho.md)), e na T-40 o primeiro commit `test(red)` (`269f344`) guardou só a spec; o vermelho de verdade é o `573278f`.
