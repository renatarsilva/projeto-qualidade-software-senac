# Arquitetura: Organização de Recursos

> Esqueleto criado na semana 34 (Aula 03). Preenchido nas tasks T-10 a T-13 (semana 35).

## 1. Stack e descoberta

### 1.1 Stack

- Java 21 e Spring Boot 3.5 (Web, Security, Data JPA, Validation)
- PostgreSQL 16, com migrações Flyway
- Interface Thymeleaf + Bootstrap no mesmo projeto
- Testes: JUnit 5, Testcontainers, WireMock, Cucumber, JMeter
- Qualidade: GitHub Actions, JaCoCo, SonarCloud

### 1.2 Contexto e fonte de verdade

O sistema organiza a alocação de **salas, professores e materiais** de uma instituição de ensino sem conflitos de horário. O foco da disciplina é a evidência de qualidade, segurança, rastreabilidade e desempenho, não só o sistema funcionando.

- **Fonte de verdade dos requisitos:** [`prd.md`](prd.md), que por sua vez segue a [especificação oficial](https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html). Esta arquitetura preserva os IDs do PRD (RF-xx, RN-xx, RNF-xx, L-xx, DP-xx) e não cria requisitos novos: o que for suposição ou decisão de projeto recebe ID próprio (S-xx, D-xx).
- **Análise de qualidade:** a [utility tree](atam/utility-tree-inicial.md) já tem 8 folhas e 3 cenários detalhados. A T-12 a completa; este documento não a repete.

### 1.3 Stakeholders

| Perfil | Persona (fictícia) | O que precisa do sistema |
|---|---|---|
| Solicitante | Marina Couto | Consultar disponibilidade e criar, alterar e cancelar as próprias reservas |
| Responsável | Rogério Lima | Aprovar recursos restritos, validar a alocação de docentes e acompanhar retiradas e devoluções |
| Administrador | Patrícia Nogueira | Gerenciar salas, professores, materiais, usuários, bloqueios e manutenção |
| Professor da disciplina | — | Avaliar evidências de qualidade, segurança e rastreabilidade; fiscalizar os limitadores de nota |
| Time (Renata, Samuel, Caio) | — | Entregar, defender na prova oral e manter o código que cada um escreveu |

### 1.4 Fatos

Observados no repositório em 31/10/2026. Nenhum é decisão: são o ponto de partida.

| ID | Fato | Onde se confirma |
|---|---|---|
| F-01 | Java 21, Spring Boot 3.5.16, PostgreSQL 16 e Flyway | `pom.xml`, `compose.yaml` |
| F-02 | Só existe o esqueleto: a classe principal e 2 testes de contexto. Não há entidade, endpoint nem migração | `src/` |
| F-03 | O esquema do banco é responsabilidade do Flyway; o Hibernate só valida (`ddl-auto=validate`) | `application.properties` |
| F-04 | Horários ficam em UTC no banco; a conversão de fuso é na borda (API e telas) | `application.properties` |
| F-05 | `open-in-view` está desligado: consultas lazy não funcionam fora da transação | `application.properties` |
| F-06 | Os testes de integração usam PostgreSQL real via Testcontainers, nunca H2 | `TestcontainersConfiguration` |
| F-07 | O Spring Security está no classpath sem configuração: hoje a aplicação sobe com senha gerada e sem perfis | saída de `./mvnw verify` |
| F-08 | Não existe pipeline de CI | ausência de `.github/workflows` |

### 1.5 Drivers priorizados

Os drivers são os requisitos que mais pesam na arquitetura. A ordem vem do risco para a nota e do custo de errar tarde.

