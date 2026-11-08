package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ValidadorDuplicados extends ValidadorSolicitud {

    @Override
    protected ResultadoValidacion validarPropio(SolicitudReembolso solicitud, List<SolicitudReembolso> pendientes) {
        if (pendientes == null || pendientes.isEmpty() || solicitud.getFechaGasto() == null) {
            return ResultadoValidacion.ok();
        }

        boolean existeDuplicado = pendientes.stream()
                .anyMatch(p -> p.getCategoria() == solicitud.getCategoria()
                        && p.getFechaGasto() != null
                        && p.getFechaGasto().getMonth() == solicitud.getFechaGasto().getMonth()
                        && p.getFechaGasto().getYear() == solicitud.getFechaGasto().getYear());

        return existeDuplicado
                ? ResultadoValidacion.rechazar("Ya existe una solicitud pendiente de la misma categoria en el periodo")
                : ResultadoValidacion.ok();
    }
}
