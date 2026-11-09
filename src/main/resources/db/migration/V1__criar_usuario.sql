-- T-15: usuários do sistema (arquitetura 3.1 e 3.2; ADR-0009: cada usuário tem um perfil e nunca é apagado).
-- Nenhum usuário nem senha é inserido aqui: o primeiro administrador vem das variáveis de ambiente (spec T-15, CA-16).
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
