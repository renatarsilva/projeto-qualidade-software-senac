# language: pt
Funcionalidade: Aprovação de reservas
  Somente o Responsável aprova ou rejeita uma reserva (RN-07), e uma reserva só muda de estado pelos caminhos
  permitidos (RN-05). Cada cenário tem um único Quando e um Então que dá para observar na API.

  Contexto:
    Dado a sala "sala-102"
    E o solicitante "Marina", o responsável "Carlos" e o administrador "Admin"

  Cenário: O Responsável aprova a reserva pedida
    Dado que "Marina" pediu a sala "sala-102" na terça-feira das 10:00 às 11:00
    Quando "Carlos" aprova a reserva
    Então a resposta é 200
    E a reserva está "APROVADA"

  Cenário: O Solicitante não aprova a própria reserva
    Dado que "Marina" pediu a sala "sala-102" na terça-feira das 10:00 às 11:00
    Quando "Marina" aprova a reserva
    Então a resposta é 403
    E a reserva está "SOLICITADA"

  Cenário: O Administrador não aprova reserva
    Dado que "Marina" pediu a sala "sala-102" na terça-feira das 10:00 às 11:00
    Quando "Admin" aprova a reserva
    Então a resposta é 403
    E a reserva está "SOLICITADA"

  Cenário: O Responsável rejeita a reserva pedida
    Dado que "Marina" pediu a sala "sala-102" na terça-feira das 10:00 às 11:00
    Quando "Carlos" rejeita a reserva
    Então a resposta é 200
    E a reserva está "REJEITADA"

  Cenário: Uma reserva rejeitada não pode ser aprovada depois
    Dado que "Marina" pediu a sala "sala-102" na terça-feira das 10:00 às 11:00
    E que "Carlos" rejeitou a reserva
    Quando "Carlos" aprova a reserva
    Então a resposta é 409
    E a reserva está "REJEITADA"
