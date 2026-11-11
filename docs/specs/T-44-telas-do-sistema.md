# Spec T-44: telas do sistema (layout, login e telas dos três perfis)

| Campo | Valor |
|---|---|
| Task | T-44, T-45, T-46 |
| Autor(a) | @renatarsilva |
| Revisor(a) | @renatarsilva (autorrevisão liberada pelo professor em 2026-11-10) |
| Status | Liberada |
| Regra crítica | Sim |
| Requisitos | RF-13, RF-01, RF-02, RF-03, RF-04, RF-07, RF-08, RF-09, RF-10, RF-12 |
| Característica ISO 25010 | Usabilidade e segurança |
| Criada em | 2026-11-11 |

## 1. Contexto e objetivo

Toda a API e as regras estão prontas e provadas, mas o sistema não tem telas, e o RF-13 (interface responsiva e mensagens de erro compreensíveis) está parcial. Esta spec junta as três tasks de interface do backlog (T-44 layout e login, T-45 telas do Solicitante, T-46 telas do Responsável e do Administrador) em uma só, porque o prazo é curto e as três dividem o mesmo layout, a mesma cadeia de segurança e o mesmo tratamento de erro.

Segue o [ADR-0006](../adr/0006-thymeleaf-no-mesmo-projeto.md) (Thymeleaf no mesmo projeto) e o [ADR-0007](../adr/0007-autenticacao-sessao-nas-telas-e-token-na-api.md) (telas com formulário, sessão e CSRF; API com token). As telas **não duplicam regra**: chamam os mesmos serviços da API, então a reserva, o conflito, a manutenção e a auditoria valem igual nos dois caminhos. É regra crítica porque é uma segunda porta de entrada: uma falha de autorização nas telas seria falha de autorização do sistema (limitador de nota).

## 2. Escopo

**Dentro:**
- Layout base com Bootstrap, responsivo (utilizável a partir de 375 px), menu que muda por perfil e mensagens de sucesso e de erro padronizadas.
- Login por formulário, sessão por cookie, CSRF ligado, logout.
- **Solicitante:** pesquisa de recursos com disponibilidade, nova reserva, alteração, cancelamento, check-in, "minhas reservas" e detalhe.
- **Responsável:** fila de aprovação (aprovar e rejeitar), retirada e devolução de materiais, histórico de auditoria da reserva, relatório de utilização.
- **Administrador:** cadastro de salas, materiais, professores e usuários (criar, ativar e desativar, trocar perfil), períodos de manutenção, relatório de utilização e consulta de todas as reservas.
- Autorização por perfil em **toda** rota de tela, negada por padrão.

**Fora (e por quê):**
- Edição e exclusão de sala, material e professor pela tela: a API tem; a tela só lista e cadastra (decisão de escopo; o uso demonstrável não depende disso).
- Teste de navegador no CI: o GitHub Actions do projeto não instala navegador. O fluxo completo é provado no CI por testes com sessão e CSRF reais (CA-15) e, **fora do CI**, pelo roteiro `e2e/fluxo-no-navegador.js` no Chrome de verdade, em 375 px (31 verificações).
- Internacionalização, tema escuro e paginação: o volume é pequeno.

## 3. Regras de negócio

- **Nenhuma regra nova.** As telas aplicam as regras do PRD através dos serviços existentes (RN-01 a RN-15). O que a tela acrescenta é só apresentação e autorização de rota.
- **Autorização por perfil (igual à matriz da API, [matriz-autorizacao.md](../seguranca/matriz-autorizacao.md)):** só o Solicitante cria, altera, cancela e faz check-in; só o Responsável aprova, rejeita, retira e devolve; só o Administrador cadastra e registra manutenção; Responsável e Administrador veem todas as reservas, a auditoria e o relatório; o Solicitante vê só as próprias reservas e não vê a auditoria.
- **Negado por padrão:** rota de tela sem regra declarada é recusada.
- **Fuso:** os horários digitados e mostrados estão no horário de Brasília (`America/Sao_Paulo`); o banco e a API seguem em UTC.
- **Erros em linguagem clara:** a tela mostra a mensagem do serviço (por exemplo, "A sala já está reservada neste horário") e, para campos inválidos, o nome do campo e o motivo; nunca mostra stack trace, SQL nem nome de classe (RF-13, T-22).
- **Sem navegador, sem script obrigatório:** todas as ações são formulários HTML; o JavaScript do Bootstrap só abre o menu no celular.

