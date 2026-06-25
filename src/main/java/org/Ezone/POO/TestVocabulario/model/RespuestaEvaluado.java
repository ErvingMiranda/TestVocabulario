package org.Ezone.POO.TestVocabulario.model;

import java.time.*;

import javax.persistence.*;
import javax.validation.constraints.*;

import org.Ezone.POO.TestVocabulario.enums.*;
import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@View(members = "pregunta; opcionSeleccionada; clasificacionRespuesta")
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

    @ManyToOne(fetch=FetchType.LAZY, optional=true)
    @DescriptionsList(descriptionProperties="letra, texto")
    OpcionRespuesta opcionSeleccionada;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    @NotNull @Required
    ClasificacionRespuesta clasificacionRespuesta = ClasificacionRespuesta.OMITIDA;

    @Required
    boolean correcta;

    LocalDateTime fechaRespuesta = LocalDateTime.now();
}
