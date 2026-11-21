package com.moreno.gestorfranquicias.crosscutting.persistence.franquicia.mapper;

import com.moreno.gestorfranquicias.crosscutting.domain.response.FranquiciaResumenResponse;
import com.moreno.gestorfranquicias.crosscutting.persistence.franquicia.entity.FranquiciaEntity;

import org.springframework.stereotype.Component;

@Component
public class FranquiciaMapper {
    public FranquiciaResumenResponse convertir(FranquiciaEntity entidad) {
        return new FranquiciaResumenResponse(entidad.idFranquicia(), entidad.nombre());
    }
}
