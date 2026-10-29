# Utility tree, cenários (A, A) e casos de teste — Semana 33

> Atividade #2 (Aula 02 · ATAM e planejamento de casos de teste) · Task **T-05**

## 1. Autoria

| Item | Registro |
|---|---|
| Responsável pela task | Caio (@CaiosAguiar) |
| Autoria do conteúdo avaliado | Caio (@CaiosAguiar), que escreve o conteúdo avaliado e o defende na prova oral |
| Revisão cruzada | Samuel (@Samwel-Merces), por decisão da equipe, em substituição a Renata. **Revisão em andamento**: o REV-04 aguarda confirmação do revisor (seção 11) |
| Formato da atividade | O material pede "Em dupla"; esta entrega foi feita individualmente. O planejamento inicial previa execução com outro integrante, mas Caio permaneceu como responsável pela T-05 e autor do conteúdo avaliado, com revisão cruzada por outro integrante. Não há confirmação de que o professor autorizou a execução individual. |

### Divisão de trabalho da equipe

| Pessoa | Papel nesta entrega |
|---|---|
| Caio | Responsável pela T-05 e autor do conteúdo avaliado |
| Samuel | Criou o esqueleto inicial deste documento (commit `18061e5`, 24/10/2026) e realiza a revisão cruzada do conteúdo produzido por Caio. Não é autor do conteúdo avaliado. **Revisão em andamento** |

## 2. Fontes

Em ordem de prioridade:

