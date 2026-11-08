package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraCapacitacionTest {

    private final CalculadoraCapacitacion calculadora = new CalculadoraCapacitacion();

    @Test
    void reembolsaOchentaPorCientoDentroDelTope() {
        SolicitudReembolso s = new SolicitudReembolso();
        s.setCategoria(CategoriaGasto.CAPACITACION);
        s.setMontoSolicitado(new BigDecimal("2000000")); // 80% = 1600000 (< 3000000)

        BigDecimal esperado = new BigDecimal("1600000");
        BigDecimal resultado = calculadora.calcular(s);
        assertEquals(0, esperado.compareTo(resultado));
    }

    @Test
    void reembolsaHastaElTopeAnualSiOchentaPorCientoLoSupera() {
        SolicitudReembolso s = new SolicitudReembolso();
        s.setCategoria(CategoriaGasto.CAPACITACION);
        s.setMontoSolicitado(new BigDecimal("5000000")); // 80% = 4000000 (> 3000000)

        assertEquals(new BigDecimal("3000000"), calculadora.calcular(s));
    }
}
