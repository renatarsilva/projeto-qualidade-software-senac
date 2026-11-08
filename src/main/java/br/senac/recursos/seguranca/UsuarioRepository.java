package br.senac.recursos.seguranca;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByPerfil(Perfil perfil);

    boolean existsByEmail(String email);

    long countByPerfilAndAtivoTrue(Perfil perfil);
}
