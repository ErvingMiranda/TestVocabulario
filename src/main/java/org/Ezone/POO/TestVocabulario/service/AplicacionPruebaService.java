package org.Ezone.POO.TestVocabulario.service;

import java.time.*;
import java.util.*;

import javax.persistence.*;

import org.Ezone.POO.TestVocabulario.enums.*;
import org.Ezone.POO.TestVocabulario.exception.*;
import org.Ezone.POO.TestVocabulario.model.*;
import org.openxava.jpa.*;

public class AplicacionPruebaService implements IAplicacionPruebaService {

    @Override
    public IntentoPrueba iniciarPrueba(String codigoAplicacion) {
        IntentoPrueba intento = buscarIntento(codigoAplicacion);
        validarInicio(intento);
        intento.setEstadoIntento(EstadoIntento.EN_PROGRESO);
        intento.setFechaInicio(LocalDateTime.now());
        return intento;
    }

    @Override
    public RespuestaEvaluado registrarRespuesta(IntentoPrueba intento, PreguntaVocabulario pregunta, OpcionRespuesta opcion) {
        return registrarRespuesta(intento, pregunta, opcion, null);
    }

    @Override
    public RespuestaEvaluado registrarRespuesta(IntentoPrueba intento, PreguntaVocabulario pregunta,
        OpcionRespuesta opcion, ClasificacionRespuesta clasificacionRespuesta) {

        ClasificacionRespuesta clasificacion = resolverClasificacion(opcion, clasificacionRespuesta);
        intento = bloquearIntento(intento);
        validarRespuesta(intento, pregunta, opcion, clasificacion);
        RespuestaEvaluado respuesta = buscarRespuesta(intento, pregunta);
        if (respuesta != null && ClasificacionRespuesta.OMITIDA.equals(clasificacion)) {
            intento.setNumeroRespuestas(contarRespuestas(intento));
            return respuesta;
        }
        if (respuesta == null) {
            respuesta = new RespuestaEvaluado();
            respuesta.setIntento(intento);
            respuesta.setPregunta(pregunta);
            XPersistence.getManager().persist(respuesta);
        }
        respuesta.setOpcionSeleccionada(opcion);
        respuesta.setClasificacionRespuesta(clasificacion);
        respuesta.setCorrecta(ClasificacionRespuesta.CORRECTA.equals(clasificacion));
        respuesta.setFechaRespuesta(LocalDateTime.now());
        intento.setNumeroRespuestas(contarRespuestas(intento));
        return respuesta;
    }

    @Override
    public IntentoPrueba finalizarPrueba(IntentoPrueba intento) {
        if (intento == null) {
            throw new AplicacionPruebaException("El intento es requerido");
        }
        intento = bloquearIntento(intento);
        if (EstadoIntento.FINALIZADO.equals(intento.getEstadoIntento()) ||
            EstadoIntento.CALIFICADO.equals(intento.getEstadoIntento())) {

            return intento;
        }
        if (!EstadoIntento.EN_PROGRESO.equals(intento.getEstadoIntento())) {
            throw new AplicacionPruebaException("Solo se puede finalizar una prueba en progreso");
        }
        validarConfiguracionPrueba(intento.getPrueba());
        registrarOmisiones(intento);
        intento.setEstadoIntento(EstadoIntento.FINALIZADO);
        intento.setFechaFin(LocalDateTime.now());
        intento.setNumeroRespuestas(contarRespuestas(intento));
        return intento;
    }

    IntentoPrueba buscarIntento(String codigoAplicacion) {
        if (codigoAplicacion == null || codigoAplicacion.isBlank()) {
            throw new AplicacionPruebaException("El codigo de aplicacion es requerido");
        }
        try {
            return XPersistence.getManager()
                .createQuery("from IntentoPrueba i where i.codigoAplicacion = :codigo", IntentoPrueba.class)
                .setParameter("codigo", codigoAplicacion)
                .getSingleResult();
        }
        catch (NoResultException ex) {
            throw new AplicacionPruebaException("No existe un intento para el codigo de aplicacion indicado");
        }
    }

    IntentoPrueba bloquearIntento(IntentoPrueba intento) {
        if (intento == null) {
            throw new AplicacionPruebaException("El intento es requerido");
        }

        EntityManager manager = XPersistence.getManager();
        IntentoPrueba intentoGestionado = intento;
        if (!manager.contains(intento)) {
            if (intento.getId() == null) {
                throw new AplicacionPruebaException("El intento debe estar persistido");
            }
            intentoGestionado = manager.find(IntentoPrueba.class, intento.getId(), LockModeType.PESSIMISTIC_WRITE);
            if (intentoGestionado == null) {
                throw new AplicacionPruebaException("No existe el intento indicado");
            }
        }
        else {
            manager.lock(intentoGestionado, LockModeType.PESSIMISTIC_WRITE);
        }
        return intentoGestionado;
    }

    RespuestaEvaluado buscarRespuesta(IntentoPrueba intento, PreguntaVocabulario pregunta) {
        return XPersistence.getManager()
            .createQuery(
                "from RespuestaEvaluado r where r.intento = :intento and r.pregunta = :pregunta order by r.fechaRespuesta desc",
                RespuestaEvaluado.class)
            .setParameter("intento", intento)
            .setParameter("pregunta", pregunta)
            .setMaxResults(1)
            .getResultList()
            .stream()
            .findFirst()
            .orElse(null);
    }

