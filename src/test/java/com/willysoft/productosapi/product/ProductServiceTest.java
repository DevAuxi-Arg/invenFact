package com.willysoft.productosapi.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.willysoft.productosapi.category.Category;
import com.willysoft.productosapi.category.CategoryService;
import com.willysoft.productosapi.exception.ResourceNotFoundException;
import com.willysoft.productosapi.product.dto.PriceBreakdown;
import com.willysoft.productosapi.product.dto.ProductRequest;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private PriceCalculationService priceCalculationService;

    @Mock
    private com.willysoft.productosapi.parametro.ParametroService parametroService;

    @InjectMocks
    private ProductService service;

    private PriceBreakdown dummyBreakdown() {
        return new PriceBreakdown(Moneda.ARS, BigDecimal.ZERO, BigDecimal.ONE,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Test
    void create_persisteConCategoriaResuelta() {
        var categoria = Category.builder().id(1L).nombre("Bebidas").build();
        var request = new ProductRequest("Coca", "600ml", new BigDecimal("15.50"), 10, 1L, Moneda.ARS, null);

        when(categoryService.getCategoryOrThrow(1L)).thenReturn(categoria);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(42L);
            return p;
        });
        when(priceCalculationService.calcular(any(Product.class))).thenReturn(dummyBreakdown());

        var response = service.create(request);

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.nombre()).isEqualTo("Coca");
        assertThat(response.categoria().id()).isEqualTo(1L);
    }

    @Test
    void create_propagaNotFound_siCategoriaNoExiste() {
        var request = new ProductRequest("X", "", new BigDecimal("1"), 1, 99L, Moneda.ARS, null);
        when(categoryService.getCategoryOrThrow(99L))
                .thenThrow(new ResourceNotFoundException("Categoría no encontrada con id: 99"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void delete_borraElProducto() {
        var p = Product.builder().id(5L)
                .categoria(Category.builder().id(1L).nombre("c").build()).build();
        when(productRepository.findById(5L)).thenReturn(Optional.of(p));

        service.delete(5L);

        verify(productRepository).delete(p);
    }

    @Test
    void delete_lanzaNotFound_siNoExiste() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_modificaCamposYCategoria() {
        var oldCat = Category.builder().id(1L).nombre("Bebidas").build();
        var newCat = Category.builder().id(2L).nombre("Snacks").build();
        var existing = Product.builder().id(5L)
                .nombre("Old").precio(new BigDecimal("1")).stock(1).categoria(oldCat).build();
        var request = new ProductRequest("New", "desc", new BigDecimal("9.99"), 50, 2L, Moneda.ARS, null);

        when(productRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(categoryService.getCategoryOrThrow(2L)).thenReturn(newCat);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(priceCalculationService.calcular(any(Product.class))).thenReturn(dummyBreakdown());

        var response = service.update(5L, request);

        assertThat(response.nombre()).isEqualTo("New");
        assertThat(response.precio()).isEqualByComparingTo("9.99");
        assertThat(response.stock()).isEqualTo(50);
        assertThat(response.categoria().id()).isEqualTo(2L);
    }
}
