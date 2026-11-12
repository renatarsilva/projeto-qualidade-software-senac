# Status dos requisitos (entrega, 2026-11-11)

> Task **T-39**. O estado de cada requisito oficial, com a evidência e o que ficou pendente. Fonte dos números: `./mvnw verify` na `main` (1043 testes, 0 falhas) e [RTM](RTM.md). Escrito com franqueza: o que não foi entregue está dito.

Legenda: **Entregue** (código, testes e evidência), **Parcial** (a parte funcional está entregue e uma parte não), **Não entregue**.

## Requisitos funcionais (RF-01 a RF-14)

| Requisito | Estado | O que está entregue | Pendência | Evidência |
|---|---|---|---|---|
| RF-01 Autenticação e perfis | **Entregue** (API e telas) | Login com token de 30 min, BCrypt, 3 perfis, perfil conferido no banco a cada chamada, negado por padrão, matriz de autorização testada célula a célula | Telas de login com sessão e CSRF | [RTM](RTM.md), [matriz](seguranca/matriz-autorizacao.md) |
| RF-02 Cadastros | **Entregue** | Salas, professores (com competências) e materiais: cadastro, alteração, consulta e exclusão com 409 em caso de vínculo | | RTM, testes das specs T-16 a T-18 |
| RF-03 Pesquisa | **Entregue** | Tipo, capacidade, localização, competência e disponibilidade (exclui reservas ativas e manutenção) | | `PesquisaDeRecursosApiTest` |
| RF-04 Criar, alterar e cancelar reservas | **Entregue** | Intervalo válido, antecedência de 1 h, alteração com prazo, cancelamento em três formas, check-in | Job que aplica as mudanças por tempo (T-60) | `IntervaloDaReservaTest`, `CicloDeVidaApiTest` |
| RF-05 Detecção de sobreposição | **Entregue** | Sala, material e agenda do professor, tudo ou nada, `[início, fim)` | | `SobreposicaoNoMesmoRecursoTest` |
| RF-06 Dupla reserva | **Entregue** | `EXCLUDE` do banco, ordem fixa dos recursos, deadlock traduzido para 409; 20 solicitantes simultâneos: 1 reserva | | [T-37](evidencias/T-37-verde.md), [carga](performance/resultado-01.md), ADR-0010 |
| RF-07 Aprovação de recurso restrito | **Entregue** | Só o Responsável aprova e rejeita, em qualquer reserva; prazo de aprovação | Nenhuma | `AutorizacaoDoPatchApiTest`, `aprovacao.feature` (5 cenários) |
| RF-08 Bloqueio por manutenção | **Entregue** | Registro e encerramento pelo Administrador, reserva recusada com fronteiras, reservas afetadas listadas, corrida resolvida por bloqueio de linha | | `ManutencaoDeRecursosApiTest` |
| RF-09 Retirada e devolução | **Entregue** | Só o Responsável registra; só de reserva aprovada; devolução atrasada sinalizada | | `RetiradaDeMateriaisApiTest` |
| RF-10 Histórico auditável | **Entregue** | Registro por mudança de estado, na mesma transação, imutável no banco; consulta só para Responsável e Administrador | | `Auditoria*Test` |
| RF-11 Notificação | **Entregue** | Aprovação, rejeição e cancelamento notificam o dono; adaptador HTTP com timeouts configuráveis e modo simulado; a falha não desfaz a reserva | | `NotificadorHttpWireMockTest` (INT-01 a INT-05) |
| RF-12 Relatório de utilização | **Entregue** | Horas por recurso, carga horária por professor e conflitos evitados; oráculo calculado à mão | | `RelatorioDeUtilizacaoApiTest` |
| RF-13 Interface responsiva e erros compreensíveis | **Entregue** | Telas Thymeleaf e Bootstrap dos três perfis (login por sessão com CSRF, menu por perfil, pesquisa, reserva, aprovação, retirada, cadastros, manutenção e relatório); erros em linguagem clara, sem detalhe interno | Edição de sala, material e professor só pela API; o teste do navegador roda à mão, e não no CI | `LoginEMenuDasTelasTest`, `AutorizacaoDasTelasTest`, `FluxoCompletoPelasTelasTest`, `ErrosDaApiTest`; [T-44-telas.md](evidencias/T-44-telas.md) |
| RF-14 Documentação da API | **Entregue** | OpenAPI com Swagger UI público, todos os endpoints com perfis permitidos e erros | | `DocumentacaoDaApiTest`, README |

