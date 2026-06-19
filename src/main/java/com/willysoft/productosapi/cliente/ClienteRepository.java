package com.willysoft.productosapi.cliente;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByDocumento(String documento);

    boolean existsByDocumento(String documento);

    Page<Cliente> findByNombreContainingIgnoreCaseOrDocumentoContainingIgnoreCase(
            String nombre, String documento, Pageable pageable);
}
