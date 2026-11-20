package com.moreno.gestorfranquicias.crosscutting.persistence.franquicia.entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
@Table("franquicias")
public record FranquiciaEntity(@Id @Column("id_franquicia") Long idFranquicia, String nombre) {}