    int contarRespuestas(IntentoPrueba intento) {
        Long total = XPersistence.getManager()
            .createQuery("select count(r) from RespuestaEvaluado r where r.intento = :intento", Long.class)
            .setParameter("intento", intento)
            .getSingleResult();
        return total.intValue();
    }

    void registrarOmisiones(IntentoPrueba intento) {
        Map<String, RespuestaEvaluado> respuestasPorPregunta = respuestasPorPregunta(intento);
        LocalDateTime fechaOmisiones = LocalDateTime.now();

        for (PreguntaVocabulario pregunta : preguntasActivas(intento.getPrueba())) {
            if (!respuestasPorPregunta.containsKey(pregunta.getId())) {
                RespuestaEvaluado respuesta = new RespuestaEvaluado();
                respuesta.setIntento(intento);
                respuesta.setPregunta(pregunta);
                respuesta.setClasificacionRespuesta(ClasificacionRespuesta.OMITIDA);
                respuesta.setCorrecta(false);
                respuesta.setFechaRespuesta(fechaOmisiones);
                XPersistence.getManager().persist(respuesta);
                respuestasPorPregunta.put(pregunta.getId(), respuesta);
            }
        }
    }

    Map<String, RespuestaEvaluado> respuestasPorPregunta(IntentoPrueba intento) {
        List<RespuestaEvaluado> respuestas = XPersistence.getManager()
            .createQuery(
                "from RespuestaEvaluado r where r.intento = :intento order by r.fechaRespuesta desc",
                RespuestaEvaluado.class)
            .setParameter("intento", intento)
            .getResultList();

        Map<String, RespuestaEvaluado> respuestasPorPregunta = new HashMap<>();
        for (RespuestaEvaluado respuesta : respuestas) {
            PreguntaVocabulario pregunta = respuesta.getPregunta();
            if (pregunta != null && pregunta.getId() != null) {
                respuestasPorPregunta.putIfAbsent(pregunta.getId(), respuesta);
            }
        }
        return respuestasPorPregunta;
    }

    java.util.List<PreguntaVocabulario> preguntasActivas(PruebaVocabulario prueba) {
        return XPersistence.getManager()
            .createQuery("from PreguntaVocabulario p where p.prueba = :prueba and p.activa = true", PreguntaVocabulario.class)
            .setParameter("prueba", prueba)
            .getResultList();
    }

    ClasificacionRespuesta resolverClasificacion(OpcionRespuesta opcion, ClasificacionRespuesta clasificacionRespuesta) {
        if (opcion != null) {
            return opcion.isCorrecta() ? ClasificacionRespuesta.CORRECTA : ClasificacionRespuesta.INCORRECTA;
        }
        if (ClasificacionRespuesta.NO_SE.equals(clasificacionRespuesta)) {
            return ClasificacionRespuesta.NO_SE;
        }
        return ClasificacionRespuesta.OMITIDA;
    }

    void validarInicio(IntentoPrueba intento) {
        if (!EstadoIntento.PENDIENTE.equals(intento.getEstadoIntento())) {
            throw new AplicacionPruebaException("Solo se puede iniciar una prueba pendiente");
        }
        if (intento.getPrueba() == null || !intento.getPrueba().activa()) {
            throw new AplicacionPruebaException("La prueba no esta activa");
        }
        validarConfiguracionPrueba(intento.getPrueba());
    }

    void validarConfiguracionPrueba(PruebaVocabulario prueba) {
        try {
            new ValidacionPruebaService().validarParaActivar(prueba);
        }
        catch (ValidacionPruebaException ex) {
            throw new AplicacionPruebaException(ex.getMessage());
        }
    }

    void validarRespuesta(IntentoPrueba intento, PreguntaVocabulario pregunta, OpcionRespuesta opcion,
        ClasificacionRespuesta clasificacionRespuesta) {

        if (intento == null || pregunta == null || clasificacionRespuesta == null) {
            throw new AplicacionPruebaException("Intento, pregunta y clasificacion son requeridos");
        }
        if (!EstadoIntento.EN_PROGRESO.equals(intento.getEstadoIntento())) {
            throw new AplicacionPruebaException("Solo se puede responder una prueba en progreso");
        }
        if (!pregunta.getPrueba().equals(intento.getPrueba())) {
            throw new AplicacionPruebaException("La pregunta no pertenece a la prueba del intento");
        }
        if (ClasificacionRespuesta.NO_SE.equals(clasificacionRespuesta) &&
            !pregunta.getPrueba().isPermiteNoSe()) {

            throw new AplicacionPruebaException("La prueba no permite respuestas NO_SE");
        }
        if (opcion == null) {
            if (ClasificacionRespuesta.CORRECTA.equals(clasificacionRespuesta) ||
                ClasificacionRespuesta.INCORRECTA.equals(clasificacionRespuesta)) {

                throw new AplicacionPruebaException("Una respuesta correcta o incorrecta requiere opcion seleccionada");
            }
            return;
        }
        if (!opcion.getPregunta().equals(pregunta)) {
            throw new AplicacionPruebaException("La opcion no pertenece a la pregunta indicada");
        }
    }
}