## Regras críticas da especificação

| Regra | Estado | Evidência |
|---|---|---|
| O término deve ser posterior ao início | Entregue | `IntervaloDaReservaTest` |
| Sem sobreposição no mesmo recurso | Entregue | `SobreposicaoNoMesmoRecursoTest`, EXCLUDE do banco |
| Solicitações simultâneas geram uma só reserva | Entregue | [T-37](evidencias/T-37-verde.md) |
| Recursos em manutenção não podem ser reservados | Entregue | `ManutencaoDeRecursosApiTest` |
| Estados `SOLICITADA → APROVADA → EM_USO → CONCLUIDA` e alternativos | Entregue (regras e endpoints); a mudança **automática** por tempo (job) ficou pendente | `MatrizDeTransicoesTest` |
| Só o Responsável aprova recurso restrito | Entregue (decisão do time: o Responsável aprova qualquer reserva) | `AutorizacaoDoPatchApiTest` |
| Reserva iniciada não se apaga | Entregue (nenhuma reserva é apagada) | `ExclusaoDeReservaTest` |
| Toda mudança de estado gera auditoria | Entregue | `AuditoriaDasMudancasTest` |

## Requisitos não funcionais

| Requisito | Meta | Resultado | Estado |
|---|---|---|---|
| RNF-01 Cobertura de linhas | ≥ 80 % | **96,0 %** (JaCoCo) | Atendido |
| RNF-02 Cobertura de branches | ≥ 70 % | **85,2 %** (JaCoCo) | Atendido |
| RNF-03 Rastreabilidade | 100 % dos críticos | RN-01 a RN-09 todas com evidência na [RTM](RTM.md) | Atendido |
| RNF-04 Defeitos críticos | 0 | SonarCloud da `main`: 0 bugs, 0 vulnerabilidades, Quality Gate aprovado | Atendido |
| RNF-05 Dupla reserva | 1 aceita, 0 duplicidades | 1 aceita e 0 duplicidades, em teste e em carga | Atendido |
| RNF-06 Autorização por perfil | 0 operações não autorizadas | 176 casos da matriz, 0 falhas | Atendido |
| RNF-07 Validação de entrada | 0 gravações inválidas | Testes de validação e restrições do banco | Atendido |
| RNF-08 Erros seguros | 0 respostas com detalhe interno | Lista de termos proibidos em todos os testes de erro | Atendido |
| RNF-09 Desempenho | p95 ≤ 300 ms, erro < 1 % | p95 de 2 a 7 ms, 0 erro real ([resultado](performance/resultado-01.md)) | Atendido |
| RNF-10 Resiliência da notificação | 0 reservas desfeitas | A falha do aviso não desfaz a aprovação | Atendido |
| RNF-11 Responsividade | utilizável a partir de 375 px | 14 páginas sem rolagem horizontal em 375 px no Chrome real | **Atendido** |

## Entregáveis de qualidade da especificação

| Item | Estado |
|---|---|
| JUnit 5: unitário, parametrizado e de integração | Entregue |
| Testes de API caixa-preta e teste de concorrência | Entregue |
| Testcontainers (PostgreSQL real) e WireMock | Entregue |
| JMeter (RNF, plano, execução, resultado) | Entregue |
| GitHub Actions em PR, JaCoCo com gate 80/70, SonarCloud | Entregue (Quality Gate aprovado) |
| TDD e BDD com evidência | Entregue: TDD ([evidencia-tdd.md](testes/evidencia-tdd.md)) e BDD executável com Cucumber, 17 cenários ([bdd-aprovacao-bloqueio.md](testes/bdd-aprovacao-bloqueio.md)) |
| Mutação manual nas regras críticas | Entregue ([mutacao-manual.md](testes/mutacao-manual.md)) |
| Teste end-to-end | **Entregue no CI**: `FluxoPorHttpRealTest` (servidor de verdade, HTTP, sessão e CSRF). Também `e2e/fluxo-no-navegador.js` (Chrome real, 31 verificações), à mão |
