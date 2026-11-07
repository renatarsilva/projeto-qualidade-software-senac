# Spec T-15: autenticação e autorização por perfil

<!--
Como usar: copie este arquivo para docs/specs/<ID-DA-TASK>-<descricao-curta>.md
e substitua todo trecho _preencher_. O fluxo completo está em docs/processo/ai-workflow.md.

Status possíveis, nesta ordem:
Rascunho → Em revisão → Aprovada → Validada → Liberada
(tasks não críticas vão de Validada direto para Liberada, sem grill)
-->

| Campo | Valor |
|---|---|
| Task | T-15 |
| Autor(a) | @renatarsilva |
| Revisor(a) | @CaiosAguiar |
| Status | Liberada |
| Regra crítica | Sim |
| Requisitos | RF-01, RNF-06 |
| Característica ISO 25010 | Segurança |
| Criada em | 2026-11-04 |

## 1. Contexto e objetivo

Hoje o Spring Security sobe sem configuração: gera uma senha aleatória e não conhece perfis (F-07). Esta task cria a entrada segura do sistema. Quem usa o navegador entra por **formulário e sessão**; quem usa a API entra com **token**; e os três perfis (SOLICITANTE, RESPONSAVEL e ADMINISTRADOR) passam a ter acesso só ao que lhes cabe. A falha de autorização é **limitador de nota**, e o RNF-06 exige 0 operações executadas por perfil não autorizado. A decisão técnica está nos ADRs [0007](../adr/0007-autenticacao-sessao-nas-telas-e-token-na-api.md) e [0009](../adr/0009-usuario-com-um-perfil.md).

## 2. Escopo

**Dentro:**
- Login por formulário e sessão para as telas, com proteção CSRF ligada.
- Login por API que devolve um token assinado com validade curta, sem CSRF.
- Usuários em banco (migração Flyway), com senha em hash BCrypt e exatamente um perfil.
- Gestão de usuários pelo Administrador: criar, trocar o perfil e desativar.
- `401` sem autenticação e `403` para perfil sem permissão, sem vazar detalhe interno.
- Autorização negada por padrão.

**Fora (e por quê):**
- A matriz completa perfil × operação (L06, RNF-06): é da T-48 e do teste da T-49. Esta spec cobre só as operações da própria T-15 e deixa o mecanismo pronto.
- O que cada recurso permite a cada perfil (salas, professores, materiais, reservas): cada task de funcionalidade declara o perfil exigido.
- A aprovação de recursos restritos (RN-07): semana 41.
- Recuperação de senha, bloqueio por tentativas e autenticação em dois fatores: não estão nos requisitos.
- SSO e provedores externos (FE-07).
- O formato das respostas de erro: é da T-22, inclusive a fábrica de `ProblemDetail` de `401` e `403` (T-22, CA-10). Esta spec diz **qual** erro ocorre e liga os filtros de segurança a essa fábrica.

## 3. Regras de negócio

- **RF-01:** autenticação e autorização por perfil.
- **RNF-06:** 0 operações executadas por perfil não autorizado (medida derivada do limitador "falha de autorização"; os 100 % da matriz são candidatos, T-48 e T-49).
- **ADR-0009:** cada usuário tem exatamente um perfil (`SOLICITANTE`, `RESPONSAVEL` ou `ADMINISTRADOR`) e nunca é apagado: é desativado.
- **NC-07 (candidata):** o Administrador atribui e altera perfis.
- **ADR-0007:** duas cadeias de segurança; autorização negada por padrão; senha só como hash BCrypt; segredos fora do repositório.

**Valores que o ADR-0007 deixa para esta spec (propostas, em validação pendente):**

