# Matriz de autorização

> Task **T-48** · RF-01 · Autoria: Renata (@renatarsilva) · Estado do código: `main` em 2026-11-10 (após as T-15 a T-18, T-20 a T-24).
> É a base do teste parametrizado da **T-49**: cada célula desta matriz vira um caso de teste. Esta matriz **descreve o que o código faz hoje**; onde ele diverge do que o PRD pede, a divergência está na seção 5 e **não foi corrigida aqui**.

## 1. Como ler

- **Perfis:** Anônimo (sem token), Solicitante, Responsável e Administrador. Cada usuário tem exatamente um perfil (ADR-0009), conferido no banco **a cada chamada**: desativar ou rebaixar vale na hora.
- **Células:** o status HTTP esperado. `201`, `200` e `204` são sucesso. `401` é falta de autenticação. `403` é perfil ou posse errados.
- **Negado por padrão (T-15, CA-08):** toda rota `/api/**` sem regra declarada é recusada, inclusive para o Administrador. Cada recurso declara as suas regras em um `*AcessoConfiguracao`.
- **Células marcadas com ★:** dependem da posse da reserva (seção 3).

## 2. Matriz por operação

| ID | Operação | Anônimo | Solicitante | Responsável | Administrador | Regra no código |
|---|---|---|---|---|---|---|
| A-01 | `POST /api/auth/login` | 200 ou 401 (credencial inválida) | 200 ou 401 | 200 ou 401 | 200 ou 401 | Pública; a mensagem de erro é a mesma para e-mail inexistente, senha errada e usuário desativado |
| U-01 | `POST /api/usuarios` | 401 | 403 | 403 | 201 | `UsuarioAcessoConfiguracao` |
| U-02 | `GET /api/usuarios` | 401 | 403 | 403 | 200 | idem |
| U-03 | `PATCH /api/usuarios/{id}/perfil` | 401 | 403 | 403 | 200 | idem; o sistema nunca fica sem administrador (409) |
| U-04 | `PATCH /api/usuarios/{id}/ativo` | 401 | 403 | 403 | 200 | idem; idem |
| S-01 | `GET /api/salas` | 401 | 200 | 200 | 200 | `SalaAcessoConfiguracao` |
| S-02 | `GET /api/salas/{id}` | 401 | 200 | 200 | 200 | idem |
| S-03 | `POST /api/salas` | 401 | 403 | 403 | 201 | idem |
| S-04 | `PUT /api/salas/{id}` | 401 | 403 | 403 | 200 | idem |
| P-01 | `GET /api/professores` | 401 | 200 | 200 | 200 | `ProfessorAcessoConfiguracao` |
| P-02 | `GET /api/professores/{id}` | 401 | 200 | 200 | 200 | idem |
| P-03 | `POST /api/professores` | 401 | 403 | 403 | 201 | idem |
| P-04 | `PUT /api/professores/{id}` | 401 | 403 | 403 | 200 | idem |
| P-05 | `DELETE /api/professores/{id}` | 401 | 403 | 403 | 204 (409 se há vínculo) | idem |
| M-01 | `GET /api/materiais` | 401 | 200 | 200 | 200 | `MaterialAcessoConfiguracao` |
| M-02 | `GET /api/materiais/{id}` | 401 | 200 | 200 | 200 | idem |
| M-03 | `POST /api/materiais` | 401 | 403 | 403 | 201 | idem |
| M-04 | `PUT /api/materiais/{id}` | 401 | 403 | 403 | 200 | idem |
| M-05 | `DELETE /api/materiais/{id}` | 401 | 403 | 403 | 204 (409 se há vínculo) | idem |
| R-01 | `POST /api/reservas` | 401 | 201 | 403 | 403 | `ReservaAcessoConfiguracao` |
| R-02 | `PUT /api/reservas/{id}` | 401 | 200 ★ | 403 | 403 | idem; o dono vem do token |
| R-03 | `POST /api/reservas/{id}/cancelamento` | 401 | 200 ★ | 403 | 403 | idem |
| R-04 | `POST /api/reservas/{id}/checkin` | 401 | 200 ★ | 403 | 403 | idem |
| R-05 | `PATCH /api/reservas/{id}/estado` com `APROVADA` ou `REJEITADA` | 401 | 403 | 200 ou 409 | 403 | só o Responsável aprova e rejeita, em qualquer reserva (RN-07) |
| R-06 | `PATCH /api/reservas/{id}/estado` com `EM_USO`, `CONCLUIDA`, `CANCELADA`, `NAO_COMPARECEU` ou `SOLICITADA` | 401 | 403 | 403 | 403 | destinos do sistema: 403 para **todos** os perfis (T-21, CA-14) |
| R-07 | `DELETE /api/reservas/{id}` | 401 | 409 | 403 | 403 | existe só para recusar: o sistema nunca apaga reservas (RN-08) |

