package org.Ezone.POO.TestVocabulario.model;

import javax.persistence.*;
import javax.validation.constraints.*;

import org.Ezone.POO.TestVocabulario.enums.*;
import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"pregunta_id", "letra"}))
@Getter @Setter
@View(members = "pregunta; letra; texto; correcta")
@Tab(properties="pregunta.numero, letra, texto, correcta")
public class OpcionRespuesta extends Identifiable {

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="pregunta_id", nullable=false)
    @DescriptionsList(descriptionProperties="numero, enunciado")
    @Required
    PreguntaVocabulario pregunta;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    @NotNull @Required
    LetraOpcion letra;

    @Column(length=250, nullable=false) @Required
    String texto;

    @Required
    boolean correcta;
}
