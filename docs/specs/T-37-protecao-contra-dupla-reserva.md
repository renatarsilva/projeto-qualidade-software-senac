# Spec T-37: proteção contra dupla reserva e teste de concorrência

<!--
Status possíveis, nesta ordem:
Rascunho → Em revisão → Aprovada → Validada → Liberada
(tasks não críticas vão de Validada direto para Liberada, sem grill)
-->

| Campo | Valor |
|---|---|
| Task | T-37 |
| Autor(a) | @renatarsilva |
| Revisor(a) | @Samwel-Merces |
| Status | Liberada |
| Regra crítica | Sim |
| Requisitos | RF-06, RN-03, RNF-05 |
| Característica ISO 25010 | Adequação funcional (correção sob concorrência) |
| Criada em | 2026-11-09 |

## 1. Contexto e objetivo

A dupla reserva (duas reservas ativas para o mesmo recurso e intervalo) é **limitador de nota**. A verificação de sobreposição da aplicação (T-20 e T-23) é sequencial: sob solicitações simultâneas, duas requisições podem passar pela verificação antes de qualquer uma gravar. A garantia final é o `EXCLUDE USING gist` do PostgreSQL (migração V9), que a T-23 já entregou. Esta spec define o **teste de concorrência** que prova essa garantia (RN-03, RNF-05, CT-02) e o ADR que registra a escolha da proteção. O perfil envolvido é o Solicitante, que cria as reservas disputadas.

## 2. Escopo

**Dentro:**
- Teste de integração com PostgreSQL real (Testcontainers) e requisições simultâneas de verdade (`ExecutorService` + `CountDownLatch`) ao `POST /api/reservas`.
- Disputa pela mesma sala e pelo mesmo intervalo (20 solicitantes distintos, 2 solicitações, mesmo usuário), pela agenda do professor e por vários recursos pedidos em **ordem diferente** (risco de deadlock, CA-14).
- Alterações simultâneas (`PUT`) para o mesmo horário livre.
- Prova da proteção **no banco**, sem passar pela aplicação (inserções diretas simultâneas).
- Contagem sempre **por consulta ao banco**, nunca só pelas respostas.
- Estabilidade: o teste de 20 solicitações roda 5 vezes seguidas **dentro da mesma execução** (`@RepeatedTest(5)`, ambiente restaurado a cada vez), com o pool de conexões dimensionado para as 20 threads.
- Evidência de que o teste fica **vermelho sem a proteção** e verde com ela, produzida nesta task e guardada em `docs/evidencias/` (a T-50 pode reaproveitá-la).
- ADR da proteção escolhida; registro em `docs/defeitos/` se o teste achar defeito.

**Fora (e por quê):**
- Teste de carga e desempenho: é da T-38 (JMeter).
- Manutenção e reserva ao mesmo tempo (RN-04-T10, CEN-04): é da T-35.
- Aprovar e cancelar a mesma reserva ao mesmo tempo (RN-07-T9, RN-09-T11): são da T-21 e da T-34.
- Auditoria da reserva recusada: é da T-40.
- Regras de intervalo, antecedência e mensagens de sobreposição sem concorrência: já são da T-20 e da T-23.

## 3. Regras de negócio

