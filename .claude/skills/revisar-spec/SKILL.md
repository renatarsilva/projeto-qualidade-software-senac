---
name: revisar-spec
description: Apoia o revisor na revisão por pares de uma spec deste projeto (passo 2 do ai-workflow), levantando pontos com base no checklist da seção 9. Use quando alguém disser que vai revisar a spec de um colega.
---

# Revisar spec (passo 2 do ai-workflow)

Fale em português. Você está ajudando o **revisor**, não o autor.

1. Pergunte qual spec será revisada e confirme, pelo campo `Revisor(a)`, que quem está pedindo é o revisor. Se for o próprio autor, explique que ele não pode revisar a própria spec.
2. Leia a spec, o PRD (`docs/prd.md`, se existir), `docs/projeto/contexto-da-disciplina.md`, a arquitetura e os ADRs (`docs/adr/`) e as outras specs em `docs/specs/`.
3. Passe pelo checklist da seção 9 e apresente uma lista de **pontos para o revisor avaliar**, cada um com o trecho da spec e o motivo. Priorize:
   - critério de aceite que não dá para verificar;
   - valor limite ou entrada inválida sem caso de teste;
   - erro 401, 403 ou 409 sem tratamento;
   - regra que contradiz o PRD, um ADR, outra spec ou uma regra crítica da disciplina;
   - escopo grande demais para uma task.
4. **Não preencha o parecer.** O revisor escreve os comentários e o parecer na seção 9 com as próprias palavras, porque isso é evidência de revisão por pares e assunto da prova oral. Você pode revisar o texto dele.
5. Sugira a mensagem de commit `docs(spec): revisa spec de <assunto> (<CHAVE>)`. Nunca faça commit nem push.
