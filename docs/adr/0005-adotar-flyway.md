# ADR-0005: Adotar o Flyway para versionar o esquema do banco

- **Status:** Proposta
- **Data:** 2026-10-31
- **Autor(a):** @renatarsilva
- **Task:** T-13

## Contexto

- As restrições que protegem a dupla reserva e a manutenção são SQL específico do PostgreSQL (`EXCLUDE USING gist`, extensão `btree_gist`, `CHECK`, `UNIQUE`), descritas na seção 3.2 de [`arquitetura.md`](../arquitetura.md). Elas precisam ser criadas de forma controlada e idêntica em todos os ambientes.
- O esquema muda ao longo das semanas, com três pessoas mexendo no banco. A [ADR-0004](0004-adotar-postgresql.md) já fixa o PostgreSQL.
- O esqueleto já traz o Flyway (`flyway-core` e `flyway-database-postgresql` no `pom.xml`), com `spring.jpa.hibernate.ddl-auto=validate`: o Hibernate só confere se as entidades batem com as tabelas, e o esquema é responsabilidade do Flyway.
- Os testes de integração sobem um PostgreSQL limpo a cada execução (Testcontainers) e precisam do mesmo esquema da aplicação.

## Decisão

O esquema do banco é criado e alterado **somente por migrações Flyway em SQL**, em `src/main/resources/db/migration`, aplicadas quando a aplicação sobe. O Hibernate não cria nem altera tabelas.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| `ddl-auto=update` do Hibernate | Não gera a restrição de exclusão, não deixa histórico das mudanças e pode alterar o banco de forma imprevisível. |
| Liquibase | Resolve o mesmo problema, mas é mais uma sintaxe para aprender. O Flyway usa SQL puro, que é exatamente o que as restrições do PostgreSQL pedem. |
| Scripts SQL rodados à mão | Sem controle de qual versão cada banco está, e cada pessoa do time poderia ter um esquema diferente. |

## Consequências

- **Positivas:** o esquema fica versionado junto com o código e revisado em PR; o banco de teste e o de desenvolvimento nascem iguais; o Hibernate acusa na subida se uma entidade divergir da tabela.
- **Negativas e riscos:** uma migração já aplicada não pode ser editada (o Flyway confere o checksum), e um erro exige uma nova migração. A edição gratuita não faz reversão automática. A migração que cria `btree_gist` precisa de permissão no banco (risco levantado na revisão do papel Ops).
- **Como vamos verificar:** o teste de integração sobe um PostgreSQL vazio, aplica todas as migrações e o Hibernate valida o esquema sem erro; uma migração com SQL inválido faz a subida falhar.
- **Condição de revisão:** rever se o time precisar desfazer migrações de forma automática, ou se o ambiente de entrega não puder aplicá-las na subida da aplicação.