- **P-01:** o token vale **30 minutos**, contados do instante de emissão. Com a hora atual vindo do `Clock` injetado (ADR 0008), o token é válido enquanto a hora atual for menor que a de expiração.
- **P-02:** a senha tem de 8 a 72 bytes. O mínimo é uma proposta; o máximo vem do BCrypt, que ignora o que passa de 72 bytes. A contagem é em bytes UTF-8, nada é truncado em silêncio, e a mensagem de erro fala em linguagem simples: "A senha deve ter de 8 a 72 caracteres (acentos e símbolos contam mais)".
- **P-03:** o login falha sempre com a mesma mensagem, "E-mail ou senha inválidos", seja por senha errada, e-mail inexistente ou usuário desativado, para não revelar quais e-mails existem.
- **P-04:** o último Administrador ativo não pode ser desativado nem rebaixado, para o sistema não ficar sem quem gerencie usuários.

## 4. Contrato da API

Os endpoints e os campos são uma proposta desta spec, porque o PRD ainda não os define. Os erros seguem o formato da T-22.

`POST /api/auth/login`: devolve o token (público).

```json
{ "email": "marina@exemplo.com", "senha": "senha-de-teste-1" }
```

Resposta `200`: `{ "token": "...", "tipo": "Bearer", "expiraEm": "2026-11-10T10:30:00Z" }`

**Chamadas protegidas** (`/api/**`, menos o login): cabeçalho `Authorization: Bearer <token>`.

**Gestão de usuários** (só ADMINISTRADOR):

| Operação | Chamada |
|---|---|
| Criar | `POST /api/usuarios` com `nome`, `email`, `senha` e `perfil` |
| Listar | `GET /api/usuarios` |
| Trocar o perfil | `PATCH /api/usuarios/{id}/perfil` com `perfil` |
| Ativar ou desativar | `PATCH /api/usuarios/{id}/ativo` com `ativo` |

Nenhuma resposta traz senha nem hash.

**Telas:** `GET /login` (formulário), `POST /login` (com token CSRF), `POST /logout`. Quem não está autenticado é levado para `/login`.

**Respostas:**

| Situação | Status HTTP | Corpo |
|---|---|---|
| Login correto | 200 | Token, tipo e instante de expiração |
| Credenciais inválidas ou usuário desativado | 401 | `ProblemDetail`: "E-mail ou senha inválidos" |
| Campo ausente ou inválido | 400 | `ProblemDetail` com `erros` (campo e motivo) |
| Sem token, token inválido ou expirado | 401 | `ProblemDetail` neutro, sem detalhe interno |
| Perfil sem permissão | 403 | `ProblemDetail`: "Você não tem permissão para esta operação" |
| E-mail já cadastrado, ou último administrador | 409 | `ProblemDetail` em linguagem de negócio |

## 5. Critérios de aceite

### CA-01: Login correto devolve o token

- **Dado** um usuário ativo com e-mail e senha cadastrados
- **Quando** o cliente envia o login pela API com as credenciais corretas
- **Então** recebe `200` com o token, o tipo `Bearer` e o instante de expiração (30 minutos depois)

### CA-02: Credenciais inválidas são recusadas sem revelar nada

- **Dado** o endpoint de login
- **Quando** o cliente envia uma senha errada ou um e-mail que não existe
- **Então** recebe `401` com a mensagem "E-mail ou senha inválidos"
- **E** as duas respostas são idênticas, de modo que não é possível descobrir quais e-mails existem

### CA-03: Usuário desativado não entra

- **Dado** um usuário com `ativo = false`
- **Quando** ele tenta entrar com as credenciais corretas
- **Então** recebe `401` com a mesma mensagem do CA-02

### CA-04: Login incompleto é recusado

- **Dado** o endpoint de login
- **Quando** o cliente não envia o e-mail ou a senha
- **Então** recebe `400` apontando o campo ausente

### CA-05: Sem token, não entra

- **Dado** uma chamada protegida da API
- **Quando** o cliente não envia o cabeçalho `Authorization`, ou o envia em formato errado
- **Então** recebe `401`, sem detalhe interno na resposta

### CA-06: Token inválido ou expirado é recusado

- **Dado** uma chamada protegida da API
- **Quando** o cliente envia um token adulterado, assinado com outra chave, ou emitido há 30 minutos ou mais
- **Então** recebe `401`
- **E** um token com menos de 30 minutos continua válido (29 min 59 s aceita, 30 min recusa)