- **RN-03:** duas solicitações simultâneas devem produzir uma única reserva aceita (regra oficial). Para N solicitações simultâneas do mesmo recurso e intervalo: exatamente 1 aceita, N-1 recusadas e exatamente 1 reserva ativa no banco.
- **RNF-05:** sob N solicitações simultâneas ao mesmo recurso e intervalo, exatamente 1 reserva aceita e 0 duplicidades ativas (N = 20 e 5 repetições são métricas candidatas, MC-02 e MC-03).
- **Forma da recusa (L03, decisão desta spec):** toda solicitação recusada recebe **409**, nunca 500. A mensagem é a específica do recurso que conflitou ("A sala já está reservada neste horário", "O professor já está ocupado neste horário" ou "O material já está reservado neste horário", quando a verificação da aplicação pega) ou a fixa "Esta operação conflita com outro registro" (quando só o banco pega, T-22). Em nenhum caso a mensagem revela quem reservou.
- **Ordem de gravação dos recursos (decisão desta spec, risco de deadlock):** dois pedidos com os mesmos recursos em ordem invertida podem travar um ao outro no PostgreSQL (SQLState `40P01`). A aplicação grava os vínculos **ordenados por `recurso_id`**, para os dois pedidos disputarem os recursos na mesma ordem. Como rede de segurança, se mesmo assim o banco abortar por deadlock, a resposta é o 409 genérico e nada fica gravado.
- **Recusada não grava:** a solicitação recusada não deixa nenhuma linha em `reserva` nem em `reserva_recurso`, e não ocupa o horário (S-02).
- **Reserva ativa:** reserva em estado que ocupa o horário (`SOLICITADA`, `APROVADA` ou `EM_USO`, S-02).
- **S-01:** períodos `[início, fim)`: solicitações simultâneas encostadas não disputam o recurso.
- **Fuso:** todos os instantes em UTC (ADR-0008). Os casos usam o `Clock` fixo de teste (segunda, 2026-11-09, 08:00 UTC) e a data da disputa é a **quarta, 2026-11-11, das 10:00 às 12:00 UTC** (o CT-02 cita UTC-03:00; a aplicação trabalha em UTC).
- **Proteção (a decidir no ADR):** `EXCLUDE USING gist` sobre `tstzrange` com `WHERE (ocupa)` em `reserva_recurso`, mais a verificação da aplicação e a tradução do erro do banco para 409. Alternativas: lock pessimista e nível de isolamento `SERIALIZABLE`.
- **Limite de espera do teste:** 30 s por disputa, só como proteção para o teste não ficar preso (não é requisito de desempenho).
- **Pool de conexões:** o padrão do Hikari é de 10 conexões e o teste usa 20 threads. O teste sobe o pool para 25 (`spring.datasource.hikari.maximum-pool-size`), para nenhuma thread esperar conexão e a disputa ser pelo recurso, não pelo pool.
- **Fuso do CT-02:** o CT-02 cita 10:00 em UTC-03:00 (13:00 UTC). Esta spec usa 10:00 UTC (ADR-0008); o teste é equivalente. A diferença é anotada na RTM.

## 4. Contrato da API

Nenhum endpoint novo: o teste usa `POST /api/reservas` e `PUT /api/reservas/{id}` (spec T-20, seção 4), chamados por vários usuários SOLICITANTE ao mesmo tempo.

**Requisição (cada solicitante):**

```json
{
  "inicio": "2026-11-11T10:00:00Z",
  "termino": "2026-11-11T12:00:00Z",
  "recursos": [{ "tipo": "SALA", "id": 1 }]
}
```

**Respostas na disputa:**

| Situação | Status HTTP | Corpo |
|---|---|---|
| Solicitação vencedora | 201 | A reserva no estado `SOLICITADA` |
| Solicitação perdedora, conflito pego pela aplicação | 409 | `ProblemDetail` com `detail` do recurso que conflitou: "A sala já está reservada neste horário", "O professor já está ocupado neste horário" ou "O material já está reservado neste horário" |
| Solicitação perdedora, conflito pego só pelo banco (`EXCLUDE`) ou deadlock abortado pelo banco (`40P01`) | 409 | `ProblemDetail` com `detail = "Esta operação conflita com outro registro"` |
| Qualquer outra resposta (500, timeout, 400) | proibido | O teste falha |

## 5. Critérios de aceite

### CA-01: N solicitações simultâneas geram uma única reserva

- **Dado** uma sala livre e 20 usuários SOLICITANTE distintos, todos com agenda livre
- **Quando** os 20 enviam, no mesmo instante, o pedido da mesma sala e do mesmo intervalo
- **Então** exatamente 1 resposta é 201 e exatamente 19 são 409
- **E** a consulta ao banco mostra exatamente 1 reserva ativa e 1 vínculo em `reserva_recurso` para a sala no intervalo

### CA-02: A solicitação recusada não grava nada

- **Dado** uma disputa com vencedora e perdedoras
- **Quando** o teste consulta o banco depois das respostas
- **Então** existe uma só linha em `reserva` e uma só em `reserva_recurso` para o intervalo, e nenhuma reserva ativa a mais

### CA-03: A recusa é sempre 409, sem vazamento

- **Dado** qualquer solicitação perdedora
- **Quando** o teste lê a resposta
- **Então** o status é 409 e o `detail` é uma das mensagens da seção 4 (sala, professor, material ou a genérica do banco)
- **E** o corpo não traz stack trace, nome de classe, SQL nem dados de quem reservou