| # | Driver | Origem | Por que pesa |
|---|---|---|---|
| 1 | Nenhuma dupla reserva sob solicitações simultâneas | RN-03, RNF-05, F2 | Limitador de nota. É condição de corrida: só o banco garante |
| 2 | Nenhuma operação executada por perfil não autorizado | RN-07, RNF-06, F4, F5 | Limitador de nota. Precisa valer em todo ponto de entrada |
| 3 | Sem sobreposição no mesmo recurso, inclusive a agenda do professor | RN-02, RF-05, F1 | Regra central do produto; define o modelo de dados |
| 4 | Toda mudança de estado gera auditoria | RN-09, RF-10, F6 | Rastreabilidade; atravessa todas as transições |
| 5 | Falha da notificação não desfaz a reserva | RNF-10, RF-11, F7 | Isola uma dependência externa do fluxo principal |
| 6 | Desempenho da disponibilidade e da criação de reserva | RNF-09 (candidata) | Meta ainda não aprovada (DP-07) |

### 1.6 Suposições

Respostas provisórias às lacunas e decisões pendentes do PRD. **Nenhuma é regra oficial**: todas estão com validação pendente (com o professor, quando a especificação for omissa, e com o time) e podem mudar. A modelagem de dados (seção 3) parte delas.

| ID | Suposição | Responde a | Efeito no desenho | Status |
|---|---|---|---|---|
| S-01 | Reservas adjacentes não conflitam: uma que termina às 10h e outra que começa às 10h convivem. O período é fechado à esquerda e aberto à direita, `[início, fim)` | L01 · RN-02 | `tstzrange` com limites `[)` na restrição anti-conflito | Validação pendente |
| S-02 | Ocupam o horário os estados `SOLICITADA`, `APROVADA` e `EM_USO`. Não ocupam `REJEITADA`, `CANCELADA`, `NAO_COMPARECEU` e `CONCLUIDA` | L02 · RN-02, RN-03, RN-05, RN-06 | A restrição de exclusão vale só para os estados que ocupam (cláusula `WHERE`) | Validação pendente |
| S-03 | Todo recurso tem um indicador booleano `restrito`. Quando verdadeiro, a reserva exige aprovação do Responsável | L05 · RN-07, RF-07 | Coluna `restrito` na entidade de recurso | Validação pendente |
| S-04 | Bloqueio e manutenção são o mesmo mecanismo: um período de indisponibilidade do recurso, com motivo | L13 · RN-04, RF-08 | Uma só entidade de período de manutenção, sem tabela separada de bloqueio | Validação pendente |
| S-05 | Criar uma manutenção **não** cancela as reservas existentes: ela impede novas reservas e lista as reservas afetadas para o Administrador tratar | L14 · RN-04, FE-03 | Consulta de reservas afetadas; nenhum cancelamento em lote | Validação pendente |
| S-06 | Um job diário, no fim do dia, marca `NAO_COMPARECEU` nas reservas ainda `APROVADA` cujo horário de início já passou, e conclui as `EM_USO` vencidas | DP-01 · RN-06 | Fluxo batch com perfil de sistema; a sequência fica na seção 2 | Validação pendente |

### 1.7 Decisões

Decisões de projeto desta arquitetura. Cada uma cita o requisito de origem. Decisões que viram ADR são formalizadas na T-13.

| ID | Decisão | Origem | Alternativas consideradas | Custo assumido | Status |
|---|---|---|---|---|---|
| D-01 | `SOLICITADA` ocupa o horário, como em S-02 | RN-02, RN-03 · premissa do CEN-01 na utility tree | (a) só `APROVADA` e `EM_USO` ocupam; (b) `SOLICITADA` também ocupa | Em (b), um pedido pendente de aprovação "segura" o recurso e o solicitante pode bloquear horários. Em (a), duas solicitações simultâneas podem ser ambas aprovadas depois, o que reabre a dupla reserva (RNF-05). Escolhida a (b). Trade-off levado à T-12 | Aceita; validação pendente |
| D-02 | Uma reserva se liga aos seus recursos pela tabela `reserva_recurso`, em vez de um `recurso_id` único na reserva | RN-02, RF-05 · agenda do professor | (a) `recurso_id` único na reserva; (b) tabela `reserva_recurso` | Em (a), uma reserva só ocupa um recurso, e reservar sala e professor juntos exige várias reservas ligadas. Em (b), uma reserva ocupa sala, professor e material ao mesmo tempo, mas o `EXCLUDE` do PostgreSQL só enxerga colunas da própria tabela: o período e o indicador de ocupação são copiados para `reserva_recurso` e precisam ficar sincronizados com o estado da reserva. Escolhida a (b). Risco levado à T-12 | Aceita; validação pendente |