### CA-07: Perfil sem permissão recebe 403

- **Dado** uma operação exclusiva do Administrador (a gestão de usuários) e uma rota aberta a qualquer usuário autenticado
- **Quando** cada identidade (anônimo, SOLICITANTE, RESPONSAVEL, ADMINISTRADOR) a chama
- **Então** na gestão de usuários: o anônimo recebe `401`, o SOLICITANTE e o RESPONSAVEL recebem `403`, e o ADMINISTRADOR recebe `200`
- **E** na rota aberta: o anônimo recebe `401` e os três perfis recebem `200`
- **E** a resposta `403` não contém detalhe interno

### CA-08: O que não foi liberado é negado

- **Dado** uma rota nova da API sem nenhuma regra de perfil declarada
- **Quando** qualquer perfil a chama
- **Então** a chamada é recusada (autorização negada por padrão)

### CA-09: O Administrador gerencia usuários

- **Dado** um Administrador autenticado
- **Quando** ele cria um usuário, troca o perfil ou desativa um usuário
- **Então** a operação é aceita e a resposta não traz senha nem hash
- **E** o perfil só pode ser um dos três valores; um e-mail já cadastrado dá `409`; uma senha fora de 8 a 72 bytes dá `400` com a mensagem em linguagem simples do P-02
- **E** o usuário desativado continua no banco e não consegue mais entrar

### CA-10: A senha nunca aparece em texto

- **Dado** o cadastro e o login de usuários
- **Quando** a senha é gravada, respondida ou registrada
- **Então** o banco guarda só o hash BCrypt, que difere da senha e muda a cada cadastro (sal)
- **E** nenhuma resposta nem linha de log contém a senha

### CA-11: As telas usam formulário e sessão

- **Dado** o navegador
- **Quando** a pessoa envia o formulário de login correto, abre uma página protegida sem sessão, ou abre uma página de outro perfil
- **Então** o login cria a sessão e leva para a página inicial; sem sessão vai para `/login`; sem permissão vê uma página de erro `403` amigável, sem termo técnico, com um link de volta para a página inicial; continua logado e não é levado para `/login`

### CA-12: O CSRF protege as telas e não atrapalha a API

- **Dado** uma pessoa autenticada pelo navegador, e um cliente autenticado por token
- **Quando** a pessoa envia um formulário sem o token CSRF, e o cliente chama a API sem token CSRF
- **Então** o formulário é recusado com `403` e a chamada da API é aceita

### CA-13: A chave de assinatura vem do ambiente

- **Dado** a configuração da aplicação
- **Quando** a aplicação sobe sem a variável com a chave de assinatura
- **Então** ela não inicia, com um erro claro
- **E** nenhuma chave nem senha fixa existe no repositório

### CA-14: O sistema nunca fica sem administrador

- **Dado** um único Administrador ativo
- **Quando** alguém tenta desativá-lo ou trocar o perfil dele
- **Então** o pedido é recusado com `409`

### CA-15: Desativação e troca de perfil valem na hora

- **Dado** um usuário com token válido ou sessão aberta
- **Quando** o Administrador o desativa, ou troca o perfil dele
- **Então** a próxima chamada dele é recusada com `401` (desativado) ou avaliada pelo perfil novo (troca de perfil)
- **E** o perfil gravado no token é ignorado: o sistema confere o estado e o perfil atuais no banco a cada chamada

### CA-16: O primeiro administrador vem do ambiente

- **Dado** um banco sem nenhum Administrador
- **Quando** a aplicação sobe
- **Então** ela cria um Administrador ativo com o e-mail e a senha das variáveis de ambiente iniciais, com a senha em hash BCrypt
- **E** sem as variáveis e sem Administrador a aplicação se recusa a subir, com mensagem clara
- **E** a migração Flyway não grava nenhuma senha, e com um Administrador já existente as variáveis são ignoradas

## 6. Casos de teste

Todo critério de aceite precisa de pelo menos um caso. Use classes de equivalência e valor limite.

