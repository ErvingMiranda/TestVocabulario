package org.Ezone.POO.TestVocabulario.model;

import java.util.*;

import javax.persistence.*;
import javax.validation.constraints.*;

import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"prueba_id", "numero"}))
@Getter @Setter
@View(members =
    "Contexto { prueba; numero, puntaje; activa, ejemplo, puntuable };" +
    "Contenido { enunciado };" +
    "Opciones { opciones }"
)
@Tab(properties="numero, prueba.nombre, enunciado, puntaje, activa, ejemplo, puntuable")
public class PreguntaVocabulario extends Identifiable {

    @OneToMany(mappedBy="pregunta", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("letra")
    @ListProperties("letra, texto, correcta")
    Collection<OpcionRespuesta> opciones;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="prueba_id", nullable=false)
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
    boolean ejemplo = false;

    @Required
    boolean puntuable = true;

    @Required
    boolean activa = true;
}
