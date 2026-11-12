# Teste ponta a ponta no navegador

Roda o fluxo principal **no Chrome de verdade**, em 375 px de largura (celular), contra a aplicação no ar: login, cadastros do Administrador, pesquisa e reserva do Solicitante, conflito de horário, acesso negado, aprovação do Responsável, histórico e relatório. Confere também que nenhuma tela tem rolagem horizontal em 375 px (RNF-11) e tira as capturas de tela de `docs/entrega/capturas/`.

**Não roda no CI** (o GitHub Actions do projeto não instala navegador): é executado à mão antes da entrega, e o resultado fica em [`docs/entrega/capturas/resultado.json`](../docs/entrega/capturas/resultado.json). O CI cobre o mesmo fluxo com o `FluxoPorHttpRealTest` (servidor de verdade, HTTP, sessão e CSRF, sem renderizar a página) e com os testes de `src/test/java/br/senac/recursos/web`.

## Como rodar

1. Suba a aplicação com as variáveis do README, usando `APP_ADMIN_EMAIL=admin@exemplo.com` e `APP_ADMIN_SENHA=senha-do-admin-123`.
2. Tenha o Google Chrome instalado (o script usa `/Applications/Google Chrome.app` no macOS; ajuste `executablePath` em outro sistema).
3. Em `e2e/`: `npm install` e `npm run fluxo`.

O roteiro cria dados novos a cada execução (sala, material, professor e dois usuários com nomes únicos), então pode ser repetido no mesmo banco.
