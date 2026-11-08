package com.empresa.reembolsos.integracion;

import com.empresa.reembolsos.aplicacion.SolicitudInvalidaException;
import com.empresa.reembolsos.aplicacion.SolicitudReembolsoService;
import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SolicitudReembolsoIntegrationTest {

    @Autowired
    private SolicitudReembolsoService servicio;

    @Test
    void flujoCompleto_viaje_calculaMontoYCambiaEstado() {
        SolicitudReembolso solicitud = new SolicitudReembolso();
        solicitud.setEmpleadoId("EMP-001");
        solicitud.setCategoria(CategoriaGasto.VIAJE);
        solicitud.setMontoSolicitado(new BigDecimal("120000"));
        solicitud.setDiasViaje(2);
        solicitud.setDocumentosSoporte(true);
        solicitud.setFechaGasto(LocalDate.of(2026, 11, 1));

        SolicitudReembolso creada = servicio.crear(solicitud);
        assertNotNull(creada.getId());
        assertEquals(EstadoSolicitud.PENDIENTE, creada.getEstado());
        assertEquals(new BigDecimal("120000"), creada.getMontoAprobado());

        SolicitudReembolso aprobada = servicio.cambiarEstado(creada.getId(), EstadoSolicitud.APROBADA);
        assertEquals(EstadoSolicitud.APROBADA, aprobada.getEstado());
    }

    @Test
    void flujoCompleto_representacion_calculaMontoAprobado() {
        SolicitudReembolso solicitud = new SolicitudReembolso();
        solicitud.setEmpleadoId("EMP-002");
        solicitud.setCategoria(CategoriaGasto.REPRESENTACION);
        solicitud.setMontoSolicitado(new BigDecimal("1800000"));
        solicitud.setDocumentosSoporte(true);
        solicitud.setFechaGasto(LocalDate.of(2026, 11, 2));

        SolicitudReembolso creada = servicio.crear(solicitud);
        assertNotNull(creada.getId());
        assertEquals(new BigDecimal("1800000"), creada.getMontoAprobado());
    }

    @Test
    void flujoCompleto_capacitacion_calculaOchentaPorCiento() {
        SolicitudReembolso solicitud = new SolicitudReembolso();
        solicitud.setEmpleadoId("EMP-003");
        solicitud.setCategoria(CategoriaGasto.CAPACITACION);
        solicitud.setMontoSolicitado(new BigDecimal("2500000"));
        solicitud.setDocumentosSoporte(true);
        solicitud.setFechaGasto(LocalDate.of(2026, 11, 3));

        SolicitudReembolso creada = servicio.crear(solicitud);
        assertNotNull(creada.getId());
        assertEquals(0, new BigDecimal("2000000").compareTo(creada.getMontoAprobado()));
    }

    @Test
    void solicitudSinDocumentosSoporte_esRechazada() {
        SolicitudReembolso solicitud = new SolicitudReembolso();
        solicitud.setEmpleadoId("EMP-004");
        solicitud.setCategoria(CategoriaGasto.VIAJE);
        solicitud.setMontoSolicitado(new BigDecimal("50000"));
        solicitud.setDiasViaje(1);
        solicitud.setDocumentosSoporte(false);
        solicitud.setFechaGasto(LocalDate.of(2026, 11, 4));

        SolicitudInvalidaException ex = assertThrows(
                SolicitudInvalidaException.class,
                () -> servicio.crear(solicitud)
        );
        assertEquals("Faltan documentos soporte", ex.getMessage());
    }
}
