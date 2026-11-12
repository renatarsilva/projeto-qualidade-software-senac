# Evidência da T-44, T-45 e T-46: telas do sistema

> Spec: [T-44-telas-do-sistema.md](../specs/T-44-telas-do-sistema.md) (regra crítica: autorização das telas). Medido em 2026-11-11.

## 1. Red: os testes antes das telas

Os 82 testes das telas foram escritos **antes** de qualquer código de tela e rodados contra a `main` (que não tinha telas):

| Resultado | Quantidade |
|---|---|
| Testes | 82 |
| Falharam **por asserção** (status ou conteúdo diferente do esperado) | **67** |
| Falharam por exceção ou erro de configuração | **0** |
| Já passavam (comportamento que já existia: o anônimo ir ao login, `/error` e Swagger abertos, `POST` sem CSRF recusado) | 15 |

Uma primeira rodada deu 11 erros por `NullPointerException`: eram do próprio teste (uma consulta ao banco devolvendo nulo). Foram corrigidos antes de valer como red, para a falha ser sempre por asserção.

## 2. Green: as telas

| Resultado | Valor |
|---|---|
| Testes das telas | 83, **todos verdes** (82 do ciclo mais o `PaginaDeAcessoNegadoTest`, do DEF-04) |
| Suíte inteira (`./mvnw verify`) | **1043 testes, 0 falhas**; gate de cobertura atendido |
| Cobertura do pacote `web` | 90,5 % das linhas (373 de 412) e 75,2 % dos branches (112 de 149) |
| Cobertura do projeto | 96,0 % das linhas e 85,2 % dos branches |

## 3. Mutação manual da regra crítica

Mutação: em `TelasSegurancaConfiguracao`, a rota de aprovar, rejeitar, retirar e devolver deixou de exigir o perfil Responsável (`hasRole(RESPONSAVEL)` virou `authenticated()`).

- Resultado: **6 testes falharam por asserção**, entre eles `matrizDeAutorizacao` (as 4 rotas do Responsável, "deveria ser recusada" para o Solicitante) e `soOResponsavelAprova` (esperado 403, veio 302).
- A mutação foi desfeita e os testes voltaram ao verde.

## 4. Teste manual com a aplicação no ar

Ao subir a aplicação e usá-la, apareceu o [DEF-04](../defeitos/registro.md): o 403 das telas virava 500. O MockMvc não percebia, porque não encaminha o `sendError` para `/error`. Corrigido, com teste novo, e conferido de novo na aplicação no ar (403 com a página "Acesso negado" no perfil errado, na falta do token CSRF e numa rota sem regra).

## 5. Navegador real

[`e2e/fluxo-no-navegador.js`](../../e2e/README.md) roda no Chrome de verdade, em **375 px** de largura: cadastros do Administrador, pesquisa e reserva do Solicitante, conflito de horário, acesso negado, aprovação do Responsável, histórico e relatório.

- **31 de 31 verificações OK**, entre elas "nenhuma tela tem rolagem horizontal em 375 px" (RNF-11), em 14 páginas.
- Resultado: [`docs/entrega/capturas/resultado.json`](../entrega/capturas/resultado.json). Capturas de tela na mesma pasta (celular e desktop).
- Não roda no CI: o GitHub Actions do projeto não instala navegador.