### 1.8 Lacunas ainda abertas

Lacunas e decisões pendentes do [PRD](prd.md#48-lacunas) que as suposições S-01 a S-06 **não** respondem. Elas não bloqueiam a arquitetura, mas cada uma tem um ponto em que será resolvida. As respondidas por S-xx (L01, L02, L05, L13, L14 e DP-01) não aparecem aqui.

| ID | Lacuna ou decisão pendente | Efeito na arquitetura | Onde será resolvida |
|---|---|---|---|
| L03 | O que é "simultâneo" e que resposta recebe a solicitação recusada | Quem decide o vencedor é o banco, pela restrição anti-conflito. A forma da resposta (código e mensagem) fica em aberto | T-37 |
| L04 | A reserva de um recurso não restrito passa por `APROVADA`? | Define as transições permitidas e se `SOLICITADA` vai direto a `APROVADA` | T-21 (spec do ciclo de vida) |
| L06 | Não existe matriz oficial de perfil × operação | A autorização por operação não pode ser detalhada ainda | T-48, T-49 |
| L07 | Conteúdo mínimo do registro de auditoria | Define as colunas da tabela de auditoria (seção 3) | T-40 |
| L08 | O que acontece com a reserva quando a notificação falha | A utility tree propõe que a reserva se mantenha; a notificação fica isolada do fluxo da reserva | T-31 |
| L09 | Notificações pendentes precisam ser reenviadas? | Reenvio está fora de escopo (FE-05); não há fila de reprocessamento | — |
| L10 | O coordenador tem permissões além das do professor no perfil Solicitante? | Nenhum efeito: os dois são o mesmo perfil (FE-02) | — |
| L11 | Até quando uma reserva pode ser alterada ou cancelada; cancelar uma reserva iniciada conta como "apagar"? | Interage com RN-08: reserva iniciada nunca é removida do banco | T-21 |
| L12 | O que significa "validar a alocação de docentes" para o Responsável | Pode exigir uma aprovação explícita da agenda do professor, com efeito no modelo | Sem task definida no PRD |
| DP-02 | Quais perfis consultam a auditoria | Afeta a autorização do acesso ao histórico | T-40, T-48 |
| DP-03 | Quais eventos disparam notificações | Define o contrato com a API externa | T-31 |
| DP-04 | Quem registra retirada e devolução | Afeta a autorização e o modelo de retirada | T-42 |
| DP-05 | Se o Administrador cria reservas | Afeta a matriz de autorização | T-48 |
| DP-06 | Relação entre "solicitação especial" e "recurso restrito" | Pode exigir um segundo mecanismo de aprovação além do campo `restrito` (S-03) | T-33, T-34, a decidir com o professor |
| DP-07 | Valores finais de desempenho | Define a meta que a arquitetura precisa atender (RNF-09) | T-38 |
| DP-08 | Timeouts da integração externa | Parâmetros configuráveis da integração, nunca fixos no código | T-31 |

TODO (T-10): glossário, diagramas C4 e sequências.

## 2. Camadas

TODO (T-10)

## 3. Modelo de dados

TODO (T-11)

## 4. Autenticação e sessão

TODO (T-13, T-15)

## 5. Decisões (ADR)

As decisões ficam em [`docs/adr/`](adr/). TODO (T-13): ADRs de arquitetura e matriz requisito → decisão → componente → evidência.
