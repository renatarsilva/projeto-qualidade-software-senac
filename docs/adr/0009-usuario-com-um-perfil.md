# ADR-0009: Representar o usuário com um único perfil

- **Status:** Proposta
- **Data:** 2026-10-31
- **Autor(a):** @renatarsilva
- **Task:** T-13

## Contexto

- O RF-01 exige autorização por perfil, e a especificação define três perfis: Solicitante, Responsável e Administrador. Somente o Responsável aprova recursos restritos (RN-07).
- O modelo de dados (seção 3.1 de [`arquitetura.md`](../arquitetura.md)) tem a entidade `USUARIO` com e-mail único, `senha_hash`, `perfil` e `ativo`. A [ADR-0007](0007-autenticacao-sessao-nas-telas-e-token-na-api.md) usa o perfil para autorizar nas telas e na API.
- O RNF-06 mede a autorização por uma matriz de perfil por operação, com 100% das células com o resultado esperado. Quanto mais perfis possíveis por pessoa, maior a matriz.
- A especificação não diz se uma pessoa pode ter mais de um perfil. O professor e o coordenador são tratados como o mesmo perfil, Solicitante (L10, FE-02). Quem atribui perfis é o Administrador (NC-07, candidata).
- O professor, quando é um recurso reservável, é uma entidade separada de `USUARIO` (seção 3.1).

## Decisão

Cada usuário tem **exatamente um perfil**, guardado em uma coluna `perfil` da tabela `usuario`, restrita aos três valores por `CHECK`. Um usuário nunca é apagado: ele é desativado (`ativo = false`), porque reservas, auditoria e retiradas o referenciam.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Vários perfis por usuário (tabela de papéis ligada a `usuario`) | A especificação não pede, e a matriz de autorização cresceria com as combinações de perfis, o que dificulta provar 100% das células (RNF-06). |
| Uma tabela por perfil (Solicitante, Responsável e Administrador separados) | Triplica as tabelas e o código de autenticação, para dados que são os mesmos. |
| Perfil derivado de atributos (por exemplo, "é Responsável se aprovou algo") | O perfil passaria a ser consequência, e não regra, e a autorização ficaria difícil de testar. |

## Consequências

- **Positivas:** a autorização é simples de explicar e de testar; a matriz de perfil por operação (T-48) tem três linhas; o `CHECK` impede um perfil inválido mesmo se o código errar.
- **Negativas e riscos:** uma pessoa que precise de dois perfis (por exemplo, um professor que também é Responsável) precisa de duas contas, ou o modelo muda; a retenção dos dados pessoais (nome, e-mail) continua sem regra (revisão cruzada, papel Segurança/LGPD).
- **Como vamos verificar:** o teste de integração mostra que o banco recusa um perfil fora dos três valores; o teste da matriz (T-49) cobre cada perfil por operação; a tentativa de apagar um usuário referenciado é recusada pela chave estrangeira.
- **Condição de revisão:** rever se o professor ou a especificação esclarecer que uma pessoa pode acumular perfis (L10), ou se o Administrador precisar de permissões parciais.
