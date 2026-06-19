package com.willysoft.productosapi.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.willysoft.productosapi.category.Category;
import com.willysoft.productosapi.category.CategoryRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category bebidas;
    private Category snacks;

    @BeforeEach
    void setUp() {
        bebidas = categoryRepository.save(Category.builder().nombre("Bebidas").build());
        snacks = categoryRepository.save(Category.builder().nombre("Snacks").build());

        productRepository.save(Product.builder()
                .nombre("Coca-Cola 600ml").precio(new BigDecimal("15.50")).stock(10)
                .categoria(bebidas).build());
        productRepository.save(Product.builder()
                .nombre("Coca-Cola 2L").precio(new BigDecimal("32.00")).stock(5)
                .categoria(bebidas).build());
        productRepository.save(Product.builder()
                .nombre("Papas saladas").precio(new BigDecimal("8.00")).stock(20)
                .categoria(snacks).build());
    }

    @Test
    void existsByCategoriaId_detectaProductosAsociados() {
        assertThat(productRepository.existsByCategoriaId(bebidas.getId())).isTrue();
    }

    @Test
    void filtroPorNombre() {
        var spec = ProductSpecifications.filtrar("coca", null);
        var result = productRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void filtroPorCategoria() {
        var spec = ProductSpecifications.filtrar(null, snacks.getId());
        var result = productRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getNombre()).isEqualTo("Papas saladas");
    }

    @Test
    void filtroCombinado() {
        var spec = ProductSpecifications.filtrar("coca", bebidas.getId());
        var result = productRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void sinFiltros_devuelveTodo() {
        var spec = ProductSpecifications.filtrar(null, null);
        var result = productRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(3);
    }
}
