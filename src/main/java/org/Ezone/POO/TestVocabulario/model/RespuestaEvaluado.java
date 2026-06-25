package org.Ezone.POO.TestVocabulario.model;

import java.time.*;

import javax.persistence.*;
import javax.validation.constraints.*;

import org.Ezone.POO.TestVocabulario.enums.*;
import org.openxava.annotations.*;
import org.openxava.model.*;

import lombok.*;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"intento_id", "pregunta_id"}))
@View(members = "pregunta; opcionSeleccionada; clasificacionRespuesta")
@Getter @Setter
public class RespuestaEvaluado extends Identifiable {

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="intento_id", nullable=false)
    @DescriptionsList(descriptionProperties="codigoAplicacion")
    @Required
    IntentoPrueba intento;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="pregunta_id", nullable=false)
    @DescriptionsList(descriptionProperties="numero, enunciado")
    @Required
    PreguntaVocabulario pregunta;

    @ManyToOne(fetch=FetchType.LAZY, optional=true)
    @JoinColumn(name="opcion_seleccionada_id")
    @DescriptionsList(descriptionProperties="letra, texto")
    OpcionRespuesta opcionSeleccionada;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    @NotNull @Required
    ClasificacionRespuesta clasificacionRespuesta = ClasificacionRespuesta.OMITIDA;

    @Required
    boolean correcta;

    LocalDateTime fechaRespuesta = LocalDateTime.now();

    @Hidden
    @AssertTrue(message="Las respuestas correctas o incorrectas requieren una opcion seleccionada; NO_SE y OMITIDA no deben tener opcion")
    public boolean isOpcionCoherenteConClasificacion() {
        if (clasificacionRespuesta == null) return false;
        if (ClasificacionRespuesta.CORRECTA.equals(clasificacionRespuesta) ||
            ClasificacionRespuesta.INCORRECTA.equals(clasificacionRespuesta)) {

            return opcionSeleccionada != null;
        }
        return opcionSeleccionada == null;
    }

    @PrePersist
    @PreUpdate
    void sincronizarCorrecta() {
        correcta = ClasificacionRespuesta.CORRECTA.equals(clasificacionRespuesta);
    }
}
