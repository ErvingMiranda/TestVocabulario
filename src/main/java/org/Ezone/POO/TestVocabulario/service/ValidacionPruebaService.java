package org.Ezone.POO.TestVocabulario.service;

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
        if (EstadoPrueba.CANCELADA.equals(prueba.getEstadoPrueba())) {
            throw new ValidacionPruebaException("No se puede activar una prueba cancelada");
        }
        int totalPreguntas = contarPreguntas(prueba);
        if (totalPreguntas == 0) {
            throw new ValidacionPruebaException("La prueba debe tener al menos una pregunta activa");
        }
        validarOpcionesPorPregunta(prueba);
    }

    int contarPreguntas(PruebaVocabulario prueba) {
        Long total = XPersistence.getManager()
            .createQuery("select count(p) from PreguntaVocabulario p where p.prueba = :prueba and p.activa = true", Long.class)
            .setParameter("prueba", prueba)
            .getSingleResult();
        return total.intValue();
    }

    void validarOpcionesPorPregunta(PruebaVocabulario prueba) {
        for (PreguntaVocabulario pregunta : preguntasActivas(prueba)) {
            int totalOpciones = contarOpciones(pregunta);
            int totalCorrectas = contarOpcionesCorrectas(pregunta);
            if (totalOpciones < 2) {
                throw new ValidacionPruebaException("Cada pregunta debe tener al menos dos opciones");
            }
            if (totalCorrectas != 1) {
                throw new ValidacionPruebaException("Cada pregunta debe tener exactamente una opcion correcta");
            }
        }
    }

    java.util.List<PreguntaVocabulario> preguntasActivas(PruebaVocabulario prueba) {
        return XPersistence.getManager()
            .createQuery("from PreguntaVocabulario p where p.prueba = :prueba and p.activa = true", PreguntaVocabulario.class)
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
}
