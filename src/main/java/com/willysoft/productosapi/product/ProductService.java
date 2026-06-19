package com.willysoft.productosapi.product;

import com.willysoft.productosapi.category.Category;
import com.willysoft.productosapi.category.CategoryService;
import com.willysoft.productosapi.category.dto.CategoryResponse;
import com.willysoft.productosapi.exception.ResourceNotFoundException;
import com.willysoft.productosapi.product.dto.CategoriaProductos;
import com.willysoft.productosapi.product.dto.PriceBreakdown;
import com.willysoft.productosapi.product.dto.ProductRequest;
import com.willysoft.productosapi.product.dto.ProductResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final PriceCalculationService priceCalculationService;

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String nombre, Long categoriaId, Pageable pageable) {
        return productRepository.findAll(ProductSpecifications.filtrar(nombre, categoriaId), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return toResponse(getProductOrThrow(id));
    }

    /**
     * Catálogo completo agrupado por categoría (corte de control).
     * Ordenado por nombre de categoría y, dentro de cada una, por nombre de producto.
     */
    @Transactional(readOnly = true)
    public List<CategoriaProductos> catalogoPorCategoria() {
        Sort orden = Sort.by(Sort.Order.asc("categoria.nombre"), Sort.Order.asc("nombre"));
        Map<Long, CategoryResponse> categorias = new LinkedHashMap<>();
        Map<Long, List<ProductResponse>> porCategoria = new LinkedHashMap<>();
        for (Product p : productRepository.findAll(orden)) {
            ProductResponse r = toResponse(p);
            Long catId = r.categoria().id();
            categorias.putIfAbsent(catId, r.categoria());
            porCategoria.computeIfAbsent(catId, k -> new ArrayList<>()).add(r);
        }
        return categorias.entrySet().stream()
                .map(e -> new CategoriaProductos(e.getValue(), porCategoria.get(e.getKey())))
                .toList();
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Category categoria = categoryService.getCategoryOrThrow(request.categoriaId());
        Product product = Product.builder()
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .imagenUrl(request.imagenUrl())
                .precio(request.precio())
                .moneda(monedaOrDefault(request.moneda()))
                .stock(request.stock())
                .categoria(categoria)
                .build();
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProductOrThrow(id);
        Category categoria = categoryService.getCategoryOrThrow(request.categoriaId());
        product.setNombre(request.nombre());
        product.setDescripcion(request.descripcion());
        product.setImagenUrl(request.imagenUrl());
        product.setPrecio(request.precio());
        product.setMoneda(monedaOrDefault(request.moneda()));
        product.setStock(request.stock());
        product.setCategoria(categoria);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        Product product = getProductOrThrow(id);
        productRepository.delete(product);
    }

    /** Desglose de precio (neto, IVA, final en ARS y USD) de un producto. */
    @Transactional(readOnly = true)
    public PriceBreakdown getBreakdown(Long id) {
        return priceCalculationService.calcular(getProductOrThrow(id));
    }

    private Product getProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
    }

    private Moneda monedaOrDefault(Moneda moneda) {
        return moneda != null ? moneda : Moneda.ARS;
    }

    private ProductResponse toResponse(Product product) {
        Category c = product.getCategoria();
        CategoryResponse categoria = new CategoryResponse(
                c.getId(), c.getNombre(), c.getDescripcion(), c.getAlicuotaIva(), c.getIconoUrl());
        PriceBreakdown precio = priceCalculationService.calcular(product);
        return new ProductResponse(
                product.getId(),
                product.getNombre(),
                product.getDescripcion(),
                product.getPrecio(),
                product.getMoneda(),
                product.getStock(),
                categoria,
                precio.precioFinalArs(),
                precio.precioFinalUsd(),
                product.getImagenUrl()
        );
    }
}
