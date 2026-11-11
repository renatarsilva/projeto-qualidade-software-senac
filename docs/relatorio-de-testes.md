# Relatório de testes

> Resultado da suíte automatizada na `main`, medido por `./mvnw verify` em 2026-11-11 (relatórios do Surefire e do JaCoCo) e conferido no CI: [run verde da `main`](https://github.com/renatarsilva/projeto-qualidade-software-senac/actions/runs/42958323295). Ligado à [RTM](RTM.md) e ao [relatório de qualidade](relatorio-qualidade.md).

## 1. Resultado geral

| Item | Valor |
|---|---|
| Testes executados | **940** (87 classes de teste) |
| Falhas / erros / ignorados | **0 / 0 / 0** |
| Tempo da suíte | cerca de 81 s (o banco real sobe uma vez e é compartilhado) |
| Cobertura de linhas (JaCoCo) | **97,0 %** (1356 de 1398); meta 80 % |
| Cobertura de branches (JaCoCo) | **86,0 %** (467 de 543); meta 70 % |
| Gate no build | `./mvnw verify` falha abaixo de 80 % de linhas ou 70 % de branches (configurado no `pom.xml`) |
| Análise estática (SonarCloud, `main`) | 0 bugs, 0 vulnerabilidades, 0 code smells, 0 hotspots, duplicação 0,0 %, **Quality Gate aprovado** |

As 556 anotações de teste viram 940 execuções porque os testes **parametrizados** e **repetidos** rodam uma vez por caso (por exemplo, as 49 combinações da tabela de estados e as 5 repetições da disputa de 20 solicitantes).

## 2. Por tipo de teste

| Tipo | Execuções | O que é |
|---|---|---|
| API e integração com PostgreSQL real (Testcontainers) | 638 | Sobem a aplicação inteira, chamam a API com tokens reais de cada perfil e conferem o banco |
| Unitário e de regra pura | 302 | Regras sem HTTP nem banco, com o `Clock` fixo (intervalo, sobreposição, ciclo de vida, prazos, auditoria, política de senha) |
| Com WireMock (integração externa) | 21 | A notificação contra uma API simulada: INT-01 a INT-05, timeouts, conexão recusada, token que não vaza |
| Concorrência (threads de verdade) | 24 | 20 solicitantes simultâneos, duplo clique, `PUT` simultâneos, deadlock, manutenção e reserva em paralelo |
| Parametrizados | dezenas de casos | Matriz de autorização (176 casos), tabela de transições (49 combinações), fronteiras de intervalo e de prazo |
| Carga (JMeter, fora do `verify`) | 7100 amostras | [resultado-01.md](performance/resultado-01.md) |

## 3. Por pacote

| Pacote | Testes | Falhas | Cobertura de linhas | Cobertura de branches |
|---|---|---|---|---|
| `reserva` (regras, ciclo de vida, concorrência, autorização, relatório, auditoria na API) | 676 | 0 | 98,1 % | 90,9 % |
| `professor` | 58 | 0 | 98,3 % | 86,0 % |
| `sala` | 54 | 0 | 98,8 % | 87,5 % |
| `seguranca` | 46 | 0 | 98,2 % | 83,9 % |
| `erro` | 41 | 0 | 94,0 % | 63,2 % |
| `material` | 38 | 0 | 94,4 % | 82,1 % |
| `notificacao` | 26 | 0 | 94,7 % | 76,7 % |
| Aplicação (contexto) | 1 | 0 | 100 % | |

Os pacotes `auditoria`, `manutencao`, `pesquisa`, `relatorio`, `retirada` e `documentacao` são exercitados pelos testes da pasta `reserva` (que testam a API de ponta a ponta) e têm de 90,4 % a 100 % das linhas cobertas.

**Ponto mais fraco:** os *branches* do pacote `erro` (63,2 %), abaixo da meta de 70 % só nesse pacote. A meta é do projeto inteiro (86,0 %) e está atendida. Os ramos que faltam são caminhos de erro de infraestrutura (por exemplo, falha de renderização da página de erro) difíceis de provocar.

## 4. Por requisito e regra

A ligação de cada requisito e de cada regra crítica com os testes que a comprovam está na [RTM](RTM.md). Os testes de maior valor para a nota:

| Garantia | Teste | Resultado |
|---|---|---|
| Sem dupla reserva sob concorrência | `DisputaPelaMesmaSalaConcorrenciaTest`, `ProtecaoDoBancoConcorrenciaTest` | 1 aceita e 19 recusadas; banco com 0 duplicidades; sem a proteção, o teste fica vermelho ([T-37](evidencias/T-37-vermelho.md)) |
| Sem falha de autorização | `MatrizDeAutorizacaoApiTest` (176 casos), `CompletudeDaMatrizTest` | 0 falhas; uma rota nova sem linha na matriz quebra o teste |
| Toda mudança de estado gera auditoria | `AuditoriaDasMudancasTest`, `RollbackDaAuditoriaApiTest`, `ImutabilidadeDaAuditoriaTest` | Registro na mesma transação, imutável no banco |
| Reserva recusada nas fronteiras | `SobreposicaoNoMesmoRecursoTest`, `ManutencaoDeRecursosApiTest`, `DonoEAntecedenciaDaReservaTest` | Os dois lados de cada fronteira testados; mutações pegas ([mutacao-manual.md](testes/mutacao-manual.md)) |
| Relatório com valores calculados à mão | `RelatorioDeUtilizacaoApiTest` | A conta está no comentário de cada teste |
| Nenhum detalhe interno em respostas de erro | `TermosProibidos` em todos os testes de erro | 0 respostas com stack trace, SQL ou nome de classe |

## 5. Defeitos que os testes encontraram

| Defeito | Achado por | Estado |
|---|---|---|
| DEF-02: deadlock do PostgreSQL sob concorrência virava erro 500 | O teste de concorrência da T-37 (9 falhas na primeira execução) | Fechado |
| DEF-03: 1 bug e 1 vulnerabilidade críticos no SonarCloud | A análise estática no CI | Fechado |

Detalhe em [registro de defeitos](defeitos/registro.md).

## 6. Como reproduzir

```bash
./mvnw verify        # roda os 940 testes e confere o gate de cobertura (precisa do Docker ligado)
```

Os relatórios ficam em `target/surefire-reports` (resultado por classe) e `target/site/jacoco/index.html` (cobertura). A carga se repete com os comandos de [resultado-01.md](performance/resultado-01.md).

## 7. O que a suíte não cobre

- **Interface responsiva e teste ponta a ponta**: as telas não foram entregues.
- **BDD com Cucumber**: não foi feito; o TDD tem evidência ([evidencia-tdd.md](testes/evidencia-tdd.md)).
- **O job periódico** das mudanças por tempo: a regra é testada com o `Clock`, mas nada a dispara sozinho.