## 4. Contrato das telas

| Rota | Quem | O que faz |
|---|---|---|
| `GET /login`, `POST /login`, `POST /logout` | Anônimo (login), autenticado (logout) | Entrada e saída |
| `GET /` | Todos | Página inicial com os atalhos do perfil |
| `GET /recursos` | Todos | Pesquisa por tipo, capacidade, localização, competência e disponibilidade |
| `GET /reservas` | Todos | Lista (Solicitante: só as próprias; os outros: todas, com filtro de estado) |
| `GET /reservas/{id}` | Todos | Detalhe, com retiradas e (Responsável, Administrador) auditoria |
| `GET /reservas/nova`, `POST /reservas` | Solicitante | Criação |
| `GET /reservas/{id}/editar`, `POST /reservas/{id}/editar` | Solicitante | Alteração (só o dono) |
| `POST /reservas/{id}/cancelar`, `POST /reservas/{id}/checkin` | Solicitante | Cancelamento e check-in (só o dono) |
| `GET /aprovacoes` | Responsável | Fila das reservas `SOLICITADA` |
| `POST /reservas/{id}/aprovar`, `.../rejeitar` | Responsável | Decisão |
| `POST /reservas/{id}/retiradas`, `POST /reservas/{id}/retiradas/{rid}/devolucao` | Responsável | Retirada e devolução |
| `GET /relatorio` | Responsável, Administrador | Relatório de utilização por período |
| `GET /admin/salas`, `/admin/materiais`, `/admin/professores`, `/admin/usuarios` e os `POST` de criação | Administrador | Cadastros |
| `POST /admin/usuarios/{id}/ativo`, `POST /admin/usuarios/{id}/perfil` | Administrador | Gestão de usuários |
| `GET /admin/manutencoes`, `POST /admin/manutencoes`, `POST /admin/manutencoes/{id}/encerrar` | Administrador | Manutenção |

| Situação | Resposta |
|---|---|
| Anônimo em rota protegida | Redirecionamento para `/login` |
| Perfil errado | 403 com a página amigável |
| `POST` sem token CSRF | 403, e nada muda |
| Erro de regra ou de validação num formulário | A mesma página, com a mensagem e os dados digitados |
| Sucesso numa ação | Redirecionamento para a lista ou o detalhe, com a mensagem de sucesso |

## 5. Critérios de aceite

### CA-01: Layout responsivo
- **Dado** qualquer página das telas
- **Quando** ela é aberta
- **Então** tem o `viewport` para celular, o Bootstrap carregado e um menu que recolhe em telas estreitas.

### CA-02: Login e logout
- **Dado** um usuário ativo com a senha certa
- **Quando** ele envia o formulário de login
- **Então** entra na página inicial com a sessão aberta; com e-mail inexistente, senha errada ou usuário desativado, volta ao login com **a mesma** mensagem; o logout encerra a sessão; quem for desativado perde a sessão na hora e o perfil que vale é o do banco (como na API).

### CA-03: Anônimo vai para o login
- **Dado** um visitante sem sessão
- **Quando** abre qualquer rota de tela protegida
- **Então** é redirecionado para `/login`; `/login`, `/error` e a documentação da API continuam abertos.

### CA-04: Menu por perfil
- **Dado** um usuário autenticado
- **Quando** a página é mostrada
- **Então** o menu traz só os links das telas do seu perfil.

### CA-05: Autorização forçada nas telas
- **Dado** um usuário de um perfil
- **Quando** ele força a URL ou o `POST` de uma tela de outro perfil
- **Então** recebe 403, a página amigável, e nenhuma operação é executada; uma rota sem regra declarada também é recusada.

