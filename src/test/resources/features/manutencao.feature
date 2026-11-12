# language: pt
Funcionalidade: Bloqueio de recursos em manutenção
  Um recurso em manutenção não pode ser reservado (RN-04). O período é [início, fim): a reserva que encosta na
  fronteira passa, e a que cruza um segundo é recusada.

  Contexto:
    Dado a sala "sala-102"
    E o solicitante "Marina", o responsável "Carlos" e o administrador "Admin"
    E que a sala "sala-102" está em manutenção na terça-feira das 10:00:00 às 12:00:00

  Esquema do Cenário: Fronteiras do período de manutenção
    Quando "Marina" pede a sala "sala-102" na terça-feira das <inicio> às <fim>
    Então o pedido é <resultado>

    Exemplos:
      | inicio   | fim      | resultado | observação                              |
      | 08:00:00 | 10:00:00 | aceito    | termina exatamente no início            |
      | 12:00:00 | 13:00:00 | aceito    | começa exatamente no fim                |
      | 09:00:00 | 10:00:01 | recusado  | cruza o início por um segundo           |
      | 11:59:59 | 13:00:00 | recusado  | cruza o fim por um segundo              |
      | 10:30:00 | 11:30:00 | recusado  | dentro do período                       |
      | 09:00:00 | 13:00:00 | recusado  | envolve o período inteiro               |

  Cenário: Encerrar a manutenção libera a sala
    Dado que "Admin" encerrou a manutenção da sala "sala-102"
    Quando "Marina" pede a sala "sala-102" na terça-feira das 10:30:00 às 11:30:00
    Então o pedido é aceito

  Cenário: O Solicitante não registra manutenção
    Quando "Marina" registra manutenção da sala "sala-102" na terça-feira das 14:00:00 às 15:00:00
    Então a resposta é 403
