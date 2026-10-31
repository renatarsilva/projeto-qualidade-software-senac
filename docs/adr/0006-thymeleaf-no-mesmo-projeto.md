# ADR-0006: Servir as telas com Thymeleaf no mesmo projeto Spring Boot

- **Status:** Proposta
- **Data:** 2026-10-31
- **Autor(a):** @renatarsilva
- **Task:** T-13

## Contexto

- O RF-13 pede interface responsiva com mensagens de erro compreensíveis, e o RNF-11 propõe telas utilizáveis a partir de 375 px (candidata). O RF-14 pede documentação da API ou dos fluxos públicos.
- A stack obrigatória é Java 21 e Spring Boot 3.x. O time tem três pessoas, e cada uma defende o próprio código na prova oral.
- O esqueleto já traz `spring-boot-starter-thymeleaf` e `thymeleaf-extras-springsecurity6` no `pom.xml`, e o contêiner de aplicação do C4 (seção 2.2 de [`arquitetura.md`](../arquitetura.md)) já mostra telas e API juntas.
- A [ADR-0004](0004-adotar-postgresql.md) e a [ADR-0005](0005-adotar-flyway.md) já definem o banco e o esquema.

## Decisão

As telas são páginas HTML renderizadas no servidor com **Thymeleaf e Bootstrap**, dentro do mesmo projeto Spring Boot que expõe a API REST. Não há um front-end separado.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Front-end separado (SPA em React ou Angular) | Seria um segundo projeto, com outro pipeline, outra suíte de testes e outra forma de autenticar (CORS, token nas duas pontas). Para três pessoas e um prazo curto, é mais superfície sem exigência da especificação. |
| Só a API, sem telas | O RF-13 exige interface responsiva. |
| Aplicativo móvel nativo | Está fora de escopo (FE-06). |

## Consequências

- **Positivas:** um só projeto, um só build e um só pipeline; as telas reaproveitam a Segurança e o tratamento de erros da aplicação; menos tecnologias para o time explicar na prova oral.
- **Negativas e riscos:** as telas e a API convivem no mesmo processo, então a autenticação precisa servir os dois caminhos (sessão nas telas, token na API, definido na ADR-0007) e as telas exigem proteção contra CSRF; a interatividade fica limitada ao que a renderização no servidor permite.
- **Como vamos verificar:** testes de controlador renderizam as telas e as mensagens de erro, e a interface é conferida a 375 px (T-44).
- **Condição de revisão:** rever se alguma tela exigir interatividade que a renderização no servidor não atende, ou se um cliente externo passar a consumir a API como produto principal.
