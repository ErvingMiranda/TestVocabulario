package org.Ezone.POO.TestVocabulario.model;

import java.util.*;

import javax.persistence.*;
import javax.validation.constraints.*;

import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@Getter @Setter
@View(members =
    "DatosDeLaPregunta { prueba; numero; ejemplo, puntuable, activa; enunciado; puntaje };" +
    "Opciones { opciones }"
)
@Tab(properties="prueba.nombre, numero, enunciado, ejemplo, puntuable, activa")
public class PreguntaVocabulario extends Identifiable {

    @OneToMany(mappedBy="pregunta", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("letra")
    @ListProperties("letra, texto, correcta")
    Collection<OpcionRespuesta> opciones;

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
    boolean ejemplo = false;

    @Required
    boolean puntuable = true;

    @Required
    boolean activa = true;
}