### CA-04: A menor disputa e as sobreposições parciais

- **Dado** 2 solicitações simultâneas
- **Quando** pedem a mesma sala no mesmo intervalo, ou em intervalos parcialmente sobrepostos (10:00-11:00 e 10:30-11:30)
- **Então** 1 é aceita e 1 é recusada com 409, e há 1 reserva ativa

### CA-05: O que não conflita é aceito, mesmo simultâneo

- **Dado** 2 solicitações simultâneas
- **Quando** pedem a mesma sala em intervalos encostados (10:00-11:00 e 11:00-12:00), ou salas diferentes no mesmo horário
- **Então** as duas são aceitas (201) e há 2 reservas ativas

### CA-06: Duplo clique do mesmo usuário

- **Dado** um usuário SOLICITANTE
- **Quando** ele envia o mesmo pedido 2 vezes ao mesmo tempo
- **Então** 1 reserva é criada e a repetição é recusada com 409

### CA-07: Agenda do professor sob concorrência

- **Dado** um professor livre e 20 solicitantes
- **Quando** cada um pede uma sala diferente, mas todos com o mesmo professor e o mesmo intervalo
- **Então** 1 é aceita e 19 são recusadas com 409
- **E** o banco mostra 1 só vínculo do professor no intervalo, e as salas dos pedidos recusados continuam livres

### CA-08: Vários recursos, tudo ou nada, sob concorrência

- **Dado** o pedido A (sala-102 e projetor) e o pedido B (sala-103 e o mesmo projetor)
- **Quando** os dois chegam ao mesmo tempo
- **Então** 1 é aceito e o outro é recusado com 409
- **E** a sala-103 do pedido recusado não fica gravada

### CA-09: Alterações simultâneas

- **Dado** duas reservas de donos diferentes, em horários livres distintos
- **Quando** os dois donos pedem, ao mesmo tempo, o `PUT` para o mesmo horário livre da mesma sala
- **Então** 1 `PUT` é 200 e o outro é 409, e a reserva recusada mantém o horário original

### CA-10: Depois da disputa, o recusado reserva outro horário

- **Dado** um solicitante recusado numa disputa
- **Quando** ele pede a mesma sala em horário livre
- **Então** o pedido é aceito (201)

### CA-11: O banco protege sozinho

- **Dado** 20 inserções simultâneas e diretas no banco (sem a aplicação), todas ativas, no mesmo recurso e período
- **Quando** elas rodam ao mesmo tempo
- **Então** exatamente 1 inserção é aceita e 19 são recusadas pelo `EXCLUDE`
- **E** as 19 recusas têm `SQLState 23P01` (violação do `EXCLUDE`) ou `40P01` (deadlock entre inserções conflitantes, que o PostgreSQL também usa para recusar), para provar que a falha veio da proteção do banco e não de outro erro (DEF-02)

### CA-12: Estável em 5 execuções seguidas

- **Dado** o teste do CA-01
- **Quando** ele roda 5 vezes seguidas na mesma execução (`@RepeatedTest(5)`), cada uma com o ambiente restaurado (banco limpo e usuários recriados)
- **Então** todas as 5 chegam ao mesmo resultado (1 aceita, 19 recusadas, 1 reserva ativa)
- **E** o pool de conexões do teste tem 25 conexões, para as 20 threads não esperarem conexão

### CA-13: Vermelho sem a proteção

- **Dado** o `EXCLUDE` removido **só no banco descartável do teste** (um PostgreSQL de Testcontainers criado para esta demonstração), por `ALTER TABLE reserva_recurso DROP CONSTRAINT reserva_recurso_sem_sobreposicao`, e **nunca por migration**
- **Quando** o teste do CA-11 (e, se a corrida aparecer, o do CA-01) roda
- **Então** ele **falha por asserção** (mais de 1 reserva ativa), e a saída do teste vermelho vai para `docs/evidencias/T-37-vermelho.md`
- **E** com a proteção restaurada (recriando a restrição com a mesma definição da V9, ou descartando o contêiner), o teste volta a ficar verde, e a saída vai para `docs/evidencias/T-37-verde.md`
- **E** a evidência é produzida nesta task; a mutação manual da T-50 pode reaproveitá-la

### CA-14: Recursos pedidos em ordem diferente não travam um ao outro

