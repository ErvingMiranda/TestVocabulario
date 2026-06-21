package org.Ezone.POO.TestVocabulario.model;

import java.math.*;
import java.time.*;

import javax.persistence.*;

import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@Getter @Setter
public class ResultadoPrueba extends Identifiable {

    @OneToOne(fetch=FetchType.LAZY, optional=false)
    @DescriptionsList(descriptionProperties="codigoAplicacion")
    @Required
    IntentoPrueba intento;

    int totalPreguntas;

    int respuestasCorrectas;

    int respuestasIncorrectas;

    BigDecimal porcentaje;

    @Required
    boolean aprobado;

    LocalDateTime fechaCalculo = LocalDateTime.now();
}
