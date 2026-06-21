package org.Ezone.POO.TestVocabulario.model;

import javax.persistence.*;

import org.Ezone.POO.TestVocabulario.enums.*;
import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@Getter @Setter
public class OpcionRespuesta extends Identifiable {

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @DescriptionsList(descriptionProperties="numero, enunciado")
    @Required
    PreguntaVocabulario pregunta;

    @Enumerated(EnumType.STRING)
    LetraOpcion letra;

    @Column(length=250, nullable=false) @Required
    String texto;

    @Required
    boolean correcta;
}
