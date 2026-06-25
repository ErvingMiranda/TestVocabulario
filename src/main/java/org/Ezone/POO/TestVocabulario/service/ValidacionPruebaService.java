package org.Ezone.POO.TestVocabulario.service;

import java.util.*;

import org.Ezone.POO.TestVocabulario.enums.*;
import org.Ezone.POO.TestVocabulario.exception.*;
import org.Ezone.POO.TestVocabulario.model.*;
import org.openxava.jpa.*;

public class ValidacionPruebaService implements IValidacionPruebaService {

    @Override
    public void validarParaActivar(PruebaVocabulario prueba) {
        if (prueba == null) {
            throw new ValidacionPruebaException("La prueba es requerida");
        }
        if (prueba.getEstadoPrueba() == null) {
            throw new ValidacionPruebaException("El estado de la prueba es requerido");
        }
        if (prueba.getTiempoLimiteMinutos() <= 0) {
            throw new ValidacionPruebaException("El tiempo limite debe ser mayor que cero");
        }
        if (EstadoPrueba.CANCELADA.equals(prueba.getEstadoPrueba())) {
            throw new ValidacionPruebaException("No se puede activar una prueba cancelada");
        }
        if (contarPreguntas(prueba) == 0) {
            throw new ValidacionPruebaException("La prueba debe tener al menos una pregunta");
        }
        if (contarPreguntasPuntuables(prueba) == 0) {
            throw new ValidacionPruebaException("La prueba debe tener al menos una pregunta puntuable que no sea de ejemplo");
        }
        if (contarRangosBaremacion(prueba) == 0) {
            throw new ValidacionPruebaException("La prueba debe tener al menos un rango de baremacion");
        }
        validarPreguntas(prueba);
        validarRangosBaremacion(prueba);
    }

    int contarPreguntas(PruebaVocabulario prueba) {
        Long total = XPersistence.getManager()
            .createQuery("select count(p) from PreguntaVocabulario p where p.prueba = :prueba", Long.class)
            .setParameter("prueba", prueba)
            .getSingleResult();
        return total.intValue();
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

    int contarRangosBaremacion(PruebaVocabulario prueba) {
        Long total = XPersistence.getManager()
            .createQuery("select count(r) from RangoBaremacion r where r.prueba = :prueba", Long.class)
            .setParameter("prueba", prueba)
            .getSingleResult();
        return total.intValue();
    }

    void validarPreguntas(PruebaVocabulario prueba) {
        for (PreguntaVocabulario pregunta : preguntas(prueba)) {
            validarPregunta(pregunta);
            if (!puntua(pregunta)) continue;

            int totalOpciones = contarOpciones(pregunta);
            int totalCorrectas = contarOpcionesCorrectas(pregunta);
            if (totalOpciones < 2) {
                throw new ValidacionPruebaException(
                    "Cada pregunta puntuable debe tener al menos dos opciones");
            }
            if (totalCorrectas != 1) {
                throw new ValidacionPruebaException(
                    "Cada pregunta puntuable debe tener exactamente una opcion correcta");
            }
        }
    }

    void validarPregunta(PreguntaVocabulario pregunta) {
        if (!pregunta.isActiva()) {
            throw new ValidacionPruebaException("La prueba no puede tener preguntas inactivas");
        }
        if (pregunta.getNumero() <= 0) {
            throw new ValidacionPruebaException("El numero de pregunta debe ser mayor que cero");
        }
        if (pregunta.getPuntaje() <= 0) {
            throw new ValidacionPruebaException("El puntaje de pregunta debe ser mayor que cero");
        }
        if (pregunta.getEnunciado() == null || pregunta.getEnunciado().isBlank()) {
            throw new ValidacionPruebaException("El enunciado de pregunta es requerido");
        }
    }

    boolean puntua(PreguntaVocabulario pregunta) {
        return pregunta.isActiva() && !pregunta.isEjemplo() && pregunta.isPuntuable();
    }

    List<PreguntaVocabulario> preguntas(PruebaVocabulario prueba) {
        return XPersistence.getManager()
            .createQuery("from PreguntaVocabulario p where p.prueba = :prueba", PreguntaVocabulario.class)
            .setParameter("prueba", prueba)
            .getResultList();
    }

    int contarOpciones(PreguntaVocabulario pregunta) {
        Long total = XPersistence.getManager()
            .createQuery("select count(o) from OpcionRespuesta o where o.pregunta = :pregunta", Long.class)
            .setParameter("pregunta", pregunta)
            .getSingleResult();
        return total.intValue();
    }

    int contarOpcionesCorrectas(PreguntaVocabulario pregunta) {
        Long total = XPersistence.getManager()
            .createQuery("select count(o) from OpcionRespuesta o where o.pregunta = :pregunta and o.correcta = true", Long.class)
            .setParameter("pregunta", pregunta)
            .getSingleResult();
        return total.intValue();
    }

    void validarRangosBaremacion(PruebaVocabulario prueba) {
        List<RangoBaremacion> rangos = rangosBaremacion(prueba);
        rangos.sort(Comparator.comparingInt(RangoBaremacion::getPuntajeMinimo));

        RangoBaremacion anterior = null;
        for (RangoBaremacion rango : rangos) {
            if (rango.getPuntajeMinimo() > rango.getPuntajeMaximo()) {
                throw new ValidacionPruebaException(
                    "El puntaje minimo no puede ser mayor que el puntaje maximo");
            }
            if (anterior != null && rango.getPuntajeMinimo() <= anterior.getPuntajeMaximo()) {
                throw new ValidacionPruebaException(
                    "No puede haber rangos de baremacion solapados para la misma prueba");
            }
            anterior = rango;
        }
    }

    List<RangoBaremacion> rangosBaremacion(PruebaVocabulario prueba) {
        return XPersistence.getManager()
            .createQuery("from RangoBaremacion r where r.prueba = :prueba", RangoBaremacion.class)
            .setParameter("prueba", prueba)
            .getResultList();
    }
}
