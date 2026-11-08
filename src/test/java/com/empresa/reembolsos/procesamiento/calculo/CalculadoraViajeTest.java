package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraViajeTest {

    private final CalculadoraViaje calculadora = new CalculadoraViaje();

    @Test
    void montoMenorAlTopeDiarioMultiplicado_reembolsaMontoCompleto() {
        SolicitudReembolso s = new SolicitudReembolso();
        s.setCategoria(CategoriaGasto.VIAJE);
        s.setDiasViaje(3); // Tope total = 150000 * 3 = 450000
        s.setMontoSolicitado(new BigDecimal("300000"));

        assertEquals(new BigDecimal("300000"), calculadora.calcular(s));
    }

    @Test
    void montoMayorAlTopeDiarioMultiplicado_reembolsaHastaElTope() {
        SolicitudReembolso s = new SolicitudReembolso();
        s.setCategoria(CategoriaGasto.VIAJE);
        s.setDiasViaje(2); // Tope total = 150000 * 2 = 300000
        s.setMontoSolicitado(new BigDecimal("500000"));

        assertEquals(new BigDecimal("300000"), calculadora.calcular(s));
    }
}
