package com.willysoft.productosapi.category;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository repository;

    @Test
    void existsByNombreIgnoreCase_devuelveTrueAunqueCambieElCase() {
        repository.save(Category.builder().nombre("Bebidas").descripcion("frias").build());

        assertThat(repository.existsByNombreIgnoreCase("bebidas")).isTrue();
        assertThat(repository.existsByNombreIgnoreCase("BEBIDAS")).isTrue();
        assertThat(repository.existsByNombreIgnoreCase("Snacks")).isFalse();
    }

    @Test
    void findByNombreContainingIgnoreCase_filtra() {
        repository.save(Category.builder().nombre("Bebidas calientes").build());
        repository.save(Category.builder().nombre("Bebidas frías").build());
        repository.save(Category.builder().nombre("Snacks").build());

        var page = repository.findByNombreContainingIgnoreCase("bebidas", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Category::getNombre)
                .allMatch(n -> n.toLowerCase().contains("bebidas"));
    }
}