- **Dado** o pedido A com [sala-102, projetor] e o pedido B com [projetor, sala-102], os mesmos recursos em ordem invertida
- **Quando** os dois chegam ao mesmo tempo
- **Então** 1 é aceito (201) e o outro é recusado com 409, nunca 500
- **E** o banco mostra os vínculos de uma só reserva, sem gravação parcial
- **E** se o banco abortar uma das transações por deadlock (`40P01`), a resposta é o 409 genérico e nada fica gravado

## 6. Casos de teste

Todos os casos de integração usam o `Clock` fixo de teste, PostgreSQL real e as chamadas simultâneas com `ExecutorService` e `CountDownLatch` (todas as threads esperam o mesmo sinal antes de enviar). A contagem vem de consulta ao banco.

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 (RN-03-T1, CT-02) | CA-01 | Conflito | 20 solicitantes distintos, mesma sala e mesmo intervalo | 1 × 201, 19 × 409; banco: 1 reserva ativa e 1 vínculo | Integração |
| TC-02 (RN-03-T2) | CA-04 | Valor limite | 2 solicitantes, mesma sala e mesmo intervalo | 1 × 201, 1 × 409; 1 reserva ativa | Integração |
| TC-03 (RN-03-T3) | CA-04 | Conflito | 2 solicitantes, 10:00-11:00 e 10:30-11:30 | 1 × 201, 1 × 409 | Integração |
| TC-04 (RN-03-T4) | CA-05 | Valor limite | 2 solicitantes, 10:00-11:00 e 11:00-12:00 na mesma sala | 2 × 201; 2 reservas ativas | Integração |
| TC-05 (RN-03-T5) | CA-05 | Classe válida | 2 solicitantes, salas diferentes, mesmo horário | 2 × 201 | Integração |
| TC-06 (RN-03-T6) | CA-06 | Conflito | O mesmo usuário envia o mesmo pedido 2 vezes ao mesmo tempo | 1 × 201, 1 × 409; 1 reserva | Integração |
| TC-07 (RN-03-T7) | CA-08 | Conflito | A: sala-102 e projetor; B: sala-103 e o mesmo projetor | 1 aceito, 1 × 409; a sala-103 do recusado não fica gravada | Integração |
| TC-08 (RN-03-T8) | CA-12 | Estabilidade | TC-02 repetido 5 vezes seguidas | O mesmo resultado nas 5 | Integração |
| TC-09 (RN-03-T9) | CA-10 | Classe válida | Depois do TC-02, o recusado pede a sala em horário livre | 201 | Integração |
| TC-10 (CT-02, sem caso no plano) | CA-07 | Conflito | 20 solicitantes, 20 salas diferentes, mesmo professor e intervalo | 1 × 201, 19 × 409; 1 vínculo do professor; salas dos recusados livres | Integração |
| TC-11 (CT-02, repetições) | CA-12 | Estabilidade | TC-01 repetido 5 vezes seguidas na mesma execução (`@RepeatedTest(5)`), ambiente restaurado a cada vez, pool de 25 conexões | O mesmo resultado nas 5 | Integração |
| TC-12 (sem caso no plano) | CA-03 | Classe inválida | Todas as respostas perdedoras do TC-01 | Todas 409, com `detail` de uma das mensagens da seção 4; nenhum 500; sem stack, classe, SQL nem nome do outro usuário | Integração |
| TC-13 (sem caso no plano) | CA-09 | Conflito | Dois donos fazem `PUT` ao mesmo tempo para o mesmo horário livre | 1 × 200, 1 × 409; a recusada mantém o horário original | Integração |
| TC-14 (sem caso no plano) | CA-11 | Conflito | 20 inserções diretas simultâneas, ativas, no mesmo recurso e período | 1 aceita; 19 recusadas pelo banco, todas com SQLState `23P01` ou `40P01` | Integração |
| TC-15 (sem caso no plano) | CA-13 | Mutação manual | Remover o `EXCLUDE` só no banco descartável do teste (nunca por migration) e rodar o TC-14; depois restaurar | O TC-14 falha por asserção; evidências em `docs/evidencias/T-37-vermelho.md` e `T-37-verde.md` | Manual |
| TC-16 (CT-02, sem caso no plano) | CA-02 | Conflito | Consulta ao banco depois do TC-01 | 1 linha em `reserva` e 1 em `reserva_recurso` no intervalo; nenhuma linha das 19 recusadas | Integração |
| TC-17 (sem caso no plano) | CA-14 | Conflito | A: [sala-102, projetor] e B: [projetor, sala-102], ao mesmo tempo | 1 × 201 e 1 × 409; nenhum 500; vínculos de uma só reserva | Integração |
| TC-18 (sem caso no plano) | CA-14 | Conflito | A transação aborta por deadlock (`40P01`, simulado) ao gravar o pedido | 409 com "Esta operação conflita com outro registro"; nada gravado | Integração |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| A corrida não aparece e o teste passa sem proteção (falso verde) | Alto | TC-14 insere direto no banco e CA-13 exige a evidência de vermelho sem o `EXCLUDE` |
| A contagem só pelas respostas esconde duplicidade | Alto | Toda conferência usa consulta ao banco (CA-01, CA-02) |
| Teste instável (passa e falha sem mudar o código) | Alto | `CountDownLatch` e 5 repetições seguidas (TC-08 e TC-11); limite de 30 s só como proteção |
| O erro do banco vaza como 500 | Alto | CA-03 e TC-12: toda recusa é 409 e a mensagem é conferida |
| Pool de conexões menor que o número de threads trava o teste | Médio | O teste sobe o pool para 25 conexões e usa o limite de espera de 30 s |
| Deadlock (`40P01`) com recursos pedidos em ordem invertida vira 500 | Alto | Gravação ordenada por `recurso_id`, tradução do `40P01` para 409 e o TC-17 e o TC-18 (CA-14) |
| A remoção do `EXCLUDE` para a evidência vaza para uma migration ou para o banco de verdade | Alto | A remoção é só no banco descartável do teste (CA-13) e o `./mvnw verify` roda sem ela |
| Recusa grava linha parcial em `reserva_recurso` (pedido de vários recursos) | Alto | TC-07 e CA-08 conferem as linhas do banco |
| Ambiente do CI mais lento muda o resultado | Médio | O resultado não depende de tempo; só o limite de espera depende, e ele é folgado |
| Os 20 usuários de teste sujam o banco e quebram outras classes | Médio | Limpeza completa em ordem de dependência, como na T-20 |

