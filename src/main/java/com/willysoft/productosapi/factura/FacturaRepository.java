package com.willysoft.productosapi.factura;

import com.willysoft.productosapi.factura.dto.ClienteTop;
import com.willysoft.productosapi.factura.dto.ProductoVendido;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    /** Respeta el {@code Sort} del Pageable (fecha, clienteNombre, total, ...) y trae las líneas. */
    @Override
    @EntityGraph(attributePaths = "lineas")
    Page<Factura> findAll(Pageable pageable);

    long countByEstado(EstadoFactura estado);

    /** Fecha, total y total de IVA de cada factura en el estado dado (para agregar en memoria). */
    @Query("select f.fecha, f.total, f.totalIva from Factura f where f.estado = :estado")
    List<Object[]> findFechaTotalIva(@Param("estado") EstadoFactura estado);

    @Query("""
            select new com.willysoft.productosapi.factura.dto.ProductoVendido(
                       l.nombreProducto, sum(l.cantidad), sum(l.totalArs))
            from LineaFactura l
            where l.factura.estado = :estado
            group by l.nombreProducto
            order by sum(l.cantidad) desc""")
    List<ProductoVendido> topProductos(@Param("estado") EstadoFactura estado, Pageable pageable);

    @Query("""
            select new com.willysoft.productosapi.factura.dto.ClienteTop(
                       f.clienteNombre, count(f), sum(f.total))
            from Factura f
            where f.estado = :estado
            group by f.clienteNombre
            order by sum(f.total) desc""")
    List<ClienteTop> topClientes(@Param("estado") EstadoFactura estado, Pageable pageable);
}