### CA-06: CSRF
- **Dado** um usuário autenticado
- **Quando** ele envia um `POST` de tela sem o token CSRF
- **Então** recebe 403 e nada muda.

### CA-07: Pesquisa pela tela
- **Dado** salas, materiais e professores cadastrados
- **Quando** o usuário pesquisa com filtros e um intervalo
- **Então** a página lista só o que atende a todos os filtros e está livre no intervalo, com o link para reservar.

### CA-08: Criar reserva pela tela
- **Dado** o Solicitante com recursos livres
- **Quando** ele envia o formulário com início, término e recursos
- **Então** a reserva nasce `SOLICITADA` e aparece em "minhas reservas", com o prazo de aprovação, no horário de Brasília.

### CA-09: Erros de regra em linguagem clara
- **Dado** um horário já ocupado, um término antes do início ou um pedido sem antecedência
- **Quando** o formulário é enviado
- **Então** a página mostra a mensagem da regra, sem detalhe interno, e mantém o que foi digitado.

### CA-10: Gestão da própria reserva
- **Dado** o dono de uma reserva
- **Quando** ele a abre, altera, cancela ou faz o check-in
- **Então** a operação vale; outro Solicitante que abre ou age sobre ela recebe 403.

### CA-11: Aprovação pelo Responsável
- **Dado** reservas `SOLICITADA`
- **Quando** o Responsável abre a fila e aprova ou rejeita
- **Então** o estado muda e a reserva sai da fila; Solicitante e Administrador não acessam a fila nem a ação.

### CA-12: Retirada, devolução e auditoria
- **Dado** uma reserva `APROVADA` com material
- **Quando** o Responsável registra a retirada e a devolução
- **Então** a tela mostra os dois registros e o atraso, se houver; o histórico de auditoria aparece para Responsável e Administrador e **não** para o Solicitante.

### CA-13: Cadastros do Administrador
- **Dado** o Administrador
- **Quando** cadastra sala, material, professor ou usuário, ou ativa, desativa e troca o perfil de um usuário
- **Então** o registro aparece na lista; dado inválido ou repetido mostra a mensagem clara; o sistema nunca fica sem administrador.

### CA-14: Manutenção e relatório
- **Dado** o Administrador (manutenção) e o Responsável ou Administrador (relatório)
- **Quando** registram o período de manutenção ou pedem o relatório
- **Então** a tela mostra as reservas afetadas e a lista de períodos (com encerramento), e o relatório mostra as horas por recurso, a carga dos professores e os conflitos evitados.

### CA-15: Fluxo completo pelas telas
- **Dado** um Solicitante, um Responsável e uma sala
- **Quando** o Solicitante reserva, o Responsável aprova e o Solicitante cancela, tudo por formulários, com sessão e CSRF
- **Então** a reserva termina `CANCELADA` e a auditoria tem três registros.

