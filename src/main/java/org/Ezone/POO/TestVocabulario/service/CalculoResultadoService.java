package org.Ezone.POO.TestVocabulario.service;

import java.math.*;
import java.time.*;
import java.util.*;

import org.Ezone.POO.TestVocabulario.enums.*;
import org.Ezone.POO.TestVocabulario.exception.*;
import org.Ezone.POO.TestVocabulario.model.*;
import org.openxava.jpa.*;

public class CalculoResultadoService implements ICalculoResultadoService {

    @Override
    public ResultadoPrueba calcularResultado(IntentoPrueba intento) {
        validarIntento(intento);
        clasificarRespuestas(intento);

        int totalPreguntas = contarPreguntasPuntuables(intento.getPrueba());
        int cantidadCorrectas = contarPorClasificacion(intento, ClasificacionRespuesta.CORRECTA);
        int cantidadIncorrectas = contarPorClasificacion(intento, ClasificacionRespuesta.INCORRECTA);
        int cantidadNoSe = contarPorClasificacion(intento, ClasificacionRespuesta.NO_SE);
        int cantidadOmitidas = contarOmitidas(intento, totalPreguntas);
        int puntajeDirecto = cantidadCorrectas;
        BigDecimal notaFinal = calcularNotaFinal(intento.getPrueba(), puntajeDirecto);

        ResultadoPrueba resultado = buscarResultado(intento);
        if (resultado == null) {
            resultado = new ResultadoPrueba();
            resultado.setIntento(intento);
            XPersistence.getManager().persist(resultado);
        }

        resultado.setTotalPreguntas(totalPreguntas);
        resultado.setRespuestasCorrectas(cantidadCorrectas);
        resultado.setRespuestasIncorrectas(cantidadIncorrectas);
        resultado.setRespuestasOmitidas(cantidadOmitidas);
        resultado.setCantidadCorrectas(cantidadCorrectas);
        resultado.setCantidadIncorrectas(cantidadIncorrectas);
        resultado.setCantidadNoSe(cantidadNoSe);
        resultado.setCantidadOmitidas(cantidadOmitidas);
        resultado.setPuntajeDirecto(puntajeDirecto);
        resultado.setNotaFinal(notaFinal);
        resultado.setPorcentaje(null);
        resultado.setInterpretacion("Nota final calculada por tabla de baremacion");
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

    void clasificarRespuestas(IntentoPrueba intento) {
        for (RespuestaEvaluado respuesta : respuestas(intento)) {
            ClasificacionRespuesta clasificacion = clasificar(respuesta);
            respuesta.setClasificacionRespuesta(clasificacion);
            respuesta.setCorrecta(ClasificacionRespuesta.CORRECTA.equals(clasificacion));
        }
    }

    List<RespuestaEvaluado> respuestas(IntentoPrueba intento) {
        return XPersistence.getManager()
            .createQuery("from RespuestaEvaluado r where r.intento = :intento", RespuestaEvaluado.class)
            .setParameter("intento", intento)
            .getResultList();
    }

    ClasificacionRespuesta clasificar(RespuestaEvaluado respuesta) {
        if (respuesta.getOpcionSeleccionada() == null) {
            if (ClasificacionRespuesta.NO_SE.equals(respuesta.getClasificacionRespuesta())) {
                return ClasificacionRespuesta.NO_SE;
            }
            return ClasificacionRespuesta.OMITIDA;
        }
        return respuesta.getOpcionSeleccionada().isCorrecta() ?
            ClasificacionRespuesta.CORRECTA :
            ClasificacionRespuesta.INCORRECTA;
    }

    int contarPreguntasPuntuables(PruebaVocabulario prueba) {
        Long total = XPersistence.getManager()
            .createQuery(
                "select count(p) from PreguntaVocabulario p " +
                    "where p.prueba = :prueba and p.activa = true and p.ejemplo = false and p.puntuable = true",
                Long.class)
            .setParameter("prueba", prueba)
            .getSingleResult();
        return total.intValue();
    }

    int contarPorClasificacion(IntentoPrueba intento, ClasificacionRespuesta clasificacion) {
        Long total = XPersistence.getManager()
            .createQuery(
                "select count(r) from RespuestaEvaluado r " +
                    "where r.intento = :intento " +
                    "and r.pregunta.activa = true " +
                    "and r.pregunta.ejemplo = false " +
                    "and r.pregunta.puntuable = true " +
                    "and r.clasificacionRespuesta = :clasificacion",
                Long.class)
            .setParameter("intento", intento)
            .setParameter("clasificacion", clasificacion)
            .getSingleResult();
        return total.intValue();
    }

    int contarOmitidas(IntentoPrueba intento, int totalPreguntas) {
        int omitidasRegistradas = contarPreguntasPorClasificacion(intento, ClasificacionRespuesta.OMITIDA);
        int preguntasConRespuesta = contarPreguntasRespondidasPuntuables(intento);
        int omitidasSinRegistro = Math.max(totalPreguntas - preguntasConRespuesta, 0);
        return omitidasRegistradas + omitidasSinRegistro;
    }

    int contarPreguntasPorClasificacion(IntentoPrueba intento, ClasificacionRespuesta clasificacion) {
        Long total = XPersistence.getManager()
            .createQuery(
                "select count(distinct r.pregunta) from RespuestaEvaluado r " +
                    "where r.intento = :intento " +
                    "and r.pregunta.activa = true " +
                    "and r.pregunta.ejemplo = false " +
                    "and r.pregunta.puntuable = true " +
                    "and r.clasificacionRespuesta = :clasificacion",
                Long.class)
            .setParameter("intento", intento)
            .setParameter("clasificacion", clasificacion)
            .getSingleResult();
        return total.intValue();
    }

    int contarPreguntasRespondidasPuntuables(IntentoPrueba intento) {
        Long total = XPersistence.getManager()
            .createQuery(
                "select count(distinct r.pregunta) from RespuestaEvaluado r " +
                    "where r.intento = :intento " +
                    "and r.pregunta.activa = true " +
                    "and r.pregunta.ejemplo = false " +
                    "and r.pregunta.puntuable = true",
                Long.class)
            .setParameter("intento", intento)
            .getSingleResult();
        return total.intValue();
    }

    BigDecimal calcularNotaFinal(PruebaVocabulario prueba, int puntajeDirecto) {
        BigDecimal nota = buscarNotaEnRango(prueba, puntajeDirecto);
        if (nota != null) {
            return nota;
        }

        Integer puntajeMaximoConfigurado = buscarPuntajeMaximoConfigurado(prueba);
        if (puntajeMaximoConfigurado == null) {
            throw new CalculoResultadoException("Debe configurar al menos un rango de baremacion");
        }
        if (puntajeDirecto > puntajeMaximoConfigurado) {
            return buscarNotaMaxima(prueba);
        }

        throw new CalculoResultadoException(
            "No existe un rango de baremacion para el puntaje directo " + puntajeDirecto);
    }

    BigDecimal buscarNotaEnRango(PruebaVocabulario prueba, int puntajeDirecto) {
        return XPersistence.getManager()
            .createQuery(
                "select r.notaCalculada from RangoBaremacion r " +
                    "where r.prueba = :prueba " +
                    "and r.puntajeMinimo <= :puntajeDirecto " +
                    "and r.puntajeMaximo >= :puntajeDirecto " +
                    "order by r.puntajeMinimo",
                BigDecimal.class)
            .setParameter("prueba", prueba)
            .setParameter("puntajeDirecto", puntajeDirecto)
            .setMaxResults(1)
            .getResultList()
            .stream()
            .findFirst()
            .orElse(null);
    }

    Integer buscarPuntajeMaximoConfigurado(PruebaVocabulario prueba) {
        return XPersistence.getManager()
            .createQuery(
                "select max(r.puntajeMaximo) from RangoBaremacion r where r.prueba = :prueba",
                Integer.class)
            .setParameter("prueba", prueba)
            .getSingleResult();
    }

    BigDecimal buscarNotaMaxima(PruebaVocabulario prueba) {
        return XPersistence.getManager()
            .createQuery(
                "select max(r.notaCalculada) from RangoBaremacion r where r.prueba = :prueba",
                BigDecimal.class)
            .setParameter("prueba", prueba)
            .getSingleResult();
    }
}
