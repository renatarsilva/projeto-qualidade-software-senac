# Evidência T-49: a matriz de autorização como teste parametrizado

> Spec [T-49](../specs/T-49-matriz-de-autorizacao-como-teste.md); matriz em [`docs/seguranca/matriz-autorizacao.md`](../seguranca/matriz-autorizacao.md). Produzida em 2026-11-10.

## Resultado

```bash
./mvnw test -Dtest='MatrizDeAutorizacaoApiTest,PosseEOrdemDosErrosApiTest,RotasForaDaMatrizApiTest,EntradaInvalidaSemVazamentoApiTest,CompletudeDaMatrizTest,PerfilDoBancoNaHoraApiTest'
```

`Tests run: 176, Failures: 0, Errors: 0`. O teste percorre cada célula da matriz com usuários e tokens reais de cada perfil, com dados novos a cada caso.

| Caso | O que prova | Testes |
|---|---|---|
| TC-01, TC-02 | Sem token ou com token adulterado, toda operação devolve 401 | 52 |
| TC-03 | Cada célula 403 da matriz devolve 403, sem detalhes internos | parametrizado |
| TC-04 | Cada célula de sucesso devolve o status da matriz (200, 201, 204 ou 409) | parametrizado |
| TC-05, TC-06 | Posse da reserva e ordem dos erros (401, 403, 404, 400) | 6 |
| TC-07 | Rotas fora da matriz: 403 para todo perfil e 401 sem token | 36 |
| TC-08, TC-09 | Entrada inválida: 400 com campo e motivo, sem vazamento de detalhes | 6 |
| TC-10 | Toda rota `/api` mapeada na aplicação tem uma linha na matriz | 1 |
| TC-11 | O perfil vem do banco a cada chamada (rebaixar e desativar valem na hora) | 1 |

**Defeitos encontrados:** nenhum. Todas as células do código batiam com a matriz. As três falhas da primeira execução eram erros do próprio teste (rota de teste na completude, token expirado depois de avançar o relógio e um corpo repetido), corrigidos antes de qualquer conclusão sobre o sistema.

## Mutação: o teste pega uma regra errada

Para provar que o teste não é decorativo, a regra do `PUT /api/salas` foi aberta ao Solicitante (`SalaAcessoConfiguracao`, perfil `SOLICITANTE` acrescentado). Resultado: `MatrizDeAutorizacaoApiTest` falhou por asserção em `celulaDeNegacao` (célula **S-04 por Solicitante**: esperado 403). A regra foi restaurada e a suíte voltou a ficar verde. A T-50 pode reaproveitar esta mutação.