A autorização é testada com um controller de teste (só em `src/test`) com uma rota só do Administrador, uma rota para qualquer autenticado e uma rota sem regra declarada, para a T-15 não depender das outras funcionalidades. O token usa o `Clock` injetado (ADR 0008). TC-17 a TC-21 formam a tabela de decisão de perfil × rota e rodam como teste parametrizado. TC-01 a TC-05 e TC-11 a TC-14 são testes unitários (regras de senha e de token); os demais são de integração e sobem o contexto da aplicação.

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 | CA-10 | Classe válida | Cadastrar usuário com senha válida | `senha_hash` começa com o prefixo BCrypt e é diferente da senha | Unitário |
| TC-02 | CA-10 | Classe válida | Cadastrar dois usuários com a mesma senha | Os dois hashes são diferentes (sal) | Unitário |
| TC-03 | CA-10 | Valor limite | Senha com 8 caracteres | Aceita | Unitário |
| TC-04 | CA-09 | Valor limite | Senha com 7 caracteres | Recusa com 400, `erros` com `campo = senha` | Unitário |
| TC-05 | CA-09 | Valor limite | Senha com 72 bytes / com 73 bytes | 72 aceita; 73 recusa (limite do BCrypt) | Unitário |
| TC-06 | CA-01 | Classe válida | `POST /api/auth/login` com e-mail e senha corretos | 200 com `token`, `tipo = Bearer` e `expiraEm` | Integração |
| TC-07 | CA-02 | Classe inválida | Login com senha errada | 401, mensagem "E-mail ou senha inválidos" | Integração |
| TC-08 | CA-02 | Classe inválida | Login com e-mail que não existe | 401, a mesma mensagem e o mesmo corpo do caso anterior | Integração |
| TC-09 | CA-03 | Classe inválida | Login de usuário com `ativo = false` | 401, a mesma mensagem | Integração |
| TC-10 | CA-04 | Classe inválida | Login sem e-mail, ou sem senha | 400, `erros` aponta o campo ausente | Integração |
| TC-11 | CA-06 | Valor limite | Token usado 29 min 59 s depois de emitido (`Clock` fixo) | Aceito | Unitário |
| TC-12 | CA-06 | Valor limite | Token usado 30 min depois de emitido | 401 (expirado) | Unitário |
| TC-13 | CA-06 | Classe inválida | Token com uma letra alterada | 401 | Unitário |
| TC-14 | CA-06 | Classe inválida | Token assinado com outra chave | 401 | Unitário |
| TC-15 | CA-05 | Classe inválida | Requisição protegida sem o cabeçalho `Authorization` | 401 `ProblemDetail`, sem detalhe interno | Integração |
| TC-16 | CA-05 | Classe inválida | Cabeçalho com formato errado (sem `Bearer`) | 401 `ProblemDetail` | Integração |
| TC-17 | CA-07 | Tabela de decisão | Anônimo em `/api/usuarios` (só Administrador) | 401 | Integração |
| TC-18 | CA-07 | Tabela de decisão | SOLICITANTE em `/api/usuarios` | 403 `ProblemDetail`, sem detalhe interno | Integração |
| TC-19 | CA-07 | Tabela de decisão | RESPONSAVEL em `/api/usuarios` | 403 `ProblemDetail` | Integração |
| TC-20 | CA-07 | Tabela de decisão | ADMINISTRADOR em `/api/usuarios` | 200 | Integração |
| TC-21 | CA-07 | Tabela de decisão | Cada um dos 4 (anônimo e 3 perfis) em uma rota só de autenticados | Anônimo 401; os 3 perfis 200 | Integração |
| TC-22 | CA-08 | Classe inválida | Rota criada sem nenhuma regra de perfil declarada | Recusada para todos os perfis (negar por padrão) | Integração |
| TC-23 | CA-09 | Classe válida | Administrador cria usuário com perfil `SOLICITANTE` | 201, resposta sem senha nem hash | Integração |
| TC-24 | CA-09 | Classe inválida | Administrador cria usuário com perfil `GERENTE` | 400, `erros` com `campo = perfil` | Integração |
| TC-25 | CA-09 | Classe inválida | Administrador cria usuário com e-mail já cadastrado | 409 em linguagem de negócio | Integração |
| TC-26 | CA-09 | Classe válida | Administrador troca o perfil de um usuário | 200; o próximo login traz o perfil novo | Integração |
| TC-27 | CA-09 | Classe válida | Administrador desativa um usuário | 200; o usuário não consegue mais entrar e continua no banco | Integração |
| TC-28 | CA-14 | Classe inválida | Administrador desativa ou rebaixa o único administrador ativo | 409 "O sistema precisa de pelo menos um administrador" | Integração |
| TC-29 | CA-10 | Classe inválida | Falha de login e criação de usuário, com o log capturado | Nenhuma senha em texto no log nem na resposta | Integração |
| TC-30 | CA-11 | Classe válida | `POST /login` (formulário) com credenciais corretas | Redireciona para a página inicial e cria a sessão | Integração |
| TC-31 | CA-11 | Classe inválida | `GET` de página protegida sem sessão | Redireciona para `/login` | Integração |
| TC-32 | CA-11 | Classe inválida | SOLICITANTE abre página só do Administrador | Página de erro 403 amigável, sem termo técnico, com link para a página inicial; a sessão continua | Integração |
| TC-33 | CA-12 | Classe inválida | `POST` de formulário autenticado sem o token CSRF | 403 | Integração |
| TC-34 | CA-12 | Classe válida | `POST /api/...` com token Bearer e sem token CSRF | Aceito (a API não usa cookie) | Integração |
| TC-35 | CA-13 | Classe inválida | Subir a aplicação sem a variável da chave de assinatura | A aplicação não inicia, com erro claro | Integração |
| TC-36 | CA-13 | Classe válida | Buscar a chave ou senha fixa no repositório | Nenhuma ocorrência no código nem nos arquivos de configuração | Integração |
| TC-37 | CA-15 | Estado proibido | Usuário com token válido é desativado e faz uma chamada | 401 `ProblemDetail` neutro | Integração |
| TC-38 | CA-15 | Classe válida | SOLICITANTE com token válido vira ADMINISTRADOR e chama uma rota só do Administrador | Aceita (vale o perfil atual, não o do token) | Integração |
| TC-39 | CA-15 | Estado proibido | Usuário com sessão aberta é desativado e abre uma página | Levado para `/login` | Integração |
| TC-40 | CA-16 | Classe válida | Subir com banco sem Administrador e com as variáveis iniciais | Administrador criado, ativo, senha em hash | Integração |
| TC-41 | CA-16 | Classe inválida | Subir com banco sem Administrador e sem as variáveis | A aplicação não sobe; mensagem clara | Integração |
| TC-42 | CA-16 | Classe válida | Subir com um Administrador já existente e com as variáveis | Nenhum usuário novo é criado | Integração |
| TC-43 | CA-09 | Valor limite | Senha de 40 caracteres acentuados (mais de 72 bytes) | Recusa `400`; a mensagem fala em caracteres, acentos e símbolos; nada é truncado | Unitário |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| Falha de autorização é limitador de nota; um perfil executa o que não devia | Alto | A tabela de decisão (TC-17 a TC-21), a regra de negar por padrão (TC-22) e o teste de cada perfil |
| A chave de assinatura vai parar no repositório (como no ACH-01) | Alto | TC-35 e TC-36 e CA-13; a chave vem de variável de ambiente |
| Um token não pode ser revogado antes de expirar | Médio | Validade de 30 minutos (P-01) e conferência do usuário e do perfil no banco a cada chamada (CA-15, TC-37 a TC-39); custa uma consulta por requisição |
| Vazar quais e-mails existem pela mensagem de login | Médio | TC-07 a TC-09 exigem a mesma resposta nos três casos (P-03) |
| A senha aparece em log, resposta ou banco | Alto | TC-01 e TC-02 e TC-29; CA-10 |
| CSRF ligado na API, ou desligado nas telas | Alto | TC-33 e TC-34 testam as duas cadeias |
| O ADR-0007 ainda está "Proposta" | Médio | A revisão desta spec deve levar o ADR para "Aceita" antes da spec ser liberada |
| Valores propostos (30 min, 8 a 72 bytes, último administrador) sem validação do professor | Médio | Marcados como P-01 a P-04; os limites têm teste (TC-11, TC-12, TC-03, TC-04 e TC-05) e mudam fácil |
| A matriz completa perfil × operação (T-48) ainda não existe | Médio | Fora desta spec; o mecanismo de perfil e de negar por padrão fica pronto para a T-48 e a T-49 |

