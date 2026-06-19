package com.willysoft.productosapi.category;

import com.willysoft.productosapi.category.dto.CategoryRequest;
import com.willysoft.productosapi.category.dto.CategoryResponse;
import com.willysoft.productosapi.exception.ConflictException;
import com.willysoft.productosapi.exception.ResourceNotFoundException;
import com.willysoft.productosapi.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<CategoryResponse> search(String nombre, Pageable pageable) {
        Page<Category> page = StringUtils.hasText(nombre)
                ? categoryRepository.findByNombreContainingIgnoreCase(nombre, pageable)
                : categoryRepository.findAll(pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public java.util.List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return toResponse(getCategoryOrThrow(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new ConflictException("Ya existe una categoría con el nombre: " + request.nombre());
        }
        Category category = Category.builder()
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .iconoUrl(request.iconoUrl())
                .alicuotaIva(alicuotaOrDefault(request.alicuotaIva()))
                .build();
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getCategoryOrThrow(id);
        if (!category.getNombre().equalsIgnoreCase(request.nombre())
                && categoryRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new ConflictException("Ya existe una categoría con el nombre: " + request.nombre());
        }
        category.setNombre(request.nombre());
        category.setDescripcion(request.descripcion());
        category.setIconoUrl(request.iconoUrl());
        category.setAlicuotaIva(alicuotaOrDefault(request.alicuotaIva()));
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = getCategoryOrThrow(id);
        if (productRepository.existsByCategoriaId(id)) {
            throw new ConflictException("No se puede eliminar la categoría porque tiene productos asociados");
        }
        categoryRepository.delete(category);
    }

    public Category getCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + id));
    }

    /** IVA general (21%) por defecto si no se especifica. */
    private java.math.BigDecimal alicuotaOrDefault(java.math.BigDecimal alicuota) {
        return alicuota != null ? alicuota : new java.math.BigDecimal("21.00");
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getNombre(),
                category.getDescripcion(),
                category.getAlicuotaIva(),
                category.getIconoUrl()
        );
    }
}
