package com.willysoft.productosapi.category;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willysoft.productosapi.category.dto.CategoryRequest;
import com.willysoft.productosapi.category.dto.CategoryResponse;
import com.willysoft.productosapi.exception.ConflictException;
import com.willysoft.productosapi.exception.GlobalExceptionHandler;
import com.willysoft.productosapi.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @MockitoBean
    private CategoryService service;

    @Test
    void getAll_devuelvePaginado() throws Exception {
        when(service.search(eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        new CategoryResponse(1L, "Bebidas", "Frías", new BigDecimal("21.00"), null),
                        new CategoryResponse(2L, "Snacks", null, new BigDecimal("10.50"), null)
                )));

        mvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].nombre").value("Bebidas"));
    }

    @Test
    void getById_devuelve200() throws Exception {
        when(service.findById(1L)).thenReturn(new CategoryResponse(1L, "Bebidas", "x", new BigDecimal("21.00"), null));

        mvc.perform(get("/api/categorias/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Bebidas"));
    }

    @Test
    void getById_devuelve404_siNoExiste() throws Exception {
        when(service.findById(99L)).thenThrow(new ResourceNotFoundException("Categoría no encontrada con id: 99"));

        mvc.perform(get("/api/categorias/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("99")));
    }

    @Test
    void create_devuelve201() throws Exception {
        var req = new CategoryRequest("Bebidas", "x", new BigDecimal("21.00"), null);
        when(service.create(any(CategoryRequest.class)))
                .thenReturn(new CategoryResponse(1L, "Bebidas", "x", new BigDecimal("21.00"), null));

        mvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void create_devuelve400_siNombreVacio() throws Exception {
        var req = new CategoryRequest("", "x", new BigDecimal("21.00"), null);

        mvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.nombre").exists());
    }

    @Test
    void create_devuelve409_siConflicto() throws Exception {
        var req = new CategoryRequest("Bebidas", null, new BigDecimal("21.00"), null);
        when(service.create(any(CategoryRequest.class)))
                .thenThrow(new ConflictException("Ya existe una categoría con el nombre: Bebidas"));

        mvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_devuelve204() throws Exception {
        mvc.perform(delete("/api/categorias/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_devuelve409_siTieneProductos() throws Exception {
        doThrow(new ConflictException("No se puede eliminar la categoría porque tiene productos asociados"))
                .when(service).delete(1L);

        mvc.perform(delete("/api/categorias/1"))
                .andExpect(status().isConflict());
    }
}
