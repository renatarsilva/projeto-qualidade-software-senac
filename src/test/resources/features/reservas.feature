# language: pt
Funcionalidade: Uma reserva por vez e rastro de cada mudança
  Duas solicitações simultâneas produzem uma única reserva aceita (RN-03), e toda mudança de estado gera auditoria
  (RN-12).

  Contexto:
    Dado a sala "sala-102"
    E o solicitante "Marina", o responsável "Carlos" e o administrador "Admin"

  Cenário: Duas solicitações simultâneas da mesma sala produzem uma única reserva
    Quando 2 solicitantes pedem ao mesmo tempo a sala "sala-102" na terça-feira das 10:00:00 às 11:00:00
    Então exatamente 1 pedido é aceito e 1 é recusado
    E existe 1 reserva que ocupa a sala

  Cenário: Vinte solicitações simultâneas da mesma sala produzem uma única reserva
    Quando 20 solicitantes pedem ao mesmo tempo a sala "sala-102" na terça-feira das 10:00:00 às 11:00:00
    Então exatamente 1 pedido é aceito e 19 são recusados
    E existe 1 reserva que ocupa a sala

  Cenário: Reservas que se encostam não conflitam
    Dado que "Marina" pediu a sala "sala-102" na terça-feira das 10:00:00 às 11:00:00
    Quando "Marina" pede a sala "sala-102" na terça-feira das 11:00:00 às 12:00:00
    Então o pedido é aceito

  Cenário: Toda mudança de estado gera um registro de auditoria
    Dado que "Marina" pediu a sala "sala-102" na terça-feira das 10:00 às 11:00
    E que "Carlos" aprovou a reserva
    Quando "Marina" cancela a reserva
    Então o histórico da reserva tem os estados "SOLICITADA, APROVADA, CANCELADA"