1. [Especificação oficial do projeto](https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html)
2. [Material da Aula 02](https://afonsolelis.github.io/aulas_senac/pages/qualidade2/material/material_planejamento-casos-teste.html)
3. [`docs/projeto/contexto-da-disciplina.md`](../projeto/contexto-da-disciplina.md) (numeração RF-01 a RF-14)
4. [`docs/planejamento/backlog.md`](../planejamento/backlog.md) e [`docs/diagnostico/diagnostico-inicial.md`](../diagnostico/diagnostico-inicial.md)

## 3. Premissas e lacunas desta entrega

- **Origem dos cenários:** cada cenário cita o `RF-xx` (numeração do `contexto-da-disciplina.md`) e o trecho literal da regra crítica da especificação. As regras de negócio ainda não têm ID: marcadas como **RN pendente (T-09)**.
- **Personas ausentes:** a T-04 (personas) ainda não foi feita. As fontes dos cenários usam os perfis da especificação (Solicitante, Responsável, Administrador). Pendência: **T-04**.
- **Métricas:** a especificação não traz números de desempenho, disponibilidade ou recuperação. Todo número sem origem oficial está marcado como **Métrica candidata, validação pendente** (seção 5.2).
- **Jira:** retirado do fluxo do projeto. A T-05 não tem chave Jira, e a branch e os commits não seguem o padrão `KAN-NN`.
- **Lacunas das folhas:** as regras que a especificação não confirma estão registradas como L1 a L9 na seção 4 ("Lacunas das folhas").

## 4. Utility tree

Escala (I, D) do material: **A**lta, **M**édia, **B**aixa. I = importância para o negócio; D = dificuldade de alcançar.

| ID | Atributo (ISO/IEC 25010) | Refinamento | Folha (estímulo → resposta esperada) | Origem | (I, D) | Justificativa (uma linha) |
|---|---|---|---|---|---|---|
| F1 | Adequação funcional | Correção funcional | Solicitante pede uma reserva cujo intervalo se sobrepõe a outra reserva do mesmo recurso (sala, material ou professor) → o pedido é recusado e nenhuma reserva nova é gravada | RF-05 · regra crítica: "Reservas do mesmo recurso (sala, material ou professor) não podem se sobrepor." · RN pendente (T-09) · lacunas L1 e L2 | (A, A) | I: regra crítica e objetivo central do sistema (alocar sem conflito). D: comparar intervalos e fronteiras em três tipos de recurso, inclusive a agenda do professor. |
| F2 | Adequação funcional | Correção funcional | Solicitações simultâneas pedem o mesmo recurso no mesmo horário → só uma reserva é aceita e as demais são recusadas | RF-06 · regra crítica: "Duas solicitações simultâneas devem produzir uma única reserva aceita." · RN pendente (T-09) · lacunas L2 e L3 | (A, A) | I: a possibilidade de dupla reserva é limitador de nota. D: é uma condição de corrida, que a verificação sequencial não impede e que exige teste automatizado de concorrência. |
| F3 | Adequação funcional | Correção funcional | Pedido para mudar uma reserva de SOLICITADA direto para EM_USO → a transição é recusada e o estado continua SOLICITADA | Regra crítica de estados ("SOLICITADA → APROVADA → EM_USO → CONCLUIDA"), sem RF específico identificado · RN pendente (T-09) · lacuna L4 | (A, M) | I: uma transição inválida pula a aprovação e corrompe o ciclo de vida e a auditoria. D: as transições são finitas e verificáveis por tabela, mais simples que concorrência. |
| F4 | Segurança | Integridade | Usuário sem o perfil Responsável tenta aprovar a reserva de um recurso restrito → a aprovação é recusada (resposta proposta, não confirmada pela especificação: o estado da reserva não muda) | RF-07 · regra crítica: "Somente responsáveis aprovam recursos restritos." · RN pendente (T-09) · lacuna L5 | (A, M) | I: falha de autorização é limitador de nota. D: a regra é uma verificação de perfil conhecida, mas precisa valer em todo ponto que aprova reservas. |
| F5 | Segurança | Integridade | Usuário com perfil Solicitante tenta uma operação do Administrador (ex.: cadastrar um usuário ou bloquear um recurso para manutenção) → a operação é negada e nenhum dado é alterado | RF-01 · perfis da especificação (Administrador "gerencia salas, professores, materiais, usuários, bloqueios e manutenção") · RN pendente (T-09) · lacuna L6 | (A, M) | I: falha de autorização é limitador de nota. D: o controle por perfil é repetitivo, mas exige cobrir cada operação administrativa sem esquecer nenhuma. |
| F6 | Segurança | Responsabilização | Uma reserva muda de estado (qualquer transição, inclusive as alternativas) → um registro de auditoria da mudança é gravado | RF-10 · regra crítica: "Toda mudança de estado gera auditoria." · RN pendente (T-09) · lacuna L7 | (A, M) | I: regra crítica que dá rastreabilidade às decisões sobre recursos. D: o registro se repete em todas as transições, incluindo os caminhos alternativos (REJEITADA, CANCELADA, NAO_COMPARECEU). |
| F7 | Confiabilidade | Tolerância a falhas | A API de notificações responde HTTP 500 ou não responde dentro do tempo limite → a falha é tratada de forma controlada, sem expor detalhes internos (comportamento exato da reserva: lacuna L8) | RF-11 · estratégia obrigatória (WireMock para integrações externas; tratamento seguro de erros) · RN pendente (T-09) · lacuna L8 | (A, A) | I: uma falha externa sem tratamento afetaria a operação principal do sistema. D: falhas de rede e timeout só são reproduzíveis com simulação, e o tempo limite precisa ser definido. |
| F8 | Confiabilidade | Recuperabilidade | Notificações que falharam ficam pendentes e a API volta a responder → as pendentes são processadas sem envio duplicado | RF-11 · recuperação não consta da especificação (lacuna L9) | (M, A) | I: a notificação é secundária à reserva, e a especificação aceita notificação simulada. D: exige guardar pendências e reprocessar sem duplicar o envio. |

**Folhas (A, A):** F1, F2 e F7.

**Rastreabilidade planejada:** F1 → CEN-01 → CT-01 · F2 → CEN-02 → CT-02 · F7 → CEN-03 → CT-03 (cenários na seção 5.3, casos na seção 6.2).

### Lacunas das folhas

Regras que a especificação não confirma. Nenhuma delas é requisito oficial: devem ser confirmadas com o professor e registradas no PRD (T-09).

| ID | Folha | Lacuna |
|---|---|---|
| L1 | F1 | A especificação não diz se uma reserva que termina exatamente quando outra começa conta como sobreposição. |
| L2 | F1 e F2 | A especificação não diz quais estados ocupam o horário (por exemplo, se uma reserva REJEITADA ou CANCELADA ainda bloqueia o recurso). |
| L3 | F2 | A especificação não define o que é "simultâneo" nem a resposta que a solicitação recusada recebe. A quantidade de 20 solicitações concorrentes usada no CEN-02 e no CT-02 já é **Métrica candidata, validação pendente**. |
| L4 | F3 | A aprovação só é obrigatória para recursos restritos (RF-07). A especificação não diz se a reserva de um recurso não restrito passa por APROVADA ou pode ir de SOLICITADA para EM_USO. A folha F3 vale como premissa até essa confirmação. |
| L5 | F4 | A especificação não define o que torna um recurso "restrito" nem como ele é marcado. |
| L6 | F5 | Não existe uma matriz oficial de perfil × operação. A lista das operações administrativas sai da tabela de perfis da especificação. |
| L7 | F6 | A especificação não define o conteúdo mínimo do registro de auditoria (por exemplo, quem, quando, estado anterior e estado novo). |
| L8 | F7 | A especificação não diz o que acontece com a reserva quando a notificação falha, nem cita HTTP 500 ou timeout. O backlog (T-31) propõe que a falha não quebre a reserva, com tempos limite de 500 ms (conexão) e 1500 ms (leitura): **Métrica candidata, validação pendente**. |
| L9 | F8 | A especificação não exige reenvio de notificações pendentes. A existência de pendências e de reprocessamento é uma suposição de projeto, não um requisito. |

### Justificativa da escolha dos atributos

- **Adequação funcional:** as regras críticas de conflito (sobreposição, concorrência e estados) são o objetivo central do sistema, e a dupla reserva é limitador de nota.
- **Segurança:** a falha de autorização é limitador de nota, e a especificação exige aprovação só por Responsáveis e auditoria de toda mudança de estado.
- **Confiabilidade:** o RF-11 traz uma integração externa, e a estratégia obrigatória exige isolá-la (WireMock) e tratar erros com segurança.

## 5. Cenários (A, A) em seis partes

### 5.1 Modelo

Copie este bloco uma vez para cada cenário (A, A).

#### CEN-NN · _título_

| # | Parte | Conteúdo |
|---|---|---|
| 1 | Fonte do estímulo | |
| 2 | Estímulo | |
| 3 | Ambiente | |
| 4 | Artefato | |
| 5 | Resposta | |
| 6 | Medida da resposta | |

- **Origem:** RF-xx: "_trecho literal da especificação_" · RN pendente (T-09)
- **Atributo / refinamento:**
- **(I, D):** · **Justificativa:**

### 5.2 Modelo de métrica candidata

Usar quando a medida da resposta não tiver número oficial.

| Campo | Conteúdo |
|---|---|
| Valor proposto | |
| Status | Métrica candidata, validação pendente |
| Justificativa do número | |
| Impacto se o número estiver errado | |
| Quem aprova | |

### 5.3 Cenários

#### CEN-01 · Sobreposição de horários (F1)

| # | Parte | Conteúdo |
|---|---|---|
| 1 | Fonte do estímulo | Usuário autenticado com perfil Solicitante |
| 2 | Estímulo | Pede uma reserva para o mesmo recurso (sala, material ou professor) com intervalo sobreposto ao de uma reserva existente |
| 3 | Ambiente | Operação normal. Existe uma reserva para o recurso, em um estado que ocupa o horário (quais estados ocupam: lacuna L2). O recurso não está em manutenção |
| 4 | Artefato | Mecanismo de validação e registro de reservas |
| 5 | Resposta | Todo intervalo sobreposto é rejeitado, sem gravar uma reserva nova. Intervalos adjacentes (a nova começa exatamente quando a existente termina, ou termina exatamente quando a existente começa) são aceitos apenas como **premissa de teste pendente de validação** (lacuna L1), não como regra oficial |
| 6 | Medida da resposta | 21 de 21 verificações com o resultado esperado: 15 sobreposições rejeitadas, e após cada uma continua existindo exatamente 1 reserva ativa ocupando o recurso e o intervalo (a existente); 6 adjacências aceitas segundo a premissa pendente L1. Registros em REJEITADA, se o sistema os gravar, não contam como reservas ativas. **Métrica candidata, validação pendente** |

- **Premissa de teste, validação pendente:** "Uma solicitação aceita cria uma reserva no estado SOLICITADA, e esse estado ocupa provisoriamente o recurso e o intervalo. Registros em REJEITADA não ocupam o horário. A relação completa entre estados e ocupação permanece na lacuna L2." Reserva ativa = reserva em estado que ocupa o horário. Este cenário não afirma que o sistema grava registros rejeitados.
- **Origem:** RF-05: "Detecção de sobreposição de horários, inclusive na agenda do professor" · regra crítica: "Reservas do mesmo recurso (sala, material ou professor) não podem se sobrepor." · RN pendente (T-09) · lacunas L1 e L2
- **Atributo / refinamento:** Adequação funcional / Correção funcional
- **(I, D):** (A, A) · **Justificativa:** I: regra crítica e objetivo central do sistema (alocar sem conflito). D: comparar intervalos e fronteiras em três tipos de recurso, inclusive a agenda do professor.

| Campo | Conteúdo |
|---|---|
| Valor proposto | 21 verificações: para cada recurso (sala, material e professor), 5 formas de sobreposição (parcial no início, parcial no fim, nova contida na existente, nova contendo completamente a existente e intervalo idêntico) e 2 formas de adjacência (nova começa exatamente quando a existente termina; nova termina exatamente quando a existente começa). Total: 15 sobreposições e 6 adjacências (3 × 5 + 3 × 2) |
| Status | Métrica candidata, validação pendente |
| Justificativa do número | Cobre os 3 tipos de recurso citados na regra crítica com as mesmas 5 formas de sobreposição e testa a fronteira da lacuna L1 nos dois sentidos em cada tipo |
| Impacto se o número estiver errado | Se L1 decidir que adjacência conta como sobreposição, as 6 adjacências passam a esperar rejeição: o resultado passa a ser 21 rejeitadas e 0 aceitas, com as mesmas 21 verificações. Se faltar uma forma de sobreposição na composição, um defeito nessa forma passa sem ser detectado |
| Quem aprova | Professor e equipe do projeto |

#### CEN-02 · Proteção contra dupla reserva simultânea (F2)

| # | Parte | Conteúdo |
|---|---|---|
| 1 | Fonte do estímulo | 20 clientes concorrentes, cada um autenticado como um usuário distinto (SOLICITANTE-001 a SOLICITANTE-020), todos com perfil Solicitante |
| 2 | Estímulo | 20 solicitações enviadas de forma sincronizada para o mesmo recurso, na mesma data e no mesmo intervalo |
| 3 | Ambiente | Operação normal e banco de dados disponível. SALA-102 está inicialmente livre no intervalo, e os 20 Solicitantes têm agenda livre no mesmo intervalo. Todos solicitam a mesma sala (SALA-102), na mesma data e no mesmo intervalo: a única disputa comum entre as solicitações é SALA-102 nesse intervalo. Assim, uma rejeição não pode ser explicada por conflito na agenda do Solicitante (RF-05) nem por deduplicação de pedidos do mesmo usuário |
| 4 | Artefato | Mecanismo de validação e persistência das reservas |
| 5 | Resposta | Uma solicitação é aceita e as demais são rejeitadas. A forma da rejeição não está definida (lacuna L3). Eventuais registros das solicitações rejeitadas não contam como reservas ativas e não podem ocupar o horário (lacuna L2) |
| 6 | Medida da resposta | Exatamente 1 solicitação aceita, 19 rejeitadas, exatamente 1 reserva ativa que ocupa o recurso e o intervalo, e 0 duplicidades ativas para o mesmo recurso e intervalo. A quantidade de 20 solicitações (e, portanto, as 19 rejeitadas) é **Métrica candidata, validação pendente**. "Uma única reserva aceita" vem da especificação. Não há meta de tempo de resposta: o cenário verifica correção funcional sob concorrência, não desempenho |

- **Premissa de teste, validação pendente:** "Uma solicitação aceita cria uma reserva no estado SOLICITADA, e esse estado ocupa provisoriamente o recurso e o intervalo. Registros em REJEITADA não ocupam o horário. A relação completa entre estados e ocupação permanece na lacuna L2." Reserva ativa = reserva em estado que ocupa o horário. Este cenário não afirma que o sistema grava registros rejeitados.
- **Origem:** RF-06: "Proteção contra dupla reserva em solicitações simultâneas" · regra crítica: "Duas solicitações simultâneas devem produzir uma única reserva aceita." · RN pendente (T-09) · lacunas L2 e L3
- **Atributo / refinamento:** Adequação funcional / Correção funcional
- **(I, D):** (A, A) · **Justificativa:** I: a possibilidade de dupla reserva é limitador de nota. D: é uma condição de corrida, que a verificação sequencial não impede e que exige teste automatizado de concorrência.

| Campo | Conteúdo |
|---|---|
| Valor proposto | 20 solicitações concorrentes (esperado: 1 aceita, 19 rejeitadas, 1 reserva ativa ocupando o recurso e o intervalo, 0 duplicidades ativas) |
| Status | Métrica candidata, validação pendente |
| Justificativa do número | A especificação fala em "duas solicitações simultâneas", mas não fixa a quantidade do teste. 20 é o valor proposto no backlog (T-37): aumenta a chance de expor a condição de corrida e continua viável no pipeline |
| Impacto se o número estiver errado | Com poucas solicitações, a corrida pode não aparecer e o teste passa sem provar a proteção. Com muitas, o teste fica lento e pode falhar por limite do ambiente, sem relação com a regra |
| Quem aprova | Professor e equipe do projeto |

#### CEN-03 · Falha na API de notificações (F7)

| # | Parte | Conteúdo |
|---|---|---|
| 1 | Fonte do estímulo | Integração externa de notificações, simulada com WireMock |
| 2 | Estímulo | Duas tentativas simuladas: em uma, a API responde HTTP 500; na outra, a API estabelece a conexão, mas atrasa a resposta em 2000 ms e excede o tempo limite de leitura de 1500 ms. O tempo limite de conexão de 500 ms fica apenas como configuração |
| 3 | Ambiente | Operação normal. Uma reserva válida de um **recurso restrito** é aprovada por um usuário com perfil Responsável, e essa aprovação é o evento que dispara a notificação neste cenário (**premissa de escopo do cenário**, não regra geral confirmada para todas as notificações). O recurso restrito garante que a reserva passa por APROVADA (RF-07), sem depender da lacuna L4. O que torna um recurso restrito ainda não está definido (lacuna L5) |
| 4 | Artefato | Integração de notificações e fluxo da reserva |
| 5 | Resposta | A falha é tratada de forma controlada, e nenhum detalhe interno é exposto ao usuário. A reserva válida é preservada: **Decisão de projeto proposta, validação pendente** (lacuna L8). A falha gera um registro da falha (definição abaixo). Este cenário não pressupõe retentativa, fila ou deduplicação |
| 6 | Medida da resposta | Para cada uma das 2 tentativas simuladas (HTTP 500 e timeout de leitura): exatamente 1 reserva preservada; 0 reversões da reserva; exatamente 1 registro observável da falha; 0 respostas ao usuário contendo stack trace, nome de classe, consulta SQL, caminho interno ou corpo bruto do erro externo. **Métrica candidata, validação pendente** |

- **Registro da falha:** evento de erro observável, associado ao identificador da reserva e que identifica a causa como HTTP 500 ou timeout. Campos mínimos (identificador da reserva e causa): **Decisão de projeto proposta, validação pendente**. Este cenário não define onde o evento fica (log, auditoria ou banco).
- **Cobertura do timeout:** o CT-03 provoca timeout de leitura. Ele não provoca falha nem timeout de conexão. A falha de conexão poderá ser coberta por um caso futuro, se continuar no escopo.

- **Origem:** RF-11: "Notificação simulada ou integração com API externa" · estratégia obrigatória: WireMock para integrações externas e tratamento seguro de erros · RF-07: "Aprovação obrigatória para recursos restritos" e regra crítica "Somente responsáveis aprovam recursos restritos." (evento de disparo) · RN pendente (T-09) · lacunas L5 e L8
- **Atributo / refinamento:** Confiabilidade / Tolerância a falhas
- **(I, D):** (A, A) · **Justificativa:** I: uma falha externa sem tratamento afetaria a operação principal do sistema. D: falhas de rede e timeout só são reproduzíveis com simulação, e o tempo limite precisa ser definido.

| Campo | Conteúdo |
|---|---|
| Valor proposto | Tempo limite de conexão: 500 ms (só configuração; não provocado). Tempo limite de leitura: 1500 ms. Atraso simulado no WireMock: 2000 ms. Por tentativa (2 tentativas: HTTP 500 e timeout de leitura): 1 reserva preservada, 0 reversões, 1 registro observável da falha e 0 respostas ao usuário com detalhe interno |
| Status | Métrica candidata, validação pendente |
| Justificativa do número | Os tempos limite são os propostos no backlog (T-31). A especificação não traz nenhum deles. As contagens de preservação e reversão decorrem da decisão proposta de preservar a reserva quando a notificação falha (L8). "1 registro por tentativa" permite saber a causa de cada falha. "0 respostas com detalhe interno" traduz em número o tratamento seguro de erros da estratégia obrigatória |
| Impacto se o número estiver errado | Tempos curtos demais tratam como falha uma API apenas lenta. Tempos longos demais seguram a operação do usuário enquanto a API não responde. Se L8 decidir que a falha deve desfazer a reserva, as contagens de preservação e reversão se invertem. Se os campos mínimos do registro mudarem, muda o que o caso de teste confere em cada registro, não a quantidade |
| Quem aprova | Professor e equipe do projeto |

## 6. Casos de teste

### 6.1 Modelo

Copie este bloco para cada caso de teste. Critério do material: outra pessoa executa o caso sem perguntar nada.

#### CT-NN · _título: o que é verificado, em uma linha_

| Campo | Conteúdo |
|---|---|
| ID | CT-NN |
| Título | |
| Pré-condições | |
| Dados de entrada (valores concretos) | |
| Passos | 1. <br> 2. <br> 3. |
| Resultado esperado (mensurável) | |
| Rastreabilidade | CT-NN → CEN-NN → Fn → RF-xx · RN pendente (T-09) · lacunas Ln |

### 6.2 Casos de teste

Nenhum destes casos foi executado ainda. Cada caso monta os próprios dados e não depende de outro caso.

#### CT-01 · Reservas sobrepostas são rejeitadas para sala, material e professor

| Campo | Conteúdo |
|---|---|
| ID | CT-01 |
| Título | Reservas sobrepostas são rejeitadas para sala, material e professor; adjacentes são aceitas segundo a premissa L1 |
| Pré-condições | 1. Datas e horários são interpretados no fuso configurado para o ambiente de teste. Nesta atividade, o deslocamento usado é UTC-03:00. <br> 2. Existem os recursos avaliados SALA-101 (sala), MAT-001 (material) e PROF-001 (professor) e os recursos auxiliares SALA-AUX-01, SALA-AUX-02, MAT-AUX-01, MAT-AUX-02, PROF-AUX-01 e PROF-AUX-02. Nenhum deles está em manutenção. <br> 3. Existe um usuário autenticado com perfil Solicitante, identificado como SOLICITANTE-001. <br> 4. Antes de cada verificação, o ambiente contém **somente** a reserva-base do conjunto da verificação (tabela "Conjuntos do CT-01"), em 10/11/2026, das 14:00 às 16:00, no estado APROVADA. <br> 5. **Premissa de teste, validação pendente:** "Para este caso, assume-se que uma reserva no estado APROVADA ocupa o intervalo do recurso. A relação completa entre estados e bloqueio do horário permanece na lacuna L2." <br> 6. **Premissa de teste, validação pendente:** "Uma solicitação aceita cria uma reserva no estado SOLICITADA, e esse estado ocupa provisoriamente o recurso e o intervalo. Registros em REJEITADA não ocupam o horário. A relação completa entre estados e ocupação permanece na lacuna L2." Neste caso, reserva ativa = reserva em SOLICITADA ou APROVADA. O caso não assume que o sistema grava registros rejeitados. <br> 7. **Premissa de teste, validação pendente:** os recursos auxiliares estão livres no intervalo e servem só para isolar o tipo de conflito. Em cada conjunto, apenas o recurso avaliado se repete entre a reserva-base e a solicitação. <br> 8. **Pendente (T-09):** a composição obrigatória de uma reserva (quais tipos de recurso ela precisa ter) ainda não está definida. O caso não assume que uma reserva possa ter só uma sala, só um material ou só um professor. |
| Dados de entrada (valores concretos) | 21 solicitações em 10/11/2026, feitas por SOLICITANTE-001, listadas nas tabelas "Conjuntos do CT-01" e "Verificações do CT-01" abaixo. A quantidade (21) e a composição (5 sobreposições e 2 adjacências por recurso avaliado) são **Métrica candidata, validação pendente** |
| Passos | 1. Para cada verificação V01 a V21, restaure o ambiente para conter somente a reserva-base do conjunto da verificação. Isso evita que uma adjacência aceita ou outra execução anterior contamine as verificações seguintes: por exemplo, se a V06 (16:00 às 18:00) fosse aceita e mantida, a V02 (15:00 às 17:00) também se sobreporia a ela. <br> 2. Anote a quantidade de reservas ativas do recurso avaliado em 10/11/2026 (esperado: 1, a reserva-base). Registros em REJEITADA, se existirem, não entram na contagem. <br> 3. Como SOLICITANTE-001, solicite uma reserva com os recursos da linha "Solicitação" do conjunto e o intervalo da verificação. <br> 4. Anote se a solicitação foi aceita ou rejeitada. <br> 5. Anote novamente a quantidade de reservas ativas do recurso avaliado em 10/11/2026, com o mesmo critério do passo 2. <br> 6. Compare com a coluna "Esperado" da tabela. |
| Resultado esperado (mensurável) | 15 de 15 solicitações sobrepostas rejeitadas. Após cada uma, continua existindo exatamente 1 reserva ativa do recurso avaliado em 10/11/2026 (a reserva-base, das 14:00 às 16:00), e nenhuma reserva ativa nova ocupa o intervalo solicitado. Eventuais registros em REJEITADA podem existir, desde que não ocupem o horário. 6 de 6 solicitações adjacentes aceitas **segundo a premissa pendente L1**: passam a existir 2 reservas ativas do recurso avaliado em 10/11/2026 (a reserva-base e a nova, sem sobreposição entre elas). Total: 21 de 21 resultados conforme o esperado. A aceitação das adjacências não é requisito oficial: se L1 determinar que adjacência é sobreposição, as 6 adjacências passam a esperar rejeição (21 rejeitadas e 0 aceitas) |
| Rastreabilidade | CT-01 → CEN-01 → F1 → RF-05 · RN pendente (T-09) · lacunas L1 e L2 |

**Conjuntos do CT-01** (reserva-base: 10/11/2026, 14:00 às 16:00, APROVADA)

| Conjunto | Verificações | Recurso avaliado | Reserva-base | Solicitação |
|---|---|---|---|---|
| Sala | V01 a V07 | SALA-101 | SALA-101 + MAT-AUX-01 + PROF-AUX-01 | SALA-101 + MAT-AUX-02 + PROF-AUX-02 |
| Material | V08 a V14 | MAT-001 | SALA-AUX-01 + MAT-001 + PROF-AUX-01 | SALA-AUX-02 + MAT-001 + PROF-AUX-02 |
| Professor | V15 a V21 | PROF-001 | SALA-AUX-01 + MAT-AUX-01 + PROF-001 | SALA-AUX-02 + MAT-AUX-02 + PROF-001 |

**Verificações do CT-01**

| # | Conjunto | Intervalo solicitado | Forma | Esperado |
|---|---|---|---|---|
| V01 | Sala | 13:00 às 15:00 | Sobreposição parcial no início | Rejeitada |
| V02 | Sala | 15:00 às 17:00 | Sobreposição parcial no fim | Rejeitada |
| V03 | Sala | 14:30 às 15:30 | Nova contida na existente | Rejeitada |
| V04 | Sala | 13:00 às 17:00 | Nova contém a existente | Rejeitada |
| V05 | Sala | 14:00 às 16:00 | Intervalo idêntico | Rejeitada |
| V06 | Sala | 16:00 às 18:00 | Começa quando a existente termina | Aceita (premissa L1) |
| V07 | Sala | 12:00 às 14:00 | Termina quando a existente começa | Aceita (premissa L1) |
| V08 | Material | 13:00 às 15:00 | Sobreposição parcial no início | Rejeitada |
| V09 | Material | 15:00 às 17:00 | Sobreposição parcial no fim | Rejeitada |
| V10 | Material | 14:30 às 15:30 | Nova contida na existente | Rejeitada |
| V11 | Material | 13:00 às 17:00 | Nova contém a existente | Rejeitada |
| V12 | Material | 14:00 às 16:00 | Intervalo idêntico | Rejeitada |
| V13 | Material | 16:00 às 18:00 | Começa quando a existente termina | Aceita (premissa L1) |
| V14 | Material | 12:00 às 14:00 | Termina quando a existente começa | Aceita (premissa L1) |
| V15 | Professor | 13:00 às 15:00 | Sobreposição parcial no início | Rejeitada |
| V16 | Professor | 15:00 às 17:00 | Sobreposição parcial no fim | Rejeitada |
| V17 | Professor | 14:30 às 15:30 | Nova contida na existente | Rejeitada |
| V18 | Professor | 13:00 às 17:00 | Nova contém a existente | Rejeitada |
| V19 | Professor | 14:00 às 16:00 | Intervalo idêntico | Rejeitada |
| V20 | Professor | 16:00 às 18:00 | Começa quando a existente termina | Aceita (premissa L1) |
| V21 | Professor | 12:00 às 14:00 | Termina quando a existente começa | Aceita (premissa L1) |

#### CT-02 · 20 solicitações simultâneas para o mesmo horário geram uma única reserva

| Campo | Conteúdo |
|---|---|
| ID | CT-02 |
| Título | 20 solicitações simultâneas para a mesma sala e o mesmo intervalo resultam em exatamente 1 reserva aceita e ativa |
| Pré-condições | 1. Existe o recurso SALA-102 (sala), fora de manutenção. <br> 2. SALA-102 não possui reserva ativa em 11/11/2026, das 10:00 às 12:00. <br> 3. Existem 20 usuários distintos, identificados como SOLICITANTE-001 a SOLICITANTE-020. <br> 4. Todos possuem perfil Solicitante. <br> 5. Todos possuem agenda livre em 11/11/2026, das 10:00 às 12:00. <br> 6. Todos estão autorizados a criar a solicitação avaliada. <br> 7. O banco de dados está disponível. <br> 8. Imediatamente antes do disparo, não existe reserva ativa para SALA-102 no intervalo. <br> 9. Registros em REJEITADA, caso existam, não ocupam o horário, conforme a **Premissa de teste, validação pendente** associada à lacuna L2: "Uma solicitação aceita cria uma reserva no estado SOLICITADA, e esse estado ocupa provisoriamente o recurso e o intervalo. Registros em REJEITADA não ocupam o horário. A relação completa entre estados e ocupação permanece na lacuna L2." Neste caso, reserva ativa = reserva em estado que ocupa o horário. O caso não assume que o sistema grava registros rejeitados. <br> 10. Datas e horários são interpretados no fuso configurado para o ambiente de teste. Nesta atividade, o deslocamento usado é UTC-03:00. |
| Dados de entrada (valores concretos) | 20 solicitações, uma por usuário: SOLICITANTE-001 envia REQ-001, SOLICITANTE-002 envia REQ-002, e assim sucessivamente, até SOLICITANTE-020, que envia REQ-020. Todas usam os mesmos dados: recurso SALA-102; data 11/11/2026; início 10:00; término 12:00. Os identificadores servem para o teste acompanhar cada solicitação. O caso não determina antecipadamente qual usuário vence a concorrência. A quantidade de 20 solicitações é **Métrica candidata, validação pendente** |
| Passos | 1. Restaure o ambiente para o estado inicial descrito nas pré-condições. <br> 2. Verifique que as agendas de SOLICITANTE-001 a SOLICITANTE-020 estão livres em 11/11/2026, das 10:00 às 12:00. <br> 3. Prepare as 20 solicitações REQ-001 a REQ-020, cada uma no cliente do seu usuário. <br> 4. Faça os 20 clientes aguardarem um ponto comum de sincronização, de forma que nenhuma solicitação seja enviada antes de todas estarem prontas. <br> 5. Libere as 20 solicitações simultaneamente. <br> 6. Colete as respostas de todas as 20 solicitações. <br> 7. Conte quantas respostas foram de aceite e quantas de rejeição. <br> 8. Consulte as reservas ativas que ocupam SALA-102 em 11/11/2026, das 10:00 às 12:00. Registros das solicitações rejeitadas, se existirem, não entram na contagem. <br> 9. Verifique a ausência de duplicidade ativa para esse recurso e intervalo. A contagem de respostas sozinha não basta: as respostas não provam que não houve gravação duplicada por condição de corrida (por exemplo, duas reservas ativas com uma única resposta de aceite) |
| Resultado esperado (mensurável) | Exatamente 1 solicitação aceita. Exatamente 19 solicitações rejeitadas (a forma técnica da rejeição não está definida: lacuna L3). Exatamente 1 reserva ativa que ocupa SALA-102 em 11/11/2026, das 10:00 às 12:00, e 0 duplicidades ativas para esse recurso e intervalo. Todas as agendas dos 20 Solicitantes estavam livres antes do disparo, portanto nenhuma rejeição pode ser atribuída a conflito prévio na agenda dos Solicitantes. Eventuais registros das solicitações rejeitadas não contam como reservas ativas e não podem ocupar o horário. Nenhuma meta de tempo de resposta é avaliada: o caso verifica correção funcional sob concorrência |
| Rastreabilidade | CT-02 → CEN-02 → F2 → RF-06 · RN pendente (T-09) · lacunas L2 e L3 |

**Repetições e sincronização (proposta para a automação futura, ainda não implementada):**

- **Repetições:** 5 execuções independentes, conforme proposta da T-37. **Métrica candidata, validação pendente**. Antes de cada repetição, o ambiente é restaurado por completo ao estado das pré-condições, e cada repetição precisa atingir o resultado esperado.
- **Sincronização:** a estratégia proposta para a futura automação é ExecutorService + CountDownLatch (T-37). A estratégia ainda não foi implementada.
- **Limite de espera pelas respostas:** serve apenas como proteção técnica do teste (para ele não ficar preso indefinidamente) e não é requisito de desempenho. O valor numérico será definido na T-37, conforme o ambiente de execução e o CI.

#### CT-03 · Falha da notificação não desfaz a aprovação nem expõe detalhes internos

| Campo | Conteúdo |
|---|---|
| ID | CT-03 |
| Título | Falha da API de notificações (HTTP 500 ou timeout de leitura) após aprovar uma reserva de recurso restrito preserva a aprovação, gera 1 evento de erro e não expõe detalhes internos |
| Pré-condições | 1. Datas e horários são interpretados no fuso configurado para o ambiente de teste. Nesta atividade, o deslocamento usado é UTC-03:00. <br> 2. Existe o recurso RES-RESTRITO-001, fora de manutenção. <br> 3. **Premissa de teste, validação pendente:** "Para este caso, RES-RESTRITO-001 está previamente configurado com a indicação de recurso restrito. Os critérios de negócio que determinam essa classificação permanecem na lacuna L5." <br> 4. Existe a reserva RESERVA-003 de RES-RESTRITO-001, em 12/11/2026, das 09:00 às 10:00, no estado SOLICITADA, e nenhuma outra reserva desse recurso. <br> 5. Existe um usuário autenticado com perfil Responsável, identificado como RESPONSAVEL-001. <br> 6. A integração de notificações aponta para a API simulada no WireMock. <br> 7. Tempos limite configurados: 500 ms de conexão e 1500 ms de leitura (valores propostos na T-31: **Métrica candidata, validação pendente**). O CT-03 não provoca falha de conexão: o tempo limite de conexão fica só como configuração. Uma falha de conexão poderá ser coberta por um caso futuro, se continuar no escopo. <br> 8. **Premissa de escopo do cenário:** a aprovação da reserva é o evento que dispara a notificação. |
| Dados de entrada (valores concretos) | Reserva RESERVA-003 (12/11/2026, 09:00 às 10:00); recurso RES-RESTRITO-001; usuário RESPONSAVEL-001; operação: aprovar RESERVA-003. Variação A: o WireMock responde HTTP 500 à notificação. Variação B: o WireMock aceita a conexão e atrasa a resposta em 2000 ms, mais que o tempo limite de leitura de 1500 ms. O atraso de 2000 ms é **Métrica candidata, validação pendente** |
| Passos | Execute as variações A e B separadamente. Antes de cada uma, restaure as pré-condições (RESERVA-003 em SOLICITADA e nenhum evento de erro registrado para ela). <br> 1. Configure o WireMock com o comportamento da variação (A: responder HTTP 500; B: aceitar a conexão e atrasar a resposta em 2000 ms). <br> 2. Como RESPONSAVEL-001, aprove a reserva RESERVA-003. <br> 3. Guarde a resposta mostrada ao usuário. <br> 4. Consulte o estado de RESERVA-003 e a quantidade de reservas de RES-RESTRITO-001. <br> 5. Conte os eventos de erro observáveis associados a RESERVA-003 e anote a causa de cada um. <br> 6. Procure na resposta guardada no passo 3: stack trace, nome de classe, consulta SQL, caminho interno e o corpo bruto do erro externo. |
| Resultado esperado (mensurável) | Em cada variação: a aprovação é preservada (RESERVA-003 continua APROVADA); exatamente 1 reserva de RES-RESTRITO-001 continua registrada; 0 reversões causadas pela falha da notificação; exatamente 1 evento de erro observável associado a RESERVA-003, com a causa "HTTP 500" (variação A) ou "timeout" (variação B); 0 respostas visíveis ao usuário com stack trace, nome de classe, consulta SQL, caminho interno ou corpo bruto do erro externo. A preservação da reserva é **Decisão de projeto proposta, validação pendente** (lacuna L8). O caso não define se o evento fica em log, auditoria ou banco, e não pressupõe retentativa, fila ou deduplicação |
| Rastreabilidade | CT-03 → CEN-03 → F7 → RF-11 · RF-07 (evento de disparo da notificação) · RN pendente (T-09) · lacunas L5 e L8|

## 7. Ponto de sensibilidade

**Parâmetro:** o mecanismo de controle de concorrência na persistência das reservas. **Atributo afetado:** Adequação funcional / Correção funcional (F2, CEN-02). Pequenas variações nessa decisão (o nível de isolamento das transações, o uso ou não de bloqueio e a existência ou não de uma restrição de unicidade ou de exclusão no banco) mudam muito o resultado do CEN-02. Com um mecanismo adequado, as 20 solicitações simultâneas do CT-02 resultam em exatamente 1 reserva ativa ocupando o recurso e o intervalo. Com um mecanismo fraco, duas ou mais solicitações podem passar pela verificação de conflito antes de qualquer uma gravar, e o sistema mantém mais de uma reserva ativa para o mesmo recurso e intervalo, o que viola o RF-06 e é limitador de nota. Nenhum desses mecanismos está implementado ainda: a solução concreta depende da definição arquitetural, prevista na T-37 (proteção contra dupla reserva), que prevê um ADR curto caso a proteção no banco seja necessária.

## 8. Ponto de tradeoff

**Propriedade:** os tempos limite da integração de notificações (500 ms de conexão e 1500 ms de leitura, propostos na T-31: **Métrica candidata, validação pendente**). **Atributos em conflito:** Confiabilidade / Tolerância a falhas (F7, CEN-03) e Eficiência de desempenho / Comportamento temporal. Um tempo limite maior dá mais oportunidade para uma API lenta responder e reduz as falhas de notificação, o que favorece a confiabilidade, mas prolonga a espera enquanto a API não responde, o que piora o comportamento temporal. Um tempo limite menor reduz a espera, mas passa a classificar como falha respostas que só estavam lentas, o que piora a confiabilidade da notificação. Não existe um valor que melhore os dois atributos ao mesmo tempo: escolher o número é escolher qual dos dois ceder. **Premissa para o conflito existir:** a chamada à API de notificações acontece dentro da operação que o usuário espera terminar (por exemplo, a aprovação). Se a arquitetura enviar a notificação fora dessa operação, o tempo limite deixa de afetar a espera do usuário, e o tradeoff precisa ser reavaliado.

## 9. Não-risco

**Decisão:** preservar a reserva quando a notificação falha (CEN-03, CT-03), ainda como **Decisão de projeto proposta, validação pendente** (lacuna L8). A decisão é adequada porque separa a falha de uma dependência externa do resultado da operação principal: uma API de notificações fora do ar não desfaz uma aprovação válida, e a falha fica visível como evento de erro associado à reserva. Ela é um não-risco enquanto a premissa abaixo for verdadeira. Se a premissa mudar (por exemplo, se a entrega da notificação passar a ser exigida para a reserva valer), a decisão deverá ser reavaliada, e a preservação da reserva pode virar um risco.

- **Premissa que precisa continuar verdadeira:** "A notificação é um efeito secundário e sua entrega não é condição para a validade da reserva."

## 10. Matriz de rastreabilidade

| Folha | Cenário | Caso de teste | Atributo / refinamento | (I, D) | Origem | Status |
|---|---|---|---|---|---|---|
| F1 | CEN-01 | CT-01 | Adequação funcional / Correção funcional | (A, A) | RF-05 · RN pendente (T-09) · lacunas L1 e L2 | Planejado, não executado |
| F2 | CEN-02 | CT-02 | Adequação funcional / Correção funcional | (A, A) | RF-06 · RN pendente (T-09) · lacunas L2 e L3 | Planejado, não executado |
| F7 | CEN-03 | CT-03 | Confiabilidade / Tolerância a falhas | (A, A) | RF-11 · RF-07 (evento de disparo da notificação) · RN pendente (T-09) · lacunas L5 e L8 | Planejado, não executado |

## 11. Registro da revisão

| Achado | Origem | Revisor | Autor do conteúdo avaliado | Resolução | Situação |
|---|---|---|---|---|---|
| REV-04 | Revisão do PR #3 · issue #4 | Samuel (@Samwel-Merces) | Caio (@CaiosAguiar) | REV-04: resolvido pela adoção de 20 usuários distintos, todos com agenda livre, com solicitações diferentes para a mesma SALA-102 e intervalo (CEN-02 e CT-02) | Ajuste aplicado, aguardando confirmação do revisor |
