package org.Ezone.POO.TestVocabulario.service;

import org.Ezone.POO.TestVocabulario.model.*;

public interface IAplicacionPruebaService {

    IntentoPrueba iniciarPrueba(String codigoAplicacion);

    RespuestaEvaluado registrarRespuesta(IntentoPrueba intento, PreguntaVocabulario pregunta, OpcionRespuesta opcion);

    IntentoPrueba finalizarPrueba(IntentoPrueba intento);
}
