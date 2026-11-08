package br.senac.recursos.material;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByPatrimonioIgnoreCase(String patrimonio);
}
