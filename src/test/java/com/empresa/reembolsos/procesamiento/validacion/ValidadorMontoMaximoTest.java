package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidadorMontoMaximoTest {

    private final ValidadorMontoMaximo validador = new ValidadorMontoMaximo();

    @Test
    void montoDentroDelTope_esValida() {
        SolicitudReembolso s = new SolicitudReembolso();
        s.setCategoria(CategoriaGasto.VIAJE);
        s.setMontoSolicitado(new BigDecimal("150000"));

        ResultadoValidacion res = validador.validar(s, List.of());
        assertTrue(res.valida());
    }

    @Test
    void montoExcedeTope_esRechazada() {
        SolicitudReembolso s = new SolicitudReembolso();
        s.setCategoria(CategoriaGasto.VIAJE);
        s.setMontoSolicitado(new BigDecimal("150001"));

        ResultadoValidacion res = validador.validar(s, List.of());
        assertFalse(res.valida());
        assertTrue(res.motivoRechazo().contains("Monto excede el tope de la categoria VIAJE"));
    }
}
