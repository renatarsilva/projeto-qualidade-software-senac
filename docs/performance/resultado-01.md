# Resultado do teste de carga 01

> Task **T-38** · RNF-09 · Meta e critério de decisão em [`rnf.md`](rnf.md), escritos antes da execução. Plano: [`perf/reservas.jmx`](../../perf/reservas.jmx). Execução em 2026-11-11.

## Decisão: **ATENDE**

p95 muito abaixo de 300 ms nas duas operações, 0 erro real, nenhuma resposta 500 e o banco confirma 1 reserva ativa por intervalo disputado.

## 1. Ambiente

| Item | Valor |
|---|---|
| Máquina | MacBook com Apple M2 Pro, 16 GB, macOS 26.3.1 |
| Aplicação | Spring Boot 3.5, Java 21 (OpenJDK Microsoft 21.0.11), subida por `./mvnw spring-boot:run` |
| Banco | PostgreSQL 16.15 em contêiner (`compose.yaml`), na mesma máquina |
| Carga | Apache JMeter 5.6.3 (CLI), na mesma máquina |
| Massa | 50 solicitantes fictícios (`perf/usuarios.csv`) e a `sala-199` (`perf/semear.sh`, só pela API) |

## 2. Carga

50 usuários simultâneos, rampa de 10 s, login uma vez por usuário. Em cada iteração: consulta de disponibilidade (`GET /api/recursos`) e criação de reserva da **mesma sala e do mesmo intervalo** por todos os usuários (disputa).

| Rodada | Iterações por usuário | Amostras | Data disputada | Arquivo |
|---|---|---|---|---|
| 1 (modelo do RNF) | 10 | 1050 | 26/11/2026, 10:00-11:00 | `perf/resultados/rodada-01.jtl` |
| 2 (estado estável) | 60 | 6050 | 27/11/2026, 10:00-11:00 | `perf/resultados/rodada-02.jtl` |

Comando: `jmeter -n -t perf/reservas.jmx -l perf/resultados/rodada-0N.jtl -e -o perf/relatorio-0N -JsalaId=1 -Jdata=2026-11-26`. Os percentis saíram do `.jtl` por `perf/analisar.py`, e não do relatório HTML (que fica fora do Git).

## 3. Resultados

### Rodada 1: 105 req/s no total, 0 erro real

| Operação | Amostras | p50 | p95 | p99 | Máx | Códigos |
|---|---|---|---|---|---|---|
| login | 50 | 92 ms | 96 ms | 106 ms | 106 ms | 200: 50 |
| disponibilidade | 500 | 2 ms | **4 ms** | 5 ms | 15 ms | 200: 500 |
| criação disputada | 500 | 3 ms | **7 ms** | 9 ms | 17 ms | **201: 1**, **409: 499** |

### Rodada 2: 594 req/s no total, 0 erro real

| Operação | Amostras | p50 | p95 | p99 | Máx | Códigos |
|---|---|---|---|---|---|---|
| login | 50 | 90 ms | 99 ms | 106 ms | 106 ms | 200: 50 |
| disponibilidade | 3000 | 1 ms | **2 ms** | 3 ms | 9 ms | 200: 3000 |
| criação disputada | 3000 | 3 ms | **5 ms** | 6 ms | 13 ms | **201: 1**, **409: 2999** |

### O 409 foi separado do erro

Na criação, 1 resposta foi 201 e todas as outras foram **409**, o comportamento correto (RN-02 e RN-03). O 409 **não** entra na taxa de erro. O erro real (status fora de 201 e 409 na criação, 200 na consulta, ou 500) foi **0 nas duas rodadas**, e o log da aplicação não tem nenhuma linha de erro.

## 4. O banco confirma o resultado

Consulta feita no PostgreSQL depois de cada rodada:

| Verificação | Rodada 1 | Rodada 2 |
|---|---|---|
| Vínculos ativos da sala no intervalo disputado | **1** | **1** |
| Reservas do dia disputado | 1 | 1 |
| Intervalos com mais de 1 reserva ativa (banco inteiro) | 0 | 0 |

No fim, a tabela `conflito_evitado` tinha 3997 registros: 499 da primeira execução do plano, 499 da rodada 1 e 2999 da rodada 2, exatamente o número de respostas 409. Isso confere o registro dos conflitos do relatório de utilização (T-41) com a carga real.

## 5. Histórico honesto da execução

A primeira execução do plano mostrou 47 % de erro no JMeter. A causa era o **plano**, e não o sistema: o JMeter trata todo 4xx como falha, mesmo com a asserção aceitando o 409. A asserção foi corrigida (opção "Ignore Status") e a carga foi repetida com outra data, para a disputa começar do zero. Os números acima são das duas rodadas posteriores à correção.

## 6. Hipótese de gargalo

- O **login** é a operação mais lenta (p95 ≈ 100 ms): o BCrypt é caro de propósito, para proteger a senha. Não é gargalo da meta (que pede 300 ms para consulta e criação) e roda uma vez por usuário.
- A criação disputada é rápida porque quase todas as tentativas são **recusadas cedo** pela verificação de sobreposição da aplicação. O caminho mais caro é o do **único 201**, que passa pelo bloqueio da linha do recurso e pelo `EXCLUDE` do banco; ele aparece nos máximos (13 a 17 ms).
- Em carga maior, a hipótese é que o limite venha do **pool de conexões** (padrão de 10 do Hikari) e da disputa pelo bloqueio da linha do recurso, e não do cálculo da regra.

## 7. Limites deste resultado

Execução local, com a aplicação, o banco e o JMeter na mesma máquina; 50 usuários e uma sala. O resultado prova o comportamento sob disputa e a folga em relação à meta, e não a capacidade de produção. Uma rodada com mais de uma sala e criações livres (todas 201) fica como evolução.
