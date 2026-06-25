package org.Ezone.POO.TestVocabulario.model;

import javax.persistence.*;
import javax.validation.constraints.*;

import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@Getter @Setter
public class PreguntaVocabulario extends Identifiable {

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @DescriptionsList(descriptionProperties="nombre")
    @Required
    PruebaVocabulario prueba;

    @Min(1) @Required
    int numero;

    @Stereotype("MEMO")
    @Required
    String enunciado;

    @Min(1) @Required
    int puntaje = 1;

    @Required
    boolean ejemplo;

    @Required
    boolean puntuable = true;

    @Required
    boolean activa = true;
}