## 8. Mini-plano de implementação (ciclos TDD)

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-01 a TC-05: senha e hash | Hash BCrypt e as regras de tamanho da senha | Isolar a política de senha em uma classe |
| 2 | TC-06 a TC-10: login pela API | Autenticação por e-mail e senha e emissão do token | Unificar a mensagem de falha |
| 3 | TC-11 a TC-14: validade do token | Verificação de assinatura e de expiração com o `Clock` | Separar emissão e verificação do token |
| 4 | TC-15 e TC-16: chamada sem token | Resposta `401` no formato da T-22 | Reaproveitar o tratamento de erro da T-22 |
| 5 | TC-17 a TC-21: perfil por rota | Regra de perfil por rota e resposta `403` | Declarar o perfil exigido em um só lugar |
| 6 | TC-22: negar por padrão | Toda rota não declarada é recusada | Revisar as rotas públicas |
| 7 | TC-23 a TC-28: gestão de usuários | Criar, trocar perfil, desativar e proteger o último administrador | Remover duplicação entre as operações |
| 8 | TC-30 a TC-34: telas, sessão e CSRF | Cadeia de segurança das telas e da API | Separar a configuração das duas cadeias |
| 9 | TC-37 a TC-39: desativação na hora | Conferir usuário ativo e perfil atual no banco em cada chamada | Isolar a leitura do usuário atual |
| 10 | TC-40 a TC-42: primeiro administrador | Criar o Administrador inicial a partir do ambiente | Isolar a leitura das variáveis iniciais |
| 9 | TC-29: senha fora do log | Garantir que nenhum log ou resposta traga a senha | Revisar mensagens de log |
| 10 | TC-35 e TC-36: segredo no ambiente | Falhar na inicialização sem a chave; remover segredos fixos | Documentar a variável de ambiente |