## 8. Mini-plano de implementação (ciclos TDD)

A proteção já existe (T-23). Por isso o red desta task é a **remoção controlada do `EXCLUDE`**, e não a falta de código.

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-14 e TC-15: inserções diretas simultâneas; sem o `EXCLUDE` (removido só no banco descartável do teste, nunca por migration) o teste falha por asserção | Com o `EXCLUDE` da V9 restaurado, o TC-14 passa; evidência do vermelho e do verde em `docs/evidencias/` | Extrair o auxiliar de disparo simultâneo (latch) |
| 2 | TC-02 e TC-03: a menor disputa e a sobreposição parcial | Nenhum código novo esperado; se falhar, registrar o defeito e corrigir | Reaproveitar o cenário de disputa |
| 3 | TC-01, TC-16, TC-12 e TC-11: 20 solicitantes, mensagens e 5 repetições | Garantir 409 em toda recusa e tradução do erro do banco (T-22) | Isolar a criação dos 20 usuários |
| 4 | TC-04, TC-05, TC-06 e TC-09: o que não conflita e o duplo clique | Idem | Nomear os cenários |
| 5 | TC-10: agenda do professor | Idem | Tratar os três tipos de recurso do mesmo jeito |
| 6 | TC-07: vários recursos, tudo ou nada | Garantir rollback da gravação parcial | Isolar a gravação em uma única transação |
| 6b | TC-17 e TC-18: ordem invertida e deadlock | Gravar os vínculos ordenados por `recurso_id` e traduzir o `40P01` para 409 | Isolar a ordenação dos recursos em um só lugar |
| 7 | TC-13: alterações simultâneas | Garantir 409 e atomicidade na alteração (`DELETE` e `INSERT` dos vínculos na mesma transação) | Consolidar a tradução dos erros |
| 8 | ADR da proteção (`docs/adr/`) | Registrar a decisão: `EXCLUDE` mais verificação da aplicação, e por que não lock pessimista nem `SERIALIZABLE` | Ligar o ADR à arquitetura 3.2 |

Em cada ciclo, se o teste revelar defeito real, ele é registrado em `docs/defeitos/` antes de corrigir.

## 9. Revisão por pares

> Preenchida pelo revisor, com commit próprio. Nunca pelo autor.

