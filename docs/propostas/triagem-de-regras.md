# Triagem das regras de negócio propostas

> Task **T-19** · Autoria: Renata (@renatarsilva) · Para o Caio (@CaiosAguiar) registrar no [PRD](../prd.md)
>
> Resultado da triagem de uma planilha com 45 regras. **Todas as regras abaixo são propostas do time, com validação pendente.** As regras oficiais, do professor, são só as RN-01 a RN-09.

## 1. Origem e como ler

- A planilha tinha 45 regras. As RN-01 a RN-13 já estão no [plano TDD](../testes/plano-tdd.md) (RN-10 a RN-13 como proposta). As **32 restantes (RN-14 a RN-45)** foram triadas aqui.
- Os IDs da planilha foram mantidos, para quem a conhece. São IDs **propostos**: o PRD pode renumerar.
- **"Usuário comum" é o perfil Solicitante.** Os três perfis do sistema são Solicitante, Responsável e Administrador.
- Critério: entra na v1 o que é pequeno, testável e não conflita com o que já foi decidido.

## 2. Resumo

| Destino | Quantidade | Regras |
|---|---|---|
| Entram na v1 | 5 | RN-17, RN-19, RN-33, RN-34, RN-37 |
| Juntam com algo que já existe | 7 | RN-14, RN-21, RN-22, RN-27, RN-29, RN-31, RN-35 |
| Viram requisito não funcional | 2 | RN-28, RN-38 |
| Ficam como proposta, sem implementar | 6 | RN-16, RN-20, RN-25, RN-36, RN-40, RN-41 |
| Fora de escopo da v1 | 12 | RN-15, RN-18, RN-23, RN-24, RN-26, RN-30, RN-32, RN-39, RN-42, RN-43, RN-44, RN-45 |

## 3. Entram na v1

Cada regra foi reescrita para não conflitar com as regras oficiais nem com as decisões da [arquitetura](../arquitetura.md).

| ID | Regra proposta | Por que foi reescrita |
|---|---|---|
| RN-17 | Quem reservou e não cancelou tem direito de usar o recurso até o fim do horário. Nenhuma reserva é liberada durante o horário: a falta só é registrada depois do fim, como `NAO_COMPARECEU` (S-06) | A versão original ("sem no-show automático") contradizia a RN-06 oficial |
| RN-19 | O sistema notifica o Solicitante na criação, na aprovação, na rejeição e no cancelamento da reserva. O aviso "30 minutos antes" fica fora | Responde a DP-03 sem exigir um agendador novo |
| RN-33 | Depois do fim do horário, o sistema encerra a reserva: `EM_USO` vira `CONCLUIDA`, e `APROVADA` sem uso vira `NAO_COMPARECEU` | A versão original mandava tudo para `CONCLUIDA`, o que apagaria o `NAO_COMPARECEU` da RN-06 |
| RN-34 | Um Solicitante não pode reservar duas ou mais salas no mesmo horário | Sem mudança |
| RN-37 | O Solicitante reserva, no máximo, 30 dias à frente (**30 dias é número candidato, validação pendente**) | Unifica a antecedência com a RN-12 (hoje ou futuro, nunca no passado) |

## 4. Juntam com algo que já existe

| ID | Junta com | Observação |
|---|---|---|
| RN-14 | RF-01 | Perfis: Solicitante, Responsável e Administrador |
| RN-21 | RN-10 | Feriados e dias não úteis já estão na janela de funcionamento |
| RN-22 e RN-29 | RN-09 | Auditoria e imutabilidade do registro (risco R-05 da arquitetura) |
| RN-27 | RN-03 | Concorrência: só a primeira solicitação simultânea é aceita |
| RN-31 | RN-16 | Várias reservas por semana, dentro do limite |
| RN-35 | RN-02 | A agenda do professor também não aceita duplo agendamento |

## 5. Viram requisito não funcional

| ID | Vai para | Observação |
|---|---|---|
| RN-28 | RNF-08 | Mensagens de erro não expõem detalhes internos nem credenciais |
| RN-38 | Novo RNF (a numerar) | Operações de reserva são idempotentes, por um token único por requisição |

## 6. Ficam como proposta, sem implementar

| ID | Regra | Motivo |
|---|---|---|
| RN-16 | O Solicitante reserva, no máximo, 10 horas por semana | Útil, mas exige contar as horas por semana |
| RN-20 | O Administrador cancela em lote as reservas afetadas por manutenção urgente | Conflita com a S-05 (manutenção não cancela reservas existentes). A S-05 continua valendo na v1 |
| RN-25 | O Responsável registra a conservação do material na retirada e na devolução | Amplia o RF-09 |
| RN-36 | Cancelamento permitido até 1 hora antes do início | Conflita com a RN-13 (cancelar até o início). **Vale a RN-13**, e a janela pode virar configuração no futuro |
| RN-40 | Empréstimo de material exige uma reserva de sala aprovada | Quebra a D-02, pela qual um material pode ser reservado sozinho |
| RN-41 | O sistema valida a lotação máxima da sala | Exige um campo novo na reserva (participantes) |

## 7. Fora de escopo da v1

| ID | Motivo |
|---|---|
| RN-15 e RN-30 | Antecedência confusa e o conceito de "evento institucional", que não existe no modelo |
| RN-18 | Prioridade institucional com aviso de 48 horas |
| RN-23 e RN-44 | Penalidades, suspensão e lista de bloqueio (dependem de contar faltas e danos) |
| RN-24 e RN-32 | Reservas recorrentes (já são o FE-01 do PRD) |
| RN-26, RN-43 e RN-45 | Quarentena de equipamentos, devolução parcial com avaria e confirmação por código |
| RN-39 | Reenvio de notificações (já é o FE-05); a parte "a reserva não é desfeita" já está no RNF-10 |
| RN-42 | Limite de tentativas de login |

## 8. Próximos passos

1. **Caio:** registrar no PRD as cinco regras da seção 3, os dois requisitos da seção 5 e o texto "Fora de escopo" da seção 7.
2. **Professor:** validar as regras novas junto das lacunas já abertas (L01, L02 e a janela de funcionamento).
3. **Renata:** escrever os casos de teste das cinco regras da v1 no [plano TDD](../testes/plano-tdd.md), em um commit à parte.
