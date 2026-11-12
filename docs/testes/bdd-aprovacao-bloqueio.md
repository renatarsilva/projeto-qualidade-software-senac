# BDD: aprovação e bloqueio por manutenção

> Tasks **T-33** (os cenários) e **T-36** (a feature executável). Requisitos RF-07, RF-08, RN-03, RN-04, RN-05, RN-07 e RN-12. Os cenários estão em `src/test/resources/features/` em Dado/Quando/Então, em português, e **rodam no `./mvnw verify`** (Cucumber sobre o JUnit Platform). Cada cenário tem **um único Quando** e um Então que dá para observar na API ou no banco.

## Como ler a ligação com os outros testes

As regras já tinham testes JUnit escritos por TDD estrito (specs T-21, T-35, T-37 e T-40). Os cenários abaixo **não substituem** esses testes: dizem a mesma regra como exemplo legível, e o Cucumber a executa **pela API de verdade**, com o login e o token de cada perfil, sem conhecer o código de negócio. Os passos só observam a resposta HTTP e o banco.

Honestidade sobre o processo: o BDD foi escrito **depois** do código das regras, como especificação executável e rede de proteção, e não antes dele. A evidência de que ele protege é a **mutação** (seção 4).

## 1. Aprovação (`aprovacao.feature`)

| Regra | Cenário | Quando (único) | Então |
|---|---|---|---|
| RN-07: só o Responsável aprova | O Responsável aprova a reserva pedida | "Carlos" aprova a reserva | 200 e a reserva fica `APROVADA` |
| RN-07 | O Solicitante não aprova a própria reserva | "Marina" aprova a reserva | 403 e continua `SOLICITADA` |
| RN-07 | O Administrador não aprova reserva | "Admin" aprova a reserva | 403 e continua `SOLICITADA` |
| RN-07 | O Responsável rejeita a reserva pedida | "Carlos" rejeita a reserva | 200 e a reserva fica `REJEITADA` |
| RN-05: transições só pelos caminhos permitidos | Uma reserva rejeitada não pode ser aprovada depois | "Carlos" aprova a reserva (já rejeitada) | 409 e continua `REJEITADA` |

Nota sobre o "recurso restrito": a decisão da spec T-21 é que **toda** reserva nasce `SOLICITADA` e só o Responsável decide, restrita ou não. Por isso os cenários valem para qualquer recurso.

## 2. Bloqueio por manutenção (`manutencao.feature`)

Período em manutenção da sala: terça-feira das 10:00:00 às 12:00:00, no intervalo `[início, fim)`.

### Tabela de fronteiras

| Pedido (início → fim) | Resultado | Por quê |
|---|---|---|
| 08:00:00 → 10:00:00 | aceito | termina exatamente no início da manutenção |
| 12:00:00 → 13:00:00 | aceito | começa exatamente no fim da manutenção |
| 09:00:00 → 10:00:01 | recusado | cruza o início por um segundo |
| 11:59:59 → 13:00:00 | recusado | cruza o fim por um segundo |
| 10:30:00 → 11:30:00 | recusado | dentro do período |
| 09:00:00 → 13:00:00 | recusado | envolve o período inteiro |

Outros cenários: encerrar a manutenção libera a sala; o Solicitante não registra manutenção (403).

**Fronteira indefinida:** nenhuma ficou em aberto. O `[início, fim)` é o mesmo da reserva (RN-01) e foi decidido na spec T-35.

## 3. Reservas e auditoria (`reservas.feature`)

| Regra | Cenário | Quando (único) | Então |
|---|---|---|---|
| RN-03: dupla reserva | Duas solicitações simultâneas | 2 solicitantes pedem ao mesmo tempo a mesma sala e horário | 1 aceito, 1 recusado, 1 reserva ocupa a sala |
| RN-03 | Vinte solicitações simultâneas | 20 solicitantes pedem ao mesmo tempo | 1 aceito, 19 recusados, 1 reserva ocupa a sala |
| RN-01: `[início, fim)` | Reservas que se encostam não conflitam | "Marina" pede 11:00 → 12:00 depois de 10:00 → 11:00 | aceito |
| RN-12: auditoria | Toda mudança de estado gera registro | "Marina" cancela a reserva (já aprovada) | histórico `SOLICITADA, APROVADA, CANCELADA` |

## 4. Mutação: o BDD pega o defeito?

Mutação: na consulta de manutenção (`ManutencaoNoBanco`), o intervalo `[)` virou `[]`, ou seja, o fim passou a valer.

- Resultado: o cenário **"termina exatamente no início da manutenção"** falhou por asserção (`pedido aceito: esperado 201, veio 409`), e os outros ficaram verdes.
- A mutação foi desfeita e a feature voltou ao verde.

## 5. Quais cenários são de unidade e quais são de API

| Nível | Cenários | Onde |
|---|---|---|
| API (Cucumber, MockMvc, banco real) | Todos os cenários deste documento | `src/test/resources/features/` e `src/test/java/br/senac/recursos/bdd/` |
| Unidade (mesmas regras, sem HTTP) | Fronteiras do intervalo e da manutenção; tabela de transições | `SobreposicaoNoMesmoRecursoTest`, `ManutencaoDeRecursosApiTest`, `CicloDeVidaApiTest` |

## 6. Como rodar

```bash
./mvnw verify                          # roda as features junto dos outros testes (Docker ligado)
./mvnw test -Dtest=FeaturesDoCucumberTest   # só as features
```

O relatório por cenário fica em `target/surefire-reports/TEST-br.senac.recursos.bdd.FeaturesDoCucumberTest.xml`.
