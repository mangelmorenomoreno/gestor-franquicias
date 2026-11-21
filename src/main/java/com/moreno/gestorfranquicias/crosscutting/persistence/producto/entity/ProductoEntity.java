package com.moreno.gestorfranquicias.crosscutting.persistence.producto.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("productos")
public record ProductoEntity(
        @Id @Column("id_producto") Long idProducto,
        @Column("id_sucursal") Long idSucursal,
        String nombre,
        Integer stock,
        @Version Long version) {}
