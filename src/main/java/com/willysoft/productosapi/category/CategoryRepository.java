package com.willysoft.productosapi.category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    Page<Category> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
}
