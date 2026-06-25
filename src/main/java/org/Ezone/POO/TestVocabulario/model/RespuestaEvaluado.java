package org.Ezone.POO.TestVocabulario.model;

import java.time.*;

import javax.persistence.*;

import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@View(members = "pregunta; opcionSeleccionada")
@Getter @Setter
public class RespuestaEvaluado extends Identifiable {

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @DescriptionsList(descriptionProperties="codigoAplicacion")
    @Required
    IntentoPrueba intento;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @DescriptionsList(descriptionProperties="numero, enunciado")
    @Required
    PreguntaVocabulario pregunta;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @DescriptionsList(descriptionProperties="letra, texto")
    @Required
    OpcionRespuesta opcionSeleccionada;

    @Required
    boolean correcta;

    LocalDateTime fechaRespuesta = LocalDateTime.now();
}
