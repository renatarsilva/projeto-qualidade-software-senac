# Diagnóstico de Qualidade — Semana 32

## 1. Objetivo

Diagnóstico inicial de qualidade (Atividade #1, T-03): executar a suíte oficial do projeto de referência, inspecionar código, configuração e documentação e priorizar os problemas segundo a ISO/IEC 25010. Usamos **falha** para problema observado em execução, **defeito** para problema no artefato identificado por inspeção e **risco** para consequência possível ainda não reproduzida.

## 2. Escopo e objeto analisado

- **Repositório:** `afonsolelis/qualidade_software`, branch `main`.
- **Commit:** `d090289e7d6eb7a07d597d62c785ba6fa27ea256`.
- **Incluído:** README, CHANGELOG, `pom.xml`, `.gitignore`, configuração, código de `src/main`, templates, os 2 testes de `src/test` e o histórico Git.
- **Fora do escopo:** branches de aulas posteriores.

Nenhum código do projeto avaliado foi corrigido, nenhum teste foi criado e a aplicação não foi iniciada.

## 3. Ambiente e procedimento

Windows 11 Pro, OpenJDK 21.0.6 (o projeto declara Java 17 ou superior), Maven Wrapper 3.9.5, sem Maven global. Comando: `.\mvnw.cmd clean test`.

1. **Tentativa 1:** impedida pela ausência de `JAVA_HOME` (`Error: JAVA_HOME not found in your environment.`).
2. **Tentativa 2:** `JAVA_HOME` definido somente no processo, sem alteração permanente no sistema ou no repositório. Encerrada com erro no `HomeControllerTest`.

## 4. Resultado da execução

| Testes executados | Aprovados | Falhas de asserção | Erros | Ignorados | Resultado |
|---|---|---|---|---|---|
| 2 | 1 | 0 | 1 | 0 | `BUILD FAILURE` |

Trecho preservado da saída observada na execução original (tentativa 2 da seção 3):

```text
Tests run: 2, Failures: 0, Errors: 1
BUILD FAILURE
```

É só um trecho preservado da execução original, não uma nova execução. O log completo não foi arquivado no repositório. O trecho registra 2 testes, 0 falhas de asserção, 1 erro de execução e o encerramento do build com falha.

O aprovado é `contextLoads()`, sem asserções. O erro é do `HomeControllerTest.shouldReturnHomeView`: `No qualifying bean of type 'MockMvc' available`. A cobertura não foi medida.

## 5. Achados

### ACH-01

**Credencial de banco em arquivos rastreados e no histórico**

- **Localização:** `src/main/resources/application.properties`, `README.md`, `.gitignore:51`.
- **Tipo:** defeito, confirmado por inspeção.
- **Evidência:** uma credencial foi encontrada em arquivos rastreados; `git log -S` a encontra também em commits anteriores; `git ls-files -ci --exclude-standard` mostra o arquivo rastreado apesar de listado no `.gitignore`.
- **Impacto:** quem acessa o repositório pode obter acesso aos dados, caso a credencial ainda esteja válida. Apagar apenas o arquivo atual não elimina a exposição histórica.
- **Limites da verificação:** a exposição da credencial foi observada; a validade da credencial não foi testada e nenhum acesso aos dados foi realizado. A equipe evitou testar a credencial para não produzir acesso indevido ou não autorizado.
- **ISO/IEC 25010:** Segurança (confidencialidade e integridade).
- **Severidade:** Crítica. **Prioridade:** P1.
- **Recomendação:** primeiro revogar ou rotacionar a credencial, ação que deve ser realizada pelo responsável pelo recurso; depois remover o segredo dos arquivos atuais e avaliar a limpeza do histórico.
- **Status:** exposição confirmada por inspeção; validade não testada. Aviso ao professor por canal privado: pendente.

### ACH-02

**Erro de preparação do `HomeControllerTest` por ausência do `MockMvc`**

- **Localização:** `src/test/java/.../controller/HomeControllerTest.java`, linhas 5, 13 e 16-17.
- **Tipo:** defeito na configuração do teste que, executado, produziu falha observada.
- **Evidência:** o teste usa `@AutoConfigureWebMvc`, que não registra o bean `MockMvc`. A execução terminou em erro de preparação (`NoSuchBeanDefinitionException`), não em falha de asserção, e o build em `BUILD FAILURE`. O `CHANGELOG.md` descreve a correção, que não está no código.
- **Impacto:** o único teste de controlador não chega a executar e o build fica bloqueado.
- **ISO/IEC 25010:** Manutenibilidade (testabilidade).
- **Severidade:** Alta. **Prioridade:** P1.
- **Recomendação:** aplicar a correção documentada no `CHANGELOG.md`.
- **Status:** confirmado por execução.

### ACH-03

**Suíte sem testes de negócio e sem configuração de teste isolada**

- **Localização:** `src/test/` (2 arquivos); `EducationalQualityProjectApplicationTests.java:9-11`.
- **Tipo:** dois defeitos confirmados por inspeção e um risco.
- **Evidência:**
  - nenhum teste cobre serviços, repositórios, controladores de cadastro ou os 10 endpoints REST do README, nem casos negativos (defeito);
  - não há `src/test/resources`, `@ActiveProfiles` nem `@TestPropertySource`, então os testes carregam a configuração principal (defeito).
- **Impacto:** regressões no cadastro passam despercebidas. **Risco:** a suíte pode depender de um banco externo e compartilhado. **Não determinado:** se houve conexão efetiva com o banco; o log não registra sucesso nem falha de conexão.
- **ISO/IEC 25010:** Manutenibilidade (testabilidade).
- **Severidade:** Alta. **Prioridade:** P2.
- **Recomendação:** testes de serviço e de API, com casos negativos, sobre banco isolado.

### ACH-04

**Ausência de validação de entrada e regra de unicidade inconsistente**

- **Localização:** `StudentController.java:32-36` e `:46-51`; `TeacherController.java:32-36`; `StudentApiController.java:47-58`; `Student.java`, `Teacher.java`.
- **Tipo:** defeito confirmado por inspeção; duplicação de dados é risco não reproduzido.
- **Evidência:** não há `@Valid`, Bean Validation nem índice único. A API verifica e-mail e matrícula duplicados antes de salvar; o formulário web salva direto. O `required` de `form.html` vale só no navegador.
- **Impacto:** risco de registros duplicados ou sem nome e e-mail.
- **ISO/IEC 25010:** Adequação Funcional (correção funcional).
- **Severidade:** Alta. **Prioridade:** P2.
- **Recomendação:** validar no servidor e garantir a unicidade com índice único, igual nos dois canais.

### ACH-05

**Sem autenticação e autorização, CORS aberto e exclusão por GET**

- **Localização:** `StudentApiController.java:15`, `TeacherApiController.java:15`, `StudentController.java:53-57`, `TeacherController.java:53-57`, `pom.xml`.
- **Tipo:** defeitos confirmados por inspeção; acesso indevido é risco não materializado.
- **Evidência:** nenhuma dependência de segurança no `pom.xml` (ausência de autenticação e autorização); `@CrossOrigin(origins = "*")` (CORS aberto); `@GetMapping("/{id}/delete")` (exclusão por GET).
- **Impacto:** se publicada, a aplicação ficaria sujeita à exposição de dados pessoais e a exclusões indevidas. A aplicação não foi iniciada e nenhum ataque ou acesso indevido foi reproduzido.
- **ISO/IEC 25010:** Segurança (confidencialidade, integridade e autenticidade).
- **Severidade:** Alta. **Prioridade:** P2.
- **Recomendação:** autenticação e autorização, CORS restrito e exclusão por POST ou DELETE com proteção CSRF.

## 6. Priorização

**Critério:** impacto × exposição × custo de manter o problema.

1. **ACH-01 (P1):** dano crítico e exposição já existente no histórico.
2. **ACH-02 (P1):** falha observada que bloqueia o build, com correção conhecida.
3. **ACH-05 (P2):** dados pessoais sem barreira, mas o dano depende de publicação.
4. **ACH-04 (P2):** integridade do dado central, fácil de afetar pelo formulário.
5. **ACH-03 (P2):** permite que os demais defeitos passem sem detecção, sem causar dano direto.

## 7. Limitações

- Executado apenas `clean test`; a aplicação não foi iniciada para não usar a credencial remota.
- JDK 21 em vez do 17 declarado.
- Cobertura e desempenho não medidos.
- ACH-03 a ACH-05 vêm de inspeção; nenhuma consequência foi reproduzida.
- Não foram determinadas a validade da credencial nem a conexão efetiva com o banco.

## 8. Conclusão

A suíte oficial não sustenta a qualidade do projeto: dos 2 testes, 1 termina em erro de preparação e o outro não tem asserções, e o build termina em `BUILD FAILURE`. O problema mais grave não aparece na execução: uma credencial de banco está em arquivos rastreados e no histórico. A ordem recomendada é rotacionar a credencial, corrigir o `HomeControllerTest` e criar testes de negócio com banco isolado.

## 9. Autoria e rastreabilidade

- **Responsável original no planejamento:** Renata (`docs/planejamento/backlog.md`, T-03; `docs/planejamento/sprints.md`, Sprint 1).
- **Execução do diagnóstico e autoria do conteúdo:** Caio (@CaiosAguiar), commit `aa3717e`.
- **Revisão:** Samuel (@Samwel-Merces), aprovação no PR #1 em 23/10/2026, conforme evidência do PR.
- **Rastreabilidade:** task T-03, commit `aa3717e`, PR #1 e issue #2 (ajustes pós-revisão). O Jira saiu do fluxo ([ADR-0003](../adr/0003-retirar-o-jira-do-fluxo.md)), por isso a T-03 não tem chave Jira.
