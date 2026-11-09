package br.senac.recursos.material;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    boolean existsByPatrimonioIgnoreCase(String patrimonio);

    boolean existsByPatrimonioIgnoreCaseAndIdNot(String patrimonio, Long id);
}
