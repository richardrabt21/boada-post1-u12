package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraRepresentacionTest {

    private final CalculadoraRepresentacion calculadora = new CalculadoraRepresentacion();

    @Test
    void montoMenorAlTopeMensual_reembolsaMontoCompleto() {
        SolicitudReembolso s = new SolicitudReembolso();
        s.setCategoria(CategoriaGasto.REPRESENTACION);
        s.setMontoSolicitado(new BigDecimal("1500000"));

        assertEquals(new BigDecimal("1500000"), calculadora.calcular(s));
    }

    @Test
    void montoMayorAlTopeMensual_reembolsaTopeMensual() {
        SolicitudReembolso s = new SolicitudReembolso();
        s.setCategoria(CategoriaGasto.REPRESENTACION);
        s.setMontoSolicitado(new BigDecimal("2500000"));

        assertEquals(new BigDecimal("2000000"), calculadora.calcular(s));
    }
}
