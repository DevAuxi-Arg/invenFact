package com.willysoft.productosapi.product;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> filtrar(String nombre, Long categoriaId) {
        Specification<Product> spec = (root, query, cb) -> cb.conjunction();
        if (StringUtils.hasText(nombre)) {
            spec = spec.and(nombreContiene(nombre));
        }
        if (categoriaId != null) {
            spec = spec.and(deCategoria(categoriaId));
        }
        return spec;
    }

    public static Specification<Product> nombreContiene(String nombre) {
        String like = "%" + nombre.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("nombre")), like);
    }

    public static Specification<Product> deCategoria(Long categoriaId) {
        return (root, query, cb) -> cb.equal(root.get("categoria").get("id"), categoriaId);
    }
}
