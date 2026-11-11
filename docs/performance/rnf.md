# RNF de desempenho e modelo de carga

> Task **T-38** · RNF-09 · Escrito **antes** de executar o teste de carga (Aula 10): a meta e o critério de decisão não podem sair do resultado.

## 1. O requisito (RNF-09, candidata MC-01 validada nesta task)

Consulta de disponibilidade e criação de reserva, sob **50 usuários simultâneos**:

| Métrica | Meta |
|---|---|
| p95 do tempo de resposta (consulta de disponibilidade e criação de reserva) | **≤ 300 ms** |
| Taxa de **erro real** | **< 1 %** |
| Resposta 500 na criação de reserva | **nenhuma** (nem uma) |

**O 409 não é erro.** Na criação de reserva disputada, 1 resposta é 201 e as demais são 409 (conflito de horário). O 409 é o comportamento correto (RN-02, RN-03) e fica **fora** da taxa de erro real; o 500, o timeout e qualquer status que não seja 201 ou 409 na criação são erro real. A decisão de aceitar a meta de 300 ms é do grupo (DP-07): o valor vem do exemplo da Aula 10.

## 2. Modelo de carga

- **Usuários:** 50 usuários fictícios (`perf/usuarios.csv`), todos com perfil Solicitante.
- **Rampa:** 10 s para chegar a 50 usuários simultâneos.
- **Repetição:** cada usuário faz 10 iterações. O login acontece uma vez por usuário e o token é reaproveitado.
- **Cada iteração:**
  1. `GET /api/recursos?tipo=SALA&disponivelDe=...&disponivelAte=...` (consulta de disponibilidade): esperado 200.
  2. `POST /api/reservas` da **mesma sala e do mesmo intervalo** por todos os usuários (disputa): esperado 201 (um só) ou 409.
- **Asserção do plano:** a criação responde 201 ou 409, nunca 500; a consulta responde 200.
- **Pensando no que a carga exercita:** a verificação de sobreposição da aplicação, o `EXCLUDE` do banco, o bloqueio de linha do recurso e a tradução do 409.

## 3. Critério de decisão

| Resultado | Quando |
|---|---|
| **Atende** | p95 ≤ 300 ms nas duas operações, erro real < 1 % e nenhum 500; e o banco confirma no máximo 1 reserva ativa por intervalo |
| **Não atende** | p95 acima de 300 ms, ou erro real ≥ 1 %, ou qualquer 500, ou mais de 1 reserva ativa no intervalo |
| **Inconclusivo** | a execução foi interrompida, o ambiente falhou (por exemplo, banco indisponível) ou houve menos de 200 amostras por operação |

## 4. Limites do experimento

- A execução é **local** (aplicação, PostgreSQL em contêiner e JMeter na mesma máquina): o JMeter disputa CPU com a aplicação, então os tempos são pessimistas em relação a um ambiente separado.
- A massa é pequena (50 usuários, 1 sala). Ela prova o comportamento sob disputa, e não a capacidade de produção.
- O relatório HTML do JMeter fica fora do Git (`perf/relatorio-*`); o arquivo `.jtl` da rodada e os números calculados ficam registrados em [`resultado-01.md`](resultado-01.md).

## 5. Rastreabilidade

RNF-09 → este documento → plano [`perf/reservas.jmx`](../../perf/reservas.jmx) → [`resultado-01.md`](resultado-01.md) → linha RNF-09 da [RTM](../RTM.md).
