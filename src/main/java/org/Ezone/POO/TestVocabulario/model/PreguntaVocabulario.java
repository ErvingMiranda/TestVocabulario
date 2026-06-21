package org.Ezone.POO.TestVocabulario.model;

import javax.persistence.*;

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

    @Required
    int numero;

    @Stereotype("MEMO")
    @Required
    String enunciado;

    @Required
    int puntaje = 1;

    @Required
    boolean activa = true;
}
