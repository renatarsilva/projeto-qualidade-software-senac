---
name: grill-with-docs
description: Sabatina uma spec já validada cruzando-a com toda a documentação do projeto, uma pergunta por vez, e registra as decisões (passo 4 do ai-workflow, obrigatório para regras críticas). Use quando alguém pedir o grill de uma spec.
---

# Grill-with-docs (passo 4 do ai-workflow)

Fale em português. O objetivo é encontrar furos **antes** do código e treinar o dono da task para a prova oral.

## Antes de começar

1. Confirme que a spec está com status `Validada` (rodou `./scripts/validar-spec.sh` com sucesso). Se não estiver, mande voltar ao passo 3.
2. Leia, nesta ordem: a spec; `docs/projeto/contexto-da-disciplina.md` (regras críticas e limitadores de nota); `docs/prd.md`; `docs/arquitetura.md` e `docs/adr/`; as outras specs em `docs/specs/`; `docs/rtm.md` e `docs/plano-de-teste.md`, se existirem.
3. Monte, sem mostrar, uma lista de 6 a 10 perguntas, cobrindo:
   - **coerência:** a spec contradiz o PRD, um ADR ou outra spec?
   - **bordas:** intervalos iguais, fuso horário, campos nulos, limites de capacidade;
   - **concorrência e estado:** o que acontece com duas requisições ao mesmo tempo, ou em um estado inesperado?
   - **segurança:** quem pode chamar? o que um perfil errado recebe? a mensagem de erro vaza algo?
   - **testabilidade:** como provar cada critério? com que nível de teste? o teste falharia se a regra quebrasse?
   - **rastreabilidade:** qual linha da RTM este trabalho preenche?

## Durante

- Faça **uma pergunta por vez** e espere a resposta. Cite o trecho do documento que motivou a pergunta.
- Se a resposta for boa, diga em uma frase por quê e siga. Se for fraca, aprofunde com mais uma pergunta antes de seguir.
- Quem responde e decide é o dono da task. Não responda por ele; se ele travar, dê uma pista.

## Depois

1. Registre na seção 11 da spec uma linha por pergunta: pergunta, resposta ou decisão e o documento afetado.
2. Se alguma decisão muda a spec, o PRD ou a arquitetura, liste o que precisa ser editado. A edição da regra é feita pelo dono da task.
3. Defina o resultado:
   - **Liberada:** nenhuma pendência. O status da spec vira `Liberada`.
   - **Pendências:** liste-as. Mudanças de regra exigem nova revisão do revisor.
4. Sugira a mensagem de commit `docs(spec): registra grill-with-docs de <assunto> (<CHAVE>)`. Nunca faça commit nem push.