## 6. Casos de teste

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 | CA-01 | Classe válida | `GET /login` e a página inicial | `viewport`, link do Bootstrap e botão do menu presentes | Integração |
| TC-02 | CA-02 | Classe válida | Login do Solicitante com a senha certa | Redireciona para `/`, sessão autenticada | Integração |
| TC-03 | CA-02 | Classe inválida | E-mail inexistente, senha errada e usuário desativado | Volta a `/login?erro` nos três, mesma mensagem | Integração |
| TC-04 | CA-02 | Classe válida | `POST /logout` | Sessão encerrada, redireciona ao login | Integração |
| TC-05 | CA-03 | Tabela de decisão | Anônimo em `/`, `/reservas`, `/admin/salas`, `/relatorio` | 302 para `/login` | Integração |
| TC-06 | CA-03 | Classe válida | Anônimo em `/login` e `/error` | 200 | Integração |
| TC-07 | CA-04 | Tabela de decisão | Página inicial dos 3 perfis | Cada menu só com os links do perfil (e os dos outros ausentes) | Integração |
| TC-08 | CA-05 | Tabela de decisão | Cada rota × cada perfil (matriz das telas) | O esperado: 200/302 ou 403, 0 falhas | Integração |
| TC-09 | CA-05 | Conflito | Solicitante faz `POST /admin/salas` e `/reservas/{id}/aprovar` | 403 e nada gravado | Integração |
| TC-10 | CA-05 | Classe inválida | `GET /rota-que-nao-existe` autenticado | Recusada (403) | Integração |
| TC-11 | CA-06 | Classe inválida | `POST /reservas` sem token CSRF | 403, 0 reservas | Integração |
| TC-12 | CA-07 | Classe válida | Pesquisa por tipo SALA, capacidade 30 e intervalo livre | Só as salas que atendem | Integração |
| TC-13 | CA-07 | Valor limite | Sala ocupada 10:00-11:00; pesquisa 10:30-11:30 | A sala não aparece | Integração |
| TC-14 | CA-08 | Classe válida | `POST /reservas` com sala e horário livres | Redireciona; a reserva `SOLICITADA` está no banco e na lista | Integração |
| TC-15 | CA-08 | Valor limite | Horário digitado 10:00 em Brasília | Gravado 13:00 UTC, mostrado 10:00 | Integração |
| TC-16 | CA-09 | Conflito | Segundo Solicitante pede a sala ocupada | Mensagem "já está reservada", sem detalhe interno, dados mantidos | Integração |
| TC-17 | CA-09 | Valor limite | Término igual ao início; início sem antecedência | Mensagem clara em cada caso | Integração |
| TC-18 | CA-10 | Classe válida | Dono abre, altera, faz check-in e cancela | 200/redireciona; estados corretos | Integração |
| TC-19 | CA-10 | Conflito | Outro Solicitante abre e cancela a reserva | 403 nos dois, reserva intacta | Integração |
| TC-20 | CA-11 | Classe válida | Responsável abre a fila, aprova uma e rejeita outra | `APROVADA` e `REJEITADA`; fila vazia | Integração |
| TC-21 | CA-11 | Conflito | Solicitante e Administrador em `/aprovacoes` e em `POST aprovar` | 403 | Integração |
| TC-22 | CA-12 | Classe válida | Retirada e devolução de um material | Dois registros na tela; atraso indicado quando passa do término | Integração |
| TC-23 | CA-12 | Tabela de decisão | Detalhe da reserva para os 3 perfis | Auditoria visível para Responsável e Administrador, ausente para o Solicitante | Integração |
| TC-24 | CA-13 | Classe válida | Administrador cadastra sala, material, professor e usuário | Aparecem nas listas | Integração |
| TC-25 | CA-13 | Classe inválida | Nome de sala fora do padrão, nome repetido, senha curta | Mensagem clara; nada gravado | Integração |
| TC-26 | CA-13 | Conflito | Desativar o único administrador | Mensagem "pelo menos um administrador"; segue ativo | Integração |
| TC-27 | CA-13 | Classe válida | Ativar e desativar usuário; trocar perfil | Estado e perfil atualizados | Integração |
| TC-28 | CA-14 | Classe válida | Manutenção sobre reserva aprovada; encerramento | Reserva afetada listada; período some ao encerrar | Integração |
| TC-29 | CA-14 | Classe válida | Relatório do período com uma reserva de 1 h e um conflito | 1,00 hora e 1 conflito evitado | Integração |
| TC-30 | CA-14 | Classe inválida | Relatório com fim antes do início | Mensagem clara | Integração |
| TC-32 | CA-02 | Conflito | Usuário desativado com sessão aberta; sessão com perfil diferente do banco | Perde o acesso na hora; vale o perfil do banco | Integração |
| TC-33 | CA-05 | Classe inválida | O 403 das telas chega ao `/error` (como no servidor de verdade) | Página amigável com status 403, e não 500 | Integração |
| TC-31 | CA-15 | Fluxo | Reservar, aprovar e cancelar por formulários, com sessão e CSRF | `CANCELADA` e 3 registros de auditoria | Integração |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| Rota de tela esquecida e aberta a um perfil errado | Alto (limitador de nota) | Negado por padrão (TC-10) e a matriz completa das telas (TC-08) |
| CSRF desligado ou esquecido num formulário | Alto | TC-11; o Thymeleaf injeta o token em todo `th:action` |
| A tela mostra erro técnico | Médio | Mensagem vem só do serviço ou de uma tradução fixa; TC-16 e TC-17 conferem os termos proibidos |
| Fuso errado no horário digitado | Médio | TC-15 |
| Dois caminhos (tela e API) com regra diferente | Alto | As telas chamam os mesmos serviços; o fluxo do CA-15 passa pelas duas regras reais |
| O teste de navegador não roda no CI | Médio | Roteiro `e2e/` executado à mão, com resultado e capturas em `docs/entrega/capturas/`; no CI o HTML é conferido nos testes (links, formulários, token) |