## 9. Revisão por pares

- [x] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [x] As regras citam RNs que existem no PRD.
- [x] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [x] Os casos de teste incluem valores limite e entradas inválidas.
- [x] O contrato da API cobre os erros (400, 401, 403, 409).
- [x] Não há conflito com outras specs nem com os ADRs.

**Comentários:** Conferi objetivo, escopo, critérios de aceite, contratos da API e casos de teste.

Os cenários permitem verificar sucesso e falha de autenticação de forma objetiva e não encontrei inconsistências com o PRD, a arquitetura ou os ADRs referenciados.

**Parecer:** Aprovada · @CaiosAguiar · 2026-11-05

## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** 2026-11-06
- **Resultado:** APROVADA na spec validation

## 11. Grill-with-docs

> Obrigatório só para regra crítica. Nas demais, escreva "Não se aplica".

| # | Pergunta | Resposta / decisão | Documento afetado |
|---|---|---|---|
| 1 | Token de usuário desativado ou com perfil trocado | A cada chamada o sistema confere o usuário e o perfil no banco; vale na hora | CA-15, TC-37 a TC-39 |
| 2 | Primeiro administrador | Criado na subida a partir de variáveis de ambiente; a migração não grava senha; sem variáveis e sem Administrador a aplicação não sobe | CA-16, TC-40 a TC-42 |
| 3 | Limite da senha | Contagem em bytes UTF-8, mensagem em linguagem simples, nada truncado em silêncio | P-02, CA-09, TC-43 |
| 4 | Página sem permissão nas telas | `403` amigável com link para a página inicial; continua logado, sem ir para `/login` | CA-11, TC-32 |

**Resultado:** Liberada · 2026-11-06
