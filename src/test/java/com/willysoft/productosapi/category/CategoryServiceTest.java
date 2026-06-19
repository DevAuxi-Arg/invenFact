package com.willysoft.productosapi.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.willysoft.productosapi.category.dto.CategoryRequest;
import com.willysoft.productosapi.exception.ConflictException;
import com.willysoft.productosapi.exception.ResourceNotFoundException;
import com.willysoft.productosapi.product.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService service;

    @Test
    void create_persiste_y_devuelveResponse() {
        var request = new CategoryRequest("Bebidas", "Frías", new BigDecimal("21.00"), null);
        when(categoryRepository.existsByNombreIgnoreCase("Bebidas")).thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(inv -> {
                    Category c = inv.getArgument(0);
                    c.setId(1L);
                    return c;
                });

        var response = service.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nombre()).isEqualTo("Bebidas");
    }

    @Test
    void create_lanzaConflict_siNombreYaExiste() {
        var request = new CategoryRequest("Bebidas", null, new BigDecimal("21.00"), null);
        when(categoryRepository.existsByNombreIgnoreCase("Bebidas")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Bebidas");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void findById_lanzaNotFound_siNoExiste() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void delete_lanzaConflict_siTieneProductos() {
        var c = Category.builder().id(1L).nombre("Bebidas").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(c));
        when(productRepository.existsByCategoriaId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("productos asociados");

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void delete_ok_sinProductos() {
        var c = Category.builder().id(1L).nombre("Bebidas").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(c));
        when(productRepository.existsByCategoriaId(1L)).thenReturn(false);

        service.delete(1L);

        verify(categoryRepository).delete(c);
    }

    @Test
    void update_permiteMismoNombre_sinChequearConflicto() {
        var existing = Category.builder().id(1L).nombre("Bebidas").build();
        var request = new CategoryRequest("Bebidas", "actualizada", new BigDecimal("21.00"), null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenReturn(existing);

        var response = service.update(1L, request);

        assertThat(response.descripcion()).isEqualTo("actualizada");
        verify(categoryRepository, never()).existsByNombreIgnoreCase(anyString());
    }

    @Test
    void update_chequeaConflicto_siCambiaNombre() {
        var existing = Category.builder().id(1L).nombre("Bebidas").build();
        var request = new CategoryRequest("Snacks", null, new BigDecimal("21.00"), null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNombreIgnoreCase("Snacks")).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(ConflictException.class);
    }
}