## 8. Mini-plano de implementação (ciclos TDD)

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-01 a TC-06: layout, login e redirecionamentos | Cadeia de segurança das telas, login, layout e página inicial | Fragmentos do Thymeleaf |
| 2 | TC-07 a TC-11: menu, matriz das telas, CSRF | Regras de acesso por rota, negado por padrão, menu por perfil | Tabela única de rotas e perfis |
| 3 | TC-12 a TC-17: pesquisa e criação de reserva com erros claros | Telas do Solicitante e o tradutor de mensagens | Conversor de fuso |
| 4 | TC-18 e TC-19: gestão da própria reserva | Detalhe, alteração, cancelamento, check-in | Reaproveitar o formulário |
| 5 | TC-20 a TC-23: aprovação, retirada, devolução, auditoria | Telas do Responsável | Extrair a ação com mensagem |
| 6 | TC-24 a TC-30: cadastros, manutenção e relatório | Telas do Administrador | Extrair o formulário comum |
| 7 | TC-31: fluxo completo | Ajustes finais | Atualizar matriz, RTM e demonstração |

## 9. Revisão por pares

> Preenchida pelo revisor, com commit próprio. Nunca pelo autor.

- [x] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [x] As regras citam RNs que existem no PRD.
- [x] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [x] Os casos de teste incluem valores limite e entradas inválidas.
- [x] O contrato das telas cobre os erros (302, 403, mensagens de regra).
- [x] Não há conflito com outras specs nem com os ADRs.

**Comentários:** Revisão feita pela própria autora, como revisora da spec (autorrevisão liberada pelo professor em 2026-11-10). Conferi que cada critério tem caso de teste, que a autorização das telas repete a matriz da API e que nenhuma regra nova nasce nas telas. O escopo foi cortado de forma explícita (sem navegador real, sem edição de cadastros).

**Parecer:** Aprovada · @renatarsilva · 2026-11-11

## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** 2026-11-11
- **Resultado:** APROVADA na spec validation

## 11. Grill-with-docs

| # | Pergunta | Resposta ou decisão | Documento afetado |
|---|---|---|---|
| 1 | A tela pode ter regra diferente da API? | Não. A tela chama os mesmos serviços, e a autorização de rota repete a matriz da API. | Esta spec, seção 3 |
| 2 | O que impede o Solicitante de forçar a URL do Administrador? | A cadeia das telas é negada por padrão e cada rota declara os perfis; o teste cruza todas as rotas com todos os perfis (TC-08). | Matriz das telas |
| 3 | O CSRF das telas está ligado? | Sim (ADR-0007); o formulário leva o token e o `POST` sem ele é recusado (TC-11). | ADR-0007 |
| 4 | Posso confiar no dono vindo do formulário? | Não. O dono é sempre o usuário da sessão (o `name` da autenticação é o id), como na API. | Spec T-20 |
| 5 | O que a página de erro pode mostrar? | Só a mensagem do serviço ou texto fixo; nada de stack trace, SQL ou classe (T-22). | Spec T-22 |
| 6 | O fluxo prova a regra sob concorrência pelas telas? | Não; a concorrência é provada na API (T-37). As telas chamam o mesmo serviço, então herdam a proteção do banco. | Spec T-37 |

**Resultado:** Liberada · 2026-11-11
