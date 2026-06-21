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
        int respuestasIncorrectas = contarRespuestasIncorrectas(intento);
        int respuestasContestadas = contarRespuestasContestadas(intento);
        int respuestasOmitidas = Math.max(totalPreguntas - respuestasContestadas, 0);
        int puntajeDirecto = calcularPuntajeDirecto(intento);
        int puntajeTotal = calcularPuntajeTotal(intento.getPrueba());
        BigDecimal porcentaje = calcularPorcentaje(puntajeDirecto, puntajeTotal);
        ResultadoPrueba resultado = buscarResultado(intento);
        if (resultado == null) {
            resultado = new ResultadoPrueba();
            resultado.setIntento(intento);
            XPersistence.getManager().persist(resultado);
        }
        resultado.setTotalPreguntas(totalPreguntas);
        resultado.setRespuestasCorrectas(respuestasCorrectas);
        resultado.setRespuestasIncorrectas(respuestasIncorrectas);
        resultado.setRespuestasOmitidas(respuestasOmitidas);
        resultado.setPuntajeDirecto(puntajeDirecto);
        resultado.setPorcentaje(porcentaje);
        resultado.setInterpretacion(interpretar(porcentaje));
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

    int contarRespuestasIncorrectas(IntentoPrueba intento) {
        Long total = XPersistence.getManager()
            .createQuery("select count(r) from RespuestaEvaluado r where r.intento = :intento and r.correcta = false", Long.class)
            .setParameter("intento", intento)
            .getSingleResult();
        return total.intValue();
    }

    int contarRespuestasContestadas(IntentoPrueba intento) {
        Long total = XPersistence.getManager()
            .createQuery("select count(r) from RespuestaEvaluado r where r.intento = :intento", Long.class)
            .setParameter("intento", intento)
            .getSingleResult();
        return total.intValue();
    }

    int calcularPuntajeDirecto(IntentoPrueba intento) {
        Long total = XPersistence.getManager()
            .createQuery(
                "select coalesce(sum(r.pregunta.puntaje), 0) from RespuestaEvaluado r where r.intento = :intento and r.correcta = true",
                Long.class)
            .setParameter("intento", intento)
            .getSingleResult();
        return total.intValue();
    }

    int calcularPuntajeTotal(PruebaVocabulario prueba) {
        Long total = XPersistence.getManager()
            .createQuery(
                "select coalesce(sum(p.puntaje), 0) from PreguntaVocabulario p where p.prueba = :prueba and p.activa = true",
                Long.class)
            .setParameter("prueba", prueba)
            .getSingleResult();
        return total.intValue();
    }

    BigDecimal calcularPorcentaje(int puntajeDirecto, int puntajeTotal) {
        if (puntajeTotal == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(puntajeDirecto)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(puntajeTotal), 2, RoundingMode.HALF_UP);
    }

    String interpretar(BigDecimal porcentaje) {
        if (porcentaje.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return "Desempeno alto en vocabulario";
        }
        if (porcentaje.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return "Desempeno esperado en vocabulario";
        }
        if (porcentaje.compareTo(BigDecimal.valueOf(40)) >= 0) {
            return "Desempeno bajo en vocabulario";
        }
        return "Desempeno muy bajo en vocabulario";
    }
}
