# ADR-0007: Autenticar por sessão nas telas e por token na API

- **Status:** Proposta
- **Data:** 2026-10-31
- **Autor(a):** @renatarsilva
- **Task:** T-13 (implementação na T-15)

## Contexto

- O RF-01 exige autenticação e autorização por perfil. A falha de autorização é **limitador de nota**, e o RNF-06 pede 0 operações executadas por perfil não autorizado.
- O sistema tem dois tipos de cliente no mesmo projeto ([ADR-0006](0006-thymeleaf-no-mesmo-projeto.md)): o navegador, que usa as telas Thymeleaf, e clientes da API REST.
- Hoje o Spring Security sobe sem configuração, com uma senha gerada e sem perfis (F-07 em [`arquitetura.md`](../arquitetura.md)).
- A revisão cruzada (seção 7) levantou três pontos sem definição: proteção contra CSRF nas telas, validade do token da API e o algoritmo do hash da senha.
- SSO ou provedor externo de identidade está fora de escopo (FE-07).

## Decisão

A aplicação tem **duas cadeias de segurança**:

- **Telas:** login por formulário, sessão por cookie e **proteção contra CSRF ligada**.
- **API (`/api/**`):** sem estado, com **token assinado** enviado no cabeçalho `Authorization`, com validade curta (valor a definir na spec da T-15) e **sem proteção CSRF**, porque não usa cookie.

Nas duas:

- Os perfis são `SOLICITANTE`, `RESPONSAVEL` e `ADMINISTRADOR`, gravados na entidade `Usuario` (ADR-0009).
- A autorização é **negada por padrão**: o que não estiver liberado explicitamente para um perfil é recusado, e cada operação protegida declara o perfil exigido.
- A senha é guardada como **hash BCrypt**, e nunca em texto.
- A chave de assinatura do token e qualquer segredo vêm de variável de ambiente, nunca do repositório.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Sessão por cookie também na API | Clientes que não são navegador lidam mal com cookie e CSRF, e a API passaria a depender de estado no servidor. |
| HTTP Basic na API | Envia a senha a cada chamada e não expira. |
| Token também nas telas | O navegador precisaria guardar o token em um lugar acessível a scripts, o que aumenta o risco de roubo. O cookie de sessão pode ser protegido contra isso. |
| Provedor externo de identidade (OAuth2 ou SSO) | Fora de escopo (FE-07) e muito além do prazo do projeto. |

## Consequências

- **Positivas:** cada tipo de cliente usa o mecanismo mais adequado a ele; "negar por padrão" faz um endpoint esquecido ficar fechado, e não aberto; a senha nunca é recuperável.
- **Negativas e riscos:**
  - são duas configurações de segurança para manter e testar;
  - um token sem estado não pode ser revogado antes de expirar, então a validade curta é parte da proteção;
  - se a chave de assinatura vazar, qualquer token pode ser forjado, e por isso ela nunca entra no repositório (o diagnóstico ACH-01 mostrou esse erro no projeto da disciplina);
  - o risco TR-01 se aplica: a regra de perfil precisa valer em todo ponto de entrada.
- **Como vamos verificar:**
  - teste parametrizado da matriz de perfil por operação nas duas entradas, com 100% das células com o resultado esperado (RNF-06, T-49);
  - chamada sem token ou sem login é recusada, e perfil errado recebe recusa de acesso;
  - requisição de tela sem o token CSRF é recusada;
  - o hash gravado nunca é igual à senha enviada;
  - SonarCloud sem vulnerabilidade crítica (RNF-04).
- **Condição de revisão:** rever se for preciso revogar acesso na hora (logout imediato na API) ou se aparecer integração com identidade externa.
