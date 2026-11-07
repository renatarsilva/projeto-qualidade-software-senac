-- Usuários do sistema (T-15, ADR-0009): um perfil por usuário; nunca apagado, só desativado.
-- A senha é guardada só como hash BCrypt (ADR-0007). Nenhuma senha é gravada por migração:
-- o primeiro Administrador nasce na subida da aplicação, a partir do ambiente (T-15, CA-16).
CREATE TABLE usuario (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome       VARCHAR(100) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    senha_hash VARCHAR(100) NOT NULL,
    perfil     VARCHAR(20)  NOT NULL,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT usuario_email_unico UNIQUE (email),
    CONSTRAINT usuario_perfil_valido CHECK (perfil IN ('SOLICITANTE', 'RESPONSAVEL', 'ADMINISTRADOR'))
);
