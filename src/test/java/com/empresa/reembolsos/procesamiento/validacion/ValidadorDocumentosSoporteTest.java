package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidadorDocumentosSoporteTest {

    private final ValidadorDocumentosSoporte validador = new ValidadorDocumentosSoporte();

    @Test
    void conDocumentos_esValida() {
        SolicitudReembolso s = new SolicitudReembolso();
        s.setDocumentosSoporte(true);

        ResultadoValidacion res = validador.validar(s, List.of());
        assertTrue(res.valida());
        assertNull(res.motivoRechazo());
    }

    @Test
    void sinDocumentos_esRechazada() {
        SolicitudReembolso s = new SolicitudReembolso();
        s.setDocumentosSoporte(false);

        ResultadoValidacion res = validador.validar(s, List.of());
        assertFalse(res.valida());
        assertEquals("Faltan documentos soporte", res.motivoRechazo());
    }
}
