package com.moreno.gestorfranquicias.infrastructure.exception;
import com.moreno.gestorfranquicias.crosscutting.domain.common.ApiResponse;
import com.moreno.gestorfranquicias.crosscutting.domain.common.DetalleError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import java.util.List;
@RestControllerAdvice
public class ManejadorExcepciones {
    private static final Logger LOG = LoggerFactory.getLogger(ManejadorExcepciones.class);
    @ExceptionHandler(ExcepcionNegocio.class)
    public ResponseEntity<ApiResponse<Void>> manejarNegocio(ExcepcionNegocio ex) {
        return responder(ex.getEstado(), ex.getMessage(), List.of());
    }
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiResponse<Void>> manejarValidacion(WebExchangeBindException ex) {
        var detalles = ex.getFieldErrors().stream()
            .map(error -> new DetalleError(error.getField(), "VALIDACION", error.getDefaultMessage())).toList();
        return responder(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos.", detalles);
    }
    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ApiResponse<Void>> manejarEntrada(ServerWebInputException ex) {
        return responder(HttpStatus.BAD_REQUEST, "El formato de la solicitud no es válido.", List.of());
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> manejarIntegridad(DataIntegrityViolationException ex) {
        return responder(HttpStatus.CONFLICT, "Los datos generan un conflicto con otro registro o una relación.", List.of());
    }
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> manejarConcurrencia(OptimisticLockingFailureException ex) {
        return responder(HttpStatus.CONFLICT, "Otro proceso modificó el producto. Consulta la información e intenta nuevamente.", List.of());
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> manejarError(Exception ex) {
        LOG.error("Error inesperado al procesar la solicitud", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Se presentó un error interno al procesar la solicitud.", List.of());
    }
    private ResponseEntity<ApiResponse<Void>> responder(HttpStatus estado, String mensaje, List<DetalleError> errores) {
        return ResponseEntity.status(estado).body(ApiResponse.error(String.valueOf(estado.value()), mensaje, errores));
    }
}
