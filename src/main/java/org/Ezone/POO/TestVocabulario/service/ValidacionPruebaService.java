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
        Set<Integer> numeros = new HashSet<>();
        for (PreguntaVocabulario pregunta : preguntas(prueba)) {
            if (!pregunta.isActiva()) continue;

            validarPregunta(pregunta);
            if (!numeros.add(pregunta.getNumero())) {
                throw new ValidacionPruebaException(
                    "No puede haber preguntas activas con el mismo numero");
            }
            validarOpciones(opciones(pregunta));
        }
    }

    void validarPregunta(PreguntaVocabulario pregunta) {
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

    List<PreguntaVocabulario> preguntas(PruebaVocabulario prueba) {
        return XPersistence.getManager()
            .createQuery("from PreguntaVocabulario p where p.prueba = :prueba", PreguntaVocabulario.class)
            .setParameter("prueba", prueba)
            .getResultList();
    }

    List<OpcionRespuesta> opciones(PreguntaVocabulario pregunta) {
        return XPersistence.getManager()
            .createQuery("from OpcionRespuesta o where o.pregunta = :pregunta", OpcionRespuesta.class)
            .setParameter("pregunta", pregunta)
            .getResultList();
    }

    void validarOpciones(List<OpcionRespuesta> opciones) {
        if (opciones.size() < 2) {
            throw new ValidacionPruebaException(
                "Cada pregunta activa debe tener al menos dos opciones");
        }

        int totalCorrectas = 0;
        Set<LetraOpcion> letras = new HashSet<>();
        for (OpcionRespuesta opcion : opciones) {
            if (opcion.getLetra() == null) {
                throw new ValidacionPruebaException("Cada opcion debe tener letra");
            }
            if (!letras.add(opcion.getLetra())) {
                throw new ValidacionPruebaException(
                    "No puede haber opciones con la misma letra en una pregunta");
            }
            if (opcion.getTexto() == null || opcion.getTexto().isBlank()) {
                throw new ValidacionPruebaException("Cada opcion debe tener texto");
            }
            if (opcion.isCorrecta()) totalCorrectas++;
        }

        if (totalCorrectas != 1) {
            throw new ValidacionPruebaException(
                "Cada pregunta activa debe tener exactamente una opcion correcta");
        }
    }

    void validarRangosBaremacion(PruebaVocabulario prueba) {
        List<RangoBaremacion> rangos = rangosBaremacion(prueba);
        rangos.sort(Comparator.comparingInt(RangoBaremacion::getPuntajeMinimo));

        int puntajeMaximoPosible = puntajeMaximoPosible(prueba);
        int coberturaHasta = -1;
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
            if (rango.getPuntajeMinimo() > coberturaHasta + 1) {
                throw new ValidacionPruebaException(
                    "Los rangos de baremacion deben cubrir todos los puntajes desde 0 hasta " +
                        puntajeMaximoPosible);
            }
            coberturaHasta = Math.max(coberturaHasta, rango.getPuntajeMaximo());
            anterior = rango;
        }
        if (coberturaHasta < puntajeMaximoPosible) {
            throw new ValidacionPruebaException(
                "Los rangos de baremacion deben cubrir todos los puntajes desde 0 hasta " +
                    puntajeMaximoPosible);
        }
    }

    List<RangoBaremacion> rangosBaremacion(PruebaVocabulario prueba) {
        return XPersistence.getManager()
            .createQuery("from RangoBaremacion r where r.prueba = :prueba", RangoBaremacion.class)
            .setParameter("prueba", prueba)
            .getResultList();
    }

    int puntajeMaximoPosible(PruebaVocabulario prueba) {
        Number total = XPersistence.getManager()
            .createQuery(
                "select sum(p.puntaje) from PreguntaVocabulario p " +
                    "where p.prueba = :prueba and p.activa = true and p.ejemplo = false and p.puntuable = true",
                Number.class)
            .setParameter("prueba", prueba)
            .getSingleResult();
        return total == null ? 0 : total.intValue();
    }
}
