package com.fitstore.catalog.inventario.repository;

import com.fitstore.catalog.inventario.model.Stock;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {

    /**
     * Reserva atómica (micro 4.6): la verificación y el aumento ocurren en una sola sentencia, así que si dos clientes
     * compran la última unidad solo uno la logra.
     *
     * @return filas afectadas: 1 = reservado, 0 = no alcanza (o la variante no tiene stock)
     */
    @Transactional
    @Modifying
    @Query("update Stock s set s.reservado = s.reservado + :cantidad "
            + "where s.varianteId = :varianteId and s.fisico - s.reservado >= :cantidad")
    int reservarSiAlcanza(@Param("varianteId") Long varianteId, @Param("cantidad") int cantidad);

    /** Libera lo reservado (cancelación, expiración); nunca baja de cero. @return filas afectadas */
    @Transactional
    @Modifying
    @Query("update Stock s set s.reservado = s.reservado - :cantidad "
            + "where s.varianteId = :varianteId and s.reservado >= :cantidad")
    int liberar(@Param("varianteId") Long varianteId, @Param("cantidad") int cantidad);

    /** Confirma la venta: descuenta del físico y del reservado a la vez (paso T5). @return filas afectadas */
    @Transactional
    @Modifying
    @Query("update Stock s set s.fisico = s.fisico - :cantidad, s.reservado = s.reservado - :cantidad "
            + "where s.varianteId = :varianteId and s.reservado >= :cantidad")
    int descontar(@Param("varianteId") Long varianteId, @Param("cantidad") int cantidad);

    /** Stock bajo: disponible menor o igual que el mínimo de la variante o, si no tiene, el de por defecto (micro 4.8). */
    @Query(value = "select s.* from stock s where s.fisico - s.reservado <= coalesce(s.minimo, "
            + "(select c.stock_minimo_por_defecto from configuracion_inventario c where c.id = 1))", nativeQuery = true)
    List<Stock> listarStockBajo();
}
