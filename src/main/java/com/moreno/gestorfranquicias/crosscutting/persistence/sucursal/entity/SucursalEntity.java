package com.moreno.gestorfranquicias.crosscutting.persistence.sucursal.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("sucursales")
public record SucursalEntity(
    @Id @Column("id_sucursal") Long idSucursal,
    @Column("id_franquicia") Long idFranquicia,
    String nombre) {}
