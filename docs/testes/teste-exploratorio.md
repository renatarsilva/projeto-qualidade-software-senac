# Teste exploratório: o que um usuário leigo ou o professor tentariam

> Executado à mão em 2026-11-12 na aplicação no ar, a partir de um **clone limpo** da `main` (commit `a26e4cc`, tag `v1.0.0-rc3` no `cdec55f`), subida com `./scripts/executar.sh`. Telas e API, com sessão e CSRF reais. É complementar aos 1043 testes automatizados: busca o que ninguém escreveu como caso de teste. **171 verificações: 166 passaram e 5 foram marcadas como "achado" pelo roteiro.** Conferidas uma a uma, as 5 são: 1 defeito real (DEF-05), 2 observações (página "não encontrado" para recurso inexistente) e 2 erros do próprio roteiro. Um segundo defeito real (DEF-06) saiu da conferência do `/error`, que o roteiro tinha contado como aprovado. Não roda no CI.

## 1. O que foi tentado

| Grupo | O que se tentou | Resultado |
|---|---|---|
| Login | Senha errada, e-mail inexistente, injeção de SQL, campos vazios, 7 telas protegidas sem login | Tudo recusado, mesma mensagem, sem pista |
| Sala | Nome repetido (também em maiúsculas), 9 nomes inválidos, capacidade 0, negativa, texto, decimal, número gigante, `<script>`, 500 caracteres, emoji | Recusados com mensagem clara; o que foi aceito aparece escapado |
| Professor e material | Duplicados, nome fora do padrão, competência repetida (Java, java), patrimônio repetido, tudo vazio | Recusados |
| Reserva: horários | Duplo clique, outra pessoa na mesma sala, sobreposição pela metade, horário que engloba o outro, término antes ou igual ao início, no passado, 30 minutos de antecedência, sem recurso, recurso mal formado, dado em texto | Recusados com a mensagem da regra. Horários que **encostam** foram aceitos, como manda o `[início, fim)` |
| Professor e material na reserva | Mesmo professor em duas salas no mesmo horário, mesmo material, tudo-ou-nada | Recusados; a sala ficou livre |
| Ciclo de vida | Aprovar duas vezes, rejeitar aprovada, cancelar duas vezes, aprovar cancelada, check-in fora do horário, cancelar libera o horário | Coerente, com mensagem clara |
| Perfis | Solicitante e Administrador aprovando; ver, alterar ou cancelar a reserva de outro | 403 em todos |
| Manutenção | Reservar em manutenção, manutenção sobreposta, sem motivo, motivo de 300 caracteres | Recusados |
| Usuários | E-mail repetido ou inválido, senhas de 0, 5, 7, 73 e 200 caracteres, perfil inexistente, HTML no nome, desativar ou rebaixar o único administrador, desativado com a sessão aberta | Todos tratados; o desativado perde a sessão na hora |
| Exclusão | Sala (403), professor com reserva (409), sem vínculo (204), de novo (404), material com reserva (409), sem token (401), como Solicitante (403) | Como esperado |
| Pesquisa e relatório | Filtros inválidos, de intervalo invertido, com SQL e `<script>`, períodos absurdos | Sem erro técnico, exceto o DEF-05 |
| Segurança | CSRF, `/actuator`, `/.env`, `/.git/config`, `/h2-console`, token falso ou adulterado, JSON quebrado, campo de 100 mil caracteres, path traversal, cabeçalho `Host` falso | Nada vazou e a aplicação seguiu respondendo; exceto o DEF-06 (cosmético) |

## 2. Defeitos encontrados

| ID | O que | Gravidade | Estado |
|---|---|---|---|
| [DEF-05](../defeitos/registro.md) | Relatório pela tela com data final `9999-12-31` dá 500 | Baixa | Aberto, risco aceito |
| [DEF-06](../defeitos/registro.md) | `/error` aberto direto mostra "500 Erro inesperado" | Baixa | Aberto, risco aceito |

Decisão de 2026-11-12: **não corrigir às vésperas da entrega**, porque nenhum dos dois toca uma regra crítica, expõe dado ou perde informação. As correções previstas estão no registro.

## 3. Observações que não são defeito

- **Reserva sem limite de duração nem de antecedência máxima:** a de 29 dias seguidos e a do ano 9999 foram aceitas. O PRD não define esses limites e não inventamos regra de negócio.
- **Pedir uma sala que não existe** (só forçando o formulário) mostra a página "não encontrado", e não uma mensagem no formulário. As telas só oferecem recursos que existem.
- **"Conflitos evitados" conta pelo dia em que a tentativa aconteceu**, e não pelo dia da reserva. No relatório de hoje, os pedidos recusados do teste apareceram (9); no de "amanhã", 0. É a definição da spec T-41.
- **Mensagem genérica** "Esta operação conflita com outro registro" quando a mesma sala é marcada duas vezes no mesmo pedido: clara o bastante, mas pouco específica.

## 4. Os falsos alarmes do roteiro (conferidos)

Dois "achados" eram erro do próprio teste: (a) a pesquisa de disponibilidade parecia mostrar uma sala ocupada, mas o horário medido tinha ficado livre depois de uma alteração; ao medir o horário certo (11:00 a 12:00), a sala ocupada **não** aparece; (b) o relatório de "amanhã" mostrou 0 conflitos pelo motivo da seção 3. Outros dois (recurso inexistente e `SALA:-1`) são a observação da seção 3, e não defeito.

## 5. O que este teste não cobre

A aparência em celular real (o Chrome em 375 px está em [`e2e/`](../../e2e/README.md)), o Windows, carga acima da do JMeter e acessibilidade (leitor de tela, contraste).
