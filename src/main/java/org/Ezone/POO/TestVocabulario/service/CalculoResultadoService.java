package org.Ezone.POO.TestVocabulario.service;

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
        new ValidacionPruebaService().validarParaActivar(intento.getPrueba());
        clasificarRespuestas(intento);

        int totalPreguntas = contarPreguntasPuntuables(intento.getPrueba());
        int cantidadCorrectas = contarPorClasificacion(intento, ClasificacionRespuesta.CORRECTA);
        int cantidadIncorrectas = contarPorClasificacion(intento, ClasificacionRespuesta.INCORRECTA);
        int cantidadNoSe = contarPorClasificacion(intento, ClasificacionRespuesta.NO_SE);
        int cantidadOmitidas = contarOmitidas(intento, totalPreguntas);
        int puntajeDirecto = calcularPuntajeDirecto(intento);
        RangoBaremacion rangoBaremacion = buscarRangoBaremacion(intento.getPrueba(), puntajeDirecto);

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
        resultado.setNotaFinal(rangoBaremacion.getNotaCalculada());
        resultado.setPorcentaje(null);
        resultado.setInterpretacion(rangoBaremacion.getInterpretacion());
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
            .setMaxResults(1)
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

    int calcularPuntajeDirecto(IntentoPrueba intento) {
        Number total = XPersistence.getManager()
            .createQuery(
                "select sum(r.pregunta.puntaje) from RespuestaEvaluado r " +
                    "where r.intento = :intento " +
                    "and r.pregunta.activa = true " +
                    "and r.pregunta.ejemplo = false " +
                    "and r.pregunta.puntuable = true " +
                    "and r.clasificacionRespuesta = :clasificacion",
                Number.class)
            .setParameter("intento", intento)
            .setParameter("clasificacion", ClasificacionRespuesta.CORRECTA)
            .getSingleResult();
        return total == null ? 0 : total.intValue();
    }

    int contarPorClasificacion(IntentoPrueba intento, ClasificacionRespuesta clasificacion) {
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

    RangoBaremacion buscarRangoBaremacion(PruebaVocabulario prueba, int puntajeDirecto) {
        RangoBaremacion rango = buscarRangoEnPuntaje(prueba, puntajeDirecto);
        if (rango != null) {
            return rango;
        }

        Integer puntajeMaximoConfigurado = buscarPuntajeMaximoConfigurado(prueba);
        if (puntajeMaximoConfigurado == null) {
            throw new CalculoResultadoException("Debe configurar al menos un rango de baremacion");
        }
        if (puntajeDirecto > puntajeMaximoConfigurado) {
            return buscarRangoMaximoConfigurado(prueba);
        }

        throw new CalculoResultadoException(
            "No existe un rango de baremacion para el puntaje directo " + puntajeDirecto);
    }

    RangoBaremacion buscarRangoEnPuntaje(PruebaVocabulario prueba, int puntajeDirecto) {
        return XPersistence.getManager()
            .createQuery(
                "from RangoBaremacion r " +
                    "where r.prueba = :prueba " +
                    "and r.puntajeMinimo <= :puntajeDirecto " +
                    "and r.puntajeMaximo >= :puntajeDirecto " +
                    "order by r.puntajeMinimo",
                RangoBaremacion.class)
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

    RangoBaremacion buscarRangoMaximoConfigurado(PruebaVocabulario prueba) {
        return XPersistence.getManager()
            .createQuery(
                "from RangoBaremacion r where r.prueba = :prueba order by r.puntajeMaximo desc",
                RangoBaremacion.class)
            .setParameter("prueba", prueba)
            .setMaxResults(1)
            .getSingleResult();
    }
}
