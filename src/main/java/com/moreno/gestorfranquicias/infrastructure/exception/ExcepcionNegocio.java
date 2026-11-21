package com.moreno.gestorfranquicias.infrastructure.exception;

import org.springframework.http.HttpStatus;

public class ExcepcionNegocio extends RuntimeException {
    private final HttpStatus estado;

    public ExcepcionNegocio(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }

    public static ExcepcionNegocio noEncontrado(String recurso) {
        return new ExcepcionNegocio(HttpStatus.NOT_FOUND, recurso + " no existe.");
    }
}
