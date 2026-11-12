# Demonstração da aplicação

> Roteiro de **8 minutos** para a prova oral e para quem quiser ver o sistema funcionando. Cada passo foi **executado de verdade** em 2026-11-11 e as respostas abaixo são as que a aplicação devolveu. Para repetir, siga o [README](../../README.md).

## Preparação (1 minuto)

```bash
export APP_JWT_CHAVE='<texto aleatório com mais de 32 caracteres>'
export APP_ADMIN_EMAIL='admin@exemplo.com'
export APP_ADMIN_SENHA='<senha do administrador>'
./mvnw spring-boot:run        # sobe a aplicação e o PostgreSQL em contêiner (Docker ligado)
```

Quando a aplicação estiver no ar, abra o **Swagger UI**: http://localhost:8080/swagger-ui.html (todos os endpoints, com os perfis permitidos e os erros). Os comandos abaixo usam `curl`; `B=http://localhost:8080`. Em cada chamada, `$ADM`, `$MAR`, `$RAF` e `$CAR` são os tokens de login do Administrador, de Marina (Solicitante), de Rafael (Solicitante) e de Carlos (Responsável): `POST /api/auth/login` com `{"email":"...","senha":"..."}` devolve `{"token": "..."}` (30 minutos).

O Administrador cadastra os usuários, a sala e o material (`POST /api/usuarios`, `/api/salas`, `/api/materiais`). Resultado da demonstração: usuários criados com `201`, sala com id 1 e material com id 2.

## Roteiro

### 1. Marina reserva a sala e o projetor (RF-04)

`POST /api/reservas` com a sala 1 e o material 2, de 30/11 das 10:00 às 11:00. Resposta:

```json
{"id": 1, "estado": "SOLICITADA", "inicio": "2026-11-30T10:00:00Z", "termino": "2026-11-30T11:00:00Z",
 "prazoAprovacaoEm": "2026-11-29T10:00:00Z"}
```

**O que mostrar:** a reserva nasce `SOLICITADA` e o prazo de aprovação é 24 h antes do início (RN-14).

### 2. Rafael pede a mesma sala no mesmo horário: conflito (RN-02, RF-05)

`POST /api/reservas` de 10:30 às 11:30. Resposta: **409** `A sala já está reservada neste horário`. A mensagem **não diz quem reservou**.

**O que mostrar:** a recusa e a ausência de dado de outro usuário. Se o professor pedir, o teste que prova isso é `CriacaoDeReservaApiTest` (TC-44).

### 3. Só o Responsável aprova (RN-07, autorização)

- Rafael (Solicitante) tenta `PATCH /api/reservas/1/estado` com `APROVADA`: **403**.
- Carlos (Responsável) faz o mesmo: **200**, estado `APROVADA`.

### 4. A pesquisa de disponibilidade (RF-03)

`GET /api/recursos?tipo=SALA&disponivelDe=2026-11-30T10:30:00Z&disponivelAte=2026-11-30T11:30:00Z`: resposta `[]`. A sala ocupada **não aparece**.

### 5. Retirada do material (RF-09)

Carlos registra `POST /api/reservas/1/retiradas` com `{"materialId": 2}`: **201**, `atrasada: false`. O prazo de devolução é o término da reserva.

### 6. Cancelamento e auditoria (RN-09, RF-10)

Marina cancela (`POST /api/reservas/1/cancelamento`): estado `CANCELADA`. Carlos consulta `GET /api/reservas/1/auditoria`:

```
None -> SOLICITADA   usuário 2
SOLICITADA -> APROVADA   usuário 4
APROVADA -> CANCELADA    usuário 2
```

**O que mostrar:** é o registro de auditoria de uma reserva cancelada, que o professor costuma pedir. Cada linha traz o estado anterior, o novo e quem fez.

### 7. O Solicitante não vê a auditoria; o relatório (RF-12)

- Marina em `GET /api/reservas/1/auditoria`: **403**.
- Carlos em `GET /api/relatorios/utilizacao?inicio=2026-11-01T00:00:00Z&fim=2026-12-31T00:00:00Z`:

```json
{"inicio":"2026-11-01T00:00:00Z","fim":"2026-12-31T00:00:00Z","recursos":[],"professores":[],"conflitosEvitados":1}
```

**O que mostrar:** a reserva cancelada **não conta** nas horas (`recursos` vazio) e o conflito do passo 2 **foi registrado** (`conflitosEvitados: 1`).

### 8. Segurança e documentação

- `GET /swagger-ui/index.html`: **200** sem token (documentação pública).
- `GET /api/salas` sem token: **401**.

## Provas que sustentam cada passo (para a pergunta "mostre o teste")

| Passo | Teste |
|---|---|
| 1 e 2 | `CriacaoDeReservaApiTest`, `SobreposicaoNoMesmoRecursoTest` |
| 3 | `AutorizacaoDoPatchApiTest`, `MatrizDeAutorizacaoApiTest` |
| 4 | `PesquisaDeRecursosApiTest` |
| 5 | `RetiradaDeMateriaisApiTest` |
| 6 | `AuditoriaApiTest` (TC-18) |
| 7 | `RelatorioDeUtilizacaoApiTest`, `AuditoriaApiTest` (TC-16) |
| 8 | `DocumentacaoDaApiTest`, `MatrizDeAutorizacaoApiTest` (TC-01) |

Todos rodam com `./mvnw verify` ([relatório de testes](../relatorio-de-testes.md)).

## Se sobrar tempo: a dupla reserva sob concorrência

Mostre `docs/evidencias/T-37-vermelho.md` e `T-37-verde.md`: **20 solicitantes simultâneos** geram 1 reserva e 19 recusas (409); sem o `EXCLUDE` do banco, 14 das 20 solicitações virariam reserva. E o teste de carga ([resultado-01.md](../performance/resultado-01.md)): 50 usuários, p95 de 2 a 7 ms.

## As telas (a demonstração visual)

Com a aplicação no ar, abra http://localhost:8080 e entre com o administrador. O mesmo fluxo da API, agora pelas telas (celular ou desktop):

1. **Administrador:** em Salas, Materiais, Professores e Usuários, cadastre uma sala (`sala-NN`), um material e dois usuários (um Solicitante e um Responsável).
2. **Solicitante:** em Pesquisar, escolha tipo Sala e um intervalo, clique em **Reservar** e salve. A reserva nasce **Solicitada**.
3. **Conflito:** peça a mesma sala em um horário que se sobrepõe. A tela mostra "A sala já está reservada neste horário" e mantém o que foi digitado.
4. **Perfil errado:** como Solicitante, digite `/admin/salas` na barra de endereço: página **Acesso negado** (403).
5. **Responsável:** em Aprovações, aprove a reserva. Abra a reserva: o **Histórico de mudanças** mostra a aprovação.
6. **Relatório:** em Relatório, escolha o período: horas por recurso e **conflitos evitados**.

Para ver o fluxo rodando sozinho no Chrome, em 375 px: `cd e2e && npm install && npm run fluxo` ([e2e/README.md](../../e2e/README.md)).

## Capturas de tela

As capturas das telas (celular em 375 px e desktop) já estão em [`capturas/`](capturas/), geradas pelo roteiro do Chrome, junto com o `resultado.json` (31 verificações). As capturas de qualidade (o painel do SonarCloud com o Quality Gate aprovado, o run verde do CI da tag `v1.0.0-rc3` e o relatório do JaCoCo) estão em [`capturas/qualidade/`](capturas/qualidade/). O índice de todas as capturas, com o que cada uma prova, é o [`capturas/README.md`](capturas/README.md).