- [x] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [x] As regras citam RNs que existem no PRD.
- [x] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [x] Os casos de teste incluem valores limite e entradas inválidas.
- [x] O contrato da API cobre os erros (400, 401, 403, 409).
- [x] Não há conflito com outras specs nem com os ADRs.

**Comentários:**

Reli a versão atual (commit `1452cd8`) e os pontos da revisão anterior foram tratados: o 409 agora cobre as mensagens de sala, professor e material, além da genérica do banco; os vínculos são gravados ordenados por `recurso_id`, com o 40P01 traduzido para 409 como rede de segurança e o CA-14 (TC-17 e TC-18); o vermelho tem procedimento concreto, só no banco descartável e nunca por migration, com restauração e evidências em `T-37-vermelho.md` e `T-37-verde.md`; o TC-14 confere o SQLState 23P01; as 5 execuções da MC-03 ficaram dentro da mesma execução (`@RepeatedTest(5)`); o pool do teste sobe para 25 conexões; a diferença entre UTC e UTC-03:00 está explicada; e a ordem do TC-15 e do TC-16 foi corrigida. A spec está pronta para seguir.

Pontos para acompanhar na implementação, sem bloquear a aprovação:

- A spec diz que a diferença de fuso é anotada na RTM e que a T-50 pode reaproveitar a evidência do vermelho, mas a RTM e o `docs/plano-de-teste.md` (que ainda fala em "mutação da T-50") não foram atualizados. Vale ajustar no PR de implementação.
- O 40P01 não chega como `DataIntegrityViolationException`: o Spring o traduz para `CannotAcquireLockException` ou `PessimisticLockingFailureException`. O tratamento do `ReservaService` precisa capturar a exceção que realmente é lançada para o deadlock virar 409.
- O TC-18 fala em deadlock simulado, mas não diz como. Vale seguir o modelo da T-22, com uma rota de teste que provoque o SQLState, para o cenário ser reproduzível.

**Parecer:** Aprovada · @Samwel-Merces · 2026-11-09


## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** 2026-11-09
- **Resultado:** APROVADA na spec validation

## 11. Grill-with-docs

> Obrigatório só para regra crítica. Nas demais, escreva "Não se aplica".

| # | Pergunta | Resposta / decisão | Documento afetado |
|---|---|---|---|
| 1 | O ADR repete a arquitetura 3.2, que já descartou lock e `SERIALIZABLE`? | Não rediscute: o ADR-0010 é curto, liga à 3.2, registra a ordenação por `recurso_id` contra deadlock e aponta as evidências de vermelho e verde | `docs/adr/0010`, arquitetura 3.2 |
| 2 | O TC-17 (ordem invertida) reproduz o deadlock de forma determinística? | Não. O TC-17 prova o resultado (sem 500, sem gravação parcial) e o TC-18 simula o `40P01`; o teste não promete provar a causa | Seção 6 |
| 3 | A mensagem do 409 vaza algo? | Diz o recurso e o fato, nunca o dono (TC-12). Dizer que o professor está ocupado é aceito (T-20, CA-05) | CA-03, TC-12 |
| 4 | E se alguém cancela enquanto outro reserva o mesmo horário? | Fora desta spec: cancelar × criar é da T-21 e T-24; aprovar × cancelar é da T-34. A T-37 cobre criar e alterar | Seção 2 |
| 5 | Quem pode criar a reserva disputada? (DP-05) | Só SOLICITANTE. O 403 dos outros perfis é da matriz de autorização (T-48) | Seção 2, T-48 |
| 6 | O teste falharia se a regra quebrasse? | Sim: a contagem vem do banco e o CA-13 exige a evidência do vermelho sem o `EXCLUDE` e do verde com ele | CA-13, `docs/evidencias/` |
| 7 | O teste é estável no CI, que é mais lento? (MC-03) | `@RepeatedTest(5)` com pool de 25 e limite de 30 s; se ficar instável, vira defeito em `docs/defeitos/` | CA-12 |
| 8 | Qual linha da RTM isto preenche? | RN-03, RF-06 e RNF-05: de `sem teste` para `red` e `green`, com as duas evidências | `docs/RTM.md` |

Decisões confirmadas em bloco por @renatarsilva (dona da task) em 2026-11-10, a partir das respostas propostas na sessão do grill.

**Resultado:** Liberada · 2026-11-10
