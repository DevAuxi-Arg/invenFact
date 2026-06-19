package com.willysoft.productosapi.product;

import com.willysoft.productosapi.category.Category;
import com.willysoft.productosapi.category.CategoryService;
import com.willysoft.productosapi.category.dto.CategoryResponse;
import com.willysoft.productosapi.exception.ConflictException;
import com.willysoft.productosapi.exception.ResourceNotFoundException;
import com.willysoft.productosapi.product.dto.CategoriaProductos;
import com.willysoft.productosapi.product.dto.PriceBreakdown;
import com.willysoft.productosapi.product.dto.ProductRequest;
import com.willysoft.productosapi.product.dto.ProductResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

    /** Campos calculados (no existen en la base): se ordenan en memoria. */
    private static final Set<String> SORTS_CALCULADOS = Set.of("precioFinalArs", "precioFinalUsd");

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String nombre, Long categoriaId, Pageable pageable) {
        var spec = ProductSpecifications.filtrar(nombre, categoriaId);
        Optional<Sort.Order> calculado = pageable.getSort().stream()
                .filter(o -> SORTS_CALCULADOS.contains(o.getProperty()))
                .findFirst();

        // Orden por columna real: paginación eficiente en la base.
        if (calculado.isEmpty()) {
            return productRepository.findAll(spec, pageable).map(this::toResponse);
        }

        // Orden por columna calculada (precio final): traigo todo, ordeno y pagino a mano.
        Sort.Order orden = calculado.get();
        List<ProductResponse> todos = new ArrayList<>(
                productRepository.findAll(spec, Sort.unsorted()).stream()
                        .map(this::toResponse)
                        .toList());
        Comparator<ProductResponse> cmp = "precioFinalUsd".equals(orden.getProperty())
                ? Comparator.comparing(ProductResponse::precioFinalUsd, Comparator.nullsLast(Comparator.naturalOrder()))
                : Comparator.comparing(ProductResponse::precioFinalArs, Comparator.nullsLast(Comparator.naturalOrder()));
        if (orden.isDescending()) {
            cmp = cmp.reversed();
        }
        todos.sort(cmp);

        int desde = (int) pageable.getOffset();
        int hasta = Math.min(desde + pageable.getPageSize(), todos.size());
        List<ProductResponse> contenido = desde >= todos.size() ? List.of() : todos.subList(desde, hasta);
        return new PageImpl<>(contenido, pageable, todos.size());
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
        return catalogoPorCategoria(null);
    }

    /**
     * Catálogo agrupado por categoría. Si {@code categoriaIds} trae elementos,
     * solo incluye esas categorías; si es null o vacío, incluye todas.
     */
    @Transactional(readOnly = true)
    public List<CategoriaProductos> catalogoPorCategoria(Collection<Long> categoriaIds) {
        boolean filtra = categoriaIds != null && !categoriaIds.isEmpty();
        Sort orden = Sort.by(Sort.Order.asc("categoria.nombre"), Sort.Order.asc("nombre"));
        Map<Long, CategoryResponse> categorias = new LinkedHashMap<>();
        Map<Long, List<ProductResponse>> porCategoria = new LinkedHashMap<>();
        for (Product p : productRepository.findAll(orden)) {
            ProductResponse r = toResponse(p);
            Long catId = r.categoria().id();
            if (filtra && !categoriaIds.contains(catId)) {
                continue;
            }
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

    /**
     * Ajuste acotado de stock (sin tocar el resto del producto).
     * modo = "sumar" | "restar" | "fijar"; valor debe ser >= 0.
     */
    @Transactional
    public ProductResponse ajustarStock(Long id, String modo, int valor) {
        if (valor < 0) {
            throw new ConflictException("El valor debe ser cero o positivo.");
        }
        Product product = getProductOrThrow(id);
        int actual = product.getStock();
        int nuevo;
        switch (modo == null ? "" : modo) {
            case "sumar"  -> nuevo = actual + valor;
            case "restar" -> nuevo = actual - valor;
            case "fijar"  -> nuevo = valor;
            default -> throw new ConflictException("Operación de stock inválida: " + modo);
        }
        if (nuevo < 0) {
            throw new ConflictException("El stock no puede quedar negativo (actual: "
                    + actual + ", quedaría: " + nuevo + ").");
        }
        product.setStock(nuevo);
        return toResponse(productRepository.save(product));
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
