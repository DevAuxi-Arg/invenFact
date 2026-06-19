package com.willysoft.productosapi.product;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willysoft.productosapi.category.dto.CategoryResponse;
import com.willysoft.productosapi.exception.GlobalExceptionHandler;
import com.willysoft.productosapi.exception.ResourceNotFoundException;
import com.willysoft.productosapi.product.dto.ProductRequest;
import com.willysoft.productosapi.product.dto.ProductResponse;
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

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @MockitoBean
    private ProductService service;

    private ProductResponse sample() {
        return new ProductResponse(1L, "Coca", "x", new BigDecimal("15.50"), Moneda.ARS, 10,
                new CategoryResponse(1L, "Bebidas", null, new BigDecimal("21.00"), null),
                new BigDecimal("18.76"), null, null);
    }

    @Test
    void getAll_devuelvePaginado() throws Exception {
        when(service.search(eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sample())));

        mvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Coca"))
                .andExpect(jsonPath("$.content[0].categoria.nombre").value("Bebidas"));
    }

    @Test
    void getAll_pasaFiltros() throws Exception {
        when(service.search(eq("coca"), eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sample())));

        mvc.perform(get("/api/productos?nombre=coca&categoriaId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void create_devuelve400_siPrecioNegativo() throws Exception {
        var req = new ProductRequest("Coca", "x", new BigDecimal("-1"), 10, 1L, Moneda.ARS, null);

        mvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.precio").exists());
    }

    @Test
    void create_devuelve400_siStockNegativo() throws Exception {
        var req = new ProductRequest("Coca", "x", new BigDecimal("10"), -1, 1L, Moneda.ARS, null);

        mvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.stock").exists());
    }

    @Test
    void create_devuelve400_siFaltaCategoria() throws Exception {
        var req = new ProductRequest("Coca", "x", new BigDecimal("10"), 1, null, Moneda.ARS, null);

        mvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.categoriaId").exists());
    }

    @Test
    void create_devuelve201_ok() throws Exception {
        var req = new ProductRequest("Coca", "x", new BigDecimal("15.50"), 10, 1L, Moneda.ARS, null);
        when(service.create(any(ProductRequest.class))).thenReturn(sample());

        mvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void create_devuelve404_siCategoriaNoExiste() throws Exception {
        var req = new ProductRequest("Coca", "x", new BigDecimal("10"), 1, 99L, Moneda.ARS, null);
        when(service.create(any(ProductRequest.class)))
                .thenThrow(new ResourceNotFoundException("Categoría no encontrada con id: 99"));

        mvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }
}
