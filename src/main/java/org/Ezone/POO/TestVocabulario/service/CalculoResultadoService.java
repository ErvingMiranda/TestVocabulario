package org.Ezone.POO.TestVocabulario.service;

import java.math.*;
import java.time.*;

import org.Ezone.POO.TestVocabulario.enums.*;
import org.Ezone.POO.TestVocabulario.exception.*;
import org.Ezone.POO.TestVocabulario.model.*;
import org.openxava.jpa.*;

public class CalculoResultadoService implements ICalculoResultadoService {

    @Override
    public ResultadoPrueba calcularResultado(IntentoPrueba intento) {
        validarIntento(intento);
        int totalPreguntas = contarPreguntas(intento.getPrueba());
        int respuestasCorrectas = contarRespuestasCorrectas(intento);
        ResultadoPrueba resultado = buscarResultado(intento);
        if (resultado == null) {
            resultado = new ResultadoPrueba();
            resultado.setIntento(intento);
            XPersistence.getManager().persist(resultado);
        }
        resultado.setTotalPreguntas(totalPreguntas);
        resultado.setRespuestasCorrectas(respuestasCorrectas);
        resultado.setRespuestasIncorrectas(totalPreguntas - respuestasCorrectas);
        resultado.setPorcentaje(calcularPorcentaje(respuestasCorrectas, totalPreguntas));
        resultado.setAprobado(respuestasCorrectas >= intento.getPrueba().getPuntajeMinimoAprobacion());
        resultado.setFechaCalculo(LocalDateTime.now());
        intento.setEstadoIntento(EstadoIntento.CALIFICADO);
        return resultado;
    }

    void validarIntento(IntentoPrueba intento) {
        if (intento == null) {
            throw new CalculoResultadoException("El intento es requerido");
        }
        if (!EstadoIntento.FINALIZADO.equals(intento.getEstadoIntento())) {
            throw new CalculoResultadoException("Solo se puede calcular un intento finalizado");
        }
    }

    ResultadoPrueba buscarResultado(IntentoPrueba intento) {
        return XPersistence.getManager()
            .createQuery("from ResultadoPrueba r where r.intento = :intento", ResultadoPrueba.class)
            .setParameter("intento", intento)
            .getResultList()
            .stream()
            .findFirst()
            .orElse(null);
    }

    int contarPreguntas(PruebaVocabulario prueba) {
        Long total = XPersistence.getManager()
            .createQuery("select count(p) from PreguntaVocabulario p where p.prueba = :prueba and p.activa = true", Long.class)
            .setParameter("prueba", prueba)
            .getSingleResult();
        return total.intValue();
    }

    int contarRespuestasCorrectas(IntentoPrueba intento) {
        Long total = XPersistence.getManager()
            .createQuery("select count(r) from RespuestaEvaluado r where r.intento = :intento and r.correcta = true", Long.class)
            .setParameter("intento", intento)
            .getSingleResult();
        return total.intValue();
    }

    BigDecimal calcularPorcentaje(int respuestasCorrectas, int totalPreguntas) {
        if (totalPreguntas == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(respuestasCorrectas)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(totalPreguntas), 2, RoundingMode.HALF_UP);
    }
}