**Rotas que não existem e são negadas por padrão** (esperado, a ser confirmado pelo teste da T-49):

| ID | Rota | Anônimo | Solicitante | Responsável | Administrador | Observação |
|---|---|---|---|---|---|---|
| X-01 | `DELETE /api/salas/{id}` | 401 | 403 | 403 | 403 | A sala não tem exclusão, ao contrário de professor e material (divergência D-02) |
| X-02 | `GET /api/reservas` e `GET /api/reservas/{id}` | 401 | 403 | 403 | 403 | Não há consulta de reservas na API (divergência D-01) |
| X-03 | qualquer outra rota `/api/**` | 401 | 403 | 403 | 403 | Negado por padrão (T-15, CA-08) |

## 3. Regras de posse

- **NC-01:** o Solicitante altera, cancela e faz o check-in **somente das próprias reservas** (R-02, R-03, R-04). Quem não é o dono recebe **403**, e a reserva não muda.
- **O dono é sempre o usuário autenticado:** o corpo do `POST` não traz dono, e um `solicitanteId` enviado é ignorado (T-20, CA-14).
- **Só o Responsável aprova e rejeita** (R-05). O Administrador não aprova, nem em reserva de recurso restrito, nem em reserva de recurso comum.
- **Reserva inexistente:** 404 só para quem tem o perfil exigido. Quem não tem recebe 403 antes (por exemplo, o Solicitante no `PATCH` de um id que não existe recebe 403, e não 404).

## 4. Ordem em que os erros valem

Para as rotas de reserva: **401** (sem autenticação), **403** por perfil, **404** (reserva inexistente), **403** por dono, **400** (entrada inválida) e **409** (regra). O primeiro que se aplica é o que a resposta mostra. Exemplos já testados: Solicitante que envia `"ABC"` no `PATCH` recebe 403, e não 400; quem não é o dono recebe 403 mesmo com o corpo inválido.

## 5. Divergências com o que o PRD pede (não corrigidas)

| ID | Divergência | Impacto | Task que resolve |
|---|---|---|---|
| D-01 | Não existe `GET /api/reservas` nem `GET /api/reservas/{id}`: ninguém consulta reservas pela API. A consulta, o histórico (RF-10) e a pesquisa de disponibilidade (RF-03) dependem disso | Alto: sem a rota, também não há o que proteger por posse na leitura | T-43 (pesquisa) e T-40 (histórico) |
| D-02 | Sala não tem `DELETE`; professor e material têm. A tentativa recebe 403 (negado por padrão), e não 405 | Baixo: decisão da T-16, só precisa estar clara | Decidir se o contrato deve dizer 405 |
| D-03 | O Responsável e o Administrador não criam nem alteram reservas (DP-05, "o Administrador cria reservas?", segue em aberto) | Médio | T-48/T-49 com a decisão da DP-05 |
| D-04 | Manutenção (RN-04), retirada e devolução (RF-09) e relatório (RF-12) ainda não têm rotas, então não estão na matriz | Médio: a matriz precisa ser estendida quando entrarem | T-35, T-42 e T-41 |

## 6. Rotas fora da matriz da API

- **Telas (Thymeleaf):** usam a cadeia de segurança de formulário com sessão e CSRF (ADR-0007) e ainda não existem. Entram na matriz quando as telas forem feitas (T-15, parte das telas; T-44 a T-47).
- **`/error`:** resposta de erro do próprio sistema, pública para renderizar a página de erro; não expõe dados.

## 7. Ligação com os testes

- Já existem testes de autorização por recurso: `PermissaoDasSalasApiTest`, `PermissaoDosProfessoresApiTest`, `PermissaoDosMateriaisApiTest`, `GestaoDeUsuariosApiTest` e `AutorizacaoPorPerfilTest`, e os de reserva (`CicloDeVidaApiTest`, `AutorizacaoDoPatchApiTest`).
- **A T-49** troca esses testes soltos por um único teste parametrizado que percorre **cada célula** desta matriz com tokens reais, mais os casos de posse. Nenhuma célula pode ser apagada para o teste passar. Cada célula vermelha vira defeito em `docs/defeitos/` antes da correção.
