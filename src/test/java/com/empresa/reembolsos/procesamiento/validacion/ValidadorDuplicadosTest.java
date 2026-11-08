package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidadorDuplicadosTest {

    private final ValidadorDuplicados validador = new ValidadorDuplicados();

    @Test
    void sinSolicitudPendienteMismoPeriodo_esValida() {
        SolicitudReembolso actual = new SolicitudReembolso();
        actual.setCategoria(CategoriaGasto.VIAJE);
        actual.setFechaGasto(LocalDate.of(2026, 11, 5));

        SolicitudReembolso otra = new SolicitudReembolso();
        otra.setCategoria(CategoriaGasto.REPRESENTACION);
        otra.setFechaGasto(LocalDate.of(2026, 11, 1));

        ResultadoValidacion res = validador.validar(actual, List.of(otra));
        assertTrue(res.valida());
    }

    @Test
    void conSolicitudPendienteMismoPeriodoYCategoria_esRechazada() {
        SolicitudReembolso actual = new SolicitudReembolso();
        actual.setCategoria(CategoriaGasto.VIAJE);
        actual.setFechaGasto(LocalDate.of(2026, 11, 5));

        SolicitudReembolso duplicada = new SolicitudReembolso();
        duplicada.setCategoria(CategoriaGasto.VIAJE);
        duplicada.setFechaGasto(LocalDate.of(2026, 11, 20));

        ResultadoValidacion res = validador.validar(actual, List.of(duplicada));
        assertFalse(res.valida());
        assertEquals("Ya existe una solicitud pendiente de la misma categoria en el periodo", res.motivoRechazo());
    }
}
